package com.example.book_keeper.ui.screens

import androidx.compose.foundation.layout.*
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
import com.example.book_keeper.utils.LanguageManager
import kotlinx.coroutines.launch

/**
 * 設定頁面 Composable。
 * 顯示目前使用者資訊並提供登出功能。
 * 
 * @param onLogout 當使用者點擊登出按鈕時的回調。
 * @param onLanguageChange 當使用者更改語言時的回調。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit, onLanguageChange: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }
    
    val currentLanguage = remember { LanguageManager.getLanguage(context) }
    
    var username by remember { mutableStateOf("...") }
    var expanded by remember { mutableStateOf(false) }
    
    val languages = listOf(
        "en" to stringResource(R.string.lang_en),
        "zh-TW" to stringResource(R.string.lang_zh_tw),
        "zh-CN" to stringResource(R.string.lang_zh_cn)
    )
    
    val selectedLanguageName = languages.find { it.first == currentLanguage }?.second ?: languages[0].second

    // 取得當前使用者資訊
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

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 使用者資訊卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_current_user), color = Color.Gray, fontSize = 14.sp)
                Text(username, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 語言設定
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_language), color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguageName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onLanguageChange(code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 登出按鈕
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_logout))
        }
    }
}
