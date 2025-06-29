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
 * Created by zq on 2017/9/20 0020.
 * 保存图片
 */
public class SaveWindow extends Dialog {
    private View.OnClickListener itemsOnClick;

    // 是否显示发送给朋友Item
    private boolean isShowForward;
    // 是否显示收藏Item
    private boolean isShowCollection;
    // 是否显示识别图中二维码
    private boolean isShowIdentifyQRCode;
    // 是否显示定位到聊天
    private boolean isShowTargetChat;

    public SaveWindow(Context context, boolean isShowIdentifyQRCode, View.OnClickListener itemsOnClick) {
        super(context, R.style.BottomDialog);
        this.isShowIdentifyQRCode = isShowIdentifyQRCode;
        this.itemsOnClick = itemsOnClick;
    }

    public SaveWindow(Context context, boolean isShowForward, boolean isShowCollection, boolean isShowIdentifyQRCode, View.OnClickListener itemsOnClick) {
        this(context, isShowForward, isShowCollection, isShowIdentifyQRCode, false, itemsOnClick);
    }

    public SaveWindow(Context context, boolean isShowForward, boolean isShowCollection, boolean isShowIdentifyQRCode, boolean isShowTargetChat, View.OnClickListener itemsOnClick) {
        super(context, R.style.BottomDialog);
        this.isShowForward = isShowForward;
        this.isShowCollection = isShowCollection;
        this.isShowIdentifyQRCode = isShowIdentifyQRCode;
        this.isShowTargetChat = isShowTargetChat;
        this.itemsOnClick = itemsOnClick;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_to_galley);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        findViewById(R.id.tv_send_to_friend).setVisibility(isShowForward ? View.VISIBLE : View.GONE);
        findViewById(R.id.v1).setVisibility(isShowForward ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_collection).setVisibility(isShowCollection ? View.VISIBLE : View.GONE);
        findViewById(R.id.v2).setVisibility(isShowCollection ? View.VISIBLE : View.GONE);
        findViewById(R.id.identification_qr_code).setVisibility(isShowIdentifyQRCode ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvTargetChat).setVisibility(isShowTargetChat ? View.VISIBLE : View.GONE);
        findViewById(R.id.v0).setVisibility(isShowTargetChat ? View.VISIBLE : View.GONE);

        // 设置按钮监听
        findViewById(R.id.tv_send_to_friend).setOnClickListener(itemsOnClick);
        findViewById(R.id.tv_collection).setOnClickListener(itemsOnClick);
        findViewById(R.id.save_image).setOnClickListener(itemsOnClick);
        findViewById(R.id.edit_image).setOnClickListener(itemsOnClick);
        findViewById(R.id.identification_qr_code).setOnClickListener(itemsOnClick);
        findViewById(R.id.tvTargetChat).setOnClickListener(itemsOnClick);

        findViewById(R.id.tvCancel).setOnClickListener(itemsOnClick);

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
}
