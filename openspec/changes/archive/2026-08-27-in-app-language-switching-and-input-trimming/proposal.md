## Why

為提升使用者對多語系切換的自主控制權，應用程式需在設定中提供獨立切換語言的功能；同時，為避免使用者在複製貼上或手動輸入檔名、筆記名、Git URL 與帳密時夾帶首尾空白導致操作異常或 Git 連線失敗，需在所有輸入框按下「OK」時自動過濾前後空白並加入防呆驗證。

## What Changes

- **設定中自由切換語系 (In-App Language Selection)**：
  - 在偏好設定中新增「語言設定 (Language)」選單，提供 5 個標準選項：跟隨系統 (預設)、繁體中文、簡體中文、日本語、English。
  - 使用 Android 官方 `AppCompatDelegate.setApplicationLocales` 標準 API，達成即時生效與向後相容。
- **全專案輸入自動過濾前後空白與防呆 (Silent Auto-Trim on OK & Validation Guardrail)**：
  - 在全專案所有輸入對話框（檔案名稱、目錄名稱、筆記名稱、Git URL、帳號、密碼/Token、作者姓名、Email、分支名稱、Commit 訊息、相片檔名等）按下「OK」時，自動執行 `.trim()` 過濾前後空白，**無需額外詢問**。
  - 若輸入全為空白（過濾後為空字串），阻擋執行並以 Toast 提示不可為空。

## Capabilities

### New Capabilities
- `language-selection`: 定義偏好設定中語言切換選單、支援語系標籤與即時套用行為規範。
- `input-sanitization`: 定義全專案文字輸入框點擊 OK 時自動過濾首尾空白與空值阻擋之安全規範。

### Modified Capabilities
<!-- 無既有 Capabilities 需修改 -->

## Impact

- **程式碼影響**：`CustomPreferenceFragment.java`、`FileExplorerActivity.java`、`ViewFileActivity.java`、`CreateLocalGitActivity.java`、`CloneGitActivity.java`、`ModifyLocalGitActivity.java`、`ModifyRemoteGitActivity.java`。
- **資源檔影響**：`res/xml/settingspreferences.xml`、各語系 `strings.xml`。
- **使用者體驗**：自由指定語言、輸入更具容錯性、避免 Git 認證或檔名空白引發的異常。
