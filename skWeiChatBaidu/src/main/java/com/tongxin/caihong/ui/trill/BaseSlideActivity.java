package com.tongxin.caihong.ui.trill;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseLoginActivity;
import com.tongxin.caihong.util.ScreenUtil;

public abstract class BaseSlideActivity extends BaseLoginActivity {

    /**
     * 是否可以滑动关闭页面
     */
    protected boolean swipeEnabled = true;
    /**
     * 是否可以在页面任意位置右滑关闭页面，如果是false则从左边滑才可以关闭。
     */
    protected boolean swipeAnyWhere = false;
    private SwipeLayout swipeLayout;
    private boolean swipeFinished = false;

    public boolean isSwipeAnyWhere() {
        return swipeAnyWhere;
    }

    public void setSwipeAnyWhere(boolean swipeAnyWhere) {
        this.swipeAnyWhere = swipeAnyWhere;
    }

    public boolean isSwipeEnabled() {
        return swipeEnabled;
    }

    public void setSwipeEnabled(boolean swipeEnabled) {
        this.swipeEnabled = swipeEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// 竖屏
        swipeLayout = new SwipeLayout(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        swipeLayout.replaceLayer(this);
    }

    @Override
    public void finish() {
        if (swipeFinished) {
            super.finish();
            overridePendingTransition(0, 0);
        } else {
            swipeLayout.cancelPotentialAnimation();
            super.finish();
            //            overridePendingTransition(0, R.anim.slide_out_right);
        }
    }

    public abstract void intentLeft();

    class SwipeLayout extends FrameLayout {
        private final int duration = 200;
        boolean canSwipe = false;
        /**
         * 超过了touchslop仍然没有达到没有条件，则忽略以后的动作
         */
        boolean ignoreSwipe = false;
        View content;
        Activity mActivity;
        int sideWidthInDP = 16;
        int sideWidth = 72;
        int screenWidth = 1080;
        VelocityTracker tracker;
        float downX;
        float downY;
        float lastX;
        float currentX;
        float currentY;
        int touchSlopDP = 20;
        int touchSlop = 60;
        boolean hasIgnoreFirstMove;
        ObjectAnimator animator;
        //private View backgroundLayer;用来设置滑动时的背景色
        private Drawable leftShadow;

        public SwipeLayout(Context context) {
            super(context);
        }

        public SwipeLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public SwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public void replaceLayer(Activity activity) {
            leftShadow = activity.getResources().getDrawable(R.drawable.left_shadow);
            touchSlop = (int) (touchSlopDP * activity.getResources().getDisplayMetrics().density);
            sideWidth = (int) (sideWidthInDP * activity.getResources().getDisplayMetrics().density);
            mActivity = activity;
            screenWidth = ScreenUtil.getScreenWidth(activity);
            setClickable(true);
            final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
            content = root.getChildAt(0);
            ViewGroup.LayoutParams params = content.getLayoutParams();
            ViewGroup.LayoutParams params2 = new ViewGroup.LayoutParams(-1, -1);
            root.removeView(content);
            this.addView(content, params2);
            root.addView(this, params);
        }

        @Override
        protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
            boolean result = super.drawChild(canvas, child, drawingTime);
            final int shadowWidth = leftShadow.getIntrinsicWidth();
            int left = (int) (getContentX()) - shadowWidth;
            leftShadow.setBounds(left, child.getTop(), left + shadowWidth, child.getBottom());
            leftShadow.draw(canvas);
            return result;
        }

        @Override
        public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
            if (swipeEnabled && !canSwipe && !ignoreSwipe) {
                if (swipeAnyWhere) {
                    switch (ev.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = ev.getX();
                            downY = ev.getY();
                            currentX = downX;
                            currentY = downY;
                            lastX = downX;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float dx = ev.getX() - downX;
                            float dy = ev.getY() - downY;
                            if (dx * dx + dy * dy > touchSlop * touchSlop) {
                                if (dy == 0f || Math.abs(dx / dy) > 1) {
                                    downX = ev.getX();
                                    downY = ev.getY();
                                    currentX = downX;
                                    currentY = downY;
                                    lastX = downX;
                                    canSwipe = true;
                                    tracker = VelocityTracker.obtain();
                                    return true;
                                } else {
                                    ignoreSwipe = true;
                                }
                            }
                            break;
                    }
                } else if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getX() < sideWidth) {
                    canSwipe = true;
                    tracker = VelocityTracker.obtain();
                    return true;
                }
            }
            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                ignoreSwipe = false;
            }
            return super.dispatchTouchEvent(ev);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return canSwipe || super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            if (canSwipe) {
                tracker.addMovement(event);
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        currentX = downX;
                        currentY = downY;
                        lastX = downX;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        currentX = event.getX();
                        currentY = event.getY();
                        float dx = currentX - lastX;
                        if (dx != 0f && !hasIgnoreFirstMove) {
                            hasIgnoreFirstMove = true;
                            dx = dx / dx;
                        }
                        if (getContentX() + dx < 0) {
                            setContentX(0);
                        } else {
                            setContentX(getContentX() + dx);
                        }
                        lastX = currentX;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tracker.computeCurrentVelocity(10000);
                        tracker.computeCurrentVelocity(1000, 20000);
                        canSwipe = false;
                        hasIgnoreFirstMove = false;
                        int mv = screenWidth * 3;
                        if (Math.abs(tracker.getXVelocity()) > mv) {
                            animateFromVelocity(tracker.getXVelocity());
                        } else {
                            if (getContentX() > screenWidth / 3) {
                                animateFinish(false);
                            } else {
                                animateBack(false);
                            }
                        }
                        tracker.recycle();
                        break;
                    default:
                        break;
                }
            }
            return super.onTouchEvent(event);
        }

        public void cancelPotentialAnimation() {
            if (animator != null) {
                animator.removeAllListeners();
                animator.cancel();
            }
        }

        public float getContentX() {
            return content.getX();
        }

        public void setContentX(float x) {
            int ix = (int) x;
            content.setX(ix);
            invalidate();
        }

        /**
         * 弹回，不关闭，因为left是0，所以setX和setTranslationX效果是一样的
         *
         * @param withVel 使用计算出来的时间
         */
        private void animateBack(boolean withVel) {
            cancelPotentialAnimation();
            animator = ObjectAnimator.ofFloat(this, "contentX", getContentX(), 0);
            int tmpDuration = withVel ? ((int) (duration * getContentX() / screenWidth)) : duration;
            if (tmpDuration < 100) {
                tmpDuration = 100;
            }
            animator.setDuration(tmpDuration);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
        }

        private void animateFinish(boolean withVel) {
            cancelPotentialAnimation();
            animator = ObjectAnimator.ofFloat(this, "contentX", getContentX(), screenWidth);
            int tmpDuration = withVel ? ((int) (duration * (screenWidth - getContentX()) / screenWidth)) : duration;
            if (tmpDuration < 100) {
                tmpDuration = 100;
            }
            animator.setDuration(tmpDuration);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mActivity.isFinishing()) {
                        swipeFinished = true;
                        intentLeft();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }
            });
            animator.start();
        }

        private void animateFromVelocity(float v) {
            if (v > 0) {
                if (getContentX() < screenWidth / 3 && v * duration / 1000 + getContentX() < screenWidth / 3) {
                    animateBack(false);
                } else {
                    animateFinish(true);
                }
            } else {
                if (getContentX() > screenWidth / 3 && v * duration / 1000 + getContentX() > screenWidth / 3) {
                    animateFinish(false);
                } else {
                    animateBack(true);
                }
            }

        }
    }
}
