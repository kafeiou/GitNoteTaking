# 提案：筆記附件底部抽屜訊息與操作卡片 (Attachment Bottom Sheet Details)

## Why

目前在文字筆記檢視畫面 (`ViewFileActivity`) 中，附件僅以「附件1」、「附件2」等抽象標籤呈現。使用者點選附件時會直接強制觸發外部 App 或 PDF 閱讀器，在此之前完全無法得知檔案真實名稱、大小、副檔名格式與修改時間，容易造成誤觸、開啟錯誤應用程式或等待時間浪費。

為了大幅改善使用者體驗，我們需要將點選附件的行為改為由螢幕底部滑出 **Material 底部抽屜卡片 (Bottom Sheet Dialog)**，清楚呈現檔案中繼資料（真實檔名、圖示、大小、類型、最後修改時間），並在抽屜內提供「開啟檔案」、「另存到下載 (Downloads)」與「刪除附件」等整合操作。

## What Changes

- **底部抽屜詳細資訊 (Bottom Sheet Dialog)**：
  - 點選或長按 `附件N` 標籤時，彈出 Material 底部抽屜。
  - 呈現檔案專屬彩色圖示（PDF / Word / Excel / PPT / 純文字 / 圖片 / 未知檔案）。
  - 呈現真實檔案名稱與副檔名（如 `2026年度第三季專案計畫書.docx`）。
  - 呈現自動格式化之檔案大小（如 `1.85 MB`、`240 KB`）。
  - 呈現人性化檔案格式（如 `Word 文件 (.docx)`、`PDF 文件 (.pdf)`）。
  - 呈現最後修改時間（`YYYY-MM-DD HH:mm`）。
- **集中整合動作按鈕**：
  - **【開啟檔案】**：若為 PDF 檔案呼叫內建 `ViewPdfActivity`，其他檔案透過 `FileProvider` 呼叫外部 App；若無可支援應用程式則彈出 Toast 提示。
  - **【另存到下載 (Downloads)】**：將原先分散在長按選單的下載功能整合至抽屜，安全複製檔案至手機 `Download` 目錄。
  - **【刪除附件】**：點擊跳出二次防呆確認，確認後實體刪除、於本地自動 Commit 純英文紀錄（`User deleted attachment: <檔名>`），不主動 Push，動態刷新畫面移除該附件按鈕。
- **介面保持簡潔**：
  - 筆記頁面上方維持 `附件1`、`附件2` 緊湊排版。
- **全域多語系與深淺色主題**：
  - 完整支援 4 國語系（繁中、簡中、英文、日文）。
  - 抽屜底色與元件適配 Dark Mode（`#1E1E1E` 護眼深灰）與 Light Mode。

## Capabilities

### New Capabilities
- `attachment-details`: 點選筆記附件標籤時，以 Material 底部抽屜呈現真實檔名、大小、格式與修改時間等中繼資料，並提供開啟、下載至手機 Downloads 與刪除附件之操作流程。

### Modified Capabilities
<!-- 無既有規格的 REQUIREMENT 變更 -->

## Impact

- **UI / Layout**：
  - 新增 `app/src/main/res/layout/dialog_attachment_bottom_sheet.xml`。
- **Activity / Logic**：
  - 修改 `app/src/main/java/inmethod/gitnotetaking/ViewFileActivity.java` 內附件點擊與長按監聽器，新增 `showAttachmentBottomSheet(File file, TextView attachButton)` 方法。
- **多語系資源檔**：
  - 更新 `app/src/main/res/values/strings.xml`、`values-zh-rTW/`、`values-zh-rCN/`、`values-ja/`。
- **單元測試**：
  - 在 `app/src/test/java/inmethod/gitnotetaking/test/` 新增附件中繼資料格式化與錯誤處理邏輯之單元測試。
