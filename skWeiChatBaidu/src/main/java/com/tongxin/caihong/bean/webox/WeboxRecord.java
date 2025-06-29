package com.tongxin.caihong.bean.webox;

import java.util.List;

public class WeboxRecord {
    private String walletId;
    private String returnSize;
    private String startDateTime;
    private String merchantId;
    private String pageIndex;
    private List<WeboxRecordsItem> records;
    private String hmac;
    private String pageSize;
    private String endDateTime;
    private String status;

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setReturnSize(String returnSize) {
        this.returnSize = returnSize;
    }

    public String getReturnSize() {
        return returnSize;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setPageIndex(String pageIndex) {
        this.pageIndex = pageIndex;
    }

    public String getPageIndex() {
        return pageIndex;
    }

    public void setRecords(List<WeboxRecordsItem> records) {
        this.records = records;
    }

    public List<WeboxRecordsItem> getRecords() {
        return records;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getHmac() {
        return hmac;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return
                "WeboxRecord{" +
                        "walletId = '" + walletId + '\'' +
                        ",returnSize = '" + returnSize + '\'' +
                        ",startDateTime = '" + startDateTime + '\'' +
                        ",merchantId = '" + merchantId + '\'' +
                        ",pageIndex = '" + pageIndex + '\'' +
                        ",records = '" + records + '\'' +
                        ",hmac = '" + hmac + '\'' +
                        ",pageSize = '" + pageSize + '\'' +
                        ",endDateTime = '" + endDateTime + '\'' +
                        ",status = '" + status + '\'' +
                        "}";
    }
}