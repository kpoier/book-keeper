package com.example.book_keeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.book_keeper.data.local.dao.TransactionDao
import com.example.book_keeper.data.local.entity.TransactionEntity
import net.sqlcipher.database.SupportFactory
import com.example.book_keeper.network.TokenManager

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "book_keeper_database"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = TokenManager.getDatabaseKey(context)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .openHelperFactory(factory)
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Method to completely delete the database
        fun clearDatabase(context: Context) {
            INSTANCE?.close()
            INSTANCE = null
            context.getDatabasePath(DATABASE_NAME).delete()
        }
    }
}
