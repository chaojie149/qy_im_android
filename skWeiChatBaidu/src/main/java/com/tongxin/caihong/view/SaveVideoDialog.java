package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.util.ScreenUtil;

public class SaveVideoDialog extends Dialog implements View.OnClickListener {

    private TextView tv1, tv2, tv3;
    // 是否显示定位到聊天
    private boolean isShowTargetChat;
    private OnSavaVideoDialogClickListener mOnSavaVideoDialogClickListener;

    public SaveVideoDialog(Context context, boolean isShowTargetChat, OnSavaVideoDialogClickListener mOnSavaVideoDialogClickListener) {
        super(context, R.style.BottomDialog);
        this.isShowTargetChat = isShowTargetChat;
        this.mOnSavaVideoDialogClickListener = mOnSavaVideoDialogClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_sava_video);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.tvTargetChat).setVisibility(isShowTargetChat ? View.VISIBLE : View.GONE);
        findViewById(R.id.v0).setVisibility(isShowTargetChat ? View.VISIBLE : View.GONE);
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);

        findViewById(R.id.tvTargetChat).setOnClickListener(this);
        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
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
            case R.id.tvTargetChat:
                mOnSavaVideoDialogClickListener.tvTargetChatClick();
                break;
            case R.id.tv1:
                mOnSavaVideoDialogClickListener.tv1Click();
                break;
            case R.id.tv2:
                mOnSavaVideoDialogClickListener.tv2Click();
                break;
            case R.id.tv3:
                mOnSavaVideoDialogClickListener.tv3Click();
                break;
        }
    }

    public interface OnSavaVideoDialogClickListener {
        void tvTargetChatClick();

        void tv1Click();

        void tv2Click();

        void tv3Click();
    }
}
