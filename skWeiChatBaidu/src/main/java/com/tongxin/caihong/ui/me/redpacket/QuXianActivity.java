package com.tongxin.caihong.ui.me.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PaySecureHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.redpacket.alipay.AlipayHelper;
import com.tongxin.caihong.ui.me.redpacket.scan.ScanWithdrawActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.view.NoDoubleClickListener;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class QuXianActivity extends BaseActivity {
    public static String amount;// 提现金额 单位:元
    private IWXAPI api;
    private EditText mMentionMoneyEdit;
    private TextView mBalanceTv;
    private TextView mAllMentionTv;
    private TextView mSureMentionTv;
    private TextView tvAlipay;
    private TextView mScanWithdrawTv;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qu_xian);

        api = WXAPIFactory.createWXAPI(QuXianActivity.this, Constants.VX_APP_ID, false);
        api.registerApp(Constants.VX_APP_ID);

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
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.withdraw));
    }

    private void initView() {
        TextView tvWithdrawCost = findViewById(R.id.tvWithdrawCost);
        tvWithdrawCost.setText(getString(R.string.tip_withdraw_cost_place_holder,
                coreManager.getConfig().myChangeWithdrawRate)
        );
        mMentionMoneyEdit = (EditText) findViewById(R.id.tixianmoney);
        mBalanceTv = (TextView) findViewById(R.id.blance_weixin);
        mBalanceTv.setText(decimalFormat.format(coreManager.getSelf().getBalance()));
        mAllMentionTv = (TextView) findViewById(R.id.tixianall);
        mSureMentionTv = (TextView) findViewById(R.id.tixian);
        tvAlipay = (TextView) findViewById(R.id.withdraw_alipay);
        mScanWithdrawTv = (TextView) findViewById(R.id.withdraw_back);
        if (coreManager.getConfig().isOpenAuditPay) {
            mScanWithdrawTv.setVisibility(View.VISIBLE);
        }
        if (coreManager.getConfig().isOpenManualPay) {
            mScanWithdrawTv.setVisibility(View.VISIBLE);
        }
        if (coreManager.getConfig().isOpenAutoPay){
            findViewById(R.id.withdraw_alipay_ll).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.withdraw_alipay_ll).setVisibility(View.GONE);
        }

        if (coreManager.getConfig().isOpenWXPay){
            findViewById(R.id.tixian_ll).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.tixian_ll).setVisibility(View.GONE);
        }
        findViewById(R.id.tixian_ll).setVisibility(coreManager.getConfig().enableWxPay ? View.VISIBLE : View.GONE);
        findViewById(R.id.withdraw_alipay_ll).setVisibility(coreManager.getConfig().enableAliPay ? View.VISIBLE : View.GONE);

        AppConfig appConfig = CoreManager.requireConfig(this);
        //2023.11.6修改提现注释
        //String description = getString(R.string.tip_withdraw_cost, appConfig.myChangeWithdrawRate, appConfig.myChangeWithdrawBase, appConfig.minWithdrawAmount);
        String description ="尊敬的用户您好：固友测试期间，提现金额最低为100元，24～48小时内到账，系统自动计算且扣除手续费！如有疑问，请联系客服公众号！";
        tvWithdrawCost.setText(description);
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
                    DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_too_little));
                } else {
                    mMentionMoneyEdit.setText(String.valueOf(money));
                }
            }
        });

        findViewById(R.id.tixian_ll).setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (checkMoney(moneyStr)) {
                    amount = Money.fromYuan(moneyStr);

                    SendAuth.Req req = new SendAuth.Req();
                    req.scope = "snsapi_userinfo";
                    req.state = "wechat_sdk_demo_test";
                    api.sendReq(req);
                }
            }
        });

        findViewById(R.id.withdraw_alipay_ll).setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (checkMoney(moneyStr)) {
                    amount = Money.fromYuan(moneyStr);
                    PaySecureHelper.inputPayPassword(mContext, getString(R.string.withdraw), amount, password -> {
                        AlipayHelper.auth(QuXianActivity.this, coreManager, password, userId -> {
                            AlipayHelper.withdraw(QuXianActivity.this, coreManager, amount, password);
                        });
                    });
                }
            }
        });
        mScanWithdrawTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanWithdrawActivity.start(mContext, mMentionMoneyEdit.getText().toString());
            }
        });
    }

    private boolean checkMoney(String moneyStr) {
        if (TextUtils.isEmpty(moneyStr)) {
            DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_empty));
        } else {
            if (Double.valueOf(moneyStr) < 1) {
                DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_withdraw_too_little));
            } else if (Double.valueOf(moneyStr) > coreManager.getSelf().getBalance()) {
                DialogHelper.tip(QuXianActivity.this, getString(R.string.tip_balance_not_enough));
            } else {// 获取用户code
                return true;
            }
        }
        return false;
    }
}
