package com.tongxin.caihong.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * 预留状态栏空间的RelativeLayout,
 * 因为系统提供的fitsSystemWindows与状态栏沉浸冲突无法使用，
 * 而且MergerStatus继承的是Toolbar且封装了背景色处理导致不够通用，所以额外定义状态栏空间的布局，
 */
public class StatusBarRelativeLayout extends RelativeLayout {
    public StatusBarRelativeLayout(Context context) {
        super(context);
        init();
    }

    public StatusBarRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusBarRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public StatusBarRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        // padding会影响RelativeLayout内部布局，所以只能margin, 但是layoutParams一开始可能为空，
        if (getLayoutParams() == null) {
            addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (getLayoutParams() != null) {
                        removeOnLayoutChangeListener(this);
                        // 添加post前时常有topMargin失效情况，
                        post(() -> init());
                    }
                }
            });
        } else {
            MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
            lp.setMargins(lp.leftMargin, lp.topMargin + getStatusHeight(), lp.rightMargin, lp.bottomMargin);
            setLayoutParams(lp);
        }
    }

    private int getStatusHeight() {
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }
}
