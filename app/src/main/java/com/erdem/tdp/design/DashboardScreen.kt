package com.erdem.tdp.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/* ---------------------------------------------------
   RENK PALETİ - DURUMA GÖRE
--------------------------------------------------- */
private val ColorNormal = Color(0xFF2196F3)      // Mavi (Her şey yolunda)
private val ColorWarning = Color(0xFFFF9800)     // Turuncu (Nabız yüksek/Geri sayım)
private val ColorCritical = Color(0xFFD32F2F)    // Kırmızı (Düştü/Panik)
private val ColorIdle = Color(0xFF9E9E9E)        // Gri (Boşta)

private val BackgroundGlass = Color(0xFFFFFFFF).copy(alpha = 0.8f)

/* ---------------------------------------------------
   DASHBOARD SCREEN (ANA EKRAN)
--------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HeartRateViewModel = viewModel(),
    userName: String = "Kullanıcı", // Varsayılan değer
    onSettingsClick: () -> Unit = {} // Tıklama işlevi
) {

    // ViewModel'dan gelen verileri dinliyoruz
    val heartRate by viewModel.heartRate.collectAsState()
    val spo2 by viewModel.spo2.collectAsState()
    val status by viewModel.systemStatus.collectAsState()
    val fallTimer by viewModel.fallCountdown.collectAsState()

    // Duruma göre ana rengi belirle (Animasyonlu geçiş)
    val mainColor by animateColorAsState(
        targetValue = when (status) {
            TrackingStatus.NORMAL -> ColorNormal
            TrackingStatus.PULSE_ALERT, TrackingStatus.WARNING_FALL -> ColorWarning
            TrackingStatus.ALARM_FALL, TrackingStatus.PANIC -> ColorCritical
            TrackingStatus.IDLE -> ColorIdle
        },
        animationSpec = tween(500), label = "colorAnim"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F9FF) // Genel arka plan
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header (İsim Bilgisi Buraya Gidiyor)
            HeaderSection(userName = userName, status = status, color = mainColor)

            // Orta Daire: Duruma göre Nabız veya Geri Sayım gösterir
            DynamicCircularCard(
                heartRate = heartRate,
                fallTimer = fallTimer,
                status = status,
                mainColor = mainColor
            )

            // Alt Bilgi Kartları
            StatusGrid(
                status = status,
                spo2 = spo2,
                mainColor = mainColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // Ayarlar Butonu (Tıklama Olayı Bağlandı)
            FilledTonalButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = mainColor.copy(alpha = 0.15f),
                    contentColor = mainColor
                )
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sistem Ayarları", fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}


/* ---------------------------------------------------
   HEADER
--------------------------------------------------- */

@Composable
fun HeaderSection(userName: String, status: TrackingStatus, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Hoş geldiniz,", fontSize = 16.sp, color = Color.Gray)
        // Dinamik İsim Gösterimi
        Text(userName, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // Durum Metni
        val statusText = when (status) {
            TrackingStatus.NORMAL -> "Sistem Normal"
            TrackingStatus.IDLE -> "Sensör Beklemede"
            TrackingStatus.WARNING_FALL -> "DÜŞME ALGILANIYOR..."
            TrackingStatus.ALARM_FALL -> "!!! DÜŞME ALARMI !!!"
            TrackingStatus.PANIC -> "!!! ACİL YARDIM !!!"
            TrackingStatus.PULSE_ALERT -> "Nabız Uyarısı"
        }

        Text(
            text = statusText,
            fontSize = 18.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------------------------------------------------
   DİNAMİK ORTA KART (NABIZ / GERİ SAYIM)
--------------------------------------------------- */

@Composable
fun DynamicCircularCard(
    heartRate: Int,
    fallTimer: Int,
    status: TrackingStatus,
    mainColor: Color
) {
    // Düşme şüphesi varsa (WARNING_FALL) geri sayımı göster, yoksa nabzı göster
    val isCountingDown = status == TrackingStatus.WARNING_FALL

    val displayValue = if (isCountingDown) fallTimer.toString() else heartRate.toString()
    val displayUnit = if (isCountingDown) "Saniye" else "bpm"
    val subText = when (status) {
        TrackingStatus.WARNING_FALL -> "Müdahale Bekleniyor"
        TrackingStatus.ALARM_FALL -> "YARDIM ÇAĞRILIYOR"
        TrackingStatus.PANIC -> "BUTONA BASILDI"
        TrackingStatus.IDLE -> "Parmağınızı Yerleştirin"
        TrackingStatus.PULSE_ALERT -> "Riskli Seviye"
        else -> "Normal Aralıkta"
    }

    val icon =
        if (isCountingDown || status == TrackingStatus.ALARM_FALL) Icons.Default.Warning else Icons.Default.Favorite

    Card(
        modifier = Modifier.size(280.dp),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundGlass)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(4.dp, mainColor.copy(alpha = 0.3f), CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(mainColor.copy(alpha = 0.1f), Color.White)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // İkon (Kalp veya Ünlem)
                Icon(
                    icon,
                    contentDescription = null,
                    tint = mainColor,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Ana Sayı (Nabız veya Sayaç)
                Text(
                    text = displayValue,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = mainColor
                )

                // Birim
                Text(displayUnit, fontSize = 20.sp, color = Color.Gray)

                Spacer(Modifier.height(8.dp))

                // Alt Açıklama
                Text(
                    text = subText,
                    fontSize = 16.sp,
                    color = mainColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ---------------------------------------------------
   STATUS GRID (DURUM KARTLARI)
--------------------------------------------------- */

@Composable
fun StatusGrid(status: TrackingStatus, spo2: Int, mainColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 1. SATIR
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Oksijen Kartı
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "SpO2 (Oksijen)",
                statusValue = "%$spo2",
                icon = Icons.Default.Air,
                color = Color(0xFF00BCD4) // Cyan
            )

            // Hareket Durumu
            val moveText = if (status == TrackingStatus.ALARM_FALL) "Düştü!" else "Normal"
            val moveColor =
                if (status == TrackingStatus.ALARM_FALL) ColorCritical else Color(0xFF4CAF50)

            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Hareket",
                statusValue = moveText,
                icon = Icons.Default.DirectionsWalk,
                color = moveColor
            )
        }

        // 2. SATIR
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            // Alarm Durumu
            val alarmText =
                if (status == TrackingStatus.ALARM_FALL || status == TrackingStatus.PANIC) "AKTİF" else "Pasif"
            val alarmColorIcon =
                if (status == TrackingStatus.ALARM_FALL || status == TrackingStatus.PANIC) ColorCritical else Color.Gray

            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Acil Durum Alarmı",
                statusValue = alarmText,
                icon = if (alarmText == "AKTİF") Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                color = alarmColorIcon
            )

            // Bağlantı (Varsayılan bağlı kabul ediyoruz veri geliyorsa)
            StatusCard(
                modifier = Modifier.weight(1f),
                title = "Bluetooth",
                statusValue = "Bağlı",
                icon = Icons.Default.BluetoothConnected,
                color = Color(0xFF3F51B5)
            )
        }
    }
}

/* ---------------------------------------------------
   TEKİL KART BİLEŞENİ
--------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    statusValue: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // İkon Kutusu
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }

            Spacer(Modifier.width(12.dp))

            // Metinler
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(
                    statusValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

// Önizleme
@Preview(showBackground = true)
@Composable
fun PreviewDashboardV2() {
    MaterialTheme {
        DashboardScreen(
            userName = "Ali Veli",
            onSettingsClick = {}
        )
    }
}