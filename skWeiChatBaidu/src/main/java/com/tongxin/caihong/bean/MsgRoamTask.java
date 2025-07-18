package com.tongxin.caihong.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * 群组分页漫游
 */
@DatabaseTable
public class MsgRoamTask {

    @DatabaseField(generatedId = true)
    private int _id;

    @DatabaseField
    private long taskId;

    @DatabaseField
    private String ownerId;// 任务拥有者

    @DatabaseField
    private String userId;// 当前任务属于哪个群组 jid

    @DatabaseField
    private long startSeqNo;// 漫游开始序号

    @DatabaseField
    private long endSeqNo;// 漫游结束序号

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getStartSeqNo() {
        return startSeqNo;
    }

    public void setStartSeqNo(long startSeqNo) {
        this.startSeqNo = startSeqNo;
    }

    public long getEndSeqNo() {
        return endSeqNo;
    }

    public void setEndSeqNo(long endSeqNo) {
        this.endSeqNo = endSeqNo;
    }

    @Override
    public String toString() {
        return "MsgRoamTask{" +
                "_id=" + _id +
                ", taskId=" + taskId +
                ", ownerId='" + ownerId + '\'' +
                ", userId='" + userId + '\'' +
                ", startSeqNo=" + startSeqNo +
                ", endSeqNo=" + endSeqNo +
                '}';
    }
}
