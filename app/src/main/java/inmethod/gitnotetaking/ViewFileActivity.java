package inmethod.gitnotetaking;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.content.res.Configuration;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.KeyListener;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.Formatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.content.pm.PackageManager;
import inmethod.gitnotetaking.utility.FileUtility;
import inmethod.gitnotetaking.utility.PermissionHelper;

import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import inmethod.gitnotetaking.utility.MyGitUtility;
import inmethod.gitnotetaking.view.FileExplorerListAdapter;


public class ViewFileActivity extends AppCompatActivity implements PickiTCallbacks {

    public static final String TAG = MainActivity.TAG;
    public static final int REQUEST_TAKE_PHOTO = 100;
    public static int READ_REQUEST_CODE = 2;
    ListView view = null;
    FileExplorerListAdapter adapter = null;
    EditText editText;
    LinearLayout layoutAttachment;
    KeyListener listener = null;
    String currentPhotoPath;
    boolean shouldBlink = true;
    private Activity activity = this;
    private List<String> m_item;
    private List<String> m_path;
    private List<String> m_files;
    private List<String> m_filesPath;
    private String sFilePath;
    private String sGitRemoteUrl;
    private File file = null;
    private MenuItem itemPreview;
    private MenuItem itemEdit;
    private MenuItem itemSave;
    private WebView webViewMarkdown;
    private ScrollView scrollView2;
    private HorizontalScrollView scrollAttachment;
    private boolean isMarkdownFile = false;
    private boolean isHtmlFile = false;
    private boolean isPreviewMode = false;

    private boolean isPreviewable() {
        return isMarkdownFile || isHtmlFile;
    }
    private File photoFile;
    //  TextView tvCountFiles;
    private boolean isModify = false;
    PickiT pickiT;


