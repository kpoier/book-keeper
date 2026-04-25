資料收集與打包 (Android 端)：使用者在 App 輸入金額與備註。Kotlin 程式碼將這些欄位組裝成 RecordPayload 物件，並由 Gson 套件自動將其打包成標準的 JSON 字串。

發出請求 (網路傳輸)：Retrofit 套件發送一個 HTTP POST 請求，攜帶上述的 JSON 資料，透過網路發送到你電腦的指定 IP 與 Port (8080)。

接收與解析 (Rust 端)：Axum 伺服器監聽到請求，驗證路徑 (/api/records) 後，攔截 JSON 資料，並自動將其反序列化為 Rust 內部的 RecordPayload 結構體。

業務邏輯處理 (Rust 端)：(目前只是印出資料，未來這裡會加入將資料寫入 SQLite 資料庫的邏輯。)

回傳標準格式 (Rust 端)：處理完畢後，伺服器必須給一個「明確的回應」。這包含兩個部分：

HTTP 狀態碼：告訴前端這次請求是否成功（例如 201 Created 代表新增成功，400 Bad Request 代表資料有誤）。

回應 JSON 資料：提供詳細資訊給前端（例如 {"status": "success", "message": "..."}）。

解析回應並更新畫面 (Android 端)：App 收到回傳的資料，Retrofit 再將其轉換回 Kotlin 物件，最後你就可以根據狀態碼，在畫面上跳出「記帳成功」的提示。