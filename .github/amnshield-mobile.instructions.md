---
applyTo: "**"
---

# AmnShield Mobile Project Instructions

When working in this repository, treat requests as updates to the existing AmnShield Android app unless the user explicitly asks for a rewrite or new architecture.

- Prefer small, targeted fixes and updates over broad refactors.
- Preserve the existing Android/Kotlin structure, including the single-activity, multi-fragment setup and View Binding patterns.
- Keep the privacy-first design intact: on-device processing only, minimal network use, and no unnecessary permissions.
- Never add new direct SharedPreferences access; use `SavedPreferencesLoader` for persistence.
- Keep the single accessibility service model intact and always skip AmnShield's own package in blocking or detection logic.
- Gate premium functionality with `PremiumManager.isPremium()` instead of reimplementing subscription checks.
- Match existing Material 3 UI patterns and reuse established app conventions before introducing new ones.
- Fix root causes rather than applying surface-level patches when the code is already failing.
- Avoid unrelated cleanups or behavior changes unless they are needed to complete the requested fix.

If a request is ambiguous, assume the user wants the app improved, fixed, or updated in place rather than rebuilt.

## Reference Links

- For full architecture and feature context, use `.github/copilot-instructions.md`.
- For validation and manual QA scenarios, use `TESTING_GUIDE.md`.
- For release sequencing and priority context, use `ROADMAP.md`.
- For the current stabilization cycle plan, use `plan-blocksFirstUxCleanupFullDocsRefresh.prompt.md`.

_Last verified: 2026-05-29_