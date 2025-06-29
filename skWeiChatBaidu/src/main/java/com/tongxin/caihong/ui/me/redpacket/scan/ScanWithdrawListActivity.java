package com.tongxin.caihong.ui.me.redpacket.scan;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.AccountItem;
import com.tongxin.caihong.bean.redpacket.ScanWithDrawSelectType;
import com.tongxin.caihong.ui.base.BaseListActivity;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

/**
 * 选择提现方式
 */
public class ScanWithdrawListActivity extends BaseListActivity {
    List<ScanWithDrawSelectType> scanWithDrawSelectTypes = new ArrayList<>();
    private boolean isEdit;
    private boolean isResumed;

    @Override
    protected void onResume() {
        super.onResume();
        if (isResumed) {
            initDatas(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = true;
    }

    @Override
    public void initView() {
        // 添加两个空数据，表示去添加item
//        scanWithDrawSelectTypes.add(new ScanWithDrawSelectType());
//        scanWithDrawSelectTypes.add(new ScanWithDrawSelectType());
        initActionBar();
    }

    TextView mTvRightTitle;

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.select_withdraw_type));
        mTvRightTitle = findViewById(R.id.tv_title_right);
        mTvRightTitle.setText(getString(R.string.edit));
        mTvRightTitle.setVisibility(View.GONE);
    }

    List<AccountItem> mAccountItems;

    private AccountItem findNameByType(int type) {
        if (mAccountItems != null) {
            for (int i = 0; i < mAccountItems.size(); i++) {
                if (mAccountItems.get(i).getType() == type) {
                    return mAccountItems.get(i);
                }
            }
        }
        return null;
    }

    @Override
    public void initDatas(int pager) {
        if (pager != 0) return;
        HashMap<String, String> params1 = new HashMap<>();
        HttpUtils.get().url(coreManager.getConfig().MANUAL_PAY_GET_WITHDRAW_ACCOUNT)
                .params(params1)
                .build()
                .execute(new ListCallback<AccountItem>(AccountItem.class) {
                    @Override
                    public void onResponse(ArrayResult<AccountItem> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            List<AccountItem> list = result.getData();
                            if (!list.isEmpty()) {
                                mAccountItems = list;
                            }
                        }

                        getData(pager);

                    }

                    @Override
                    public void onError(Call call, Exception e) {
//                        getData(pager);
//                        update(scanWithDrawSelectTypes);
                    }
                });
    }

    public void getData(int pager) {
        HashMap<String, String> params = new HashMap<>();
        params.put("pageIndex", String.valueOf(pager));
        params.put("pageSize", String.valueOf(PAGE_SIZE));
        HttpUtils.get().url(coreManager.getConfig().MANUAL_PAY_GET_WITHDRAW_ACCOUNT_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<ScanWithDrawSelectType>(ScanWithDrawSelectType.class) {
                    @Override
                    public void onResponse(ArrayResult<ScanWithDrawSelectType> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            scanWithDrawSelectTypes.clear();
                            if (mAccountItems != null) {
                                for (int i = 0; i < mAccountItems.size(); i++) {
                                    boolean isAdd = false;
                                    if(result.getData()!=null) {
                                        for (int j = 0; j < result.getData().size(); j++) {
                                            if (result.getData().get(j).getType() == mAccountItems.get(i).getType()) {
                                                isAdd = true;
                                                scanWithDrawSelectTypes.add(result.getData().get(j));
                                                break;
                                            }
                                        }
                                    }
                                    if (!isAdd) {
                                        ScanWithDrawSelectType scanWithDrawSelectType = new ScanWithDrawSelectType();
                                        scanWithDrawSelectType.setAccountPos(i);
                                        scanWithDrawSelectTypes.add(scanWithDrawSelectType);
                                    }
                                }
                            }

//                            if (mAccountItem != null && mAccountItem.getType() == 1) {
//                                update(scanWithDrawSelectTypes);
//                            } else {
//                                scanWithDrawSelectTypes.clear();
                            update(scanWithDrawSelectTypes);
//                            }
//                            if (result.getData().size() != PAGE_SIZE) {
//                                more = false;
//                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
//                        update(scanWithDrawSelectTypes);
                    }
                });
    }

    @Override
    public RecyclerView.ViewHolder initHolder(ViewGroup parent) {
        View v = mInflater.inflate(R.layout.item_scan_withdraw_add, parent, false);
        return new ScanWithDrawSelectTypeViewHolder(v);
    }

    @Override
    public void fillData(RecyclerView.ViewHolder holder, int position) {
        ScanWithDrawSelectTypeViewHolder drawSelectTypeViewHolder = (ScanWithDrawSelectTypeViewHolder) holder;
        ScanWithDrawSelectType drawSelectType = scanWithDrawSelectTypes.get(position);
        if (TextUtils.isEmpty(drawSelectType.getId())) {
            drawSelectTypeViewHolder.addTv.setVisibility(View.VISIBLE);
            drawSelectTypeViewHolder.typeIv.setVisibility(View.GONE);
            drawSelectTypeViewHolder.typeTv.setVisibility(View.GONE);
            drawSelectTypeViewHolder.nextIv.setVisibility(View.GONE);
            drawSelectTypeViewHolder.type_edit.setVisibility(View.GONE);
            drawSelectTypeViewHolder.addTv.setText((getString(R.string.add_to) + (mAccountItems.get(drawSelectType.getAccountPos()) == null ? ""
                    : (mAccountItems.get(drawSelectType.getAccountPos()).getName()) + getString(R.string.select_withdraw_account))));
            drawSelectTypeViewHolder.item.setOnClickListener(v -> ScanWithdrawAddActivity.start(mContext, mAccountItems.get(drawSelectType.getAccountPos()).getType(), drawSelectTypeViewHolder.addTv.getText().toString()));
        } else {
            drawSelectTypeViewHolder.addTv.setVisibility(View.GONE);
            drawSelectTypeViewHolder.typeIv.setVisibility(View.VISIBLE);
            drawSelectTypeViewHolder.typeTv.setVisibility(View.VISIBLE);
            drawSelectTypeViewHolder.nextIv.setVisibility(isEdit ? View.VISIBLE : View.GONE);
//            if (drawSelectType.getType() == 1) {//支付宝
//                drawSelectTypeViewHolder.typeIv.setImageResource(R.mipmap.ic_alipay_small);
//                drawSelectTypeViewHolder.typeTv.setText(drawSelectType.getAliPayAccount());
//            } else {// 银行卡
//                drawSelectTypeViewHolder.typeIv.setImageResource(R.mipmap.ic_band_small);
            AccountItem accountItem = findNameByType(drawSelectType.getType());
            if (accountItem != null) {
                drawSelectTypeViewHolder.typeTv.setText(accountItem.getName() + ":" + drawSelectType.getAliPayAccount() + "");
            }
//            }
//            mTvRightTitle.setVisibility(View.GONE);
            drawSelectTypeViewHolder.type_edit.setOnClickListener(v -> {
                ScanWithdrawAddActivity.start(mContext, drawSelectType.getType(), (getString(R.string.transfer_modify) + (accountItem == null ? "" : accountItem.getName()) + getString(R.string.select_withdraw_account)), drawSelectType.getAliPayAccount(), drawSelectType.getAliPayName(), drawSelectType.getId());
            });
            drawSelectTypeViewHolder.item.setOnClickListener(v -> {
                if (isEdit) {
                    ScanWithdrawUpdateActivity.start(mContext, JSON.toJSONString(drawSelectType));
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("drawSelectType", JSON.toJSONString(drawSelectType));
                    intent.putExtra("name", (accountItem == null ? "" : accountItem.getName()));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }
    }

    public class ScanWithDrawSelectTypeViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout item;
        public TextView addTv;
        public ImageView typeIv;
        public TextView typeTv, type_edit;
        public ImageView nextIv;

        public ScanWithDrawSelectTypeViewHolder(View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.item_add);
            addTv = itemView.findViewById(R.id.add_tv);
            typeIv = itemView.findViewById(R.id.type_iv);
            typeTv = itemView.findViewById(R.id.type_tv);
            nextIv = itemView.findViewById(R.id.next_iv);
            type_edit = itemView.findViewById(R.id.type_edit);
        }
    }
}
