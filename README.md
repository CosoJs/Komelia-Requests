# Komelia Requests

A fork of [Komelia](https://github.com/Snd-R/Komelia) (Komga media client) with a **Requests** tab: discover
manga and light novels, request them, and follow their availability in your Komga library — without leaving
the reader app.

It works together with **[Taberna Requests](https://github.com/CosoJs/taberna-requests)**, a small self-hosted
request server for Komga. This app is the mobile/desktop front end; the server does the AniList search, approval,
indexer search (Prowlarr) and downloads (qBittorrent).

<img src="screenshots/fork/requests-tab.jpg" alt="Requests tab" width="270">

### What this fork adds

- **Requests tab** — bottom bar on mobile, side menu on desktop:
  - **Discover:** search AniList (manga / light novels) and request a title.
  - **Requests:** every request with its state and Komga availability (*Available*, *Partial 8/12*).
  - **For admins:** approve or deny, pick a release from your indexers, follow ongoing series.
- **No extra login:** it signs in to the request server with the Komga session Komelia already keeps, so no
  password is stored or sent.
- **Installs next to upstream Komelia** (own application id) and has self-updates disabled, so it never gets
  replaced by the upstream app.

### Requirements

- A [Taberna Requests](https://github.com/CosoJs/taberna-requests) server on the **same host as Komga**, served
  at `/pedidos/` (for example `https://example.com/komga` → `https://example.com/pedidos`).
- Sign in to Komga with *remember me* (Komelia's default).

### Downloads

Debug APKs (arm64) are built by GitHub Actions: open the latest successful
[Android APK](../../actions/workflows/android.yml) run and download the `komelia-requests-debug-apk` artifact.

### Changes compared to upstream

All new code lives in `komelia-ui/src/commonMain/kotlin/snd/komelia/ui/requests/`. Upstream files touched
(each change is marked with `// fork:`): `MainScreen.kt`, `topbar/NavigationMenuContent.kt`,
`ViewModelFactory.kt`, `komelia-app/androidApp/build.gradle.kts`, the Android `app_name`, plus the CI workflow in
`.github/workflows/`. The Requests tab UI is currently in Spanish.

Komelia is licensed under the Apache License 2.0 and so is this fork; all credit for the app goes to
[Snd-R](https://github.com/Snd-R) and the Komelia contributors.

---

*Original Komelia README below.*

# Komelia - Komga media client

### Downloads:

- Latest prebuilt release is available at https://github.com/Snd-R/Komelia/releases
- Google Play Store https://play.google.com/store/apps/details?id=io.github.snd_r.komelia
- F-Droid https://f-droid.org/packages/io.github.snd_r.komelia/
- AUR package https://aur.archlinux.org/packages/komelia

## Screenshots

<details>
  <summary>Mobile</summary>
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Komelia" width="270">  
   <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" alt="Komelia" width="270">  
</details>

<details>
  <summary>Tablet</summary>
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/1.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/2.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/3.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/4.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/5.jpg" alt="Komelia" width="400" height="640">  
   <img src="/fastlane/metadata/android/en-US/images/tenInchScreenshots/6.jpg" alt="Komelia" width="400" height="640">  
</details>

<details>
  <summary>Desktop</summary>
   <img src="/screenshots/1.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/2.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/3.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/4.jpg" alt="Komelia" width="1280">  
   <img src="/screenshots/5.jpg" alt="Komelia" width="1280">  
</details>

## Translations
You can help translate this project to your language by using service provided by [Weblate](https://hosted.weblate.org/engage/komelia/)

[![Translation status](https://hosted.weblate.org/widget/komelia/horizontal-auto.svg)](https://hosted.weblate.org/engage/komelia/)

## Build instructions
Make sure you download all git submodules\
`git clone --recurse-submodules https://github.com/Snd-R/Komelia` \
if you already cloned repository without recurse command run\
`git submodule update --init --recursive`

Requires jdk 17 or higher\
Android and JVM targets require C and C++ compiler for native libraries and Node.js for epub readers build.\
Recommended way to build is by using docker images that contain all required build dependencies.\
If you want to build with system toolchain and dependencies try running:\
`./gradlew komeliaBuildNonJvmDependencies` (Linux Only)

## Desktop App
Replace <*platform*> placeholder with your target platform. \
Available platforms include: `linux-x86_64`, `windows-x86_64`

- `docker build -t komelia-build-<platfrom> . -f ./cmake/<paltform>.Dockerfile `
- `docker run -v .:/build komelia-build-<paltform>`
- `./gradlew <platform>_copyJniLibs`
- `./gradlew buildEpubReaders`

Then choose your packaging option:
- `./gradlew :desktopRun` to launch desktop app
- `./gradlew :desktopJar` output in `./komelia-app/desktopApp/build/compose/jars`
- `./gradlew :desktopDeb` output in `./komelia-app/desktopApp/build/compose/binaries`
- `./gradlew :desktopMsi` output in `./komelia-app/desktopApp/build/compose/binaries`

## Android App
Replace <*arch*> placeholder with your target architecture.\
Available architectures include:  `aarch64`, `armv7a`, `x86_64`, `x86`

- `docker build -t komelia-build-android . -f ./cmake/android.Dockerfile `
- `docker run -v .:/build komelia-build-android <arch>`
- `./gradlew <arch>_copyJniLibs`
- `./gradlew buildEpubReaders`

Then choose app build option:

- `./gradlew :androidDebug` output in `./komelia-app/androidApp/build/outputs/apk/debug`
- `./gradlew :androidRelease` output in `./komelia-app/androidApp/build/outputs/apk/release`


## Komf Wasm WebUI
run `./gradlew :komfWebUI` output will be in `./build/komf-webui`

## Komf Wasm Extension
for chrome `./gradlew :komfExtensionChrome` \
for firefox `./gradlew :komfExtensionFirefox` \
output archive will be in `./komelia-komf-extension/app/build/distributions`