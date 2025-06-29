package com.tongxin.caihong.service.bean;

public class ServiceInfo {
    private int serviceIsOnline;
    private String serviceUserId;

    public int getServiceIsOnline() {
        return serviceIsOnline;
    }

    public void setServiceIsOnline(int serviceIsOnline) {
        this.serviceIsOnline = serviceIsOnline;
    }

    public String getServiceUserId() {
        return serviceUserId;
    }

    public void setServiceUserId(String serviceUserId) {
        this.serviceUserId = serviceUserId;
    }

    @Override
    public String toString() {
        return
                "ServiceInfo{" +
                        "serviceIsOnline = '" + serviceIsOnline + '\'' +
                        ",service_userId = '" + serviceUserId + '\'' +
                        "}";
    }
}
