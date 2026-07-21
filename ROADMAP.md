# AmnShield Roadmap

Last updated: 2026-07-18

## Strategic Architectural Decisions (July 2026)
- **Web Administration Portal Transition:**
  - **Decision:** Suspended development on the native Android **AmnShield Guardian** app in favor of a centralized **Web Administration Console** (React/Next.js + Supabase DB/Auth).
  - **Rationale:** A web portal provides platform-agnostic parental and administrative controls (accessible from iOS, Android, and Desktop browsers). It eliminates native Android IPC Binder dependencies and conforms to Google Play's strict security/policy guidelines regarding administrative control permissions.
  - **Client Impact:** The main `AmnShield-Android` app retains its local bound API (`IAmnShieldApi.aidl` and `AmnShieldApiService.kt`) for local extensibility, but will utilize HTTPS REST sync against Supabase for parental remote configuration instead of native inter-app IPC.

## Recently Completed
- **Blocker Rules Redesign & Always Block Mode (July 2026)**
  - Removed implicit 24/7 fallbacks from background services, ensuring Keyword Blocker, Website Blocker, and Reels Blocker only run if there is an explicit database rule configured.
  - Added mutually exclusive "Always Block (24/7)" and "Block Schedule" options to rule creator screens for App Blocker, Keyword Blocker, Website Blocker, and Reels Blocker.
  - Migrated Reels count limit configuration (Limit by Reels Scrolled switch and daily limit text field) from the general config screen to the rule creator screen (`CreateReelsBlockerRuleScreen.kt`), updating preferences and broadcasting refresh triggers upon save.
  - Removed count limit mode configuration options from the general Reels Blocker config screen layout (`fragment_reel_blocker_config.xml`) and controller (`FeatureConfigFragments.kt`).
- **Unified Feature Scheduling Purge & Decoupling (July 2026)**
  - Completely deleted the `UnifiedFeatureScheduleRule` database model and periodic switch-flipping scheduling logic.
  - Segregated Keyword Blocker, Website Blocker, and Reels Blocker to execute 24/7 based on global toggles and manual block lists/configurations, matching CureBox's clean, self-contained architecture.
  - Cleaned up Blocks Manager tab (`BlocksFragment`, `BlocksManagerFragment`) to load, list, and edit only App Blocker schedule and limit rules.
  - Deleted the secondary creator screens and cleaned up unused helper methods from `ScheduleUtils.kt`.
- **Blocker-Specific Rule Creators & Mode Gating Removal (July 2026)**
  - Completely removed Simple/Advanced mode selection from onboarding and switches in Settings and Profile. The app now runs uniformly with advanced features always available.
  - Tailored `CreateRuleScreen` to focus purely on App Blocker rules.
- **Simple vs Advanced Mode Enforcement & Usage Limits (July 2026)**
  - Implemented Simple vs Advanced Mode toggle, synchronized in both Settings and Profile screens.
  - In Simple Mode, the warning screen is a hard blocked overlay with no configuration and no proceed button. All rule configurations are hidden/simplified but enforced in the background.
  - Implemented "Usage Limit" rule type, allowing users to define daily foreground usage duration limits (screen time) in hours for selected apps.
  - Built foreground app usage duration checks inside `AppBlocker` by querying `UsageStatsManager` daily stats.
  - Redesigned individual App Blocker Configuration fragment to support selecting blocking modes (Block Schedule, Launch Limit, Usage Limit, Cheat Window) by navigating directly to rule creator prefilled with correct parameters.
- **Focus Mode & Shortform Content Blocker (July 2026)**
  - Re-implemented Focus Mode start experience using a modern Compose-based `StartFocusSessionDialog` featuring custom duration sliders, whitelisting/blacklisting option cards, and background-loaded app list pickers.
  - Implemented Reels/Shorts shortform content tracking in YouTube, Instagram, Facebook, and TikTok within `AmnShieldAccessibilityService` (counting scrolls and watch time, blocking once limits are hit).
  - Added Reels Scrolled and Reels Watch Time metric blocks to the `StatsScreen.kt` dashboard.
