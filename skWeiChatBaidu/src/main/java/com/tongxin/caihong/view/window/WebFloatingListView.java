package com.tongxin.caihong.view.window;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;

public class WebFloatingListView extends LinearLayout {
    public WebFloatingListView(Context context) {
        super(context);
        init();
    }

    public WebFloatingListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WebFloatingListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WebFloatingListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.dialog_web_floating_list, this, true);

    }
}
