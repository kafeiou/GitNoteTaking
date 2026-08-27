package inmethod.gitnotetaking;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
import inmethod.gitnotetaking.utility.MyGitUtility;

public class ModifyRemoteGitActivity extends AppCompatActivity {
    public static final String TAG =MainActivity.TAG;

    private String sRemoteURL = null;
    private EditText editUserAccount = null;
    private EditText editUserPassword = null;
    private EditText editNickName = null;
    private EditText editAuthorName = null;
    private EditText editAuthorEmail = null;
    private Spinner spinnerRemoteBranch = null;
    private Activity activity;
    private ArrayAdapter<String> adapter = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_modify_remote);
        activity = this;
        View view =  findViewById(android.R.id.content);
        MyApplication.setView(activity,view);

        Intent myIntent = getIntent();
        sRemoteURL = myIntent.getStringExtra("GIT_REMOTE_URL");
        editUserAccount = (EditText) findViewById(R.id.editUserAccount);
        editUserPassword = (EditText) findViewById(R.id.editUserPassword);
        editNickName = (EditText) findViewById(R.id.editLocalGitName);
        editAuthorName = (EditText) findViewById(R.id.editAuthorName);
        editAuthorEmail = (EditText) findViewById(R.id.editAuthorEmail);
        spinnerRemoteBranch = (Spinner) findViewById(R.id.spinnerBranch);
        String[] items = new String[]{"master"};
        adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinnerRemoteBranch.setAdapter(adapter);



        Button buttonOK = (Button) findViewById(R.id.buttonOK);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String sUid = editUserAccount.getText().toString().trim();
                final String sPwd = editUserPassword.getText().toString().trim();
                final String sNick = editNickName.getText().toString().trim();
                final String sAuthorName = editAuthorName.getText().toString().trim();
                final String sAuthorEmail = editAuthorEmail.getText().toString().trim();

                if (sUid.isEmpty() || sPwd.isEmpty() || sNick.isEmpty()) {
                    AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(activity);
                    MyAlertDialog.setTitle(getResources().getString(R.string.main_notes_title_modify));
                    MyAlertDialog.setMessage(getResources().getString(R.string.tv_all_parametes_must_be_set));
                    DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    };
                    MyAlertDialog.setNeutralButton("OK", OkClick);
                    MyAlertDialog.show();
                    return;
                } else {
                    RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(activity);
                    RemoteGit aValue = aRemoteGitDAO.getByURL(sRemoteURL);


                    if (aValue != null) {
                        try {
                            aValue.setUid(sUid);
                            aValue.setPwd(sPwd);
                            aValue.setNickname(sNick);
                            aValue.setAuthor_name(sAuthorName);
                            aValue.setAuthor_email(sAuthorEmail);
                            aValue.setBranch(spinnerRemoteBranch.getSelectedItem().toString());
                            aValue.setRemoteName(spinnerRemoteBranch.getSelectedItem().toString());
                            aRemoteGitDAO.update(aValue);
                            aRemoteGitDAO.close();
                            boolean bCheck = MyGitUtility.checkout(MyApplication.getAppContext(), sRemoteURL);
                            Log.d(TAG, "bCheckout=" + bCheck);
                        } catch (Exception ex) {

                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ModifyRemoteGitActivity.this, activity.getResources().getText(R.string.main_notes_not_found), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    onBackPressed();
                    finish();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(activity);
        RemoteGit aValue = aRemoteGitDAO.getByURL(sRemoteURL);
        fetchBranches afetchBranches = new fetchBranches();//.execute();
        afetchBranches.execute();

        if (aValue != null) {
            try {
                editUserAccount.setText(aValue.getUid());
                editUserPassword.setText(aValue.getPwd());
                editNickName.setText(aValue.getNickname());
                editAuthorName.setText(aValue.getAuthor_name());
                editAuthorEmail.setText(aValue.getAuthor_email());
              //  editRemoteBranch.setText(aValue.getRemoteName());
            } catch (Exception ex) {

            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ModifyRemoteGitActivity.this, activity.getResources().getText(R.string.main_notes_not_found), Toast.LENGTH_SHORT).show();
                }
            });
        }
        aRemoteGitDAO.close();
    }
    class fetchBranches extends AsyncTask<Void, Void, Void>{
        String[] items = new String[]{"master"};
        int iPosition = 0;
        boolean bGet = false;
        @Override
        protected Void doInBackground(Void... params) {
          List<String> aRemoteBranchList = MyGitUtility.fetchGitBranches(activity,sRemoteURL);
          if( aRemoteBranchList.size()>1) {
              bGet = true;
              items = new String[aRemoteBranchList.size()];
              int i = 0;
              for (String sBranch : aRemoteBranchList) {
                  if( sBranch.equalsIgnoreCase("master"))
                      iPosition = i;
                  items[i] = sBranch;
                  i++;
              }
          }
          return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if( bGet) {
                adapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, items);
                spinnerRemoteBranch.setAdapter(adapter);
                spinnerRemoteBranch.setSelection(iPosition);
                Toast.makeText(activity,MyApplication.getAppContext().getResources().getText(R.string.modify_remote_settings_branches_fetched),Toast.LENGTH_SHORT).show();
            }
        }
    }
}
