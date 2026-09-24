# 任務清單：筆記附件底部抽屜訊息與操作卡片 (Tasks)

## 1. 多語系字串與 XML 介面配置 (Resources & Layout)

- [x] 1.1 在 4 國語系檔（`values/strings.xml`、`values-zh-rTW/`、`values-zh-rCN/`、`values-ja/`）新增附件中繼資料標籤（大小、格式、修改時間）、動作按鈕文字（開啟檔案、另存到下載、刪除附件）與 Toast 提示字串，並以 `./gradlew processDebugResources` 驗證無缺失。
- [x] 1.2 建立 `app/src/main/res/layout/dialog_attachment_bottom_sheet.xml` Material 底部抽屜版面配置，包含頂部拖曳把手、檔案圖示、真實檔名、中繼資料清單與三大動作按鈕，並驗證深淺色主題顏色屬性 (`?attr/colorSurface`, `?android:attr/textColorPrimary`) 配置完備。

## 2. 邏輯核心與 Activity 整合 (Core Implementation)

- [x] 2.1 在 `ViewFileActivity.java` 實作 `showAttachmentBottomSheet(File file, TextView attachButton)`，讀取檔案屬性、格式化大小 (`Formatter.formatFileSize`)、取得圖示與格式描述並呈現於 `BottomSheetDialog`。
- [x] 2.2 實作抽屜中的【開啟檔案】點擊動作：若為 `.pdf` 檔案啟動 `ViewPdfActivity`，其餘檔案透過 `FileProvider` 發送 `ACTION_VIEW` 意圖，並以 `try-catch (ActivityNotFoundException)` 捕捉例外並彈出 Toast 提示。
- [x] 2.3 實作抽屜中的【另存到下載 (Downloads)】點擊動作：將附件安全複製至 `Environment.DIRECTORY_DOWNLOADS` 目錄，防呆檢查同名檔案並顯示對應 Toast 提示。
- [x] 2.4 實作抽屜中的【刪除附件】點擊動作：彈出 AlertDialog 二次確認，確認後實體刪除本機檔案、於本地 Git 儲存庫建立英文 Commit（`User deleted attachment: <檔名>`，不觸發遠端 Push）、關閉抽屜、動態自 `layoutAttachment` 移除該按鈕，並於附件清空時自動隱藏 `scrollAttachment`。
- [x] 2.5 將 `ViewFileActivity.java` 內動態生成之附件按鈕短按 (`setOnClickListener`) 與長按 (`setOnLongClickListener`) 皆統一轉向呼叫 `showAttachmentBottomSheet(...)`。

## 3. 單元測試與整合驗證 (Testing & Verification)

- [x] 3.1 在 `app/src/test/java/inmethod/gitnotetaking/test/` 撰寫單元測試，驗證檔案大小格式化、MIME 類型轉換、刪除提交訊息格式（`User deleted attachment: <檔名>`）與極端條件判斷，執行 `./gradlew testDebugUnitTest` 確保 100% 通過。
- [x] 3.2 執行 `./gradlew assembleDebug` 驗證全專案編譯順利無誤，確認 Dark / Light 模式下介面對比度正常。
