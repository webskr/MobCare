package com.mobcare.security

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mobcare.audit.AuditLogManager
import com.mobcare.audit.BrowserVaultManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MobCareAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AuditLogManager.logSystemEvent(
            this,
            packageName,
            "MobCare Sentinel",
            "[Accessibility Active] Full system sentinel and URL interception armed."
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // 1. Instant 0ms App Locker Interceptor
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != packageName && AppLockerManager.isPackageLocked(this, pkg)) {
                android.util.Log.d("MobCareSentinel", "LOCKED APP INTERCEPTED: $pkg -> Opening AppLockGateActivity")
                val gateIntent = Intent(this, AppLockGateActivity::class.java).apply {
                    putExtra("EXTRA_TARGET_PACKAGE", pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(gateIntent)
                return
            }
        }

        // 2. Web Intelligence / Browser URL Interceptor
        if (isBrowserPackage(pkg)) {
            inspectBrowserUrl(pkg)
        }
    }

    private fun isBrowserPackage(pkg: String): Boolean {
        return pkg.contains("chrome", true) ||
               pkg.contains("brave", true) ||
               pkg.contains("browser", true) ||
               pkg.contains("firefox", true) ||
               pkg.contains("opera", true) ||
               pkg.contains("edge", true)
    }

    private fun inspectBrowserUrl(pkg: String) {
        val root = rootInActiveWindow ?: return
        val urlNodes = mutableListOf<AccessibilityNodeInfo>()

        // Common omnibox / URL address bar IDs
        val targetIds = listOf(
            "$pkg:id/url_bar",
            "$pkg:id/omnibox_text",
            "$pkg:id/location_bar_edit_text",
            "$pkg:id/search_box",
            "$pkg:id/toolbar"
        )

        for (id in targetIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                urlNodes.addAll(nodes)
                break
            }
        }

        for (node in urlNodes) {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty() && (text.contains(".") || text.contains("/") || text.startsWith("http"))) {
                val domain = extractDomain(text)
                val appLabel = if (pkg.contains("chrome")) "Chrome" else if (pkg.contains("brave")) "Brave" else "Browser"
                BrowserVaultManager.saveWebVisit(this, domain, text, "Web Session", appLabel)
                break
            }
        }
    }

    private fun extractDomain(url: String): String {
        var clean = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        val slashIdx = clean.indexOf('/')
        if (slashIdx != -1) {
            clean = clean.substring(0, slashIdx)
        }
        return clean
    }

    override fun onInterrupt() {}

    companion object {
        var instance: MobCareAccessibilityService? = null

        fun takeSilentScreenshot(context: Context, callback: (Boolean, String?) -> Unit) {
            val service = instance
            if (service == null) {
                callback(false, "Accessibility Service is not active")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    service.takeScreenshot(
                        Display.DEFAULT_DISPLAY,
                        context.mainExecutor,
                        object : TakeScreenshotCallback {
                            override fun onSuccess(screenshotResult: ScreenshotResult) {
                                try {
                                    val hardwareBuffer = screenshotResult.hardwareBuffer
                                    val colorSpace = screenshotResult.colorSpace
                                    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                    if (bitmap != null) {
                                        val copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                        val path = saveScreenshot(context, copyBitmap)
                                        hardwareBuffer.close()
                                        callback(true, path)
                                        return
                                    }
                                    hardwareBuffer.close()
                                    callback(false, "Failed to capture buffer")
                                } catch (e: Exception) {
                                    callback(false, e.message)
                                }
                            }

                            override fun onFailure(errorCode: Int) {
                                callback(false, "Error code: $errorCode")
                            }
                        }
                    )
                } catch (e: Exception) {
                    callback(false, e.message)
                }
            } else {
                callback(false, "Silent screenshot requires Android 11+")
            }
        }

        private fun saveScreenshot(context: Context, bitmap: Bitmap): String {
            val dir = File(context.filesDir, "screenshots").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "SCREEN_$timeStamp.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            return file.absolutePath
        }
    }
}