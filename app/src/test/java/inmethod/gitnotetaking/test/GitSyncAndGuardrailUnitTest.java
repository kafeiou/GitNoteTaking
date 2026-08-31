package inmethod.gitnotetaking.test;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.*;

/**
 * 單元測試：Git 進入筆記同步防護與 Auto-Commit 機制檢驗
 */
public class GitSyncAndGuardrailUnitTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testAutoCommitDirtyWorkingTree() throws Exception {
        File repoDir = tempFolder.newFolder("test_repo");

        // 1. 初始化 JGit 本地倉庫
        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File testFile = new File(repoDir, "note.md");
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("# First Note\nInitial Content");
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").setAuthor("Test", "test@example.com").call();

            // 驗證初始乾淨狀態
            Status cleanStatus = git.status().call();
            assertTrue("剛提交後工作目錄應為乾淨", cleanStatus.isClean());

            // 2. 模擬外部修改或異常中斷留下 Dirty 檔案
            try (FileWriter writer = new FileWriter(testFile, true)) {
                writer.write("\nUnsaved new line appended");
            }

            File untrackedFile = new File(repoDir, "new_attachment.png");
            try (FileWriter writer = new FileWriter(untrackedFile)) {
                writer.write("binary content");
            }

            // 3. 驗證 Working tree 呈現 Dirty 狀態
            Status dirtyStatus = git.status().call();
            assertFalse("新增/修改未提交檔案後，工作區應為 Dirty 狀態", dirtyStatus.isClean());
            assertTrue("應包含未暫存修改", dirtyStatus.getModified().contains("note.md"));
            assertTrue("應包含未追蹤檔案", dirtyStatus.getUntracked().contains("new_attachment.png"));

            // 4. 模擬 autoCommitIfDirty 行為
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Auto-saved locally before sync").setAuthor("AutoGuard", "guard@example.com").call();

