package com.tongxin.caihong.service.bean;

public class EventServiceForward {
    public String serviceId;
    public String companyMpId;

    public EventServiceForward(String companyMpId, String serviceId) {
        this.companyMpId = companyMpId;
        this.serviceId = serviceId;
    }
}
