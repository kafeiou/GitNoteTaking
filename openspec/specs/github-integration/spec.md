# github-integration Specification

## Purpose

定義 InMethodGitNoteTaking 應用程式中「建立 GitHub 筆記」功能、GitHub 授權管理、`note*` 儲存庫過濾挑選、GraphQL 檔案真實 Commit 時間同步、本地端歷史瘦身 (Purge) 以及多語系 Google Play 發布規範。

## Requirements

### Requirement: 主選單建立 GitHub 筆記入口
主畫面右上角「建立」選單 MUST 包含「建立 GitHub 筆記」選項，配置 GitHub Octocat 圖示，並依序置於選單最底端（排序順序：建立本地筆記 $\rightarrow$ 下載遠端筆記 $\rightarrow$ 建立 GitHub 筆記）。點擊後觸發 GitHub 連線流程。

#### Scenario: 點選主選單之建立 GitHub 筆記
- **WHEN** 使用者在主畫面點開「建立」選單並選擇「建立 GitHub 筆記」
- **THEN** 系統開啟 GitHub 連線引導對話框

### Requirement: GitHub Token 獲取與剪貼簿自動辨識
系統 MUST 在 GitHub 連線對話框中提供「一鍵 OAuth 授權」與「PAT 手動輸入」雙軌並存機制。
1. **OAuth 一鍵授權**：提供顯著的【一鍵登入 GitHub 授權】按鈕，點擊後透過 Chrome Custom Tabs 開啟授權頁面，使用者同意授權後，系統 MUST 透過 Deep Link（`gitnotetaking://oauth/github`）自動接收回調並換取 Access Token，直接進入儲存庫挑選流程。
2. **PAT 手動輸入**：提供【前往產生 Token】按鈕自動開啟預選權限頁面，並在使用者複製切回時自動辨識剪貼簿內容填入輸入框，點擊【確定連線】後進入儲存庫挑選流程。

```mermaid
flowchart TD
    A[點擊「建立 GitHub 筆記」] --> B[彈出雙軌並存 GitHub 連線對話框]
    B -->|途徑 1: OAuth 一鍵授權| C[點擊「一鍵登入 GitHub 授權」]
    C --> D[瀏覽器開啟 GitHub 授權頁面]
    D --> E[使用者點擊 Authorize]
    E --> F[Deep Link 回跳 App: 自動換取 Access Token]
    F --> I[呼叫 GitHub API 取得 note* 儲存庫]
    B -->|途徑 2: PAT 手動模式| G[點擊「前往產生 Token」或手動輸入]
    G --> H[切回 App 自動帶入剪貼簿 Token 並點擊確定連線]
    H --> I
```

#### Scenario: 成功透過 OAuth 一鍵授權取得儲存庫
- **WHEN** 使用者在對話框點擊「一鍵登入 GitHub 授權」並在瀏覽器完成授權
- **THEN** 系統透過 Deep Link 接收授權碼、換取 Token 並自動取得儲存庫清單

#### Scenario: 成功透過 Token 取得儲存庫
- **WHEN** 使用者輸入或由剪貼簿帶入有效之 GitHub Token 並點擊「確定連線」
- **THEN** 系統透過 API 獲取使用者的 Repository 清單並進入篩選流程

### Requirement: note 前綴儲存庫過濾與選取
系統獲取使用者的 Repository 清單後，MUST 自動過濾名稱開頭為 `note`（不區分大小寫，例如 `note-work`、`NoteTaking`）之儲存庫並呈現於對話框供使用者挑選。使用者挑選後，系統 MUST 自動帶入 Repo 資訊、Username 與 Access Token 執行 JGit Clone 下載、進行時間校正並立即刷新主畫面清單。

```mermaid
flowchart TD
    A[取得 GitHub Repos 清單] --> B[過濾: repo.name 以 note 或 Note 開頭]
    B --> C{是否含有符合條件之 Repo}
    C -->|有符合之 Repo| D[顯示 note 儲存庫挑選對話框]
    D --> E[使用者點選目標 Repo]
    E --> F[自動以 depth=1 執行 JGit Clone]
    F --> G[執行 GraphQL Commit 時間同步]
    G --> H[刷新主畫面清單並進入筆記]
    C -->|查無任何符合之 Repo| I[彈出防呆提醒對話框]
    I --> J[提供前往 GitHub 建立以 note 開頭 Repo 之捷徑]
```

#### Scenario: 選擇筆記儲存庫並克隆
- **WHEN** 使用者從過濾清單中選擇一個 note 開頭之儲存庫
- **THEN** 系統以 `depth = 1` 完成 Clone，完成 GraphQL 時間校正後直接在主畫面顯示

