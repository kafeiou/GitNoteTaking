# InMethodGitNoteTaking 自動化測試指南 (Testing Guide)

本文件說明專案中的自動化測試架構、目前已建立的測試項目，以及如何執行相關測試指令。

---

## 📑 測試套件總覽 (Test Suites)

專案採用雙層測試架構：
1. **本機單元測試 (Local JVM Unit Tests)**：執行速度極快（< 1 秒），無須真機或模擬器，用於檢核語系一致性、資源完整性與核心邏輯。
2. **Android 儀器測試 (Instrumented Tests)**：在 Android 實體環境中驗證動態語系切換與 UI 行為。

---

## 🧪 目前已包含之測試項目

### 1. 語系與資源一致性單元測試 (`LocaleAndResourceConsistencyTest.java`)
- **檔案路徑**：[`app/src/test/java/inmethod/gitnotetaking/test/LocaleAndResourceConsistencyTest.java`](file:///W:/developer/project/inmethod/android/InMethodGitNoteTaking/app/src/test/java/inmethod/gitnotetaking/test/LocaleAndResourceConsistencyTest.java)
- **測試項目**：
  - `testLocalesConfigMatchesLanguageValues`：驗證 `res/xml/locales_config.xml` 宣告之標籤（`zh-TW`, `zh-CN`, `ja-JP`, `en`）與所有 `arrays.xml` 中的 `language_values` 100% 吻合對齊。
  - `testLanguageEntriesAndValuesCountMatch`：驗證 4 國語系（繁中、簡中、日文、英文）的選項名稱與標籤數量、順序皆一致。
  - `testAllLanguageStringsComplete`：自動掃描比對所有語系的 `strings.xml`，確保 **零遺漏翻譯（Zero Missing Keys）**。
  - `testNoHkResourcesRemain`：確保 `values-zh-rHK` 冗餘目錄已被徹底清理。

### 2. Markdown 離線預覽與排版單元測試 (`MarkdownPreviewAndLayoutUnitTest.java`)
- **檔案路徑**：[`app/src/test/java/inmethod/gitnotetaking/test/MarkdownPreviewAndLayoutUnitTest.java`](file:///W:/developer/project/inmethod/android/InMethodGitNoteTaking/app/src/test/java/inmethod/gitnotetaking/test/MarkdownPreviewAndLayoutUnitTest.java)
- **測試項目**：
  - `testMarkdownAssetsExistAndAreValid`：驗證 `marked.min.js`, `github-markdown.css`, `preview.html` 離線靜態資產完整性與 API 接口。
  - `testGithubMarkdownCssCoverage`：驗證深淺色主題 (`prefers-color-scheme`)、圖片防跑版、表格獨立滾動與 Obsidian wikilink 樣式覆蓋。
  - `testMarkdownFileExtensionDetection`：驗證 `.md`, `.markdown` 等大小寫副檔名過濾與純文字檔案隔離邏輯。
  - `testMarkdownContentJsonEscapingSafety`：驗證跨語言傳遞給 WebView 時之字元跳脫安全（防止雙引號、換行與 XSS 導致語法截斷）。
  - `testWikilinkPatternParsing`：驗證 Obsidian 雙向連結 (`[[筆記名]]` 與 `[[筆記名|別名]]`) 正則提取與標準化補全副檔名邏輯。
  - `testMarkedJsSyntaxRulesContract`：驗證 Markdown 引擎完整具備標題 (H1~H6)、代碼塊、待辦方框 (Checkboxes)、表格、雙向鏈接與 Callout 區塊語法規範。
  - `testUrlRoutingClassification`：驗證外網連結 (`http/https`)、雙向連結 (`wikilink:`) 與相對路徑之智慧分類路由。

### 3. PDF 原生預覽、密碼解鎖與主題適配單元測試 (`PdfPreviewUnitTest.java`)
- **檔案路徑**：[`app/src/test/java/inmethod/gitnotetaking/test/PdfPreviewUnitTest.java`](file:///W:/developer/project/inmethod/android/InMethodGitNoteTaking/app/src/test/java/inmethod/gitnotetaking/test/PdfPreviewUnitTest.java)
- **測試項目**：
  - `testPdfAssetsExistAndAreValid`：驗證 `pdf_viewer.html`, `pdf.min.js`, `pdf.worker.min.js` 靜態資產完整性、JavascriptInterface 串接與深淺色 CSS 適配。
  - `testPdfJsApiAndSecurityContract`：驗證 `pdf.min.js` 提供標準 `getDocument` API、`NEED_PASSWORD`、`INCORRECT_PASSWORD` 密碼事件與 `/Encrypt` 加密字典識別。
  - `testPdfExtensionDetection`：驗證 `.pdf`, `.PDF` 等副檔名識別與專屬預覽路由分流。
  - `testThemeModeMapping`：驗證 DayNight 深淺色主題模式值（`system`, `light`, `dark`）映射正確性。
  - `testSampleProtectedPdfStructureAndEncryption`：驗證測試用加密 PDF 結構、標頭與 `/Encrypt` 密碼保護字典。
  - `testPdfPasswordSessionCacheLifecycle`：驗證 Session 記憶體暫存密碼之讀寫與銷毀生命週期。

### 4. 動態語系切換儀器測試 (`LanguageSwitchingInstrumentedTest.java`)
- **檔案路徑**：[`app/src/androidTest/java/inmethod/gitnotetaking/test/LanguageSwitchingInstrumentedTest.java`](file:///W:/developer/project/inmethod/android/InMethodGitNoteTaking/app/src/androidTest/java/inmethod/gitnotetaking/test/LanguageSwitchingInstrumentedTest.java)
- **測試項目**：
  - `testDynamicLocaleSwitching`：在 Android 運行環境中依序切換為 `zh-TW`、`zh-CN`、`ja-JP`、`en`，驗證系統能否即時正確解析對應語言之字串資源。

---

## 💻 測試執行指令 (Commands)

在專案根目錄下開啟終端機（PowerShell / Bash）即可執行以下指令：

### 1. 執行本機單元測試（最常用、秒級完成）
```bash
./gradlew testDebugUnitTest
```
或執行全部單元測試：
```bash
./gradlew test
```

### 2. 執行單一測試類別
```bash
./gradlew testDebugUnitTest --tests inmethod.gitnotetaking.LocaleAndResourceConsistencyTest
```

### 3. 執行真機 / 模擬器 UI 儀器測試（需先開啟模擬器或接上真機）
```bash
./gradlew connectedDebugAndroidTest
```

### 4. 執行全專案健康檢查 (Check)
包含代碼檢查與所有單元測試：
```bash
./gradlew check
```

---

## 📊 查看視覺化測試報告 (HTML Reports)

測試執行完成後，Gradle 會自動生成完整的 HTML 視覺化測試報告，可直接用瀏覽器開啟查看：

- **單元測試報告路徑**：
  `app/build/reports/tests/testDebugUnitTest/index.html`
- **儀器測試報告路徑**：
  `app/build/reports/androidTests/connected/index.html`
