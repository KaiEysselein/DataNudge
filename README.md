# DataNudge

<p align="center">
  <a href="https://github.com/KaiEysselein/DataNudge/releases/download/v0.1.4.1/DataNudge-0.1.4.1.apk"><strong>Download DataNudge 0.1.4.1 APK</strong></a>
  &nbsp;|&nbsp;
  <a href="https://github.com/KaiEysselein/DataNudge/releases/tag/v0.1.4.1">View release details</a>
</p>

> **Android installation:** Download the APK, open it on your phone, and choose **Update** or **Install**. Android may ask you to allow installation from your browser or file manager.
## Mobile-data awareness without the clutter

**DataNudge** is an Android app that helps users avoid unexpected mobile-data usage.

Choose apps such as YouTube, Netflix, Spotify, TikTok, or Instagram. When one of the selected apps opens while the phone is using mobile data, DataNudge displays a clear reminder.

**Current version:** `0.1.4.1`

## What DataNudge does

- Detects whether the phone is using Wi-Fi, mobile data, a VPN, Ethernet, another connection type, or no connection
- Lets the user choose which apps should be monitored
- Displays a reminder when a selected app opens while mobile data is active
- Provides a guided first-run setup
- Continues monitoring in the background when enabled
- Restarts monitoring after a phone restart when it was previously enabled
- Stores settings and selected apps locally on the device

## Main features

### Connection awareness

DataNudge continuously shows the phone's current connection status through its notification and main screen.

### Selected-app monitoring

The user chooses which high-data apps should trigger a reminder. Nothing is selected automatically.

### Mobile-data reminders

When a selected app enters the foreground while mobile data is active, DataNudge displays a clear reminder above the app.

### Guided setup

The setup wizard guides the user through the required Android permissions one step at a time. The user cannot continue until the current step is completed.

### Background monitoring

Monitoring can be paused or resumed at any time. When enabled, DataNudge continues working after the main app screen is closed.

## Android permissions

| Permission or access | Why DataNudge needs it |
|---|---|
| **Notifications** | Shows the ongoing connection-status notification |
| **Usage Access** | Detects when a selected app enters the foreground |
| **Display over other apps** | Displays the mobile-data reminder above the selected app |
| **Network state** | Detects the active connection type |
| **Foreground service** | Keeps user-enabled monitoring active |
| **Boot completed** | Restarts monitoring after a phone restart when previously enabled |

Usage Access does not allow DataNudge to read passwords, messages, typed text, or the contents displayed inside monitored apps.

## Privacy

DataNudge stores the following information locally on the device:

- Whether monitoring is enabled
- Which apps the user selected
- Whether the initial setup was completed

The current alpha version does not intentionally send the selected-app list or foreground-app usage events to the developer.

Clearing DataNudge's app data or uninstalling the app removes its locally stored settings.

## Current status

DataNudge is currently in alpha development and has not yet been published on Google Play.

The current version includes:

- Clean home screen
- Guided setup wizard
- App selection
- Connection monitoring
- Mobile-data reminders
- Background monitoring
- Start after reboot
- Corrected UI text encoding

## Project links

- [Project website](https://kaieysselein.github.io/DataNudge/)
- [Privacy information](https://kaieysselein.github.io/DataNudge/privacy.html)
- [Disclaimer and Terms of Use](https://kaieysselein.github.io/DataNudge/disclaimer.html)
- [Repository](https://github.com/KaiEysselein/DataNudge)
- [Issues](https://github.com/KaiEysselein/DataNudge/issues)
- [Licence](LICENSE)

## Licence

DataNudge is licensed under the **GNU General Public License v3.0**.

## Development status

The current development backlog and project handover notes are maintained in:

- [Bugs and planned features](BUGS_AND_FEATURES.md)
- [ChatGPT project handover](HANDOVER.md)

The next planned maintenance release is `0.1.4.1`.  
The next planned feature release is `0.1.4.1`.








