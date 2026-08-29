package inmethod.gitnotetaking.view;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import inmethod.gitnotetaking.MyApplication;
import inmethod.gitnotetaking.R;
import inmethod.gitnotetaking.utility.MyGitUtility;

public class GitList {

    private String sRemoteUrl = null;
    private String sGitName = null;
    private int iPushStatus = MyGitUtility.GIT_STATUS_SUCCESS;
    private String sBranch = BRANCH_MASTER;
    public static final String BRANCH_MASTER = "master";

    private GitList() {
    }

    public GitList(String sGitName, String sRemoteUrl, int iPushStatus, String sBranch) {
        this.sGitName = sGitName;
        this.sRemoteUrl = sRemoteUrl;
        this.iPushStatus = iPushStatus;
        this.sBranch = sBranch;
    }

    public String getRemoteUrl() {
        return sRemoteUrl;
    }

    public void setsRemoteUrl(String sRemoteUrl) {
        this.sRemoteUrl = sRemoteUrl;
    }

    public String getGitName() {
        return sGitName;
    }

    public void setGitName(String sGitName) {
        this.sGitName = sGitName;
    }

    public int getPushStatus() {
        return iPushStatus;
    }

    public void setPushStatus(int iPushStatus) {
        this.iPushStatus = iPushStatus;
    }

    public void setBranch(String sBranch) {
        this.sBranch = sBranch;
    }

    public String getBranch() {
        return this.sBranch;
    }

    public static void mapDeviceInfoToLayout(Object[] layoutData, Object aGitListObject) {
        GitList aGitList = (GitList) aGitListObject;
        TextView layout0 = ((TextView) layoutData[0]);

        if (aGitList.getRemoteUrl().indexOf("local") == -1) {
            if (aGitList.getPushStatus() ==  MyGitUtility.GIT_STATUS_PUSH_FAIL) {
                layout0.setTextColor(Color.RED);
                if (!aGitList.getBranch().equalsIgnoreCase(BRANCH_MASTER))
                    layout0.setText( "[" + aGitList.getBranch() + "] "+aGitList.getGitName() + MyApplication.getAppContext().getResources().getString(R.string.main_notes_need_push) );
                else
                    layout0.setText(aGitList.getGitName() + MyApplication.getAppContext().getResources().getString(R.string.main_notes_need_push));
            } else if (aGitList.getPushStatus() ==  MyGitUtility.GIT_STATUS_CLONING) {
                layout0.setTextColor(Color.RED);
                layout0.setText("「"+aGitList.getGitName() + "」" + MyApplication.getAppContext().getResources().getString(R.string.main_notes_cloning));
            } else {
                layout0.setTextColor(androidx.core.content.ContextCompat.getColor(MyApplication.getAppContext(), R.color.text_primary));

                if (!aGitList.getBranch().equalsIgnoreCase(BRANCH_MASTER))
                    layout0.setText("[" + aGitList.getBranch() + "] "+aGitList.getGitName());
                else
                    layout0.setText(aGitList.getGitName());
            }

        } else {
            layout0.setTextColor(androidx.core.content.ContextCompat.getColor(MyApplication.getAppContext(), R.color.text_primary));
            layout0.setText(aGitList.getGitName());
        }

        ((TextView) layoutData[1]).setText(aGitList.getRemoteUrl());

    }

    public static Object[] getDeviceInfoFromLayoutId(View view) {
        TextView[] aLayoutData = new TextView[2];
        aLayoutData[0] = (TextView) view.findViewById(R.id.git_name);
        aLayoutData[1] = (TextView) view.findViewById(R.id.git_remote_url);
        return aLayoutData;
    }
}
