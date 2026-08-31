package inmethod.gitnotetaking.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
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

import java.io.ByteArrayOutputStream;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.res.Configuration;
import androidx.appcompat.app.AlertDialog;
import inmethod.gitnotetaking.R;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    public static final int PULL_RESULT_FAILED = 0;
    public static final int PULL_RESULT_UP_TO_DATE = 1;
    public static final int PULL_RESULT_UPDATED = 2;
    public static boolean bGitLock = false;


    public static boolean deleteLocalGitRepository(Context context, String sRemoteUrl) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        if (sLocalDirectory == null || sLocalDirectory.trim().isEmpty()) {
            return false;
        }
        File localDir = new File(sLocalDirectory);
        if (localDir.exists()) {
            try {
                if (checkLocalGitRepository(context, sRemoteUrl)) {
                    GitUtil aGitUtil = new GitUtil(sRemoteUrl, sLocalDirectory);
                    aGitUtil.removeLocalGitRepository();
                    aGitUtil.close();
                }
            } catch (Exception ee) {
                Log.w(TAG, "GitUtil remove failed, falling back to FileUtils: " + ee.getMessage());
            }
            try {
                if (localDir.exists()) {
                    FileUtils.delete(localDir, FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.SKIP_MISSING);
                }
                return true;
            } catch (Exception ee) {
                Log.e(TAG, "FileUtils delete failed: " + ee.getMessage(), ee);
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

    public static boolean autoCommitIfDirty(Context context, String sRemoteUrl) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        File gitDir = new File(sLocalDirectory, ".git");
        if (!gitDir.exists()) {
            return false;
        }
        try (Git git = Git.open(new File(sLocalDirectory))) {
            Status status = git.status().call();
            if (!status.isClean()) {
                Log.d(TAG, "Working tree is dirty, auto-committing before sync: uncommitted changes = " + status.getUncommittedChanges());
                return commit(context, sRemoteUrl, "Auto-saved locally before sync");
            }
        } catch (Exception e) {
            Log.e(TAG, "autoCommitIfDirty exception: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean deleteByRemoteUrl(Context context, String sRemoteUrl) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        boolean sReturn = aRemoteGitDAO.delete(sRemoteUrl);
        aRemoteGitDAO.close();
        return sReturn;
    }

    public static boolean backupToDownloads(Context context, String sRemoteUrl, String zipFileName) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        if (sLocalDirectory == null) return false;
        File sourceDir = new File(sLocalDirectory);
        if (!sourceDir.exists()) return false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;
                try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                     BufferedOutputStream bos = new BufferedOutputStream(os);
                     ZipOutputStream zos = new ZipOutputStream(bos)) {
                    zipDirectoryRecursive(sourceDir, sourceDir, zos);
                    zos.flush();
                }
                Log.d(TAG, "Backup successfully written to MediaStore Downloads: " + zipFileName);
                return true;
            } else {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();
                File destZip = new File(downloadDir, zipFileName);
                try (FileOutputStream fos = new FileOutputStream(destZip);
                     BufferedOutputStream bos = new BufferedOutputStream(fos);
                     ZipOutputStream zos = new ZipOutputStream(bos)) {
                    zipDirectoryRecursive(sourceDir, sourceDir, zos);
                    zos.flush();
                }
                Log.d(TAG, "Backup successfully written to public Downloads: " + destZip.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to backup repository to Downloads: " + sRemoteUrl, e);
            return false;
        }
    }

    public static void backup(Context context, String sRemoteUrl, String sBackupDestLocation) throws Exception {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        File sourceDir = new File(sLocalDirectory);
        if (!sourceDir.exists()) {
            throw new java.io.FileNotFoundException("Local directory does not exist: " + sLocalDirectory);
        }
        File destZip = new File(sBackupDestLocation);
        File parentDir = destZip.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(destZip);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            zipDirectoryRecursive(sourceDir, sourceDir, zos);
            zos.flush();
        }
    }

    public static void zipDirectoryRecursive(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
        if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipDirectoryRecursive(rootDir, file, zos);
                }
            }
        } else {
            String relativePath = rootDir.toURI().relativize(sourceFile.toURI()).getPath();
            ZipEntry zipEntry = new ZipEntry(relativePath);
            zipEntry.setTime(sourceFile.lastModified());
            zos.putNextEntry(zipEntry);
            try (FileInputStream fis = new FileInputStream(sourceFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = bis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
        }
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
        return pullWithResult(context, sRemoteUrl) != PULL_RESULT_FAILED;
    }

    public static int pullWithResult(Context context, String sRemoteUrl) {
        RemoteGitDAO aRemoteGitDAO = new RemoteGitDAO(context);
        RemoteGit aRemoteGit = aRemoteGitDAO.getByURL(sRemoteUrl);
        if (aRemoteGit == null) return PULL_RESULT_FAILED;
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
                return PULL_RESULT_FAILED;
            }
            Log.d(TAG, "Remote repository exists ? " + bIsRemoteRepositoryExist);
            if (bIsRemoteRepositoryExist) {
                Log.d(TAG, "try to pull remote repository , branch="+aRemoteGit.getRemoteName());
                ObjectId oldHead = null;
                try (Git git = Git.open(new File(sLocalDirectory))) {
                    oldHead = git.getRepository().resolve("HEAD");
                } catch (Exception ignored) {}

                try{
                  if (aGitUtil.pull(aRemoteGit.getRemoteName(), sUserName, sUserPassword)) {
                    Log.d(TAG, "pull finished!");
                    syncFileTimestampsAsync(context, sLocalDirectory, sRemoteUrl, sUserPassword);
                    aRemoteGit.setStatus(GIT_STATUS_SUCCESS);
                    aRemoteGitDAO.update(aRemoteGit);
                    setGitLock(false);

                    ObjectId newHead = null;
                    try (Git git = Git.open(new File(sLocalDirectory))) {
                        newHead = git.getRepository().resolve("HEAD");
                    } catch (Exception ignored) {}

                    boolean hasNewCommits = (oldHead != null && newHead != null && !oldHead.equals(newHead));
                    if (aGitUtil != null) aGitUtil.close();
                    return hasNewCommits ? PULL_RESULT_UPDATED : PULL_RESULT_UP_TO_DATE;
                  } else {
                      aGitUtil.reset(ResetCommand.ResetType.MIXED, PreferenceManager.getDefaultSharedPreferences(context).getString("GitRemoteName", "master"));
                      aRemoteGitDAO.update(aRemoteGit);
                      Log.d(TAG, "pull failed!");
                      setGitLock(false);
                      if (aGitUtil != null) aGitUtil.close();
                      return PULL_RESULT_FAILED;
                  }
                }catch(LockFailedException lockfail){
                    lockfail.printStackTrace();
                    aRemoteGitDAO.update(aRemoteGit);
                    Log.d(TAG, "pull failed!");
                    setGitLock(false);
                    FileUtility.deleteLockFile(aGitUtil);
                    if (aGitUtil != null) aGitUtil.close();
                    return PULL_RESULT_FAILED;
                }catch(JGitInternalException aJGitInternalException){
                        aRemoteGitDAO.update(aRemoteGit);
                        Log.d(TAG, "pull failed!");
                        setGitLock(false);
                        FileUtility.deleteLockFile(aGitUtil);
                        if (aGitUtil != null) aGitUtil.close();
                        return PULL_RESULT_FAILED;
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
                    return PULL_RESULT_FAILED;
                }
            }
            aRemoteGitDAO.update(aRemoteGit);
            setGitLock(false);
            if (aGitUtil != null) aGitUtil.close();
            if( aRemoteGit!=null )
            aRemoteGitDAO.close();
            return PULL_RESULT_FAILED;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return PULL_RESULT_FAILED;
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
                    syncFileTimestampsAsync(context, sLocalDirectory, sRemoteUrl, sUserPassword);
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
                    syncFileTimestampsAsync(context, sLocalDirectory, sRemoteUrl, sUserPassword);
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

    public static void syncFileTimestampsAsync(final Context context, final String sLocalDirectory, final String sRemoteUrl, final String token) {
        if (sLocalDirectory == null || sLocalDirectory.isEmpty()) return;
        // 1. Fast local sync with HEAD commit time (~2ms)
        syncFileTimestampsWithGit(sLocalDirectory);

        // 2. Background async sync via GitHub GraphQL API with Commit hash marker
        if (sRemoteUrl != null && sRemoteUrl.contains("github.com") && token != null && !token.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    syncFileTimestampsFromGitHubApi(sLocalDirectory, sRemoteUrl, token);
                }
            }).start();
        }
    }

    public static void syncFileTimestampsFromGitHubApi(String sLocalDirectory, String sRemoteUrl, String token) {
        if (sRemoteUrl == null || !sRemoteUrl.contains("github.com") || sLocalDirectory == null || token == null || token.isEmpty()) return;
        try {
            File gitDir = new File(sLocalDirectory, ".git");
            if (!gitDir.exists()) return;

            String currentHeadSha = "";
            try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
                ObjectId head = repo.resolve(Constants.HEAD);
                if (head != null) {
                    currentHeadSha = head.getName();
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error resolving HEAD commit for timestamp sync", ex);
            }

            if (!currentHeadSha.isEmpty()) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getAppContext());
                String lastSyncedCommit = sp.getString("SYNC_TIMESTAMP_COMMIT_" + sLocalDirectory, "");
                if (currentHeadSha.equals(lastSyncedCommit)) {
                    Log.d(TAG, "File timestamps already synced for commit " + currentHeadSha + " at " + sLocalDirectory);
                    return;
                }
            }

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

            // Batch files in chunks of 100 to minimize HTTP round-trips
            int chunkSize = 100;
            for (int i = 0; i < allFiles.size(); i += chunkSize) {
                List<File> chunk = allFiles.subList(i, Math.min(i + chunkSize, allFiles.size()));
                syncChunkViaGraphQL(owner, repoName, workingDir, chunk, token);
            }

            if (!currentHeadSha.isEmpty()) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MyApplication.getAppContext());
                sp.edit().putString("SYNC_TIMESTAMP_COMMIT_" + sLocalDirectory, currentHeadSha).apply();
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

    public static boolean isWorkingTreeDirty(Context context, String sRemoteUrl) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        if (sLocalDirectory == null) return false;
        try (Git git = Git.open(new File(sLocalDirectory))) {
            Status status = git.status().call();
            return status.hasUncommittedChanges();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean autoCommitIfDirtyWithMessage(Context context, String sRemoteUrl, String commitMessage) {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        if (sLocalDirectory == null) return false;
        try (Git git = Git.open(new File(sLocalDirectory))) {
            Status status = git.status().call();
            if (!status.hasUncommittedChanges()) {
                return false; // Nothing to commit
            }
            git.add().addFilepattern(".").call();
            String authorName = PreferenceManager.getDefaultSharedPreferences(context).getString("GitAuthorName", "root");
            String authorEmail = PreferenceManager.getDefaultSharedPreferences(context).getString("GitAuthorEmail", "root@your.email.com");
            git.commit()
               .setMessage(commitMessage)
               .setAuthor(authorName, authorEmail)
               .setCommitter(authorName, authorEmail)
               .call();
            Log.d(TAG, "Auto-committed changes successfully for: " + sRemoteUrl);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to auto-commit changes for: " + sRemoteUrl, e);
            return false;
        }
    }

    public static class StorageBreakdown {
        public long workingTreeBytes = 0;
        public long gitDirBytes = 0;
        public int fileCount = 0;

        public long getTotalBytes() {
            return workingTreeBytes + gitDirBytes;
        }
    }

    public static StorageBreakdown calculateRepositoryStorage(File rootDir) {
        StorageBreakdown breakdown = new StorageBreakdown();
        if (rootDir == null || !rootDir.exists()) return breakdown;
        calculateStorageRecursive(rootDir, breakdown, false);
        return breakdown;
    }

    private static void calculateStorageRecursive(File file, StorageBreakdown breakdown, boolean isInsideGitDir) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            boolean isGit = isInsideGitDir || file.getName().equals(".git");
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    calculateStorageRecursive(child, breakdown, isGit);
                }
            }
        } else {
            long length = file.length();
            if (isInsideGitDir) {
                breakdown.gitDirBytes += length;
            } else {
                breakdown.workingTreeBytes += length;
                breakdown.fileCount++;
            }
        }
    }

    public static String formatStorageSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return new java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void purgeAllLocalRepositoriesHistory(Context context) {
        ArrayList<RemoteGit> list = getRemoteGitList(context);
        for (RemoteGit g : list) {
            String localDir = getLocalGitDirectory(context, g.getUrl());
            purgeLocalGitRepositoryHistory(localDir);
        }
    }

    public static boolean isTemporaryFileName(String name) {
        if (name == null || name.isEmpty()) return false;
        String lower = name.toLowerCase();
        return name.startsWith("~") ||
               name.endsWith("~") ||
               lower.endsWith(".tmp") ||
               lower.endsWith(".temp") ||
               lower.endsWith(".swp") ||
               lower.endsWith(".swo") ||
               name.equals(".DS_Store") ||
               name.equalsIgnoreCase("Thumbs.db") ||
               name.equalsIgnoreCase("desktop.ini");
    }

    public static boolean ensureDefaultGitIgnore(String sLocalDirectory) {
        if (sLocalDirectory == null || sLocalDirectory.isEmpty()) return false;
        File ignoreFile = new File(sLocalDirectory, ".gitignore");
        List<String> requiredRules = new ArrayList<>();
        requiredRules.add("~*");
        requiredRules.add("*~");
        requiredRules.add("~$*");
        requiredRules.add("*.tmp");
        requiredRules.add("*.temp");
        requiredRules.add("*.swp");
        requiredRules.add("*.swo");
        requiredRules.add(".DS_Store");
        requiredRules.add("Thumbs.db");
        requiredRules.add("desktop.ini");

        List<String> existingLines = new ArrayList<>();
        if (ignoreFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ignoreFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    existingLines.add(line.trim());
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to read existing .gitignore", e);
            }
        }

        List<String> rulesToAdd = new ArrayList<>();
        for (String rule : requiredRules) {
            if (!existingLines.contains(rule)) {
                rulesToAdd.add(rule);
            }
        }

        if (rulesToAdd.isEmpty() && ignoreFile.exists()) {
            return false; // Already up to date
        }

        try (FileWriter writer = new FileWriter(ignoreFile, true)) {
            if (ignoreFile.length() > 0 && !existingLines.isEmpty()) {
                writer.write("\n");
            }
            if (existingLines.isEmpty()) {
                writer.write("# Temporary & System Files\n");
            }
            for (String r : rulesToAdd) {
                writer.write(r + "\n");
            }
            writer.flush();
            Log.d(TAG, "Updated .gitignore with " + rulesToAdd.size() + " new rules in " + sLocalDirectory);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write .gitignore", e);
            return false;
        }
    }

    public static int cleanTemporaryFilesAndSync(Context context, String sRemoteUrl) throws Exception {
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        if (sLocalDirectory == null) return 0;
        File localDir = new File(sLocalDirectory);
        if (!localDir.exists()) return 0;

        // 1. Guardrail: If working tree has uncommitted user edits, auto-commit first to protect user notes
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        autoCommitIfDirtyWithMessage(context, sRemoteUrl, "Auto-saved before excluding temp files: " + timestamp);

        // 2. Sync: Pull latest changes from remote first to prevent non-fast-forward push rejections
        if (MyApplication.isNetworkConnected() && sRemoteUrl != null && !sRemoteUrl.contains("local")) {
            try {
                pull(context, sRemoteUrl);
            } catch (Exception e) {
                Log.w(TAG, "Pull before excluding temp files encountered issue (proceeding with local cleanup): " + sRemoteUrl, e);
            }
        }

        int removedCount = 0;
        boolean gitignoreUpdated = ensureDefaultGitIgnore(sLocalDirectory);

        try (Git git = Git.open(localDir)) {
            Repository repository = git.getRepository();
            ObjectId headId = repository.resolve(Constants.HEAD);
            
            // 3. Scan and untrack temporary files already tracked in HEAD
            if (headId != null) {
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit commit = revWalk.parseCommit(headId);
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(commit.getTree());
                        treeWalk.setRecursive(true);
                        while (treeWalk.next()) {
                            String pathString = treeWalk.getPathString();
                            String fileName = pathString.substring(pathString.lastIndexOf('/') + 1);
                            if (isTemporaryFileName(fileName)) {
                                Log.d(TAG, "Removing tracked temp file: " + pathString);
                                git.rm().addFilepattern(pathString).call();
                                removedCount++;
                            }
                        }
                    }
                }
            }

            // 4. Also delete any untracked temporary files from disk
            cleanWorkingTreeTempFilesRecursive(localDir);

            // 5. If files were removed or .gitignore was updated, commit and push
            Status status = git.status().call();
            if (status.hasUncommittedChanges() || removedCount > 0 || gitignoreUpdated) {
                git.add().addFilepattern(".gitignore").call();
                git.add().addFilepattern(".").call();
                String authorName = PreferenceManager.getDefaultSharedPreferences(context).getString("GitAuthorName", "root");
                String authorEmail = PreferenceManager.getDefaultSharedPreferences(context).getString("GitAuthorEmail", "root@your.email.com");
                git.commit()
                   .setMessage("chore: exclude temporary files and update .gitignore")
                   .setAuthor(authorName, authorEmail)
                   .setCommitter(authorName, authorEmail)
                   .call();
                Log.d(TAG, "Committed exclusion of " + removedCount + " temp files in " + sRemoteUrl);

                if (MyApplication.isNetworkConnected() && sRemoteUrl != null && !sRemoteUrl.contains("local")) {
                    push(context, sRemoteUrl);
                }
            }
        }

        return removedCount;
    }

    private static void cleanWorkingTreeTempFilesRecursive(File current) {
        if (current == null || !current.exists()) return;
        if (current.isDirectory()) {
            if (current.getName().equals(".git")) return;
            File[] files = current.listFiles();
            if (files != null) {
                for (File f : files) {
                    cleanWorkingTreeTempFilesRecursive(f);
                }
            }
        } else {
            if (isTemporaryFileName(current.getName())) {
                try {
                    current.delete();
                } catch (Exception e) {
                    // Best effort
                }
            }
        }
    }

    /**
     * 尋找給定路徑所屬之 Git 根目錄（包含 .git 的目錄）
     */
    public static File findGitRootDir(File fileOrDir) {
        if (fileOrDir == null) return null;
        File current = fileOrDir.isDirectory() ? fileOrDir : fileOrDir.getParentFile();
        while (current != null) {
            File gitDir = new File(current, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    /**
     * 從指定 Commit (例如 HEAD, HEAD~1) 讀取目標檔案內容
     */
    public static String getCommitFileContent(File repoRoot, String commitRef, File targetFile) {
        if (repoRoot == null || targetFile == null || commitRef == null) return null;
        File gitDir = new File(repoRoot, ".git");
        if (!gitDir.exists()) return null;

        try {
            String repoCanonical = repoRoot.getCanonicalPath();
            String targetCanonical = targetFile.getCanonicalPath();
            String relativePath = "";
            if (targetCanonical.startsWith(repoCanonical)) {
                relativePath = targetCanonical.substring(repoCanonical.length());
                if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                    relativePath = relativePath.substring(1);
                }
            } else {
                relativePath = targetFile.getName();
            }
            relativePath = relativePath.replace('\\', '/');

            try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
                ObjectId commitId = repo.resolve(commitRef);
                if (commitId == null) return "";

                try (RevWalk revWalk = new RevWalk(repo)) {
                    RevCommit commit = revWalk.parseCommit(commitId);
                    try (TreeWalk treeWalk = TreeWalk.forPath(repo, relativePath, commit.getTree())) {
                        if (treeWalk != null) {
                            ObjectId blobId = treeWalk.getObjectId(0);
                            byte[] bytes = repo.open(blobId).getBytes();
                            return new String(bytes, StandardCharsets.UTF_8);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getCommitFileContent failed for " + targetFile + " at " + commitRef, e);
        }
        return null;
    }

    /**
     * 從 Git 倉庫讀取 HEAD 提交版本之指定檔案內容
     */
    public static String getHeadContent(Context context, String sRemoteUrl, String relativeFilePath) {
        if (context == null || sRemoteUrl == null || relativeFilePath == null) return null;
        String sLocalDirectory = getLocalGitDirectory(context, sRemoteUrl);
        if (sLocalDirectory == null) return null;
        File gitDir = new File(sLocalDirectory, ".git");
        if (!gitDir.exists()) return null;

        String normalizedPath = relativeFilePath.replace('\\', '/');
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
            ObjectId head = repo.resolve(Constants.HEAD);
            if (head == null) return "";

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(head);
                try (TreeWalk treeWalk = TreeWalk.forPath(repo, normalizedPath, commit.getTree())) {
                    if (treeWalk != null) {
                        ObjectId blobId = treeWalk.getObjectId(0);
                        byte[] bytes = repo.open(blobId).getBytes();
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getHeadContent failed for " + relativeFilePath, e);
        }
        return null;
    }

    /**
     * 比對兩段字串產生 Unified Diff 文本
     */
    public static String computeDiff(String oldContent, String newContent, String oldLabel, String newLabel) {
        try {
            String oldStr = (oldContent != null ? oldContent : "");
            String newStr = (newContent != null ? newContent : "");
            if (oldStr.equals(newStr)) {
                return "";
            }

            RawText a = new RawText(oldStr.getBytes(StandardCharsets.UTF_8));
            RawText b = new RawText(newStr.getBytes(StandardCharsets.UTF_8));
            EditList edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
                    .diff(RawTextComparator.DEFAULT, a, b);

            if (edits == null || edits.isEmpty()) {
                return "";
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DiffFormatter formatter = new DiffFormatter(out)) {
                formatter.format(edits, a, b);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            Log.e(TAG, "computeDiff error", e);
            return null;
        }
    }

    public static class FileDiffResult {
        public String diffText;
        public String subtitle;
        public boolean hasChanges;

        public FileDiffResult(String diffText, String subtitle, boolean hasChanges) {
            this.diffText = diffText;
            this.subtitle = subtitle;
            this.hasChanges = hasChanges;
        }
    }

    /**
     * 雙軌智慧差異比對：
     * 1. 優先比對「HEAD vs 當前內容 (未提交修改)」
     * 2. 若無未提交修改，自動比對「HEAD~1 vs HEAD (最新提交變更)」
     */
    public static FileDiffResult getSmartFileDiff(Context context, File targetFile, String currentContent) {
        if (targetFile == null) {
            return new FileDiffResult("", "", false);
        }
        File repoRoot = findGitRootDir(targetFile);
        if (repoRoot == null) {
            return new FileDiffResult("", "", false);
        }

        String fileName = targetFile.getName();
        String headContent = getCommitFileContent(repoRoot, Constants.HEAD, targetFile);
        String textToCompare = (currentContent != null) ? currentContent : "";

        // 1. 軌道一：比對 HEAD 與當前內容
        String diffVsHead = computeDiff(headContent, textToCompare, "a/" + fileName + " (HEAD)", "b/" + fileName + " (Current)");
        if (diffVsHead != null && !diffVsHead.trim().isEmpty()) {
            String title = (context != null) ? context.getString(R.string.diff_status_uncommitted) : "Uncommitted Changes";
            return new FileDiffResult(diffVsHead, title, true);
        }

        // 2. 軌道二：若無未提交修改，比對 HEAD~1 與 HEAD (最新一次 Commit 的修改)
        String parentContent = getCommitFileContent(repoRoot, "HEAD~1", targetFile);
        if (parentContent != null && headContent != null) {
            String diffVsParent = computeDiff(parentContent, headContent, "a/" + fileName + " (HEAD~1)", "b/" + fileName + " (HEAD)");
            if (diffVsParent != null && !diffVsParent.trim().isEmpty()) {
                String title = (context != null) ? context.getString(R.string.diff_status_latest_commit) : "Latest Commit Changes";
                return new FileDiffResult(diffVsParent, title, true);
            }
        }

        // 3. 兩者皆無變更
        String noChangeMsg = (context != null) ? context.getString(R.string.diff_no_changes) : "No changes found.";
        return new FileDiffResult("", noChangeMsg, false);
    }

    /**
     * 生成 GitHub 風格之純 HTML/CSS 視覺化差異對照表
     */
    public static String generateGitHubDiffHtml(Context context, String fileName, String subtitle, String diffText, boolean isDark) {
        if (diffText == null || diffText.trim().isEmpty()) {
            return "";
        }

        int addedCount = 0;
        int deletedCount = 0;
        String[] rawLines = diffText.split("\n");
        for (String line : rawLines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                addedCount++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                deletedCount++;
            }
        }

        String bgColor = isDark ? "#121212" : "#ffffff";
        String textColor = isDark ? "#c9d1d9" : "#24292f";
        String borderColor = isDark ? "#30363d" : "#d0d7de";
        String hunkBg = isDark ? "#1e293b" : "#ddf4ff";
        String hunkText = isDark ? "#7dd3fc" : "#0969da";
        String lineNumBg = isDark ? "#161b22" : "#f6f8fa";
        String lineNumText = isDark ? "#768390" : "#8c959f";
        String delBg = isDark ? "#3d1b20" : "#ffebe9";
        String delText = isDark ? "#ff7b72" : "#24292f";
        String delSign = isDark ? "#f85149" : "#cf222e";
        String delWordBg = isDark ? "#6e1d24" : "#ffc1be";
        String insBg = isDark ? "#143821" : "#e6ffec";
        String insText = isDark ? "#7ee787" : "#24292f";
        String insSign = isDark ? "#3fb950" : "#1a7f37";
        String insWordBg = isDark ? "#1f6f3b" : "#abf2bc";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes'>");
        sb.append("<style>");
        sb.append("* { box-sizing: border-box; }");
        sb.append("body { margin: 0; padding: 10px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 12px; line-height: 1.45; background-color: ").append(bgColor).append("; color: ").append(textColor).append("; }");
        sb.append(".badge-container { margin-bottom: 10px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }");
        sb.append(".subtitle { font-size: 13px; font-weight: bold; color: #58a6ff; margin-bottom: 4px; }");
        sb.append(".stats { font-size: 12px; color: #8b949e; }");
        sb.append(".diff-table { width: 100%; border-collapse: collapse; border: 1px solid ").append(borderColor).append("; border-radius: 6px; overflow: hidden; margin-top: 6px; }");
        sb.append(".hunk-header { background-color: ").append(hunkBg).append("; color: ").append(hunkText).append("; font-weight: bold; padding: 6px 10px; font-size: 11px; border-top: 1px solid ").append(borderColor).append("; border-bottom: 1px solid ").append(borderColor).append("; }");
        sb.append(".diff-row { font-family: inherit; }");
        sb.append(".diff-row.del { background-color: ").append(delBg).append("; color: ").append(delText).append("; }");
        sb.append(".diff-row.ins { background-color: ").append(insBg).append("; color: ").append(insText).append("; }");
        sb.append(".diff-row.cntx { background-color: transparent; }");
        sb.append(".num { width: 34px; min-width: 34px; max-width: 34px; padding: 2px 4px; text-align: right; user-select: none; font-size: 10px; color: ").append(lineNumText).append("; background-color: ").append(lineNumBg).append("; border-right: 1px solid ").append(borderColor).append("; vertical-align: top; }");
        sb.append(".sign { width: 16px; min-width: 16px; max-width: 16px; text-align: center; padding: 2px 0; font-weight: bold; user-select: none; vertical-align: top; }");
        sb.append(".diff-row.del .sign { color: ").append(delSign).append("; }");
        sb.append(".diff-row.ins .sign { color: ").append(insSign).append("; }");
        sb.append(".code { padding: 2px 6px; white-space: pre-wrap; word-break: break-all; vertical-align: top; }");
        sb.append(".w-del { background-color: ").append(delWordBg).append("; border-radius: 2px; padding: 0 2px; }");
        sb.append(".w-ins { background-color: ").append(insWordBg).append("; border-radius: 2px; padding: 0 2px; font-weight: bold; }");
        sb.append("</style></head><body>");

        // Top badges
        sb.append("<div class='badge-container'>");
        if (context != null) {
            String statsStr = context.getString(R.string.diff_stats_summary, addedCount, deletedCount);
            sb.append("<div class='stats'>").append(escapeHtml(statsStr)).append("</div>");
        }
        sb.append("</div>");

        sb.append("<table class='diff-table'>");

        java.util.regex.Pattern hunkPattern = java.util.regex.Pattern.compile("@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@(.*)");
        int curOldLine = 1;
        int curNewLine = 1;

        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i];
            if (line.startsWith("diff ") || line.startsWith("index ") || line.startsWith("---") || line.startsWith("+++")) {
                continue;
            }

            java.util.regex.Matcher m = hunkPattern.matcher(line);
            if (m.matches()) {
                try {
                    curOldLine = Integer.parseInt(m.group(1));
                    curNewLine = Integer.parseInt(m.group(2));
                } catch (Exception ignored) {}

                String sectionLabel = (context != null) ? context.getString(R.string.diff_line_near, curNewLine > 0 ? curNewLine : curOldLine) : ("📍 Near line " + (curNewLine > 0 ? curNewLine : curOldLine));
                sb.append("<tr><td colspan='4' class='hunk-header'>").append(escapeHtml(sectionLabel)).append("</td></tr>");
            } else if (line.startsWith("-")) {
                String delContent = line.substring(1);
                // Check if next line is an addition (replacement) for word-level highlighting
                if (i + 1 < rawLines.length && rawLines[i + 1].startsWith("+") && !rawLines[i + 1].startsWith("+++")) {
                    String insContent = rawLines[i + 1].substring(1);
                    String[] highlighted = computeWordLevelDiff(delContent, insContent);
                    sb.append("<tr class='diff-row del'><td class='num'>").append(curOldLine++).append("</td><td class='num'></td><td class='sign'>-</td><td class='code'>").append(highlighted[0]).append("</td></tr>");
                    sb.append("<tr class='diff-row ins'><td class='num'></td><td class='num'>").append(curNewLine++).append("</td><td class='sign'>+</td><td class='code'>").append(highlighted[1]).append("</td></tr>");
                    i++; // Skip the next line since it was processed together
                } else {
                    sb.append("<tr class='diff-row del'><td class='num'>").append(curOldLine++).append("</td><td class='num'></td><td class='sign'>-</td><td class='code'>").append(escapeHtml(delContent)).append("</td></tr>");
                }
            } else if (line.startsWith("+")) {
                String insContent = line.substring(1);
                sb.append("<tr class='diff-row ins'><td class='num'></td><td class='num'>").append(curNewLine++).append("</td><td class='sign'>+</td><td class='code'>").append(escapeHtml(insContent)).append("</td></tr>");
            } else {
                String cntxContent = line.startsWith(" ") ? line.substring(1) : line;
                sb.append("<tr class='diff-row cntx'><td class='num'>").append(curOldLine++).append("</td><td class='num'>").append(curNewLine++).append("</td><td class='sign'> </td><td class='code'>").append(escapeHtml(cntxContent)).append("</td></tr>");
            }
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    public static String[] computeWordLevelDiff(String oldStr, String newStr) {
        if (oldStr == null) oldStr = "";
        if (newStr == null) newStr = "";

        int prefixLen = 0;
        int minLen = Math.min(oldStr.length(), newStr.length());
        while (prefixLen < minLen && oldStr.charAt(prefixLen) == newStr.charAt(prefixLen)) {
            prefixLen++;
        }

        int suffixLen = 0;
        while (suffixLen < (minLen - prefixLen) &&
                oldStr.charAt(oldStr.length() - 1 - suffixLen) == newStr.charAt(newStr.length() - 1 - suffixLen)) {
            suffixLen++;
        }

        String prefix = escapeHtml(oldStr.substring(0, prefixLen));
        String suffix = escapeHtml(oldStr.substring(oldStr.length() - suffixLen));

        String oldMid = escapeHtml(oldStr.substring(prefixLen, oldStr.length() - suffixLen));
        String newMid = escapeHtml(newStr.substring(prefixLen, newStr.length() - suffixLen));

        String resOld = prefix + (oldMid.isEmpty() ? "" : "<span class='w-del'>" + oldMid + "</span>") + suffix;
        String resNew = prefix + (newMid.isEmpty() ? "" : "<span class='w-ins'>" + newMid + "</span>") + suffix;

        return new String[]{resOld, resNew};
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 顯示 GitHub 風格視覺化差異表 (100% 離線純 HTML/CSS) 對話框
     */
    public static void showDiffDialog(Activity activity, String fileName, String subtitle, String diffText) {
        if (activity == null || activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(fileName + " (" + activity.getString(R.string.diff_dialog_title) + ")");

        if (diffText == null || diffText.trim().isEmpty()) {
            builder.setMessage(subtitle != null && !subtitle.isEmpty() ? subtitle : activity.getString(R.string.diff_no_changes));
            builder.setPositiveButton(R.string.dialog_ok, null);
            builder.show();
            return;
        }

        boolean isDark = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        String htmlContent = generateGitHubDiffHtml(activity, fileName, subtitle, diffText, isDark);

        WebView webView = new WebView(activity);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportZoom(true);
        webView.setBackgroundColor(isDark ? Color.parseColor("#121212") : Color.WHITE);

        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        int targetHeight = Math.max((int) (screenHeight * 0.65), (int) (350 * activity.getResources().getDisplayMetrics().density));
        android.widget.FrameLayout container = new android.widget.FrameLayout(activity);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                targetHeight
        );
        webView.setLayoutParams(lp);
        container.addView(webView);

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null);

        builder.setView(container);
        builder.setPositiveButton(R.string.dialog_ok, null);
        builder.show();
    }

    public static void showDiffDialog(Activity activity, String fileName, String diffText) {
        showDiffDialog(activity, fileName, null, diffText);
    }
}
