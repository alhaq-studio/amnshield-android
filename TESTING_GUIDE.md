# AI Image Blocking - Testing Guide

## Latest UI and Navigation Updates (May 2026)

## Test Coverage Overview

| Feature Area | Coverage | Notes |
|---|---|---|
| Smart Blur / AI image blocking | Complete | Tests 1-10 |
| App Blocker | Expanded | Test 11 |
| Keyword Blocker | Expanded | Test 12 |
| Focus Mode | Expanded | Test 13 |
| Cheat Hours | Expanded | Test 14 |
| Schedules & Groups | Expanded | Test 15 |
| Launch Limits | Expanded | Test 16 |
| Error Reporting | Expanded | Test 17 |
| Blocks Dashboard | Expanded | Test 18 (see below) |

### Navigation Sanity Check
1. Open app and verify bottom navigation shows: **Home, Stats, Blocks, Profile**
2. Confirm **Settings** is available from top-right overflow menu (not bottom nav)
3. Open left drawer and confirm **About** is not present there
4. Open top-right overflow and confirm **About** exists there
5. Tap **Stats** tab then tap **View Reports** button — confirm it opens `ReportsActivity` (Reports is not a bottom-nav tab)

### Blocks Dashboard Sanity Check
1. Open **Blocks** from bottom navigation
2. Verify all management cards are visible:
  - App Blocker
  - Keyword Blocker
  - Focus Mode
  - Cheat Hours
  - Schedules
  - Launch Limits
3. Tap the floating **+** button and verify one all-in-one setup menu appears with those same options
4. Verify each option opens the expected screen/dialog without crash

### Launch Limits Flow Check
1. Go to **Stats** → **View Details** → app usage breakdown for any app
2. Tap **App Opens** and create a launch limit
3. Return to Blocks dashboard and verify launch limit count updates
4. Open **Manage Launch Limits** and verify edit/delete works

### Test 18 — Blocks Dashboard Status Cards

**Goal**: Verify Blocks dashboard status cards reflect live enable/disable state.

| Step | Action | Expected |
|------|--------|----------|
| 1 | Open Blocks tab | All status cards visible |
| 2 | Enable App Blocker + add a blocked app | App Blocker card shows active state |
| 3 | Disable App Blocker | App Blocker card shows inactive state |
| 4 | Enable Keyword Blocker + add a keyword | Keyword Blocker card shows active state |
| 5 | Enable Focus Mode and start a session | Focus Mode card shows active state |
| 6 | Tap Schedules card | Opens ManageBlockSchedulesFragment |
| 7 | Tap Launch Limits card | Opens ManageLaunchLimitsFragment |
| 8 | Tap FAB + | All-in-one setup menu appears with all 6 options |

### Test 18b — Schedule Group Actions

**Goal**: Verify group schedule create/edit/duplicate/delete is clear and safe.

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create a unified schedule with 3+ apps | Group rule appears in schedules list |
| 2 | Tap the group row edit button | Dialog title shows "Manage App Group (N rules)" |
| 3 | Tap "View group members" | Shows list of app names in the group |
| 4 | Tap "Duplicate group (N rules)" | Group is duplicated; total rule count increases |
| 5 | Tap "Delete entire group (N rules)" | Confirmation shows exact count before delete |
| 6 | Confirm delete | Toast shows "Deleted N schedule rules from batch" |
| 7 | Verify list updates and service refreshes | No stale state observed |

---

## Building the App

> **Important**: AmnShield uses **product flavors** (`playstore`, `fdroid`, `universal`). The plain
> `assembleDebug` task is no longer valid — always use a flavor-qualified task name.
> Using the bare task will silently serve a stale cached APK from before flavors were introduced.

### Debug Builds (for testing)

```powershell
# Build all three flavors at once
.\gradlew assemblePlaystoreDebug assembleFdroidDebug assembleUniversalDebug

# Or build individually
.\gradlew assemblePlaystoreDebug   # Google Play Store flavor
.\gradlew assembleFdroidDebug      # F-Droid / offline license flavor
.\gradlew assembleUniversalDebug   # Universal / sideload flavor
```

APKs are output to:
- `app/build/outputs/apk/playstore/debug/app-playstore-debug.apk`
- `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk`
- `app/build/outputs/apk/universal/debug/app-universal-debug.apk`

### Release Builds

```powershell
.\gradlew assemblePlaystoreRelease
.\gradlew assembleFdroidRelease
.\gradlew assembleUniversalRelease
```

### Android Studio

