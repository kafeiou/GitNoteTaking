package inmethod.gitnotetaking;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.jgit.revwalk.RevCommit;

import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;

import org.eclipse.jgit.util.FileUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.util.List;

import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
import inmethod.gitnotetaking.utility.GitHubAuthManager;
import inmethod.gitnotetaking.utility.GitHubAuthManager.GitHubRepo;
import inmethod.gitnotetaking.utility.MyGitUtility;
import inmethod.gitnotetaking.view.GitList;
import inmethod.gitnotetaking.view.RecyclerAdapterForDevice;


@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "GitNoteTaking";
    private Activity activity = this;
    RecyclerView rv = null;
    RecyclerAdapterForDevice adapter = null;
    AlertDialog.Builder waitBuilder = null;
    AlertDialog waitDialog;
    private String currentLanguageSetting = null;


    public void showError(Exception ex)
    {
        Log.e(TAG, ex.getMessage(), ex);
        //UIUtils.showDialog(this, R.string.warning, ex.getMessage());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        View rootView = findViewById(android.R.id.content);
        MyApplication.setView(activity,rootView);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        waitBuilder = new AlertDialog.Builder(activity);
        waitBuilder.setCancelable(false);
        waitBuilder.setView(R.layout.loading_dialog);
        waitDialog = waitBuilder.create();


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
            m.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentLanguageSetting = PreferenceManager.getDefaultSharedPreferences(this).getString("AppLanguage", "system");
    }

    private final Handler loadingMsgHandler = new Handler(Looper.getMainLooper());
    private Runnable loadingMsgRunnable;
    private int loadingMsgIndex = 0;

    private void showWaitDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (waitDialog == null) {
                    waitBuilder = new AlertDialog.Builder(activity);
                    waitBuilder.setCancelable(false);
                    waitBuilder.setView(R.layout.loading_dialog);
                    waitDialog = waitBuilder.create();
                }
                if (!waitDialog.isShowing()) {
                    waitDialog.show();
                    startLoadingMessageCycle();
                }
            }
        });
    }

    private void dismissWaitDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopLoadingMessageCycle();
                if (waitDialog != null && waitDialog.isShowing()) {
                    waitDialog.dismiss();
                }
            }
        });
    }

    private void startLoadingMessageCycle() {
        stopLoadingMessageCycle();
        loadingMsgIndex = 0;
        final int[] msgResIds = new int[]{
                R.string.tv_please_wait,
                R.string.tv_fetching_github_dates,
                R.string.tv_syncing_timestamps
        };
        TextView tv = waitDialog != null ? waitDialog.findViewById(R.id.tv_loading_msg) : null;
        if (tv != null) {
            tv.setText(msgResIds[0]);
        }
        loadingMsgRunnable = new Runnable() {
            @Override
            public void run() {
                if (waitDialog != null && waitDialog.isShowing()) {
                    TextView tvMsg = waitDialog.findViewById(R.id.tv_loading_msg);
                    if (tvMsg != null) {
                        loadingMsgIndex = (loadingMsgIndex + 1) % msgResIds.length;
                        tvMsg.setText(msgResIds[loadingMsgIndex]);
                    }
                    loadingMsgHandler.postDelayed(this, 10000);
                }
            }
        };
        loadingMsgHandler.postDelayed(loadingMsgRunnable, 10000);
    }

    private void stopLoadingMessageCycle() {
        if (loadingMsgRunnable != null) {
            loadingMsgHandler.removeCallbacks(loadingMsgRunnable);
            loadingMsgRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLoadingMessageCycle();
        if (waitDialog != null && waitDialog.isShowing()) {
            waitDialog.dismiss();
        }
        GitHubAuthManager.getInstance().cancelPolling();
    }

    @Override
    public void onStart() {
        super.onStart();
        rv = (RecyclerView) findViewById(R.id.rv);
        adapter = new RecyclerAdapterForDevice(this);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        if (rv.getItemDecorationCount() == 0) {
            rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        }
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener(new RecyclerAdapterForDevice.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // Toast.makeText(activity, "click position=" + position, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, FileExplorerActivity.class);
                Object[] aTextView = GitList.getDeviceInfoFromLayoutId(view);
                String sGitName = ((TextView) aTextView[0]).getText().toString();
                String sRemoteUrl = ((TextView) aTextView[1]).getText().toString();
                RemoteGit aRemoteGit = MyGitUtility.getRemoteGit(activity, sRemoteUrl);
                if (aRemoteGit.getStatus() == MyGitUtility.GIT_STATUS_PULLING) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, getString(R.string.toast_pulling) , Toast.LENGTH_SHORT).show();
                            intent.putExtra("GIT_ROOT_DIR", MyGitUtility.getLocalGitDirectory(activity, sRemoteUrl));
                            intent.putExtra("GIT_NAME", sGitName);
                            intent.putExtra("GIT_REMOTE_URL", sRemoteUrl);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);


                        }
                    });
                } else if (aRemoteGit.getStatus() == MyGitUtility.GIT_STATUS_CLONING) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, getString(R.string.toast_cloning), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    intent.putExtra("GIT_ROOT_DIR", MyGitUtility.getLocalGitDirectory(activity, sRemoteUrl));
                    intent.putExtra("GIT_NAME", sGitName);
                    intent.putExtra("GIT_REMOTE_URL", sRemoteUrl);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                }
            }
        });

        adapter.setOnItemLongClickListener(new RecyclerAdapterForDevice.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                final Object[] aTextView = GitList.getDeviceInfoFromLayoutId(view);
                final String sRemoteUrl = ((TextView) aTextView[1]).getText().toString();

                PopupMenu popup = new PopupMenu(MainActivity.this, view);

                popup.getMenuInflater()
                        .inflate(R.menu.lognclick_popup_menu, popup.getMenu());
                if (sRemoteUrl.indexOf("local") != -1) {
                    for (int i = 0; i < popup.getMenu().size(); i++) {
                        if (popup.getMenu().getItem(i).getItemId() == R.id.Push) {
                            popup.getMenu().getItem(i).setVisible(false);
                        }
                        if (popup.getMenu().getItem(i).getItemId() == R.id.show_all_remote_branches) {
                            popup.getMenu().getItem(i).setVisible(false);
                        }
                        if (popup.getMenu().getItem(i).getItemId() == R.id.Pull) {
                            popup.getMenu().getItem(i).setVisible(false);
                        }
                    }
                }
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.Remove) {

                            new AlertDialog.Builder(activity)
                                    .setTitle( MyApplication.getAppContext().getString(R.string.title_remove))
                                    .setMessage( MyApplication.getAppContext().getString(R.string.message_remove))
                                    .setPositiveButton(MyApplication.getAppContext().getText(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            showWaitDialog();
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        MyGitUtility.deleteByRemoteUrl(activity, ((TextView) aTextView[1]).getText().toString());
                                                        Log.d(TAG, "try to delete local git repository");
                                                        MyGitUtility.deleteLocalGitRepository(activity, sRemoteUrl);
                                                        final ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(activity);
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                adapter.clear();
                                                                for (final RemoteGit a : aList) {
                                                                    adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
                                                                }
                                                                Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_success), Toast.LENGTH_SHORT).show();
                                                                dismissWaitDialog();
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Failed to remove repository: " + sRemoteUrl, e);
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_failed), Toast.LENGTH_SHORT).show();
                                                                dismissWaitDialog();
                                                            }
                                                        });
                                                    }
                                                }
                                            }).start();
                                        }
                                    }).setNegativeButton(MyApplication.getAppContext().getText(R.string.dialog_cancel) , new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                        }
                                    }).show();

                            return true;
                        } else if (id == R.id.Push) {
                            if (!MyApplication.isNetworkConnected()) {
                                Log.d(TAG, "no netework ");
                                Toast.makeText(activity, "No Network", Toast.LENGTH_SHORT).show();
                            } else {
                                showWaitDialog();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final boolean success = MyGitUtility.push(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString());
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dismissWaitDialog();
                                                    reloadRepositoryList();
                                                    if (success) {
                                                        Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_success), Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_failed), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            Log.e(TAG, "Push failed for " + sRemoteUrl, e);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dismissWaitDialog();
                                                    reloadRepositoryList();
                                                    Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_failed), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            }

                        } else if (id == R.id.Pull) {
                            if (!MyApplication.isNetworkConnected()) {
                                Log.d(TAG, "no netework ");
                                Toast.makeText(activity, "No Network", Toast.LENGTH_SHORT).show();
                            } else {
                                showWaitDialog();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final boolean success = MyGitUtility.pull(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString());
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dismissWaitDialog();
                                                    reloadRepositoryList();
                                                    if (success) {
                                                        Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pulling_success), Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pulling_failed), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            Log.e(TAG, "Pull failed for " + sRemoteUrl, e);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dismissWaitDialog();
                                                    reloadRepositoryList();
                                                    Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pulling_failed), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            }

                        } else if (id == R.id.Modify) {
                            Intent intent = null;
                            if (sRemoteUrl.indexOf("local") == -1)
                                intent = new Intent(MainActivity.this, ModifyRemoteGitActivity.class);
                            else
                                intent = new Intent(MainActivity.this, ModifyLocalGitActivity.class);
                            String sRemoteUrl = ((TextView) aTextView[1]).getText().toString();
                            intent.putExtra("GIT_REMOTE_URL", sRemoteUrl);
                            startActivity(intent);
                        } else if (id == R.id.show_commit_short_log) {
                            String sNoteName = ((TextView) aTextView[0]).getText().toString();
                            String sRemoteUrl = ((TextView) aTextView[1]).getText().toString();
                            AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(activity);
                            dialogbuilder.setTitle(sNoteName);
                            TextView txtUrl = new TextView(activity);
                            String sListMessages = "";
                            txtUrl.setMaxLines(15);
                            txtUrl.setMovementMethod(new ScrollingMovementMethod());
                            int i = 0;

                            FrameLayout container = new FrameLayout(activity);
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.leftMargin = 40;
                            params.rightMargin = 40;
                            params.topMargin = 20;
                            params.bottomMargin = 20;
                            txtUrl.setLayoutParams(params);
                            container.addView(txtUrl);

                            for (RevCommit aRev : MyGitUtility.getLocalCommitLogList(activity, sRemoteUrl)) {
                                if( aRev.getFullMessage()==null || aRev.getFullMessage().trim().isEmpty())
                                  continue;
                                i++;
                                sListMessages = sListMessages + "\n" + getDate(aRev.getCommitTime()) + "\n--\n" + aRev.getFullMessage() + "\n";
                                if (i == 50) break;

                            }
                            txtUrl.setText(sListMessages.trim());
                            dialogbuilder.setView(container).setPositiveButton(getString(R.string.dialog_ok), null);
                            dialogbuilder.create().show();
                        } else if (id == R.id.show_all_remote_branches) {
                            String sNoteName = ((TextView) aTextView[0]).getText().toString();
                            String sRemoteUrl = ((TextView) aTextView[1]).getText().toString();
                            AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(activity);
                            dialogbuilder.setTitle(sNoteName);
                            TextView txtUrl = new TextView(activity);
                            String sListMessages = "";
                            txtUrl.setMaxLines(15);
                            txtUrl.setMovementMethod(new ScrollingMovementMethod());
                            int i = 0;

                            FrameLayout container = new FrameLayout(activity);
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.leftMargin = 40;
                            params.rightMargin = 40;
                            params.topMargin = 20;
                            params.bottomMargin = 20;
                            txtUrl.setLayoutParams(params);
                            container.addView(txtUrl);

                            List<String> aBranchesList = MyGitUtility.fetchGitBranches(activity, sRemoteUrl);
                            for (String sBranch : aBranchesList) {
                                i++;

                                sListMessages = sListMessages + "\n" + sBranch;
                                if (i == 50) break;

                            }
                            txtUrl.setText(sListMessages.trim());
                            dialogbuilder.setView(container).setPositiveButton(getString(R.string.dialog_ok), null);
                            dialogbuilder.create().show();
                        } else if (id == R.id.Backup) {
                            String sBackupZip = ((TextView) aTextView[0]).getText().toString() + "_" + inmethod.commons.util.DateUtil.getDateStringWithFormat("yyyyMMdd") + ".zip";
                            showWaitDialog();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean success = MyGitUtility.backupToDownloads(activity, sRemoteUrl, sBackupZip);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dismissWaitDialog();
                                            if (success) {
                                                Toast.makeText(activity, getString(R.string.backup_success) + "\n" + sBackupZip, Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(activity, getString(R.string.backup_failed) + "\n" + sBackupZip, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }else if (id == R.id.AutoCommit) {
                            showWaitDialog();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean isDirty = MyGitUtility.isWorkingTreeDirty(activity, sRemoteUrl);
                                    if (!isDirty) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dismissWaitDialog();
                                                Toast.makeText(activity, getString(R.string.toast_auto_commit_not_needed), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        return;
                                    }
                                    String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                                    boolean committed = MyGitUtility.autoCommitIfDirtyWithMessage(activity, sRemoteUrl, "Auto-commit: " + timestamp);
                                    if (committed) {
                                        boolean isLocal = sRemoteUrl.contains("local");
                                        if (isLocal) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dismissWaitDialog();
                                                    Toast.makeText(activity, getString(R.string.toast_auto_commit_success), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            boolean pushSuccess = false;
                                            if (MyApplication.isNetworkConnected()) {
                                                pushSuccess = MyGitUtility.push(activity, sRemoteUrl);
                                            }
                                            final boolean finalPushSuccess = pushSuccess;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dismissWaitDialog();
                                                    reloadRepositoryList();
                                                    if (finalPushSuccess) {
                                                        Toast.makeText(activity, getString(R.string.toast_auto_commit_push_success), Toast.LENGTH_LONG).show();
                                                    } else {
                                                        Toast.makeText(activity, getString(R.string.toast_auto_commit_success) + " (" + getString(R.string.pushing_failed) + ")", Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                        }
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dismissWaitDialog();
                                                reloadRepositoryList();
                                                Toast.makeText(activity, getString(R.string.toast_auto_commit_not_needed), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            }).start();
                        } else if (id == R.id.CalculateStorage) {
                            showWaitDialog();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String localDir = MyGitUtility.getLocalGitDirectory(activity, sRemoteUrl);
                                    File repoDir = new File(localDir);
                                    MyGitUtility.StorageBreakdown storage = MyGitUtility.calculateRepositoryStorage(repoDir);
                                    String workingSize = MyGitUtility.formatStorageSize(storage.workingTreeBytes);
                                    String gitSize = MyGitUtility.formatStorageSize(storage.gitDirBytes);
                                    String totalSize = MyGitUtility.formatStorageSize(storage.getTotalBytes());

                                    StringBuilder message = new StringBuilder();
                                    message.append(getString(R.string.dialog_storage_working_dir)).append(workingSize).append(" (").append(storage.fileCount).append(" files)\n");
                                    message.append(getString(R.string.dialog_storage_git_dir)).append(gitSize).append("\n\n");
                                    message.append(getString(R.string.dialog_storage_total)).append(totalSize);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dismissWaitDialog();
                                            new AlertDialog.Builder(activity)
                                                    .setTitle(getString(R.string.dialog_storage_title))
                                                    .setMessage(message.toString())
                                                    .setPositiveButton(getString(R.string.dialog_ok), null)
                                                    .show();
                                        }
                                    });
                                }
                            }).start();
                        } else if (id == R.id.CleanTempFiles) {
                            showWaitDialog();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final int cleanedCount = MyGitUtility.cleanTemporaryFilesAndSync(activity, sRemoteUrl);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dismissWaitDialog();
                                                reloadRepositoryList();
                                                if (cleanedCount > 0) {
                                                    Toast.makeText(activity, String.format(getString(R.string.toast_clean_temp_files_success), cleanedCount), Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(activity, getString(R.string.toast_clean_temp_files_none), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } catch (final Exception e) {
                                        Log.e(TAG, "Failed to clean temporary files: " + sRemoteUrl, e);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dismissWaitDialog();
                                                reloadRepositoryList();
                                                Toast.makeText(activity, getString(R.string.toast_clean_temp_files_failed) + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            }).start();
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

    }

    private void reloadRepositoryList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.clear();
                ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(activity);
                for (final RemoteGit a : aList) {
                    adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
                }
            }
        });
        String sGitAuthorName = PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorName", null);
        String sGitAuthorEmail = PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorEmail", null);

        if (sGitAuthorName == null || sGitAuthorName.equals("") || sGitAuthorEmail == null || sGitAuthorEmail.equals("")) {
            final AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(activity);
            MyAlertDialog.setTitle(getResources().getString(R.string.tv_title_first_time));
            MyAlertDialog.setMessage(getResources().getString(R.string.tv_first_time));
            DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainActivity.this, PreferencesSettings.class);
                    startActivity(intent);
                    dialog.dismiss();
                }
            };
            MyAlertDialog.setNeutralButton("OK", OkClick);
            MyAlertDialog.show();
        }

        handleGitHubOAuthCallback(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleGitHubOAuthCallback(intent);
    }

    private void handleGitHubOAuthCallback(Intent intent) {
        if (intent == null) return;
        Uri uri = intent.getData();
        if (uri != null && "gitnotetaking".equals(uri.getScheme()) && "oauth".equals(uri.getHost()) && "/github".equals(uri.getPath())) {
            // Immediately consume and clear intent data so returning to MainActivity won't re-execute with expired code
            intent.setData(null);
            setIntent(new Intent(this, MainActivity.class));

            String error = uri.getQueryParameter("error");
            if (error != null) {
                Toast.makeText(activity, R.string.github_oauth_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }
            String code = uri.getQueryParameter("code");
            if (code != null && !code.isEmpty()) {
                showWaitDialog();
                GitHubAuthManager.getInstance().exchangeCodeForToken(code, new GitHubAuthManager.GitHubAuthCallback() {
                    @Override
                    public void onSuccess(String username, String accessToken, List<GitHubRepo> noteRepos, int totalReposCount) {
                        dismissWaitDialog();
                        showGitHubRepoSelectionDialog(username, accessToken, noteRepos, totalReposCount);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        dismissWaitDialog();
                        Toast.makeText(activity, getString(R.string.github_auth_failed) + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String savedLang = PreferenceManager.getDefaultSharedPreferences(this).getString("AppLanguage", "system");
        if ("ja".equalsIgnoreCase(savedLang)) {
            savedLang = "ja-JP";
        }
        if (currentLanguageSetting != null && !currentLanguageSetting.equals(savedLang)) {
            currentLanguageSetting = savedLang;
            recreate();
            return;
        }
        currentLanguageSetting = savedLang;
        adapter.clear();
        MyApplication.resetFiles();
        ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(MyApplication.getAppContext());
        boolean bCloning = false;
        for (final RemoteGit a : aList) {
            adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
            if (a.getStatus() == MyGitUtility.GIT_STATUS_CLONING)
                bCloning = true;
        }
        if (bCloning) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean bCloning = true;
                    // 3 min
                    for (int i = 0; i < 1000; i++) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!bCloning) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.clear();
                                    ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(activity);
                                    for (final RemoteGit a : aList) {
                                        adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
                                    }
                                }
                            });
                            break;
                        }
                        ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(MyApplication.getAppContext());
                        bCloning = false;
                        for (final RemoteGit a : aList) {
                            if (a.getStatus() == MyGitUtility.GIT_STATUS_CLONING) bCloning = true;
                            else if (a.getStatus() == MyGitUtility.GIT_STATUS_PUSH_FAIL) {
                                RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(activity);
                                aRemoteGitDAO.delete(a.getUrl());
                                aRemoteGitDAO.close();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, a.getNickname() + " " + MyApplication.getAppContext().getResources().getString(R.string.tv_clone_fail), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            }
                        }
                    }
                    if (bCloning) {
                        ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(MyApplication.getAppContext());
                        for (final RemoteGit a : aList) {
                            if (a.getStatus() == MyGitUtility.GIT_STATUS_CLONING)
                                MyGitUtility.deleteByRemoteUrl(MyApplication.getAppContext(), a.getUrl());
                        }
                    }
                }
            }).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (menu != null) {
            try {
                Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, true);
            } catch (Exception e) {
                // Ignore if not supported
            }
            MenuItem areaItem = menu.findItem(R.id.m_area);
            if (areaItem != null && areaItem.hasSubMenu()) {
                Menu subMenu = areaItem.getSubMenu();
                if (subMenu != null) {
                    try {
                        Method m = subMenu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                        m.setAccessible(true);
                        m.invoke(subMenu, true);
                    } catch (Exception e) {
                        // Ignore if not supported
                    }
                }
            }
        }
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
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, PreferencesSettings.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_create_github_git) {
            startCreateGitHubNoteFlow();
            return true;
        } else if (id == R.id.action_clone_git_remote) {
            Intent intent = new Intent(MainActivity.this, CloneGitActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_create_local_git) {
            Intent intent = new Intent(MainActivity.this, CreateLocalGitActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void startCreateGitHubNoteFlow() {
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        int paddingSmall = (int) (8 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, paddingSmall, padding, paddingSmall);

        // 1. One-Tap OAuth Button (Top Recommendation)
        Button btnOAuth = new Button(activity);
        btnOAuth.setText(R.string.github_oauth_login_btn);
        btnOAuth.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_github, 0, 0, 0);
        btnOAuth.setCompoundDrawablePadding(paddingSmall);
        btnOAuth.setTextSize(15);
        btnOAuth.setAllCaps(false);

        // 2. Divider / Subtitle
        TextView tvDivider = new TextView(activity);
        tvDivider.setText(R.string.github_oauth_divider);
        tvDivider.setTextSize(12);
        tvDivider.setTextColor(0xFF888888);
        tvDivider.setGravity(android.view.Gravity.CENTER);
        tvDivider.setPadding(0, paddingSmall * 2, 0, paddingSmall);

        // 3. Instruction Message (4-step guide)
        TextView tvMsg = new TextView(activity);
        tvMsg.setText(R.string.github_connect_dialog_msg);
        tvMsg.setTextSize(14);
        tvMsg.setPadding(0, paddingSmall, 0, paddingSmall);

        // 4. Token EditText
        final EditText editText = new EditText(activity);
        editText.setHint("ghp_xxxx or github_pat_xxxx");
        editText.setMaxLines(2);

        // Auto paste from clipboard if starts with ghp_ or github_pat_
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                android.content.ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        String str = text.toString().trim();
                        if (str.startsWith("ghp_") || str.startsWith("github_pat_")) {
                            editText.setText(str);
                            editText.setSelection(str.length());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        container.addView(btnOAuth);
        container.addView(tvDivider);
        container.addView(tvMsg);
        container.addView(editText);
        scrollView.addView(container);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.action_create_github_git)
                .setView(scrollView)
                .setPositiveButton(R.string.github_btn_connect, null)
                .setNeutralButton(R.string.github_btn_generate_token, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        btnOAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                GitHubAuthManager.getInstance().startOAuthWebFlow(activity);
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button btnConnect = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String token = editText.getText().toString().trim();
                        if (token.isEmpty()) {
                            Toast.makeText(activity, R.string.input_cannot_be_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dialog.dismiss();
                        showWaitDialog();
                        GitHubAuthManager.getInstance().loadUserDataAndRepos(token, new GitHubAuthManager.GitHubAuthCallback() {
                            @Override
                            public void onSuccess(String username, String accessToken, List<GitHubRepo> noteRepos, int totalReposCount) {
                                dismissWaitDialog();
                                showGitHubRepoSelectionDialog(username, accessToken, noteRepos, totalReposCount);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                dismissWaitDialog();
                                Toast.makeText(activity, getString(R.string.github_auth_failed) + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });

                Button btnGenToken = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                btnGenToken.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = "https://github.com/settings/tokens/new?scopes=repo,read:user&description=InMethodGitNoteTaking";
                        try {
                            androidx.browser.customtabs.CustomTabsIntent customTabsIntent = new androidx.browser.customtabs.CustomTabsIntent.Builder().build();
                            customTabsIntent.launchUrl(activity, Uri.parse(url));
                        } catch (Exception e) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                        }
                    }
                });
            }
        });

        dialog.show();
    }

    private void showGitHubRepoSelectionDialog(final String username, final String token, final List<GitHubRepo> noteRepos, int totalReposCount) {
        if (noteRepos == null || noteRepos.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.github_no_note_repos_title)
                    .setMessage(R.string.github_no_note_repos_msg)
                    .setPositiveButton(R.string.github_create_repo_online, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/new?name=note-"));
                            startActivity(browserIntent);
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            return;
        }

        Set<String> downloadedUrls = new HashSet<>();
        RemoteGitDAO dao = new RemoteGitDAO(MyApplication.getAppContext());
        ArrayList<RemoteGit> localGits = dao.getAll();
        dao.close();
        for (RemoteGit g : localGits) {
            if (g.getUrl() != null) {
                String trimmed = g.getUrl().toLowerCase().trim();
                downloadedUrls.add(trimmed);
                if (trimmed.endsWith(".git")) {
                    downloadedUrls.add(trimmed.substring(0, trimmed.length() - 4));
                } else {
                    downloadedUrls.add(trimmed + ".git");
                }
            }
        }

        final boolean[] isDownloaded = new boolean[noteRepos.size()];
        for (int i = 0; i < noteRepos.size(); i++) {
            GitHubRepo r = noteRepos.get(i);
            String cloneUrl = r.getCloneUrl() != null ? r.getCloneUrl().toLowerCase().trim() : "";
            boolean downloaded = downloadedUrls.contains(cloneUrl) || (cloneUrl.endsWith(".git") && downloadedUrls.contains(cloneUrl.substring(0, cloneUrl.length() - 4)));
            isDownloaded[i] = downloaded;
        }

        final BaseAdapter repoAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return noteRepos.size();
            }

            @Override
            public Object getItem(int position) {
                return noteRepos.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean isEnabled(int position) {
                return !isDownloaded[position];
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(activity).inflate(R.layout.item_github_repo, parent, false);
                }
                GitHubRepo r = noteRepos.get(position);
                TextView tvName = convertView.findViewById(R.id.tv_repo_name);
                TextView tvDesc = convertView.findViewById(R.id.tv_repo_desc);

                String badge = r.isPrivate() ? getString(R.string.github_private_badge) : getString(R.string.github_public_badge);
                String downloadedBadge = isDownloaded[position] ? " " + getString(R.string.github_downloaded_badge) : "";
                tvName.setText(r.getFullName() + badge + downloadedBadge);

                String desc = r.getDescription();
                boolean hasDesc = desc != null && !desc.trim().isEmpty() && !desc.equalsIgnoreCase("null");
                if (hasDesc) {
                    tvDesc.setText(desc.trim());
                    tvDesc.setVisibility(View.VISIBLE);
                } else {
                    tvDesc.setVisibility(View.GONE);
                }

                if (isDownloaded[position]) {
                    tvName.setTextColor(0xFF888888);
                    tvDesc.setTextColor(0xFF666666);
                } else {
                    tvName.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
                    tvDesc.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
                }
                return convertView;
            }
        };

        new AlertDialog.Builder(activity)
                .setTitle(getString(R.string.github_select_repo_title) + " (" + username + ")")
                .setAdapter(repoAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!isDownloaded[which]) {
                            GitHubRepo selectedRepo = noteRepos.get(which);
                            cloneSelectedGitHubRepo(username, token, selectedRepo);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void cloneSelectedGitHubRepo(final String username, final String token, final GitHubRepo repo) {
        showWaitDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String sUrl = repo.getCloneUrl();
                final String sRemoteName = repo.getDefaultBranch();
                final String sNick = repo.getName();

                try {
                    if (MyGitUtility.checkLocalGitRepository(activity, sUrl)) {
                        String sLocalDirectory = MyGitUtility.getLocalGitDirectory(activity, sUrl);
                        FileUtils.delete(new File(sLocalDirectory), FileUtils.RECURSIVE);
                    }

                    RemoteGit aValue = new RemoteGit();
                    aValue.setId(0);
                    aValue.setRemoteName(sRemoteName);
                    aValue.setBranch(sRemoteName);
                    aValue.setUrl(sUrl);
                    aValue.setUid(username);
                    aValue.setPwd(token);
                    aValue.setNickname(sNick);
                    aValue.setStatus(MyGitUtility.GIT_STATUS_CLONING);
                    aValue.setAuthor_name(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorName", username));
                    aValue.setAuthor_email(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorEmail", username + "@users.noreply.github.com"));

                    RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(MyApplication.getAppContext());
                    RemoteGit existing = aRemoteGitDAO.getByURL(sUrl);
                    if (existing != null) {
                        aRemoteGitDAO.delete(sUrl);
                    }
                    aRemoteGitDAO.insert(aValue);
                    aRemoteGitDAO.close();

                    boolean bSuccess = MyGitUtility.cloneGit(MyApplication.getAppContext(), sUrl, sRemoteName, username, token);

                    RemoteGitDAO updateDao = new RemoteGitDAO(MyApplication.getAppContext());
                    RemoteGit g = updateDao.getByURL(sUrl);
                    if (g != null) {
                        g.setStatus(bSuccess ? MyGitUtility.GIT_STATUS_SUCCESS : MyGitUtility.GIT_STATUS_PUSH_FAIL);
                        updateDao.update(g);
                    }
                    updateDao.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dismissWaitDialog();
                            if (adapter != null) {
                                adapter.clear();
                                ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(MyApplication.getAppContext());
                                for (final RemoteGit a : aList) {
                                    adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
                                }
                            }
                            Toast.makeText(activity, bSuccess ? getString(R.string.github_clone_success) : getString(R.string.github_clone_failed), Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dismissWaitDialog();
                            Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String getDate(long time) {
        Date date = new Date(time * 1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss"); // the format of your date
        // sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        return sdf.format(date);
    }
}
