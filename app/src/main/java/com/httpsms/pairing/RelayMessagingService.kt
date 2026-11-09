package com.httpsms.pairing

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RelayMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (!SecureStorageManager(applicationContext).isPaired) return
        if (message.data.containsKey(KEY_HEARTBEAT_ID)) {
            SmsBridgeScheduler.sendHeartbeat(
                applicationContext,
                BridgeDeviceInfo.phoneNumbers(applicationContext),
                BridgeDeviceInfo.isCharging(applicationContext)
            )
            return
        }
        message.data[KEY_MESSAGE_ID]?.let { SmsBridgeScheduler.enqueueOutstanding(applicationContext, it) }
            ?: Log.w(TAG, "FCM message did not include message_id")
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_HEARTBEAT_ID = "heartbeat_id"
        private const val TAG = "RelayMessagingService"
    }
}
