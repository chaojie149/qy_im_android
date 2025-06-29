package com.tongxin.caihong.ui.me;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.view.ControlFontSize;

/**
 * Created by Administrator on 2017/12/5 0005.
 */

public class FontSizeActivity extends BaseActivity {
    private static final int BASE_FONT_SIZE = 15;
    private static final int DEFAULT_FONT_SIZE = 1;

    private ControlFontSize mControlFontSize;
    private TextView tv1, tv2;
    private int size = DEFAULT_FONT_SIZE;// 默认选中标准

    public static int getFontSize(Context ctx) {
        return PreferenceUtils.getInt(ctx, Constants.FONT_SIZE, DEFAULT_FONT_SIZE) + BASE_FONT_SIZE;
    }

    private static void saveFontSize(Context ctx, int size) {
        PreferenceUtils.putInt(ctx, Constants.FONT_SIZE, size);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_size);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.font_size));
        TextView tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(getString(R.string.finish));
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFontSize(mContext, size);
                finish();
            }
        });
    }

    private void initView() {
        TextView tvExampleMin = findViewById(R.id.tvExampleMin);
        tvExampleMin.setTextSize(BASE_FONT_SIZE);
        TextView tvExampleMax = findViewById(R.id.tvExampleMax);
        tvExampleMax.setTextSize(BASE_FONT_SIZE + 5);
        TextView tvExampleDefault = findViewById(R.id.tvExampleDefault);
        tvExampleDefault.setTextSize(BASE_FONT_SIZE + DEFAULT_FONT_SIZE);

        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        mControlFontSize = (ControlFontSize) findViewById(R.id.control_font);
        size = getFontSize(mContext) - BASE_FONT_SIZE;

        setTextSize(BASE_FONT_SIZE + size);
        mControlFontSize.setCurrentProgress(size);
        mControlFontSize.setOnPointResultListener(new ControlFontSize.OnPointResultListener() {
            @Override
            public void onPointResult(int position) {
                size = position;
                setTextSize(BASE_FONT_SIZE + size);
            }
        });
    }

    private void setTextSize(int size) {
        tv1.setTextSize(size);
        tv2.setTextSize(size);
    }
}
