package com.mobcare.security

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

class LockscreenTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val reason = intent.getStringExtra("EXTRA_REASON") ?: "Lockscreen Attempt Failed"

        // Execute foreground silent camera capture
        IntruderCaptureManager.captureIntruderPhoto(this, reason)

        // Exit immediately after capture command is dispatched
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 350)
    }
}
