package com.tongxin.caihong.bean.event;

public class EventPublicMessageRemoved {
    public int type;
    public String id;

    public EventPublicMessageRemoved(int type, String id) {
        this.type = type;
        this.id = id;
    }
}
