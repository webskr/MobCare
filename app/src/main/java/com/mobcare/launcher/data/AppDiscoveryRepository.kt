package com.mobcare.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.mobcare.R
import com.mobcare.launcher.model.LauncherAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppDiscoveryRepository(private val context: Context) {

    /**
     * Query all launchable apps asynchronously, applying Identity Swap & cloaking rules.
     */
    suspend fun loadLauncherApps(): List<LauncherAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val activeSwap = IdentitySwapManager.getActiveSwap(context)

        val resultList = mutableListOf<LauncherAppItem>()
        var foundMobCare = false

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            val cls = info.activityInfo.name

            // Check if this package is the target app and should be hidden
            if (activeSwap != null && activeSwap.isActive && activeSwap.isOriginalHidden && pkg == activeSwap.targetPackageName) {
                // Skip the original target app so it is completely hidden from the launcher
                continue
            }

            if (pkg == context.packageName) {
                foundMobCare = true
                // We will handle MobCare separately below based on active swap
                continue
            }

            val label = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }

            val icon = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
                    ?: ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
            }

            val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val launchIntent = pm.getLaunchIntentForPackage(pkg)

            resultList.add(
                LauncherAppItem(
                    packageName = pkg,
                    className = cls,
                    label = label,
                    icon = icon,
                    isMobCareIdentity = false,
                    isSystemApp = isSystem,
                    launchIntent = launchIntent
                )
            )
        }

        // Now inject MobCare's representation:
        if (activeSwap != null && activeSwap.isActive) {
            // MobCare appears disguised with custom label and cloned/custom icon!
            val customBitmap = activeSwap.customIconFileName?.let {
                IdentitySwapManager.getCustomIconBitmap(context, it)
            }
            val displayIcon = if (customBitmap != null) {
                BitmapDrawable(context.resources, customBitmap)
            } else {
                // Fallback to target app icon if cached or default
                try {
                    pm.getApplicationIcon(activeSwap.targetPackageName)
                } catch (e: Exception) {
                    ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
                }
            }

            resultList.add(
                LauncherAppItem(
                    packageName = context.packageName,
                    className = "com.mobcare.dashboard.MainActivity",
                    label = activeSwap.customLabel,
                    icon = displayIcon,
                    isMobCareIdentity = true,
                    isSystemApp = false,
                    launchIntent = null // Special handling on tap!
                )
            )
        } else {
            // Normal MobCare launcher item
            val mobCareIcon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
            resultList.add(
                LauncherAppItem(
                    packageName = context.packageName,
                    className = "com.mobcare.dashboard.MainActivity",
                    label = context.getString(R.string.app_name),
                    icon = mobCareIcon,
                    isMobCareIdentity = true,
                    isSystemApp = false,
                    launchIntent = null
                )
            )
        }

        // Sort alphabetically by label
        resultList.sortBy { it.label.lowercase() }
        resultList
    }

    /**
     * Get a raw list of installed third-party apps for the Identity Swap selector screen.
     */
    suspend fun getAvailableSwapTargets(): List<LauncherAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val list = mutableListOf<LauncherAppItem>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue // Cannot swap with ourselves

            val label = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }

            val icon = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
            }

            val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            list.add(
                LauncherAppItem(
                    packageName = pkg,
                    className = info.activityInfo.name,
                    label = label,
                    icon = icon,
                    isMobCareIdentity = false,
                    isSystemApp = isSystem,
                    launchIntent = pm.getLaunchIntentForPackage(pkg)
                )
            )
        }
        list.sortBy { it.label.lowercase() }
        list
    }
}
