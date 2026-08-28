package inmethod.gitnotetaking;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.io.File;

import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
import inmethod.gitnotetaking.utility.MyGitUtility;

public class CreateLocalGitActivity extends AppCompatActivity {

    private String sRemoteName = "";
    private String sRemoteURL = null;
    private EditText editLocalGitName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_local_git);
        final Activity activity = this;

        View view =  findViewById(android.R.id.content);
        MyApplication.setView(activity,view);
        Button buttonOK = (Button) findViewById(R.id.buttonOK);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sRemoteName = PreferenceManager.getDefaultSharedPreferences(activity).getString("GitRemoteName", "origin");
                sRemoteURL = "localhost://local";
                editLocalGitName = (EditText) findViewById(R.id.editLocalGitName);
                final String sLocalName = editLocalGitName.getText().toString().trim();
                if (sLocalName.isEmpty()) {
                    AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(activity);
                    MyAlertDialog.setTitle(getResources().getString(R.string.tv_title_create_local_git));
                    MyAlertDialog.setMessage(getResources().getString (R.string.tv_all_parametes_must_be_set));
                    DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    };
                    MyAlertDialog.setNeutralButton("OK", OkClick);
                    runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          MyAlertDialog.show();
                                      }

                                  }
                    );
                    return;
                }

                final RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(activity);
                sRemoteURL =  sRemoteURL+ File.separator+ sLocalName+".git";
                RemoteGit aValue = aRemoteGitDAO.getByURL( sRemoteURL);

                if (aValue == null) {
                    try {
                        if (MyGitUtility.checkLocalGitRepository(activity,sRemoteURL)) {
                            aValue = new RemoteGit();
                            aValue.setId(0);
                            aValue.setRemoteName(sRemoteName);
                            aValue.setUrl(sRemoteURL);
                            aValue.setUid("UID");
                            aValue.setPwd("PWD");
                            aValue.setNickname(sLocalName);
                            aValue.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                            aValue.setAuthor_name(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorName", "root"));
                            aValue.setAuthor_email(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorEmail", "root@your.email.com"));
                            aRemoteGitDAO.insert(aValue);
                            aRemoteGitDAO.close();
                            onBackPressed();
                            return;
                        } else {
                            try{
                                new Thread(new Runnable(){
                                    @Override
                                    public void run() {
                                        if (MyGitUtility.createLocalGitRepository(activity, sLocalName)) {
                                            RemoteGit aValue = new RemoteGit();
                                            aValue.setId(0);
                                            aValue.setRemoteName(sRemoteName);
                                            aValue.setUrl(sRemoteURL);
                                            aValue.setUid("UID");
                                            aValue.setPwd("PWD");
                                            aValue.setNickname(sLocalName);
                                            aValue.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                                            aValue.setAuthor_name(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorName", "root"));
                                            aValue.setAuthor_email(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorEmail", "root@your.email.com"));
                                            aRemoteGitDAO.insert(aValue);
                                            aRemoteGitDAO.close();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    finish();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(activity);
                                                    MyAlertDialog.setTitle(getResources().getString(R.string.tv_create_local_git_repository));
                                                    MyAlertDialog.setMessage(getResources().getString(R.string.tv_clone_fail));
                                                    MyAlertDialog.setNeutralButton("OK", null);
                                                    MyAlertDialog.show();
                                                }
                                            });
                                        }
                                    }
                                }).start();

                            }catch(Exception ee){
                                ee.printStackTrace();
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(activity);
                    MyAlertDialog.setTitle(getResources().getString(R.string.tv_title_remote_git_clone));
                    MyAlertDialog.setMessage(getResources().getString(R.string.tv_git_already_cloned));
                    DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    };
                    MyAlertDialog.setNeutralButton("OK", OkClick);
                    runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          MyAlertDialog.show();
                                      }

                                  }
                    );
                }
              //  Log.d("asdf", "count=" + aRemoteGitDAO.getCount());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}
