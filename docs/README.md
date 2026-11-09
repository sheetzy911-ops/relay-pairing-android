# Relay Desk GitHub Pages console

This folder is a static frontend for the paired Android relay app. It can be published through GitHub Pages from the `main` branch using the `/docs` folder.

## Deploy

In GitHub repository settings, open **Pages**, choose **Deploy from a branch**, select `main`, select `/docs`, and save. The expected URL is:

```text
https://sheetzy911-ops.github.io/relay-pairing-android/
```

## Runtime contract

The browser sends SMS queue requests to `POST /bridge/messages` using a browser session or short-lived browser token. The Android app never receives the browser token. It independently authenticates with its encrypted `X-Device-Key` and uses:

- `GET /v1/messages/outstanding?message_id=...`
- `POST /v1/messages/{id}/events`
- `POST /v1/heartbeats`

Do not place an Android device key, relay administrator password, or long-lived provider credential in this static site. GitHub Pages is public and cannot host the relay backend, database, FCM credentials, or Twilio credentials.
