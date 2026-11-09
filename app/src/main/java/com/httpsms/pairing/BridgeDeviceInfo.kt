package com.httpsms.pairing

import android.content.Context
import android.os.BatteryManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object BridgeDeviceInfo {
    fun phoneNumbers(context: Context): List<String> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return context.getSystemService(SubscriptionManager::class.java)
            ?.activeSubscriptionInfoList.orEmpty()
            .mapNotNull { it.number?.takeIf(String::isNotBlank) }
    }

    fun isCharging(context: Context): Boolean {
        val manager = context.getSystemService(BatteryManager::class.java) ?: return false
        return manager.isCharging
    }
}
