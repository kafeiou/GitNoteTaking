# GIT文字筆記 Git Note Taking

[繁體中文 (台灣)](README.md) | [English](README_en.md) | [日本語](README_ja.md) | [简体中文](README_zh-CN.md) | [繁體中文 (香港)](README_zh-HK.md)

> **軟體工程師專用、麻瓜勿擾**

## 💡 特色
1. **使用 Git 版本控制機制**：完整保留修改紀錄與 Commit 訊息。
2. **支援免費雲端 GitHub 同步**：隨時隨地備份與多裝置同步。
3. **完全可離線使用**：沒有網路時依然能流暢查閱與編輯筆記。
4. **全文搜尋**：快速尋找歷史筆記與檔案內容。
5. **本地歷史瘦身**：一鍵將本地版本庫清理至單一版本（`depth = 1`），節省手機空間。

## 🎯 設計理念
透過雲端 GitHub 服務，可將平時紀錄的文件同步到 APP；可離線觀看或編輯，適當時機再將檔案 Push 同步回雲端 GitHub。

**Git 特有優勢**：每次編輯時可以寫下編輯原因（Commit Message），方便事後查閱與追溯歷史。

## 📖 如何使用本 APP

### 方式一：建立 GitHub 筆記（推薦）
1. 開啟 APP，在右上角點擊「建立」選單最下方的 **「建立 GitHub 筆記」**。
2. 點擊 **【前往產生 Token】**，系統將自動開啟 GitHub 網頁並為您預先勾選好需要的權限（`repo` 與 `read:user`）。
3. 將「Expiration」選為 **No expiration（永不過期）**，滑至最下方點擊 **Generate token** 並複製。
4. 切回 APP，系統將**自動為您帶入剪貼簿中的 Token**，點擊【確定連線】。
5. 系統會自動列出您帳號中所有以 `note` 開頭的儲存庫（如 `note-work`, `NoteTaking`），點選即可極速下載並開始使用！

### 方式二：自訂遠端 Git / 本地筆記
1. **遠端 Git**：在右上角選單點選「下載遠端筆記」，輸入 Git URL、帳號與 Token 即可手動 Clone。
2. **本地筆記**：在右上角選單點選「建立本地筆記」，即可建立純本地離線 Git 筆記庫。

## 📦 Google Play 商店發布與版本更新機制 (Release & Distribution)

- **`distribution/whatsnew/`**：存放**當次發布**至 Google Play 商店的多語系發布日誌（What's New / Release Notes）。每次發布新版本時覆蓋此目錄下的檔案（字數嚴格限制在 500 字元內以符合 Google Play 規範）：
  - `whatsnew-zh-TW`（繁體中文 台灣）
  - `whatsnew-zh-HK`（繁體中文 香港）
  - `whatsnew-zh-CN`（簡體中文）
  - `whatsnew-ja-JP`（日本語）
  - `whatsnew-en-US`（English / 預設）
- **`CHANGELOG.md`**：保留產品完整的歷史版本演進歷程。

### 📌 新版本發布 Prompt 範本 (Release Prompt Template)
未來發布新版本時，直接複製以下 Prompt 給 AI 即可：
```text
請幫我發布新版本 [版本號，例如 4.002]：
1. 更新 app/build.gradle 的 versionName 與 versionCode。
2. 在 CHANGELOG.md 最上方追加新版本的完整更新紀錄。
3. 覆蓋更新 distribution/whatsnew/ 目錄下的 5 國語系 Play 商店發布日誌（每篇字數小於 500 字元）。
4. 執行 ./gradlew assembleDebug 進行建置與驗證。
```

## 🌐 開源資訊 Open Source
- GitHub 專案：https://github.com/WilliamFromTW/GitNoteTaking

## 📚 第三方函式庫與資源授權
- [Eclipse JGit](https://www.eclipse.org/jgit) (version 7.4.0)
- Git Logo by Jason Long is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
- 系統需求：只支援 Android 13 (API 33) 或以上版本

## 🤖 開發工具致謝
- 本專案使用 **Gemini CLI 1.1.22 版** 與 **OpenSpec 1.11.0** 協助開發。