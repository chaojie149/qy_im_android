package com.tongxin.caihong.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.ConfigBean;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.AdHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.service.customservice.CheckConfigService;
import com.tongxin.caihong.ui.account.LoginActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.SetConfigActivity;
import com.tongxin.caihong.ui.notification.NotificationProxyActivity;
import com.tongxin.caihong.ui.other.PrivacyAgreeActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.VersionUtil;
import com.tongxin.caihong.view.PermissionExplainDialog;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.view.ad.AdView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 启动页
 */
public class SplashActivity extends BaseActivity implements PermissionUtil.OnRequestPermissionsResultCallbacks{
    // 声明一个数组，用来存储所有需要动态申请的权限
    private static final int REQUEST_CODE = 0;
    private final Map<String, Integer> permissionsMap = new LinkedHashMap<>();
    private long startTime;
    private boolean adSkip;
    private boolean adOpened;

    // 配置是否成功
    private boolean mConfigReady = false;
    // 复用请求权限的说明对话框，
    private PermissionExplainDialog permissionExplainDialog;

   /* public SplashActivity() {
        // 这个页面不需要已经获取config, 也不需要已经登录，
        noConfigRequired();
        noLoginRequired();
    }*/
    public SplashActivity() {
        // 这个页面不需要已经获取config, 也不需要已经登录，
        noConfigRequired();
        noLoginRequired();

        // 手机状态
        permissionsMap.put(Manifest.permission.READ_PHONE_STATE, R.string.permission_phone_status);
        // 照相
        permissionsMap.put(Manifest.permission.CAMERA, R.string.permission_photo);
        // 麦克风
        permissionsMap.put(Manifest.permission.RECORD_AUDIO, R.string.permission_microphone);
        // 存储权限
        permissionsMap.put(Manifest.permission.READ_EXTERNAL_STORAGE, R.string.permission_storage);
        permissionsMap.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.permission_storage);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        LogUtils.log(TAG, intent);
        Intent serviceIntent = new Intent(this, CheckConfigService.class);
        startService(serviceIntent);
        if (NotificationProxyActivity.processIntent(intent)) {
            // 如果是通知点击进来的，带上参数转发给NotificationProxyActivity处理，
            intent.setClass(this, NotificationProxyActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        // 如果不是任务栈第一个页面，就直接结束，显示上一个页面，
        // 主要是部分设备上Jitsi_pre页面退后台再回来会打开这个启动页flg=0x10200000，此时应该结束启动页，回到Jitsi_pre,
        if (!isTaskRoot()) {
            finish();
            return;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_splash);

        initAd();
        // 初始化配置
        initConfig();
// 同时请求定位以外的权限，
        requestPermissions();

        EventBusHelper.register(this);
    }

    private void initAd() {
        AdView adView = findViewById(R.id.adView);
        AdHelper.bind(this, adView, AdHelper.AdType.Splash);
        adSkip = true;
        AdHelper.setShowed(this, AdHelper.AdType.Splash);
        adView.setOnAdSkipListener(() -> {
            adSkip = true;
            ready();
        });
        // 点了广告就跳过，不过不是立即进入主页，否则会覆盖浏览器，
        adView.setOnAdClickListener(() -> {
            adSkip = true;
        });
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        adOpened = false;
        // 请求权限过程中离开了回来就再请求吧，
        ready();
    }

    @Override
    protected void onStop() {
        if (adSkip) {
            // 如果是打开广告的状态，记住，避免在activity后台情况加载完成打开主页，
            adOpened = true;
        }
        super.onStop();
    }

    /**
     * 配置参数初始化
     */
    List<SetConfigActivity.ConfigData> list = new ArrayList<>();
    int i=0;

    private void initConfig() {

        String str = PreferenceUtils.getString(this, "APP_LIST_CONFIG", null);
        if (str == null) {
            list=AppConfig.ROUTE_LINE;
            ToastUtil.showToast(this, "正在使用线路" + (i + 1));
            getConfig(list.get(i).url);
        }else {
            getConfig("");
        }
    }

    private void getConfig(String url) {

        String mConfigApi;
        if (!TextUtils.isEmpty(url)){
            url=url.replace("/config","").replace("/","")+"/config";
            mConfigApi = url;
        }else {
            mConfigApi = AppConfig.readConfigUrl(mContext);
        }

        Map<String, String> params = new HashMap<>();
        Reporter.putUserData("configUrl", mConfigApi);
        long requestTime = System.currentTimeMillis();
        HttpUtils.get().url(mConfigApi)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<ConfigBean>(ConfigBean.class) {
                    @Override
                    public void onResponse(ObjectResult<ConfigBean> result) {
                        if (result != null) {
                            long responseTime = System.currentTimeMillis();
                            TimeUtils.responseTime(requestTime, result.getCurrentTime(), result.getCurrentTime(), responseTime);
                        }
                        ConfigBean configBean;
                        if (result == null || result.getData() == null || result.getResultCode() != Result.CODE_SUCCESS) {
                            Log.e("zq", "获取网络配置失败，使用已经保存了的配置");
                            if (BuildConfig.DEBUG) {
                                ToastUtil.showToast(SplashActivity.this, R.string.tip_get_config_failed);
                            }
                            // 获取网络配置失败，使用已经保存了的配置，
                            configBean = coreManager.readConfigBean();
                        } else {
                            Log.e("zq", "获取网络配置成功，使用服务端返回的配置并更新本地配置");
                            configBean = result.getData();
                            if (!TextUtils.isEmpty(configBean.getAddress())) {
                                PreferenceUtils.putString(SplashActivity.this, AppConstant.EXTRA_CLUSTER_AREA, configBean.getAddress());
                            }
                            coreManager.saveConfigBean(configBean);
                            MyApplication.IS_OPEN_CLUSTER = configBean.getIsOpenCluster() == 1 ? true : false;
                        }
                        setConfig(configBean);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        i++;

                        if (i<list.size()){
                            ToastUtil.showToast(mContext,"正在使用线路"+(i+1));
                            getConfig(list.get(i).url);
                        }else {
//                            Log.e("zq", "获取网络配置失败，使用已经保存了的配置");
                             ToastUtil.showToast(SplashActivity.this,"获取网络配置失败，使用已经保存了的配置");
                            // 获取网络配置失败，使用已经保存了的配置，
                            ConfigBean configBean = coreManager.readConfigBean();
                            setConfig(configBean);
//                            ready();
                        }
                    }
                });
    }

