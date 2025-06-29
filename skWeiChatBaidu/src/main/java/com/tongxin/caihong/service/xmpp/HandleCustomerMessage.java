package com.tongxin.caihong.service.xmpp;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.service.bean.EventServiceForward;
import com.tongxin.caihong.R;

import de.greenrobot.event.EventBus;

/**
 * 处理访客消息
 */
public class HandleCustomerMessage {

    public static boolean process(String mLoginUserId, ChatMessage chatMessage) {
        if (chatMessage.getType() == XmppMessage.TYPE_SERVICE_FORWARD) {
            chatMessage.setFromUserId(chatMessage.getObjectId());
            chatMessage.setToUserId(mLoginUserId);
            chatMessage.setType(XmppMessage.TYPE_TIP);
            if (chatMessage.getFileSize() == 0) {
                // 自动分配，
                chatMessage.setContent(MyApplication.getContext().getString(R.string.service_tip_auto_forward_place_holder, chatMessage.getToUserName()));
            } else if (chatMessage.getFileSize() == 1) {
                // 手动转接，
                chatMessage.setContent(MyApplication.getContext().getString(R.string.service_tip_forward_place_holder, chatMessage.getToUserName()));
            } else {
                // 无客服在线，
                chatMessage.setContent(MyApplication.getContext().getString(R.string.service_tip_forward_offline));
            }
            EventBus.getDefault().post(new EventServiceForward(chatMessage.getObjectId(), chatMessage.getServiceId()));
        }
        return false;
    }

}
