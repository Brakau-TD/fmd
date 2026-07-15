# Server Backend Integration Guide

This document describes how to build a server backend that is compatible with the
Find My Device Android app. It covers the transport, the pairing model, the message
envelope, the HMAC authentication scheme, and the full message/command catalog the
app sends and expects.

> The app talks to the backend over a **single WebSocket connection**. There are no
> REST endpoints. Every message (in both directions) is a JSON object wrapped in a
> signed envelope.

---

## 1. Transport

- **Protocol:** WebSocket (`ws://`) or WebSocket Secure (`wss://`).
- **Path:** `/ws/tracker`
- **Default port:** `8080` (plaintext dev) / `443` (TLS).
- **Keep-alive:** the client sends a WebSocket **ping every 15 seconds**. Your server
  must respond to standard WebSocket ping/pong frames (most libraries do this
  automatically).

### URL construction (client side)

If the pairing config provides an explicit `websocketUrl`, the client uses it verbatim.
Otherwise it builds the URL from `serverHost` + `serverPort`:

```
<scheme>://<host>:<port>/ws/tracker
```

The scheme is chosen as follows:

| Condition                                   | Scheme  |
| ------------------------------------------- | ------- |
| `serverPort == 443`                         | `wss://`|
| Public hostname (not localhost/LAN/`.local`)| `wss://`|
| Private host (localhost, `10.*`, `192.168.*`, `172.16–31.*`, `*.local`) in **debug** build | `ws://` |
| Private host in **release** build           | `wss://`|

**Implication:** For any internet-facing deployment you **must** serve `wss://` (TLS).
Plaintext `ws://` is only used for local development against a LAN/localhost host in a
debug build.

### Reconnection behavior

The client reconnects automatically with exponential backoff starting at **1s**,
doubling up to a **30s** cap. On successful connect it immediately sends one location
update. Your server should tolerate reconnects and treat each new socket as the same
logical device (keyed by `clientId`).

---

## 2. Pairing model

Each device is identified by a `clientId` and authenticated with a shared secret
(`secretToken`). The client obtains these during pairing. Your backend is responsible
for provisioning them and displaying/encoding them for the device.

The app supports three pairing input methods:

### a) QR code — JSON payload (recommended)

Encode this JSON into a QR code shown by your web portal:

```json
{
  "clientId": "device_abc",
  "token": "shared_secret_key_123",
  "serverHost": "tracker.example.com",
  "serverPort": 443,
  "websocketUrl": "wss://tracker.example.com:443/ws/tracker"
}
```

Field notes:
- `clientId` — unique device identifier you assign.
- `token` — the shared HMAC secret. `secret` is also accepted as an alias.
- `serverHost` / `serverPort` — optional; override the manually entered host/port.
- `websocketUrl` — optional; if present it overrides host/port entirely. `wsUrl` is
  accepted as an alias.

### b) QR code / deep link — URL form

```
tracker://pair?clientId=device_abc&token=shared_secret_key_123&websocketUrl=wss://tracker.example.com:443/ws/tracker
```

### c) Raw token paste

If the user pastes a bare string (not JSON, not a `tracker://` URL), it is treated as
the raw `secretToken` and a random `clientId` (`DEVICE-XXXX`) is generated. In this
case the host/port must already be configured in the app.

> **Provisioning requirement:** Your backend must store, per device, the `clientId`
> and its `secretToken`, and must be able to look up the secret by `clientId` to verify
> and sign messages.

---

## 3. The signed envelope

**Every** message in both directions is this JSON envelope:

```json
{
  "clientId": "device_abc",
  "timestamp": 1734105600000,
  "signature": "base64url_no_padding_hmac",
  "payload": "{\"type\":\"location\",\"latitude\":37.4220, ... }"
}
```

- `clientId` — string, the device identifier.
- `timestamp` — integer, **milliseconds** since Unix epoch.
- `payload` — a **string** containing the exact serialized inner JSON (not a nested
  object). The signature is computed over this exact string, so you must sign/verify
  the byte-for-byte string you place in this field.
- `signature` — see below.

### Signature algorithm

```
data      = clientId + "|" + timestamp + "|" + payload
signature = Base64URL_NoPadding( HMAC_SHA256(key = secretToken, message = data) )
```

Details that must match exactly:
- **Algorithm:** HMAC-SHA256.
- **Key:** the `secretToken` bytes (UTF-8).
- **Message:** the UTF-8 bytes of `clientId|timestamp|payload` (pipe-separated, no
  spaces). `timestamp` is the decimal string of the same integer used in the envelope.
- **Encoding:** Base64 **URL-safe**, **no padding** (`-`/`_` alphabet, no trailing `=`).

### Verification rules (server should mirror these)

The client enforces, and your server should enforce, on every inbound message:

1. `clientId` matches a known device.
2. `|now - timestamp| <= 5 minutes` (replay protection window).
3. `signature` recomputed from the secret matches exactly (constant-time compare
   recommended).

Reject (drop) the message if any check fails.

---

## 4. Messages the app SENDS to the server

The inner `payload` object always has a `type` field.

### 4.1 Location update (`type: "location"`)

Sent on the active/stationary cadence and once immediately after (re)connecting, and
in response to a `get_current_location` command.

```json
{
  "type": "location",
  "latitude": 37.4220,
  "longitude": -122.0841,
  "accuracy": 12.4,
  "speed": 1.5,
  "altitude": 45.2,
  "battery": 85,
  "status": "active"
}
```

| Field       | Type   | Notes                                   |
| ----------- | ------ | --------------------------------------- |
| `latitude`  | number | degrees                                 |
| `longitude` | number | degrees                                 |
| `accuracy`  | number | meters                                  |
| `speed`     | number | meters/second                           |
| `altitude`  | number | meters                                  |
| `battery`   | number | 0–100 (%)                               |
| `status`    | string | `"active"` or `"stationary"`            |

