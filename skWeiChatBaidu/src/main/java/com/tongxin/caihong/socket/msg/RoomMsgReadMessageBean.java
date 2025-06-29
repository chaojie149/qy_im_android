package com.tongxin.caihong.socket.msg;

import com.alibaba.fastjson.JSON;

public class RoomMsgReadMessageBean extends AbstractMessage {
    private String jid;
    private String content;

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
