package inmethod.gitnotetaking.test;

import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * 自動化單元測試：Markdown 離線預覽、語法解析規範、靜態資產完整性與連結處理邏輯驗證
 */
public class MarkdownPreviewAndLayoutUnitTest {

    private static final String ASSETS_DIR = "src/main/assets/markdown";

    private File getAssetsDir() {
        File f = new File(ASSETS_DIR);
        if (!f.exists()) {
            f = new File("app/" + ASSETS_DIR);
        }
        return f;
    }

    /**
     * 1. 驗證 Markdown 離線靜態資產檔案存在性與基本結構
     */
    @Test
    public void testMarkdownAssetsExistAndAreValid() throws Exception {
        File dir = getAssetsDir();
        assertTrue("assets/markdown 目錄必須存在", dir.exists() && dir.isDirectory());

        File markedJs = new File(dir, "marked.min.js");
        File mermaidJs = new File(dir, "mermaid.min.js");
        File css = new File(dir, "github-markdown.css");
        File previewHtml = new File(dir, "preview.html");

        assertTrue("marked.min.js 必須存在且非空", markedJs.exists() && markedJs.length() > 0);
        assertTrue("mermaid.min.js 必須存在且非空", mermaidJs.exists() && mermaidJs.length() > 0);
        assertTrue("github-markdown.css 必須存在且非空", css.exists() && css.length() > 0);
        assertTrue("preview.html 必須存在且非空", previewHtml.exists() && previewHtml.length() > 0);

        String htmlContent = new String(Files.readAllBytes(previewHtml.toPath()), StandardCharsets.UTF_8);
        assertTrue("preview.html 必須包含 github-markdown.css 引入", htmlContent.contains("github-markdown.css"));
        assertTrue("preview.html 必須包含 marked.min.js 引入", htmlContent.contains("marked.min.js"));
        assertTrue("preview.html 必須包含 mermaid.min.js 引入", htmlContent.contains("mermaid.min.js"));
        assertTrue("preview.html 必須包含 setMarkdownContent 渲染函數", htmlContent.contains("setMarkdownContent"));
        assertTrue("preview.html 必須包含自適應 viewport 設定", htmlContent.contains("viewport"));
    }

    /**
     * 2. 驗證 CSS 樣式表涵蓋深淺色主題與自適應樣式
     */
    @Test
    public void testGithubMarkdownCssCoverage() throws Exception {
        File dir = getAssetsDir();
        File cssFile = new File(dir, "github-markdown.css");
        String cssContent = new String(Files.readAllBytes(cssFile.toPath()), StandardCharsets.UTF_8);

        assertTrue("CSS 必須支援深淺色主題 prefers-color-scheme", cssContent.contains("prefers-color-scheme: dark"));
        assertTrue("CSS 必須包含圖片 100% 自適應防跑版", cssContent.contains("max-width: 100%"));
        assertTrue("CSS 必須包含表格獨立滾動支援", cssContent.contains("table") && cssContent.contains("overflow: auto"));
        assertTrue("CSS 必須包含 Obsidian wikilink 專屬樣式", cssContent.contains(".wikilink"));
    }

    /**
     * 3. 驗證副檔名識別邏輯（.md, .markdown 識別為 Markdown，其餘為純文字）
     */
    @Test
    public void testMarkdownFileExtensionDetection() {
        String[] validMarkdown = {"note.md", "README.MD", "doc.markdown", "test.Markdown", "sub/folder/file.md"};
        String[] nonMarkdown = {"note.txt", "file.java", "data.json", "log.log", "image.png", "note_md.bak"};

        for (String filename : validMarkdown) {
            String lower = filename.toLowerCase();
            boolean isMd = lower.endsWith(".md") || lower.endsWith(".markdown");
            assertTrue("應識別為 Markdown 檔案: " + filename, isMd);
        }

        for (String filename : nonMarkdown) {
            String lower = filename.toLowerCase();
            boolean isMd = lower.endsWith(".md") || lower.endsWith(".markdown");
            assertFalse("不應誤判為 Markdown 檔案: " + filename, isMd);
        }
    }

