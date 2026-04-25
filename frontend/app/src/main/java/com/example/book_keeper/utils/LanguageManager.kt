package com.example.book_keeper.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * 語言管理工具類。
 * 負責處理語系切換、本地化配置儲存以及 Locale 的套用。
 */
object LanguageManager {
    private const val PREFS_NAME = "settings"
    private const val KEY_LANGUAGE = "language"

    /**
     * 儲存所選語言並套用。
     * @param languageCode 語言代碼 (如 "en", "zh-TW", "zh-CN")
     */
    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        updateBaseContextLocale(context)
    }

    /**
     * 獲取目前儲存的語言代碼，預設為英文 "en"。
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    /**
     * 套用儲存的語系配置到 Context。
     * 通常在 Activity 的 attachBaseContext 或重啟時呼叫。
     */
    fun updateBaseContextLocale(context: Context): Context {
        val language = getLanguage(context)
        val locale = when (language) {
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.ENGLISH
        }
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}
