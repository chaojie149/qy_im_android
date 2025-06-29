package com.tongxin.caihong.sortlist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tongxin.caihong.util.DisplayUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SideBar extends View {
    // 26个字母和#,首字母不是英文字母的放到#分类
    public static String[] b = {"#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
            "X", "Y", "Z"};
    // 触摸事件
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    private int choose = -1;// 选中
    private Paint paint = new Paint();
    private Paint selectedBackgroundPaint = new Paint();

    private TextView mTextDialog;
    private Runnable dialogDismissRunnable = () -> {
        choose = -1;
        invalidate();
        if (mTextDialog != null && mTextDialog.getVisibility() == View.VISIBLE) {
            mTextDialog.setVisibility(View.INVISIBLE);
        }
    };

    private Map<String, Integer> isExistMap;
    private List<String> existsList;
    private int wrapHeight;

    public SideBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SideBar(Context context) {
        super(context);
        init();
    }

    public static int spToPx(float sp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public Map<String, Integer> getExistMap() {
        return isExistMap;
    }

    public void setExistMap(Map<String, Integer> existMap) {
        isExistMap = existMap;
        existsList.clear();
        for (int i = 0; i < b.length; i++) {
            if (!isExistMap.containsKey(b[i])) {
                continue;
            }
            existsList.add(b[i]);
        }
        invalidate();
    }

    public void setTextView(TextView mTextDialog) {
        this.mTextDialog = mTextDialog;
    }

    private void init() {
        isExistMap = new HashMap<String, Integer>();
        existsList = new ArrayList<>();
        paint.setTypeface(Typeface.DEFAULT);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(spToPx(11, getContext()));
        selectedBackgroundPaint.setColor(Color.parseColor("#FF333333"));
        selectedBackgroundPaint.setAntiAlias(true);
        selectedBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 获取宽-测量规则的模式和大小
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        // 获取高-测量规则的模式和大小
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // 设置wrap_content的默认宽 / 高值
        // 默认宽/高的设定并无固定依据,根据需要灵活设置
        // 类似TextView,ImageView等针对wrap_content均在onMeasure()对设置默认宽 / 高值有特殊处理,具体读者可以自行查看
        int mWidth = DisplayUtil.dip2px(getContext(), 20);
        this.wrapHeight = paint.getFontMetricsInt(null);
        int mHeight = (wrapHeight + getAlphaPadding()) * isExistMap.size();

        // 当布局参数设置为wrap_content时，设置默认值
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mWidth, mHeight);
            // 宽 / 高任意一个布局参数为= wrap_content时，都设置默认值
        } else if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mWidth, heightSize);
        } else if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(widthSize, mHeight);
        }
    }

    private int getAlphaPadding() {
        return DisplayUtil.dip2px(getContext(), 3);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isExistMap.isEmpty()) {
            return;
        }
        // 获取焦点改变背景颜色.
        int height = getHeight();// 获取对应高度
        int width = getWidth(); // 获取对应宽度
        int singleHeight = wrapHeight + getAlphaPadding();// 获取每一个字母的高度
        int top = (height - singleHeight * isExistMap.size()) / 2;

        for (int i = 0; i < existsList.size(); i++) {
            // x坐标等于中间-字符串宽度的一半.
            float xPos = width / 2;
            float yPos = singleHeight * (i + 1) + top;
            // 选中的状态
            if (i == choose) {
                paint.setColor(Color.parseColor("#FFFFFFFF"));
                // 0.3是大概ascent到baseline占比的一半，
                float centerY = (float) (yPos - wrapHeight * 0.3f);
                float radius = (float) (1.1 * wrapHeight / 2);
                canvas.drawCircle(xPos, centerY, radius, selectedBackgroundPaint);
            } else {
                paint.setColor(Color.parseColor("#FFBBBBBB"));
            }
            canvas.drawText(existsList.get(i), xPos, yPos, paint);
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float y = event.getY();// 点击y坐标
        final int oldChoose = choose;
        int height = getHeight();// 获取对应高度
        int singleHeight = wrapHeight + getAlphaPadding();// 获取每一个字母的高度
        int top = (height - singleHeight * isExistMap.size()) / 2;
        // 0.25是大概baseline到bottom的占比，
        final int c = (int) ((y - top - wrapHeight * 0.25) / singleHeight);

        switch (action) {
            case MotionEvent.ACTION_UP:
                choose = -1;//
                invalidate();
                if (mTextDialog != null) {
                    mTextDialog.setVisibility(View.INVISIBLE);
                    mTextDialog.removeCallbacks(dialogDismissRunnable);
                }
                break;
            default:
                if (oldChoose != c) {
                    if (c >= 0 && c < existsList.size()) {
                        if (onTouchingLetterChangedListener != null) {
                            String s = existsList.get(c);
                            int count = 0;
                            if (isExistMap.containsKey(s)) {
                                count = isExistMap.get(s);
                            }
                            if (count > 0) {
                                onTouchingLetterChangedListener.onTouchingLetterChanged(s);
                                if (mTextDialog != null) {
                                    mTextDialog.setText(s);
                                    mTextDialog.setVisibility(View.VISIBLE);
                                    mTextDialog.removeCallbacks(dialogDismissRunnable);
                                    mTextDialog.postDelayed(dialogDismissRunnable, 2000);
                                }
                            }
                        }
                        choose = c;
                        invalidate();
                    }
                }

                break;
        }
        return true;
    }

    /**
     * 向外公开的方法
     *
     * @param onTouchingLetterChangedListener
     */
    public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    /**
     * 接口
     *
     * @author coder
     */
    public interface OnTouchingLetterChangedListener {
        public void onTouchingLetterChanged(String s);
    }

}