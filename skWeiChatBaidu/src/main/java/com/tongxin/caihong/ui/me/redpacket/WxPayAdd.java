package com.tongxin.caihong.ui.me.redpacket;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.tongxin.caihong.bean.event.EventPaySuccess;
import com.tongxin.caihong.bean.redpacket.Balance;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.me.redpacket.alipay.AlipayHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 充值
 */
public class WxPayAdd extends BaseActivity {
    private IWXAPI api;

    private List<BigDecimal> mRechargeList = new ArrayList<>();
    private List<CheckedTextView> mRechargeMoneyViewList = new ArrayList<>();

    private EditText mSelectMoneyTv;
    private int mSelectedPosition = 0;
    private String minRechargeAmountStr;
    private BigDecimal minRechargeAmount;

    public static ColorStateList getPayTextColorState() {
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
        };

        int[] colors = new int[]{
                0xff888888,
                0xffffffff
        };

        return new ColorStateList(states, colors);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wx_pay_add);

        api = WXAPIFactory.createWXAPI(this, Constants.VX_APP_ID, false);
        api.registerApp(Constants.VX_APP_ID);

        initActionBar();
        initData();
        initView();

        EventBusHelper.register(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventPaySuccess message) {
        finish();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.recharge));
    }

    private void initData() {
        minRechargeAmountStr = coreManager.getConfig().minRechargeAmount;
        minRechargeAmount = new BigDecimal(10);
        if (!TextUtils.isEmpty(minRechargeAmountStr)){
            try {
                minRechargeAmount = new BigDecimal(minRechargeAmountStr);
                if (minRechargeAmount.compareTo(BigDecimal.ZERO)<=0){
                    minRechargeAmount = new BigDecimal(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mRechargeList.add(minRechargeAmount);
        mRechargeList.add(minRechargeAmount.multiply(new BigDecimal(2)));
        mRechargeList.add(minRechargeAmount.multiply(new BigDecimal(5)));
        mRechargeList.add(minRechargeAmount.multiply(new BigDecimal(10)));
        mRechargeList.add(minRechargeAmount.multiply(new BigDecimal(20)));
        mRechargeList.add(minRechargeAmount.multiply(new BigDecimal(50)));
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ViewGroup tableLayout = findViewById(R.id.tableLayoutRechargeMoney);
        SkinUtils.Skin skin = SkinUtils.getSkin(this);
        ColorStateList highlightColorState = skin.getHighlightColorState();
        View.OnClickListener onMoneyClickListener = v -> {
            mSelectedPosition = -1;
            for (int i = 0, mRechargeMoneyViewListSize = mRechargeMoneyViewList.size(); i < mRechargeMoneyViewListSize; i++) {
                CheckedTextView textView = mRechargeMoneyViewList.get(i);
                if (textView == v) {
                    mSelectedPosition = i;
                    mSelectMoneyTv.setText(mRechargeList.get(i).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
                    textView.setChecked(true);
                } else {
                    textView.setChecked(false);
                }
            }
        };
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            ViewGroup tableRow = (ViewGroup) tableLayout.getChildAt(i);
            for (int k = 0; k < tableRow.getChildCount(); k++) {
                CheckedTextView tvMoney = tableRow.getChildAt(k).findViewById(R.id.tvRechargeMoney);
                tvMoney.setOnClickListener(onMoneyClickListener);
                tvMoney.setTextColor(WxPayAdd.getPayTextColorState());
                ViewCompat.setBackgroundTintList(tvMoney, skin.getPayColorState());
                int index = i * tableRow.getChildCount() + k;
                tvMoney.setText(mRechargeList.get(index).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " " + getString(R.string.yuan));
                mRechargeMoneyViewList.add(tvMoney);
            }
        }

        mSelectMoneyTv = findViewById(R.id.select_money_tv);
        mSelectMoneyTv.setTextColor(skin.getAccentColor());
        mSelectMoneyTv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0,
                                s.toString().indexOf(".") + 3);
                        mSelectMoneyTv.setText(s);
                        mSelectMoneyTv.setSelection(s.length());
                    }
                }

                if (!TextUtils.isEmpty(s) && s.toString().trim().substring(0, 1).equals(".")) {
                    s = "0" + s;
                    mSelectMoneyTv.setText(s);
                    mSelectMoneyTv.setSelection(1);
                }

                if (s.toString().startsWith("0")
                        && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        mSelectMoneyTv.setText(s.subSequence(0, 1));
                        mSelectMoneyTv.setSelection(1);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
/*
                try {
                    BigDecimal money = new BigDecimal(s.toString());
                    if (money.scale() > 2) {
                        mSelectMoneyTv.setText(money.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
                        return;
                    }
                } catch (Exception ignored) {
                    // 就算TextUtils.isEmpty判断了s不空，还是可能NumberFormatException: For input string: ""
                    // 看着像是存在异步修改，多线程冲突，但是没找到，
                }
*/
                if (!TextUtils.isEmpty(s)) {
                    mSelectMoneyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
                    mSelectMoneyTv.setHint(null);
                } else {
                    // invisible占着高度，
                    mSelectMoneyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    mSelectMoneyTv.setHint(R.string.need_input_money);
                }
            }
        });

        findViewById(R.id.recharge_wechat).setVisibility(coreManager.getConfig().enableWxPay ? View.VISIBLE : View.GONE);
        findViewById(R.id.recharge_wechat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (api.getWXAppSupportAPI() < Build.PAY_SUPPORTED_SDK_INT) {
                    Toast.makeText(getApplicationContext(), R.string.tip_no_wechat, Toast.LENGTH_SHORT).show();
                } else {
                    if (!checkMoney()){
                        Toast.makeText(getApplicationContext(), "低于最小充值金额,不能充值", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    recharge(getCurrentMoney());
                }
            }
        });

        findViewById(R.id.recharge_alipay).setVisibility(coreManager.getConfig().enableAliPay ? View.VISIBLE : View.GONE);
        findViewById(R.id.recharge_alipay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkMoney()){
                    Toast.makeText(getApplicationContext(), "低于最小充值金额,不能充值", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlipayHelper.recharge(WxPayAdd.this, coreManager, getCurrentMoney());
            }
        });
    }

    private String getCurrentMoney() {
        if (TextUtils.isEmpty(mSelectMoneyTv.getText())) {
            return "0";
        }
        return new BigDecimal(mSelectMoneyTv.getText().toString()).stripTrailingZeros().toPlainString();
    }

    private boolean checkMoney(){
        BigDecimal money = new BigDecimal(getCurrentMoney());
        minRechargeAmountStr = coreManager.getConfig().minRechargeAmount;
        minRechargeAmount = new BigDecimal(100);
        if (!TextUtils.isEmpty(minRechargeAmountStr)){
            try {
                minRechargeAmount = new BigDecimal(minRechargeAmountStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return money.compareTo(minRechargeAmount)>=0;
    }


    private void recharge(String money) {// 调用服务端接口，由服务端统一下单
        DialogHelper.showDefaulteMessageProgressDialog(this);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("price", money);
        params.put("payType", "2");// 支付方式 1.支付宝 2.微信
        params.put("handleType", "0");
        HttpUtils.get().url(coreManager.getConfig().VX_RECHARGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            PayReq req = new PayReq();
                            req.appId = result.getData().getAppId();
                            req.partnerId = result.getData().getPartnerId();
                            req.prepayId = result.getData().getPrepayId();
                            req.packageValue = "Sign=WXPay";
                            req.nonceStr = result.getData().getNonceStr();
                            req.timeStamp = result.getData().getTimeStamp();
                            req.sign = result.getData().getSign();
                            api.sendReq(req);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(WxPayAdd.this);
                    }
                });
    }
}
