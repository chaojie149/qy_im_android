package com.tongxin.caihong.bean.event;

import com.tongxin.caihong.bean.message.ChatMessage;

public class EventRemoveNotice {
    private String roomJid;

    public EventRemoveNotice(ChatMessage chatMessage) {
        this.roomJid = chatMessage.getObjectId();
    }

    public String getRoomJid() {
        return roomJid;
    }
}
