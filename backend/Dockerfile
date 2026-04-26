# ---- 階段一：編譯 ----
FROM rust:1.85 AS builder

WORKDIR /app

# 複製所有原始碼
COPY . .

# 編譯 release 版本
RUN cargo build --release

# ---- 階段二：最終映像（極小） ----
FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get install -y ca-certificates libsqlite3-0 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 只複製編譯好的二進位檔案
COPY --from=builder /app/target/release/backend /app/backend

# 資料庫會存在 /app/data 目錄，方便掛載 volume
RUN mkdir -p /app/data

ENV RUST_LOG=info

EXPOSE 8080

CMD ["./backend"]