In the **Build Variants** panel (bottom-left), set the Active Build Variant to e.g.
`playstoreDebug` before hitting **Run**. If it shows just `debug`, you will get the
wrong (stale) output.

### If the Build Looks Stale / Shows Old UI

Nuke the Gradle build cache entirely and rebuild:

```powershell
.\gradlew clean --no-daemon
.\gradlew assemblePlaystoreDebug assembleFdroidDebug assembleUniversalDebug --no-daemon
```

The `--no-daemon` flag forces a fresh JVM instead of a cached Gradle daemon that may be
pinned to an older Gradle version.

---

## Prerequisites

### 1. Enable Features
1. Open AmnShield app
2. Go to **Smart Features** from drawer menu
3. Enable **Smart Blur** toggle
4. Click **Blur Sensitivity** → Select **Balanced** (recommended for testing)
5. Enable accessibility service if prompted

### 2. Check Status
- ✅ Smart Blur toggle is ON
- ✅ Accessibility service is enabled
- ✅ Sensitivity is set (Low/Balanced/Strict)
- ⚠️ Optional: Add `smart_blur_nsfw.tflite` model to assets (works without it)

---

## Test Scenarios

### Test 1: Instagram - Female Image Detection

**Objective**: Verify female image detection across all apps

**Steps**:
1. Open Instagram
2. View posts with images tagged "woman", "model", "actress", or "beauty"
3. Observe behavior

**Expected Results**:
- **Balanced sensitivity**: Images should blur or show overlay
- **Log output**: `AmnShield-AI: BLUR verdict for com.instagram.android: female_detected (confidence: 0.65+)`
- **Overlay**: Shows "Protected by Smart Blur" with "Continue Anyway" button
- **Stats**: View block recorded in Reports

**Debugging**:
```bash
# Check logs
adb logcat | grep "AmnShield-AI"

# Expected output:
# D/AmnShield-AI: Processing smart content for: com.instagram.android
# D/AmnShield-AI: BLUR verdict for com.instagram.android: female_detected (confidence: 0.75)
```

---

### Test 2: Chrome - Male Image Detection (Browser-Only)

**Objective**: Verify male images are detected ONLY in browsers

**Steps**:
1. Open Chrome
2. Search for "shirtless man" or "muscular man"
3. View image results
4. **Control test**: Open Instagram, view same male images

**Expected Results**:
- ✅ **Chrome** (browser): Images should blur/block
  - Log: `male_detected_browser (confidence: 0.70+)`
- ✅ **Instagram** (non-browser): Images should be ALLOWED
  - Log: `ALLOW` or no verdict

**Why Different?**:
- Male detection is browser-specific to prevent fitness/sports content from being blocked in social media
- Browsers have stricter thresholds (0.25-0.40 vs 0.40-0.70)

**Debugging**:
```bash
# Chrome should detect:
# D/AmnShield-AI: BLUR verdict for com.android.chrome: male_detected_browser (confidence: 0.70)

# Instagram should allow:
# (no BLUR/BLOCK log for male-only images)
```

---

### Test 3: YouTube - Video Thumbnail Detection

**Objective**: Verify video thumbnails are analyzed before playback

**Steps**:
1. Open YouTube
2. Browse videos with thumbnails containing:
   - Female images with "woman", "girl", "model" in title
   - NSFW keywords like "bikini", "revealing", "provocative"
3. Click on a flagged video

**Expected Results**:
- **Before play**: Thumbnail analyzed (1.5s interval checks)
- **If inappropriate**: Overlay shown or navigated back
- **Log output**: 
  ```
  D/AmnShield-AI: Processing smart content for: com.google.android.youtube
  D/AmnShield-AI: BLUR verdict: female_detected (confidence: 0.68)
  ```
- **Behavior**: Video prevents playback or shows warning

**Technical Details**:
- Video detection uses `findVideoNode()` to locate VideoView/PlayerView
- Checks every 1500ms while video is visible
- Extracts bitmap from video frame for analysis

---

### Test 4: NSFW Keyword Detection

**Objective**: Verify explicit keyword blocking

**Steps**:
1. Open Chrome or Instagram
2. Search/view content with keywords:
   - "nsfw", "bikini", "lingerie", "explicit"
   - "porn", "xxx", "adult content"
3. Observe blocking behavior

**Expected Results**:
- **High confidence** (0.80+): Immediate BLOCK action
  - Navigates back automatically
  - No "Continue Anyway" option
- **Medium confidence** (0.55-0.80): BLUR action
  - Shows overlay with option to continue
- **Low confidence** (<0.55): ALLOW
  - Content shown normally

