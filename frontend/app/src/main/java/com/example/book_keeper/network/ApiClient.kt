package com.example.book_keeper.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 使用你規格書中測試成功的本機 IP
    private const val BASE_URL = "http://192.168.100.7:8080"

    // 我們需要傳入 Context，因為讀取 Token 需要它
    fun create(context: Context): ApiService {

        // 1. 建立攔截器
        val authInterceptor = Interceptor { chain ->
            // 拿到原始的請求
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            // 從 TokenManager 拿出 Token
            val token = TokenManager.getToken(context)

            // 如果 Token 存在，就塞進 Header
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            // 放行並執行修改後的請求
            chain.proceed(requestBuilder.build())
        }

        // 2. 把攔截器裝進 OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        // 3. 建立並回傳 Retrofit 實體
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 使用我們特製的 client
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}