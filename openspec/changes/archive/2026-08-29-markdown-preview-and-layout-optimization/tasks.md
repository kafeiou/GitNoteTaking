## 1. 建立離線 Markdown 渲染靜態資產 (Assets)

- [x] 1.1 建立 `app/src/main/assets/markdown/` 目錄，加入輕量 `marked.min.js`、`github-markdown.css` 與自適應手機螢幕之 `preview.html` 樣板，並驗證靜態檔案完整性。

## 2. 修復全螢幕滿版佈局與新增選單控制項

- [x] 2.1 修改 `app/src/main/res/layout/activity_view_file_content.xml`，為 `ScrollView` 加上 `android:fillViewport="true"`、將高度改為 `0dp` 垂直約束，移除寫死之 30dp 邊距與固定行數，並平行加入 `WebView` 預覽容器。
- [x] 2.2 修改 `app/src/main/res/menu/menu_view_file.xml` 與 4 國語系 `strings.xml`，新增「預覽 / 編輯」切換選單項目與提示字串。

## 3. ViewFileActivity 雙模式切換與資料連動實作

- [x] 3.1 修改 `ViewFileActivity.java`，實作副檔名判斷邏輯（`.md` / `.markdown` 預設進入預覽模式），初始化 `WebView` 並配置自適應與本地圖片路徑支援。
- [x] 3.2 在 `ViewFileActivity.java` 實作預覽與編輯雙模式切換邏輯，確保修改內容即時傳遞至預覽頁面重新渲染，且原本之存檔、Commit、拍照與附件功能 100% 正常運作。

## 4. 測試驗證與品質把關

- [x] 4.1 執行全專案單元測試 `./gradlew testDebugUnitTest`，驗證所有語系、資源與邏輯測試 100% 通過。
- [x] 4.2 執行 `./gradlew assembleDebug`，確認編譯建置成功無任何錯誤。
