# DataNudge

## Mobile-data awareness without the clutter

**DataNudge** is an Android connection-awareness app built around a foreground monitoring service and a lightweight Jetpack Compose interface.

It monitors the phone's current connection type and can remind the user when a selected high-data app is opened while mobile data is active.

**Current version:** `0.0.4.1-alpha`

## What DataNudge does

DataNudge separates continuous background monitoring from the normal user interface:

- **NetworkMonitorService** runs in the background, monitors the active connection, checks foreground-app usage events and owns the status notification.
- **MainActivity** provides the home screen, setup wizard, monitored-app selection, permissions, settings and about interfaces.
- **BootReceiver** restarts monitoring after a phone restart or app update when monitoring was previously enabled.

DataNudge is designed for local use. Selected apps and monitoring preferences remain on the phone.

## Main capabilities

- Wi-Fi, mobile-data, VPN, Ethernet, Bluetooth-network and offline detection
- User-selected app monitoring
- Mobile-data reminders for selected foreground apps
- Guided first-run setup wizard
- Notification-permission setup
- Android Usage Access setup
- Display-over-other-apps setup
- Pause and resume control
- Persistent foreground-service notification
- Connection-specific status-bar icons
- Automatic restart after phone reboot or app update
- Local storage of selected packages and monitoring preferences
- Curated list of common high-data apps
- Responsive dark user interface based on the DataNudge logo colours

## Connection status icons

| State | Meaning |
|---|---|
| **Wi-Fi** | The active connection uses Wi-Fi |
| **Mobile data** | The active connection uses the cellular network |
| **VPN** | The active connection uses a VPN |
| **Offline** | No active network connection is available |
| **Other** | Ethernet, Bluetooth networking or another connection type is active |

The status-vector resources are stored under:

```text
app\src\main\res\drawable
```

The launcher icons are stored under the Android mipmap density folders.

## Android permissions

| Permission or access | Purpose |
|---|---|
| **Notifications** | Shows the ongoing connection-status notification |
| **Usage Access** | Detects when a selected app enters the foreground |
| **Display over other apps** | Shows the mobile-data reminder above the selected app |
| **Network state** | Detects the active connection type |
| **Foreground service** | Keeps user-enabled monitoring active |
| **Boot completed** | Restarts monitoring after a phone restart when previously enabled |

Usage Access does not give DataNudge access to passwords, messages, typed text or the contents displayed inside monitored apps.

## Local data

| Purpose | Storage |
|---|---|
| Monitoring enabled state | Android app preferences |
| Selected app package names | Android app preferences |
| Setup completion state | Android app preferences |
| Activity history | Not stored by DataNudge |
| Cloud synchronisation | Not currently used |

Clearing DataNudge's app data or uninstalling the app removes its locally stored settings.

## Current development boundary

Version `0.0.4.1-alpha` includes the cleaned home screen, guided setup wizard, monitored-app selection, background connection monitoring, mobile-data overlays, reboot support and corrected UI text encoding.

The project is still in alpha development and has not yet been published on Google Play.

## Build and install

Build the debug APK from PowerShell:

```powershell
cd "D:\Github\DataNudge"
.\gradlew.bat assembleDebug
```

The APK is created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install or update it on a connected Android phone:

```powershell
$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

& $Adb install -r `
    "D:\Github\DataNudge\app\build\outputs\apk\debug\app-debug.apk"
```

Launch DataNudge:

```powershell
& $Adb shell monkey `
    -p com.kaieysselein.datanudge `
    -c android.intent.category.LAUNCHER 1
```

## Project links

- [Project website](https://kaieysselein.github.io/DataNudge/)
- [Privacy information](https://kaieysselein.github.io/DataNudge/privacy.html)
- [Repository](https://github.com/KaiEysselein/DataNudge)
- [Issues](https://github.com/KaiEysselein/DataNudge/issues)
- [License](LICENSE)

DataNudge is licensed under the **GNU General Public License v3.0**.
