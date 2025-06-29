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
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.MergerStatus;
import com.tongxin.caihong.view.verificationcodejavademo.widget.BlockPuzzleDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;

/**
 * 重置密码-填写短信验证码界面
 */
public class FindPwdActivity2 extends BaseActivity implements View.OnClickListener {
    private EditText etSMSCode;
    private TextView tvCountdownReSend;
    private String mobilePrefix ="86";
    private String account;
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
                tvCountdownReSend.setOnClickListener(FindPwdActivity2.this);
                reckonTime = 60;
            }
        }
    };

    public FindPwdActivity2() {
        noLoginRequired();
    }

    public static void start(Context ctx, String mobilePrefix, String account) {
        Intent intent = new Intent(ctx, FindPwdActivity2.class);
        intent.putExtra("mobilePrefix", mobilePrefix);
        intent.putExtra("account", account);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password2);
        mobilePrefix = getIntent().getStringExtra("mobilePrefix");
        account = getIntent().getStringExtra("account");
        initActionBar();
        initView();
        // 进入页面，自动获取短信验证码
        smsCheck();
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
        params.put("isRegister", String.valueOf(0));

        HttpUtils.get().url(coreManager.getConfig().VERIFY_SMS_CODE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            FindPwdActivity3.start(mContext, mobilePrefix, account, smsCode);
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
