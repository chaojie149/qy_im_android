package com.tongxin.caihong.bean.message;

public class JXSeqNoModel {
    private String userId;// 好友/群组id
    private long maxSeqNo;// 当前用户本地消息最大序号
    private String loseSeqNos;// 已丢失的消息序号队列，","号拼接
    private String updateSeqNos;// 正在调用接口下载的消息序号队列，","号拼接，用于接口回调之后准确更新loseSeqNos
    private boolean isGetLostSeqNo;// 是否正在补消息
    private boolean isGroup;// 是否为群组

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getMaxSeqNo() {
        return maxSeqNo;
    }

    public void setMaxSeqNo(long maxSeqNo) {
        this.maxSeqNo = maxSeqNo;
    }

    public String getLoseSeqNos() {
        return loseSeqNos;
    }

    public void setLoseSeqNos(String loseSeqNos) {
        this.loseSeqNos = loseSeqNos;
    }

    public String getUpdateSeqNos() {
        return updateSeqNos;
    }

    public void setUpdateSeqNos(String updateSeqNos) {
        this.updateSeqNos = updateSeqNos;
    }

    public boolean isGetLostSeqNo() {
        return isGetLostSeqNo;
    }

    public void setGetLostSeqNo(boolean getLostSeqNo) {
        isGetLostSeqNo = getLostSeqNo;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }
}
