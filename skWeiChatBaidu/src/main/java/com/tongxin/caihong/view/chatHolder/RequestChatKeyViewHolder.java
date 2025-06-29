package com.tongxin.caihong.view.chatHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.message.HandleSecureChatMessage;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;

class RequestChatKeyViewHolder extends AChatHolderInterface {

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
        requestTv.setText(getString(R.string.tip_need_secret, message.getFromUserName()));

        if (TextUtils.equals(message.getFromUserId(), mLoginUserId)) {
            // 自己请求密钥
            if (message.getFileSize() == 3) {
                // 已收到密钥
                sendTv.setText(getString(R.string.tip_has_receive_chat_key));
            } else {
                // 等待他人发送密钥
                sendTv.setText(getString(R.string.wait_member_send_key));
            }
        } else {
            if (message.getFileSize() == 2 || message.getFileSize() == 3) {
                // 已发送密钥
                sendTv.setTextColor(MyApplication.getContext().getResources().getColor(R.color.Grey_500));
                if (message.getFileSize() == 2) {
                    // 自己发送的密钥
                    sendTv.setText(getString(R.string.tip_self_send_chat_key));
                } else {
                    // 其他人发送的密钥
                    sendTv.setText(getString(R.string.tip_each_send_chat_key));
                }
                sendTv.setOnClickListener(null);
            } else {
                // 未发送密钥
                sendTv.setTextColor(MyApplication.getContext().getResources().getColor(R.color.app_black));
                sendTv.setText(getString(R.string.send_key));
                sendTv.setOnClickListener(v -> {
                    if (TextUtils.equals(message.getFromUserId(), mLoginUserId)) {
                        // 上面已经针对自己修改了按钮描述，此处就不提示了
                        // ToastUtil.showToast(mContext, getString(R.string.self_cannot_send_chat_key_to_self));
                        return;
                    }
                    Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mToUserId);
                    if (friend != null && friend.getIsLostChatKeyGroup() == 1) {
                        ToastUtil.showToast(mContext, getString(R.string.you_cannot_send_chat_key_to_self));
                        return;
                    }
                    message.setFileSize(2);
                    HandleSecureChatMessage.sendChatKeyForRequestedMember(message);
                });
            }
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
