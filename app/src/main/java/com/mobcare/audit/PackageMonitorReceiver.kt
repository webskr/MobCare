package com.mobcare.audit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class PackageMonitorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action == null) return

        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        val appName = AuditLogManager.getFriendlyAppName(context, packageName)

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val source = detectInstallSource(context, packageName)
                val isSideload = source.contains("Sideload", ignoreCase = true)
                val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm:ss a", java.util.Locale.getDefault())
                val timeStr = sdf.format(java.util.Date())
                if (isReplacing) {
                    val content = "[App Updated] Version update installed"
                    AuditLogManager.logSystemEvent(context, packageName, appName, content)
                    MobCareSecurityDatabase.getInstance(context).insertPackageEvent(packageName, appName, "UPDATED", isSideload, timeStr)
                } else {
                    val content = "[New App Installed] Source: $source"
                    AuditLogManager.logSystemEvent(context, packageName, appName, content)
                    incrementPackageEventCount(context)
                    MobCareSecurityDatabase.getInstance(context).insertPackageEvent(packageName, appName, "INSTALLED", isSideload, timeStr)
                }
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                val source = detectInstallSource(context, packageName)
                val isSideload = source.contains("Sideload", ignoreCase = true)
                val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm:ss a", java.util.Locale.getDefault())
                val timeStr = sdf.format(java.util.Date())
                if (!isReplacing) {
                    val content = "[App Uninstalled] Application was deleted from device"
                    AuditLogManager.logSystemEvent(context, packageName, appName, content)
                    incrementPackageEventCount(context)
                    DeletedAppsVault.recordDeletedApp(context, packageName, appName, source)
                    MobCareSecurityDatabase.getInstance(context).insertPackageEvent(packageName, appName, "REMOVED", isSideload, timeStr)
                }
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                val content = "[App Replaced] Binary package replaced"
                AuditLogManager.logSystemEvent(context, packageName, appName, content)
            }
        }
    }

    private fun detectInstallSource(context: Context, pkg: String): String {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(pkg)
                val initiator = info.initiatingPackageName ?: info.installingPackageName
                when {
                    initiator == "com.android.vending" -> "Google Play Store"
                    initiator != null && initiator.contains("chrome") -> "Sideloaded APK (Chrome)"
                    initiator != null && initiator.contains("packageinstaller") -> "Sideloaded APK (File Manager)"
                    initiator != null -> "Sideloaded APK ($initiator)"
                    else -> "Direct APK Sideload"
                }
            } else {
                @Suppress("DEPRECATION")
                val installer = pm.getInstallerPackageName(pkg)
                if (installer == "com.android.vending") {
                    "Google Play Store"
                } else {
                    "Sideloaded APK"
                }
            }
        } catch (e: Exception) {
            "Unknown Source"
        }
    }

    private fun incrementPackageEventCount(context: Context) {
        val sp = context.getSharedPreferences("mobcare_package_guardian", Context.MODE_PRIVATE)
        val current = sp.getInt("package_events_count", 0)
        sp.edit().putInt("package_events_count", current + 1).apply()
    }
}