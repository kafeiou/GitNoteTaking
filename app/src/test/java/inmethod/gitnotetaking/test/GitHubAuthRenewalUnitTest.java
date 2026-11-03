package inmethod.gitnotetaking.test;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import inmethod.gitnotetaking.db.MyDbHelper;
import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
import inmethod.gitnotetaking.utility.GitHubAuthManager;
import inmethod.gitnotetaking.utility.MyGitUtility;

import static org.junit.Assert.*;

/**
 * 單元測試：GitHub OAuth 授權過期重獲、State 狀態碼路由、錯誤碼與多語系 FAQ 文件驗證
 */
public class GitHubAuthRenewalUnitTest {

    private File getProjectRootDir() {
        try {
            File cur = new File(".").getCanonicalFile();
            if (new File(cur, "app").isDirectory()) {
                return cur;
            }
            if (cur.getParentFile() != null && new File(cur.getParentFile(), "app").isDirectory()) {
                return cur.getParentFile();
            }
            return cur;
        } catch (Exception e) {
            return new File(".");
        }
    }

    /**
     * 1. 測試 GitHubAuthManager 的 OAuth State 路由封裝與解析邏輯
     */
    @Test
    public void testOAuthStateRouting() {
        // 建立新倉庫常數
        assertEquals("create_new", GitHubAuthManager.STATE_CREATE_NEW);
        assertFalse(GitHubAuthManager.isReauthState(GitHubAuthManager.STATE_CREATE_NEW));
        assertEquals(-1, GitHubAuthManager.parseRepoIdFromState(GitHubAuthManager.STATE_CREATE_NEW));

        // 重新授權狀態編碼
        assertEquals("reauth_1", GitHubAuthManager.buildReauthState(1));
        assertEquals("reauth_42", GitHubAuthManager.buildReauthState(42));
        assertEquals("reauth_9999", GitHubAuthManager.buildReauthState(9999));

        // 重新授權狀態判斷
        assertTrue(GitHubAuthManager.isReauthState("reauth_1"));
        assertTrue(GitHubAuthManager.isReauthState("reauth_42"));
        assertTrue(GitHubAuthManager.isReauthState("reauth_9999"));

        // 異常與邊界情況
        assertFalse(GitHubAuthManager.isReauthState(null));
        assertFalse(GitHubAuthManager.isReauthState(""));
        assertFalse(GitHubAuthManager.isReauthState("reauth_"));
        assertFalse(GitHubAuthManager.isReauthState("reauth_abc"));
        assertFalse(GitHubAuthManager.isReauthState("reauth_12_extra"));
        assertFalse(GitHubAuthManager.isReauthState("login"));

        // 倉庫 ID 解析
        assertEquals(1, GitHubAuthManager.parseRepoIdFromState("reauth_1"));
        assertEquals(42, GitHubAuthManager.parseRepoIdFromState("reauth_42"));
        assertEquals(9999, GitHubAuthManager.parseRepoIdFromState("reauth_9999"));
        assertEquals(-1, GitHubAuthManager.parseRepoIdFromState(null));
        assertEquals(-1, GitHubAuthManager.parseRepoIdFromState(""));
        assertEquals(-1, GitHubAuthManager.parseRepoIdFromState("reauth_"));
        assertEquals(-1, GitHubAuthManager.parseRepoIdFromState("reauth_not_a_number"));
        assertEquals(-1, GitHubAuthManager.parseRepoIdFromState("create_new"));
    }

    /**
     * 2. 測試 Git 狀態碼常數唯一性與定義
     */
    @Test
    public void testGitAuthFailedStatusCode() {
        assertEquals(-5, MyGitUtility.GIT_STATUS_AUTH_FAILED);

        // 確保與其他狀態碼互斥
        int[] statuses = new int[]{
                MyGitUtility.GIT_STATUS_SUCCESS,
                MyGitUtility.GIT_STATUS_PUSH_FAIL,
                MyGitUtility.GIT_STATUS_CLONING,
                MyGitUtility.GIT_STATUS_PULLING,
                MyGitUtility.GIT_STATUS_AUTH_FAILED
        };

        for (int i = 0; i < statuses.length; i++) {
            for (int j = i + 1; j < statuses.length; j++) {
                assertNotEquals("狀態碼常數不得重複: " + statuses[i], statuses[i], statuses[j]);
            }
        }
    }

