package com.tongxin.caihong.bean;

import java.util.List;

public class OfflineReadPersons {
    private List<MessageListItem> messageList;
    private String roomJid;

    public List<MessageListItem> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<MessageListItem> messageList) {
        this.messageList = messageList;
    }

    public String getRoomJid() {
        return roomJid;
    }

    public void setRoomJid(String roomJid) {
        this.roomJid = roomJid;
    }

    @Override
    public String toString() {
        return
                "OfflineReadPersons{" +
                        "messageList = '" + messageList + '\'' +
                        ",roomJid = '" + roomJid + '\'' +
                        "}";
    }
}