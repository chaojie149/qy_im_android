package com.tongxin.caihong.ui.base;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.util.LocaleHelper;
import com.tongxin.caihong.view.helper.TouchHelper;

public abstract class BaseActivity extends BaseLoginActivity {

    private View swipeBackLayout;
    private boolean touchHideSoftInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.setLocale(this);
        super.onCreate(savedInstanceState);
        swipeBackLayout = this.getSwipeBackLayout();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // 因为webView初始化会导致context中的语言字段重置，所以要重新设置，
        LocaleHelper.setLocale(getApplicationContext());
        LocaleHelper.setLocale(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (touchHideSoftInput) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                View view = getCurrentFocus();
                if (TouchHelper.isShouldHideKeyBord(view, ev)) {
                    TouchHelper.hintKeyBoards(view);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setTouch(boolean touchHideSoftInput) {
        this.touchHideSoftInput = touchHideSoftInput;
    }

    /**
     * 戳一戳动画
     *
     * @param type
     */
    public void shake(int type) {
        if (swipeBackLayout != null) {
            Animation shake;
            if (type == 0) {
                shake = AnimationUtils.loadAnimation(MyApplication.getContext(), R.anim.shake_from);
            } else {
                shake = AnimationUtils.loadAnimation(MyApplication.getContext(), R.anim.shake_to);
            }
            swipeBackLayout.startAnimation(shake);
        }
    }
}