- **High-Fidelity Visual Redesign (July 2026)**
  - Ported the premium modern design system and layouts from `AmnGuard` into `AmnShield-Android`.
  - Redesigned `StatsScreen.kt` with a detailed screen time hero card, circular goal meter, Canvas-drawn weekly bar chart, and modern app breakdown list.
  - Redesigned `FocusScreen.kt` with a Mindfulness & Wellbeing Insights hub, including a pulsing mindful breathing visualizer and interactive focus/distraction simulation tools.
  - Ported and migrated Settings and Profile screens to Compose (`SettingsScreen.kt`, `ProfileScreen.kt`), wiring them into `SettingsFragment` and `ProfileFragment` with local data persistence.
  - Implemented optional Google Account linking on the Profile screen, wired to GMS sign-in/out launcher flow on universal & playstore variants.
  - Ported and migrated Schedules & Rules management and creation screens to Compose (`ManageSchedulesScreen.kt`, `CreateRuleScreen.kt`), replacing `ManageBlockSchedulesFragment` with a clean, unified Compose implementation supporting multi-feature selection, multiple time windows, automatic consolidation, and conflict auto-correction.
  - Updated `Theme.kt` to globally synchronize Android status bar and navigation bar colors to match active themes (Sunset Glow, Emerald Calm, Cosmic Night).
- **Social Media Blocker Integration & Consolidation (July 2026)**
  - Re-implemented the dedicated Social Media Blocker screen matching design requirements (enabling global switch, managing blocked apps, and adding custom blocked websites/domains).
  - Consolidated gaming blocker into general App Blocker to clean up duplicate features.
  - Enforced blocking rules in `AmnShieldAccessibilityService` to intercept blocked social media apps and browser domains.
  - Linked companion app via bound service API to support `SET_SOCIAL_MEDIA_BLOCKER` command for Guardian-based rule enforcement.
- **Scheduling System Consolidation (July 2026)**
  - Consolidated legacy timed scheduling systems (Cheat Hours, Auto-Focus, and Reel Blocker slider ranges) into the unified schedules framework.
  - Retired the redundant `TimedActionActivity`, `AutoTimedActionItem` data model, and custom slider dialogs.
  - Redirected all timed settings buttons to the unified `ManageBlockSchedulesFragment` for a clean, consistent UX.
  - Implemented automatic data migration in `SavedPreferencesLoader` to transition existing users' cheat and auto-focus hours seamlessly.
  - Resolved the bug where auto-focus schedules set in the UI were ignored by the background Accessibility Service.
- **Release Distribution Strategy & CI Workflow (July 2026)**
  - Clarified that `universal` and `fdroid` are the two GitHub Releases APKs; `playstore` goes to Play Console only.
  - Created `release.yml` GitHub Actions workflow: triggers on version tags (`v*.*.*`), signs APKs using keystore stored as GitHub Secret, and auto-publishes `universal` + `fdroid` release APKs to GitHub Releases.
  - Pre-release tags (`-alpha`, `-beta`, `-rc`) auto-flagged as pre-release on GitHub.
  - Created `README.md` with product flavor guide, correct build commands, Android Studio setup, stale build recovery, and release strategy table.
  - Added **"Building the App"** section to `TESTING_GUIDE.md` documenting flavor-qualified Gradle tasks and stale build fix.
- **Monetization & Licensing Track (July 2026)**
  - Configured custom build-flavor partitioning via `IS_PLAYSTORE` BuildConfig flags.
  - Implemented offline-first ECDSA cryptographic license validator on client builds.
  - Built Deno/TypeScript Supabase Edge Function to parse and verification sign Lemon Squeezy orders.
  - Upgraded Android client to use a versioned keyring map supporting key rotation.
  - Created automated regression test suites verifying signature, expiration, and payload-tamper checks.
- Replaced bottom navigation Settings tab with Blocks dashboard tab
- Added unified Blocks dashboard with live status cards:
  - App Blocker
  - Keyword Blocker
  - Focus Mode
  - Cheat Hours
  - Schedules
  - Launch Limits
