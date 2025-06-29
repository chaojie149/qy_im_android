package com.tongxin.caihong.bean.webox;

public class WeboxStatus {
    private String packetType;
    private String bankCode;
    private String walletId;
    private int amount;
    private String completeDateTime;
    private String orderErrorMessage;
    private String serialNumber;
    private String bankCardNumber;
    private String packetCount;
    private String errorMessage;
    private String orderStatus;
    private String bankName;
    private String createDateTime;
    private String paymentType;
    private String merchantId;
    private String requestId;
    private String hmac;
    private String currency;
    private String status;

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public String getPacketType() {
        return packetType;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setCompleteDateTime(String completeDateTime) {
        this.completeDateTime = completeDateTime;
    }

    public String getCompleteDateTime() {
        return completeDateTime;
    }

    public void setOrderErrorMessage(String orderErrorMessage) {
        this.orderErrorMessage = orderErrorMessage;
    }

    public String getOrderErrorMessage() {
        return orderErrorMessage;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setBankCardNumber(String bankCardNumber) {
        this.bankCardNumber = bankCardNumber;
    }

    public String getBankCardNumber() {
        return bankCardNumber;
    }

    public void setPacketCount(String packetCount) {
        this.packetCount = packetCount;
    }

    public String getPacketCount() {
        return packetCount;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setCreateDateTime(String createDateTime) {
        this.createDateTime = createDateTime;
    }

    public String getCreateDateTime() {
        return createDateTime;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getHmac() {
        return hmac;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
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
                "WeboxStatus{" +
                        "packetType = '" + packetType + '\'' +
                        ",bankCode = '" + bankCode + '\'' +
                        ",walletId = '" + walletId + '\'' +
                        ",amount = '" + amount + '\'' +
                        ",completeDateTime = '" + completeDateTime + '\'' +
                        ",orderErrorMessage = '" + orderErrorMessage + '\'' +
                        ",serialNumber = '" + serialNumber + '\'' +
                        ",bankCardNumber = '" + bankCardNumber + '\'' +
                        ",packetCount = '" + packetCount + '\'' +
                        ",errorMessage = '" + errorMessage + '\'' +
                        ",orderStatus = '" + orderStatus + '\'' +
                        ",bankName = '" + bankName + '\'' +
                        ",createDateTime = '" + createDateTime + '\'' +
                        ",paymentType = '" + paymentType + '\'' +
                        ",merchantId = '" + merchantId + '\'' +
                        ",requestId = '" + requestId + '\'' +
                        ",hmac = '" + hmac + '\'' +
                        ",currency = '" + currency + '\'' +
                        ",status = '" + status + '\'' +
                        "}";
    }
}
