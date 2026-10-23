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

    private const val FILE_NAME = "mobcare_notifications.json"

    @Synchronized
    fun saveNotification(context: Context, packageName: String, title: String, text: String) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val appName = AuditLogManager.getFriendlyAppName(context, packageName)

        val obj = JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("packageName", packageName)
            put("appName", appName)
            put("title", title)
            put("text", text)
            put("timestamp", timeStr)
        }

        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonArray = if (file.exists()) {
                val txt = file.readText()
                if (txt.isNotEmpty()) JSONArray(txt) else JSONArray()
            } else {
                JSONArray()
            }

            val updated = JSONArray()
            updated.put(obj)
            for (i in 0 until jsonArray.length()) {
                if (i < 200) {
                    updated.put(jsonArray.get(i))
                }
            }
            file.writeText(updated.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNotifications(context: Context): List<NotificationRecord> {
        val list = mutableListOf<NotificationRecord>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return list
            val content = file.readText()
            if (content.isEmpty()) return list
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NotificationRecord(
                        id = obj.optString("id"),
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        title = obj.optString("title"),
                        text = obj.optString("text"),
                        timestamp = obj.optString("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearNotifications(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}