package com.tongxin.caihong.ui.company;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.company.Companys;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.VerifyDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

/**
 * 搜索公司
 */
public class SearchCompanyActivity extends BaseActivity {
    private SearchCompanyAdapter adapter;
    private List<Companys> data = new ArrayList<>();
    private String companyId;
    private String cSelectCompanyId;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, SearchCompanyActivity.class);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, String companyId) {
        Intent intent = new Intent(ctx, SearchCompanyActivity.class);
        intent.putExtra("companyId", companyId);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_search);
        companyId = getIntent().getStringExtra("companyId");
        initActionBar();
        initView();
        setTouch(true);
        if (!TextUtils.isEmpty(companyId)) {
            // 公司id不为空，加载公司信息
            loadCompanyInfo();
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        EditText etSearch = findViewById(R.id.search_edit);
        RecyclerView rySearchCompany = findViewById(R.id.rySearchCompany);
        rySearchCompany.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new SearchCompanyAdapter();
        rySearchCompany.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString().trim())) {
                    data.clear();
                    findViewById(R.id.rySearchCompany).setVisibility(View.VISIBLE);
                    findViewById(R.id.rlEmptyDefault).setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                } else {
                    search(editable.toString().trim());
                }
            }
        });
    }

    private void loadCompanyInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("companyId", companyId);
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        HttpUtils.get().url(coreManager.getConfig().ORG_COMPANY_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Companys>(Companys.class) {

                    @Override
                    public void onResponse(ObjectResult<Companys> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (result.getData() != null) {
                                data.clear();
                                data.add(result.getData());
                                adapter.notifyDataSetChanged2();
                            } else {
                                ToastUtil.showToast(mContext, getString(R.string.tip_server_error));
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void search(String keyword) {
        Map<String, String> params = new HashMap<>();
        params.put("keyword", keyword);

        HttpUtils.get().url(coreManager.getConfig().ORG_COMPANY_SEARCH)
                .params(params)
                .build()
                .execute(new ListCallback<Companys>(Companys.class) {

                    @Override
                    public void onResponse(ArrayResult<Companys> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            data.clear();
                            data.addAll(result.getData());
                            adapter.notifyDataSetChanged2();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void applyJoin(String reason, String departmentId, boolean userJoinCompanyIsNeedManagerConfirm) {
        Map<String, String> params = new HashMap<>();
        params.put("companyId", cSelectCompanyId);
        if (!TextUtils.isEmpty(reason)) {
            params.put("reason", reason);
        }
        if (!userJoinCompanyIsNeedManagerConfirm) {
            params.put("departmentId", departmentId);
        }

        HttpUtils.get().url(coreManager.getConfig().ORG_COMPANY_APPLY_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            if (userJoinCompanyIsNeedManagerConfirm) {
                                ToastUtil.showToast(mContext, getString(R.string.tip_msg_send_success_wait_creator_verify_pass));
                            } else {
                                // 加入成功，通知同事页面刷新
                                ToastUtil.showToast(mContext, getString(R.string.join_success));
                                EventBus.getDefault().post(new MessageEvent("Update"));// 数据有更新
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ChangeEmployeeDepartment.REQUEST_SELECT_DEPARTMENT
                && resultCode == RESULT_OK && data != null) {
            String departmentId = data.getStringExtra("departmentId");
            applyJoin("", departmentId, false);
        }
    }

    class SearchCompanyAdapter extends RecyclerView.Adapter<SearchCompanyHolder> {

        public SearchCompanyAdapter() {

        }

        @NonNull
        @Override
        public SearchCompanyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SearchCompanyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_search, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SearchCompanyHolder holder, int position) {
            Companys companys = data.get(position);
            if (companys != null) {
                holder.tvName.setText(companys.getCompanyName());
                holder.tvPeopleNum.setText(getString(R.string.people_sums, String.valueOf(companys.getEmpNum())));
                holder.tvCreateTime.setText(getString(R.string.create_time) + "：" + TimeUtils.s_long_2_str(companys.getCreateTime() * 1000));

                holder.btnJoin.setVisibility(companys.getCurrentUserIsExist() == 1 ? View.GONE : View.VISIBLE);
                holder.btnJoined.setVisibility(companys.getCurrentUserIsExist() == 1 ? View.VISIBLE : View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public void notifyDataSetChanged2() {
            findViewById(R.id.rySearchCompany).setVisibility(data.size() > 0 ? View.VISIBLE : View.GONE);
            findViewById(R.id.rlEmptyDefault).setVisibility(data.size() > 0 ? View.GONE : View.VISIBLE);
            notifyDataSetChanged();
        }
    }

    class SearchCompanyHolder extends RecyclerView.ViewHolder {
        ImageView ivHead;
        TextView tvName;
        TextView tvPeopleNum;
        TextView tvCreateTime;
        Button btnJoin;
        Button btnJoined;

        SearchCompanyHolder(View itemView) {
            super(itemView);
            ivHead = itemView.findViewById(R.id.ivHead);
            ImageViewCompat.setImageTintList(ivHead, ColorStateList.valueOf(SkinUtils.getSkin(mContext).getAccentColor()));
            tvName = itemView.findViewById(R.id.tvName);
            tvPeopleNum = itemView.findViewById(R.id.tvPeopleNum);
            tvCreateTime = itemView.findViewById(R.id.tvCreateTime);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            ButtonColorChange.colorChange(mContext, btnJoin, R.drawable.bg_company_search_join);
            btnJoined = itemView.findViewById(R.id.btnJoined);
            btnJoin.setOnClickListener(view -> {
                cSelectCompanyId = data.get(getAdapterPosition()).getId();
                if (data.get(getAdapterPosition()).getUserJoinCompanyIsNeedManagerConfirm() == 1) {
                    VerifyDialog verifyDialog = new VerifyDialog(mContext);
                    verifyDialog.setVerifyClickListener(getString(R.string.apply_join_company)
                            , getString(R.string.tip_apply_join_company_verify)
                            , "", "", 15, new VerifyDialog.VerifyClickListener() {
                                @Override
                                public void cancel() {

                                }

                                @Override
                                public void send(String str) {
                                    applyJoin(str, "", true);
                                }
                            });
                    verifyDialog.show();
                } else {
                    ChangeEmployeeDepartment.start(SearchCompanyActivity.this, cSelectCompanyId);
                }
            });
        }
    }
}
