package com.tongxin.caihong.pay.new_ui;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Receipt;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.pay.EventReceiptSuccess;
import com.tongxin.caihong.pay.ReceiptSetMoneyActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ScreenUtil;
import com.example.qrcode.utils.CommonUtils;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.view.CircleImageView;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReceiptFragment extends Fragment {
    private int resumeCount;
    private String mLoginUserId;
    private ImageView mReceiptQrCodeIv;
    private ImageView mReceiptQrCodeAvatarIv;
    private String money, description;
    private TextView mMoneyTv, mDescTv;
    private TextView mSetMoneyTv;

    public ReceiptFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receipt, container, false);
        mLoginUserId = CoreManager.getSelf(requireActivity()).getUserId();
        money = PreferenceUtils.getString(requireContext(), Constants.RECEIPT_SETTING_MONEY + mLoginUserId);
        description = PreferenceUtils.getString(requireContext(), Constants.RECEIPT_SETTING_DESCRIPTION + mLoginUserId);
        initActionBar(view);
        initView(view);
        initEvent(view);
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeCount++;
        if (resumeCount > 1) {
            money = PreferenceUtils.getString(requireContext(), Constants.RECEIPT_SETTING_MONEY + mLoginUserId);
            description = PreferenceUtils.getString(requireContext(), Constants.RECEIPT_SETTING_DESCRIPTION + mLoginUserId);
            refreshView();
            refreshReceiptQRCode();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar(View v) {
        v.findViewById(R.id.iv_title_left).setOnClickListener(view -> getActivity().finish());
        TextView titleTv = v.findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.receipt));
    }

    private void initView(View view) {
        CircleImageView civ_user_receipt = view.findViewById(R.id.civ_user_receipt);
        AvatarHelper.getInstance().displayAvatar(mLoginUserId, civ_user_receipt);
        mReceiptQrCodeIv = view.findViewById(R.id.rp_qr_code_iv);
        mReceiptQrCodeAvatarIv = view.findViewById(R.id.rp_qr_code_avatar_iv);
        refreshReceiptQRCode();
        AvatarHelper.getInstance().displayAvatar(mLoginUserId, mReceiptQrCodeAvatarIv);

        mMoneyTv = view.findViewById(R.id.rp_money_tv);
        mDescTv = view.findViewById(R.id.rp_desc_tv);
        mSetMoneyTv = view.findViewById(R.id.rp_set_money_tv);
        refreshView();
    }

    private void initEvent(View view) {
        view.findViewById(R.id.rl_set_money).setOnClickListener(v -> {
            if (!TextUtils.isEmpty(money)) { // 清除金额
                money = "";
                description = "";
                PreferenceUtils.putString(requireContext(), Constants.RECEIPT_SETTING_MONEY + mLoginUserId, money);
                PreferenceUtils.putString(requireContext(), Constants.RECEIPT_SETTING_DESCRIPTION + mLoginUserId, description);
                mSetMoneyTv.setText(getString(R.string.rp_receipt_tip2));
                refreshView();
                refreshReceiptQRCode();
            } else { // 设置金额
                startActivity(new Intent(requireContext(), ReceiptSetMoneyActivity.class));
            }
        });

        view.findViewById(R.id.rl_save_receipt_code).setOnClickListener(v -> {
            FileUtil.saveImageToGallery2(requireContext(), getBitmap(getActivity().getWindow().getDecorView()), true);
        });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventReceiptSuccess message) {
        DialogHelper.tip(requireContext(), getString(R.string.payment, message.getPaymentName()));
    }

    private void refreshView() {
        mMoneyTv.setText("￥" + money);
        mDescTv.setText(description);

        if (!TextUtils.isEmpty(money)) {
            mSetMoneyTv.setText(getString(R.string.rp_receipt_tip3));
            mMoneyTv.setVisibility(View.VISIBLE);
        } else {
            mSetMoneyTv.setText(getString(R.string.rp_receipt_tip2));
            mMoneyTv.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(description)) {
            mDescTv.setVisibility(View.VISIBLE);
        } else {
            mDescTv.setVisibility(View.GONE);
        }
    }

    private void refreshReceiptQRCode() {
        Receipt receipt = new Receipt();
        receipt.setUserId(mLoginUserId);
        receipt.setUserName(CoreManager.getSelf(requireContext()).getNickName());
        receipt.setMoney(money);
        receipt.setDescription(description);
        String content = JSON.toJSONString(receipt);

        int otherHeight = 380;
        if (!TextUtils.isEmpty(money)) {
            otherHeight = 410;
        } else if (!TextUtils.isEmpty(description)) {
            otherHeight = 425;
        }
        int height = ScreenUtil.getScreenHeight(MyApplication.getContext()) - DisplayUtil.dip2px(MyApplication.getContext(), otherHeight);
        Bitmap mQRCodeBitmap = CommonUtils.createQRCode(content, height, height);
        ViewGroup.LayoutParams layoutParams1 = mReceiptQrCodeIv.getLayoutParams();
        layoutParams1.width = height;
        layoutParams1.height = height;
        mReceiptQrCodeIv.setLayoutParams(layoutParams1);
        mReceiptQrCodeIv.setImageBitmap(mQRCodeBitmap);
    }

    /**
     * 获取这个view的缓存bitmap,
     */
    private Bitmap getBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap result = Bitmap.createBitmap(view.getDrawingCache());
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);
        return result;
    }
}
