package com.httpsms.pairing

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun timestamp(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date())

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra(AndroidSmsDispatcher.EXTRA_MESSAGE_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val storage = SecureStorageManager(context.applicationContext)
                if (!storage.isPaired) return@launch
                val api = SmsBridgeApi(RelayApiClient(storage))
                val timestamp = timestamp()
                when (intent.action) {
                    AndroidSmsDispatcher.ACTION_SENT -> if (resultCode == Activity.RESULT_OK) {
                        api.sendSentEvent(messageId.substringBeforeLast('.'), timestamp)
                    } else {
                        api.sendFailedEvent(messageId.substringBeforeLast('.'), timestamp, "SMS send failed with result code $resultCode")
                    }
                    AndroidSmsDispatcher.ACTION_DELIVERED -> if (resultCode == Activity.RESULT_OK) {
                        api.sendDeliveredEvent(messageId.substringBeforeLast('.'), timestamp)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
