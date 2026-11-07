# LinkPicker

Android app that asks **which browser to use** every time you tap a web link.

## Features

- **Browser picker** on every http/https link (bottom sheet over the app you were in), listing all installed browsers
- **Set as default browser** button (system dialog on Android 10+, default-apps settings otherwise)
- **Always use X for this site** – per-domain rules, managed (and removable) in the app
- **WhatsApp vs WhatsApp Business** – for wa.me / whatsapp.com links, both installed apps are listed so you pick which one opens the chat
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

### Building a signed release (optional)

Without any setup, `assembleRelease` produces an **unsigned** APK. To sign it with your own key:

1. Create a keystore **outside** the project folder, e.g. `keytool -genkeypair -keystore ~/keys/linkpicker.jks -alias linkpicker -keyalg RSA -keysize 4096 -validity 36500`.
2. Create `keystore.properties` in the project root (it is gitignored, never commit it):
   ```
   storeFile=C:/Users/you/keys/linkpicker.jks
   storePassword=...
   keyAlias=linkpicker
   keyPassword=...
   ```
3. Run `./gradlew assembleRelease`. The APK is in `app/build/outputs/apk/release/`.

Back up your keystore and passwords: if you lose them you can't ship updates that install over an existing install.

## How it works

- `ChooserActivity` declares a VIEW intent filter for `http`/`https` with `BROWSABLE` + `DEFAULT`. That is what makes the app selectable as the default browser and lets it receive every link.
- Installed browsers are found by querying a generic `https://www.example.com` VIEW intent: only apps that accept *any* web link match. LinkPicker excludes itself from the list.
- A browser is launched with an explicit `ComponentName`, so the system default (LinkPicker) is bypassed and there is no loop.
- `<queries>` in the manifest provides the package visibility required on Android 11+ (including the two WhatsApp packages, `com.whatsapp` and `com.whatsapp.w4b`).
- For WhatsApp hosts, `BrowserRepo.whatsappApps` checks which of those two packages can open the link and puts them before the browsers in the list.

## WhatsApp vs WhatsApp Business

If you have both WhatsApp and WhatsApp Business installed, Android picks one for `wa.me` links on its own, and it is often the wrong one. LinkPicker fixes that: for `wa.me`, `api.whatsapp.com`, `chat.whatsapp.com` and `web.whatsapp.com` links, every installed WhatsApp is listed at the top of the picker, above your browsers. Pick one, or turn on **Always use X for this site** to remember it.

For these links to reach LinkPicker, turn off WhatsApp's own link handling:

1. Settings > Apps > **WhatsApp** > Open by default > turn **Open supported links** off.
2. Do the same for **WhatsApp Business**.

(Menu names vary slightly by manufacturer.) LinkPicker must also be your default browser.

## Limitations (Android, not the app)

- Links verified as **App Links** (e.g. youtube.com, maps links) may open in their own app and never reach a browser. For WhatsApp, see the steps above.
- Apps that use **in-app browsers / Custom Tabs** (Instagram, Gmail, etc.) don't always go through the default browser.
- Some manufacturers hide the role dialog; use the "Default apps settings" button in that case.

## Ideas for later

Per-app rules (needs usage-access permission), incognito shortcuts for specific browsers, import/export of rules, handling shared text containing URLs.

## License

[MIT](LICENSE)
