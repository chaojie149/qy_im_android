package com.tongxin.caihong.ui.me;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.AccountSwitchHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.account.LoginActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.other.PrivacyAgreeActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.view.VerifyDialog;
import com.tongxin.caihong.view.window.WindowShowService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;

import okhttp3.Call;

public class UserDestroyActivity extends BaseActivity {
    private boolean privacyAgree;

    public static void start(Context context) {
        Intent starter = new Intent(context, UserDestroyActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_destroy);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        ((TextView) findViewById(R.id.tvTitle3)).setText(getString(R.string.tip_user_destroy_title_3, getString(R.string.app_name)));
        ((TextView) findViewById(R.id.tvSubtitle3)).setText(getString(R.string.tip_user_destroy_subtitle_3, getString(R.string.app_name)));

        CheckBox cbProtocol = findViewById(R.id.cbProtocol);
        SkinUtils.Skin skin = SkinUtils.getSkin(this);
        ColorStateList colorStateList = skin.getCheckBoxColorState();
        cbProtocol.setButtonTintList(colorStateList);
        ButtonColorChange.textChange(mContext, findViewById(R.id.tvUserDestroyProtocol));
        cbProtocol.setOnCheckedChangeListener((compoundButton, b) -> privacyAgree = b);
        findViewById(R.id.llProtocol).setOnClickListener(v -> {
            cbProtocol.toggle();
        });
        findViewById(R.id.tvUserDestroyProtocol).setOnClickListener(view -> {
            if (UiUtils.isNormalClick(view) && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix)) {
                PrivacyAgreeActivity.startForUserDestroy(mContext);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.server_not_config_user_destroy_protocol_address));
            }
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        View btnNext = findViewById(R.id.btnNext);
        ButtonColorChange.tintBackground(btnNext);
        btnNext.setOnClickListener(v -> {
            if (!privacyAgree) {
                ToastUtil.showToast(mContext, getString(R.string.tip_user_destroy_protocol_not_agree));
                return;
            }
            userDestroy();
        });
    }

    private void userDestroy() {
        VerifyDialog mVerifyDialog = new VerifyDialog(mContext);
        mVerifyDialog.setVerifyClickListener(getString(R.string.user_destroy)
                , getString(R.string.user_destroy_verify)
                , ""
                , new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void send(String str) {
                        userDestroy(str);
                    }
                });
        mVerifyDialog.setOkButton(R.string.sure);
        mVerifyDialog.setPasswordMode();
        mVerifyDialog.show();
    }

    private void userDestroy(String password) {
        HashMap<String, String> params = new HashMap<>();
        params.put("password", LoginPassword.encodeMd5(password));
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HttpUtils.get().url(coreManager.getConfig().USER_DESTROY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, R.string.user_destroy_success, Toast.LENGTH_SHORT).show();
                            quit();
                            AsyncUtils.postDelayed(UserDestroyActivity.this, Activity::finish, 200);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void quit() {
        mContext.stopService(new Intent(mContext, WindowShowService.class));
        AccountSwitchHelper.removeExistsUser(mContext, coreManager.getSelf().getUserId());
        UserSp.getInstance(mContext).clearAll();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
        coreManager.logout();
        LoginHelper.broadcastLogout(mContext);
        startActivity(new Intent(mContext, LoginActivity.class));
    }


}
