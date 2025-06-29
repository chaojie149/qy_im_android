package com.tongxin.caihong.ui.me.redpacket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.EventPaySuccess;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.redpacket.Balance;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PaySecureHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.me.redpacket.alipay.AlipayHelper;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tencent.mm.opensdk.constants.Build;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 充值
 */
public class PayJoinRoom extends BaseActivity {
    private IWXAPI api;
    private String mRoomId;
    private int days;
    private int amount;

    private List<Integer> mRechargeList = new ArrayList<>();
    private List<CheckedTextView> mRechargeMoneyViewList = new ArrayList<>();

    private EditText mSelectMoneyTv;
    private int mSelectedPosition = -1;
    public static int PAY_REQUEST = 1002;

    public static void start(Activity activity,String roomId,int days,int amount){
        Intent intent = new Intent(activity, PayJoinRoom.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("days", days);
        intent.putExtra("amount", amount);
        activity.startActivityForResult(intent,PAY_REQUEST);
    }

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
        Intent intent = getIntent();
        mRoomId = intent.getStringExtra("roomId");
        amount = intent.getIntExtra("amount",0);
        days = intent.getIntExtra("days",0);
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
        //Intent intent = new Intent();
        setResult(RESULT_OK);
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
        tvTitle.setText("付费入群");
    }

    private void initData() {
        mRechargeList.add(1);
        mRechargeList.add(2);
        mRechargeList.add(5);
        mRechargeList.add(10);
        mRechargeList.add(20);
        mRechargeList.add(50);
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
                    int roomAmount = amount*mRechargeList.get(i);
                    mSelectMoneyTv.setText(roomAmount+"");
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
                tvMoney.setTextColor(PayJoinRoom.getPayTextColorState());
                ViewCompat.setBackgroundTintList(tvMoney, skin.getPayColorState());
                int index = i * tableRow.getChildCount() + k;
                int roomDays = days*mRechargeList.get(index);
                int roomAmount = amount*mRechargeList.get(index);
                String text = roomAmount + "元 " + roomDays+"天";
                tvMoney.setText(text);
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
                    if (mSelectedPosition==-1){
                        Toast.makeText(getApplicationContext(), "请选择支付金额", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    recharge(getCurrentMoney(),mRechargeList.get(mSelectedPosition));
                }
            }
        });

        findViewById(R.id.recharge_alipay).setVisibility(coreManager.getConfig().enableAliPay ? View.VISIBLE : View.GONE);
        findViewById(R.id.recharge_alipay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedPosition==-1){
                    Toast.makeText(getApplicationContext(), "请选择支付金额", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlipayHelper.payForJoin(PayJoinRoom.this, coreManager, getCurrentMoney(),mRoomId,mRechargeList.get(mSelectedPosition));
            }
        });
        findViewById(R.id.recharge_purse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedPosition==-1){
                    Toast.makeText(getApplicationContext(), "请选择支付金额", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean hasPayPassword = PreferenceUtils.getBoolean(PayJoinRoom.this, Constants.IS_PAY_PASSWORD_SET + coreManager.getSelf().getUserId(), true);
                if (!hasPayPassword) {
                    ToastUtil.showToast(PayJoinRoom.this, R.string.tip_no_pay_password);
                    Intent intent = new Intent(PayJoinRoom.this, ChangePayPasswordActivity.class);
                    startActivity(intent);
                    return;
                }
                //AlipayHelper.payForJoin(PayJoinRoom.this, coreManager, getCurrentMoney(),mRoomId,mRechargeList.get(mSelectedPosition));
                PaySecureHelper.inputPayPassword(PayJoinRoom.this, getString(R.string.chat_redpacket), getCurrentMoney(), password -> {
                    balancePay(getCurrentMoney(),mRoomId,mRechargeList.get(mSelectedPosition), password);
                });
            }
        });
    }

    private void balancePay(String currentMoney, String mRoomId, Integer periods, String payPassword) {
        if (!coreManager.isLogin()) {
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap();
        params.put("roomId", mRoomId);
        params.put("price", currentMoney);
        params.put("periods", periods+"");
        PaySecureHelper.generateParam(
                this, payPassword, params,
                "" + mRoomId + currentMoney + periods,
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_pay_secure_place_holder, t.getMessage()));
                }, (p, code) -> {
                    HttpUtils.get().url(coreManager.getConfig().BALANCEPAYFORJOIN)
                            .params(p)
                            .build()
                            .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                                @Override
                                public void onResponse(ObjectResult<MucRoom> result) {
                                    DialogHelper.dismissProgressDialog();
                                    if (Result.checkSuccess(mContext, result)) {
                                        MucRoom room = result.getData();
                                        String objectId = room.getId();
                                        result(room);
                                    } else if (result.getResultCode() == 104001) {
                                        // 余额不足
                                        SelectionFrame selectionFrame = new SelectionFrame(mContext);
                                        selectionFrame.setSomething(getString(R.string.title_balance_not_enough), getString(R.string.tip_is_pay),
                                                getString(R.string.cancel), getString(R.string.recharge), new SelectionFrame.OnSelectionFrameClickListener() {
                                                    @Override
                                                    public void cancelClick() {
                                                    }

                                                    @Override
                                                    public void confirmClick() {
                                                        startActivity(new Intent(mContext, WxPayBlance.class));
                                                    }
                                                });
                                        selectionFrame.show();
                                    }
                                }

                                @Override
                                public void onError(Call call, Exception e) {
                                    DialogHelper.dismissProgressDialog();
                                    ToastUtil.showNetError(mContext, e);
                                }
                            });
                });
    }

    /**
     * 支付成功
     * @param room
     */
    private void result(MucRoom room) {
        ToastUtil.showToast(this, "支付成功");
        EventBus.getDefault().post(new EventPaySuccess());
    }

    private String getCurrentMoney() {
        if (TextUtils.isEmpty(mSelectMoneyTv.getText())) {
            return "0";
        }
        return new BigDecimal(mSelectMoneyTv.getText().toString()).stripTrailingZeros().toPlainString();
    }

    private void interMucChat(String roomJid, String roomName) {
        Intent intent = new Intent(mContext, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);

        // 更新群组页面
        MucgroupUpdateUtil.broadcastUpdateUi(mContext);
    }

    private void recharge(String money,int periods) {// 调用服务端接口，由服务端统一下单
        DialogHelper.showDefaulteMessageProgressDialog(this);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("price", money);
        params.put("payType", "2");// 支付方式 1.支付宝 2.微信
        params.put("handleType", "0");
        params.put("roomId", mRoomId);
        params.put("periods", periods+"");
        HttpUtils.get().url(coreManager.getConfig().PAY_FOR_JOIN)
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
                        ToastUtil.showErrorNet(PayJoinRoom.this);
                    }
                });
    }
}
