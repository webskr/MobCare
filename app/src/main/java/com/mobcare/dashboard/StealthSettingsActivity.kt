package com.mobcare.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mobcare.R
import com.mobcare.security.IntruderCaptureManager
import com.mobcare.security.IntruderRecord
import com.mobcare.security.StealthManager
import com.mobcare.security.MobCareAccessibilityService
import com.mobcare.launcher.data.IdentitySwapManager
import com.mobcare.launcher.ui.IdentitySwapActivity
import android.os.PowerManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import com.mobcare.audit.AppTimelineManager
import com.mobcare.audit.CallSmsVaultManager
import java.io.File

class StealthSettingsActivity : AppCompatActivity() {

    // Master System Permissions Hub
    private lateinit var tvMasterPermsBadge: TextView
    private lateinit var tvPermAccessibilityBadge: TextView
    private lateinit var btnPermAccessibility: Button
    private lateinit var tvPermDeviceAdminBadge: TextView
    private lateinit var btnPermDeviceAdmin: Button
    private lateinit var tvPermNotificationBadge: TextView
    private lateinit var btnPermNotification: Button
    private lateinit var tvPermUsageStatsBadge: TextView
    private lateinit var btnPermUsageStats: Button
    private lateinit var tvPermOverlayMasterBadge: TextView
    private lateinit var btnPermOverlay: Button
    private lateinit var tvPermCameraBadge: TextView
    private lateinit var btnPermCamera: Button
    private lateinit var tvPermKeyboardBadge: TextView
    private lateinit var btnPermKeyboard: Button
    private lateinit var tvPermCallSmsBadge: TextView
    private lateinit var btnPermCallSms: Button
    private lateinit var tvPermBatteryBadge: TextView
    private lateinit var btnPermBattery: Button

    private lateinit var tvAdminBadge: TextView
    private lateinit var btnToggleAdmin: Button
    private lateinit var tvOverlayBadge: TextView
    private lateinit var btnGrantOverlay: Button
    private lateinit var tvFakeCrashBadge: TextView
    private lateinit var btnToggleFakeCrash: Button
    private lateinit var tvPinInfo: TextView
    private lateinit var btnChangePin: Button

    private lateinit var tvCamouflageBadge: TextView
    private lateinit var btnToggleCamouflage: Button
    private lateinit var tvIntruderBadge: TextView
    private lateinit var btnGrantCamera: Button
    private lateinit var btnViewIntruders: Button
    private lateinit var btnClearIntruders: Button
    private lateinit var tvSentinelBadge: TextView
    private lateinit var btnToggleSentinel: Button
    private lateinit var tvScreenshotBadge: TextView
    private lateinit var btnTakeSilentScreenshot: Button
    private lateinit var btnViewScreenshots: Button