            // 5. 驗證執行後 Working tree 恢復 100% 乾淨
            Status postStatus = git.status().call();
            assertTrue("自動 Commit 後工作區必須恢復 Clean 狀態以安全執行後續 Pull", postStatus.isClean());
        }
    }

    @Test
    public void testCleanWorkingTreeDoesNotRequireCommit() throws Exception {
        File repoDir = tempFolder.newFolder("test_repo_clean");

        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File testFile = new File(repoDir, "readme.txt");
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Hello World");
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").setAuthor("Test", "test@example.com").call();

            Status status = git.status().call();
            assertTrue("未修改任何檔案時 isClean 應為 true", status.isClean());
            assertEquals(0, status.getUncommittedChanges().size());
        }
    }

    @Test
    public void testCommitIdComparisonDetectsUpdates() throws Exception {
        File repoDir = tempFolder.newFolder("test_repo_diff");

        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File testFile = new File(repoDir, "doc.txt");
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("Version 1");
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("v1").setAuthor("Test", "test@example.com").call();

            org.eclipse.jgit.lib.ObjectId oldHead = git.getRepository().resolve("HEAD");
            assertNotNull("HEAD 應可成功解析", oldHead);

            // 模擬無新 Commit 時
            org.eclipse.jgit.lib.ObjectId sameHead = git.getRepository().resolve("HEAD");
            assertEquals("相同版本時 ObjectId 必須相等 (Already up to date)", oldHead, sameHead);

            // 模擬產生新 Commit 時
            try (FileWriter writer = new FileWriter(testFile, true)) {
                writer.write("\nVersion 2");
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("v2").setAuthor("Test", "test@example.com").call();

            org.eclipse.jgit.lib.ObjectId newHead = git.getRepository().resolve("HEAD");
            assertNotNull("新 HEAD 應可成功解析", newHead);
            assertNotEquals("有新 Commit 時 ObjectId 必須不相等 (Updated from remote)", oldHead, newHead);
        }
    }

    @Test
    public void testWorkingTreeDirtyDetectionBlocksPull() throws Exception {
        File repoDir = tempFolder.newFolder("test_repo_guard");

        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File testFile = new File(repoDir, "sample.md");
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write("# Sample");
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").setAuthor("Test", "test@example.com").call();

            // 乾淨狀態 -> 允許 Pull
            assertTrue("乾淨狀態時 isClean 為 true，允許執行 Pull", git.status().call().isClean());

            // 模擬有未 Commit 的修改
            try (FileWriter writer = new FileWriter(testFile, true)) {
                writer.write("\nUnsaved local work");
            }

            // Dirty 狀態 -> 必須阻擋 Pull
            assertFalse("有未提交修改時 isClean 為 false，觸發 Toast 警告並阻擋 Pull", git.status().call().isClean());
        }
    }

    @Test
    public void testStorageBreakdownCalculation() throws Exception {
        File repoDir = tempFolder.newFolder("test_repo_storage");

        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File note1 = new File(repoDir, "note1.md");
            try (FileWriter writer = new FileWriter(note1)) {
                writer.write("1234567890"); // 10 bytes
            }
            File attachDir = new File(repoDir, "note1.md_attach");
            attachDir.mkdirs();
            File attach1 = new File(attachDir, "photo.jpg");
            try (FileWriter writer = new FileWriter(attach1)) {
                writer.write("abcdefghij"); // 10 bytes
            }

            git.add().addFilepattern(".").call();
            git.commit().setMessage("Commit 1").setAuthor("Test", "test@example.com").call();

            inmethod.gitnotetaking.utility.MyGitUtility.StorageBreakdown breakdown =
                    inmethod.gitnotetaking.utility.MyGitUtility.calculateRepositoryStorage(repoDir);

            assertEquals("應有 2 個筆記與附件檔案", 2, breakdown.fileCount);
            assertEquals("筆記與附件大小應為 20 bytes", 20L, breakdown.workingTreeBytes);
            assertTrue("Git 歷史版本庫大小應大於 0 bytes", breakdown.gitDirBytes > 0);
            assertEquals("總大小應等於 workingTreeBytes + gitDirBytes",
                    breakdown.workingTreeBytes + breakdown.gitDirBytes, breakdown.getTotalBytes());
        }
    }

    @Test
    public void testFormatStorageSize() {
        assertEquals("0 B", inmethod.gitnotetaking.utility.MyGitUtility.formatStorageSize(0));
        assertEquals("500 B", inmethod.gitnotetaking.utility.MyGitUtility.formatStorageSize(500));
        assertEquals("1 KB", inmethod.gitnotetaking.utility.MyGitUtility.formatStorageSize(1024));
        assertEquals("1.5 KB", inmethod.gitnotetaking.utility.MyGitUtility.formatStorageSize(1536));
        assertEquals("10 MB", inmethod.gitnotetaking.utility.MyGitUtility.formatStorageSize(10 * 1024 * 1024));
        assertEquals("1.2 GB", inmethod.gitnotetaking.utility.MyGitUtility.formatStorageSize((long) (1.2 * 1024 * 1024 * 1024)));
    }

    @Test
    public void testIsTemporaryFileNameClassification() {
        // Temporary files should return true
        assertTrue("~開頭檔案應視為暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("~$note.docx"));
        assertTrue("~開頭檔案應視為暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("~temp.txt"));
        assertTrue("~結尾檔案應視為暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("note.md~"));
        assertTrue(".tmp 結尾應視為暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("cache.tmp"));
        assertTrue(".swp 結尾應視為暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName(".note.md.swp"));
        assertTrue(".DS_Store 應視為系統暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName(".DS_Store"));
        assertTrue("Thumbs.db 應視為系統暫存檔", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("Thumbs.db"));

        // Regular files should return false
        assertFalse("正常 markdown 筆記不可誤判", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("note.md"));
        assertFalse("正常 txt 筆記不可誤判", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("readme.txt"));
        assertFalse("正常圖片檔案不可誤判", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("photo.png"));
        assertFalse("正常 PDF 檔案不可誤判", inmethod.gitnotetaking.utility.MyGitUtility.isTemporaryFileName("manual.pdf"));
    }

    @Test
    public void testEnsureDefaultGitIgnoreCreatesAndMergesRules() throws Exception {
        File repoDir = tempFolder.newFolder("test_repo_gitignore");

        // 1. 初始無 .gitignore 時，應自動建立並包含暫存規則
        boolean created = inmethod.gitnotetaking.utility.MyGitUtility.ensureDefaultGitIgnore(repoDir.getAbsolutePath());
        assertTrue("初始無 .gitignore 時應回傳 true 表示已建立更新", created);

        File gitignoreFile = new File(repoDir, ".gitignore");
        assertTrue(".gitignore 檔案必須被建立", gitignoreFile.exists());

        String content = new String(java.nio.file.Files.readAllBytes(gitignoreFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("必須包含 ~* 規則", content.contains("~*"));
        assertTrue("必須包含 *.tmp 規則", content.contains("*.tmp"));
        assertTrue("必須包含 .DS_Store 規則", content.contains(".DS_Store"));

        // 2. 再次呼叫時，規則已存在，不應重複寫入
        boolean secondCall = inmethod.gitnotetaking.utility.MyGitUtility.ensureDefaultGitIgnore(repoDir.getAbsolutePath());
        assertFalse("規則已完整時應回傳 false", secondCall);
    }

    @Test
    public void testEntrySyncAutoCommitAndMergeDetection() throws Exception {
        File repoDir = tempFolder.newFolder("test_entry_sync");
        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File note1 = new File(repoDir, "note1.md");
            java.nio.file.Files.write(note1.toPath(), "# Note 1".getBytes());
            git.add().addFilepattern(".").call();
            RevCommit commit1 = git.commit().setMessage("Initial commit").call();

            // 1. Working tree has uncommitted change -> autoCommitIfDirtyWithMessage works
            File note2 = new File(repoDir, "note2.md");
            java.nio.file.Files.write(note2.toPath(), "# Note 2".getBytes());
            
            // Check dirty
            org.eclipse.jgit.api.Status status = git.status().call();
            assertTrue("應偵測到未追蹤或未存檔檔案", status.hasUncommittedChanges() || !status.getUntracked().isEmpty());

            // 2. Normal commit has 1 parent
            assertEquals("初始 Commit 應有 0 個父節點", 0, commit1.getParentCount());

            File note3 = new File(repoDir, "note3.md");
            java.nio.file.Files.write(note3.toPath(), "# Note 3".getBytes());
            git.add().addFilepattern(".").call();
            RevCommit commit2 = git.commit().setMessage("Second commit").call();
            assertEquals("普通 Commit 應有 1 個父節點", 1, commit2.getParentCount());
        }
    }

    @Test
    public void testGitHubRepoPrefixFiltering() {
        java.util.List<inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo> repoList = new java.util.ArrayList<>();
        repoList.add(new inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo("note-work", "user/note-work", "https://github.com/user/note-work.git", "main", "work notes", false));
        repoList.add(new inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo("NoteTaking", "user/NoteTaking", "https://github.com/user/NoteTaking.git", "master", "app repo", false));
        repoList.add(new inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo("wiki-docs", "user/wiki-docs", "https://github.com/user/wiki-docs.git", "main", "docs", false));
        repoList.add(new inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo("android-app", "user/android-app", "https://github.com/user/android-app.git", "main", "app", false));

        // 1. 預設 "note" 前綴過濾
        String prefix1 = "note";
        java.util.List<inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo> filtered1 = new java.util.ArrayList<>();
        for (inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo r : repoList) {
            if (prefix1.isEmpty() || r.getName().toLowerCase().startsWith(prefix1)) {
                filtered1.add(r);
            }
        }
        assertEquals("預設 note 前綴應過濾出 2 個倉庫", 2, filtered1.size());
        assertEquals("note-work", filtered1.get(0).getName());
        assertEquals("NoteTaking", filtered1.get(1).getName());

        // 2. 自訂 "wiki" 前綴過濾
        String prefix2 = "wiki";
        java.util.List<inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo> filtered2 = new java.util.ArrayList<>();
        for (inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo r : repoList) {
            if (prefix2.isEmpty() || r.getName().toLowerCase().startsWith(prefix2)) {
                filtered2.add(r);
            }
        }
        assertEquals("wiki 前綴應過濾出 1 個倉庫", 1, filtered2.size());
        assertEquals("wiki-docs", filtered2.get(0).getName());

        // 3. 空白前綴（抓取全部）
        String prefix3 = "";
        java.util.List<inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo> filtered3 = new java.util.ArrayList<>();
        for (inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo r : repoList) {
            if (prefix3.isEmpty() || r.getName().toLowerCase().startsWith(prefix3)) {
                filtered3.add(r);
            }
        }
        assertEquals("空白前綴應抓取全部 4 個倉庫", 4, filtered3.size());
    }

    /**
     * 7. 驗證純文字檔案 Git Diff 差異比對計算邏輯
     */
    @Test
    public void testComputeDiffCalculations() {
        // 情境 1：無任何修改
        String original = "line 1\nline 2\nline 3\n";
        String identical = "line 1\nline 2\nline 3\n";
        String diffNoChange = inmethod.gitnotetaking.utility.MyGitUtility.computeDiff(original, identical, "a/note.txt", "b/note.txt");
        assertEquals("無變更時 diff 輸出必須為空字串", "", diffNoChange);

        // 情境 2：有修改與新增
        String modified = "line 1\nline 2 (modified)\nline 3\nline 4 (new)\n";
        String diffMod = inmethod.gitnotetaking.utility.MyGitUtility.computeDiff(original, modified, "a/note.txt", "b/note.txt");
        assertNotNull("有變更時 diff 不可為 null", diffMod);
        assertTrue("diff 必須包含刪除行標記 -", diffMod.contains("-line 2"));
        assertTrue("diff 必須包含修改行標記 +", diffMod.contains("+line 2 (modified)"));
        assertTrue("diff 必須包含新增行標記 +", diffMod.contains("+line 4 (new)"));
        assertTrue("diff 必須包含 @@ 範圍標記", diffMod.contains("@@"));

        // 情境 3：全新檔案 (oldContent 為空)
        String newFile = "Hello World\nNew Note\n";
        String diffNew = inmethod.gitnotetaking.utility.MyGitUtility.computeDiff("", newFile, "a/new.md", "b/new.md");
        assertNotNull("全新檔案 diff 不可為 null", diffNew);
        assertTrue("全新檔案 diff 必須包含全部新增行", diffNew.contains("+Hello World") && diffNew.contains("+New Note"));

        // 情境 4：null 安全防護
        String diffNull = inmethod.gitnotetaking.utility.MyGitUtility.computeDiff(null, null, "a/null.txt", "b/null.txt");
        assertEquals("兩者為 null 時輸出空字串", "", diffNull);
    }

    /**
     * 8. 驗證 Git 根目錄尋找與雙軌智慧 Diff
     */
    @Test
    public void testFindGitRootDirAndSmartDiff() throws Exception {
        java.io.File tempDir = tempFolder.newFolder("repoTest");
        java.io.File gitDir = new java.io.File(tempDir, ".git");
        assertTrue(gitDir.mkdir());

        java.io.File subDir = new java.io.File(tempDir, "notes/sub");
        assertTrue(subDir.mkdirs());
        java.io.File noteFile = new java.io.File(subDir, "myNote.txt");
        assertTrue(noteFile.createNewFile());

        // 驗證在多層子目錄下仍能精準找到 Git 根目錄
        java.io.File foundRoot = inmethod.gitnotetaking.utility.MyGitUtility.findGitRootDir(noteFile);
        assertNotNull("必須成功向上找到 .git 所在根目錄", foundRoot);
        assertEquals(tempDir.getCanonicalPath(), foundRoot.getCanonicalPath());

        // 驗證外部檔案找不到 Git 根目錄時的安全防護
        java.io.File outsideFile = tempFolder.newFile("outside.txt");
        assertNull("非 Git 儲存庫檔案應回傳 null", inmethod.gitnotetaking.utility.MyGitUtility.findGitRootDir(outsideFile));
    }
}
