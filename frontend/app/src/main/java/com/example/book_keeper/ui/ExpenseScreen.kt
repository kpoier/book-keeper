package com.example.book_keeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.RecordPayload
import com.example.book_keeper.network.RecordResponse
import com.example.book_keeper.network.TokenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// 取得當下時間並格式化為 ISO 8601 標準字串 (供 Rust 後端使用)
fun getCurrentIsoTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC") // 統一轉換為 UTC 時間
    return sdf.format(Date())
}

@OptIn(ExperimentalMaterial3Api::class) // 使用下拉選單與 SwipeToDismissBox 需要這個標籤
@Composable
fun ExpenseScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }

    // --- 表單狀態 ---
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // 下拉選單的狀態
    val categories = listOf("餐飲", "交通", "居住", "娛樂", "購物", "電子設備", "其他")
    var expanded by remember { mutableStateOf(false) } // 控制選單展開/收起
    var selectedCategory by remember { mutableStateOf(categories[0]) } // 預設選中第一項

    // --- 來自後端的資料狀態 ---
    var records by remember { mutableStateOf<List<RecordResponse>>(emptyList()) }
    var totalExpense by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // 建立一個可以重複呼叫的載入函式
    suspend fun loadData() {
        try {
            val recordsRes = apiService.getRecords()
            if (recordsRes.isSuccessful) {
                records = recordsRes.body() ?: emptyList()
            }

            val summaryRes = apiService.getSummary()
            if (summaryRes.isSuccessful) {
                totalExpense = summaryRes.body()?.total_expense ?: 0.0
            }
        } catch (e: Exception) {
            println("讀取資料發生錯誤：${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 進入畫面時自動載入資料
    LaunchedEffect(Unit) {
        loadData()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // 頂部：總結與登出
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("總支出", fontSize = 16.sp, color = Color.Gray)
                Text("$ $totalExpense", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onLogout) {
                Text("登出", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 中間：新增記帳表單
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("新增紀錄", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // 第一排：金額與分類選單
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 金額輸入
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("金額") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // 分類下拉選單
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分類") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第二排：備註
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註 (選填)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 送出按鈕
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val amountValue = amount.toDoubleOrNull() ?: return@launch

                            val payload = RecordPayload(
                                amount = amountValue,
                                category = selectedCategory,
                                recordType = "expense",
                                date = getCurrentIsoTime(),
                                note = note.ifBlank { null }
                            )
                            val response = apiService.createRecord(payload)

                            if (response.isSuccessful) {
                                amount = ""
                                note = ""
                                loadData() // 重新整理列表
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("送出")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 底部：歷史紀錄列表
        Text("歷史紀錄", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (records.isEmpty()) {
            Text("目前還沒有任何紀錄", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = records, key = { it.id }) { record ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                coroutineScope.launch {
                                    try {
                                        val response = apiService.deleteRecord(record.id)
                                        if (response.isSuccessful) {
                                            loadData()
                                        }
                                    } catch (e: Exception) {
                                        println("刪除失敗: ${e.message}")
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "刪除",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        content = {
                            RecordItem(record)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordItem(record: RecordResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.category, fontWeight = FontWeight.Bold)
                Text(record.date.take(10), fontSize = 12.sp, color = Color.Gray)
                if (!record.note.isNullOrBlank()) {
                    Text(record.note, fontSize = 14.sp)
                }
            }
            Text(
                text = "$ ${record.amount}",
                fontWeight = FontWeight.Bold,
                color = if (record.record_type == "expense") MaterialTheme.colorScheme.error else Color.Green
            )
        }
    }
}