package com.tongxin.caihong.ui.live.bean;

/**
 * Created by Administrator on 2017/7/19 0019.
 */

// 直播间成员
public class Member {
    /**
     * createTime : 1501043103
     * id : 5978199f7760c91b3ec7cd91
     * nickName : MC子龙
     * number : 0
     * online : 1
     * roomId : 5977ffdc7760c92961655f34
     * state : 0
     * type : 3
     * userId : 10009312
     */
    private int createTime;
    private String id;
    private String nickName;
    private int number;
    private int online;
    private String roomId;
    private int state;
    private int type;
    private int userId;

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
