package com.tongxin.caihong.ui.message.multi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.ToastUtil;

public class ProclamationActivity extends BaseActivity {
    private EditText etNotice;
    private String noticeId;
    private String text;

    public static void start(Activity activity, String noticeId, String text) {
        Intent intent = new Intent(activity, ProclamationActivity.class);
        intent.putExtra("noticeId", noticeId);
        intent.putExtra("text", text);
        activity.startActivityForResult(intent, NoticeListActivity.PROCLAMATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proclamation);
        noticeId = getIntent().getStringExtra("noticeId");
        text = getIntent().getStringExtra("text");
        initActionBar();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.group_bulletin);
    }

    private void initView() {
        etNotice = findViewById(R.id.etNotice);
        if (!TextUtils.isEmpty(text)) {
            etNotice.setText(text);
            etNotice.setSelection(text.length());
        }
        ButtonColorChange.colorChange(mContext, findViewById(R.id.btnPublish));
        findViewById(R.id.btnPublish).setOnClickListener(view -> {
            String input = etNotice.getText().toString();
            if (!TextUtils.isEmpty(input)) {
                Intent intent = new Intent();
                if (!TextUtils.isEmpty(noticeId)) {
                    // 编辑
                    if (TextUtils.equals(text, input)) {
                        // 内容未改变，直接finish
                        finish();
                        return;
                    }
                    intent.putExtra("noticeId", noticeId);
                }
                intent.putExtra("text", input);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                ToastUtil.showToast(mContext, getString(R.string.notice_cannot_null));
            }
        });
    }
}
