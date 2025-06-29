package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.tongxin.caihong.R;
import com.tongxin.caihong.util.ScreenUtil;

/**
 * 禁言
 */
public class BannedDialog extends Dialog implements View.OnClickListener {
    private int fromType;
    private OnBannedDialogClickListener mOnBannedDialogClickListener;

    public BannedDialog(Context context, int fromType, OnBannedDialogClickListener onBannedDialogClickListener) {
        super(context, R.style.BottomDialog);
        this.fromType = fromType;
        this.mOnBannedDialogClickListener = onBannedDialogClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gag_dialog);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.no_gag).setOnClickListener(this);
        findViewById(R.id.one_gag).setOnClickListener(this);
        findViewById(R.id.two_gag).setOnClickListener(this);
        findViewById(R.id.three_gag).setOnClickListener(this);
        findViewById(R.id.four_gag).setOnClickListener(this);
        findViewById(R.id.five_gag).setOnClickListener(this);
        findViewById(R.id.six_gag).setOnClickListener(this);
        findViewById(R.id.llPermanent).setOnClickListener(this);
        findViewById(R.id.tvCancel).setOnClickListener(this);

        if (fromType == 1) {
            // 来自直播，隐藏不禁言
            findViewById(R.id.no_gag).setVisibility(View.GONE);
        } else {
            // 来自群组，显示永久选项
            findViewById(R.id.llPermanent).setVisibility(View.VISIBLE);
        }

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        // x/y坐标
        // lp.x = 100;
        // lp.y = 100;
        lp.width = ScreenUtil.getScreenWidth(getContext());
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        switch (v.getId()) {
            case R.id.no_gag:
                mOnBannedDialogClickListener.tv1Click();
                break;
            case R.id.one_gag:
                mOnBannedDialogClickListener.tv2Click();
                break;
            case R.id.two_gag:
                mOnBannedDialogClickListener.tv3Click();
                break;
            case R.id.three_gag:
                mOnBannedDialogClickListener.tv4Click();
                break;
            case R.id.four_gag:
                mOnBannedDialogClickListener.tv5Click();
                break;
            case R.id.five_gag:
                mOnBannedDialogClickListener.tv6Click();
                break;
            case R.id.six_gag:
                mOnBannedDialogClickListener.tv7Click();
                break;
            case R.id.llPermanent:
                mOnBannedDialogClickListener.llPermanentClick();
                break;
        }
    }

    public interface OnBannedDialogClickListener {
        void tv1Click();

        void tv2Click();

        void tv3Click();

        void tv4Click();

        void tv5Click();

        void tv6Click();

        void tv7Click();

        default void llPermanentClick() {

        }
    }
}
