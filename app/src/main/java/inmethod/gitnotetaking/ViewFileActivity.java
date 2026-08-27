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
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.content.pm.PackageManager;
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
    private MenuItem itemEdit;
    private MenuItem itemSave;
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

        try {
            layoutAttachment = findViewById(R.id.layoutAttachment);
            layoutAttachment.removeAllViews();

            int iTextSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18"));
            editText.setTextSize(iTextSize);
            editText.setTextColor(Color.BLACK);

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
                            if (sMimeType == null) {
                                aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.unknown24, 0, 0, 0);
                                aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                            } else {
                                sMimeType = sMimeType.toLowerCase();
                                if (sMimeType.contains("image")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.image24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else if (sMimeType.contains("plain")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.txt24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else if (sMimeType.contains("excel")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.xls24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else if (sMimeType.contains("word")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.doc24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else if (sMimeType.contains("pdf")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pdf24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else if (sMimeType.contains("powerpoint")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ppt24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else if (sMimeType.contains("presentation")) {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ppt24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                } else {
                                    aTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.unknown24, 0, 0, 0);
                                    aTV.setText(MyApplication.getAppContext().getText(R.string.attachment).toString() + iFileCount);
                                }
                            }

                            aTV.setTextSize(iTextSize);
                            aTV.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Log.d(TAG, "filuri=" + filuri.toString());
                                    Intent intent = new Intent();
                                    intent.setAction(android.content.Intent.ACTION_VIEW);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    String authority = activity.getPackageName() + ".fileprovider";
                                    Uri filuri = FileProvider.getUriForFile(activity, authority, file);
                                    intent.setDataAndType(filuri, getMimeType(filuri, activity));
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                    }
                                }
                            });
                            aTV.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View view) {
                                    PopupMenu popup = new PopupMenu(ViewFileActivity.this, view);

                                    popup.getMenuInflater()
                                            .inflate(R.menu.lognclick_popup_menu_viewfile, popup.getMenu());
                                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                        public boolean onMenuItemClick(MenuItem item) {
                                            int id = item.getItemId();
                                            if (id == R.id.ViewFileDelete) {

                                                final String sFileName;
                                                try {
                                                    sFileName = file.getCanonicalPath().toString().substring(MyGitUtility.getLocalGitDirectory(activity, sGitRemoteUrl).length());
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                    return false;
                                                }
                                                new AlertDialog.Builder(activity)
                                                        .setTitle(getResources().getString(R.string.view_title_remove_attach))
                                                        .setMessage(sFileName)
                                                        .setCancelable(true)
                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                                try {
                                                                    file.getCanonicalFile().delete();

                                                                    new Thread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Log.d(TAG,"commit when file be deleted");

                                                                            if (MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, MyApplication.getAppContext().getString(R.string.view_file_delete_attachment_file_commit) + "\n" + sFileName))
                                                                                MyGitUtility.push(MyApplication.getAppContext(), sGitRemoteUrl);
                                                                            else {

                                                                            }
                                                                        }
                                                                    }).start();
                                                                    layoutAttachment.removeView(aTV);
                                                                } catch (IOException e) {
                                                                    e.printStackTrace();
                                                                }

                                                            }
                                                        })
                                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                // finish();
                                                            }
                                                        })
                                                        .show();

                                            }
                                            else if( id== R.id.ViewFileDownload){
                                                try {
                                                    File aDest = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+ file.getName());
                                                    if( !aDest.exists()) {
                                                        try (FileOutputStream aFOS = new FileOutputStream(aDest)) {
                                                            Files.copy(file.toPath(), aFOS);
                                                        }catch(Exception ee){
                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.download_file_exists)+"\n"+file.getName(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                            ee.printStackTrace();
                                                        }
                                                        if( aDest.exists()) {
                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.download_success)+"\n"+file.getName(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    }else{
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.download_file_exists)+"\n"+file.getName(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
//                                throw new RuntimeException(e);
                                                }

                                            }
                                            return true;
                                        }
                                    });

                                    popup.show();
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

    private void disable() {
        editText.setKeyListener(null);
        editText.requestFocus();
        itemEdit.setVisible(true);
        itemSave.setVisible(false);


    }

    private void enable() {
        itemEdit.setVisible(false);
        itemSave.setVisible(true);
        SpannableString s = new SpannableString(itemSave.getTitle());
        s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
        itemSave.setTitle(s);
        editText.setKeyListener(listener);
        editText.setFocusableInTouchMode(true);
        editText.setFocusable(true);
        editText.requestFocus();
        editText.setText(editText.getText());
        editText.setPressed(true);
        editText.setSelection(editText.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

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
        itemEdit = menu.findItem(R.id.view_file_action_edit);
        itemSave = menu.findItem(R.id.view_file_action_save);
        if (isModify)
            enable();
        else
            disable();
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
                                            if (txtUrl.getText().toString().trim().equals(""))
                                                txtUrl.setText("");
                                            else
                                                txtUrl.setText(txtUrl.getText() + "\n<" + file.getName() + ">");
                                            boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, txtUrl.getText().toString());
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
                            if (txtUrl.getText().toString().trim().equals(""))
                                txtUrl.setText("");
                            else
                                txtUrl.setText(txtUrl.getText() + "\n<" + file.getName() + ">");
                            boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, txtUrl.getText().toString());
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
        } else if (id == R.id.view_file_action_edit) {
            enable();
            isModify = true;
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
                                    }
                                });

                                isModify = false;
                                disable();
                               // Log.d(TAG, "asdf");

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (txtUrl.getText().toString().trim().equals(""))
                                                            txtUrl.setText("");
                                                        else
                                                            txtUrl.setText(txtUrl.getText() + "\n<" + file.getName() + ">");

                                                    }
                                                });
                                        boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, txtUrl.getText().toString());
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
                    }
                });
                isModify = false;
                disable();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (txtUrl.getText().toString().trim().equals(""))
                                txtUrl.setText("");
                            else
                                txtUrl.setText(txtUrl.getText() + "\n<" + file.getName() + ">");
                            Log.d(TAG,"commit when view_file_action_save be triggered");

                            boolean bCommitStatus = MyGitUtility.commit(MyApplication.getAppContext(), sGitRemoteUrl, txtUrl.getText().toString());
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
            PermissionHelper.requestMedia(this, new PermissionHelper.PermissionCallback() {
                @Override
                public void onGranted() {
                    openDocumentPicker();
                }

                @Override
                public void onDenied() {
                    // Silent cancel per user requirements
                }
            });
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
        } else if (requestCode == PermissionHelper.REQUEST_CODE_MEDIA) {
            String primary = PermissionHelper.getMediaPrimaryPermission();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionHelper.resetDenialCount(this, primary);
                openDocumentPicker();
            } else {
                PermissionHelper.incrementDenialCount(this, primary);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == READ_REQUEST_CODE) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                pickiT.getPath(uri, Build.VERSION.SDK_INT);

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
                                        final File aDestFile;
                                        try {
                                            aDestFile = new File(aDestFileDirectory.getCanonicalPath() + File.separator + txtUrl.getText().toString().trim());
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
    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String reason) {
        //Dismiss dialog and return the path
        Log.d(TAG, "pickiT real path =" + path + ", was successful = " + wasSuccessful + ", reason = " + reason);

        final File aSelectedFile = new File(path);
        try {
            final File aDestFileDirectory = new File(file.getCanonicalPath().toString() + "_attach".trim());
            //  Log.d(TAG,"aDestFileDirectory file = "+aDestFileDirectory.getCanonicalPath());
            if (!aDestFileDirectory.isDirectory())
                aDestFileDirectory.mkdir();

            final EditText txtUrl = new EditText(this);
            txtUrl.setText(aSelectedFile.getName());
            txtUrl.setMaxLines(3);
            txtUrl.setLines(3);
            txtUrl.setTextSize(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitEditTextSize", "18")));

            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_title_add))
                    .setMessage(getResources().getString(R.string.dialog_file_name))
                    .setView(txtUrl)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            isModify = false;
                            disable();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final File aDestFile;
                                    try {
                                        aDestFile = new File(aDestFileDirectory.getCanonicalPath() + File.separator + txtUrl.getText().toString().trim());
                                        //     Log.d(TAG,"dest file = "+aDestFile.getCanonicalPath());
                                        final String sDestFileNameString;
                                        sDestFileNameString = aDestFile.getCanonicalPath().toString().substring(MyGitUtility.getLocalGitDirectory(activity, sGitRemoteUrl).length());
                                        Files.copy(aSelectedFile.toPath(), aDestFile.toPath());
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Thread.sleep(100);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                boolean bCommit = false;
                                                Log.d(TAG,"commit when file added");

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
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
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

    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> arrayList, boolean b, String s) {

    }


}
