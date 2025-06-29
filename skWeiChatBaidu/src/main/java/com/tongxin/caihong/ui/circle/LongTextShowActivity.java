package com.tongxin.caihong.ui.circle;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.link.HttpTextView;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

public class LongTextShowActivity extends BaseActivity {

    private String body;

    public static void start(Context ctx, String body) {
        Intent intent = new Intent(ctx, LongTextShowActivity.class);
        intent.putExtra("body", body);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_text_show);
        body = getIntent().getStringExtra("body");
        iniActionBar();
        initView();
    }

    private void iniActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.weibo_cell_all_text));
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        HttpTextView mBodyTv = findViewById(R.id.body_tv);
        String content = StringUtils.replaceSpecialChar(body);
        CharSequence charSequence = HtmlUtils.transform200SpanString(content, true);
        mBodyTv.setUrlText(charSequence);

/*
        mBodyTv.setOnClickListener(v -> {
            copy(charSequence);
        });
        mBodyTv.setOnLongClickListener(v -> {
            copy(charSequence);
            return true;
        });
*/
    }

    private void copy(CharSequence charSequence) {
        ClipboardManager cmb = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(charSequence);
        Toast.makeText(mContext, getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
    }
}
