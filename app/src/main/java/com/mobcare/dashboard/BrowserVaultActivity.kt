package com.mobcare.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R
import com.mobcare.audit.BrowserVaultManager
import com.mobcare.audit.WebVisitItem
import com.mobcare.security.MobCareAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BrowserVaultActivity : AppCompatActivity() {

    private lateinit var tvBrowserBadge: TextView
    private lateinit var bannerAccessibility: LinearLayout
    private lateinit var btnGrantAccessibility: Button
    private lateinit var etBrowserSearch: EditText
    private lateinit var llBrowserListContainer: LinearLayout

    private var allVisits = listOf<WebVisitItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_vault)

        tvBrowserBadge = findViewById(R.id.tvBrowserBadge)
        bannerAccessibility = findViewById(R.id.bannerAccessibility)
        btnGrantAccessibility = findViewById(R.id.btnGrantAccessibility)
        etBrowserSearch = findViewById(R.id.etBrowserSearch)
        llBrowserListContainer = findViewById(R.id.llBrowserListContainer)


        btnGrantAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        etBrowserSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderList(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadWebData()
    }

    override fun onResume() {
        super.onResume()
        loadWebData()
    }

    private fun isAccessibilityEnabled(): Boolean {
        return MobCareAccessibilityService.instance != null
    }

    private fun loadWebData() {
        val isEnabled = isAccessibilityEnabled()
        bannerAccessibility.visibility = if (isEnabled) View.GONE else View.VISIBLE
        tvBrowserBadge.text = if (isEnabled) "ARMED" else "SETUP"
        tvBrowserBadge.setTextColor(Color.parseColor(if (isEnabled) "#81C995" else "#FDD663"))

        allVisits = BrowserVaultManager.getWebVisits(this)
        renderList(etBrowserSearch.text.toString().trim())
    }

    private fun renderList(query: String) {
        llBrowserListContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

        val filtered = if (query.isEmpty()) {
            allVisits
        } else {
            allVisits.filter {
                it.domain.contains(query, true) || it.url.contains(query, true) || it.browserApp.contains(query, true)
            }
        }

        if (filtered.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No web history logged yet. Visited websites in Chrome, Brave, and other browsers will be mirrored here in real time."
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding((16 * density).toInt(), (40 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            }
            llBrowserListContainer.addView(emptyTv)
            return
        }

        filtered.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (10 * density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                    setColor(Color.parseColor("#1E1F24"))
                    setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
                }
            }

            // Top Row: Domain Badge + Time
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvDomain = TextView(this).apply {
                text = item.domain
                setTextColor(Color.parseColor("#8AB4F8"))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTime = TextView(this).apply {
                text = sdf.format(Date(item.timestamp))
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 11f
            }

            topRow.addView(tvDomain)
            topRow.addView(tvTime)
            card.addView(topRow)

            // URL Row
            val tvUrl = TextView(this).apply {
                text = item.url
                setTextColor(Color.parseColor("#E8EAED"))
                textSize = 12f
                setPadding(0, (6 * density).toInt(), 0, (4 * density).toInt())
            }
            card.addView(tvUrl)

            // Browser Badge Row
            val tvBrowser = TextView(this).apply {
                text = "Captured from ${item.browserApp}"
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 11f
            }
            card.addView(tvBrowser)

            llBrowserListContainer.addView(card)
        }
    }
}