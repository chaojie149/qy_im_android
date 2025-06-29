package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.LoginRegisterResult;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.CertificationHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.LoginSecureHelper;
import com.tongxin.caihong.helper.PasswordHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.view.MergerStatus;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 登陆-输入密码界面
 */
public class LoginActivity2 extends BaseActivity implements View.OnClickListener {
    private EditText etPassword;
    private String mobilePrefix ="86";
    /**
     * thirdToken 为空 third 为false
     * 正常账号密码登录
     * thirdToken 为空 third 为true
     * 此情况不存在
     * <p>
     * thirdToken 不为空 third 为false
     * 微信QQ登录授权成功(config内的isNoRegisterThirdLogin为0(即第三方登录需要绑定手机号))
     * 但login方法内的resultCode返回1040305(第三方登录未绑定手机号)时跳转至注册界面，注册界面在点击“绑定已有账号”回到该界面时出现
     * thirdToken 不为空 third 为true
     * 本机号一键登录、微信QQ登录授权成功时出现，根据thirdTokenType区分
     */
    private String account;
    private String thirdToken;
    private String thirdTokenType;
    // 取消授权登录
    private boolean isCanceledAuth;

    public LoginActivity2() {
        noLoginRequired();
    }

    public static void bindThird(Context ctx, String account, String thirdToken, String thirdTokenType) {
        Intent intent = new Intent(ctx, LoginActivity2.class);
        intent.putExtra("account", account);
        intent.putExtra("thirdToken", thirdToken);
        intent.putExtra("thirdTokenType", thirdTokenType);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        account = getIntent().getStringExtra("account");
        thirdToken = getIntent().getStringExtra("thirdToken");
        thirdTokenType = getIntent().getStringExtra("thirdTokenType");
        initActionBar();
        initView();
        EventBusHelper.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果没有保存用户定位信息，那么去地位用户当前位置
        if (!MyApplication.getInstance().getBdLocationHelper().isLocationUpdate()) {
            MyApplication.getInstance().getBdLocationHelper().requestLocation();
        }
    }

