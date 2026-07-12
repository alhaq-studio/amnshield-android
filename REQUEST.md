## Constraints (Do NOT violate)

- Do NOT change any public API contracts — the AIDL interfaces in `app/src/main/aidl/` and the API behavior documented in `CURBOX_API.md` must remain backward-compatible.
- Do NOT modify the build flavor structure — the `full`, `playstore`, and `fdroid` flavors, their source sets, and their BuildConfig flags must stay as-is.
- Do NOT add new third-party library dependencies — fix issues using existing dependencies and standard Android/Kotlin APIs only.
- Do NOT copy code from external open-source projects for core logic fixes.
- Preserve all existing comments and docstrings unrelated to changed code.
- All user-facing text must go in `res/values/strings.xml` — never hardcode English strings.