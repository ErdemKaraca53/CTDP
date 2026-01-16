package com.erdem.tdp.design

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Sistemin olası durumlarını tanımlıyoruz
enum class TrackingStatus {
    IDLE,           // Sensör boşta/beklemede
    NORMAL,         // Her şey yolunda
    PULSE_ALERT,    // Nabız çok yüksek/düşük (Risk)
    WARNING_FALL,   // Düşme şüphesi (Geri sayım)
    ALARM_FALL,     // DÜŞTÜ (Kritik)
    PANIC           // Butona basıldı (Acil)
}

class HeartRateViewModel : ViewModel() {

    // 1. Nabız Verisi
    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    // 2. Oksijen (SpO2) Verisi (Varsayılan 98)
    private val _spo2 = MutableStateFlow(98)
    val spo2: StateFlow<Int> = _spo2.asStateFlow()

    // 3. Sistem Durumu (Normal, Panik, Düşme vs.)
    private val _systemStatus = MutableStateFlow(TrackingStatus.IDLE)
    val systemStatus: StateFlow<TrackingStatus> = _systemStatus.asStateFlow()

    // 4. Düşme Geri Sayım Sayacı
    private val _fallCountdown = MutableStateFlow(0)
    val fallCountdown: StateFlow<Int> = _fallCountdown.asStateFlow()

    // --- GÜNCELLEME FONKSİYONLARI ---

    // Sadece nabız ve oksijeni güncellemek için
    fun updateMetrics(bpm: Int, spo2Val: Int = 98) {
        _heartRate.value = bpm
        _spo2.value = spo2Val
    }

    // Sistemin genel durumunu değiştirmek için
    fun updateStatus(status: TrackingStatus) {
        _systemStatus.value = status
    }

    // Düşme geri sayımını güncellemek için
    fun updateFallTimer(seconds: Int) {
        _fallCountdown.value = seconds
        // Sayaç çalışıyorsa durumu otomatik WARNING_FALL yap
        if (_systemStatus.value != TrackingStatus.WARNING_FALL) {
            _systemStatus.value = TrackingStatus.WARNING_FALL
        }
    }

    // Panik durumunu tetikle
    fun triggerPanic() {
        _systemStatus.value = TrackingStatus.PANIC
    }
}