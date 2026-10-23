package com.mobcare.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobcare.R
import com.mobcare.audit.AuditLogManager

class KeyboardTelemetryActivity : AppCompatActivity() {

        private lateinit var tvKeyboardState: TextView
    private lateinit var tvKeyboardBadge: TextView
    private lateinit var btnEnableKeyboard: Button
    private lateinit var btnSelectKeyboard: Button
    private lateinit var btnToggleTestPad: Button
    private lateinit var llTestPadContainer: LinearLayout
    private lateinit var btnRefreshLogs: Button
    private lateinit var btnClearLogs: Button
    private lateinit var tvLogCount: TextView
    private lateinit var logsContainer: LinearLayout

    private val appIconCache = mutableMapOf<String, Drawable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_telemetry)

                tvKeyboardState = findViewById(R.id.tvKeyboardState)
        tvKeyboardBadge = findViewById(R.id.tvKeyboardBadge)
        btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard)
        btnSelectKeyboard = findViewById(R.id.btnSelectKeyboard)
        btnToggleTestPad = findViewById(R.id.btnToggleTestPad)
        llTestPadContainer = findViewById(R.id.llTestPadContainer)
        btnRefreshLogs = findViewById(R.id.btnRefreshLogs)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        tvLogCount = findViewById(R.id.tvLogCount)
        logsContainer = findViewById(R.id.logsContainer)

        
        btnEnableKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnSelectKeyboard.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        btnToggleTestPad.setOnClickListener {
            if (llTestPadContainer.visibility == View.VISIBLE) {
                llTestPadContainer.visibility = View.GONE
                btnToggleTestPad.text = "Test Pad"
                btnToggleTestPad.setTextColor(Color.parseColor("#8AB4F8"))
            } else {
                llTestPadContainer.visibility = View.VISIBLE
                btnToggleTestPad.text = "Close Pad"
                btnToggleTestPad.setTextColor(Color.parseColor("#F28B82"))
            }
        }

        btnRefreshLogs.setOnClickListener {
            loadAuditLogs()
        }

        btnClearLogs.setOnClickListener {
            AuditLogManager.clearLogs(this)
            loadAuditLogs()
        }
    }

    override fun onResume() {
        super.onResume()
        updateKeyboardStatus()
        loadAuditLogs()
    }

    private fun updateKeyboardStatus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val isEnabled = enabledMethods.any { it.packageName == packageName }

        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isDefault = defaultIme != null && defaultIme.contains(packageName)

        when {
            isDefault -> {
                tvKeyboardState.text = "MobCare Keyboard is active & set as default input method."
                tvKeyboardState.setTextColor(Color.parseColor("#81C995"))
                tvKeyboardBadge.text = "DEFAULT"
                tvKeyboardBadge.setTextColor(Color.parseColor("#81C995"))
            }
            isEnabled -> {
                tvKeyboardState.text = "Enabled in system settings. Tap Switch IME to set as default."
                tvKeyboardState.setTextColor(Color.parseColor("#FDD663"))
                tvKeyboardBadge.text = "READY"
                tvKeyboardBadge.setTextColor(Color.parseColor("#FDD663"))
            }
            else -> {
                tvKeyboardState.text = "Keyboard is not yet enabled in system settings."
                tvKeyboardState.setTextColor(Color.parseColor("#F28B82"))
                tvKeyboardBadge.text = "INACTIVE"
                tvKeyboardBadge.setTextColor(Color.parseColor("#F28B82"))
            }
        }
    }

    private fun loadAuditLogs() {
        logsContainer.removeAllViews()
        val logs = AuditLogManager.getLogs(this)
        tvLogCount.text = "${logs.size} diagnostic events recorded"

        val density = resources.displayMetrics.density

        if (logs.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No recorded activity yet. Type in any app, then tap Refresh."
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
            }
            logsContainer.addView(emptyTv)
            return
        }

        logs.forEach { record ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                    setColor(Color.parseColor("#26272E"))
                    setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, (12 * density).toInt())
                layoutParams = lp
            }

            // Top Header: Real App Icon + App Name + Timestamp
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Real App Icon with rounded corners
            val appIconIv = ImageView(this).apply {
                val iconSize = (32 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    setMargins(0, 0, (10 * density).toInt(), 0)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 8f * density)
                    }
                }
                val icon = appIconCache.getOrPut(record.packageName) {
                    try {
                        packageManager.getApplicationIcon(record.packageName)
                    } catch (e: Exception) {
                        ContextCompat.getDrawable(this@KeyboardTelemetryActivity, android.R.drawable.sym_def_app_icon)!!
                    }
                }
                setImageDrawable(icon)
            }

            val appBadge = TextView(this).apply {
                text = record.appName
                setTextColor(Color.parseColor("#8AB4F8"))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = lp
            }

            val timeTv = TextView(this).apply {
                text = record.timestamp
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 11f
            }

            topRow.addView(appIconIv)
            topRow.addView(appBadge)
            topRow.addView(timeTv)
            card.addView(topRow)

            // Content Box for Typed Sentence or System Event
            val contentBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 8f * density
                    setColor(Color.parseColor("#1A1B20"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (10 * density).toInt(), 0, (6 * density).toInt())
                }
                layoutParams = lp
            }

            val contentTv = TextView(this).apply {
                text = record.content
                setTextColor(
                    when {
                        record.content.startsWith("[Intruder Alert]") || record.content.startsWith("[Tamper Attempt]") -> {
                            Color.parseColor("#F28B82")
                        }
                        record.content.startsWith("[New App Installed]") || record.content.startsWith("[App Uninstalled]") -> {
                            Color.parseColor("#FDD663")
                        }
                        record.content.startsWith("[Anti-Uninstall Armed]") || record.content.startsWith("[Security Disarm]") -> {
                            Color.parseColor("#81C995")
                        }
                        else -> Color.parseColor("#E8EAED")
                    }
                )
                textSize = 14f
            }
            contentBox.addView(contentTv)
            card.addView(contentBox)

            // Bottom row: Package Name
            val pkgTv = TextView(this).apply {
                text = record.packageName
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 10f
            }
            card.addView(pkgTv)

            logsContainer.addView(card)
        }
    }
}