# DataNudge ChatGPT Handover

**Project:** DataNudge  
**Repository:** `https://github.com/KaiEysselein/DataNudge`  
**Local project path:** `D:\Github\DataNudge`  
**Application ID:** `com.kaieysselein.datanudge`  
**Current released version:** `0.2.1.0`  
**Current GitHub release tag:** `v0.2.1.0`  
**Licence:** GNU General Public License v3.0  
**Last updated:** 1 August 2026

## 1. Purpose

DataNudge is an Android app that helps users remain aware of their current network connection and avoid accidental high mobile-data use.

The user selects apps such as YouTube, Netflix, Spotify, TikTok or Instagram. When a selected app opens while mobile data is active, DataNudge displays a reminder.

## 2. Current architecture

### MainActivity

Path:

```text
app\src\main\java\com\kaieysselein\datanudge\MainActivity.kt
```

Responsibilities include:

- main Compose user interface;
- home screen;
- settings;
- monitored-app selection;
- permissions;
- About page;
- first-run setup wizard;
- mandatory disclaimer acceptance;
- permanent disclaimer viewer;
- displayed app version.

### NetworkMonitorService

Path:

```text
app\src\main\java\com\kaieysselein\datanudge\NetworkMonitorService.kt
```

Responsibilities include:

- foreground monitoring service;
- active connection detection;
- foreground-app usage-event checking;
- mobile-data reminder overlay;
- ongoing status notification;
- connection-specific status icons;
- selected-app monitoring state.

### BootReceiver

Path:

```text
app\src\main\java\com\kaieysselein\datanudge\BootReceiver.kt
```

Responsibilities include:

- restarting monitoring after device reboot;
- restarting monitoring after an app update when monitoring was previously enabled.

## 3. Current functionality

- Wi-Fi, mobile-data, VPN, Ethernet, Bluetooth-network and offline detection
- Foreground monitoring service
- Ongoing status notification
- Connection-specific shield icons
- User-selected monitored apps
- Curated list of common high-data apps
- Overlay warning when a selected app opens on mobile data
- Sound and vibration on connection changes
- Start monitoring after reboot
- Guided setup wizard
- Permissions, Settings and About screens
- Mandatory disclaimer acceptance
- Online disclaimer and privacy pages
- Local storage of preferences and selected package names

## 4. Current release

### Version and code

```text
Version name: 0.2.1.0
Version code: 10001
```

### GitHub release

```text
https://github.com/KaiEysselein/DataNudge/releases/tag/v0.2.1.0
```

### Direct APK

```text
https://github.com/KaiEysselein/DataNudge/releases/download/v0.2.1.0/DataNudge-0.2.1.0.apk
```

### APK SHA-256

```text
C98A0F329915BD4AAEAC39684A45636AC9277BEBF8062B064C54408927FE7E4A
```

### Signing status

The current downloadable APK is signed with the Android debug key. It is suitable for direct testing and GitHub distribution, but it is not the final Google Play production build.

A permanent upload keystore has not yet been created.

## 5. GitHub Pages

Website:

```text
https://kaieysselein.github.io/DataNudge/
```

Privacy page:

```text
https://kaieysselein.github.io/DataNudge/privacy.html
```

Disclaimer page:

```text
https://kaieysselein.github.io/DataNudge/disclaimer.html
```

GitHub Pages is configured from:

```text
Branch: main
Folder: /docs
```

## 6. Licence

The repository uses the GNU General Public License v3.0. The canonical licence text is stored in `LICENSE`.

## 7. Versioning convention

```text
major.play-store-publication.feature.bug-fix
```

Examples:

```text
0.2.1.0  current release
0.2.1.0  next bug-fix release
0.2.1.0  next feature release
```

## 8. Planned work

Read `BUGS_AND_FEATURES.md`.

The next planned bug-fix release is `0.2.1.0`.

The next planned feature release is `0.2.1.0`, which will add current connection duration.

## 9. Build environment

### Project path

```text
D:\Github\DataNudge
```

### Android Studio Java

```text
D:\Program Files\Android\Android Studio\jbr
```

For PowerShell builds:

