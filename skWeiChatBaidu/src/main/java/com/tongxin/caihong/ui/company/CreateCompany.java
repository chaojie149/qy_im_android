package com.tongxin.caihong.ui.company;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.bean.company.Companys;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

public class CreateCompany extends BaseActivity implements View.OnClickListener {
    private EditText mCompanyEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_company);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.new_company);
    }

    private void initView() {
        mCompanyEdit = (EditText) findViewById(R.id.company_edit);
        findViewById(R.id.create_company_btn).setOnClickListener(this);
        ButtonColorChange.colorChange(this, findViewById(R.id.create_company_btn));
    }

    @Override
    public void onClick(View view) {
        if (!UiUtils.isNormalClick(view)) {
            return;
        }
        switch (view.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.create_company_btn:
                String mCompanyName = mCompanyEdit.getText().toString().trim();
                if (TextUtils.isEmpty(mCompanyName)) {
                    // 公司名不能为空
                    Toast.makeText(this, R.string.name_connot_null, Toast.LENGTH_SHORT).show();
                } else {
                    createCompany(mCompanyName);
                }
                break;
        }
    }

    private void createCompany(String mCompanyName) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyName", mCompanyName);
        params.put("createUserId", coreManager.getSelf().getUserId());

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().CREATE_COMPANY)
                .params(params)
                .build()
                .execute(new BaseCallback<Companys>(Companys.class) {
                    @Override
                    public void onResponse(ObjectResult<Companys> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(CreateCompany.this, R.string.create_company_succ, Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new MessageEvent("Update"));// 数据有更新
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(CreateCompany.this);
                    }
                });
    }
}