**Keyword List** (30+ keywords detected):
```
nsfw, bikini, lingerie, swimsuit, nude, thigh, cleavage, explicit, haram,
breasts, exposed, revealing, provocative, sensual, erotic, sexual,
underwear, bra, panties, intimate, adult, xxx, porn
```

---

### Test 5: Sensitivity Levels Comparison

**Objective**: Understand different sensitivity behaviors

**Setup**: Same content (e.g., Chrome search for "woman model")

| Sensitivity | Block Threshold | Blur Threshold | Expected Behavior |
|-------------|-----------------|----------------|-------------------|
| **Low (0)** | 0.70 | 0.45 | Minimal blocking, only obvious content |
| **Balanced (1)** | 0.55 | 0.30 | Recommended, balanced protection |
| **Strict (2)** | 0.40 | 0.20 | Maximum protection, blocks more |

**Test Process**:
1. Set to **Low** → Search "woman" → Count blocks
2. Set to **Balanced** → Same search → Count blocks
3. Set to **Strict** → Same search → Count blocks

**Expected Difference**:
- Low: 20-30% of images blocked
- Balanced: 50-60% of images blocked
- Strict: 70-80% of images blocked

**Browser Bonus**: In Chrome, thresholds are even stricter (0.25-0.55)

---

### Test 6: Smart Exclusions

**Objective**: Verify whitelisted apps bypass detection

**Steps**:
1. Go to Smart Features → **Manage Allowed Apps**
2. Add Chrome to exclusions
3. Search for "woman" in Chrome
4. Observe: NO blocking should occur
5. Remove Chrome from exclusions
6. Same search: Blocking should resume

**Expected Results**:
- ✅ Excluded apps: AI completely bypassed, no logs
- ✅ Non-excluded: Normal detection continues

**Use Case**: Work apps, medical apps, educational content

---

### Test 7: Context-Aware Detection

**Objective**: Verify safe context keywords reduce false positives

**Steps**:
1. Search for "breast cancer awareness"
2. Search for "family feeding education"
3. Search for "medical health support"

**Expected Results**:
- **Safe context detected**: Score reduced by 0.15
- Images with medical/educational context: Less likely to block
- **Log**: Still processes but lower confidence scores

**Safe Context Keywords**:
```
education, medical, health, awareness, feeding,
cancer, support, family, charity, news
```

---

### Test 8: Model vs No-Model Behavior

**Objective**: Compare performance with and without TFLite model

#### Without Model (Default)
- ✅ Uses 30+ keyword heuristics
- ✅ Text context analysis
- ✅ Fast (5-10ms per image)
- ⚠️ Less accurate for subtle content
- ⚠️ More false positives possible

**Test**: Remove model from assets (if present)
1. Go to `app/src/main/assets/`
2. Rename `smart_blur_nsfw.tflite` → `smart_blur_nsfw.tflite.bak`
3. Rebuild app
4. Test same scenarios
5. Observe: Still works, relies on keywords

#### With Model (Enhanced)
- ✅ AI-powered image analysis
- ✅ 90%+ accuracy
- ✅ Better subtle content detection
- ⚠️ Slower (80-150ms per image)
- ⚠️ Requires ~4-6 MB model file

**Test**: Add model
1. Place `smart_blur_nsfw.tflite` in assets
2. Rebuild app
3. Test same scenarios
4. Compare: Better detection of borderline content

**Log Difference**:
```
# Without model:
# W/SmartImageModerator: Failed to load smart blur model

# With model:
# (no warning, model loads successfully)
```

---

### Test 9: Statistics & Reports

**Objective**: Verify blocking events are recorded

**Steps**:
1. Trigger 5-10 blocks/blurs
2. Go to **Reports** (drawer menu)
3. Check **View Blocker Report**

**Expected Results**:
- ✅ Each block/blur is logged with:
  - Timestamp
  - Package name
  - Reason (female_detected, male_detected_browser, high_confidence, etc.)
- ✅ Can export to CSV
- ✅ Daily statistics shown

**Example Report**:
```
View Blocker Report - November 15, 2025

Total Blocks: 15

Breakdown:
- Instagram: 7 blocks (female_detected)
- Chrome: 5 blocks (male_detected_browser)
- YouTube: 3 blocks (high_confidence)

Export available: CSV, TXT
```

---

### Test 10: Edge Cases & Error Handling

**Objective**: Ensure app doesn't crash on errors

**Test Scenarios**:

#### 10.1: Accessibility Service Disabled
1. Disable accessibility service in system settings
2. Open Instagram
3. **Expected**: No blocking, no crashes

