package com.tongxin.caihong.ui.webox;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.bean.webox.WeboxRecord;
import com.tongxin.caihong.bean.webox.WeboxRecordsItem;
import com.tongxin.caihong.helper.DatePickerHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseListActivity;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

/**
 * Created by wzw on 2016/9/26.
 */
public class WeboxRecordActivity extends BaseListActivity<WeboxRecordActivity.MyConsumeHolder> {
    private static final String TAG = "WeboxRecordActivity";
    List<WeboxRecordsItem> datas = new ArrayList<>();
    private TextView tvStartTime;
    private TextView tvEndTime;
    private View ivQuery;
    private DatePickerHelper.QueryWrap queryWrap = new DatePickerHelper.QueryWrap();

    public static void start(Context context) {
        Intent starter = new Intent(context, WeboxRecordActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_webox_record;
    }

    @Nullable
    @Override
    protected Integer getMiddleDivider() {
        return R.drawable.divider_consume_record;
    }

    @Override
    public void initView() {
        super.initView();
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getResources().getString(R.string.bill));
        initSearch();
    }

    private void initSearch() {
        View llQuery = findViewById(R.id.llQuery);
        tvStartTime = llQuery.findViewById(R.id.tvStartTime);
        tvEndTime = llQuery.findViewById(R.id.tvEndTime);
        ivQuery = llQuery.findViewById(R.id.ivQuery);
        tvStartTime.setOnClickListener(v -> selectData(true));
        tvEndTime.setOnClickListener(v -> selectData(false));
        ivQuery.setOnClickListener(v -> query());
        resetQueryData();
    }

    private void resetQueryData() {
        // 默认间隔15天，
        DatePickerHelper.resetQueryData(queryWrap, 15);
        showDate(tvStartTime, queryWrap.startTime);
        showDate(tvEndTime, queryWrap.endTime);
    }

    private void query() {
        pager = 0;
        initDatas(pager);
    }

    private void selectData(boolean startTime) {
        DatePickerHelper.selectData(mContext, queryWrap, startTime, time -> {
            if (startTime) {
                showDate(tvStartTime, time);
            } else {
                showDate(tvEndTime, time);
            }
        });
    }

    private void showDate(TextView tv, long time) {
        tv.setText(TimeUtils.s_long_2_str(time));
    }

    @Override
    public void initDatas(int pager) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 如果是下拉刷新就重新加载第一页
        params.put("startDateTime", TimeUtils.s_format.format(new Date(queryWrap.startTime)));
        params.put("endDateTime", TimeUtils.s_format.format(new Date(queryWrap.endTime)));
        params.put("pageIndex", String.valueOf(pager));
        params.put("pageSize", String.valueOf(PAGE_SIZE));
        HttpUtils.get().url(coreManager.getConfig().WEBOX_RECORD_RED)
                .params(params)
                .build()
                .execute(new BaseCallback<WeboxRecord>(WeboxRecord.class) {

                    @Override
                    public void onResponse(ObjectResult<WeboxRecord> result) {
                        if (result.getData() != null
                                && result.getData().getRecords() != null) {
                            if (pager == 0) {
                                datas.clear();
                            }
                            datas.addAll(result.getData().getRecords());
                            if (result.getData().getRecords().size() != PAGE_SIZE) {
                                more = false;
                            } else {
                                more = true;
                            }
                        } else {
                            more = false;
                        }
                        runOnUiThread(() -> update(datas));
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(WeboxRecordActivity.this);
                    }
                });
    }

    @Override
    public MyConsumeHolder initHolder(ViewGroup parent) {
        View v = mInflater.inflate(R.layout.consumerecord_item, parent, false);
        MyConsumeHolder holder = new MyConsumeHolder(v);
        return holder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void fillData(MyConsumeHolder holder, int position) {
        WeboxRecordsItem item = datas.get(position);
        if (item != null) {
            holder.timeTv.setText(item.getCreateDateTime());
            String[] stringArray = getResources().getStringArray(R.array.webox_record_type);
            List<String> typeList = Arrays.asList("WEBOX_RECHARGE", "WEBOX_REDPACKET", "WEBOX_TRANSFER", "WEBOX_WITHHOLDING", "WEBOX_REDPACKET_REFUND", "WEBOX_TRANSFER_REFUND", "WEBOX_MERCHANT_RECHARGE", "WEBOX_APP_PAY", "WEBOX_APP_PAY_REFUND", "SPLIT_PAYMENT", "SPLIT_REFUND_PAYMENT");
            holder.nameTv.setText(stringArray[typeList.indexOf(item.getTradeType())]);
            if (TextUtils.equals(item.getDirection(), "DECREASE")) {
                holder.moneyTv.setTextColor(getResources().getColor(R.color.records_of_consumption));
                holder.moneyTv.setText("-" + Money.fromCent(item.getAmount()));
            } else {
                holder.moneyTv.setTextColor(getResources().getColor(R.color.ji_jian_lan));
                holder.moneyTv.setText("+" + Money.fromCent(item.getAmount()));
            }
        }
    }

    static class MyConsumeHolder extends RecyclerView.ViewHolder {
        private TextView nameTv, timeTv, moneyTv;

        MyConsumeHolder(View itemView) {
            super(itemView);
            nameTv = (TextView) itemView.findViewById(R.id.textview_name);
            timeTv = (TextView) itemView.findViewById(R.id.textview_time);
            moneyTv = (TextView) itemView.findViewById(R.id.textview_money);
        }
    }

}
