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

/**
 * 选择图片弹窗
 */
public class SelectImageDialog extends Dialog implements View.OnClickListener {
    private OnSelectImageItemListener onSelectImageItemListener;

    public SelectImageDialog(Context context, OnSelectImageItemListener onSelectImageItemListener) {
        super(context, R.style.BottomDialog);
        this.onSelectImageItemListener = onSelectImageItemListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_image_dialog);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.tvTakePhoto).setOnClickListener(this);
        findViewById(R.id.tvSelectPhoto).setOnClickListener(this);
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
            case R.id.tvTakePhoto:
                onSelectImageItemListener.tvTakePhotoClick();
                break;
            case R.id.tvSelectPhoto:
                onSelectImageItemListener.tvSelectPhotoClick();
                break;
        }
    }

    public interface OnSelectImageItemListener {
        void tvTakePhotoClick();

        void tvSelectPhotoClick();
    }
}
