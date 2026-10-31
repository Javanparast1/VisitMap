# VisitMap

Offline-first Android customer mapping and visit planning.

## Project status

The Android bootstrap is in progress. The current branch adds the first native Android application shell and a GitHub Actions workflow for producing a debug APK.

The initial screen is a Persian, right-to-left Compose screen. Customer records, location access, local database, and offline maps have **not** been implemented yet.

## Build baseline

- Android Gradle Plugin: 8.13.2
- Gradle: 8.13
- Kotlin: 2.3.21
- JDK: 17
- Compile SDK: 36
- Minimum SDK: 26
- Jetpack Compose BOM: 2026.09.00

Versions are centralized in `gradle/libs.versions.toml`.

## Build with GitHub Actions

Pushes to `main` and `feat/**` branches, pull requests targeting `main`, and manual workflow dispatches run the **Android CI** workflow.

On success, the workflow uploads an artifact named `visitmap-debug-apk`. The artifact contains a debug-only APK and is retained for 7 days.

The bootstrap workflow installs Gradle directly on the runner; a Gradle Wrapper is not included yet.

## Local build

With JDK 17, Android SDK platform 36, Build Tools 36.0.0, and Gradle 8.13 installed:

```bash
gradle --no-daemon assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Privacy boundary

The initial application requests no location or network permissions and does not collect or transmit customer data. Do not add real customer data to this public repository, CI logs, or workflow artifacts.
