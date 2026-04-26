# Book Keeper

**English** | [中文](README.md)

A lightweight personal bookkeeping application built with a Rust backend and Android frontend.

## Features

- 🔐 **User Authentication** — Register & login with Argon2 password hashing
- 🎫 **Stateless JWT Authorization** — Token-based authentication, no repeated database lookups
- 📝 **Record CRUD** — Create, read, update, and delete financial records
- 📊 **Financial Summary** — Monthly income and expense statistics
- 🐳 **Docker Deployment** — Automated CI/CD with GitHub Actions pushing to DockerHub

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | [Axum](https://github.com/tokio-rs/axum) (Rust) |
| **Database** | SQLite + [SQLx](https://github.com/launchbadge/sqlx) |
| **Authentication** | Argon2 password hashing + JWT |
| **Frontend** | Android (Kotlin + Jetpack Compose) |
| **CI/CD** | GitHub Actions → DockerHub |
| **Logging** | tracing + tracing-subscriber |

## Project Structure

```
book-keeper/
├── backend/                 # Rust backend
│   ├── src/
│   │   ├── main.rs          # Entry point & routing
│   │   ├── handlers.rs      # API handler logic
│   │   ├── models.rs        # Data structure definitions
│   │   └── db.rs            # Database initialization
│   ├── Dockerfile
│   └── Cargo.toml
├── frontend/                # Android frontend
│   └── app/
├── .github/workflows/       # CI/CD automation
│   └── build-backend.yml
└── README.md
```

## API Endpoints

| Method | Path | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/register` | Register a new account | ❌ |
| `POST` | `/api/login` | Login and receive JWT | ❌ |
| `GET` | `/api/me` | Get current user info | ✅ |
| `POST` | `/api/records` | Create a new record | ✅ |
| `GET` | `/api/records` | List records | ✅ |
| `PUT` | `/api/records/{id}` | Update a record | ✅ |
| `DELETE` | `/api/records/{id}` | Delete a record | ✅ |
| `GET` | `/api/records/summary` | Get income/expense summary | ✅ |

## Getting Started

### Local Development

```bash
# Navigate to the backend directory
cd backend

# Start the server (SQLite database will be created automatically)
cargo run
```

The server will be running at `http://localhost:8080`.

### Docker Deployment

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

## License

This project is licensed under the [MIT License](LICENSE).
