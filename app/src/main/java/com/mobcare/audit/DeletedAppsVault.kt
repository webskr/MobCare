package com.mobcare.audit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeletedAppRecord(
    val packageName: String,
    val appName: String,
    val deletedTimestamp: String,
    val lastSource: String
)

object DeletedAppsVault {

    private const val FILE_NAME = "deleted_apps_vault.json"

    fun recordDeletedApp(context: Context, packageName: String, appName: String, source: String = "Unknown") {
        if (packageName == context.packageName) return // Exclude MobCare from self-logging

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date())

        val record = JSONObject().apply {
            put("packageName", packageName)
            put("appName", appName)
            put("deletedTimestamp", timeStr)
            put("lastSource", source)
        }

        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonArray = if (file.exists()) {
                val txt = file.readText()
                if (txt.isNotEmpty()) JSONArray(txt) else JSONArray()
            } else {
                JSONArray()
            }

            // Check if already in vault recently to avoid duplicate records
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("packageName") == packageName) {
                    return // Already recorded
                }
            }

            val updated = JSONArray()
            updated.put(record)
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

    fun syncBaseline(context: Context) {
        try {
            val pm = context.packageManager
            val sp = context.getSharedPreferences("mobcare_package_baseline", Context.MODE_PRIVATE)

            // Current installed packages (excluding MobCare itself)
            val currentInstalled = pm.getInstalledPackages(0)
                .map { it.packageName }
                .filter { it != context.packageName }
                .toSet()

            val savedBaseline = sp.getStringSet("installed_packages_set", null)

            if (savedBaseline == null) {
                // Initialize baseline
                sp.edit().putStringSet("installed_packages_set", currentInstalled).apply()
                return
            }

            // Find apps that existed previously but are now missing
            val missingPackages = savedBaseline - currentInstalled
            if (missingPackages.isNotEmpty()) {
                for (pkg in missingPackages) {
                    val fallbackName = pkg.substringAfterLast(".").replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    }
                    recordDeletedApp(context, pkg, fallbackName, "Uninstalled from device")
                }
                sp.edit().putStringSet("installed_packages_set", currentInstalled).apply()
            } else {
                val newPackages = currentInstalled - savedBaseline
                if (newPackages.isNotEmpty()) {
                    sp.edit().putStringSet("installed_packages_set", currentInstalled).apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDeletedApps(context: Context): List<DeletedAppRecord> {
        val list = mutableListOf<DeletedAppRecord>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return list
            val content = file.readText()
            if (content.isEmpty()) return list
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkg = obj.optString("packageName")
                if (pkg != context.packageName) {
                    list.add(
                        DeletedAppRecord(
                            packageName = pkg,
                            appName = obj.optString("appName"),
                            deletedTimestamp = obj.optString("deletedTimestamp"),
                            lastSource = obj.optString("lastSource", "Unknown")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearVault(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
