package com.tongxin.caihong.ui.contacts.label;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

public class LabelActivityNewUI extends BaseActivity {

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, LabelActivityNewUI.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_new_ui);
        initActionBar();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.flContainer, new LabelFragment(), "LabelFragment")
                .commitAllowingStateLoss();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.mergerStatus).setBackgroundColor(getResources().getColor(R.color.normal_bg));
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
    }
}
