# attachment-details Specification

## Purpose
提供文字筆記檢視器中附件檔案的 Material 底部抽屜互動卡片，讓使用者在開啟前能清晰檢視真實檔名、檔案大小、格式類型與修改時間等中繼資料，並整合開啟、另存至 Downloads 與本地 Git 刪除提交操作。

## Requirements

### Requirement: 顯示附件詳細資訊抽屜
當使用者在文字筆記檢視畫面 (`ViewFileActivity`) 中點選或長按任何附件標籤（`附件N`）時，系統 MUST 彈出 Material 底部抽屜訊息 (`BottomSheetDialog`)，完整展示該附件檔案之中繼資料。

```mermaid
flowchart TD
    A[使用者點選或長按筆記附件標籤] --> B[彈出 Material 底部抽屜 BottomSheetDialog]
    B --> C[顯示真實檔名、圖示、大小、類型、修改時間]
    C --> D{使用者選擇操作}
    D -->|開啟檔案| E{檔案格式為 PDF?}
    E -->|是| F[啟動 ViewPdfActivity 原生預覽]
    E -->|否| G[FileProvider 發送 ACTION_VIEW 呼叫外部 App]
    G -->|未安裝對應 App| H[Toast 提示找不到可開啟的應用程式]
    D -->|另存到下載| I{Downloads 是否已存在同名檔案?}
    I -->|是| J[Toast 提示檔案已存在]
    I -->|否| K[複製檔案至 Downloads 並 Toast 提示成功]
    D -->|刪除附件| L[AlertDialog 二次確認刪除]
    L -->|取消| M[關閉對話框，不執行刪除]
    L -->|確認| N[實體刪除 _attach 檔案]
    N --> O["本地 Git Commit: User deleted attachment: <檔名> (不 Push)"]
    O --> P[關閉抽屜並動態移除附件按鈕]
    P --> Q{剩餘附件數為 0?}
    Q -->|是| R[隱藏水平附件列 scrollAttachment]
    Q -->|否| S[維持附件列顯示]
```

#### Scenario: 成功開啟附件抽屜並檢視中繼資料
- **WHEN** 使用者點選或長按筆記上方的任一 `附件N` 按鈕
- **THEN** 系統彈出底部抽屜，正確顯示該檔案對應之格式圖示、完整真實檔名、格式化之檔案大小（B/KB/MB）、人性化檔案格式（如 Word 文件、PDF 文件）與最後修改時間（YYYY-MM-DD HH:mm）

#### Scenario: 圖片與其他文件類型維持一致外觀
- **WHEN** 使用者點選的附件為圖片檔案（如 `.png`, `.jpg`）
- **THEN** 抽屜維持標準卡片排版，僅顯示圖片類型圖示與中繼資料，不額外展開即時圖片縮圖

### Requirement: 開啟附件檔案
使用者在底部抽屜內點選【開啟檔案】按鈕時，系統 MUST 根據檔案格式安全喚起對應的檢視器或外部應用程式。

#### Scenario: 點選開啟 PDF 格式附件
- **WHEN** 使用者在抽屜內對 `.pdf` 檔案點選【開啟檔案】
- **THEN** 系統關閉抽屜並直接啟動 App 內建的 `ViewPdfActivity` 進行離線閱讀

#### Scenario: 點選開啟非 PDF 格式附件
- **WHEN** 使用者在抽屜內對非 PDF 檔案點選【開啟檔案】且手機內有支援該格式之應用程式
- **THEN** 系統透過 `FileProvider` 取得安全 URI，並發送 `ACTION_VIEW` 意圖呼叫外部應用程式開啟

#### Scenario: 手機內無支援該格式之應用程式
- **WHEN** 使用者在抽屜內點選【開啟檔案】，但系統拋出 `ActivityNotFoundException`
- **THEN** 系統彈出 Toast 提示「找不到可開啟此檔案的應用程式」，不發生崩潰或無回應

### Requirement: 另存附件至下載目錄
使用者在底部抽屜內點選【另存到下載 (Downloads)】按鈕時，系統 MUST 安全將該附件複製到系統公開的 Downloads 目錄。

#### Scenario: 成功複製附件至 Downloads 資料夾
- **WHEN** 使用者點選【另存到下載 (Downloads)】且 Downloads 目錄下尚無同名檔案
- **THEN** 系統將該附件完整複製至 Downloads 目錄，並彈出 Toast 提示下載成功

#### Scenario: 下載目錄已存在同名檔案
- **WHEN** 使用者點選【另存到下載 (Downloads)】但 Downloads 目錄下已有同名檔案
- **THEN** 系統終止複製並彈出 Toast 提示「檔案已存在」與檔案名稱

### Requirement: 刪除附件與本地 Git 提交
使用者在底部抽屜內點選【刪除附件】按鈕時，系統 MUST 先跳出二次確認對話框，經確認後實體刪除本機檔案並於本地 Git 儲存庫建立英文 Commit 紀錄。

#### Scenario: 確認刪除附件
- **WHEN** 使用者在抽屜中點選【刪除附件】並在確認對話框中點選確認
- **THEN** 系統自筆記所屬的 `_attach` 資料夾中實體刪除該檔案，並以純英文訊息 `User deleted attachment: <檔名>` 建立本地 Git Commit，不主動發起遠端 Push，關閉抽屜並動態從筆記畫面中移除該 `附件N` 按鈕

#### Scenario: 取消刪除附件
- **WHEN** 使用者在刪除確認對話框中點選取消或點擊視窗外部
- **THEN** 系統關閉對話框，維持檔案完整不刪除，亦不產生任何 Git 提交

#### Scenario: 筆記所有附件刪除完畢
- **WHEN** 筆記中的最後一個附件被成功刪除
- **THEN** 系統自動隱藏筆記上方的水平附件滾動區塊 (`scrollAttachment`)，保持畫面乾淨
