package com.tongxin.caihong.ui.webox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.bean.Code;
import com.tongxin.caihong.bean.webox.WeboxCreate;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.filter.RegexInputFilter;
import com.ehking.sdk.wepay.interfaces.WalletPay;
import com.ehking.sdk.wepay.net.bean.AuthType;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.account.SelectPrefixActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;

public class WeboxOpenActivity extends BaseActivity {
    private Button btn_getCode;
    private ImageView mImageCodeIv;
    private EditText mAuthCodeEdit;
    private EditText mImageCodeEdit;
    private EditText etRealName;
    private EditText etIDCard;
    private EditText etPhone;
    private EditText etNickname;
    private Spinner spProfession;
    private String selectedProfession;
    private TextView tv_prefix;
    private String mobilePrefix ="86";
    private int reckonTime = 60;
    private Handler mReckonHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0x1) {
                btn_getCode.setText("(" + reckonTime + ")");
                reckonTime--;
                if (reckonTime < 0) {
                    mReckonHandler.sendEmptyMessage(0x2);
                } else {
                    mReckonHandler.sendEmptyMessageDelayed(0x1, 1000);
                }
            } else if (msg.what == 0x2) {
                // 60秒结束
                btn_getCode.setText(getString(R.string.send));
                btn_getCode.setEnabled(true);
                reckonTime = 60;
            }
        }
    };

    public static void start(Context context) {
        Intent starter = new Intent(context, WeboxOpenActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webox_open);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mAuthCodeEdit = findViewById(R.id.auth_code_edit);
        mImageCodeEdit = findViewById(R.id.image_tv);
        mImageCodeIv = findViewById(R.id.image_iv);
        etRealName = findViewById(R.id.etRealName);
        // 只能输入中文，
        etRealName.setFilters(new InputFilter[]{new RegexInputFilter("[\\u4e00-\\u9fa5]*")});
        etIDCard = findViewById(R.id.etIDCard);
        // 18位，只能输入数字或者X，
        etIDCard.setFilters(new InputFilter[]{new InputFilter.LengthFilter(18), new RegexInputFilter("[0-9xX]*")});
        etPhone = findViewById(R.id.etPhone);
        etNickname = findViewById(R.id.etNickname);
        etNickname.setText(coreManager.getSelf().getUserId());
        spProfession = findViewById(R.id.spProfession);
        selectedProfession = "A";
        // 11位，只能输入数字，
        etPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11), new RegexInputFilter("[0-9]*")});
        Button btnOpen = (Button) findViewById(R.id.btnOpen);
        ButtonColorChange.colorChange(this, btnOpen);
        btnOpen.setOnClickListener(v -> {
            if (checkInput()) {
                return;
            }
            openAccount(etRealName.getText().toString(), etIDCard.getText().toString(), etPhone.getText().toString(), etNickname.getText().toString(), selectedProfession);
        });
        btn_getCode = (Button) findViewById(R.id.send_again_btn);
        ButtonColorChange.colorChange(this, btn_getCode);
        btn_getCode.setOnClickListener(v -> {
            // 获取验证码
            String phoneNumber = etPhone.getText().toString().trim();
            String imagecode = mImageCodeEdit.getText().toString().trim();
            if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(imagecode)) {
                ToastUtil.showToast(mContext, getString(R.string.tip_phone_number_verification_code_empty));
                return;
            }
            verifyTelephone(phoneNumber, imagecode);
        });
        View mRefreshIv = (ImageView) findViewById(R.id.image_iv_refresh);
        mRefreshIv.setOnClickListener(v -> {
            if (TextUtils.isEmpty(etPhone.getText().toString())) {
                ToastUtil.showToast(this, getString(R.string.tip_phone_number_empty_request_verification_code));
            } else {
                requestImageCode();
            }
        });
        etPhone.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // 手机号输入完成后自动刷新验证码，
                    // 只在移开焦点，也就是点击其他EditText时调用，
                    requestImageCode();
                }
            }
        });
        tv_prefix = (TextView) findViewById(R.id.tv_prefix);
        tv_prefix.setOnClickListener(v -> {
            // 选择国家区号
            Intent intent = new Intent(this, SelectPrefixActivity.class);
            startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
        });
        mobilePrefix = PreferenceUtils.getString(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tv_prefix.setText("+" + mobilePrefix);

        spProfession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProfession = String.valueOf((char) ('A' + position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedProfession = "";
            }
        });
    }

    /**
     * 请求图形验证码
     */
    private void requestImageCode() {
        Map<String, String> params = new HashMap<>();
        params.put("telephone", mobilePrefix + etPhone.getText().toString().trim());
        String url = HttpUtils.get().url(coreManager.getConfig().USER_GETCODE_IMAGE)
                .params(params)
                .buildUrl();
        ImageLoadHelper.loadBitmapWithoutCache(
                mContext,
                url,
                b -> {
                    mImageCodeIv.setImageBitmap(b);
                }, e -> {
                    Toast.makeText(mContext, R.string.tip_verification_code_load_failed, Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * 请求验证码
     */
    private void verifyTelephone(String phoneNumber, String imageCode) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        String language = Locale.getDefault().getLanguage();
        params.put("language", language);
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", phoneNumber);
        params.put("imgCode", imageCode);

        HttpUtils.get().url(coreManager.getConfig().YOP_OPEN_SEND_AUTH_CODE)
                .params(params)
                .build()
                .execute(new BaseCallback<Code>(Code.class) {
                    @Override
                    public void onResponse(ObjectResult<Code> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, R.string.verification_code_send_success, Toast.LENGTH_SHORT).show();
                            btn_getCode.setEnabled(false);
                            // 开始计时
                            mReckonHandler.sendEmptyMessage(0x1);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initWeboxParams(HashMap<String, String> params) {
        params.put("version", "3.0");
        long time = System.currentTimeMillis();
        params.put("requestId", String.valueOf(time));
        params.put("merchantUserId", String.valueOf(time));
        params.put("merchantId", "890000595");
    }

    private void openAccount(String realName, String idCard, String phone, String nickName, String profession) {
        HashMap<String, String> params = new HashMap<>();
        initWeboxParams(params);
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("smsCode", mAuthCodeEdit.getText().toString());
        params.put("name", realName);
        params.put("certificateNo", idCard);
        params.put("mobile", phone);
        params.put("nickName", nickName);
        params.put("profession", profession);
        params.put("mac", NetUtils.getMacAddress(getApplicationContext()));

        HttpUtils.get().url(coreManager.getConfig().WEBOX_OPEN_ACCOUNT)
                .params(params)
                .build()
                .execute(new BaseCallback<WeboxCreate>(WeboxCreate.class) {
                    @Override
                    public void onResponse(ObjectResult<WeboxCreate> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, R.string.webox_open_success);
                            WeboxCreate walletCreate = result.getData();
                            if (walletCreate == null || walletCreate.getWalletId() == null) {
                                ToastUtil.showToast(mContext, R.string.tip_server_error);
                                return;
                            }
                            WeboxHelper.saveWalletId(mContext, walletCreate.getWalletId());
                            if (!TextUtils.isEmpty(walletCreate.getSecretKey())) {
                                //开户成功后执行自动安装证书业务
                                WalletPay.Companion.getInstance().destroy();
                                WalletPay walletPay = WalletPay.Companion.getInstance();
                                walletPay.setEnvironment(WeboxHelper.environment);
                                walletPay.init(WeboxOpenActivity.this);
                                walletPay.setWalletPayCallback((s, s1, s2) -> {
                                    finish();
                                });
                                walletPay.evoke(WeboxHelper.merchantId, walletCreate.getWalletId(), walletCreate.getSecretKey(), AuthType.AUTO_CHECK_CER.name());
                            } else {
                                // 服务器是老版，开户后没有安装证书环节，直接结束，用钱时会要求安装证书，
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private boolean checkInput() {
        if (TextUtils.isEmpty(etRealName.getText())) {
            ToastUtil.showToast(mContext, R.string.input_real_name);
            return true;
        }
        if (TextUtils.isEmpty(etIDCard.getText())) {
            ToastUtil.showToast(mContext, R.string.input_id_card);
            return true;
        }
        if (TextUtils.isEmpty(etPhone.getText())) {
            ToastUtil.showToast(mContext, R.string.hint_input_phone_number);
            return true;
        }
        if (TextUtils.isEmpty(etNickname.getText())) {
            ToastUtil.showToast(mContext, etNickname.getHint().toString());
            return true;
        }
        if (TextUtils.isEmpty(selectedProfession)) {
            ToastUtil.showToast(mContext, R.string.please_input_profession);
            return true;
        }
/*
        String authCode = mAuthCodeEdit.getText().toString().trim();
        if (TextUtils.isEmpty(authCode)) {
            Toast.makeText(this, getString(R.string.input_message_code), Toast.LENGTH_SHORT).show();
            return true;
        }
*/
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        mobilePrefix = data.getStringExtra(Constants.MOBILE_PREFIX);
        tv_prefix.setText("+" + mobilePrefix);
        // 图形验证码可能因区号失效，
        // 请求图形验证码
        if (!TextUtils.isEmpty(etPhone.getText().toString())) {
            requestImageCode();
        }
    }
}