### Requirement: GraphQL 單次批次 Commit 時間同步
在淺層下載（`depth = 1`）的前提下，首次拉取（Clone）與日常同步（Pull）MUST 僅執行快速的本地 `HEAD` 時間初始化以確保極速同步體驗；當使用者需要還原歷史真實修改時間時，系統 MUST 在主畫面長按選單提供【更新檔案真實日期】功能，先彈出效能影響提醒與可自訂回溯 Commit 數量輸入框（預設 `300` 筆），確認後透過 JGit `fetch(depth = N)` 與本地 Commit Tree Diff 比對更新各檔案 `setLastModified(epochMilli)`，並於等待對話框即時顯示進度百分比與完成數量。

#### Scenario: 筆記檔案時間校正
- **WHEN** GitHub 筆記 Clone 完成
- **THEN** 系統先以 `depth = 1` 最新 Commit 時間快速完成初始化並顯示於主畫面

#### Scenario: 長按選單手動觸發自訂 Commit 數量之檔案真實日期更新
- **WHEN** 使用者在主畫面長按遠端筆記並點選「更新檔案真實日期」，確認效能提醒並輸入回溯 Commit 數量（預設 300）
- **THEN** 系統擷取最近 N 筆 Commit 並由新到舊比對檔案異動以更新真實修改日期，期間於等待對話框即時顯示處理進度，完成後關閉對話框並提示成功

### Requirement: 本地端歷史紀錄瘦身 (Purge to Depth = 1) (實驗中)
系統 MUST 在「設定（Settings）」最下方提供【歷史紀錄瘦身 (保留單一版本) (實驗中)】功能。使用者點擊並確認後，系統 MUST 在本地離線建立 Orphan Root Commit，將本機歷史紀錄重置為單一版本（`depth = 1`）並執行垃圾回收（GC），釋放手機磁碟空間。執行過程 MUST 具備確認對話框與請稍候進度對話框防呆機制。

```mermaid
flowchart TD
    A[設定畫面點擊「歷史紀錄瘦身」] --> B[彈出第一道確認對話框]
    B -->|使用者取消| C[取消操作]
    B -->|使用者確認| D[顯示「請稍候...」轉圈視窗]
    D --> E[背景執行: 建立 Root Commit 指標]
    E --> F[背景執行: 寫入 .git/shallow]
    F --> G[背景執行: 清理 reflogs 與執行 GC]
    G --> H[關閉請稍候視窗，彈出完成 Toast]
```

#### Scenario: 執行本地歷史瘦身
- **WHEN** 使用者在設定中點擊歷史瘦身並確認
- **THEN** 系統在背景將所有本地儲存庫瘦身為單一版本並釋放空間

### Requirement: 多語系支援與 Google Play 發布標準
所有介面字串 MUST 支援繁體中文（台灣）、繁體中文（香港）、簡體中文、日文、英文 5 國語言。每次版本升級時 MUST 維護 `CHANGELOG.md` 並同步覆蓋 `distribution/whatsnew/` 下之 5 國語系 Play 商店發布檔案（字數均限制在 500 字元內）。

#### Scenario: 多語系與發布日誌同步
- **WHEN** 升級應用程式版本
- **THEN** 系統提供 5 國語系字串支援並產出合規之發布日誌

### Requirement: OAuth State 路由分流與筆記重新綁定
系統在發起 GitHub OAuth 授權流程時，MUST 支援在授權網址中帶入 `state` 參數以記錄發起情境；在透過 Deep Link 接收授權碼時，MUST 依據回傳之 `state` 精準分流處理。

```mermaid
flowchart TD
    A[發起 GitHub OAuth Web 授權] --> B{發起情境判斷}
    B -->|建立或挑選新筆記| C["帶入 state = create_new"]
    B -->|特定筆記重新授權| D["帶入 state = reauth_&lt;repoId&gt;"]
    C --> E[瀏覽器 GitHub 授權頁面]
    D --> E
    E --> F["Deep Link 回跳 App: gitnotetaking://oauth/github?code=...&state=..."]
    F --> G{檢查 state 標籤}
    G -->|create_new| H[換取 Token 並開啟儲存庫挑選清單]
    G -->|reauth_&lt;repoId&gt;| I[換取 Token 並以 Transaction 更新該筆記 PWD]
    I --> J[檢查是否有待處理之未同步 Push]
    J -->|有| K[背景自動重試 Push 並通知同步成功]
    J -->|無| L[彈出 Toast 提示授權更新成功]
```

