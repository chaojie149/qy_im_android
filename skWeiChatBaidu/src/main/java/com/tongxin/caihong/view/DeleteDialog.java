package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.R;

public class DeleteDialog extends Dialog implements View.OnClickListener {
    private OnDeleteListener onDeleteListener;

    public DeleteDialog(Context context, OnDeleteListener onDeleteListener) {
        super(context, R.style.BottomDialog);
        this.onDeleteListener = onDeleteListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.tvDelete).setOnClickListener(this);
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
            case R.id.tvDelete:
                onDeleteListener.tvDeleteClick();
                break;
        }
    }

    public interface OnDeleteListener {
        void tvDeleteClick();
    }
}
