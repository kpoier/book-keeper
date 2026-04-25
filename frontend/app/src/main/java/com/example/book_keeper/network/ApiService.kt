package com.example.book_keeper.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

// --- 定義資料結構 ---
data class AuthPayload(val username: String, val password: String)
data class AuthResponse(val status: String, val token: String?, val username: String?, val message: String?)

data class RecordPayload(
    val amount: Double,
    val category: String,
    @com.google.gson.annotations.SerializedName("type") val recordType: String,
    val date: String,
    val note: String?
)
data class ApiResponse(val status: String, val message: String)

data class RecordResponse(
    val id: Int,
    val amount: Double,
    val category: String,
    val record_type: String, // 這裡要對應 Rust 回傳的 JSON key
    val date: String,
    val note: String?
)

data class SummaryResponse(val total_expense: Double, val total_income: Double)

data class UserResponse(val username: String)

// --- 定義 API 端點 ---
interface ApiService {
    @POST("/api/register")
    suspend fun register(@Body payload: AuthPayload): Response<ApiResponse>

    @POST("/api/login")
    suspend fun login(@Body payload: AuthPayload): Response<AuthResponse>

    @POST("/api/records")
    suspend fun createRecord(@Body payload: RecordPayload): Response<ApiResponse>

    @PUT("/api/records/{id}")
    suspend fun updateRecord(
        @Path("id") id: Int,
        @Body payload: RecordPayload
    ): retrofit2.Response<ApiResponse>

    @DELETE("/api/records/{id}")
    suspend fun deleteRecord(
        @Path("id") id: Int
    ): retrofit2.Response<ApiResponse>

    @GET("/api/records")
    suspend fun getRecords(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("month") month: String? = null
    ): Response<List<RecordResponse>>

    @GET("/api/records/summary")
    suspend fun getSummary(@Query("month") month: String? = null): Response<SummaryResponse>

    @GET("/api/me")
    suspend fun getMe(): Response<UserResponse>
}