## 1. 建立離線 PDF 渲染靜態資產 (Assets)

- [x] 1.1 建立 `app/src/main/assets/pdf/` 目錄，配置輕量離線 `pdf.min.js`、`pdf.worker.min.js` 與支援密碼輸入、手勢縮放之 `pdf_viewer.html` 離線樣板。

## 2. 實作專屬 ViewPdfActivity 與密碼解鎖對話框

- [x] 2.1 建立 `app/src/main/res/layout/activity_view_pdf.xml` 佈局檔，包含 Material Toolbar 與滿版 WebView 檢視容器。
- [x] 2.2 實作 `ViewPdfActivity.java`，配置 JavaScriptInterface 接收加密偵測事件（`onPasswordRequired`），並彈出安全密碼輸入對話框傳回解密密碼，且在當前應用程式生命週期內於記憶體暫存密碼避免重複輸入。
- [x] 2.3 於 `ViewPdfActivity` 頂部標準 Toolbar 加入返回鍵、檔名、頁數指示器與右上角「外部開啟」控制項。
- [x] 2.4 在 `AndroidManifest.xml` 中註冊 `ViewPdfActivity`。

## 3. 檔案總管與筆記附件之 PDF 點擊導航分流

- [x] 3.1 修改 `FileExplorerActivity.java`，在點擊 `.pdf` 檔案時轉為啟動 `ViewPdfActivity`。
- [x] 3.2 修改 `ViewFileActivity.java`，在底部附件橫條點擊 PDF 附件時轉為啟動 `ViewPdfActivity`。
- [x] 3.3 新增 4 國語系關於 PDF 密碼輸入對話框標題、確認/取消、密碼錯誤提示等字串資源。

## 4. 全 App 原生介面 DayNight 深淺色主題適配

- [x] 4.1 將 `styles.xml` 的 `AppTheme` 升級為 `Theme.AppCompat.DayNight.DarkActionBar`，並建立 `app/src/main/res/values-night/styles.xml` 與 `colors.xml` 配置夜間暗黑色彩資源。
- [x] 4.2 於偏好設定中新增主題模式選單（跟隨系統 / 淺色 / 深色）並即時套用切換。
- [x] 4.3 檢視主畫面卡片、文字編輯器、對話框與設定介面，確保在深淺色模式下文字與圖示皆具備高對比度。

## 5. 自動化單元測試與驗證

- [x] 5.1 建立 `PdfPreviewUnitTest.java` 單元測試，驗證 PDF 靜態資產完整性、副檔名分流邏輯、密碼保護事件處理合約與主題色彩覆蓋。
- [x] 5.2 執行 `./gradlew testDebugUnitTest` 確保全專案單元測試 100% 通過。
- [x] 5.3 執行 `./gradlew assembleDebug` 確認打包成功。

