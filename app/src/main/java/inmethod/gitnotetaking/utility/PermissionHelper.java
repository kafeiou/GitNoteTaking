package inmethod.gitnotetaking.utility;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import inmethod.gitnotetaking.R;

public class PermissionHelper {

    private static final String PREFS_NAME = "permission_prefs";
    private static final String KEY_PREFIX_REQUESTED = "permission_requested_";
    private static final String KEY_PREFIX_DENIAL_COUNT = "permission_denial_count_";

    public static final int REQUEST_CODE_CAMERA = 1001;
    public static final int REQUEST_CODE_MEDIA = 1002;
    private static final int MAX_DISAGREE_COUNT = 1;

    public interface PermissionCallback {
        void onGranted();
        void onDenied();
    }

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasCameraPermission(Context context) {
        return hasPermission(context, Manifest.permission.CAMERA);
    }

    public static boolean hasMediaPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                   hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES);
        }
        return true;
    }

    public static void requestCamera(final Activity activity, final PermissionCallback callback) {
        if (hasCameraPermission(activity)) {
            resetDenialCount(activity, Manifest.permission.CAMERA);
            if (callback != null) callback.onGranted();
            return;
        }

        if (shouldDirectlyGuideToSettings(activity, Manifest.permission.CAMERA)) {
            showSettingsBottomSheet(activity, R.string.permission_camera_title, R.string.permission_camera_settings_msg);
            if (callback != null) callback.onDenied();
            return;
        }

        showRationaleBottomSheet(
                activity,
                R.string.permission_camera_title,
                R.string.permission_camera_rationale,
                () -> {
                    markPermissionRequested(activity, Manifest.permission.CAMERA);
                    ActivityCompat.requestPermissions(
                            activity,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CODE_CAMERA
                    );
                },
                () -> {
                    incrementDenialCount(activity, Manifest.permission.CAMERA);
                    if (callback != null) callback.onDenied();
                }
        );
    }

    public static void requestMedia(final Activity activity, final PermissionCallback callback) {
        String primaryPermission = getMediaPrimaryPermission();

        if (hasMediaPermission(activity)) {
            resetDenialCount(activity, primaryPermission);
            if (callback != null) callback.onGranted();
            return;
        }

        if (shouldDirectlyGuideToSettings(activity, primaryPermission)) {
            showSettingsBottomSheet(activity, R.string.permission_media_title, R.string.permission_media_settings_msg);
            if (callback != null) callback.onDenied();
            return;
        }

        showRationaleBottomSheet(
                activity,
                R.string.permission_media_title,
                R.string.permission_media_rationale,
                () -> {
                    markPermissionRequested(activity, primaryPermission);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ActivityCompat.requestPermissions(
                                activity,
                                new String[]{
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO,
                                        Manifest.permission.READ_MEDIA_AUDIO,
                                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                },
                                REQUEST_CODE_MEDIA
                        );
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(
                                activity,
                                new String[]{
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO,
                                        Manifest.permission.READ_MEDIA_AUDIO
                                },
                                REQUEST_CODE_MEDIA
                        );
                    }
                },
                () -> {
                    incrementDenialCount(activity, primaryPermission);
                    if (callback != null) callback.onDenied();
                }
        );
    }

    public static String getMediaPrimaryPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.INTERNET;
    }

    public static void showRationaleBottomSheet(
            final Activity activity,
            @StringRes int titleRes,
            @StringRes int msgRes,
            final Runnable onAgree,
            final Runnable onDisagree
    ) {
        if (activity == null || activity.isFinishing()) return;

        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_permission_bottom_sheet, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvPermissionTitle);
        TextView tvMsg = view.findViewById(R.id.tvPermissionMessage);
        Button btnAgree = view.findViewById(R.id.btnPermissionAgree);
        Button btnDisagree = view.findViewById(R.id.btnPermissionDisagree);

        tvTitle.setText(titleRes);
        tvMsg.setText(msgRes);
        btnAgree.setText(R.string.permission_btn_agree);
        btnDisagree.setText(R.string.permission_btn_disagree);

        btnAgree.setOnClickListener(v -> {
            dialog.dismiss();
            if (onAgree != null) onAgree.run();
        });

        btnDisagree.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDisagree != null) onDisagree.run();
        });

        dialog.show();
    }

    public static void showSettingsBottomSheet(
            final Activity activity,
            @StringRes int titleRes,
            @StringRes int msgRes
    ) {
        if (activity == null || activity.isFinishing()) return;

        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_permission_bottom_sheet, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvPermissionTitle);
        TextView tvMsg = view.findViewById(R.id.tvPermissionMessage);
        Button btnAgree = view.findViewById(R.id.btnPermissionAgree);
        Button btnDisagree = view.findViewById(R.id.btnPermissionDisagree);

        tvTitle.setText(titleRes);
        tvMsg.setText(msgRes);
        btnAgree.setText(R.string.permission_btn_settings);
        btnDisagree.setText(R.string.dialog_cancel);

        btnAgree.setOnClickListener(v -> {
            dialog.dismiss();
            openAppSettings(activity);
        });

        btnDisagree.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static void openAppSettings(Context context) {
        if (context == null) return;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void markPermissionRequested(Context context, String permission) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_PREFIX_REQUESTED + permission, true).apply();
    }

    public static int getDenialCount(Context context, String permission) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_PREFIX_DENIAL_COUNT + permission, 0);
    }

    public static void incrementDenialCount(Context context, String permission) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int current = sp.getInt(KEY_PREFIX_DENIAL_COUNT + permission, 0);
        sp.edit().putInt(KEY_PREFIX_DENIAL_COUNT + permission, current + 1).apply();
    }

    public static void resetDenialCount(Context context, String permission) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_PREFIX_DENIAL_COUNT + permission, 0).apply();
    }

    public static boolean isPermanentlyDenied(Activity activity, String permission) {
        SharedPreferences sp = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean wasRequested = sp.getBoolean(KEY_PREFIX_REQUESTED + permission, false);
        return wasRequested &&
               !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
               !hasPermission(activity, permission);
    }

    public static boolean shouldDirectlyGuideToSettings(Activity activity, String permission) {
        if (hasPermission(activity, permission)) {
            return false;
        }
        if (isPermanentlyDenied(activity, permission)) {
            return true;
        }
        return getDenialCount(activity, permission) >= MAX_DISAGREE_COUNT;
    }
}
