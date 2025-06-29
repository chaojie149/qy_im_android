package com.tongxin.caihong.ui.me.redpacket;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.view.PasswordInputView;

public class PayPasswordVerifyDialog extends Dialog {
    private TextView tvAction;
    private View llMoney;
    private TextView tvMoney;
    private PasswordInputView passwordInputView;

    private String action;
    private String money;

    private OnInputFinishListener onInputFinishListener;
    private Context ctx;

    public PayPasswordVerifyDialog(@NonNull Context context) {
        super(context, R.style.MyDialog);
        this.ctx = context;
    }

    public PayPasswordVerifyDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.ctx = context;
    }

    protected PayPasswordVerifyDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.ctx = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay_password_verify_dialog);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        findViewById(R.id.ivClose).setOnClickListener(v -> {
            cancel();
        });
        tvAction = findViewById(R.id.tvAction);
        if (action != null) {
            tvAction.setText(action);
        }
        llMoney = findViewById(R.id.llMoney);
        tvMoney = findViewById(R.id.tvMoney);
        if (!TextUtils.isEmpty(money)) {
            tvMoney.setText(money);
            llMoney.setVisibility(View.VISIBLE);
        } else {
            llMoney.setVisibility(View.GONE);
        }
        passwordInputView = findViewById(R.id.passwordInputView);
        passwordInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == AppConstant.PASS_WORD_LENGTH) {
                    dismiss();
                    if (onInputFinishListener != null) {
                        onInputFinishListener.onInputFinish(s.toString());
                    }
                }
            }
        });
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.8);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * dialog show 时，强制弹出软键盘
     */
    @Override
    public void show() {
        super.show();
        if (passwordInputView != null) {
            passwordInputView.setFocusable(true);
            passwordInputView.setFocusableInTouchMode(true);
            passwordInputView.requestFocus();
            InputMethodManager ctxSystemService = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (ctxSystemService != null) {
                ctxSystemService.showSoftInput(passwordInputView, 0);
            }
        }
    }

    public void setAction(String action) {
        this.action = action;
        if (tvAction != null) {
            tvAction.setText(action);
        }
    }

    public void setMoney(String money) {
        this.money = money;
        if (tvMoney != null) {
            tvMoney.setText(money);
        }
        if (llMoney != null) {
            if (!TextUtils.isEmpty(money)) {
                llMoney.setVisibility(View.VISIBLE);
            } else {
                llMoney.setVisibility(View.GONE);
            }
        }
    }

    public void setOnInputFinishListener(OnInputFinishListener onInputFinishListener) {
        this.onInputFinishListener = onInputFinishListener;
    }

    public interface OnInputFinishListener {
        void onInputFinish(String password);
    }
}
