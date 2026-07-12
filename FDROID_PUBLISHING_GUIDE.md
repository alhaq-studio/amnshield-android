# F-Droid Publishing Guide - AmnShield

F-Droid is an independent repository of Free and Open Source Software (FOSS) for Android. Since we have successfully decoupled GMS and created the `fdroid` build variant, AmnShield is fully ready to be published on F-Droid!

This guide outlines the prerequisites, provides the F-Droid build recipe/metadata, and details the submission workflow.

---

## 📋 Prerequisites

Before submitting to F-Droid, you must complete the following:

1. **Add a License File:** 
   F-Droid requires a clear open-source license. Create a file named `LICENSE` in the root of your project directory containing the full text of your chosen license (e.g., GPL-3.0, Apache 2.0, or MIT).
2. **Release Tag:**
   F-Droid build servers compile directly from Git tags. Create and push a git tag for your release (e.g., `v0.14.6.2026`):
   ```bash
   git tag -a v0.14.6.2026 -m "Release v0.14.6.2026"
   git push origin v0.14.6.2026
   ```

---

## 🛠️ F-Droid Metadata Recipe (`com.alhaq.deenshield.yml`)

Save the following YAML snippet as `com.alhaq.deenshield.yml` in your project root or submit it to the F-Droid metadata repository.

```yaml
Categories:
  - Security
  - System
License: GPL-3.0-only  # Update to match your LICENSE file
AuthorName: Afrasyaab
SourceCode: https://github.com/Afrasyaab-GH/AmnShield-Android
IssueTracker: https://github.com/Afrasyaab-GH/AmnShield-Android/issues

Summary: Privacy-focused app blocker, keyword filter, and focus mode utility.
Description: |
    AmnShield is an open-source app blocker, keyword filter, and focus mode
    utility designed to help you stay focused and protect your digital wellness.
    
    All processing is performed entirely offline on your device for maximum privacy.

RepoType: git
Repo: https://github.com/Afrasyaab-GH/AmnShield-Android

Builds:
  - versionName: 0.14.6.2026
    versionCode: 125
    commit: v0.14.6.2026
    subdir: app
    gradle:
      - yes
    # Execute the FOSS-only F-Droid task to exclude GMS/Billing dependencies
    gradleTasks:
      - :app:assembleFdroidRelease

AutoName: AmnShield
```

---

## 🚀 Step-by-Step Submission Workflow

F-Droid is managed via the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository on GitLab.

### Step 1: Fork and Clone the F-Droid Data Repo
1. Go to [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata) and click **Fork**.
2. Clone your fork locally:
   ```bash
   git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
   cd fdroiddata
   ```

### Step 2: Add Your App's Metadata File
1. Create a new file named `com.alhaq.deenshield.yml` inside the `templates/` or `metadata/` directory of the `fdroiddata` repo.
2. Paste the YAML configuration provided above into it.

### Step 3: Test the Build Locally (Optional but Recommended)
To verify that F-Droid's build server won't run into errors, you can test it using the `fdroid build` tool:
1. Install the `fdroidserver` tools.
2. Run the build test:
   ```bash
   fdroid readmeta
   fdroid build --only-branch com.alhaq.deenshield
   ```

### Step 4: Submit a Merge Request
1. Commit and push the new metadata file to your GitLab fork:
   ```bash
   git checkout -b add-amnshield
   git add metadata/com.alhaq.deenshield.yml
   git commit -m "Add AmnShield (com.alhaq.deenshield)"
   git push origin add-amnshield
   ```
2. Open a **Merge Request** on the upstream GitLab repo ([fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata)).
3. F-Droid maintainers will review the submission, run automated checks, and merge your request. Once merged, F-Droid build servers will automatically compile the app and publish it to the official F-Droid store!
