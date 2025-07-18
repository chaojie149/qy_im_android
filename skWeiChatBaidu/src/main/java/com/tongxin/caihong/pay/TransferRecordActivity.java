package com.tongxin.caihong.pay;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.bean.TransferRecord;
import com.example.qrcode.Constant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.DateFormatUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class TransferRecordActivity extends BaseActivity {
    private RecyclerView rl_transfer;
    private TransferProAdapter proxyAdapter;
    private List<TransferRecord.PageDataBean> pageDataBeans = new ArrayList<>();
    private String mFriendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_record);
        mFriendId = getIntent().getStringExtra(Constant.TRANSFE_RRECORD);
        initActionBar();
        initView();
        initTransfer();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener((view) -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.transfer_record));
    }

    private void initView() {
        rl_transfer = findViewById(R.id.rl_transfer);
        rl_transfer.setLayoutManager(new LinearLayoutManager(TransferRecordActivity.this));
    }

    private void initTransfer() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("pageIndex", "0");
        params.put("pageSize", "50");
        HttpUtils.get().url(coreManager.getConfig().TRANSACTION_RECORD)
                .params(params)
                .build()
                .execute(new BaseCallback<TransferRecord>(TransferRecord.class) {
                    @Override
                    public void onResponse(ObjectResult<TransferRecord> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            // 类型·
                            // 1:用户充值, 支出
                            // 2:用户提现, 支出
                            // 3:后台充值, 支出
                            // 4:发红包,支出
                            // 5:领取红包,收入
                            // 6:红包退款收入
                            // 7:转账，支出
                            // 8:接受转账，收入
                            // 9:转账退回，收入
                            // 10:付款码付款，支出
                            // 11:付款码到账，收入
                            // 12:二维码付款，支出
                            // 13:二维码到账，收入
                            if (result.getData() != null) {
                                pageDataBeans = result.getData().getPageData();
                            }
                            if (pageDataBeans == null || pageDataBeans.size() == 0) {
                                findViewById(R.id.rlNoData).setVisibility(View.VISIBLE);
                                return;
                            }
                            int lastMonth = -1;
                            int lastIndex = 0;
                            double totalOutMoney = 0f;
                            double totalInMoney = 0f;

                            SparseArray<TransferRecord.PageDataBean> array = new SparseArray<>();
                            int size = pageDataBeans.size();
                            for (int i = 0; i < size; ++i) {
                                TransferRecord.PageDataBean bean = pageDataBeans.get(i);
                                String d = DateFormatUtil.timeMonthI(bean.getTime());
                                int month = Integer.valueOf(d);

                                if (lastMonth != -1 && month != lastMonth) {
                                    TransferRecord.PageDataBean dataBean = new TransferRecord.PageDataBean();
                                    dataBean.setIsTitle(true);
                                    dataBean.setTotalInMoney(totalInMoney);
                                    dataBean.setTotalOutMoney(totalOutMoney);
                                    dataBean.setMonth(lastMonth);

                                    array.put(lastIndex, dataBean);
                                    totalInMoney = 0;
                                    totalOutMoney = 0;
                                    lastIndex = i;
                                }
                                int type = pageDataBeans.get(i).getType();
                                if (type == 1 || type == 2 || type == 3 || type == 4 || type == 7 || type == 10 || type == 12) {
                                    totalOutMoney += bean.getMoney();

                                } else {
                                    totalInMoney += bean.getMoney();

                                }
                                lastMonth = month;
                                if (i == pageDataBeans.size() - 1) {
                                    TransferRecord.PageDataBean dataBean = new TransferRecord.PageDataBean();
                                    dataBean.setIsTitle(true);
                                    dataBean.setTotalInMoney(totalInMoney);
                                    dataBean.setTotalOutMoney(totalOutMoney);
                                    dataBean.setMonth(lastMonth);

                                    array.put(lastIndex, dataBean);
                                }
                            }

                            int addTotal = 0;
                            int arraySize = array.size();
                            for (int i = 0; i < arraySize; ++i) {
                                pageDataBeans.add(array.keyAt(i) + addTotal, array.valueAt(i));
                                ++addTotal;
                            }

                            proxyAdapter = new TransferProAdapter(TransferRecordActivity.this, pageDataBeans);
                            rl_transfer.setAdapter(proxyAdapter);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

}
