# AmnShield — Android App

AmnShield is a privacy-first digital wellness and content-blocking app for Android. It
combines app blocking, keyword filtering, focus mode, scheduled blocking, launch limits,
and AI-powered image filtering into a single accessibility-service–based guardian.

---

## Table of Contents

1. [Requirements](#requirements)
2. [Building the App](#building-the-app)
3. [Product Flavors](#product-flavors)
4. [Android Studio Setup](#android-studio-setup)
5. [If the Build Shows Old UI](#if-the-build-shows-old-ui)
6. [Release Builds](#release-builds)
7. [Project Documentation](#project-documentation)

---

## Requirements

| Tool | Version |
|---|---|
| Android Studio | Meerkat or newer |
| Gradle | 9.5+ (managed by wrapper) |
| Android SDK compile target | API 36 |
| Min Android version | API 26 (Android 8.0) |
| JDK | 17 |

---

## Building the App

> **⚠️ Important — Product Flavors**: AmnShield uses three **product flavors**
> (`playstore`, `fdroid`, `universal`). The plain `assembleDebug` Gradle task is
> **no longer valid**. Always use a flavor-qualified task. Running bare `assembleDebug`
> will silently serve a stale cached APK with old UI and themes.

### Debug Builds (for testing / development)

```powershell
# Build all three flavors at once (recommended)
.\gradlew assemblePlaystoreDebug assembleFdroidDebug assembleUniversalDebug

# Or build individually
.\gradlew assemblePlaystoreDebug   # Google Play Store flavor
.\gradlew assembleFdroidDebug      # F-Droid / offline license flavor
.\gradlew assembleUniversalDebug   # Universal / sideload flavor
```

APK output paths:

| Flavor | APK Location |
|---|---|
| playstore | `app/build/outputs/apk/playstore/debug/app-playstore-debug.apk` |
| fdroid | `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk` |
| universal | `app/build/outputs/apk/universal/debug/app-universal-debug.apk` |

---

## Product Flavors

| Flavor | Description | Google Services | Billing |
|---|---|---|---|
| `playstore` | Google Play Store distribution | ✅ Firebase, Play Billing | Play Billing |
| `universal` | Sideload / alternative stores | ✅ Firebase | Play Billing |
| `fdroid` | F-Droid / fully open source | ❌ No Google dependencies | Offline ECDSA license |

Each flavor has its own source set under `app/src/<flavor>/java/` for swapping
billing and sign-in implementations without `#ifdef`-style hacks.

---

## Android Studio Setup

1. Open the project root in Android Studio.
2. Open the **Build Variants** panel (`View → Tool Windows → Build Variants`).
3. Set the **Active Build Variant** for `:app` to one of:
   - `playstoreDebug`
   - `fdroidDebug`
   - `universalDebug`
4. Hit **Run ▶** as normal.

> If the Build Variants panel shows just `debug` (no flavor prefix), the IDE has not yet
> synced the new flavor configuration. Run **File → Sync Project with Gradle Files** to fix it.

---

## If the Build Shows Old UI

If you see the legacy purple gradient background, old DeenShield icons, or the old
Settings bottom-nav tab instead of the new Blocks tab, the Gradle build cache is serving
a stale APK. Fix it by doing a clean build:

```powershell
# Step 1: Wipe all build outputs and cached intermediates
.\gradlew clean --no-daemon

# Step 2: Rebuild fresh (--no-daemon forces a new JVM, bypassing any stale cached daemon)
.\gradlew assemblePlaystoreDebug assembleFdroidDebug assembleUniversalDebug --no-daemon
```

The `--no-daemon` flag is the key — without it, Gradle may reuse an old daemon process
pinned to a previous Gradle version that has incorrect cached fingerprints.

---

## Release Strategy

### Which APK Goes Where?

| Flavor | Published to | Audience |
|---|---|---|
| `universal` | ✅ **GitHub Releases** | Standard Android — sideloading with Google Play Services |
| `fdroid` | ✅ **GitHub Releases** | De-Googled phones (GrapheneOS, CalyxOS, LineageOS without GMS) |
| `playstore` | ✅ **Play Console only** | Google Play Store users — never published to GitHub |

> `universal` and `fdroid` are **not interchangeable**. The `universal` flavor depends on
> Google Play Services for billing and sign-in. Users on de-Googled ROMs must use the
> `fdroid` APK, which has zero Google dependencies and uses offline ECDSA licensing instead.

### Automated GitHub Releases (CI)

The [release.yml](.github/workflows/release.yml) workflow fires automatically when you
push a version tag. It builds, signs, and publishes both APKs to GitHub Releases:

```bash
# Tag a release (triggers the workflow)
git tag v1.2.3
git push origin v1.2.3
```
For Self Build:
```powershell
# GitHub Releases (the two public APKs)
.\gradlew assembleUniversalRelease
.\gradlew assembleFdroidRelease

# Play Store (submitted via Play Console, not GitHub)
.\gradlew assemblePlaystoreRelease
```

> **Never commit `keystore.properties` or `.jks` files to version control.**
> These are already listed in `.gitignore`.

---

## Project Documentation

| File | Description |
|---|---|
| [TESTING_GUIDE.md](TESTING_GUIDE.md) | Full manual test scenarios for all features |
| [ROADMAP.md](ROADMAP.md) | Feature roadmap and recent changelog |
| [PRIVACY_POLICY.md](PRIVACY_POLICY.md) | App privacy policy |
| [TERMS_OF_SERVICE.md](TERMS_OF_SERVICE.md) | Terms of service |
| [FDROID_PUBLISHING_GUIDE.md](FDROID_PUBLISHING_GUIDE.md) | Steps to publish on F-Droid |
| [MONETIZATION_SETUP_GUIDE.md](MONETIZATION_SETUP_GUIDE.md) | Lemon Squeezy + Supabase licensing setup |
| [ERROR_REPORTING_IMPLEMENTATION_GUIDE.md](ERROR_REPORTING_IMPLEMENTATION_GUIDE.md) | Error reporting architecture |

---

## License

This project is licensed under the **GNU General Public License v3.0**.
See [LICENSE](LICENSE) for the full text.
