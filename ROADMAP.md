# AmnShield Roadmap

Last updated: 2026-05-15

## Recently Completed
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

## Current Stabilization Goals
- Expand end-to-end tests for Blocks dashboard and launch-limit flows
- Improve UX copy consistency across management screens
- Validate migration behavior for users upgrading from older nav layouts

## Next Planned Improvements
- Add richer active-block diagnostics on Blocks dashboard (last trigger time, source)
- Add optional quick actions per card (edit/delete shortcuts)
- Add clearer premium-gated affordances on blocked actions
- Improve schedule and launch-limit conflict resolution hints

## Release Readiness Checklist
- [ ] Debug build passes
- [ ] Release APK build passes
- [ ] Release AAB build passes
- [ ] Manual sanity checks complete (navigation + blocks flows)
- [ ] Regression checks complete for existing feature configs
