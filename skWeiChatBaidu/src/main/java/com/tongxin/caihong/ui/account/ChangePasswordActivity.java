package com.tongxin.caihong.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.UserRandomStr;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.PasswordHelper;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
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
 * 修改密码界面
 */
public class ChangePasswordActivity extends BaseActivity implements View.OnClickListener {
    private TextView tvPrefix;
    private EditText etAccount;
    private EditText etOldPassword;
    private EditText etPassword;
    private EditText etSurePassword;
    private String mobilePrefix ="86";
    // SecureFlag
    private String authCode;
    private boolean isSupportSecureChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        mobilePrefix = PreferenceUtils.getString(this, Constants.AREA_CODE_KEY, mobilePrefix);
        initActionBar();
        initView();
        isSupportSecureChat = MyApplication.IS_SUPPORT_SECURE_CHAT
                && !TextUtils.isEmpty(SecureChatUtil.getDHPrivateKey(coreManager.getSelf().getUserId()));
    }

    private void initActionBar() {
        ((MergerStatus) findViewById(R.id.mergerStatus)).hideShadow();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        tvPrefix = (TextView) findViewById(R.id.tvPrefix);
        tvPrefix.setOnClickListener(this);
        tvPrefix.setText("+" + mobilePrefix);
        if (coreManager.getConfig().registerUsername) {
            findViewById(R.id.llPrefix).setVisibility(View.GONE);
        }
        etAccount = findViewById(R.id.etAccount);
        UsernameHelper.initEditText(etAccount, coreManager.getConfig().registerUsername);
        String telephone = coreManager.getSelf().getTelephoneNoAreaCode();
        etAccount.setText(telephone);
        etOldPassword = (EditText) findViewById(R.id.etOldPassword);
        PasswordHelper.bindPasswordEye(etOldPassword, findViewById(R.id.tbEye1));
        etPassword = (EditText) findViewById(R.id.etPassword);
        PasswordHelper.bindPasswordEye(etPassword, findViewById(R.id.tbEye2));
        etSurePassword = findViewById(R.id.etSurePassword);
        PasswordHelper.bindPasswordEye(etSurePassword, findViewById(R.id.tbEye3));
        Button btnSureModify = (Button) findViewById(R.id.btnSureModify);
        ButtonColorChange.colorChange(this, btnSureModify);
        btnSureModify.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvPrefix:
                Intent intent = new Intent(mContext, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.btnSureModify:
                String account = etAccount.getText().toString().trim();
                String oldPassword = etOldPassword.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String surePassword = etSurePassword.getText().toString().trim();
                if (checkInput(oldPassword, password, surePassword)) {
                    return;
                }
                if (isSupportSecureChat) {
                    getCheckCode(account, oldPassword, password);
                } else {
                    changePassword(account, oldPassword, password);
                }
                break;
        }
    }

    private boolean checkInput(String oldPassword, String password, String surePassword) {
        if (TextUtils.isEmpty(oldPassword)) {
            ToastUtil.showToast(mContext, getString(R.string.tip_old_password_cannot_null));
            return true;
        }
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
        if (TextUtils.equals(oldPassword, password)) {
            ToastUtil.showToast(mContext, getString(R.string.tip_password_no_change));
            return true;
        }
        return false;
    }

    private void getCheckCode(String account, String oldPassword, String password) {
        Map<String, String> params = new HashMap<>();
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_RANDOM_STR)
                .params(params)
                .build()
                .execute(new BaseCallback<UserRandomStr>(UserRandomStr.class) {
                    @Override
                    public void onResponse(ObjectResult<UserRandomStr> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            String key = SecureChatUtil.getRSAPrivateKey(coreManager.getSelf().getUserId());
                            authCode = new String(RSA.decryptFromBase64(result.getData().getUserRandomStr(), Base64.decode(key)));
                            changePassword(account, oldPassword, password);
                        } else {
                            DialogHelper.dismissProgressDialog();
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
     * 修改密码
     */
    private void changePassword(String account, String oldPassword, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);
        params.put("oldPassword", LoginPassword.encodeMd5(oldPassword));
        params.put("newPassword", LoginPassword.encodeMd5(password));
        String url;
        if (isSupportSecureChat) {
            url = coreManager.getConfig().USER_PASSWORD_UPDATE_V1;
            // SecureFlag 取出本地保存的私钥，使用新密码加密私钥，上传服务器
            String dhPrivateKey = SecureChatUtil.getDHPrivateKey(coreManager.getSelf().getUserId());
            String rsaPrivateKey = SecureChatUtil.getRSAPrivateKey(coreManager.getSelf().getUserId());
            String newDHPrivateKey = SecureChatUtil.aesEncryptDHPrivateKey(password, dhPrivateKey);
            String newRSAPrivateKey = SecureChatUtil.aesEncryptRSAPrivateKey(password, rsaPrivateKey);
            String signature = SecureChatUtil.signatureUpdateKeys(password, authCode);
            params.put("dhPrivateKey", newDHPrivateKey);
            params.put("rsaPrivateKey", newRSAPrivateKey);
            params.put("mac", signature);
        } else {
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
            url = coreManager.getConfig().USER_PASSWORD_UPDATE;
        }
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, getString(R.string.update_sccuess));
                            if (coreManager.getSelf() != null
                                    && !TextUtils.isEmpty(coreManager.getSelf().getTelephone())) {
                                UserSp.getInstance(mContext).clearUserInfo();
                                MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
                                coreManager.logout();
                                LoginHelper.broadcastLogout(mContext);
                                LoginHistoryActivity.start(ChangePasswordActivity.this);
                            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS)
            return;
        mobilePrefix = data.getStringExtra(Constants.MOBILE_PREFIX);
        tvPrefix.setText("+" + mobilePrefix);
    }
}
