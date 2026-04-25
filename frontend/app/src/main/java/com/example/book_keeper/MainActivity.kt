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
import com.example.book_keeper.ui.ExpenseScreen
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

                // 根據狀態決定要顯示哪個畫面
                if (isLoggedIn) {
                    // 傳入登出邏輯：將 isLoggedIn 設回 false
                    ExpenseScreen(onLogout = {
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