# GIT文本笔记 Git Note Taking

[繁體中文](README.md) | [English](README_en.md) | [日本語](README_ja.md) | [简体中文](README_zh-CN.md)

> **软件工程师专用、麻瓜勿扰**

## 💡 特色
1. **使用 Git 版本控制机制**：完整保留修改记录与 Commit 信息。
2. **支持免费云端 GitHub 同步**：随时随地备份与多设备同步。
3. **Markdown 离线可视化与 Mermaid 流程图预览**：支持 Obsidian 双向链接 (`[[...]]`)、表格、待办清单与 Mermaid 矢量图表。
4. **完全可离线使用**：无网络环境下依然能流畅查阅与编辑笔记。
5. **全文搜索**：快速寻找历史笔记与文件内容。
6. **本地历史瘦身**：一键将本地版本库清理至单一版本（`depth = 1`），节省手机存储空间。

## 🏛️ 系统架构与自动化测试
- **[系统架构与 Mermaid 设计流程图 (Architecture)](ARCHITECTURE.md)**：详细记录分层架构、双模式生命周期与图表渲染数据流。
- **[自动化测试指南 (Testing Guide)](TESTING.md)**：单元测试与仪器测试运行说明。

## 🎯 设计理念
通过云端 GitHub 服务，可将平时记录的文件同步到 APP；可离线查看或编辑，适当时机再将文件 Push 同步回云端 GitHub。

**Git 特有优势**：每次编辑时可以写下编辑原因（Commit Message），方便事后查阅与追溯历史。

## 📖 如何使用本 APP

### 方式一：创建 GitHub 笔记（推荐）
1. 打开 APP，在右上角点击“创建”菜单最下方的 **“创建 GitHub 笔记”**。
2. 点击 **【前往生成 Token】**，系统将自动打开 GitHub 网页并为您预先勾选好所需权限（`repo` 与 `read:user`）。
3. 将“Expiration”选为 **No expiration（永不过期）**，滑至最下方点击 **Generate token** 并复制。
4. 切回 APP，系统将**自动为您填入剪贴板中的 Token**，点击【确定连接】。
5. 系统会自动列出您账号中所有以 `note` 开头的仓库（如 `note-work`, `NoteTaking`），点击即可极速克隆并开始使用！

### 方式二：自定义远程 Git / 本地笔记
1. **远程 Git**：在右上角菜单点击“克隆远程笔记”，输入 Git URL、账号与 Token 即可手动克隆。
2. **本地笔记**：在右上角菜单点击“创建本地笔记”，即可创建纯本地离线 Git 笔记库。

## 📦 Google Play 商店发布与版本更新机制 (Release & Distribution)

- **`distribution/whatsnew/`**：存放**当次发布**至 Google Play 商店的多语言发布日志（What's New / Release Notes）。每次发布新版本时覆盖此目录下的文件（字数严格限制在 500 字符内以符合 Google Play 规范）：
  - `whatsnew-zh-TW`（繁体中文）
  - `whatsnew-zh-CN`（简体中文）
  - `whatsnew-ja-JP`（日语）
  - `whatsnew-en-US`（English / 默认）
- **`CHANGELOG.md`**：保留产品完整的历史版本演进历程。

### 📌 新版本发布 Prompt 范本 (Release Prompt Template)
未来发布新版本时，直接复制以下 Prompt 给 AI 即可：
```text
请帮我发布新版本 [版本号，例如 4.002]：
1. 更新 app/build.gradle 的 versionName 与 versionCode。
2. 在 CHANGELOG.md 最上方追加新版本的完整更新记录。
3. 覆盖更新 distribution/whatsnew/ 目录下的 4 种语言 Play 商店发布日志（每篇字数小于 500 字符）。
4. 执行 ./gradlew assembleDebug 进行构建与验证。
```

## 🌐 开源信息 Open Source
- GitHub 项目：https://github.com/kafeiou/GitNoteTaking

## 📚 第三方库与资源授权
- [Eclipse JGit](https://www.eclipse.org/jgit) (version 7.7.1)
- Git Logo by Jason Long is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
- 系统要求：仅支持 Android 13 (API 33) 或更高版本

## 🤖 开发工具致谢
- 本项目使用 **Gemini CLI 1.1.22 版** 与 **OpenSpec 1.11.0** 协助开发。
