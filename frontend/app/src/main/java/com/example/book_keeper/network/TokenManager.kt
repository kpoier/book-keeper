package com.example.book_keeper.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

// 使用 object 宣告，代表這是一個 Singleton (單例模式)，全 App 共用同一個實體
object TokenManager {
    private const val PREFS_NAME = "book_keeper_secure_prefs"
    private const val TOKEN_KEY = "jwt_token"
    private const val DB_KEY_KEY = "db_encryption_key"

    // 取得 EncryptedSharedPreferences 實體
    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
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

    // 取得或產生 SQLCipher 資料庫的隨機加密金鑰
    fun getDatabaseKey(context: Context): ByteArray {
        val prefs = getPrefs(context)
        val existingKeyBase64 = prefs.getString(DB_KEY_KEY, null)
        if (existingKeyBase64 != null) {
            return Base64.decode(existingKeyBase64, Base64.DEFAULT)
        }
        
        // 產生新的 256-bit (32 bytes) 金鑰
        val newKey = ByteArray(32)
        SecureRandom().nextBytes(newKey)
        prefs.edit().putString(DB_KEY_KEY, Base64.encodeToString(newKey, Base64.DEFAULT)).apply()
        return newKey
    }

    // 清除資料庫金鑰 (登出時呼叫)
    fun clearDatabaseKey(context: Context) {
        getPrefs(context).edit().remove(DB_KEY_KEY).apply()
    }
}