package com.tongxin.caihong.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;

public class SelectSexView extends LinearLayout {
    private TextView tvMan;
    private TextView tvWoman;
    private OnSexChangeListener onSexChangeListener;

    public SelectSexView(Context context) {
        this(context, null);
    }

    public SelectSexView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectSexView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = View.inflate(getContext(), R.layout.select_sex_view, this);
        tvMan = view.findViewById(R.id.tvMan);
        tvWoman = view.findViewById(R.id.tvWoman);

        tvMan.setOnClickListener(view1 -> sexChange(1, true));
        tvWoman.setOnClickListener(view1 -> sexChange(0, true));
    }

    public void setOnSexChangeListener(OnSexChangeListener onSexChangeListener) {
        this.onSexChangeListener = onSexChangeListener;
    }

    public void setSex(int sex) {
        sexChange(sex, false);
    }

    private void sexChange(int sex, boolean notify) {
        if (sex == 1) {
            tvMan.setTextColor(getResources().getColor(R.color.team_text_sex_man));
            tvMan.setBackground(getResources().getDrawable(R.drawable.bg_sex_man));
            tvWoman.setTextColor(getResources().getColor(R.color.team_text_sex_not_selected));
            tvWoman.setBackground(getResources().getDrawable(R.drawable.bg_sex_woman_not_selected));
        } else {
            tvMan.setTextColor(getResources().getColor(R.color.team_text_sex_not_selected));
            tvMan.setBackground(getResources().getDrawable(R.drawable.bg_sex_man_not_selected));
            tvWoman.setTextColor(getResources().getColor(R.color.team_text_sex_woman));
            tvWoman.setBackground(getResources().getDrawable(R.drawable.bg_sex_woman));
        }
        if (notify) {
            if (onSexChangeListener != null) {
                onSexChangeListener.sexChange(sex);
            }
        }
    }

    public interface OnSexChangeListener {
        void sexChange(int sex);
    }
}
