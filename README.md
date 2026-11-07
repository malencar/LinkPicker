# LinkPicker

Android app that asks **which browser to use** every time you tap a web link.

## Features

- **Browser picker** on every http/https link (bottom sheet over the app you were in), listing all installed browsers
- **Set as default browser** button (system dialog on Android 10+, default-apps settings otherwise)
- **Always use X for this site** – per-domain rules, managed (and removable) in the app
- **Most-used browsers first**, plus the option to hide browsers you never use
- **Remove tracking parameters** (utm_*, fbclid, gclid, msclkid, ...) before opening
- Real **host shown prominently** in the picker to help spot look-alike domains
- **Copy link / Share** from the picker
- Optional **link history** (off by default, stored locally only)
- Opens directly when only one browser is available
- English and Brazilian Portuguese UI; dark mode and Material You colors

## Build

1. Open the folder in **Android Studio** (Ladybug or newer, JDK 17). It downloads Gradle 8.9 and syncs on its own.
2. Run on a device/emulator (minSdk 26, Android 8.0+), or `Build > Build APK(s)`.
3. Open LinkPicker and tap **Set as default browser**.

Package: `com.malencar.linkpicker`

## How it works

- `ChooserActivity` declares a VIEW intent filter for `http`/`https` with `BROWSABLE` + `DEFAULT`. That is what makes the app selectable as the default browser and lets it receive every link.
- Installed browsers are found by querying a generic `https://www.example.com` VIEW intent: only apps that accept *any* web link match. LinkPicker excludes itself from the list.
- A browser is launched with an explicit `ComponentName`, so the system default (LinkPicker) is bypassed and there is no loop.
- `<queries>` in the manifest provides the package visibility required on Android 11+.

## Limitations (Android, not the app)

- Links verified as **App Links** (e.g. youtube.com, maps links) may open in their own app and never reach a browser.
- Apps that use **in-app browsers / Custom Tabs** (Instagram, Gmail, etc.) don't always go through the default browser.
- Some manufacturers hide the role dialog; use the "Default apps settings" button in that case.

## Ideas for later

Per-app rules (needs usage-access permission), incognito shortcuts for specific browsers, import/export of rules, handling shared text containing URLs.

## License

[MIT](LICENSE)
