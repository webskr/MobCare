<div align="center">

# 🛡️ MobCare

**Enterprise-Grade On-Device Security Sentinel, Tactical App Locker & Telemetry IME for Android**

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%201.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)
[![Security: 100% On--Device](https://img.shields.io/badge/Security-100%25%20On--Device-success?style=for-the-badge&logo=shield)](SECURITY.md)
[![Privacy: Zero Trackers](https://img.shields.io/badge/Privacy-Zero%20Third--Party%20Trackers-red?style=for-the-badge)](SECURITY.md)

<p align="center">
  <b>MobCare</b> is a high-performance, privacy-first Android security sentinel designed for personal device protection, physical tamper defense, honeypot intruder traps, and tactical application locking.
</p>

</div>

---

## 🌟 Core Feature Highlights

### 1. 🛡️ Tactical App Locker Sentinel
- **Dual Lock Strategy:**
  - **Always Protected:** Seamless PIN gate interception whenever selected applications are opened.
  - **Intruder Trigger:** Honeypot mode — leaves apps unlocked until unauthorized tampering or failed verification occurs, then automatically seals designated sensitive apps.
- **Dedicated PIN Gate:** Supports either the universal Master PIN or an app-locker dedicated 4–6 digit security PIN.
- **Instant Session Pass:** Once verified, grants immediate single-session access without repeated friction.

### 2. ⌨️ Native Telemetry Keyboard (IME)
- Built completely from scratch on the native Android `InputMethodService` framework.
- **Dynamic 3-Tier Layout System:**
  - **QWERTY (Alpha):** Fluid typing with dedicated emoji drawer and voice integration.
  - **Symbols (`?123`):** Instant symbol tray with direct one-tap shortcut `[ 1234 ]` into numeric mode.
  - **Dedicated 3x4 Numpad:** High-speed calculation & PIN entry layout with instant `[ ABC ]` return navigation.
- **Per-App Context Intelligence:** Identifies active target packages and switches automatically into numeric mode for password and PIN fields.
- **Zero Cloud Leakage:** All keystroke metrics stay strictly sandboxed in on-device volatile storage.

### 3. 📞 Call & SMS Vault
- **Anti-Deletion Telecommunications Ledger:** Automatically captures call logs and SMS metadata before malicious actors or physical snoops can delete them.
- **Full Call Intelligence:** Logs incoming, outgoing, missed calls, exact durations, and contact names.
- **SMS Ledger:** Tracks sender, timestamp, and message snippets directly into a protected encrypted repository.

### 4. 🕵️ Stealth Cloak & Anti-Tamper Engine
- **Anti-Uninstall Device Administrator:** Prevents unauthorized uninstallation from Android settings.
- **Decoy Crash Screen:** Triggers a fake *"App has stopped"* system error dialog to confuse unauthorized intruders attempting to inspect MobCare.
- **Diagnostics Camouflage:** Conceals MobCare behind a legitimate-looking device battery & RAM benchmark cloak.
- **Front-Camera Intruder Capture:** Automatically snaps a silent front-facing photo upon failed unlock attempts.

### 5. 📸 Silent Sentinel & Media Vault
- **Silent Screen Capture:** Takes discreet background screenshots upon trigger events.
- **Encrypted Media Gallery:** High-performance 3-column media grid with full-screen pinch-to-zoom inspector for captured intruder photos.

---

## 🏛️ System Architecture

```
                          ┌────────────────────────────────┐
                          │         User Interface         │
                          │   Material 3 Dark Charcoal UI  │
                          └───────────────┬────────────────┘
                                          │
            ┌─────────────────────────────┼─────────────────────────────┐
            │                             │                             │
            ▼                             ▼                             ▼
┌───────────────────────┐   ┌───────────────────────────┐   ┌───────────────────────┐
│  App Locker Sentinel  │   │  MobCare Keyboard (IME)   │   │  Call & SMS Vault     │
│  Foreground Monitor   │   │  Native InputMethodService│   │  ContentObserver Sync │
└───────────┬───────────┘   └─────────────┬─────────────┘   └───────────┬───────────┘
            │                             │                             │
            └─────────────────────────────┼─────────────────────────────┘
                                          │
                                          ▼
                         ┌─────────────────────────────────┐
                         │      Local Storage Engine       │
                         │  Jetpack Room + Encrypted Vault │
                         │      (100% On-Device Only)      │
                         └─────────────────────────────────┘
```

---

## 🎨 Design Philosophy: Material 3 Charcoal & Emerald

MobCare adheres to a clean, non-glaring dark aesthetic tailored for professional security tools:
- **Base Surface:** Dark Charcoal (`#121316`) & Elevated Cards (`#1E1F24`)
- **Primary Accent:** Soothing Material Emerald Green (`#81C995`)
- **Secondary Actions:** Deep Muted Slate (`#26272E`)
- **Destructive/Emergency:** Soft Crimson (`#2D1A1A` / `#F28B82`)
- **Centralized Colors:** All button states are unified in `app/src/main/res/values/button_colors.xml`.

---

## 🚀 Building & Running from Source

MobCare can be compiled directly via Gradle without requiring Android Studio:

### Prerequisites:
- **JDK 17** or higher
- **Android SDK** (API 34 / Platform Tools)

### Build Debug APK:
```bash
# Clone the repository
git clone https://github.com/your-username/MobCare.git
cd MobCare

# Verify and compile all Kotlin sources
./gradlew compileDebugSources

# Assemble Debug APK
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install directly to connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Security & Privacy Guarantee

MobCare is built on an uncompromising privacy foundation:
1. **Zero External Servers:** MobCare has no third-party telemetry, analytics, crash-reporters, or advertising SDKs.
2. **Local Cryptography:** Sensitive audit logs and captured media remain encrypted inside the app's protected sandbox.
3. **Transparent Source Code:** Full codebase is publicly auditable under the GPLv3 license.

For security vulnerability disclosure, see [SECURITY.md](SECURITY.md).

---

## 🤝 Contributing

We welcome community contributions! Please review our [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on our code style, pull request lifecycle, and development setup.

---

## 📄 License

MobCare is licensed under the **GNU General Public License v3.0 (GPLv3)**.  
See the [LICENSE](LICENSE) file for complete terms and legal permissions.

```
MobCare - System Telemetry & Device Protection Sentinel
Copyright (C) 2026 MobCare Project Contributors
```