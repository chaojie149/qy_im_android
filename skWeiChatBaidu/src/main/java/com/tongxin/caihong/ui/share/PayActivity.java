package com.tongxin.caihong.ui.share;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.OrderInfo;
import com.tongxin.caihong.bean.SKPayBean;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.PayDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 开放平台拉起的支付页面
 */
public class PayActivity extends BaseActivity {
    private String mShareContent;
    private SKPayBean mSKPayBean;

    private boolean isNeedExecuteLogin;

    public PayActivity() {
        noLoginRequired();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_pay);

        // 已进入授权界面
        ShareConstant.IS_SHARE_P_COME = true;

        mShareContent = getIntent().getStringExtra(ShareConstant.EXTRA_SHARE_CONTENT);
        if (TextUtils.isEmpty(mShareContent)) {// 外部跳转进入
            mShareContent = ShareConstant.ShareContent;
        } else {// 数据下载页面进入
            ShareConstant.ShareContent = mShareContent;
        }
        mSKPayBean = JSON.parseObject(mShareContent, SKPayBean.class);

        // 判断本地登录状态
        int userStatus = LoginHelper.prepareUser(mContext, coreManager);
        switch (userStatus) {
            case LoginHelper.STATUS_USER_FULL:
            case LoginHelper.STATUS_USER_NO_UPDATE:
            case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
                boolean isConflict = PreferenceUtils.getBoolean(this, Constants.LOGIN_CONFLICT, false);
                if (isConflict) {
                    isNeedExecuteLogin = true;
                }
                break;
            case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                isNeedExecuteLogin = true;
                break;
            case LoginHelper.STATUS_NO_USER:
            default:
                isNeedExecuteLogin = true;
        }

        if (isNeedExecuteLogin) {// 需要先执行登录操作
            startActivity(new Intent(mContext, ShareLoginActivity.class));
            finish();
            return;
        }

        initActionBar();
        generateOrder();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        TextView mTvTitleLeft = findViewById(R.id.tv_title_left);
        mTvTitleLeft.setText(getString(R.string.close));
        mTvTitleLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void generateOrder() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("appid", mSKPayBean.getAppId());
        params.put("trade_type", "APP");
        params.put("description", mSKPayBean.getDescribe());
        params.put("amount", String.valueOf(mSKPayBean.getMoney()));

        // 获取订单信息
        HttpUtils.get().url(coreManager.getConfig().PAY_UNIFIED_ORDER)
                .params(params)
                .build()
                .execute(new BaseCallback<OrderInfo>(OrderInfo.class) {

                    @Override
                    public void onResponse(ObjectResult<OrderInfo> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            OrderInfo orderInfo = result.getData();
                            getOrderInfo(orderInfo.getPrepayId(), orderInfo.getSign());
                        } else {
                            DialogHelper.dismissProgressDialog();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        // 网络异常
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void getOrderInfo(String prepayId, String sign) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("appId", mSKPayBean.getAppId());
        params.put("prepayId", prepayId);
        params.put("sign", sign);

        // 获取订单信息
        HttpUtils.get().url(coreManager.getConfig().PAY_GET_ORDER_INFO)
                .params(params)
                .build()
                .execute(new BaseCallback<OrderInfo>(OrderInfo.class) {

                    @Override
                    public void onResponse(ObjectResult<OrderInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            PayDialog payDialog = new PayDialog(mContext, mSKPayBean.getAppId(), prepayId, sign, result.getData(), mSKPayBean, new PayDialog.PayResultListener() {
                                @Override
                                public void payResult(String result) {
                                    payResults();
                                }
                            });
                            payDialog.show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        // 网络异常
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void payResults() {
        // 这个action要和分享sdk接收的广播action相同，不能直接改，
        Intent intent = new Intent("android.intent.action.SK_Result");
        intent.putExtra(ShareConstant.EXTRA_RESULT_TYPE, 1);
        sendBroadcast(intent);
        if (DeviceInfoUtil.isOppoRom()) {
            // 调试发现OPPO手机被调起后当前界面不会自动回到后台，手动调一下
            moveTaskToBack(true);
        }
        finish();
    }
}
