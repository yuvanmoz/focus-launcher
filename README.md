# 🎯 Focus Launcher

> A minimalist, distraction-free Android launcher designed to reclaim your attention and time.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg?logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Modular%20Clean%20Arch-brightgreen)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 💡 Overview

Smartphones are intentionally engineered to steal attention through colorful icons, notification badges, and algorithmic feeds. **Focus Launcher** strips away the digital clutter, replacing addictive visual hooks with a clean, intentional interface that puts you back in control of your device.

---

## ✨ Key Features

- **🧘 Minimalist Home Screen:** Text-first and clean icon interfaces to eliminate impulsive app opening.
- **⚡ Focus Profiles:** Create custom contextual modes (e.g., *Deep Work*, *Reading*, *Personal*, *Sleep*) with granular app whitelists.
- **🛡️ Active App Blocker:** Dedicated `FocusBlockerService` intercepts addictive apps when a focus profile is active.
- **🔕 Notification Filter:** `FocusNotificationListenerService` silences non-critical pings while you are in deep work.
- **📱 Clean Widget Dashboard:** Swipe over to customized, clutter-free utility widgets without opening distracting apps.
- **💨 Gesture-First Navigation:** Smooth overscroll physics and swift swipe gestures to launch apps and switch profiles.
- **🔋 AMOLED Battery Friendly:** True black backgrounds designed to maximize OLED battery longevity and reduce eye strain.

---

## 🏗 Architecture & Modules

The project is built following **Modern Android Architecture** and modular clean architecture principles:

```text
focus-launcher/
├── app/                  # Application entry point, launcher activity, and background services
│   ├── FocusBlockerService.kt
│   ├── FocusNotificationListenerService.kt
│   └── MainActivity.kt
├── core/
│   ├── common/           # Shared utilities, permissions, themes, and UI modifiers
│   ├── data/             # Repositories for apps, profiles, and widget states
│   ├── database/         # Local Room database (FocusDatabase, entities, DAOs)
│   └── model/            # Domain models (FocusProfile, AppMetadata, HomeScreenItem)
└── feature/
    └── launcher/         # Jetpack Compose UI (HomeScreen, ProfileSelector, WidgetPage)
```

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose & Material 3
- **Local Persistence:** Room Database (SQLite)
- **Concurrency & Streams:** Kotlin Coroutines & StateFlow
- **System Services:** Android UsageStatsManager, NotificationListenerService, and Accessibility APIs

---

## 🚀 Building & Running

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API 34+

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yuvanmoz/focus-launcher.git
   cd focus-launcher
   ```

2. **Open the project in Android Studio.**

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install and set as default launcher:**
   Install onto your Android device and navigate to **Settings > Apps > Default Apps > Home app** and select **Focus Launcher**.

---

## 📄 License

This project is licensed under the Apache 2.0 License.
