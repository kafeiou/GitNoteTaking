# Git Note Taking

[繁體中文](README.md) | [English](README_en.md) | [日本語](README_ja.md) | [简体中文](README_zh-CN.md)

> **For software engineers only. Muggles stay away.**

## 💡 Features
1. **Git Version Control**: Full modification history and commit logs preserved.
2. **Cloud GitHub Sync**: Free cloud backup and multi-device synchronization.
3. **Markdown Preview & Offline Mermaid Diagrams**: Obsidian Wikilinks (`[[...]]`), tables, task lists, and offline SVG Mermaid charts.
4. **100% Offline Support**: Read and edit notes seamlessly even without an internet connection.
5. **Full-text Search**: Fast search across note titles and file contents.
6. **Local History Purge**: Easily slim down local repositories to depth = 1 to save device storage.

## 🏛️ Architecture & Testing
- **[System Architecture & Mermaid Flowcharts (Architecture)](ARCHITECTURE.md)**: Detailed layered architecture, dual-mode lifecycle, and diagram rendering data flow.
- **[Automated Testing Guide (Testing Guide)](TESTING.md)**: Guide for running unit and instrumentation tests.

## 🎯 Design Philosophy
Sync your daily markdown notes and documentation via GitHub. Read or edit offline anytime, and push back to GitHub when convenient.

**Git Advantage**: Every edit can have an explanatory Commit Message for effortless retrospective review.

## 📖 How to Use

### Method 1: Create GitHub Note (Recommended)
1. Open the App, tap the menu at the top-right and select **"Create GitHub Note"** at the bottom.
2. Tap **[Generate Token]** to automatically open GitHub with all required scopes pre-checked (`repo` and `read:user`).
3. Set "Expiration" to **No expiration**, scroll to the bottom, tap **Generate token**, and copy it.
4. Switch back to the App; the token is **automatically pasted from your clipboard**. Tap [Connect].
5. The App will automatically list all repositories starting with `note` (e.g. `note-work`, `NoteTaking`). Select one to clone and start taking notes!

### Method 2: Custom Remote Git / Local Notes
1. **Remote Git**: Tap "Clone Remote Note" in the menu, input the Git URL, username, and token.
2. **Local Note**: Tap "Create Local Note" to create a pure local offline repository.

## 📦 Google Play Release & Distribution Mechanism

- **`distribution/whatsnew/`**: Contains the **current release notes** for Google Play Store across supported locales (strictly under 500 characters per Google Play policy):
  - `whatsnew-zh-TW` (Traditional Chinese)
  - `whatsnew-zh-CN` (Simplified Chinese)
  - `whatsnew-ja-JP` (Japanese)
  - `whatsnew-en-US` (English / Default)
- **`CHANGELOG.md`**: Tracks the full release history of the product.

### 📌 Release Prompt Template
To publish a new version, give the following prompt to AI:
```text
Please help me release a new version [version number, e.g. 4.002]:
1. Update versionName and versionCode in app/build.gradle.
2. Prepend the full release notes for the new version in CHANGELOG.md.
3. Overwrite the 4 language release notes in distribution/whatsnew/ (< 500 chars each).
4. Run ./gradlew assembleDebug to build and verify.
```

## 🌐 Open Source
- GitHub Repository: https://github.com/WilliamFromTW/GitNoteTaking

## 📚 3rd-Party Libraries & Asset Licensing
- [Eclipse JGit](https://www.eclipse.org/jgit) (version 7.7.1)
- Git Logo by Jason Long is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
- System Requirement: Android 13 (API 33) or above

## 🤖 Development Tools
- Developed with the assistance of **Gemini CLI v1.1.22** and **OpenSpec v1.11.0**.
