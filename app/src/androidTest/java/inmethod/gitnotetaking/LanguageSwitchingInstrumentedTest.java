package inmethod.gitnotetaking;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 儀器測試：驗證在 Android 執行環境中動態切換 ApplicationLocales 與字串解析
 */
@RunWith(AndroidJUnit4.class)
public class LanguageSwitchingInstrumentedTest {

    @Test
    public void testDynamicLocaleSwitching() {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);

        // 1. 切換為繁體中文
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-TW"));
        assertEquals("語言設定", context.getString(R.string.pref_app_language));

        // 2. 切換為簡體中文
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"));
        assertEquals("语言设置", context.getString(R.string.pref_app_language));

        // 3. 切換為日文
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ja-JP"));
        assertEquals("言語設定", context.getString(R.string.pref_app_language));

        // 4. 切換為英文
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
        assertEquals("Language", context.getString(R.string.pref_app_language));

        // 5. 還原為系統預設
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
    }
}
