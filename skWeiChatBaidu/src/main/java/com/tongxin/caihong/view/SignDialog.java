package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.SignUpBean;
import com.tongxin.caihong.bean.event.EventPostSuccess;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.sign.WithdrawalActivity;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class SignDialog extends Dialog {
    private RelativeLayout rl_sign;
    private RelativeLayout rl_sign_ed;
    private TextView tv_money;
    private Context mContext;
    private CoreManager mCoreManager;
    private SignCallBack signCallBack;
    private String m;

    public SignDialog(@NonNull Context context, CoreManager coreManager, String money, SignCallBack callBack) {
        super(context, R.style.MyDialog);
        this.mContext = context;
        this.mCoreManager = coreManager;
        this.signCallBack = callBack;
        this.m = money;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_dialog);
        initView();

        Window window = getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        // lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        // lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.setGravity(Gravity.CENTER);
    }

    private void initView() {
        rl_sign = findViewById(R.id.rl_sign);
        rl_sign_ed = findViewById(R.id.rl_sign_ed);
        tv_money = findViewById(R.id.tv_money);

        findViewById(R.id.iv_close1).setOnClickListener(v -> dismiss());
        findViewById(R.id.iv_close2).setOnClickListener(v -> dismiss());
        findViewById(R.id.bt_sign).setOnClickListener(v -> signUp());
    }

    private void signUp() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<String, String>();
        params.put("userId", mCoreManager.getSelf().getUserId());

        HttpUtils.get().url(mCoreManager.getConfig().RED_SIGN_UP)
                .params(params)
                .build()
                .execute(new BaseCallback<SignUpBean>(SignUpBean.class) {
                    @Override
                    public void onResponse(ObjectResult<SignUpBean> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 103003) {
                            signCallBack.signed(false);
                            ToastUtil.showToast(mContext, result.getResultMsg());
                            return;
                        }
                        if (Result.checkSuccess(mContext, result)) {
                            SignUpBean signUpBean = result.getData();
                            signUpBean.setIsSign(true);
                            EventBus.getDefault().post(new EventPostSuccess());
                            rl_sign.setVisibility(View.GONE);
                            findViewById(R.id.iv_close1).setVisibility(View.GONE);
                            signCallBack.signed(true);
                            rl_sign_ed.setVisibility(View.VISIBLE);
                            findViewById(R.id.iv_close2).setVisibility(View.VISIBLE);
                            tv_money.setText(MyApplication.getContext().getResources().getString(R.string.money_page));
                            ((TextView) findViewById(R.id.tv_money_sign)).setText(String.valueOf(signUpBean.getAwardMoney()));
                            findViewById(R.id.bt_withdrawal).setOnClickListener(v ->
                                    {
                                        WithdrawalActivity.start(mContext, String.valueOf(Double.parseDouble(m) + signUpBean.getAwardMoney()));
                                        dismiss();
                                    }
                            );
                            return;
                        }
                        signCallBack.signed(false);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, e.toString());
                        signCallBack.signed(false);
                    }
                });
    }

    public interface SignCallBack {
        void signed(boolean b);
    }
}