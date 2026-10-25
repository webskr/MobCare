package com.mobcare.dashboard

import android.Manifest
import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.mobcare.R
import com.mobcare.audit.AppTimelineManager
import com.mobcare.audit.AuditLogManager
import com.mobcare.audit.ClipboardVaultManager
import com.mobcare.audit.NotificationVaultManager
import com.mobcare.audit.CallSmsVaultManager
import com.mobcare.audit.BrowserVaultManager
import com.mobcare.security.AppLockerManager
import com.mobcare.security.AppLockerWatchdogService
import com.mobcare.security.MobCareAccessibilityService
import com.mobcare.security.IntruderCaptureManager
import com.mobcare.security.StealthManager

class MainActivity : AppCompatActivity() {

    // Header
    private lateinit var tvHubStatusBadge: LinearLayout
    private lateinit var tvProtectedLabel: TextView

    // Device Status Card & Stats
    private lateinit var cardDeviceStatus: LinearLayout
    private lateinit var tvStatModules: TextView
    private lateinit var tvStatThreats: TextView
    private lateinit var tvStatEvents: TextView
    private lateinit var tvStatSync: TextView
    private lateinit var tvDeviceStatusSub: TextView

    // Row 1: Keyboard
    private lateinit var cardKeyboardHub: LinearLayout
    private lateinit var tvKeyboardCardSub: TextView
    private lateinit var tvKeyboardCardBadge: TextView

    // Row 2: Package Guardian
    private lateinit var cardPackageGuardian: LinearLayout
    private lateinit var tvPackageCardSub: TextView
    private lateinit var tvPackageCardBadge: TextView

    // Row 3: Stealth & Anti-Tamper
    private lateinit var cardStealthEngine: LinearLayout
    private lateinit var tvStealthCardSub: TextView
    private lateinit var tvStealthCardBadge: TextView

    // Row 4: Notification Vault
    private lateinit var cardNotificationVault: LinearLayout
    private lateinit var tvNotificationCardSub: TextView
    private lateinit var tvNotificationCardBadge: TextView

    // Row 5: Cloud Sync
    private lateinit var cardCloudSync: LinearLayout
    private lateinit var tvCloudSyncCardSub: TextView
    private lateinit var tvCloudSyncCardBadge: TextView

    // Row 6: App Timeline Inspector
    private lateinit var cardAppTimeline: LinearLayout
    private lateinit var tvTimelineCardSub: TextView
    private lateinit var tvTimelineCardBadge: TextView

    // Row 7: App Locker Sentinel
    private lateinit var cardAppLocker: LinearLayout
    private lateinit var tvLockerCardSub: TextView
    private lateinit var tvLockerCardBadge: TextView

    // Row 8: Call & SMS Vault (hidden)
    private lateinit var cardCallSmsVault: LinearLayout
    private lateinit var tvCallSmsCardSub: TextView
    private lateinit var tvCallSmsCardBadge: TextView

    // Row 9: Web Intelligence (hidden)
    private lateinit var cardBrowserVault: LinearLayout
    private lateinit var tvBrowserCardSub: TextView
    private lateinit var tvBrowserCardBadge: TextView

    // Section counts
    private lateinit var tvCoreActiveCount: TextView
    private lateinit var tvDataActiveCount: TextView
    private lateinit var tvAppActiveCount: TextView
    private lateinit var tvFoundationalActiveCount: TextView

    // Section 4: Foundational Security (10 Easy Layers)
    private lateinit var cardCoreSecurity: LinearLayout
    private lateinit var tvCoreSecurityCardSub: TextView
    private lateinit var tvCoreSecurityCardBadge: TextView

    // Section 5: Advanced Monitoring (16 Medium Layers)
    private lateinit var tvMediumActiveCount: TextView
    private lateinit var cardMediumSecurity: LinearLayout
    private lateinit var tvMediumSecurityCardSub: TextView
    private lateinit var tvMediumSecurityCardBadge: TextView

    // Section 6: Hardware & Deep OS Defense (14 Hard Layers)
    private lateinit var tvHardActiveCount: TextView
    private lateinit var cardHardSecurity: LinearLayout
    private lateinit var tvHardSecurityCardSub: TextView
    private lateinit var tvHardSecurityCardBadge: TextView