#### Scenario: 透過 Deep Link 接收帶有特定儲存庫標記之 state
- **WHEN** 使用者針對 ID 為 12 的儲存庫點選重新授權並在瀏覽器完成授權
- **THEN** 系統透過 Deep Link 接收到 `state=reauth_12`，自動將新換發之 Token 寫入 ID 為 12 之 `REMOTE_GIT` 記錄，並提示授權更新成功

#### Scenario: 建立新筆記時之預設 state 處理
- **WHEN** 使用者自主畫面點選「建立 GitHub 筆記」並完成授權
- **THEN** 系統透過 Deep Link 接收到 `state=create_new`，保持原有行為，列出使用者的 `note*` 儲存庫以供挑選下載

### Requirement: 401 授權失效主動攔截與斷點自動續推
當筆記執行 Git Push 或 Pull 遭遇遠端 GitHub 回傳 401 Unauthorized（`TransportException: not authorized`）時，系統 MUST 識別該狀態為 `GIT_STATUS_AUTH_FAILED`，並於介面彈出專屬授權過期引導對話框，引導使用者完成一鍵重新登入並自動背景續推未同步之修改。

```mermaid
flowchart TD
    A[使用者點選同步遠端或儲存觸發 Push] --> B[JGit 執行 Remote 連線]
    B --> C{是否遭遇 401 not authorized?}
    C -->|否，成功| D[顯示同步成功]
    C -->|否，其他錯誤| E[顯示一般同步失敗原因]
    C -->|是| F[標記狀態為 GIT_STATUS_AUTH_FAILED]
    F --> G[彈出「GitHub 授權已失效」專屬對話框]
    G --> H{使用者選擇}
    H -->|稍後再說| I[關閉對話框，保留手機端未推送之本地 Commit]
    H -->|一鍵重新登入| J["開啟 CustomTabs 進行 OAuth 授權 (state=reauth_&lt;id&gt;)"]
    J --> K[授權成功回跳 App 並自動寫入新 Token]
    K --> L[自動重新觸發剛才中斷之 Push]
    L --> M[完成推送，筆記修改 100% 保全]
```

#### Scenario: 同步遇到 401 拋出專屬授權過期對話框
- **WHEN** 筆記同步因 GitHub Token 過期導致遠端回傳 401 錯誤
- **THEN** 系統阻擋一般無效 Toast，改為彈出「GitHub 授權已失效」對話框，清楚說明可能是 8 小時授權過期，並提供【重新登入並同步】按鈕

#### Scenario: 重新授權成功後自動續推未同步筆記
- **WHEN** 使用者在 401 對話框中點選【重新登入並同步】並完成網頁授權回跳
- **THEN** 系統自動更新該筆記資料庫認證資訊，並在背景自動接續執行原本失敗之 Push 操作，推送成功後通知使用者，本地修改完全無損

#### Scenario: 使用者取消重新授權
- **WHEN** 使用者在 401 授權過期對話框中點選【取消】或【稍後】
- **THEN** 系統安全關閉對話框，維持本地 Git Commit 紀錄不被覆蓋或遺失

### Requirement: 儲存庫修改畫面之 GitHub 網頁重新授權入口
在「修改儲存庫」畫面 (`ModifyRemoteGitActivity`) 中，當目標儲存庫為 GitHub 遠端 URL 時，系統 MUST 提供【透過 GitHub 網頁重新授權】按鈕，供使用者無需手動複製貼上任何 Token 即可一鍵完成憑證刷新。

#### Scenario: 點擊修改儲存庫中的 GitHub 重新授權按鈕
- **WHEN** 使用者開啟任一 GitHub 筆記的修改畫面並點擊【透過 GitHub 網頁重新授權】
- **THEN** 系統以該儲存庫之 ID 啟動 Web OAuth 流程，完成後自動更新該畫面與資料庫中的使用者帳號與密碼欄位

### Requirement: 建立筆記清單支援已下載筆記之授權更新
在「建立 GitHub 筆記」之儲存庫挑選清單中，對於標記為【已下載】的儲存庫，系統 MUST 允許使用者點擊，並彈出更新確認對話框。

#### Scenario: 點擊已下載之筆記更新授權
- **WHEN** 使用者在儲存庫清單中點擊已被標記為已下載的筆記
- **THEN** 系統彈出對話框詢問「此筆記已在手機中，是否要為其更新授權？」，使用者確認後立即更新該筆記之 Access Token

