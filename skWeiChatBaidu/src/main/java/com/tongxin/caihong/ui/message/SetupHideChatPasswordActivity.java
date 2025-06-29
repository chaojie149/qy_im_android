package com.tongxin.caihong.ui.message;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.bean.EventHideChatPasswordCreated;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.search.SearchAllActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.view.PasswordInputView;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * create by zq
 * 设置隐藏会话密码
 */
public class SetupHideChatPasswordActivity extends BaseActivity {
    private TextView tvTip;
    private boolean needTwice = true;
    private String password;

    public static void start(Context context) {
        Intent intent = new Intent(context, SetupHideChatPasswordActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_hide_chat_password);

        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.gain_set_up_hide_conversation_password));
    }

    private void initView() {
        tvTip = findViewById(R.id.tvTip);
        final TextView tvFinish = findViewById(R.id.tvFinish);
        ButtonColorChange.colorChange(SetupHideChatPasswordActivity.this, tvFinish);
        tvFinish.setOnClickListener(v -> {
            DialogHelper.showDefaulteMessageProgressDialog(SetupHideChatPasswordActivity.this);
            HttpUtils.get().url(coreManager.getConfig().USER_SET_HIDE_CHAT_PASSWORD)
                    .params("password", password)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {
                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (Result.checkSuccess(SetupHideChatPasswordActivity.this, result)) {
                                PreferenceUtils.putString(mContext, Constants.HIDE_CHAT_PASSWORD + coreManager.getSelf().getUserId(), password);
                                EventBus.getDefault().post(new EventHideChatPasswordCreated());
                            }
                            SelectionFrame selectionFrame = new SelectionFrame(mContext);
                            selectionFrame.setSomething(null
                                    , getString(R.string.gain_hide_conversation_tip1)
                                    , getString(R.string.cancel), getString(R.string.search)
                                    , new SelectionFrame.OnSelectionFrameClickListener() {
                                        @Override
                                        public void cancelClick() {
                                            finish();
                                        }

                                        @Override
                                        public void confirmClick() {
                                            SearchAllActivity.start(mContext, "friend");
                                            finish();
                                        }
                                    });
                            selectionFrame.setOnDismissListener(dialogInterface -> finish());
                            selectionFrame.show();
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        });
        final PasswordInputView passwordInputView = findViewById(R.id.passwordInputView);
        passwordInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvFinish.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable psw) {
                if (psw.length() == AppConstant.PASS_WORD_LENGTH) {
                    if (needTwice) {
                        needTwice = false;
                        password = psw.toString();
                        passwordInputView.setText("");
                        tvTip.setText(R.string.gain_input_twice_set_up_hide_conversation_password);
                    } else if (psw.toString().equals(password)) {
                        // 二次确认成功，
                        tvFinish.setVisibility(View.VISIBLE);
                    } else {
                        // 二次确认失败，重新输入新密码，
                        passwordInputView.setText("");
                        needTwice = true;
                        tvTip.setText(R.string.tip_change_pay_password_input_incorrect);
                        tvFinish.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}
