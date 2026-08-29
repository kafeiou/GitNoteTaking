# Changelog

All notable changes to this project will be documented in this file.

## [4.008] - 2026-08-29

### 🚀 New Features & Enhancements

#### 1. 長按選單新增「排除暫存檔(.gitignore)」(Exclude Temp Files & .gitignore)
- **主動掃描與歷史暫存檔剔除**：自動從 Git 追蹤索引中執行 `git rm` 剔除已被誤傳的暫存檔案，並實體清理磁碟上的未追蹤暫存檔案。
- **自動補齊與同步 .gitignore**：自動檢查並將常見暫存規則寫入本機 `.gitignore`，完成 Commit 並推送到遠端 GitHub，防止未來再次追蹤。
- **支援過濾之暫存檔特徵清單**：
  - `~*` / `~$*`：Microsoft Office（Word、Excel、PowerPoint）的鎖定與臨時編輯暫存檔（例如 `~$筆記.docx`、`~temp.tmp`）。
  - `*~`：Linux/Unix 與各類文字編輯器自動備份檔案（例如 `note.md~`）。
  - `*.tmp` / `*.temp`：系統與應用程式產生的通用暫存檔。
  - `*.swp` / `*.swo`：Vim/Vi 編輯器的交換檔案（Swap files）。
  - `.DS_Store`：macOS Finder 目錄結構與自訂屬性快取檔。
  - `Thumbs.db` / `desktop.ini`：Windows 縮圖快取與資料夾配置檔。
- **完整 4 國語系支援**：繁體中文（`排除暫存檔(.gitignore)`）、簡體中文（`排除暂存文件(.gitignore)`）、日本語（`一時ファイル除外(.gitignore)`）與 English（`Exclude Temp Files (.gitignore)`）。

#### 2. Mermaid 流程圖全語系完美 Fit & 深色模式高對比
- **萬國碼全語系字寬分級引擎**：全面精準適配繁體中文、簡體中文、日文（漢字/平假名/片假名/全形標點「」【】、。〜）、韓文 (Hangul) 與歐美西文，方塊文字完美包裹居中，徹底解決文字溢出。
- **動態 ViewBox 畫布計算**：移除寫死高度，由節點幾何尺寸動態撐開，杜絕任何圖表被截斷破版。
- **深色模式超高對比連接線**：連接線與箭頭改用 `#79c0ff` 亮藍色（WCAG AAA 9.5:1 對比度），線條清清楚楚。

#### 3. 深色模式 3D 視覺與圖示適配
- **立體亮銀框線**：主畫面筆記卡片與長按彈出選單（PopupMenu）加入 `1.5dp` 亮銀立體外框（`#5A5A5A`）與 `8dp` 圓角，黑底質感大幅提升。
- **筆記總管檔案圖示高亮**：Markdown、TXT、PDF、圖片、DOC 等檔案圖示在深色模式下套用純白高亮剪影濾鏡，黃色資料夾維持經典黃。
- **GitHub 挑選 Repo 對話框適配**：Repo 名稱與描述改用 DayNight 高對比文字與實體分隔線。
- **100% 官方標準 Git Logo**：替換「建立」選單中 Git 圖示為官方正統 45 度珊瑚橘（`#F05032`）雙層分支標誌。

#### 4. 外部 PDF 閱讀器密碼解鎖權限與 ISO 標準加密修復
- **外部 Reader 密碼驗證支援**：移除呼叫外部 PDF 閱讀器時的 `FLAG_ACTIVITY_NO_HISTORY` 旗標，並主動透過 `grantUriPermission` 授權，避免第三方 Reader（如 Adobe Acrobat、Google PDF 等）跳出輸入密碼框時 Android 系統提前收回 FileProvider 讀取權限。
- **補齊 ProviderPaths 路徑**：在 `provider_paths.xml` 補齊 `<external-files-path>`、`<files-path>` 與 `<cache-path>`。
- **正統 ISO-32000-1 密碼學加密**：測試檔 `sample_protected_123456.pdf` 全面以正統 RC4-128 演算法重新生成，內部檢視與第三方 Reader 均可 100% 順暢解鎖。

#### 5. 檔案總管「系統隱藏檔」預設隱形
- **清爽知識庫視覺**：在 `FileExplorerActivity` 中將 `.` 開頭的系統檔案（如 `.gitignore`、`.DS_Store`）預設隱藏，維持純淨閱讀清單並防止誤觸與手滑刪除。