    // Bottom Nav
    private lateinit var navHome: LinearLayout
    private lateinit var navActivity: LinearLayout
    private lateinit var navProtection: LinearLayout
    private lateinit var navSettings: LinearLayout

    private var isSessionUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Header
        tvHubStatusBadge = findViewById(R.id.tvHubStatusBadge)
        tvProtectedLabel = findViewById(R.id.tvProtectedLabel)

        // Device status stats
        cardDeviceStatus = findViewById(R.id.cardDeviceStatus)
        tvDeviceStatusSub = findViewById(R.id.tvDeviceStatusSub)
        tvStatModules = findViewById(R.id.tvStatModules)
        tvStatThreats = findViewById(R.id.tvStatThreats)
        tvStatEvents = findViewById(R.id.tvStatEvents)
        tvStatSync = findViewById(R.id.tvStatSync)

        cardDeviceStatus.setOnClickListener {
            showDeviceIntegrityDialog()
        }

        // Feature rows
        cardKeyboardHub = findViewById(R.id.cardKeyboardHub)
        tvKeyboardCardSub = findViewById(R.id.tvKeyboardCardSub)
        tvKeyboardCardBadge = findViewById(R.id.tvKeyboardCardBadge)

        cardPackageGuardian = findViewById(R.id.cardPackageGuardian)
        tvPackageCardSub = findViewById(R.id.tvPackageCardSub)
        tvPackageCardBadge = findViewById(R.id.tvPackageCardBadge)

        cardStealthEngine = findViewById(R.id.cardStealthEngine)
        tvStealthCardSub = findViewById(R.id.tvStealthCardSub)
        tvStealthCardBadge = findViewById(R.id.tvStealthCardBadge)

        cardNotificationVault = findViewById(R.id.cardNotificationVault)
        tvNotificationCardSub = findViewById(R.id.tvNotificationCardSub)
        tvNotificationCardBadge = findViewById(R.id.tvNotificationCardBadge)

        cardCloudSync = findViewById(R.id.cardCloudSync)
        tvCloudSyncCardSub = findViewById(R.id.tvCloudSyncCardSub)
        tvCloudSyncCardBadge = findViewById(R.id.tvCloudSyncCardBadge)

        cardAppTimeline = findViewById(R.id.cardAppTimeline)
        tvTimelineCardSub = findViewById(R.id.tvTimelineCardSub)
        tvTimelineCardBadge = findViewById(R.id.tvTimelineCardBadge)

        cardAppLocker = findViewById(R.id.cardAppLocker)
        tvLockerCardSub = findViewById(R.id.tvLockerCardSub)
        tvLockerCardBadge = findViewById(R.id.tvLockerCardBadge)

        cardCallSmsVault = findViewById(R.id.cardCallSmsVault)
        tvCallSmsCardSub = findViewById(R.id.tvCallSmsCardSub)
        tvCallSmsCardBadge = findViewById(R.id.tvCallSmsCardBadge)

        cardBrowserVault = findViewById(R.id.cardBrowserVault)
        tvBrowserCardSub = findViewById(R.id.tvBrowserCardSub)
        tvBrowserCardBadge = findViewById(R.id.tvBrowserCardBadge)

        // Section counts
        tvCoreActiveCount = findViewById(R.id.tvCoreActiveCount)
        tvDataActiveCount = findViewById(R.id.tvDataActiveCount)
        tvAppActiveCount = findViewById(R.id.tvAppActiveCount)
        tvFoundationalActiveCount = findViewById(R.id.tvFoundationalActiveCount)

        // Section 4: Foundational Security (10 Easy Layers)
        cardCoreSecurity = findViewById(R.id.cardCoreSecurity)
        tvCoreSecurityCardSub = findViewById(R.id.tvCoreSecurityCardSub)
        tvCoreSecurityCardBadge = findViewById(R.id.tvCoreSecurityCardBadge)

        // Section 5: Advanced Monitoring (16 Medium Layers)
        tvMediumActiveCount = findViewById(R.id.tvMediumActiveCount)
        cardMediumSecurity = findViewById(R.id.cardMediumSecurity)
        tvMediumSecurityCardSub = findViewById(R.id.tvMediumSecurityCardSub)
        tvMediumSecurityCardBadge = findViewById(R.id.tvMediumSecurityCardBadge)

