package com.erdem.tdp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import com.erdem.tdp.design.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class MainActivity : ComponentActivity() {

    // Bluetooth UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var heartRateViewModel: HeartRateViewModel

    // Global Bluetooth Socket
    private var bluetoothSocket: BluetoothSocket? = null
    private var isConnected = false

    // Bildirim Değişkenleri
    private val CHANNEL_ID = "YASLI_TAKIP_CHANNEL"
    private var lastNotificationTime = 0L // Bildirim spamını önlemek için

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. İzinleri İste (Bluetooth + Bildirim)
        checkAndRequestPermissions()

        // 2. Bildirim Kanalını Oluştur
        createNotificationChannel()

        // 3. ViewModel ve Tercihler
        heartRateViewModel = ViewModelProvider(this)[HeartRateViewModel::class.java]
        val preferences = UserPreferences(this)

        // 4. Bluetooth Başlat
        startBluetoothListening()

        // 5. Arayüz
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                if (currentScreen == "dashboard") {
                    DashboardScreen(
                        viewModel = heartRateViewModel,
                        userName = preferences.userName,
                        onSettingsClick = { currentScreen = "settings" }
                    )
                } else {
                    SettingsScreen(
                        onBackClick = { currentScreen = "dashboard" },
                        onSaveClick = { high, low ->
                            sendDataToArduino("SET_HIGH:$high")
                            try { Thread.sleep(100) } catch (e: Exception){}
                            sendDataToArduino("SET_LOW:$low")
                            currentScreen = "dashboard"
                        }
                    )
                }
            }
        }
    }

    // ------------------------------------------------
    // BİLDİRİM FONKSİYONLARI (YENİ)
    // ------------------------------------------------

    private fun createNotificationChannel() {
        // Android 8.0 ve üzeri için kanal zorunludur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Acil Durum Bildirimleri"
            val descriptionText = "Düşme ve Panik uyarıları"
            val importance = NotificationManager.IMPORTANCE_HIGH // Sesli ve titreşimli
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendRiskNotification(title: String, content: String) {
        // Spam koruması: Aynı bildirimi 10 saniye içinde tekrar gönderme
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < 10000) return

        lastNotificationTime = currentTime

        // İzin kontrolü (Android 13+)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Bildirime tıklayınca uygulamayı açmak için Intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Varsayılan uyarı ikonu
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX) // En yüksek öncelik
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Titreşim deseni

        with(NotificationManagerCompat.from(this)) {
            notify(101, builder.build())
        }
    }

    // ------------------------------------------------
    // VERİ AYRIŞTIRMA (GÜNCELLENDİ)
    // ------------------------------------------------
    private fun parseArduinoData(message: String) {
        if (message.startsWith("#")) {
            val parts = message.substring(1).split(",")
            if (parts.isNotEmpty()) {
                val tag = parts[0]
                val val1 = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val val2 = parts.getOrNull(2)?.toIntOrNull() ?: 0

                runOnUiThread {
                    when (tag) {
                        "NORMAL" -> {
                            heartRateViewModel.updateStatus(TrackingStatus.NORMAL)
                            heartRateViewModel.updateMetrics(bpm = val1, spo2Val = val2)
                        }
                        "ALERT_BPM" -> {
                            heartRateViewModel.updateStatus(TrackingStatus.PULSE_ALERT)
                            heartRateViewModel.updateMetrics(bpm = val1, spo2Val = val2)
                            // BİLDİRİM TETİKLE
                            sendRiskNotification("RİSKLİ NABIZ!", "Nabız değerleri sınır dışına çıktı: $val1 bpm")
                        }
                        "WARNING_FALL" -> {
                            heartRateViewModel.updateFallTimer(val1)
                        }
                        "ALARM_FALL" -> {
                            heartRateViewModel.updateStatus(TrackingStatus.ALARM_FALL)
                            heartRateViewModel.updateMetrics(bpm = val1, spo2Val = val2)
                            // BİLDİRİM TETİKLE
                            sendRiskNotification("DÜŞME TESPİT EDİLDİ!", "Acil durum! Kişi düştü ve hareket etmiyor.")
                        }
                        "PANIC" -> {
                            heartRateViewModel.triggerPanic()
                            // BİLDİRİM TETİKLE
                            sendRiskNotification("ACİL YARDIM BUTONU!", "Kullanıcı panik butonuna bastı!")
                        }
                        "RECOVERY" -> heartRateViewModel.updateStatus(TrackingStatus.NORMAL)
                        "IDLE" -> {
                            heartRateViewModel.updateStatus(TrackingStatus.IDLE)
                            heartRateViewModel.updateMetrics(0)
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------
    // İZİN KONTROLLERİ (TOPLU)
    // ------------------------------------------------
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        // Android 13+ için Bildirim İzni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 1)
        }
    }

    // ------------------------------------------------
    // BLUETOOTH İŞLEMLERİ (AYNI KALDI)
    // ------------------------------------------------
    private fun sendDataToArduino(message: String) {
        if (bluetoothSocket != null && isConnected) {
            try {
                bluetoothSocket!!.outputStream.write((message + "\n").toByteArray())
            } catch (e: Exception) {
                Log.e("BLUETOOTH", "Hata: ${e.message}")
            }
        }
    }

    private fun startBluetoothListening() {
        Thread {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return@Thread

            try {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@Thread
                val device = adapter.bondedDevices.firstOrNull { it.name == "Yasli_Takip_Sistemi" } ?: return@Thread

                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                isConnected = true

                val reader = BufferedReader(InputStreamReader(bluetoothSocket!!.inputStream))
                while (isConnected) {
                    try {
                        val rawMsg = reader.readLine()
                        if (!rawMsg.isNullOrEmpty()) parseArduinoData(rawMsg.trim())
                    } catch (e: Exception) {
                        isConnected = false
                        break
                    }
                }
            } catch (e: Exception) {
                isConnected = false
            }
        }.start()
    }
}