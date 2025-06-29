package com.tongxin.caihong.view.chatHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.R;

class ServiceCommentViewHolder extends AChatHolderInterface {

    ImageView requestIv;
    TextView requestTv;
    TextView sendTv;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_request_chat_key : R.layout.chat_to_item_request_chat_key;
    }

    @Override
    public void initView(View view) {
        requestIv = view.findViewById(R.id.request_iv);
        requestTv = view.findViewById(R.id.request_tv);
        sendTv = view.findViewById(R.id.tv_bottom);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        AvatarHelper.getInstance().displayAvatar(message.getFromUserName(), message.getFromUserId(), requestIv, true);
        requestTv.setText(getString(R.string.service_title_grade));

        if (message.isDownload()) {// 已经发送过了
            sendTv.setText(R.string.service_has_grade);
            sendTv.setTextColor(MyApplication.getContext().getResources().getColor(R.color.Grey_500));
            sendTv.setOnClickListener(null);
        } else {
            sendTv.setText(R.string.service_to_grade);
            sendTv.setTextColor(MyApplication.getContext().getResources().getColor(R.color.app_black));
            sendTv.setOnClickListener(this);
        }
    }

    @Override
    public boolean isOnClick() {
        return false;
    }

    @Override
    protected void onRootClick(View v) {

    }

    @Override
    public boolean enableSendRead() {
        return true;
    }
}
