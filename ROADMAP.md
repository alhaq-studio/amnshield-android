# AmnShield Roadmap

Last updated: 2026-07-13

## Recently Completed
- **Scheduling System Consolidation (July 2026)**
  - Consolidated legacy timed scheduling systems (Cheat Hours, Auto-Focus, and Reel Blocker slider ranges) into the unified schedules framework.
  - Retired the redundant `TimedActionActivity`, `AutoTimedActionItem` data model, and custom slider dialogs.
  - Redirected all timed settings buttons to the unified `ManageBlockSchedulesFragment` for a clean, consistent UX.
  - Implemented automatic data migration in `SavedPreferencesLoader` to transition existing users' cheat and auto-focus hours seamlessly.
  - Resolved the bug where auto-focus schedules set in the UI were ignored by the background Accessibility Service.
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

