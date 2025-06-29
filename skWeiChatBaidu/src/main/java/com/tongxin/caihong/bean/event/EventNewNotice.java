package com.tongxin.caihong.bean.event;

import com.tongxin.caihong.bean.message.ChatMessage;

public class EventNewNotice {
    private String text;
    private String roomJid;

    public EventNewNotice(ChatMessage chatMessage) {
        this.text = chatMessage.getContent();
        this.roomJid = chatMessage.getObjectId();
    }

    public String getText() {
        return text;
    }

    public String getRoomJid() {
        return roomJid;
    }
}
