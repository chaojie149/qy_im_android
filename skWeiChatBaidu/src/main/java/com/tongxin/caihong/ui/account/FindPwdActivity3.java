package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PasswordHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.DH;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.view.MergerStatus;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 重置密码-输入密码界面
 */
public class FindPwdActivity3 extends BaseActivity implements View.OnClickListener {
    private EditText etPassword;
    private EditText etSurePassword;
    private String mobilePrefix ="86";
    private String account;
    private String smsCode;

    public FindPwdActivity3() {
        noLoginRequired();
    }

    public static void start(Context ctx, String mobilePrefix, String account, String smsCode) {
        Intent intent = new Intent(ctx, FindPwdActivity3.class);
        intent.putExtra("mobilePrefix", mobilePrefix);
        intent.putExtra("account", account);
        intent.putExtra("smsCode", smsCode);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password3);
        mobilePrefix = getIntent().getStringExtra("mobilePrefix");
        account = getIntent().getStringExtra("account");
        smsCode = getIntent().getStringExtra("smsCode");
        initActionBar();
        initView();
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
        Button btnSureReset = findViewById(R.id.btnSureReset);
        ButtonColorChange.colorChange(mContext, btnSureReset);

        btnSureReset.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSureReset:
                String password = etPassword.getText().toString().trim();
                String surePassword = etSurePassword.getText().toString().trim();
                if (checkInput(password, surePassword)) {
                    return;
                }
                if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
                    resetPassword(password, true);
                } else {
                    resetPassword(password, false);
                }
                break;
        }
    }

    /**
     * 重置密码
     */
    private void resetPassword(String password, boolean isSupportSecureChat) {
        Map<String, String> params = new HashMap<>();

        String url;
        if (isSupportSecureChat) {
            url = coreManager.getConfig().USER_PASSWORD_RESET_V1;
            // SecureFlag 将密钥对上传服务器
            DH.DHKeyPair dhKeyPair = DH.genKeyPair();
            String dhPublicKey = dhKeyPair.getPublicKeyBase64();
            String dhPrivateKey = dhKeyPair.getPrivateKeyBase64();
            String aesEncryptDHPrivateKeyResult = SecureChatUtil.aesEncryptDHPrivateKey(password, dhPrivateKey);
            RSA.RsaKeyPair rsaKeyPair = RSA.genKeyPair();
            String rsaPublicKey = rsaKeyPair.getPublicKeyBase64();
            String rsaPrivateKey = rsaKeyPair.getPrivateKeyBase64();
            String aesEncryptRSAPrivateKeyResult = SecureChatUtil.aesEncryptRSAPrivateKey(password, rsaPrivateKey);
            String signature = SecureChatUtil.signatureUploadKeys(password, account);
            params.put("dhPublicKey", dhPublicKey);
            params.put("dhPrivateKey", aesEncryptDHPrivateKeyResult);
            params.put("rsaPublicKey", rsaPublicKey);
            params.put("rsaPrivateKey", aesEncryptRSAPrivateKeyResult);
            params.put("mac", signature);
        } else {
            url = coreManager.getConfig().USER_PASSWORD_RESET;
        }
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);
        params.put("randcode", smsCode);
        params.put("newPassword", LoginPassword.encodeMd5(password));
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));

        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(url)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (isSupportSecureChat) {
                                SecureChatUtil.setFindPasswordStatus(account, true);
                            }
                            ToastUtil.showToast(mContext, getString(R.string.update_sccuess));
                            startActivity(new Intent(mContext, LoginActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, getString(R.string.error_network));
                    }
                });
    }

    /**
     * 检查是否需要停止重置，
     *
     * @return 测试不合法返回true, 停止重置密码，
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
}
