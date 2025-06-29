package com.tongxin.caihong.bean.event;

public class EventRoomBlackChange {
    private String roomJid;

    public EventRoomBlackChange(String roomJid) {
        this.roomJid = roomJid;
    }

    public String getRoomJid() {
        return roomJid;
    }
}
