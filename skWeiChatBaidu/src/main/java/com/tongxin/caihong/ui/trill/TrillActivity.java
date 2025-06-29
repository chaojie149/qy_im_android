package com.tongxin.caihong.ui.trill;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;


/**
 * 抖音模块
 */
public class TrillActivity extends BaseActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trill_wrap);
        getSupportActionBar().hide();
    }
}