    private lateinit var tvSwapBadge: TextView
    private lateinit var btnConfigureSwap: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stealth_settings)

        // Master Permissions Hub Views
        tvMasterPermsBadge = findViewById(R.id.tvMasterPermsBadge)
        tvPermAccessibilityBadge = findViewById(R.id.tvPermAccessibilityBadge)
        btnPermAccessibility = findViewById(R.id.btnPermAccessibility)
        tvPermDeviceAdminBadge = findViewById(R.id.tvPermDeviceAdminBadge)
        btnPermDeviceAdmin = findViewById(R.id.btnPermDeviceAdmin)
        tvPermNotificationBadge = findViewById(R.id.tvPermNotificationBadge)
        btnPermNotification = findViewById(R.id.btnPermNotification)
        tvPermUsageStatsBadge = findViewById(R.id.tvPermUsageStatsBadge)
        btnPermUsageStats = findViewById(R.id.btnPermUsageStats)
        tvPermOverlayMasterBadge = findViewById(R.id.tvPermOverlayMasterBadge)
        btnPermOverlay = findViewById(R.id.btnPermOverlay)
        tvPermCameraBadge = findViewById(R.id.tvPermCameraBadge)
        btnPermCamera = findViewById(R.id.btnPermCamera)
        tvPermKeyboardBadge = findViewById(R.id.tvPermKeyboardBadge)
        btnPermKeyboard = findViewById(R.id.btnPermKeyboard)
        tvPermCallSmsBadge = findViewById(R.id.tvPermCallSmsBadge)
        btnPermCallSms = findViewById(R.id.btnPermCallSms)
        tvPermBatteryBadge = findViewById(R.id.tvPermBatteryBadge)
        btnPermBattery = findViewById(R.id.btnPermBattery)

        btnPermAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnPermDeviceAdmin.setOnClickListener {
            if (StealthManager.isDeviceAdminActive(this)) {
                Toast.makeText(this, "Anti-Uninstall is already active & protecting MobCare.", Toast.LENGTH_SHORT).show()
                try {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } catch (e: Exception) {
                    // fallback
                }
            } else {
                startActivity(StealthManager.getDeviceAdminPromptIntent(this))
            }
        }

        btnPermNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnPermUsageStats.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnPermOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnPermCamera.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }

        btnPermKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnPermCallSms.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_SMS
                ),
                102
            )
        }

        btnPermBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e2: Exception) {
                        Toast.makeText(this, "Please adjust battery optimization in phone settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        tvAdminBadge = findViewById(R.id.tvAdminBadge)
        btnToggleAdmin = findViewById(R.id.btnToggleAdmin)
        tvOverlayBadge = findViewById(R.id.tvOverlayBadge)
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)

        btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        tvFakeCrashBadge = findViewById(R.id.tvFakeCrashBadge)
        btnToggleFakeCrash = findViewById(R.id.btnToggleFakeCrash)
        tvPinInfo = findViewById(R.id.tvPinInfo)
        btnChangePin = findViewById(R.id.btnChangePin)

        tvCamouflageBadge = findViewById(R.id.tvCamouflageBadge)
        btnToggleCamouflage = findViewById(R.id.btnToggleCamouflage)
        tvIntruderBadge = findViewById(R.id.tvIntruderBadge)
        btnGrantCamera = findViewById(R.id.btnGrantCamera)
        btnViewIntruders = findViewById(R.id.btnViewIntruders)
        btnClearIntruders = findViewById(R.id.btnClearIntruders)
        tvSentinelBadge = findViewById(R.id.tvSentinelBadge)
        btnToggleSentinel = findViewById(R.id.btnToggleSentinel)
        tvScreenshotBadge = findViewById(R.id.tvScreenshotBadge)
        btnTakeSilentScreenshot = findViewById(R.id.btnTakeSilentScreenshot)
        btnViewScreenshots = findViewById(R.id.btnViewScreenshots)

        tvSwapBadge = findViewById(R.id.tvSwapBadge)
        btnConfigureSwap = findViewById(R.id.btnConfigureSwap)

        btnConfigureSwap.setOnClickListener {
            startActivity(Intent(this, IdentitySwapActivity::class.java))
        }

        btnTakeSilentScreenshot.setOnClickListener {
            if (MobCareAccessibilityService.instance == null) {
                Toast.makeText(this, "Please enable MobCare Sentinel in Accessibility Settings", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                MobCareAccessibilityService.takeSilentScreenshot(this) { success, path ->
                    if (success) {
                        Toast.makeText(this, "Silent snapshot saved to secure vault", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Capture failed: " + (path ?: "unknown"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnViewScreenshots.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("INITIAL_TAB", "SCREENSHOTS")
            startActivity(intent)
        }

        btnToggleSentinel.setOnClickListener {
            val current = StealthManager.isPostUnlockSentinelEnabled(this)
            val newState = !current
            StealthManager.setPostUnlockSentinelEnabled(this, newState)
            updateUiState()
            val msg = if (newState) "Post-Unlock Face Sentinel enabled" else "Post-Unlock Face Sentinel disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnToggleAdmin.setOnClickListener {
            if (StealthManager.isDeviceAdminActive(this)) {
                Toast.makeText(this, "Anti-Uninstall is already active & protecting MobCare.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(StealthManager.getDeviceAdminPromptIntent(this))
            }
        }

        btnToggleFakeCrash.setOnClickListener {
            val current = StealthManager.isFakeCrashEnabled(this)
            val newState = !current
            StealthManager.setFakeCrashEnabled(this, newState)
            updateUiState()
            val msg = if (newState) "Fake Crash Gate enabled (PIN: ${StealthManager.getMasterPin(this)})" else "Fake Crash Gate disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnChangePin.setOnClickListener {
            showChangePinDialog()
        }

        btnToggleCamouflage.setOnClickListener {
            val current = StealthManager.isCamouflageEnabled(this)
            val newState = !current
            StealthManager.setCamouflageEnabled(this, newState)
            updateUiState()
            val msg = if (newState) "System Diagnostics Cloak enabled" else "System Diagnostics Cloak disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnGrantCamera.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }

        btnViewIntruders.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("INITIAL_TAB", "INTRUDERS")
            startActivity(intent)
        }

        btnClearIntruders.setOnClickListener {
            IntruderCaptureManager.clearRecords(this)
            updateUiState()
            Toast.makeText(this, "Intruder photos cleared", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            updateUiState()
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted! Intruder silent snap active.", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 102) {
            updateUiState()
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "Call & SMS vault permissions granted!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUiState() {
        val isAdmin = StealthManager.isDeviceAdminActive(this)
        val isFakeCrash = StealthManager.isFakeCrashEnabled(this)
        val isCamouflage = StealthManager.isCamouflageEnabled(this)
        val currentPin = StealthManager.getMasterPin(this)
        val panicPin = StealthManager.getPanicPin(this)
        val intruderRecords = IntruderCaptureManager.getIntruderRecords(this)
        val isCamGranted = IntruderCaptureManager.isCameraPermissionGranted(this)

        // --- MASTER SYSTEM PERMISSIONS & ACCESS HUB UPDATE ---
        val isAcc = MobCareAccessibilityService.instance != null
        if (isAcc) {
            tvPermAccessibilityBadge.text = "ARMED"
            tvPermAccessibilityBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermAccessibility.text = "Accessibility Sentinel Active"
            btnPermAccessibility.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermAccessibility.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermAccessibilityBadge.text = "REQUIRED"
            tvPermAccessibilityBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermAccessibility.text = "Configure Accessibility"
            btnPermAccessibility.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermAccessibility.setTextColor(Color.parseColor("#FFFFFF"))
        }

        if (isAdmin) {
            tvPermDeviceAdminBadge.text = "ARMED"
            tvPermDeviceAdminBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermDeviceAdmin.text = "Device Administrator Protected"
            btnPermDeviceAdmin.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermDeviceAdmin.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermDeviceAdminBadge.text = "REQUIRED"
            tvPermDeviceAdminBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermDeviceAdmin.text = "Activate Device Administrator"
            btnPermDeviceAdmin.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermDeviceAdmin.setTextColor(Color.parseColor("#FFFFFF"))
        }

        val isNotif = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (isNotif) {
            tvPermNotificationBadge.text = "ARMED"
            tvPermNotificationBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermNotification.text = "Notification Vault Active"
            btnPermNotification.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermNotification.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermNotificationBadge.text = "REQUIRED"
            tvPermNotificationBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermNotification.text = "Enable Notification Access"
            btnPermNotification.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermNotification.setTextColor(Color.parseColor("#FFFFFF"))
        }

        val hasUsage = AppTimelineManager.hasUsageStatsPermission(this)
        if (hasUsage) {
            tvPermUsageStatsBadge.text = "ACTIVE"
            tvPermUsageStatsBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermUsageStats.text = "Usage Stats Active"
            btnPermUsageStats.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermUsageStats.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermUsageStatsBadge.text = "REQUIRED"
            tvPermUsageStatsBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermUsageStats.text = "Grant Usage Access"
            btnPermUsageStats.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermUsageStats.setTextColor(Color.parseColor("#FFFFFF"))
        }

        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        if (canOverlay) {
            tvPermOverlayMasterBadge.text = "ARMED"
            tvPermOverlayMasterBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermOverlay.text = "Overlay Permission Active"
            btnPermOverlay.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermOverlay.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermOverlayMasterBadge.text = "REQUIRED"
            tvPermOverlayMasterBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermOverlay.text = "Grant Overlay Permission"
            btnPermOverlay.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermOverlay.setTextColor(Color.parseColor("#FFFFFF"))
        }

        if (isCamGranted) {
            tvPermCameraBadge.text = "ARMED"
            tvPermCameraBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermCamera.text = "Camera Permission Active"
            btnPermCamera.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermCamera.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermCameraBadge.text = "REQUIRED"
            tvPermCameraBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermCamera.text = "Grant Camera Permission"
            btnPermCamera.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermCamera.setTextColor(Color.parseColor("#FFFFFF"))
        }

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val isImeEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isImeDefault = defaultIme != null && defaultIme.contains(packageName)
        when {
            isImeDefault -> {
                tvPermKeyboardBadge.text = "ACTIVE"
                tvPermKeyboardBadge.setTextColor(Color.parseColor("#81C995"))
                btnPermKeyboard.text = "Default Keyboard Active"
                btnPermKeyboard.setBackgroundColor(Color.parseColor("#163A34"))
                btnPermKeyboard.setTextColor(Color.parseColor("#81C995"))
            }
            isImeEnabled -> {
                tvPermKeyboardBadge.text = "READY"
                tvPermKeyboardBadge.setTextColor(Color.parseColor("#FDD663"))
                btnPermKeyboard.text = "Set As Default Keyboard"
                btnPermKeyboard.setBackgroundColor(Color.parseColor("#26272E"))
                btnPermKeyboard.setTextColor(Color.parseColor("#FFFFFF"))
            }
            else -> {
                tvPermKeyboardBadge.text = "INACTIVE"
                tvPermKeyboardBadge.setTextColor(Color.parseColor("#F28B82"))
                btnPermKeyboard.text = "Enable in Input Settings"
                btnPermKeyboard.setBackgroundColor(Color.parseColor("#26272E"))
                btnPermKeyboard.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }

        val hasCallSms = CallSmsVaultManager.hasPermissions(this)
        if (hasCallSms) {
            tvPermCallSmsBadge.text = "ARMED"
            tvPermCallSmsBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermCallSms.text = "Telephony Vault Active"
            btnPermCallSms.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermCallSms.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermCallSmsBadge.text = "REQUIRED"
            tvPermCallSmsBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermCallSms.text = "Grant Call & SMS Permissions"
            btnPermCallSms.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermCallSms.setTextColor(Color.parseColor("#FFFFFF"))
        }

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
        if (isIgnoringBattery) {
            tvPermBatteryBadge.text = "OPTIMIZED"
            tvPermBatteryBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermBattery.text = "Unrestricted Battery"
            btnPermBattery.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermBattery.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermBatteryBadge.text = "RESTRICTED"
            tvPermBatteryBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermBattery.text = "Configure Battery Optimization"
            btnPermBattery.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermBattery.setTextColor(Color.parseColor("#FFFFFF"))
        }

        var grantedTotal = 0
        if (isAcc) grantedTotal++
        if (isAdmin) grantedTotal++
        if (isNotif) grantedTotal++
        if (hasUsage) grantedTotal++
        if (canOverlay) grantedTotal++
        if (isCamGranted) grantedTotal++
        if (isImeDefault || isImeEnabled) grantedTotal++
        if (hasCallSms) grantedTotal++
        if (isIgnoringBattery) grantedTotal++

        tvMasterPermsBadge.text = "$grantedTotal/9 GRANTED"
        tvMasterPermsBadge.setTextColor(if (grantedTotal >= 8) Color.parseColor("#81C995") else Color.parseColor("#FDD663"))

        // --- EXISTING SECTIONS UPDATE ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canOverlay) {
                tvOverlayBadge.text = "ARMED"
                tvOverlayBadge.setTextColor(Color.parseColor("#81C995"))
                btnGrantOverlay.text = "Overlay Permission Active"
                btnGrantOverlay.setBackgroundColor(Color.parseColor("#163A34"))
                btnGrantOverlay.setTextColor(Color.parseColor("#81C995"))
            } else {
                tvOverlayBadge.text = "REQUIRED"
                tvOverlayBadge.setTextColor(Color.parseColor("#FDD663"))
                btnGrantOverlay.text = "Grant Overlay Permission"
                btnGrantOverlay.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_bg))
                btnGrantOverlay.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }

        if (isAdmin) {
            tvAdminBadge.text = "ARMED"
            tvAdminBadge.setTextColor(Color.parseColor("#81C995"))
            btnToggleAdmin.text = "Device Administrator Protected"
            btnToggleAdmin.setBackgroundColor(Color.parseColor("#163A34"))
            btnToggleAdmin.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvAdminBadge.text = "INACTIVE"
            tvAdminBadge.setTextColor(Color.parseColor("#FDD663"))
            btnToggleAdmin.text = "Activate Device Administrator"
            btnToggleAdmin.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_bg))
            btnToggleAdmin.setTextColor(Color.parseColor("#FFFFFF"))
        }

        if (isFakeCrash) {
            tvFakeCrashBadge.text = "ACTIVE"
            tvFakeCrashBadge.setTextColor(Color.parseColor("#81C995"))
            btnToggleFakeCrash.text = "Disable Fake Crash Gate"
            btnToggleFakeCrash.setBackgroundColor(Color.parseColor("#163A34"))
            btnToggleFakeCrash.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvFakeCrashBadge.text = "OFF"
            tvFakeCrashBadge.setTextColor(Color.parseColor("#9AA0A6"))
            btnToggleFakeCrash.text = "Enable Fake Crash Gate"
            btnToggleFakeCrash.setBackgroundColor(Color.parseColor("#3C4043"))
            btnToggleFakeCrash.setTextColor(Color.parseColor("#FFFFFF"))
        }

        val isSentinel = StealthManager.isPostUnlockSentinelEnabled(this)
        if (isSentinel) {
            tvSentinelBadge.text = "ACTIVE"
            tvSentinelBadge.setTextColor(Color.parseColor("#81C995"))
            btnToggleSentinel.text = "Disable Post-Unlock Sentinel"
            btnToggleSentinel.setBackgroundColor(Color.parseColor("#163A34"))
            btnToggleSentinel.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvSentinelBadge.text = "OFF"
            tvSentinelBadge.setTextColor(Color.parseColor("#9AA0A6"))
            btnToggleSentinel.text = "Enable Post-Unlock Sentinel"
            btnToggleSentinel.setBackgroundColor(Color.parseColor("#3C4043"))
            btnToggleSentinel.setTextColor(Color.parseColor("#FFFFFF"))
        }

        if (isCamouflage) {
            tvCamouflageBadge.text = "ACTIVE"
            tvCamouflageBadge.setTextColor(Color.parseColor("#81C995"))
            btnToggleCamouflage.text = "Disable Diagnostics Cloak"
            btnToggleCamouflage.setBackgroundColor(Color.parseColor("#163A34"))
            btnToggleCamouflage.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvCamouflageBadge.text = "OFF"
            tvCamouflageBadge.setTextColor(Color.parseColor("#9AA0A6"))
            btnToggleCamouflage.text = "Enable Diagnostics Cloak"
            btnToggleCamouflage.setBackgroundColor(Color.parseColor("#3C4043"))
            btnToggleCamouflage.setTextColor(Color.parseColor("#FFFFFF"))
        }

        tvPinInfo.text = "Master PIN: $currentPin • Duress Panic PIN: $panicPin"

        if (isCamGranted) {
            btnGrantCamera.visibility = View.GONE
            tvIntruderBadge.text = "${intruderRecords.size} SNAPS (ACTIVE)"
            tvIntruderBadge.setTextColor(if (intruderRecords.isNotEmpty()) Color.parseColor("#F28B82") else Color.parseColor("#81C995"))
        } else {
            btnGrantCamera.visibility = View.VISIBLE
            tvIntruderBadge.text = "PERMISSION NEEDED"
            tvIntruderBadge.setTextColor(Color.parseColor("#FDD663"))
        }

        // Identity Swap Status
        val activeSwap = IdentitySwapManager.getActiveSwap(this)
        if (activeSwap != null && activeSwap.isActive) {
            tvSwapBadge.text = "SWAPPED"
            tvSwapBadge.setTextColor(Color.parseColor("#81C995"))
            btnConfigureSwap.text = "Cloaked: ${activeSwap.customLabel}"
        } else {
            tvSwapBadge.text = "READY"
            tvSwapBadge.setTextColor(Color.parseColor("#8AB4F8"))
            btnConfigureSwap.text = "Identity Swapper"
        }
    }

    private fun showIntruderGalleryDialog() {
        val records = IntruderCaptureManager.getIntruderRecords(this)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val displayMetrics = resources.displayMetrics

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Intruder Photo Gallery"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "${records.size} unauthorized attempts captured (Tap row to inspect full image)"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 2, 0, (14 * density).toInt())
        }
        root.addView(subTv)

        if (records.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No intruder photos captured yet.\nWhen someone enters an incorrect PIN or fails device lock screen, their front camera photo will appear here."
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, (40 * density).toInt(), 0, (40 * density).toInt())
            }
            root.addView(emptyTv)
        } else {
            val scrollView = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (380 * density).toInt()
                )
            }
            val listContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            records.forEach { record ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (10 * density).toInt()
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 14f * density
                        setColor(Color.parseColor("#25262C"))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        showFullScreenPhotoDialog(record)
                    }
                }

                val iv = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams((60 * density).toInt(), (60 * density).toInt()).apply {
                        marginEnd = (12 * density).toInt()
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, 10f * density)
                        }
                    }
                    clipToOutline = true
                    try {
                        val bmp = BitmapFactory.decodeFile(record.filePath)
                        if (bmp != null) setImageBitmap(bmp) else setImageResource(android.R.drawable.ic_menu_camera)
                    } catch (e: Exception) {
                        setImageResource(android.R.drawable.ic_menu_camera)
                    }
                }
                card.addView(iv)

                val infoCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val timeTv = TextView(this).apply {
                    text = record.timestamp
                    setTextColor(Color.parseColor("#E8EAED"))
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                infoCol.addView(timeTv)

                val pinTv = TextView(this).apply {
                    text = "Attempted: ${record.attemptedPin}"
                    setTextColor(Color.parseColor("#F28B82"))
                    textSize = 12f
                    setPadding(0, 2, 0, 0)
                }
                infoCol.addView(pinTv)

                val tapHintTv = TextView(this).apply {
                    text = "Tap to enlarge"
                    setTextColor(Color.parseColor("#8AB4F8"))
                    textSize = 11f
                    setPadding(0, 2, 0, 0)
                }
                infoCol.addView(tapHintTv)

                card.addView(infoCol)
                listContainer.addView(card)
            }

            scrollView.addView(listContainer)
            root.addView(scrollView)
        }

        val btnClose = Button(this).apply {
            text = "Dismiss"
            setTextColor(Color.parseColor("#8AB4F8"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
                topMargin = (10 * density).toInt()
            }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()

        val dialogWidth = (displayMetrics.widthPixels * 0.92).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showFullScreenPhotoDialog(record: IntruderRecord) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val displayMetrics = resources.displayMetrics

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#121316"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Intruder Snapshot"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(titleTv)

        val metaTv = TextView(this).apply {
            text = "${record.timestamp} • ${record.attemptedPin}"
            setTextColor(Color.parseColor("#F28B82"))
            textSize = 13f
            setPadding(0, 2, 0, (12 * density).toInt())
        }
        root.addView(metaTv)

        val bigIv = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (420 * density).toInt()
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = ColorDrawable(Color.BLACK)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 16f * density)
                }
            }
            clipToOutline = true
            try {
                val bmp = BitmapFactory.decodeFile(record.filePath)
                if (bmp != null) setImageBitmap(bmp) else setImageResource(android.R.drawable.ic_menu_camera)
            } catch (e: Exception) {
                setImageResource(android.R.drawable.ic_menu_camera)
            }
        }
        root.addView(bigIv)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (14 * density).toInt(), 0, 0)
        }

        val btnDelete = Button(this).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#F28B82"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                try {
                    File(record.filePath).delete()
                    Toast.makeText(this@StealthSettingsActivity, "Photo deleted", Toast.LENGTH_SHORT).show()
                } catch (ignored: Exception) {}
                dialog.dismiss()
                showIntruderGalleryDialog()
            }
        }

        val btnDismiss = Button(this).apply {
            text = "Close"
            setTextColor(Color.parseColor("#8AB4F8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dialog.dismiss() }
        }

        btnRow.addView(btnDelete)
        btnRow.addView(btnDismiss)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.show()

        val dialogWidth = (displayMetrics.widthPixels * 0.94).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showChangePinDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Configure Security PINs"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        root.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "Set your Master PIN (Full access) and optional Panic PIN (Decoy dummy mode):"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 13f
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(subTv)

        val pinInput = EditText(this).apply {
            hint = "Master PIN (Current: ${StealthManager.getMasterPin(this@StealthSettingsActivity)})"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.parseColor("#E8EAED"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 16f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(Color.parseColor("#121316"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
        }
        root.addView(pinInput)

        val panicInput = EditText(this).apply {
            hint = "Decoy Panic PIN (Current: ${StealthManager.getPanicPin(this@StealthSettingsActivity)})"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.parseColor("#E8EAED"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (10 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(Color.parseColor("#121316"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
        }
        root.addView(panicInput)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (16 * density).toInt(), 0, 0)
        }

        val btnCancel = Button(this).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#9AA0A6"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dialog.dismiss() }
        }

        val btnSave = Button(this).apply {
            text = "Save PINs"
            setTextColor(Color.parseColor("#81C995"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val newPin = pinInput.text.toString().trim()
                val newPanic = panicInput.text.toString().trim()

                if (newPin.isNotEmpty()) {
                    if (newPin.length >= 4) {
                        StealthManager.setMasterPin(this@StealthSettingsActivity, newPin)
                    } else {
                        Toast.makeText(this@StealthSettingsActivity, "Master PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                if (newPanic.isNotEmpty()) {
                    if (newPanic.length >= 4) {
                        StealthManager.setPanicPin(this@StealthSettingsActivity, newPanic)
                    } else {
                        Toast.makeText(this@StealthSettingsActivity, "Panic PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                updateUiState()
                dialog.dismiss()
                Toast.makeText(this@StealthSettingsActivity, "Security PINs updated successfully", Toast.LENGTH_SHORT).show()
            }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnSave)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.show()
    }
    private fun showScreenshotGalleryDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Silent Screenshots Vault"
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        root.addView(titleTv)

        val dir = File(filesDir, "screenshots")
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No silent screenshots captured yet."
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                setPadding(0, (20 * density).toInt(), 0, (20 * density).toInt())
            }
            root.addView(emptyTv)
        } else {
            val scroll = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (360 * density).toInt()
                )
            }
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            files.forEach { file ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
                }
                val nameTv = TextView(this).apply {
                    text = file.name
                    setTextColor(Color.parseColor("#8AB4F8"))
                    textSize = 12f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                val iv = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (180 * density).toInt()
                    ).apply {
                        topMargin = (6 * density).toInt()
                        bottomMargin = (10 * density).toInt()
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    setImageBitmap(bmp)
                }
                row.addView(nameTv)
                row.addView(iv)
                container.addView(row)
            }
            scroll.addView(container)
            root.addView(scroll)
        }

        val btnClose = Button(this).apply {
            text = "Close"
            setTextColor(Color.parseColor("#81C995"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }
}
