package com.tongxin.caihong.ui.sign;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tongxin.caihong.bean.SignBean;
import com.tongxin.caihong.bean.SignUpBean;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.EventPostSuccess;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.view.SwitchButton;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.SignDialog;
import com.tongxin.caihong.view.SkinImageView;
import com.tongxin.caihong.view.SkinTextView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

public class SignUpActivity extends BaseActivity implements View.OnClickListener {
    private SkinImageView iv_title_left;
    private SkinTextView tv_title_center;
    private TextView tv_sign_sum;
    private LinearLayout ll_imageview;
    private LinearLayout ll_day;
    private Button bt_sign;
    private SignBean signBean;
    // 小圆圈的位置
    private Integer[] integers = {1, 3, 5, 7, 9, 11, 13, 15};
    private String moneySum;

    public static void start(Context context, SignBean signBean) {
        Intent intent = new Intent(context, SignUpActivity.class);
        intent.putExtra("sign", signBean);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        signBean = (SignBean) getIntent().getSerializableExtra("sign");
        initActionBar();
        initView();
        loadMoney();
        EventBusHelper.register(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        iv_title_left = (SkinImageView) findViewById(R.id.iv_title_left);
        iv_title_left.setOnClickListener(v -> finish());
        tv_title_center = (SkinTextView) findViewById(R.id.tv_title_center);
        tv_title_center.setText(getResources().getString(R.string.sign_title));
    }

    private void initView() {
        User user = coreManager.getSelf();
        HeadView avatar_img = findViewById(R.id.avatar_img);
        AvatarHelper.getInstance().displayAvatar(user.getNickName(), user.getUserId(), avatar_img.getHeadImage(), true);
        findViewById(R.id.bt_withdrawal).setOnClickListener(this);
        tv_sign_sum = findViewById(R.id.tv_sign_sum);
        SwitchButton sb_sign = findViewById(R.id.sb_sign);
        sb_sign.setChecked(!PreferenceUtils.getBoolean(SignUpActivity.this,
                AppConstant.SIGN_IN_NO_REMIND + coreManager.getSelf().getUserId(), false));
        sb_sign.setOnCheckedChangeListener((view, isChecked) -> PreferenceUtils.putBoolean(SignUpActivity.this,
                AppConstant.SIGN_IN_NO_REMIND + coreManager.getSelf().getUserId(), !isChecked));
        ll_imageview = findViewById(R.id.ll_imageview);
        ll_day = findViewById(R.id.ll_day);
        bt_sign = (Button) findViewById(R.id.bt_sign);
        bt_sign.setAlpha(signBean.getIsSign() == 0 ? 1f : 0.6f);
        bt_sign.setEnabled(signBean.getIsSign() == 0);
        bt_sign.setText(signBean.getIsSign() == 0 ? getResources().getString(R.string.sign) :
                getResources().getString(R.string.signed));
        bt_sign.setOnClickListener(this);
        loadView();
    }

    private void loadView() {
        //判断哪些位置是有红包的
        List<Integer> numList = new ArrayList();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < signBean.getSignPolicyAward().size(); i++) {
            if (signBean.getSignPolicyAward().get(i) > 0) {
                numList.add(i);
                builder.append(i + 1);
                builder.append(getString(R.string.tip_sign_day));
                if (i != 6) {
                    builder.append(",");
                }
            }
        }
        if (numList.size() == 0) {
            // 每天签到都没有红包领取
            findViewById(R.id.tv_des).setVisibility(View.GONE);
        } else if (numList.size() == 7) {
            // 每天签到均可领取一个红包
            ((TextView) findViewById(R.id.tv_des)).setText(getString(R.string.tip_sign_every_get));
        } else {
            // 指定签到天数均可领取一个红包
            ((TextView) findViewById(R.id.tv_des)).setText(getString(R.string.sign_title_des, builder.toString()));
        }
        if (signBean.getIsSign() == 1) {
            ((TextView) ll_day.getChildAt(signBean.getSignCount() - 1)).setTextColor(0xfff2ab69);
        } else {
            ((TextView) ll_day.getChildAt(signBean.getSignCount())).setTextColor(0xfff2ab69);
        }
        tv_sign_sum.setText(getString(R.string.sign_sum, String.valueOf(signBean.getSignCount())));
        if (numList.size() > 0) {
            for (int i = 0; i < numList.size(); i++) {
                if (ll_imageview.getChildAt(integers[numList.get(i)]) instanceof ImageView) {
                    ((ImageView) (ll_imageview.getChildAt(integers[numList.get(i)])))
                            .setImageDrawable(getResources().getDrawable(R.mipmap.sign_red));
                }
            }
        }
        if (signBean.getSignCount() > 0) {
            for (int i = 0; i < signBean.getSignCount() * 2; i++) {
                if ((i + 1) % 2 == 0) {
                    if (ll_imageview.getChildAt(i) instanceof ImageView) {
                        ((ImageView) (ll_imageview.getChildAt(i))).setImageDrawable(getResources().getDrawable(R.mipmap.sign_up_ed));
                    }
                } else {
                    ll_imageview.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.gray));
                }
            }
        }

        //根据服务端返回的数据  显示相应的签到过的UI
        if (signBean.getSignCount() > 0) {
            //先初始化  签到的都给染成灰色背景
            for (int i = 0; i < signBean.getSignCount(); i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.layout_sign_money_item, null);
                // 服务端返回的数据 [10,0,10,0,10,0,10]
                if (signBean.getSignPolicyAward().get(i) != 0) {
                    ((TextView) view.findViewById(R.id.tv_money)).setText(String.valueOf(signBean.getSignPolicyAward().get(i)));
                    ll_imageview.removeViewAt(integers[i]);
                    ll_imageview.addView(view, integers[i]);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_sign:
                SignDialog SignDialog = new SignDialog(SignUpActivity.this, coreManager, moneySum, new SignDialog.SignCallBack() {
                    @Override
                    public void signed(boolean b) {
                        if (b) {
                            loadSignUP();
                        }
                    }
                });
                if (signBean.getSignPolicyAward().get(signBean.getSignCount()) > 0) {
                    SignDialog.show();
                } else {
                    signUp();
                }

                break;
            case R.id.bt_withdrawal:
                WithdrawalActivity.start(SignUpActivity.this, moneySum);
                break;
        }
    }

    private void loadMoney() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userId", coreManager.getSelf().getUserId());
        HttpUtils.get().url(coreManager.getConfig().BILL_GET_MONEY)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            moneySum = result.getData();
                            ((TextView) findViewById(R.id.tv_money)).setText(result.getData());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, e.toString());
                    }
                });
    }

    private void signUp() {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userId", coreManager.getSelf().getUserId());
        HttpUtils.get().url(coreManager.getConfig().RED_SIGN_UP)
                .params(params)
                .build()
                .execute(new BaseCallback<SignUpBean>(SignUpBean.class) {
                    @Override
                    public void onResponse(ObjectResult<SignUpBean> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 103003) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                            return;
                        }
                        if (Result.checkSuccess(mContext, result)) {
                            SignUpBean signUpBean = result.getData();
                            signUpBean.setIsSign(true);
                            EventBus.getDefault().post(new EventPostSuccess());
                            loadSignUP();
                            return;
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, e.toString());
                    }
                });
    }

    private void loadSignUP() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userId", coreManager.getSelf().getUserId());
        HttpUtils.get().url(coreManager.getConfig().RED_SIGN_QUERY)
                .params(params)
                .build()
                .execute(new BaseCallback<SignBean>(SignBean.class) {
                    @Override
                    public void onResponse(ObjectResult<SignBean> result) {
                        if (Result.checkSuccess(SignUpActivity.this, result)) {
                            //签到数据返回
                            signBean = result.getData();
                            initView();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventPostSuccess message) {
        loadMoney();
    }
}