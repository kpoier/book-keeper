use axum::{
    extract::{Query, State, FromRequestParts, Path},
    http::{StatusCode, header},
    http::request::Parts,
    response::IntoResponse,
    Json,
};
use uuid::Uuid;
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
    UserRow,
    UserSettingsPayload,
    UserSettingsResponse
};

// POST /api/records
pub async fn create_record(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Json(payload): Json<RecordPayload>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub; // 從 Token 中拿出剛剛登入的使用者 ID·
    tracing::info!("preparing to write data into db: {}", user_id);

    // SQL 語法加入 user_id, uuid
    let record_id = Uuid::new_v4().to_string();
    let result = sqlx::query(
        "INSERT INTO records (id, user_id, amount, category, record_type, date, note, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))"
    )
    .bind(&record_id)
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
            "SELECT * FROM records WHERE user_id = ? AND date LIKE ? AND deleted_at IS NULL ORDER BY date DESC LIMIT ? OFFSET ?",
        )
        .bind(user_id)
        .bind(month_pattern)
        .bind(limit)
        .bind(offset)
        .fetch_all(&pool)
        .await
    } else {
        sqlx::query_as::<_, Record>("SELECT * FROM records WHERE user_id = ? AND deleted_at IS NULL ORDER BY date DESC LIMIT ? OFFSET ?")
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
        WHERE user_id = ? AND date LIKE ? AND deleted_at IS NULL
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
    Path(id): Path<String>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub;

    tracing::info!("preparing to delete record with id: {}, user_id: {}", id, user_id);

    // 執行軟刪除
    let result = sqlx::query("UPDATE records SET deleted_at = datetime('now', 'localtime'), updated_at = datetime('now', 'localtime') WHERE id = ? AND user_id = ?")
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
    Path(id): Path<String>,
    Json(payload): Json<RecordPayload>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub;

    tracing::info!("preparing to update record with id: {}, user_id: {}", id, user_id);
    
    let result = sqlx::query(
        r#"
        UPDATE records 
        SET amount = ?, category = ?, record_type = ?, date = ?, note = ?, updated_at = datetime('now', 'localtime') 
        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
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

// 導出資料 (CSV)
pub async fn export_records(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Query(params): Query<RecordQuery>,
) -> impl IntoResponse {
    let user_id = claims.sub;
    tracing::info!("exporting records for user_id: {}", user_id);

    let records_result = if let Some(month) = params.month {
        let month_pattern = format!("{}%", month);
        sqlx::query_as::<_, Record>(
            "SELECT * FROM records WHERE user_id = ? AND date LIKE ? AND deleted_at IS NULL ORDER BY date DESC"
        )
        .bind(user_id)
        .bind(month_pattern)
        .fetch_all(&pool)
        .await
    } else {
        sqlx::query_as::<_, Record>(
            "SELECT * FROM records WHERE user_id = ? AND deleted_at IS NULL ORDER BY date DESC"
        )
        .bind(user_id)
        .fetch_all(&pool)
        .await
    };

    match records_result {
        Ok(records) => {
            let mut wtr = csv::Writer::from_writer(vec![]);
            for r in records {
                let _ = wtr.serialize(r);
            }
            let data = String::from_utf8(wtr.into_inner().unwrap()).unwrap();
            
            let mut headers = header::HeaderMap::new();
            headers.insert(
                header::CONTENT_TYPE,
                "text/csv; charset=utf-8".parse().unwrap(),
            );
            headers.insert(
                header::CONTENT_DISPOSITION,
                "attachment; filename=\"records.csv\"".parse().unwrap(),
            );

            (StatusCode::OK, headers, data).into_response()
        }
        Err(e) => {
            tracing::error!("failed to export records: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "Failed to export data").into_response()
        }
    }
}

// 復原被刪除的資料
pub async fn restore_record(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Path(id): Path<String>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub;

    tracing::info!("preparing to restore record with id: {}, user_id: {}", id, user_id);

    let result = sqlx::query("UPDATE records SET deleted_at = NULL, updated_at = datetime('now', 'localtime') WHERE id = ? AND user_id = ?")
        .bind(id)
        .bind(user_id)
        .execute(&pool)
        .await;

    match result {
        Ok(row) if row.rows_affected() > 0 => {
            (StatusCode::OK, Json(ApiResponse { status: "success".to_string(), message: "record restored".to_string() }))
        }
        Ok(_) => {
            (StatusCode::NOT_FOUND, Json(ApiResponse { status: "error".to_string(), message: "record not found".to_string() }))
        }
        Err(e) => {
            tracing::error!("failed to restore record: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(ApiResponse { status: "error".to_string(), message: "server error".to_string() }))
        }
    }
}

// 獲取使用者設定
pub async fn get_user_settings(
    State(pool): State<SqlitePool>,
    claims: Claims,
) -> (StatusCode, Json<UserSettingsResponse>) {
    let user_id = claims.sub;

    // 取得 username
    let username = sqlx::query_scalar::<_, String>("SELECT username FROM users WHERE id = ?")
        .bind(user_id)
        .fetch_one(&pool)
        .await
        .unwrap_or_else(|_| "Unknown".to_string());

    // 取得設定
    #[derive(sqlx::FromRow)]
    struct UserSettingsRow {
        display_name: Option<String>,
        language: Option<String>,
        theme: Option<String>,
    }

    let record = sqlx::query_as::<_, UserSettingsRow>(
        "SELECT display_name, language, theme FROM user_settings WHERE user_id = ?"
    )
    .bind(user_id)
    .fetch_optional(&pool)
    .await;

    match record {
        Ok(Some(row)) => (
            StatusCode::OK,
            Json(UserSettingsResponse {
                username,
                display_name: row.display_name,
                language: row.language,
                theme: row.theme,
            }),
        ),
        _ => (
            StatusCode::OK,
            Json(UserSettingsResponse {
                username,
                display_name: None,
                language: None,
                theme: None,
            }),
        ),
    }
}

// 更新使用者設定
pub async fn update_user_settings(
    State(pool): State<SqlitePool>,
    claims: Claims,
    Json(payload): Json<UserSettingsPayload>,
) -> (StatusCode, Json<ApiResponse>) {
    let user_id = claims.sub;

    let result = sqlx::query(
        r#"
        INSERT INTO user_settings (user_id, display_name, language, theme, updated_at)
        VALUES (?, ?, ?, ?, datetime('now', 'localtime'))
        ON CONFLICT(user_id) DO UPDATE SET
            display_name = excluded.display_name,
            language = excluded.language,
            theme = excluded.theme,
            updated_at = excluded.updated_at
        "#
    )
    .bind(user_id)
    .bind(&payload.display_name)
    .bind(&payload.language)
    .bind(&payload.theme)
    .execute(&pool)
    .await;

    match result {
        Ok(_) => (StatusCode::OK, Json(ApiResponse { status: "success".to_string(), message: "settings updated".to_string() })),
        Err(e) => {
            tracing::error!("failed to update user settings: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(ApiResponse { status: "error".to_string(), message: "server error".to_string() }))
        }
    }
}