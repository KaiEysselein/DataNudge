# DataNudge

<p align="center">
  <a href="https://github.com/KaiEysselein/DataNudge/releases/download/v0.2.3.1/DataNudge-0.2.3.1.apk"><strong>Download stable DataNudge 0.2.3.1 APK</strong></a>
  &nbsp;|&nbsp;
  <a href="https://github.com/KaiEysselein/DataNudge/releases/tag/v0.2.3.1">View release details</a>
</p>

> **Android installation:** Download the APK, open it on your phone, and choose **Update** or **Install**. Android may ask you to allow installation from your browser or file manager.

## Mobile-data awareness without the clutter

**DataNudge** is an Android app that helps users avoid unexpected mobile-data usage.

Choose installed apps such as YouTube, Netflix, Spotify, TikTok, or Instagram. When a selected app opens while the phone is using mobile data, DataNudge displays a clear reminder.

**Current version:** `0.2.3.1`

## Release channels

### Stable

Stable releases are recommended for normal use. DataNudge's manual and automatic in-app update checks use GitHub's latest full-release endpoint and therefore follow this channel only.

- [Download the current stable APK](https://github.com/KaiEysselein/DataNudge/releases/download/v0.2.3.1/DataNudge-0.2.3.1.apk)
- [View the current stable release](https://github.com/KaiEysselein/DataNudge/releases/tag/v0.2.3.1)

### Nightly

Nightly builds are optional GitHub prereleases for testers. They may contain unfinished changes or regressions, require manual installation, and are never returned by DataNudge's stable update check. No nightly build is currently published.

- [Browse releases and prereleases](https://github.com/KaiEysselein/DataNudge/releases)
- Nightly builds must be explicitly marked as GitHub prereleases and include `nightly` in their release tag or title.

## What DataNudge does

- Detects whether the phone is using Wi-Fi, mobile data, a VPN, Ethernet, Bluetooth networking, another connection type, or no connection
- Lets the user choose installed apps to monitor
- Displays a reminder when a selected app opens while mobile data is active
- Provides a guided first-run setup
- Continues monitoring in the background when enabled
- Restarts monitoring after a phone restart when it was previously enabled
- Shows connection duration and approximate device-wide data use for the current connection session
- Supports manual and automatic GitHub release checks
- Stores settings and selected apps locally on the device

## Main features

### Connection awareness

DataNudge shows the phone's current connection status through its notification and main screen.

### Installed-app monitoring

Apps to monitor lists launchable apps installed on the device. On first initialization only, installed matches from DataNudge's former default list are selected. Existing selections—including an intentionally empty selection—are preserved.

### Mobile-data reminders

When a selected app enters the foreground while mobile data is active, DataNudge displays a clear reminder above the app.

### Guided setup and permissions

The setup wizard guides the user through the required Android permissions. Permission review remains available later from Settings.

### Background monitoring

Monitoring can be paused or resumed at any time. When enabled, DataNudge continues working after the main app screen is closed and can restart after a reboot.

### Updates

DataNudge can check GitHub for newer releases. Downloading and installation still require explicit user action and Android confirmation.

## Android permissions

| Permission or access | Why DataNudge needs it |
|---|---|
| **Notifications** | Shows the ongoing connection-status notification and update notices |
| **Usage Access** | Detects when a selected app enters the foreground |
| **Display over other apps** | Displays the mobile-data reminder above the selected app |
| **Network state** | Detects the active connection type |
| **Foreground service** | Keeps user-enabled monitoring active |
| **Boot completed** | Restarts monitoring after a phone restart when previously enabled |
| **Install unknown apps** | Opens Android's package installer when the user chooses to install a downloaded GitHub APK |
| **Internet access** | Checks GitHub releases and opens online project, privacy, and disclaimer pages |

Usage Access does not allow DataNudge to read passwords, messages, typed text, or the contents displayed inside monitored apps.

## Privacy

DataNudge stores the following information locally on the device:

- Whether monitoring is enabled
- Which apps the user selected
- Whether the initial setup was completed
- Disclaimer acceptance and update-check preferences
- Connection-session timing and traffic-counter baselines used for approximate usage reporting

The current alpha version does not intentionally send the selected-app list or foreground-app usage events to the developer. Network access is used for GitHub update checks and online project pages.

Clearing DataNudge's app data or uninstalling the app removes its locally stored settings.

## Current status

DataNudge is in alpha testing and has not yet been published on Google Play. The current GitHub APK is a debug-signed testing build.

Version `0.2.3.1` includes:

- Installed-app-based monitoring selection
- Wi-Fi, mobile-data, VPN, Ethernet, Bluetooth-network and offline detection
- Connection duration and approximate usage reporting
- Background monitoring and start after reboot
- Material 3 pull-to-refresh on Home
- Persistent permission review actions
- Manual and automatic update checks with user-confirmed APK installation
- WhatsApp sharing and DataNudge group access
- Published privacy and disclaimer pages

## Project links

- [Project website](https://kaieysselein.github.io/DataNudge/)
- [Privacy information](https://kaieysselein.github.io/DataNudge/privacy.html)
- [Disclaimer and Terms of Use](https://kaieysselein.github.io/DataNudge/disclaimer.html)
- [Current release](https://github.com/KaiEysselein/DataNudge/releases/tag/v0.2.3.1)
- [Repository](https://github.com/KaiEysselein/DataNudge)
- [Issues](https://github.com/KaiEysselein/DataNudge/issues)
- [Licence](LICENSE)

## Licence

DataNudge is licensed under the **GNU General Public License v3.0**.

## Development status

The current development backlog and project handover notes are maintained in:

- [Bugs and planned features](BUGS_AND_FEATURES.md)
- [ChatGPT project handover](HANDOVER.md)

The next planned bug-fix release is `0.2.3.2`.
The next planned feature release is `0.2.4.0`.