```powershell
$env:JAVA_HOME = "D:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### Build debug APK

```powershell
cd "D:\Github\DataNudge"
.\gradlew.bat assembleDebug
```

Output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 10. Git workflow

Repository:

```text
https://github.com/KaiEysselein/DataNudge
```

Branch:

```text
main
```

Before committing:

```powershell
git status --short
git diff
```

Typical commit and push:

```powershell
git add <explicit-files>
git commit -m "Describe the change"
git push origin main
```

Use explicit paths where practical. Do not stage backup files.

## 11. Backup conventions

Local backup folders and files are excluded through `.gitignore`.

Examples:

```text
backup-before-*/
*.backup
*.backup-before-*
```

Scripts create timestamped backups before editing source files.

## 12. Encoding precautions

Earlier PowerShell edits caused text-encoding problems.

Rules:

- prefer ASCII-only PowerShell scripts where possible;
- represent non-ASCII Kotlin text using Unicode escapes;
- for `für Lena`, use `"f\u00fcr Lena"`;
- avoid inserting literal PowerShell backtick markers into Kotlin source;
- always build after scripted text replacement;
- inspect `git diff` before committing.

## 13. Current known warning

The debug build reports a deprecation warning in `NetworkMonitorService.kt` relating to `MOVE_TO_FOREGROUND`.

The warning does not stop the build, but it should be reviewed later.

## 14. Disclaimer behaviour

The current app contains:

- mandatory first-launch disclaimer acceptance;
- a checkbox that must be selected before continuing;
- `Accept and continue`;
- `Decline and exit`;
- a permanent About-page disclaimer link;
- an online disclaimer page.

The acceptance version is stored locally. If the disclaimer changes materially, increase the disclaimer version so users must accept again.

Known issue: the decline action can be inaccessible on smaller screens. Keep the action area fixed while only the disclaimer body scrolls.

## 15. Important next-session instructions

1. Read `HANDOVER.md`.
2. Read `BUGS_AND_FEATURES.md`.
3. Confirm the current Git branch and status.
4. Create a timestamped backup.
5. Make one controlled group of changes.
6. Build successfully.
7. Inspect the app manually.
8. Commit and push only after verification.
9. Publish a new GitHub release only after increasing the version.







## Legal-information access

DataNudge exposes permanent Privacy statement and Disclaimer notice
actions from the About page. The disclaimer can be viewed in-app, while
the published privacy page opens in the browser. Mandatory disclaimer
acceptance on first use remains separate and unchanged.



## Resume refresh behaviour

The Home screen observes the Activity lifecycle. On ON_RESUME, it
immediately re-reads the active connection and the persisted connection
session, recalculates approximate traffic usage, and refreshes monitoring
state. The one-second UI refresh loop runs only while the Activity is
resumed. Resuming must not reset the session baseline.



## In-app privacy notice

The About page opens the Privacy statement inside DataNudge using a
scrollable Android dialog. The dialog includes a Read online action for
the published privacy page. The Disclaimer notice uses the same general
interaction pattern. Settings no longer duplicates the dedicated About
menu item.

## Resume refresh behaviour

The Home screen refreshes its connection session immediately on
ON_RESUME and once per second while the Activity is visible. The UI
refresh loop pauses while the app is backgrounded and does not reset the
persisted connection-session baseline.



## Automatic update architecture

UpdateCheckWorker.kt contains the daily WorkManager worker and the
persistent update-state manager. Work is scheduled as unique periodic
work with a connected-network constraint. The monitoring service reads
the stored update state and adds an update line to its existing ongoing
notification.

The Updates page always performs a fresh check. For GitHub/testing
builds it can download the APK into the app cache, verify the GitHub
asset SHA-256 digest when supplied, expose the file through FileProvider,
and open Android's installer. Android user confirmation remains
mandatory. A future Google Play build should disable this GitHub
installation path and use Play-managed updates.



## Installed apps page

Version 0.2.1.0 adds a read-only Installed apps page under Settings.
It uses Android LauncherApps.getActivityList() for the current user,
groups multiple launcher activities by package, and displays one
alphabetically sorted row per launchable package. Each row contains the
badged launcher icon, label and package name. This is deliberately not a
complete list of hidden system packages and it does not yet change the
existing monitoring selection model.



## Integrated app selector

Version 0.2.1.0 removes the temporary read-only Installed apps page.
Apps to monitor now loads launchable installed apps through
LauncherApps.getActivityList() and displays app icons, labels, package
names and checkboxes.

The former curated package set is retained only as a one-time default.
NetworkMonitorService.hasSelectedPackagesPreference() distinguishes an
uninitialized installation from an intentionally empty user selection.
If the preference has never existed, only installed matches from the
former defaults are selected. Existing selections are preserved.


## Home refresh and permission review

Version 0.2.2.0 removes Run setup again from the top-right menu while
retaining it in Settings.

The Home screen recognizes an upward swipe of at least 120 pixels and
immediately refreshes the current connection, elapsed duration,
approximate usage and monitoring state. The existing service-maintained
connection session and traffic baseline are not reset.

The Permissions screen always displays all three permission cards and
their Android settings actions, including when every permission is
already granted.

