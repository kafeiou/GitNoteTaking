package inmethod.gitnotetaking;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    private static Context context;
    private static ArrayList<File> aFileList = new ArrayList<File>();
    public final static int NONE = 0;
    public final static int CUT = 1;
    public final static int COPY = 2;
    private static int iStatus = NONE;


    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String lang = sp.getString("AppLanguage", "system");
        if ("ja".equalsIgnoreCase(lang)) {
            lang = "ja-JP";
        }
        if (!"system".equalsIgnoreCase(lang) && !lang.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        }

        String theme = sp.getString("AppTheme", "system");
        CustomPreferenceFragment.applyThemeMode(theme);

        try {
            org.eclipse.jgit.storage.file.WindowCacheConfig config = new org.eclipse.jgit.storage.file.WindowCacheConfig();
            config.setPackedGitLimit(10 * 1024 * 1024); // 10MB limit for mobile RAM safety
            config.setPackedGitWindowSize(8192); // 8KB window size
            config.setPackedGitMMAP(false); // Disable MMAP on Android to avoid leaks & fragmentation
            config.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isNetworkConnected(){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public static boolean isText(String sFileName){
        String lower = sFileName.toLowerCase();
        return lower.endsWith(".txt") ||
                lower.endsWith(".xml") ||
                lower.endsWith(".kt") ||
                lower.endsWith(".java") ||
                lower.endsWith(".html") ||
                lower.endsWith(".htm") ||
                lower.endsWith(".py") ||
                lower.endsWith(".sql") ||
                lower.endsWith(".md") ||
                lower.endsWith(".markdown");
    }

    public static boolean isLocal(String sRemoteUrl ){
       if(sRemoteUrl!=null && sRemoteUrl.indexOf("local")!=-1)
           return false;
       else return true;
    }

    public static void resetFiles(){
        aFileList.clear();
        iStatus = NONE;
    }
    public static void storeFiles(File aFile){
        aFileList.add(aFile);
    }

    public static List<File> getStoreFiles(){
        return aFileList;
    }

    public static void setCutCopyStatus(int i){
        iStatus = i;
    }

    public static int getCutCopyStatus(){
        return iStatus;
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }

    public static void setView(Activity act, View view) {
        if (act instanceof ComponentActivity) {
            EdgeToEdge.enable((ComponentActivity) act);
        }

        if (view != null) {
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                                | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }
}