### Requirement: 多語系 GitHub 授權設定與排錯指引 (FAQ)
專案 MUST 於 `docs/` 目錄建立繁體中文、簡體中文、英文與日文 4 國語系之 FAQ 指引文件，詳細記錄 GitHub OAuth 8 小時過期政策之背景成因、App 內一鍵復原步驟，以及 OAuth App 建立者關閉 "Expire user authorization tokens" 之圖文步驟。

#### Scenario: 提供 4 國語系 FAQ 說明文件
- **WHEN** 開發者或使用者查閱專案說明
- **THEN** 能在 `docs/FAQ.md`、`docs/FAQ_zh-CN.md`、`docs/FAQ_en.md` 與 `docs/FAQ_ja.md` 取得完整排錯說明與 OAuth App 永不過期之設定指引

### Requirement: 資料庫明確記錄 GitHub 網頁認證類型與無損遷移
系統 MUST 在本地 SQLite `REMOTE_GIT` 資料表中維護 `AUTH_TYPE` 欄位，明確區分該儲存庫是否透過「GitHub 網頁認證（`WEB_OAUTH`）」或「一般密碼/個人存取權杖（`PASSWORD`）」建立；於資料庫版本升級時 MUST 採用 `ALTER TABLE` 無損升級，並自動將既有非 PAT 之 GitHub 儲存庫標記為 `WEB_OAUTH`。

#### Scenario: 資料庫升級自動保留資料並標記既有網頁認證儲存庫
- **WHEN** 使用者將 App 更新至包含資料庫 v3 之版本並啟動應用程式
- **THEN** 系統透過 `ALTER TABLE` 新增 `AUTH_TYPE` 欄位而不刪除任何既有儲存庫紀錄，並將既有 `github.com` 且非 `ghp_` / `github_pat_` 開頭之儲存庫自動標記為 `WEB_OAUTH`

#### Scenario: 透過網頁 OAuth 建立或重新授權時標記為 WEB_OAUTH
- **WHEN** 使用者透過 GitHub 網頁 OAuth 建立新筆記或重新取得授權
- **THEN** 系統將該筆 `RemoteGit` 紀錄的 `auth_type` 設為 `WEB_OAUTH` 並寫入資料庫

### Requirement: 進入筆記與編輯存檔時主動檢查網頁認證過期並引導重獲授權
當使用者點擊進入筆記目錄（`FileExplorerActivity`）或於筆記內編輯存檔觸發同步時，系統 MUST 主動檢查該儲存庫是否為 GitHub 網頁認證且 Token 已過期（401 Unauthorized）；若已過期，系統 MUST 立即在當前畫面彈出對話框主動告知並引導重新取得網頁認證，且於授權完成後自動返回原筆記目錄完成同步。

```mermaid
flowchart TD
    A[使用者點擊進入筆記目錄 FileExplorerActivity] --> B{是否為 GitHub 儲存庫?}
    B -->|否| C[載入本地目錄列表]
    B -->|是| D[背景主動檢查 GitHub API Token 有效性與遠端同步]
    D --> E{Token 是否 401 過期?}
    E -->|未過期 200 OK| F[正常執行 Pull / Push 雙向同步]
    E -->|已過期 401| G[更新 DB 狀態為 GIT_STATUS_AUTH_FAILED]
    G --> H{本瀏覽會話是否已點選稍後且無新修改?}
    H -->|是| I[不重複彈窗打擾，允許離線閱讀]
    H -->|否，首次進入或剛存檔修改| J[在當前畫面主動彈出「GitHub 授權已失效」對話框]
    J --> K{使用者選擇}
    K -->|取消 / 稍後| L[標記本會話已提示，保留本地修改]
    K -->|重新登入並同步| M[記錄原目錄上下文並開啟 GitHub 網頁 OAuth]
    M --> N[授權成功回跳更新 Token 與 WEB_OAUTH 標記]
    N --> O[自動返回原 FileExplorerActivity 並完成同步刷新]
```

#### Scenario: 點擊進入已過期的網頁認證筆記時主動彈窗
- **WHEN** 使用者在主畫面點擊進入一個使用 GitHub 網頁認證（`WEB_OAUTH`）的筆記目錄，且該 Token 已過期（回傳 401）
- **THEN** 系統即使在 Public 儲存庫或自動同步冷卻期間，仍主動偵測出 401 過期並在 `FileExplorerActivity` 彈出「GitHub 授權已失效」對話框引導重新授權

