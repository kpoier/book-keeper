package com.example.book_keeper.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object UserManager {
    private const val PREFS_NAME = "user_secure_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_DISPLAY_NAME = "display_name"

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

    fun saveUsername(context: Context, username: String) {
        getPrefs(context).edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }

    fun saveDisplayName(context: Context, displayName: String?) {
        val editor = getPrefs(context).edit()
        if (displayName.isNullOrBlank()) {
            editor.remove(KEY_DISPLAY_NAME)
        } else {
            editor.putString(KEY_DISPLAY_NAME, displayName)
        }
        editor.apply()
    }

    fun getDisplayName(context: Context): String? {
        return getPrefs(context).getString(KEY_DISPLAY_NAME, null)
    }

    fun clearUser(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
