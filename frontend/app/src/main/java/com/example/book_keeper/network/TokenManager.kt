package com.example.book_keeper.data

import android.content.Context
import android.content.SharedPreferences

// 使用 object 宣告，代表這是一個 Singleton (單例模式)，全 App 共用同一個實體
object TokenManager {
    private const val PREFS_NAME = "book_keeper_prefs"
    private const val TOKEN_KEY = "jwt_token"

    // 取得 SharedPreferences 實體
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 儲存 Token (登入成功時呼叫)
    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(TOKEN_KEY, token).apply()
    }

    // 讀取 Token (攔截器要發送 API 時呼叫)
    fun getToken(context: Context): String? {
        return getPrefs(context).getString(TOKEN_KEY, null)
    }

    // 清除 Token (登出時呼叫)
    fun clearToken(context: Context) {
        getPrefs(context).edit().remove(TOKEN_KEY).apply()
    }
}