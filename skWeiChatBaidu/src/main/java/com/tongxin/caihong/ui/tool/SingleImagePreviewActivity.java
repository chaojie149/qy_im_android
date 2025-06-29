package com.tongxin.caihong.ui.tool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.message.HandleQRCodeScanUtil;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.PlayUploadUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.SaveWindow;
import com.tongxin.caihong.view.ZoomImageView;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.UserAvatarDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.chatHolder.MessageEventClickFire;
import com.tongxin.caihong.view.imageedit.IMGEditActivity;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import pl.droidsonroids.gif.GifDrawable;

/**
 * 单张图片预览
 * <p>
 * modify by zq
 * 目前有四个地方会进入此类，
 * 1.头像预览
 * 2.自定义表情预览
 * 3.聊天页面阅后即焚图片查看
 * 4.动态、我的收藏单图预览
 * <p>
 * 现图片长按弹窗针对这四个地方做不同的ui显示处理
 * 1.2 弹窗只显示编辑、保存图片
 * 3.4 在上面的基础上在显示发送给朋友与收藏功能
 * 注：3 因为是阅后即焚图片，其实不会显示弹窗。。。，所以弹窗ui显示判断publicMessage即可
 */
public class SingleImagePreviewActivity extends BaseActivity {
    public static final int REQUEST_IMAGE_EDIT = 1;
    private ZoomImageView mImageView;
    // 通用
    private String mImageUri;
    // 聊天图片预览
    private String mImagePath;
    // 是否为阅后即焚类型、消息id
    private boolean isReadDel;
    private String delPackedId;
    // 动态、收藏图片预览
    private PublicMessage publicMessage;
    private boolean isMyCollection;
    // 编辑之后的图片路径
    private String mEditedPath;

    // 用于 识别图中二维码
    private Bitmap mBitmap;
    private SaveWindow mSaveWindow;
    private My_BroadcastReceiver my_broadcastReceiver = new My_BroadcastReceiver();

