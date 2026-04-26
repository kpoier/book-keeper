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
import com.example.book_keeper.utils.ThemeManager
import kotlinx.coroutines.launch

/**
 * 設定頁面 Composable。
 * 
 * @param onLogout 當使用者點擊登出按鈕時的回調。
 * @param onLanguageChange 當使用者更改語言時的回調。
 * @param onThemeChange 當使用者更改主題時的回調。
 */
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
    
    // 這裡我們直接監聽來自 ThemeManager 的最新狀態，或者更好的方式是從外部傳入
    // 為確保即時性，我們使用 side-effect 監聽 context 變化
    val currentLanguage = LanguageManager.getLanguage(context)
    val currentThemeMode = ThemeManager.getThemeMode(context)
    
    var username by remember { mutableStateOf("...") }
    var langExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    
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
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguageName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languages.forEach { (code, resId) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(resId)) },
                                onClick = {
                                    onLanguageChange(code)
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 主題設定
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_theme), color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = !themeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedThemeName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        themes.forEach { (mode, resId) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(resId)) },
                                onClick = {
                                    onThemeChange(mode)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_logout))
        }
    }
}
