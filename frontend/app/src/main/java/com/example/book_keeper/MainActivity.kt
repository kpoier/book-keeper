package com.example.book_keeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.book_keeper.network.ApiClient
import com.example.book_keeper.network.RecordPayload
import com.example.book_keeper.network.TokenManager
import com.example.book_keeper.ui.AuthScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current

                // 檢查登入狀態
                var isLoggedIn by remember {
                    mutableStateOf(TokenManager.getToken(context) != null)
                }

                if (isLoggedIn) {
                    ExpenseScreen(onLogout = {
                        TokenManager.clearToken(context)
                        isLoggedIn = false
                    })
                } else {
                    AuthScreen(onAuthSuccess = {
                        isLoggedIn = true
                    })
                }
            }
        }
    }
}

@Composable
fun ExpenseScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 使用封裝好的 ApiClient，它會自動處理 Token 攔截
    val apiService = remember { ApiClient.create(context) }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "新增收支紀錄",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("金額") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("備註") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        val payload = RecordPayload(
                            amount = amountValue,
                            category = "一般",
                            recordType = "expense",
                            date = "2024-04-01T12:00:00Z",
                            note = note
                        )

                        val response = apiService.createRecord(payload)
                        if (response.isSuccessful) {
                            println("成功建立紀錄: ${response.body()?.message}")
                            amount = ""
                            note = ""
                        } else {
                            println("錯誤: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        println("發生錯誤: ${e.message}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("儲存紀錄")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
        ) {
            Text("登出帳號")
        }
    }
}