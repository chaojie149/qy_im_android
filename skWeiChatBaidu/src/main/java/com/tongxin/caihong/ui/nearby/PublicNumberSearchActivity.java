package com.tongxin.caihong.ui.nearby;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;

public class PublicNumberSearchActivity extends BaseActivity {
    private EditText mKeyWordEdit;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, PublicNumberSearchActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_number_search);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.search_public_number);
        initView();
    }

    private void initView() {
        mKeyWordEdit = findViewById(R.id.keyword_edit);
        // 获取焦点，键盘弹出
        mKeyWordEdit.requestFocus();

        Button mSearchBtn = findViewById(R.id.search_btn);
        ButtonColorChange.colorChange(this, mSearchBtn);
        mSearchBtn.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(mKeyWordEdit.getText().toString().trim())) {
                PublicNumberListActivity.start(mContext, mKeyWordEdit.getText().toString());
            }
        });
    }
}
