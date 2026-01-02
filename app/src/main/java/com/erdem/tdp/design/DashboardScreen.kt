package com.erdem.tdp.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Özel Renk Paleti ---
object AppColors {
    val Background = Color(0xFFF4F7FA) // Çok açık gri-mavi arka plan
    val PrimaryDark = Color(0xFF1A237E) // Koyu Mavi (Başlıklar için)
    val PrimaryAccent = Color(0xFF3949AB) // Orta Mavi (Butonlar için)
    val TextSecondary = Color(0xFF757575) // Gri metinler
    val CardSurface = Color.White

    // Durum Renkleri (Daha modern tonlar)
    val HeartRed = Color(0xFFE91E63)
    val RedGradientStart = Color(0xFFFF8A80)
    val RedGradientEnd = Color(0xFFD32F2F)
    val SuccessGreen = Color(0xFF00C853)
    val WarningOrange = Color(0xFFFFAB00)
    val InfoBlue = Color(0xFF2962FF)
    val NeutralGray = Color(0xFF607D8B)
}

/* ---------------------------------------------------
   DASHBOARD SCREEN (YENİLENMİŞ)
--------------------------------------------------- */

@Composable
fun DashboardScreen() {
    // Ekranın kaydırılabilir olması için ScrollState ekliyoruz
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background) // Tüm arka plan rengi
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        HeaderSection()

        HeartRateCardStylish(heartRate = 78)

        Text(
            text = "Cihaz Durumu",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.PrimaryDark,
            modifier = Modifier.padding(top = 8.dp)
        )

        StatusGridStylish()

        Spacer(modifier = Modifier.weight(1f)) // Butonu alta itmek için

        Button(
            onClick = { /* Ayarlar */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.PrimaryAccent
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ayarlar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp)) // Alt boşluk
    }
}

/* ---------------------------------------------------
   HEADER (YENİLENMİŞ)
--------------------------------------------------- */

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hoş Geldiniz,",
                fontSize = 16.sp,
                color = AppColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Gerçek Zamanlı İzleme",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryDark
            )
        }
        // Profil resmi veya ikon için yer tutucu
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(AppColors.PrimaryAccent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profil",
                tint = AppColors.PrimaryAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/* ---------------------------------------------------
   HEART RATE (HERO CARD - YENİLENMİŞ)
--------------------------------------------------- */

@Composable
fun HeartRateCardStylish(heartRate: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        // Gradyan Arka Plan
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AppColors.RedGradientStart, AppColors.RedGradientEnd)
                    )
                )
                .padding(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Canlı Nabız",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$heartRate",
                            fontSize = 48.sp, // Sayıyı çok daha büyük yaptık
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "bpm",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Kalp İkonu için süslü bir kapsayıcı
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------
   STATUS GRID (YENİLENMİŞ)
--------------------------------------------------- */

@Composable
fun StatusGridStylish() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Modifier.weight(1f) kullanarak yan yana eşit alan kaplamalarını sağlıyoruz
            StatusItemStylish(
                title = "Bluetooth",
                value = "Bağlı",
                icon = Icons.Default.BluetoothConnected, // Daha uygun ikon
                accentColor = AppColors.SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            StatusItemStylish(
                title = "Hareket",
                value = "Normal",
                icon = Icons.Default.DirectionsWalk,
                accentColor = AppColors.WarningOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusItemStylish(
                title = "Güvenlik",
                value = "Güvenli",
                icon = Icons.Default.Shield, // Daha modern ikon
                accentColor = AppColors.InfoBlue,
                modifier = Modifier.weight(1f)
            )
            StatusItemStylish(
                title = "Alarm",
                value = "Pasif",
                icon = Icons.Default.NotificationsOff, // Daha uygun ikon
                accentColor = AppColors.NeutralGray,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* ---------------------------------------------------
   STATUS ITEM (YENİLENMİŞ)
--------------------------------------------------- */

@Composable
fun StatusItemStylish(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // İkonu renkli bir daire içine alıyoruz
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.PrimaryDark
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun PreviewStylish() {
    MaterialTheme {
        DashboardScreen()
    }
}