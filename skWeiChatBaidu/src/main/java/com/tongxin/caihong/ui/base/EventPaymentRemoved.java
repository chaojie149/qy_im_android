package com.tongxin.caihong.ui.base;

public class EventPaymentRemoved {
    private String id;

    public EventPaymentRemoved(String id) {

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
