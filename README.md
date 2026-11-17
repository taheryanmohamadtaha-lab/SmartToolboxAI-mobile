# Smart Toolbox AI – Always Available Floating Tools

Android app (Kotlin, Jetpack Compose, Material 3) with a floating toolbox that stays above other apps.
UI inspired by the "Floating Toolbox" reference: purple→magenta gradient, white cards, centered dark radial menu.

## Build the APK (no programming needed)
1. Create a new GitHub repository and upload **all files of this folder** (including the hidden `.github` folder).
2. Open the **Actions** tab → *Android Build* runs automatically (or press *Run workflow*).
3. When finished, open the run → **Artifacts** → download `app-debug` (contains `app-debug.apk`) or `app-release`.
4. Copy the APK to your phone and install it (allow "install unknown apps").
Push a tag like `v1.0.0` to also publish the APKs as a GitHub Release.

### Release signing (optional)
Without secrets the release APK is signed with the debug key (installable, not for Play Store).
For your own key add these repository secrets: `KEYSTORE_BASE64` (base64 of your .jks), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Features
- Floating assistant: draggable, size/transparency/color/icon, left/right, edge snapping, hide/show (also from notification), radial menu with selectable tools, custom app/URL shortcuts in the menu, boot auto-start (where Android allows), foreground service notification.
- Screenshot Studio (MediaProjection): delay, PNG/JPEG + quality, own folder, auto names, sound, notification; editor with pen/arrow/line/rect/circle/text, pixelate/blur, crop, rotate, resize, undo/redo, save, share, copy, rename, delete, info, gallery.
- Floating calculator (memory, history, copy, drag, resize, light/dark) and floating notepad (auto-save, multiple notes, pin, share, transparent).
- Notes (search, pin), clipboard manager (manual/consented, favorites, search), stopwatch, countdown timer, clock, calendar shortcut, reminders, unit converter, translator (opens Google Translate), QR scan (camera / image) and QR generator (text, URL, Wi-Fi, contact; save/share).
- Mini tools: brightness/orientation/timeout/magnification (open the proper system screens), lock screen/recents/back (optional accessibility helper), camera, flashlight, settings, share text, quick search, display info.
- System info: battery %, temperature, charging, network, storage, RAM, device, Android version; Wi-Fi and Bluetooth shortcuts.
- Persian + English (RTL), light/dark/AMOLED, accent colors, backup/restore, reset.

## Permissions
| Permission | Why |
|---|---|
| SYSTEM_ALERT_WINDOW | Show the floating button/windows above other apps |
| FOREGROUND_SERVICE (+ specialUse, mediaProjection) | Keep the toolbox alive / capture screenshots |
| POST_NOTIFICATIONS | Service notice, screenshot and reminder notifications |
| CAMERA | QR scanning (only when you open the scanner) |
| RECEIVE_BOOT_COMPLETED | Optional auto-start and reminder re-scheduling |
| ACCESS_NETWORK_STATE | Show network status |
| Accessibility service (optional) | Only lock screen / recents / back. Never reads content |
| MediaProjection consent | Asked by Android for every screenshot session |

No data is uploaded. Everything stays on the device.

## Known Android limitations
- Android asks screen-capture consent each time; no silent/background screenshots. Secure windows (banking apps, DRM video) appear black.
- Scrolling screenshots are not included (no reliable universal API).
- Brightness, rotation, timeout, Wi-Fi, Bluetooth, magnification cannot be toggled by normal apps: the app opens the relevant system screen.
- Clipboard cannot be read in the background on Android 10+.
- Some manufacturers (Xiaomi, Huawei, Samsung…) kill background services: allow "autostart" / disable battery optimization for the app.
- Notes/clipboard/shortcuts use local JSON storage (SharedPreferences) instead of Room, and manual DI instead of Hilt, to keep the build simple and stable.

## Updating
Upload the new source to the same repository; Actions builds a new APK. Install it over the old one (same signing key keeps your data).

---
## فارسی (خلاصه)
1. همهٔ فایل‌ها (با پوشهٔ `.github`) را در یک مخزن گیت‌هاب آپلود کنید.
2. در تب **Actions** ساخت خودکار اجرا می‌شود.
3. از بخش **Artifacts** فایل `app-debug` را دانلود و نصب کنید.
