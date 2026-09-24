# 常見問題與解答 (FAQ) - GitHub 授權與同步說明

## Q1: 為什麼同步到 GitHub 時會出現「GitHub 授權過期或失敗」？

### 原因說明
InMethod GitNoteTaking 支援透過 GitHub OAuth 網頁登入授權。根據 GitHub 官方安全策略，若 GitHub OAuth App 啟用了「**Expire user authorization tokens**（使使用者授權權杖過期）」設定，所核發的 Access Token 有效期限為 **8 小時**。

當權杖超過 8 小時未更新或過期時，GitHub 伺服器會拒絕 Git 操作並回傳 `HTTP 401 Unauthorized`（`not authorized`），導致無法推送（Push）或拉取（Pull）筆記。

---

## Q2: 出現授權過期時，我辛苦寫的筆記會遺失嗎？該如何恢復同步？

**不會遺失任何筆記！**

本地修改的筆記已經完整儲存在手機本地的 Git 儲存庫中。當同步遇到授權過期時，App 會啟動自動保護與恢復機制：

### 恢復同步步驟：
1. **點擊重新登入**：當彈出「GitHub 授權過期」對話框時，點擊「**重新登入並同步**」。
2. **網頁授權**：App 將自動開啟瀏覽器前往 GitHub 授權頁面，點擊「**Authorize**」授權。
3. **自動接續同步**：授權成功後，App 會自動返回並**自動補推先前未完成的筆記**，無需重新編輯或手動重複點擊同步。

> **提示**：您也可以隨時在主畫面長按該遠端倉庫 -> 選擇「**修改**」-> 點擊「**透過 GitHub 網頁重新登入授權**」來手動更新授權。

---

## Q3: ［開發者／OAuth App 管理者］如何讓 GitHub 授權永不過期？

如果您是該 GitHub OAuth App 的擁有者或自行架設 OAuth 伺服器，可以關閉 8 小時過期限制，讓授權永久有效：

1. 開啟並登入 [GitHub](https://github.com/)。
2. 點擊右上角個人頭像 -> 進入 **Settings**（設定）。
3. 在左側選單最下方，點擊 **Developer settings**（開發人員設定）。
4. 選擇 **OAuth Apps**，並點擊您的 GitNoteTaking 應用程式。
5. 向下滾動找到 **Optional features** 區塊中的 **Expire user authorization tokens**。
6. **取消勾選**（Uncheck）該選項，或保持 Opt-out 狀態。
7. 點擊 **Save changes** 儲存。

> 取消勾選後，未來所有透過該 OAuth App 登入的使用者，取得的 Access Token 將為永久有效（除非使用者手動在 GitHub 取消授權），再也不會遇到 8 小時過期問題。

---

## Q4: 我可以使用 GitHub 個人存取權杖（Personal Access Token, PAT）嗎？

**可以！**

如果您不想使用 OAuth 網頁授權，也可以使用 GitHub 的 Personal Access Token：

1. 前往 GitHub **Settings** -> **Developer settings** -> **Personal access tokens** -> **Tokens (classic)**。
2. 點擊 **Generate new token (classic)**。
3. Note 填寫 `GitNoteTaking`，Expiration 選擇 **No expiration**（永不過期），並勾選 **repo** 權限。
4. 生成並複製權杖（格式如 `ghp_xxxx...`）。
5. 回到 GitNoteTaking App：
   - 若為新增倉庫：在「複製遠端倉庫」頁面中，帳號輸入 GitHub 用戶名，密碼貼上該 Personal Access Token。
   - 若為現有倉庫：在倉庫列表長按選擇「修改」，密碼欄位直接貼上新的 Token 並點擊「OK」儲存。
