package com.erdem.tdp.design

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("YasliTakipPrefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("USER_NAME", "Kullanıcı") ?: "Kullanıcı"
        set(value) = prefs.edit().putString("USER_NAME", value).apply()

    // Email yerine Acil Durum Telefon Numarası
    var emergencyPhone: String
        get() = prefs.getString("EMERGENCY_PHONE", "") ?: ""
        set(value) = prefs.edit().putString("EMERGENCY_PHONE", value).apply()

    var highBpmLimit: String
        get() = prefs.getString("HIGH_BPM", "120") ?: "120"
        set(value) = prefs.edit().putString("HIGH_BPM", value).apply()

    var lowBpmLimit: String
        get() = prefs.getString("LOW_BPM", "45") ?: "45"
        set(value) = prefs.edit().putString("LOW_BPM", value).apply()
}