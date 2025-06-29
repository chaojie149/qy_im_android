package com.tongxin.caihong.socket.msg;


import com.alibaba.fastjson.JSON;

public class SuccessMessage extends AbstractMessage {

    private static final long serialVersionUID = 1L;

    /**
     * 消息序列号
     */
    private long seqNo;

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
