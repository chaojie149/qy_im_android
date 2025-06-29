package com.tongxin.caihong.ui.base;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import me.imid.swipebacklayout.lib.SwipeBackLayout;
import me.imid.swipebacklayout.lib.Utils;
import me.imid.swipebacklayout.lib.app.SwipeBackActivityBase;
import me.imid.swipebacklayout.lib.app.SwipeBackActivityHelper;

public abstract class SwipeBackActivity extends AppCompatActivity implements SwipeBackActivityBase {
    // 侧滑这个影响activity透明，所以图片相关几个页面禁用侧滑，连初始化都不可以，
    protected boolean disableSwipeBack = false;
    private SwipeBackActivityHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// 竖屏
        if (!disableSwipeBack) {
            mHelper = new SwipeBackActivityHelper(this);
            mHelper.onActivityCreate();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!disableSwipeBack) {
            mHelper.onPostCreate();
        }
    }

    @Override
    public void setRequestedOrientation(int requestedOrientation) {
        try {
            super.setRequestedOrientation(requestedOrientation);
        } catch (Exception e) {
            // 不要崩溃，不能设置就不设置，
            // 安卓8不允许透明页面设置屏幕方向，侧滑又给所有页面设置了透明，因此所有页面在侧滑初始化完成后都不能设置方向，
            // 项目里原本就是初始化时固定竖屏的，该设置移到侧滑初始化前，
            Log.d("SwipeBack", "忽略安卓8的屏幕旋转");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends View> T findViewById(int id) {
        return (T) realFindViewById(id);
    }

    public View realFindViewById(int id) {
        View v = super.findViewById(id);
        if (v == null && mHelper != null)
            return mHelper.findViewById(id);
        return v;
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = getWindow().getDecorView();
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        super.finish();
    }

    @Override
    public SwipeBackLayout getSwipeBackLayout() {
        if (disableSwipeBack) {
            return null;
        }
        return mHelper.getSwipeBackLayout();
    }

    @Override
    public void setSwipeBackEnable(boolean enable) {
        if (disableSwipeBack) {
            return;
        }
        getSwipeBackLayout().setEnableGesture(enable);
    }

    @Override
    public void scrollToFinishActivity() {
        if (disableSwipeBack) {
            return;
        }
        Utils.convertActivityToTranslucent(this);
        getSwipeBackLayout().scrollToFinishActivity();
    }
}
