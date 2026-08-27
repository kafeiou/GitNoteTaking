package inmethod.gitnotetaking.utility;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import inmethod.gitnotetaking.MyApplication;
import inmethod.gitnotetaking.db.RemoteGit;
import inmethod.gitnotetaking.db.RemoteGitDAO;
import inmethod.jakarta.vcs.GitUtil;

public class MyGitUtility {

    public static final String TAG = "GitNoteTaking";
    public static final int GIT_STATUS_SUCCESS = 0;
    public static final int GIT_STATUS_PUSH_FAIL = -1;
    public static final int GIT_STATUS_CLONING = -3;
    public static final int GIT_STATUS_PULLING = -4;
    public static boolean bGitLock = false;


    public static boolean deleteLocalGitRepository(Context context, String sRemoteUrl) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        //   Log.d(TAG, "check local repository, status = " + checkLocalGitRepository(sRemoteUrl));
        if (checkLocalGitRepository(context, sRemoteUrl)) {
            GitUtil aGitUtil;
            try {
                aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
                aGitUtil.removeLocalGitRepository();
                if (aGitUtil != null) aGitUtil.close();
                return true;
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        return false;
    }

    public static List<String> fetchGitBranches(Context context, String sRemoteUrl) {
        GitUtil aGitUtil;
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        aRemoteGitDAO.close();
        if (aRemoteGit == null) return null;
        List<String> aReturn = null;
        try {
            aGitUtil = new GitUtil(sRemoteUrl, null);
            aReturn = aGitUtil.getRemoteBranches(aRemoteGit.getUid(),aRemoteGit.getPwd());
            if (aGitUtil != null) aGitUtil.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return aReturn;
    }

    public static void setGitLock(boolean bLock){
        bGitLock = bLock;
    }

    public static boolean isGitLock(){
        return bGitLock;
    }

    public static boolean push(Context context, String sRemoteUrl) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        if (aRemoteGit == null) return false;
        if (!MyApplication.isNetworkConnected()){
            aRemoteGit.setStatus( MyGitUtility.GIT_STATUS_PUSH_FAIL);
            aRemoteGitDAO.update(aRemoteGit);
            setGitLock(false);
            aRemoteGitDAO.close();
            return false;
        }

        setGitLock(true);
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        boolean bIsRemoteRepositoryExist = false;
        GitUtil aGitUtil=null;
        try {
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            bIsRemoteRepositoryExist = aGitUtil.checkRemoteRepository(aRemoteGit.getUid(), aRemoteGit.getPwd());
            if (!bIsRemoteRepositoryExist) {
                Log.e(TAG, "check remote url failed");
                aRemoteGitDAO.update(aRemoteGit);
                setGitLock(false);
                if (aGitUtil != null) aGitUtil.close();
                aRemoteGitDAO.close();
                return false;
            }
            Log.d(TAG, "Remote repository exists ? " + bIsRemoteRepositoryExist);
            if (bIsRemoteRepositoryExist) {
                Log.d(TAG, "try to push \n");
                if (aGitUtil.push(sRemoteUrl, aRemoteGit.getUid(), aRemoteGit.getPwd())) {
                    Log.d(TAG, "push finished!");
                    aRemoteGit.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                    aRemoteGitDAO.update(aRemoteGit);
                    setGitLock(false);
                    aRemoteGitDAO.close();
                    if (aGitUtil != null) aGitUtil.close();
                    return true;
                } else {
                    if (MyApplication.isNetworkConnected()) {
                        aRemoteGit.setStatus(MyGitUtility.GIT_STATUS_PUSH_FAIL);
                    }
                    aRemoteGitDAO.update(aRemoteGit);
                    Log.d(TAG, "push failed!");
                    setGitLock(false);
                    aRemoteGitDAO.close();
                    if (aGitUtil != null) aGitUtil.close();
                    return false;
                }
            }
            setGitLock(false);

            if( aRemoteGitDAO!=null)
            aRemoteGitDAO.close();
            if (aGitUtil != null) aGitUtil.close();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        setGitLock(false);
        if (aGitUtil != null) aGitUtil.close();

        return false;
    }

    public static boolean commit(Context context, String sRemoteUrl, String sCommitMessages) {

        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        GitUtil aGitUtil=null;
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        if (aRemoteGit == null) return false;
        if (!MyApplication.isNetworkConnected()){
            aRemoteGitDAO.update(aRemoteGit);
            setGitLock(false);
            aRemoteGitDAO.close();
            aRemoteGitDAO = new RemoteGitDAO(context);
            aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        }
        setGitLock(true);

        Log.d(TAG, "MyGitUtility.commit()");
        try {
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            String sAuthorName = aRemoteGit.getAuthor_name();
            String sAuthorEmail = aRemoteGit.getAuthor_email();
            aRemoteGitDAO.close();
            try {
                if (aGitUtil.commit(sCommitMessages, sAuthorName, sAuthorEmail)) {
                    aRemoteGit.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                    Log.d(TAG, "commit finished!");
                    setGitLock(false);
                    if (aGitUtil != null) aGitUtil.close();
                    return true;
                } else {
                    Log.d(TAG, "commit failed!");
                    setGitLock(false);
                    if (aGitUtil != null) aGitUtil.close();
                    return false;
                }
            }catch(LockFailedException ee){
                ee.printStackTrace();
                Log.d(TAG, "commit failed! Got LockFailedException = " + ee.getMessage());

                Log.d(TAG, "commit failed!");
                setGitLock(false);
                FileUtility.deleteLockFile(aGitUtil);
                if (aGitUtil.commit(sCommitMessages, sAuthorName, sAuthorEmail)) {
                    aRemoteGit.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                    if (aGitUtil != null) aGitUtil.close();
                    return true;
                }
                if (aGitUtil != null) aGitUtil.close();
                return false;

            }catch(JGitInternalException aJGitInternalException){
                Log.d(TAG, "commit failed! Got aJGitInternalException = " + aJGitInternalException.getMessage());
                aJGitInternalException.printStackTrace();
                    Log.d(TAG, "commit failed!");
                    setGitLock(false);
                    FileUtility.deleteLockFile(aGitUtil);
                    if (aGitUtil.commit(sCommitMessages, sAuthorName, sAuthorEmail)) {
                        aRemoteGit.setStatus(MyGitUtility.GIT_STATUS_SUCCESS);
                        if (aGitUtil != null) aGitUtil.close();
                        return true;
                    }
                    if (aGitUtil != null) aGitUtil.close();
                    return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "commit failed! Got Exception = " + e.getMessage());
            e.printStackTrace();
        }
        setGitLock(false);
        if (aGitUtil != null) aGitUtil.close();
        return false;
    }

    public static boolean deleteByRemoteUrl(Context context, String sRemoteUrl) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        boolean sReturn = aRemoteGitDAO.delete(sRemoteUrl);
        aRemoteGitDAO.close();
        return sReturn;
    }

    public static void backup(Context context,String sRemoteUrl,String sBackupDestLocation) throws Exception {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        GitUtil  aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
        aGitUtil.backup(sBackupDestLocation);
        aGitUtil.close();
    }

    public static ArrayList<RemoteGit> getRemoteGitList(Context context) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        ArrayList<RemoteGit> aList = aRemoteGitDAO.getAll();
        aRemoteGitDAO.close();
        return aList;
    }

