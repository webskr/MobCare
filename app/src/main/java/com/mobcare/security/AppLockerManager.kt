package com.mobcare.security

import android.content.Context
import android.content.SharedPreferences

object AppLockerManager {

    private const val PREF_NAME = "mobcare_app_locker"
    private const val KEY_LOCKED_PACKAGES = "locked_packages"
    private const val KEY_LOCK_MODE = "lock_mode"
    private const val KEY_IS_ARMED = "is_armed"
    private const val KEY_CUSTOM_PIN = "custom_app_locker_pin"

    const val MODE_ALWAYS_LOCKED = "ALWAYS_LOCKED"
    const val MODE_INTRUDER_TRIGGERED = "INTRUDER_TRIGGERED"

    private val temporaryUnlockedPackages = mutableSetOf<String>()
    private var isIntruderLockArmed = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getLockedPackages(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun isPackageLocked(context: Context, packageName: String): Boolean {
        val lockedList = getLockedPackages(context)
        if (!lockedList.contains(packageName)) return false

        // Check if currently unlocked in this session
        if (temporaryUnlockedPackages.contains(packageName)) return false

        val mode = getLockMode(context)
        return if (mode == MODE_ALWAYS_LOCKED) {
            true
        } else {
            isIntruderLockArmed || getPrefs(context).getBoolean(KEY_IS_ARMED, false)
        }
    }

    fun setPackageLocked(context: Context, packageName: String, locked: Boolean) {
        val current = getLockedPackages(context).toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            temporaryUnlockedPackages.remove(packageName)
        }
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    fun getLockMode(context: Context): String {
        return getPrefs(context).getString(KEY_LOCK_MODE, MODE_ALWAYS_LOCKED) ?: MODE_ALWAYS_LOCKED
    }

    fun setLockMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_LOCK_MODE, mode).apply()
    }

    fun getCustomPin(context: Context): String? {
        val pin = getPrefs(context).getString(KEY_CUSTOM_PIN, null)
        return if (!pin.isNullOrEmpty()) pin else null
    }

    fun setCustomPin(context: Context, pin: String?) {
        getPrefs(context).edit().putString(KEY_CUSTOM_PIN, pin).apply()
    }

    fun verifyPin(context: Context, entered: String): Boolean {
        val customPin = getCustomPin(context)
        if (customPin != null && entered == customPin) {
            return true
        }
        return StealthManager.verifyPin(context, entered) || StealthManager.verifyPanicPin(context, entered)
    }

    fun armIntruderLock(context: Context) {
        isIntruderLockArmed = true
        temporaryUnlockedPackages.clear()
        getPrefs(context).edit().putBoolean(KEY_IS_ARMED, true).apply()
    }

    fun disarmIntruderLock(context: Context) {
        isIntruderLockArmed = false
        getPrefs(context).edit().putBoolean(KEY_IS_ARMED, false).apply()
    }

    fun unlockPackageForSession(packageName: String) {
        temporaryUnlockedPackages.add(packageName)
    }

    fun clearSessionUnlocks() {
        temporaryUnlockedPackages.clear()
    }
}