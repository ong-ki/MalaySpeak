# Third-Party Notices

This document lists the third-party libraries, build plugins, platform services, and assets referenced by **MalaySpeak (Cakap Melayu)** (`android/gradle/libs.versions.toml`).

## Android & Kotlin Libraries

All licenses below were verified from each artifact's Maven POM metadata in the local Gradle dependency cache.

| Dependency | Artifact | License |
|---|---|---|
| AndroidX Core KTX | `androidx.core:core-ktx:1.12.0` | Apache License 2.0 |
| AndroidX Core SplashScreen | `androidx.core:core-splashscreen:1.0.1` | Apache License 2.0 |
| AndroidX Lifecycle Runtime KTX | `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0` | Apache License 2.0 |
| AndroidX Lifecycle Runtime Compose | `androidx.lifecycle:lifecycle-runtime-compose:2.7.0` | Apache License 2.0 |
| AndroidX Lifecycle ViewModel Compose | `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0` | Apache License 2.0 |
| AndroidX Activity Compose | `androidx.activity:activity-compose:1.8.2` | Apache License 2.0 |
| Jetpack Compose BOM | `androidx.compose:compose-bom:2024.04.00` | Apache License 2.0 |
| Jetpack Compose UI | `androidx.compose.ui:ui` | Apache License 2.0 |
| Jetpack Compose UI Graphics | `androidx.compose.ui:ui-graphics` | Apache License 2.0 |
| Jetpack Compose UI Tooling (debug) | `androidx.compose.ui:ui-tooling` | Apache License 2.0 |
| Jetpack Compose Material 3 | `androidx.compose.material3:material3` | Apache License 2.0 |
| Jetpack Compose Material Icons Extended | `androidx.compose.material:material-icons-extended` | Apache License 2.0 |
| AndroidX Navigation Compose | `androidx.navigation:navigation-compose:2.7.7` | Apache License 2.0 |
| AndroidX DataStore Preferences | `androidx.datastore:datastore-preferences:1.0.0` | Apache License 2.0 |
| AndroidX Room Runtime | `androidx.room:room-runtime:2.6.1` | Apache License 2.0 |
| AndroidX Room KTX | `androidx.room:room-ktx:2.6.1` | Apache License 2.0 |
| AndroidX Room Compiler (KSP) | `androidx.room:room-compiler:2.6.1` | Apache License 2.0; BSD License (ANTLR) |
| Kotlinx Coroutines Core | `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0` | Apache License 2.0 |
| Kotlinx Coroutines Android | `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0` | Apache License 2.0 |
| Kotlinx Coroutines Test (unit tests) | `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0` | Apache License 2.0 |
| JUnit 4 (unit tests) | `junit:junit:4.13.2` | Eclipse Public License 1.0 |
| MockK (unit tests) | `io.mockk:mockk:1.13.10` | Apache License 2.0 |
| Robolectric (unit tests) | `org.robolectric:robolectric:4.12.2` | MIT License |
| AndroidX Test Core (unit tests) | `androidx.test:core:1.5.0` | Apache License 2.0 |

## Gradle Plugins

| Plugin | Artifact | License |
|---|---|---|
| Android Gradle Plugin | `com.android.tools.build:gradle:8.3.2` | Apache License 2.0 |
| Kotlin Android Plugin | `org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22` | Apache License 2.0 |
| Kotlin Symbol Processing (KSP) | `com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.22-1.0.17` | Apache License 2.0 |

## Fonts, Audio, and Phrase Data

- **Android fonts**: Uses Android system `FontFamily.Serif` and `FontFamily.SansSerif` (`ui/theme/Type.kt`); no custom font files are bundled in the APK.
- **Audio playback**: No prerecorded audio files are bundled; pronunciation playback uses the Android platform `TextToSpeech` API (`android.speech.tts.TextToSpeech`) with the device's installed `ms-MY` voice engine.
- **Web prototype fonts** (`index.html`): Links Google Fonts stylesheets (`Inter`, `Noto Sans Myanmar`, `Space Mono`) when opened in a browser.

## Needs verification

- **Phrase dataset** (`android/app/src/main/assets/phrases.json`, `android/app/src/main/java/com/malayspeak/app/data/local/seed/PhraseSeedData.kt`, and `src/data/phrases500.ts`): No upstream source attribution or separate data license file is present in the repository.
