package com.mobcare.audit

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MobCareNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        if (sbn.isOngoing) return

        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (title.isNotEmpty() || text.isNotEmpty()) {
            Log.i("MobCareNotif", "Captured notification from $pkg: $title - $text")
            NotificationVaultManager.saveNotification(this, pkg, title, text)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("MobCareNotif", "NotificationListenerService connected & active.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("MobCareNotif", "NotificationListenerService disconnected.")
    }
}