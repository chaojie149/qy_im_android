package com.tongxin.caihong.ui.account;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.tongxin.caihong.bean.Code;
import com.tongxin.caihong.bean.LoginRegisterResult;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.CertificationHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.LoginSecureHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.view.MergerStatus;
import com.tongxin.caihong.view.VerifyDialog;
import com.tongxin.caihong.view.verificationcodejavademo.widget.BlockPuzzleDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 登录-填写短信验证码界面
 */
public class LoginActivity3 extends BaseActivity implements View.OnClickListener {
    private EditText etSMSCode;
    private TextView tvCountdownReSend;
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
    private VerifyDialog mVerifyDialog;
    private int reckonTime = 60;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0x1) {
                tvCountdownReSend.setText(getString(R.string.after_countdown_can_resend, reckonTime));
                reckonTime--;
                if (reckonTime < 0) {
                    handler.sendEmptyMessage(0x2);
                } else {
                    handler.sendEmptyMessageDelayed(0x1, 1000);
                }
            } else if (msg.what == 0x2) {
                // 60秒结束
                ButtonColorChange.textChange(mContext, tvCountdownReSend);
                tvCountdownReSend.setText(getString(R.string.regain_code));
                tvCountdownReSend.setOnClickListener(LoginActivity3.this);
                reckonTime = 60;
            }
        }
    };

    public LoginActivity3() {
        noLoginRequired();
    }

    public static void bindThird(Context ctx, String account, String thirdToken, String thirdTokenType) {
        Intent intent = new Intent(ctx, LoginActivity3.class);
        intent.putExtra("account", account);
        intent.putExtra("thirdToken", thirdToken);
        intent.putExtra("thirdTokenType", thirdTokenType);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login3);

        account = getIntent().getStringExtra("account");
        thirdToken = getIntent().getStringExtra("thirdToken");
        thirdTokenType = getIntent().getStringExtra("thirdTokenType");
        initActionBar();
        initView();
        // 进入页面，自动获取短信验证码
        smsCheck();
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
        TextView tvTelephone = findViewById(R.id.tvTelephone);
        tvTelephone.setText("+" + mobilePrefix + account);
        etSMSCode = findViewById(R.id.etSMSCode);
        tvCountdownReSend = findViewById(R.id.tvCountdownReSend);
        ButtonColorChange.textChange(mContext, tvCountdownReSend);
        tvCountdownReSend.setOnClickListener(this);
        // 登录
        Button btnLogin = findViewById(R.id.btnLogin);
        ButtonColorChange.colorChange(mContext, btnLogin);
        btnLogin.setOnClickListener(this);
        findViewById(R.id.main_content).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvCountdownReSend:
                smsCheck();
                break;
            case R.id.btnLogin:
                // 登录
                if (!HttpUtil.isGprsOrWifiConnected(mContext)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_network_error));
                    return;
                }
                login();
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

    /**
     * 获取短信验证码之前需要校验
     */
    private void smsCheck() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);

        HttpUtils.get().url(coreManager.getConfig().USER_SMS_CHECK)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Code>(Code.class) {
                    @Override
                    public void onResponse(ObjectResult<Code> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            // 成功，无需图形验证码即请求短信
                            requestAuthCode();
                        } else if (result.getResultCode() == 100218) {
                            // 需要图形验证码才能请求短信 ，弹出验证码框
                            requestImageCode();
                        } else if (result.getResultCode() == 200003) {
                            // 不允许发送短信
                            ToastUtil.showToast(mContext, getString(R.string.not_allow_send_sms));
                        } else {
                            ToastUtil.showToast(mContext, result.getResultMsg() + "");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 请求验证码
     */
    private void requestAuthCode() {
        requestAuthCode(null);
    }

    /**
     * 请求图形验证码
     */
    private void requestImageCode() {
        BlockPuzzleDialog dialog = new BlockPuzzleDialog(mContext);
        dialog.setOnResultsListener(this::requestAuthCode);
        dialog.show();
    }

    private void requestAuthCode(String imageCodeStr) {
        Map<String, String> params = new HashMap<>();
        String language = Locale.getDefault().getLanguage();
        params.put("language", language);
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);
        if (!TextUtils.isEmpty(imageCodeStr)) {
            params.put("captchaVerification", imageCodeStr);
        }
        params.put("isRegister", String.valueOf(0));
        params.put("version", "1");

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().SEND_AUTH_CODE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Code>(Code.class) {

                    @Override
                    public void onResponse(ObjectResult<Code> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            tvCountdownReSend.setTextColor(getResources().getColor(R.color.text_color));
                            tvCountdownReSend.setOnClickListener(null);
                            // 开始倒计时
                            handler.sendEmptyMessage(0x1);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void login() {
        String smsCode = etSMSCode.getText().toString().trim();
        if (TextUtils.isEmpty(smsCode)) {
            ToastUtil.showToast(mContext, getString(R.string.sms_code_cannot_null));
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<>();
        params.put("xmppVersion", "1");
        // 附加信息
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
        params.put("loginType", "1");//验证码登录

        // 地址信息
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        if (MyApplication.IS_OPEN_CLUSTER) {// 服务端集群需要
            String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
            if (!TextUtils.isEmpty(area)) {
                params.put("area", area);
            }
        }

        LoginSecureHelper.smsLogin(
                this, coreManager, smsCode, String.valueOf(mobilePrefix), account,
                params,
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_login_secure_place_holder, t.getMessage()));
                }, result -> {
                    DialogHelper.dismissProgressDialog();
                    if (!Result.checkSuccess(getApplicationContext(), result)) {
                        return;
                    }
                    if (!TextUtils.isEmpty(result.getData().getAuthKey())) {
                        isCanceledAuth = false;
                        DialogHelper.showMessageProgressDialogAddCancel(this, getString(R.string.tip_need_auth_login), (DialogInterface dialogInterface) -> {
                            isCanceledAuth = true;
                        });
                        CheckAuthLoginRunnable authLogin = new CheckAuthLoginRunnable(result.getData().getAuthKey());
                        waitAuth(authLogin);
                        return;
                    }
                    afterLogin(result);
                });
    }

    private void afterLogin(ObjectResult<LoginRegisterResult> result) {
        if (MyApplication.IS_SUPPORT_SECURE_CHAT
                && result.getData().getIsSupportSecureChat() == 1) {// 新用户才需要，老用户不支持端到端加密，不需要
            // SecureFlag 短信验证码登录成功，将无法解密服务端返回的私钥，需要让用户输入密码解密
            mVerifyDialog = new VerifyDialog(mContext);
            mVerifyDialog.setVerifyClickListener(getString(R.string.login)
                    , getString(R.string.input_password_to_decrypt_keys)
                    , ""
                    , new VerifyDialog.VerifyClickListener() {
                        @Override
                        public void cancel() {
                            mVerifyDialog.dismiss();
                            FindPwdActivity.start(mContext, mobilePrefix, account);
                        }

                        @Override
                        public void send(String str) {
                            checkPasswordWXAuthCodeLogin(str, result);
                        }
                    });
            mVerifyDialog.setDismiss(false);
            mVerifyDialog.setCancelButton(R.string.forget_password);
            mVerifyDialog.show();
        } else {
            start("", result);
        }
/*
        boolean success = LoginHelper.setLoginUser(mContext, coreManager, phone, result.getData().getPassword(), result);// 设置登陆用户信息
        if (success) {
            if (MyApplication.IS_SUPPORT_SECURE_CHAT
                    && result.getData().getIsSupportSecureChat() == 1) {// 新用户才需要，老用户不支持端到端加密，不需要
                // SecureFlag 短信验证码登录成功，将无法解密服务端返回的私钥，需要让用户输入密码解密
                mVerifyDialog = new VerifyDialog(mContext);
                mVerifyDialog.setVerifyClickListener(getString(R.string.input_password_to_decrypt_keys), new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {
                        mVerifyDialog.dismiss();
                        startActivity(new Intent(mContext, FindPwdActivity.class));
                    }

                    @Override
                    public void send(String str) {
                        checkPasswordWXAuthCodeLogin(str, result.getData());
                    }
                });
                mVerifyDialog.setDismiss(false);
                mVerifyDialog.setCancelButton(R.string.forget_password);
                mVerifyDialog.show();
            } else {
                start("", result.getData());
            }
        } else {
            // 登录失败
            String message = TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.login_failed) : result.getResultMsg();
            ToastUtil.showToast(mContext, message);
        }
*/
    }

    private void waitAuth(CheckAuthLoginRunnable authLogin) {
        authLogin.waitAuthHandler.postDelayed(authLogin, 3000);
    }

    private void checkPasswordWXAuthCodeLogin(String password, ObjectResult<LoginRegisterResult> registerResult) {

        LoginHelper.saveUserForThirdSmsVerifyPassword(mContext, coreManager,
                account, registerResult.getData().getPassword(), registerResult);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("password", LoginPassword.encodeMd5(password));

        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        HttpUtils.get().url(coreManager.getConfig().USER_VERIFY_PASSWORD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkSuccess(mContext, result)) {
                            mVerifyDialog.dismiss();
                            start(password, registerResult);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void start(String password, ObjectResult<LoginRegisterResult> result) {
        LoginHelper.setLoginUser(mContext, coreManager, account, result.getData().getPassword(), result);

        LoginRegisterResult.Settings settings = result.getData().getSettings();
        MyApplication.getInstance().initPayPassword(result.getData().getUserId(), result.getData().getPayPassword());
        WeboxHelper.saveWalletId(mContext, result.getData().getWalletId());
        CertificationHelper.saveAuth(mContext, result.getData().getRealNameCertified() == 1);
        PrivacySettingHelper.setPrivacySettings(LoginActivity3.this, settings);
        MyApplication.getInstance().initMulti();

        DataDownloadActivity.start(mContext, result.getData().getIsupdate(), password);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    private class CheckAuthLoginRunnable implements Runnable {
        private Handler waitAuthHandler = new Handler();
        private int waitAuthTimes = 10;

        private String authKey;

        public CheckAuthLoginRunnable(String authKey) {
            this.authKey = authKey;
        }

        @Override
        public void run() {
            HttpUtils.get().url(coreManager.getConfig().CHECK_AUTH_LOGIN)
                    .params("authKey", authKey)
                    .build(true, true)
                    .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {
                        @Override
                        public void onResponse(ObjectResult<LoginRegisterResult> result) {
                            if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkError(result, com.xuan.xuanhttplibrary.okhttp.result.Result.CODE_AUTH_LOGIN_SCUESS)) {
                                DialogHelper.dismissProgressDialog();
                                login();
                            } else if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkError(result, com.xuan.xuanhttplibrary.okhttp.result.Result.CODE_AUTH_LOGIN_FAILED_1)) {
                                if (!isCanceledAuth) {
                                    waitAuth(CheckAuthLoginRunnable.this);
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
