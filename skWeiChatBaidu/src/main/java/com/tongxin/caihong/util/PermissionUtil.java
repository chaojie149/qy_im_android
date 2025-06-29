package com.tongxin.caihong.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;


import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.view.PermissionExplainDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2017/12/6 0006.
 */

@SuppressWarnings("unused")
public class PermissionUtil {
    //视频权限
    public static String[] getRecordVideoPermissions() {
        return new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }
//录音权限
    public static String[] getRecordAudioPermissions() {
        return new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }
//储存权限
    public static String[] getStoragePermissions() {
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }
//相机权限
    public static String[] getCameraPermissions() {
        //2022.12.23 修改相机权限
        return new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }
//读取联系人
    public static String[] getReadContactsPermissions() {
        return new String[]{Manifest.permission.READ_CONTACTS};
    }
//定位权限
    public static String[] getLocationPermissions() {
        return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION};
    }

    /**
     * 检查是否拥有权限，如果没有就自动请求并返回false，
     * 重点在于，如果已经有权限的情况是不会执行onGrant的，所以onGrant只能用于得到权限后重试，
     *
     * @return 已经有权限就返回true以便直接运行后续代码，没有权限就返回false以便阻止后续代码执行，
     */
    public static boolean checkAndAutoRequestPermission(@NonNull AppCompatActivity activity, int requestCode, Runnable onGrant, Runnable onDenied, @NonNull String... permissions) {
        if (checkSelfPermissions(activity, permissions)) {
            return true;
        }
        autoRequestPermission(activity, activity.getLifecycle(), requestCode, onGrant, onDenied, permissions);
        return false;
    }

    /**
     * 自动请求权限，
     * 封装请求以及判断请求是否完成，完成后判断是否已经授权，并执行相应回调，
     * 重点在于，如果已经有权限的情况是会执行onGrant的，所以onGrant要包含需要权限的代码，不能是重试代码，
     */
    public static void autoRequestPermission(@NonNull AppCompatActivity activity, int requestCode, Runnable onGrant, Runnable onDenied, @NonNull String... permissions) {
        autoRequestPermission(activity, activity.getLifecycle(), requestCode, onGrant, onDenied, permissions);
    }
    /**
     * 申请权限返回
     */
    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults,
                                                  @NonNull OnRequestPermissionsResultCallbacks callBack) {
        List<String> granted = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm);
            } else {
                denied.add(perm);
            }
        }
        if (!granted.isEmpty()) {
            callBack.onPermissionsGranted(requestCode, granted, denied.isEmpty());
        }
        if (!denied.isEmpty()) {
            callBack.onPermissionsDenied(requestCode, denied, granted.isEmpty());
        }
    }
    private static void autoRequestPermission(
            @NonNull Activity activity,
            @NonNull Lifecycle lifecycle,
            int requestCode,
            @Nullable Runnable onGrant,
            @Nullable Runnable onDenied,
            @NonNull String... permissions) {
        List<String> deniedPermissions = getDeniedPermissions(activity, permissions);
        if (deniedPermissions.isEmpty()) {
            if (onGrant != null) {
                onGrant.run();
            }
            return;
        }

        // 如果可以监听权限请求结果，那就可以无脑发起请求，被拒绝或者被自动拒绝都会收到回调，再弹解释对话框，
        // 如果无法监听回调的话就只能通过pause/resume生命周期判断权限请求结束，
        // 但如果是不再提示，就不会有弹窗，无法被知道是否请求完成，
        // 因此需要判断是否不需要提示，就表示已经被指定不再提示，但第一次请求也会被判定成不需要提示，无法区分，
        // 因此通过sp保存权限请求次数，如果没请求过才直接弹权限请求，
        // 但这里无法判断是否在系统设置里被手动设置成“询问”，这时候sp中记录的请求次数不为0，但其实可以直接请求，
        // FIXME: 2022/2/24 可能存在的问题，就是首次通过这个工具类请求权限之前就被设置成拒绝且不再询问，
        //  那么这里第一次将不会请求权限也不会弹窗也不会回调，

        // true表示可以监听权限请求结果，
        boolean listen = activity instanceof PermissionListenerWrapper;
        // 是否第一次请求，第一次可以先请求拒绝了再弹提示说明，否则就是先弹提示同意了再请求，
        boolean isFirst = isFirstRequest(activity, permissions);
        if (listen) {
            ((PermissionListenerWrapper) activity).setPermissionListener((requestCode1, permissions1, grantResults) -> {
                if (requestCode1 != requestCode) {
                    // 不是这个请求，
                    return false;
                }
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        // 有权限没有通过，
                        showPermissionExplainDialog(activity, lifecycle, requestCode, onGrant, onDenied, permissions, null);
                        return true;
                    }
                }
                // 所有权限都成功获得，
                if (onGrant != null) {
                    onGrant.run();
                }
                return true;
            });
            PermissionUtil.finalRequestPermissions(activity, requestCode, permissions);
        } else if (isFirst) {
            NextResumeObserver.bind(lifecycle, () -> {
                if (checkSelfPermissions(activity, permissions)) {
                    if (onGrant != null) {
                        onGrant.run();
                    }
                } else {
                    showPermissionExplainDialog(activity, lifecycle, requestCode, onGrant, onDenied, permissions, null);
                }
            });
            PermissionUtil.finalRequestPermissions(activity, requestCode, permissions);
        } else {
            showPermissionExplainDialog(activity, lifecycle, requestCode, onGrant, onDenied, permissions, null);
        }
        return;
    }

    private static void showPermissionExplainDialog(
            @NonNull Activity activity, @NonNull Lifecycle lifecycle, int requestCode,
            @Nullable Runnable onGrant, @Nullable Runnable onDenied,
            @NonNull String[] permissions, @Nullable String tipString) {
        List<String> deniedPermissions = getDeniedPermissions(activity, permissions);
        if (deniedPermissions.isEmpty()) {
            if (onGrant != null) {
                onGrant.run();
            }
            return;
        }
        // 是否第一次请求，第一次可以先请求拒绝了再弹提示说明，否则就是先弹提示同意了再请求，
        boolean isFirst = isFirstRequest(activity, permissions);
        // 已经设置不再询问，直接请求打开设置页去手动设置，
        boolean goSettings = !isFirst && deniedRequestPermissionsAgain(activity, permissions);
        PermissionExplainDialog tip = new PermissionExplainDialog(activity, goSettings);
        tip.setPermissions(deniedPermissions.toArray(new String[0]));
        if (!TextUtils.isEmpty(tipString)) {
            tip.setTipString(tipString);
        }
        tip.setOnCancelListener(onDenied);
        tip.setOnConfirmListener(() -> {
            NextResumeObserver.bind(lifecycle, () -> {
                if (checkSelfPermissions(activity, permissions)) {
                    if (onGrant != null) {
                        onGrant.run();
                    }
                } else {
                    if (onDenied != null) {
                        onDenied.run();
                    }
                }
            });
            if (goSettings) {
                startApplicationDetailsSettings(activity, 0);
            } else {
                PermissionUtil.finalRequestPermissions(activity, requestCode, permissions);
            }
        });
        tip.show();
    }

    public static boolean checkSelfPermissions(@NonNull Context ctx, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 安卓10以上保存媒体文件不需要权限，
     * 自己创建的文件自己拥有权限，
     * https://developer.android.google.cn/training/data-storage/shared/media?hl=zh-cn#app-attribution
     */
    public static boolean needMediaPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    /**
     * 获取被拒绝的权限
     */
    @NonNull
    public static List<String> getDeniedPermissions(@NonNull Activity activity, @NonNull String... permissions) {
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }

    /**
     * 是否拒绝了再次申请权限的请求（选择了不再询问 || 部分机型默认为不在询问）
     */
    public static boolean deniedRequestPermissionsAgain(@NonNull Activity activity, @NonNull String... permissions) {
        for (String permission : permissions) {
            /**
             * 注：调用该方法的前提是应用已申请过该权限，如应用未申请就调用此方法，返回false
             * 1.已请求过该权限且用户拒绝了请求,返回true
             * 2.用于拒绝了请求，且选择了不再询问,返回false
             * 3.设备规范禁止应用具有该权限，返回false
             */
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    public static void startApplicationDetailsSettings(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 申请权限<br/>
     *
     * @return 是否已经获取权限
     */
    public static boolean requestPermissions(Activity activity, int requestCode, String... permissions) {
        if (!checkSelfPermissions(activity, permissions)) {// 权限不全
            List<String> deniedPermissions = getDeniedPermissions(activity, permissions);
            if (deniedPermissions != null) {// 申请权限
                try {
                    finalRequestPermissions(activity, requestCode, deniedPermissions.toArray(new String[0]));
                } catch (Exception e) {
                    // 部分vivo手机系统内部崩溃，NullPointerException:Attempt to invoke virtual method 'boolean java.lang.Boolean.booleanValue()' on a null object reference
                    e.printStackTrace();
                }
            }
            return false;
        }
        return true;
    }

    private static void finalRequestPermissions(Activity activity, int requestCode, String[] permissions) {
        finalRequestPermissions(activity, null, requestCode, permissions);
    }

    private static void finalRequestPermissions(Fragment fragment, int requestCode, String[] permissions) {
        finalRequestPermissions(fragment.requireActivity(), fragment, requestCode, permissions);
    }

    private static void finalRequestPermissions(@NonNull Activity activity, @Nullable Fragment fragment, int requestCode, String[] permissions) {
        for (String permission : permissions) {
            count(activity, permission);
        }
        if (fragment != null) {
            fragment.requestPermissions(permissions, requestCode);
        } else {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    /**
     * 计数，请求该权限的次数加一，
     *
     * @param scene 可以是权限，也可以是任意字符串代表特定需要权限的场景，
     */
    private static void count(Context ctx, String scene) {
        int c = getNumberOfRequests(ctx, scene);
        getSp(ctx).edit().putInt(scene, c + 1).apply();
    }

    /**
     * 获取请求过该权限的次数，
     *
     * @param scene 可以是权限，也可以是任意字符串代表特定需要权限的场景，
     */
    private static int getNumberOfRequests(Context ctx, String scene) {
        return getSp(ctx).getInt(scene, 0);
    }

    /**
     * 判断是否第一次请求这些权限，
     * 有一个权限是第一次那个整体就是第一次，
     */
    public static boolean isFirstRequest(Context ctx, String[] permissions) {
        for (String permission : permissions) {
            if (getNumberOfRequests(ctx, permission) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 只第一次请求权限，
     * 针对一些不那么需要权限的，避免被指责反复请求不必要权限，
     *
     * @param scene     不同场景是分开算的第一次，
     * @param tipString 为空就提示默认内容，
     */
    public static void requestPermissionFirstTime(
            @NonNull AppCompatActivity activity,
            int requestCode,
            @Nullable Runnable onGrant,
            @Nullable Runnable onDenied,
            @NonNull String[] permissions,
            @NonNull String scene,
            @NonNull String tipString) {
        if (PermissionUtil.getNumberOfRequests(activity, scene) == 0) {
            PermissionUtil.count(activity, scene);
            if (PermissionUtil.isFirstRequest(activity, permissions)) {
                showPermissionExplainDialog(activity, activity.getLifecycle(), requestCode, onGrant, onDenied, permissions, tipString);
            }
        }
    }

    private static SharedPreferences getSp(Context ctx) {
        return ctx.getSharedPreferences("PermissionUtil", Context.MODE_PRIVATE);
    }

    public static List<String> getPermissionExplainList(Context ctx, String... permissions) {
        Set<String> explainList = new LinkedHashSet<>();
        for (String permission : permissions) {
            switch (permission) {
                case Manifest.permission.CAMERA:
                    explainList.add(ctx.getString(R.string.tip_permission_name_camera));
                    break;
                case Manifest.permission.RECORD_AUDIO:
                    explainList.add(ctx.getString(R.string.tip_permission_name_voice));
                    break;
                case Manifest.permission.READ_PHONE_STATE:
                    explainList.add(ctx.getString(R.string.tip_permission_name_phone));
                    break;
                case Manifest.permission.READ_CONTACTS:
                    explainList.add(ctx.getString(R.string.tip_permission_name_contacts_read));
                    break;
                case Manifest.permission.WRITE_CONTACTS:
                    explainList.add(ctx.getString(R.string.tip_permission_name_contacts_write));
                    break;
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    explainList.add(ctx.getString(R.string.tip_permission_name_storage));
                    break;
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                case Manifest.permission.ACCESS_FINE_LOCATION:
                    explainList.add(ctx.getString(R.string.tip_permission_name_location));
                    break;
                default:
                    Reporter.post("权限说明没定义, " + permission);
            }
        }
        return new ArrayList<>(explainList);
    }

    public static boolean checkInstallPermission(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true;
        }
        return ctx.getPackageManager().canRequestPackageInstalls();
    }

    /**
     * 自动请求权限并安装apk,
     */
    @UiThread
    public static void autoInstallApkFile(
            @NonNull Context ctx,
            @NonNull Lifecycle lifecycle,
            @NonNull File apkFile) {
        if (checkInstallPermission(ctx)) {
            installApkFile(ctx, lifecycle, apkFile);
            return;
        }
        // 现在是直接调用安装就会弹出请求，但记得以前直接安装会抛异常，所以这里catch一下，
        try {
            installApkFile(ctx, lifecycle, apkFile);
        } catch (Exception e) {
            ToastUtil.showToast(ctx, R.string.string_install_unknow_apk_note);
            NextResumeObserver.bind(lifecycle, () -> {
                if (checkInstallPermission(ctx)) {
                    installApkFile(ctx, lifecycle, apkFile);
                }
            });
            requestInstallPermission(ctx);
        }
    }

    /**
     * 安装apk,
     */
    @SuppressLint("ObsoleteSdkInt")
    @UiThread
    public static void installApkFile(
            @NonNull Context ctx,
            @NonNull Lifecycle lifecycle,
            @NonNull File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //兼容7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(ctx.getApplicationContext(), ctx.getPackageName() + ".fileProvider", apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ctx.startActivity(intent);
    }

    public static String getPermissionExplainText(Context ctx, String... permissions) {
        List<String> explainList = getPermissionExplainList(ctx, permissions);
        return ctx.getString(R.string.tip_need_permission_header) + '\n' +
                TextUtils.join("\n", explainList);
    }

    /**
     * 跳转到设置-允许安装未知来源-页面
     */
    private static void requestInstallPermission(Context ctx) {
        if (checkInstallPermission(ctx)) {
            return;
        }
        // 带uri可以直接打开app自己的页面，不过最初的安卓8.0似乎不支持，
        @SuppressLint("InlinedApi")
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public static int[] makeGrantResult(Context ctx, String[] permissions) {
        int[] ret = new int[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            ret[i] = ContextCompat.checkSelfPermission(ctx, permissions[i]);
        }
        return ret;
    }

    public interface PermissionListenerWrapper {
        void setPermissionListener(PermissionListener permissionListener);
    }

    public interface PermissionListener {
        /**
         * @return 消费了这个响应就返回true，
         */
        boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }

    private static class NextResumeObserver implements LifecycleObserver {
        private final Lifecycle lifecycle;
        private final Runnable onResume;
        // 回调了pause说明弹出了权限请求，等再回到resume就可以判断是否已经授权了，
        private boolean started;

        public NextResumeObserver(Lifecycle lifecycle, Runnable onResume) {
            this.lifecycle = lifecycle;
            this.onResume = onResume;
        }

        static void bind(Lifecycle lifecycle, Runnable onResume) {
            lifecycle.addObserver(new NextResumeObserver(lifecycle, onResume));
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        void resume() {
            if (!started) {
                return;
            }
            lifecycle.removeObserver(this);
            onResume.run();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void pause() {
            started = true;
        }
    }
    /**
     * 申请权限返回
     */
    public interface OnRequestPermissionsResultCallbacks {
        /**
         * @param isAllGranted 是否全部同意
         */
        void onPermissionsGranted(int requestCode, List<String> perms, boolean isAllGranted);

        /**
         * @param isAllDenied 是否全部拒绝
         */
        void onPermissionsDenied(int requestCode, List<String> perms, boolean isAllDenied);
    }
}