#### 6. 穩定性與防呆機制強化
- **筆記移除強制物理清空**：強化 `deleteLocalGitRepository` 雙層保險刪除，確保點擊「移除」時 100% 清空本機實體檔案與資料夾。
- **主畫面非同步執行緒化**：將主畫面 `Pull`、`Push`、`Remove` 全面轉為背景工作執行緒，消除主執行緒卡頓並提升 Pixel 10 (Android 16) 相容性。

## [4.007] - 2026-08-29

### 🚀 New Features & Enhancements

#### 1. 長按選單新增「自動提交」與「專案容量計算」 (Auto Commit & Storage Calculation)
- **智慧按需自動提交**：透過 `status.hasUncommittedChanges()` 精準檢測，有未存修改時自動產生時間戳 Commit 並同步 Push 回遠端；工作區乾淨時提示無需提交。
- **專案空間佔用分析對話框**：背景非同步遞迴計算，結構化呈現「筆記與附件」、「.git 歷史版本庫」與「總佔用空間」大小及檔案總數。

#### 2. `MediaStore.Downloads` 零權限標準備份 (Zero-Permission Backup)
- **免權限合規匯出**：採用 Android 系統標準 `MediaStore.Downloads` API，徹底符合 Google Play 政策且不受 Android 11~16 Scoped Storage 限制。
- **原生串流 ZIP 打包**：改用原生 Java `ZipOutputStream` 遞迴串流打包，徹底修復 JGit `Inflater` 崩潰問題，完整備份所有筆記、附件與歷史。
- **儲存位置明確告知**：備份完成後提示存放於系統「下載 (Download)」資料夾。

#### 3. 深色模式對話框與 Git 同步體驗修復 (Dark Theme & Sync Fixes)
- **深色模式文字可見度修復**：移除寫死之 Light 主題，讓「查看修改紀錄」與「遠端分支」對話框在深色模式下完美呈現清晰白色文字。
- **剛 Clone 完進入筆記誤判修復**：修正 `isWorkingTreeDirty` 判定邏輯，排除未追蹤輔助目錄誤判，點進筆記流暢無阻。

## [4.006] - 2026-08-29

### 🚀 New Features & Architecture

#### 1. Markdown 離線視覺化預覽與圖表 (Markdown & Mermaid In-App Preview)
- **100% 離線渲染**：整合 Marked.js，支援 GFM 表格、任務清單、代碼區塊高亮與自適應滿版排版。
- **Mermaid 向量圖表**：支援流程圖（`flowchart`）、時序圖（`sequenceDiagram`）與甘特圖等向量即時渲染。
- **雙向筆記跳轉 (WikiLinks)**：支援 `[[筆記名稱]]` 語法，點擊即時跳轉至關聯筆記，構建個人知識庫網絡。
- **即時預覽與編輯無縫切換**：檢視模式自動載入視覺化預覽，點擊編輯按鈕即時切換至純文字編輯框。

#### 2. App 內原生 PDF 離線閱讀器 (In-App Offline PDF Viewer)
- **專屬離線檢視畫面 (`ViewPdfActivity`)**：支援點選獨立 `.pdf` 筆記或附件時於 App 內原生加載，提供流暢雙指縮放與滑動翻頁。
- **密碼保護 PDF 互動解鎖**：自動偵測加密 PDF，提供安全密碼輸入對話框、錯誤防呆與 Session 記憶體暫存快取。
- **外部開啟整合**：預覽工具列提供「外部開啟」選單，透過 `FileProvider` 安全跳轉至系統安裝之第三方 PDF Reader。

#### 3. 全域 DayNight 深淺色主題適配 (DayNight Theme Adaptation)
- **跟隨系統與手動覆蓋**：全 App 採用 `DayNight` 主題架構，於「設定」提供「跟隨系統 / 淺色模式 / 深色模式」三選一選項與無縫秒速切換。
- **全方位介面與組件深色適配**：包含主畫面、檔案總管、純文字編輯器、Markdown / PDF 預覽 WebView、Toolbar 標題列與功能彈窗選單全面深淺色適配。
- **語系樣式隔離**：清理多語系目錄中的覆蓋樣式，確保所有語系（繁中/簡中/日文/英文）100% 正確繼承 DayNight 主題。

#### 4. 核心相依升級與單元測試強化 (Core Upgrades & Testing Standard)
- **JGit 核心升級**：升級 Eclipse JGit 核心至 7.7.1，大幅提升 Clone / Pull 傳輸效能與大儲存庫相容性。
- **單元測試目錄與 Package 統一**：全專案單元測試與儀器測試統一重構至 `inmethod.gitnotetaking.test`，新增 Manifest 主題合規性與語系樣式安全性自動化檢驗。

