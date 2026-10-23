package com.mobcare.audit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClipboardRecord(
    val id: String,
    val content: String,
    val timestamp: String
)

object ClipboardVaultManager {

    private const val FILE_NAME = "mobcare_clipboard.json"

    @Synchronized
    fun saveClipboard(context: Context, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
        val timeStr = sdf.format(Date())

        val obj = JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("content", trimmed)
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

            // Deduplicate if identical to latest
            if (jsonArray.length() > 0) {
                val latest = jsonArray.getJSONObject(0).optString("content")
                if (latest == trimmed) return
            }

            val updated = JSONArray()
            updated.put(obj)
            for (i in 0 until jsonArray.length()) {
                if (i < 100) {
                    updated.put(jsonArray.get(i))
                }
            }
            file.writeText(updated.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getClipboardHistory(context: Context): List<ClipboardRecord> {
        val list = mutableListOf<ClipboardRecord>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return list
            val content = file.readText()
            if (content.isEmpty()) return list
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ClipboardRecord(
                        id = obj.optString("id"),
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

    fun clearClipboard(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}