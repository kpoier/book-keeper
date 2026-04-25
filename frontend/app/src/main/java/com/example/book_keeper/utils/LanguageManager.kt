package com.example.book_keeper.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
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
        // 注意：這裡只更新存檔，實際套用通常透過重啟 Activity
    }

    /**
     * 獲取目前儲存的語言代碼。
     * 如果沒有儲存，則預設偵測系統語言。
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return saved

        // 偵測系統語言
        val locale = Locale.getDefault()
        return when (locale.language) {
            "zh" -> {
                if (locale.country == "TW" || locale.country == "HK" || locale.toLanguageTag().contains("Hant")) "zh-TW"
                else "zh-CN"
            }
            else -> "en"
        }
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

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        }

        return context.createConfigurationContext(configuration)
    }
}
