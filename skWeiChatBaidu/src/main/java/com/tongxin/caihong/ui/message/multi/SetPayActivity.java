package com.tongxin.caihong.ui.message.multi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.assistant.KeyWord;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

public class SetPayActivity extends BaseActivity {
    private EditText etDays;
    private EditText etAmount;
    private String roomId;
    public static final int SETPAY = 111;

    public static void start(Activity activity, String roomId) {
        Intent intent = new Intent(activity, SetPayActivity.class);
        intent.putExtra("roomId", roomId);
        activity.startActivityForResult(intent, SetPayActivity.SETPAY);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pay);
        roomId = getIntent().getStringExtra("roomId");
        initActionBar();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText("设为付费群");
    }

    private void initView() {
        etDays = findViewById(R.id.et1);
        etAmount = findViewById(R.id.et2);
        ButtonColorChange.colorChange(mContext, findViewById(R.id.btn));
        findViewById(R.id.btn).setOnClickListener(view -> {
            String amount = etAmount.getText().toString();
            String days = etDays.getText().toString();
            if (!TextUtils.isEmpty(amount)&&!TextUtils.isEmpty(days)) {
                setPay(roomId,amount,days);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.notice_cannot_null));
            }
        });
    }

    private void setPay(String roomId, String amount,String days) {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("roomId", roomId);
        params.put("amount", amount);
        params.put("days", days);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).ROOM_SET_PAY)
                .params(params)
                .build()
                .execute(new BaseCallback<KeyWord>(KeyWord.class) {

                    @Override
                    public void onResponse(ObjectResult<KeyWord> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result != null && result.getResultCode() == 1) {
                            Intent intent = new Intent();
                            intent.putExtra("amount", amount);
                            intent.putExtra("days", days);
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            if (result != null && !TextUtils.isEmpty(result.getResultMsg())) {
                                ToastUtil.showToast(mContext, result.getResultMsg());
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
}
