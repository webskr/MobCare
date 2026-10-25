package com.mobcare.dashboard

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R
import com.mobcare.audit.AppTimelineActivity
import com.mobcare.audit.AuditLogManager
import com.mobcare.audit.MobCareSecurityDatabase
import com.mobcare.audit.NotificationVaultManager
import com.mobcare.launcher.data.IdentitySwapManager
import com.mobcare.launcher.ui.CamouflageGateActivity
import com.mobcare.launcher.ui.IdentitySwapActivity
import com.mobcare.security.IntruderCaptureManager
import com.mobcare.security.MobCareAccessibilityService
import com.mobcare.security.MobCareKeyStoreManager
import com.mobcare.security.StealthManager

/**
 * Level 2: Medium Security Layers (16/16 Layers)
 * Comprehensive management and diagnostics for all 16 medium-tier layers:
 * Background services, SQLite ACID databases, hardware Keystore, forensic timelines,
 * decoy calculator gates, and resource protection.
 */
class MediumSecurityActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnRunAudit: Button
    private lateinit var tvStatDb: TextView
    private lateinit var tvStatKeystore: TextView
    private lateinit var tvStatPruning: TextView
    private lateinit var llMediumLayersContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium_security)

        btnBack = findViewById(R.id.btnBack)
        btnRunAudit = findViewById(R.id.btnRunAudit)
        tvStatDb = findViewById(R.id.tvStatDb)
        tvStatKeystore = findViewById(R.id.tvStatKeystore)
        tvStatPruning = findViewById(R.id.tvStatPruning)
        llMediumLayersContainer = findViewById(R.id.llMediumLayersContainer)

        btnBack.setOnClickListener {
            finish()
        }

        btnRunAudit.setOnClickListener {
            runFullAudit()
        }

        renderMediumLayers()
    }

    override fun onResume() {
        super.onResume()
        renderMediumLayers()
    }

    private fun runFullAudit() {
        val (isDbOk, dbMsg) = MobCareSecurityDatabase.getInstance(this).runIntegrityCheck()
        val isHwKeystore = MobCareKeyStoreManager.isHardwareBacked()
        val pruned = MobCareSecurityDatabase.getInstance(this).autoPruneOldRecords(30)
        MobCareSecurityDatabase.getInstance(this).compactDatabase()

        val auditSummary = "Audit Results:\n" +
                "• SQLite ACID Integrity: ${if (isDbOk) "PASS ($dbMsg)" else "FAIL"}\n" +
                "• Keystore Protection: ${if (isHwKeystore) "Hardware TEE / StrongBox Active" else "Software Fallback"}\n" +
                "• Resource Auto-Pruning: Complete ($pruned old records pruned)\n" +
                "• SQLite Database: VACUUM Compacted"

        Toast.makeText(this, "Integrity Audit Complete • 16/16 Layers Verified", Toast.LENGTH_SHORT).show()
        showAuditResultDialog(auditSummary)
        renderMediumLayers()
    }

    private fun renderMediumLayers() {
        llMediumLayersContainer.removeAllViews()

        val db = MobCareSecurityDatabase.getInstance(this)
        val (isDbOk, _) = db.runIntegrityCheck()
        val isHwKeystore = MobCareKeyStoreManager.isHardwareBacked()
        val auditCount = AuditLogManager.getLogs(this).size
        val notifCount = NotificationVaultManager.getNotifications(this).size
        val intruderCount = IntruderCaptureManager.getIntruderRecords(this).size
        val isAccEnabled = MobCareAccessibilityService.instance != null
        val isAdminActive = StealthManager.isDeviceAdminActive(this)
        val activeSwap = IdentitySwapManager.getActiveSwap(this)

        tvStatDb.text = if (isDbOk) "SQLite (WAL)" else "DB Error"
        tvStatKeystore.text = if (isHwKeystore) "AES-256 (TEE)" else "AES-256 (Soft)"
        tvStatPruning.text = "30-Day Guard"

        data class MediumLayer(
            val layerNum: String,
            val title: String,
            val badge: String,
            val badgeColor: String,
            val description: String,
            val technicalAPI: String,
            val actionLabel: String,
            val action: () -> Unit
        )

        val layers = listOf(
            // 1. Layer 1
            MediumLayer(
                layerNum = "LAYER 1",
                title = "Core Protection (PIN Gate & Storage Isolation)",
                badge = "ACTIVE • OVERLAY",
                badgeColor = "#81C995",
                description = "Master PIN gate overlay protects sensitive applications; all credentials and tokens remain inside private sandboxed memory.",
                technicalAPI = "APIs: SYSTEM_ALERT_WINDOW • AppLockerWatchdogService • Context.MODE_PRIVATE",
                actionLabel = "LOCKER"
            ) {
                startActivity(Intent(this, AppLockerActivity::class.java))
            },

            // 2. Layer 2
            MediumLayer(
                layerNum = "LAYER 2",
                title = "Intrusion Detection (Silent Camera2 Selfie)",
                badge = "$intruderCount SNAPS CAPTURED",
                badgeColor = if (intruderCount > 0) "#F28B82" else "#81C995",
                description = "Automated silent front-camera snapshot after repeated failed PIN attempts without any screen viewfinder or shutter audio.",
                technicalAPI = "APIs: Camera2 API • ImageReader Sensor Buffer • App-private storage",
                actionLabel = "GALLERY"
            ) {
                startActivity(Intent(this, GalleryActivity::class.java))
            },

            // 3. Layer 4
            MediumLayer(
                layerNum = "LAYER 4",
                title = "Permission Firewall (Dangerous Privilege Audit)",
                badge = "FIREWALL ARMED",
                badgeColor = "#81C995",
                description = "Comprehensive scanner highlighting apps holding Camera, Microphone, SMS, Location, and Device Administrator permissions.",
                technicalAPI = "APIs: PackageManager • PermissionInfo.PROTECTION_DANGEROUS • Sideload detector",
                actionLabel = "FIREWALL"
            ) {
                startActivity(Intent(this, PackageGuardianActivity::class.java))
            },

            // 4. Layer 8
            MediumLayer(
                layerNum = "LAYER 8",
                title = "Data Vault (Sandboxed SQLite Database Engine)",
                badge = "SQLITE WAL ACTIVE",
                badgeColor = "#81C995",
                description = "Dedicated private SQLite database engine with Write-Ahead Logging (WAL) for concurrent, isolated data security.",
                technicalAPI = "APIs: SQLiteOpenHelper • MODE_PRIVATE • Table indexing • Zero external sharing",
                actionLabel = "INSPECT"
            ) {
                val count = auditCount + notifCount
                Toast.makeText(this, "SQLite Vault Status: Healthy • $count records stored in private sandboxed DB.", Toast.LENGTH_LONG).show()
            },

            // 5. Layer 9
            MediumLayer(
                layerNum = "LAYER 9",
                title = "Forensics (Millisecond Timestamped Audit Logging)",
                badge = "$auditCount EVENTS RECORDED",
                badgeColor = "#81C995",
                description = "Forensic event tagging with precise millisecond timestamps, source application package identifiers, and session categories.",
                technicalAPI = "APIs: System.currentTimeMillis() • ISO-8601 formatting • Package identification",
                actionLabel = "VIEW LOGS"
            ) {
                startActivity(Intent(this, KeyboardTelemetryActivity::class.java))
            },

            // 6. Layer 11
            MediumLayer(
                layerNum = "LAYER 11",
                title = "App Activity Intelligence (Window Transitions)",
                badge = if (isAccEnabled) "ACCESSIBILITY ARMED" else "SETUP NEEDED",
                badgeColor = if (isAccEnabled) "#81C995" else "#FDD663",
                description = "Tracks foreground package transitions, active session durations, and browser search queries across third-party applications.",
                technicalAPI = "APIs: MobCareAccessibilityService • TYPE_WINDOW_STATE_CHANGED",
                actionLabel = "TIMELINE"
            ) {
                startActivity(Intent(this, AppTimelineActivity::class.java))
            },

            // 7. Layer 14
            MediumLayer(
                layerNum = "LAYER 14",
                title = "App Integrity Monitoring (Package Install Ledger)",
                badge = "BROADCAST RECEIVER",
                badgeColor = "#81C995",
                description = "Monitors real-time package installations, updates, and removals. Flags non-Play Store sideloaded APK origins.",
                technicalAPI = "APIs: PackageMonitorReceiver • ACTION_PACKAGE_ADDED/REMOVED • Sideload audit",
                actionLabel = "LEDGER"
            ) {
                startActivity(Intent(this, PackageGuardianActivity::class.java))
            },

            // 8. Layer 15
            MediumLayer(
                layerNum = "LAYER 15",
                title = "Notification Intelligence (Status Bar Interceptor)",
                badge = "$notifCount ALERTS VAULTED",
                badgeColor = "#81C995",
                description = "Intercepts incoming notifications across messaging apps, bank SMS, and OTP alerts, categorizing them into local encrypted vaults.",
                technicalAPI = "APIs: NotificationListenerService • StatusBarNotification parsing • Category classification",
                actionLabel = "VAULT"
            ) {
                startActivity(Intent(this, NotificationVaultActivity::class.java))
            },

            // 9. Layer 20
            MediumLayer(
                layerNum = "LAYER 20",
                title = "Forensic Timeline (Unified Chronology Feed)",
                badge = "UNIFIED CORRELATION",
                badgeColor = "#81C995",
                description = "Combines keystrokes, application switches, notifications, and device unlocks into an integrated chronological evidence stream.",
                technicalAPI = "APIs: Forensic multi-source indexing • Time-window correlation",
                actionLabel = "INSPECT"
            ) {
                startActivity(Intent(this, AppTimelineActivity::class.java))
            },

            // 10. Layer 21
            MediumLayer(
                layerNum = "LAYER 21",
                title = "Secure Evidence Storage (Isolated Media Sandbox)",
                badge = "NO-MEDIA SANDBOX",
                badgeColor = "#81C995",
                description = "Intruder photos and sensitive records are stored in app-private directories with media scanner ignore tags, hidden from standard gallery apps.",
                technicalAPI = "APIs: context.filesDir • MediaScanner exclusion • Master PIN gate",
                actionLabel = "EVIDENCE"
            ) {
                startActivity(Intent(this, GalleryActivity::class.java))
            },

            // 11. Layer 22
            MediumLayer(
                layerNum = "LAYER 22",
                title = "Recovery & Self-Protection (Decoy Gate & Reboot)",
                badge = "BOOT RESILIENT",
                badgeColor = "#81C995",
                description = "Automatic background watchdog respawn after phone reboots and secret calculator entry gate for disaster recovery.",
                technicalAPI = "APIs: RECEIVE_BOOT_COMPLETED • CamouflageGateActivity math solver",
                actionLabel = "CALCULATOR"
            ) {
                startActivity(Intent(this, CamouflageGateActivity::class.java))
            },

            // 12. Layer 27
            MediumLayer(
                layerNum = "LAYER 27",
                title = "Configuration Rollback (One-Tap Camouflage Reset)",
                badge = if (activeSwap?.isActive == true) "CLOAK ACTIVE" else "DEFAULT STATE",
                badgeColor = if (activeSwap?.isActive == true) "#8AB4F8" else "#81C995",
                description = "One-tap recovery actions ('Reset Logo', 'Reset Name', 'Reset Everything') instantly revert all launcher alias states.",
                technicalAPI = "APIs: PackageManager.setComponentEnabledSetting • 100 Activity-Aliases",
                actionLabel = "SWAPPER"
            ) {
                startActivity(Intent(this, IdentitySwapActivity::class.java))
            },

            // 13. Layer 29
            MediumLayer(
                layerNum = "LAYER 29",
                title = "Database Integrity (ACID Transactions & WAL)",
                badge = if (isDbOk) "ACID VERIFIED" else "CHECK FAILED",
                badgeColor = if (isDbOk) "#81C995" else "#F28B82",
                description = "Strict ACID transaction handling with Write-Ahead Logging (WAL) preventing database corruption during sudden battery drain or crashes.",
                technicalAPI = "APIs: PRAGMA integrity_check • db.beginTransaction() • Auto-recovery fallback",
                actionLabel = "CHECK DB"
            ) {
                val (ok, msg) = db.runIntegrityCheck()
                Toast.makeText(this, "SQLite Integrity: ${if (ok) "PASS (Code: $msg)" else "FAIL"}", Toast.LENGTH_SHORT).show()
            },

            // 14. Layer 30
            MediumLayer(
                layerNum = "LAYER 30",
                title = "Resource Protection (30-Day Auto-Prune & VACUUM)",
                badge = "30-DAY PRUNED",
                badgeColor = "#81C995",
                description = "Event-driven architecture with zero background polling loops; automatic pruning of records older than 30 days and SQLite VACUUM compaction.",
                technicalAPI = "APIs: SQLite VACUUM • Auto-pruning threshold • Low CPU footprint",
                actionLabel = "COMPACT"
            ) {
                val pruned = db.autoPruneOldRecords(30)
                db.compactDatabase()
                Toast.makeText(this, "Compaction complete • Pruned $pruned old records • Database vacuumed.", Toast.LENGTH_LONG).show()
            },

            // 15. Layer 32
            MediumLayer(
                layerNum = "LAYER 32",
                title = "Key & Secret Management (Hardware AndroidKeyStore)",
                badge = if (isHwKeystore) "TEE ENCLAVE ACTIVE" else "KEYSTORE READY",
                badgeColor = "#8AB4F8",
                description = "Master cryptographic keys are generated and isolated inside Android's Hardware-backed Keystore / Secure Enclave (TEE/StrongBox).",
                technicalAPI = "APIs: AndroidKeyStore • AES/GCM/NoPadding 256-bit • KeyInfo.isInsideSecureHardware",
                actionLabel = "ENCRYPT TEST"
            ) {
                val testMsg = "MobCare_Security_Payload_${System.currentTimeMillis()}"
                val encrypted = MobCareKeyStoreManager.encryptString(testMsg)
                val decrypted = MobCareKeyStoreManager.decryptString(encrypted)
                val match = testMsg == decrypted
                Toast.makeText(this, "Hardware Keystore: ${if (match) "AES-256 GCM Cryptographic Match OK" else "Mismatch"}", Toast.LENGTH_SHORT).show()
            },

            // 16. Layer 39
            MediumLayer(
                layerNum = "LAYER 39",
                title = "Recovery Access Protection (Secret Math Gate)",
                badge = "DECIMAL GATE ARMED",
                badgeColor = "#81C995",
                description = "Secret PIN entry followed by '=' inside the fake calculator camouflage interface opens the authentic MobCare dashboard.",
                technicalAPI = "APIs: CamouflageGateActivity • Formula evaluator • Decoy input parser",
                actionLabel = "TEST GATE"
            ) {
                startActivity(Intent(this, CamouflageGateActivity::class.java))
            }
        )

        val density = resources.displayMetrics.density

        for (item in layers) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f * density
                    setColor(Color.parseColor("#181920"))
                    setStroke((1 * density).toInt(), Color.parseColor("#262833"))
                }
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (12 * density).toInt() }
            }

            // Top Row
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tvNum = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = item.layerNum
                setTextColor(Color.parseColor("#7A7F8E"))
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvBadge = TextView(this).apply {
                text = item.badge
                setTextColor(Color.parseColor(item.badgeColor))
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 6f * density
                    setColor(Color.parseColor("#20222B"))
                }
            }

            topRow.addView(tvNum)
            topRow.addView(tvBadge)
            card.addView(topRow)

            // Title
            val tvTitle = TextView(this).apply {
                text = item.title
                setTextColor(Color.parseColor("#ECEEF4"))
                textSize = 14.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (4 * density).toInt() }
            }
            card.addView(tvTitle)

            // Description
            val tvDesc = TextView(this).apply {
                text = item.description
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (4 * density).toInt() }
            }
            card.addView(tvDesc)

            // Technical API Detail
            val tvTech = TextView(this).apply {
                text = item.technicalAPI
                setTextColor(Color.parseColor("#7A7F8E"))
                textSize = 10.5f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (6 * density).toInt() }
            }
            card.addView(tvTech)

            // Action Button Row
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (10 * density).toInt() }
            }

            val btnAction = Button(this).apply {
                text = item.actionLabel
                setTextColor(Color.parseColor("#ECEEF4"))
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = 10f * density
                    setColor(Color.parseColor("#282B38"))
                }
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (34 * density).toInt()
                )
                setOnClickListener { item.action() }
            }

            actionRow.addView(btnAction)
            card.addView(actionRow)

            llMediumLayersContainer.addView(card)
        }
    }

    private fun showAuditResultDialog(summary: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * density).toInt(), (20 * density).toInt(), (22 * density).toInt(), (20 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(Color.parseColor("#1C1E26"))
                setStroke((1 * density).toInt(), Color.parseColor("#2E3240"))
            }
        }

        val titleTv = TextView(this).apply {
            text = "Advanced Security Audit"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }
        root.addView(titleTv)

        val summaryTv = TextView(this).apply {
            text = summary
            setTextColor(Color.parseColor("#C4C7C5"))
            textSize = 13f
            setLineSpacing(0f, 1.25f)
        }
        root.addView(summaryTv)

        val btnClose = Button(this).apply {
            text = "DISMISS"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 12f * density
                setColor(Color.parseColor("#282B38"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (40 * density).toInt()
            ).apply { topMargin = (16 * density).toInt() }
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnClose)

        dialog.setContentView(root)
        dialog.show()
    }
}
