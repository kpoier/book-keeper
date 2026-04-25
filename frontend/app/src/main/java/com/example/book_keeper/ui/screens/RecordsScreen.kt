package com.example.book_keeper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.book_keeper.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }

    var records by remember { mutableStateOf<List<RecordResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editingRecord by remember { mutableStateOf<RecordResponse?>(null) }

    val loadRecords = {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getRecords()
                if (response.isSuccessful) records = response.body() ?: emptyList()
            } catch (e: Exception) {
                println("載入紀錄失敗: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadRecords() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("歷史紀錄", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("目前沒有紀錄", color = Color.Gray) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = records, key = { it.id }) { record ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                coroutineScope.launch {
                                    if (apiService.deleteRecord(record.id).isSuccessful) loadRecords()
                                }
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(20.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, "刪除")
                            }
                        },
                        content = { RecordItem(record, onClick = { editingRecord = record }) }
                    )
                }
            }
        }
    }

    if (editingRecord != null) {
        EditRecordDialog(
            record = editingRecord!!,
            onDismiss = { editingRecord = null },
            onConfirm = { id, payload ->
                coroutineScope.launch {
                    if (apiService.updateRecord(id, payload).isSuccessful) {
                        editingRecord = null
                        loadRecords()
                    }
                }
            }
        )
    }
}

@Composable
fun RecordItem(record: RecordResponse, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(record.category, fontWeight = FontWeight.Bold)
                Text(record.date.take(10), fontSize = 12.sp, color = Color.Gray)
                if (!record.note.isNullOrBlank()) Text(record.note, fontSize = 14.sp)
            }
            Text("$ ${record.amount}", fontWeight = FontWeight.Bold, color = if (record.record_type == "expense") Color.Red else Color.Green)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordDialog(record: RecordResponse, onDismiss: () -> Unit, onConfirm: (Int, RecordPayload) -> Unit) {
    var amount by remember { mutableStateOf(record.amount.toString()) }
    var note by remember { mutableStateOf(record.note ?: "") }
    var selectedDateMillis by remember { mutableStateOf(DateUtils.isoToMillis(record.date)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val categories = listOf("餐飲", "交通", "居住", "娛樂", "購物", "電子設備", "其他")
    var selectedCategory by remember { mutableStateOf(if (categories.contains(record.category)) record.category else categories[0]) }
    var expanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = { selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis; showDatePicker = false }) { Text("確定") }
        }) { DatePicker(datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯紀錄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金額") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("分類") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        categories.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedCategory = it; expanded = false }) }
                    }
                }
                OutlinedTextField(value = DateUtils.millisToDisplayDate(selectedDateMillis), onValueChange = {}, readOnly = true, label = { Text("日期") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, enabled = false)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("備註") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@Button
                onConfirm(record.id, RecordPayload(amt, selectedCategory, record.record_type, DateUtils.millisToIsoString(selectedDateMillis), note.ifBlank { null }))
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}