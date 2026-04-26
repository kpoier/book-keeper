package com.example.book_keeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.book_keeper.R
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.AuthPayload
import com.example.book_keeper.network.TokenManager
import com.example.book_keeper.utils.LanguageManager
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 認證畫面 Composable。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit, onLanguageChange: (String) -> Unit) {
    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) }

    val currentLanguage = remember { LanguageManager.getLanguage(context) }
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf(
        "en" to stringResource(R.string.lang_en),
        "zh-TW" to stringResource(R.string.lang_zh_tw),
        "zh-CN" to stringResource(R.string.lang_zh_cn)
    )
    val selectedLanguageName = languages.find { it.first == currentLanguage }?.second ?: languages[0].second

    // 使用 Surface 確保背景色與內容文字顏色自動對應主題
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoginMode) stringResource(R.string.auth_login_title) else stringResource(R.string.auth_register_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.auth_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.auth_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                message = context.getString(R.string.auth_empty_error)
                                return@Button
                            }
                            isLoading = true
                            message = ""

                            coroutineScope.launch {
                                try {
                                    val payload = AuthPayload(username, password)
                                    if (isLoginMode) {
                                        val response = apiService.login(payload)
                                        if (response.isSuccessful) {
                                            val token = response.body()?.token
                                            if (token != null) {
                                                TokenManager.saveToken(context, token)
                                                onAuthSuccess()
                                            } else {
                                                message = "Server error"
                                            }
                                        } else {
                                            message = context.getString(R.string.auth_login_fail)
                                        }
                                    } else {
                                        val response = apiService.register(payload)
                                        if (response.isSuccessful) {
                                            message = context.getString(R.string.auth_register_success)
                                            isLoginMode = true
                                        } else {
                                            val errorJson = response.errorBody()?.string()
                                            val errorMsg = try {
                                                JSONObject(errorJson ?: "").getString("message")
                                            } catch (e: Exception) { "" }
                                            
                                            message = if (errorMsg.contains("exist", ignoreCase = true)) {
                                                context.getString(R.string.auth_register_fail)
                                            } else {
                                                context.getString(R.string.auth_register_fail)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    message = context.getString(R.string.auth_network_error)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoginMode) stringResource(R.string.auth_login_btn) else stringResource(R.string.auth_register_btn))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    isLoginMode = !isLoginMode
                    message = ""
                }) {
                    Text(if (isLoginMode) stringResource(R.string.auth_to_register) else stringResource(R.string.auth_to_login))
                }

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }

            // 語言選擇器
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .widthIn(max = 200.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguageName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
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
    }
}
