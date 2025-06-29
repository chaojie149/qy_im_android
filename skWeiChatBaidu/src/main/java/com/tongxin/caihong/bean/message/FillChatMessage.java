package com.tongxin.caihong.bean.message;

import java.util.List;

/**
 * Created by Administrator on 2017/6/28.
 * 同步聊天记录
 * 处理服务器返回的聊天记录实体
 */

public class FillChatMessage {

    private String userId;
    private List<ChatRecord> resultList;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<ChatRecord> getResultList() {
        return resultList;
    }

    public void setResultList(List<ChatRecord> resultList) {
        this.resultList = resultList;
    }
}
