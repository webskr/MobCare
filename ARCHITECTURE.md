# 🛡️ MobCare - Clean Architecture & System Design

MobCare is structured following **Enterprise Android Clean Architecture** (similar to Feature/Domain driven patterns in React/React Native), separating Concerns into Core, Engine, Data, Domain, and Presentation layers.

---

## 📁 Clean Architecture Folder Layout

`	ext
D:\app\MobCare\
│
├── ARCHITECTURE.md                          <- System Design & Module Guide
├── README.md                                <- Public Project Documentation
├── build.gradle.kts                         <- Root build settings
├── settings.gradle.kts                      <- Gradle module settings
│
└── app/
    ├── build.gradle.kts                     <- Dependencies & build targets
    └── src/main/
        ├── AndroidManifest.xml              <- System declarations & permissions
        │
        ├── java/com/mobcare/
        │   │
        │   ├── core/                        <- Foundation & Utilities (Like React utils/ & hooks/)
        │   │   ├── constants/               <- AppConstants.kt, SecurityConfig.kt
        │   │   ├── extensions/              <- ViewExtensions.kt, ContextExt.kt
        │   │   ├── permissions/             <- PermissionManager.kt (Camera, Storage, Overlay)
        │   │   └── haptics/                 <- KeyboardHapticFeedback.kt (Vibration/Click sounds)
        │   │
        │   ├── engine/                      <- Background Services & System Hardware Engines
        │   │   ├── ime/                     <- Keyboard Engine (Phase 1: Hardest)
        │   │   │   ├── lifecycle/           <- MobCareKeyboardService.kt (InputMethodService)
        │   │   │   ├── layout/              <- QwertyLayout.kt, SymbolsLayout.kt, KeyModel.kt
        │   │   │   ├── touch/               <- KeyGestureDetector.kt, PopupPreview.kt
        │   │   │   └── suggestions/         <- Autocorrect / SuggestionBar.kt
        │   │   │
        │   │   ├── camera/                  <- Camera Engine
        │   │   │   ├── CameraXManager.kt    <- Silent snapshot controller
        │   │   │   └── FrameAnalyzer.kt     <- Face frame validator
        │   │   │
        │   │   ├── security/                <- Intruder & Disarm Engine (Phase 2)
        │   │   │   ├── DisarmStateMachine.kt<- 20s secret handshake timer logic
        │   │   │   ├── KnockDetector.kt     <- Volume keys / Secret gesture listener
        │   │   │   └── DeviceAdminService.kt<- Screen unlock & password failure events
        │   │   │
        │   │   └── observers/               <- Media & Download Watchers (Phase 3)
        │   │       ├── MediaContentObserver.kt <- Photos captured/deleted watcher
        │   │       └── DownloadWatcher.kt   <- APK & browser download monitor
        │   │
        │   ├── data/                        <- Storage & Network (Like React store/ & pi/)
        │   │   ├── local/
        │   │   │   ├── database/            <- AppDatabase.kt (Room)
        │   │   │   ├── dao/                 <- AuditLogDao.kt, IntruderCaptureDao.kt
        │   │   │   └── entities/            <- AuditEventEntity.kt, CaptureEntity.kt
        │   │   ├── crypto/                  <- SecurityCipher.kt (SQLCipher / AES-GCM)
        │   │   └── repository/              <- AuditRepositoryImpl.kt
        │   │
        │   ├── domain/                      <- Pure Business Logic (Like React 	ypes/ & UseCases)
        │   │   ├── models/                  <- AuditLog.kt, IntruderPhoto.kt, KeyStroke.kt
        │   │   ├── repository/              <- IAuditRepository.kt (Interface)
        │   │   └── usecases/                <- LogKeyStrokeUseCase.kt, TriggerTrapUseCase.kt
        │   │
        │   └── presentation/                <- UI Layer (Like React components/ & screens/)
        │       ├── common/                  <- Reusable UI Widgets (Buttons, Cards, Dialogs)
        │       ├── keyboard/                <- Custom Keyboard UI Elements
        │       │   ├── components/          <- KeyView.kt, SpaceBarView.kt, PopupBubbleView.kt
        │       │   └── theme/               <- KeyboardThemes.kt (Dark, Light, AMOLED)
        │       ├── screens/                 <- Full Application Screens (Pages)
        │       │   ├── dashboard/           <- DashboardActivity.kt (Main stats)
        │       │   ├── logs/                <- LogsActivity.kt (WhatsApp/Chrome logs list)
        │       │   ├── decoy/               <- DecoyCaptchaActivity.kt (Honeypot Trap screen)
        │       │   └── settings/            <- SettingsActivity.kt (Feature toggles ON/OFF)
        │       └── theme/                   <- Color.kt, Typography.kt, Shape.kt
        │
        └── res/
            ├── layout/                      <- XML Layout definitions
            ├── xml/                         <- System configs (method.xml, device_admin.xml)
            └── values/                      <- strings.xml, colors.xml
`

---

## 🎯 Layer Responsibility & Communication Flow

1. **User Touches Keyboard Key:**
   KeyView (Presentation) ➔ KeyGestureDetector (Engine) ➔ MobCareKeyboardService (InputConnection) ➔ LogKeyStrokeUseCase (Domain) ➔ AuditRepositoryImpl (Data) ➔ Room DB (Local Storage).

2. **Intruder Trap Flow:**
   Device Unlocked ➔ DisarmStateMachine starts countdown ➔ If handshake not performed ➔ DecoyCaptchaActivity presented ➔ Touch interaction triggers CameraXManager ➔ Face snapshot saved to encrypted internal storage.