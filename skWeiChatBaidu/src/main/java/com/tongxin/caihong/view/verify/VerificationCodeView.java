package com.tongxin.caihong.view.verify;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tongxin.caihong.R;

public class VerificationCodeView extends RelativeLayout {
    private LinearLayout containerEt;
    private PwdEditText et;
    private int mEtNumber;
    private int mEtWidth;
    private Drawable mEtDividerDrawable;
    private int mEtTextColor;
    private float mEtTextSize;
    private Drawable mEtBackgroundDrawableFocus;
    private Drawable mEtBackgroundDrawableNormal;
    private boolean mEtPwd;
    private float mEtPwdRadius;
    private PwdTextView[] mPwdTextViews;
    private MyTextWatcher myTextWatcher;
    private InputCompleteListener inputCompleteListener;

    public VerificationCodeView(Context context) {
        this(context, (AttributeSet) null);
    }

    public VerificationCodeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.myTextWatcher = new MyTextWatcher();
        this.init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.layout_identifying_code, this);
        this.containerEt = (LinearLayout) this.findViewById(R.id.container_et);
        this.et = (PwdEditText) this.findViewById(R.id.et);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerificationCodeView, defStyleAttr, 0);
        this.mEtNumber = typedArray.getInteger(R.styleable.VerificationCodeView_icv_et_number, 1);
        this.mEtWidth = typedArray.getDimensionPixelSize(R.styleable.VerificationCodeView_icv_et_width, 42);
        this.mEtDividerDrawable = typedArray.getDrawable(R.styleable.VerificationCodeView_icv_et_divider_drawable);
        this.mEtTextSize = (float) typedArray.getDimensionPixelSize(R.styleable.VerificationCodeView_icv_et_text_size, (int) this.sp2px(16.0F, context));
        this.mEtTextColor = typedArray.getColor(R.styleable.VerificationCodeView_icv_et_text_color, -16777216);
        this.mEtBackgroundDrawableFocus = typedArray.getDrawable(R.styleable.VerificationCodeView_icv_et_bg_focus);
        this.mEtBackgroundDrawableNormal = typedArray.getDrawable(R.styleable.VerificationCodeView_icv_et_bg_normal);
        this.mEtPwd = typedArray.getBoolean(R.styleable.VerificationCodeView_icv_et_pwd, false);
        this.mEtPwdRadius = (float) typedArray.getDimensionPixelSize(R.styleable.VerificationCodeView_icv_et_pwd_radius, 0);
        typedArray.recycle();
        if (this.mEtDividerDrawable == null) {
            this.mEtDividerDrawable = context.getResources().getDrawable(R.drawable.shape_divider_identifying);
        }

        if (this.mEtBackgroundDrawableFocus == null) {
            this.mEtBackgroundDrawableFocus = context.getResources().getDrawable(R.drawable.shape_icv_et_bg_focus);
        }

        if (this.mEtBackgroundDrawableNormal == null) {
            this.mEtBackgroundDrawableNormal = context.getResources().getDrawable(R.drawable.shape_icv_et_bg_normal);
        }

        this.initUI();
    }

    private void initUI() {
        this.initTextViews(this.getContext(), this.mEtNumber, this.mEtWidth, this.mEtDividerDrawable, this.mEtTextSize, this.mEtTextColor);
        this.initEtContainer(this.mPwdTextViews);
        this.setListener();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mHeightMeasureSpec = heightMeasureSpec;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == -2147483648) {
            mHeightMeasureSpec = MeasureSpec.makeMeasureSpec((int) this.dp2px(50.0F, this.getContext()), 1073741824);
        }

        super.onMeasure(widthMeasureSpec, mHeightMeasureSpec);
    }

    private void initTextViews(Context context, int etNumber, int etWidth, Drawable etDividerDrawable, float etTextSize, int etTextColor) {
        this.et.setCursorVisible(false);
        this.et.setFilters(new InputFilter[]{new LengthFilter(etNumber)});
        if (etDividerDrawable != null) {
            etDividerDrawable.setBounds(0, 0, etDividerDrawable.getMinimumWidth(), etDividerDrawable.getMinimumHeight());
            this.containerEt.setDividerDrawable(etDividerDrawable);
        }

        this.mPwdTextViews = new PwdTextView[etNumber];

        for (int i = 0; i < this.mPwdTextViews.length; ++i) {
            PwdTextView textView = new PwdTextView(context);
            textView.setTextSize(0, etTextSize);
            textView.setTextColor(etTextColor);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setWidth(etWidth);
            textView.setHeight(etWidth);
            if (i == 0) {
                textView.setBackgroundDrawable(this.mEtBackgroundDrawableFocus);
            } else {
                textView.setBackgroundDrawable(this.mEtBackgroundDrawableNormal);
            }

            textView.setGravity(17);
            textView.setFocusable(false);
            this.mPwdTextViews[i] = textView;
        }

    }

    private void initEtContainer(TextView[] mTextViews) {
        TextView[] var2 = mTextViews;
        int var3 = mTextViews.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            TextView mTextView = var2[var4];
            this.containerEt.addView(mTextView);
        }

    }

    private void setListener() {
        this.et.addTextChangedListener(this.myTextWatcher);
        this.et.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 67 && event.getAction() == 0) {
                    onKeyDelete();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void setText(String inputContent) {
        for (int i = 0; i < this.mPwdTextViews.length; ++i) {
            PwdTextView tv = this.mPwdTextViews[i];
            if (tv.getText().toString().trim().equals("")) {
                if (this.mEtPwd) {
                    tv.drawPwd(this.mEtPwdRadius);
                }

                tv.setText(inputContent);
                if (this.inputCompleteListener != null) {
                    this.inputCompleteListener.inputComplete();
                }

                tv.setBackgroundDrawable(this.mEtBackgroundDrawableNormal);
                if (i < this.mEtNumber - 1) {
                    this.mPwdTextViews[i + 1].setBackgroundDrawable(this.mEtBackgroundDrawableFocus);
                }
                break;
            }
        }

    }

    private void onKeyDelete() {
        for (int i = this.mPwdTextViews.length - 1; i >= 0; --i) {
            PwdTextView tv = this.mPwdTextViews[i];
            if (!tv.getText().toString().trim().equals("")) {
                if (this.mEtPwd) {
                    tv.clearPwd();
                }

                tv.setText("");
                if (this.inputCompleteListener != null) {
                    this.inputCompleteListener.deleteContent();
                }

                tv.setBackgroundDrawable(this.mEtBackgroundDrawableFocus);
                if (i < this.mEtNumber - 1) {
                    this.mPwdTextViews[i + 1].setBackgroundDrawable(this.mEtBackgroundDrawableNormal);
                }
                break;
            }
        }

    }

    public String getInputContent() {
        StringBuffer buffer = new StringBuffer();
        PwdTextView[] var2 = this.mPwdTextViews;
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            TextView tv = var2[var4];
            buffer.append(tv.getText().toString().trim());
        }

        return buffer.toString();
    }

    public void clearInputContent() {
        for (int i = 0; i < this.mPwdTextViews.length; ++i) {
            if (i == 0) {
                this.mPwdTextViews[i].setBackgroundDrawable(this.mEtBackgroundDrawableFocus);
            } else {
                this.mPwdTextViews[i].setBackgroundDrawable(this.mEtBackgroundDrawableNormal);
            }

            if (this.mEtPwd) {
                this.mPwdTextViews[i].clearPwd();
            }

            this.mPwdTextViews[i].setText("");
        }

    }

    public int getEtNumber() {
        return this.mEtNumber;
    }

    public void setEtNumber(int etNumber) {
        this.mEtNumber = etNumber;
        this.et.removeTextChangedListener(this.myTextWatcher);
        this.containerEt.removeAllViews();
        this.initUI();
    }

    public void setPwdMode(boolean isPwdMode) {
        this.mEtPwd = isPwdMode;
    }

    public EditText getEditText() {
        return this.et;
    }

    public void setInputCompleteListener(InputCompleteListener inputCompleteListener) {
        this.inputCompleteListener = inputCompleteListener;
    }

    public float dp2px(float dpValue, Context context) {
        return TypedValue.applyDimension(1, dpValue, context.getResources().getDisplayMetrics());
    }

    public float sp2px(float spValue, Context context) {
        return TypedValue.applyDimension(2, spValue, context.getResources().getDisplayMetrics());
    }

    public interface InputCompleteListener {
        void inputComplete();

        void deleteContent();
    }

    private class MyTextWatcher implements TextWatcher {
        private MyTextWatcher() {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable editable) {
            String inputStr = editable.toString();
            if (!TextUtils.isEmpty(inputStr)) {
                String[] strArray = inputStr.split("");

                for (int i = 0; i < strArray.length && i <= mEtNumber; ++i) {
                    setText(strArray[i]);
                    et.setText("");
                }
            }

        }
    }
}
