# DataNudge Bugs and Features

**Current released version:** `0.1.0.1`  
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

### About-page dedication colour

**Current behaviour:**  
The `für Lena` text uses the default text colour.

**Required change:**  
Display `für Lena` using the same colour and visual treatment as the version number directly below it.

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

## Planned feature for 0.1.1.0

### Current connection duration

Display how long the phone has been on its current connection type.

Example for mobile data:

```text
On mobile data since 1 August 2026, 14:35
Connected for 26 minutes
```

Example for Wi-Fi:

```text
On Wi-Fi since 1 August 2026, 13:10
Connected for 1 hour 51 minutes
```

### Functional requirements

The connection-duration feature should:

- start a new timer whenever the connection type changes;
- update while the main screen is open;
- survive closing and reopening the app;
- survive service restarts;
- store the connection start time locally;
- reset when switching between Wi-Fi, mobile data, VPN, offline or another connection type;
- display concise durations such as `4 minutes`, `1 hour 12 minutes`, or `2 days 3 hours`.

### Suggested placement

Display the connection start time and duration directly below the current connection status on the home screen.

## Completed in 0.1.0.1

- Added `für Lena` above the version number on the About page.
- Added mandatory first-launch acceptance of the Disclaimer and Terms of Use.
- Added a permanent Disclaimer and Terms of Use link on the About page.
- Added an online disclaimer page.
- Added wording covering battery and device-resource use.
- Added explicit user responsibility for connection checking, data use, battery level and resulting charges.
- Published a downloadable GitHub APK release.