#### Scenario: 點選稍後閱讀文章不重複打擾、存檔修改時再次提醒
- **WHEN** 使用者在進入筆記目錄時對過期對話框點選「取消」，接著點開文章僅閱讀後按返回回到目錄
- **THEN** 系統在同一次目錄瀏覽會話中不再重複彈窗；但若使用者編輯文章並存檔（觸發推送失敗）或退回主畫面重新進入該筆記，系統立即再次彈出重新授權提醒

#### Scenario: 在筆記目錄完成重新授權後自動回到原目錄
- **WHEN** 使用者在 `FileExplorerActivity` 的過期提醒視窗中點擊【重新登入並同步】並完成網頁授權
- **THEN** 系統自動更新 Token、執行同步，並自動跳轉回原本正在瀏覽的 `FileExplorerActivity` 目錄畫面

### Requirement: 主畫面清單顯示授權過期狀態提示
當儲存庫狀態為 `GIT_STATUS_AUTH_FAILED` (-5) 時，主畫面（`MainActivity` / `GitList`）MUST 以醒目警告色標示該儲存庫為授權已過期狀態。

#### Scenario: 主畫面標示授權過期之筆記
- **WHEN** 某個 GitHub 筆記被偵測到認證過期（狀態碼 `-5`）且使用者查看主畫面清單
- **THEN** 該筆記項目以醒目顏色顯示名稱並附加「(授權已過期)」提示文字

### Requirement: 首次同步 (Clone) 與檔案真實日期還原即時進度回報
系統在執行首次下載（Clone `depth = 1`）與手動更新檔案真實日期時，MUST 移除固定計時輪播文字，改透過 JGit `ProgressMonitor` 與 Commit 比對回呼，即時於等待對話框（及主畫面複製中狀態）呈現真實同步進度：
1. **下載資料階段 (`Receiving objects`)**：顯示進度百分比與物件進度（`已完成/總物件數`），不顯示不準確之 `0 B` 容量。
2. **解析差異階段 (`Resolving deltas`)**：顯示進度百分比與完成數量（`已完成/總數`）。
3. **寫入檔案階段 (`Checking out files`)**：顯示進度百分比與檔案數量（`已完成/總檔案數`）。
4. **真實日期還原階段 (`SyncFileDates`)**：於長按選單觸發時，顯示比對最近 N 筆 Commit 還原檔案真實修改日期之進度百分比與檔案數量（`已完成/總檔案數`）。

```mermaid
flowchart TD
    A[開始首次同步 Clone depth=1] --> B[顯示等待對話框: 正在準備同步資料...]
    B --> C[JGit CloneCommand 掛載自訂 ProgressMonitor]
    C --> D["階段 1 (Receiving objects):<br/>正在下載筆記資料... X% (completed/total)"]
    D --> E["階段 2 (Resolving deltas):<br/>正在解析資料差異... X% (completed/total)"]
    E --> F["階段 3 (Checking out files):<br/>正在寫入筆記檔案... X% (completed/total)"]
    F --> G[完成本地 HEAD 時間快速初始化並關閉對話框]
    G --> H[使用者於主畫面長按選單點選「更新檔案真實日期」]
    H --> I["彈出效能提醒與回溯 Commit 數量輸入框 (預設 300)"]
    I --> J["JGit fetch(depth=N) + 逐筆 Commit Tree Diff 比對:<br/>正在還原檔案真實日期... X% (updated/totalFiles)"]
    J --> K[完成真實日期還原並彈出完成提示]
```

#### Scenario: 首次同步大容量儲存庫時即時顯示下載物件進度
- **WHEN** 使用者首次下載包含大量筆記與附件的遠端儲存庫
- **THEN** 等待對話框即時更新顯示目前下載百分比與已接收物件數（例如 `正在下載筆記資料... 42% (210/500)`），不再以固定秒數盲目輪播文字或顯示 `0 B`

#### Scenario: 長按觸發檔案真實日期還原時顯示即時進度
- **WHEN** 使用者於主畫面長按選單點選「更新檔案真實日期」並確認回溯 Commit 數量（預設 50）
- **THEN** 等待對話框即時顯示遠端歷史拉取進度與「正在還原檔案真實日期... X% (已處理檔案數/總檔案數)」，直到全數還原完畢才關閉對話框

### Requirement: 筆記本過期狀態顯示引導與字型縮放
當儲存庫標記為 `GIT_STATUS_AUTH_FAILED` 時，主畫面筆記本列表 MUST 顯示 `(授權已過期，請點選更新)`（多語系），並透過 `RelativeSizeSpan(0.65f)` 縮放後綴狀態文字大小，且列表卡片高度 MUST 設為 `wrap_content` 自適應以防止長名稱換行裁切；點選該筆記本時 MUST 立即彈出重新授權對話框。

