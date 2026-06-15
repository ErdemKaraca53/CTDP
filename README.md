<div align="center">

# 🩺 ÇTDP

### Akıllı Giyilebilir Sağlık Takip Uygulaması

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)

[![minSdk](https://img.shields.io/badge/minSdk-24-blue?style=flat-square)]()
[![targetSdk](https://img.shields.io/badge/targetSdk-36-blue?style=flat-square)]()
[![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)]()

</div>

---

ÇTDP, Bluetooth üzerinden bağlanılan bir giyilebilir sensörden alınan **nabız (BPM)** ve **oksijen (SpO2)** verilerini takip eden, **düşme algılama** ve **acil durum (panik) bildirimi** sağlayan bir Android uygulamasıdır.

## ✨ Özellikler

| Özellik | Açıklama |
|---|---|
| ❤️ **Nabız & SpO2 Takibi** | Sensörden gelen anlık nabız ve oksijen seviyesi verilerini gösterir |
| 📊 **Sistem Durumu İzleme** | `IDLE`, `NORMAL`, `PULSE_ALERT`, `WARNING_FALL`, `ALARM_FALL`, `PANIC` durumlarını yönetir |
| 🚨 **Düşme Algılama** | Düşme şüphesinde geri sayım başlatır, süre dolarsa alarma geçer |
| 🆘 **Panik Butonu** | Acil durumda manuel olarak panik modu tetiklenebilir |
| 💬 **SMS Bildirimi** | Acil durumlarda otomatik SMS gönderir |
| 🔔 **Bildirimler** | Sistem bildirimleri ile kullanıcı uyarılır |

## 🚦 Sistem Durumları

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> NORMAL
    NORMAL --> PULSE_ALERT : Anormal nabız
    NORMAL --> WARNING_FALL : Düşme şüphesi
    WARNING_FALL --> ALARM_FALL : Geri sayım bitti
    WARNING_FALL --> NORMAL : İptal edildi
    NORMAL --> PANIC : Panik butonu
    PULSE_ALERT --> NORMAL : Stabilize oldu
    ALARM_FALL --> NORMAL : Onaylandı
```

## 🛠️ Teknolojiler

- **Kotlin**
- **Jetpack Compose** (Material 3)
- **Android ViewModel & StateFlow**
- **Bluetooth** (Classic/BLE) izinleri

## 📁 Proje Yapısı

```
app/src/main/java/com/erdem/tdp/
├── MainActivity.kt
├── data/
│   └── HeartRateViewModel.kt   # Nabız, SpO2 ve sistem durumu yönetimi
├── design/
│   ├── DashboardScreen.kt      # Ana ekran
│   ├── SettingsScreen.kt       # Ayarlar ekranı
│   ├── StatusCard.kt           # Durum kartı bileşeni
│   └── UserPreferences.kt      # Kullanıcı tercihleri
└── ui/theme/                   # Compose tema dosyaları
```

## ⚙️ Gereksinimler

- Android Studio
- minSdk 24, targetSdk/compileSdk 36

## 🔐 İzinler

| İzin | Amaç |
|---|---|
| `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` | Sensör ile kablosuz iletişim |
| `POST_NOTIFICATIONS` | Durum bildirimleri |
| `SEND_SMS` | Acil durum SMS gönderimi |

## 🚀 Kurulum ve Çalıştırma

1. Projeyi Android Studio ile açın.
2. Gradle senkronizasyonunun tamamlanmasını bekleyin.
3. Bir cihaz veya emülatörde `app` modülünü çalıştırın.

```bash
./gradlew assembleDebug
```

---

<div align="center">
Made with ❤️ using Jetpack Compose
</div>
