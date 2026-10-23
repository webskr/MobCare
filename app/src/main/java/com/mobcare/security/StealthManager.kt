package com.mobcare.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object StealthManager {

    private const val PREFS_NAME = "mobcare_stealth_prefs"
    private const val KEY_FAKE_CRASH = "key_fake_crash_enabled"
    private const val KEY_CAMOUFLAGE = "key_camouflage_enabled"
    private const val KEY_POST_UNLOCK_SENTINEL = "key_post_unlock_sentinel"
    private const val KEY_MASTER_PIN = "key_master_pin"
    private const val KEY_PANIC_PIN = "key_panic_pin"
    private const val DEFAULT_PIN = "1337"
    private const val DEFAULT_PANIC_PIN = "0000"

    // When true, dashboard renders in Duress Sandbox mode (empty/fake logs)
    var isDuressSandboxActive = false

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val component = ComponentName(context, MobCareDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(component)
    }

    fun getDeviceAdminPromptIntent(context: Context): Intent {
        val component = ComponentName(context, MobCareDeviceAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Device Administrator to lock uninstallation and monitor lock-screen security tampering."
            )
        }
    }

    fun isFakeCrashEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_FAKE_CRASH, false)
    }

    fun setFakeCrashEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_FAKE_CRASH, enabled).apply()
    }

    fun isCamouflageEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_CAMOUFLAGE, false)
    }

    fun setCamouflageEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_CAMOUFLAGE, enabled).apply()
    }

    fun isPostUnlockSentinelEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_POST_UNLOCK_SENTINEL, false)
    }

    fun setPostUnlockSentinelEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_POST_UNLOCK_SENTINEL, enabled).apply()
    }

    fun executeSafeLockdown(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (isDeviceAdminActive(context) && dpm != null) {
                dpm.lockNow()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getMasterPin(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_MASTER_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun setMasterPin(context: Context, newPin: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_MASTER_PIN, newPin).apply()
    }

    fun getPanicPin(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_PANIC_PIN, DEFAULT_PANIC_PIN) ?: DEFAULT_PANIC_PIN
    }

    fun setPanicPin(context: Context, newPin: String) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_PANIC_PIN, newPin).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        return pin == getMasterPin(context)
    }

    fun verifyPanicPin(context: Context, pin: String): Boolean {
        return pin == getPanicPin(context)
    }
}
