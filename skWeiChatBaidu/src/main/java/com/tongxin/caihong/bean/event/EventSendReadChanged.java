package com.tongxin.caihong.bean.event;

public class EventSendReadChanged {
    public String userId;
    public boolean isSendRead;

    public EventSendReadChanged(String userId, boolean isSendRead) {
        this.userId = userId;
        this.isSendRead = isSendRead;
    }

}