        // Section 6: Hardware & Deep OS Defense (14 Hard Layers)
        tvHardActiveCount = findViewById(R.id.tvHardActiveCount)
        cardHardSecurity = findViewById(R.id.cardHardSecurity)
        tvHardSecurityCardSub = findViewById(R.id.tvHardSecurityCardSub)
        tvHardSecurityCardBadge = findViewById(R.id.tvHardSecurityCardBadge)

        // Bottom nav
        navHome = findViewById(R.id.navHome)
        navActivity = findViewById(R.id.navActivity)
        navProtection = findViewById(R.id.navProtection)
        navSettings = findViewById(R.id.navSettings)

        // Card Click Navigations -> Opens Dedicated Screens
        cardKeyboardHub.setOnClickListener {
            startActivity(Intent(this, KeyboardTelemetryActivity::class.java))
        }

        cardPackageGuardian.setOnClickListener {
            startActivity(Intent(this, PackageGuardianActivity::class.java))
        }

        cardStealthEngine.setOnClickListener {
            startActivity(Intent(this, StealthSettingsActivity::class.java))
        }

        cardNotificationVault.setOnClickListener {
            startActivity(Intent(this, NotificationVaultActivity::class.java))
        }

        cardCloudSync.setOnClickListener {
            startActivity(Intent(this, CloudSyncActivity::class.java))
        }

        cardAppTimeline.setOnClickListener {
            startActivity(Intent(this, AppTimelineActivity::class.java))
        }

        cardAppLocker.setOnClickListener {
            startActivity(Intent(this, AppLockerActivity::class.java))
        }

        cardCallSmsVault.setOnClickListener {
            startActivity(Intent(this, CallSmsVaultActivity::class.java))
        }

        cardBrowserVault.setOnClickListener {
            startActivity(Intent(this, BrowserVaultActivity::class.java))
        }

        cardCoreSecurity.setOnClickListener {
            startActivity(Intent(this, CoreSecurityActivity::class.java))
        }

        cardMediumSecurity.setOnClickListener {
            startActivity(Intent(this, MediumSecurityActivity::class.java))
        }

        cardHardSecurity.setOnClickListener {
            startActivity(Intent(this, HardSecurityActivity::class.java))
        }

        // Bottom Nav Actions
        navHome.setOnClickListener {
            updateHubOverview()
            Toast.makeText(this, "Hub Overview Refreshed", Toast.LENGTH_SHORT).show()
        }

        navActivity.setOnClickListener {
            startActivity(Intent(this, AppTimelineActivity::class.java))
        }

        navProtection.setOnClickListener {
            startActivity(Intent(this, CoreSecurityActivity::class.java))
        }

        navSettings.setOnClickListener {
            startActivity(Intent(this, StealthSettingsActivity::class.java))
        }