    /**
     * 3. 測試 4 國語系中新增的 GitHub 授權字串鍵值完整性
     */
    @Test
    public void testStringsResourceKeysExistInAllLocales() throws Exception {
        File root = getProjectRootDir();
        List<String> localePaths = Arrays.asList(
                "app/src/main/res/values/strings.xml",
                "app/src/main/res/values-zh-rTW/strings.xml",
                "app/src/main/res/values-zh-rCN/strings.xml",
                "app/src/main/res/values-ja/strings.xml"
        );

        List<String> requiredKeys = Arrays.asList(
                "github_auth_expired_title",
                "github_auth_expired_msg",
                "github_auth_relogin_and_sync",
                "github_reauth_success",
                "github_reauth_button",
                "github_update_auth_title",
                "github_update_auth_confirm_msg",
                "main_notes_auth_expired",
                "sync_progress_preparing",
                "sync_progress_receiving",
                "sync_progress_resolving",
                "sync_progress_checking_out",
                "sync_progress_restoring_dates",
                "main_notes_sync_file_dates",
                "dialog_sync_file_dates_title",
                "dialog_sync_file_dates_msg",
                "toast_sync_file_dates_success",
                "toast_sync_file_dates_failed"
        );

        for (String relPath : localePaths) {
            File stringsFile = new File(root, relPath);
            assertTrue("語系檔案必須存在: " + relPath, stringsFile.exists());

            String content = new String(Files.readAllBytes(stringsFile.toPath()), StandardCharsets.UTF_8);
            for (String key : requiredKeys) {
                String searchTag = "name=\"" + key + "\"";
                assertTrue("檔案 " + relPath + " 必須包含字串鍵值 " + key, content.contains(searchTag));
            }
        }
    }

    /**
     * 4. 測試多語系 FAQ Markdown 說明文件完整性
     */
    @Test
    public void testFaqDocumentsExistAndContainKeyInformation() throws Exception {
        File root = getProjectRootDir();
        List<String> faqFiles = Arrays.asList(
                "docs/FAQ.md",
                "docs/FAQ_zh-CN.md",
                "docs/FAQ_en.md",
                "docs/FAQ_ja.md"
        );

        for (String relPath : faqFiles) {
            File faqFile = new File(root, relPath);
            assertTrue("FAQ 文件必須存在: " + relPath, faqFile.exists());

            String content = new String(Files.readAllBytes(faqFile.toPath()), StandardCharsets.UTF_8);
            assertTrue("FAQ 必須包含 8 小時說明: " + relPath, content.contains("8"));
            assertTrue("FAQ 必須包含 401 錯誤說明: " + relPath, content.contains("401"));
            assertTrue("FAQ 必須包含 GitHub Developer 設定說明: " + relPath, content.contains("Expire user authorization tokens"));
            assertTrue("FAQ 必須包含 Personal Access Token 說明: " + relPath, content.contains("Personal Access Token"));
        }
    }

