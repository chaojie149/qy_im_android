package com.tongxin.caihong.bean.webox;

public class WeboxOrder {
    private String id;
    private String token;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return
                "WeboxOrder{" +
                        "requestId = '" + id + '\'' +
                        ",token = '" + token + '\'' +
                        "}";
    }
}
