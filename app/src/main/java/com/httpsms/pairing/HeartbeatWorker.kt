package com.httpsms.pairing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class HeartbeatWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val storage = SecureStorageManager(applicationContext)
        if (!storage.isPaired) return@withContext Result.success()
        return@withContext try {
            val ok = SmsBridgeApi(RelayApiClient(storage)).storeHeartbeat(
                BridgeDeviceInfo.phoneNumbers(applicationContext),
                BridgeDeviceInfo.isCharging(applicationContext)
            )
            if (ok) Result.success() else Result.retry()
        } catch (_: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
