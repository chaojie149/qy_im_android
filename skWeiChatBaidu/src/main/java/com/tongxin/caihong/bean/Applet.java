package com.tongxin.caihong.bean;

public class Applet {
    private String gameId;
    private String appName;
    private String appUrl;
    private int isAuthShare;
    private String accountId;
    private int isAuthPay;
    private String appIcon;
    private int modifyTime;
    private int createTime;
    private int isAuthLogin;
    private String appIntroduction;
    private int appType;
    private String appId;
    private int isGroupHelper;
    private String appSecret;
    private String icon;
    private String name;
    private String url;
    private int status;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public int getIsAuthShare() {
        return isAuthShare;
    }

    public void setIsAuthShare(int isAuthShare) {
        this.isAuthShare = isAuthShare;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public int getIsAuthPay() {
        return isAuthPay;
    }

    public void setIsAuthPay(int isAuthPay) {
        this.isAuthPay = isAuthPay;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String mIcon) {
        icon = mIcon;
    }

    public String getName() {
        return name;
    }

    public void setName(String mName) {
        name = mName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String mUrl) {
        url = mUrl;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    public int getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(int modifyTime) {
        this.modifyTime = modifyTime;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public int getIsAuthLogin() {
        return isAuthLogin;
    }

    public void setIsAuthLogin(int isAuthLogin) {
        this.isAuthLogin = isAuthLogin;
    }

    public String getAppIntroduction() {
        return appIntroduction;
    }

    public void setAppIntroduction(String appIntroduction) {
        this.appIntroduction = appIntroduction;
    }

    public int getAppType() {
        return appType;
    }

    public void setAppType(int appType) {
        this.appType = appType;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getIsGroupHelper() {
        return isGroupHelper;
    }

    public void setIsGroupHelper(int isGroupHelper) {
        this.isGroupHelper = isGroupHelper;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return
                "Applet{" +
                        "gameId = '" + gameId + '\'' +
                        ",appName = '" + appName + '\'' +
                        ",appUrl = '" + appUrl + '\'' +
                        ",isAuthShare = '" + isAuthShare + '\'' +
                        ",accountId = '" + accountId + '\'' +
                        ",isAuthPay = '" + isAuthPay + '\'' +
                        ",appIcon = '" + appIcon + '\'' +
                        ",modifyTime = '" + modifyTime + '\'' +
                        ",createTime = '" + createTime + '\'' +
                        ",isAuthLogin = '" + isAuthLogin + '\'' +
                        ",appIntroduction = '" + appIntroduction + '\'' +
                        ",appType = '" + appType + '\'' +
                        ",appId = '" + appId + '\'' +
                        ",isGroupHelper = '" + isGroupHelper + '\'' +
                        ",appSecret = '" + appSecret + '\'' +
                        ",status = '" + status + '\'' +
                        "}";
    }
}
