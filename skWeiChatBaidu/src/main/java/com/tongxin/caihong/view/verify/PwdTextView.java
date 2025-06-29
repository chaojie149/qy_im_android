package com.tongxin.caihong.view.verify;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class PwdTextView extends AppCompatTextView {
    private float radius;
    private boolean hasPwd;

    public PwdTextView(Context context) {
        this(context, (AttributeSet) null);
    }

    public PwdTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PwdTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.hasPwd) {
            Paint paint = new Paint(1);
            paint.setColor(-16777216);
            paint.setStyle(Style.FILL);
            canvas.drawCircle((float) (this.getWidth() / 2), (float) (this.getHeight() / 2), this.radius, paint);
        }

    }

    public void clearPwd() {
        this.hasPwd = false;
        this.invalidate();
    }

    public void drawPwd(float radius) {
        this.hasPwd = true;
        if (radius == 0.0F) {
            this.radius = (float) (this.getWidth() / 4);
        } else {
            this.radius = radius;
        }

        this.invalidate();
    }
}
