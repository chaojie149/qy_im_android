package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.other.PrivacyAgreeActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
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
 * 注册界面
 */
public class RegisterActivity extends BaseActivity {
    public static final String EXTRA_AUTH_CODE = "auth_code";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_SMS_CODE = "sms_code";
    public static final String EXTRA_INVITE_CODE = "invite_code";
    public static int isSmsRegister = 0;
    private TextView tvPrefix;
    private EditText etAccount;
    private EditText etInvitationCode;
    private String mobilePrefix = "86";
    private String thirdToken;
    private String thirdTokenType;
    private boolean privacyAgree;

    public RegisterActivity() {
        noLoginRequired();
    }

    public static void registerFromThird(Context ctx, String thirdToken, String thirdTokenType) {
        Intent intent = new Intent(ctx, RegisterActivity.class);
        intent.putExtra("thirdToken", thirdToken);
        intent.putExtra("thirdTokenType", thirdTokenType);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
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
        if (!TextUtils.isEmpty(thirdToken)) {
            // todo 绑定新账号
        }
    }

    private void initView() {
        tvPrefix = findViewById(R.id.tvPrefix);
        etAccount = findViewById(R.id.etAccount);
        UsernameHelper.initEditText(etAccount, coreManager.getConfig().registerUsername);
        etInvitationCode = findViewById(R.id.etInvitationCode);
        Button btnNext = findViewById(R.id.btnNext);
        ButtonColorChange.colorChange(mContext, btnNext);
        CheckBox cbProtocol = findViewById(R.id.cbProtocol);
        SkinUtils.Skin skin = SkinUtils.getSkin(this);
        ColorStateList colorStateList = skin.getCheckBoxColorState();
        cbProtocol.setButtonTintList(colorStateList);
        ButtonColorChange.textChange(mContext, findViewById(R.id.tvUserProtocol));
        ButtonColorChange.textChange(mContext, findViewById(R.id.tvPrivacyProtocol));

        if (coreManager.getConfig().registerUsername) {
            findViewById(R.id.llPrefix).setVisibility(View.GONE);
        }

        if (coreManager.getConfig().registerInviteCode == 1 || coreManager.getConfig().registerInviteCode == 2) {
            // 启用邀请码，
            findViewById(R.id.llInvitationCode).setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(thirdToken)) {
            // 绑定原有账号
            ButtonColorChange.textChange(mContext, findViewById(R.id.tvBindOldAccount));
            findViewById(R.id.tvBindOldAccount).setVisibility(View.VISIBLE);
        }

        tvPrefix.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, SelectPrefixActivity.class);
            startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
        });

        findViewById(R.id.tvBindOldAccount).setOnClickListener(v -> LoginActivity.bindThird(this, thirdToken, thirdTokenType));

        btnNext.setOnClickListener(view -> {
            findViewById(R.id.main_content).performClick();

            String account = etAccount.getText().toString().trim();
            String invitationCode = etInvitationCode.getText().toString().trim();
            if (checkInput(account, invitationCode)) {
                return;
            }

            next(account, invitationCode);
        });

        cbProtocol.setOnCheckedChangeListener((compoundButton, b) -> privacyAgree = b);

        findViewById(R.id.tvUserProtocol).setOnClickListener(view -> {
            if (UiUtils.isNormalClick(view) && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix)) {
                PrivacyAgreeActivity.start(mContext, false, true);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.server_not_config_protocol_address));
            }
        });

        findViewById(R.id.tvPrivacyProtocol).setOnClickListener(view -> {
            if (UiUtils.isNormalClick(view) && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix)) {
                PrivacyAgreeActivity.start(mContext, true, true);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.server_not_config_protocol_address));
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

    private void next(String account, String invitationCode) {
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
                            RegisterActivity3.bindThird(mContext,
                                    mobilePrefix,
                                    account,
                                    invitationCode,
                                    thirdToken,
                                    thirdTokenType);
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
    private boolean checkInput(String account, String invitationCode) {
        if (!privacyAgree) {
            ToastUtil.showToast(mContext, getString(R.string.tip_privacy_not_agree) + getString(R.string.and) + getString(R.string.yinsizhengce));
            return true;
        }
        if (!UsernameHelper.verify(this, account, coreManager.getConfig().registerUsername)) {
            return true;
        }
        if (coreManager.getConfig().registerInviteCode == 1 || coreManager.getConfig().registerInviteCode == 2 && TextUtils.isEmpty(invitationCode)) {
            ToastUtil.showToast(mContext, getString(R.string.tip_invite_code_empty));
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
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
