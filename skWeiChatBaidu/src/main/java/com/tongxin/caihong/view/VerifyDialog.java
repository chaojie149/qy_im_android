package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.PasswordHelper;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.ScreenUtil;

import java.sql.SQLException;

public class VerifyDialog extends Dialog {
    private TextView mTitleTv;
    private TextView mTipTv;
    private EditText mVerifyEdit;
    private TextView mCancel;
    private TextView mSend;

    private String mTtitle;
    private String mTip;
    private CharSequence mHint;
    private String mText;
    private int mInputLength;
    private VerifyClickListener mVerifyClickListener;
    private boolean dismiss = true;
    private boolean password = false;
    @StringRes
    private Integer cancel;
    private Integer ok;

    public VerifyDialog(Context context) {
        super(context, R.style.MyDialog);
    }

    public void setVerifyClickListener(String title, String tip, String hint, VerifyClickListener verifyClickListener) {
        setVerifyClickListener(title, tip, hint, "", 0, verifyClickListener);
    }

    public void setVerifyClickListener(String title, String tip, String hint, String text, VerifyClickListener verifyClickListener) {
        setVerifyClickListener(title, tip, hint, text, 0, verifyClickListener);
    }

    public void setVerifyClickListener(String title, String tip, CharSequence hint, String text, int inputLength, VerifyClickListener verifyClickListener) {
        this.mTtitle = title;
        this.mTip = tip;
        this.mHint = hint;
        this.mText = text;
        this.mInputLength = inputLength;
        this.mVerifyClickListener = verifyClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_dialog);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        mTitleTv = (TextView) findViewById(R.id.title_tv);
        mTipTv = (TextView) findViewById(R.id.tip_tv);
        if (!TextUtils.isEmpty(mTtitle)) {
            mTitleTv.setText(mTtitle);
        } else {
            mTitleTv.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mTip)) {
            mTipTv.setText(mTip);
        } else {
            mTipTv.setVisibility(View.GONE);
        }
        mVerifyEdit = (EditText) findViewById(R.id.verify_et);
        if (!TextUtils.isEmpty(mHint)) {
            mVerifyEdit.setHint(mHint);
        }
        if (!TextUtils.isEmpty(mText)) {
            mVerifyEdit.setText(mText);
            mVerifyEdit.setSelection(mText.length());
        }
        if (mInputLength != 0) {
            mVerifyEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mInputLength)});
        }
        mCancel = (TextView) findViewById(R.id.cancel);
        mSend = (TextView) findViewById(R.id.send);
        ButtonColorChange.textChange(this.getContext(), mSend);
        if (cancel != null) {
            mCancel.setText(cancel);
        }
        if (ok != null) {
            mSend.setText(ok);
        }
        updatePasswordMode();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.9);
        initEvent();
    }

    private void updatePasswordMode() {
        if (mVerifyEdit == null) {
            return;
        }
        if (password) {
            findViewById(R.id.tbEye).setVisibility(View.VISIBLE);
            PasswordHelper.bindPasswordEye(mVerifyEdit, findViewById(R.id.tbEye));
        }
    }

    private void initEvent() {
        mCancel.setOnClickListener(v -> {
            if (dismiss) {
                dismiss();
            }
            if (mVerifyClickListener != null) {
                mVerifyClickListener.cancel();
            }
        });

        mSend.setOnClickListener(v -> {
            if (dismiss) {
                dismiss();
            }
            String str = mVerifyEdit.getText().toString().trim();
            if (mVerifyClickListener != null) {
                try {
                    mVerifyClickListener.send(str);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public EditText getVerifyEdit() {
        return mVerifyEdit;
    }

    public void setDismiss(boolean dismiss) {
        this.dismiss = dismiss;
    }

    public void setPasswordMode() {
        this.password = true;
        updatePasswordMode();
    }

    public void setCancelButton(@StringRes int cancel) {
        this.cancel = cancel;
        if (mCancel != null) {
            mCancel.setText(cancel);
        }
    }

    public void setOkButton(@StringRes int ok) {
        this.ok = ok;
        if (mSend != null) {
            mSend.setText(ok);
        }
    }

    public interface VerifyClickListener {
        void cancel();

        void send(String str) throws SQLException;
    }
}
