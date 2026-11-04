package inmethod.gitnotetaking.view;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
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
    public static final float STATUS_SUFFIX_SIZE_RATIO = 0.65f;

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

    public static SpannableString formatTitleWithStatus(String baseTitle, String statusSuffix) {
        String safeBase = baseTitle != null ? baseTitle : "";
        String safeSuffix = statusSuffix != null ? statusSuffix : "";
        if (safeSuffix.isEmpty()) {
            return new SpannableString(safeBase);
        }
        String fullText = safeBase + " " + safeSuffix;
        SpannableString span = new SpannableString(fullText);
        int start = safeBase.length() + 1;
        span.setSpan(new RelativeSizeSpan(STATUS_SUFFIX_SIZE_RATIO), start, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    public static void mapDeviceInfoToLayout(Object[] layoutData, Object aGitListObject) {
        GitList aGitList = (GitList) aGitListObject;
        TextView layout0 = ((TextView) layoutData[0]);

        String baseTitle = !aGitList.getBranch().equalsIgnoreCase(BRANCH_MASTER)
                ? "[" + aGitList.getBranch() + "] " + aGitList.getGitName()
                : aGitList.getGitName();

        if (aGitList.getRemoteUrl().indexOf("local") == -1) {
            if (aGitList.getPushStatus() == MyGitUtility.GIT_STATUS_PUSH_FAIL) {
                int errorColor = androidx.core.content.ContextCompat.getColor(layout0.getContext(), R.color.colorAccent);
                layout0.setTextColor(errorColor);
                String statusSuffix = MyApplication.getAppContext().getResources().getString(R.string.main_notes_need_push);
                layout0.setText(formatTitleWithStatus(baseTitle, statusSuffix));
            } else if (aGitList.getPushStatus() == MyGitUtility.GIT_STATUS_AUTH_FAILED) {
                int errorColor = androidx.core.content.ContextCompat.getColor(layout0.getContext(), R.color.colorAccent);
                layout0.setTextColor(errorColor);
                String statusSuffix = MyApplication.getAppContext().getResources().getString(R.string.main_notes_auth_expired);
                layout0.setText(formatTitleWithStatus(baseTitle, statusSuffix));
            } else if (aGitList.getPushStatus() == MyGitUtility.GIT_STATUS_CLONING) {
                int errorColor = androidx.core.content.ContextCompat.getColor(layout0.getContext(), R.color.colorAccent);
                layout0.setTextColor(errorColor);
                String progress = MyGitUtility.getCloneProgress(aGitList.getRemoteUrl());
                if (progress != null && !progress.isEmpty()) {
                    layout0.setText(formatTitleWithStatus("「" + aGitList.getGitName() + "」", progress));
                } else {
                    String cloningSuffix = MyApplication.getAppContext().getResources().getString(R.string.main_notes_cloning);
                    layout0.setText(formatTitleWithStatus("「" + aGitList.getGitName() + "」", cloningSuffix));
                }
            } else {
                layout0.setTextColor(androidx.core.content.ContextCompat.getColor(layout0.getContext(), R.color.text_primary));
                layout0.setText(baseTitle);
            }

        } else {
            layout0.setTextColor(androidx.core.content.ContextCompat.getColor(layout0.getContext(), R.color.text_primary));
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
