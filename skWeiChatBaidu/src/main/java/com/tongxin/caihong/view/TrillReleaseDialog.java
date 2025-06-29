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

public class TrillReleaseDialog extends Dialog implements View.OnClickListener {
    private OnItemClickListener onItemClickListener;

    public TrillReleaseDialog(Context context, OnItemClickListener onItemClickListener) {
        super(context, R.style.BottomDialog);
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_trill_release);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.tvPreview).setOnClickListener(this);
        findViewById(R.id.tvSelect).setOnClickListener(this);
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
            case R.id.tvPreview:
                onItemClickListener.onItem1Click();
                break;
            case R.id.tvSelect:
                onItemClickListener.onItem2Click();
                break;
        }
    }

    public interface OnItemClickListener {
        void onItem1Click();

        void onItem2Click();
    }
}
