package com.mobcare.audit

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppTimelineItem(
    val packageName: String,
    val appName: String,
    val totalTimeForegroundMillis: Long,
    val lastTimeUsedMillis: Long,
    val formattedScreenTime: String,
    val formattedLastUsed: String,
    val keystrokeCount: Int,
    val auditLogs: List<AuditRecord>
)

object AppTimelineManager {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getAppTimeline(context: Context): List<AppTimelineItem> {
        val list = mutableListOf<AppTimelineItem>()
        val pm = context.packageManager

        // All captured keystroke logs grouped by package
        val allLogs = AuditLogManager.getLogs(context)
        val logsByPackage = allLogs.groupBy { it.packageName }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val usageMap = mutableMapOf<String, UsageStats>()

        if (usageStatsManager != null && hasUsageStatsPermission(context)) {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -2) // Past 48 hours
            val startTime = calendar.timeInMillis

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (stats != null) {
                for (stat in stats) {
                    val existing = usageMap[stat.packageName]
                    if (existing == null || stat.lastTimeUsed > existing.lastTimeUsed) {
                        usageMap[stat.packageName] = stat
                    }
                }
            }
        }

        // Combine packages from usageStats AND packages that have captured keystroke logs
        val allPackages = (usageMap.keys + logsByPackage.keys).toSet()

        val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

        for (pkg in allPackages) {
            if (pkg == context.packageName) continue // Hide MobCare from its own timeline (zero-footprint)

            val stat = usageMap[pkg]
            val logs = logsByPackage[pkg] ?: emptyList()

            val foregroundMillis = stat?.totalTimeInForeground ?: 0L
            val lastUsedMillis = stat?.lastTimeUsed ?: 0L

            // Only include if app was either used or has keystrokes logged
            if (foregroundMillis == 0L && logs.isEmpty() && lastUsedMillis == 0L) continue

            val friendlyName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }

            val screenTimeFormatted = formatDuration(foregroundMillis)
            val lastUsedFormatted = if (lastUsedMillis > 0) timeFormat.format(Date(lastUsedMillis)) else "Recent session"

            list.add(
                AppTimelineItem(
                    packageName = pkg,
                    appName = friendlyName,
                    totalTimeForegroundMillis = foregroundMillis,
                    lastTimeUsedMillis = lastUsedMillis,
                    formattedScreenTime = screenTimeFormatted,
                    formattedLastUsed = lastUsedFormatted,
                    keystrokeCount = logs.size,
                    auditLogs = logs
                )
            )
        }

        // Sort by most recently used first, then by screen time
        list.sortWith(compareByDescending<AppTimelineItem> { it.lastTimeUsedMillis }
            .thenByDescending { it.keystrokeCount }
            .thenByDescending { it.totalTimeForegroundMillis })

        return list
    }

    private fun formatDuration(millis: Long): String {
        if (millis <= 0) return "< 1m"
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}