    @SuppressWarnings("unused")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableSwipeBack = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_image_preview);

        if (getIntent() != null) {
            mImageUri = getIntent().getStringExtra(AppConstant.EXTRA_IMAGE_URI);

            mImagePath = getIntent().getStringExtra("image_path");
            isReadDel = getIntent().getBooleanExtra("isReadDel", false);
            if (isReadDel) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }
            delPackedId = getIntent().getStringExtra("DEL_PACKEDID");

            String message = getIntent().getStringExtra("json");
            publicMessage = JSON.parseObject(message, PublicMessage.class);
            isMyCollection = getIntent().getBooleanExtra("isMyCollection", false);
        }

        initView();
        register();
        if (!TextUtils.isEmpty(delPackedId)) {
            // 发送广播去更新聊天界面，移除该message
            EventBus.getDefault().post(new MessageEventClickFire("delete", delPackedId));
        }
    }

    @Override
    public void onBackPressed() {
        doBack();
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (my_broadcastReceiver != null) {
            unregisterReceiver(my_broadcastReceiver);
        }
    }

    private void initView() {
        getSupportActionBar().hide();
        mImageView = findViewById(R.id.image_view);
        if (TextUtils.isEmpty(mImageUri)) {
            Toast.makeText(mContext, R.string.image_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        /*
        加载图片 || 头像
         */
        if (mImageUri.contains("http")) {
            // 图片 头像的mImageUri为UserId
            boolean isExists = false;
            if (!TextUtils.isEmpty(mImagePath)) {
                File file = new File(mImagePath);
                if (file.exists()) {
                    isExists = true;
                }
            }
            if (isExists) {
                // 本地加载
                if (mImageUri.endsWith(".gif")) {
                    try {
                        GifDrawable gifDrawable = new GifDrawable(new File(mImagePath));
                        mImageView.setImageDrawable(gifDrawable);
                    } catch (Exception e) {
                        mImageView.setImageResource(R.drawable.image_download_fail_icon);
                        e.printStackTrace();
                    }
                } else {
                    ImageLoadHelper.loadBitmapCenterCropDontAnimateWithError(
                            mContext,
                            mImagePath,
                            R.drawable.image_download_fail_icon,
                            b -> {
                                mBitmap = b;
                                mImageView.setImageBitmap(b);
                            }, e -> {
                                mImageView.setImageResource(R.drawable.image_download_fail_icon);
                            });
                }
            } else {
                // 网络加载
                if (mImageUri.endsWith(".gif")) {
                    ImageLoadHelper.showGifWithError(
                            mContext,
                            mImageUri,
                            R.drawable.image_download_fail_icon,
                            mImageView
                    );
                } else {
                    ImageLoadHelper.loadBitmapCenterCropDontAnimateWithError(
                            mContext,
                            mImageUri,
                            R.drawable.image_download_fail_icon,
                            b -> {
                                mBitmap = b;
                                mImageView.setImageBitmap(b);
                            }, e -> {
                                mImageView.setImageResource(R.drawable.image_download_fail_icon);
                            });
                }
            }
        } else {
            // 头像
            String time = UserAvatarDao.getInstance().getUpdateTime(mImageUri);
            // 为头像重新赋值，用于保存功能
            mImageUri = AvatarHelper.getAvatarUrl(mImageUri, false);
            if (TextUtils.isEmpty(mImageUri)) {
                mImageView.setImageResource(R.drawable.avatar_normal);
                return;
            }
            ImageLoadHelper.loadImageSignatureDontAnimateWithPlaceHolder(
                    mContext,
                    mImageUri,
                    R.drawable.avatar_normal,
                    time,
                    d -> {
                        mBitmap = BitmapUtil.drawableToBitmap(d);
                        mImageView.setImageDrawable(d);
                    }, e -> mImageView.setImageResource(R.drawable.avatar_normal)
            );
        }
    }

    private void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(OtherBroadcast.singledown);
        filter.addAction(OtherBroadcast.longpress);
        registerReceiver(my_broadcastReceiver, filter);
    }

    public void doBack() {
        // 有可能在查看时强行关闭app，导致可以无限查看，直接在进入该activity时删除
/*
        if (!TextUtils.isEmpty(delPackedId)) {
            // 发送广播去更新聊天界面，移除该message
            EventBus.getDefault().post(new MessageEventClickFire("delete", delPackedId));
        }
*/
        finish();
        overridePendingTransition(0, 0);// 关闭过场动画
    }

    private void collection(String emoji) {
        Map<String, String> params = new HashMap<>();
        if (!TextUtils.isEmpty(mEditedPath)
                && TextUtils.isEmpty(emoji)) {
            // 编辑的图片，先上传，得到新url，在调用收藏方法，传入封装好的参数
            PlayUploadUtil.uploadSingle(mContext, mEditedPath, new PlayUploadUtil.UploadSingleCallback() {
                @Override
                public void uploadSingleFail() {
                    ToastUtil.showToast(mContext, getString(R.string.upload_failed));
                }

                @Override
                public void uploadSingleSuccess(String result) {
                    com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
                    com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
                    json.put("type", String.valueOf(CollectionEvery.TYPE_IMAGE));
                    json.put("msg", result);
                    json.put("collectType", -1);// 与消息无关的收藏
                    array.add(json);
                    collection(JSON.toJSONString(array));
                }
            });
            return;
        }
        if (TextUtils.isEmpty(emoji)) {
            emoji = collectionParam(publicMessage);
        }
        params.put("emoji", emoji);

        HttpUtils.post().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, mContext.getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private String collectionParam(PublicMessage message) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = CollectionEvery.TYPE_IMAGE_SINGLE;
        String msg = "";
        String id = message.getMessageId();
        PublicMessage.Resource res = null;
        if (message.getBody() != null) {
            List<PublicMessage.Resource> images = message.getBody().getImages();
            // 莫名出现类型为图片，但是没有图片的朋友圈消息，略做兼容，
            if (images == null || images.isEmpty()) {
                Log.e(TAG, "图片为空！");
            }
            StringBuilder sb = new StringBuilder();
            boolean firstTime = true;
            for (PublicMessage.Resource token : images) {
                String url = token.getOriginalUrl();
                if (TextUtils.isEmpty(url)) {
                    continue;
                }
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(',');
                }
                sb.append(url);
            }
            msg = sb.toString();
        }

        json.put("type", String.valueOf(type));
        json.put("msg", msg);
        json.put("url", msg);
        json.put("collectType", 1);
        json.put("collectMsgId", id);
        json.put("toUserId", message.getUserId());
        json.put("targetType", 1); // 收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊

        array.add(json);
        return JSON.toJSONString(array);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_EDIT:
                    mImagePath = mEditedPath;
                    mImageUri = new File(mEditedPath).toURI().toString();
                    ImageLoadHelper.loadBitmapCenterCropDontAnimateWithError(
                            mContext,
                            mImagePath,
                            R.drawable.image_download_fail_icon,
                            b -> {
                                mBitmap = b;
                                mImageView.setImageBitmap(b);
                            },
                            e -> {
                                mImageView.setImageResource(R.drawable.image_download_fail_icon);
                            }
                    );
                    // 模拟那个长按，弹出菜单，
                    Intent intent = new Intent(OtherBroadcast.longpress);
                    sendBroadcast(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(OtherBroadcast.singledown)) {
                // 轻触屏幕，退出预览
                doBack();
            } else if (intent.getAction().equals(OtherBroadcast.longpress)) {
                // 长按屏幕，弹出菜单
                if (isReadDel) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_burn_image_cannot_more));
                    return;
                }
                String text = BitmapUtil.getTextQRCode(SingleImagePreviewActivity.this, mBitmap);
                boolean isShowCollection = publicMessage != null;
                if (isMyCollection) {
                    isShowCollection = false;
                }
                mSaveWindow = new SaveWindow(SingleImagePreviewActivity.this
                        , publicMessage != null
                        , isShowCollection
                        , !TextUtils.isEmpty(text)
                        , new ClickListener(text));
                mSaveWindow.show();
            }
        }
    }

    class ClickListener implements View.OnClickListener {

        private String text;

        public ClickListener(String text) {
            this.text = text;
        }

        @Override
        public void onClick(View v) {
            mSaveWindow.dismiss();
            switch (v.getId()) {
                case R.id.tv_send_to_friend:
                    if (!TextUtils.isEmpty(mEditedPath)) {
                        // 编辑的图片，先上传，得到新url，在发送
                        PlayUploadUtil.uploadSingle(mContext, mEditedPath, new PlayUploadUtil.UploadSingleCallback() {
                            @Override
                            public void uploadSingleFail() {
                                ToastUtil.showToast(mContext, getString(R.string.upload_failed));
                            }

                            @Override
                            public void uploadSingleSuccess(String result) {
                                MessageUtil.generateImageMessage2Intent(mContext, coreManager.getSelf().getUserId(), result);
                            }
                        });
                    } else {
                        // 未编辑的图片，直接发送原url
                        MessageUtil.generateImageMessage2Intent(mContext, coreManager.getSelf().getUserId(), publicMessage.getFirstImageOriginal());
                    }
                    break;
                case R.id.tv_collection:
                    collection("");
                    break;
                case R.id.save_image:
                    if (!TextUtils.isEmpty(delPackedId)) {
                        Toast.makeText(SingleImagePreviewActivity.this, R.string.tip_burn_image_cannot_save, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!TextUtils.isEmpty(mImagePath) && mImageUri.toLowerCase().endsWith("gif")) {// 保存Gif
                        FileUtil.downImageToGallery(SingleImagePreviewActivity.this, mImagePath);
                    } else {// 保存图片
                        FileUtil.downImageToGallery(SingleImagePreviewActivity.this, mImageUri);
                    }
                    break;
                case R.id.edit_image:
                    ImageLoadHelper.loadFile(
                            SingleImagePreviewActivity.this,
                            mImageUri,
                            f -> {
                                mEditedPath = FileUtil.createImageFileForEdit().getAbsolutePath();
                                IMGEditActivity.startForResult(SingleImagePreviewActivity.this, Uri.fromFile(f), mEditedPath, REQUEST_IMAGE_EDIT);
                            });
                    break;
                case R.id.identification_qr_code:
                    // 识别图中二维码
                    if (TextUtils.isEmpty(text)) {// 理论上不太可能了，因为该item显示时，bitmap都不为空
                        Toast.makeText(SingleImagePreviewActivity.this, R.string.unrecognized, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    HandleQRCodeScanUtil.handleScanResult(mContext, text);
                    break;
            }
        }
    }

}
