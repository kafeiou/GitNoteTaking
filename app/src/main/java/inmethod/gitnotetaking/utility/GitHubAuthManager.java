package inmethod.gitnotetaking.utility;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;

import inmethod.gitnotetaking.MyApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GitHubAuthManager {
    public static final String TAG = "GitHubAuthManager";
    public static final String GITHUB_CLI_CLIENT_ID = "178c6fc778cca21e8f48";
    public static final String GITHUB_CLIENT_ID = "Ov23licBa82hfK5H5sos";
    public static final String GITHUB_CLIENT_SECRET = "21385fe595345f415e04e715b06ec77b7812f344";
    public static final String GITHUB_REDIRECT_URI = "gitnotetaking://oauth/github";

    private static GitHubAuthManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isPollingCancelled = false;

    public interface DeviceCodeCallback {
        void onCodeReceived(String userCode, String verificationUri);
        void onError(String errorMessage);
    }

    public interface GitHubAuthCallback {
        void onSuccess(String username, String accessToken, List<GitHubRepo> noteRepos, int totalReposCount);
        void onError(String errorMessage);
    }

    public static class GitHubRepo {
        private final String name;
        private final String fullName;
        private final String cloneUrl;
        private final String defaultBranch;
        private final String description;
        private final boolean isPrivate;

        public GitHubRepo(String name, String fullName, String cloneUrl, String defaultBranch, String description, boolean isPrivate) {
            this.name = name;
            this.fullName = fullName;
            this.cloneUrl = cloneUrl;
            this.defaultBranch = defaultBranch;
            this.description = description;
            this.isPrivate = isPrivate;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return fullName;
        }

        public String getCloneUrl() {
            return cloneUrl;
        }

        public String getDefaultBranch() {
            return defaultBranch != null && !defaultBranch.isEmpty() ? defaultBranch : "master";
        }

        public String getDescription() {
            return description != null ? description : "";
        }

        public boolean isPrivate() {
            return isPrivate;
        }
    }

    private GitHubAuthManager() {}

    public static synchronized GitHubAuthManager getInstance() {
        if (instance == null) {
            instance = new GitHubAuthManager();
        }
        return instance;
    }

    public void startOAuthWebFlow(Activity activity) {
        String authUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=" + GITHUB_CLIENT_ID +
                "&scope=" + Uri.encode("repo read:user") +
                "&redirect_uri=" + Uri.encode(GITHUB_REDIRECT_URI);
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.launchUrl(activity, Uri.parse(authUrl));
        } catch (Exception e) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            activity.startActivity(browserIntent);
        }
    }

    public void exchangeCodeForToken(String code, GitHubAuthCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://github.com/login/oauth/access_token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData = "client_id=" + URLEncoder.encode(GITHUB_CLIENT_ID, "UTF-8") +
                        "&client_secret=" + URLEncoder.encode(GITHUB_CLIENT_SECRET, "UTF-8") +
                        "&code=" + URLEncoder.encode(code, "UTF-8") +
                        "&redirect_uri=" + URLEncoder.encode(GITHUB_REDIRECT_URI, "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                String response = readStream(is);
                conn.disconnect();

                JSONObject json = new JSONObject(response);
                if (json.has("access_token")) {
                    String accessToken = json.getString("access_token");
                    loadUserDataAndRepos(accessToken, callback);
                } else {
                    String error = json.optString("error_description", json.optString("error", "Failed to obtain access token"));
                    mainHandler.post(() -> callback.onError(error));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to exchange OAuth code", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void cancelPolling() {
        isPollingCancelled = true;
    }

    public void startDeviceFlow(Activity activity, DeviceCodeCallback deviceCodeCallback, GitHubAuthCallback authCallback) {
        isPollingCancelled = false;
        new Thread(() -> {
            try {
                URL url = new URL("https://github.com/login/device/code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData = "client_id=" + URLEncoder.encode(GITHUB_CLI_CLIENT_ID, "UTF-8") +
                        "&scope=" + URLEncoder.encode("repo read:user", "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                String response = readStream(is);
                conn.disconnect();

                JSONObject json = new JSONObject(response);
                if (!json.has("device_code")) {
                    String err = json.optString("error_description", json.optString("error", "Unknown error requesting device code"));
                    mainHandler.post(() -> deviceCodeCallback.onError(err));
                    return;
                }

                String deviceCode = json.getString("device_code");
                String userCode = json.getString("user_code");
                String verificationUri = json.optString("verification_uri", "https://github.com/login/device");
                int interval = json.optInt("interval", 5);
                int expiresIn = json.optInt("expires_in", 900);

                // Copy user code to clipboard on main thread
                mainHandler.post(() -> {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("GitHub Code", userCode);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to copy to clipboard", e);
                    }
                    deviceCodeCallback.onCodeReceived(userCode, verificationUri);
                });

                // Start polling in background
                pollForAccessToken(deviceCode, interval, expiresIn, authCallback);

            } catch (Exception e) {
                Log.e(TAG, "Device flow request failed", e);
                mainHandler.post(() -> deviceCodeCallback.onError(e.getMessage()));
            }
        }).start();
    }

    private void pollForAccessToken(String deviceCode, int intervalSec, int expiresInSec, GitHubAuthCallback authCallback) {
        long startTime = System.currentTimeMillis();
        long expiryTime = startTime + (expiresInSec * 1000L);
        int currentInterval = Math.max(intervalSec, 5);

        while (System.currentTimeMillis() < expiryTime && !isPollingCancelled) {
            try {
                Thread.sleep(currentInterval * 1000L);
            } catch (InterruptedException e) {
                break;
            }

            if (isPollingCancelled) {
                break;
            }

            try {
                URL url = new URL("https://github.com/login/oauth/access_token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData = "client_id=" + URLEncoder.encode(GITHUB_CLI_CLIENT_ID, "UTF-8") +
                        "&device_code=" + URLEncoder.encode(deviceCode, "UTF-8") +
                        "&grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                String response = readStream(is);
                conn.disconnect();

                JSONObject json = new JSONObject(response);
                if (json.has("access_token")) {
                    String accessToken = json.getString("access_token");
                    loadUserDataAndRepos(accessToken, authCallback);
                    return;
                }

                String error = json.optString("error", "");
                if ("authorization_pending".equals(error)) {
                    // Still waiting for user authorization, continue loop
                    continue;
                } else if ("slow_down".equals(error)) {
                    currentInterval += 5;
                } else if ("expired_token".equals(error)) {
                    mainHandler.post(() -> authCallback.onError("授權碼已過期，請重新嘗試"));
                    return;
                } else if ("access_denied".equals(error)) {
                    mainHandler.post(() -> authCallback.onError("使用者已拒絕授權"));
                    return;
                } else {
                    String errorDesc = json.optString("error_description", error);
                    mainHandler.post(() -> authCallback.onError(errorDesc));
                    return;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error polling access token", e);
            }
        }
    }

    public void loadUserDataAndRepos(String accessToken, GitHubAuthCallback callback) {
        new Thread(() -> {
            try {
                String username = fetchUsername(accessToken);
                List<GitHubRepo> allRepos = fetchUserRepos(accessToken);
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getAppContext());
                String prefix = sp.getString("GitHubRepoPrefix", "note").trim().toLowerCase();
                List<GitHubRepo> noteRepos = new ArrayList<>();
                for (GitHubRepo repo : allRepos) {
                    if (prefix.isEmpty() || repo.getName().toLowerCase().startsWith(prefix)) {
                        noteRepos.add(repo);
                    }
                }
                mainHandler.post(() -> callback.onSuccess(username, accessToken, noteRepos, allRepos.size()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load GitHub user/repos", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public String fetchUsername(String accessToken) throws Exception {
        URL url = new URL("https://api.github.com/user");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "InMethodGitNoteTaking-Android");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String err = readStream(conn.getErrorStream());
            conn.disconnect();
            throw new Exception("GitHub API Error (" + responseCode + "): " + err);
        }

        String response = readStream(conn.getInputStream());
        conn.disconnect();

        JSONObject json = new JSONObject(response);
        return json.getString("login");
    }

    public List<GitHubRepo> fetchUserRepos(String accessToken) throws Exception {
        List<GitHubRepo> repoList = new ArrayList<>();
        java.util.Set<String> seenFullNames = new java.util.HashSet<>();
        int page = 1;
        while (true) {
            URL url = new URL("https://api.github.com/user/repos?per_page=100&page=" + page + "&sort=updated&affiliation=owner,collaborator,organization_member");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "InMethodGitNoteTaking-Android");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String err = readStream(conn.getErrorStream());
                conn.disconnect();
                throw new Exception("GitHub API Error (" + responseCode + "): " + err);
            }

            String response = readStream(conn.getInputStream());
            conn.disconnect();

            JSONArray array = new JSONArray(response);
            if (array.length() == 0) {
                break;
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                String fullName = obj.optString("full_name", name);
                String cloneUrl = obj.optString("clone_url", "https://github.com/" + fullName + ".git");
                String defaultBranch = obj.optString("default_branch", "master");
                String description = "";
                if (!obj.isNull("description")) {
                    description = obj.optString("description", "").trim();
                    if (description.equalsIgnoreCase("null")) {
                        description = "";
                    }
                }
                boolean isPrivate = obj.optBoolean("private", false);

                String key = fullName.toLowerCase().trim();
                if (!seenFullNames.contains(key)) {
                    seenFullNames.add(key);
                    repoList.add(new GitHubRepo(name, fullName, cloneUrl, defaultBranch, description, isPrivate));
                }
            }

            if (array.length() < 100) {
                break;
            }
            page++;
            if (page > 5) break; // Limit to 500 repos max
        }
        return repoList;
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
