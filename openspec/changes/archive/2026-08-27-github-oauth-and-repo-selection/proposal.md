# Proposal: 建立 GitHub 筆記與 OAuth 授權整合 (GitHub OAuth & Note Repo Selection)

## Summary

在主畫面「建立」選單中新增「建立 GitHub 筆記」專屬選項（帶有 Octocat 圖示）。透過 GitHub OAuth 2.0 Web Flow 提供一鍵式網頁/GitHub App 授權登入，自動取得 Access Token 與使用者名稱，並自動查詢與篩選名稱開頭為 `note` (大小寫不拘) 之儲存庫供使用者一鍵選取克隆；若無符合條件之 Repo 則提供友善防呆指引。同時清理原有 `CloneGitActivity` 左上角過時之 Click to Sign Up 介面。

## Motivation

目前使用者若要同步 GitHub 遠端儲存庫，必須先手動前往 GitHub 網站建立 Personal Access Token (PAT)，再手動複製貼上 Git URL、帳號名稱與長串 Token 至輸入框中，對非技術使用者（麻瓜）門檻極高且容易出錯。透過 GitHub OAuth 一鍵授權與 `note*` 儲存庫自動篩選，大幅提升使用者體驗與現代化操作流暢度。

## Capabilities

### New Capabilities
- `github-integration`: 包含主選單「建立 GitHub 筆記」入口、GitHub OAuth 2.0 授權登入流程、`note*` 前綴儲存庫過濾與選取、無符合儲存庫之防呆指引對話框，以及移除舊有 Click to Sign Up 介面。

### Modified Capabilities
- 無（本變更為全新功能模組）。

## Impact

- **UI / 選單**：
  - `res/menu/menu_main.xml`：新增「建立 GitHub 筆記」選單項與 Octocat 圖示。
  - `res/layout/activity_clone_git.xml`：移除 `searchButton` 與 `tvRegistration`。
- **認證與網路**：
  - `AndroidManifest.xml`：註冊 OAuth 回調 Deep Link Scheme（`gitnotetaking://oauth/github`）。
  - 新增 GitHub OAuth 授權與 API 呼叫工具模組。
- **相容性**：
  - 取得之 Token 與使用者名稱無縫對接現有 `RemoteGit` 資料庫與 JGit 核心（`clone`, `push`, `pull`）。
