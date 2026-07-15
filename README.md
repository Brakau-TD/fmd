# Find My Device - Android Tracker App

A resilient Android tracking application designed for finding lost or stolen devices. The app pairs with a secure web portal (via WebSocket) and runs an encrypted background service that handles location updates and remote commands.

## Key Features
- **Background Location Sync**: Uses a foreground service and wake locks to continue transmitting location even when the device sleeps.
- **Location Reporting Frequency Sliders**: Allows dynamic real-time adjustment of GPS reporting frequency for both active tracking (moving) and power-saving mode (stationary).
- **Log Pruning & Cache Preservation**: Automatically limits the local Room Database audit trail to the latest 1,000 entries to prevent infinite disk storage growth in the background.
- **Robust Log Exporting**: Exports complete log details with custom device metadata via a secure Android FileProvider and incorporates an automatic clipboard-copy fallback for maximum cross-app compatibility.
- **Strict HMAC Signature Verification**: Features highly flexible validation of incoming commands, supporting Base64, Base64 URL-Safe, Hexadecimal signatures, and multiple JSON-whitespace layouts.
- **Simulated Triggers**: Included Dashboard Simulator for testing alarm sounds, message screens, and flashlight toggling without needing the web portal.

## Background Stability & Android 14 AppOps

If you observe `AppOps : Operation not started: ... op=MONITOR_LOCATION` or `op=WAKE_LOCK` in Logcat:
1. **Battery Optimization**: Modern Android (Doze Mode) may throttle GPS scans and network requests when the screen is off. For 100% reliable tracking in the background, navigate to **Settings > Apps > Find My Device > Battery** and set to **"Unrestricted"**.
2. **Background Location Permission**: The app requests "While in use" location by default to avoid complex permission rejection. However, the foreground service handles location. If your phone restricts FGS location access on screen-off, you must grant "Allow all the time" location access in Android Settings manually.

## Development & Testing
- **Run the Application**: Verify all configurations by selecting `Run` in AI Studio.
- **Run Unit Tests**: We have included Robolectric tests simulating command payloads, state flow, and signature verifications.
  `gradle :app:testDebugUnitTest`

## Author
Developed using Google AI Studio.
