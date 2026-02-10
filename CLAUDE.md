# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app (Java) for saving WhatsApp and WhatsApp Business status updates (photos/videos). Also includes WhatsApp Web viewer, direct chat without saving contacts, and text formatting tools.

**Package:** `com.Udaicoders.wawbstatussaver`
**Min SDK:** 21 | **Target/Compile SDK:** 34 | **Java:** 1.8
**License:** Apache 2.0

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.Udaicoders.wawbstatussaver.ExampleUnitTest"
```

CI uses JDK 11 (temurin). Ensure `gradlew` has execute permission (`chmod +x gradlew`).

## Multi-Module Structure

- **`:app`** — Main application module
- **`:countrycodepicker`** — Local library module for phone number country code selection (uses libphonenumber-android)

## Architecture

**Pattern:** Activity/Fragment-based with static utility singletons. No DI framework.

### App Flow
`SplashActivity` → (first launch) `IntroActivity` → `MainActivity`

### MainActivity Tabs
Uses `ViewPager` with two primary fragments:
- **`RecentStatusFragment`** — Displays WhatsApp statuses from the device, with image/video tab filtering
- **`DownloadsFragment`** — Shows locally saved statuses

### Key Packages
```
com.Udaicoders.wawbstatussaver/
├── adapter/          # RecyclerView/GridView adapters (RecentAdapter, MyStatusAdapter, PreviewAdapter)
├── fragment/         # RecentStatusFragment, DownloadsFragment, MyPhotos, MyVideos
├── model/            # StatusModel (Parcelable) — represents a status file
├── util/             # Static utility classes
│   ├── AdController  # AdMob + IronSource ad management
│   ├── SharedPrefs   # SharedPreferences wrapper (prefs name: "wa_data")
│   ├── FileUtils     # File operations singleton
│   └── Utils         # Permissions, downloads, language switching
├── font/             # Text formatting feature (FontActivity + sub-fragments/adapters)
├── waweb/            # WhatsApp Web WebView (WAWebActivity, BlobDownloader)
├── intro/            # Onboarding slides (IntroActivity)
└── slidingrootnav/   # Embedded custom sliding navigation drawer library
```

### File Access Strategy
- **Android 11+ (API 30+):** Uses `DocumentFile` API via Storage Access Framework to read WhatsApp's scoped storage directories
- **Older APIs:** Direct file system access to WhatsApp status directories
- Saved statuses go to external storage (`Images/`, `Videos/` subdirectories)

### Ad Integration
Dual ad network setup: **AdMob** (primary) + **IronSource** mediation (with Facebook Audience Network adapter). Controlled via `AdController` with a toggle (`isLoadIronSourceAd`). Current string resources use AdMob test IDs.

### Navigation
Custom embedded `slidingrootnav` library provides the sliding drawer menu in `MainActivity`. Menu items include WhatsApp/WB selection, saved statuses, dark mode toggle, language picker, share, rate, privacy policy, and help.

## Key Dependencies

- **Glide** — Image loading/caching
- **Lottie** — Animation playback
- **Room** — SQLite database
- **Gson** — JSON serialization
- **SDP** — Scalable size units for responsive layouts
- **Apache Commons IO** — File operations (bundled as JAR in `libs/`)
- **IronSource + Facebook Audience Network** — Ad mediation

## Localization

Three languages supported: English (default), Hindi (`values-hi-rIN`), Arabic (`values-ar`). Language bundle splitting is disabled in the Gradle config so all translations ship in the APK. Language preference is stored via `Utils.setLanguage()` / `SharedPrefs`.

## Theming

Dark/Light mode via `AppCompatDelegate` theme switching. Night-specific colors in `values-night/colors.xml`. Theme preference stored in `SharedPrefs`.
