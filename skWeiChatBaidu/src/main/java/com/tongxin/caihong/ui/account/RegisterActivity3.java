package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PasswordHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.view.MergerStatus;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 注册-输入密码界面
 */
public class RegisterActivity3 extends BaseActivity {
    private EditText etPassword;
    private EditText etSurePassword;
    private String mobilePrefix ="86";
    private String account;
    private String invitationCode;
    private String thirdToken;
    private String thirdTokenType;

    public RegisterActivity3() {
        noLoginRequired();
    }

    public static void bindThird(Context ctx, String mobilePrefix, String account, String invitationCode, String thirdToken, String thirdTokenType) {
        Intent intent = new Intent(ctx, RegisterActivity3.class);
        intent.putExtra("mobilePrefix", mobilePrefix);
        intent.putExtra("account", account);
        intent.putExtra("invitationCode", invitationCode);
        intent.putExtra("thirdToken", thirdToken);
        intent.putExtra("thirdTokenType", thirdTokenType);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register3);
        mobilePrefix = getIntent().getStringExtra("mobilePrefix");
        account = getIntent().getStringExtra("account");
        invitationCode = getIntent().getStringExtra("invitationCode");
        thirdToken = getIntent().getStringExtra("thirdToken");
        thirdTokenType = getIntent().getStringExtra("thirdTokenType");
        initActionBar();
        initView();
        EventBusHelper.register(this);
    }

    private void initActionBar() {
        ((MergerStatus) findViewById(R.id.mergerStatus)).hideShadow();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        etPassword = findViewById(R.id.etPassword);
        PasswordHelper.bindPasswordEye(etPassword, findViewById(R.id.tbEye1));
        etSurePassword = findViewById(R.id.etSurePassword);
        PasswordHelper.bindPasswordEye(etSurePassword, findViewById(R.id.tbEye2));
        Button btnNext = findViewById(R.id.btnNext);
        ButtonColorChange.colorChange(mContext, btnNext);

        if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
            findViewById(R.id.tvSecureTip).setVisibility(View.VISIBLE);
        }

        btnNext.setOnClickListener(view -> {
            findViewById(R.id.main_content).performClick();

            String password = etPassword.getText().toString().trim();
            String surePassword = etSurePassword.getText().toString().trim();
            if (checkInput(password, surePassword)) {
                return;
            }

            if (!coreManager.getConfig().registerUsername
                    && coreManager.getConfig().isOpenSMSCode) {
                // 点击跳转到获取验证码
                next(password, true);
            } else {
                // 用户名注册 || 注册免验证码，点击直接跳转至填写信息页面
                next(password, false);
            }
        });

        findViewById(R.id.main_content).setOnClickListener(v -> {
            // 点击空白区域隐藏软键盘
            InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(findViewById(R.id.main_content).getWindowToken(), 0); //强制隐藏键盘
            }
        });
    }

    private void next(String password, boolean isVerifySMSCode) {
        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);
        HttpUtils.get().url(coreManager.getConfig().VERIFY_TELEPHONE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (isVerifySMSCode) {
                                RegisterActivity2.bindThird(mContext,
                                        mobilePrefix,
                                        account,
                                        password,
                                        invitationCode,
                                        thirdToken,
                                        thirdTokenType);
                            } else {
                                RegisterUserBasicInfoActivity.start(mContext,
                                        String.valueOf(mobilePrefix),
                                        account,
                                        LoginPassword.encodeMd5(password),
                                        "",
                                        invitationCode,
                                        thirdToken,
                                        thirdTokenType,
                                        password);
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

    /**
     * 检查是否需要停止注册，
     *
     * @return 测试不合法返回true, 停止继续注册，
     */
    private boolean checkInput(String password, String surePassword) {
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            ToastUtil.showToast(mContext, getString(R.string.password_empty_error));
            return true;
        }
        if (TextUtils.isEmpty(surePassword) || surePassword.length() < 6) {
            ToastUtil.showToast(mContext, getString(R.string.confirm_password_empty_error));
            return true;
        }
        if (!TextUtils.equals(password, surePassword)) {
            ToastUtil.showToast(mContext, getString(R.string.password_confirm_password_not_match));
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }
}