    private void setConfig(ConfigBean configBean) {
        if (configBean == null) {
            if (BuildConfig.DEBUG) {
                ToastUtil.showToast(this, R.string.tip_get_config_failed);
            }

            // 如果没有保存配置，也就是第一次使用，就连不上服务器，使用默认配置
            configBean = CoreManager.getDefaultConfig(this);
            if (configBean == null) {
                // 不可到达，本地assets一定要提供默认config,
                DialogHelper.tip(this, getString(R.string.tip_get_config_failed));
                return;
            }
            coreManager.saveConfigBean(configBean);
        }

        // todo 定位权限放到应用内请求，class：LoginActivity、SelectAreaActivity、NearPersonActivity、MapPickerActivity

        if (!coreManager.getConfig().disableLocationServer) {// 定位
            permissionsMap.put(Manifest.permission.ACCESS_COARSE_LOCATION, R.string.tip_permission_location);
            permissionsMap.put(Manifest.permission.ACCESS_FINE_LOCATION, R.string.tip_permission_location);
        }

        // 配置完毕
        mConfigReady = true;
        MyApplication.IS_SUPPORT_SECURE_CHAT = configBean.getIsOpenSecureChat() == 1;
        // 如果没有androidDisable字段就不判断，
        // 当前版本没被禁用才继续打开，
        if (TextUtils.isEmpty(configBean.getAndroidDisable()) || !blockVersion(configBean.getAndroidDisable(), configBean.getAndroidAppUrl())) {
            // 进入主界面
            ready();
        }
    }

