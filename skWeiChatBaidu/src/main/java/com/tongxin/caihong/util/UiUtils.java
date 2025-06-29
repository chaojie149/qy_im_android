package com.tongxin.caihong.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Administrator on 2017/10/26.
 */

public class UiUtils {
    private static final int MIN_CLICK_DELAY_TIME = 600;
    private static long lastClickTime;
    private static int clickedView;
    private static boolean showTitle = true;

    public static void updateNum(TextView numTv, int unReadNum) {
        if (numTv == null) {
            return;
        }
        if (unReadNum == -1) {
            // 好友更新了动态且本地社交圈无未读数量
            ViewGroup.LayoutParams layoutParams = numTv.getLayoutParams();
            layoutParams.width = ScreenUtil.dip2px(MyApplication.getContext(), 12);
            layoutParams.height = ScreenUtil.dip2px(MyApplication.getContext(), 12);
            numTv.setLayoutParams(layoutParams);

            numTv.setText("");
            numTv.setVisibility(View.VISIBLE);
            return;
        } else {
            ViewGroup.LayoutParams layoutParams = numTv.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = (int) numTv.getResources().getDimension(R.dimen.indicator_size);
            numTv.setLayoutParams(layoutParams);
            int padding;
            if (unReadNum < 10) {
                padding = 0;
            } else {
                padding = ScreenUtil.dip2px(MyApplication.getContext(), 6);
            }
            numTv.setPadding(padding, numTv.getPaddingTop(), padding, numTv.getPaddingBottom());
        }
        if (unReadNum < 1) {
            numTv.setText("");
            numTv.setVisibility(View.INVISIBLE);
        } else if (unReadNum >= 1000) {
            numTv.setText(new BigDecimal(unReadNum)
                    .divide(new BigDecimal(1000), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString() + "K");
            numTv.setVisibility(View.VISIBLE);
        } else {
            numTv.setText(String.valueOf(unReadNum));
            numTv.setVisibility(View.VISIBLE);
        }
    }

    /**
     * {@link UiUtils#isNormalClick(android.view.View)}
     */
    public static boolean isNormalClick() {
        boolean isNormal = false;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            isNormal = true;
        }
        lastClickTime = currentTime;
        return isNormal;
    }

    public static boolean isNormalClick(View view) {
        // hashCode确保同一个view计算出来是一样的，不同view也几乎不会遇到相同hashCode的情况，
        long currentTime = System.currentTimeMillis();
        if (clickedView != view.hashCode()) {
            // 点击不同的view，不限制时间间隔，
            clickedView = view.hashCode();
            lastClickTime = currentTime;
            return true;
        }
        // 同一个view多次点击，限制连续点击时间，
        clickedView = view.hashCode();
        boolean isNormal = false;
        if ((currentTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            isNormal = true;
        }
        lastClickTime = currentTime;
        return isNormal;
    }

    public static boolean isVideoNormalClick(View view) {
        // hashCode确保同一个view计算出来是一样的，不同view也几乎不会遇到相同hashCode的情况，
        long currentTime = System.currentTimeMillis();
 /*       if (clickedView != view.hashCode()) {
            // 点击不同的view，不限制时间间隔，
            clickedView = view.hashCode();
            lastClickTime = currentTime;
            return true;
        }
        // 同一个view多次点击，限制连续点击时间，
        clickedView = view.hashCode();*/
        boolean isNormal = false;
        if ((currentTime - lastClickTime) >= 1000) {
            isNormal = true;
        }
        lastClickTime = currentTime;
        return isNormal;
    }

    /**
     * 动态改变根布局高度来适配软键盘显示与隐藏
     *
     * @param activity 需要适配的activity
     * @param view     根布局view
     */
    public static void supportChangeRootHeightAdaptationKeyboard(Activity activity, View view) {
        SoftKeyBoardListener.setListener(activity, new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {
            @Override
            public void keyBoardShow(int height) {
                int statusHeight = ScreenUtil.getStatusHeight(MyApplication.getContext());
                if (statusHeight == -1) {
                    // 未获取到状态栏高度时，默认状态栏高度为25dp
                    statusHeight = ScreenUtil.dip2px(MyApplication.getContext(), 25);
                }
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = ScreenUtil.getScreenHeight(MyApplication.getContext()) + statusHeight - height;
                view.setLayoutParams(params);
            }

            @Override
            public void keyBoardHide(int height) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = WindowManager.LayoutParams.MATCH_PARENT;
                view.setLayoutParams(params);
            }
        });
    }
}
