package com.mobcare.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UnlockSentinelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            if (StealthManager.isPostUnlockSentinelEnabled(context)) {
                SentinelLauncher.launchPostUnlockSentinel(context)
            }
        }
    }
}
