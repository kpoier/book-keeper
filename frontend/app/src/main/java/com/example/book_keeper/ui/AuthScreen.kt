package com.example.book_keeper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.AuthPayload
import com.example.book_keeper.network.TokenManager
import kotlinx.coroutines.launch

@Composable
// 傳入一個 callback 函數，當登入成功時通知外部切換畫面
fun AuthScreen(onAuthSuccess: () -> Unit) {
    // 定義畫面的各種狀態
    var isLoginMode by remember { mutableStateOf(true) } // true=登入, false=註冊
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.create(context) } // 初始化自動帶 Token 的 API 客戶端

    // 垂直置中排列所有元件
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 標題
        Text(
            text = if (isLoginMode) "登入 Book-Keeper" else "註冊新帳號",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 帳號輸入框
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("帳號") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 密碼輸入框 (輸入時會變成隱碼)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密碼") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 登入/註冊按鈕與載入中動畫
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        message = "帳號密碼不能為空"
                        return@Button
                    }
                    isLoading = true
                    message = ""

                    // 開啟協程發送網路請求
                    coroutineScope.launch {
                        try {
                            val payload = AuthPayload(username, password)
                            if (isLoginMode) {
                                // 執行登入
                                val response = apiService.login(payload)
                                if (response.isSuccessful) {
                                    val token = response.body()?.token
                                    if (token != null) {
                                        TokenManager.saveToken(context, token) // 將 Token 存入手機
                                        onAuthSuccess() // 呼叫成功事件，準備跳轉畫面
                                    }
                                } else {
                                    message = "登入失敗：帳號或密碼錯誤"
                                }
                            } else {
                                // 執行註冊
                                val response = apiService.register(payload)
                                if (response.isSuccessful) {
                                    message = "註冊成功！請直接登入"
                                    isLoginMode = true // 註冊完自動切回登入模式
                                } else {
                                    message = "註冊失敗：帳號可能已存在"
                                }
                            }
                        } catch (e: Exception) {
                            message = "網路錯誤：${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoginMode) "登入" else "註冊")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 切換模式的純文字按鈕
        TextButton(onClick = {
            isLoginMode = !isLoginMode
            message = "" // 切換時清空錯誤訊息
        }) {
            Text(if (isLoginMode) "還沒有帳號？點此註冊" else "已有帳號？點此登入")
        }

        // 顯示錯誤或提示訊息
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }
}