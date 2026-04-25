package com.example.book_keeper.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.book_keeper.R
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.RecordPayload
import com.example.book_keeper.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 主頁面 Composable。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onRecordAdded: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }

    // 定義類別代碼與資源 ID 的映射
    val categoryOptions = remember {
        listOf(
            "food" to R.string.cat_food,
            "transport" to R.string.cat_transport,
            "housing" to R.string.cat_housing,
            "entertainment" to R.string.cat_entertainment,
            "shopping" to R.string.cat_shopping,
            "electronics" to R.string.cat_electronics,
            "other" to R.string.cat_other
        )
    }

    // 當前月份數據
    var totalExpense by remember { mutableStateOf(0.0) }
    var categoryBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    // 上個月份數據
    var lastMonthTotalExpense by remember { mutableStateOf(0.0) }
    var lastMonthCategoryBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    // 這裡儲存的是 Key (如 "food")
    var selectedCategoryKey by remember { mutableStateOf(categoryOptions[0].first) }

    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val currentMonth = remember { sdf.format(Date()) }
    val lastMonth = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        sdf.format(cal.time)
    }

    val loadData = {
        coroutineScope.launch {
            try {
                // 獲取本月數據
                val summaryResponse = apiService.getSummary(month = currentMonth)
                if (summaryResponse.isSuccessful) {
                    totalExpense = summaryResponse.body()?.total_expense ?: 0.0
                }
                val recordsResponse = apiService.getRecords(month = currentMonth, limit = 100)
                if (recordsResponse.isSuccessful) {
                    val records = recordsResponse.body() ?: emptyList()
                    categoryBreakdown = records
                        .filter { it.record_type == "expense" }
                        .groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                }

                // 獲獲取上月數據
                val lastSummaryResponse = apiService.getSummary(month = lastMonth)
                if (lastSummaryResponse.isSuccessful) {
                    lastMonthTotalExpense = lastSummaryResponse.body()?.total_expense ?: 0.0
                }
                val lastRecordsResponse = apiService.getRecords(month = lastMonth, limit = 100)
                if (lastRecordsResponse.isSuccessful) {
                    val records = lastRecordsResponse.body() ?: emptyList()
                    lastMonthCategoryBreakdown = records
                        .filter { it.record_type == "expense" }
                        .groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) { loadData() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.home_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.home_total_expense), fontSize = 16.sp, color = Color.Gray)
        Text("$ $totalExpense", fontSize = 36.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        // 新增紀錄卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.home_new_record), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text(stringResource(R.string.home_amount)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            // 顯示翻譯後的文字
                            value = stringResource(categoryOptions.find { it.first == selectedCategoryKey }?.second ?: R.string.cat_other),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.home_category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categoryOptions.forEach { (key, resId) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(resId)) },
                                    onClick = { selectedCategoryKey = key; expanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = DateUtils.millisToDisplayDate(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.home_date)) },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.home_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val amountValue = amount.toDoubleOrNull() ?: return@launch
                            val payload = RecordPayload(
                                amount = amountValue,
                                category = selectedCategoryKey, // 發送給後端統一的 Key (如 "food")
                                recordType = "expense",
                                date = DateUtils.millisToIsoString(selectedDateMillis),
                                note = note.ifBlank { null }
                            )
                            if (apiService.createRecord(payload).isSuccessful) {
                                amount = ""; note = ""; onRecordAdded()
                                loadData()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.home_submit)) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 本月支出結構
        Text("${stringResource(R.string.home_expense_structure)} ($currentMonth)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (categoryBreakdown.isNotEmpty()) {
            ExpenseChart(categoryBreakdown, totalExpense, categoryOptions.toMap())
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_no_data), color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 上月支出結構
        Text("${stringResource(R.string.home_expense_structure)} ($lastMonth)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (lastMonthCategoryBreakdown.isNotEmpty()) {
            ExpenseChart(lastMonthCategoryBreakdown, lastMonthTotalExpense, categoryOptions.toMap())
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 支出統計分析
            ComparisonSummary(
                currentTotal = totalExpense,
                lastTotal = lastMonthTotalExpense,
                currentBreakdown = categoryBreakdown,
                lastBreakdown = lastMonthCategoryBreakdown,
                categoryNameMap = categoryOptions.toMap()
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_no_data), color = Color.Gray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ComparisonSummary(
    currentTotal: Double,
    lastTotal: Double,
    currentBreakdown: Map<String, Double>,
    lastBreakdown: Map<String, Double>,
    categoryNameMap: Map<String, Int>
) {
    val totalDiff = currentTotal - lastTotal
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_comparison_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // 總額對比
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_compared_to_last), fontSize = 14.sp)
                Text(
                    text = if (totalDiff >= 0) "+$${totalDiff.toInt()}" else "-$${(-totalDiff).toInt()}",
                    color = if (totalDiff > 0) Color.Red else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.home_analysis_title), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            val allCategories = (currentBreakdown.keys + lastBreakdown.keys).toSet()
            
            allCategories.forEach { categoryKey ->
                val currentVal = currentBreakdown[categoryKey] ?: 0.0
                val lastVal = lastBreakdown[categoryKey] ?: 0.0
                val diff = currentVal - lastVal
                
                // 根據 Key 獲取在地化名稱，若找不到則顯示原始 Key
                val localizedName = categoryNameMap[categoryKey]?.let { stringResource(it) } ?: categoryKey

                if (lastVal > 0 || currentVal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(localizedName, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (diff >= 0) "+$${diff.toInt()}" else "-$${(-diff).toInt()}",
                                color = if (diff > 0) Color.Red else Color(0xFF2E7D32),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (lastVal > 0) {
                                val percent = (diff / lastVal) * 100
                                Text(
                                    text = if (percent >= 0) "(+${percent.toInt()}%)" else "(${percent.toInt()}%)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseChart(breakdown: Map<String, Double>, total: Double, categoryNameMap: Map<String, Int>) {
    val chartColors = listOf(
        Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFA726),
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF26C6DA), Color(0xFFD4E157)
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    var startAngle = -90f
                    breakdown.entries.forEachIndexed { index, entry ->
                        val sweepAngle = if (total > 0) (entry.value / total).toFloat() * 360f else 0f
                        drawArc(
                            color = chartColors[index % chartColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40f)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.home_total), fontSize = 12.sp, color = Color.Gray)
                    Text("$${total.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            breakdown.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(12.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = chartColors[index % chartColors.size])
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val localizedName = categoryNameMap[entry.key]?.let { stringResource(it) } ?: entry.key
                    Text(localizedName, modifier = Modifier.weight(1f), fontSize = 14.sp)

                    val percentage = if (total > 0) (entry.value / total * 100) else 0.0
                    Text(
                        "$${entry.value.toInt()} (${String.format("%.1f", percentage)}%)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
