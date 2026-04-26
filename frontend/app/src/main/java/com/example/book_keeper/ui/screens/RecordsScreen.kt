package com.example.book_keeper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.book_keeper.R
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.RecordPayload
import com.example.book_keeper.network.RecordResponse
import com.example.book_keeper.utils.DateUtils
import kotlinx.coroutines.launch

/**
 * 歷史紀錄頁面 Composable。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }

    val categoryMap = remember {
        mapOf(
            "food" to R.string.cat_food,
            "transport" to R.string.cat_transport,
            "housing" to R.string.cat_housing,
            "entertainment" to R.string.cat_entertainment,
            "shopping" to R.string.cat_shopping,
            "electronics" to R.string.cat_electronics,
            "other" to R.string.cat_other
        )
    }

    var allRecords by remember { mutableStateOf<List<RecordResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editingRecord by remember { mutableStateOf<RecordResponse?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allCategoriesKey = "all"
    var selectedCategoryKey by remember { mutableStateOf(allCategoriesKey) }
    
    val sortOptionsMap = mapOf(
        stringResource(R.string.sort_date_desc) to "date_desc",
        stringResource(R.string.sort_date_asc) to "date_asc",
        stringResource(R.string.sort_amount_desc) to "amount_desc",
        stringResource(R.string.sort_amount_asc) to "amount_asc"
    )
    val sortOptions = sortOptionsMap.keys.toList()
    var sortOrderLabel by remember { mutableStateOf(sortOptions[0]) }
    
    var showFilters by remember { mutableStateOf(false) }

    val filterCategories = remember {
        listOf(allCategoriesKey) + categoryMap.keys.toList()
    }

    val loadRecords = {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.getRecords(limit = 200)
                if (response.isSuccessful) allRecords = response.body() ?: emptyList()
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadRecords() }

    val filteredRecords = remember(allRecords, searchQuery, selectedCategoryKey, sortOrderLabel) {
        var list = allRecords.filter { record ->
            val matchesQuery = (record.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    record.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryKey == allCategoriesKey || record.category == selectedCategoryKey
            matchesQuery && matchesCategory
        }

        when (sortOptionsMap[sortOrderLabel]) {
            "date_desc" -> list = list.sortedByDescending { it.date }
            "date_asc" -> list = list.sortedBy { it.date }
            "amount_desc" -> list = list.sortedByDescending { it.amount }
            "amount_asc" -> list = list.sortedBy { it.amount }
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.records_history), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    imageVector = if (showFilters) Icons.Default.Close else Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.records_filter)
                )
            }
        }

        AnimatedVisibility(visible = showFilters) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.records_search_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                            }
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var categoryExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedCategoryKey == allCategoriesKey) stringResource(R.string.records_all_categories) else stringResource(categoryMap[selectedCategoryKey] ?: R.string.cat_other),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.home_category)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                                filterCategories.forEach { key ->
                                    val label = if (key == allCategoriesKey) stringResource(R.string.records_all_categories) else stringResource(categoryMap[key] ?: R.string.cat_other)
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { selectedCategoryKey = key; categoryExpanded = false }
                                    )
                                }
                            }
                        }

                        var sortExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = sortExpanded,
                            onExpandedChange = { sortExpanded = !sortExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = sortOrderLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.records_sort)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sortExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(sortExpanded, { sortExpanded = false }) {
                                sortOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = { sortOrderLabel = opt; sortExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filteredRecords.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.records_empty), color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = filteredRecords, key = { it.id }) { record ->
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
                            // 修正處：加入圓角裁切以避免邊緣露出的問題
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CardDefaults.shape) // 使用與前景卡片相同的圓角
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(20.dp), 
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, stringResource(R.string.records_delete))
                            }
                        },
                        content = { RecordItem(record, categoryMap, onClick = { editingRecord = record }) }
                    )
                }
            }
        }
    }

    if (editingRecord != null) {
        EditRecordDialog(
            record = editingRecord!!,
            categoryMap = categoryMap,
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
fun RecordItem(record: RecordResponse, categoryMap: Map<String, Int>, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                val localizedCategory = categoryMap[record.category]?.let { stringResource(it) } ?: record.category
                Text(localizedCategory, fontWeight = FontWeight.Bold)
                Text(record.date.take(10), fontSize = 12.sp, color = Color.Gray)
                if (!record.note.isNullOrBlank()) Text(record.note, fontSize = 14.sp)
            }
            Text(
                "$ ${record.amount}",
                fontWeight = FontWeight.Bold,
                color = if (record.record_type == "expense") Color.Red else Color.Green
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordDialog(
    record: RecordResponse, 
    categoryMap: Map<String, Int>,
    onDismiss: () -> Unit, 
    onConfirm: (Int, RecordPayload) -> Unit
) {
    var amount by remember { mutableStateOf(record.amount.toString()) }
    var note by remember { mutableStateOf(record.note ?: "") }
    var selectedDateMillis by remember { mutableStateOf(DateUtils.isoToMillis(record.date)) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val categoryKeys = categoryMap.keys.toList()
    var selectedCategoryKey by remember { mutableStateOf(if (categoryKeys.contains(record.category)) record.category else categoryKeys[0]) }
    var expanded by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis; showDatePicker = false
            }) { Text(stringResource(R.string.home_confirm)) }
        }) { DatePicker(datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.records_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.home_amount)) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = stringResource(categoryMap[selectedCategoryKey] ?: R.string.cat_other),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.home_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        categoryKeys.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(stringResource(categoryMap[key] ?: R.string.cat_other)) },
                                onClick = { selectedCategoryKey = key; expanded = false })
                        }
                    }
                }
                
                OutlinedTextField(
                    value = DateUtils.millisToDisplayDate(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.home_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.home_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@Button
                onConfirm(
                    record.id,
                    RecordPayload(
                        amt,
                        selectedCategoryKey,
                        record.record_type,
                        DateUtils.millisToIsoString(selectedDateMillis),
                        note.ifBlank { null })
                )
            }) { Text(stringResource(R.string.records_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_cancel)) } }
    )
}
