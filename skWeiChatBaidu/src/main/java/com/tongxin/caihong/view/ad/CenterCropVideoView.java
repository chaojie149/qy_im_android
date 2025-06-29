package com.tongxin.caihong.view.ad;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * 用这个实现视频放大居中会出现朋友圈弹出半透明窗口时显示了完整视频的问题，
 */
public class CenterCropVideoView extends VideoView {
    private int leftAdjustment;
    private int topAdjustment;

    public CenterCropVideoView(Context context) {
        super(context);
    }

    public CenterCropVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CenterCropVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CenterCropVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int videoWidth = getMeasuredWidth();
        int videoHeight = getMeasuredHeight();

        int viewWidth = getDefaultSize(0, widthMeasureSpec);
        int viewHeight = getDefaultSize(0, heightMeasureSpec);

        leftAdjustment = 0;
        topAdjustment = 0;
        if (videoWidth == viewWidth) {
            int newWidth = (int) ((float) videoWidth / videoHeight * viewHeight);
            setMeasuredDimension(newWidth, viewHeight);
            leftAdjustment = -(newWidth - viewWidth) / 2;
        } else {
            int newHeight = (int) ((float) videoHeight / videoWidth * viewWidth);
            setMeasuredDimension(viewWidth, newHeight);
            topAdjustment = -(newHeight - viewHeight) / 2;

        }
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l + leftAdjustment, t + topAdjustment, r + leftAdjustment, b + topAdjustment);
    }
}
