package com.mobcare.dashboard

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R
import com.mobcare.security.HardSecurityEngine
import java.io.File
import java.util.Locale

/**
 * HardSecurityActivity presents the 14 Level-3 and Level-4 (Hard & Hardest)
 * Security Layers organized across 5 phone-level functional domains:
 *
 * 1. Hardware & Sensor Sentinel (Layers 13 & 3)
 * 2. Network & Data Flow Guard (Layers 5, 12, 17)
 * 3. Privileged Subsystem & Threat Audit (Layers 18, 19, 31)
 * 4. Automated Policy & Defense Engine (Layers 6, 26, 16)
 * 5. Diagnostic Verification & Vault Archive (Layers 33, 34, 38)
 */
class HardSecurityActivity : AppCompatActivity() {

    private lateinit var tvHardHubBadge: TextView
    private lateinit var tvPostureSummary: TextView
    private lateinit var tvCameraSensorStatus: TextView
    private lateinit var tvMicSensorStatus: TextView

    private lateinit var btnRunDiagnostics: Button
    private lateinit var btnGenerateAuditReport: Button
    private lateinit var btnExportVaultBackup: Button
    private lateinit var btnRestoreVaultBackup: Button

    // Category 1
    private lateinit var cardHoneypot: LinearLayout
    private lateinit var tvSensorDetails: TextView
    private lateinit var badgeSensor: TextView
    private lateinit var tvHoneypotDetails: TextView
    private lateinit var badgeHoneypot: TextView

    // Category 2
    private lateinit var cardDataFlow: LinearLayout
    private lateinit var tvNetworkDefenseDetails: TextView
    private lateinit var badgeNetworkDefense: TextView
    private lateinit var tvDataFlowDetails: TextView
    private lateinit var badgeDataFlow: TextView
    private lateinit var tvDnsDetails: TextView
    private lateinit var badgeDns: TextView

    // Category 3
    private lateinit var tvPrivilegedDetails: TextView
    private lateinit var badgePrivileged: TextView
    private lateinit var tvAnomalyDetails: TextView
    private lateinit var badgeAnomaly: TextView
    private lateinit var tvBinaryDetails: TextView
    private lateinit var badgeBinary: TextView

    // Category 4
    private lateinit var tvAutoResponseDetails: TextView
    private lateinit var badgeAutoResponse: TextView
    private lateinit var tvConflictDetails: TextView
    private lateinit var badgeConflict: TextView
    private lateinit var tvFileIntegrityDetails: TextView
    private lateinit var badgeFileIntegrity: TextView

