package com.mobcare.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AppLockerWatchdogService : Service() {

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var lastCheckedPackage: String = ""
    private var isRunning = false

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            checkForegroundApp()
            workerHandler?.postDelayed(this, 500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureForegroundNotification()
        val thread = HandlerThread("AppLockerWorker").apply { start() }
        workerThread = thread
        workerHandler = Handler(thread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            workerHandler?.post(checkRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        workerHandler?.removeCallbacks(checkRunnable)
        workerThread?.quitSafely()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val time = System.currentTimeMillis()
        val events = usm.queryEvents(time - 1500, time) ?: return
        val event = UsageEvents.Event()

        var currentPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentPackage = event.packageName
            }
        }

        if (currentPackage != null) {
            if (currentPackage == packageName || currentPackage.contains("launcher", true)) {
                lastCheckedPackage = ""
            } else if (currentPackage != lastCheckedPackage) {
                lastCheckedPackage = currentPackage
                if (AppLockerManager.isPackageLocked(this, currentPackage)) {
                    android.util.Log.d("AppLockerWatchdog", "WATCHDOG INTERCEPT: $currentPackage -> Starting gate")
                    val gateIntent = Intent(this, AppLockGateActivity::class.java).apply {
                        putExtra("EXTRA_TARGET_PACKAGE", currentPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    }
                    startActivity(gateIntent)
                }
            }
        }
    }

    private fun ensureForegroundNotification() {
        val channelId = "mobcare_watchdog_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sentinel Protection Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MobCare Sentinel")
            .setContentText("Application integrity active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    2001,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(2001, notification, 0)
            } else {
                startForeground(2001, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AppLockerWatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (ignored: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }
    }
}