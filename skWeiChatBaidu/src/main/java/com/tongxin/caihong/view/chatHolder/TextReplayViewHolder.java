package com.tongxin.caihong.view.chatHolder;

import android.graphics.Point;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.ui.me.FontSizeActivity;
import com.tongxin.caihong.util.DES;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.Md5Util;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.link.HttpTextView;
import com.tongxin.caihong.view.selectable.SelectableTextHelper;

public class TextReplayViewHolder extends AChatHolderInterface {

    public HttpTextView mTvContent;
    public HttpTextView mTvReplayContent;
    public TextView tvFireTime;
    @Nullable
    public View rlTranslation;
    @Nullable
    public TextView tvTranslation;
    private Point eventPoint;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_text_replay : R.layout.chat_to_item_text_replay;
    }

    @Override
    public void initView(View view) {
        rlTranslation = view.findViewById(R.id.rlTranslation);
        tvTranslation = view.findViewById(R.id.tvTranslation);
        mTvContent = view.findViewById(R.id.chat_text);
        mTvReplayContent = view.findViewById(R.id.chat_text_replay);
        mRootView = view.findViewById(R.id.chat_warp_view);
        if (!isMysend) {
            tvFireTime = view.findViewById(R.id.tv_fire_time);
        }
    }

    @Override
    public void fillData(ChatMessage message) {
        // 修改字体功能
        int size = FontSizeActivity.getFontSize(mContext);
        mTvContent.setTextSize(size);
        mTvContent.setTextColor(mContext.getResources().getColor(R.color.black));

        String content = StringUtils.replaceSpecialChar(message.getContent());
        CharSequence charSequence = HtmlUtils.transform200SpanString(content, true);
        if (message.getIsReadDel() && !isMysend) {// 阅后即焚
            if (!message.isGroup() && !message.isSendRead()) {
                mTvContent.setText(R.string.tip_click_to_read);
                mTvContent.setTextColor(mContext.getResources().getColor(R.color.redpacket_bg));
            } else {
                // 已经查看了，当适配器再次刷新的时候，不需要重新赋值
                mTvContent.setText(charSequence);
            }
        } else {
            mTvContent.setText(charSequence);
        }

        if (!TextUtils.isEmpty(message.getObjectId())) {
            mTvReplayContent.setVisibility(View.VISIBLE);
            ChatMessage replayMessage = new ChatMessage(message.getObjectId());
            /**
             * 在此处对消息进行解密
             */
            if (replayMessage.getIsEncrypt() == 1) {
                try {
                    String decryptKey = Md5Util.toMD5(AppConfig.getApiKey() + replayMessage.getTimeSend() + replayMessage.getPacketId());
                    String decryptContent = DES.decryptDES(replayMessage.getContent(), decryptKey);
                    // 为chatMessage重新设值
                    replayMessage.setContent(decryptContent);
                } catch (Exception e) {
                    LogUtils.log(replayMessage.toJsonString());
                    Reporter.post("解密失败<" + replayMessage.getPacketId() + ">", e);
                }
            }
            SpannableStringBuilder sb = new SpannableStringBuilder()
                    .append(replayMessage.getFromUserName())
                    .append(": ")
                    .append(HtmlUtils.addSmileysToMessage(replayMessage.getSimpleContent(mContext), false));
            mTvReplayContent.setText(sb);
        } else {
            mTvReplayContent.setVisibility(View.GONE);
        }

        mTvReplayContent.setOnClickListener(v -> {
            mHolderListener.onReplayClick(v, this, mdata);
        });

        mTvContent.setUrlText(mTvContent.getText());
        mTvContent.setOnClickListener(v -> mHolderListener.onItemClick(mRootView, TextReplayViewHolder.this, mdata));
        mTvContent.setOnLongClickListener(v -> {
            mTvContent.breakClick();
            mHolderListener.onItemLongClick(v, eventPoint, TextReplayViewHolder.this, mdata);
            return true;
        });
        mTvContent.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTvContent.resetClick();
                    eventPoint = new Point((int) e.getX(), (int) e.getY());
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    // 更新选中，
                    Object tag = mTvContent.getTag(R.id.selectable_helper);
                    if (tag instanceof SelectableTextHelper) {
                        v.post(() -> {
                            ((SelectableTextHelper) tag).ensureSelection();
                        });
                    }
                    break;
            }
            return false;
        });
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

    }

    @Override
    public boolean enableFire() {
        return true;
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    public void showFireTime(boolean show) {
        if (tvFireTime != null) {
            tvFireTime.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
