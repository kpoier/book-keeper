use serde::{Deserialize, Serialize};

// frontend data structure
#[derive(Debug, Serialize, Deserialize)]
pub struct RecordPayload {
    pub amount: f64,
    pub category: String,
    #[serde(rename = "type")]
    pub record_type: String,
    pub date: String,
    pub note: Option<String>,
}

// response payload structure
#[derive(Serialize)]
pub struct ApiResponse {
    pub status: String,
    pub message: String,
}

// 用於「從資料庫讀取」並「傳回」前端的資料 (包含 ID)
#[derive(Debug, Serialize, Deserialize, sqlx::FromRow)]
pub struct Record {
    pub id: i32,
    pub amount: f64,
    pub category: String,
    pub record_type: String,
    pub date: String,
    pub note: Option<String>,
}

// 定義網址列的查詢參數
#[derive(Deserialize)]
pub struct RecordQuery {
    pub limit: Option<u32>,
    pub offset: Option<u32>,
    pub month: Option<String>,
}

// 定義統計結果的回傳結構
#[derive(Serialize, sqlx::FromRow)]
pub struct SummaryResponse {
    pub total_expense: f64,
    pub total_income: f64,
}

// 賬號
#[derive(Deserialize)]
pub struct AuthPayload {
    pub username: String,
    pub password: String,
}

// 登入成功的回傳
#[derive(Serialize)]
pub struct LoginResponse {
    pub status: String,
    pub token: String, // JWT
}

// 登入驗證 Claim
#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: i32, // subject, 也就是 user_id
    pub exp: i64, // 預計結束時間 (Unix time)
}

// 資料庫查詢回傳的使用者結構
#[derive(sqlx::FromRow)]
pub struct UserRow {
    pub id: i32,
    pub password_hash: String,
}