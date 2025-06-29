package com.tongxin.caihong.xmpp.util;

import android.content.Intent;
import android.text.TextUtils;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zq
 * 处理公司消息
 */
public class HandleCompanyMessage {

    public static void handleMessage(String mLoginUserId, ChatMessage chatMessage) {
        if (chatMessage.getType() == XmppMessage.TYPE_COMPANY_APPLY_JOIN) {
            List<ChatMessage> data = new ArrayList<>();
            data = ChatMessageDao.getInstance().getOneGroupChatMessages(data
                    , mLoginUserId
                    , Friend.ID_SYSTEM_COLLEAGUE_NOTIFICATION
                    , TimeUtils.sk_time_current_time()
                    , 1000);
            List<ChatMessage> cFromUserData = new ArrayList<>();
            for (ChatMessage message : data) {
                if (TextUtils.equals(message.getFromUserId()
                        , chatMessage.getFromUserId())) {
                    cFromUserData.add(message);
                }
            }
            if (cFromUserData.size() > 0) {
                // 本地之前有此人的申请加入公司记录，判断申请加入的是否为同一个公司
                ChatMessage sameApplyMessage = null;
                for (ChatMessage cFromUserMessage : cFromUserData) {
                    if (TextUtils.equals(cFromUserMessage.getObjectId(), chatMessage.getObjectId())) {
                        sameApplyMessage = cFromUserMessage;
                        break;
                    }
                }
                if (sameApplyMessage != null) {
                    // 本地之前有此人的申请加入公司记录，且当前申请加入的公司与本地记录的一致，更新时间以及内容
                    ChatMessageDao.getInstance().updateMessageCompanyApply(mLoginUserId
                            , Friend.ID_SYSTEM_COLLEAGUE_NOTIFICATION
                            , false
                            , chatMessage.getContent()
                            , chatMessage.getTimeSend()
                            , sameApplyMessage.getPacketId());
                } else {
                    // 本地之前有此人的申请加入公司记录，但当前申请加入的公司与本地记录的不一致，直接存
                    ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, Friend.ID_SYSTEM_COLLEAGUE_NOTIFICATION, chatMessage);
                }
            } else {
                // 本地之前没有此人的申请加入公司记录，直接存
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, Friend.ID_SYSTEM_COLLEAGUE_NOTIFICATION, chatMessage);
            }

            // 标记有新消息，广播通知刷新
            PreferenceUtils.putBoolean(MyApplication.getContext(), AppConstant.COMPANY_APPLY_JOIN_MSG, true);
            MyApplication.getContext().sendBroadcast(new Intent(OtherBroadcast.ACTION_COMPANY_APPLY_JOIN_MSG));
        }
    }
}
