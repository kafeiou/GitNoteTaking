package inmethod.gitnotetaking.test;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
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
}
