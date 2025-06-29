package com.tongxin.caihong.socket.msg;


import com.alibaba.fastjson.JSON;

public class AuthRespMessage extends AbstractMessage {

    /**
     * 登陆结果  1 登陆 成功     0 登陆失败
     */
    private byte status;

    /**
     * 提示信息
     */
    private String arg;

    /**
     * token
     */
    private String token;

    /**
     * 在线设备列表
     */
    private String resources;

    private short version;

    private String mcode;

    private String apiKey;

    private String appName;

    private String companyName;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public String getMcode() {
        return mcode;
    }

    public void setMcode(String mcode) {
        this.mcode = mcode;
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

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
