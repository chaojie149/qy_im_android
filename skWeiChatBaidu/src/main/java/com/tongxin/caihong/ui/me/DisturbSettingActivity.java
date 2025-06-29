package com.tongxin.caihong.ui.me;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.helper.DisturbHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.view.SwitchButton;
import com.tongxin.caihong.R;

import java.util.Date;

public class DisturbSettingActivity extends BaseActivity {
    private View llDisturbTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disturb_setting);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.disturb_settings);
    }

    private void initView() {
        llDisturbTime = findViewById(R.id.llDisturbTime);
        SwitchButton sbDisturb = findViewById(R.id.sbDisturb);
        boolean enabled = DisturbHelper.isEnabled(mContext);
        sbDisturb.setChecked(enabled);
        updateDisturbTimeLayout(enabled);
        sbDisturb.setOnCheckedChangeListener((view, isChecked) -> {
            DisturbHelper.saveEnabled(mContext, isChecked);
            updateDisturbTimeLayout(isChecked);
        });
        TextView tvStartTime = findViewById(R.id.tvStartTime);
        View rlStartTime = findViewById(R.id.rlStartTime);
        rlStartTime.setOnClickListener(v -> {
            Date date = DisturbHelper.getEndDate(mContext);
            new TimePickerDialog(mContext, (view, hourOfDay, minute) -> {
                date.setHours(hourOfDay);
                date.setMinutes(minute);
                DisturbHelper.saveStartDate(mContext, date);
                updateTime(tvStartTime, date);
            }, date.getHours(), date.getMinutes(), true).show();
        });
        updateTime(tvStartTime, DisturbHelper.getStartDate(mContext));
        TextView tvEndTime = findViewById(R.id.tvEndTime);
        View rlEndTime = findViewById(R.id.rlEndTime);
        rlEndTime.setOnClickListener(v -> {
            Date date = DisturbHelper.getEndDate(mContext);
            new TimePickerDialog(mContext, (view, hourOfDay, minute) -> {
                date.setHours(hourOfDay);
                date.setMinutes(minute);
                DisturbHelper.saveEndDate(mContext, date);
                updateTime(tvEndTime, date);
            }, date.getHours(), date.getMinutes(), true).show();
        });
        updateTime(tvEndTime, DisturbHelper.getEndDate(mContext));
    }

    private void updateTime(TextView tvTime, Date date) {
        tvTime.setText(TimeUtils.friendly_format1.format(date));
    }

    private void updateDisturbTimeLayout(boolean isChecked) {
        if (isChecked) {
            llDisturbTime.setVisibility(View.VISIBLE);
        } else {
            llDisturbTime.setVisibility(View.GONE);
        }
    }
}