#### 5. Git 同步效能與未 Commit 安全防護 (Git Sync Optimization & Working Tree Guardrails)
- **進入筆記零延遲背景同步**：進入筆記畫面時秒開本機清單，背景比對 Commit ID 執行 Pull，僅在遠端有新筆記下載時提示 Toast。
- **本地未 Commit 嚴格防護**：手動重整或進入筆記時，若偵測到本地有未存/未提交修改，立即停止 Pull 並發出 Toast 警示，100% 杜絕覆蓋風險。
- **手動「重整」按鈕接通遠端同步**：檔案總管「重整」按鈕完整串接遠端 Git 同步與本地/遠端智慧分流。

## [4.005] - 2026-08-29

### 🚀 Enhancements & Architecture

#### 1. 多語系即時切換與 Android 13+ 標準適配 (Instant Multilingual Switching)
- **標準地區碼補全**：將日文語系標籤補全為標準 `ja-JP`（繁中 `zh-TW`、簡中 `zh-CN`、英文 `en`），杜絕 CJK 漢字碼位渲染延遲。
- **Android 13+ 標準設定檔**：建立 `res/xml/locales_config.xml` 並於 `AndroidManifest.xml` 配置 `AppLocalesMetadataHolderService` 自動持久化。
- **即時刷新與生命週期連動**：在 `CustomPreferenceFragment` 與 `MainActivity.onResume()` 自動偵測語言切換並執行 `recreate()`，達成零延遲即時秒切。

#### 2. 香港 (HK) 冗餘語系清理 (HK Redundancy Cleanup)
- 徹底移除 `values-zh-rHK` 冗餘資源檔與相關發布日誌，全面統整為 4 大標準語系（繁中、簡中、日文、英文）。

#### 3. 本地筆記建立流程優化 (Streamlined Local Note Creation)
- 本地端筆記建立成功後直接秒速關閉畫面返回主清單，徹底移除多餘的「建立成功」確認對話框。

#### 4. 自動化測試套件與專案規範 (Automated Testing Suite)
- 新增 `LocaleAndResourceConsistencyTest`（資源與語系標籤 100% 完整性檢驗）與 `LanguageSwitchingInstrumentedTest` 儀器測試。
- 產出 `TESTING.md` 測試指南，並將一鍵單元測試規範寫入專案鐵律。

## [4.004] - 2026-08-28

### 🎨 UI & UX Enhancements

#### 1. GitHub 筆記儲存庫挑選提示優化 (Repository Selection Hint)
- **明確過濾標題**：將儲存庫挑選對話框頂部標題調整為「選擇 note 開頭筆記 ({帳號})」，明確告知使用者系統僅過濾並抓取 `note*` 相關筆記儲存庫。
- **多語系極簡文案**：同步更新 5 國語系字串，英文採用 `Select Note (note* only)`、日文採用 `ノート選択 (note* のみ)`，簡潔清晰。

## [4.003] - 2026-08-28

### 🚀 New Features & Enhancements

#### 1. GitHub OAuth 2.0 官方一鍵授權登入 (GitHub OAuth Dual-Track Integration)
- **官方 OAuth 2.0 Web Flow**：整合 Chrome Custom Tabs 呼叫 GitHub 官方授權流程，使用者同意授權後透過專屬 Deep Link（`gitnotetaking://oauth/github`）秒速自動返回 App 並完成 Token 交換。
- **雙軌並存 UI 架構**：於建立 GitHub 筆記對話框頂部配置顯眼的「🐙 一鍵登入 GitHub 授權 (推薦)」按鈕，同時完整保留既有 PAT 4 步驟手動指引與剪貼簿自動帶入功能，兼具極致便利與靈活性。
- **OAuth 生命週期防呆**：授權碼採即收即銷機制（Consume-on-receive），避免 Activity 重建或返回主畫面時因重複提交過期授權碼而導致驗證失敗。

#### 2. 智慧進度輪播提示 (Dynamic Progress Indicator)
- **多階段動態輪播**：在儲存庫同步（Pull）、下載（Clone）與備份時，每 10 秒平滑輪播切換「請稍候...」、「向 GitHub 取得檔案最近更新日...」、「正在更新檔案真實日期...」，消除使用者對長時間網路作業的疑慮。
- **多語系支援**：同步更新 5 國語系提示字串（繁中台/港、簡中、日文、英文），日文與英文均經精簡最佳化。

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