    /**
     * 如果当前版本被禁用，就自杀，
     *
     * @param disabledVersion 禁用该版本以下的版本，
     * @param appUrl          版本被禁用时打开的地址，
     * @return 返回是否被禁用，
     */
    private boolean blockVersion(String disabledVersion, String appUrl) {
        String currentVersion = BuildConfig.VERSION_NAME;
        if (VersionUtil.compare(currentVersion, disabledVersion) > 0) {
            // 当前版本大于被禁用版本，
            return false;
        } else {
            // 通知一下，
            TipDialog tipDialog = new TipDialog(this);
            tipDialog.setmConfirmOnClickListener(getString(R.string.tip_version_disabled), () -> {

            });
            tipDialog.setOnDismissListener(dialog -> {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(appUrl));
                    startActivity(i);
                } catch (Exception e) {
                    // 弹出浏览器失败的话无视，
                    // 比如没有浏览器的情况，
                    // 比如appUrl不合法的情况，
                }
                // 自杀，
                finish();
                MyApplication.getInstance().destory();
            });
            tipDialog.show();
            return true;
        }
    }

    private void ready() {
        if (!mConfigReady) {// 配置失败
            return;
        }

        // 检查 || 请求权限
        boolean hasAll = requestPermissions();
        if (hasAll) {// 已获得所有权限
            jump();
        }
    }

    private void jump() {
        if (isDestroyed()) {
            return;
        }
        if (adOpened) {
            // 打开了广告等回来才继续，
            return;
        }
        if (!adSkip) {
            return;
        }
        int userStatus = LoginHelper.prepareUser(mContext, coreManager);
        Intent intent = new Intent();
        switch (userStatus) {
            case LoginHelper.STATUS_USER_FULL:
            case LoginHelper.STATUS_USER_NO_UPDATE:
            case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
                boolean login = PreferenceUtils.getBoolean(this, Constants.LOGIN_CONFLICT, false);
                if (login) {// 登录冲突，退出app再次进入，跳转至历史登录界面
                    intent.setClass(mContext, LoginActivity.class);
                } else {
                    intent.setClass(mContext, MainActivity.class);
                }
                break;
            case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                intent.setClass(mContext, LoginActivity.class);
                break;
            case LoginHelper.STATUS_NO_USER:
            default:
                stay();
                return;// must return
        }
        startActivity(intent);
        finish();
    }

    // 第一次进入，显示登录、注册按钮
    private void stay() {
        // 因为启动页有时会替换，无法做到按钮与图片的完美适配，干脆直接进入到登录页面
        startActivity(new Intent(mContext, LoginActivity.class));
        finish();
    }

    /*private boolean requestPermissions() {
        if (mConfigReady && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix) &&
                !PreferenceUtils.getBoolean(this, Constants.PRIVACY_AGREE_STATUS, false)) {
            // 先同意隐私政策，
            PrivacyAgreeActivity.start(this, true, false);
            return false;
        }
        MyApplication.getInstance().getBdLocationHelper().setEnabled(true);
        return true;
    }*/
    private boolean requestPermissions() {
//        if (mConfigReady && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix) &&
//                !PreferenceUtils.getBoolean(this, Constants.PRIVACY_AGREE_STATUS, false)) {
//            // 先同意隐私政策，
//            PrivacyAgreeActivity.start(this, true, false);
//            return false;
//        } else {
//            // 请求定位以外的权限，
//            return requestPermissions(permissionsMap.keySet().toArray(new String[]{}));
//        }
        return true;
    }
    private boolean requestPermissions(String... permissions) {
        List<String> deniedPermission = PermissionUtil.getDeniedPermissions(this, permissions);
        if (deniedPermission.size()!=0) {
            PermissionExplainDialog tip = getPermissionExplainDialog();
            tip.setPermissions(deniedPermission.toArray(new String[0]));
            tip.setOnConfirmListener(() -> {
                PermissionUtil.requestPermissions(this, REQUEST_CODE, permissions);
            });
            tip.show();
            return false;
        }
        return true;
    }

    private PermissionExplainDialog getPermissionExplainDialog() {
        if (permissionExplainDialog == null) {
            permissionExplainDialog = new PermissionExplainDialog(this);
        }
        return permissionExplainDialog;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {// 设置 手动开启权限 返回 再次判断是否获取全部权限
            ready();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms, boolean isAllGranted) {
        if (isAllGranted) {// 请求权限返回 已全部授权
            ready();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms, boolean isAllDenied) {
        if (perms != null && perms.size() > 0) {
            // 用set去重，有的复数权限
            Set<String> tipSet = new HashSet<>();
            for (int i = 0; i < perms.size(); i++) {
                tipSet.add(getString(permissionsMap.get(perms.get(i))));
            }
            String tipS = TextUtils.join(", ", tipSet);
            boolean onceAgain = PermissionUtil.deniedRequestPermissionsAgain(this, perms.toArray(new String[perms.size()]));
            TipDialog mTipDialog = new TipDialog(this);
            if (onceAgain) {// 部分 || 所有权限被拒绝且选择了（选择了不再询问 || 部分机型默认为不在询问）
                mTipDialog.setmConfirmOnClickListener(getString(R.string.tip_reject_permission_place_holder, tipS), new TipDialog.ConfirmOnClickListener() {
                    @Override
                    public void confirm() {
                        PermissionUtil.startApplicationDetailsSettings(SplashActivity.this, REQUEST_CODE);
                    }
                });
            } else {// 部分 || 所有权限被拒绝
                mTipDialog.setTip(getString(R.string.tip_permission_reboot_place_holder, tipS));
            }
            mTipDialog.show();
        }
    }
}
