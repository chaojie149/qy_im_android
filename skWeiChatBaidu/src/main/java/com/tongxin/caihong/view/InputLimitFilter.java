package com.tongxin.caihong.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

import com.tongxin.caihong.R;

public class InputLimitFilter extends InputFilter.LengthFilter {
    private Context ctx;
    private Toast toast;
    private boolean toastEnable;

    public InputLimitFilter(Context ctx, int max) {
        this(ctx, max, true);
    }

    public InputLimitFilter(Context ctx, int max, boolean toastEnable) {
        super(max);
        this.ctx = ctx;
        this.toastEnable = toastEnable;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        CharSequence ret = super.filter(source, start, end, dest, dstart, dend);
        if (ret != null && toastEnable) {
            toastTextLimit();
        }
        return ret;
    }

    @SuppressLint("ShowToast")
    private void toastTextLimit() {
        if (toast == null) {
            toast = Toast.makeText(ctx, ctx.getString(R.string.input_most_length, getMax()), Toast.LENGTH_SHORT);
        }
        toast.show();
    }
}
