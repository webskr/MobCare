package com.mobcare.launcher.model

import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * Represents an app item displayed on MobCare's Launcher Home & App Drawer.
 */
data class LauncherAppItem(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable,
    val isMobCareIdentity: Boolean = false,
    val isSystemApp: Boolean = false,
    val launchIntent: Intent? = null
)
