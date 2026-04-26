package com.example.book_keeper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.book_keeper.network.TokenManager
import com.example.book_keeper.ui.AuthScreen
import com.example.book_keeper.ui.MainScaffold
import com.example.book_keeper.ui.theme.BookkeeperTheme
import com.example.book_keeper.utils.LanguageManager
import com.example.book_keeper.utils.ThemeManager

/**
 * 應用程式的主要進入點。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            
            // 監聽語言與主題變更
            var language by remember { mutableStateOf(LanguageManager.getLanguage(context)) }
            var themeMode by remember { mutableStateOf(ThemeManager.getThemeMode(context)) }

            val onLanguageChange = { newLang: String ->
                LanguageManager.setLanguage(context, newLang)
                language = newLang
                recreate()
            }

            val onThemeChange = { newMode: String ->
                ThemeManager.setThemeMode(context, newMode)
                themeMode = newMode
            }

            BookkeeperTheme(themeMode = themeMode) {
                var isLoggedIn by remember {
                    mutableStateOf(TokenManager.getToken(context) != null)
                }

                if (isLoggedIn) {
                    MainScaffold(
                        onLogout = {
                            TokenManager.clearToken(context)
                            isLoggedIn = false
                        },
                        onLanguageChange = onLanguageChange,
                        onThemeChange = onThemeChange
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
        super.attachBaseContext(LanguageManager.updateBaseContextLocale(newBase))
    }
}