    public static int countFilesInDirectory(File directory) {

        int count = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                count++;
            }
            if (file.isDirectory()) {
                count += countFilesInDirectory(file);
            }
        }
        return count;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    /*Save your data to be restored here
    Example : outState.putLong("time_state", time); , time is a long variable*/
        outState.putBoolean("isModify", isModify);
        outState.putString("editText", editText.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickiT = new PickiT(this, this, this);

        if (savedInstanceState != null) {
       /*When rotation occurs
        Example : time = savedInstanceState.getLong("time_state", 0); */
            isModify = savedInstanceState.getBoolean("isModify");
        } else {
            isModify = false;
            //When onCreate is called for the first time
        }

        setContentView(R.layout.activity_view_file_main);
        View view =  findViewById(android.R.id.content);
        MyApplication.setView(activity,view);

        Intent myIntent = getIntent(); // gets the previously created intent
        sFilePath = myIntent.getStringExtra("FILE_PATH");
        sGitRemoteUrl = myIntent.getStringExtra("GIT_REMOTE_URL");
        Toolbar toolbar = findViewById(R.id.toolbar3);
        file = new File(sFilePath);
        if (file.exists())
            toolbar.setTitle(file.getName());
        else
            toolbar.setTitle("");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        editText = findViewById(R.id.editFile);
        editText.setText("");
        listener = editText.getKeyListener();

        webViewMarkdown = findViewById(R.id.webViewMarkdown);
        scrollView2 = findViewById(R.id.scrollView2);
        scrollAttachment = findViewById(R.id.scrollAttachment);

        if (file != null && file.getName() != null) {
            String nameLower = file.getName().toLowerCase();
            isMarkdownFile = nameLower.endsWith(".md") || nameLower.endsWith(".markdown");
            isHtmlFile = nameLower.endsWith(".html") || nameLower.endsWith(".htm");
        }

        try {
            layoutAttachment = findViewById(R.id.layoutAttachment);
            layoutAttachment.removeAllViews();

            int iTextSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18"));
            editText.setTextSize(iTextSize);
            editText.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            editText.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.window_bg));
            if (scrollView2 != null) {
                scrollView2.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.window_bg));
            }
            if (webViewMarkdown != null) {
                webViewMarkdown.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.window_bg));
            }

            if (file.exists()) {
                if (isModify) {
                    editText.setText(savedInstanceState.getString("editText"));
                } else {
                    editText.setText("");
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);
                    while (br.ready()) {
                        editText.append(br.readLine() + "\n");
                    }
                    fr.close();

                }
                Log.d(TAG, "file = " + file.getCanonicalPath());

                if (isPreviewable() && webViewMarkdown != null) {
                    WebSettings settings = webViewMarkdown.getSettings();
                    settings.setJavaScriptEnabled(true);
                    settings.setAllowFileAccess(true);
                    settings.setAllowContentAccess(true);
                    settings.setDomStorageEnabled(true);
                    settings.setBuiltInZoomControls(true);
                    settings.setDisplayZoomControls(false);

                    webViewMarkdown.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            return handleUrlLoading(request.getUrl().toString());
                        }

                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            return handleUrlLoading(url);
                        }

                        @Override
                        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                            super.onReceivedError(view, request, error);
                            if (request != null && request.isForMainFrame()) {
                                if (isMarkdownFile) {
                                    renderMarkdownInWebView();
                                }
                            }
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            if (isMarkdownFile) {
                                renderMarkdownInWebView();
                            }
                        }
                    });

                    if (isMarkdownFile) {
                        webViewMarkdown.loadUrl("file:///android_asset/markdown/preview.html");
                    } else if (isHtmlFile) {
                        renderHtmlInWebView();
                    }

                    if (!isModify) {
                        isPreviewMode = true;
                        webViewMarkdown.setVisibility(View.VISIBLE);
                        if (scrollView2 != null) scrollView2.setVisibility(View.GONE);
                        if (scrollAttachment != null) scrollAttachment.setVisibility(View.GONE);
                    } else {
                        isPreviewMode = false;
                        webViewMarkdown.setVisibility(View.GONE);
                        if (scrollView2 != null) scrollView2.setVisibility(View.VISIBLE);
                        if (scrollAttachment != null) scrollAttachment.setVisibility(View.VISIBLE);
                    }
                } else {
                    isPreviewMode = false;
                    if (webViewMarkdown != null) webViewMarkdown.setVisibility(View.GONE);
                    if (scrollView2 != null) scrollView2.setVisibility(View.VISIBLE);
                    if (scrollAttachment != null) scrollAttachment.setVisibility(View.VISIBLE);
                }

                File attachDirectory = new File(file.getAbsolutePath() + "_attach");
                int iFileCount = 0;
                if (attachDirectory.isDirectory()) {
                    for (final File file : attachDirectory.listFiles()) {
                        if (file.isFile()) {
                            iFileCount++;
                            final TextView aTV = new TextView(activity);
                            aTV.setTextColor(Color.BLUE);
                            aTV.setBackgroundColor(Color.LTGRAY);

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(0, 0, 10, 0);
                            aTV.setLayoutParams(lp);
                            final Uri filuri = Uri.fromFile(file);
                            String sMimeType = getMimeType(filuri, activity);
                            aTV.setCompoundDrawablesWithIntrinsicBounds(getAttachmentIconResByFileName(file.getName(), sMimeType), 0, 0, 0);
                            aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);

                            aTV.setTextSize(iTextSize);
                            aTV.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    showAttachmentBottomSheet(file, aTV);
                                }
                            });
                            aTV.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View view) {
                                    showAttachmentBottomSheet(file, aTV);
                                    return true;
                                }
                            });
                            layoutAttachment.addView(aTV);
                        }
                    }
                    //           tvCountFiles.setText(countFilesInDirectory(attachDirectory) + getResources().getString(R.string.view_attach_files));
                    //       tvCountFiles.setTextColor(Color.BLUE);
                } else {
                    //        tvCountFiles.setText("");
                }
            } else {
                Toast.makeText(activity, "File read error!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static int getAttachmentIconResByFileName(String fileName, String mimeType) {
        if (mimeType != null && !mimeType.isEmpty()) {
            int icon = getAttachmentIconRes(mimeType);
            if (icon != R.drawable.unknown24) {
                return icon;
            }
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
                return R.drawable.image24;
            } else if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log")) {
                return R.drawable.txt24;
            } else if (lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv")) {
                return R.drawable.xls24;
            } else if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
                return R.drawable.doc24;
            } else if (lower.endsWith(".pdf")) {
                return R.drawable.pdf24;
            } else if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) {
                return R.drawable.ppt24;
            }
        }
        return R.drawable.unknown24;
    }

    public static int getAttachmentIconRes(String mimeType) {
        if (mimeType == null) return R.drawable.unknown24;
        String lower = mimeType.toLowerCase(Locale.ROOT);
        if (lower.contains("image")) {
            return R.drawable.image24;
        } else if (lower.contains("plain")) {
            return R.drawable.txt24;
        } else if (lower.contains("excel") || lower.contains("spreadsheet") || lower.contains("sheet")) {
            return R.drawable.xls24;
        } else if (lower.contains("powerpoint") || lower.contains("presentation")) {
            return R.drawable.ppt24;
        } else if (lower.contains("word") || lower.contains("wordprocessingml") || lower.contains("msword")) {
            return R.drawable.doc24;
        } else if (lower.contains("pdf")) {
            return R.drawable.pdf24;
        } else {
            return R.drawable.unknown24;
        }
    }

    public static String getAttachmentTypeDisplay(String fileName, String mimeType) {
        if (mimeType != null && !mimeType.trim().isEmpty()) {
            return mimeType.trim();
        }
        if (fileName != null) {
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx >= 0 && dotIdx < fileName.length() - 1) {
                return fileName.substring(dotIdx + 1).toUpperCase(Locale.ROOT);
            }
        }
        return "UNKNOWN";
    }

    public static String getAttachmentDeleteCommitMessage(String fileName) {
        return "User deleted attachment: " + fileName;
    }

    private void showAttachmentBottomSheet(final File attachFile, final TextView aTV) {
        if (attachFile == null || !attachFile.exists()) {
            Toast.makeText(this, R.string.attach_sheet_file_not_found, Toast.LENGTH_SHORT).show();
            if (layoutAttachment != null && aTV != null) {
                layoutAttachment.removeView(aTV);
                if (layoutAttachment.getChildCount() == 0 && scrollAttachment != null) {
                    scrollAttachment.setVisibility(View.GONE);
                }
            }
            return;
        }

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_attachment_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(R.color.card_bg);
        }

        ImageView ivIcon = sheetView.findViewById(R.id.ivAttachIcon);
        TextView tvName = sheetView.findViewById(R.id.tvAttachFileName);
        TextView tvSize = sheetView.findViewById(R.id.tvAttachSize);
        TextView tvType = sheetView.findViewById(R.id.tvAttachType);
        TextView tvModified = sheetView.findViewById(R.id.tvAttachModified);
        Button btnOpen = sheetView.findViewById(R.id.btnOpenAttachment);
        Button btnDownload = sheetView.findViewById(R.id.btnDownloadAttachment);
        Button btnDelete = sheetView.findViewById(R.id.btnDeleteAttachment);

        tvName.setText(attachFile.getName());

        Uri fileUri = Uri.fromFile(attachFile);
        String sMimeType = getMimeType(fileUri, this);
        ivIcon.setImageResource(getAttachmentIconResByFileName(attachFile.getName(), sMimeType));

        String sizeFormatted = Formatter.formatFileSize(this, attachFile.length());
        tvSize.setText(getString(R.string.attach_sheet_size, sizeFormatted));

        String typeDisplay = getAttachmentTypeDisplay(attachFile.getName(), sMimeType);
        tvType.setText(getString(R.string.attach_sheet_type, typeDisplay));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String modifiedFormatted = sdf.format(new Date(attachFile.lastModified()));
        tvModified.setText(getString(R.string.attach_sheet_modified, modifiedFormatted));

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                if (attachFile.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    Intent intent = new Intent(ViewFileActivity.this, ViewPdfActivity.class);
                    intent.putExtra("FILE_PATH", attachFile.getAbsolutePath());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    String authority = activity.getPackageName() + ".fileprovider";
                    Uri contentUri = FileProvider.getUriForFile(activity, authority, attachFile);
                    intent.setDataAndType(contentUri, getMimeType(contentUri, activity));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, R.string.attach_sheet_no_app, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File dest = new File(downloadDir, attachFile.getName());
                    if (dest.exists()) {
                        Toast.makeText(activity, getString(R.string.download_file_exists) + "\n" + attachFile.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        try (FileOutputStream fos = new FileOutputStream(dest)) {
                            Files.copy(attachFile.toPath(), fos);
                        }
                        if (dest.exists()) {
                            Toast.makeText(activity, getString(R.string.attach_sheet_download_success, attachFile.getName()), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(activity, getString(R.string.download_file_exists) + "\n" + attachFile.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.view_title_remove_attach)
                        .setMessage(attachFile.getName())
                        .setCancelable(true)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    attachFile.getCanonicalFile().delete();
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG, "commit when attachment deleted");
                                            String commitMsg = getAttachmentDeleteCommitMessage(attachFile.getName());
                                            MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, commitMsg);
                                        }
                                    }).start();
                                    if (layoutAttachment != null && aTV != null) {
                                        layoutAttachment.removeView(aTV);
                                        if (layoutAttachment.getChildCount() == 0 && scrollAttachment != null) {
                                            scrollAttachment.setVisibility(View.GONE);
                                        }
                                    }
                                    bottomSheetDialog.dismiss();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        bottomSheetDialog.show();
    }

    private void disable() {
        editText.setKeyListener(null);
        if (itemEdit != null) itemEdit.setVisible(true);
        if (itemSave != null) itemSave.setVisible(false);
        if (isPreviewable() && itemPreview != null) {
            itemPreview.setVisible(!isPreviewMode);
        }
    }

    private void enable() {
        if (itemEdit != null) itemEdit.setVisible(false);
        if (itemSave != null) {
            itemSave.setVisible(true);
            SpannableString s = new SpannableString(itemSave.getTitle());
            s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
            itemSave.setTitle(s);
        }
        if (isPreviewable() && itemPreview != null) {
            itemPreview.setVisible(true);
        }
        editText.setKeyListener(listener);
        editText.setFocusableInTouchMode(true);
        editText.setFocusable(true);
        editText.requestFocus();
        editText.setText(editText.getText());
        editText.setPressed(true);
        editText.setSelection(editText.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    private void switchToPreviewMode() {
        isPreviewMode = true;
        isModify = false;
        if (webViewMarkdown != null) {
            if (isMarkdownFile) {
                renderMarkdownInWebView();
            } else if (isHtmlFile) {
                renderHtmlInWebView();
            }
            webViewMarkdown.setVisibility(View.VISIBLE);
        }
        if (scrollView2 != null) {
            scrollView2.setVisibility(View.GONE);
        }
        if (scrollAttachment != null) {
            scrollAttachment.setVisibility(View.GONE);
        }
        disable();
        hideSoftKeyboard();
        invalidateOptionsMenu();
    }

    private void renderHtmlInWebView() {
        if (webViewMarkdown == null || !isHtmlFile) return;
        String text = editText != null ? editText.getText().toString() : "";
        String baseUrl = null;
        if (file != null && file.getParentFile() != null) {
            baseUrl = "file://" + file.getParentFile().getAbsolutePath() + "/";
        }
        webViewMarkdown.loadDataWithBaseURL(baseUrl, text, "text/html", "UTF-8", null);
    }

    private void switchToEditMode() {
        isPreviewMode = false;
        isModify = true;
        if (webViewMarkdown != null) {
            webViewMarkdown.setVisibility(View.GONE);
        }
        if (scrollView2 != null) {
            scrollView2.setVisibility(View.VISIBLE);
        }
        if (scrollAttachment != null) {
            scrollAttachment.setVisibility(View.VISIBLE);
        }
        enable();
        invalidateOptionsMenu();
    }

    private void hideSoftKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            }
        }
    }

    private void renderMarkdownInWebView() {
        if (webViewMarkdown == null || !isMarkdownFile) return;
        boolean isNightMode = false;
        String themePref = PreferenceManager.getDefaultSharedPreferences(this).getString("AppTheme", "system");
        if ("dark".equalsIgnoreCase(themePref)) {
            isNightMode = true;
        } else if ("light".equalsIgnoreCase(themePref)) {
            isNightMode = false;
        } else {
            isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        }
        String text = editText != null ? editText.getText().toString() : "";
        String escaped = JSONObject.quote(text);
        String js = "setMarkdownContent(" + escaped + ", " + isNightMode + ");";
        webViewMarkdown.evaluateJavascript(js, null);
    }

    private boolean handleUrlLoading(String url) {
        if (url == null) return false;
        try {
            if (url.startsWith("wikilink:")) {
                String noteTarget = Uri.decode(url.substring("wikilink:".length()));
                openInternalNote(noteTarget);
                return true;
            } else if (url.startsWith("http://") || url.startsWith("https://")) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            } else if (url.contains("preview.html")) {
                return false;
            } else {
                String targetPath = url.startsWith("file://") ? Uri.parse(url).getPath() : url;
                if (targetPath != null) {
                    File targetFile = new File(targetPath);
                    if (!targetFile.exists() && file != null && file.getParentFile() != null) {
                        targetFile = new File(file.getParentFile(), targetPath);
                    }
                    if (!targetFile.exists()) {
                        String repoRoot = MyGitUtility.getLocalGitDirectory(this, sGitRemoteUrl);
                        if (repoRoot != null) {
                            String fileName = new File(targetPath).getName();
                            targetFile = findFileRecursive(new File(repoRoot), fileName);
                        }
                    }
                    if (targetFile != null && targetFile.exists()) {
                        Intent intent = new Intent(this, ViewFileActivity.class);
                        intent.putExtra("FILE_PATH", targetFile.getAbsolutePath());
                        intent.putExtra("GIT_REMOTE_URL", sGitRemoteUrl);
                        startActivity(intent);
                    } else {
                        String displayName = new File(targetPath).getName();
                        Toast.makeText(this, getString(R.string.toast_note_not_found) + ": " + displayName, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void openInternalNote(String noteName) {
        if (noteName == null || noteName.trim().isEmpty()) return;
        String cleanName = noteName.trim();
        if (!cleanName.toLowerCase().endsWith(".md")) {
            cleanName += ".md";
        }
        File targetFile = null;
        if (file != null && file.getParentFile() != null) {
            targetFile = new File(file.getParentFile(), cleanName);
        }
        if (targetFile == null || !targetFile.exists()) {
            String repoRoot = MyGitUtility.getLocalGitDirectory(this, sGitRemoteUrl);
            if (repoRoot != null) {
                targetFile = findFileRecursive(new File(repoRoot), cleanName);
            }
        }
        if (targetFile != null && targetFile.exists()) {
            Intent intent = new Intent(this, ViewFileActivity.class);
            intent.putExtra("FILE_PATH", targetFile.getAbsolutePath());
            intent.putExtra("GIT_REMOTE_URL", sGitRemoteUrl);
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.toast_note_not_found) + ": " + noteName, Toast.LENGTH_SHORT).show();
        }
    }

    private File findFileRecursive(File dir, String targetFileName) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().equalsIgnoreCase(targetFileName)) {
                return f;
            } else if (f.isDirectory() && !f.getName().startsWith(".")) {
                File found = findFileRecursive(f, targetFileName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void blink() {
        if (shouldBlink) {
            editText.setText(editText.getText());
            editText.setPressed(true);
            editText.setSelection(editText.getText().length());

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (shouldBlink) {
                        blink();
                    }
                }
            }, 500);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_file, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spanString.length();
            //spanString
            spanString.setSpan(new RelativeSizeSpan(1.2f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spanString);
        }
        itemPreview = menu.findItem(R.id.view_file_action_preview);
        itemEdit = menu.findItem(R.id.view_file_action_edit);
        itemSave = menu.findItem(R.id.view_file_action_save);

        if (isPreviewable()) {
            if (isPreviewMode) {
                if (itemPreview != null) itemPreview.setVisible(false);
                if (itemEdit != null) itemEdit.setVisible(true);
                if (itemSave != null) itemSave.setVisible(false);
            } else {
                if (itemPreview != null) itemPreview.setVisible(true);
                if (itemEdit != null) itemEdit.setVisible(false);
                if (itemSave != null) itemSave.setVisible(true);
            }
        } else {
            if (itemPreview != null) itemPreview.setVisible(false);
            if (isModify) {
                if (itemEdit != null) itemEdit.setVisible(false);
                if (itemSave != null) itemSave.setVisible(true);
            } else {
                if (itemEdit != null) itemEdit.setVisible(true);
                if (itemSave != null) itemSave.setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public void onBackPressed() {
        if (isModify) {
            Log.d(TAG, "onback");

            FileWriter fw = null;
            final EditText txtUrl = new EditText(this);
            txtUrl.setMaxLines(3);
            txtUrl.setLines(3);
            txtUrl.setText("");
            txtUrl.setTextSize(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18")));

            try {
                fw = new FileWriter(new File(sFilePath));
                BufferedWriter bw = new BufferedWriter(fw);
                fw.write(editText.getText().toString());
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("GitCheckBoxCommitMessage", false)) {
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.commit))
                        .setMessage(getResources().getString(R.string.commit_messages))
                        .setView(txtUrl)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.toast_pulling), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                isModify = false;
                                disable();

                               // boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, txtUrl.getText().toString());

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String commitMsg = txtUrl.getText().toString().trim();
                                            if (commitMsg.isEmpty())
                                                commitMsg = "<" + file.getName() + ">";
                                            else
                                                commitMsg = commitMsg + "\n<" + file.getName() + ">";
                                            boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, commitMsg);
                                            Thread.sleep(100);
                                            if (bCommitStatus) {
                                                if (sGitRemoteUrl.indexOf("local") == -1) {
                                                    if (bCommitStatus) {
                                                        MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);
                                                    }
                                                }

                                            }

                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                        }).show();
            } else {
                Log.d(TAG, "onback");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.toast_pulling), Toast.LENGTH_SHORT).show();
                    }
                });


                isModify = false;
                disable();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String commitMsg = txtUrl.getText().toString().trim();
                            if (commitMsg.isEmpty())
                                commitMsg = "<" + file.getName() + ">";
                            else
                                commitMsg = commitMsg + "\n<" + file.getName() + ">";
                            boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, commitMsg);
                            Thread.sleep(100);
                            if (bCommitStatus) {
                                if (sGitRemoteUrl.indexOf("local") == -1) {
                                    if (bCommitStatus) {
                                        MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);
                                    }
                                }

                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                super.onBackPressed();
            }
        } else super.onBackPressed();

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "temp picture = " + storageDir);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.view_file_action_preview) {
            switchToPreviewMode();
            return true;
        } else if (id == R.id.view_file_action_edit) {
            if (isPreviewable()) {
                switchToEditMode();
            } else {
                enable();
                isModify = true;
            }
            return true;
        } else if (id == R.id.view_file_action_camera_picture) {
            PermissionHelper.requestCamera(this, new PermissionHelper.PermissionCallback() {
                @Override
                public void onGranted() {
                    launchCamera();
                }

                @Override
                public void onDenied() {
                    // Silent cancel per user requirements
                }
            });
            return true;
        } else if (id == R.id.view_file_action_save) {
            FileWriter fw = null;
            final EditText txtUrl = new EditText(this);
            //  txtUrl.setHint("your hint");
            txtUrl.setMaxLines(3);
            txtUrl.setLines(3);
            txtUrl.setTextSize(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18")));

            try {
                fw = new FileWriter(new File(sFilePath));
                BufferedWriter bw = new BufferedWriter(fw);
                fw.write(editText.getText().toString());
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("GitCheckBoxCommitMessage", false)) {

                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.commit))
                        .setMessage(getResources().getString(R.string.commit_messages))
                        .setView(txtUrl)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.toast_pulling), Toast.LENGTH_SHORT).show();
                                        if (isPreviewable()) {
                                            switchToPreviewMode();
                                        } else {
                                            isModify = false;
                                            disable();
                                        }
                                    }
                                });

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String commitMsg = txtUrl.getText().toString().trim();
                                        if (commitMsg.isEmpty())
                                            commitMsg = "<" + file.getName() + ">";
                                        else
                                            commitMsg = commitMsg + "\n<" + file.getName() + ">";
                                        boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, commitMsg);
                                        if (bCommitStatus) {
                                            if (sGitRemoteUrl.indexOf("local") == -1) {
                                                MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);
                                            }
                                        }


                                    }
                                }).start();
                            }
                        }).show();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.toast_pulling), Toast.LENGTH_SHORT).show();
                        if (isPreviewable()) {
                            switchToPreviewMode();
                        } else {
                            isModify = false;
                            disable();
                        }
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String commitMsg = txtUrl.getText().toString().trim();
                            if (commitMsg.isEmpty())
                                commitMsg = "<" + file.getName() + ">";
                            else
                                commitMsg = commitMsg + "\n<" + file.getName() + ">";
                            Log.d(TAG,"commit when view_file_action_save be triggered");

                            boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, commitMsg);
                            Thread.sleep(100);
                            if (bCommitStatus) {
                                if (sGitRemoteUrl.indexOf("local") == -1) {
                                    MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            return true;
        } else if (id == R.id.view_file_action_select_file) {
            openDocumentPicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "createImageFile failed", ex);
            }
            String authority = activity.getPackageName() + ".fileprovider";
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        authority,
                        photoFile);
                Log.d(TAG, "photoURI" + photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionHelper.resetDenialCount(this, Manifest.permission.CAMERA);
                launchCamera();
            } else {
                PermissionHelper.incrementDenialCount(this, Manifest.permission.CAMERA);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == READ_REQUEST_CODE) {
            if (resultData != null && resultData.getData() != null) {
                showAddAttachmentDialog(resultData.getData());
            }
        } else if (requestCode == REQUEST_TAKE_PHOTO) {
            try {

                final File aDestFileDirectory = new File(file.getCanonicalPath().toString() + "_attach".trim());
                //  Log.d(TAG,"aDestFileDirectory file = "+aDestFileDirectory.getCanonicalPath());
                if (!aDestFileDirectory.isDirectory())
                    aDestFileDirectory.mkdir();
                final EditText txtUrl = new EditText(this);
                txtUrl.setText(photoFile.getName());
                txtUrl.setMaxLines(3);
                txtUrl.setLines(3);
                txtUrl.setTextSize(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18")));

                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.dialog_title_modify))
                        .setMessage(getResources().getString(R.string.dialog_file_name))
                        .setView(txtUrl)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                isModify = false;
                                disable();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String sPhotoName = txtUrl.getText().toString().trim();
                                        if (sPhotoName.isEmpty()) {
                                            runOnUiThread(() -> Toast.makeText(MyApplication.getAppContext(), R.string.input_cannot_be_empty, Toast.LENGTH_SHORT).show());
                                            return;
                                        }
                                        final File aDestFile;
                                        try {
                                            aDestFile = new File(aDestFileDirectory.getCanonicalPath() + File.separator + sPhotoName);
                                            //     Log.d(TAG,"dest file = "+aDestFile.getCanonicalPath());
                                            final String sDestFileNameString;
                                            sDestFileNameString = aDestFile.getCanonicalPath().toString().substring(MyGitUtility.getLocalGitDirectory(activity, sGitRemoteUrl).length());
                                            Files.copy(photoFile.toPath(), aDestFile.toPath());
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        Thread.sleep(1000);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                    boolean bCommit = false;
                                                    Log.d(TAG,"commit when REQUEST_TAKE_PHOTO be triggered");

                                                    bCommit = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, MyApplication.getAppContext().getString(R.string.view_file_add_attachment_file_commit) + "\n" + sDestFileNameString);
                                                    if (sGitRemoteUrl.indexOf("local") == -1 && bCommit)
                                                        MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);

                                                }
                                            }).start();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }).start();
                                finish();
                                startActivity(getIntent());

                            }
                        }).show();


            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyApplication.getAppContext(), "Add Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        }
    }

    public String getMimeType(Uri uri, Context context) {
        String mimeType = null;
        try {
            if (Objects.equals(uri.getScheme(), ContentResolver.SCHEME_CONTENT)) {
                ContentResolver cr = context.getContentResolver();
                mimeType = cr.getType(uri);
            } else {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                        .toString());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        fileExtension.toLowerCase());
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return mimeType;
    }


    //When selecting a file from Google Drive, for example, the Uri will be returned before the file is available(if it has not yet been cached/downloaded).
    //We are unable to see the progress
    //Apps like Dropbox will display a dialog inside the picker
    //This will only be called when selecting a drive file
    @Override
    public void PickiTonUriReturned() {
        //Use to let user know that we are waiting for the application to return the file
        //See the demo project to see how I used this.
    }

    //Called when the file creations starts (similar to onPreExecute)
    //This will only be called if the selected file is not local or if the file is from an unknown file provider
    @Override
    public void PickiTonStartListener() {
        //Can be used to display a ProgressDialog
    }

    //Returns the progress of the file being created (in percentage)
    //This will only be called if the selected file is not local or if the file is from an unknown file provider
    @Override
    public void PickiTonProgressUpdate(int progress) {
        //Can be used to update the progress of your dialog
    }

    //If the selected file was a local file then this will be called directly, returning the path as a String.
    //String path - returned path
    //boolean wasDriveFile - check if it was a drive file
    //boolean wasUnknownProvider - check if it was from an unknown file provider
    //boolean wasSuccessful - check if it was successful
    //String reason - the get the reason why wasSuccessful returned false
    private void showAddAttachmentDialog(final Uri uri) {
        final String originalFileName = FileUtility.getFileName(this, uri);
        try {
            final File aDestFileDirectory = new File(file.getCanonicalPath().toString() + "_attach".trim());
            if (!aDestFileDirectory.isDirectory())
                aDestFileDirectory.mkdir();

            final EditText txtUrl = new EditText(this);
            txtUrl.setText(originalFileName);
            txtUrl.setMaxLines(3);
            txtUrl.setLines(3);
            txtUrl.setTextSize(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18")));

            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_title_add))
                    .setMessage(getResources().getString(R.string.dialog_file_name))
                    .setView(txtUrl)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final String sFileName = txtUrl.getText().toString().trim();
                            if (sFileName.isEmpty()) {
                                Toast.makeText(MyApplication.getAppContext(), R.string.input_cannot_be_empty, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            isModify = false;
                            disable();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final File aDestFile = new File(aDestFileDirectory, sFileName);
                                    boolean copied = FileUtility.copyUriToFile(activity, uri, aDestFile);
                                    if (!copied) {
                                        runOnUiThread(() -> Toast.makeText(MyApplication.getAppContext(), "Add Failed!", Toast.LENGTH_SHORT).show());
                                        return;
                                    }
                                    try {
                                        final String sDestFileNameString;
                                        String localGitDir = MyGitUtility.getLocalGitDirectory(activity, sGitRemoteUrl);
                                        if (aDestFile.getCanonicalPath().startsWith(localGitDir)) {
                                            String rel = aDestFile.getCanonicalPath().substring(localGitDir.length());
                                            sDestFileNameString = (rel.startsWith(File.separator) || rel.startsWith("/")) ? rel.substring(1) : rel;
                                        } else {
                                            sDestFileNameString = aDestFile.getName();
                                        }

                                        boolean bCommit = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, MyApplication.getAppContext().getString(R.string.view_file_add_attachment_file_commit) + "\n" + sDestFileNameString);
                                        if (sGitRemoteUrl.indexOf("local") == -1 && bCommit)
                                            MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    runOnUiThread(() -> {
                                        finish();
                                        startActivity(getIntent());
                                    });
                                }
                            }).start();
                        }
                    }).setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String reason) {
    }

    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> arrayList, boolean b, String s) {

    }


}
