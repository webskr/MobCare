package com.mobcare.dashboard

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobcare.R
import com.mobcare.security.AppLockerManager
import com.mobcare.security.AppLockerWatchdogService
import com.mobcare.security.MobCareAccessibilityService

class AppLockerActivity : AppCompatActivity() {

    private lateinit var tvLockerBadge: TextView
    private lateinit var cardPermissionWarning: View
    private lateinit var tvPermissionWarningTitle: TextView
    private lateinit var tvPermissionWarningDesc: TextView
    private lateinit var btnGrantRequiredPermission: Button
    private lateinit var btnModeAlways: Button
    private lateinit var btnModeIntruder: Button
    private lateinit var tvPinStatus: TextView
    private lateinit var btnCustomPin: Button
    private lateinit var tvCrashModeSub: TextView
    private lateinit var btnToggleCrashMode: Button
    private lateinit var tvDecoyPinStatus: TextView
    private lateinit var btnSetDecoyPin: Button
    private lateinit var etAppSearch: EditText
    private lateinit var tvProtectedCount: TextView
    private lateinit var btnClearAllLocks: Button
    private lateinit var pbLoadingApps: ProgressBar
    private lateinit var rvAppsGrid: RecyclerView

    private var allApps = listOf<AppLockerItem>()
    private lateinit var adapter: AppLockerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_locker)

        tvLockerBadge = findViewById(R.id.tvLockerBadge)
        btnModeAlways = findViewById(R.id.btnModeAlways)
        btnModeIntruder = findViewById(R.id.btnModeIntruder)
        tvPinStatus = findViewById(R.id.tvPinStatus)
        btnCustomPin = findViewById(R.id.btnCustomPin)
        tvCrashModeSub = findViewById(R.id.tvCrashModeSub)
        btnToggleCrashMode = findViewById(R.id.btnToggleCrashMode)
        tvDecoyPinStatus = findViewById(R.id.tvDecoyPinStatus)
        btnSetDecoyPin = findViewById(R.id.btnSetDecoyPin)
        etAppSearch = findViewById(R.id.etAppSearch)
        tvProtectedCount = findViewById(R.id.tvProtectedCount)
        btnClearAllLocks = findViewById(R.id.btnClearAllLocks)
        pbLoadingApps = findViewById(R.id.pbLoadingApps)
        rvAppsGrid = findViewById(R.id.rvAppsGrid)

        cardPermissionWarning = findViewById(R.id.cardPermissionWarning)
        tvPermissionWarningTitle = findViewById(R.id.tvPermissionWarningTitle)
        tvPermissionWarningDesc = findViewById(R.id.tvPermissionWarningDesc)
        btnGrantRequiredPermission = findViewById(R.id.btnGrantRequiredPermission)

        // Setup 5-Column Grid natively
        rvAppsGrid.layoutManager = GridLayoutManager(this, 5)
        rvAppsGrid.setHasFixedSize(true)

        adapter = AppLockerAdapter(emptyList()) { item ->
            val currentlyLocked = AppLockerManager.getLockedPackages(this).contains(item.packageName)
            val newLockedState = !currentlyLocked
            AppLockerManager.setPackageLocked(this, item.packageName, newLockedState)
            if (newLockedState) {
                try {
                    AppLockerWatchdogService.start(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            adapter.notifyDataSetChanged()
            updateStats()
        }
        rvAppsGrid.adapter = adapter

        btnModeAlways.setOnClickListener {
            AppLockerManager.setLockMode(this, AppLockerManager.MODE_ALWAYS_LOCKED)
            updateModeUi()
        }

        btnModeIntruder.setOnClickListener {
            AppLockerManager.setLockMode(this, AppLockerManager.MODE_INTRUDER_TRIGGERED)
            updateModeUi()
        }

        btnCustomPin.setOnClickListener {
            showCustomPinDialog()
        }

        btnToggleCrashMode.setOnClickListener {
            val isCrash = AppLockerManager.isAppCrashMode(this)
            val newState = !isCrash
            AppLockerManager.setAppCrashMode(this, newState)
            updateCrashModeUi()
            val msg = if (newState) "App Crash Mode Active: Locked apps simulate system crash!" else "App Crash Mode Disabled: Standard PIN gate active."
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnSetDecoyPin.setOnClickListener {
            showDecoyPinDialog()
        }

        btnClearAllLocks.setOnClickListener {
            val locked = AppLockerManager.getLockedPackages(this)
            locked.forEach { pkg ->
                AppLockerManager.setPackageLocked(this, pkg, false)
            }
            adapter.notifyDataSetChanged()
            updateStats()
            Toast.makeText(this, "All applications unlocked", Toast.LENGTH_SHORT).show()
        }

        etAppSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadInstalledAppsAsync()
        updateModeUi()
        updatePinStatus()
        updateCrashModeUi()
        checkRequiredPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkRequiredPermissions()
        try {
            if (AppLockerManager.getLockedPackages(this).isNotEmpty()) {
                AppLockerWatchdogService.start(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkRequiredPermissions() {
        val isAcc = MobCareAccessibilityService.instance != null
        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true

        if (!isAcc) {
            cardPermissionWarning.visibility = View.VISIBLE
            tvPermissionWarningTitle.text = "Sentinel Accessibility Service Required"
            tvPermissionWarningDesc.text = "Android requires 'MobCare Sentinel' in Accessibility Settings to detect when locked apps open and trigger the PIN lock or fake crash."
            btnGrantRequiredPermission.text = "ENABLE IN ACCESSIBILITY SETTINGS"
            btnGrantRequiredPermission.setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Open Settings -> Accessibility -> MobCare Sentinel", Toast.LENGTH_LONG).show()
                }
            }
        } else if (!canOverlay) {
            cardPermissionWarning.visibility = View.VISIBLE
            tvPermissionWarningTitle.text = "Display Over Other Apps Required"
            tvPermissionWarningDesc.text = "MobCare needs overlay permission to display the PIN lock screen or fake crash dialog over protected apps."
            btnGrantRequiredPermission.text = "GRANT OVERLAY PERMISSION"
            btnGrantRequiredPermission.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        startActivity(intent)
                    } catch (e: Exception) {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    }
                }
            }
        } else {
            cardPermissionWarning.visibility = View.GONE
        }
    }

    private fun loadInstalledAppsAsync() {
        pbLoadingApps.visibility = View.VISIBLE
        rvAppsGrid.visibility = View.GONE

        Thread {
            val pm = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val loaded = mutableListOf<AppLockerItem>()

            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == packageName) continue // Skip MobCare itself

                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                loaded.add(AppLockerItem(pkg, appName, icon))
            }

            allApps = loaded.sortedBy { it.appName.lowercase() }

            runOnUiThread {
                pbLoadingApps.visibility = View.GONE
                rvAppsGrid.visibility = View.VISIBLE
                adapter.updateList(allApps)
                updateStats()
            }
        }.start()
    }

    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            adapter.updateList(allApps)
        } else {
            val filtered = allApps.filter {
                it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
            adapter.updateList(filtered)
        }
    }

    private fun updateStats() {
        val lockedCount = AppLockerManager.getLockedPackages(this).size
        tvProtectedCount.text = "Applications ($lockedCount Locked)"
        val primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_bg)
        if (lockedCount > 0) {
            tvLockerBadge.text = "ARMED"
            tvLockerBadge.setTextColor(primaryColor)
        } else {
            tvLockerBadge.text = "STANDBY"
            tvLockerBadge.setTextColor(Color.parseColor("#9AA0A6"))
        }
    }

    private fun updatePinStatus() {
        val customPin = AppLockerManager.getCustomPin(this)
        val primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_bg)
        val secondaryBg = androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_bg)
        val primaryText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_text)

        if (customPin != null) {
            tvPinStatus.text = "Dedicated App PIN Active"
            tvPinStatus.setTextColor(primaryColor)
            btnCustomPin.text = "Change PIN"
            btnCustomPin.backgroundTintList = ColorStateList.valueOf(primaryColor)
            btnCustomPin.setTextColor(primaryText)
        } else {
            tvPinStatus.text = "Master PIN (Default)"
            tvPinStatus.setTextColor(Color.parseColor("#9AA0A6"))
            btnCustomPin.text = "Set Dedicated"
            btnCustomPin.backgroundTintList = ColorStateList.valueOf(secondaryBg)
            btnCustomPin.setTextColor(primaryColor)
        }
    }

    private fun updateModeUi() {
        val mode = AppLockerManager.getLockMode(this)

        val primaryTint = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_bg))
        val transparentTint = ColorStateList.valueOf(Color.TRANSPARENT)
        val primaryText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_text)
        val secText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_text)

        if (mode == AppLockerManager.MODE_ALWAYS_LOCKED) {
            btnModeAlways.backgroundTintList = primaryTint
            btnModeAlways.setTextColor(primaryText)

            btnModeIntruder.backgroundTintList = transparentTint
            btnModeIntruder.setTextColor(secText)
        } else {
            btnModeIntruder.backgroundTintList = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.btn_destructive_bg))
            btnModeIntruder.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.btn_destructive_text))

            btnModeAlways.backgroundTintList = transparentTint
            btnModeAlways.setTextColor(secText)
        }
    }

    private fun showCustomPinDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_bg)
        val primaryText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_text)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (22 * density).toInt(), (24 * density).toInt(), (22 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "App Locker PIN Setup"
            setTextColor(Color.parseColor("#F1F3F4"))
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        root.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "Set an exclusive PIN for locked apps, or revert to Master PIN"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(subTv)

        val pinInput = EditText(this).apply {
            hint = "Enter 4-6 digit PIN"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.parseColor("#FFFFFF"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(Color.parseColor("#121316"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#3C4043"))
            }
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        }
        root.addView(pinInput)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (18 * density).toInt(), 0, 0)
        }

        val btnReset = Button(this).apply {
            text = "Use Master PIN"
            setTextColor(Color.parseColor("#F28B82"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#2D1A1A"))
                setStroke((1 * density).toInt(), Color.parseColor("#4A1F1F"))
            }
            setPadding((14 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            setOnClickListener {
                AppLockerManager.setCustomPin(this@AppLockerActivity, null)
                updatePinStatus()
                dialog.dismiss()
                Toast.makeText(this@AppLockerActivity, "Reverted to Master PIN", Toast.LENGTH_SHORT).show()
            }
        }

        val btnSave = Button(this).apply {
            text = "Save PIN"
            setTextColor(primaryText)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(primaryColor)
            }
            setTypeface(null, android.graphics.Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (10 * density).toInt()
            }
            layoutParams = params
            setPadding((18 * density).toInt(), (8 * density).toInt(), (18 * density).toInt(), (8 * density).toInt())
            setOnClickListener {
                val entered = pinInput.text.toString().trim()
                if (entered.length >= 4) {
                    AppLockerManager.setCustomPin(this@AppLockerActivity, entered)
                    updatePinStatus()
                    dialog.dismiss()
                    Toast.makeText(this@AppLockerActivity, "Dedicated PIN saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AppLockerActivity, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRow.addView(btnReset)
        btnRow.addView(btnSave)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun updateCrashModeUi() {
        val isCrash = AppLockerManager.isAppCrashMode(this)
        val decoyPin = AppLockerManager.getDecoyPin(this)

        if (isCrash) {
            btnToggleCrashMode.text = "CRASH ON"
            btnToggleCrashMode.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1B3C2A"))
            btnToggleCrashMode.setTextColor(Color.parseColor("#81C995"))
            tvCrashModeSub.text = "Active • Locked apps simulate instant Android crash dialog"
            tvCrashModeSub.setTextColor(Color.parseColor("#81C995"))
        } else {
            btnToggleCrashMode.text = "CRASH OFF"
            btnToggleCrashMode.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#26272E"))
            btnToggleCrashMode.setTextColor(Color.parseColor("#9AA0A6"))
            tvCrashModeSub.text = "Disabled • Standard PIN security gate active"
            tvCrashModeSub.setTextColor(Color.parseColor("#9AA0A6"))
        }

        tvDecoyPinStatus.text = "Decoy PIN ($decoyPin) • Simulates crash and snaps photo upon entry"
    }

    private fun showDecoyPinDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (22 * density).toInt(), (24 * density).toInt(), (22 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Decoy Panic PIN Setup"
            setTextColor(Color.parseColor("#F1F3F4"))
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        root.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "Entering this PIN on a locked app secretly snaps an intruder photo and displays an authentic Android system crash."
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(subTv)

        val pinInput = EditText(this).apply {
            hint = "Decoy PIN (e.g. 0000)"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.parseColor("#FFFFFF"))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(Color.parseColor("#121316"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#3C4043"))
            }
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        }
        root.addView(pinInput)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (18 * density).toInt(), 0, 0)
        }

        val btnReset = Button(this).apply {
            text = "Default (0000)"
            setTextColor(Color.parseColor("#FDD663"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#2B2516"))
                setStroke((1 * density).toInt(), Color.parseColor("#483D1B"))
            }
            setPadding((14 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            setOnClickListener {
                AppLockerManager.setDecoyPin(this@AppLockerActivity, "0000")
                updateCrashModeUi()
                dialog.dismiss()
                Toast.makeText(this@AppLockerActivity, "Decoy PIN reset to default (0000)", Toast.LENGTH_SHORT).show()
            }
        }

        val btnSave = Button(this).apply {
            text = "Save Decoy"
            setTextColor(Color.parseColor("#121316"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#FDD663"))
            }
            setTypeface(null, android.graphics.Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (10 * density).toInt()
            }
            layoutParams = params
            setPadding((18 * density).toInt(), (8 * density).toInt(), (18 * density).toInt(), (8 * density).toInt())
            setOnClickListener {
                val entered = pinInput.text.toString().trim()
                if (entered.length >= 4) {
                    AppLockerManager.setDecoyPin(this@AppLockerActivity, entered)
                    updateCrashModeUi()
                    dialog.dismiss()
                    Toast.makeText(this@AppLockerActivity, "Decoy PIN configured successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AppLockerActivity, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRow.addView(btnReset)
        btnRow.addView(btnSave)
        root.addView(btnRow)

        dialog.setContentView(root)
        dialog.show()
    }
}
