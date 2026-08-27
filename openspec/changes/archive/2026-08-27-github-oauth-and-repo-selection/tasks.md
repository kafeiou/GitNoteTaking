## 1. GitHub OAuth 基礎建設與 Deep Link 配置

- [x] 1.1 在 `AndroidManifest.xml` 中為 `MainActivity` 或 Callback Activity 配置 Deep Link Intent Filter (`gitnotetaking://oauth/github`)
- [x] 1.2 建立 `GitHubAuthManager` 處理 Custom Tabs 授權跳轉與 Token 換取邏輯
- [x] 1.3 實作 GitHub User API 與 Repos API 查詢

## 2. note* 儲存庫過濾與選取對話框

- [x] 2.1 實作 Repository 清單過濾邏輯（篩選名稱開頭為 `note`，不區分大小寫）
- [x] 2.2 實作 Repo 挑選對話框與點選後自動 Clone 流程
- [x] 2.3 實作查無 note* 儲存庫時之防呆指引對話框

## 3. 主選單 UI 與舊介面清理

- [x] 3.1 在 `res/menu/menu_main.xml` 新增「建立 GitHub 筆記」選項並配置 Octocat 圖示
- [x] 3.2 在 `MainActivity.java` 處理「建立 GitHub 筆記」點擊事件
- [x] 3.3 在 `res/layout/activity_clone_git.xml` 與 `CloneGitActivity.java` 移除舊有的 Click to Sign Up 相關元件與點擊監聽器
- [x] 3.4 新增繁中、簡中、日文、英文等各語系字串資源

## 4. 建置、驗證與測試

- [x] 4.1 執行 `./gradlew assembleDebug` 驗證專案編譯與打包無誤
- [x] 4.2 驗證 GitHub OAuth 授權登入、`note*` 儲存庫過濾、Clone 與防呆提示流程

## 5. 版本控制與變更歸檔

- [x] 5.1 執行 `git add .` 與 `git commit` 保存變更
- [x] 5.2 執行 OpenSpec 變更歸檔並自動同步主文件
