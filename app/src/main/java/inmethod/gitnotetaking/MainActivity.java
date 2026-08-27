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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
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
    int REQUEST_CODE_PERMISSIONS = 123;

    String [] _app_permissions = {
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
    };




    private void requestPermission()
    {
        List<String> listPermissionNeed = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            listPermissionNeed.add( Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);

        for(String per : _app_permissions)
        {

            if (ContextCompat.checkSelfPermission(this, per) != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionNeed.add(per);
            }
        }

        if (listPermissionNeed.size()>0)
        {
            String [] pers = listPermissionNeed.toArray(new String[0]);
            // Permission request logic

            ActivityCompat.requestPermissions(this, pers, REQUEST_CODE_PERMISSIONS);
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String [] permissions, int [] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "PERMISSION_GRANTED=" + permissions[i]);
                } else {
                    Log.i(TAG, "PERMISSION_DENY=" + permissions[i]);
                    sb.append(permissions[i]).append("\r\n");
                }
            }

            if (sb.length() > 0) {
                showError(new Exception("PERMISSION_DENY\r\n" + sb.toString()));
            }
        }
    }

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
    }


    @Override
    public void onStart() {
        super.onStart();
        rv = (RecyclerView) findViewById(R.id.rv);
        adapter = new RecyclerAdapterForDevice(this);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
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

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    waitDialog.show();

                                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                MyGitUtility.deleteByRemoteUrl(activity, ((TextView) aTextView[1]).getText().toString());
                                                                Log.d(TAG, "try to delete local git repository");
                                                                MyGitUtility.deleteLocalGitRepository(activity, sRemoteUrl);
                                                                adapter.clear();
                                                                ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(activity);
                                                                for (final RemoteGit a : aList) {
                                                                    adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
                                                                }

                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_success), Toast.LENGTH_SHORT).show();
                                                                            waitDialog.dismiss();
                                                                        }
                                                                    });

                                                            } catch (Exception e) {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        Toast.makeText(activity,MyApplication.getAppContext().getText(R.string.pushing_failed) , Toast.LENGTH_SHORT).show();
                                                                        waitDialog.dismiss();
                                                                    }
                                                                });
                                                                throw new RuntimeException(e);
                                                            }
                                                        }                                                // Your Code

                                                    }, 300);
                                                }
                                            });




                                        }
                                    }).setNegativeButton(MyApplication.getAppContext().getText(R.string.dialog_cancel) , new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                        }
                                    }).show();



                            return true;
                        } else if (id == R.id.Push) {
                            if (!MyApplication.isNetworkConnected()) {
                                Log.d(TAG, "no netework ");

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(activity, "No Network", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        waitDialog.show();

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if (MyGitUtility.push(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString())) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_success), Toast.LENGTH_SHORT).show();
                                                                waitDialog.dismiss();
                                                            }
                                                        });
                                                    }else{
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pushing_failed), Toast.LENGTH_SHORT).show();
                                                                waitDialog.dismiss();
                                                            }
                                                        });

                                                    }
                                                } catch (Exception e) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(activity,MyApplication.getAppContext().getText(R.string.pushing_failed) , Toast.LENGTH_SHORT).show();
                                                            waitDialog.dismiss();
                                                        }
                                                    });
                                                    throw new RuntimeException(e);
                                                }
                                            }                                                // Your Code

                                        }, 300);
                                    }
                                });

                                /*
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                builder.setCancelable(false);
                                builder.setView(R.layout.loading_dialog);
                                final AlertDialog dialog = builder.create();
                                dialog.show();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (MyGitUtility.push(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString())) {
                                            ((TextView) aTextView[0]).setTextColor(Color.BLACK);
                                            ((TextView) aTextView[0]).setText(MyGitUtility.getRemoteGit(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString()).getNickname());
                                        }
                                        dialog.dismiss();
                                    }
                                }).start();

                                 */
                            }

                        } else if (id == R.id.Pull) {
                            if (!MyApplication.isNetworkConnected()) {
                                Log.d(TAG, "no netework ");

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(activity, "No Network", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        waitDialog.show();

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if (MyGitUtility.pull(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString())) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pulling_success), Toast.LENGTH_SHORT).show();
                                                                waitDialog.dismiss();
                                                            }
                                                        });
                                                    }else{
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(activity, MyApplication.getAppContext().getText(R.string.pulling_failed), Toast.LENGTH_SHORT).show();
                                                                waitDialog.dismiss();
                                                            }
                                                        });

                                                    }
                                                } catch (Exception e) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(activity,MyApplication.getAppContext().getText(R.string.pulling_failed) , Toast.LENGTH_SHORT).show();
                                                            waitDialog.dismiss();
                                                        }
                                                    });
                                                    throw new RuntimeException(e);
                                                }
                                            }                                                // Your Code

                                        }, 300);
                                    }
                                });
                                /*
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (MyGitUtility.pull(MyApplication.getAppContext(), ((TextView) aTextView[1]).getText().toString())) {

                                        }else{
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(activity,MyApplication.getAppContext().getText(R.string.pulling_failed) , Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                }).start();

                                 */
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
                            AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
                            TextView txtUrl = new TextView(activity);
                            String sListMessages = "";
                            txtUrl.setMaxLines(10);
                            txtUrl.setMovementMethod(new ScrollingMovementMethod());
                            //    txtUrl.setCompoundDrawablesWithIntrinsicBounds(R.drawable.list24, 0, 0, 0);
                            int i = 0;


                            FrameLayout container = new FrameLayout(activity);
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.leftMargin = 20;
                            params.rightMargin = 20;
                            txtUrl.setLayoutParams(params);
                            container.addView(txtUrl);


                            for (RevCommit aRev : MyGitUtility.getLocalCommitLogList(activity, sRemoteUrl)) {
                                if( aRev.getFullMessage()==null || aRev.getFullMessage().trim().isEmpty())
                                  continue;
                                i++;
                                sListMessages = sListMessages + "\n" + getDate(aRev.getCommitTime()) + "\n--\n" + aRev.getFullMessage() + "\n";
                                if (i == 50) break;

                            }
                            txtUrl.setText(sListMessages);
                            dialogbuilder.setView(container).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                        }
                                    }).start();
                                }
                            });
                            dialogbuilder.create().show();
                        } else if (id == R.id.show_all_remote_branches) {
                            String sNoteName = ((TextView) aTextView[0]).getText().toString();
                            String sRemoteUrl = ((TextView) aTextView[1]).getText().toString();
                            AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
                            TextView txtUrl = new TextView(activity);
                            String sListMessages = "";
                            txtUrl.setMaxLines(10);
                            txtUrl.setMovementMethod(new ScrollingMovementMethod());
                            //    txtUrl.setCompoundDrawablesWithIntrinsicBounds(R.drawable.list24, 0, 0, 0);
                            int i = 0;


                            FrameLayout container = new FrameLayout(activity);
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.leftMargin = 20;
                            params.rightMargin = 20;
                            txtUrl.setLayoutParams(params);
                            container.addView(txtUrl);

                            List<String> aBranchesList = MyGitUtility.fetchGitBranches(activity, sRemoteUrl);
                            for (String sBranch : aBranchesList) {
                                i++;

                                sListMessages = sListMessages + "\n" + sBranch;
                                if (i == 50) break;

                            }
                            txtUrl.setText(sListMessages);
                            dialogbuilder.setView(container).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                        }
                                    }).start();
                                }
                            });
                            dialogbuilder.create().show();
                        }else if (id == R.id.Backup) {
                            String sBackupLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                            String sBackupZip = ((TextView) aTextView[0]).getText().toString()+"_"+inmethod.commons.util.DateUtil.getDateStringWithFormat("yyyyMMdd")+".zip";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    waitDialog.show();

                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                MyGitUtility.backup(activity, sRemoteUrl, sBackupLocation + "/" + sBackupZip);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.backup_success) + "\n" + sBackupZip, Toast.LENGTH_SHORT).show();
                                                        waitDialog.dismiss();
                                                    }
                                                });
                                            } catch (Exception e) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MyApplication.getAppContext(), MyApplication.getAppContext().getText(R.string.backup_failed)+"\n"+sBackupZip, Toast.LENGTH_SHORT).show();
                                                        waitDialog.dismiss();
                                                    }
                                                });
                                                throw new RuntimeException(e);
                                            }
                                        }                                                // Your Code

                                    }, 300);
                                }
                            });
                        }
                        return true;
                    }
                });
                popup.show();
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

    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.clear();
        MyApplication.resetFiles();
        ArrayList<RemoteGit> aList = MyGitUtility.getRemoteGitList(MyApplication.getAppContext());
        boolean bCloning = false;
        for (final RemoteGit a : aList) {
            adapter.addData(new GitList(a.getNickname(), a.getUrl(), (int) a.getStatus(), a.getBranch()));
            if (a.getUrl().indexOf("local") == -1 && a.getStatus() != MyGitUtility.GIT_STATUS_CLONING) {
                Log.d(TAG, "try to pull from remote git to local repository");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String sBranchName = MyGitUtility.getLocalBranchName(MyApplication.getAppContext(), a.getUrl());
                        Log.d(TAG, "remote url = " + a.getUrl() +",local branch name = " + sBranchName + ", setting's branch name = " + a.getRemoteName());
                        if (sBranchName != null && sBranchName.equalsIgnoreCase(a.getRemoteName()))
                            if (MyApplication.isNetworkConnected())
                                MyGitUtility.pull(MyApplication.getAppContext(), a.getUrl());
                    }
                }).start();
            }
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
        } else if (id == R.id.action_clone_git_remote) {
            Intent intent = new Intent(MainActivity.this, CloneGitActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_create_local_git) {
            Intent intent = new Intent(MainActivity.this, CreateLocalGitActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private String getDate(long time) {
        Date date = new Date(time * 1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss"); // the format of your date
        // sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        return sdf.format(date);
    }
}
