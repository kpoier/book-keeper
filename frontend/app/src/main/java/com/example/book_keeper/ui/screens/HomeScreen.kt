package com.example.book_keeper.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val database = remember { com.example.book_keeper.data.local.AppDatabase.getDatabase(context) }
    val repository = remember { com.example.book_keeper.data.repository.TransactionRepository(apiService, database.transactionDao()) }

    // 定義類別代碼、資源 ID 與 圖標的映射
    val categoryOptions = remember {
        listOf(
            Triple("food", R.string.cat_food, Icons.Default.Restaurant),
            Triple("transport", R.string.cat_transport, Icons.Default.DirectionsCar),
            Triple("housing", R.string.cat_housing, Icons.Default.Home),
            Triple("entertainment", R.string.cat_entertainment, Icons.Default.TheaterComedy),
            Triple("shopping", R.string.cat_shopping, Icons.Default.ShoppingCart),
            Triple("electronics", R.string.cat_electronics, Icons.Default.Devices),
            Triple("other", R.string.cat_other, Icons.Default.Category)
        )
    }

    val allRecords by repository.getAllActiveRecords().collectAsState(initial = emptyList())

    val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
    val currentMonth = remember { sdf.format(java.util.Date()) }
    val lastMonth = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -1)
        sdf.format(cal.time)
    }

    val currentMonthRecords = remember(allRecords, currentMonth) {
        allRecords.filter { it.date.startsWith(currentMonth) }
    }
    val totalExpense = remember(currentMonthRecords) {
        currentMonthRecords.filter { it.record_type == "expense" }.sumOf { it.amount }
    }
    val categoryBreakdown = remember(currentMonthRecords) {
        currentMonthRecords.filter { it.record_type == "expense" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val lastMonthRecords = remember(allRecords, lastMonth) {
        allRecords.filter { it.date.startsWith(lastMonth) }
    }
    val lastMonthTotalExpense = remember(lastMonthRecords) {
        lastMonthRecords.filter { it.record_type == "expense" }.sumOf { it.amount }
    }
    val lastMonthCategoryBreakdown = remember(lastMonthRecords) {
        lastMonthRecords.filter { it.record_type == "expense" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    var selectedCategoryKey by remember { mutableStateOf(categoryOptions[0].first) }

    LaunchedEffect(Unit) { 
        com.example.book_keeper.sync.SyncManager.scheduleSync(context)
    }

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

    val currentSelectedOption = categoryOptions.find { it.first == selectedCategoryKey } ?: categoryOptions[0]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.home_total_expense), fontSize = 16.sp, color = Color.Gray)
        Text("$ $totalExpense", fontSize = 36.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

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
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = stringResource(currentSelectedOption.second),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.home_category)) },
                            leadingIcon = { Icon(currentSelectedOption.third, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categoryOptions.forEach { (key, resId, icon) ->
                                DropdownMenuItem(
                                    leadingIcon = { Icon(icon, null) },
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
                                category = selectedCategoryKey,
                                recordType = "expense",
                                date = DateUtils.millisToIsoString(selectedDateMillis),
                                note = note.ifBlank { null }
                            )
                            repository.createRecord(payload)
                            com.example.book_keeper.sync.SyncManager.scheduleSync(context)
                            amount = ""; note = ""; onRecordAdded()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.home_submit)) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("${stringResource(R.string.home_expense_structure)} ($currentMonth)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (categoryBreakdown.isNotEmpty()) {
            ExpenseChart(categoryBreakdown, totalExpense, categoryOptions.associate { it.first to (it.second to it.third) })
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_no_data), color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("${stringResource(R.string.home_expense_structure)} ($lastMonth)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        if (lastMonthCategoryBreakdown.isNotEmpty()) {
            ExpenseChart(lastMonthCategoryBreakdown, lastMonthTotalExpense, categoryOptions.associate { it.first to (it.second to it.third) })
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ComparisonSummary(
                currentTotal = totalExpense,
                lastTotal = lastMonthTotalExpense,
                currentBreakdown = categoryBreakdown,
                lastBreakdown = lastMonthCategoryBreakdown,
                categoryNameMap = categoryOptions.associate { it.first to it.second }
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
fun ExpenseChart(breakdown: Map<String, Double>, total: Double, categoryMap: Map<String, Pair<Int, ImageVector>>) {
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
                    
                    val mapping = categoryMap[entry.key]
                    if (mapping != null) {
                        Icon(mapping.second, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(mapping.first), modifier = Modifier.weight(1f), fontSize = 14.sp)
                    } else {
                        Text(entry.key, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    }

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
