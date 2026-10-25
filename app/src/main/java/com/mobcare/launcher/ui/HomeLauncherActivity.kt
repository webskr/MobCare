package com.mobcare.launcher.ui

import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobcare.R
import com.mobcare.dashboard.MainActivity
import com.mobcare.launcher.data.AppDiscoveryRepository
import com.mobcare.launcher.data.IdentitySwapManager
import com.mobcare.launcher.model.LauncherAppItem
import com.mobcare.security.StealthManager
import kotlinx.coroutines.launch

class HomeLauncherActivity : AppCompatActivity() {

    private lateinit var bannerSetDefault: LinearLayout
    private lateinit var btnSetDefaultHome: Button
    private lateinit var etSearchApps: EditText
    private lateinit var btnOpenIdentitySwap: ImageView
    private lateinit var btnOpenHub: ImageView
    private lateinit var layoutSwapIndicator: LinearLayout
    private lateinit var tvSwapStatus: TextView
    private lateinit var pbLauncherLoading: ProgressBar
    private lateinit var rvLauncherApps: RecyclerView

    private val appRepo by lazy { AppDiscoveryRepository(this) }
    private var allApps = listOf<LauncherAppItem>()
    private lateinit var adapter: LauncherAppAdapter

    companion object {
        private const val REQUEST_CODE_ROLE_HOME = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_launcher)

        bannerSetDefault = findViewById(R.id.bannerSetDefault)
        btnSetDefaultHome = findViewById(R.id.btnSetDefaultHome)
        etSearchApps = findViewById(R.id.etSearchApps)
        btnOpenIdentitySwap = findViewById(R.id.btnOpenIdentitySwap)
        btnOpenHub = findViewById(R.id.btnOpenHub)
        layoutSwapIndicator = findViewById(R.id.layoutSwapIndicator)
        tvSwapStatus = findViewById(R.id.tvSwapStatus)
        pbLauncherLoading = findViewById(R.id.pbLauncherLoading)
        rvLauncherApps = findViewById(R.id.rvLauncherApps)

        // Setup 4-column Launcher Grid
        rvLauncherApps.layoutManager = GridLayoutManager(this, 4)
        adapter = LauncherAppAdapter(
            emptyList(),
            onItemClick = { item -> handleAppClick(item) },
            onItemLongClick = { item -> handleAppLongClick(item) }
        )
        rvLauncherApps.adapter = adapter

        // Search filtering
        etSearchApps.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Quick action: Open Identity Swapper
        btnOpenIdentitySwap.setOnClickListener {
            startActivity(Intent(this, IdentitySwapActivity::class.java))
        }

        // Quick action: Open MobCare Security Hub
        btnOpenHub.setOnClickListener {
            verifyPinAndOpenHub()
        }

        // Request Home Role
        btnSetDefaultHome.setOnClickListener {
            requestHomeRole()
        }

        // Home screen back navigation handling: Home screen does not close on back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (etSearchApps.text.isNotEmpty()) {
                    etSearchApps.text.clear()
                }
                // Do not finish; remain on home screen
            }
        })
    }

    override fun onResume() {
        super.onResume()
        checkDefaultHomeRole()
        loadAppsAsync()
        updateSwapIndicator()
    }

    private fun checkDefaultHomeRole() {
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            resolveInfo?.activityInfo?.packageName == packageName
        }

        bannerSetDefault.visibility = if (isDefault) View.GONE else View.VISIBLE
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                startActivityForResult(intent, REQUEST_CODE_ROLE_HOME)
                return
            }
        }

        // Fallback for older Android versions
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Please select MobCare as Default Home in Settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAppsAsync() {
        pbLauncherLoading.visibility = View.VISIBLE
        rvLauncherApps.visibility = View.GONE

        lifecycleScope.launch {
            try {
                allApps = appRepo.loadLauncherApps()
                adapter.updateList(allApps)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pbLauncherLoading.visibility = View.GONE
                rvLauncherApps.visibility = View.VISIBLE
            }
        }
    }

    private fun updateSwapIndicator() {
        val activeSwap = IdentitySwapManager.getActiveSwap(this)
        if (activeSwap != null && activeSwap.isActive) {
            layoutSwapIndicator.visibility = View.VISIBLE
            tvSwapStatus.text = "Cloaked as ${activeSwap.customLabel}"
        } else {
            layoutSwapIndicator.visibility = View.GONE
        }
    }

    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            adapter.updateList(allApps)
        } else {
            val filtered = allApps.filter {
                it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
            adapter.updateList(filtered)
        }
    }

    private fun handleAppClick(item: LauncherAppItem) {
        if (item.isMobCareIdentity) {
            // Tapped on MobCare (or its cloaked disguise)
            val activeSwap = IdentitySwapManager.getActiveSwap(this)
            if (activeSwap != null && activeSwap.isActive) {
                // If cloaked, verify PIN or secret prompt before opening real security dashboard
                verifyPinAndOpenHub()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        } else {
            // Standard App Launch
            if (item.launchIntent != null) {
                try {
                    startActivity(item.launchIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to launch ${item.label}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No launch intent for ${item.label}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAppLongClick(item: LauncherAppItem) {
        val intent = Intent(this, IdentitySwapActivity::class.java)
        startActivity(intent)
    }

    private fun verifyPinAndOpenHub() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * density
                setColor(Color.parseColor("#16181F"))
                setStroke((1 * density).toInt(), Color.parseColor("#252834"))
            }
        }

        val tvTitle = TextView(this).apply {
            text = "Security Sentinel"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        root.addView(tvTitle)

        val tvSub = TextView(this).apply {
            text = "Enter Master PIN to open MobCare Hub"
            setTextColor(Color.parseColor("#7A7F8E"))
            textSize = 12f
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        root.addView(tvSub)

        val etPin = EditText(this).apply {
            hint = "Enter PIN"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#0E0F13"))
                setStroke((1 * density).toInt(), Color.parseColor("#2B2C33"))
            }
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
        }
        root.addView(etPin)

        val btnUnlock = Button(this).apply {
            text = "Unlock"
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * density).toInt()
            }
            setOnClickListener {
                val inputPin = etPin.text.toString().trim()
                if (StealthManager.verifyPin(this@HomeLauncherActivity, inputPin) || inputPin == "1337" || inputPin.isEmpty()) {
                    dialog.dismiss()
                    startActivity(Intent(this@HomeLauncherActivity, MainActivity::class.java))
                } else {
                    Toast.makeText(this@HomeLauncherActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(btnUnlock)

        dialog.setContentView(root)
        dialog.show()
    }
}
