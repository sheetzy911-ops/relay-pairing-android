package com.httpsms.pairing

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the relay device identity and key using Android Keystore-backed
 * encryption. The plaintext device key is never written to ordinary prefs,
 * files, logs, URLs, or Intent extras.
 */
class SecureStorageManager(context: Context) {
    private val appContext = context.applicationContext

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val preferences by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val isPaired: Boolean
        get() = !deviceKey.isNullOrBlank() && !deviceId.isNullOrBlank()

    val relayBaseUrl: String?
        get() = preferences.getString(KEY_RELAY_URL, null)

    val deviceId: String?
        get() = preferences.getString(KEY_DEVICE_ID, null)

    private val deviceKey: String?
        get() = preferences.getString(KEY_DEVICE_KEY, null)

    fun savePairing(relayUrl: String, deviceId: String, deviceKey: String) {
        require(relayUrl.startsWith("https://")) { "The relay must use HTTPS." }
        require(deviceId.isNotBlank()) { "Device ID cannot be empty." }
        require(deviceKey.isNotBlank()) { "Device key cannot be empty." }

        preferences.edit()
            .putString(KEY_RELAY_URL, relayUrl.trimEnd('/'))
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_KEY, deviceKey)
            .apply()
    }

    fun authenticatedDeviceKey(): String {
        return deviceKey ?: throw NotPairedException()
    }

    fun authenticatedRelayUrl(): String {
        return relayBaseUrl ?: throw NotPairedException()
    }

    /** Clears credentials after a server-side revocation or explicit sign-out. */
    fun clearPairing() {
        preferences.edit()
            .remove(KEY_RELAY_URL)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_DEVICE_KEY)
            .apply()
    }

    class NotPairedException : IllegalStateException("This Android bridge is not paired.")

    private companion object {
        const val PREFS_FILE = "relay_secure_prefs"
        const val KEY_RELAY_URL = "relay_url"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_KEY = "device_key"
    }
}
