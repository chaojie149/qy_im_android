package com.tongxin.caihong.ui.message.single;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.UploadingHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by Administrator on 2017/12/5 0005.
 * 聊天背景
 */

public class SetChatBackActivity extends BaseActivity implements View.OnClickListener {
    private ImageView mChaIv;
    private String mFriendId;
    private String mLoginUserId;
    private String mChatBackgroundPath;
    private String mChatBackground;

    public static void start(Context ctx, String friendId, String path) {
        Intent intent = new Intent(ctx, SetChatBackActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendId);
        intent.putExtra(AppConstant.EXTRA_IMAGE_FILE_PATH, path);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_chang_chatbg);
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mChatBackgroundPath = getIntent().getStringExtra(AppConstant.EXTRA_IMAGE_FILE_PATH);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.preview));
        TextView tvRight = findViewById(R.id.tv_title_right);
        tvRight.setText(getString(R.string.finish));
        tvRight.setOnClickListener(this);
    }

    private void initView() {
        mChaIv = findViewById(R.id.chat_bg);
        File file = new File(mChatBackgroundPath);
        if (file.exists()) { // 加载本地
            if (mChatBackgroundPath.toLowerCase().endsWith("gif")) {
                try {
                    GifDrawable gifDrawable = new GifDrawable(file);
                    mChaIv.setImageDrawable(gifDrawable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ImageLoadHelper.showFileWithError(
                        SetChatBackActivity.this,
                        file,
                        R.drawable.fez,
                        mChaIv
                );
            }
        } else { // 加载网络
            ImageLoadHelper.showImageWithError(
                    this,
                    mChatBackgroundPath,
                    R.drawable.fez,
                    mChaIv
            );
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_title_right:
                DialogHelper.showDefaulteMessageProgressDialog(SetChatBackActivity.this);
                UploadingHelper.uploadFile(coreManager.getSelf().getUserId(), new File(mChatBackgroundPath), new UploadingHelper.OnUpFileListener() {
                    @Override
                    public void onSuccess(String url, String filePath) {
                        DialogHelper.dismissProgressDialog();
                        mChatBackground = url;
                        sureSet();
                    }

                    @Override
                    public void onFailure(String err, String filePath) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, err);
                    }
                }, false);
                break;
        }
    }

    private void sureSet() {
        PreferenceUtils.putString(SetChatBackActivity.this, Constants.SET_CHAT_BACKGROUND_PATH
                + mFriendId + mLoginUserId, mChatBackgroundPath);

        PreferenceUtils.putString(SetChatBackActivity.this, Constants.SET_CHAT_BACKGROUND
                + mFriendId + mLoginUserId, mChatBackground);
        Intent intent = new Intent();
        intent.putExtra("Operation_Code", 1);
        intent.setAction(OtherBroadcast.QC_FINISH);
        sendBroadcast(intent); // 设置聊天背景成功，发送广播更新单聊界面
        finish();
    }
}
