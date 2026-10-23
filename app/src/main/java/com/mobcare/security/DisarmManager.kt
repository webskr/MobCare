package com.mobcare.security

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * DisarmManager
 * Manages the "Secret Handshake" state machine.
 * Starts a 20-second countdown upon screen unlock.
 * If not disarmed by opening the designated secret app, it activates the Decoy Trap.
 */
object DisarmManager {

    private val TAG = "DisarmManager"
    private const val DISARM_WINDOW_MS = 20_000L // 20 seconds to disarm
    private var isDisarmed = false
    private val handler = Handler(Looper.getMainLooper())

    private val triggerTrapRunnable = Runnable {
        if (!isDisarmed) {
            Log.w(TAG, "Disarm timeout expired! Launching Honeypot Decoy Trap.")
            launchDecoyTrap()
        }
    }

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun onDeviceUnlocked() {
        isDisarmed = false
        Log.i(TAG, "Device unlocked. Starting 20s Disarm Window...")
        handler.removeCallbacks(triggerTrapRunnable)
        handler.postDelayed(triggerTrapRunnable, DISARM_WINDOW_MS)
    }

    fun onSecretHandshakeCompleted() {
        isDisarmed = true
        handler.removeCallbacks(triggerTrapRunnable)
        Log.i(TAG, "Secret Handshake verified! Device armed state disarmed successfully.")
    }

    private fun launchDecoyTrap() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, DecoyCaptchaActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            ctx.startActivity(intent)
        }
    }
}