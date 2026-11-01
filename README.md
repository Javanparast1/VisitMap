# VisitMap

Offline-first Android app for recording and managing physical shops.

## Implemented

- Native Android application using Kotlin and Jetpack Compose (Persian RTL interface).
- Local Room database with customer, current-location, location-observation, and location-history tables.
- Customer registration without requiring a location.
- Customer list and unified search by shop name, contact name, or phone.
- Customer detail and editing while preserving the customer ID.
- Deactivation/reactivation without physically deleting customer data.
- Domain validation for required shop name and supported shop types.
- Location proposals remain separate from the approved current location and require explicit acceptance.

## Not yet implemented

- GPS/location capture and manual map selection.
- Offline map rendering and geospatial/geocoder features.
- Duplicate-customer assessment.
- Backup and restore.
- Full field-pilot acceptance on the reference Redmi 12C device.

These capabilities remain governed by the specifications in `docs/`. A successful build or emulator test is not evidence that the app has passed field acceptance.

## Build baseline

- Android Gradle Plugin: 8.13.2
- Gradle: 8.13
- Kotlin: 2.3.21
- JDK: 17
- Compile SDK: 36
- Minimum SDK: 26
- Target SDK: 35
- Jetpack Compose BOM: 2026.06.01
- Room: 2.8.5

Versions are centralized in `gradle/libs.versions.toml`.

## Build and test

CI runs JVM unit tests, Android lint, debug APK build, and Room instrumentation tests on an Android emulator. It uploads the debug APK and Room schema as a short-lived workflow artifact.

The workflow installs Gradle directly; a Gradle Wrapper is not included yet.

Local build requirements: JDK 17, Android SDK platform 36, Build Tools 36.0.0, and Gradle 8.13.

```bash
gradle --no-daemon test lint assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Privacy boundary

Customer records are stored locally in the app's private database. The app does not request network or location permissions in this customer-operations stage. Do not add real customer data to this public repository, CI logs, or workflow artifacts.

Android Auto Backup remains disabled. Explicit user-controlled backup/restore is not implemented yet.
