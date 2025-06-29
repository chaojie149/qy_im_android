package com.tongxin.caihong.bean;

public class MessageListItem {
    private int count;
    private String messageId;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return
                "MessageListItem{" +
                        "count = '" + count + '\'' +
                        ",messageId = '" + messageId + '\'' +
                        "}";
    }
}
