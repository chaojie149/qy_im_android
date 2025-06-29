package com.tongxin.caihong.view.selectable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.tongxin.caihong.R;

/**
 * Created by Jaeger on 16/8/30.
 * <p>
 * Email: chjie.jaeger@gmail.com
 * GitHub: https://github.com/laobie
 */
public class SelectableTextHelper {

    private final static int DEFAULT_SELECTION_LENGTH = 1;
    private static final int DEFAULT_SHOW_DURATION = 100;
    private CursorHandle mStartHandle;
    private CursorHandle mEndHandle;
    private OperateWindow mOperateWindow;
    private SelectionInfo mSelectionInfo = new SelectionInfo();
    private OnSelectListener mSelectListener;
    private Context mContext;
    private TextView mTextView;
    private Spannable mSpannable;
    private int mCursorHandleColor;
    private int mCursorHandleSize;
    private boolean isHideWhenScroll;
    private boolean isHide = true;
    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHide) return;
            if (mOperateWindow != null) {
                mOperateWindow.show();
            }
            if (mStartHandle != null) {
                showCursorHandle(mStartHandle);
            }
            if (mEndHandle != null) {
                showCursorHandle(mEndHandle);
            }
            ensureSelection();
        }
    };
    private int position;

    public SelectableTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mContext = mTextView.getContext();
        mTextView.setHighlightColor(builder.mSelectedColor);
        mCursorHandleColor = builder.mCursorHandleColor;
        position = builder.position;
        mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
        mOperateWindow = new OperateWindow(mContext, builder.mWindow);
        init();
    }

    private void init() {
        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);
        mTextView.setFocusable(true);
        mTextView.setFocusableInTouchMode(true);
        mTextView.setTag(R.id.selectable_helper, this);
    }

    public void onScrollIdle() {
        if (isHideWhenScroll) {
            isHideWhenScroll = false;
            postShowSelectView(DEFAULT_SHOW_DURATION);
        }
    }

    public void onScrolling() {
        if (!isHideWhenScroll && !isHide) {
            isHideWhenScroll = true;
            if (mOperateWindow != null) {
                mOperateWindow.dismiss();
            }
            if (mStartHandle != null) {
                mStartHandle.dismiss();
            }
            if (mEndHandle != null) {
                mEndHandle.dismiss();
            }
        }
    }

    public void ensureSelection() {
        if (mSpannable != null) {
            Selection.setSelection(mSpannable, mSelectionInfo.mStart, mSelectionInfo.mEnd);
            mTextView.requestFocus();
            mTextView.invalidate();
        }
    }

    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    private void hideSelectView() {
        isHide = true;
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
        if (mOperateWindow != null) {
            mOperateWindow.dismiss();
        }
    }

    private void resetSelectionInfo() {
        mSelectionInfo.mSelectionContent = null;
        if (mSpannable != null) {
            Selection.removeSelection(mSpannable);
        }
    }

    public void showSelectView(int x, int y) {
        hideSelectView();
        resetSelectionInfo();
        isHide = false;
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);

