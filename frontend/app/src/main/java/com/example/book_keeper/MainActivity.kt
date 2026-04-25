package com.example.book_keeper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.book_keeper.network.TokenManager
import com.example.book_keeper.ui.AuthScreen
import com.example.book_keeper.ui.MainScaffold
import com.example.book_keeper.utils.LanguageManager

/**
 * 應用程式的主要進入點。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 監聽語言變更，強制重新組件
            val context = LocalContext.current
            var language by remember { mutableStateOf(LanguageManager.getLanguage(context)) }

            // 提供一個回調來刷新語言
            val onLanguageChange = { newLang: String ->
                LanguageManager.setLanguage(context, newLang)
                language = newLang
                // 重啟 Activity 以完整套用資源變更
                recreate()
            }

            MaterialTheme {
                // 檢查登入狀態
                var isLoggedIn by remember {
                    mutableStateOf(TokenManager.getToken(context) != null)
                }

                if (isLoggedIn) {
                    MainScaffold(
                        onLogout = {
                            TokenManager.clearToken(context)
                            isLoggedIn = false
                        },
                        onLanguageChange = onLanguageChange
                    )
                } else {
                    AuthScreen(
                        onAuthSuccess = { isLoggedIn = true },
                        onLanguageChange = onLanguageChange
                    )
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // 在 Activity 附加時套用儲存的語言
        super.attachBaseContext(LanguageManager.updateBaseContextLocale(newBase))
    }
}
