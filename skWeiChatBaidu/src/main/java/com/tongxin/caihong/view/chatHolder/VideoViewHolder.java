package com.tongxin.caihong.view.chatHolder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.downloader.DownloadListener;
import com.tongxin.caihong.downloader.DownloadProgressListener;
import com.tongxin.caihong.downloader.DownloadTask;
import com.tongxin.caihong.downloader.Downloader;
import com.tongxin.caihong.downloader.FailReason;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.UploadEngine;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.video.ChatVideoPreviewActivity;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.XuanProgressPar;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;

public class VideoViewHolder extends AChatHolderInterface implements DownloadListener, DownloadProgressListener {

    // JVCideoPlayerStandardforchat mVideo;
    ImageView mVideo;
    ImageView ivStart;
    XuanProgressPar progressPar;
    TextView tvInvalid;
    ImageView ivUploadCancel;
    boolean autoPlay;
    private ChatMessage message;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_video : R.layout.chat_to_item_video;
    }

    @Override
    public void initView(View view) {
        mVideo = view.findViewById(R.id.chat_jcvideo);
        ivStart = view.findViewById(R.id.iv_start);
        progressPar = view.findViewById(R.id.img_progress);
        tvInvalid = view.findViewById(R.id.tv_invalid);
        ivUploadCancel = view.findViewById(R.id.chat_upload_cancel_iv);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        this.message = message;
        autoPlay = false;
        tvInvalid.setVisibility(View.GONE);
        ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_play_selector);
        String filePath = UploadCacheUtils.get(mContext, message);
        boolean isExist = FileUtil.isExist(filePath);

        if (!isExist) {
            AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(message.getContent(), mVideo);
        } else {
            AvatarHelper.getInstance().asyncDisplayVideoThumb(filePath, mVideo);
        }

        if (isMysend) { // 判断是否上传
            // 没有上传
            boolean show = !message.isUpload() && message.getUploadSchedule() < 100
                    && message.getMessageState() == ChatMessageListener.MESSAGE_SEND_ING;
            changeVisible(progressPar, show);
            changeVisible(ivStart, !show);

            if (show) {
                if (ivUploadCancel != null) {
                    ivUploadCancel.setVisibility(View.VISIBLE);
                }
            } else {
                if (ivUploadCancel != null) {
                    ivUploadCancel.setVisibility(View.GONE);
                }
            }
        } else {
            changeVisible(progressPar, false);
            changeVisible(ivStart, true);
        }

        progressPar.update(message.getUploadSchedule());

        if (ivUploadCancel != null) {
            ivUploadCancel.setOnClickListener(v -> {
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(getString(R.string.cancel_upload), getString(R.string.sure_cancel_upload), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        // 用户可能在弹窗弹起后停留很久，所以点击确认的时候还需要判断一下
                        if (!mdata.isUpload()) {
                            UploadEngine.cancel(mdata.getPacketId());
                        }
                    }
                });
                selectionFrame.show();
            });
        }

        Downloader.getInstance().cancelDisplayTaskFor(progressPar);
        if (!isExist) {
            filePath = message.getContent();
            if (DownloadTask.sProgressListenerMap.containsValue(filePath)) {
                DownloadTask.sProgressListenerMap.put(VideoViewHolder.this, filePath);
            }
        }
    }

    @Override
    protected void onRootClick(View v) {
        if (tvInvalid.getVisibility() == View.VISIBLE) {
            return;
        }

        String filePath = UploadCacheUtils.get(mContext, mdata);
        if (!FileUtil.isExist(filePath)) {
            filePath = mdata.getContent();
            // 本地不存在，传网络路径进去播放，下载。。。
            if (HttpUtil.isConnectedGprs(mContext)) {
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                String finalFilePath = filePath;
                selectionFrame.setSomething(null, getString(R.string.tips_not_wifi), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        autoPlay = true;
                        Downloader.getInstance().addDownload(finalFilePath, mSendingBar, VideoViewHolder.this, VideoViewHolder.this);
                    }
                });
                selectionFrame.show();
            } else {
                autoPlay = true;
                Downloader.getInstance().addDownload(filePath, mSendingBar, VideoViewHolder.this, VideoViewHolder.this);
            }
        } else {
            startPlay(filePath);
        }
    }

    private void startPlay(String filePath) {
        if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
            // activity已经结束了就不播放了，
            return;
        }
        Intent intent = new Intent(mContext, ChatVideoPreviewActivity.class);
        intent.putExtra(AppConstant.EXTRA_VIDEO_FILE_PATH, filePath);
        if (mdata.getIsReadDel() && !isMysend) {
            intent.putExtra("DEL_PACKEDID", mdata.getPacketId());
        }
        intent.putExtra("json", JSON.toJSONString(message));
        intent.putExtra("isGounp", isGounp);
        ivUnRead.setVisibility(View.GONE);
        mContext.startActivity(intent);
    }

    @Override
    public void onStarted(String uri, View view) {
        changeVisible(progressPar, true);
        changeVisible(ivStart, false);
    }

    @Override
    public void onFailed(String uri, FailReason failReason, View view) {
        changeVisible(progressPar, false);
        ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_error_selector);
        tvInvalid.setVisibility(View.VISIBLE);
        ivStart.setVisibility(View.VISIBLE);
    }

    @Override
    public void onComplete(String uri, String filePath, View view) {
        mdata.setFilePath(filePath);
        changeVisible(progressPar, false);
        changeVisible(ivStart, true);
        ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_play_selector);

        // 更新数据库
        ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, mdata.get_id(), true, filePath);
        AvatarHelper.getInstance().asyncDisplayVideoThumb(filePath, mVideo);
        Context ctx = view.getContext();
        startPlay(filePath);
    }

    @Override
    public void onCancelled(String uri, View view) {
        changeVisible(progressPar, false);
        changeVisible(ivStart, true);
    }

    @Override
    public void onProgressUpdate(String imageUri, View view, int current, int total) {
        int pro = (int) (current / (float) total * 100);
        progressPar.update(pro);
        if (pro >= 100) {
            changeVisible(progressPar, false);
            changeVisible(ivStart, true);
        }
    }

    @Override
    public boolean enableUnRead() {
        return true;
    }

    @Override
    public boolean enableFire() {
        return true;
    }

}
