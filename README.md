# Relay Pairing Android

Standalone Android Studio project for pairing an Android bridge with the secure SMS relay.

## Build

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Pairing flow

1. The relay administrator calls `POST /auth/pairings`.
2. Enter the relay HTTPS URL, one-time pairing code, and generated device key.
3. The app calls `POST /v1/pairing/activate`.
4. The device key is stored using AndroidX Security encrypted preferences backed by Android Keystore.
5. Future bridge calls use `X-Device-Key`.

This is a pairing-focused bridge shell. Existing SMS fetch, send-event, inbound-message, heartbeat, SIM selection, and SMS permissions from the original APK must be added as separate features before using this as a replacement for `HttpSms.apk`.
