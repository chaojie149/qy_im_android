package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

public class AccountSwitchActivity extends BaseActivity {

    private boolean editMode;
    private TextView tvRight;

    public AccountSwitchActivity() {
        noLoginRequired();
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, AccountSwitchActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_switch);
        initView();
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> {
            finish();
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.switch_account);

        tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(R.string.edit);
        tvRight.setOnClickListener(v -> {
            editModeToggle();
        });

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setArguments(getIntent().getExtras());
    }

    private void editModeToggle() {
        AccountSwitchFragment fragment = (AccountSwitchFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        editMode = !editMode;
        fragment.setEditMode(editMode);
        if (editMode) {
            tvRight.setText(R.string.finish);
        } else {
            tvRight.setText(R.string.edit);
        }
    }

}
