package com.tongxin.caihong.bean;

import java.util.List;

/**
 * 检查Token是否过期的Bean
 */
public class LoginAuto {

    private String accessToken;
    private String httpKey;
    private String messageKey;
    private String payKey;
    private int payPassword; // 是否已经设置了支付密码，
    private String walletId; // 云钱包ID，
    private int realNameCertified; // 是否已经实名认证，
    // 1=游客（用于后台浏览数据）；2=公众号 ；3=机器账号，由系统自动生成；4=客服账号;5=管理员；6=超级管理员；7=财务；
    private List<Integer> role; // 身份，
    private String myInviteCode;
    private Settings settings;
    // 隐私会话进入密码
    private String hideChatPassword;

    public String getPayKey() {
        return payKey;
    }

    public void setPayKey(String payKey) {
        this.payKey = payKey;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getHttpKey() {
        return httpKey;
    }

    public void setHttpKey(String httpKey) {
        this.httpKey = httpKey;
    }

    public int getPayPassword() {
        return payPassword;
    }

    public void setPayPassword(int payPassword) {
        this.payPassword = payPassword;
    }

    public List<Integer> getRole() {
        return role;
    }

    public void setRole(List<Integer> role) {
        this.role = role;
    }

    public String getMyInviteCode() {
        return myInviteCode;
    }

    public void setMyInviteCode(String myInviteCode) {
        this.myInviteCode = myInviteCode;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public String getHideChatPassword() {
        return hideChatPassword;
    }

    public void setHideChatPassword(String hideChatPassword) {
        this.hideChatPassword = hideChatPassword;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public int getRealNameCertified() {
        return realNameCertified;
    }

    public void setRealNameCertified(int realNameCertified) {
        this.realNameCertified = realNameCertified;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    /**
     * User Private Settings
     */
    public static class Settings extends PrivacySetting {

    }

}
