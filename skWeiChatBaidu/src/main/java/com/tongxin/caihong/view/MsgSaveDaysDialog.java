package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.R;


/**
 * 消息保存时间
 */
public class MsgSaveDaysDialog extends Dialog implements View.OnClickListener {
    private TextView tv1, tv2, tv3, tv4, tv5, tv6, tv7;

    private OnMsgSaveDaysDialogClickListener mOnMsgSaveDaysDialogClickListener;

    public MsgSaveDaysDialog(Context context, OnMsgSaveDaysDialogClickListener onMsgSaveDaysDialogClickListener) {
        super(context, R.style.BottomDialog);
        this.mOnMsgSaveDaysDialogClickListener = onMsgSaveDaysDialogClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.msg_save_days_dialog);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);
        tv4 = (TextView) findViewById(R.id.tv4);
        tv5 = (TextView) findViewById(R.id.tv5);
        tv6 = (TextView) findViewById(R.id.tv6);
        tv7 = (TextView) findViewById(R.id.tv7);
        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
        tv4.setOnClickListener(this);
        tv5.setOnClickListener(this);
        tv6.setOnClickListener(this);
        tv7.setOnClickListener(this);
        findViewById(R.id.tvCancel).setOnClickListener(this);

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
            case R.id.tv1:
                mOnMsgSaveDaysDialogClickListener.tv1Click();
                break;
            case R.id.tv2:
                mOnMsgSaveDaysDialogClickListener.tv2Click();
                break;
            case R.id.tv3:
                mOnMsgSaveDaysDialogClickListener.tv3Click();
                break;
            case R.id.tv4:
                mOnMsgSaveDaysDialogClickListener.tv4Click();
                break;
            case R.id.tv5:
                mOnMsgSaveDaysDialogClickListener.tv5Click();
                break;
            case R.id.tv6:
                mOnMsgSaveDaysDialogClickListener.tv6Click();
                break;
            case R.id.tv7:
                mOnMsgSaveDaysDialogClickListener.tv7Click();
                break;
        }
    }

    public interface OnMsgSaveDaysDialogClickListener {
        void tv1Click();

        void tv2Click();

        void tv3Click();

        void tv4Click();

        void tv5Click();

        void tv6Click();

        void tv7Click();
    }
}
