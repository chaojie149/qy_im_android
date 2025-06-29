package com.tongxin.caihong.ui.other;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.SelectionFrame;

import java.util.Locale;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * 请求同意隐私政策的页面，
 */
public class PrivacyAgreeActivity extends BaseActivity {
    private WebView mWebView;
    private TextView mTitleTv;
    private ImageView mTitleLeftIv;
    private boolean viewOnly;
    private boolean isPrivacy;

    public PrivacyAgreeActivity() {
        noConfigRequired();
        noLoginRequired();
        disableSwipeBack = true;
    }

    public static void start(Context ctx, boolean isPrivacy, boolean viewOnly) {
        Intent intent = new Intent(ctx, PrivacyAgreeActivity.class);
        intent.putExtra("viewOnly", viewOnly);
        intent.putExtra("isPrivacy", isPrivacy);
        ctx.startActivity(intent);
    }

    public static void startForUserDestroy(Context ctx) {
        Intent intent = new Intent(ctx, PrivacyAgreeActivity.class);
        intent.putExtra("userDestroy", true);
        intent.putExtra("viewOnly", true);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_agree);
        isPrivacy = getIntent().getBooleanExtra("isPrivacy", false);
        viewOnly = getIntent().getBooleanExtra("viewOnly", false);
        initActionBar();
        EventBusHelper.register(this);
        findViewById(R.id.rl_agree).setVisibility(viewOnly ? View.GONE : View.VISIBLE);
        mWebView = (WebView) findViewById(R.id.mWebView);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setAllowFileAccess(false);
        String prefix = coreManager.getConfig().privacyPolicyPrefix;
        if (TextUtils.isEmpty(prefix)) {
            PreferenceUtils.putBoolean(mContext, Constants.PRIVACY_AGREE_STATUS, true);
            finish();
            return;
        }
        String url;
        if (isPrivacy) {
            url = coreManager.getConfig().privacyPolicyPrefix + "privacy.html";
        } else {
            String language = Locale.getDefault().getLanguage();
            if (language.startsWith("zh")) {
                language = "zh";
            } else {
                language = "en";
            }
            url = prefix + language + ".html";
        }
        mWebView.loadUrl(url);

        findViewById(R.id.btnAgree).setOnClickListener((v) -> {
            PreferenceUtils.putBoolean(mContext, Constants.PRIVACY_AGREE_STATUS, true);
            finish();
        });
        ButtonColorChange.textChange(mContext, findViewById(R.id.btnAgree));

        findViewById(R.id.btnDisagree).setOnClickListener((v) -> {
            disagree();
        });

    }

    @Override
    public void onBackPressed() {
        if (!viewOnly) {
            disagree();
        } else {
            super.onBackPressed();
        }
    }

    private void disagree() {
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(null, getString(R.string.tip_privacy_can_not_disagree),
                getString(R.string.btn_privacy_disagree), getString(R.string.btn_privacy_re_reading),
                new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {
                        // 关闭Splash页面和当前页面，
                        EventBus.getDefault().post(new MessageLogin());
                    }

                    @Override
                    public void confirmClick() {
                    }
                });
        selectionFrame.show();
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewOnly)
                    finish();
                else disagree();
            }
        });
        mTitleTv = findViewById(R.id.tv_title_center);
//        mTitleTv.setText(R.string.title_privacy_policy);
        mTitleTv.setText(!isPrivacy ? R.string.yonghuxieyi : R.string.title_privacy_policy);
        mTitleLeftIv = findViewById(R.id.iv_title_left);
        mTitleLeftIv.setImageResource(R.drawable.icon_close);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }
}
