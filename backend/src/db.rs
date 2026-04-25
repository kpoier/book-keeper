use sqlx::{sqlite::SqlitePoolOptions, SqlitePool};

pub async fn init_db() -> SqlitePool {
    // 設定 SQLite 連線。mode=rwc 代表如果檔案不存在，就自動建立一個
    let db_url = "sqlite://records.db?mode=rwc";
    let pool = SqlitePoolOptions::new()
        .max_connections(5)
        .connect(db_url)
        .await
        .expect("cannot connect to db");

    // 使用者
    sqlx::query(
        "CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL
        )"
    )
    .execute(&pool)
    .await
    .expect("cannot create users table");

    // 記帳記錄表
    sqlx::query(
        "CREATE TABLE IF NOT EXISTS records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL, 
            amount REAL NOT NULL,
            category TEXT NOT NULL,
            record_type TEXT NOT NULL,
            date TEXT NOT NULL,
            note TEXT,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )"
    )
    .execute(&pool)
    .await
    .expect("cannot create records table");

    println!("db connection and initialized successfully!");

    pool
}