    public static RemoteGit getRemoteGit(Context context,String sRemoteUrl) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aReturn = aRemoteGitDAO.getByURL(sRemoteUrl);
        aRemoteGitDAO.close();
        return aReturn;
    }

    public static List<RevCommit> getLocalCommitIdListByFilePath(Context context, String sRemoteUrl,String sFilePath) {
        GitUtil aGitUtil;
        List<RevCommit> aList = null;
        try {
            String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            aList = aGitUtil.getLocalCommitIdListByFilePath(sFilePath);
            if (aGitUtil != null) aGitUtil.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return aList;
    }


    public static List<RevCommit> getLocalCommitLogList(Context context, String sRemoteUrl) {
        GitUtil aGitUtil;
        List<RevCommit> aList = null;
        try {
            String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            aList = aGitUtil.getLocalCommitIdList();
            if (aGitUtil != null) aGitUtil.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return aList;
    }

    public static boolean checkout(Context context,String sRemoteUrl){
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        if (aRemoteGit == null) return false;
        String sUserName = aRemoteGit.getUid();
        String sUserPassword = aRemoteGit.getPwd();
        aRemoteGitDAO.close();
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        GitUtil aGitUtil;
        boolean bReturn = false;
        setGitLock(true);

        try {
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
                bReturn = aGitUtil.checkout(aRemoteGit.getBranch());
            aGitUtil.close();
        }catch (Exception ee){
            Log.d(TAG,ee.getLocalizedMessage());
        }
        setGitLock(false);
        return bReturn;
    }

    public static String getLocalBranchName(Context context,String sRemoteUrl){
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        GitUtil aGitUtil;
        try {
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            String sReturn = aGitUtil.getLocalDefaultBranch();
            aGitUtil.close();
            return sReturn;
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean pull(Context context, String sRemoteUrl) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        if (aRemoteGit == null) return false;
        setGitLock(true);

        String sUserName = aRemoteGit.getUid();
        aRemoteGit.setStatus(GIT_STATUS_PULLING);
        aRemoteGitDAO.update(aRemoteGit);
        String sUserPassword = aRemoteGit.getPwd();
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        boolean bIsRemoteRepositoryExist = false;
        GitUtil aGitUtil;
        try {
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            aGitUtil.setContentMergeStrategyOURS();
            bIsRemoteRepositoryExist = aGitUtil.checkRemoteRepository(sUserName, sUserPassword);
            if (!bIsRemoteRepositoryExist) {
                aRemoteGitDAO.update(aRemoteGit);
                Log.e(TAG, "check remote url failed");
                setGitLock(false);
                if (aGitUtil != null) aGitUtil.close();
                return false;
            }
            Log.d(TAG, "Remote repository exists ? " + bIsRemoteRepositoryExist);
            if (bIsRemoteRepositoryExist) {
                Log.d(TAG, "try to pull remote repository , branch="+aRemoteGit.getRemoteName());
                try{
                  if (aGitUtil.pull(aRemoteGit.getRemoteName(), sUserName, sUserPassword)) {
                    Log.d(TAG, "pull finished!");
                    syncFileTimestampsWithGit(sLocalDirectory);
                    syncFileTimestampsFromGitHubApi(sLocalDirectory, sRemoteUrl, sUserPassword);
                    aRemoteGit.setStatus(GIT_STATUS_SUCCESS);
                    aRemoteGitDAO.update(aRemoteGit);
                    setGitLock(false);
                    if (aGitUtil != null) aGitUtil.close();
                    return true;
                  } else {
                      aGitUtil.reset(ResetCommand.ResetType.MIXED, PreferenceManager.getDefaultSharedPreferences(context).getString("GitRemoteName", "master"));
                      aRemoteGitDAO.update(aRemoteGit);
                      Log.d(TAG, "pull failed!");
                      setGitLock(false);
                      if (aGitUtil != null) aGitUtil.close();
                      return false;
                  }
                }catch(LockFailedException lockfail){
                    lockfail.printStackTrace();
                    aRemoteGitDAO.update(aRemoteGit);
                    Log.d(TAG, "pull failed!");
                    setGitLock(false);
                    FileUtility.deleteLockFile(aGitUtil);
                    if (aGitUtil != null) aGitUtil.close();
                    return false;
                }catch(JGitInternalException aJGitInternalException){
                        aRemoteGitDAO.update(aRemoteGit);
                        Log.d(TAG, "pull failed!");
                        setGitLock(false);
                        FileUtility.deleteLockFile(aGitUtil);
                        if (aGitUtil != null) aGitUtil.close();
                        return false;
                }catch(WrongRepositoryStateException asd){
                    asd.printStackTrace();
                    try {
                        FileUtility.deleteLockFile(aGitUtil);
                        aGitUtil.reset(ResetCommand.ResetType.HARD, "HEAD");
                    }catch (Exception resetEx){
                        Log.e(TAG,"reset exception");
                        resetEx.printStackTrace();
                    }
                    aRemoteGitDAO.update(aRemoteGit);
                    Log.d(TAG, "pull failed!");
                    setGitLock(false);
                    if (aGitUtil != null) aGitUtil.close();
                    return false;
                }
            }
            aRemoteGitDAO.update(aRemoteGit);
            setGitLock(false);
            if (aGitUtil != null) aGitUtil.close();
            if( aRemoteGit!=null )
            aRemoteGitDAO.close();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean cloneGit(Context context, String sRemoteUrl, String sRemoteName, String sUserName, String sUserPassword) {

        // 檢查本機是否已有
        if (checkLocalGitRepository(context, sRemoteUrl)) {
            if( PreferenceManager.getDefaultSharedPreferences(context).getBoolean("GitCloneSkipLocal", false))
              return true;
            else
              return false;
        }
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);

        setGitLock(true);

        boolean bIsRemoteRepositoryExist = false;
        GitUtil aGitUtil;
        try {
            aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
            bIsRemoteRepositoryExist = aGitUtil.checkRemoteRepository(sUserName, sUserPassword);
            if (!bIsRemoteRepositoryExist) {
                Log.e(TAG, "check remote url failed");
                setGitLock(false);
                if (aGitUtil != null) aGitUtil.close();
                return false;
            }
            Log.d(TAG, "Remote repository exists ? " + bIsRemoteRepositoryExist);
            if (bIsRemoteRepositoryExist) {
                Log.d(TAG, "try to clone remote repository if local repository is not exists \n");
                if (aGitUtil.clone(sUserName, sUserPassword,1)) {
                    Log.d(TAG, "clone finished!");
                    syncFileTimestampsWithGit(sLocalDirectory);
                    syncFileTimestampsFromGitHubApi(sLocalDirectory, sRemoteUrl, sUserPassword);
                    setGitLock(false);
                    if (aGitUtil != null) aGitUtil.close();
                    return true;
                } else {
                    Log.d(TAG, "clone failed!");
                    setGitLock(false);
                    if (aGitUtil != null) aGitUtil.close();
                    return false;
                }
            } else if (bIsRemoteRepositoryExist && aGitUtil.checkLocalRepository()) {
                boolean bPull = aGitUtil.pull(sRemoteName, sUserName, sUserPassword);
                Log.d(TAG, "pull branch = " + aGitUtil.getRemoteDefaultBranch() + " , status : " + bPull);
                if (bPull) {
                    syncFileTimestampsWithGit(sLocalDirectory);
                    syncFileTimestampsFromGitHubApi(sLocalDirectory, sRemoteUrl, sUserPassword);
                }
                setGitLock(false);
                if (aGitUtil != null) aGitUtil.close();
                return bPull;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setGitLock(false);
        return false;
    }

    public static void syncFileTimestampsWithGit(String sLocalDirectory) {
        if (sLocalDirectory == null || sLocalDirectory.isEmpty()) return;
        File gitDir = new File(sLocalDirectory, ".git");
        if (!gitDir.exists()) return;

        try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build();
             RevWalk revWalk = new RevWalk(repository);
             TreeWalk treeWalk = new TreeWalk(repository)) {

            ObjectId head = repository.resolve(Constants.HEAD);
            if (head == null) return;
            RevCommit headCommit = revWalk.parseCommit(head);
            revWalk.markStart(headCommit);

            // Collect all files in working tree from HEAD commit
            treeWalk.addTree(headCommit.getTree());
            treeWalk.setRecursive(true);
            Map<String, File> filesToUpdate = new HashMap<>();
            File workingDir = new File(sLocalDirectory);
            while (treeWalk.next()) {
                String path = treeWalk.getPathString();
                File file = new File(workingDir, path);
                if (file.exists()) {
                    filesToUpdate.put(path, file);
                }
            }

            if (filesToUpdate.isEmpty()) return;

            // Iterate through commits from newest to oldest
            RevCommit commit;
            while ((commit = revWalk.next()) != null && !filesToUpdate.isEmpty()) {
                long commitTimeMs = ((long) commit.getCommitTime()) * 1000L;
                if (commit.getParentCount() == 0) {
                    // Initial commit
                    try (TreeWalk initWalk = new TreeWalk(repository)) {
                        initWalk.addTree(commit.getTree());
                        initWalk.setRecursive(true);
                        while (initWalk.next()) {
                            String path = initWalk.getPathString();
                            File f = filesToUpdate.remove(path);
                            if (f != null) {
                                f.setLastModified(commitTimeMs);
                            }
                        }
                    }
                    break;
                }

                RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                try (TreeWalk diffWalk = new TreeWalk(repository)) {
                    diffWalk.addTree(parent.getTree());
                    diffWalk.addTree(commit.getTree());
                    diffWalk.setRecursive(true);
                    diffWalk.setFilter(TreeFilter.ANY_DIFF);
                    while (diffWalk.next()) {
                        String path = diffWalk.getPathString();
                        File f = filesToUpdate.remove(path);
                        if (f != null) {
                            f.setLastModified(commitTimeMs);
                        }
                    }
                }
            }
            Log.d(TAG, "File timestamps synced with Git commit history for " + sLocalDirectory);
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync file timestamps with git", e);
        }
    }

    public static void syncFileTimestampsFromGitHubApi(String sLocalDirectory, String sRemoteUrl, String token) {
        if (sRemoteUrl == null || !sRemoteUrl.contains("github.com") || sLocalDirectory == null || token == null || token.isEmpty()) return;
        try {
            String cleanUrl = sRemoteUrl;
            if (cleanUrl.endsWith(".git")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 4);
            }
            int idx = cleanUrl.indexOf("github.com/");
            if (idx == -1) return;
            String repoPath = cleanUrl.substring(idx + "github.com/".length());
            String[] parts = repoPath.split("/");
            if (parts.length != 2) return;
            String owner = parts[0];
            String repoName = parts[1];

            File workingDir = new File(sLocalDirectory);
            if (!workingDir.exists() || !workingDir.isDirectory()) return;

            List<File> allFiles = new ArrayList<>();
            collectFiles(workingDir, allFiles);
            if (allFiles.isEmpty()) return;

            // Batch files in chunks of 40 to avoid overly large GraphQL query payloads
            int chunkSize = 40;
            for (int i = 0; i < allFiles.size(); i += chunkSize) {
                List<File> chunk = allFiles.subList(i, Math.min(i + chunkSize, allFiles.size()));
                syncChunkViaGraphQL(owner, repoName, workingDir, chunk, token);
            }
            Log.d(TAG, "File timestamps synced from GitHub GraphQL API for " + sLocalDirectory);
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync timestamps from GitHub API", e);
        }
    }

    private static void collectFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().equals(".git")) continue;
            if (f.isDirectory()) {
                collectFiles(f, result);
            } else {
                result.add(f);
            }
        }
    }

    private static void syncChunkViaGraphQL(String owner, String repoName, File rootDir, List<File> chunk, String token) {
        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("query { repository(owner: \"").append(owner).append("\", name: \"").append(repoName).append("\") { ");

            String rootPath = rootDir.getAbsolutePath();
            for (int j = 0; j < chunk.size(); j++) {
                File file = chunk.get(j);
                String relPath = file.getAbsolutePath().substring(rootPath.length());
                if (relPath.startsWith(File.separator)) {
                    relPath = relPath.substring(1);
                }
                relPath = relPath.replace('\\', '/');

                queryBuilder.append("f").append(j).append(": defaultBranchRef { target { ... on Commit { history(path: \"")
                        .append(relPath.replace("\"", "\\\""))
                        .append("\", first: 1) { nodes { committedDate } } } } } ");
            }
            queryBuilder.append("} }");

            JSONObject payload = new JSONObject();
            payload.put("query", queryBuilder.toString());

            URL url = new URL("https://api.github.com/graphql");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "InMethodGitNoteTaking-Android");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);

            byte[] out = payload.toString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONObject resJson = new JSONObject(sb.toString());
                    JSONObject data = resJson.optJSONObject("data");
                    if (data != null) {
                        JSONObject repoObj = data.optJSONObject("repository");
                        if (repoObj != null) {
                            for (int j = 0; j < chunk.size(); j++) {
                                JSONObject fObj = repoObj.optJSONObject("f" + j);
                                if (fObj != null) {
                                    JSONObject target = fObj.optJSONObject("target");
                                    if (target != null) {
                                        JSONObject history = target.optJSONObject("history");
                                        if (history != null) {
                                            JSONArray nodes = history.optJSONArray("nodes");
                                            if (nodes != null && nodes.length() > 0) {
                                                String dateStr = nodes.getJSONObject(0).optString("committedDate", "");
                                                if (!dateStr.isEmpty()) {
                                                    Instant instant = Instant.parse(dateStr);
                                                    chunk.get(j).setLastModified(instant.toEpochMilli());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error in GraphQL chunk sync", e);
        }
    }

    public static boolean createLocalGitRepository(Activity activity, String sLocalGitName) {
        try {
            Log.d(TAG,"Local git directory = "+getLocalGitDirectory(activity, "localhost://local" + File.separator + sLocalGitName + ".git"));
            GitUtil aGitUtil = new GitUtil("localhost://local" + File.separator + sLocalGitName + ".git", getLocalGitDirectory(activity, "localhost://local" + File.separator + sLocalGitName + ".git"));
            boolean bReturn = aGitUtil.createLocalRepository();
            if (aGitUtil != null) aGitUtil.close();
            return bReturn;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getLocalGitDir(String sRemoteUrl) {
        String sReturn = "";
        if (sRemoteUrl.indexOf(".git") == -1) return sReturn;
        int lastPath = sRemoteUrl.lastIndexOf("//");
        if (lastPath != -1) {
            sReturn = sRemoteUrl.substring(lastPath + 2);
        }
        if (sReturn.length() > 4)
            sReturn = sReturn.substring(0, sReturn.length() - 4);
        else return "";
        return sReturn;
    }


    public static String getLocalGitDirectory(Context context, String sRemoteUrl) {
        return context.getExternalFilesDir("")+
                File.separator + PreferenceManager.getDefaultSharedPreferences(context).getString("GitLocalDirName", "gitnotetaking") + File.separator + getLocalGitDir(sRemoteUrl);
    }

    public static boolean checkLocalGitRepository(Context context, String sRemoteUrl) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        Log.d(TAG, "default local directory = " + sLocalDirectory);
        boolean bIsLocalRepositoryExist = false;
        try {
            bIsLocalRepositoryExist = GitUtil.checkLocalRepository(sLocalDirectory + "/.git");
            Log.d(TAG, "bIsLocalRepositoryExist=" + bIsLocalRepositoryExist);
            if (bIsLocalRepositoryExist) return true;
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return false;
    }

    public static boolean purgeLocalGitRepositoryHistory(String sLocalDirectory) {
        if (sLocalDirectory == null || sLocalDirectory.isEmpty()) return false;
        File gitDir = new File(sLocalDirectory, ".git");
        if (!gitDir.exists()) return false;

        try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build();
             RevWalk revWalk = new RevWalk(repository)) {

            ObjectId headId = repository.resolve(Constants.HEAD);
            if (headId == null) return false;

            RevCommit currentCommit = revWalk.parseCommit(headId);

            // 1. Create a new root commit with the same tree snapshot and NO parent commits (depth = 1)
            CommitBuilder commitBuilder = new CommitBuilder();
            commitBuilder.setTreeId(currentCommit.getTree().getId());
            commitBuilder.setAuthor(currentCommit.getAuthorIdent());
            commitBuilder.setCommitter(currentCommit.getCommitterIdent());
            commitBuilder.setMessage(currentCommit.getFullMessage());

            ObjectInserter inserter = repository.newObjectInserter();
            ObjectId newRootCommitId = inserter.insert(commitBuilder);
            inserter.flush();

            // 2. Update current branch Ref to point to the new root commit
            String fullBranch = repository.getFullBranch();
            if (fullBranch != null) {
                RefUpdate refUpdate = repository.updateRef(fullBranch);
                refUpdate.setNewObjectId(newRootCommitId);
                refUpdate.setForceUpdate(true);
                refUpdate.update();
            }

            // 3. Mark as shallow by writing the commit ID to .git/shallow
            File shallowFile = new File(gitDir, "shallow");
            try (FileWriter writer = new FileWriter(shallowFile)) {
                writer.write(newRootCommitId.name() + "\n");
            }

            // 4. Clean up reflogs so old objects can be pruned
            File logsDir = new File(gitDir, "logs");
            if (logsDir.exists()) {
                FileUtils.delete(logsDir, FileUtils.RECURSIVE);
            }

            // 5. Run GC to prune old loose objects and packfiles
            try {
                org.eclipse.jgit.internal.storage.file.GC gc = new org.eclipse.jgit.internal.storage.file.GC((org.eclipse.jgit.internal.storage.file.FileRepository) repository);
                gc.setExpire(new Date(System.currentTimeMillis() + 1000L));
                gc.gc();
            } catch (Exception gcEx) {
                // Best effort GC
            }

            Log.d(TAG, "Successfully purged local git history to depth=1 for " + sLocalDirectory);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to purge local git repository history for " + sLocalDirectory, e);
            return false;
        }
    }

    public static void purgeAllLocalRepositoriesHistory(Context context) {
        ArrayList<RemoteGit> list = getRemoteGitList(context);
        for (RemoteGit g : list) {
            String localDir = getLocalGitDirectory(context, g.getUrl());
            purgeLocalGitRepositoryHistory(localDir);
        }
    }
}
