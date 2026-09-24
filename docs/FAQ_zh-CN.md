# 常见问题与解答 (FAQ) - GitHub 授权与同步说明

## Q1: 为什么同步到 GitHub 时会出现“GitHub 授权过期或失败”？

### 原因说明
InMethod GitNoteTaking 支持通过 GitHub OAuth 网页登录授权。根据 GitHub 官方安全策略，若 GitHub OAuth App 启用了“**Expire user authorization tokens**（使用户授权令牌过期）”设置，所发放的 Access Token 有效期限为 **8 小时**。

当令牌超过 8 小时未刷新或过期时，GitHub 服务器会拒绝 Git 操作并返回 `HTTP 401 Unauthorized`（`not authorized`），导致无法推送（Push）或拉取（Pull）笔记。

---

## Q2: 出现授权过期时，我写好的笔记会丢失吗？该如何恢复同步？

**不会丢失任何笔记！**

本地修改的笔记已经完整保存在手机本地的 Git 仓库中。当同步遇到授权过期时，App 会启动自动保护与恢复机制：

### 恢复同步步骤：
1. **点击重新登录**：当弹出“GitHub 授权过期”对话框时，点击“**重新登录并同步**”。
2. **网页授权**：App 将自动打开浏览器前往 GitHub 授权页面，点击“**Authorize**”授权。
3. **自动继续同步**：授权成功后，App 会自动返回并**自动补推先前未完成的笔记**，无需重新编辑或手动重复点击同步。

> **提示**：您也可以随时在主界面长按该远程仓库 -> 选择“**修改**”-> 点击“**通过 GitHub 网页重新登录授权**”来手动更新授权。

---

## Q3: ［开发者／OAuth App 管理员］如何让 GitHub 授权永不过期？

如果您是该 GitHub OAuth App 的所有者或自行搭建 OAuth 服务器，可以关闭 8 小时过期限制，让授权永久有效：

1. 打开并登录 [GitHub](https://github.com/)。
2. 点击右上角个人头像 -> 进入 **Settings**（设置）。
3. 在左侧菜单最下方，点击 **Developer settings**（开发者设置）。
4. 选择 **OAuth Apps**，并点击您的 GitNoteTaking 应用程序。
5. 向下滚动找到 **Optional features** 区域中的 **Expire user authorization tokens**。
6. **取消勾选**（Uncheck）该选项，或保持 Opt-out 状态。
7. 点击 **Save changes** 保存。

> 取消勾选后，未来所有通过该 OAuth App 登录的用户，获取的 Access Token 将为永久有效（除非用户手动在 GitHub 撤销授权），再也不会遇到 8 小时过期问题。

---

## Q4: 我可以使用 GitHub 个人访问令牌（Personal Access Token, PAT）吗？

**可以！**

如果您不想使用 OAuth 网页授权，也可以使用 GitHub 的 Personal Access Token：

1. 前往 GitHub **Settings** -> **Developer settings** -> **Personal access tokens** -> **Tokens (classic)**。
2. 点击 **Generate new token (classic)**。
3. Note 填写 `GitNoteTaking`，Expiration 选择 **No expiration**（永不过期），并勾选 **repo** 权限。
4. 生成并复制令牌（格式如 `ghp_xxxx...`）。
5. 回到 GitNoteTaking App：
   - 若为新增仓库：在“克隆远程仓库”页面中，账号输入 GitHub 用户名，密码粘贴该 Personal Access Token。
   - 若为现有仓库：在仓库列表长按选择“修改”，密码栏直接粘贴新的 Token 并点击“OK”保存。
