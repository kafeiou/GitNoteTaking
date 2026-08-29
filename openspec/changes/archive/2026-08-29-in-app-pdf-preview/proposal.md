## Why

目前使用者在 App 內點擊 `.pdf` 檔案或筆記中的 PDF 附件時，系統皆會發送 Intent 跳出 App 呼叫外部閱讀器，破壞了一體化的閱讀與編輯流暢度。此外，對於工程師存放在 Git 倉庫中的加密技術手冊或有密碼保護的 PDF 文件，使用者期望能在 App 內部直接解密並預覽。本變更旨在引入「App 內原生 PDF 預覽與密碼解鎖支援」，提供一站式、無縫且具備密碼保護支援的離線 PDF 檢視體驗。

## What Changes

- **App 內建 PDF 預覽器 (In-App PDF Viewer)**：
  - 當使用者在檔案總管或筆記附件列中點選 `.pdf` 檔案時，改為直接在 App 內部啟動預覽畫面，不再強制跳出 App。
  - 支援離線向量渲染、雙指縮放 (Pinch-to-zoom)、平滑滑動翻頁與深淺色介面適配。
- **PDF 密碼保護與互動解鎖 (Password-Protected PDF Support)**：
  - 自動偵測加密 PDF 檔案。若遇到有密碼保護之 PDF，於 App 內彈出安全密碼輸入對話框，解密後即時呈現。
- **全 App 原生介面深淺色主題跟隨 (Full App DayNight Theme Support)**：
  - 將應用程式主題升級為 DayNight，使清單、編輯器、對話框與設定頁面能 100% 主動跟隨 Android 系統深淺色模式自動切換，兼顧白天清晰度與夜間護眼舒適度。

## Capabilities

### New Capabilities
- `pdf-viewing`: 涵蓋獨立 PDF 檔案與筆記附件 PDF 之 App 內原生離線預覽、手勢縮放、密碼保護解鎖與外部開啟轉發規範。
- `theme-adaptation`: 涵蓋全 App 原生介面、清單、編輯器、對話框與 Web 預覽之 DayNight 自動跟隨系統深淺色模式切換規範。

### Modified Capabilities
（無現有 Capability 需求變更）

## Impact

- **UI / Activity**：新增或擴充專屬 PDF 預覽 Activity / Fragment，更新 `FileExplorerActivity` 與 `ViewFileActivity` 之點擊導航分流；升級全 App 主題為 `DayNight` 並新增夜間色彩資源。
- **靜態資產 / 相依套件**：引入輕量離線 `pdf.js` 或 Android 原生/Jetpack PDF 渲染模組。
- **多語系資源**：新增 4 國語系關於 PDF 密碼輸入對話框、密碼錯誤提示與選單文字。
