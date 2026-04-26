# ---- 階段一：編譯 ----
FROM rust:1.85-slim AS builder

WORKDIR /app

# 安裝 SQLite 編譯所需的系統依賴
RUN apt-get update && apt-get install -y pkg-config libssl-dev && rm -rf /var/lib/apt/lists/*

# 先複製 Cargo 檔案，利用 Docker 快取加速（依賴不變就不用重新編譯）
COPY Cargo.toml Cargo.lock* ./
RUN mkdir src && echo "fn main() {}" > src/main.rs
RUN cargo build --release && rm -rf src

# 複製真正的原始碼並編譯
COPY src ./src
RUN touch src/main.rs && cargo build --release

# ---- 階段二：最終映像（極小） ----
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 只複製編譯好的二進位檔案
COPY --from=builder /app/target/release/backend /app/backend

# 資料庫會存在 /app/data 目錄，方便掛載 volume
RUN mkdir -p /app/data

ENV RUST_LOG=info

EXPOSE 8080

CMD ["./backend"]
