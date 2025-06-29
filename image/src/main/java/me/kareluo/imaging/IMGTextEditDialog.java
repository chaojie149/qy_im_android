package me.kareluo.imaging;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import me.kareluo.imaging.core.IMGText;
import me.kareluo.imaging.view.IMGColorGroup;

/**
 * Created by felix on 2017/12/1 上午11:21.
 */

public class IMGTextEditDialog extends Dialog implements View.OnClickListener,
        RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "IMGTextEditDialog";

    private EditText mEditText;
    private TextView tvExample;
    private IMGText currentText = new IMGText(null, Color.WHITE, false);

    private Callback mCallback;

    private IMGText mDefaultText;

    private IMGColorGroup mColorGroup;

    public IMGTextEditDialog(Context context, Callback callback) {
        super(context, R.style.ImageTextDialog);
        setContentView(R.layout.image_text_dialog);
        mCallback = callback;
        Window window = getWindow();
        if (window != null) {
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mColorGroup = findViewById(R.id.cg_colors);
        mColorGroup.setOnCheckedChangeListener(this);
        mEditText = findViewById(R.id.et_text);
        tvExample = findViewById(R.id.tvExample);
        tvExample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentText.setTintBackground(!currentText.isTintBackground());
                updateColor();
            }
        });

        findViewById(R.id.tv_cancel).setOnClickListener(this);
        findViewById(R.id.tv_done).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDefaultText != null) {
            mEditText.setText(mDefaultText.getText());
            mEditText.setTextColor(mDefaultText.getColor());
            if (!mDefaultText.isEmpty()) {
                mEditText.post(new Runnable() {
                    @Override
                    public void run() {
                        mEditText.setSelection(mEditText.length());
                    }
                });
            }
            currentText = mDefaultText;
            updateColor();
            mDefaultText = null;
            mColorGroup.setCheckColor(currentText.getColor());
        } else {
            mEditText.setText("");
            mColorGroup.setCheckColor(mEditText.getCurrentTextColor());
        }
        mEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, InputMethodManager.SHOW_FORCED);
    }

    public void setText(IMGText text) {
        mDefaultText = text;
    }

    public void reset() {
        setText(new IMGText(null, Color.WHITE, false));
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.tv_done) {
            onDone();
        } else if (vid == R.id.tv_cancel) {
            dismiss();
        }
    }

    private void onDone() {
        String text = mEditText.getText().toString();
        currentText.setText(text);
        if (!TextUtils.isEmpty(text) && mCallback != null) {
            mCallback.onText(currentText.copy());
        }
        dismiss();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        currentText.setColor(mColorGroup.getCheckColor());
        updateColor();
    }

    private void updateColor() {
        mEditText.setTextColor(currentText.getRealColor());
        if (currentText.isTintBackground()) {
            Spannable spannable = mEditText.getText();
            spannable.setSpan(new BackgroundColorSpan(currentText.getRealBackgroundColor()), 0, spannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            mEditText.setText(spannable);
            tvExample.setTextColor(Color.WHITE);
            tvExample.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7c7c7c")));
        } else {
            // toString清背景色，
            mEditText.setText(mEditText.getText().toString());
            tvExample.setTextColor(Color.parseColor("#7c7c7c"));
            tvExample.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));

        }
    }

    public interface Callback {

        void onText(IMGText text);
    }
}
