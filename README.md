# Gaming Keyboard

An Android keyboard for phones that sends real PC key presses, which makes shortcuts like
**Alt+Tab**, **Alt+F4**, **Win+D** and **Ctrl+Shift+Esc** work when streaming a PC with
[Moonlight](https://moonlight-stream.org/).

## Why another keyboard?

Typical phone keyboards type *text*. Moonlight needs *key presses*: separate press and release
events for every key, including modifiers held across several other keys. This keyboard sends
only raw key events, exactly as a USB keyboard would, so Moonlight passes each one through to
the PC.

## Features

- **Shortcut row** on top: Esc, Alt+Tab, Alt+F4, Win, Win+Tab, Win+D, Task Manager, Hide.
  Holding a shortcut holds all of its keys.
- **Full PC layout**: number row, Tab, Caps, both Shifts, Ctrl, Win, Alt, Del and arrows.
- **Fn layer**: F1–F12, PrtSc, Ins/Home/End/PgUp/PgDn, ScrLk, Pause, Menu and a number pad.
- **Multi-touch**: hold several keys at once (W + Shift + Space). A key stays down until its
  finger lifts, even if the finger slides off the key.
- **Modifiers** (Ctrl, Alt, Shift, Win):
  - *hold* it while pressing other keys, like a real keyboard;
  - *tap* it to apply it to the next key only (tapping Win then D sends Win+D without opening
    the Start menu first);
  - *double-tap* it to lock it down until it is tapped again. To pick any window: lock Alt,
    tap Tab until the window you want is selected, then tap Alt.
  - Turn off "Sticky modifiers" in settings to make modifiers plain keys instead.
- Never goes fullscreen in landscape, and releases every key when it closes, so nothing gets
  stuck down on the PC.
- Adjustable key height and haptics.

## Install

1. Open the latest successful run of the **Build APK** workflow under the repository's
   **Actions** tab and download the `gaming-keyboard-apk` artifact.
2. Unzip it and install `app-debug.apk` on the phone (allow installs from unknown sources).
3. Open **Gaming Keyboard**, tap **Enable the keyboard**, then **Switch to it**.

Every build is signed with the same debug key (`app/debug.keystore`), so new builds install
over old ones without uninstalling first.

## Using it with Moonlight

Start a stream and open the keyboard (three-finger tap, or from Moonlight's in-stream menu).

Moonlight keeps a few combos for itself and never sends them to the PC:
Ctrl+Alt+Shift+Q (quit stream), Ctrl+Alt+Shift+Z (release mouse) and Ctrl+Alt+Shift+C
(toggle cursor).

Shortcuts only reach the app the keyboard is typing into. Android doesn't let keyboards trigger
system actions, so on the phone itself Alt+Tab won't open Android's app switcher.

## Building

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

## Code layout

| Path | What it does |
| --- | --- |
| `keyboard/Layouts.kt` | Shortcut row, main layer and Fn layer |
| `keyboard/KeyboardController.kt` | Touches → key presses; hold / one-shot / lock behavior of modifiers |
| `keyboard/HostKeys.kt` | Tracks which keys the PC sees as held; computes Android meta state |
| `keyboard/KeyboardView.kt` | Draws keys and handles multi-touch |
| `ime/GamingKeyboardService.kt` | The Android input method; sends raw `KeyEvent`s |
| `MainActivity.kt` | Setup steps and settings |
