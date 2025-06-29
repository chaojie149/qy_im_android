package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.MergerStatus;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 重置密码界面
 */
public class FindPwdActivity extends BaseActivity implements View.OnClickListener {
    private TextView tvPrefix;
    private EditText etAccount;
    private String mobilePrefix ="86";
    private String account;

    public FindPwdActivity() {
        noLoginRequired();
    }

    public static void start(Context ctx, String mobilePrefix, String account) {
        Intent intent = new Intent(ctx, FindPwdActivity.class);
        intent.putExtra("mobilePrefix", mobilePrefix);
        intent.putExtra("account", account);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        mobilePrefix = getIntent().getStringExtra("mobilePrefix");
        account = getIntent().getStringExtra("account");
        initActionBar();
        initView();
    }

    private void initActionBar() {
        ((MergerStatus) findViewById(R.id.mergerStatus)).hideShadow();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        tvPrefix = findViewById(R.id.tvPrefix);
        mobilePrefix = PreferenceUtils.getString(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tvPrefix.setText("+" + mobilePrefix);
        etAccount = findViewById(R.id.etAccount);
        UsernameHelper.initEditText(etAccount, coreManager.getConfig().registerUsername);
        if (!TextUtils.isEmpty(account)) {
            etAccount.setText(account);
        }
        Button btnNext = findViewById(R.id.btnNext);
        ButtonColorChange.colorChange(mContext, btnNext);

        tvPrefix.setOnClickListener(this);
        btnNext.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvPrefix:
                Intent intent = new Intent(mContext, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.btnNext:
                String account = etAccount.getText().toString().trim();
                if (checkInput(account)) {
                    return;
                }
                verifyAccount(account);
                break;
        }
    }

    private void verifyAccount(String account) {
        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);
        params.put("verifyType", "1");
        HttpUtils.get().url(coreManager.getConfig().VERIFY_TELEPHONE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            FindPwdActivity2.start(mContext, mobilePrefix, account);
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
     * 检查是否需要停止重置，
     *
     * @return 测试不合法返回true, 停止重置密码，
     */
    private boolean checkInput(String account) {
        if (!UsernameHelper.verify(this, account, coreManager.getConfig().registerUsername)) {
            return true;
        }
        return false;
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
