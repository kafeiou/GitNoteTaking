# Changelog

All notable changes to this project will be documented in this file.

## [4.002] - 2026-08-28

### 🔒 Policy Compliance & Modern Architecture

#### 1. Google Play 相片和影片權限政策合規 (Photo & Video Permissions Policy)
- **全面移除廣泛媒體權限**：從 `AndroidManifest.xml` 與啟動流程中徹底移除 `READ_MEDIA_IMAGES`、`READ_MEDIA_VIDEO`、`READ_MEDIA_AUDIO` 與 `READ_MEDIA_VISUAL_USER_SELECTED` 權限宣告。
- **標準系統選擇器 (System File Picker)**：夾帶檔案與相片時直接呼叫 Android 原生 `Intent.ACTION_OPEN_DOCUMENT` 系統選擇器，零權限彈窗安全讀取並串流匯入附件，100% 符合 Google Play 最新發布規範。

#### 2. 無邊框 API 現代化升級 (Edge-to-Edge Modernization)
- **消除淘汰 API 警示**：全面升級至 Google 官方 Jetpack `androidx.activity.EdgeToEdge.enable()` 標準架構，移除過時的 `setDecorFitsSystemWindows(false)` 與實驗性無邊框 API。
- **跨版本無縫相容**：智慧適配 Android 13、14、15、16 狀態列、導航列與螢幕瀏海安全區（Window Insets），徹底解決 Google Play Console 之「應用程式使用已淘汰的無邊框 API 或參數」警示。

### 🎨 UI & UX Enhancements

#### 3. 自適應桌面圖示滿版升級 (Adaptive Launcher Icons)
- **消除白邊與滿版顯示**：建立標準 `mipmap-anydpi-v26/` 自適應圖示結構，底色填滿手機圖示外框。
- **16% 安全區內縮 (Safe Zone Inset)**：透過 Inset 16% 機制將前景筆記與「GIT」紅字完美容納於 72dp 安全視區內，徹底解決圖案被外框裁切或縮小問題。

#### 4. 選單圖示與視覺對稱性
- **建立選單 3 大圖示配置**：
  - 📱 建立本地筆記（`ic_local_note` 手機本機新增圖示）
  - 🔶 下載遠端筆記（`ic_git_logo` 官方 Git 菱形標誌）
  - 🐙 建立 GitHub 筆記（`ic_github` 官方 Octocat 標誌）
- **選單圖示強制顯示**：於 `onCreateOptionsMenu` 啟用子選單圖示顯示相容支援。
- **Git 官方授權宣告**：依 CC BY 3.0 規範於 5 國語系 `README.md` 中補齊 Git Logo 創作者（Jason Long）版權致謝。

#### 5. GitHub 連線指引與防重複機制
- **儲存庫防重複下載**：已下載的 GitHub 筆記於挑選清單中自動標記 `[已下載]` / `[Downloaded]` 並反灰禁用點選，避免重複下載覆蓋。
- **4 步驟清晰指引**：連線對話框加入「1. 瀏覽器登入 github」等 4 項步驟，並採用簡潔的 `--` 區隔線。

### 🐛 Bug Fixes

#### 6. 文字檔案 Commit History 記錄修復
- **新建檔案自動 Commit**：修復於檔案瀏覽器新建 `.txt` 檔案時未觸發 Git Commit 的問題。
- **編輯自動儲存非空 Commit 訊息**：修復未勾選手動輸入 Commit 訊息時送出空訊息而被 Log 清單略過的問題，預設自動帶入 `<檔名>`。

---

## [4.001] - 2026-08-27

### 🚀 New Features & Enhancements

#### 1. GitHub 筆記深度整合 (GitHub Note Integration)
- **主畫面快速建立**：在主選單「建立」新增「建立 GitHub 筆記」功能，支援 GitHub Octocat 圖示，並依使用習慣置於選單最底端。
- **一鍵預選 Token 權限**：提供【前往產生 Token】快捷按鈕，直接開啟 GitHub 官方頁面並自動勾選 `repo` 與 `read:user` 權限及預先填寫說明。
- **剪貼簿智慧偵測**：切回 App 時自動辨識剪貼簿中的 GitHub Token（`ghp_` / `github_pat_`）並自動填入輸入框。
- **智慧篩選與去重 (Deduplication)**：
  - 僅列出名稱開頭為 `note` (例如 `note-work`, `NoteTaking`) 的儲存庫。
  - 實作完整路徑去重機制，避免同名或協作儲存庫重複出現。
  - 乾淨呈現儲存庫資訊，無備註時自動隱藏，避免出現 `null` 字樣。
  - 查無 `note*` 儲存庫時提供友善指引與線上建立捷徑。
- **即時清單刷新**：Clone 完成後主畫面立即呈現新筆記，無須重啟 App。
- **移除廢棄介面**：徹底移除舊版 Clone 畫面中的 Click to Sign Up 與過時元件。

