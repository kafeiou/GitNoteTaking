package inmethod.gitnotetaking.test;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * 自動化測試：語系配置、字串資源完整性與標籤一致性檢核
 */
public class LocaleAndResourceConsistencyTest {

    private static final String RES_DIR = "src/main/res";

    private File getResDir() {
        File f = new File(RES_DIR);
        if (!f.exists()) {
            f = new File("app/" + RES_DIR);
        }
        return f;
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    /**
     * 1. 驗證 locales_config.xml 與 values/arrays.xml 中的 language_values 100% 吻合對齊
     */
    @Test
    public void testLocalesConfigMatchesLanguageValues() throws Exception {
        File resDir = getResDir();
        File localesConfigFile = new File(resDir, "xml/locales_config.xml");
        assertTrue("locales_config.xml 必須存在", localesConfigFile.exists());

        Document localesDoc = parseXml(localesConfigFile);
        NodeList localeNodes = localesDoc.getElementsByTagName("locale");
        Set<String> declaredLocales = new HashSet<>();
        for (int i = 0; i < localeNodes.getLength(); i++) {
            Element el = (Element) localeNodes.item(i);
            declaredLocales.add(el.getAttribute("android:name"));
        }

        // 預期的 4 大核心語系
        assertTrue("必須包含繁中 zh-TW", declaredLocales.contains("zh-TW"));
        assertTrue("必須包含簡中 zh-CN", declaredLocales.contains("zh-CN"));
        assertTrue("必須包含日文 ja-JP (包含國家碼以杜絕 CJK 渲染延遲)", declaredLocales.contains("ja-JP"));
        assertTrue("必須包含英文 en", declaredLocales.contains("en"));

        // 驗證 arrays.xml 裡的 language_values
        File arraysFile = new File(resDir, "values/arrays.xml");
        assertTrue("values/arrays.xml 必須存在", arraysFile.exists());
        Document arraysDoc = parseXml(arraysFile);
        List<String> arrayValues = extractStringArrayItems(arraysDoc, "language_values");

        for (String locale : declaredLocales) {
            assertTrue("language_values 必須包含 locales_config.xml 宣告的 " + locale,
                    arrayValues.contains(locale));
        }
    }

    /**
     * 2. 驗證 4 國語系的 arrays.xml 中 language_entries 與 language_values 長度與順序一致
     */
    @Test
    public void testLanguageEntriesAndValuesCountMatch() throws Exception {
        File resDir = getResDir();
        String[] valuesDirs = {"values", "values-zh-rTW", "values-zh-rCN", "values-ja"};

        for (String dir : valuesDirs) {
            File arraysFile = new File(resDir, dir + "/arrays.xml");
            assertTrue(dir + "/arrays.xml 必須存在", arraysFile.exists());
            Document doc = parseXml(arraysFile);

            List<String> entries = extractStringArrayItems(doc, "language_entries");
            List<String> values = extractStringArrayItems(doc, "language_values");

            assertEquals(dir + " 的 language_entries 與 language_values 長度必須相同",
                    entries.size(), values.size());
            assertEquals(dir + " 必須包含 5 個選項 (系統預設 + 4 國語系)", 5, values.size());
            assertEquals("第一個選項必須為 system", "system", values.get(0));
            assertEquals("第二個選項必須為 zh-TW", "zh-TW", values.get(1));
            assertEquals("第三個選項必須為 zh-CN", "zh-CN", values.get(2));
            assertEquals("第四個選項必須為 ja-JP", "ja-JP", values.get(3));
            assertEquals("第五個選項必須為 en", "en", values.get(4));
        }
    }

    /**
     * 3. 驗證所有語系的 strings.xml 包含完整 Key，零遺漏翻譯
     */
    @Test
    public void testAllLanguageStringsComplete() throws Exception {
        File resDir = getResDir();
        File baseStringsFile = new File(resDir, "values/strings.xml");
        assertTrue("values/strings.xml 基準檔必須存在", baseStringsFile.exists());

        Set<String> baseKeys = extractStringKeys(parseXml(baseStringsFile));
        assertTrue("基準字串庫不可為空", !baseKeys.isEmpty());

        String[] targetDirs = {"values-zh-rTW", "values-zh-rCN", "values-ja"};
        for (String dir : targetDirs) {
            File stringsFile = new File(resDir, dir + "/strings.xml");
            assertTrue(dir + "/strings.xml 必須存在", stringsFile.exists());

            Set<String> targetKeys = extractStringKeys(parseXml(stringsFile));
            Set<String> missingKeys = new HashSet<>(baseKeys);
            missingKeys.removeAll(targetKeys);

            assertTrue("【" + dir + "】缺少了以下字串 Key: " + missingKeys, missingKeys.isEmpty());
        }
    }

    /**
     * 4. 驗證 values-zh-rHK 冗餘目錄已被徹底清理，無殘留檔案
     */
    @Test
    public void testNoHkResourcesRemain() {
        File resDir = getResDir();
        File hkDir = new File(resDir, "values-zh-rHK");
        assertFalse("values-zh-rHK 冗餘目錄必須已被刪除", hkDir.exists());
    }

    private List<String> extractStringArrayItems(Document doc, String arrayName) {
        List<String> items = new ArrayList<>();
        NodeList arrayNodes = doc.getElementsByTagName("string-array");
        for (int i = 0; i < arrayNodes.getLength(); i++) {
            Element arrayEl = (Element) arrayNodes.item(i);
            if (arrayName.equals(arrayEl.getAttribute("name"))) {
                NodeList itemNodes = arrayEl.getElementsByTagName("item");
                for (int j = 0; j < itemNodes.getLength(); j++) {
                    items.add(itemNodes.item(j).getTextContent().trim());
                }
            }
        }
        return items;
    }

    private Set<String> extractStringKeys(Document doc) {
        Set<String> keys = new HashSet<>();
        NodeList stringNodes = doc.getElementsByTagName("string");
        for (int i = 0; i < stringNodes.getLength(); i++) {
            Element stringEl = (Element) stringNodes.item(i);
            keys.add(stringEl.getAttribute("name"));
        }
        return keys;
    }
}
