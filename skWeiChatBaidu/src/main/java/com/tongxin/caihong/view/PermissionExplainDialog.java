package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.ScreenUtil;

/**
 * 专门用于动态权限申请说明的提示框
 */
public class PermissionExplainDialog extends Dialog {
    private TextView
            mTipTv,
            mCancel,
            mConfirm;
    private String mTipString;
    private Runnable mOnConfirmListener;
    private Runnable mOnCancelListener;
    private boolean isConfirmed;
    private boolean goSettings;

    public PermissionExplainDialog(Context context) {
        this(context, false);
    }

    public PermissionExplainDialog(Context context, boolean goSettings) {
        super(context, R.style.BottomDialog);
        this.goSettings = goSettings;
    }

    public void setPermissions(String[] permissions) {
        setTipString(PermissionUtil.getPermissionExplainText(getContext(), permissions));
    }

    public void setTipString(String tip) {
        mTipString = tip;
        updateUI();
    }

    public void setOnConfirmListener(Runnable listener) {
        this.mOnConfirmListener = listener;
    }

    public void setOnCancelListener(Runnable listener) {
        this.mOnCancelListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tip_dialog_permission_explain);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void updateUI() {
        if (mTipTv != null) {
            mTipTv.setText(mTipString);
        }
    }

    private void initView() {
        mTipTv = (TextView) findViewById(R.id.tip_tv);
        mConfirm = findViewById(R.id.confirm);
        if (goSettings) {
            mConfirm.setText(R.string.btn_permission_go_settings);
        }
        mCancel = findViewById(R.id.cancel);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.9);
        updateUI();
        initEvent();
    }

    private void initEvent() {
        mConfirm.setOnClickListener(v -> {
            isConfirmed = true;
            dismiss();
            if (mOnConfirmListener != null) {
                mOnConfirmListener.run();
            }
        });
        mCancel.setOnClickListener(v -> {
            isConfirmed = true;
            dismiss();
            if (mOnCancelListener != null) {
                mOnCancelListener.run();
            }
        });
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }
}
