package com.tongxin.caihong.ui.lock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.PasswordInputView;

public class DeviceLockActivity extends BaseActivity {
    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, DeviceLockActivity.class);
        ctx.startActivity(intent);
    }

    public static void verify(Activity ctx, int requestCode) {
        Intent intent = new Intent(ctx, DeviceLockActivity.class);
        ctx.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_lock);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);

        PasswordInputView passwordInputView = findViewById(R.id.passwordInputView);
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
                    if (DeviceLockHelper.checkPassword(s.toString())) {
                        setResult(Activity.RESULT_OK);
                        DeviceLockHelper.unlock();
                        finish();
                    } else {
                        passwordInputView.setText("");
                        DialogHelper.tip(DeviceLockActivity.this, getString(R.string.tip_device_lock_password_incorrect));
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
