mod db;
mod handlers;
mod models;


use axum::{
    routing::{get, post, delete, put},
    Router,
};
use tokio::net::TcpListener;

#[tokio::main]
async fn main() {
    // 0. 初始化日誌系統
    tracing_subscriber::fmt::init();

    // 1. 初始化資料庫連線
    let pool = db::init_db().await;

    // 2. 建立路由，使用 handlers 模組裡面的函數，並把連線池分享給它們
    let app = Router::new()
        .route("/api/records", post(handlers::create_record))
        .route("/api/records", get(handlers::get_records))
        .route("/api/records/summary", get(handlers::get_summary))
        .route("/api/register", post(handlers::register))
        .route("/api/login", post(handlers::login))
        .route("/api/records/{id}", delete(handlers::delete_record))
        .route("/api/records/{id}", put(handlers::update_record))
        .route("/api/me", get(handlers::get_me))
        .with_state(pool);

    // 3. 設定伺服器監聽 IP 與 Port
    let listener = TcpListener::bind("0.0.0.0:8080").await.unwrap();
    tracing::info!("server is running, listening on {}", listener.local_addr().unwrap());

    // 4. 啟動伺服器
    axum::serve(listener, app).await.unwrap();
}