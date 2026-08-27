package inmethod.gitnotetaking;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.eclipse.jgit.util.FileUtils;

import java.io.File;

import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
import inmethod.gitnotetaking.utility.MyGitUtility;

public class CloneGitActivity extends AppCompatActivity {
    public static final String TAG =MainActivity.TAG;

    private String sRemoteName = "";
    private EditText editRemoteURL = null;
    private EditText editUserAccount = null;
    private EditText editUserPassword = null;
    private EditText editNickName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone_git);
        final Activity activity = this;
        View view =  findViewById(android.R.id.content);
        MyApplication.setView(activity,view);

        sRemoteName = PreferenceManager.getDefaultSharedPreferences(activity).getString("GitRemoteName", "master");
        editRemoteURL = (EditText) findViewById(R.id.editRemoteURL);
        editUserAccount = (EditText) findViewById(R.id.editUserAccount);
        editUserPassword = (EditText) findViewById(R.id.editUserPassword);
        editNickName = (EditText) findViewById(R.id.editLocalGitName);
        Button buttonOK = (Button) findViewById(R.id.buttonOK);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String sUrl = editRemoteURL.getText().toString().trim();
                final String sUid = editUserAccount.getText().toString().trim();
                final String sPwd = editUserPassword.getText().toString().trim();
                final String sNick = editNickName.getText().toString().trim();

                if (sRemoteName.isEmpty() || sUrl.isEmpty() || sUid.isEmpty() || sPwd.isEmpty() || sNick.isEmpty()) {
                    AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(activity);
                    MyAlertDialog.setTitle(getResources().getString(R.string.tv_title_remote_git_clone));
                    MyAlertDialog.setMessage(getResources().getString(R.string.tv_all_parametes_must_be_set));
                    DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    };
                    MyAlertDialog.setNeutralButton("OK", OkClick);
                    MyAlertDialog.show();
                    return;
                }

                RemoteGit aValue = MyGitUtility.getRemoteGit(activity, sUrl);

                if (aValue == null) {
                    try {
                        if (MyGitUtility.checkLocalGitRepository(activity, sUrl)) {
                            String sLocalDirectory = MyGitUtility.getLocalGitDirectory(activity, sUrl);
                            FileUtils.delete(new File(sLocalDirectory),FileUtils.RECURSIVE);
                        }


                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setCancelable(false);
                            builder.setView(R.layout.loading_dialog);
                            final AlertDialog dialog = builder.create();
                            dialog.show();
                            aValue = new RemoteGit();
                            aValue.setId(0);
                            aValue.setRemoteName(sRemoteName);
                            aValue.setBranch(sRemoteName);
                            aValue.setUrl(sUrl);
                            aValue.setUid(sUid);
                            aValue.setPwd(sPwd);
                            aValue.setNickname(sNick);
                            aValue.setStatus(MyGitUtility.GIT_STATUS_CLONING);
                            aValue.setAuthor_name(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorName", "root"));
                            aValue.setAuthor_email(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorEmail", "root@your.email.com"));
                            RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(MyApplication.getAppContext());
                            aRemoteGitDAO.insert(aValue);
                            aRemoteGitDAO.close();
                            dialog.dismiss();
                            onBackPressed();
                            try {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        RemoteGit aValue = new RemoteGit();
                                        aValue.setId(0);
                                        aValue.setRemoteName(sRemoteName);
                                        aValue.setUrl(sUrl);
                                        aValue.setUid(sUid);
                                        aValue.setPwd(sPwd);
                                        aValue.setNickname(sNick);
                                        aValue.setAuthor_name(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorName", "root"));
                                        aValue.setAuthor_email(PreferenceManager.getDefaultSharedPreferences(activity).getString("GitAuthorEmail", "root@your.email.com"));
                                        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(MyApplication.getAppContext());
                                        if (MyGitUtility.cloneGit(MyApplication.getAppContext(), sUrl, sRemoteName, sUid, sPwd)) {
                                            aValue.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                                            if( aRemoteGitDAO.updateByRemoteUrl(aValue) )
                                                Log.d(TAG,aValue.getNickname() +"cloning success");
                                            else
                                                Log.d(TAG,aValue.getNickname() +"cloning fail");

                                            aRemoteGitDAO.close();
                                        } else {
                                            aValue.setStatus(MyGitUtility.GIT_STATUS_PUSH_FAIL);
                                            aRemoteGitDAO.updateByRemoteUrl(aValue);
                                            aRemoteGitDAO.close();
                                        }
                                    }
                                }).start();

                            } catch (Exception ee) {
                                aValue.setStatus(MyGitUtility.GIT_STATUS_PUSH_FAIL);
                                aRemoteGitDAO.updateByRemoteUrl(aValue);
                                aRemoteGitDAO.close();
                                ee.printStackTrace();
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
                    MyAlertDialog.show();
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
