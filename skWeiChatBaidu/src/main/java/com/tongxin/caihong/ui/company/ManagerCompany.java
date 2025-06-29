package com.tongxin.caihong.ui.company;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

public class ManagerCompany extends BaseActivity {

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, ManagerCompany.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_company);
        initView();
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> {
            finish();
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.service_title_grade);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setArguments(getIntent().getExtras());
    }
}
