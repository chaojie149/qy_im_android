package com.tongxin.caihong.ui.nearby;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;

/**
 * 添加好友
 */
public class UserSearchActivity extends BaseActivity implements View.OnClickListener {
    private TextView mKeyWordText;
    private EditText mKeyWordEdit;

    public static void start(Context ctx) {
        FriendAddActivity.start(ctx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);
        initActionBar();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.add_friend));
    }

    private void initView() {
        mKeyWordText = findViewById(R.id.keyword_text);
        mKeyWordEdit = findViewById(R.id.keyword_edit);
        // 获取焦点，键盘弹出
        mKeyWordEdit.requestFocus();

        UsernameHelper.initSearchLabel(mKeyWordText, coreManager.getConfig());
        UsernameHelper.initSearchEdit(mKeyWordEdit, coreManager.getConfig());

        Button mSearchBtn = findViewById(R.id.search_btn);
        ButtonColorChange.colorChange(this, mSearchBtn);
        mSearchBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_btn:
                if (!TextUtils.isEmpty(mKeyWordEdit.getText().toString().trim())) {
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (inputManager != null) {
                        inputManager.hideSoftInputFromWindow(findViewById(R.id.main_content).getWindowToken(), 0); //强制隐藏键盘
                    }

                    Intent intent = new Intent(mContext, UserListGatherActivity.class);
                    intent.putExtra("key_word", mKeyWordEdit.getText().toString());
                    startActivity(intent);
                }
                break;
        }
    }
}
