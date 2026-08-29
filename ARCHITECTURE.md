# InMethodGitNoteTaking 系統架構設計文件 (System Architecture)

本文件詳細記錄 InMethodGitNoteTaking（GIT文字筆記）的整體系統架構、離線 Markdown / Mermaid 渲染子系統、Git 版本控制資料流以及雙模式切換流程。此文件同時作為 App 內 Markdown 與 Mermaid 圖表渲染之標準測試檔案。

---

## 🏛️ 1. 系統整體分層架構 (Layered Architecture)

應用程式採用模組化分層架構，將 UI 互動、離線渲染、Git 核心服務與檔案儲存解耦：

```mermaid
flowchart TD
    subgraph UI_Layer["📱 使用者介面層 (UI Layer)"]
        MainActivity["MainActivity (專案與筆記清單)"]
        ViewFileActivity["ViewFileActivity (預覽 / 編輯模式)"]
        FileExplorerActivity["FileExplorerActivity (檔案總管)"]
        CustomPreferenceFragment["CustomPreferenceFragment (多語系與偏好設定)"]
    end

    subgraph Core_Engines["⚙️ 核心處理與渲染引擎 (Core Engines)"]
        MarkdownEngine["Markdown & Mermaid 離線渲染器 (WebView + marked.js + mermaid.js)"]
        GitUtility["MyGitUtility & Eclipse JGit 核心 (Commit / Push / Pull / Clone)"]
        PermissionHelper["PermissionHelper (Android 14/15 權限管理)"]
    end

    subgraph Storage_Layer["💾 資料儲存與版本控制 (Storage Layer)"]
        LocalGitRepo["本機 Git 倉庫 (Local Repositories)"]
        AttachmentsDir["筆記附件目錄 (Photo / Attachments *_attach)"]
        SharedPreferences["使用者設定 (App Preferences & Locales)"]
        RemoteGit["遠端 Git 服務 (GitHub / GitLab / 自建伺服器)"]
    end

    MainActivity --> ViewFileActivity
    MainActivity --> FileExplorerActivity
    MainActivity --> CustomPreferenceFragment
    
    ViewFileActivity --> MarkdownEngine
    ViewFileActivity --> GitUtility
    FileExplorerActivity --> GitUtility
    
    GitUtility --> LocalGitRepo
    ViewFileActivity --> AttachmentsDir
    LocalGitRepo -.->|SSH / HTTPS 同步| RemoteGit
```

---

## 🔄 2. Markdown 預覽與編輯雙模式資料流 (Dual-Mode Lifecycle)

當使用者開啟副檔名為 `.md` 或 `.markdown` 之筆記時，系統將自動啟動視覺化預覽流水線：

```mermaid
sequenceDiagram
    participant User as 使用者 (User)
    participant Activity as ViewFileActivity
    participant WebView as WebView (preview.html)
    participant Parser as marked.js & mermaid.js
    participant Git as Eclipse JGit

    User->>Activity: 點擊開啟 .md 筆記
    Activity->>Activity: 判斷副檔名為 .md
    Activity->>WebView: 載入 preview.html & 傳遞 Markdown 原始文字
    WebView->>Parser: 解析 Markdown 語法與 Mermaid 流程圖
    Parser-->>WebView: 繪製 HTML 排版與 SVG 向量圖表
    WebView-->>User: 呈現視覺化美化排版 (預覽模式)

    Note over User,Activity: 使用者欲修改內容
    User->>Activity: 點擊右上角 ✏️ 編輯按鈕
    Activity->>Activity: 切換至純文字編輯器 (EditText)
    User->>Activity: 編輯修改筆記內容
    User->>Activity: 點擊 儲存 或 👁️ 預覽按鈕
    Activity->>Git: 自動本機 Commit & 背景 Push
    Activity->>WebView: 重新載入最新文字
    Parser-->>WebView: 即時重新渲染 SVG 圖表
    WebView-->>User: 呈現最新預覽畫面
```

---

## 🧭 3. 雙向筆記跳轉與連結路由 (Wikilink & Link Navigation)

支援 Obsidian 雙向連結語法（`[[筆記名]]` 或 `[[筆記名|別名]]`）與外部網址跳轉：

```mermaid
flowchart LR
    A[使用者點擊預覽畫面連結] --> B{URL 協議判斷}
    B -->|http:// 或 https://| C[呼叫系統預設瀏覽器開啟網頁]
    B -->|wikilink:Scheme| D[Obsidian 雙向連結解析]
    B -->|.md 相對路徑| E[定位相對路徑檔案]
    
    D --> F{在本地 Git 倉庫中尋找檔案}
    E --> F
    F -->|檔案存在| G[在新畫面開啟 ViewFileActivity 檢視筆記]
    F -->|檔案不存在| H[彈出提示找不到筆記]
```

---

## 📊 4. 模組功能與語系分佈範例 (Diagram Feature Showcase)

### 各模組核心職責圓餅圖：

```mermaid
pie title 系統核心模組職責分佈
    "Git 版本控制 (JGit)" : 40
    "Markdown & Mermaid 預覽" : 25
    "多語系即時切換系統" : 15
    "相片與附件管理" : 10
    "檔案瀏覽與管理" : 10
```

---

## 🛠️ 相關技術規格與測試

- **測試指南**：請參閱 [`TESTING.md`](TESTING.md) 了解單元測試與自動化測試套件。
- **功能規範**：請參閱 `openspec/specs/` 了解各項功能變更與歷史規格。
