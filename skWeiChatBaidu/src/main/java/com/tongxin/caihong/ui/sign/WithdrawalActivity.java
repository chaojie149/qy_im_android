package com.tongxin.caihong.ui.sign;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.event.EventPostSuccess;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.NumberUtil;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.view.SignExplainDialog;
import com.tongxin.caihong.view.SkinImageView;
import com.tongxin.caihong.view.SkinTextView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class WithdrawalActivity extends BaseActivity implements View.OnClickListener {
    private SkinImageView iv_title_left;
    private SkinTextView tv_title_center;
    private TextView tv_money;
    private RecyclerView rc_item;
    private WithdrawalAdapter withdrawalAdapter;
    private Button bt_commit;
    private List<String> virtualDeposit;
    private Map<String, String> stringMap = new HashMap<>();
    private String commitMoney = "0.00";
    private String sumMoney;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public static void start(Context context, String money) {
        Intent intent = new Intent(context, WithdrawalActivity.class);
        intent.putExtra("money", money);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdrawal);
        sumMoney = getIntent().getStringExtra("money");
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        iv_title_left = (SkinImageView) findViewById(R.id.iv_title_left);
        iv_title_left.setOnClickListener(v -> finish());
        tv_title_center = (SkinTextView) findViewById(R.id.tv_title_center);
        tv_title_center.setText(getResources().getString(R.string.yeepay_withdraw));
    }

    private void initView() {
        virtualDeposit = coreManager.getConfig().virtualDeposit;
        findViewById(R.id.tv_explain).setOnClickListener(this);
        tv_money = (TextView) findViewById(R.id.tv_money);
        BigDecimal bg = BigDecimal.valueOf(Double.parseDouble(sumMoney));
        tv_money.setText(String.valueOf(bg.setScale(2, BigDecimal.ROUND_HALF_DOWN).doubleValue()));
        rc_item = (RecyclerView) findViewById(R.id.rc_item);
        rc_item.setLayoutManager(new GridLayoutManager(this, 3));
        withdrawalAdapter = new WithdrawalAdapter();
        rc_item.setAdapter(withdrawalAdapter);
        rc_item.addItemDecoration(new SpaceItemDecoration(ScreenUtil.dip2px(this, 10)));
        bt_commit = (Button) findViewById(R.id.bt_commit);
        bt_commit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_commit:
                if (!TextUtils.equals(commitMoney, "0.00")) {
                    commit();
                }
                break;
            case R.id.tv_explain:
                SignExplainDialog signExplainDialog = new SignExplainDialog(WithdrawalActivity.this);
                signExplainDialog.show();
                break;
        }
    }

    private void commit() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userId", coreManager.getSelf().getUserId());
        params.put("amount", String.valueOf(commitMoney));
        HttpUtils.get().url(coreManager.getConfig().DEPOSIT_MONEY)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(WithdrawalActivity.this, result)) {
                            EventBus.getDefault().post(new EventPostSuccess());
                            CoreManager.updateMyBalance();
                            ToastUtil.showToast(WithdrawalActivity.this, mContext.getResources().getString(R.string.success));
                            stringMap.clear();
                            BigDecimal bg = BigDecimal.valueOf(Double.parseDouble(tv_money.getText().toString()) - Double.parseDouble(commitMoney));
                            tv_money.setText(String.valueOf(bg.setScale(2, BigDecimal.ROUND_HALF_DOWN).doubleValue()));
                            commitMoney = "0.00";
                            withdrawalAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(WithdrawalActivity.this, e.toString());
                    }
                });
    }

    class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalViewHolder> {

        @NonNull
        @Override
        public WithdrawalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(WithdrawalActivity.this).inflate(R.layout.withdrawal_item, parent, false);
            return new WithdrawalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WithdrawalViewHolder holder, int position) {
            String money = tv_money.getText().toString();
            holder.tv_item.setText(NumberUtil.getTwoDecimalPlaces(Double.parseDouble(virtualDeposit.get(position))) + "元");
            holder.rl_content.setBackground(stringMap.containsKey(virtualDeposit.get(position)) ?
                    MyApplication.getContext().getResources().getDrawable(R.drawable.withdrawal_item_bg) :
                    MyApplication.getContext().getResources().getDrawable(R.drawable.withdrawal_item__nomal_bg));
            holder.tv_item.setTextColor(stringMap.containsKey(virtualDeposit.get(position)) ? getResources().getColor(R.color.check_sign) : getResources().getColor(R.color.checked_sign));
            if (Double.parseDouble(money) - Double.parseDouble(virtualDeposit.get(position)) < 0) {
                holder.tv_item.setTextColor(getResources().getColor(R.color.checked_sign));
                holder.rl_content.setEnabled(false);
            } else {
                holder.tv_item.setTextColor(getResources().getColor(R.color.text_check_sign));
                holder.rl_content.setEnabled(true);
            }

            holder.rl_content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    commitMoney = virtualDeposit.get(position);
                    if (stringMap.containsKey(virtualDeposit.get(position))) {
                        //如果有 就删掉自己
                        stringMap.remove(virtualDeposit.get(position));
                        commitMoney = "0.00";
                    } else {
                        //没有就添加
                        stringMap.clear(); //添加之前先清除所有
                        stringMap.put(virtualDeposit.get(position), virtualDeposit.get(position));
                    }
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return virtualDeposit.size();
        }
    }

    class WithdrawalViewHolder extends RecyclerView.ViewHolder {
        TextView tv_item;
        RelativeLayout rl_content;

        public WithdrawalViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_item = itemView.findViewById(R.id.tv_item);
            rl_content = itemView.findViewById(R.id.rl_content);
        }
    }

    public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            //不是第一个的格子都设一个左边和底部的间距
            outRect.bottom = space;
            //由于每行都只有3个，所以第一个都是3的倍数，把左边距设为0
            if (parent.getChildLayoutPosition(view) % 3 == 0) {
            }
        }
    }
}