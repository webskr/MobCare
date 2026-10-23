package com.mobcare.dashboard

import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R
import com.mobcare.audit.DeletedAppRecord
import com.mobcare.audit.DeletedAppsVault
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackageGuardianActivity : AppCompatActivity() {

    private lateinit var tabInstalled: Button
    private lateinit var tabSystem: Button
    private lateinit var tabRecent: Button
    private lateinit var tabDeleted: Button
    private lateinit var tvAppCount: TextView
    private lateinit var appsListContainer: LinearLayout

    private var currentTab = 0 // 0: User, 1: System, 2: Recent, 3: Deleted

    // Preloaded in memory for 0ms tab switching
    private var cachedUserApps = listOf<PackageInfo>()
    private var cachedSystemApps = listOf<PackageInfo>()
    private var cachedRecentApps = listOf<PackageInfo>()

    // App Icon In-Memory Cache
    private val appIconCache = mutableMapOf<String, Drawable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_package_guardian)

        tabInstalled = findViewById(R.id.tabInstalled)
        tabSystem = findViewById(R.id.tabSystem)
        tabRecent = findViewById(R.id.tabRecent)
        tabDeleted = findViewById(R.id.tabDeleted)
        tvAppCount = findViewById(R.id.tvAppCount)
        appsListContainer = findViewById(R.id.appsListContainer)

        tabInstalled.setOnClickListener { switchTab(0) }
        tabSystem.setOnClickListener { switchTab(1) }
        tabRecent.setOnClickListener { switchTab(2) }
        tabDeleted.setOnClickListener { switchTab(3) }

        preloadPackagesInMemory()
    }

    private fun switchTab(tabIndex: Int) {
        currentTab = tabIndex
        updateTabStyles()
        renderCurrentTab()
    }

    private fun updateTabStyles() {
        val activeBg = Color.parseColor("#26272E")
        val activeText = Color.parseColor("#FFFFFF")
        val inactiveBg = Color.parseColor("#26272E")
        val inactiveText = Color.parseColor("#9AA0A6")

        val tabs = listOf(tabInstalled, tabSystem, tabRecent, tabDeleted)
        tabs.forEachIndexed { i, tab ->
            if (i == currentTab) {
                tab.setBackgroundColor(activeBg)
                tab.setTextColor(activeText)
            } else {
                tab.setBackgroundColor(inactiveBg)
                tab.setTextColor(inactiveText)
            }
        }
    }

    private fun preloadPackagesInMemory() {
        tvAppCount.text = "Syncing package baseline & loading apps..."
        Thread {
            try {
                // Sync baseline so missing apps are archived into DeletedAppsVault
                DeletedAppsVault.syncBaseline(this)

                val pm = packageManager
                val list = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

                val user = mutableListOf<PackageInfo>()
                val system = mutableListOf<PackageInfo>()

                for (pkg in list) {
                    if (pkg.packageName == packageName) continue // Hide MobCare from its own guardian
                    if ((pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                        user.add(pkg)
                    } else {
                        system.add(pkg)
                    }
                }

                user.sortBy { pm.getApplicationLabel(it.applicationInfo).toString().lowercase(Locale.ROOT) }
                system.sortBy { pm.getApplicationLabel(it.applicationInfo).toString().lowercase(Locale.ROOT) }
                val recent = list.filter { it.packageName != packageName }
                    .sortedByDescending { it.firstInstallTime }
                    .take(30)

                runOnUiThread {
                    cachedUserApps = user
                    cachedSystemApps = system
                    cachedRecentApps = recent
                    renderCurrentTab()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvAppCount.text = "Failed to load packages"
                }
            }
        }.start()
    }

    private fun renderCurrentTab() {
        appsListContainer.removeAllViews()

        when (currentTab) {
            0 -> {
                tvAppCount.text = "User Installed Apps: ${cachedUserApps.size}"
                cachedUserApps.forEach { pkgInfo ->
                    appsListContainer.addView(createAppRowView(pkgInfo))
                }
            }
            1 -> {
                tvAppCount.text = "Android System Apps: ${cachedSystemApps.size}"
                cachedSystemApps.forEach { pkgInfo ->
                    appsListContainer.addView(createAppRowView(pkgInfo))
                }
            }
            2 -> {
                tvAppCount.text = "Recently Installed & Updated: ${cachedRecentApps.size}"
                cachedRecentApps.forEach { pkgInfo ->
                    appsListContainer.addView(createAppRowView(pkgInfo))
                }
            }
            3 -> renderDeletedApps()
        }
    }

    private fun renderDeletedApps() {
        val deleted = DeletedAppsVault.getDeletedApps(this)
        tvAppCount.text = "Deleted Apps Vault: ${deleted.size} apps recorded"

        if (deleted.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No deleted applications recorded yet.\nWhen an app is uninstalled from this device, it will permanently be archived here."
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
            }
            appsListContainer.addView(emptyTv)
            return
        }

        deleted.forEach { record ->
            appsListContainer.addView(createDeletedAppRowView(record))
        }
    }

    private fun createAppRowView(pkgInfo: PackageInfo): View {
        val pm = packageManager
        val density = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { showAppMetadataDialog(pkgInfo) }
        }

        val iconIv = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt()).apply {
                marginEnd = (14 * density).toInt()
            }
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10f * density)
                }
            }
            clipToOutline = true
        }

        val cachedIcon = appIconCache[pkgInfo.packageName]
        if (cachedIcon != null) {
            iconIv.setImageDrawable(cachedIcon)
        } else {
            Thread {
                val icon = try { pm.getApplicationIcon(pkgInfo.applicationInfo) } catch (e: Exception) { null }
                if (icon != null) {
                    appIconCache[pkgInfo.packageName] = icon
                    runOnUiThread { iconIv.setImageDrawable(icon) }
                }
            }.start()
        }
        row.addView(iconIv)

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val appName = try { pm.getApplicationLabel(pkgInfo.applicationInfo).toString() } catch (e: Exception) { pkgInfo.packageName }
        val nameTv = TextView(this).apply {
            text = appName
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        infoCol.addView(nameTv)

        val pkgTv = TextView(this).apply {
            text = pkgInfo.packageName
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 2, 0, 0)
        }
        infoCol.addView(pkgTv)

        row.addView(infoCol)

        val sourceBadge = TextView(this).apply {
            val isSideloaded = detectIsSideloaded(pkgInfo.packageName)
            text = if (isSideloaded) "SIDELOAD" else "STORE"
            setTextColor(if (isSideloaded) Color.parseColor("#F28B82") else Color.parseColor("#81C995"))
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(if (isSideloaded) Color.parseColor("#2D1E1E") else Color.parseColor("#1A2D22"))
            }
        }
        row.addView(sourceBadge)

        return row
    }

    private fun createDeletedAppRowView(record: DeletedAppRecord): View {
        val density = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(Color.parseColor("#221A1A"))
                setStroke((1 * density).toInt(), Color.parseColor("#3D2525"))
            }
        }

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameTv = TextView(this).apply {
            text = record.appName
            setTextColor(Color.parseColor("#F28B82"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        infoCol.addView(nameTv)

        val pkgTv = TextView(this).apply {
            text = "${record.packageName} • Deleted ${record.deletedTimestamp}"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 2, 0, 0)
        }
        infoCol.addView(pkgTv)

        row.addView(infoCol)

        val badge = TextView(this).apply {
            text = "DELETED"
            setTextColor(Color.parseColor("#F28B82"))
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(Color.parseColor("#3B1D1D"))
            }
        }
        row.addView(badge)

        return row
    }

    private fun detectIsSideloaded(pkg: String): Boolean {
        return try {
            val pm = packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(pkg)
                val initiator = info.initiatingPackageName ?: info.installingPackageName
                initiator != "com.android.vending"
            } else {
                @Suppress("DEPRECATION")
                val installer = pm.getInstallerPackageName(pkg)
                installer != "com.android.vending"
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun showAppMetadataDialog(pkgInfo: PackageInfo) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val displayMetrics = resources.displayMetrics

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val pm = packageManager
        val appName = try { pm.getApplicationLabel(pkgInfo.applicationInfo).toString() } catch (e: Exception) { pkgInfo.packageName }

        val titleTv = TextView(this).apply {
            text = appName
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(titleTv)

        val pkgTv = TextView(this).apply {
            text = pkgInfo.packageName
            setTextColor(Color.parseColor("#8AB4F8"))
            textSize = 12f
            setPadding(0, 2, 0, (14 * density).toInt())
        }
        root.addView(pkgTv)

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val installTime = try { sdf.format(Date(pkgInfo.firstInstallTime)) } catch (e: Exception) { "Unknown" }
        val updateTime = try { sdf.format(Date(pkgInfo.lastUpdateTime)) } catch (e: Exception) { "Unknown" }
        val permCount = pkgInfo.requestedPermissions?.size ?: 0
        val isSystem = (pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        val details = listOf(
            "Version" to "${pkgInfo.versionName ?: "1.0"} (Build ${if (Build.VERSION.SDK_INT >= 28) pkgInfo.longVersionCode else @Suppress("DEPRECATION") pkgInfo.versionCode})",
            "Category" to if (isSystem) "Android System Binary" else "User Installed Application",
            "First Installed" to installTime,
            "Last Updated" to updateTime,
            "Permissions Count" to "$permCount granted/declared",
            "Install Origin" to if (detectIsSideloaded(pkgInfo.packageName)) "Sideloaded APK" else "Google Play Store"
        )

        details.forEach { (label, value) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }
            val lTv = TextView(this).apply {
                text = "$label: "
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams((120 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val vTv = TextView(this).apply {
                text = value
                setTextColor(Color.parseColor("#E8EAED"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(lTv)
            row.addView(vTv)
            root.addView(row)
        }

        val btnClose = Button(this).apply {
            text = "Close"
            setTextColor(Color.parseColor("#8AB4F8"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
                topMargin = (16 * density).toInt()
            }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()

        // 92% screen width for wide comfortable reading
        val dialogWidth = (displayMetrics.widthPixels * 0.92).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
