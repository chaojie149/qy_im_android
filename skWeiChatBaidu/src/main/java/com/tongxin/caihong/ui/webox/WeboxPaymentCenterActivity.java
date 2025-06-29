package com.tongxin.caihong.ui.webox;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.SkinImageView;
import com.tongxin.caihong.view.SkinTextView;

public class WeboxPaymentCenterActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webox_payment_center);
        initView();
    }

    private void initView() {
        getSupportActionBar().hide();
        SkinImageView iv_title_left = (SkinImageView) findViewById(R.id.iv_title_left);
        SkinTextView tv_title_center = (SkinTextView) findViewById(R.id.tv_title_center);
        tv_title_center.setText(getResources().getString(R.string.payment_center));
        tv_title_center.setTextColor(getResources().getColor(R.color.black));
        RelativeLayout yeepay_bind = (RelativeLayout) findViewById(R.id.yeepay_bind);
        RelativeLayout yeepay_secure = (RelativeLayout) findViewById(R.id.yeepay_secure);
        RelativeLayout bill = (RelativeLayout) findViewById(R.id.bill);

        iv_title_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        yeepay_bind.setOnClickListener(this);
        yeepay_secure.setOnClickListener(this);
        bill.setOnClickListener(this);
        findViewById(R.id.delete_cert).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bill:
                WeboxRecordActivity.start(mContext);
                break;
            case R.id.yeepay_bind:
                WeboxHelper.bind(this, coreManager);
                break;
            case R.id.yeepay_secure:
                WeboxHelper.secure(this, coreManager);
                break;
            case R.id.delete_cert:
                SelectionFrame dialog = new SelectionFrame(mContext);
                dialog.setSomething(null, getString(R.string.tip_delete_cert), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        WeboxHelper.deleteCert(mContext);
                        ToastUtil.showToast(mContext, R.string.delete_success);
                    }
                });
                dialog.show();
                break;
        }

    }
}
