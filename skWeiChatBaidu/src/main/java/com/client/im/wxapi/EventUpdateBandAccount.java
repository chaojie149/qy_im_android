package com.client.im.wxapi;

public class EventUpdateBandAccount {
    public String result;
    public String msg;

    public EventUpdateBandAccount(String result, String ok) {
        this.result = result;
        this.msg = ok;
    }
}
