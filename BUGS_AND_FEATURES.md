## Completed in 0.1.0.2

- Corrected the `für Lena` text colour so it matches the version number.

# DataNudge Bugs and Features

**Current released version:** `0.2.1.0`  
**Next planned bug-fix version:** `0.1.0.2`  
**Next planned feature version:** `0.1.1.0`  
**Last updated:** 1 August 2026

## Versioning convention

DataNudge uses four numeric version components:

```text
major.play-store-publication.feature.bug-fix
```

- The first number represents major maturity.
- The second number increases with consecutive Google Play publications.
- The third number increases for feature releases.
- The fourth number increases for bug-fix releases.

## Planned bug fixes for 0.1.0.2

### Disclaimer action buttons on smaller screens

**Current behaviour:**  
The mandatory disclaimer text scrolls, but the `Decline and exit` action can sit too low and become inaccessible on smaller screens.

**Required change:**  
Keep the action area permanently visible while only the disclaimer body scrolls.

The following actions must always remain accessible:

- `Decline and exit`
- `Accept and continue`

### Mobile-data alert redesign

**Current behaviour:**  
The alert shown when a monitored app such as YouTube opens on mobile data needs improvement.

**Required change:**  
Review and redesign the wording, layout, visual hierarchy, size, positioning, button behaviour, duration and repeat frequency.

The redesigned alert should:

- clearly identify the monitored app;
- clearly state that mobile data is active;
- avoid appearing excessively often;
- provide an obvious dismissal action;
- remain readable on smaller screens;
- use DataNudge's blue and green visual language.

### Move configuration items into Settings

**Current behaviour:**  
Permissions and monitored-app selection are presented too prominently on the main screen.

**Required change:**  
Move the following items into the top-right Settings area:

- Permissions
- Apps to monitor

The home screen should remain focused on:

- current connection status;
- current connection duration;
- monitoring enabled or paused state;
- essential monitoring controls.

### Launcher icon appears too small

**Current behaviour:**  
The launcher icon has too much transparent padding and appears smaller than neighbouring Android app icons.

**Required change:**  
Increase the shield artwork within the adaptive-icon safe area and reduce unnecessary transparent padding without allowing clipping on common launcher shapes.

### About page needs a Close button

**Current behaviour:**  
The About page has no obvious visible close action.

**Required change:**  
Add a clearly visible `Close` button while retaining support for the Android back gesture or back button.

## Completed in 0.1.0.1

- Added `für Lena` above the version number on the About page.
- Added mandatory first-launch acceptance of the Disclaimer and Terms of Use.
- Added a permanent Disclaimer and Terms of Use link on the About page.
- Added an online disclaimer page.
- Added wording covering battery and device-resource use.
- Added explicit user responsibility for connection checking, data use, battery level and resulting charges.
- Published a downloadable GitHub APK release.


## Completed in 0.1.1.0

- Added a live Connected for duration to the current-connection card.
- Added approximate device-wide data usage since the connection changed.
- Stored the connection-session start time and traffic baselines locally.
- Reset the connection session when the effective connection type changes.
- Added safe handling for device reboot, unsupported counters and counter resets.
- Labelled usage as approximate because Android traffic counters may differ from provider billing.


## Completed in 0.1.2.0

- Added the DataNudge logo to the left side of the mobile-data reminder.
- Reworked the reminder header to use horizontal space more efficiently.
- Added an Updates option to the top-right menu.
- Added a manual GitHub latest-release check.
- Added installed-version and latest-version reporting.
- Added an Open download page action when a newer version is available.
- The update checker does not download or install updates automatically.


## Completed in 0.1.3.0

- Removed Apps to monitor and Permissions from the Home screen.
- Kept those functions available through Settings.
- Added a top-left back arrow to every secondary screen.
- Added an in-app navigation history.
- Android Back now returns to the previous DataNudge screen.
- Back from a nested page returns to its actual parent screen.
- Removed the Apps screen Done button.
- Back from Home retains normal Android behaviour and minimizes/exits the app.


## Completed in 0.1.4.0

- Added a direct Download APK button to the Updates page.
- The button uses the APK asset URL from the latest GitHub release.
- Added a secondary View release information action.
- The app still does not download or install updates silently.
- Renamed Close app - keep monitoring to Hide DataNudge.
- Added the supporting text Monitoring will continue in the background.
- Removed the monitored-app count from the Home connection card.
- The selected-app count remains available under Settings.


## Additional completion in 0.1.4.0

- Added permanent Privacy statement access on the About page.
- Added permanent Disclaimer notice access on the About page.
- The disclaimer opens inside DataNudge and retains its Read online option.
- Privacy information opens the published DataNudge privacy page.
- Existing mandatory first-run disclaimer acceptance remains unchanged.


## Completed in 0.1.4.1

- Fixed stale connection duration after reopening DataNudge.
- Fixed inconsistent data-used refresh after returning from the background.
- Added an immediate refresh when the Activity receives ON_RESUME.
- Re-reads the active connection, session start, traffic counters and monitoring state.
- Foreground UI values refresh once per second while DataNudge is visible.
- The UI refresh loop pauses while DataNudge is in the background.
- Resuming the Activity does not reset the active connection session.


## Completed in 0.1.4.2

- Privacy statement now opens inside DataNudge, like the disclaimer notice.
- The in-app privacy notice retains a Read online option.
- Removed the duplicate About DataNudge row from Settings.
- About remains available from its dedicated top-right menu item.
- Fixed stale connection duration after reopening DataNudge.
- Fixed inconsistent data-used refresh after returning from the background.
- Added immediate lifecycle refresh on ON_RESUME.
- The foreground UI refresh pauses while DataNudge is backgrounded.


## Completed in 0.1.5.0

- Added automatic GitHub update checks approximately once per day.
- Added a Settings switch for automatic update checks.
- Uses unique WorkManager jobs to prevent duplicate schedules.
- Manual opening of Updates performs a fresh check.
- The existing monitoring notification shows when an update is available.
- Tapping an update-aware monitoring notification opens Updates.
- Added Download and install APK.
- Shows download progress.
- Verifies the GitHub-provided SHA-256 digest when available.
- Opens the Android package installer after downloading.
- Handles Android's per-app Install unknown apps permission.
- Installation still requires explicit Android user confirmation.
- Automatic checking is intended for the GitHub/testing build; a future Play build should use Play-managed updates.


## Completed in 0.2.0.0

- Added a separate Installed apps option under Settings.
- Added a read-only Installed apps page.
- The page is populated from apps that can normally be launched on the phone.
- Apps are sorted alphabetically.
- Each row shows the app icon, display name and package name.
- Added loading, empty-list and error states.
- Existing Apps to monitor behaviour remains unchanged.
- Hidden Android system components are intentionally excluded.


## Completed in 0.2.1.0

- Replaced the fixed visible high-data app list with installed apps.
- Apps to monitor now lists all launchable installed apps.
- Each app shows its icon, display name, package name and checkbox.
- Removed the temporary separate Installed apps Settings option and page.
- On first initialization only, installed matches from the former 20 defaults are selected.
- Existing saved selections are preserved and are never overwritten.
- A user may deliberately select no apps; that empty choice is preserved.
- Monitoring continues to use the saved package-name selection set.