#### 10.2: Smart Blur Disabled
1. Turn off Smart Blur toggle
2. View inappropriate content
3. **Expected**: Content allowed, no processing logs

#### 10.3: Low Memory
1. Open 10+ apps in background
2. Trigger image processing
3. **Expected**: Graceful degradation, possible skips

#### 10.4: Corrupted Bitmap
1. (Developer test) Pass null bitmap to evaluator
2. **Expected**: Fallback to text-only analysis

#### 10.5: Invalid Package Name
1. (Developer test) Pass null package name
2. **Expected**: Default thresholds used

---

## Core Blocker & Management Tests

### Test 11: App Blocker Configuration & Management

**Objective**: Verify app blocker setup and management consistency.

**Steps**:
1. Open **Blocks** tab and enter **App Blocker** config.
2. Add at least 2 apps to blocked apps list.
3. Open **Blocks → Schedules** and confirm blocked apps appear in candidate scope.
4. Remove one blocked app and verify status updates in App Blocker config.

**Expected Results**:
- Blocked app count is reflected consistently across App Blocker and Schedules manager.
- Removed app no longer appears as blocked target unless re-added.

### Test 12: Keyword Blocker Setup & Trigger Path

**Objective**: Verify keyword list + feature toggle work with service refresh.

**Steps**:
1. Add 2 custom keywords in Keyword Blocker config.
2. Ensure Keyword Blocker feature is enabled.
3. Open Chrome and enter one blocked keyword in search.
4. Disable Keyword Blocker and repeat.

**Expected Results**:
- Enabled state: keyword blocking action triggers.
- Disabled state: no keyword blocking action triggers.

### Test 13: Focus Mode Session & Enforcement

**Objective**: Verify focus mode can be started and ended reliably.

**Steps**:
1. Configure Focus Mode with a short duration and one selected app.
2. Start Focus Mode.
3. Open selected app during session.
4. Wait session expiry and retry.

**Expected Results**:
- During session: app is blocked according to mode.
- After expiry: blocking stops and state is cleared.

### Test 14: Cheat Hours Behavior

**Objective**: Verify cheat windows allow controlled exceptions configured via Schedules.

**Steps**:
1. Open the Schedules manager and create an App Schedule of type CHEAT for a blocked app.
2. Enter the app during the active cheat schedule window.
3. Verify access outside the active cheat schedule window (normal block behavior resumes).

**Expected Results**:
- During active cheat schedule: app is temporarily allowed.
- Outside cheat schedule: normal block behavior resumes.

### Test 15: Schedules Manager (Individual + Group)

**Objective**: Verify expanded management controls for both single and grouped schedules.

**Steps**:
1. Create one single app schedule.
2. Create one unified app group schedule (multi-app).
3. Create one unified feature schedule.
4. In **Manage Schedules**, run actions:
  - Change type
  - Change recurrence/time
  - Duplicate
  - Delete
5. For grouped schedules, use **View group members**.

**Expected Results**:
- All actions apply correctly.
- Group actions affect all members in that group.
- Confirmation messages show clear scope before delete.

### Test 16: Launch Limits End-to-End

**Objective**: Verify launch limit creation, editing, and deletion across entry points.

**Steps**:
1. Create launch limit from app usage breakdown.
2. Edit same rule in **Manage Launch Limits**.
3. Delete rule and verify it disappears from all screens.
4. Confirm service refresh occurs (no stale manager state).

**Expected Results**:
- Rule changes are visible immediately in management screens.
- Launch count display remains consistent with configured limit.

### Test 17: Error Reporting & Crash Recovery

**Objective**: Validate local-only diagnostics flow and user control.

**Steps**:
1. Open Settings → Error Reporting and verify toggles.
2. Trigger a controlled non-fatal error log path.
3. Verify crash/error data appears in local viewer.
4. Export report text via share intent.
5. Clear all reports and verify local data removal.

**Expected Results**:
- Logs are stored locally only.
- Export is user-initiated.
- Delete action removes saved diagnostics/feedback artifacts.

---

## Debugging Commands

### Enable Verbose Logging
```bash
# Filter for AmnShield AI logs
adb logcat -s AmnShield-AI

# Filter for all AmnShield logs
adb logcat | grep AmnShield

# Clear logs and start fresh
adb logcat -c && adb logcat -s AmnShield-AI
```

### Check Service Status
```bash
# Verify accessibility service is running
adb shell dumpsys accessibility | grep AmnShield

# Check if Smart Blur is enabled
adb shell "run-as com.alhaq.amnshield cat /data/data/com.alhaq.amnshield/shared_prefs/smart_features.xml" | grep blur_enabled
```

