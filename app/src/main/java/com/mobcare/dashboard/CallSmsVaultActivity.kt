package com.mobcare.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mobcare.R
import com.mobcare.audit.CallSmsVaultManager
import com.mobcare.audit.CallVaultItem
import com.mobcare.audit.SmsVaultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallSmsVaultActivity : AppCompatActivity() {

    private lateinit var tvVaultBadge: TextView
    private lateinit var bannerCallPermissions: LinearLayout
    private lateinit var btnGrantPermissions: Button
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterCalls: Button
    private lateinit var btnFilterSms: Button
    private lateinit var etVaultSearch: EditText
    private lateinit var llVaultListContainer: LinearLayout

    private var currentFilter = "ALL"
    private var allCalls = listOf<CallVaultItem>()
    private var allSms = listOf<SmsVaultItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_sms_vault)

        tvVaultBadge = findViewById(R.id.tvVaultBadge)
        bannerCallPermissions = findViewById(R.id.bannerCallPermissions)
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterCalls = findViewById(R.id.btnFilterCalls)
        btnFilterSms = findViewById(R.id.btnFilterSms)
        etVaultSearch = findViewById(R.id.etVaultSearch)
        llVaultListContainer = findViewById(R.id.llVaultListContainer)


        btnGrantPermissions.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                ),
                201
            )
        }

        btnFilterAll.setOnClickListener { setFilter("ALL") }
        btnFilterCalls.setOnClickListener { setFilter("CALLS") }
        btnFilterSms.setOnClickListener { setFilter("SMS") }

        etVaultSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderList(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadVaultData()
    }

    override fun onResume() {
        super.onResume()
        if (CallSmsVaultManager.hasPermissions(this)) {
            CallSmsVaultManager.syncSystemCallLog(this)
            CallSmsVaultManager.syncSystemSms(this)
        }
        loadVaultData()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 201) {
            if (CallSmsVaultManager.hasPermissions(this)) {
                CallSmsVaultManager.syncSystemCallLog(this)
                CallSmsVaultManager.syncSystemSms(this)
            }
            loadVaultData()
        }
    }

    private fun loadVaultData() {
        val hasPerms = CallSmsVaultManager.hasPermissions(this)
        bannerCallPermissions.visibility = if (hasPerms) View.GONE else View.VISIBLE
        tvVaultBadge.text = if (hasPerms) "ARMED" else "SETUP"
        tvVaultBadge.setTextColor(Color.parseColor(if (hasPerms) "#81C995" else "#FDD663"))

        allCalls = CallSmsVaultManager.getCalls(this)
        allSms = CallSmsVaultManager.getSms(this)

        renderList(etVaultSearch.text.toString().trim())
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        val activeTint = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_bg))
        val inactiveTint = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_bg))
        val activeText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_text)
        val inactiveText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_text)

        btnFilterAll.backgroundTintList = if (filter == "ALL") activeTint else inactiveTint
        btnFilterAll.setTextColor(if (filter == "ALL") activeText else inactiveText)

        btnFilterCalls.backgroundTintList = if (filter == "CALLS") activeTint else inactiveTint
        btnFilterCalls.setTextColor(if (filter == "CALLS") activeText else inactiveText)

        btnFilterSms.backgroundTintList = if (filter == "SMS") activeTint else inactiveTint
        btnFilterSms.setTextColor(if (filter == "SMS") activeText else inactiveText)

        renderList(etVaultSearch.text.toString().trim())
    }

    private fun renderList(query: String) {
        llVaultListContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

        val combinedItems = mutableListOf<VaultRowItem>()

        if (currentFilter == "ALL" || currentFilter == "CALLS") {
            allCalls.forEach { c ->
                if (query.isEmpty() || c.number.contains(query, true) || c.contactName.contains(query, true)) {
                    combinedItems.add(VaultRowItem(isCall = true, title = if (c.contactName.isNotEmpty() && c.contactName != "Unknown Contact") "${c.contactName} (${c.number})" else c.number, sub = "Call: ${c.type} • Duration: ${c.durationSeconds}s", time = c.timestamp, badgeColor = if (c.type == "MISSED") "#F28B82" else "#81C995"))
                }
            }
        }

        if (currentFilter == "ALL" || currentFilter == "SMS") {
            allSms.forEach { s ->
                if (query.isEmpty() || s.address.contains(query, true) || s.body.contains(query, true)) {
                    combinedItems.add(VaultRowItem(isCall = false, title = "SMS: ${s.address}", sub = s.body, time = s.timestamp, badgeColor = "#8AB4F8"))
                }
            }
        }

        combinedItems.sortByDescending { it.time }

        if (combinedItems.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No records vaulted. All future calls and SMS messages will be captured and protected against deletion here."
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding((16 * density).toInt(), (40 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            }
            llVaultListContainer.addView(emptyTv)
            return
        }

        combinedItems.forEach { item ->
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

            // Header row: Title + Time
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvTitle = TextView(this).apply {
                text = item.title
                setTextColor(Color.parseColor("#E8EAED"))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTime = TextView(this).apply {
                text = sdf.format(Date(item.time))
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 11f
            }

            topRow.addView(tvTitle)
            topRow.addView(tvTime)
            card.addView(topRow)

            // Subtitle / message body
            val tvSub = TextView(this).apply {
                text = item.sub
                setTextColor(Color.parseColor("#D1D5DB"))
                textSize = 12f
                setPadding(0, (6 * density).toInt(), 0, 0)
            }
            card.addView(tvSub)

            llVaultListContainer.addView(card)
        }
    }

    private data class VaultRowItem(
        val isCall: Boolean,
        val title: String,
        val sub: String,
        val time: Long,
        val badgeColor: String
    )
}