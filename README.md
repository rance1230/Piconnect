# PiConnect

一个 Android 应用，通过 Raspberry Pi Connect 服务和 SSH 连接到树莓派。

An Android app for connecting to your Raspberry Pi via Raspberry Pi Connect service and SSH.

## Features / 功能

- **SSH Terminal / SSH 终端**: Native SSH terminal for direct command-line access to your Raspberry Pi
- **Raspberry Pi Connect / 树莓派连接**: Integrated WebView for Raspberry Pi Connect service (connect.raspberrypi.com)
- **Device Management / 设备管理**: Save and manage multiple Raspberry Pi device profiles
- **Multiple Auth Methods / 多种认证方式**: Support for password and private key authentication
- **Material 3 UI**: Modern Android UI with Jetpack Compose and Material 3 design

## Tech Stack / 技术栈

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **SSH Library**: JSch
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Navigation**: Jetpack Navigation Compose
- **Storage**: DataStore Preferences + Gson
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Project Structure / 项目结构

```
app/src/main/java/com/piconnect/app/
├── MainActivity.kt              # Main entry point
├── PiConnectApp.kt              # Application class
├── data/
│   ├── model/
│   │   ├── PiDevice.kt          # Device data model
│   │   └── ConnectionState.kt   # Connection state sealed class
│   ├── repository/
│   │   └── DeviceRepository.kt  # Device persistence (DataStore)
│   └── ssh/
│       └── SshManager.kt        # SSH connection management (JSch)
└── ui/
    ├── navigation/
    │   ├── Screen.kt             # Screen route definitions
    │   └── NavGraph.kt           # Navigation graph
    ├── screens/
    │   ├── home/                 # Home screen (device list)
    │   ├── device/               # Add/edit device screen
    │   ├── ssh/                  # SSH terminal screen
    │   └── connect/              # Pi Connect WebView screen
    └── theme/                    # Material 3 theme (raspberry-themed)
```

## Getting Started / 开始使用

### Prerequisites / 前提条件

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build / 构建

1. Clone the repository / 克隆仓库
2. Open the project in Android Studio / 用 Android Studio 打开项目
3. Sync Gradle and build / 同步 Gradle 并构建

```bash
./gradlew assembleDebug
```

### Run Tests / 运行测试

```bash
./gradlew test
```

## Usage / 使用方法

1. **Add a Device / 添加设备**: Tap the + button to add a new Raspberry Pi with its connection details (host, port, username, authentication method)
2. **Connect via SSH / SSH 连接**: Tap "Connect" on a saved device to open an interactive SSH terminal
3. **Pi Connect**: Enable "Use Raspberry Pi Connect" for a device to access the Pi Connect web interface
4. **Manage Devices / 管理设备**: View, edit, or delete saved device profiles from the home screen

## License

MIT License - See [LICENSE](LICENSE) for details.
