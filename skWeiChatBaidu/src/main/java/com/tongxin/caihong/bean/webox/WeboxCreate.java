package com.tongxin.caihong.bean.webox;

public class WeboxCreate {
    private String walletId;
    private String secretKey;

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String toString() {
        return
                "WeboxCreate{" +
                        "walletId = '" + walletId + '\'' +
                        ",secretKey = '" + secretKey + '\'' +
                        "}";
    }
}
