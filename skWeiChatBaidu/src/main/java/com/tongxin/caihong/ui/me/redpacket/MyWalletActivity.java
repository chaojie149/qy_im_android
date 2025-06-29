package com.tongxin.caihong.ui.me.redpacket;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.webox.WeboxOpenActivity;
import com.tongxin.caihong.ui.webox.WeboxWalletActivity;

public class MyWalletActivity extends BaseActivity {
    public static void start(Context context) {
        if (WeboxHelper.ENABLE) {
            Intent starter = new Intent(context, MyWalletActivity.class);
            context.startActivity(starter);
        } else {
            context.startActivity(new Intent(context, WxPayBlance.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_wallet);
        initActionBar();
        initView();
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
        mTvTitle.setText(getString(R.string.my_purse));
    }

    private void initView() {
        findViewById(R.id.my_change).setOnClickListener(v -> {
            startActivity(new Intent(this, WxPayBlance.class));
        });
        findViewById(R.id.rlWebox).setOnClickListener(v -> {
            if (WeboxHelper.isOpened(this)) {
                WeboxWalletActivity.start(this);
            } else {
                WeboxOpenActivity.start(this);
            }
        });
        findViewById(R.id.rlWeboxBind).setOnClickListener(v -> {
            WeboxHelper.bind(this, coreManager);
        });
    }
}
