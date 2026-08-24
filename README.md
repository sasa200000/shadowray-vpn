<div align="center">

# 🛡️ ShadowRay VPN

**A full-featured Android VPN & proxy client built with Kotlin + Jetpack Compose**

Supports VLESS · VMess · Trojan · Shadowsocks · WireGuard

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/compose)

</div>

---

## ✨ Features

- 🔌 **Multiple protocols** — VLESS, VMess, Trojan, Shadowsocks, WireGuard
- 🔗 **Config import** — paste links or scan QR codes
- 📡 **Subscriptions** — auto-refresh server lists from subscription URLs
- 🏓 **Ping testing** — measure latency of each config
- 📱 **Split tunneling** — choose which apps use the VPN
- 📊 **Traffic monitoring** — live speed graph and stats
- 📝 **Connection logs**

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest version recommended)
- Android 7.0+ device or emulator (minSdk 24)

### Build & Run

1. Clone this repository:
   ```bash
   git clone https://github.com/haha90000/shadowray-vpn.git
   ```
2. Open the project in Android Studio (**File → Open**)
3. If your build uses the Gemini API, create a `.env` file in the project root:
   ```
   GEMINI_API_KEY=your_key_here
   ```
   (see `.env.example`)
4. Run the app on an emulator or physical device ▶️

> 💡 **Note:** If you hit a signing error during release builds, remove this line
> from `app/build.gradle.kts`: `signingConfig = signingConfigs.getByName("debugConfig")`

## 🧱 Project Structure

```
app/src/main/java/com/example/
├── data/          # Room database, repositories, preferences
├── model/         # Data models (ProxyConfig, VpnState, ...)
├── parser/        # Config parsing, ping tester, QR utils
├── ui/            # Compose screens, components, theme
├── viewmodel/     # VpnViewModel
├── vpn/           # VpnManager + LocalVpnService
└── util/          # Helpers (formatters, localization, installed apps)
```

## 📦 Download APK

Grab the latest APK from the [Releases](../../releases) page, or build it yourself
with `./gradlew assembleDebug`.

## 📄 License

This project was generated with [Google AI Studio](https://aistudio.google.com).
