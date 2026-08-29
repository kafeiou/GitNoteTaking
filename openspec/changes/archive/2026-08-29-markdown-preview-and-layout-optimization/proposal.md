## Why

目前 `ViewFileActivity` 在檢視與編輯文字檔案時存在兩大問題：
1. **版面未滿版與邊界死區**：由於 `ScrollView` 缺少 `fillViewport="true"` 且內部佈局高度未採用 `0dp` 約束，導致短文章在手機螢幕下方留有大片無法點擊編輯的空白死區，且人工寫死的固定下邊距影響了 EdgeToEdge 的完整呈現。
2. **缺乏 Markdown 視覺化預覽**：當使用者從 GitHub 同步 Obsidian 等 Markdown 筆記時，只能以純文字方式查看充滿 `#`、`**`、`- [ ]` 等符號的原始碼，缺少美觀易讀的渲染排版模式。

因此，本變更旨在修正編輯畫面的滿版約束，並引入 100% 離線之 Markdown 渲染與「預覽/編輯」雙模式切換功能，讓使用者能同時享受精美閱讀與即時編輯 Git 筆記的體驗。

## What Changes

- **編輯器全螢幕滿版與 EdgeToEdge 修復**：
  - 修正 `activity_view_file_content.xml`，為 `ScrollView` 加上 `android:fillViewport="true"`，高度改為 `0dp` 約束。
  - 移除寫死的邊距與固定行數，確保 `EditText` 100% 鋪滿可用空間，點擊任意空白區域皆可直接打字編輯。
- **內建 100% 本地離線 Markdown 渲染引擎**：
  - 於 `assets/markdown/` 引入輕量且成熟之離線 Markdown 解析庫（`marked.min.js` 與 `github-markdown.css`）。
  - 在 `ViewFileActivity` 整合 Android 系統原生 `WebView`，提供自適應手機螢幕之 Markdown 渲染能力（支援標題、粗斜體、表格、清單 Checkbox、代碼區塊與本地圖片自適應縮放）。
- **「預覽 / 編輯」雙模式無縫切換**：
  - 當開啟 `.md` 或 `.markdown` 檔案時，預設進入「美化預覽模式」。
  - 於工具列選單提供切換按鈕（👁️ 預覽 / ✏️ 編輯），使用者可一鍵切換回原始純文字編輯框。
  - 當開啟非 Markdown 檔案（如 `.txt`、程式碼檔案）時，維持原本的純文字編輯模式。
- **保留完整既有功能**：
  - 既有之拍照存檔、附件選取、儲存變更、Commit 訊息輸入與遠端同步邏輯 100% 保留且不受影響。

## Capabilities

### New Capabilities
- `markdown-viewing`: 提供 `.md` 筆記之 100% 本地離線 Markdown 美化渲染、自適應螢幕排版、以及「預覽/編輯」雙模式一鍵切換功能。

### Modified Capabilities
<!-- 無既有 spec 需求變更 -->

## Impact

- **程式碼影響**：
  - 新增 `app/src/main/assets/markdown/`（`marked.min.js`, `github-markdown.css`, `preview.html`）。
  - 修改 `app/src/main/res/layout/activity_view_file_content.xml`（滿版約束與 `WebView` 容器）。
  - 修改 `app/src/main/res/menu/menu_view_file.xml`（加入預覽/編輯切換選單項目）。
  - 修改 `app/src/main/java/inmethod/gitnotetaking/ViewFileActivity.java`（雙模式控制與 Markdown 資料傳遞）。
- **依賴與系統**：
  - 使用 Android 內建原生 `WebView`，無需引進大型外部第三方相依套件。
  - 100% 離線運作，不需任何外部網路連線。
