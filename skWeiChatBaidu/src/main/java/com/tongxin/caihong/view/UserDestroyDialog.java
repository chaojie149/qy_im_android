package com.tongxin.caihong.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.ScreenUtil;


/**
 * 对话框
 */
public class UserDestroyDialog extends Dialog {
    private TextView
            mTitle,
            mDescribe,
            mCancel,
            mConfirm;
    private CoreManager coreManager;
    private Runnable onConfirm;
    private CountDownTimer confirmTimer = new CountDownTimer(5_000, 100) {
        @SuppressLint("SetTextI18n")
        @Override
        public void onTick(long millisUntilFinished) {
            int left = (int) (millisUntilFinished / 1000 + 1);
            mConfirm.setText(getContext().getString(R.string.sure) + "(" + left + ")");
        }

        @Override
        public void onFinish() {
            mConfirm.setText(R.string.sure);
            mConfirm.setEnabled(true);
        }
    };

    public UserDestroyDialog(Context context, CoreManager coreManager, Runnable onConfirm) {
        super(context, R.style.BottomDialog);
        this.coreManager = coreManager;
        this.onConfirm = onConfirm;
    }

    public static void show(Context context, CoreManager coreManager, Runnable onConfirm) {
        UserDestroyDialog dialog = new UserDestroyDialog(context, coreManager, onConfirm);
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_user_destroy);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        mTitle = (TextView) findViewById(R.id.title);
        mDescribe = (TextView) findViewById(R.id.describe);
        mCancel = (TextView) findViewById(R.id.cancel);
        mConfirm = (TextView) findViewById(R.id.confirm);

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.9);
        lp.gravity = Gravity.CENTER;
        o.setAttributes(lp);
        initEvent();
    }

    private void initEvent() {
        mCancel.setOnClickListener(v -> {
            dismiss();
        });
        mConfirm.setEnabled(false);
        mConfirm.setOnClickListener(v -> {
            dismiss();
            onConfirm.run();
        });
        confirmTimer.start();
    }

}
