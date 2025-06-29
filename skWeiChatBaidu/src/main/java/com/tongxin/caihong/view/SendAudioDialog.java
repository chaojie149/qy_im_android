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

public class SendAudioDialog extends Dialog implements View.OnClickListener {
    private OnSendAudioItemListener onSendAudioItemListener;

    public SendAudioDialog(Context context, OnSendAudioItemListener onSendAudioItemListener) {
        super(context, R.style.BottomDialog);
        this.onSendAudioItemListener = onSendAudioItemListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_send_audio);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.tvPlay).setOnClickListener(this);
        findViewById(R.id.tvRecordAgain).setOnClickListener(this);
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
            case R.id.tvPlay:
                onSendAudioItemListener.tvPlay();
                break;
            case R.id.tvRecordAgain:
                onSendAudioItemListener.tvRecordAgain();
                break;
        }
    }

    public interface OnSendAudioItemListener {
        void tvPlay();

        void tvRecordAgain();
    }
}
