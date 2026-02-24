# PiConnect

Android app for connecting to your Raspberry Pi via [Raspberry Pi Connect](https://connect.raspberrypi.com) with an SSH terminal.

## Features

- **Login** – Authenticate with your Raspberry Pi Connect account
- **Device List** – Browse all your registered Raspberry Pi devices and their online/offline status
- **SSH Terminal** – Open an SSH session to any online device through an encrypted WebSocket tunnel

## Architecture

```
app/src/main/java/com/piconnect/app/
├── data/
│   ├── api/           # Retrofit API service & models
│   ├── repository/    # PiConnectRepository (data layer)
│   └── ssh/           # SshClient (JSch over WebSocket)
├── ui/
│   ├── navigation/    # Compose NavGraph
│   ├── screens/       # LoginScreen, DeviceListScreen, SshTerminalScreen
│   └── theme/         # Material 3 theme (Color, Type, Theme)
└── viewmodel/         # LoginViewModel, DeviceListViewModel, SshTerminalViewModel
```

**Pattern**: MVVM · Jetpack Compose · Kotlin Coroutines · StateFlow

## Connection Flow

```
Android App
    │
    ├─ HTTPS ──► connect.raspberrypi.com/api/v1/auth/login      (get JWT token)
    ├─ HTTPS ──► connect.raspberrypi.com/api/v1/devices          (list devices)
    ├─ HTTPS ──► connect.raspberrypi.com/api/v1/devices/{id}/tunnel  (get WebSocket URL)
    │
    └─ WSS ───► <tunnel WebSocket URL>
                    │
                    └─ SSH (JSch) ──► Raspberry Pi sshd
```

JSch is configured with a custom `SocketFactory` that routes all TCP traffic through
the WebSocket pipes, so the SSH handshake and subsequent I/O travel entirely inside the
Raspberry Pi Connect encrypted tunnel.

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API level 26+
- A [Raspberry Pi Connect](https://connect.raspberrypi.com) account with at least one registered device

### Build & Run

```bash
git clone https://github.com/rance1230/Piconnect.git
cd Piconnect
./gradlew assembleDebug          # build APK
./gradlew installDebug           # install on connected device / emulator
```

### Usage

1. Open the app and sign in with your Raspberry Pi Connect credentials
2. The device list shows all your Raspberry Pis – tap an **online** device
3. Enter your SSH username (default: `pi`) and password, then tap **Connect via SSH**
4. A terminal session opens – type commands and press **Send** (or the keyboard Enter/Done key)

## Security Notes

- All API traffic uses HTTPS; the tunnel uses WSS (TLS)
- `android:usesCleartextTraffic="false"` is set in the manifest
- HTTP logging is disabled in release builds (`BuildConfig.DEBUG` guard)
- SSH credentials are held in memory only and are never persisted to disk
- The JWT auth token is stored in unencrypted DataStore preferences (suitable for development; consider [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) for production hardening)
- `StrictHostKeyChecking=no` is used for the SSH session because the server identity is already validated by the Raspberry Pi Connect service tunnel

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.02.00 | UI toolkit |
| Kotlin Coroutines | 1.7.3 | Async / threading |
| Retrofit 2 | 2.9.0 | REST API client |
| OkHttp 3 | 4.12.0 | HTTP + WebSocket |
| JSch | 0.1.55 | SSH protocol |
| AndroidX DataStore | 1.0.0 | Token persistence |

## License

[MIT License](LICENSE)
