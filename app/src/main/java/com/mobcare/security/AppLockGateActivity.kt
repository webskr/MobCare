package com.mobcare.security

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.mobcare.R
import com.mobcare.audit.AuditLogManager

class AppLockGateActivity : Activity() {

    private lateinit var layoutPinGateContent: LinearLayout
    private lateinit var ivLockedAppIcon: ImageView
    private lateinit var tvLockedAppName: TextView
    private lateinit var etGatePin: EditText
    private lateinit var btnGateUnlock: Button
    private lateinit var btnGateExit: Button

    private var targetPackage: String = ""
    private var appLabel: CharSequence = "Protected Application"
    private var appIcon: Drawable? = null
    private var crashDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock_gate)

        targetPackage = intent.getStringExtra("EXTRA_TARGET_PACKAGE") ?: ""

        layoutPinGateContent = findViewById(R.id.layoutPinGateContent)
        ivLockedAppIcon = findViewById(R.id.ivLockedAppIcon)
        tvLockedAppName = findViewById(R.id.tvLockedAppName)
        etGatePin = findViewById(R.id.etGatePin)
        btnGateUnlock = findViewById(R.id.btnGateUnlock)
        btnGateExit = findViewById(R.id.btnGateExit)

        setupTargetApp()

        if (AppLockerManager.isAppCrashMode(this)) {
            layoutPinGateContent.visibility = View.INVISIBLE
            showSystemCrashDialog()
        } else {
            layoutPinGateContent.visibility = View.VISIBLE
        }

        btnGateUnlock.setOnClickListener {
            handlePinAttempt()
        }

        btnGateExit.setOnClickListener {
            goToHomeScreen()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetPackage = intent?.getStringExtra("EXTRA_TARGET_PACKAGE") ?: ""
        setupTargetApp()
        if (AppLockerManager.isAppCrashMode(this)) {
            layoutPinGateContent.visibility = View.INVISIBLE
            showSystemCrashDialog()
        } else {
            layoutPinGateContent.visibility = View.VISIBLE
        }
    }

    private fun setupTargetApp() {
        if (targetPackage.isNotEmpty()) {
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(targetPackage, 0)
                appLabel = pm.getApplicationLabel(appInfo)
                appIcon = pm.getApplicationIcon(appInfo)
                tvLockedAppName.text = appLabel
                ivLockedAppIcon.setImageDrawable(appIcon)
            } catch (e: Exception) {
                tvLockedAppName.text = "Protected Application"
            }
        }
    }

    private fun handlePinAttempt() {
        val entered = etGatePin.text.toString().trim()
        val status = AppLockerManager.verifyPinWithDecoy(this, entered)

        when (status) {
            PinVerifyStatus.PRIMARY_SUCCESS -> {
                AppLockerManager.unlockPackageForSession(targetPackage)
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        startActivity(launchIntent)
                    }
                } catch (ignored: Exception) {}
                finish()
            }

            PinVerifyStatus.DECOY_CRASH -> {
                // Capture intruder photo silently
                AuditLogManager.logSystemEvent(
                    this,
                    targetPackage,
                    "App Locker Decoy",
                    "[Decoy Panic] Decoy PIN entered for $targetPackage. Triggering simulated Android system crash."
                )
                IntruderCaptureManager.captureIntruderPhoto(this, "Decoy PIN Trap: $targetPackage")

                // Immediately trigger fake system crash dialog
                showSystemCrashDialog()
            }

            PinVerifyStatus.FAILED -> {
                AuditLogManager.logSystemEvent(
                    this,
                    targetPackage,
                    "App Locker",
                    "[Security Alert] Incorrect PIN entered for locked app: $targetPackage"
                )
                IntruderCaptureManager.captureIntruderPhoto(this, "App Locker Failed: $targetPackage")
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                etGatePin.text.clear()
            }
        }
    }

    private fun showSystemCrashDialog() {
        crashDialog?.dismiss()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_app_crash)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val ivCrashAppIcon = dialog.findViewById<ImageView>(R.id.ivCrashAppIcon)
        val tvCrashTitle = dialog.findViewById<TextView>(R.id.tvCrashTitle)
        val rowCrashClose = dialog.findViewById<LinearLayout>(R.id.rowCrashClose)
        val rowCrashFeedback = dialog.findViewById<LinearLayout>(R.id.rowCrashFeedback)

        if (appIcon != null) {
            ivCrashAppIcon.setImageDrawable(appIcon)
        }
        tvCrashTitle.text = "$appLabel keeps stopping"

        // Regular click: exit immediately to home screen like real crash
        rowCrashClose.setOnClickListener {
            dialog.dismiss()
            goToHomeScreen()
        }

        rowCrashFeedback.setOnClickListener {
            Toast.makeText(this, "Thank you for your feedback", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            goToHomeScreen()
        }

        // Secret Owner Bypass: Long click on "Close app" or triple-tap title reveals genuine PIN gate
        rowCrashClose.setOnLongClickListener {
            dialog.dismiss()
            layoutPinGateContent.visibility = View.VISIBLE
            Toast.makeText(this, "Owner Security Gate Revealed", Toast.LENGTH_SHORT).show()
            true
        }

        var tapCount = 0
        tvCrashTitle.setOnClickListener {
            tapCount++
            if (tapCount >= 3) {
                dialog.dismiss()
                layoutPinGateContent.visibility = View.VISIBLE
                Toast.makeText(this, "Owner Security Gate Revealed", Toast.LENGTH_SHORT).show()
            }
        }

        crashDialog = dialog
        dialog.show()
    }

    override fun onBackPressed() {
        goToHomeScreen()
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        crashDialog?.dismiss()
    }
}