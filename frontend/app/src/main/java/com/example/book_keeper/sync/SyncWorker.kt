package com.example.book_keeper.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.book_keeper.data.local.AppDatabase
import com.example.book_keeper.data.repository.TransactionRepository
import com.example.book_keeper.network.ApiClient

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val apiService = ApiClient.create(applicationContext)
            val repository = TransactionRepository(apiService, database.transactionDao())

            repository.syncWithRemote()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
