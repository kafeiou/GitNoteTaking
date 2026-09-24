package inmethod.gitnotetaking.test;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

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
                "github_update_auth_confirm_msg"
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
}
