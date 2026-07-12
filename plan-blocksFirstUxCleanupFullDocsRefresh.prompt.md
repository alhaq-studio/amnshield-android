## Plan: Blocks-First UX Cleanup + Full Docs Refresh

Deliver in two tracks: first a low-risk UX cleanup centered on Blocks/Schedules manager plus targeted consistency fixes for Home/Stats/Reports, then a full documentation refresh to align guides, testing coverage, roadmap, and privacy statements with shipped behavior.

**Steps**
1. Phase 1A (Blocks/Schedules Manager Controls Hardening): Refine action flows for individual/group schedules to improve clarity and reduce accidental destructive actions. Add explicit action labels, confirmation text improvements, and guardrails for group operations in ManageBlockSchedulesFragment. Keep data model stable and avoid architecture rewrites.
2. Phase 1B (Blocks/Schedules Visibility Improvements): Improve list readability and management affordances in schedule rows (group badges, recurrence readability, empty-state guidance, and post-action feedback consistency). Ensure grouped and individual rules are visually distinct and easier to scan.
3. Phase 1C (Cross-screen Consistency Fixes, low risk): Standardize refresh behavior and messaging from Blocks/Schedules and Launch Limits screens (shared helper path, consistent toasts, consistent back navigation behavior). Keep implementation minimal and avoid large framework changes.
4. Phase 1D (Targeted Hub Polishing, parallel with 3): Apply small UX cleanups in Home/Stats/Reports that do not require architecture changes: empty states, wording consistency, and safer error feedback where generic catch blocks currently mask context.
5. Phase 1E (Validation Gate for App UX Track): Run assemble debug, then perform manual smoke checklist for create/edit/duplicate/delete for both individual and grouped schedules, and verify service refresh behavior.
6. Phase 2A (Docs Baseline Sync): Update high-signal project instructions and architecture references first so implementation contributors have aligned guidance before deeper docs edits.
7. Phase 2B (Testing Guide Expansion): Add missing end-to-end test scenarios for app blocker, keyword blocker, focus mode, cheat hours, schedules/groups, launch-limit flows, and error-reporting workflow.
8. Phase 2C (Roadmap + Policy + Implementation Docs): Update roadmap status/checklists, privacy policy sections affected by current reporting/diagnostics behavior, and cross-link implementation guides to testing sections.
9. Phase 2D (Docs QA Pass): Verify internal consistency (navigation model, Blocks tab, grouped schedule behavior, premium gates) across all updated docs and remove drift.

**Relevant files**
- d:/Projects/AmnShield-Mobile-app/app/src/main/java/com/alhaq/deenshield/ui/fragments/ManageBlockSchedulesFragment.kt — primary schedule manager controls, grouped/individual operations, action menu and feedback text.
- d:/Projects/AmnShield-Mobile-app/app/src/main/java/com/alhaq/deenshield/ui/adapters/BlockScheduleAdapter.kt — schedule row readability, recurrence/group labels, visual scanning quality.
- d:/Projects/AmnShield-Mobile-app/app/src/main/res/layout/block_schedule_item.xml — row-level visual affordances and grouped schedule presentation.
- d:/Projects/AmnShield-Mobile-app/app/src/main/res/layout/fragment_manage_block_schedules.xml — manager-level UX (section labels, empty-state messaging, control placement).
- d:/Projects/AmnShield-Mobile-app/app/src/main/java/com/alhaq/deenshield/ui/fragments/ManageLaunchLimitsFragment.kt — consistency with schedules manager refresh and user feedback.
- d:/Projects/AmnShield-Mobile-app/app/src/main/java/com/alhaq/deenshield/ui/fragments/HomeFragment.kt — lightweight empty-state and wording polish.
- d:/Projects/AmnShield-Mobile-app/app/src/main/java/com/alhaq/deenshield/ui/fragments/StatsFragment.kt — low-risk clarity and empty-state refinements.
- d:/Projects/AmnShield-Mobile-app/app/src/main/java/com/alhaq/deenshield/ui/activity/ReportsActivity.kt — safer user-facing error messaging/logging polish.
- d:/Projects/AmnShield-Mobile-app/TESTING_GUIDE.md — add missing blocker/schedule/error-reporting test matrix and execution steps.
- d:/Projects/AmnShield-Mobile-app/ROADMAP.md — update completion state and near-term priorities.
- d:/Projects/AmnShield-Mobile-app/PRIVACY_POLICY.md — align policy wording with implemented reporting/data handling.
- d:/Projects/AmnShield-Mobile-app/.github/copilot-instructions.md — architecture/status reference alignment.
- d:/Projects/AmnShield-Mobile-app/.github/amnshield-mobile.instructions.md — concise contributor instruction sync and cross-links.
- d:/Projects/AmnShield-Mobile-app/ERROR_REPORTING_IMPLEMENTATION_GUIDE.md — cross-reference testing procedures and current behavior.

**Verification**
1. Build validation: run ./gradlew.bat :app:assembleDebug --no-daemon after each phase chunk (1A-1D and 2A-2D complete).
2. Manual scheduler UX checks: create/edit/duplicate/delete for single app schedules, grouped app schedules, single feature schedules, grouped feature schedules.
3. Manual consistency checks: verify toast/error messages and empty states are coherent across Blocks/Schedules, Launch Limits, Home, Stats, and Reports.
4. Service refresh checks: confirm schedule edits/deletes apply without stale state when returning between screens.
5. Docs QA checklist: ensure navigation model, schedule grouping behavior, premium gating, and testing steps are consistent across all edited docs.

**Decisions**
- Track order: App UX cleanup first, docs second.
- Hub focus: Blocks/Schedules manager is first priority.
- Risk profile: safe incremental changes only in this cycle.
- Docs scope: full refresh, including testing/roadmap/privacy/implementation references.

**Further Considerations**
1. Scheduler safeguards recommendation: add optional undo snackbar for delete actions (single/group) in a follow-up if current cycle should remain minimal-risk.
2. Documentation governance recommendation: add a short “Last verified on” line in each major doc to reduce future drift.
3. Post-cycle recommendation: after this safe pass, evaluate one medium refactor to split ManageBlockSchedulesFragment into coordinator + dialogs for maintainability.