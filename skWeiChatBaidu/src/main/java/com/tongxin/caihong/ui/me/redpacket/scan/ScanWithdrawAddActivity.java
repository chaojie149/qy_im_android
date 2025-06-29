package com.tongxin.caihong.ui.me.redpacket.scan;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.redpacket.ScanRecharge;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 添加提现账号
 */
public class ScanWithdrawAddActivity extends BaseActivity {
    private int mAddType;
    private EditText mAlipayNameEdit, mAlipayAccount;
    private EditText mBandCardOwnerNameEdit, mBandCardAccountEdit, mBandCardNameEdit, mBandCardSonNameEdit, mRemarkEdit;

    public static void start(Context context, int type, String title) {
        Intent intent = new Intent(context, ScanWithdrawAddActivity.class);
        intent.putExtra("add_type", type);
        intent.putExtra("title", title);
        context.startActivity(intent);
    }

    public static void start(Context context, int type, String title, String content, String name,String id) {
        Intent intent = new Intent(context, ScanWithdrawAddActivity.class);
        intent.putExtra("add_type", type);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("name", name);
        intent.putExtra("id", id);
        context.startActivity(intent);
    }

    private String content,name,id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_withdraw_add);
        mAddType = getIntent().getIntExtra("add_type", 1);// 1 支付宝 2 银行卡
        String title = getIntent().getStringExtra("title");// 1 支付宝 2 银行卡
        content = getIntent().getStringExtra("content");// 1 支付宝 2 银行卡
        name = getIntent().getStringExtra("name");// 1 支付宝 2 银行卡
        id = getIntent().getStringExtra("id");// 1 支付宝 2 银行卡
        TextView mTvTitle = findViewById(R.id.tv_title_center);
        mTvTitle.setText(title);
        initActionbar();
        initView();
        intEvent();
        mAlipayAccount.setText(content);
        mAlipayNameEdit.setText(name);
        mAlipayAccount.setSelection(content != null ? content.length() : 0);
        mAlipayNameEdit.setSelection(name != null ? name.length() : 0);
        if (content != null) {
            ((Button) findViewById(R.id.sure_band_btn)).setText(getString(R.string.transfer_modify));
        }
    }

    private void initActionbar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView mTvTitle = findViewById(R.id.tv_title_center);
//        mTvTitle.setText(mAddType == 1 ? getString(R.string.select_withdraw_add_alipay_account) : getString(R.string.select_withdraw_add_band_card_account));
    }

    private void initView() {
        findViewById(mAddType == 1 ? R.id.ll1 : R.id.ll1).setVisibility(View.VISIBLE);
        ButtonColorChange.colorChange(mContext, findViewById(R.id.sure_band_btn));
        mAlipayNameEdit = findViewById(R.id.alipay_name_et);
        mAlipayAccount = findViewById(R.id.alipay_account_et);
        mBandCardOwnerNameEdit = findViewById(R.id.band_card_owner_name_et);
        mBandCardAccountEdit = findViewById(R.id.band_card_account_et);
        mBandCardNameEdit = findViewById(R.id.band_name_et);
        mBandCardSonNameEdit = findViewById(R.id.band_son_name_et);
        mRemarkEdit = findViewById(R.id.band_card_remark_et);
    }

    private void intEvent() {
        findViewById(R.id.sure_band_btn).setOnClickListener(v -> {
            band();
        });
    }

    private void band() {
        Map<String, String> params = new HashMap<>();
//        if (mAddType == 1) {
            String alipayName = mAlipayNameEdit.getText().toString().trim();
            String alipayAccount = mAlipayAccount.getText().toString().trim();
            if (TextUtils.isEmpty(alipayAccount)) {
                ToastUtil.showToast(mContext, getString(R.string.must_edit_info_cannot_null));
                return;
            }
            if(content!=null){
                params.put("id",id);
            }

            params.put("type", String.valueOf(mAddType));
            params.put("aliPayName", alipayName);
            params.put("aliPayAccount", alipayAccount);
//        } else {
//            String bandCardOwnerName = mBandCardOwnerNameEdit.getText().toString().trim();
//            String bandCardAccount = mBandCardAccountEdit.getText().toString().trim();
//            String bandCardName = mBandCardNameEdit.getText().toString().trim();
//            String bandCardSonName = mBandCardSonNameEdit.getText().toString().trim();
//            String remark = mRemarkEdit.getText().toString().trim();
//            if (TextUtils.isEmpty(bandCardOwnerName) || TextUtils.isEmpty(bandCardAccount) || TextUtils.isEmpty(bandCardName)) {
//                ToastUtil.showToast(mContext, getString(R.string.must_edit_info_cannot_null));
//                return;
//            }
//            params.put("type", String.valueOf(2));
//            params.put("cardName", bandCardOwnerName);
//            params.put("bankCardNo", bandCardAccount);
//            params.put("bankName", bandCardName);
//            params.put("bankBranchName", bandCardSonName);
//            params.put("desc", remark);
//        }

        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HttpUtils.get().url(content != null ? coreManager.getConfig().MANUAL_PAY_UPDATE_WITHDRAW_ACCOUNT : coreManager.getConfig().MANUAL_PAY_ADD_WITHDRAW_ACCOUNT)
                .params(params)
                .build()
                .execute(new BaseCallback<ScanRecharge>(ScanRecharge.class) {

                    @Override
                    public void onResponse(ObjectResult<ScanRecharge> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, getString(R.string.update_success));
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });

    }
}