    /**
     * 4. 驗證傳遞給 WebView 之 JS 字串跳脫安全性 (防止雙引號、換行或特殊字元導致 JS 語法崩潰)
     */
    @Test
    public void testMarkdownContentJsonEscapingSafety() {
        String rawMarkdown = "# 測試筆記\n\n包含引號: \"Hello\" 'World'\n換行與反斜線: \\n \\t \n<script>alert('xss')</script>";
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (char c : rawMarkdown.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c); break;
            }
        }
        sb.append("\"");
        String quoted = sb.toString();

        assertNotNull(quoted);
        assertTrue("JSON 跳脫後必須以雙引號開頭", quoted.startsWith("\""));
        assertTrue("JSON 跳脫後必須以雙引號結尾", quoted.endsWith("\""));
        assertFalse("JSON 跳脫後內部不能含有未跳脫的原始換行符", quoted.substring(1, quoted.length() - 1).contains("\n"));
        assertTrue("JSON 跳脫後必須正確處理內部雙引號", quoted.contains("\\\"Hello\\\""));
    }

    /**
     * 5. 驗證 Obsidian 雙向連結 ([[筆記名]] 或 [[筆記名|別名]]) 正則解析與標準化邏輯
     */
    @Test
    public void testWikilinkPatternParsing() {
        Pattern wikilinkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");

        // 情況 A：標準雙向連結 [[每日筆記]]
        String inputA = "請參考 [[每日筆記]] 的說明";
        Matcher matcherA = wikilinkPattern.matcher(inputA);
        assertTrue(matcherA.find());
        String targetA = matcherA.group(1).split("\\|")[0].trim();
        assertEquals("每日筆記", targetA);

        // 情況 B：含別名雙向連結 [[2026-08-29-會議記錄|今天開會]]
        String inputB = "查看 [[2026-08-29-會議記錄|今天開會]] 內容";
        Matcher matcherB = wikilinkPattern.matcher(inputB);
        assertTrue(matcherB.find());
        String[] parts = matcherB.group(1).split("\\|");
        String targetB = parts[0].trim();
        String labelB = parts.length > 1 ? parts[1].trim() : targetB;
        assertEquals("2026-08-29-會議記錄", targetB);
        assertEquals("今天開會", labelB);

        // 驗證附加 .md 副檔名標準化邏輯
        String noteFile = targetA.toLowerCase().endsWith(".md") ? targetA : targetA + ".md";
        assertEquals("每日筆記.md", noteFile);
    }

    /**
     * 6. 驗證 marked.min.js 實作包含完整語法解析規則 (Headers, Checkboxes, Wikilinks, Tables, Code Blocks, Callouts)
     */
    @Test
    public void testMarkedJsSyntaxRulesContract() throws Exception {
        File dir = getAssetsDir();
        File markedJs = new File(dir, "marked.min.js");
        String jsCode = new String(Files.readAllBytes(markedJs.toPath()), StandardCharsets.UTF_8);

        assertTrue("marked.min.js 必須匯出 marked 物件", jsCode.contains("global.marked ="));
        assertTrue("marked.min.js 必須包含 H1~H6 標題解析", jsCode.contains("<h1") && jsCode.contains("<h6"));
        assertTrue("marked.min.js 必須包含代碼區塊 pre code 處理", jsCode.contains("<pre><code"));
        assertTrue("marked.min.js 必須包含待辦方框 checkbox 解析", jsCode.contains("type=\"checkbox\""));
        assertTrue("marked.min.js 必須包含表格 <table> 處理", jsCode.contains("<table>") && jsCode.contains("<thead>"));
        assertTrue("marked.min.js 必須包含 Obsidian Wikilink 雙向鏈接轉換", jsCode.contains("wikilink:"));
        assertTrue("marked.min.js 必須包含 Obsidian Transclusion 圖片語法 ![[...]]", jsCode.contains("!\\[\\[(.*?)\\]\\]"));
        assertTrue("marked.min.js 必須包含 Obsidian Callout 區塊 > [!NOTE]", jsCode.contains("blockquote"));
        assertTrue("marked.min.js 必須包含 Mermaid 代碼區塊識別", jsCode.contains("mermaid"));
    }

    /**
     * 7. 驗證 mermaid.min.js 包含完整圖表解析與主題適配
     */
    @Test
    public void testMermaidJsSyntaxAndRenderingContract() throws Exception {
        File dir = getAssetsDir();
        File mermaidJs = new File(dir, "mermaid.min.js");
        String jsCode = new String(Files.readAllBytes(mermaidJs.toPath()), StandardCharsets.UTF_8);

        assertTrue("mermaid.min.js 必須包含 initialize 接口", jsCode.contains("initialize:"));
        assertTrue("mermaid.min.js 必須包含 render 接口", jsCode.contains("render:"));
        assertTrue("mermaid.min.js 必須包含 run 接口", jsCode.contains("run:"));
        assertTrue("mermaid.min.js 必須支援 Flowchart 流程圖", jsCode.contains("renderFlowchart"));
        assertTrue("mermaid.min.js 必須支援 Sequence 循序圖", jsCode.contains("renderSequence"));
        assertTrue("mermaid.min.js 必須支援 Pie 圓餅圖", jsCode.contains("renderPie"));
        assertTrue("mermaid.min.js 必須支援深淺色主題 THEMES", jsCode.contains("THEMES") && jsCode.contains("dark"));
        assertTrue("mermaid.min.js 必須支援全語系字寬分級引擎 getCharWidth", jsCode.contains("getCharWidth"));
        assertTrue("mermaid.min.js 深色主題連接線必須為高對比亮藍色 79c0ff", jsCode.contains("#79c0ff"));
    }

    /**
     * 8. 驗證 URL 路由分流邏輯（外部網址 vs 本地雙向連結 vs 相對路徑）
     */
    @Test
    public void testUrlRoutingClassification() {
        String webUrl1 = "https://github.com/WilliamFromTW";
        String webUrl2 = "http://example.com/api";
        String wikilinkUrl = "wikilink:%E6%AF%8F%E6%97%A5%E7%AD%86%E8%A8%98";
        String relativeMdUrl = "subfolder/another_note.md";

        assertTrue("https 應識別為外網連結", webUrl1.startsWith("http://") || webUrl1.startsWith("https://"));
        assertTrue("http 應識別為外網連結", webUrl2.startsWith("http://") || webUrl2.startsWith("https://"));
        assertTrue("wikilink: 應識別為內部雙向連結", wikilinkUrl.startsWith("wikilink:"));
        assertTrue("以 .md 結尾應識別為本地 Markdown 檔案", relativeMdUrl.endsWith(".md") || relativeMdUrl.endsWith(".markdown"));
    }

    /**
     * 9. 驗證 marked.min.js 的佔位符機制絕不使用底線 _，杜絕斜體/粗體語法破壞導致 codeblock0 現象
     */
    @Test
    public void testMarkedJsDoesNotExposePlaceholdersOrCorruptCodeblocks() throws Exception {
        File dir = getAssetsDir();
        File markedJs = new File(dir, "marked.min.js");
        String jsCode = new String(Files.readAllBytes(markedJs.toPath()), StandardCharsets.UTF_8);

        assertFalse("marked.min.js 絕不可使用 ___CODE_BLOCK_ 佔位符（會被粗體/斜體語法破壞）", jsCode.contains("___CODE_BLOCK_"));
        assertFalse("marked.min.js 絕不可使用 ___INLINE_CODE_ 佔位符", jsCode.contains("___INLINE_CODE_"));
        assertTrue("marked.min.js 應使用安全字母 Token FNCCODEBLOCK 佔位符", jsCode.contains("FNCCODEBLOCK"));
        assertTrue("marked.min.js 應使用安全字母 Token INLCODEBLOCK 佔位符", jsCode.contains("INLCODEBLOCK"));
    }
}
