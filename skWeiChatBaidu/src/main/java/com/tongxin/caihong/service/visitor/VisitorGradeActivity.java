package com.tongxin.caihong.service.visitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

import java.util.concurrent.TimeUnit;

public class VisitorGradeActivity extends BaseActivity implements VisitorGradeFragment.OnSuccessListener {
    public static long sLastGradeTime = 0;

    public static void start(Activity context, int requestCode, String friendId, String toId, @Nullable String packetId) {
        if (System.currentTimeMillis() - sLastGradeTime < TimeUnit.SECONDS.toMillis(10)) {
            ToastUtil.showToast(context, context.getString(R.string.service_tip_grade_too_fast));
            return;
        }
        Intent starter = new Intent(context, VisitorGradeActivity.class);
        starter.putExtra("toId", toId);
        starter.putExtra("packetId", packetId);
        starter.putExtra("friendId", friendId);
        context.startActivityForResult(starter, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_visitor_grade);
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

    @Override
    public void onSuccess() {
        setResult(Activity.RESULT_OK, getIntent());
        finish();
    }
}
