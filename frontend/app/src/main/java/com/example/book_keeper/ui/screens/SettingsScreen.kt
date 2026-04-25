package com.example.book_keeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.book_keeper.network.TokenManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("帳號設定", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("當前使用者", color = Color.Gray, fontSize = 14.sp)
                Text("測試帳號", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                TokenManager.clearToken(context)
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("登出帳號") }
    }
}