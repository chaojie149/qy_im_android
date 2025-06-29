package com.tongxin.caihong.ui.me;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.helper.ShareSdkHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.other.PrivacyAgreeActivity;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.view.ShareDialog;

public class AboutActivity extends BaseActivity {
    private ShareDialog shareDialog;
    private ShareDialog.OnShareDialogClickListener onShareDialogClickListener = new ShareDialog.OnShareDialogClickListener() {
        @Override
        public void tv1Click() {
            ShareSdkHelper.shareWechat(mContext, getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    AboutActivity.this.coreManager.getConfig().website);
        }

        @Override
        public void tv2Click() {
            ShareSdkHelper.shareWechatMoments(mContext, getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    coreManager.getConfig().website);
        }
    };

    public void PrivacyAgree(View view) {
        if (UiUtils.isNormalClick(view) && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix)) {
            PrivacyAgreeActivity.start(this, false, true);
        }
    }

    public void Privacy(View view) {
        if (UiUtils.isNormalClick(view) && !TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix)) {
            PrivacyAgreeActivity.start(this, true, true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.about_us));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.mipmap.share_icon);
        ivRight.setOnClickListener(view -> {
            shareDialog = new ShareDialog(AboutActivity.this, onShareDialogClickListener);
            shareDialog.show();
        });

        TextView versionTv = (TextView) findViewById(R.id.version_tv);
        versionTv.setText(getString(R.string.app_name) + DeviceInfoUtil.getVersionName(mContext));

        TextView tvCompany = findViewById(R.id.company_tv);
        TextView tvCopyright = findViewById(R.id.copy_right_tv);

//        tvCompany.setText(BuildConfig.COMPANY_NAME);
        tvCopyright.setVisibility(View.GONE);

        if (TextUtils.isEmpty(BuildConfig.COMPANY_NAME)) {
            tvCompany.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(BuildConfig.WECHAT_APP_ID)) {
            ivRight.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(coreManager.getConfig().privacyPolicyPrefix)) {
            findViewById(R.id.llPrivacy).setVisibility(View.GONE);
        }
    }
}
