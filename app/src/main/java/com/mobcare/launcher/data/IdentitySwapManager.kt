package com.mobcare.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.mobcare.launcher.model.IdentitySwapModel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object IdentitySwapManager {

    private const val PREFS_NAME           = "mobcare_identity_swap_prefs"
    private const val KEY_ACTIVE_SWAP      = "key_active_swap_json"
    private const val KEY_CUSTOM_ICON      = "key_custom_icon_file"
    private const val KEY_CUSTOM_NAME      = "key_custom_app_name"
    private const val KEY_CURRENT_ICON_KEY = "key_current_icon_key"
    private const val KEY_CURRENT_NAME_KEY = "key_current_name_key"
    private const val KEY_ENABLED_ALIAS    = "key_enabled_alias"
    private const val ICONS_DIR            = "identity_icons"

    val ICONS = listOf("default", "browser", "clock", "compass", "files", "google_one", "radio", "secure", "setting", "weather")
    val NAMES = listOf("default", "browser", "clock", "compass", "files", "google_one", "radio", "secure", "setting", "weather")

    val PRESET_ITEMS = listOf(
        Triple("default",    "MobCare",    "logo.png"),
        Triple("browser",    "Browser",    "Browser.jpg"),
        Triple("clock",      "Clock",      "Clock.jpg"),
        Triple("compass",    "Compass",    "Compass.jpg"),
        Triple("files",      "Files",      "File Manager.jpg"),
        Triple("google_one", "Google One", "Google one.png"),
        Triple("radio",      "Radio",      "Radio.jpg"),
        Triple("secure",     "Security",   "Secure.jpg"),
        Triple("setting",    "Settings",   "Setting.jpg"),
        Triple("weather",    "Weather",    "Weather.jpg")
    )

    fun getCurrentIconKey(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_CURRENT_ICON_KEY, "default") ?: "default"
    }

    fun getCurrentNameKey(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_CURRENT_NAME_KEY, "default") ?: "default"
    }

    fun getCurrentEnabledAlias(context: Context): String {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_ENABLED_ALIAS, "${context.packageName}.alias.I_default_N_default")
            ?: "${context.packageName}.alias.I_default_N_default"
    }

    fun getDisplayNameForKey(key: String): String = when (key) {
        "browser"    -> "Browser"
        "clock"      -> "Clock"
        "compass"    -> "Compass"
        "files"      -> "Files"
        "google_one" -> "Google One"
        "radio"      -> "Radio"
        "secure"     -> "Security"
        "setting"    -> "Settings"
        "weather"    -> "Weather"
        else         -> "MobCare"
    }

    fun getAssetFileForIconKey(key: String): String = when (key) {
        "browser"    -> "Browser.jpg"
        "clock"      -> "Clock.jpg"
        "compass"    -> "Compass.jpg"
        "files"      -> "File Manager.jpg"
        "google_one" -> "Google one.png"
        "radio"      -> "Radio.jpg"
        "secure"     -> "Secure.jpg"
        "setting"    -> "Setting.jpg"
        "weather"    -> "Weather.jpg"
        else         -> "logo.png"
    }

    fun assetFileNameToIconKey(fileName: String?): String = when (fileName) {
        "Browser.jpg"      -> "browser"
        "Clock.jpg"        -> "clock"
        "Compass.jpg"      -> "compass"
        "File Manager.jpg" -> "files"
        "Google one.png"   -> "google_one"
        "Radio.jpg"        -> "radio"
        "Secure.jpg"       -> "secure"
        "Setting.jpg"      -> "setting"
        "Weather.jpg"      -> "weather"
        else               -> "default"
    }

    fun nameToKey(name: String?): String = when (name?.trim()?.lowercase()) {
        "browser"                   -> "browser"
        "clock"                     -> "clock"
        "compass"                   -> "compass"
        "files", "file manager"     -> "files"
        "google one", "google_one"  -> "google_one"
        "radio"                     -> "radio"
        "secure", "security"        -> "secure"
        "settings", "setting"       -> "setting"
        "weather"                   -> "weather"
        else                        -> "default"
    }

    /**
     * Applies the system-level Activity Alias disguise.
     * This instantly changes the real App Icon and App Name in the Android App Drawer and Home Screen.
     */
    fun applyAliasDisguise(context: Context, iconKey: String, nameKey: String): Boolean {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val targetAlias = "$pkg.alias.I_${iconKey}_N_${nameKey}"

            // 1. Enable new target alias FIRST
            pm.setComponentEnabledSetting(
                ComponentName(pkg, targetAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other 99 aliases
            for (i in ICONS) {
                for (n in NAMES) {
                    val aliasName = "$pkg.alias.I_${i}_N_${n}"
                    if (aliasName != targetAlias) {
                        try {
                            pm.setComponentEnabledSetting(
                                ComponentName(pkg, aliasName),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP
                            )
                        } catch (_: Exception) {}
                    }
                }
            }

            // 3. Save state in prefs
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_ICON_KEY, iconKey)
                .putString(KEY_CURRENT_NAME_KEY, nameKey)
                .putString(KEY_ENABLED_ALIAS, targetAlias)
                .putString(KEY_CUSTOM_ICON, getAssetFileForIconKey(iconKey))
                .putString(KEY_CUSTOM_NAME, getDisplayNameForKey(nameKey))
                .apply()

            // Also keep active swap model in sync
            val label = getDisplayNameForKey(nameKey)
            val iconFile = getAssetFileForIconKey(iconKey)
            val isDisguised = (iconKey != "default" || nameKey != "default")
            setActiveSwap(context, IdentitySwapModel(
                targetPackageName  = iconKey,
                originalLabel      = "MobCare",
                customLabel        = label,
                iconType           = IdentitySwapModel.ICON_TYPE_PRESET,
                customIconFileName = iconFile,
                isOriginalHidden   = isDisguised,
                isActive           = isDisguised
            ))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Changes ONLY the icon — preserves whatever name is currently set. */
    fun updateIconOnly(context: Context, iconKey: String): Boolean {
        val currentNameKey = getCurrentNameKey(context)
        return applyAliasDisguise(context, iconKey, currentNameKey)
    }

    /** Changes ONLY the name — preserves whatever icon is currently set. */
    fun updateNameOnly(context: Context, nameKey: String): Boolean {
        val currentIconKey = getCurrentIconKey(context)
        return applyAliasDisguise(context, currentIconKey, nameKey)
    }

    /** Resets ONLY the icon to default MobCare logo — preserves name. */
    fun resetIconOnly(context: Context): Boolean {
        return updateIconOnly(context, "default")
    }

    /** Resets ONLY the name to default MobCare — preserves icon. */
    fun resetNameOnly(context: Context): Boolean {
        return updateNameOnly(context, "default")
    }

    /** Resets both logo and name to default MobCare. */
    fun resetAllDisguise(context: Context): Boolean {
        clearActiveSwap(context)
        return applyAliasDisguise(context, "default", "default")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core Swap model & Preferences
    // ─────────────────────────────────────────────────────────────────────────

    fun getActiveSwap(context: Context): IdentitySwapModel? {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_ACTIVE_SWAP, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val model = IdentitySwapModel.fromJson(json)
            if (model.isActive) model else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setActiveSwap(context: Context, swap: IdentitySwapModel) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_ACTIVE_SWAP, swap.toJson().toString()).apply()
    }

    fun clearActiveSwap(context: Context) {
        val active = getActiveSwap(context)
        active?.customIconFileName?.let { deleteCustomIcon(context, it) }
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .remove(KEY_ACTIVE_SWAP)
            .remove(KEY_CUSTOM_ICON)
            .remove(KEY_CUSTOM_NAME)
            .remove(KEY_CURRENT_ICON_KEY)
            .remove(KEY_CURRENT_NAME_KEY)
            .apply()
    }

    fun saveIndependentIcon(context: Context, iconFileName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_ICON, iconFileName).apply()
    }

    fun getIndependentIconFileName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_ICON, null)

    fun clearIndependentIcon(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CUSTOM_ICON).apply()
    }

    fun saveIndependentName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_NAME, name.trim()).apply()
    }

    fun getIndependentName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_NAME, null)?.takeIf { it.isNotBlank() }

    fun clearIndependentName(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CUSTOM_NAME).apply()
    }

    fun getEffectiveIconFileName(context: Context): String? =
        getIndependentIconFileName(context) ?: getActiveSwap(context)?.customIconFileName

    fun getEffectiveName(context: Context, defaultName: String): String =
        getIndependentName(context) ?: getActiveSwap(context)?.customLabel ?: defaultName

    fun isPackageHidden(context: Context, packageName: String): Boolean {
        val active = getActiveSwap(context) ?: return false
        return active.isOriginalHidden && active.targetPackageName == packageName
    }

    fun isCloakedAs(context: Context, packageName: String): Boolean {
        val active = getActiveSwap(context) ?: return false
        return active.targetPackageName == packageName
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Custom Icon File Storage (for pinned shortcuts / gallery)
    // ─────────────────────────────────────────────────────────────────────────

    fun saveCustomIconBitmap(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val dir = File(context.filesDir, ICONS_DIR)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveDrawableAsCustomIcon(context: Context, drawable: Drawable, fileName: String): Boolean {
        val bitmap = drawableToBitmap(drawable)
        return saveCustomIconBitmap(context, bitmap, fileName)
    }

    fun getCustomIconBitmap(context: Context, fileName: String): Bitmap? {
        val internalFile = File(File(context.filesDir, ICONS_DIR), fileName)
        if (internalFile.exists()) {
            return try { BitmapFactory.decodeFile(internalFile.absolutePath) } catch (e: Exception) { null }
        }
        return try {
            context.assets.open(fileName).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteCustomIcon(context: Context, fileName: String) {
        try {
            val file = File(File(context.filesDir, ICONS_DIR), fileName)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width  = if (drawable.intrinsicWidth  > 0) drawable.intrinsicWidth  else 144
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 144
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
