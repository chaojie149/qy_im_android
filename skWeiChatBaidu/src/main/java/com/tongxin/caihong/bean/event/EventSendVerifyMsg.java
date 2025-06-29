package com.tongxin.caihong.bean.event;

import java.util.List;

/**
 * 发送群验证消息
 */
public class EventSendVerifyMsg {
    private String createUserId;
    private String groupJid;
    private String reason;
    private List<String> groupManagerUserAccidList;

    public EventSendVerifyMsg(String createUserId, String groupJid, String reason) {
        this.createUserId = createUserId;
        this.groupJid = groupJid;
        this.reason = reason;
    }

    public EventSendVerifyMsg(String createUserId, String groupJid, String reason, List<String> groupManagerUserAccidList) {
        this.createUserId = createUserId;
        this.groupJid = groupJid;
        this.reason = reason;
        this.groupManagerUserAccidList = groupManagerUserAccidList;
    }

    public List<String> getGroupManagerUserAccidList() {
        return groupManagerUserAccidList;
    }

    public void setGroupManagerUserAccidList(List<String> groupManagerUserAccidList) {
        this.groupManagerUserAccidList = groupManagerUserAccidList;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getGroupJid() {
        return groupJid;
    }

    public void setGroupJid(String groupJid) {
        this.groupJid = groupJid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
