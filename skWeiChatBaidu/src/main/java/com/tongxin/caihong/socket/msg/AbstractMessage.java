package com.tongxin.caihong.socket.msg;


/**
 * @author lidaye
 */

public abstract class AbstractMessage {

    public MessageHead messageHead;

    public MessageHead getMessageHead() {
        return messageHead;
    }

    public void setMessageHead(MessageHead messageHead) {
        this.messageHead = messageHead;
    }

    public abstract String toString();

}
