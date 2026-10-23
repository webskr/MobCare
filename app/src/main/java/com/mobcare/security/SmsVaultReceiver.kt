package com.mobcare.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.mobcare.audit.AuditLogManager
import com.mobcare.audit.CallSmsVaultManager
import com.mobcare.audit.SmsVaultItem
import java.util.UUID

class SmsVaultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: "Unknown Sender"
                    val body = sms.displayMessageBody ?: ""
                    val timestamp = sms.timestampMillis

                    val item = SmsVaultItem(
                        id = UUID.randomUUID().toString(),
                        address = sender,
                        contactName = "Sender",
                        body = body,
                        type = "INCOMING",
                        timestamp = timestamp
                    )
                    CallSmsVaultManager.saveSms(context, item)

                    AuditLogManager.logSystemEvent(
                        context,
                        "com.android.mms",
                        "SMS Sentinel",
                        "[SMS Intercepted] From: $sender - Body: $body"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}