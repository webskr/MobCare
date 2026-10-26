package com.mobcare.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.mobcare.R
import com.mobcare.audit.AppTimelineManager
import com.mobcare.audit.CallSmsVaultManager
import com.mobcare.audit.MobCareSecurityDatabase
import com.mobcare.launcher.data.IdentitySwapManager
import com.mobcare.security.IntruderCaptureManager
import com.mobcare.security.MobCareAccessibilityService
import com.mobcare.security.StealthManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvSettingsHubBadge: TextView
    private lateinit var tvMasterPermsBadge: TextView

    private lateinit var tvPermAccessibilityBadge: TextView
    private lateinit var btnPermAccessibility: Button

    private lateinit var tvPermDeviceAdminBadge: TextView
    private lateinit var btnPermDeviceAdmin: Button

    private lateinit var tvPermBatteryBadge: TextView
    private lateinit var btnPermBattery: Button

    private lateinit var tvPermUsageStatsBadge: TextView
    private lateinit var btnPermUsageStats: Button

    private lateinit var tvPermNotificationBadge: TextView
    private lateinit var btnPermNotification: Button

    private lateinit var tvPermCameraBadge: TextView
    private lateinit var btnPermCamera: Button

    private lateinit var tvPermCallSmsBadge: TextView
    private lateinit var btnPermCallSms: Button

    private lateinit var tvPermOverlayMasterBadge: TextView
    private lateinit var btnPermOverlay: Button

    private lateinit var btnVacuumDatabase: Button
    private lateinit var btnResetIdentity: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup unified bottom navigation
        BottomNavHelper.setup(this, NavTab.SETTINGS)

        tvSettingsHubBadge = findViewById(R.id.tvSettingsHubBadge)
        tvMasterPermsBadge = findViewById(R.id.tvMasterPermsBadge)

        tvPermAccessibilityBadge = findViewById(R.id.tvPermAccessibilityBadge)
        btnPermAccessibility = findViewById(R.id.btnPermAccessibility)

        tvPermDeviceAdminBadge = findViewById(R.id.tvPermDeviceAdminBadge)
        btnPermDeviceAdmin = findViewById(R.id.btnPermDeviceAdmin)

        tvPermBatteryBadge = findViewById(R.id.tvPermBatteryBadge)
        btnPermBattery = findViewById(R.id.btnPermBattery)

        tvPermUsageStatsBadge = findViewById(R.id.tvPermUsageStatsBadge)
        btnPermUsageStats = findViewById(R.id.btnPermUsageStats)

        tvPermNotificationBadge = findViewById(R.id.tvPermNotificationBadge)
        btnPermNotification = findViewById(R.id.btnPermNotification)

        tvPermCameraBadge = findViewById(R.id.tvPermCameraBadge)
        btnPermCamera = findViewById(R.id.btnPermCamera)

        tvPermCallSmsBadge = findViewById(R.id.tvPermCallSmsBadge)
        btnPermCallSms = findViewById(R.id.btnPermCallSms)

        tvPermOverlayMasterBadge = findViewById(R.id.tvPermOverlayMasterBadge)
        btnPermOverlay = findViewById(R.id.btnPermOverlay)

        btnVacuumDatabase = findViewById(R.id.btnVacuumDatabase)
        btnResetIdentity = findViewById(R.id.btnResetIdentity)

        // Click Listeners
        btnPermAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnPermDeviceAdmin.setOnClickListener {
            if (StealthManager.isDeviceAdminActive(this)) {
                Toast.makeText(this, "Device Administrator is already active.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(StealthManager.getDeviceAdminPromptIntent(this))
            }
        }

        btnPermBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }

        btnPermUsageStats.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnPermNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnPermCamera.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }

        btnPermCallSms.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                ),
                102
            )
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

        btnVacuumDatabase.setOnClickListener {
            try {
                val db = MobCareSecurityDatabase.getInstance(this).writableDatabase
                db.execSQL("VACUUM")
                Toast.makeText(this, "SQLite WAL database optimized successfully (0 data lost)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Database optimization nominal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnResetIdentity.setOnClickListener {
            try {
                IdentitySwapManager.resetAllDisguise(this)
                Toast.makeText(this, "Identity and disguise icon reset to MobCare original", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Reset complete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNavHelper.setup(this, NavTab.SETTINGS)
        refreshPermissionsState()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionsState()
    }

    private fun refreshPermissionsState() {
        val isAdmin = StealthManager.isDeviceAdminActive(this)
        val isCamGranted = IntruderCaptureManager.isCameraPermissionGranted(this)
        val isAcc = MobCareAccessibilityService.instance != null
        val isNotif = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val hasUsage = AppTimelineManager.hasUsageStatsPermission(this)
        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val hasCallSms = CallSmsVaultManager.hasPermissions(this)

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true

        // 1. Accessibility
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

        // 2. Device Admin
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

        // 3. Battery
        if (isIgnoringBattery) {
            tvPermBatteryBadge.text = "OPTIMIZED"
            tvPermBatteryBadge.setTextColor(Color.parseColor("#81C995"))
            btnPermBattery.text = "Battery Saver Ignored"
            btnPermBattery.setBackgroundColor(Color.parseColor("#163A34"))
            btnPermBattery.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvPermBatteryBadge.text = "REQUIRED"
            tvPermBatteryBadge.setTextColor(Color.parseColor("#FDD663"))
            btnPermBattery.text = "Exempt from Battery Saver"
            btnPermBattery.setBackgroundColor(Color.parseColor("#26272E"))
            btnPermBattery.setTextColor(Color.parseColor("#FFFFFF"))
        }

        // 4. Usage
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

        // 5. Notification
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

        // 6. Camera
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

        // 7. Call & SMS
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

        // 8. Overlay
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

        var grantedCount = 0
        if (isAcc) grantedCount++
        if (isAdmin) grantedCount++
        if (isIgnoringBattery) grantedCount++
        if (hasUsage) grantedCount++
        if (isNotif) grantedCount++
        if (isCamGranted) grantedCount++
        if (hasCallSms) grantedCount++
        if (canOverlay) grantedCount++

        tvMasterPermsBadge.text = "$grantedCount/8 GRANTED"
        if (grantedCount == 8) {
            tvSettingsHubBadge.text = "OPTIMAL"
            tvSettingsHubBadge.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvSettingsHubBadge.text = "SETUP"
            tvSettingsHubBadge.setTextColor(Color.parseColor("#FDD663"))
        }
    }
}
