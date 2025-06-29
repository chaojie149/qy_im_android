package com.tongxin.caihong.socket.msg;

/**
 * description: 离线消息 <br>
 * date: 2020/5/13 0013  <br>
 * author: lidaye <br>
 * version: 1.0 <br>
 */

public class OffChatMessage extends ChatMessage {

    private boolean offend;

    public boolean isOffend() {
        return offend;
    }

    public void setOffend(boolean offend) {
        this.offend = offend;
    }
}
