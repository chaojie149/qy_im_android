package com.tongxin.caihong.socket.msg;


import com.alibaba.fastjson.JSONObject;

/**
 * Login Message
 */
public class AuthMessage extends AbstractMessage {

    public int version;
    private String token;
    private String password;
    private String deviceId;
    private String apiKey;
    private String appName;
    private String companyName;
    private String secret;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        object.put("messageHead", this.messageHead);
        object.put("token", this.token);
        object.put("password", this.password);
        object.put("deviceId", this.deviceId);
        object.put("version", this.version);
        object.put("apiKey", this.apiKey);
        object.put("appName", this.appName);
        object.put("companyName", this.companyName);
        object.put("secret", this.secret);
        String msg = object.toString();
        return msg;
    }

}
