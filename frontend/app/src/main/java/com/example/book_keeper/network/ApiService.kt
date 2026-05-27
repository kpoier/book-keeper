package com.example.book_keeper.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName

// --- 定義資料結構 ---
data class AuthPayload(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("token") val token: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("message") val message: String?
)

data class RecordPayload(
    @SerializedName("amount") val amount: Double,
    @SerializedName("category") val category: String,
    @SerializedName("type") val recordType: String,
    @SerializedName("date") val date: String,
    @SerializedName("note") val note: String?
)

data class ApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class RecordResponse(
    @SerializedName("id") val id: String, // 已變更為 String (UUID)
    @SerializedName("amount") val amount: Double,
    @SerializedName("category") val category: String,
    @SerializedName("record_type") val record_type: String,
    @SerializedName("date") val date: String,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("deleted_at") val deletedAt: String?
)

data class SummaryResponse(
    @SerializedName("total_expense") val total_expense: Double,
    @SerializedName("total_income") val total_income: Double
)

data class UserResponse(
    @SerializedName("username") val username: String
)

data class UserSettingsPayload(
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("theme") val theme: String?
)

data class UserSettingsResponse(
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("theme") val theme: String?
)

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
        @Path("id") id: String, // 已變更為 String
        @Body payload: RecordPayload
    ): Response<ApiResponse>

    @DELETE("/api/records/{id}")
    suspend fun deleteRecord(
        @Path("id") id: String // 已變更為 String
    ): Response<ApiResponse>

    @POST("/api/records/{id}/restore")
    suspend fun restoreRecord(
        @Path("id") id: String // 新增復原 API
    ): Response<ApiResponse>

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

    @GET("/api/records/export")
    suspend fun exportRecords(
        @Query("month") month: String? = null // 更新以支援選定時段匯出
    ): Response<ResponseBody>

    @GET("/api/settings")
    suspend fun getUserSettings(): Response<UserSettingsResponse>

    @PUT("/api/settings")
    suspend fun updateUserSettings(@Body payload: UserSettingsPayload): Response<ApiResponse>
}