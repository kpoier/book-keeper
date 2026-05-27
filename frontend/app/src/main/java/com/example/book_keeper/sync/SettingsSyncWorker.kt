package com.example.book_keeper.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.UserSettingsPayload
import com.example.book_keeper.utils.LanguageManager
import com.example.book_keeper.utils.ThemeManager
import com.example.book_keeper.utils.UserManager

class SettingsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val apiService = ApiClient.create(applicationContext)

            val displayName = UserManager.getDisplayName(applicationContext)
            val language = LanguageManager.getLanguage(applicationContext)
            val theme = ThemeManager.getThemeMode(applicationContext)

            val payload = UserSettingsPayload(
                displayName = displayName,
                language = language,
                theme = theme
            )

            val response = apiService.updateUserSettings(payload)
            
            if (response.isSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
