package com.tongxin.caihong.ui.me.redpacket.scan;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.redpacket.ScanWithDrawSelectType;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PaySecureHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.me.redpacket.ChangePayPasswordActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.Money;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 扫码提现
 */
public class ScanWithdrawActivity extends BaseActivity {
    private static final int SELECT_TYPE_REQUEST_CODE = 0x01;
    public static String amount;// 提现金额 单位:元
    private EditText mMentionMoneyEdit;
    private TextView mBalanceTv;
    private TextView mAllMentionTv;
    private ImageView mTypeIv;
    private TextView mTypeTv;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private ScanWithDrawSelectType drawSelectType;

    public static void start(Context context, String money) {
        Intent intent = new Intent(context, ScanWithdrawActivity.class);
        intent.putExtra("money", money);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_withdraw);

        initActionbar();
        initView();
        intEvent();

        checkHasPayPassword();
    }

    private void checkHasPayPassword() {
        boolean hasPayPassword = PreferenceUtils.getBoolean(this, Constants.IS_PAY_PASSWORD_SET + coreManager.getSelf().getUserId(), true);
        if (!hasPayPassword) {
            ToastUtil.showToast(this, R.string.tip_no_pay_password);
            Intent intent = new Intent(this, ChangePayPasswordActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initActionbar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.withdraw));
    }

    private void initView() {
        mMentionMoneyEdit = (EditText) findViewById(R.id.tixianmoney);
        String money = getIntent().getStringExtra("money");
        if (!TextUtils.isEmpty(money)) {
            mMentionMoneyEdit.setText(money);
        }
        mBalanceTv = (TextView) findViewById(R.id.blance_weixin);
        mBalanceTv.setText(decimalFormat.format(coreManager.getSelf().getBalance()));
        mAllMentionTv = (TextView) findViewById(R.id.tixianall);
        mTypeIv = findViewById(R.id.type_iv);
        mTypeTv = findViewById(R.id.type_tv);
        ButtonColorChange.colorChange(mContext, findViewById(R.id.sure_withdraw_btn));
    }

    private void intEvent() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mMentionMoneyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        mMentionMoneyEdit.setText(s);
                        mMentionMoneyEdit.setSelection(s.length());
                    }
                }

                if (!TextUtils.isEmpty(s) && s.toString().trim().substring(0, 1).equals(".")) {
                    s = "0" + s;
                    mMentionMoneyEdit.setText(s);
                    mMentionMoneyEdit.setSelection(1);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        mMentionMoneyEdit.setText(s.subSequence(0, 1));
                        mMentionMoneyEdit.setSelection(1);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mAllMentionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double money = coreManager.getSelf().getBalance();
                if (money < 1) {
                    DialogHelper.tip(mContext, getString(R.string.tip_withdraw_too_little));
                } else {
                    mMentionMoneyEdit.setText(String.valueOf(money));
                }
            }
        });

        findViewById(R.id.ll_select).setOnClickListener(v -> startActivityForResult(new Intent(mContext, ScanWithdrawListActivity.class), SELECT_TYPE_REQUEST_CODE));

        findViewById(R.id.sure_withdraw_btn).setOnClickListener(v -> {
            String moneyStr = mMentionMoneyEdit.getText().toString();
            if (checkMoney(moneyStr)) {
                amount = Money.fromYuan(moneyStr);
                if (drawSelectType != null) {
                    PaySecureHelper.inputPayPassword(mContext, mContext.getString(R.string.withdraw), amount, password -> {
                        withdraw(amount, password);
                    });
                } else {
                    ToastUtil.showToast(mContext, getString(R.string.please_select_withdraw_type));
                }
            }
        });
    }

    /**
     * 提交提现申请
     */
    public void withdraw(String money, String password) {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        final Map<String, String> params = new HashMap<>();
        params.put("amount", money);
        params.put("withdrawAccountId", drawSelectType.getId());

        PaySecureHelper.generateParam(
                mContext, password, params,
                money + drawSelectType.getId(),
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_pay_secure_place_holder, t.getMessage()));
                }, (p, code) -> HttpUtils.post().url(coreManager.getConfig().MANUAL_PAY_WITHDRAW)
                        .params(p)
                        .build()
                        .execute(new BaseCallback<Void>(Void.class) {

                            @Override
                            public void onResponse(ObjectResult<Void> result) {
                                DialogHelper.dismissProgressDialog();
                                if (Result.checkSuccess(mContext, result)) {
                                    ToastUtil.showToast(mContext, R.string.wait_server_notify);
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                DialogHelper.dismissProgressDialog();
                                ToastUtil.showErrorData(mContext);
                            }
                        }));
    }

    private boolean checkMoney(String moneyStr) {
        if (TextUtils.isEmpty(moneyStr)) {
            DialogHelper.tip(mContext, getString(R.string.tip_withdraw_empty));
        } else {
            if (Double.valueOf(moneyStr) < 1) {
                DialogHelper.tip(mContext, getString(R.string.tip_withdraw_too_little));
            } else if (Double.valueOf(moneyStr) > coreManager.getSelf().getBalance()) {
                DialogHelper.tip(mContext, getString(R.string.tip_balance_not_enough));
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_TYPE_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null) {
            String str = data.getStringExtra("drawSelectType");
            String name = data.getStringExtra("name");
            ScanWithDrawSelectType drawSelectType = JSON.parseObject(str, ScanWithDrawSelectType.class);
            if (drawSelectType != null) {
                this.drawSelectType = drawSelectType;
                mTypeIv.setVisibility(View.GONE);
//                if (drawSelectType.getType() == 1) {
//                    mTypeIv.setImageResource(R.mipmap.ic_alipay_small);
                    mTypeTv.setText(name+":"+drawSelectType.getAliPayAccount());
//                } else {
//                    mTypeIv.setImageResource(R.mipmap.ic_band_small);
//                    mTypeTv.setText(drawSelectType.getBankName() + "(" + drawSelectType.getBankCardNo() + ")");
//                }
            }
        }
    }
}