#### 2. Git Commit 真實時間同步 (GraphQL Commit Timestamp Sync)
- **極速淺層下載 (Depth = 1)**：本地 Clone 維持 `depth = 1` 淺層複製，確保下載極速且不浪費手機儲存空間。
- **GitHub GraphQL 批次查詢**：透過單次 GraphQL 請求在背景一次性抓取所有檔案在遠端的最後 Commit 時間（耗時僅約 0.3~0.5 秒）。
- **對話框完全保護**：所有檔案日期皆在「請稍候」對話框關閉前校正完成，點入任何筆記時日期皆 100% 正確無虞。

#### 3. 本地端歷史紀錄瘦身 (Purge to Depth = 1) (實驗中)
- **設定頁面瘦身功能**：於「設定（Settings）」最下方新增【歷史紀錄瘦身 (保留單一版本) (實驗中)】選項。
- **純本地離線執行**：透過 JGit 本地建立 Orphan Root Commit，將本地歷史截斷至單一版本（`depth = 1`）並執行垃圾回收（GC），在 0.05 秒內徹底釋放手機磁碟空間。
- **雙重防呆與請稍候機制**：點擊先彈出確認警示視窗，確認後顯示「請稍候...」進度視窗並於背景執行，兼顧安全與流暢體驗。

#### 4. 多語系國際化支援 (Multilingual Support)
- 所有新增之對話框、引導文字、按鈕、公開/私有標籤與結果提示，全面支援 5 種語言：
  - 繁體中文 (台灣 `zh-rTW`)
  - 繁體中文 (香港 `zh-rHK`)
  - 簡體中文 (`zh-rCN`)
  - 日文 (`ja`)
  - 英文 (`en`)

#### 5. 穩定性與生命週期防護 (Stability & Lifecycle Safeguards)
- 全面強化 `MainActivity` 與 `CustomPreferenceFragment` 的 `onDestroy()` 生命週期管理，徹底防止 Dialog `WindowLeaked` 異常。

---

### 📱 Google Play Store Release Notes (What's New)

#### 繁體中文 (台灣 - zh-TW)
```text
【v4.001 更新內容】
• 全新 GitHub 筆記整合：支援一鍵產生 Token 與剪貼簿自動帶入，快速挑選並建立您的 GitHub 筆記。
• 智慧 Commit 日期同步：下載筆記時自動校正每份檔案在遠端的最後更新時間。
• 本地歷史紀錄瘦身 (實驗中)：可在「設定」中一鍵清除本機冗長歷程，釋放手機儲存空間。
• 完整支援多國語言設定與即時切換。
• 效能優化與各項穩定度提升。
```

#### 繁體中文 (香港 - zh-HK)
```text
【v4.001 更新內容】
• 全新 GitHub 筆記整合：支援一鍵產生 Token 與剪貼簿自動填入，快速挑選並建立您的 GitHub 筆記。
• 智能 Commit 日期同步：下載筆記時自動校準每份檔案在遠端的最後更新時間。
• 本地歷史紀錄瘦身 (實驗中)：可在「設定」中一鍵清除本機冗長歷程，釋放手機儲存空間。
• 完整支援多國語言設定與即時切換。
• 效能優化與各項穩定度提升。
```

#### 簡體中文 (zh-CN)
```text
【v4.001 更新内容】
• 全新 GitHub 笔记整合：支持一键生成 Token 与剪贴板自动填入，快速选择并创建您的 GitHub 笔记。
• 智能 Commit 日期同步：克隆笔记时自动校准每个文件在远端的最后修改时间。
• 本地历史记录瘦身 (实验中)：可在“设置”中一键清理本机冗长历史，释放手机存储空间。
• 完整支持多语言设置与即时切换。
• 性能优化与各项稳定性提升。
```

#### 日本語 (ja-JP)
```text
【v4.001 アップデート内容】
• 新しい GitHub ノート連携：ワンタップでの Token 生成とクリップボード自動入力をサポートし、GitHub ノートを素早く作成できます。
• スマートな Commit 日時同期：ノート取得時に各ファイルの最終更新日時を正確に自動反映。
• ローカル履歴パージ (実験中)：「設定」から端末の不要な履歴を一括削除し、空き容量を節約できます。
• 多言語設定の即時切り替えに対応。
• パフォーマンス向上と安定性の改善。
```

#### English (en-US / Default)
```text
【What's New in v4.001】
• New GitHub Integration: Quickly create and import GitHub notes with one-tap Token generation and clipboard auto-fill.
• Smart Commit Timestamp Sync: Automatically syncs the accurate last commit dates for all note files.
• Local History Purge (Experimental): Clean up local Git history to free up phone storage in Settings.
• Full multilingual support with in-app language switching.
• Performance optimizations and stability improvements.
```

---
