package com.mobcare.audit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationRecord(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: String
)

object NotificationVaultManager {

    @Synchronized
    fun saveNotification(context: Context, packageName: String, title: String, text: String) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val appName = AuditLogManager.getFriendlyAppName(context, packageName)

        try {
            MobCareSecurityDatabase.getInstance(context).insertNotification(
                pkg = packageName,
                appName = appName,
                title = title,
                text = text,
                category = "INTERCEPTED",
                timeFormatted = timeStr
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNotifications(context: Context): List<NotificationRecord> {
        return try {
            MobCareSecurityDatabase.getInstance(context).getAllNotifications()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearNotifications(context: Context) {
        try {
            MobCareSecurityDatabase.getInstance(context).clearNotifications()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}