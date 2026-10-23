package com.mobcare.audit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class WebVisitItem(
    val id: String,
    val domain: String,
    val url: String,
    val title: String,
    val browserApp: String,
    val timestamp: Long
)

object BrowserVaultManager {

    private const val FILE_NAME = "mobcare_browser_vault.json"

    @Synchronized
    fun getWebVisits(context: Context): List<WebVisitItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        val list = mutableListOf<WebVisitItem>()
        try {
            val jsonArray = JSONArray(file.readText(Charsets.UTF_8))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WebVisitItem(
                        id = obj.optString("id"),
                        domain = obj.optString("domain"),
                        url = obj.optString("url"),
                        title = obj.optString("title"),
                        browserApp = obj.optString("browserApp", "Chrome"),
                        timestamp = obj.optLong("timestamp", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun saveWebVisit(context: Context, domain: String, url: String, title: String, browserApp: String) {
        val list = getWebVisits(context).toMutableList()
        // Deduplicate recent visits to same URL within 10 seconds
        val now = System.currentTimeMillis()
        if (list.any { it.url == url && (now - it.timestamp) < 10000 }) {
            return
        }

        val newItem = WebVisitItem(
            id = java.util.UUID.randomUUID().toString(),
            domain = domain,
            url = url,
            title = title,
            browserApp = browserApp,
            timestamp = now
        )
        list.add(0, newItem)

        val jsonArray = JSONArray()
        list.take(500).forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("domain", v.domain)
                put("url", v.url)
                put("title", v.title)
                put("browserApp", v.browserApp)
                put("timestamp", v.timestamp)
            }
            jsonArray.put(obj)
        }
        File(context.filesDir, FILE_NAME).writeText(jsonArray.toString(), Charsets.UTF_8)
    }

    fun clearAll(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}