use axum::{
    extract::{Query, State, FromRequestParts, Path},
    http::StatusCode,
    http::request::Parts,
    response::IntoResponse,
    Json,
};
use sqlx::SqlitePool;
use rand::rngs::OsRng;
use argon2::{
    password_hash::{
        PasswordHash, 
        PasswordHasher, 
        PasswordVerifier, 
        SaltString
    }, 
    Argon2,
};
use jsonwebtoken::{encode, decode, Header, EncodingKey, DecodingKey, Validation};
use serde_json::json;

// 從 models 模組引入結構體
use crate::models::{
    ApiResponse, 
    Record, 
    RecordPayload, 
    RecordQuery, 
    SummaryResponse, 
    AuthPayload,
    LoginResponse,
    Claims,
    UserRow
};

// POST /api/records
pub async fn create_record(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Json(payload): Json<RecordPayload>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub; // 從 Token 中拿出剛剛登入的使用者 ID·
    tracing::info!("preparing to write data into db: {}", user_id);

    // SQL 語法加入 user_id
    let result = sqlx::query(
        "INSERT INTO records (user_id, amount, category, record_type, date, note) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(user_id)
    .bind(payload.amount)
    .bind(&payload.category)
    .bind(&payload.record_type)
    .bind(&payload.date)
    .bind(&payload.note)
    .execute(&pool)
    .await;
    match result {
        Ok(_) => {
            let response = ApiResponse {
                status: "success".to_string(),
                message: "data has been written into db successfully!".to_string(),
            };
            (StatusCode::CREATED, Json(response))
        }
        Err(e) => {
            tracing::error!("failed to write data into db: {}", e);
            let response = ApiResponse {
                status: "error".to_string(),
                message: "server error, failed to save data".to_string(),
            };
            (StatusCode::INTERNAL_SERVER_ERROR, Json(response))
        }
    }
}

// 獲取資料
pub async fn get_records(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Query(params): Query<RecordQuery>,
) -> (StatusCode, Json<Vec<Record>>) {
    let user_id = claims.sub;
    let limit = params.limit.unwrap_or(50);
    let offset = params.offset.unwrap_or(0);

    let result = if let Some(month) = params.month {
        let month_pattern = format!("{}%", month);
        sqlx::query_as::<_, Record>(
            "SELECT * FROM records WHERE user_id = ? AND date LIKE ? ORDER BY date DESC LIMIT ? OFFSET ?",
        )
        .bind(user_id)
        .bind(month_pattern)
        .bind(limit)
        .bind(offset)
        .fetch_all(&pool)
        .await
    } else {
        sqlx::query_as::<_, Record>("SELECT * FROM records WHERE user_id = ? ORDER BY date DESC LIMIT ? OFFSET ?")
            .bind(user_id)
            .bind(limit)
            .bind(offset)
            .fetch_all(&pool)
            .await
    };

    match result {
        Ok(data) => (StatusCode::OK, Json(data)),
        Err(e) => {
            tracing::error!("failed to get records from db: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(vec![]))
        }
    }
}

// 獲取統計資料
pub async fn get_summary(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Query(params): Query<RecordQuery>,
) -> (StatusCode, Json<SummaryResponse>) {
    let user_id = claims.sub;
    let month_pattern = params
        .month
        .map(|m| format!("{}%", m))
        .unwrap_or_else(|| "%".to_string());

    let record = sqlx::query_as::<_, SummaryResponse>(
        r#"
        SELECT 
            CAST(COALESCE(SUM(CASE WHEN record_type = 'expense' THEN amount ELSE 0 END), 0.0) AS REAL) as total_expense,
            CAST(COALESCE(SUM(CASE WHEN record_type = 'income' THEN amount ELSE 0 END), 0.0) AS REAL) as total_income
        FROM records
        WHERE user_id = ? AND date LIKE ?
        "#,
    )
    .bind(user_id)
    .bind(month_pattern)
    .fetch_one(&pool)
    .await;

    match record {
        Ok(row) => (
            StatusCode::OK,
            Json(SummaryResponse {
                total_expense: row.total_expense,
                total_income: row.total_income,
            }),
        ),
        Err(e) => {
            tracing::error!("failed to get summary from db: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(SummaryResponse {
                    total_expense: 0.0,
                    total_income: 0.0,
                }),
            )
        }
    }
}

// 註冊
pub async fn register(
    State(pool): State<SqlitePool>,
    Json(payload): Json<AuthPayload>,
) -> (StatusCode, Json<ApiResponse>) {
    // 1. 雜湊密碼
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let password_hash = argon2
        .hash_password(payload.password.as_bytes(), &salt)
        .expect("password hashing failed")
        .to_string();

    // 2. 存入資料庫
    let result = sqlx::query(
        "INSERT INTO users (username, password_hash) VALUES (?, ?)"
    )
    .bind(&payload.username)
    .bind(password_hash)
    .execute(&pool)
    .await;

    match result {
        Ok(_) => (StatusCode::CREATED, Json(ApiResponse { status: "success".to_string(), message: "success".to_string() })),
        Err(_) => (StatusCode::BAD_REQUEST, Json(ApiResponse { status: "error".to_string(), message: "account already exists".to_string() })),
    }
}

