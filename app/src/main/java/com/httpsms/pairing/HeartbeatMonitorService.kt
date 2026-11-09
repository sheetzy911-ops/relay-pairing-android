package com.httpsms.pairing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class HeartbeatMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatTask = object : Runnable {
        override fun run() {
            if (SecureStorageManager(applicationContext).isPaired) {
                SmsBridgeScheduler.sendHeartbeat(
                    applicationContext,
                    BridgeDeviceInfo.phoneNumbers(applicationContext),
                    BridgeDeviceInfo.isCharging(applicationContext)
                )
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            } else {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        handler.post(heartbeatTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
        .setContentTitle("Relay bridge is monitoring")
        .setContentText("Heartbeat monitoring is active for the paired Android device")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Relay heartbeat", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "relay-heartbeat"
        private const val NOTIFICATION_ID = 4101
        const val HEARTBEAT_INTERVAL_MS = 15 * 60 * 1000L

        fun start(context: android.content.Context) {
            val intent = Intent(context, HeartbeatMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, HeartbeatMonitorService::class.java))
        }
    }
}
