package com.example.book_keeper.data.repository

import com.example.book_keeper.data.local.dao.TransactionDao
import com.example.book_keeper.data.local.entity.SyncStatus
import com.example.book_keeper.data.local.entity.TransactionEntity
import com.example.book_keeper.data.local.entity.toEntity
import com.example.book_keeper.data.local.entity.toRecordResponse
import com.example.book_keeper.network.ApiService
import com.example.book_keeper.network.RecordPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TransactionRepository(
    private val apiService: ApiService,
    private val transactionDao: TransactionDao
) {

    // 取得所有本地尚未標記為刪除的紀錄
    fun getAllActiveRecords(): Flow<List<com.example.book_keeper.network.RecordResponse>> {
        return transactionDao.getAllActiveTransactions().map { entities ->
            entities.map { it.toRecordResponse() }
        }
    }

    // 新增紀錄（離線優先）
    suspend fun createRecord(payload: RecordPayload) {
        val newId = UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        val newEntity = TransactionEntity(
            id = newId,
            amount = payload.amount,
            category = payload.category,
            recordType = payload.recordType,
            date = payload.date,
            note = payload.note,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        transactionDao.insertTransaction(newEntity)
        // Note: 實際同步交由 WorkManager 處理
    }

    // 更新紀錄（離線優先）
    suspend fun updateRecord(id: String, payload: RecordPayload) {
        val existing = transactionDao.getTransactionById(id) ?: return
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // 如果這個紀錄原本還沒上傳，我們更新它依然是 PENDING_CREATE。
        // 如果原本已經 SYNCED，修改後變成 PENDING_UPDATE。
        val newSyncStatus = if (existing.syncStatus == SyncStatus.PENDING_CREATE) {
            SyncStatus.PENDING_CREATE
        } else {
            SyncStatus.PENDING_UPDATE
        }

        val updatedEntity = existing.copy(
            amount = payload.amount,
            category = payload.category,
            recordType = payload.recordType,
            date = payload.date,
            note = payload.note,
            updatedAt = now,
            syncStatus = newSyncStatus
        )
        transactionDao.updateTransaction(updatedEntity)
    }

    // 刪除紀錄（離線優先，軟刪除）
    suspend fun deleteRecord(id: String) {
        val existing = transactionDao.getTransactionById(id) ?: return
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        if (existing.syncStatus == SyncStatus.PENDING_CREATE) {
            // 如果這筆紀錄根本還沒上傳，我們可以直接在本地刪除，不用通知伺服器
            // 為了保持簡單，這裡也可以設為 PENDING_DELETE，但 WorkManager 要能處理
            // 建議如果還沒上傳就直接標記為 PENDING_DELETE，後端如果找不到會忽略
        }

        val deletedEntity = existing.copy(
            deletedAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_DELETE
        )
        transactionDao.updateTransaction(deletedEntity)
    }

    // 復原紀錄（離線優先）
    suspend fun restoreRecord(id: String) {
        val existing = transactionDao.getTransactionById(id) ?: return
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val restoredEntity = existing.copy(
            deletedAt = null,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPDATE // 復原視為一種更新
        )
        transactionDao.updateTransaction(restoredEntity)
    }

    // 執行遠端同步（由 WorkManager 呼叫）
    suspend fun syncWithRemote() {
        // 1. Push: 把本地待同步的資料上傳
        pushLocalChanges()

        // 2. Pull: 從遠端拉取最新資料 (覆蓋本地)
        pullRemoteChanges()
    }

    private suspend fun pushLocalChanges() {
        val pendingCreates = transactionDao.getUnsyncedTransactions(SyncStatus.SYNCED)
        
        for (entity in pendingCreates) {
            try {
                when (entity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        val payload = RecordPayload(
                            amount = entity.amount,
                            category = entity.category,
                            recordType = entity.recordType,
                            date = entity.date,
                            note = entity.note
                        )
                        // Note: 後端 API 似乎沒有傳 ID，所以這裡如果是新增，ID 會是由後端重新產生
                        // 修正：如果後端支援 UUID，我們應該修改 API 讓前端可以帶 ID 上去
                        // 若後端不支援傳入 ID，則本地的 UUID 會跟後端不一致。
                        // 這邊假設後端 API 已被修改為接受 UUID（或在 update 的時候以 UUID 為準）。
                        
                        // 暫時處理：直接上傳，等待 Pull 覆蓋。
                        val res = apiService.createRecord(payload)
                        if (res.isSuccessful) {
                            transactionDao.updateTransaction(entity.copy(syncStatus = SyncStatus.SYNCED))
                        }
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        val payload = RecordPayload(
                            amount = entity.amount,
                            category = entity.category,
                            recordType = entity.recordType,
                            date = entity.date,
                            note = entity.note
                        )
                        val res = apiService.updateRecord(entity.id, payload)
                        if (res.isSuccessful) {
                            transactionDao.updateTransaction(entity.copy(syncStatus = SyncStatus.SYNCED))
                        }
                    }
                    SyncStatus.PENDING_DELETE -> {
                        val res = apiService.deleteRecord(entity.id)
                        if (res.isSuccessful) {
                            transactionDao.updateTransaction(entity.copy(syncStatus = SyncStatus.SYNCED))
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // Network error, ignore and try again next time
                e.printStackTrace()
            }
        }
    }

    private suspend fun pullRemoteChanges() {
        try {
            // 一次性下載所有歷史（由於之前的決定）
            // 在實務上，後端 getRecords 預設為 limit=50，要抓全部可能需要設定極大值，或是支援無分頁
            val response = apiService.getRecords(limit = 10000, offset = 0)
            if (response.isSuccessful) {
                response.body()?.let { remoteRecords ->
                    val newEntities = remoteRecords.map { it.toEntity(SyncStatus.SYNCED) }
                    // 覆蓋本地資料
                    transactionDao.clearAll()
                    transactionDao.insertTransactions(newEntities)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
