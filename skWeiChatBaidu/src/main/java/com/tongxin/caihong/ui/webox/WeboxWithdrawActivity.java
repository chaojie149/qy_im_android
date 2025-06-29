package com.tongxin.caihong.ui.webox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;

import java.text.DecimalFormat;

public class WeboxWithdrawActivity extends BaseActivity {
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private EditText mMentionMoneyEdit;
    private TextView mBalanceTv;
    private TextView mAllMentionTv;
    private TextView mSureMentionTv;

    public static void start(Context context) {
        Intent starter = new Intent(context, WeboxWithdrawActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webox_withdraw);

        initActionbar();
        initView();
        intEvent();
    }

    private void initActionbar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.withdraw));
    }

    private void initView() {
        mMentionMoneyEdit = findViewById(R.id.tixianmoney);
        mBalanceTv = findViewById(R.id.blance_weixin);
        mBalanceTv.setText(decimalFormat.format(coreManager.getSelf().getBalance()));
        mAllMentionTv = findViewById(R.id.tixianall);
        mSureMentionTv = findViewById(R.id.tixian);
        ButtonColorChange.rechargeChange(this, mSureMentionTv, R.drawable.recharge_icon);
        mSureMentionTv.setTextColor(SkinUtils.getSkin(this).getAccentColor());
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

                if (!TextUtils.isEmpty(s) && s.toString().trim().startsWith(".")) {
                    s = "0" + s;
                    mMentionMoneyEdit.setText(s);
                    mMentionMoneyEdit.setSelection(1);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().startsWith(".", 1)) {
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
                    DialogHelper.tip(WeboxWithdrawActivity.this, getString(R.string.tip_withdraw_too_little));
                } else {
                    mMentionMoneyEdit.setText(String.valueOf((int) money));
                }
            }
        });

        mSureMentionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String moneyStr = mMentionMoneyEdit.getText().toString();
                if (checkMoney(moneyStr)) {
                    WeboxHelper.withdraw(WeboxWithdrawActivity.this, coreManager, moneyStr, getType());
                }
            }
        });
    }

    private String getType() {
        return "0";
    }

    private boolean checkMoney(String moneyStr) {
        if (TextUtils.isEmpty(moneyStr)) {
            DialogHelper.tip(WeboxWithdrawActivity.this, getString(R.string.tip_withdraw_empty));
        } else {
            if (!BuildConfig.DEBUG && Double.valueOf(moneyStr) < 1) {
                DialogHelper.tip(WeboxWithdrawActivity.this, getString(R.string.tip_withdraw_too_little));
            } else {// 获取用户code
                return true;
            }
        }
        return false;
    }
}