/*
        int startOffset = TextLayoutUtil.getPreciseOffset(mTextView, x, y);
        int endOffset = startOffset + DEFAULT_SELECTION_LENGTH;
*/
        int startOffset = 0;
        int endOffset = mTextView.getText().length();
        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        if (mSpannable == null || startOffset >= mTextView.getText().length()) {
            return;
        }
        selectText(startOffset, endOffset);
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        mOperateWindow.show();
    }

    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int offset = cursorHandle.isLeft ? mSelectionInfo.mStart : mSelectionInfo.mEnd;
        cursorHandle.show((int) layout.getPrimaryHorizontal(offset), layout.getLineBottom(layout.getLineForOffset(offset)));
    }

    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.mStart = startPos;
        }
        if (endPos != -1) {
            mSelectionInfo.mEnd = endPos;
        }
        if (mSelectionInfo.mStart > mSelectionInfo.mEnd) {
            int temp = mSelectionInfo.mStart;
            mSelectionInfo.mStart = mSelectionInfo.mEnd;
            mSelectionInfo.mEnd = temp;
        }

        if (mSpannable != null) {

            mSelectionInfo.mSelectionContent = mSpannable.subSequence(mSelectionInfo.mStart, mSelectionInfo.mEnd).toString();
            Selection.setSelection(mSpannable, mSelectionInfo.mStart, mSelectionInfo.mEnd);
            if (mSelectListener != null) {
                mSelectListener.onTextSelected(mSelectionInfo.mSelectionContent);
            }
        }
    }

    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    @Nullable
    public String getSelectionContent() {
        return mSelectionInfo.mSelectionContent;
    }

    public void destroy() {
        mTextView.setTag(R.id.selectable_helper, null);
        resetSelectionInfo();
        hideSelectView();
        mStartHandle = null;
        mEndHandle = null;
        mOperateWindow = null;
    }

    public int getPosition() {
        return position;
    }

    private CursorHandle getCursorHandle(boolean isLeft) {
        if (mStartHandle.isLeft == isLeft) {
            return mStartHandle;
        } else {
            return mEndHandle;
        }
    }

    public interface IPopupWindow {
        int getWidth();

        int getHeight();

        void setOnDismissListener(PopupWindow.OnDismissListener onDismissListener);

        void showAtLocation(View parent, int gravity, int x, int y);

        void dismiss();

        boolean isShowing();
    }

    public static class Builder {
        private TextView mTextView;
        private int mCursorHandleColor = 0xFF1379D6;
        private int mSelectedColor = 0xFFAFE1F4;
        private float mCursorHandleSizeInDp = 24;
        private IPopupWindow mWindow;
        private int position;

        public Builder(TextView textView) {
            mTextView = textView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public Builder setPopupWindow(IPopupWindow window) {
            this.mWindow = window;
            return this;
        }

        public Builder setPosition(int position) {
            this.position = position;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }
    }

    /**
     * Operate windows : copy, select all
     */
    private class OperateWindow {

        private IPopupWindow mWindow;
        private int[] mTempCoors = new int[2];
        private boolean dismissFromCode = false;

        public OperateWindow(final Context context, IPopupWindow window) {
            mWindow = window;

            mWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (dismissFromCode) {
                        dismissFromCode = false;
                        return;
                    }
                    destroy();
                }
            });
        }

        public void show() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            int posX = (int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) + getExtraX();
            int posY = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY() - mWindow.getHeight() - 16;
            if (posX <= 0) posX = 16;
            if (posY < 0) posY = 16;
            if (posX + mWindow.getWidth() > TextLayoutUtil.getScreenWidth(mContext)) {
                posX = TextLayoutUtil.getScreenWidth(mContext) - mWindow.getWidth() - 16;
            }
            mWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, posX, posY);
        }

        public int getExtraX() {
            return mTempCoors[0] + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            return mTempCoors[1] + mTextView.getPaddingTop();
        }

        public void dismiss() {
            if (mWindow.isShowing()) {
                dismissFromCode = true;
                mWindow.dismiss();
            }
        }

        public boolean isShowing() {
            return mWindow.isShowing();
        }
    }

    private class CursorHandle extends View {

        private PopupWindow mPopupWindow;
        private Paint mPaint;

        private int mCircleRadius = mCursorHandleSize / 2;
        private int mWidth = mCircleRadius * 2;
        private int mHeight = mCircleRadius * 2;
        private int mPadding = 25;
        private boolean isLeft;
        private int mAdjustX;
        private int mAdjustY;
        private int mBeforeDragStart;
        private int mBeforeDragEnd;
        private int[] mTempCoors = new int[2];

        public CursorHandle(boolean isLeft) {
            super(mContext);
            this.isLeft = isLeft;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(mCursorHandleColor);

            mPopupWindow = new PopupWindow(this);
            mPopupWindow.setClippingEnabled(false);
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            mPopupWindow.setHeight(mHeight + mPadding / 2);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(mCircleRadius + mPadding, mCircleRadius, mCircleRadius, mPaint);
            if (isLeft) {
                canvas.drawRect(mCircleRadius + mPadding, 0, mCircleRadius * 2 + mPadding, mCircleRadius, mPaint);
            } else {
                canvas.drawRect(mPadding, 0, mCircleRadius + mPadding, mCircleRadius, mPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSelectionInfo.mStart;
                    mBeforeDragEnd = mSelectionInfo.mEnd;
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOperateWindow.show();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOperateWindow.dismiss();
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    update(rawX + mAdjustX - mWidth, rawY + mAdjustY - mHeight);
                    break;
            }
            return true;
        }

        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        public void dismiss() {
            mPopupWindow.dismiss();
        }

        public void update(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int oldOffset;
            if (isLeft) {
                oldOffset = mSelectionInfo.mStart;
            } else {
                oldOffset = mSelectionInfo.mEnd;
            }

            x -= mTempCoors[0];
            y -= mTempCoors[1];

            int offset = TextLayoutUtil.getHysteresisOffset(mTextView, x, y, oldOffset);

            if (offset != oldOffset) {
                resetSelectionInfo();
                if (isLeft) {
                    if (offset > mBeforeDragEnd) {
                        CursorHandle handle = getCursorHandle(false);
                        changeDirection();
                        handle.changeDirection();
                        mBeforeDragStart = mBeforeDragEnd;
                        selectText(mBeforeDragEnd, offset);
                        handle.updateCursorHandle();
                    } else {
                        selectText(offset, -1);
                    }
                    updateCursorHandle();
                } else {
                    if (offset < mBeforeDragStart) {
                        CursorHandle handle = getCursorHandle(true);
                        handle.changeDirection();
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectText(offset, mBeforeDragStart);
                        handle.updateCursorHandle();
                    } else {
                        selectText(mBeforeDragStart, offset);
                    }
                    updateCursorHandle();
                }
            }
        }

        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) - mWidth + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY(), -1, -1);
            } else {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mEnd) + getExtraX(),
                        layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mEnd)) + getExtraY(), -1, -1);
            }
        }

        public void show(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int offset = isLeft ? mWidth : 0;
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY());
        }

        public int getExtraX() {
            return mTempCoors[0] - mPadding + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            return mTempCoors[1] + mTextView.getPaddingTop();
        }
    }
}


