package com.tongxin.caihong.view.chatHolder;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;
import com.tongxin.caihong.audio_x.VoiceAnimView;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.downloader.DownloadListener;
import com.tongxin.caihong.downloader.Downloader;
import com.tongxin.caihong.downloader.FailReason;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.view.ChatContentView;

public class VoiceViewHolder extends AChatHolderInterface implements DownloadListener {

    public VoiceAnimView voiceView;
    @Nullable
    public View rlTranslation;
    @Nullable
    public TextView tvTranslation;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_voice : R.layout.chat_to_item_voice;
    }

    @Override
    public void initView(View view) {
        rlTranslation = view.findViewById(R.id.rlTranslation);
        tvTranslation = view.findViewById(R.id.tvTranslation);
        voiceView = view.findViewById(R.id.chat_voice);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        voiceView.fillData(message);

        String filePath = UploadCacheUtils.get(mContext, message);
        // 文件不存在 就去下载
        if (!FileUtil.isExist(filePath)) {
            Downloader.getInstance().addDownload(message.getContent(), mSendingBar, this);
        }
        if (rlTranslation != null && tvTranslation != null) {
            if (TextUtils.isEmpty(message.getTranslation())) {
                rlTranslation.setVisibility(View.GONE);
            } else {
                rlTranslation.setVisibility(View.VISIBLE);
                tvTranslation.setText(message.getTranslation());
            }
        }
    }

    @Override
    protected void onRootClick(View v) {
        ivUnRead.setVisibility(View.GONE);
        if (ChatContentView.fireVoiceStopMap.containsKey(voiceView.getVoiceMsgId())) {
            return;
        }
        VoicePlayer.instance().playVoice(voiceView);
    }

    @Override
    public void onStarted(String uri, View view) {
        mSendingBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFailed(String uri, FailReason failReason, View view) {
        Log.e("VOICE", "onFailed" + failReason.getType());
        mSendingBar.setVisibility(View.GONE);
        mIvFailed.setVisibility(View.VISIBLE);
        if (isMysend && mdata.isSendRead()) {// 服务端将文件删除了但是消息还在，漫游拉下来会显示感叹号
            mIvFailed.setVisibility(View.GONE);
        }
    }

    @Override
    public void onComplete(String uri, String filePath, View view) {
        mdata.setFilePath(filePath);
        mSendingBar.setVisibility(View.GONE);

        if (mHolderListener != null) {
            mHolderListener.onCompDownVoice(mdata);
        }

        // 更新数据库
        ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, mdata.get_id(), true, filePath);
    }

    @Override
    public void onCancelled(String uri, View view) {
        Log.e("VOICE", "onCancelled");
        mSendingBar.setVisibility(View.GONE);
        // mIvFailed.setVisibility(View.VISIBLE);
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
