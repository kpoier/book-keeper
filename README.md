# Book Keeper

[English](README_EN.md) | **中文**

一個輕量級的個人記帳應用程式，採用 Rust 後端搭配 Android 前端的架構設計。

## 功能特色

- 🔐 **使用者認證** — 註冊、登入，密碼使用 Argon2 雜湊保護
- 🎫 **JWT 無狀態授權** — 登入後簽發 Token，無需重複查詢資料庫驗證身分
- 📝 **記帳 CRUD** — 支援 UUID 主鍵、防誤刪的軟刪除 (Soft Delete) 與復原機制
- 📊 **收支統計** — 依月份統計總收入與總支出
- 📄 **資料導出** — 一鍵匯出為 CSV 格式 (支援指定月份)
- 🔄 **離線優先 (Offline-First)** — 支援斷網操作。搭載 SQLCipher 加密本地 SQLite，透過 Android WorkManager 在背景自動與後端雙向同步 (Sync) 帳務與個人設定。
- 🛡️ **軍規級安全保護** — 使用者名稱與暱稱等機密設定使用 `EncryptedSharedPreferences` (AES-256) 進行加密儲存。
- 🐳 **Docker 部署** — 支援 GitHub Actions 自動構建並推送映像至 DockerHub

## 技術架構

| 層級 | 技術 |
|---|---|
| **後端框架** | [Axum](https://github.com/tokio-rs/axum) (Rust) |
| **資料庫** | SQLite + [SQLx](https://github.com/launchbadge/sqlx) |
| **認證機制** | Argon2 密碼雜湊 + JWT |
| **前端** | Android (Kotlin + Jetpack Compose) |
| **CI/CD** | GitHub Actions → DockerHub |
| **日誌系統** | tracing + tracing-subscriber |

## 專案結構

```
book-keeper/
├── backend/                 # Rust 後端
│   ├── src/
│   │   ├── main.rs          # 進入點、路由設定
│   │   ├── handlers.rs      # API 處理邏輯
│   │   ├── models.rs        # 資料結構定義
│   │   └── db.rs            # 資料庫初始化
│   ├── Dockerfile
│   └── Cargo.toml
├── frontend/                # Android 前端
│   └── app/
├── .github/workflows/       # CI/CD 自動化
│   └── build-backend.yml
└── README.md
```

## API 端點

| 方法 | 路徑 | 說明 | 需要 Token |
|---|---|---|---|
| `POST` | `/api/register` | 註冊帳號 | ❌ |
| `POST` | `/api/login` | 登入取得 Token | ❌ |
| `GET` | `/api/me` | 取得當前使用者資訊 | ✅ |
| `POST` | `/api/records` | 新增記帳紀錄 (支援 UUID) | ✅ |
| `GET` | `/api/records` | 查詢記帳紀錄 (可使用 `?month` 過濾) | ✅ |
| `PUT` | `/api/records/{id}` | 更新記帳紀錄 | ✅ |
| `DELETE` | `/api/records/{id}` | 軟刪除記帳紀錄 | ✅ |
| `POST` | `/api/records/{id}/restore`| 復原已刪除的記帳紀錄 | ✅ |
| `GET` | `/api/records/summary` | 取得收支統計 (可使用 `?month` 過濾) | ✅ |
| `GET` | `/api/records/export` | 導出記帳紀錄為 CSV 檔案 (可使用 `?month` 過濾) | ✅ |
| `GET` | `/api/settings` | 取得使用者個人設定 (包含暱稱、語言、主題) | ✅ |
| `PUT` | `/api/settings` | 更新使用者個人設定 | ✅ |

## 快速開始

### 本地開發

```bash
# 進入後端目錄
cd backend

# 啟動伺服器（會自動建立 SQLite 資料庫）
cargo run
```

伺服器將在 `http://localhost:8080` 上運行。

### Docker 部署

```yaml
# docker-compose.yml
services:
  book-keeper:
    image: kpoier/book-keeper-backend:latest
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
    environment:
      - DATABASE_URL=sqlite:///app/data/records.db?mode=rwc
      - RUST_LOG=info
```

```bash
docker compose up -d
```

## 授權條款

本專案採用 [MIT License](LICENSE) 授權。
