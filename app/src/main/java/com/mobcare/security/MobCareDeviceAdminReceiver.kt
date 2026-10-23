package com.mobcare.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobcare.audit.AuditLogManager

class MobCareDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("MobCareAdmin", "Device Admin protection enabled.")
        AuditLogManager.logSystemEvent(
            context,
            context.packageName,
            "MobCare Security",
            "[Anti-Uninstall Armed] Device Administrator activated. App is protected against unauthorized uninstallation."
        )
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        AuditLogManager.logSystemEvent(
            context,
            context.packageName,
            "MobCare Security",
            "[Tamper Attempt] Unauthorized attempt to deactivate Device Administrator!"
        )
        SentinelLauncher.launchLockscreenTrampoline(context, "Admin Deactivation Attempt")
        return "Warning: Deactivating Device Administrator will compromise device telemetry and security protection."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("MobCareAdmin", "Device Admin protection disabled.")
        AuditLogManager.logSystemEvent(
            context,
            context.packageName,
            "MobCare Security",
            "[Security Warning] Device Administrator was deactivated."
        )
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.w("MobCareAdmin", "Lock screen password attempt failed!")
        AuditLogManager.logSystemEvent(
            context,
            context.packageName,
            "Intruder Guard",
            "[Lock Screen Intruder] Incorrect PIN/Pattern entered on device lock screen!"
        )
        SentinelLauncher.launchLockscreenTrampoline(context, "LockScreen Failed Pattern/PIN")
        AppLockerManager.armIntruderLock(context)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.i("MobCareAdmin", "Lock screen password entered correctly.")
    }
}