// 登入
pub async fn login(
    State(pool): State<SqlitePool>,
    Json(payload): Json<AuthPayload>,
) -> axum::response::Response {
    // 1. 從資料庫抓取使用者
    let user = sqlx::query_as::<_, UserRow>("SELECT id, password_hash FROM users WHERE username = ?")
        .bind(&payload.username)
        .fetch_optional(&pool)
        .await;

    if let Ok(Some(user_row)) = user {
        // 2. 比對密碼
        let parsed_hash = PasswordHash::new(&user_row.password_hash).unwrap();
        if Argon2::default().verify_password(payload.password.as_bytes(), &parsed_hash).is_ok() {
            
            // 3. 密碼正確，簽發 JWT
            let claims = Claims {
                sub: user_row.id, // 把 user_id 塞進 Token 裡
                exp: 10000000000, // 過期時間 (實際開發建議設定較短)
            };
            let token = encode(&Header::default(), &claims, &EncodingKey::from_secret("secret_key".as_ref())).unwrap();

            return (
                StatusCode::OK, 
                Json(LoginResponse { 
                    status: "success".to_string(), 
                    token,
                    username: payload.username.clone(),
                })
            ).into_response();
        }
    }

    (
        StatusCode::UNAUTHORIZED, 
        Json(ApiResponse { 
            status: "error".to_string(), 
            message: "account or password error".to_string() 
        })
    ).into_response()
}

// 獲取當前使用者資訊
pub async fn get_me(
    State(pool): State<SqlitePool>,
    claims: Claims,
) -> (StatusCode, Json<serde_json::Value>) {
    let result = sqlx::query_scalar::<_, String>(
        "SELECT username FROM users WHERE id = ?"
    )
    .bind(claims.sub)
    .fetch_one(&pool)
    .await;

    match result {
        Ok(username) => (StatusCode::OK, Json(json!({"username": username}))),
        Err(e) => {
            tracing::error!("failed to get user info: {}", e);
            (StatusCode::NOT_FOUND, Json(json!({"error": "user not found"})))
        }
    }
}

impl<S> FromRequestParts<S> for Claims
where
    S: Send + Sync,
{
    // 如果驗證失敗，回傳 401 Unauthorized 與錯誤 JSON
    type Rejection = (StatusCode, Json<serde_json::Value>);

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        // 1. 尋找 Authorization header
        let auth_header = parts
            .headers
            .get(axum::http::header::AUTHORIZATION)
            .and_then(|val| val.to_str().ok());

        match auth_header {
            Some(header) if header.starts_with("Bearer ") => {
                // 2. 切割出真正的 Token 字串
                let token = &header["Bearer ".len()..];
                
                // 3. 解碼並驗證 Token
                let token_data = decode::<Claims>(
                    token,
                    &DecodingKey::from_secret("secret_key".as_ref()),
                    &Validation::default(),
                ).map_err(|_| {
                    (StatusCode::UNAUTHORIZED, Json(json!({"status": "error", "message": "Invalid or expired token"})))
                })?;

                // 回傳 claims (包含 user_id)
                Ok(token_data.claims)
            }
            // 沒帶 Token 或格式錯誤
            _ => Err((StatusCode::UNAUTHORIZED, Json(json!({"status": "error", "message": "Missing Authorization Token"})))),
        }
    }
}

// 刪除
pub async fn delete_record(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Path(id): Path<i32>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub;

    tracing::info!("preparing to delete record with id: {}, user_id: {}", id, user_id);

    // 執行刪除
    let result = sqlx::query("DELETE FROM records WHERE id = ? AND user_id = ?")
        .bind(id)
        .bind(user_id)
        .execute(&pool)
        .await;

    match result {
        Ok(row) if row.rows_affected() > 0 => {
            (StatusCode::OK, Json(ApiResponse { status: "success".to_string(), message: "record deleted".to_string() }))
        }
        Ok(_) => {
            (StatusCode::NOT_FOUND, Json(ApiResponse { status: "error".to_string(), message: "record not found".to_string() }))
        }
        Err(e) => {
            tracing::error!("failed to delete record: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(ApiResponse { status: "error".to_string(), message: "server error".to_string() }))
        }
    }
}

// edit
pub async fn update_record(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Path(id): Path<i32>,
    Json(payload): Json<RecordPayload>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub;

    tracing::info!("preparing to update record with id: {}, user_id: {}", id, user_id);
    
    let result = sqlx::query(
        r#"
        UPDATE records 
        SET amount = ?, category = ?, record_type = ?, date = ?, note = ? 
        WHERE id = ? AND user_id = ?
        "#
    )
        .bind(payload.amount)
        .bind(payload.category)
        .bind(payload.record_type)
        .bind(payload.date)
        .bind(payload.note)
        .bind(id)
        .bind(user_id)
        .execute(&pool)
        .await;

    match result {
        Ok(row) if row.rows_affected() > 0 => {
            (StatusCode::OK, Json(ApiResponse { status: "success".to_string(), message: "record updated".to_string() }))
        }
        Ok(_) => {
            (StatusCode::NOT_FOUND, Json(ApiResponse { status: "error".to_string(), message: "record not found".to_string() }))
        }
        Err(e) => {
            tracing::error!("failed to update record: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(ApiResponse { status: "error".to_string(), message: "server error".to_string() }))
        }
    }
}