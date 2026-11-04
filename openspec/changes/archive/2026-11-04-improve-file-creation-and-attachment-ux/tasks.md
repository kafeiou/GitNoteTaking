# Tasks: 改善建立檔案互動、自動開啟新檔與大檔案新增等待提示

- [x] 1. 在 4 國語系 `strings.xml`（`values-zh-rTW`、`values-zh-rCN`、`values-ja`、`values`）新增 `tv_adding_file`（「正在新增中...」/「追加中...」/「Adding...」）字串資源。
- [x] 2. 修改 `FileExplorerActivity.java`：
  - [x] 2.1 新增 `normalizeNewTxtFileName(String rawInput)` 靜態輔助方法，處理空白過濾、空字串防呆與 `.txt` 副檔名自動補齊及去重。
  - [x] 2.2 修改 `createNewFolder(int p_opt)`：當 `p_opt == 0` 時，使用水平 `LinearLayout` 呈現左側 `EditText`（預設 `"newfile"` 並自動全選）與右側不可編輯之 `TextView`（`".txt"`），修正 `Toast.show()`，並在建立成功後立即啟動 `ViewFileActivity`。
  - [x] 2.3 修改 `showAddExternalFileDialog(Uri)`：點擊確定後顯示 `loading_dialog`（「正在新增中...」），涵蓋檔案複製與本地 Git Commit，完成後關閉視窗並刷新列表，遠端 Push 於背景執行。
- [x] 3. 修改 `ViewFileActivity.java`：
  - [x] 3.1 抽取 `refreshAttachmentViews()` 方法以支援原地刷新底部附件列，不重啟 Activity。
  - [x] 3.2 修改 `showAddAttachmentDialog(Uri)` 與 `REQUEST_TAKE_PHOTO` 拍照存檔：點擊確定後顯示 `loading_dialog`（「正在新增中...」），涵蓋檔案複製與本地 Git Commit，完成後關閉等待視窗並呼叫 `refreshAttachmentViews()` 原地刷新附件列，遠端 Push 於背景執行。
- [x] 4. 在 `inmethod/gitnotetaking/test/FileCreationAndAttachmentUxUnitTest.java` 撰寫自動化單元測試並執行 `./gradlew testDebugUnitTest` 驗證 100% 通過。
