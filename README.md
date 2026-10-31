# VisitMap

Offline-first Android customer mapping and visit planning.

## Project status

The Android bootstrap is in progress. This branch adds the first native Android application shell and a GitHub Actions workflow for validating and producing a debug APK.

The initial screen is a Persian, right-to-left Compose screen. Customer records, MVVM feature flows, local database, location capture, and offline maps have **not** been implemented yet. These remain governed by the specifications in `docs/`.

## Build baseline

- Android Gradle Plugin: 8.13.2
- Gradle: 8.13
- Kotlin: 2.3.21
- JDK: 17
- Compile SDK: 36
- Minimum SDK: 26
- Jetpack Compose BOM: 2026.07.00
- Activity Compose: 1.13.0

Compose BOM 2026.07.00 is pinned to the Compose 1.11.4 generation, which predates the API 37 requirement introduced in Compose 1.12. The first CI attempt exposed an obsolete Kotlin compiler configuration; the project now uses the typed `compilerOptions` DSL. The baseline remains subject to successful CI and device validation.

Versions are centralized in `gradle/libs.versions.toml`.

## Build with GitHub Actions

Pushes to `main` and `feat/**` branches, pull requests targeting `main`, and manual workflow dispatches run the **Android CI** workflow.

The workflow runs unit-test, Android lint, and debug APK build tasks. On success, it uploads an artifact named `visitmap-debug-apk`, retained for 7 days.

The workflow installs Gradle directly on the runner; a Gradle Wrapper is not included yet.

## Local build

With JDK 17, Android SDK platform 36, Build Tools 36.0.0, and Gradle 8.13 installed:

```bash
gradle --no-daemon test lint assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Privacy boundary

The initial application requests no location or network permissions and does not collect or transmit customer data. Do not add real customer data to this public repository, CI logs, or workflow artifacts.
