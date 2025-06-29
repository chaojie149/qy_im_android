package com.tongxin.caihong.ui.me.collection;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生活圈、视界PopupWindow
 */
public class PublicMessageEditWindow extends PopupWindow {
    private final LayoutInflater inflater;
    private boolean isOpen;
    private boolean isReport = true;
    private Context mContext;
    private View.OnClickListener itemsOnClick;

    public PublicMessageEditWindow(Context context) {
        super(context);
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContext = context;
        initView();
    }

    public void setOpenDelete(boolean b) {
        this.isOpen = b;
        initView();
    }

    public void setOpenReport(boolean b) {
        this.isReport = b;
        initView();
    }

    public void setClick(View.OnClickListener itemsOnClick) {
        this.itemsOnClick = itemsOnClick;
        initView();
        //设置SelectPicPopupWindow弹出窗体的背景
        // 透明背景，
        this.setBackgroundDrawable(new ColorDrawable(0));

        WindowManager.LayoutParams lp = ((Activity) mContext).getWindow().getAttributes();
        lp.alpha = 0.7f;
        ((Activity) mContext).getWindow().setAttributes(lp);
    }

    public void initView() {
        ViewGroup menuView = (ViewGroup) inflater.inflate(R.layout.popu_public_message, null);
        menuView.findViewById(R.id.llDelete).setVisibility(isOpen ? View.VISIBLE : View.GONE);
        menuView.findViewById(R.id.llReport).setVisibility(isReport ? View.VISIBLE : View.GONE);

        // 黑色背景，白色文字图标，
        ColorStateList foreground = ColorStateList.valueOf(Color.WHITE);
        List layoutWidth = new ArrayList();
        for (int i = 0; i < menuView.getChildCount(); i++) {
            View child = menuView.getChildAt(i);
            if (child instanceof LinearLayout) {
                child.measure(0, 0);
                //获取组件的宽度
                int width = child.getMeasuredWidth();
                layoutWidth.add(width);
                LinearLayout layout = (LinearLayout) child;

                layout.setOnClickListener(itemsOnClick);
                for (int j = 0; j < layout.getChildCount(); j++) {
                    View lChild = layout.getChildAt(j);
                    if (lChild instanceof ImageView) {
                        // ImageViewCompat.setImageTintList((ImageView) lChild, foreground);
                    } else if (lChild instanceof TextView) {
                        ((TextView) lChild).setTextColor(foreground);
                    }
                }
            }
        }

        int widthMax = (int) Collections.max(layoutWidth);
        for (int i = 0; i < menuView.getChildCount(); i++) {
            View child = menuView.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout.LayoutParams lpw = (LinearLayout.LayoutParams) child.getLayoutParams();
                lpw.width = widthMax;
                child.setLayoutParams(lpw);
            }
        }

        //设置SelectPicPopupWindow的View
        this.setContentView(menuView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        // this.setWidth(ViewPiexlUtil.dp2px(context,200));
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);

        this.setOutsideTouchable(true);

        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);

        setOnDismissListener(() -> {
            WindowManager.LayoutParams lp1 = ((Activity) mContext).getWindow().getAttributes();
            lp1.alpha = 1f;
            ((Activity) mContext).getWindow().setAttributes(lp1);
        });
    }
}
