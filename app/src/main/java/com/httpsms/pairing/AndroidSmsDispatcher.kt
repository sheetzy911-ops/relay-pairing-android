package com.httpsms.pairing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class AndroidSmsDispatcher(private val context: Context) {
    fun send(message: BridgeMessage) {
        require(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            "SEND_SMS permission is required"
        }
        require(message.contact.isNotBlank()) { "The contact phone number is empty." }
        require(message.attachments.isNullOrEmpty()) { "MMS attachments are not yet migrated." }
        require(!message.encrypted) { "Encrypted outgoing messages require the original encryption-key migration." }

        val smsManager = smsManagerFor(message.sim)
        val parts = smsManager.divideMessage(message.content)
        if (parts.size == 1) {
            smsManager.sendTextMessage(message.contact, null, message.content, intentFor(ACTION_SENT, message.id), intentFor(ACTION_DELIVERED, message.id))
        } else {
            val sent = parts.mapIndexed { index, _ -> intentFor(ACTION_SENT, if (index == parts.lastIndex) message.id else "${message.id}.$index") }
            val delivered = parts.mapIndexed { index, _ -> intentFor(ACTION_DELIVERED, if (index == parts.lastIndex) message.id else "${message.id}.$index") }
            smsManager.sendMultipartTextMessage(message.contact, null, parts, ArrayList(sent), ArrayList(delivered))
        }
    }

    private fun intentFor(action: String, messageId: String): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_MESSAGE_ID, messageId)
        return PendingIntent.getBroadcast(
            context,
            ("$action:$messageId").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun smsManagerFor(sim: String): SmsManager {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        val active: List<SubscriptionInfo> = subscriptionManager?.activeSubscriptionInfoList.orEmpty()
        val index = when (sim.uppercase()) { "SIM2" -> 1; else -> 0 }
        val subscriptionId = active.getOrNull(index)?.subscriptionId
            ?: SubscriptionManager.getDefaultSmsSubscriptionId()
        require(subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) { "No active SMS subscription is available for $sim" }
        return SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
    }

    companion object {
        const val ACTION_SENT = "com.httpsms.SMS_SENT"
        const val ACTION_DELIVERED = "com.httpsms.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "message_id"
    }
}