- Added floating + setup hub on Blocks dashboard for all-in-one block setup
- Removed About from left drawer to avoid duplication; About remains in overflow menu
- Added launch-limit management flows across:
  - App usage breakdown
  - App Blocker config
  - Dedicated Manage Launch Limits screen
  - Unified schedule/limits management experience
- Added per-app launch tracking and automatic blocking when limit is reached

## Stabilization Cycle 2026-05-29 (Blocks-First UX + Docs)
### App UX Track (1A–1E) — Completed
- 1A: ManageBlockSchedulesFragment — contextual dialog titles, rule counts in group confirmations, clearer destructive action labels
- 1B: BlockScheduleAdapter — improved group badge fallback and empty-days weekly flag
- 1C: ManageLaunchLimitsFragment — error messages improved with logging
- 1D: HomeFragment + StatsFragment — TODO tagging for debt items, fixed misleading refresh text
- 1E: Debug build validated (BUILD SUCCESSFUL)
### Docs Track (2A–2C) — Completed
- 2A: copilot-instructions.md — fixed nav model (4-tab), added unified schedule broadcast, updated date
- 2A: amnshield-mobile.instructions.md — added last-verified date and plan cross-link
- 2B: TESTING_GUIDE.md — fixed nav tab name, added Test 18 / 18b for Blocks Dashboard and Schedule Group actions
- 2C: ROADMAP.md updated (this entry)

## Current Stabilization Goals
- [x] Blocks dashboard UX clarity improvements
- [x] Docs baseline sync (nav model, broadcasts, test scenarios)
- [x] Hybrid monetization architecture implemented (Play Billing + Serverless ECDSA Licensing)
- [ ] Expand end-to-end tests for schedules/groups and launch-limit flows on device
- [ ] Validate migration behavior for users upgrading from older nav layouts

## Next Planned Improvements
- Add richer active-block diagnostics on Blocks dashboard (last trigger time, source)
- Add optional quick actions per card (edit/delete shortcuts)
- Add clearer premium-gated affordances on blocked actions
- Improve schedule and launch-limit conflict resolution hints
- Add optional undo snackbar for destructive schedule actions (single/group)
- Move keyword-pack prefs reading into SavedPreferencesLoader (flagged TODO)
- Surface actionable enable prompt when accessibility service is disabled (flagged TODO)

## Release Readiness Checklist
- [x] Debug build passes
- [ ] Release APK build passes
- [ ] Release AAB build passes
- [ ] Manual sanity checks complete (navigation + blocks/schedules/groups flows)
- [ ] Regression checks complete for existing feature configs

## Future Roadmap: Global Cloud Sync Architecture (Supabase)

A multi-phase implementation roadmap to establish cross-platform synchronization of rules, schedules, and active focus sessions across Android, Windows, and Web Extension clients using Supabase as the central authority:

### Phase 1: Database Schema & RLS Setup
- Provision tables in Supabase: `profiles`, `devices`, and `sync_rules`.
- Implement Row-Level Security (RLS) policies to isolate user device rules.
- Set up signup triggers linking parental accounts.

### Phase 2: Android App Integration
- Migrate legacy `src/sync` worker code to the active namespace (`com.alhaq.amnshield.data.sync`).
- Bind Google Account authentication in Compose settings to Supabase Auth.
- Implement background `SyncWorker` scheduling via `WorkManager`.
- Configure silent Firebase Cloud Messaging (FCM) wakeup receivers to trigger real-time updates.
- Fire local IPC broadcasts to keep the companion FireWall app in sync offline.

### Phase 3: Windows Client Integration
- Implement Supabase API REST polling in the Flutter client.
- Securely write synced settings updates to `C:\ProgramData\AmnShield\config.json`.
- Let the background C# Guardian Service watch and apply these rules locally.

### Phase 4: Browser Web Extension Integration
- Wire periodic fetch queries inside the background worker script.
- Synchronize configurations into `chrome.storage.local`.
- Let content scripts intercept page loads matching the blocked domains.

### Phase 5: Web Administration Console
- Build the Next.js parent administration dashboard.
- Display registered devices and their current status.
- Allow parents to edit blocking rules, schedules, and start focus sessions remotely.
