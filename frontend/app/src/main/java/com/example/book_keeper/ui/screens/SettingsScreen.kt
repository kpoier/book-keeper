package com.example.book_keeper.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.book_keeper.utils.FileUtils
import com.example.book_keeper.utils.LanguageManager
import com.example.book_keeper.utils.ThemeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit, 
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    
    val currentLanguage = LanguageManager.getLanguage(context)
    val currentThemeMode = ThemeManager.getThemeMode(context)
    
    var username by remember { mutableStateOf("...") }
    var langExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var exportExpanded by remember { mutableStateOf(false) }

    // 只儲存選中的月份 Key (null 代表全部)
    var selectedExportMonthKey by remember { mutableStateOf<String?>(null) }

    // 當語言改變時，重新生成選項列表
    val monthOptions = remember(currentLanguage) {
        val options = mutableListOf<Pair<String?, String>>()
        // 使用 US Locale 確保 Key 是標準數字 (2024-05)，而 Display 使用預設 Locale
        val keySdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val displaySdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        val calendar = Calendar.getInstance()
        for (i in 0 until 12) {
            val key = keySdf.format(calendar.time)
            val display = displaySdf.format(calendar.time)
            options.add(key to display)
            calendar.add(Calendar.MONTH, -1)
        }
        options
    }

    val languages = listOf(
        "en" to R.string.lang_en,
        "zh-TW" to R.string.lang_zh_tw,
        "zh-CN" to R.string.lang_zh_cn
    )
    
    val themes = listOf(
        "light" to R.string.theme_light,
        "dark" to R.string.theme_dark,
        "system" to R.string.theme_system
    )
    
    val selectedLanguageName = stringResource(languages.find { it.first == currentLanguage }?.second ?: R.string.lang_en)
    val selectedThemeName = stringResource(themes.find { it.first == currentThemeMode }?.second ?: R.string.theme_system)

    // 動態決定顯示的月份標籤
    val selectedExportLabel = if (selectedExportMonthKey == null) {
        stringResource(R.string.settings_export_all)
    } else {
        // 從選項中找對應的顯示名稱，若找不到則顯示 Key 本身
        monthOptions.find { it.first == selectedExportMonthKey }?.second ?: selectedExportMonthKey!!
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val response = apiService.getMe()
                if (response.isSuccessful) {
                    username = response.body()?.username ?: "Unknown"
                }
            } catch (e: Exception) {
                username = "Error"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_current_user), color = Color.Gray, fontSize = 14.sp)
                    Text(username, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 語言設定
            SettingsDropdownCard(
                label = stringResource(R.string.settings_language),
                selectedText = selectedLanguageName,
                expanded = langExpanded,
                onExpandedChange = { langExpanded = it },
                items = languages,
                onItemClick = { onLanguageChange(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 主題設定
            SettingsDropdownCard(
                label = stringResource(R.string.settings_theme),
                selectedText = selectedThemeName,
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = it },
                items = themes,
                onItemClick = { onThemeChange(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 匯出 CSV 設定
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_export_data), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.settings_export_month_desc), color = Color.Gray, fontSize = 12.sp)
                    
                    ExposedDropdownMenuBox(
                        expanded = exportExpanded,
                        onExpandedChange = { exportExpanded = !exportExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExportLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exportExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = exportExpanded,
                            onDismissRequest = { exportExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_export_all)) },
                                onClick = {
                                    selectedExportMonthKey = null
                                    exportExpanded = false
                                }
                            )
                            monthOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = {
                                        selectedExportMonthKey = option.first
                                        exportExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val response = apiService.exportRecords(month = selectedExportMonthKey)
                                    if (response.isSuccessful && response.body() != null) {
                                        val fileName = if (selectedExportMonthKey == null) "records_all.csv" else "records_${selectedExportMonthKey}.csv"
                                        val isSaved = FileUtils.saveCsvToDownloads(context, response.body()!!, fileName)
                                        val msgRes = if (isSaved) R.string.settings_export_success else R.string.settings_export_fail
                                        snackbarHostState.showSnackbar(context.getString(msgRes))
                                    } else {
                                        snackbarHostState.showSnackbar(context.getString(R.string.settings_export_fail))
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_fail))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_export_btn))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 登出按鈕
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_logout), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownCard(
    label: String,
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<Pair<String, Int>>,
    onItemClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
                OutlinedTextField(
                    value = selectedText,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    items.forEach { (key, resId) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(resId)) },
                            onClick = { onItemClick(key); onExpandedChange(false) }
                        )
                    }
                }
            }
        }
    }
}