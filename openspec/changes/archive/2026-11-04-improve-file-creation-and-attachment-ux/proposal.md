# Proposal: 改善建立檔案互動、自動開啟新檔與大檔案新增等待提示

## Why
1. **建立檔案副檔名易誤改**：目前進入筆記本點選「建立檔案」時，開窗的輸入框預設填入 `NewFile.txt`，使用者改名時容易誤刪或改壞 `.txt` 副檔名。
2. **建立檔案後需二次點擊**：建立空白 `.txt` 檔案成功後，目前僅停留在目錄列表，使用者還必須在列表中尋找並點擊該檔案才能檢視或編輯。
3. **新增大檔案時無回應感**：在筆記內「新增附件」、「拍照存檔」或目錄頁「加入檔案」時，若遇到體積較大的文件或照片，背景執行「複製檔案 + 本地 Git Commit + 遠端 Push」期間完全無任何等待提示，且筆記頁面在全部完成後才透過 `finish() + startActivity()` 重啟刷新，造成使用者誤以為 App 當機。

## What Changes
1. **建立檔案視窗固定 `.txt` 副檔名**：
   - `FileExplorerActivity.createNewFolder(0)` 開窗改為水平排列：左側 `EditText` 預設填入小寫 `"newfile"` 並自動全選文字（`selectAll()`），右側 `TextView` 固定顯示不可修改的 `".txt"`。
   - 新增 `FileExplorerActivity.normalizeNewTxtFileName(String)`：自動 `.trim()` 過濾前後空白，若使用者重複輸入 `.txt` 則自動去重為單一 `.txt`；空字串則阻擋並提示「名稱不可為空」。
   - 修正 `FileExplorerActivity.createNewFolder` 既有 `Toast.makeText(...)` 漏寫 `.show()` 的錯誤。
2. **建立檔案成功後立即開啟**：
   - 於 `FileExplorerActivity.createNewFolder(0)` 成功建立新檔案並刷新目錄列表後，立即啟動 `ViewFileActivity` 開啟該檔案（維持目前的預設開啟狀態）。
3. **新增文件/附件顯示「正在新增中...」等待視窗與原地刷新**：
   - 新增 4 國語系字串 `tv_adding_file`（繁中/簡中：`正在新增中...`、日文：`追加中...`、英文：`Adding...`）。
   - 在 `ViewFileActivity`（「新增附件」与「拍照存檔」）與 `FileExplorerActivity`（「加入檔案」）按下確定後，立即彈出不可誤觸取消的 `R.layout.loading_dialog`（「正在新增中...」），涵蓋「① 複製檔案 + ② 本地 Git Commit」階段。
   - 完成本地複製與 Git Commit 後立即安全關閉等待視窗；在 `ViewFileActivity` 中改為呼叫 `refreshAttachmentViews()` 原地刷新底部附件列（不重啟 Activity、不閃爍、不遺失未存文字），並將「③ 遠端網路 Push」留在背景執行緒非同步完成。

## Capabilities

### Modified Capabilities
- `input-sanitization`: 新增建立檔案固定 `.txt` 副檔名、預設小寫 `"newfile"` 全選與檔名正規化防呆規範，以及建立後自動開啟檔案規範。
- `attachment-details`: 新增新增附件、拍照存檔與外部加入檔案時的「正在新增中...」等待視窗與原地刷新附件列規範。

## Impact
- `app/src/main/java/inmethod/gitnotetaking/FileExplorerActivity.java`
- `app/src/main/java/inmethod/gitnotetaking/ViewFileActivity.java`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`
- `app/src/main/res/values-ja/strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/inmethod/gitnotetaking/test/FileCreationAndAttachmentUxUnitTest.java`
