package com.tongxin.caihong.bean.event;

import com.tongxin.caihong.ui.message.ChatActivity;

/**
 * Created by Administrator on 2017/6/26 0026.
 */
public class MessageUploadChatRecord {
    public String chatIds;
    public String toUserId;

    /**
     * @see ChatActivity
     */
    public MessageUploadChatRecord(String chatIds, String toUserId) {
        this.chatIds = chatIds;
        this.toUserId = toUserId;
    }
}