package com.example.book_keeper.utils

import android.content.Context

/**
 * 主題管理工具類。
 */
object ThemeManager {
    private const val PREFS_NAME = "settings"
    private const val KEY_THEME = "theme_mode"

    /**
     * 儲存所選主題模式 ("light", "dark", "system")。
     */
    fun setThemeMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    /**
     * 獲取目前儲存的主題模式，預設為 "system"。
     */
    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }
}
