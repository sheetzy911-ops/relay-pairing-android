package com.httpsms.pairing

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

private fun timestamp(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date())

object SmsBridgeScheduler {
    fun enqueueOutstanding(context: Context, messageId: String) {
        val request = OneTimeWorkRequest.Builder(SmsBridgeWorker::class.java)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(androidx.work.workDataOf(SmsBridgeWorker.KEY_MESSAGE_ID to messageId))
            .addTag(messageId)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sms:$messageId", ExistingWorkPolicy.REPLACE, request)
    }

    fun sendHeartbeat(context: Context, phoneNumbers: List<String>, charging: Boolean) {
        val storage = SecureStorageManager(context)
        if (!storage.isPaired) return
        Thread {
            runCatching { SmsBridgeApi(RelayApiClient(storage)).storeHeartbeat(phoneNumbers, charging) }
        }.start()
    }

    fun nowTimestamp(): String = timestamp()
}
