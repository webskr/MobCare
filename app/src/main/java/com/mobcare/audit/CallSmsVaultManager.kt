package com.mobcare.audit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CallVaultItem(
    val id: String,
    val number: String,
    val contactName: String,
    val type: String, // INCOMING, OUTGOING, MISSED
    val durationSeconds: Long,
    val timestamp: Long
)

data class SmsVaultItem(
    val id: String,
    val address: String,
    val contactName: String,
    val body: String,
    val type: String, // INCOMING, OUTGOING
    val timestamp: Long
)

object CallSmsVaultManager {

    private const val CALLS_FILE = "mobcare_vault_calls.json"
    private const val SMS_FILE = "mobcare_vault_sms.json"

    fun hasPermissions(context: Context): Boolean {
        val callLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val phoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val sms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        return callLog && phoneState && sms
    }

    // --- CALL VAULT ---

    @Synchronized
    fun getCalls(context: Context): List<CallVaultItem> {
        val file = File(context.filesDir, CALLS_FILE)
        if (!file.exists()) return emptyList()
        val list = mutableListOf<CallVaultItem>()
        try {
            val jsonArray = JSONArray(file.readText(Charsets.UTF_8))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CallVaultItem(
                        id = obj.optString("id"),
                        number = obj.optString("number"),
                        contactName = obj.optString("contactName", "Unknown"),
                        type = obj.optString("type", "INCOMING"),
                        durationSeconds = obj.optLong("durationSeconds", 0),
                        timestamp = obj.optLong("timestamp", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun saveCall(context: Context, item: CallVaultItem) {
        val current = getCalls(context).toMutableList()
        // Avoid duplicate by number + timestamp within 5 seconds
        if (current.any { it.number == item.number && Math.abs(it.timestamp - item.timestamp) < 5000 }) {
            return
        }
        current.add(0, item)
        val jsonArray = JSONArray()
        current.take(500).forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("number", c.number)
                put("contactName", c.contactName)
                put("type", c.type)
                put("durationSeconds", c.durationSeconds)
                put("timestamp", c.timestamp)
            }
            jsonArray.put(obj)
        }
        File(context.filesDir, CALLS_FILE).writeText(jsonArray.toString(), Charsets.UTF_8)
    }

    fun syncSystemCallLog(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return
        try {
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 50"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)

                while (it.moveToNext()) {
                    val id = it.getString(idIdx) ?: ""
                    val num = it.getString(numIdx) ?: "Private"
                    val name = it.getString(nameIdx) ?: "Unknown Contact"
                    val typeInt = it.getInt(typeIdx)
                    val dur = it.getLong(durIdx)
                    val date = it.getLong(dateIdx)

                    val typeStr = when (typeInt) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                        else -> "CALL"
                    }

                    saveCall(context, CallVaultItem(id, num, name, typeStr, dur, date))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- SMS VAULT ---

    @Synchronized
    fun getSms(context: Context): List<SmsVaultItem> {
        val file = File(context.filesDir, SMS_FILE)
        if (!file.exists()) return emptyList()
        val list = mutableListOf<SmsVaultItem>()
        try {
            val jsonArray = JSONArray(file.readText(Charsets.UTF_8))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SmsVaultItem(
                        id = obj.optString("id"),
                        address = obj.optString("address"),
                        contactName = obj.optString("contactName", "Unknown"),
                        body = obj.optString("body"),
                        type = obj.optString("type", "INCOMING"),
                        timestamp = obj.optLong("timestamp", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun saveSms(context: Context, item: SmsVaultItem) {
        val current = getSms(context).toMutableList()
        if (current.any { it.address == item.address && it.body == item.body && Math.abs(it.timestamp - item.timestamp) < 5000 }) {
            return
        }
        current.add(0, item)
        val jsonArray = JSONArray()
        current.take(500).forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("address", s.address)
                put("contactName", s.contactName)
                put("body", s.body)
                put("type", s.type)
                put("timestamp", s.timestamp)
            }
            jsonArray.put(obj)
        }
        File(context.filesDir, SMS_FILE).writeText(jsonArray.toString(), Charsets.UTF_8)
    }

    fun syncSystemSms(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.TYPE, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 50"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val id = it.getString(idIdx) ?: ""
                    val addr = it.getString(addrIdx) ?: "Unknown"
                    val body = it.getString(bodyIdx) ?: ""
                    val typeInt = it.getInt(typeIdx)
                    val date = it.getLong(dateIdx)
                    val typeStr = if (typeInt == Telephony.Sms.MESSAGE_TYPE_SENT) "OUTGOING" else "INCOMING"

                    saveSms(context, SmsVaultItem(id, addr, "Contact", body, typeStr, date))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAll(context: Context) {
        File(context.filesDir, CALLS_FILE).delete()
        File(context.filesDir, SMS_FILE).delete()
    }
}