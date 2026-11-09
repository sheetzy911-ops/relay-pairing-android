# Relay Pairing Android

Standalone Android Studio project for pairing an Android bridge with the secure SMS relay and processing outgoing SMS jobs.

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
5. Future bridge calls use `X-Device-Key`; the old shared API key is not used.

## Migrated bridge behavior

The new Kotlin bridge preserves the original APK’s HTTP contract:

| Original behavior | New implementation |
|---|---|
| Fetch queued message | `GET /v1/messages/outstanding?message_id=...` |
| Report `SENT` | `POST /v1/messages/{id}/events` with `event_name: SENT` |
| Report `DELIVERED` | Same event endpoint with `event_name: DELIVERED` |
| Report `FAILED` | Same event endpoint with `event_name: FAILED` and `reason` |
| Heartbeat | `POST /v1/heartbeats` with `phone_numbers` and `charging` |
| FCM message trigger | `RelayMessagingService` enqueues `SmsBridgeWorker` |
| Reliable processing | WorkManager with connected-network constraint |
| SIM routing | `SIM1` uses the first active subscription; `SIM2` uses the second |
| Multipart SMS | Uses Android `SmsManager.divideMessage` and multipart send |

`SmsStatusReceiver` converts Android sent/delivery callbacks into relay events. HTTP 401 clears the encrypted pairing, and an event HTTP 404 is treated as terminal success to match the original client.

## Firebase setup

The project includes the Firebase Messaging service dependency and manifest declaration, but Firebase credentials are intentionally not committed. Add the Firebase Android configuration for the application ID `com.httpsms.pairing` before expecting FCM delivery. The relay should send data messages containing either:

```json
{"message_id":"outgoing-message-id"}
```

or:

```json
{"heartbeat_id":"heartbeat-id"}
```

## Runtime permissions

Request `SEND_SMS`, `READ_PHONE_STATE`, and `READ_PHONE_NUMBERS` at runtime before using the bridge. The pairing screen itself does not request these permissions yet; the production integration should request them from the app’s main bridge screen and explain why they are required.

## Deliberate migration boundaries

Plain SMS, multipart SMS, fetch, event callbacks, heartbeat, pairing, and SIM1/SIM2 routing are migrated. MMS attachment composition/download and the original encrypted-message format are not yet migrated; those paths fail safely and report a `FAILED` event instead of sending an unsafe or incomplete message.

Never log pairing codes, device keys, relay URLs containing credentials, or SMS content.
