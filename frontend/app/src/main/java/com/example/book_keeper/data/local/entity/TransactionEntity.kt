package com.example.book_keeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.book_keeper.network.RecordResponse

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val recordType: String,
    val date: String,
    val note: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val deletedAt: String?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

fun TransactionEntity.toRecordResponse() = RecordResponse(
    id = id,
    amount = amount,
    category = category,
    record_type = recordType,
    date = date,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RecordResponse.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED) = TransactionEntity(
    id = id,
    amount = amount,
    category = category,
    recordType = record_type,
    date = date,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus
)
