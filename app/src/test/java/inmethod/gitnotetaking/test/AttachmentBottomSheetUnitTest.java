package inmethod.gitnotetaking.test;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import inmethod.gitnotetaking.R;
import inmethod.gitnotetaking.ViewFileActivity;

import static org.junit.Assert.*;

/**
 * 自動化單元測試：筆記附件底部抽屜中繼資料、圖示映射、提交刪除訊息與佈局語系驗證
 */
public class AttachmentBottomSheetUnitTest {

    private File getProjectDir() {
        File f = new File("app");
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        return new File(".");
    }

    /**
     * 1. 測試附件類型與圖示資源映射邏輯 (MIME type 與副檔名 fallback)
     */
    @Test
    public void testAttachmentIconMapping() {
        // MIME 優先
        assertEquals(R.drawable.image24, ViewFileActivity.getAttachmentIconRes("image/png"));
        assertEquals(R.drawable.image24, ViewFileActivity.getAttachmentIconRes("image/jpeg"));
        assertEquals(R.drawable.txt24, ViewFileActivity.getAttachmentIconRes("text/plain"));
        assertEquals(R.drawable.pdf24, ViewFileActivity.getAttachmentIconRes("application/pdf"));
        assertEquals(R.drawable.xls24, ViewFileActivity.getAttachmentIconRes("application/vnd.ms-excel"));
        assertEquals(R.drawable.xls24, ViewFileActivity.getAttachmentIconRes("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertEquals(R.drawable.doc24, ViewFileActivity.getAttachmentIconRes("application/msword"));
        assertEquals(R.drawable.doc24, ViewFileActivity.getAttachmentIconRes("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertEquals(R.drawable.ppt24, ViewFileActivity.getAttachmentIconRes("application/vnd.ms-powerpoint"));
        assertEquals(R.drawable.ppt24, ViewFileActivity.getAttachmentIconRes("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertEquals(R.drawable.unknown24, ViewFileActivity.getAttachmentIconRes("application/octet-stream"));
        assertEquals(R.drawable.unknown24, ViewFileActivity.getAttachmentIconRes(null));

        // 帶副檔名輔助判斷
        assertEquals(R.drawable.pdf24, ViewFileActivity.getAttachmentIconResByFileName("document.pdf", null));
        assertEquals(R.drawable.image24, ViewFileActivity.getAttachmentIconResByFileName("photo.JPG", null));
        assertEquals(R.drawable.image24, ViewFileActivity.getAttachmentIconResByFileName("graphic.png", ""));
        assertEquals(R.drawable.txt24, ViewFileActivity.getAttachmentIconResByFileName("readme.md", null));
        assertEquals(R.drawable.xls24, ViewFileActivity.getAttachmentIconResByFileName("budget.xlsx", null));
        assertEquals(R.drawable.doc24, ViewFileActivity.getAttachmentIconResByFileName("memo.docx", null));
        assertEquals(R.drawable.ppt24, ViewFileActivity.getAttachmentIconResByFileName("slides.pptx", null));
        assertEquals(R.drawable.unknown24, ViewFileActivity.getAttachmentIconResByFileName("archive.zip", null));
        assertEquals(R.drawable.unknown24, ViewFileActivity.getAttachmentIconResByFileName("no_extension", null));
    }

    /**
     * 2. 測試附件格式字串顯示轉換邏輯
     */
    @Test
    public void testAttachmentTypeDisplay() {
        assertEquals("application/pdf", ViewFileActivity.getAttachmentTypeDisplay("report.pdf", "application/pdf"));
        assertEquals("image/png", ViewFileActivity.getAttachmentTypeDisplay("photo.png", "image/png"));

        // 無 MIME 時應退回為大寫副檔名
        assertEquals("PDF", ViewFileActivity.getAttachmentTypeDisplay("report.pdf", null));
        assertEquals("DOCX", ViewFileActivity.getAttachmentTypeDisplay("contract.docx", ""));
        assertEquals("PNG", ViewFileActivity.getAttachmentTypeDisplay("avatar.png", null));
        assertEquals("UNKNOWN", ViewFileActivity.getAttachmentTypeDisplay("README", null));
        assertEquals("UNKNOWN", ViewFileActivity.getAttachmentTypeDisplay(null, null));
    }

    /**
     * 3. 測試刪除附件時的 Git Commit 訊息規範（純英文，格式為 User deleted attachment: <檔名>）
     */
    @Test
    public void testAttachmentDeleteCommitMessage() {
        String fileName = "presentation_final.pdf";
        String expected = "User deleted attachment: presentation_final.pdf";
        assertEquals(expected, ViewFileActivity.getAttachmentDeleteCommitMessage(fileName));

        String spacedName = "2026 Project Report v2.docx";
        assertEquals("User deleted attachment: 2026 Project Report v2.docx",
                ViewFileActivity.getAttachmentDeleteCommitMessage(spacedName));
    }

    /**
     * 4. 測試底部抽屜 XML 佈局結構完整性與關鍵 ID 存在
     */
    @Test
    public void testAttachmentBottomSheetLayoutXml() throws Exception {
        File projectDir = getProjectDir();
        File layoutFile = new File(projectDir, "src/main/res/layout/dialog_attachment_bottom_sheet.xml");
        assertTrue("dialog_attachment_bottom_sheet.xml 必須存在", layoutFile.exists());

        String content = new String(Files.readAllBytes(layoutFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("必須包含 ivAttachIcon", content.contains("android:id=\"@+id/ivAttachIcon\""));
        assertTrue("必須包含 tvAttachFileName", content.contains("android:id=\"@+id/tvAttachFileName\""));
        assertTrue("必須包含 tvAttachSize", content.contains("android:id=\"@+id/tvAttachSize\""));
        assertTrue("必須包含 tvAttachType", content.contains("android:id=\"@+id/tvAttachType\""));
        assertTrue("必須包含 tvAttachModified", content.contains("android:id=\"@+id/tvAttachModified\""));
        assertTrue("必須包含 btnOpenAttachment", content.contains("android:id=\"@+id/btnOpenAttachment\""));
        assertTrue("必須包含 btnDownloadAttachment", content.contains("android:id=\"@+id/btnDownloadAttachment\""));
        assertTrue("必須包含 btnDeleteAttachment", content.contains("android:id=\"@+id/btnDeleteAttachment\""));
    }

    /**
     * 5. 測試 4 國語系中所有 attach_sheet_* 字串資源無缺漏
     */
    @Test
    public void testAttachmentStringResourcesInAllLanguages() throws Exception {
        File projectDir = getProjectDir();
        String[] langDirs = new String[]{
                "src/main/res/values/strings.xml",
                "src/main/res/values-zh-rTW/strings.xml",
                "src/main/res/values-zh-rCN/strings.xml",
                "src/main/res/values-ja/strings.xml"
        };

        String[] requiredKeys = new String[]{
                "attach_sheet_size",
                "attach_sheet_type",
                "attach_sheet_modified",
                "attach_sheet_open",
                "attach_sheet_download",
                "attach_sheet_delete",
                "attach_sheet_close",
                "attach_sheet_no_app",
                "attach_sheet_download_success",
                "attach_sheet_file_not_found"
        };

        for (String langPath : langDirs) {
            File stringsFile = new File(projectDir, langPath);
            assertTrue("語系檔案必須存在: " + langPath, stringsFile.exists());

            String xml = new String(Files.readAllBytes(stringsFile.toPath()), StandardCharsets.UTF_8);
            for (String key : requiredKeys) {
                assertTrue("語系檔案 " + langPath + " 必須包含鍵值: " + key,
                        xml.contains("name=\"" + key + "\""));
            }
        }
    }
}
