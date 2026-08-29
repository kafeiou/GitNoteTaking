package inmethod.gitnotetaking;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 專屬 PDF 離線預覽 Activity
 * 支援 100% 離線渲染、手勢雙指縮放、密碼保護檔案解鎖與外部 Reader 轉發
 */
public class ViewPdfActivity extends AppCompatActivity {

    private static final String TAG = "ViewPdfActivity";
    private static final Map<String, String> sSessionPasswordCache = new HashMap<>();

    private String mFilePath;
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private Toolbar mToolbar;
    private int mTotalPages = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.setView(this, null);
        setContentView(R.layout.activity_view_pdf);

        mFilePath = getIntent().getStringExtra("FILE_PATH");
        if (mFilePath == null || mFilePath.isEmpty()) {
            Toast.makeText(this, "Invalid PDF path", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(mFilePath);
        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mToolbar = findViewById(R.id.toolbarPdf);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(file.getName());
        }

        mProgressBar = findViewById(R.id.progressBarPdf);
        mWebView = findViewById(R.id.webViewPdf);

        initWebView();
    }

    private void initWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        mWebView.addJavascriptInterface(new PdfJsInterface(), "AndroidPDF");

        mProgressBar.setVisibility(View.VISIBLE);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadPdfFileIntoWebView();
            }
        });

        mWebView.loadUrl("file:///android_asset/pdf/pdf_viewer.html");
    }

    private void loadPdfFileIntoWebView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(mFilePath);
                    byte[] bytes = new byte[(int) file.length()];
                    try (InputStream is = new FileInputStream(file)) {
                        int read = 0;
                        int offset = 0;
                        while (offset < bytes.length && (read = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                            offset += read;
                        }
                    }
                    final String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.evaluateJavascript("loadPdfFromBase64(\"" + base64 + "\");", null);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read PDF file", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(ViewPdfActivity.this, "Failed to load PDF file", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public class PdfJsInterface {
        @JavascriptInterface
        public void onPasswordRequired(final boolean isWrongPassword) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);

                    // Check if cached session password works and wasn't wrong
                    String cachedPwd = sSessionPasswordCache.get(mFilePath);
                    if (cachedPwd != null && !cachedPwd.isEmpty() && !isWrongPassword) {
                        sendPasswordToWebView(cachedPwd);
                        return;
                    }

                    showPasswordPromptDialog(isWrongPassword);
                }
            });
        }

        @JavascriptInterface
        public void onPdfLoaded(final int numPages) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTotalPages = numPages;
                    mProgressBar.setVisibility(View.GONE);
                    if (getSupportActionBar() != null) {
                        String indicator = getString(R.string.pdf_pages_indicator, 1, numPages);
                        getSupportActionBar().setSubtitle(indicator);
                    }
                }
            });
        }

        @JavascriptInterface
        public void onPdfLoadError(final String errorMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                    Log.e(TAG, "PDF Load Error: " + errorMessage);
                }
            });
        }
    }

    private void showPasswordPromptDialog(boolean isWrongPassword) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.pdf_password_dialog_hint);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.pdf_password_dialog_title)
                .setMessage(isWrongPassword ? getString(R.string.pdf_password_incorrect) : getString(R.string.pdf_password_dialog_message))
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = input.getText().toString();
                        if (!password.isEmpty()) {
                            sSessionPasswordCache.put(mFilePath, password);
                            mProgressBar.setVisibility(View.VISIBLE);
                            sendPasswordToWebView(password);
                        } else {
                            showPasswordPromptDialog(true);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        builder.show();
    }

    private void sendPasswordToWebView(String password) {
        String quoted = JSONObject.quote(password);
        mWebView.evaluateJavascript("providePassword(" + quoted + ");", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_pdf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_open_external) {
            openWithExternalReader();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openWithExternalReader() {
        try {
            File file = new File(mFilePath);
            if (!file.exists()) return;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            String authority = getPackageName() + ".fileprovider";
            Uri uri = FileProvider.getUriForFile(this, authority, file);
            intent.setDataAndType(uri, "application/pdf");

            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening external PDF reader", e);
        }
    }
}
