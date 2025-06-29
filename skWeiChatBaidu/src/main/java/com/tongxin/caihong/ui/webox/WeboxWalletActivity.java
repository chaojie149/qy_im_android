package com.tongxin.caihong.ui.webox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

public class WeboxWalletActivity extends BaseActivity {
    private TextView mBalanceTv;
    private TextView mRechargeTv;
    private TextView mWithdrawTv;

    public static void start(Context context) {
        Intent starter = new Intent(context, WeboxWalletActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webox);
        initActionBar();
        initView();
        EventBusHelper.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.my_yeepay));
        ImageView mImageView = findViewById(R.id.iv_title_right);
        mImageView.setImageDrawable(getResources().getDrawable(R.mipmap.navigation));
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 访问接口 获取记录
                Intent intent = new Intent(mContext, WeboxPaymentCenterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initView() {
        mBalanceTv = (TextView) findViewById(R.id.myblance);
        mRechargeTv = (TextView) findViewById(R.id.chongzhi);
        mWithdrawTv = (TextView) findViewById(R.id.quxian);
        ButtonColorChange.rechargeChange(this, mWithdrawTv, R.drawable.recharge_icon);
        ButtonColorChange.rechargeChange(this, mRechargeTv, R.drawable.chongzhi_icon);
        mWithdrawTv.setTextColor(SkinUtils.getSkin(this).getAccentColor());

        mRechargeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WeboxRechargeActivity.start(mContext);
            }
        });

        mWithdrawTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WeboxWithdrawActivity.start(mContext);
            }
        });
    }

    private void initData() {
        WeboxHelper.query(this, coreManager, w -> {
            mBalanceTv.setText("￥" + Money.fromCent(w.getBalance()));
        });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventWeboxRechargeSuccess message) {
        mRechargeTv.postDelayed(this::initData, 1000);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, EventNotifyByTag.Withdraw)) {
            initData();
        }
    }
}
