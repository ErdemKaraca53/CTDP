package com.erdem.tdp.design

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onSaveClick: (String, String) -> Unit // HighBpm, LowBpm gönderilecek
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    // State'ler (Varsayılan değerleri hafızadan çeker)
    var name by remember { mutableStateOf(prefs.userName) }
    var email by remember { mutableStateOf(prefs.userEmail) }
    var highBpm by remember { mutableStateOf(prefs.highBpmLimit) }
    var lowBpm by remember { mutableStateOf(prefs.lowBpmLimit) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sistem Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F9FF)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- KİŞİSEL BİLGİLER ---
            Text("Kişisel Bilgiler", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ad Soyad") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-posta Adresi") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // --- GÜVENLİK AYARLARI ---
            Text("Güvenlik Limitleri (Nabız)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            Text("Bu değerler aşılırsa sistem uyarı verir.", fontSize = 12.sp, color = Color.Gray)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = lowBpm,
                    onValueChange = { lowBpm = it },
                    label = { Text("Alt Limit") },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = highBpm,
                    onValueChange = { highBpm = it },
                    label = { Text("Üst Limit") },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- KAYDET BUTONU ---
            Button(
                onClick = {
                    // 1. Verileri Telefona Kaydet
                    prefs.userName = name
                    prefs.userEmail = email
                    prefs.highBpmLimit = highBpm
                    prefs.lowBpmLimit = lowBpm

                    // 2. Arduino'ya Göndermek İçin Callback Çağır
                    onSaveClick(highBpm, lowBpm)

                    Toast.makeText(context, "Ayarlar Kaydedildi ve Cihaza Gönderildi!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ayarları Kaydet ve Eşitle", fontSize = 16.sp)
            }
        }
    }
}