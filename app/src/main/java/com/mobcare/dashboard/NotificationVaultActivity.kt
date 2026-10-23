package com.mobcare.dashboard

import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mobcare.R
import com.mobcare.audit.ClipboardVaultManager
import com.mobcare.audit.NotificationVaultManager

class NotificationVaultActivity : AppCompatActivity() {

    private lateinit var tvVaultBadge: TextView
    private lateinit var tabNotifications: Button
    private lateinit var tabClipboard: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var bannerPermission: LinearLayout
    private lateinit var btnGrantAccess: Button
    private lateinit var tvCountSubtitle: TextView
    private lateinit var itemsContainer: LinearLayout

    private var currentTab = 0 // 0 = Notifications, 1 = Clipboard

    private val appIconCache = mutableMapOf<String, Drawable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_vault)

        tvVaultBadge = findViewById(R.id.tvVaultBadge)
        tabNotifications = findViewById(R.id.tabNotifications)
        tabClipboard = findViewById(R.id.tabClipboard)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)
        bannerPermission = findViewById(R.id.bannerPermission)
        btnGrantAccess = findViewById(R.id.btnGrantAccess)
        tvCountSubtitle = findViewById(R.id.tvCountSubtitle)
        itemsContainer = findViewById(R.id.itemsContainer)

        tabNotifications.setOnClickListener { switchTab(0) }
        tabClipboard.setOnClickListener { switchTab(1) }

        btnRefresh.setOnClickListener {
            checkAndSyncCurrentClipboard()
            renderCurrentTab()
        }

        btnClear.setOnClickListener {
            if (currentTab == 0) {
                NotificationVaultManager.clearNotifications(this)
            } else {
                ClipboardVaultManager.clearClipboard(this)
            }
            renderCurrentTab()
        }

        btnGrantAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        checkAndSyncCurrentClipboard()
        renderCurrentTab()
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

        if (currentTab == 0) {
            tabNotifications.setBackgroundColor(activeBg)
            tabNotifications.setTextColor(activeText)
            tabClipboard.setBackgroundColor(inactiveBg)
            tabClipboard.setTextColor(inactiveText)
        } else {
            tabNotifications.setBackgroundColor(inactiveBg)
            tabNotifications.setTextColor(inactiveText)
            tabClipboard.setBackgroundColor(activeBg)
            tabClipboard.setTextColor(activeText)
        }
    }

    private fun updatePermissionStatus() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        val isEnabled = enabledPackages.contains(packageName)

        if (isEnabled) {
            bannerPermission.visibility = View.GONE
            tvVaultBadge.text = "LISTENING"
            tvVaultBadge.setTextColor(Color.parseColor("#81C995"))
        } else {
            bannerPermission.visibility = View.VISIBLE
            tvVaultBadge.text = "ACCESS NEEDED"
            tvVaultBadge.setTextColor(Color.parseColor("#FDD663"))
        }
    }

    private fun checkAndSyncCurrentClipboard() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val txt = clip.getItemAt(0).text?.toString()
                if (!txt.isNullOrBlank()) {
                    ClipboardVaultManager.saveClipboard(this, txt)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun renderCurrentTab() {
        itemsContainer.removeAllViews()

        if (currentTab == 0) {
            renderNotifications()
        } else {
            renderClipboard()
        }
    }

    private fun renderNotifications() {
        val list = NotificationVaultManager.getNotifications(this)
        tvCountSubtitle.text = "${list.size} notification snapshots recorded"

        val density = resources.displayMetrics.density

        if (list.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No notification snapshots recorded yet. When incoming WhatsApp, SMS, or app notifications arrive, they will be archived here."
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                setPadding((8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt())
            }
            itemsContainer.addView(emptyTv)
            return
        }

        list.forEach { record ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f * density
                    setColor(Color.parseColor("#1E1F24"))
                    setStroke((1 * density).toInt(), Color.parseColor("#26272E"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, (10 * density).toInt())
                layoutParams = lp
            }

            // Top Header: Real Icon + Sender/Title + Timestamp
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconIv = ImageView(this).apply {
                val s = (36 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    setMargins(0, 0, (12 * density).toInt(), 0)
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
                        ContextCompat.getDrawable(this@NotificationVaultActivity, android.R.drawable.sym_def_app_icon)!!
                    }
                }
                setImageDrawable(icon)
            }

            val titleCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val appNameTv = TextView(this).apply {
                text = record.appName
                setTextColor(Color.parseColor("#8AB4F8"))
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
            }

            val senderTv = TextView(this).apply {
                text = if (record.title.isNotEmpty()) record.title else "Notification"
                setTextColor(Color.parseColor("#F1F3F4"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }

            titleCol.addView(appNameTv)
            titleCol.addView(senderTv)

            val timeTv = TextView(this).apply {
                text = record.timestamp
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 11f
            }

            topRow.addView(iconIv)
            topRow.addView(titleCol)
            topRow.addView(timeTv)
            card.addView(topRow)

            // Message content box
            if (record.text.isNotEmpty()) {
                val contentBox = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                    background = GradientDrawable().apply {
                        cornerRadius = 8f * density
                        setColor(Color.parseColor("#16171B"))
                    }
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, (10 * density).toInt(), 0, (6 * density).toInt())
                    }
                    layoutParams = lp
                }

                val msgTv = TextView(this).apply {
                    text = record.text
                    setTextColor(Color.parseColor("#E8EAED"))
                    textSize = 14f
                }
                contentBox.addView(msgTv)
                card.addView(contentBox)
            }

            val pkgTv = TextView(this).apply {
                text = record.packageName
                setTextColor(Color.parseColor("#5F6368"))
                textSize = 10f
            }
            card.addView(pkgTv)

            itemsContainer.addView(card)
        }
    }

    private fun renderClipboard() {
        val list = ClipboardVaultManager.getClipboardHistory(this)
        tvCountSubtitle.text = "${list.size} copied text items archived"

        val density = resources.displayMetrics.density

        if (list.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Clipboard vault is empty. Copy any text, password, or link anywhere on your phone, and it will be preserved here."
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                setPadding((8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt())
            }
            itemsContainer.addView(emptyTv)
            return
        }

        list.forEach { record ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f * density
                    setColor(Color.parseColor("#1E1F24"))
                    setStroke((1 * density).toInt(), Color.parseColor("#26272E"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, (10 * density).toInt())
                layoutParams = lp
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tagTv = TextView(this).apply {
                text = "Clipboard Item"
                setTextColor(Color.parseColor("#81C995"))
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = lp
            }

            val timeTv = TextView(this).apply {
                text = record.timestamp
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, (10 * density).toInt(), 0)
                }
            }

            val btnCopy = Button(this).apply {
                text = "Copy"
                textSize = 11f
                setTextColor(Color.parseColor("#8AB4F8"))
                background = GradientDrawable().apply {
                    cornerRadius = 8f * density
                    setColor(Color.parseColor("#26272E"))
                }
                val lp = LinearLayout.LayoutParams(
                    (64 * density).toInt(),
                    (30 * density).toInt()
                )
                layoutParams = lp
                setOnClickListener {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Copied", record.content))
                    Toast.makeText(this@NotificationVaultActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }

            topRow.addView(tagTv)
            topRow.addView(timeTv)
            topRow.addView(btnCopy)
            card.addView(topRow)

            // Content box
            val contentBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 8f * density
                    setColor(Color.parseColor("#16171B"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (10 * density).toInt(), 0, 0)
                }
                layoutParams = lp
            }

            val contentTv = TextView(this).apply {
                text = record.content
                setTextColor(Color.parseColor("#E8EAED"))
                textSize = 14f
            }
            contentBox.addView(contentTv)
            card.addView(contentBox)

            itemsContainer.addView(card)
        }
    }
}