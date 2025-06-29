package com.tongxin.caihong.bean;

public class EventMucMeetStatusChange {
    private String roomJid;
    private String fromUserName;
    private int status;

    public EventMucMeetStatusChange(String roomJid, int status) {
        this(roomJid, "", status);
    }

    public EventMucMeetStatusChange(String roomJid, String fromUserName, int status) {
        this.roomJid = roomJid;
        this.fromUserName = fromUserName;
        this.status = status;
    }

    public String getRoomJid() {
        return roomJid;
    }

    public int getStatus() {
        return status;
    }

    public String getFromUserName() {
        return fromUserName;
    }
}