    private void initActionBar() {
        ((MergerStatus) findViewById(R.id.mergerStatus)).hideShadow();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        mobilePrefix = PreferenceUtils.getString(this, Constants.AREA_CODE_KEY, mobilePrefix);
        etPassword = findViewById(R.id.etPassword);
        PasswordHelper.bindPasswordEye(etPassword, findViewById(R.id.tbEye));
        // 重置密码
        TextView tvResetPassword = findViewById(R.id.tvResetPassword);
        ButtonColorChange.textChange(mContext, findViewById(R.id.tvResetPassword));
        tvResetPassword.setOnClickListener(this);
        // 登录
        Button btnLogin = findViewById(R.id.btnLogin);
        ButtonColorChange.colorChange(mContext, btnLogin);
        btnLogin.setOnClickListener(this);
        // 使用短信登录
        findViewById(R.id.tvSMSLogin).setOnClickListener(this);
        ButtonColorChange.textChange(mContext, findViewById(R.id.tvSMSLogin));
        if (coreManager.getConfig().registerUsername) {
            // 用户名注册，去掉忘记密码
            findViewById(R.id.llResetPassword).setVisibility(View.GONE);
        }
        if (coreManager.getConfig().registerUsername ||
                !TextUtils.isEmpty(thirdToken)) {
            // 用户名注册或绑定原有账号，去掉短信登录
            findViewById(R.id.tvSMSLogin).setVisibility(View.GONE);
        }
        findViewById(R.id.main_content).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvResetPassword:
                FindPwdActivity.start(mContext, mobilePrefix, account);
                break;
            case R.id.btnLogin:
                if (!HttpUtil.isGprsOrWifiConnected(mContext)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_network_error));
                    return;
                }
                login();
                break;
            case R.id.tvSMSLogin:
                LoginActivity3.bindThird(mContext, account, thirdToken, thirdTokenType);
                break;
            case R.id.main_content:
                // 点击空白区域隐藏软键盘
                InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.hideSoftInputFromWindow(findViewById(R.id.main_content).getWindowToken(), 0); //强制隐藏键盘
                }
                break;
        }
    }

    private void login() {
        final String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(mContext, getString(R.string.input_pass_word), Toast.LENGTH_SHORT).show();
            return;
        }
        // 加密之后的密码
        final String digestPwd = LoginPassword.encodeMd5(password);

        DialogHelper.showDefaulteMessageProgressDialog(this);

        Map<String, String> params = new HashMap<>();
        params.put("xmppVersion", "1");
        // 附加信息+
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
        // 地址信息
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        if (MyApplication.IS_OPEN_CLUSTER) {
            // 服务端集群需要
            String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
            if (!TextUtils.isEmpty(area)) {
                params.put("area", area);
            }
        }

        LoginSecureHelper.secureLogin(
                this, coreManager, String.valueOf(mobilePrefix), account, password, thirdToken, thirdTokenType, false,
                params,
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_login_secure_place_holder, t.getMessage()));
                }, result -> {
                    DialogHelper.dismissProgressDialog();
                    if (!Result.checkSuccess(getApplicationContext(), result)) {
                        // 以下两个if,else基本不可能走到
                        if (Result.checkError(result, Result.CODE_THIRD_NO_EXISTS)) {
                            // 如果返回1040306表示这个IM账号不存在，跳到注册页面让用户注册IM账号并绑定微信，
                            register();
                        } else if (Result.checkError(result, Result.CODE_THIRD_NO_PHONE)) {
                            // 微信没有绑定IM账号，跳到注册，注册页有回来登录老账号的按钮，
                            register();
                            finish();
                        }
                        return;
                    }
                    if (!TextUtils.isEmpty(result.getData().getAuthKey())) {
                        isCanceledAuth = false;
                        DialogHelper.showMessageProgressDialogAddCancel(this, getString(R.string.tip_need_auth_login), (DialogInterface dialogInterface) -> {
                            isCanceledAuth = true;
                        });
                        CheckAuthLoginRunnable authLogin = new CheckAuthLoginRunnable(result.getData().getAuthKey(), account, digestPwd);
                        waitAuth(authLogin);
                        return;
                    }
                    start(password, result, account, digestPwd);
                }
        );
    }

    private void register() {
        RegisterActivity.registerFromThird(
                this,
                thirdToken,
                thirdTokenType
        );
    }

    private void waitAuth(CheckAuthLoginRunnable authLogin) {
        authLogin.waitAuthHandler.postDelayed(authLogin, 3000);
    }

    private void start(String password, ObjectResult<LoginRegisterResult> result, String phoneNumber, String digestPwd) {
        LoginHelper.setLoginUser(mContext, coreManager, phoneNumber, digestPwd, result);

        if (!TextUtils.isEmpty(result.getData().getHeadimgurl())) {
            saveAvatar(result.getData().getUserId(), result.getData().getHeadimgurl());
        }

        LoginRegisterResult.Settings settings = result.getData().getSettings();
        MyApplication.getInstance().initPayPassword(result.getData().getUserId(), result.getData().getPayPassword());
        WeboxHelper.saveWalletId(mContext, result.getData().getWalletId());
        CertificationHelper.saveAuth(mContext, result.getData().getRealNameCertified() == 1);
        PrivacySettingHelper.setPrivacySettings(mContext, settings);
        MyApplication.getInstance().initMulti();

        // startActivity(new Intent(mContext, DataDownloadActivity.class));
        DataDownloadActivity.start(mContext, result.getData().getIsupdate(), password);
        finish();
    }

    /**
     * 第三方登录，将头像保存至本地
     */
    private void saveAvatar(String userId, String headImageUrl) {
        ImageLoadHelper.loadBitmapCenterCropDontAnimate(MyApplication.getContext(), headImageUrl, b -> {
            String path = FileUtil.saveBitmap(b);
            uploadAvatar(userId, new File(path));
        }, e -> {

        });
    }

    /**
     * 第三方登录，上传头像，上传成功与否都不管
     */
    private void uploadAvatar(String userId, File file) {
        if (!file.exists()) {
            // 文件不存在
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        HttpUtils.post().url(coreManager.getConfig().AVATAR_UPLOAD_URL)
                .params(params)
                .params("files", file)
                .build(true, true)
                .execute(new FileCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(Void result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    private class CheckAuthLoginRunnable implements Runnable {
        private final String phoneNumber;
        private final String digestPwd;
        private Handler waitAuthHandler = new Handler();
        private int waitAuthTimes = 10;
        private String authKey;

        public CheckAuthLoginRunnable(String authKey, String phoneNumber, String digestPwd) {
            this.authKey = authKey;
            this.phoneNumber = phoneNumber;
            this.digestPwd = digestPwd;
        }

        @Override
        public void run() {
            HttpUtils.get().url(coreManager.getConfig().CHECK_AUTH_LOGIN)
                    .params("authKey", authKey)
                    .build(true, true)
                    .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {
                        @Override
                        public void onResponse(ObjectResult<LoginRegisterResult> result) {
                            if (Result.checkError(result, Result.CODE_AUTH_LOGIN_SCUESS)) {
                                DialogHelper.dismissProgressDialog();
                                login();
                            } else if (Result.checkError(result, Result.CODE_AUTH_LOGIN_FAILED_1)) {
                                if (!isCanceledAuth) {
                                    waitAuth(LoginActivity2.CheckAuthLoginRunnable.this);
                                }
                            } else {
                                DialogHelper.dismissProgressDialog();
                                if (!TextUtils.isEmpty(result.getResultMsg())) {
                                    ToastUtil.showToast(mContext, result.getResultMsg());
                                } else {
                                    ToastUtil.showToast(mContext, R.string.tip_server_error);
                                }
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                            ToastUtil.showErrorNet(mContext);
                        }
                    });
        }
    }
}
