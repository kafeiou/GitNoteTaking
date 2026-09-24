# 任務清單：GitHub 授權續期、401 攔截自動續推與多語系 FAQ (Tasks)

## 1. OAuth State 路由與核心管理 (OAuth Core & Routing)

- [x] 1.1 在 `GitHubAuthManager.java` 重構 `startOAuthWebFlow(activity, state)` 支援傳入 `state` 參數，若未指定則預設為 `create_new`，並以單元測試驗證 State 產生與解析邏輯。
- [x] 1.2 在 `MainActivity.java` 之 `onNewIntent` 擴充 Deep Link 解析，支援辨識 `reauth_<repoId>`，自 `RemoteGitDAO` 取得儲存庫後以 Transaction 更新 Access Token，並以編譯指令驗證邏輯完整。

## 2. 401 錯誤攔截、專屬彈窗與自動續推 (Error Handling & Recovery)

- [x] 2.1 在 `MyGitUtility.java` 定義 `GIT_STATUS_AUTH_FAILED = -5`，於 `checkRemoteRepository` 捕捉 401 Unauthorized 時標記該狀態，並於 4 國語系 `strings.xml` 新增授權失效對話框相關字串。
- [x] 2.2 在 `MainActivity.java` 同步結束後加入狀態判斷，遇 `GIT_STATUS_AUTH_FAILED` 彈出「GitHub 授權已失效」引導對話框，提供【重新登入並同步】；使用者完成授權回跳後自動背景接續完成 Push，確保本地修改零遺失。
- [x] 2.3 在 `MainActivity.java` 之「建立 GitHub 筆記」清單解除已下載 Repo 點擊限制，提供「更新此筆記授權」選項。

## 3. 修改儲存庫介面整合、多語系 FAQ 文件與測試 (UI Integration, FAQ Docs & Testing)

- [x] 3.1 在 `activity_main_modify_remote.xml` 與 `ModifyRemoteGitActivity.java` 增設【透過 GitHub 網頁重新授權】按鈕（僅在 Remote URL 為 GitHub 時顯示），點擊後以 `reauth_<id>` 發起網頁授權並即時更新畫面。
- [x] 3.2 於 `docs/` 建立 4 國語系 FAQ 文件（`docs/FAQ.md`、`docs/FAQ_zh-CN.md`、`docs/FAQ_en.md`、`docs/FAQ_ja.md`），詳述 8 小時過期機制成因、App 內一鍵復原步驟與關閉過期之完整操作手冊。
- [x] 3.3 在 `app/src/test/java/inmethod/gitnotetaking/test/` 撰寫 `GitHubAuthRenewalUnitTest.java`，並執行 `./gradlew testDebugUnitTest` 確保全專案測試 100% 通過。
