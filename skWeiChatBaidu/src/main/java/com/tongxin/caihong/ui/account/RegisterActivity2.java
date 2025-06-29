package com.tongxin.caihong.ui.account;

import android.annotation.SuppressLint;
import android.content.Context;
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

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Code;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.view.MergerStatus;
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
 * 注册-填写短信验证码界面
 */
public class RegisterActivity2 extends BaseActivity implements View.OnClickListener {
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
    private String password;
    private String invitationCode;
    private String thirdToken;
    private String thirdTokenType;
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
                tvCountdownReSend.setOnClickListener(RegisterActivity2.this);
                reckonTime = 60;
            }
        }
    };

    public RegisterActivity2() {
        noLoginRequired();
    }

    public static void bindThird(Context ctx, String mobilePrefix, String account, String password, String invitationCode, String thirdToken, String thirdTokenType) {
        Intent intent = new Intent(ctx, RegisterActivity2.class);
        intent.putExtra("mobilePrefix", mobilePrefix);
        intent.putExtra("account", account);
        intent.putExtra("password", password);
        intent.putExtra("invitationCode", invitationCode);
        intent.putExtra("thirdToken", thirdToken);
        intent.putExtra("thirdTokenType", thirdTokenType);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        mobilePrefix = getIntent().getStringExtra("mobilePrefix");
        account = getIntent().getStringExtra("account");
        password = getIntent().getStringExtra("password");
        invitationCode = getIntent().getStringExtra("invitationCode");
        thirdToken = getIntent().getStringExtra("thirdToken");
        thirdTokenType = getIntent().getStringExtra("thirdTokenType");
        initActionBar();
        initView();
        // 进入页面，自动获取短信验证码
        smsCheck();
        EventBusHelper.register(this);
    }

    private void initActionBar() {
        ((MergerStatus) findViewById(R.id.mergerStatus)).hideShadow();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        TextView tvTelephone = findViewById(R.id.tvTelephone);
        tvTelephone.setText("+" + mobilePrefix + account);
        etSMSCode = findViewById(R.id.etSMSCode);
        tvCountdownReSend = findViewById(R.id.tvCountdownReSend);
        ButtonColorChange.textChange(mContext, tvCountdownReSend);
        tvCountdownReSend.setOnClickListener(this);
        Button btnNext = findViewById(R.id.btnNext);
        ButtonColorChange.colorChange(mContext, btnNext);
        btnNext.setOnClickListener(this);
        findViewById(R.id.main_content).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvCountdownReSend:
                smsCheck();
                break;
            case R.id.btnNext:
                next();
                break;
            case R.id.main_content:
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
        params.put("isRegister", String.valueOf(1));
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


    private void next() {
        String smsCode = etSMSCode.getText().toString().trim();
        if (TextUtils.isEmpty(smsCode)) {
            ToastUtil.showToast(mContext, getString(R.string.sms_code_cannot_null));
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone",  account);
        params.put("smsCode", smsCode);
        params.put("isRegister", String.valueOf(1));

        HttpUtils.get().url(coreManager.getConfig().VERIFY_SMS_CODE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            RegisterActivity.isSmsRegister = 1;
                            RegisterUserBasicInfoActivity.start(mContext,
                                    String.valueOf(mobilePrefix),
                                    account,
                                    LoginPassword.encodeMd5(password),
                                    smsCode,
                                    invitationCode,
                                    thirdToken,
                                    thirdTokenType,
                                    password
                            );
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }
}
