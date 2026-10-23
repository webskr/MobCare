package com.mobcare.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import com.mobcare.audit.CallSmsVaultManager

class CallVaultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended or missed: sync system call log with small delay
                Handler(Looper.getMainLooper()).postDelayed({
                    CallSmsVaultManager.syncSystemCallLog(context)
                }, 1200)
            }
        }
    }
}