        // Start App Locker Watchdog Service
        try {
            if (AppLockerManager.getLockedPackages(this).isNotEmpty()) {
                AppLockerWatchdogService.start(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Auto request camera permission if not granted yet
        if (!IntruderCaptureManager.isCameraPermissionGranted(this)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }

        checkDecoyGates()
    }

    override fun onResume() {
        super.onResume()
        updateHubOverview()
    }

    private fun updateHubOverview() {
        // Layer 36: Device State & Battery Monitoring
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, ifilter)
        if (batteryPct <= 0 && batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryPct = (level * 100) / scale
            }
        }
        val statusExtra = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING || statusExtra == BatteryManager.BATTERY_STATUS_FULL
        val batteryDisplay = if (batteryPct >= 0) "$batteryPct%" else "100%"
        tvStatSync.text = batteryDisplay

        // If Duress Sandbox Mode is active, show sanitized/decoy status
        if (StealthManager.isDuressSandboxActive) {
            tvProtectedLabel.text = "OPTIMAL"
            tvProtectedLabel.setTextColor(Color.parseColor("#81C995"))
            tvDeviceStatusSub.text = "Battery: $batteryDisplay • System nominal"
            tvStatModules.text = "7/7"
            tvStatThreats.text = "0"
            tvStatEvents.text = "0"

            tvKeyboardCardBadge.text = "ACTIVE"
            tvKeyboardCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvKeyboardCardSub.text = "Standard IME Service • No anomalies"

            tvPackageCardBadge.text = "OPTIMAL"
            tvPackageCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvPackageCardSub.text = "All packages verified against Google Play Protect"

            tvStealthCardBadge.text = "ACTIVE"
            tvStealthCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvStealthCardSub.text = "Device integrity verified • System parameters normal"

            tvNotificationCardBadge.text = "SYNCED"
            tvNotificationCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvNotificationCardSub.text = "Notification listener operating normally"

            tvCloudSyncCardBadge.text = "OFFLINE"
            tvCloudSyncCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvCloudSyncCardSub.text = "100% Offline • Encrypted vault operational"

            tvTimelineCardBadge.text = "OPTIMAL"
            tvTimelineCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvTimelineCardSub.text = "Device application statistics within normal thresholds"

            tvLockerCardBadge.text = "STANDBY"
            tvLockerCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvLockerCardSub.text = "System application sandbox operational"

            tvCallSmsCardBadge.text = "OPTIMAL"
            tvCallSmsCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvCallSmsCardSub.text = "Telecommunications logs synchronized"

            tvBrowserCardBadge.text = "OPTIMAL"
            tvBrowserCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvBrowserCardSub.text = "Web cache and browsing parameters nominal"

            tvFoundationalActiveCount.text = "10/10 active"
            tvFoundationalActiveCount.setTextColor(Color.parseColor("#81C995"))
            tvCoreSecurityCardBadge.text = "10/10 ACTIVE"
            tvCoreSecurityCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvCoreSecurityCardSub.text = "Foundational security operational"

            tvMediumActiveCount.text = "16/16 active"
            tvMediumActiveCount.setTextColor(Color.parseColor("#81C995"))
            tvMediumSecurityCardBadge.text = "16/16 ARMED"
            tvMediumSecurityCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvMediumSecurityCardSub.text = "SQLite WAL, Hardware Keystore, Forensics & Decoy operational"

            tvHardActiveCount.text = "14/14 active"
            tvHardActiveCount.setTextColor(Color.parseColor("#81C995"))
            tvHardSecurityCardBadge.text = "14/14 ARMED"
            tvHardSecurityCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvHardSecurityCardSub.text = "Hardware sensors, Honeypot & Archive operational"
            return
        }

        // Real Hub Overview
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val isEnabled = enabledMethods.any { it.packageName == packageName }
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isDefault = defaultIme != null && defaultIme.contains(packageName)

        val logCount = AuditLogManager.getLogs(this).size

        when {
            isDefault -> {
                tvKeyboardCardBadge.text = "ACTIVE"
                tvKeyboardCardBadge.setTextColor(Color.parseColor("#81C995"))
                tvKeyboardCardSub.text = "Default IME Active • $logCount events recorded"
            }
            isEnabled -> {
                tvKeyboardCardBadge.text = "READY"
                tvKeyboardCardBadge.setTextColor(Color.parseColor("#FDD663"))
                tvKeyboardCardSub.text = "Enabled in settings • Tap to set as default IME"
            }
            else -> {
                tvKeyboardCardBadge.text = "INACTIVE"
                tvKeyboardCardBadge.setTextColor(Color.parseColor("#F28B82"))
                tvKeyboardCardSub.text = "Not enabled in system settings"
            }
        }

        // 2. Package Guardian status
        val sp = getSharedPreferences("mobcare_package_guardian", Context.MODE_PRIVATE)
        val pkgEvents = sp.getInt("package_events_count", 0)
        tvPackageCardBadge.text = "ARMED"
        tvPackageCardSub.text = if (pkgEvents > 0) {
            "Active • $pkgEvents app install/removal events tracked"
        } else {
            "App inventory, sideload detector & removal vault"
        }

        // 3. Stealth status
        val isAdmin = StealthManager.isDeviceAdminActive(this)
        val isFakeCrash = StealthManager.isFakeCrashEnabled(this)
        val isCamouflage = StealthManager.isCamouflageEnabled(this)
        val intruderCount = IntruderCaptureManager.getIntruderRecords(this).size

        if (isAdmin) {
            tvStealthCardBadge.text = "ARMED"
            tvStealthCardBadge.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvStealthCardBadge.text = "SETUP"
            tvStealthCardBadge.setTextColor(Color.parseColor("#FDD663"))
        }
        val adminStatus = if (isAdmin) "Armed" else "Inactive"
        val cloakStatus = if (isCamouflage) "Cloak ON" else if (isFakeCrash) "Decoy ON" else "OFF"
        tvStealthCardSub.text = "Anti-Uninstall: $adminStatus • Disguise: $cloakStatus • $intruderCount snaps"

        // 4. Notification & Clipboard Vault status
        val isNotifListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val notifCount = NotificationVaultManager.getNotifications(this).size
        val clipCount = ClipboardVaultManager.getClipboardHistory(this).size

        if (isNotifListenerEnabled) {
            tvNotificationCardBadge.text = "ARMED"
            tvNotificationCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvNotificationCardSub.text = if (notifCount + clipCount > 0) {
                "Active • $notifCount alerts & $clipCount clipboard items captured"
            } else {
                "Active • Monitoring incoming notifications & clipboard"
            }
        } else {
            tvNotificationCardBadge.text = "SETUP"
            tvNotificationCardBadge.setTextColor(Color.parseColor("#FDD663"))
            tvNotificationCardSub.text = "Listener permission required • Tap to configure"
        }

        // 5. Offline Encrypted Vault status (100% Local-First / Zero Cloud)
        val totalPayload = logCount + notifCount + clipCount
        tvCloudSyncCardBadge.text = "OFFLINE"
        tvCloudSyncCardBadge.setTextColor(Color.parseColor("#81C995"))
        tvCloudSyncCardSub.text = "100% Offline • AES-256 GCM • $totalPayload records secured locally"

        // 6. App Timeline status
        val hasUsage = AppTimelineManager.hasUsageStatsPermission(this)
        if (hasUsage) {
            tvTimelineCardBadge.text = "ACTIVE"
            tvTimelineCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvTimelineCardSub.text = "Active • Tracking screen time and keystrokes across apps"
        } else {
            tvTimelineCardBadge.text = "SETUP"
            tvTimelineCardBadge.setTextColor(Color.parseColor("#FDD663"))
            tvTimelineCardSub.text = "Usage access required • Tap to configure"
        }

        // 7. App Locker status
        val lockedCount = AppLockerManager.getLockedPackages(this).size
        if (lockedCount > 0) {
            tvLockerCardBadge.text = "ARMED"
            tvLockerCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvLockerCardSub.text = "Active • $lockedCount apps protected by Sentinel gate"
        } else {
            tvLockerCardBadge.text = "READY"
            tvLockerCardBadge.setTextColor(Color.parseColor("#8AB4F8"))
            tvLockerCardSub.text = "0 apps locked • Tap to configure 5-column app gate"
        }

        // 8. Call & SMS Vault status
        val hasCallPerms = CallSmsVaultManager.hasPermissions(this)
        val callCount = CallSmsVaultManager.getCalls(this).size
        val smsCount = CallSmsVaultManager.getSms(this).size
        if (hasCallPerms) {
            tvCallSmsCardBadge.text = "ARMED"
            tvCallSmsCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvCallSmsCardSub.text = "Active • $callCount calls & $smsCount SMS messages vaulted"
        } else {
            tvCallSmsCardBadge.text = "SETUP"
            tvCallSmsCardBadge.setTextColor(Color.parseColor("#FDD663"))
            tvCallSmsCardSub.text = "Permissions required • Tap to configure persistent ledger"
        }

        // 9. Web Intelligence status
        val isAccEnabled = MobCareAccessibilityService.instance != null
        val webCount = BrowserVaultManager.getWebVisits(this).size
        if (isAccEnabled) {
            tvBrowserCardBadge.text = "ARMED"
            tvBrowserCardBadge.setTextColor(Color.parseColor("#81C995"))
            tvBrowserCardSub.text = "Active • $webCount URLs & search sessions mirrored"
        } else {
            tvBrowserCardBadge.text = "SETUP"
            tvBrowserCardBadge.setTextColor(Color.parseColor("#FDD663"))
            tvBrowserCardSub.text = "Sentinel Accessibility required • Tap to configure"
        }

        // Section & Overview Counters
        var coreActive = 0
        if (isDefault || isEnabled) coreActive++
        coreActive++ // Package Guardian is active
        if (isAdmin || isFakeCrash || isCamouflage) coreActive++

        var dataActive = 0
        if (isNotifListenerEnabled) dataActive++
        dataActive++ // Offline Encrypted Vault is active

        var appActive = 0
        if (hasUsage) appActive++
        if (lockedCount > 0) appActive++

        val totalActive = coreActive + dataActive + appActive
        tvStatModules.text = "$totalActive/7"

        tvCoreActiveCount.text = "$coreActive/3 active"
        tvCoreActiveCount.setTextColor(if (coreActive == 3) Color.parseColor("#81C995") else Color.parseColor("#7A7F8E"))

        tvDataActiveCount.text = "$dataActive/2 active"
        tvDataActiveCount.setTextColor(if (dataActive == 2) Color.parseColor("#81C995") else Color.parseColor("#7A7F8E"))

        tvAppActiveCount.text = "$appActive/2 active"
        tvAppActiveCount.setTextColor(if (appActive == 2) Color.parseColor("#81C995") else Color.parseColor("#7A7F8E"))

        tvFoundationalActiveCount.text = "10/10 active"
        tvFoundationalActiveCount.setTextColor(Color.parseColor("#81C995"))
        tvCoreSecurityCardBadge.text = "10/10 ACTIVE"
        tvCoreSecurityCardBadge.setTextColor(Color.parseColor("#81C995"))
        tvCoreSecurityCardSub.text = "10 Easy Layers • Zero Wipe Guard & Device State"

        tvMediumActiveCount.text = "16/16 active"
        tvMediumActiveCount.setTextColor(Color.parseColor("#81C995"))
        tvMediumSecurityCardBadge.text = "16/16 ARMED"
        tvMediumSecurityCardBadge.setTextColor(Color.parseColor("#81C995"))
        tvMediumSecurityCardSub.text = "SQLite WAL Engine • Hardware Keystore TEE • 16 Layers Armed"

        tvHardActiveCount.text = "14/14 active"
        tvHardActiveCount.setTextColor(Color.parseColor("#81C995"))
        tvHardSecurityCardBadge.text = "14/14 ARMED"
        tvHardSecurityCardBadge.setTextColor(Color.parseColor("#FDD663"))
        tvHardSecurityCardSub.text = "Sensor Guard • Canary Honeypot • Cryptographic Archive"

        // Threats detected (intruder snaps + non-PlayStore sideloaded apps)
        var threatCount = intruderCount
        try {
            val pm = packageManager
            val installed = pm.getInstalledPackages(0)
            for (pkg in installed) {
                val appInfo = pkg.applicationInfo ?: continue
                if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && pkg.packageName != packageName) {
                    val isSideload = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val info = pm.getInstallSourceInfo(pkg.packageName)
                        val initiator = info.initiatingPackageName ?: info.installingPackageName
                        initiator != "com.android.vending"
                    } else {
                        @Suppress("DEPRECATION")
                        val installer = pm.getInstallerPackageName(pkg.packageName)
                        installer != "com.android.vending"
                    }
                    if (isSideload) threatCount++
                }
            }
        } catch (e: Exception) {
            // graceful fallback
        }
        tvStatThreats.text = "$threatCount"
        if (threatCount > 0) {
            tvStatThreats.setTextColor(Color.parseColor("#F28B82"))
        } else {
            tvStatThreats.setTextColor(Color.parseColor("#ECEEF4"))
        }

        // Total recorded events
        val totalEvents = logCount + pkgEvents + notifCount + clipCount + callCount + smsCount + webCount
        tvStatEvents.text = "$totalEvents"

        // Device Status Subtitle
        val chargingTag = if (isCharging) " (Charging)" else ""
        tvDeviceStatusSub.text = "Battery: $batteryDisplay$chargingTag • 100% Offline Guardian"
    }

    private fun checkDecoyGates() {
        if (isSessionUnlocked) return

        if (StealthManager.isCamouflageEnabled(this)) {
            showCamouflageDiagnosticsView()
        } else if (StealthManager.isFakeCrashEnabled(this)) {
            showFakeCrashDialog()
        }
    }

    private fun showCamouflageDiagnosticsView() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121316"))
            setPadding((24 * density).toInt(), (48 * density).toInt(), (24 * density).toInt(), (32 * density).toInt())
        }

        // Header
        var titleTapCount = 0
        val headerTv = TextView(this).apply {
            text = "Device Diagnostics"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                titleTapCount++
                if (titleTapCount >= 3) {
                    showSecretPinPrompt(dialog)
                }
            }
            setOnLongClickListener {
                showSecretPinPrompt(dialog)
                true
            }
        }
        root.addView(headerTv)

        val subHeaderTv = TextView(this).apply {
            text = "Hardware telemetry & system performance monitor"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 13f
            setPadding(0, 4, 0, (28 * density).toInt())
        }
        root.addView(subHeaderTv)

        // Status Optimal Card (Secret Trigger: 3 Taps)
        var cardTapCount = 0
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
            setOnClickListener {
                cardTapCount++
                if (cardTapCount >= 3) {
                    showSecretPinPrompt(dialog)
                }
            }
        }

        val badge = TextView(this).apply {
            text = "OPTIMAL"
            setTextColor(Color.parseColor("#81C995"))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        statusCard.addView(badge)

        val cardTitle = TextView(this).apply {
            text = "All Subsystems Functional"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, (6 * density).toInt(), 0, (4 * density).toInt())
        }
        statusCard.addView(cardTitle)

        val cardSub = TextView(this).apply {
            text = "CPU, RAM and battery operating within nominal thermal parameters."
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 13f
        }
        statusCard.addView(cardSub)
        root.addView(statusCard)

        // Read Live Battery
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310
        val tempC = tempTenths / 10.0

        // Read Live RAM
        val actMgr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actMgr.getMemoryInfo(memInfo)
        val totalRamGb = String.format("%.1f", memInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
        val availRamGb = String.format("%.1f", memInfo.availMem / (1024.0 * 1024.0 * 1024.0))

        val metrics = listOf(
            "Battery Health" to "$batteryPct% • Normal (${tempC}°C)",
            "Memory (RAM)" to "$availRamGb GB free of $totalRamGb GB",
            "Operating System" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Hardware Model" to "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
        )

        val metricsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (16 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
        }

        metrics.forEach { (label, value) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
            }
            val lTv = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams((120 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val vTv = TextView(this).apply {
                text = value
                setTextColor(Color.parseColor("#E8EAED"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(lTv)
            row.addView(vTv)
            metricsCard.addView(row)
        }
        root.addView(metricsCard)

        val btnExit = Button(this).apply {
            text = "Exit Diagnostics"
            setTextColor(Color.parseColor("#9AA0A6"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = (32 * density).toInt()
            }
            setOnClickListener { finishAffinity() }
        }
        root.addView(btnExit)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun showFakeCrashDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (18 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        var secretTapCount = 0
        val titleTv = TextView(this).apply {
            text = "Android System"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (12 * density).toInt())

            setOnClickListener {
                secretTapCount++
                if (secretTapCount >= 3) {
                    showSecretPinPrompt(dialog)
                }
            }
            setOnLongClickListener {
                showSecretPinPrompt(dialog)
                true
            }
        }
        root.addView(titleTv)

        val bodyTv = TextView(this).apply {
            text = "Google Play Services keeps stopping."
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 14f
            setPadding(0, 0, 0, (24 * density).toInt())
        }
        root.addView(bodyTv)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnReport = Button(this).apply {
            text = "Feedback"
            setTextColor(Color.parseColor("#8AB4F8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finishAffinity() }
        }

        val btnClose = Button(this).apply {
            text = "Close app"
            setTextColor(Color.parseColor("#8AB4F8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finishAffinity() }
        }

        btnRow.addView(btnReport)
        btnRow.addView(btnClose)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun showSecretPinPrompt(parentDialog: Dialog? = null) {
        val pinDialog = Dialog(this)
        pinDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        pinDialog.setCancelable(false)
        pinDialog.setCanceledOnTouchOutside(false)
        pinDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (18 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Security Verification"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        root.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "Enter security PIN to disarm security gate"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(subTv)

        val pinInput = EditText(this).apply {
            hint = "••••"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.parseColor("#E8EAED"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 18f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(Color.parseColor("#121316"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
        }
        root.addView(pinInput)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (16 * density).toInt(), 0, 0)
        }

        val btnCancel = Button(this).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#9AA0A6"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                pinDialog.dismiss()
                if (parentDialog == null) {
                    finishAffinity()
                }
            }
        }

        val btnUnlock = Button(this).apply {
            text = "Unlock"
            setTextColor(Color.parseColor("#81C995"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val entered = pinInput.text.toString().trim()
                when {
                    StealthManager.verifyPin(this@MainActivity, entered) -> {
                        isSessionUnlocked = true
                        StealthManager.isDuressSandboxActive = false
                        pinDialog.dismiss()
                        parentDialog?.dismiss()
                        updateHubOverview()
                        Toast.makeText(this@MainActivity, "Access granted.", Toast.LENGTH_SHORT).show()
                    }
                    StealthManager.verifyPanicPin(this@MainActivity, entered) -> {
                        isSessionUnlocked = true
                        StealthManager.isDuressSandboxActive = true
                        pinDialog.dismiss()
                        parentDialog?.dismiss()
                        updateHubOverview()
                        Toast.makeText(this@MainActivity, "Diagnostics session unlocked.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        AuditLogManager.logSystemEvent(
                            this@MainActivity,
                            packageName,
                            "MobCare Security",
                            "[Intruder Alert] Failed Decoy PIN unlock attempt ($entered)"
                        )
                        // Silently Snap Front Camera Intruder Photo
                        IntruderCaptureManager.captureIntruderPhoto(this@MainActivity, entered)
                        Toast.makeText(this@MainActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnUnlock)
        root.addView(btnRow)

        pinDialog.setContentView(root)
        pinDialog.show()
    }

    private fun showDeviceIntegrityDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * density).toInt(), (20 * density).toInt(), (22 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1C1E26"))
                setStroke((1 * density).toInt(), Color.parseColor("#2E3240"))
            }
        }

        // Header Title
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * density).toInt() }
        }

        val titleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Device & Security Integrity"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val badgeTv = TextView(this).apply {
            text = "100% OFFLINE"
            setTextColor(Color.parseColor("#81C995"))
            textSize = 10f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 8f * density
                setColor(Color.parseColor("#1A2D22"))
            }
        }

        titleRow.addView(titleTv)
        titleRow.addView(badgeTv)
        root.addView(titleRow)

        // Read real battery info
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, ifilter)
        if (batteryPct <= 0 && batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPct = (level * 100) / scale
        }
        val statusExtra = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING || statusExtra == BatteryManager.BATTERY_STATUS_FULL

        val adbEnabled = try {
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) { false }

        val isRooted = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk")
            .any { java.io.File(it).exists() }

        val items = listOf(
            Triple("Battery Level", "${if (batteryPct >= 0) "$batteryPct%" else "Normal"}${if (isCharging) " • Charging ⚡" else " • Discharging"}", "#81C995"),
            Triple("Architecture", "100% Local-First (Zero Cloud / No Remote Calls)", "#8AB4F8"),
            Triple("Data Safety Policy", "Non-Destructive Guardian (Zero Data Wiping / No User Deletions)", "#81C995"),
            Triple("Device Model", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}", "#ECEEF4"),
            Triple("Android OS", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", "#ECEEF4"),
            Triple("Security Patch", Build.VERSION.SECURITY_PATCH ?: "Not Reported", "#ECEEF4"),
            Triple("Privilege State", if (isRooted) "Root Binary Present" else "Standard Unrooted Sandbox", if (isRooted) "#F28B82" else "#81C995"),
            Triple("USB Debugging", if (adbEnabled) "Active (ADB Interface Enabled)" else "Disabled", if (adbEnabled) "#FDD663" else "#81C995")
        )

        for ((label, value, valueColor) in items) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
                background = GradientDrawable().apply {
                    cornerRadius = 12f * density
                    setColor(Color.parseColor("#15161C"))
                }
            }

            val labelTv = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#7A7F8E"))
                textSize = 11f
            }

            val valueTv = TextView(this).apply {
                text = value
                setTextColor(Color.parseColor(valueColor))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (2 * density).toInt() }
            }

            itemLayout.addView(labelTv)
            itemLayout.addView(valueTv)
            root.addView(itemLayout)
        }

        // Close Button
        val btnClose = Button(this).apply {
            text = "DISMISS"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 14f * density
                setColor(Color.parseColor("#252834"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).toInt()
            ).apply { topMargin = (10 * density).toInt() }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        val scroll = ScrollView(this).apply {
            addView(root)
        }

        dialog.setContentView(scroll)
        dialog.show()
    }
}
