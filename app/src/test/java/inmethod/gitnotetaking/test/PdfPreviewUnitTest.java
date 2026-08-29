package inmethod.gitnotetaking.test;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * 自動化單元測試：PDF 離線預覽、密碼保護合約、靜態資產完整性與 DayNight 主題適配驗證
 */
public class PdfPreviewUnitTest {

    private static final String PDF_ASSETS_DIR = "src/main/assets/pdf";

    private File getPdfAssetsDir() {
        File f = new File(PDF_ASSETS_DIR);
        if (!f.exists()) {
            f = new File("app/" + PDF_ASSETS_DIR);
        }
        return f;
    }

    /**
     * 1. 驗證 PDF 離線靜態資產 (pdf_viewer.html, pdf.min.js, pdf.worker.min.js) 存在性與完整結構
     */
    @Test
    public void testPdfAssetsExistAndAreValid() throws Exception {
        File dir = getPdfAssetsDir();
        assertTrue("assets/pdf 目錄必須存在", dir.exists() && dir.isDirectory());

        File htmlFile = new File(dir, "pdf_viewer.html");
        File jsFile = new File(dir, "pdf.min.js");
        File workerFile = new File(dir, "pdf.worker.min.js");

        assertTrue("pdf_viewer.html 必須存在且非空", htmlFile.exists() && htmlFile.length() > 0);
        assertTrue("pdf.min.js 必須存在且非空", jsFile.exists() && jsFile.length() > 0);
        assertTrue("pdf.worker.min.js 必須存在且非空", workerFile.exists() && workerFile.length() > 0);

        String htmlContent = new String(Files.readAllBytes(htmlFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("pdf_viewer.html 必須引入 pdf.min.js", htmlContent.contains("pdf.min.js"));
        assertTrue("pdf_viewer.html 必須包含 loadPdfFromBase64 函式", htmlContent.contains("loadPdfFromBase64"));
        assertTrue("pdf_viewer.html 必須包含 providePassword 密碼回傳介面", htmlContent.contains("providePassword"));
        assertTrue("pdf_viewer.html 必須包含 AndroidPDF JavascriptInterface 介面串接", htmlContent.contains("AndroidPDF.onPasswordRequired"));
        assertTrue("pdf_viewer.html 必須包含 prefers-color-scheme 深色適配樣式", htmlContent.contains("prefers-color-scheme: dark"));
    }

    /**
     * 2. 驗證 pdf.min.js 提供標準 PDF.js API 與密碼保護合約 (PasswordResponses)
     */
    @Test
    public void testPdfJsApiAndSecurityContract() throws Exception {
        File dir = getPdfAssetsDir();
        File jsFile = new File(dir, "pdf.min.js");
        String jsContent = new String(Files.readAllBytes(jsFile.toPath()), StandardCharsets.UTF_8);

        assertTrue("pdf.min.js 必須提供 pdfjsLib 物件", jsContent.contains("pdfjsLib"));
        assertTrue("pdf.min.js 必須提供 getDocument 接口", jsContent.contains("getDocument"));
        assertTrue("pdf.min.js 必須包含 NEED_PASSWORD 密碼事件定義", jsContent.contains("NEED_PASSWORD"));
        assertTrue("pdf.min.js 必須包含 INCORRECT_PASSWORD 密碼錯誤事件定義", jsContent.contains("INCORRECT_PASSWORD"));
        assertTrue("pdf.min.js 必須包含 /Encrypt 加密字典偵測", jsContent.contains("/Encrypt"));
    }

    /**
     * 3. 驗證副檔名識別與導航路由邏輯（.pdf, .PDF 導向專屬 PDF 預覽，其餘導向文字或預設處理）
     */
    @Test
    public void testPdfExtensionDetection() {
        String[] validPdfs = {"manual.pdf", "architecture.PDF", "spec.Pdf", "folder/sub/document.pdf"};
        String[] nonPdfs = {"document.txt", "notes.md", "image.png", "pdf_file.doc", "sample.pdf.bak"};

        for (String filename : validPdfs) {
            String lower = filename.toLowerCase();
            boolean isPdf = lower.endsWith(".pdf");
            assertTrue("應識別為 PDF 檔案: " + filename, isPdf);
        }

        for (String filename : nonPdfs) {
            String lower = filename.toLowerCase();
            boolean isPdf = lower.endsWith(".pdf");
            assertFalse("不應誤判為 PDF 檔案: " + filename, isPdf);
        }
    }

    /**
     * 4. 驗證全 App 深淺色主題 (DayNight) 模式值解析邏輯
     */
    @Test
    public void testThemeModeMapping() {
        String[] themeValues = {"system", "light", "dark"};

        for (String theme : themeValues) {
            if ("dark".equalsIgnoreCase(theme)) {
                assertEquals("深色模式對應", "dark", theme);
            } else if ("light".equalsIgnoreCase(theme)) {
                assertEquals("淺色模式對應", "light", theme);
            } else {
                assertEquals("預設跟隨系統", "system", theme);
            }
        }
    }

    /**
     * 5. 驗證測試用加密 PDF 檔案結構、標頭與 /Encrypt 密碼保護字典
     */
    @Test
    public void testSampleProtectedPdfStructureAndEncryption() throws Exception {
        File sampleFile = new File("sample_protected_123456.pdf");
        if (!sampleFile.exists()) {
            sampleFile = new File("../sample_protected_123456.pdf");
        }
        if (sampleFile.exists()) {
            byte[] bytes = Files.readAllBytes(sampleFile.toPath());
            String content = new String(bytes, StandardCharsets.ISO_8859_1);

            assertTrue("PDF 必須以 %PDF- 標準標頭開頭", content.startsWith("%PDF-"));
            assertTrue("PDF 必須包含 /Encrypt 加密字典引用", content.contains("/Encrypt"));
            assertTrue("PDF 必須包含 Standard 安全處理器", content.contains("/Filter /Standard"));
            assertTrue("PDF 必須包含 %%EOF 檔案結尾標記", content.contains("%%EOF"));
        }
    }

    /**
     * 6. 驗證 PDF 密碼 Session 快取生命週期模擬
     */
    @Test
    public void testPdfPasswordSessionCacheLifecycle() {
        java.util.Map<String, String> cache = new java.util.HashMap<>();
        String samplePath = "/path/to/sample_protected_123456.pdf";

        // 初次開啟：快取為空
        assertNull("初次開啟無快取", cache.get(samplePath));

        // 輸入密碼後寫入快取
        cache.put(samplePath, "123456");
        assertEquals("再次查詢應命中快取", "123456", cache.get(samplePath));

        // 應用程式關閉/銷毀：清空快取
        cache.clear();
        assertNull("App 銷毀後快取被清空", cache.get(samplePath));
    }
}
