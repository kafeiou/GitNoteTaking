package inmethod.gitnotetaking;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class CustomPreferenceFragment extends PreferenceFragmentCompat {

    public static final String FRAGMENT_TAG = "my_preference_fragment";
    public static final String TAG = MainActivity.TAG;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settingspreferences, rootKey);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        ListPreference appLanguage = (ListPreference) findPreference("AppLanguage");
        if (appLanguage != null) {
            String currentLang = sharedPreferences.getString("AppLanguage", "system");
            if ("ja".equalsIgnoreCase(currentLang)) {
                currentLang = "ja-JP";
                sharedPreferences.edit().putString("AppLanguage", currentLang).apply();
            }
            appLanguage.setValue(currentLang);
            int langIndex = appLanguage.findIndexOfValue(currentLang);
            if (langIndex >= 0) {
                appLanguage.setSummary(appLanguage.getEntries()[langIndex]);
            }
            appLanguage.setOnPreferenceChangeListener((preference, o) -> {
                String selectedLang = (o != null) ? o.toString().trim() : "system";
                if ("ja".equalsIgnoreCase(selectedLang)) {
                    selectedLang = "ja-JP";
                }
                sharedPreferences.edit().putString("AppLanguage", selectedLang).apply();
                int idx = ((ListPreference) preference).findIndexOfValue(selectedLang);
                if (idx >= 0) {
                    appLanguage.setSummary(((ListPreference) preference).getEntries()[idx]);
                }
                if ("system".equalsIgnoreCase(selectedLang) || selectedLang.isEmpty()) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLang));
                }
                if (getActivity() != null) {
                    getActivity().getWindow().getDecorView().post(() -> {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().recreate();
                        }
                    });
                }
                return true;
            });
        }

        ListPreference appTheme = (ListPreference) findPreference("AppTheme");
        if (appTheme != null) {
            String currentTheme = sharedPreferences.getString("AppTheme", "system");
            appTheme.setValue(currentTheme);
            int themeIndex = appTheme.findIndexOfValue(currentTheme);
            if (themeIndex >= 0) {
                appTheme.setSummary(appTheme.getEntries()[themeIndex]);
            }
            appTheme.setOnPreferenceChangeListener((preference, o) -> {
                String selectedTheme = (o != null) ? o.toString().trim() : "system";
                sharedPreferences.edit().putString("AppTheme", selectedTheme).apply();
                int idx = ((ListPreference) preference).findIndexOfValue(selectedTheme);
                if (idx >= 0) {
                    appTheme.setSummary(((ListPreference) preference).getEntries()[idx]);
                }
                applyThemeMode(selectedTheme);
                if (getActivity() != null) {
                    getActivity().getWindow().getDecorView().post(() -> {
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().finish();
                            getActivity().startActivity(getActivity().getIntent());
                        }
                    });
                }
                return true;
            });
        }

        EditTextPreference GitAuthorName = (EditTextPreference) findPreference("GitAuthorName");
        if (GitAuthorName != null) {
            GitAuthorName.setSummary(sharedPreferences.getString("GitAuthorName", ""));
            GitAuthorName.setOnPreferenceChangeListener((preference, o) -> {
                String yourString = (o != null) ? o.toString().trim() : "";
                ((EditTextPreference) preference).setText(yourString);
                GitAuthorName.setSummary(yourString);
                return false;
            });
        }

        EditTextPreference GitAuthorEmail = (EditTextPreference) findPreference("GitAuthorEmail");
        if (GitAuthorEmail != null) {
            GitAuthorEmail.setSummary(sharedPreferences.getString("GitAuthorEmail", ""));
            GitAuthorEmail.setOnPreferenceChangeListener((preference, o) -> {
                String yourString = (o != null) ? o.toString().trim() : "";
                ((EditTextPreference) preference).setText(yourString);
                GitAuthorEmail.setSummary(yourString);
                return false;
            });
        }

        ListPreference aSort = (ListPreference) findPreference("Sort");
        if (aSort != null) {
            aSort.setDefaultValue(sharedPreferences.getString("Sort", "11"));
            int sortIndex = aSort.findIndexOfValue(sharedPreferences.getString("Sort", "11"));
            if (sortIndex >= 0) {
                aSort.setSummary(aSort.getEntries()[sortIndex]);
            }
            aSort.setOnPreferenceChangeListener((preference, o) -> {
                int index = ((ListPreference) preference).findIndexOfValue(o.toString());
                if (index >= 0) {
                    aSort.setSummary(((ListPreference) preference).getEntries()[index]);
                }
                String yourString = (o != null) ? o.toString().trim() : "";
                sharedPreferences.edit().putString("Sort", yourString).apply();
                return true;
            });
        }

        EditTextPreference GitEditTextSize = (EditTextPreference) findPreference("GitEditTextSize");
        if (GitEditTextSize != null) {
            GitEditTextSize.setSummary(sharedPreferences.getString("GitEditTextSize", ""));
            GitEditTextSize.setOnPreferenceChangeListener((preference, o) -> {
                String yourString = (o != null) ? o.toString().trim() : "";
                ((EditTextPreference) preference).setText(yourString);
                GitEditTextSize.setSummary(yourString);
                return false;
            });
        }

        EditTextPreference GitLocalDirName = (EditTextPreference) findPreference("GitLocalDirName");
        if (GitLocalDirName != null) {
            GitLocalDirName.setSummary(sharedPreferences.getString("GitLocalDirName", "gitnotetaking"));
            GitLocalDirName.setOnPreferenceChangeListener((preference, o) -> {
                String yourString = (o != null) ? o.toString().trim() : "";
                ((EditTextPreference) preference).setText(yourString);
                GitLocalDirName.setSummary(yourString);
                return false;
            });
        }

        EditTextPreference GitRemoteName = (EditTextPreference) findPreference("GitRemoteName");
        if (GitRemoteName != null) {
            GitRemoteName.setSummary(sharedPreferences.getString("GitRemoteName", "master"));
            GitRemoteName.setOnPreferenceChangeListener((preference, o) -> {
                String yourString = (o != null) ? o.toString().trim() : "";
                ((EditTextPreference) preference).setText(yourString);
                GitRemoteName.setSummary(yourString);
                return false;
            });
        }

        Preference gitPurgeHistory = findPreference("GitPurgeHistory");
        if (gitPurgeHistory != null) {
            gitPurgeHistory.setOnPreferenceClickListener(preference -> {
                if (getContext() == null || getActivity() == null) return true;

                new androidx.appcompat.app.AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.pref_purge_dialog_title)
                        .setMessage(R.string.pref_purge_dialog_msg)
                        .setPositiveButton(R.string.pref_purge_btn_confirm, (dialog, which) -> {
                            showWaitDialog();
                            new Thread(() -> {
                                inmethod.gitnotetaking.utility.MyGitUtility.purgeAllLocalRepositoriesHistory(MyApplication.getAppContext());
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        dismissWaitDialog();
                                        android.widget.Toast.makeText(MyApplication.getAppContext(), R.string.pref_purge_success, android.widget.Toast.LENGTH_LONG).show();
                                    });
                                }
                            }).start();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                return true;
            });
        }
    }

    private androidx.appcompat.app.AlertDialog waitDialog;

    private void showWaitDialog() {
        if (getActivity() == null) return;
        if (waitDialog == null) {
            androidx.appcompat.app.AlertDialog.Builder waitBuilder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
            waitBuilder.setCancelable(false);
            waitBuilder.setView(R.layout.loading_dialog);
            waitDialog = waitBuilder.create();
        }
        if (!waitDialog.isShowing()) {
            waitDialog.show();
        }
    }

    private void dismissWaitDialog() {
        if (waitDialog != null && waitDialog.isShowing()) {
            try {
                waitDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissWaitDialog();
    }

    public static void applyThemeMode(String theme) {
        if ("dark".equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
