package com.tongxin.caihong.ui.company;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.company.Companys;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 公司邀请
 */
public class InviteSureJoinCompanyActivity extends BaseActivity {
    private ChatMessage chatMessage;
    private TextView tvName;
    private TextView tvNumber;

    public static void start(Context context, String message) {
        Intent intent = new Intent(context, InviteSureJoinCompanyActivity.class);
        intent.putExtra("message", message);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_invite_sure_join);
        String message = getIntent().getStringExtra("message");
        chatMessage = JSON.parseObject(message, ChatMessage.class);
        initActionBar();
        initView();
        loadCompany();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.company_invite);
    }

    private void initView() {
        ImageViewCompat.setImageTintList(findViewById(R.id.ivHead), ColorStateList.valueOf(SkinUtils.getSkin(mContext).getAccentColor()));
        tvName = findViewById(R.id.tvName);
        tvNumber = findViewById(R.id.tvNumber);
        TextView tvDesc = findViewById(R.id.tvDesc);
        tvDesc.setText(getString(R.string.invite_join_company, chatMessage.getFromUserName()));
        ButtonColorChange.colorChange(mContext, findViewById(R.id.btnJoin));

        findViewById(R.id.btnJoin).setOnClickListener(view -> joinCompany(chatMessage.getObjectId(), chatMessage.getFilePath()));
    }

    private void loadCompany() {
        Map<String, String> params = new HashMap<>();
        params.put("companyId", chatMessage.getObjectId());
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
                                tvName.setText(result.getData().getCompanyName());
                                tvNumber.setText(getString(R.string.total_count_place_holder, result.getData().getEmpNum()));
                                findViewById(R.id.tvJoined).setVisibility(result.getData().getCurrentUserIsExist() == 1 ? View.VISIBLE : View.GONE);
                                findViewById(R.id.btnJoin).setVisibility(result.getData().getCurrentUserIsExist() == 1 ? View.GONE : View.VISIBLE);
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

    private void joinCompany(String companyId, String departmentId) {
        Map<String, String> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("departmentId", departmentId);
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).ORG_COMPANY_USER_CONFIRM_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(final ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(MyApplication.getContext(), result)) {
                            ToastUtil.showToast(mContext, getString(R.string.join_success));
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

}
