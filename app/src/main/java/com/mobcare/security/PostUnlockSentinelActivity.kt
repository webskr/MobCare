package com.mobcare.security

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.mobcare.R

class PostUnlockSentinelActivity : Activity() {

    private lateinit var tvScannerStatus: TextView
    private lateinit var btnSentinelDismiss: Button
    private lateinit var btnSentinelLockdown: Button

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_unlock_sentinel)

        tvScannerStatus = findViewById(R.id.tvScannerStatus)
        btnSentinelDismiss = findViewById(R.id.btnSentinelDismiss)
        btnSentinelLockdown = findViewById(R.id.btnSentinelLockdown)

        // Silently snap front-camera face of whoever just unlocked the phone
        IntruderCaptureManager.captureIntruderPhoto(this, "Post-Unlock Face Verification")

        // 3-Second Mandatory Verification Countdown
        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000) + 1
                btnSentinelDismiss.text = "Verifying (${secondsRemaining}s)..."
                btnSentinelDismiss.isEnabled = false
            }

            override fun onFinish() {
                tvScannerStatus.text = "Identity Verified"
                tvScannerStatus.setTextColor(Color.parseColor("#81C995"))

                btnSentinelDismiss.text = "Dismiss / Continue"
                btnSentinelDismiss.isEnabled = true
                btnSentinelDismiss.setBackgroundColor(Color.parseColor("#163A34"))
                btnSentinelDismiss.setTextColor(Color.parseColor("#81C995"))
            }
        }.start()

        btnSentinelDismiss.setOnClickListener {
            finish()
        }

        btnSentinelLockdown.setOnClickListener {
            val locked = StealthManager.executeSafeLockdown(this)
            if (locked) {
                finish()
            } else {
                Toast.makeText(this, "Device Administrator required for instant lockdown.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back-button bypass during the 3-second face capture window
        if (btnSentinelDismiss.isEnabled) {
            super.onBackPressed()
        }
    }
}
