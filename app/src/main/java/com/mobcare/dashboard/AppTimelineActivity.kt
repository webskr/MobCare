package com.mobcare.dashboard

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R
import com.mobcare.audit.AppTimelineItem
import com.mobcare.audit.AppTimelineManager

class AppTimelineActivity : AppCompatActivity() {

    private lateinit var tvTimelineBadge: TextView
    private lateinit var bannerUsagePermission: LinearLayout
    private lateinit var btnGrantUsage: Button
    private lateinit var etTimelineSearch: EditText
    private lateinit var llTimelineContainer: LinearLayout

    private var allTimelineItems = listOf<AppTimelineItem>()
    private val iconCache = mutableMapOf<String, Drawable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_timeline)

        tvTimelineBadge = findViewById(R.id.tvTimelineBadge)
        bannerUsagePermission = findViewById(R.id.bannerUsagePermission)
        btnGrantUsage = findViewById(R.id.btnGrantUsage)
        etTimelineSearch = findViewById(R.id.etTimelineSearch)
        llTimelineContainer = findViewById(R.id.llTimelineContainer)

        btnGrantUsage.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        etTimelineSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTimeline(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadTimelineData()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionState()
        loadTimelineData()
    }

    private fun checkPermissionState() {
        val hasPerm = AppTimelineManager.hasUsageStatsPermission(this)
        if (hasPerm) {
            bannerUsagePermission.visibility = View.GONE
            tvTimelineBadge.text = "SYNCED"
            tvTimelineBadge.setTextColor(Color.parseColor("#81C995"))
        } else {
            bannerUsagePermission.visibility = View.VISIBLE
            tvTimelineBadge.text = "SETUP NEEDED"
            tvTimelineBadge.setTextColor(Color.parseColor("#FDD663"))
        }
    }

    private fun loadTimelineData() {
        Thread {
            val items = AppTimelineManager.getAppTimeline(this)
            runOnUiThread {
                allTimelineItems = items
                renderTimeline(items)
            }
        }.start()
    }

    private fun filterTimeline(query: String) {
        if (query.isEmpty()) {
            renderTimeline(allTimelineItems)
            return
        }

        val filtered = allTimelineItems.filter { item ->
            item.appName.contains(query, ignoreCase = true) ||
            item.packageName.contains(query, ignoreCase = true) ||
            item.auditLogs.any { it.content.contains(query, ignoreCase = true) }
        }
        renderTimeline(filtered)
    }

    private fun renderTimeline(items: List<AppTimelineItem>) {
        llTimelineContainer.removeAllViews()

        if (items.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No application sessions recorded yet.\nOpen apps or type using MobCare Keyboard to populate the activity timeline."
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 80, 0, 0)
            }
            llTimelineContainer.addView(emptyTv)
            return
        }

        items.forEach { item ->
            llTimelineContainer.addView(createAppTimelineRow(item))
        }
    }

    private fun createAppTimelineRow(item: AppTimelineItem): View {
        val pm = packageManager
        val density = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showAppKeystrokeDialog(item)
            }
        }

        // App Icon
        val iconIv = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                marginEnd = (14 * density).toInt()
            }
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10f * density)
                }
            }
            clipToOutline = true
        }

        val cachedIcon = iconCache[item.packageName]
        if (cachedIcon != null) {
            iconIv.setImageDrawable(cachedIcon)
        } else {
            Thread {
                val icon = try {
                    val info = pm.getApplicationInfo(item.packageName, 0)
                    pm.getApplicationIcon(info)
                } catch (e: Exception) {
                    null
                }
                if (icon != null) {
                    iconCache[item.packageName] = icon
                    runOnUiThread { iconIv.setImageDrawable(icon) }
                }
            }.start()
        }
        row.addView(iconIv)

        // Title and Subtitle Info
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameTv = TextView(this).apply {
            text = item.appName
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        infoCol.addView(nameTv)

        val metaTv = TextView(this).apply {
            text = "Active: ${item.formattedScreenTime} • ${item.formattedLastUsed}"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 2, 0, 0)
        }
        infoCol.addView(metaTv)

        row.addView(infoCol)

        // Keystroke Count Pill Badge
        val keyBadge = TextView(this).apply {
            val count = item.keystrokeCount
            text = if (count > 0) "$count keys" else "No keys"
            setTextColor(if (count > 0) Color.parseColor("#81C995") else Color.parseColor("#5F6368"))
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(if (count > 0) Color.parseColor("#163A34") else Color.parseColor("#26272E"))
            }
        }
        row.addView(keyBadge)

        return row
    }

    private fun showAppKeystrokeDialog(item: AppTimelineItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val displayMetrics = resources.displayMetrics

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * density).toInt(), (20 * density).toInt(), (22 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1E1F24"))
                setStroke((1 * density).toInt(), Color.parseColor("#3C4043"))
            }
        }

        val titleTv = TextView(this).apply {
            text = item.appName
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "${item.packageName} • Active Screen Time: ${item.formattedScreenTime}"
            setTextColor(Color.parseColor("#8AB4F8"))
            textSize = 12f
            setPadding(0, 2, 0, (14 * density).toInt())
        }
        root.addView(subTv)

        if (item.auditLogs.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No keystrokes or chats recorded in this app yet.\nKeystrokes typed in ${item.appName} using MobCare Keyboard will automatically be listed here."
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, (40 * density).toInt(), 0, (40 * density).toInt())
            }
            root.addView(emptyTv)
        } else {
            val scrollView = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (380 * density).toInt()
                )
            }
            val logsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            item.auditLogs.forEach { log ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * density).toInt()
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(Color.parseColor("#121316"))
                        setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
                    }
                }

                val timeTv = TextView(this).apply {
                    text = log.timestamp
                    setTextColor(Color.parseColor("#9AA0A6"))
                    textSize = 11f
                    setPadding(0, 0, 0, (4 * density).toInt())
                }
                card.addView(timeTv)

                val contentTv = TextView(this).apply {
                    text = log.content
                    setTextColor(Color.parseColor("#E8EAED"))
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                }
                card.addView(contentTv)

                logsContainer.addView(card)
            }

            scrollView.addView(logsContainer)
            root.addView(scrollView)
        }

        val btnClose = Button(this).apply {
            text = "Close"
            setTextColor(Color.parseColor("#8AB4F8"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = (12 * density).toInt()
            }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()

        val dialogWidth = (displayMetrics.widthPixels * 0.92).toInt()
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
