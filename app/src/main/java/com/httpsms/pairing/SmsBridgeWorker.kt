package com.httpsms.pairing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private fun timestamp(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date())

class SmsBridgeWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val messageId = inputData.getString(KEY_MESSAGE_ID) ?: return@withContext Result.failure()
        val storage = SecureStorageManager(applicationContext)
        if (!storage.isPaired) return@withContext Result.failure()
        val api = SmsBridgeApi(RelayApiClient(storage))
        val message = try {
            api.getOutstandingMessage(messageId)
        } catch (error: IOException) {
            return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        if (message == null) return@withContext Result.failure()

        try {
            AndroidSmsDispatcher(applicationContext).send(message)
            Result.success()
        } catch (error: Exception) {
            runCatching { api.sendFailedEvent(message.id, timestamp(), error.message ?: error.javaClass.simpleName) }
            Result.failure()
        }
    }

    companion object { const val KEY_MESSAGE_ID = "message_id" }
}
