# iOS Keyboard for Android

An iPhone-inspired Android keyboard built as a real Android Input Method Editor (IME).

## Features
- iOS-inspired light keyboard surface, rounded white letter keys and gray modifier keys
- QWERTY and symbols layouts
- Shift, delete, space, return and emoji key
- Android input-method picker support
- No typing analytics, network calls, or text collection in this starter version
- GitHub Actions builds a debug APK

## Build
The GitHub Actions workflow builds the APK on pushes and pull requests. Download **iOS-Keyboard-debug** from the workflow run's Artifacts section.

To build locally with JDK 17 and Gradle 8.9:
```bash
gradle --no-daemon :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Install and enable
1. Install the APK on your Android device.
2. Open **iOS Keyboard**.
3. Tap **Enable keyboard** and enable **iOS Style Keyboard** in Android settings.
4. Return to the app, tap **Select iOS Keyboard**, then choose it.
5. Open any text field to use the keyboard.

Android displays a system warning when enabling third-party keyboards. Review the app's permissions and privacy before enabling any keyboard.

## Notes
This is an iPhone-inspired design, not Apple's proprietary keyboard software. Predictive text, swipe typing, voice dictation, and multilingual dictionaries are not included in this first build.