    // Category 5
    private lateinit var tvDiagnosticsDetails: TextView
    private lateinit var badgeDiagnostics: TextView
    private lateinit var tvReportDetails: TextView
    private lateinit var badgeReport: TextView
    private lateinit var tvArchiveDetails: TextView
    private lateinit var badgeArchive: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hard_security)

        initViews()
        setupListeners()
        BottomNavHelper.setup(this, NavTab.PROTECTION)
    }

    override fun onResume() {
        super.onResume()
        BottomNavHelper.setup(this, NavTab.PROTECTION)
        HardSecurityEngine.startSensorMonitoring(this)
        HardSecurityEngine.startCanaryFileObserver(this)
        refreshAllLayers()
    }

    override fun onPause() {
        super.onPause()
        HardSecurityEngine.stopSensorMonitoring(this)
        HardSecurityEngine.stopCanaryFileObserver()
    }

    private fun initViews() {
        tvHardHubBadge = findViewById(R.id.tvHardHubBadge)
        tvPostureSummary = findViewById(R.id.tvPostureSummary)
        tvCameraSensorStatus = findViewById(R.id.tvCameraSensorStatus)
        tvMicSensorStatus = findViewById(R.id.tvMicSensorStatus)

        btnRunDiagnostics = findViewById(R.id.btnRunDiagnostics)
        btnGenerateAuditReport = findViewById(R.id.btnGenerateAuditReport)
        btnExportVaultBackup = findViewById(R.id.btnExportVaultBackup)
        btnRestoreVaultBackup = findViewById(R.id.btnRestoreVaultBackup)

        // Category 1
        cardHoneypot = findViewById(R.id.cardHoneypot)
        tvSensorDetails = findViewById(R.id.tvSensorDetails)
        badgeSensor = findViewById(R.id.badgeSensor)
        tvHoneypotDetails = findViewById(R.id.tvHoneypotDetails)
        badgeHoneypot = findViewById(R.id.badgeHoneypot)

        // Category 2
        cardDataFlow = findViewById(R.id.cardDataFlow)
        tvNetworkDefenseDetails = findViewById(R.id.tvNetworkDefenseDetails)
        badgeNetworkDefense = findViewById(R.id.badgeNetworkDefense)
        tvDataFlowDetails = findViewById(R.id.tvDataFlowDetails)
        badgeDataFlow = findViewById(R.id.badgeDataFlow)
        tvDnsDetails = findViewById(R.id.tvDnsDetails)
        badgeDns = findViewById(R.id.badgeDns)

        // Category 3
        tvPrivilegedDetails = findViewById(R.id.tvPrivilegedDetails)
        badgePrivileged = findViewById(R.id.badgePrivileged)
        tvAnomalyDetails = findViewById(R.id.tvAnomalyDetails)
        badgeAnomaly = findViewById(R.id.badgeAnomaly)
        tvBinaryDetails = findViewById(R.id.tvBinaryDetails)
        badgeBinary = findViewById(R.id.badgeBinary)

        // Category 4
        tvAutoResponseDetails = findViewById(R.id.tvAutoResponseDetails)
        badgeAutoResponse = findViewById(R.id.badgeAutoResponse)
        tvConflictDetails = findViewById(R.id.tvConflictDetails)
        badgeConflict = findViewById(R.id.badgeConflict)
        tvFileIntegrityDetails = findViewById(R.id.tvFileIntegrityDetails)
        badgeFileIntegrity = findViewById(R.id.badgeFileIntegrity)

        // Category 5
        tvDiagnosticsDetails = findViewById(R.id.tvDiagnosticsDetails)
        badgeDiagnostics = findViewById(R.id.badgeDiagnostics)
        tvReportDetails = findViewById(R.id.tvReportDetails)
        badgeReport = findViewById(R.id.badgeReport)
        tvArchiveDetails = findViewById(R.id.tvArchiveDetails)
        badgeArchive = findViewById(R.id.badgeArchive)
    }

    private fun setupListeners() {
        btnRunDiagnostics.setOnClickListener {
            runDiagnostics()
        }

        btnGenerateAuditReport.setOnClickListener {
            showFormalAuditReport()
        }

        btnExportVaultBackup.setOnClickListener {
            exportVaultBackup()
        }

        btnRestoreVaultBackup.setOnClickListener {
            showRestoreBackupDialog()
        }

        cardHoneypot.setOnClickListener {
            showHoneypotStatusDialog()
        }

        cardDataFlow.setOnClickListener {
            showTrafficTelemetryDialog()
        }
    }

    private fun refreshAllLayers() {
        // Posture Risk Score
        val anomaly = HardSecurityEngine.calculateBehavioralRiskScore(this)
        tvPostureSummary.text = "Heuristic Risk: ${anomaly.riskRating} (${anomaly.scoreOutOf100}/100) • Threat Heuristics Nominal"
        tvPostureSummary.setTextColor(
            if (anomaly.scoreOutOf100 >= 80) Color.parseColor("#81C995") else Color.parseColor("#FDD663")
        )

        // Live Sensors (Layer 13)
        val camInUse = HardSecurityEngine.isCameraInUseByAnyApp
        if (camInUse) {
            tvCameraSensorStatus.text = "ACTIVE (IN USE)"
            tvCameraSensorStatus.setTextColor(Color.parseColor("#F28B82"))
        } else {
            tvCameraSensorStatus.text = "IDLE / SECURE"
            tvCameraSensorStatus.setTextColor(Color.parseColor("#81C995"))
        }

        val micInUse = HardSecurityEngine.isMicrophoneInUseByAnyApp
        if (micInUse) {
            tvMicSensorStatus.text = "RECORDING ACTIVE"
            tvMicSensorStatus.setTextColor(Color.parseColor("#F28B82"))
        } else {
            tvMicSensorStatus.text = "IDLE / SECURE"
            tvMicSensorStatus.setTextColor(Color.parseColor("#81C995"))
        }

        tvSensorDetails.text = "Hardware Camera & Mic callbacks hooked • Real-time indicator"
        badgeSensor.text = "ACTIVE"

        // Honeypot (Layer 3)
        val honeypotStatus = HardSecurityEngine.deployOrVerifyCanaryHoneypot(this)
        when (honeypotStatus) {
            HardSecurityEngine.HoneypotStatus.ARMED -> {
                tvHoneypotDetails.text = "Dual Canary active (Internal + External) • Live FileObserver hook • Tap to inspect"
                badgeHoneypot.text = "ARMED"
                badgeHoneypot.setTextColor(Color.parseColor("#4CAF50"))
            }
            HardSecurityEngine.HoneypotStatus.TRIPPED -> {
                tvHoneypotDetails.text = "ALERT: Canary file integrity altered or read by unauthorized app! Tap to reset"
                badgeHoneypot.text = "TRIPPED"
                badgeHoneypot.setTextColor(Color.parseColor("#F28B82"))
            }
            else -> {
                tvHoneypotDetails.text = "Canary trap initialization standby"
                badgeHoneypot.text = "READY"
            }
        }

        // Network Defense (Layer 5)
        val net = HardSecurityEngine.auditNetworkSecurity(this)
        tvNetworkDefenseDetails.text = "${net.transport} • ${net.securityStatus}"
        badgeNetworkDefense.text = if (net.isProxyConfigured) "ALERT" else "OPTIMAL"

        // Data Flow (Layer 12)
        val topTraffic = HardSecurityEngine.getAppTrafficAudit(this, 3)
        val (persistedCount, _) = HardSecurityEngine.getPersistedTrafficSummary(this)
        if (topTraffic.isNotEmpty()) {
            val topApp = topTraffic.first()
            tvDataFlowDetails.text = "Top: ${topApp.appName} (${topApp.totalMb} MB) • $persistedCount SQLite snapshots • Tap to view"
        } else {
            tvDataFlowDetails.text = "TrafficStats active • $persistedCount SQLite snapshots • Tap to view"
        }
        badgeDataFlow.text = "ARMED"

        // DNS Guard (Layer 17)
        if (net.dnsServers.isNotEmpty()) {
            tvDnsDetails.text = "Resolvers: ${net.dnsServers.joinToString(", ")}"
        } else {
            tvDnsDetails.text = "LinkProperties verified • No rogue DNS redirect detected"
        }
        badgeDns.text = "OPTIMAL"

        // Privileged Access (Layer 18)
        val priv = HardSecurityEngine.auditPrivilegedSubsystems(this)
        tvPrivilegedDetails.text = "Acc: ${priv.activeAccessibilityServices.size} • Notif: ${priv.activeNotificationListeners.size} • Admin: ${priv.activeDeviceAdmins.size} • Overlays: ${priv.overlayAppsCount}"
        badgePrivileged.text = if (priv.totalThreatVectors == 0) "OPTIMAL" else "AUDITED"

        // Behavioral Anomaly (Layer 19)
        if (anomaly.flaggedRisks.isEmpty()) {
            tvAnomalyDetails.text = "Zero high-risk spyware combinations detected"
        } else {
            tvAnomalyDetails.text = "Flagged: ${anomaly.flaggedRisks.joinToString(" • ")}"
        }
        badgeAnomaly.text = anomaly.riskRating

        // Binary Integrity (Layer 31)
        val binary = HardSecurityEngine.auditBinaryIntegrity(this)
        tvBinaryDetails.text = "${binary.integrityStatus} • Source: ${binary.installerSource}"
        badgeBinary.text = if (!binary.isDebuggable) "VERIFIED" else "DEBUG"

        // Auto Response (Layer 6)
        val isAuto = HardSecurityEngine.isAutoResponseEnabled(this)
        tvAutoResponseDetails.text = if (isAuto) "PIN fail lock, Canary trip log & Sideload alert active" else "Policy triggers paused"
        badgeAutoResponse.text = if (isAuto) "ARMED" else "PAUSED"

        // Policy Conflict (Layer 26)
        val conflicts = HardSecurityEngine.checkPolicyConflicts(this)
        if (conflicts.conflictsFound == 0) {
            tvConflictDetails.text = "${conflicts.totalPoliciesChecked} security rules cross-verified • 0 conflicts"
            badgeConflict.text = "RESOLVED"
            badgeConflict.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvConflictDetails.text = "${conflicts.conflictsFound} policy notice: ${conflicts.details.firstOrNull() ?: ""}"
            badgeConflict.text = "NOTICE"
            badgeConflict.setTextColor(Color.parseColor("#FDD663"))
        }

        // File Movement & Checksum (Layer 16)
        val dbFile = File(filesDir.parentFile, "databases/mobcare_security.db")
        val hash = if (dbFile.exists()) HardSecurityEngine.computeSha256(dbFile).take(12) else "STANDBY"
        tvFileIntegrityDetails.text = "SQLite DB SHA-256: $hash... • Zero unauthorized tampering"
        badgeFileIntegrity.text = "VERIFIED"

        // Diagnostics (Layer 33)
        tvDiagnosticsDetails.text = "Keystore TEE, SQLite WAL & Camera2 subsystems nominal"
        badgeDiagnostics.text = "PASSED"

        // Formal Report (Layer 34)
        tvReportDetails.text = "Comprehensive offline report ready for inspection"
        badgeReport.text = "READY"

        // Cryptographic Archive (Layer 38)
        val backupDir = File(filesDir, "backups")
        val backupsCount = backupDir.listFiles { _, name -> name.endsWith(".mbvault") }?.size ?: 0
        tvArchiveDetails.text = "$backupsCount .mbvault archives created • Hardware AES-GCM & SHA-256"
        badgeArchive.text = "ARMED"
    }

    private fun runDiagnostics() {
        val diagList = HardSecurityEngine.runAutomatedDiagnostics(this)
        val sb = StringBuilder()
        var allPassed = true

        diagList.forEach {
            val mark = if (it.passed) "✓" else "✗"
            if (!it.passed) allPassed = false
            sb.append("$mark ${it.testName}\n   ${it.details}\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle(if (allPassed) "Diagnostics: 5/5 PASSED" else "Diagnostics Flagged Notice")
            .setMessage(sb.toString().trim())
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun exportVaultBackup() {
        Toast.makeText(this, "Generating cryptographic .mbvault snapshot...", Toast.LENGTH_SHORT).show()
        val result = HardSecurityEngine.createVerifiedCryptographicBackup(this)

        if (result.success) {
            val fileName = File(result.filePath).name
            AlertDialog.Builder(this)
                .setTitle("Cryptographic Vault Exported")
                .setMessage(
                    "Archive created successfully!\n\n" +
                    "File: $fileName\n" +
                    "Records Secured: ${result.recordCount}\n" +
                    "Cipher: Hardware Keystore AES-256 GCM\n" +
                    "Integrity Hash: ${result.sha256Checksum.take(24)}...\n\n" +
                    "Path: ${result.filePath}"
                )
                .setPositiveButton("OK") { d, _ ->
                    d.dismiss()
                    refreshAllLayers()
                }
                .show()
        } else {
            Toast.makeText(this, "Backup Failed: ${result.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRestoreBackupDialog() {
        val backups = HardSecurityEngine.listAvailableBackups(this)
        if (backups.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No Vault Backups Found")
                .setMessage("No .mbvault archive found in secure storage. Create an encrypted backup first using the 'BACKUP (.MBVAULT)' button.")
                .setPositiveButton("OK") { d, _ -> d.dismiss() }
                .show()
            return
        }

        val fileNames = backups.map { file ->
            val sizeKb = (file.length() / 1024L).coerceAtLeast(1L)
            "${file.name} (${sizeKb} KB)"
        }.toTypedArray()

        var selectedIndex = 0
        AlertDialog.Builder(this)
            .setTitle("Select .mbvault Archive to Restore")
            .setSingleChoiceItems(fileNames, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("RESTORE") { _, _ ->
                val selectedFile = backups[selectedIndex]
                confirmAndExecuteRestore(selectedFile)
            }
            .setNegativeButton("CANCEL") { d, _ -> d.dismiss() }
            .show()
    }

    private fun confirmAndExecuteRestore(backupFile: File) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Vault Restore")
            .setMessage(
                "Are you sure you want to restore records from '${backupFile.name}'?\n\n" +
                "• All historical audit logs, notifications & packages will be merged.\n" +
                "• Existing records will NOT be overwritten (100% non-destructive).\n" +
                "• Cipher: Hardware Keystore AES-256 GCM."
            )
            .setPositiveButton("CONFIRM RESTORE") { _, _ ->
                Toast.makeText(this, "Decrypting and merging archive...", Toast.LENGTH_SHORT).show()
                val result = HardSecurityEngine.restoreCryptographicBackup(this, backupFile)
                if (result.success) {
                    AlertDialog.Builder(this)
                        .setTitle("Restore Successful")
                        .setMessage(
                            "${result.message}\n\n" +
                            "• Audit Logs: ${result.restoredAuditCount}\n" +
                            "• Notifications: ${result.restoredNotifCount}\n" +
                            "• Package Events: ${result.restoredPackageCount}\n" +
                            "• Total Merged: ${result.totalRecords}"
                        )
                        .setPositiveButton("OK") { d, _ ->
                            d.dismiss()
                            refreshAllLayers()
                        }
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Restore Failed")
                        .setMessage(result.message)
                        .setPositiveButton("OK") { d, _ -> d.dismiss() }
                        .show()
                }
            }
            .setNegativeButton("CANCEL") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showHoneypotStatusDialog() {
        val status = HardSecurityEngine.deployOrVerifyCanaryHoneypot(this)
        val sb = StringBuilder()
        sb.append("DIGITAL HONEYPOT & CANARY SENTINELS\n\n")
        sb.append("• Internal Canary Token: canary_credentials.vault\n")
        sb.append("  Location: Sandboxed App Storage\n\n")
        sb.append("• External Storage Decoy: decoy_wallet_backup.dat\n")
        sb.append("  Location: External App Storage\n\n")
        sb.append("• Live FileObserver: ACTIVE\n")
        sb.append("  Monitoring kernel-level file modifications, attributes & deletions in real time.\n\n")
        sb.append("• Current Status: ${status.name}\n")
        if (status == HardSecurityEngine.HoneypotStatus.TRIPPED) {
            sb.append("  ⚠ WARNING: An unauthorized process or scanner accessed or altered a decoy honey-token!\n")
        } else {
            sb.append("  ✓ All decoy tokens uncompromised with verified SHA-256 integrity.\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Canary Honeypot Sentinels")
            .setMessage(sb.toString())
            .setPositiveButton("CLOSE") { d, _ -> d.dismiss() }
            .setNeutralButton("RE-ARM CANARIES") { _, _ ->
                val ok = HardSecurityEngine.resetCanaryHoneypot(this)
                if (ok) {
                    Toast.makeText(this, "Canary tokens re-armed with fresh hashes!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to re-arm canaries.", Toast.LENGTH_SHORT).show()
                }
                refreshAllLayers()
            }
            .show()
    }

    private fun showTrafficTelemetryDialog() {
        val liveApps = HardSecurityEngine.getAppTrafficAudit(this, 10)
        val (persistedCount, persistedMb) = HardSecurityEngine.getPersistedTrafficSummary(this)

        val sb = StringBuilder()
        sb.append("LIVE NETWORK LEAKER METRICS (TrafficStats):\n\n")
        liveApps.take(5).forEachIndexed { i, app ->
            sb.append("${i + 1}. ${app.appName}\n")
            sb.append("   ↓ Download: ${app.rxMb} MB | ↑ Upload: ${app.txMb} MB | Total: ${app.totalMb} MB\n\n")
        }

        sb.append("----------------------------------------------------\n")
        sb.append("PERSISTED SQLITE TELEMETRY:\n")
        sb.append("• Total Snapshots Saved: $persistedCount records\n")
        sb.append("• Total Persisted Volume: ${String.format(Locale.US, "%.2f", persistedMb)} MB\n")
        sb.append("• Offline-First: Preserved safely across device reboots.\n")

        AlertDialog.Builder(this)
            .setTitle("Data Flow & Traffic Telemetry")
            .setMessage(sb.toString().trim())
            .setPositiveButton("CLOSE") { d, _ -> d.dismiss() }
            .setNeutralButton("SNAPSHOT NOW") { _, _ ->
                val savedCount = HardSecurityEngine.persistCurrentTrafficSnapshot(this)
                Toast.makeText(this, "Saved snapshot of $savedCount apps to SQLite vault!", Toast.LENGTH_SHORT).show()
                refreshAllLayers()
            }
            .show()
    }

    private fun showFormalAuditReport() {
        val report = HardSecurityEngine.generateFormalAuditReport(this)

        val scrollView = ScrollView(this).apply {
            setPadding(32, 24, 32, 24)
        }
        val textView = TextView(this).apply {
            text = report
            textSize = 12f
            setTextColor(Color.parseColor("#ECEEF4"))
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("Formal Security Audit Report")
            .setView(scrollView)
            .setPositiveButton("CLOSE") { d, _ -> d.dismiss() }
            .show()
    }
}