### 4.2 Status change (`type: "status_change"`)

Sent when the device switches into power-saving (stationary) mode.

```json
{
  "type": "status_change",
  "status": "stationary",
  "batterySaving": true,
  "battery": 45
}
```

### Reporting cadence

| Mode                    | Interval (default) | Configurable field        |
| ----------------------- | ------------------ | ------------------------- |
| Active (moving)         | 10 seconds         | `locationIntervalSec`     |
| Stationary (idle > 5 m) | 300 seconds (5 m)  | `stationaryIntervalSec`   |

The device auto-switches to stationary after ~5 minutes without significant motion and
back to active on motion (accelerometer delta > ~1.2 m/s²).

---

## 5. Commands the server SENDS to the app

Wrap each command payload in the same signed envelope (signed with that device's
secret). The inner payload uses a `command` field.

### 5.1 `get_current_location`

```json
{ "command": "get_current_location" }
```
The device replies with an immediate `location` message.

### 5.2 `flash_flashlight_and_screen`

```json
{ "command": "flash_flashlight_and_screen" }
```
Flashes the flashlight/screen and brings the app to the foreground.

### 5.3 `display_message_on_screen`

```json
{ "command": "display_message_on_screen", "message": "Please call me!" }
```
Shows `message` (defaults to `"Emergency Alert!"` if omitted).

### 5.4 `trigger_emergency_alarm`

```json
{ "command": "trigger_emergency_alarm" }
```
Sounds an alarm and brings the app to the foreground.

> Commands are fire-and-forget from the server's perspective. There is no explicit
> ACK message; the effect of `get_current_location` is the subsequent `location`
> update. Other commands produce a device-side action only.

---

## 6. Minimal server example (Node.js)

This illustrates the envelope, HMAC verification, and sending a command. It is a
reference sketch, not production code (add TLS, persistence, auth for your portal,
rate limiting, and structured logging).

```js
// npm i ws
const crypto = require("crypto");
const { WebSocketServer } = require("ws");

// Map clientId -> secretToken (populate during pairing/provisioning).
const secrets = new Map([["device_abc", "shared_secret_key_123"]]);

function sign(clientId, timestamp, payload, secret) {
  const data = `${clientId}|${timestamp}|${payload}`;
  return crypto
    .createHmac("sha256", secret)
    .update(data, "utf8")
    .digest("base64url"); // base64url, no padding
}

function verify(envelope) {
  const { clientId, timestamp, signature, payload } = envelope;
  const secret = secrets.get(clientId);
  if (!secret) return false;
  if (Math.abs(Date.now() - Number(timestamp)) > 5 * 60 * 1000) return false;
  const expected = sign(clientId, timestamp, payload, secret);
  // constant-time compare
  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function buildEnvelope(clientId, innerObj) {
  const secret = secrets.get(clientId);
  const payload = JSON.stringify(innerObj); // exact string that gets signed
  const timestamp = Date.now();
  const signature = sign(clientId, timestamp, payload, secret);
  return JSON.stringify({ clientId, timestamp, signature, payload });
}

// Only handles the /ws/tracker path.
const wss = new WebSocketServer({ port: 8080, path: "/ws/tracker" });

wss.on("connection", (ws) => {
  ws.on("message", (raw) => {
    let envelope;
    try {
      envelope = JSON.parse(raw.toString());
    } catch {
      return; // ignore malformed
    }
    if (!verify(envelope)) return; // drop unauthenticated

    const inner = JSON.parse(envelope.payload);
    // Track this socket for the clientId so you can send commands later.
    ws.clientId = envelope.clientId;

    if (inner.type === "location") {
      console.log("location", envelope.clientId, inner.latitude, inner.longitude);
    } else if (inner.type === "status_change") {
      console.log("status", envelope.clientId, inner.status);
    }
  });
});

// Example: request an on-demand location from a connected device.
function requestLocation(ws) {
  ws.send(buildEnvelope(ws.clientId, { command: "get_current_location" }));
}
```

### Python note

Use `hmac.new(secret.encode(), data.encode(), hashlib.sha256).digest()` then
`base64.urlsafe_b64encode(...).rstrip(b"=")` to produce the same base64url-no-padding
signature. Sign/verify over the exact `clientId|timestamp|payload` string.

---

## 7. Security checklist

- Serve **`wss://` with a valid TLS certificate** for any non-local deployment.
- Store each device's `secretToken` securely; never log it.
- Enforce the **±5 minute timestamp window** and use **constant-time** signature
  comparison.
- Treat `clientId` from the envelope as untrusted until the signature verifies against
  that client's stored secret.
- Rotate secrets by re-pairing (issue a new QR with a new `token`).
- Protect your pairing/portal endpoints with their own authentication.

---

## 8. Quick reference

| Item                  | Value                                             |
| --------------------- | ------------------------------------------------- |
| Transport             | WebSocket, path `/ws/tracker`                     |
| Default port          | 8080 (dev) / 443 (TLS)                            |
| Ping interval         | 15 s (client → server)                            |
| Envelope fields       | `clientId`, `timestamp` (ms), `signature`, `payload` (string) |
| Signature             | Base64URL-no-padding HMAC-SHA256 of `clientId\|timestamp\|payload` |
| Replay window         | ±5 minutes                                         |
| Outbound msg types    | `location`, `status_change`                       |
| Inbound commands      | `get_current_location`, `flash_flashlight_and_screen`, `display_message_on_screen`, `trigger_emergency_alarm` |
| Active interval       | 10 s (default, `locationIntervalSec`)             |
| Stationary interval   | 300 s (default, `stationaryIntervalSec`)          |