### Trigger Test Event
```kotlin
// In SmartFeaturesConfigFragment or test activity
val testBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
val result = smartFeaturesManager.evaluateSmartBlur(
    SmartImageMetadata(
        packageName = "com.test",
        textSnippets = listOf("woman", "model", "bikini"),
        bitmap = testBitmap
    )
)
Log.d("TEST", "Verdict: ${result.action}, Confidence: ${result.confidence}")
```

---

## Performance Metrics

### Expected Processing Times

| Operation | With Model | Without Model |
|-----------|-----------|---------------|
| Text analysis | 5ms | 5ms |
| Bitmap extraction | 30-50ms | 30-50ms |
| AI inference | 80-150ms | - |
| Total | 150-200ms | 40-60ms |

### Memory Usage
- **Without model**: +10-15 MB baseline
- **With model**: +20-30 MB baseline
- **Per image**: ~2-5 MB temporary (recycled)

### Battery Impact
- **Low sensitivity**: Minimal (processes fewer images)
- **Balanced**: ~2-3% per hour of active use
- **Strict**: ~5-7% per hour (more aggressive)

---

## Common Issues & Solutions

### Issue 1: No Blocking Occurs
**Symptoms**: Images shown normally, no logs

**Solutions**:
1. ✅ Check accessibility service is enabled
2. ✅ Verify Smart Blur toggle is ON
3. ✅ Confirm package is not in exclusions
4. ✅ Check sensitivity is not too low
5. ✅ Review logs for errors

### Issue 2: Too Many False Positives
**Symptoms**: Legitimate content blocked

**Solutions**:
1. Lower sensitivity: Strict → Balanced → Low
2. Add safe apps to exclusions (e.g., news apps)
3. Check for overly broad keywords
4. Review context detection logic

### Issue 3: Model Not Loading
**Symptoms**: Log shows "Failed to load smart blur model"

**Solutions**:
1. ✅ **This is OK!** App works without model using heuristics
2. Optional: Add `smart_blur_nsfw.tflite` to assets
3. Ensure file is valid TFLite format (~4-6 MB)
4. Rebuild and reinstall app

### Issue 4: Performance Lag
**Symptoms**: App feels slow, UI stutters

**Solutions**:
1. Increase throttle time (currently 1200ms)
2. Reduce image processing frequency
3. Resize bitmaps before processing (800x800 max)
4. Consider disabling model for faster processing

### Issue 5: Service Crashes
**Symptoms**: Accessibility service stops unexpectedly

**Solutions**:
1. Check logs for exceptions
2. Verify null safety in bitmap extraction
3. Ensure try-catch blocks around AI processing
4. Re-enable service in system settings

---

## Test Checklist

Before releasing, verify:

- [ ] Instagram: Female images detected and blurred
- [ ] Chrome: Male images detected (browser-only)
- [ ] YouTube: Video thumbnails analyzed
- [ ] NSFW keywords trigger blocks
- [ ] Sensitivity levels work as expected
- [ ] Smart exclusions bypass detection
- [ ] Safe context reduces false positives
- [ ] Works with and without ML model
- [ ] Statistics recorded correctly
- [ ] No crashes on edge cases
- [ ] Logs show expected output
- [ ] Performance is acceptable
- [ ] Battery usage is reasonable

---

## Success Criteria

✅ **Feature Complete**: All detection types working  
✅ **Stable**: No crashes in normal usage  
✅ **Performant**: <200ms per image processing  
✅ **Accurate**: 80%+ detection rate (balanced)  
✅ **Private**: 100% on-device processing  
✅ **User-Friendly**: Clear UI and controls  

---

## Next Steps After Testing

1. **Collect Metrics**: Track detection accuracy over 1 week
2. **User Feedback**: Gather reports on false positives/negatives
3. **Tune Thresholds**: Adjust based on real-world usage
4. **Add Model**: Download and integrate TFLite model for better accuracy
5. **Expand Keywords**: Add language-specific terms (Arabic, Urdu, etc.)
6. **Custom Training**: Train model on Islamic-specific content

---

## Support

**Questions?** Check:
- `AI_FEATURES.md` - Feature documentation
- `INTEGRATION_GUIDE.md` - Implementation details
- `README_MODELS.md` - ML model information

**Issues?** File bug report with:
- Steps to reproduce
- Expected vs actual behavior
- Logs from the AmnShield AI tag: `adb logcat -s AmnShield-AI`
- Device info (model, Android version)
- Sensitivity setting used
