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
import android.telephony.SmsManager
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

    // Bildirim ve SMS Değişkenleri
    private val CHANNEL_ID = "YASLI_TAKIP_CHANNEL"
    private var lastNotificationTime = 0L
    private var lastSmsTime = 0L // SMS spamını önlemek için zaman kilidi
    private val EMERGENCY_PHONE_NUMBER = "05466568475" // Buraya acil durum numarasını yazın

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. İzinleri İste (Bluetooth + Bildirim + SMS)
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
    // SMS GÖNDERME FONKSİYONU (YENI)
    // ------------------------------------------------
    // MainActivity.kt içinde:
    private fun sendEmergencySMS(message: String) {
        val preferences = UserPreferences(this)
        val number = preferences.emergencyPhone // Hafızadaki güncel numarayı alır

        if (number.isEmpty()) {
            Log.e("SMS", "Telefon numarası ayarlanmamış!")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSmsTime < 30000) return

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(number, null, message, null, null)
            lastSmsTime = currentTime
        } catch (e: Exception) {
            Log.e("SMS", "Hata: ${e.message}")
        }
    }

    // ------------------------------------------------
    // BİLDİRİM FONKSİYONLARI
    // ------------------------------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Acil Durum Bildirimleri"
            val descriptionText = "Düşme ve Panik uyarıları"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendRiskNotification(title: String, content: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < 10000) return
        lastNotificationTime = currentTime

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        with(NotificationManagerCompat.from(this)) {
            notify(101, builder.build())
        }
    }

    // ------------------------------------------------
    // VERİ AYRIŞTIRMA (SMS ENTEGRELİ)
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
                            sendRiskNotification("RİSKLİ NABIZ!", "Nabız: $val1 bpm")
                            // Kritik nabızda SMS gönder
                            sendEmergencySMS("UYARI: Kişinin nabzı riskli seviyeye ulaştı: $val1 bpm.")
                        }
                        "WARNING_FALL" -> {
                            heartRateViewModel.updateFallTimer(val1)
                        }
                        "ALARM_FALL" -> {
                            heartRateViewModel.updateStatus(TrackingStatus.ALARM_FALL)
                            heartRateViewModel.updateMetrics(bpm = val1, spo2Val = val2)
                            sendRiskNotification("DÜŞME TESPİT EDİLDİ!", "Acil durum!")
                            // Düşme durumunda SMS gönder
                            sendEmergencySMS("ACİL DURUM: Düşme tespit edildi! Kişi hareketsiz, lütfen acil müdahale edin.")
                        }
                        "PANIC" -> {
                            heartRateViewModel.triggerPanic()
                            sendRiskNotification("ACİL YARDIM BUTONU!", "Kullanıcı panik butonuna bastı!")
                            // Panik butonuna basıldığında SMS gönder
                            sendEmergencySMS("ACİL YARDIM: Kullanıcı panik butonuna bastı! Yardım gerekiyor.")
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
    // İZİN KONTROLLERİ (SMS EKLENDİ)
    // ------------------------------------------------
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // SMS Gönderme İzni
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 1)
        }
    }

    // ------------------------------------------------
    // BLUETOOTH İŞLEMLERİ
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
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                Log.d("BT_DEBUG", "Adapter alındı")

                val device = adapter.bondedDevices.firstOrNull { it.name == "Yasli_Takip_Sistemi" }
                if (device == null) {
                    Log.e("BT_DEBUG", "Cihaz bulunamadı! Eşleşmiş isimleri kontrol et.")
                    return@Thread
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                isConnected = true
                Log.d("BT_DEBUG", "Bağlantı başarılı!")

                val reader = BufferedReader(InputStreamReader(bluetoothSocket!!.inputStream))
                while (isConnected) {
                    val rawMsg = reader.readLine()
                    Log.d("BT_DEBUG", "Gelen Veri: $rawMsg") // Veri geliyor mu burada görünüyor
                    if (!rawMsg.isNullOrEmpty()) parseArduinoData(rawMsg.trim())
                }
            } catch (e: Exception) {
                Log.e("BT_DEBUG", "Hata oluştu: ${e.message}")
                isConnected = false
            }
        }.start()
    }
}