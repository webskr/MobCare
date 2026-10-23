package com.mobcare.security

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.mobcare.R
import com.mobcare.audit.AuditLogManager

class AppLockGateActivity : Activity() {

    private lateinit var ivLockedAppIcon: ImageView
    private lateinit var tvLockedAppName: TextView
    private lateinit var etGatePin: EditText
    private lateinit var btnGateUnlock: Button
    private lateinit var btnGateExit: Button

    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock_gate)

        targetPackage = intent.getStringExtra("EXTRA_TARGET_PACKAGE") ?: ""

        ivLockedAppIcon = findViewById(R.id.ivLockedAppIcon)
        tvLockedAppName = findViewById(R.id.tvLockedAppName)
        etGatePin = findViewById(R.id.etGatePin)
        btnGateUnlock = findViewById(R.id.btnGateUnlock)
        btnGateExit = findViewById(R.id.btnGateExit)

        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(targetPackage, 0)
            tvLockedAppName.text = pm.getApplicationLabel(appInfo)
            ivLockedAppIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (e: Exception) {
            tvLockedAppName.text = "Protected Application"
        }

        btnGateUnlock.setOnClickListener {
            val entered = etGatePin.text.toString().trim()
            if (AppLockerManager.verifyPin(this, entered)) {
                AppLockerManager.unlockPackageForSession(targetPackage)

                // Launch target application directly so user enters smoothly into the app!
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        startActivity(launchIntent)
                    }
                } catch (ignored: Exception) {}

                finish()
            } else {
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

        btnGateExit.setOnClickListener {
            goToHomeScreen()
        }
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
        finish()
    }
}