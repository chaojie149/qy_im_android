package com.tongxin.caihong.view.chatHolder;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.ui.me.FontSizeActivity;
import com.tongxin.caihong.ui.message.multi.NoticeListActivity;
import com.tongxin.caihong.util.SkinUtils;

class NoticeViewHolder extends AChatHolderInterface {
    private ImageView ivNotice;
    private TextView tvNotice;
    private TextView tvLook;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_notice : R.layout.chat_to_item_notice;
    }

    @Override
    public void initView(View view) {
        ivNotice = view.findViewById(R.id.ivNotice);
        tvNotice = view.findViewById(R.id.tvNotice);
        tvLook = view.findViewById(R.id.tvLook);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        ImageViewCompat.setImageTintList(ivNotice, ColorStateList.valueOf(SkinUtils.getSkin(mContext).getAccentColor()));
        // 修改字体功能
        int size = FontSizeActivity.getFontSize(mContext);
        tvNotice.setTextSize(size);
        tvNotice.setText(message.getContent());
        if (message.isDownload()) {
            // 公告已删除
            tvLook.setText(getString(R.string.tip_notice_deleted));
            tvLook.setTextColor(mContext.getResources().getColor(R.color.text_value));
            tvLook.setOnClickListener(null);
        } else {
            tvLook.setText(getString(R.string.look_over));
            tvLook.setTextColor(mContext.getResources().getColor(R.color.black));
            tvLook.setOnClickListener(view -> {
                if (selfGroupRole == null) {
                    selfGroupRole = 3;
                }
                NoticeListActivity.start(mContext, selfGroupRole, roomId);
            });
        }
    }

    @Override
    public void showTime(String time) {

    }

    @Override
    protected void onRootClick(View v) {

    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

}
