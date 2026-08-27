package inmethod.gitnotetaking;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;

public class ModifyLocalGitActivity extends AppCompatActivity {

    private String sRemoteURL = null;
    private EditText editNickName = null;
    private Activity activity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_modify_local);
        activity = this;
        View view =  findViewById(android.R.id.content);
        MyApplication.setView(activity,view);

        Intent myIntent = getIntent();
        sRemoteURL =   myIntent.getStringExtra("GIT_REMOTE_URL");
        editNickName = (EditText) findViewById(R.id.editLocalGitName);

        Button buttonOK = (Button) findViewById(R.id.buttonOK);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String sNick = editNickName.getText().toString().trim();
                if (sNick.isEmpty()) {
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
                }else{
                    RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(activity);
                    RemoteGit aValue = aRemoteGitDAO.getByURL(sRemoteURL);


                    if (aValue != null) {
                        try {
                            aValue.setNickname(sNick);
                            aRemoteGitDAO.update(aValue);
                        } catch (Exception ex) {

                        }
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ModifyLocalGitActivity.this, activity.getResources().getText(R.string.main_notes_not_found), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    aRemoteGitDAO.close();
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


        if (aValue != null) {
            try {
                editNickName.setText(aValue.getNickname());
            } catch (Exception ex) {

            }
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ModifyLocalGitActivity.this, activity.getResources().getText(R.string.main_notes_not_found), Toast.LENGTH_SHORT).show();
                }
            });
        }
        aRemoteGitDAO.close();
    }
}
