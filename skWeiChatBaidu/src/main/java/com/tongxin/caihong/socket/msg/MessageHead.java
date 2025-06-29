package com.tongxin.caihong.socket.msg;

import com.alibaba.fastjson.JSON;

public class MessageHead implements Cloneable {

    /**
     * 发送用户id;
     */
    protected String from;
    /**
     * 目标用户id;
     */
    protected String to;

    /**
     * 聊天类型;(1 单聊 2 群聊 )
     */
    protected byte chatType;

    /**
     * 消息id
     */
    protected String messageId;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public byte getChatType() {
        return chatType;
    }

    public void setChatType(byte chatType) {
        this.chatType = chatType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
