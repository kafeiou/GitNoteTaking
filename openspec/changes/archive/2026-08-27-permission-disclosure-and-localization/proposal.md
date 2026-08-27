## Why

為符合 Google Play 隱私權政策規範並提升使用者體驗，應用程式在索取危險權限（相機、多媒體讀取）前，必須先向使用者說明原因（前置告知），待使用者確認同意後才向系統發起請求，未授權時必須阻擋功能執行並提供再次提醒機制；同時，為拓展國際化支援並確保介面零跑版，需為全 App 導入簡體中文與日語多語系支援。

## What Changes

- **權限按需請求與前置告知 (Just-In-Time Permission Disclosure)**：
  - 建立 `PermissionHelper` 共用工具，在調用系統權限視窗前彈出 App 內說明對話框，說明權限用途與原因。
  - 使用者點擊「同意」才呼叫系統權限請求；點擊「暫不同意」則絕對不呼叫系統請求並安全阻擋功能。
  - 移除 `MainActivity` 開機無條件全域索取權限邏輯，改為在功能點（如拍照、選取附件）觸發時按需請求。
  - 事後再次點擊未授權功能時，提供防呆阻擋提示與重新授權管道；若使用者已勾選「不再詢問」，提供跳轉系統設定頁面按鈕。
- **全 App 多語系擴展 (Full App Localization - i18n)**：
  - 新增 `values-zh-rCN/strings.xml`（簡體中文全 App 語系支援）。
  - 新增 `values-ja/strings.xml`（日文 日本語全 App 語系支援，採用精簡漢字詞彙防止跑版）。
  - 同步更新 `values/strings.xml`（英文）與 `values-zh-rTW/strings.xml`、`values-zh-rHK/strings.xml`（繁體中文）之權限相關字串。

## Capabilities

### New Capabilities
- `permission-handling`: 定義危險權限之前置告知、按需請求、拒絕阻擋與跳轉系統設定之行為規範。
- `multilingual-support`: 定義全 App 之多語系資源組織架構（繁中、簡中、日文、英文）與在地化標準。

### Modified Capabilities
<!-- 無既有 Capabilities 需修改 -->

## Impact

- **程式碼影響**：新增 `PermissionHelper.java`，修改 `MainActivity.java`、`ViewFileActivity.java`。
- **資源檔影響**：新增 `values-zh-rCN/strings.xml`、`values-ja/strings.xml`，更新 `values/strings.xml`、`values-zh-rTW/strings.xml`、`values-zh-rHK/strings.xml`。
- **使用者體驗**：啟動更乾淨、權限意圖更透明、多國語系切換順暢且無跑版風險。