    /**
     * 5. 測試 修改遠端設定頁面的 Layout 與 Manifest launchMode 設定
     */
    @Test
    public void testLayoutAndManifestConfiguration() throws Exception {
        File root = getProjectRootDir();

        // 佈局檔驗證
        File layoutFile = new File(root, "app/src/main/res/layout/activity_main_modify_remote.xml");
        assertTrue("修改遠端佈局檔案必須存在", layoutFile.exists());
        String layoutContent = new String(Files.readAllBytes(layoutFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("佈局檔必須包含 btnGitHubReauth", layoutContent.contains("android:id=\"@+id/btnGitHubReauth\""));
        assertTrue("btnGitHubReauth 預設應為 gone", layoutContent.contains("android:visibility=\"gone\""));

        // 清單檔驗證
        File manifestFile = new File(root, "app/src/main/AndroidManifest.xml");
        assertTrue("AndroidManifest.xml 必須存在", manifestFile.exists());
        String manifestContent = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("MainActivity launchMode 應為 singleTask 確保 OAuth Callback 重定向安全",
                manifestContent.contains("android:launchMode=\"singleTask\""));
    }

    /**
     * 6. 測試 Release R8 模糊化最佳化與 ProGuard 規則配置
     */
    @Test
    public void testR8ObfuscationAndProguardRules() throws Exception {
        File root = getProjectRootDir();

        File buildGradleFile = new File(root, "app/build.gradle");
        assertTrue("app/build.gradle 必須存在", buildGradleFile.exists());
        String gradleContent = new String(Files.readAllBytes(buildGradleFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("Release 必須啟用 minifyEnabled true", gradleContent.contains("minifyEnabled true"));
        assertTrue("Release 必須啟用 shrinkResources true", gradleContent.contains("shrinkResources true"));

        File proguardFile = new File(root, "app/proguard-rules.pro");
        assertTrue("app/proguard-rules.pro 必須存在", proguardFile.exists());
        String proguardContent = new String(Files.readAllBytes(proguardFile.toPath()), StandardCharsets.UTF_8);
        assertFalse("ProGuard 不得包含 -dontobfuscate 以符合 Google Play DEX 模糊化規範",
                proguardContent.contains("-dontobfuscate"));
        assertTrue("ProGuard 必須保留 JGit TranslationBundle 子類別",
                proguardContent.contains("org.eclipse.jgit.nls.TranslationBundle"));
    }

    /**
     * 7. 測試資料庫 v3 認證類型標記 (AUTH_TYPE) 與智慧推斷邏輯
     */
    @Test
    public void testDatabaseAuthTypeModelAndInference() {
        assertEquals(3, MyDbHelper.Companion.getVERSION());
        assertEquals("AUTH_TYPE", RemoteGitDAO.Companion.getAUTH_TYPE_COLUMN());
        assertTrue(RemoteGitDAO.Companion.getCREATE_TABLE().contains("AUTH_TYPE TEXT NOT NULL DEFAULT 'PASSWORD'"));

        assertEquals("PASSWORD", RemoteGit.AUTH_TYPE_PASSWORD);
        assertEquals("WEB_OAUTH", RemoteGit.AUTH_TYPE_WEB_OAUTH);

        // GitHub Web OAuth Token (ghu_... 或一般 OAuth 權杖) 應推斷為 WEB_OAUTH
        assertEquals(RemoteGit.AUTH_TYPE_WEB_OAUTH,
                RemoteGit.inferAuthType("https://github.com/user/notes.git", "ghu_1234567890abcdef"));
        assertEquals(RemoteGit.AUTH_TYPE_WEB_OAUTH,
                RemoteGit.inferAuthType("https://github.com/user/notes.git", "gho_abcdef123456"));

        // GitHub PAT (ghp_ / github_pat_) 應推斷為 PASSWORD
        assertEquals(RemoteGit.AUTH_TYPE_PASSWORD,
                RemoteGit.inferAuthType("https://github.com/user/notes.git", "ghp_classicToken123"));
        assertEquals(RemoteGit.AUTH_TYPE_PASSWORD,
                RemoteGit.inferAuthType("https://github.com/user/notes.git", "github_pat_fineGrainedToken456"));

        // 非 GitHub URL 或空密碼應推斷為 PASSWORD
        assertEquals(RemoteGit.AUTH_TYPE_PASSWORD,
                RemoteGit.inferAuthType("https://gitlab.com/user/notes.git", "glpat-12345"));
        assertEquals(RemoteGit.AUTH_TYPE_PASSWORD,
                RemoteGit.inferAuthType("https://github.com/user/notes.git", ""));
        assertEquals(RemoteGit.AUTH_TYPE_PASSWORD,
                RemoteGit.inferAuthType(null, null));

        // RemoteGit 實體方法驗證
        RemoteGit webRepo = new RemoteGit();
        webRepo.setUrl("https://github.com/user/notes.git");
        webRepo.setAuth_type(RemoteGit.AUTH_TYPE_WEB_OAUTH);
        assertTrue(webRepo.isWebOAuth());

        RemoteGit patRepo = new RemoteGit();
        patRepo.setUrl("https://github.com/user/notes.git");
        patRepo.setAuth_type(RemoteGit.AUTH_TYPE_PASSWORD);
        assertFalse(patRepo.isWebOAuth());
    }

    /**
     * 8. 測試首次同步 (Clone depth=1) 階段進度與檔案真實日期還原進度格式化
     */
    @Test
    public void testCloneProgressAndDateRestoreFormatting() {
        // 前置準備階段 (totalWork <= 0)
        assertEquals("Preparing repository sync...",
                MyGitUtility.formatCloneTaskProgress(null, "remote: Counting objects", 0, 0, 0L));

        // 階段 1: Receiving objects (不顯示 0B 容量，僅顯示精確百分比與物件進度)
        long tenMb = 10L * 1024L * 1024L;
        String recvMsg = MyGitUtility.formatCloneTaskProgress(null, "Receiving objects", 210, 500, tenMb);
        assertEquals("Downloading note data... 42% (210/500)", recvMsg);

        // 階段 2: Resolving deltas
        String resolveMsg = MyGitUtility.formatCloneTaskProgress(null, "Resolving deltas", 90, 120, 0L);
        assertEquals("Resolving data deltas... 75% (90/120)", resolveMsg);

        // 階段 3: Checking out files
        String checkoutMsg = MyGitUtility.formatCloneTaskProgress(null, "Checking out files", 280, 350, 0L);
        assertEquals("Writing note files... 80% (280/350)", checkoutMsg);

        // 階段 4: 還原檔案真實日期
        String dateMsg0 = MyGitUtility.formatDateRestoreProgress(null, 0, 350);
        assertEquals("Restoring file dates... 0% (0/350)", dateMsg0);
        String dateMsgMid = MyGitUtility.formatDateRestoreProgress(null, 200, 350);
        assertEquals("Restoring file dates... 57% (200/350)", dateMsgMid);
        String dateMsgDone = MyGitUtility.formatDateRestoreProgress(null, 350, 350);
        assertEquals("Restoring file dates... 100% (350/350)", dateMsgDone);
    }

    /**
     * 9. 測試重新 Clone 前清理殘留非空目錄 (無 .git 子目錄時仍能完整清除)
     */
    @Test
    public void testCleanDirectoryForCloneRemovesLeftoverNonGitFiles() throws Exception {
        File tempDir = Files.createTempDirectory("test_leftover_notes_").toFile();
        File subDir = new File(tempDir, "subfolder");
        assertTrue(subDir.mkdirs());
        File leftoverNote = new File(subDir, "note1.md");
        Files.write(leftoverNote.toPath(), "residual content".getBytes(StandardCharsets.UTF_8));
        leftoverNote.setReadOnly();

        assertTrue("清理前目錄應存在且非空", tempDir.exists() && subDir.exists() && leftoverNote.exists());
        boolean cleaned = MyGitUtility.cleanDirectoryForClone(tempDir);
        assertTrue("cleanDirectoryForClone 應回傳 true", cleaned);
        assertFalse("清理後目標目錄應已完全移除，防止 JGit 拋出 already exists and is not an empty directory",
                tempDir.exists());
    }

    /**
     * 10. 測試強制關閉導致 .git/shallow 指向不存在之 Missing commit 時，能自動修復為本地實際邊界 Commit
     */
    @Test
    public void testRepairShallowRepositoryAfterInterruptedFetch() throws Exception {
        File repoDir = Files.createTempDirectory("test_shallow_repair_").toFile();
        try (Git git = Git.init().setDirectory(repoDir).call()) {
            File noteFile = new File(repoDir, "note.md");
            Files.write(noteFile.toPath(), "v1".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("note.md").call();
            RevCommit commit1 = git.commit().setMessage("commit 1").call();

            Files.write(noteFile.toPath(), "v2".getBytes(StandardCharsets.UTF_8));
            git.add().addFilepattern("note.md").call();
            RevCommit commit2 = git.commit().setMessage("commit 2").call();

            // 模擬 depth=1 狀態：移除 commit1 的 loose object，使 commit2 成為本地唯一存在的邊界 Commit
            String c1Hex = commit1.getName();
            File c1ObjFile = new File(repoDir, ".git/objects/" + c1Hex.substring(0, 2) + "/" + c1Hex.substring(2));
            assertTrue("commit1 object 應存在並被刪除以模擬 shallow clone", c1ObjFile.delete());

            // 模擬強制關閉 App 導致 .git/shallow 寫入尚未下載完成的幽靈 Commit (629bf956...) 且殘留 shallow.lock
            File shallowFile = new File(repoDir, ".git/shallow");
            File shallowLock = new File(repoDir, ".git/shallow.lock");
            Files.write(shallowFile.toPath(), "629bf956104b2ecb1ec13905dca75e3636a36aab\n".getBytes(StandardCharsets.UTF_8));
            Files.write(shallowLock.toPath(), "lock".getBytes(StandardCharsets.UTF_8));

            boolean repaired = MyGitUtility.repairShallowRepositoryIfNeeded(repoDir.getAbsolutePath());
            assertTrue("應成功偵測並修復損壞的 .git/shallow", repaired);
            assertFalse("殘留的 shallow.lock 應被清除", shallowLock.exists());

            String repairedContent = new String(Files.readAllBytes(shallowFile.toPath()), StandardCharsets.UTF_8).trim();
            assertEquals("修復後的 .git/shallow 應指向本地實際存在的邊界 commit2", commit2.getName(), repairedContent);
        } finally {
            MyGitUtility.cleanDirectoryForClone(repoDir);
        }
    }
}
