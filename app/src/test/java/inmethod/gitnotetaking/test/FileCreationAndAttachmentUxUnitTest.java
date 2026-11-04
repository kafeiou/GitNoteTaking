package inmethod.gitnotetaking.test;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import inmethod.gitnotetaking.FileExplorerActivity;
import inmethod.gitnotetaking.view.GitList;

import static org.junit.Assert.*;

/**
 * 單元測試：建立檔案固定 .txt 副檔名正規化、建立後自動開啟、新增文件等待視窗與過期提示文字縮放驗證
 */
public class FileCreationAndAttachmentUxUnitTest {

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

    @Test
    public void testNormalizeNewTxtFileNameStandard() {
        assertEquals("newfile.txt", FileExplorerActivity.normalizeNewTxtFileName("newfile"));
        assertEquals("my_note.txt", FileExplorerActivity.normalizeNewTxtFileName("my_note"));
        assertEquals("trimmed_note.txt", FileExplorerActivity.normalizeNewTxtFileName("  trimmed_note  "));
        assertEquals("會議記錄 2026.txt", FileExplorerActivity.normalizeNewTxtFileName(" 會議記錄 2026 "));
    }

    @Test
    public void testNormalizeNewTxtFileNameDeduplicatesTxtExtension() {
        assertEquals("newfile.txt", FileExplorerActivity.normalizeNewTxtFileName("newfile.txt"));
        assertEquals("newfile.txt", FileExplorerActivity.normalizeNewTxtFileName("newfile.TXT"));
        assertEquals("newfile.txt", FileExplorerActivity.normalizeNewTxtFileName("newfile.txt.txt"));
        assertEquals("note.txt", FileExplorerActivity.normalizeNewTxtFileName("  note.TxT  "));
    }

    @Test
    public void testNormalizeNewTxtFileNameRejectsBlankOrExtensionOnly() {
        assertEquals("", FileExplorerActivity.normalizeNewTxtFileName(null));
        assertEquals("", FileExplorerActivity.normalizeNewTxtFileName(""));
        assertEquals("", FileExplorerActivity.normalizeNewTxtFileName("   "));
        assertEquals("", FileExplorerActivity.normalizeNewTxtFileName(".txt"));
        assertEquals("", FileExplorerActivity.normalizeNewTxtFileName("  .TXT  "));
        assertEquals("", FileExplorerActivity.normalizeNewTxtFileName(".txt.txt"));
    }

    @Test
    public void testMultilingualAddingFileAndAuthExpiredStringsPresent() throws Exception {
        File root = getProjectRootDir();
        String[] resFiles = new String[]{
                "app/src/main/res/values-zh-rTW/strings.xml",
                "app/src/main/res/values-zh-rCN/strings.xml",
                "app/src/main/res/values-ja/strings.xml",
                "app/src/main/res/values/strings.xml"
        };
        for (String relPath : resFiles) {
            File f = new File(root, relPath);
            assertTrue("語系檔應存在: " + relPath, f.exists());
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertTrue("語系檔應包含 tv_adding_file: " + relPath,
                    content.contains("<string name=\"tv_adding_file\">"));
            assertTrue("語系檔應包含 main_notes_auth_expired: " + relPath,
                    content.contains("<string name=\"main_notes_auth_expired\">"));
        }

        String zhTw = new String(Files.readAllBytes(new File(root, "app/src/main/res/values-zh-rTW/strings.xml").toPath()), StandardCharsets.UTF_8);
        assertTrue("繁中過期提示應包含請點選更新", zhTw.contains("(授權已過期，請點選更新)"));
    }

    @Test
    public void testGitListStatusFontScalingAndCardWrapContent() throws Exception {
        assertEquals(0.65f, GitList.STATUS_SUFFIX_SIZE_RATIO, 0.001f);

        File root = getProjectRootDir();
        File gitListFile = new File(root, "app/src/main/java/inmethod/gitnotetaking/view/GitList.java");
        File layoutFile = new File(root, "app/src/main/res/layout/recycler_adapter_layout.xml");

        String gitListSrc = new String(Files.readAllBytes(gitListFile.toPath()), StandardCharsets.UTF_8);
        String layoutXml = new String(Files.readAllBytes(layoutFile.toPath()), StandardCharsets.UTF_8);

        assertTrue(gitListSrc.contains("formatTitleWithStatus("));
        assertTrue(gitListSrc.contains("RelativeSizeSpan(STATUS_SUFFIX_SIZE_RATIO)"));
        assertTrue("筆記本卡片高度應為 wrap_content 搭配 minHeight 防止長文字被裁切",
                layoutXml.contains("android:minHeight=\"80dp\"") && layoutXml.contains("android:layout_height=\"wrap_content\""));
    }

    @Test
    public void testFileCreationAndAttachmentLoadingImplementation() throws Exception {
        File root = getProjectRootDir();
        File explorerFile = new File(root, "app/src/main/java/inmethod/gitnotetaking/FileExplorerActivity.java");
        File viewFile = new File(root, "app/src/main/java/inmethod/gitnotetaking/ViewFileActivity.java");

        assertTrue(explorerFile.exists());
        assertTrue(viewFile.exists());

        String explorerSrc = new String(Files.readAllBytes(explorerFile.toPath()), StandardCharsets.UTF_8);
        String viewFileSrc = new String(Files.readAllBytes(viewFile.toPath()), StandardCharsets.UTF_8);

        // 驗證功能 1：預設 "newfile"、全選、固定 ".txt"
        assertTrue(explorerSrc.contains("m_edtinput.setText(\"newfile\")"));
        assertTrue(explorerSrc.contains("m_edtinput.selectAll()"));
        assertTrue(explorerSrc.contains("tvSuffix.setText(\".txt\")"));

        // 驗證功能 2：建立檔案成功後立即啟動 ViewFileActivity
        assertTrue(explorerSrc.contains("new Intent(FileExplorerActivity.this, ViewFileActivity.class)"));

        // 驗證功能 3：顯示正在新增中 (showAddingFileLoadingDialog) 與原地刷新附件列 (refreshAttachmentViews)
        assertTrue(explorerSrc.contains("showAddingFileLoadingDialog()"));
        assertTrue(viewFileSrc.contains("showAddingFileLoadingDialog()"));
        assertTrue(viewFileSrc.contains("refreshAttachmentViews()"));
        assertFalse("ViewFileActivity 新增附件不應再重啟 Activity",
                viewFileSrc.contains("startActivity(getIntent())"));
    }
}
