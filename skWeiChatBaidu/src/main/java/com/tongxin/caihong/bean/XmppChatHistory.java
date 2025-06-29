package com.tongxin.caihong.bean;

public class XmppChatHistory {
    private String roomJid;
    private int time;
    private long maxSeqNo;

    public String getRoomJid() {
        return roomJid;
    }

    public void setRoomJid(String roomJid) {
        this.roomJid = roomJid;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public long getMaxSeqNo() {
        return maxSeqNo;
    }

    public void setMaxSeqNo(long maxSeqNo) {
        this.maxSeqNo = maxSeqNo;
    }

    @Override
    public String toString() {
        return "XmppChatHistory{" +
                "roomJid='" + roomJid + '\'' +
                ", time=" + time +
                ", maxSeqNo=" + maxSeqNo +
                '}';
    }
}
