package com.tongxin.caihong.pay.new_ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.pay.TransferMoneyActivity;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.view.ClearEditText;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

public class MobileRemitNumberActivity extends BaseActivity {

    private ClearEditText cet_phone;
    private Button bt_sure;
    private int screenHeight = 0;
    private int keyHeight = 0;
    private LinearLayout.LayoutParams lp;
    private String number;
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            bt_sure.setAlpha(TextUtils.isEmpty(s.toString()) ? 0.6f : 1f);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, MobileRemitNumberActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_remit_number);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        // 获取屏幕高度
        screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        // 阀值设置为屏幕高度的1/3(软件盘高度)
        keyHeight = screenHeight / 3;
        initView();
    }

    private void initView() {
        cet_phone = findViewById(R.id.cet_phone);
        bt_sure = findViewById(R.id.bt_sure);
        bt_sure.setAlpha(0.6f);
        lp = (LinearLayout.LayoutParams) bt_sure.getLayoutParams();
        lp.setMargins(ScreenUtil.dip2px(this, 15), 0, ScreenUtil.dip2px(this, 15), keyHeight);
        bt_sure.setLayoutParams(lp);
        showSoftInputFromWindow(cet_phone);
        cet_phone.addTextChangedListener(textWatcher);
        ButtonColorChange.colorChange(this, bt_sure);
        bt_sure.setOnClickListener(v -> {
            number = cet_phone.getText().toString().trim();
            if (!TextUtils.isEmpty(number)) {
                requestData(number);
            }
        });
    }

    public void showSoftInputFromWindow(EditText editText) {
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        InputMethodManager inputManager =
                (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(editText, 0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void requestData(String mKeyWord) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(0));
        params.put("pageSize", "1");
        params.put("nickname", mKeyWord);
        params.put("active", String.valueOf(0));

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().USER_NEAR)
                .params(params)
                .build()
                .execute(new ListCallback<User>(User.class) {
                    @Override
                    public void onResponse(ArrayResult<User> result) {
                        DialogHelper.dismissProgressDialog();
                        List<User> datas = result.getData();
                        if (datas != null && datas.size() > 0) {
                            if (datas.get(0).getUserId().equals(coreManager.getSelf().getUserId())) {
                                Toast.makeText(MobileRemitNumberActivity.this, getResources().getString(R.string.not_self), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            TransferMoneyActivity.start(MobileRemitNumberActivity.this, datas.get(0).getUserId(), datas.get(0).getNickName(), number);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(MobileRemitNumberActivity.this, R.string.check_network, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
