package com.tongxin.caihong.bean.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.tongxin.caihong.bean.TitleUrl;
import com.tongxin.caihong.Reporter;

import java.util.ArrayList;
import java.util.List;

public class MucRoom {
    private String id; // 房间Id
    private String jid;// 房间jid
    private String name;   // 房间名字
    private String subject; // 房间主题
    private String desc;    // 房间描述
    private long createTime;// 创建时间
    private int userSize;   // 当前用户数
    private int maxUserSize;// 最大用户数
    private String userId;// 创建者的Id
    @JSONField(name = "nickname")
    private String nickName;// 创建者的昵称
    private int areaId;
    private int cityId;
    private int provinceId;
    private int countryId;
    private double latitude;
    private double longitude;

    private long talkTime;// >0 全体禁言
    // 群主特权
    private double chatRecordTimeOut;// 消息保存天数
    private int showRead;// 是否显示群已读
    private int isLook;// 是否公开群组
    private int isNeedVerify;// 是否开启群主验证
    private int showMember;// 是否显示群成员列表
    private int allowSendCard;// 允许普通群成员私聊
    private int allowInviteFriend;// 允许普通群成员邀请好友
    private int allowUploadFile;// 允许普通群成员上传文件
    private int allowConference;// 允许普通群成员召开会议
    private int allowSpeakCourse;// 允许普通群成员发起讲课
    private int allowOpenLive;// 允许普通群成员发起直播
    private int isAttritionNotice;// 群组减员通知
    private int allowHostUpdate;// 是否允许群主修改群属性
    private int showMarker;// 开启聊天背景水印，
    private int allowModifyCard = 1;// 允许w群成员修改群名片，1允许修改群名片; 0禁止修改群名片

    private int s;// 群组状态 -1 锁定 1 正常
    private int category;// 类别，510 手机联系人群
    private MucRoomMember member;// 代表我在这个房间的状态
    private List<MucRoomMember> members;
    private Notice notice; // 最后一条公告，
    private List<Notice> notices;

    private int isSecretGroup;// 是否为私密群组
    private int encryptType;// 群组的消息加密模式

    private int meetingStatus;
    private String meetingUserName;
    private int liveStatus;
    private String liveUserId;
    private String liveUserName;

    private String roomTitleUrl;
    private byte needPay;  //是否为付费群组，付费才能加入群聊   1:付费群组  0:非付费群组
    private int payForDays;  //付费群组,购买能进入群聊的天数
    private int payForAmount;

    public byte getNeedPay() {
        return needPay;
    }

    public void setNeedPay(byte needPay) {
        this.needPay = needPay;
    }

    public int getPayForDays() {
        return payForDays;
    }

    public void setPayForDays(int payForDays) {
        this.payForDays = payForDays;
    }

    public int getPayForAmount() {
        return payForAmount;
    }

    public void setPayForAmount(int payForAmount) {
        this.payForAmount = payForAmount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getMaxUserSize() {
        return maxUserSize;
    }

    public void setMaxUserSize(int maxUserSize) {
        this.maxUserSize = maxUserSize;
    }

    public int getUserSize() {
        return userSize;
    }

    public void setUserSize(int userSize) {
        this.userSize = userSize;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

    public int getCountryId() {
        return countryId;
    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTalkTime() {
        return talkTime;
    }

    public void setTalkTime(long talkTime) {
        this.talkTime = talkTime;
    }

    public double getChatRecordTimeOut() {
        return chatRecordTimeOut;
    }

    public void setChatRecordTimeOut(double chatRecordTimeOut) {
        this.chatRecordTimeOut = chatRecordTimeOut;
    }

    public int getShowRead() {
        return showRead;
    }

    public void setShowRead(int showRead) {
        this.showRead = showRead;
    }

    public int getIsLook() {
        return isLook;
    }

    public void setIsLook(int isLook) {
        this.isLook = isLook;
    }

    public int getIsNeedVerify() {
        return isNeedVerify;
    }

    public void setIsNeedVerify(int isNeedVerify) {
        this.isNeedVerify = isNeedVerify;
    }

    public int getShowMember() {
        return showMember;
    }

    public void setShowMember(int showMember) {
        this.showMember = showMember;
    }

    public int getAllowSendCard() {
        return allowSendCard;
    }

    public void setAllowSendCard(int allowSendCard) {
        this.allowSendCard = allowSendCard;
    }

    public int getAllowInviteFriend() {
        return allowInviteFriend;
    }

    public void setAllowInviteFriend(int allowInviteFriend) {
        this.allowInviteFriend = allowInviteFriend;
    }

    public int getAllowUploadFile() {
        return allowUploadFile;
    }

    public void setAllowUploadFile(int allowUploadFile) {
        this.allowUploadFile = allowUploadFile;
    }

    public int getAllowConference() {
        return allowConference;
    }

    public void setAllowConference(int allowConference) {
        this.allowConference = allowConference;
    }

    public int getAllowSpeakCourse() {
        return allowSpeakCourse;
    }

    public void setAllowSpeakCourse(int allowSpeakCourse) {
        this.allowSpeakCourse = allowSpeakCourse;
    }

    public int getAllowOpenLive() {
        return allowOpenLive;
    }

    public void setAllowOpenLive(int allowOpenLive) {
        this.allowOpenLive = allowOpenLive;
    }

    public int getIsAttritionNotice() {
        return isAttritionNotice;
    }

    public void setIsAttritionNotice(int isAttritionNotice) {
        this.isAttritionNotice = isAttritionNotice;
    }

    public int getAllowHostUpdate() {
        return allowHostUpdate;
    }

    public void setAllowHostUpdate(int allowHostUpdate) {
        this.allowHostUpdate = allowHostUpdate;
    }

    public int getShowMarker() {
        return showMarker;
    }

    public void setShowMarker(int showMarker) {
        this.showMarker = showMarker;
    }

    public int getS() {
        return s;
    }

    public void setS(int s) {
        this.s = s;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public MucRoomMember getMember() {
        return member;
    }

    public void setMember(MucRoomMember member) {
        this.member = member;
    }

    public List<MucRoomMember> getMembers() {
        return members;
    }

    public void setMembers(List<MucRoomMember> members) {
        this.members = members;
    }

    public Notice getNotice() {
        return notice;
    }

    public void setNotice(Notice notice) {
        this.notice = notice;
    }

    public List<Notice> getNotices() {
        return notices;
    }

    public void setNotices(List<Notice> notices) {
        this.notices = notices;
    }

    public int getIsSecretGroup() {
        return isSecretGroup;
    }

    public void setIsSecretGroup(int isSecretGroup) {
        this.isSecretGroup = isSecretGroup;
    }

    public int getEncryptType() {
        return encryptType;
    }

    public void setEncryptType(int encryptType) {
        this.encryptType = encryptType;
    }

    public int getMeetingStatus() {
        return meetingStatus;
    }

    public void setMeetingStatus(int meetingStatus) {
        this.meetingStatus = meetingStatus;
    }

    public int getLiveStatus() {
        return liveStatus;
    }

    public void setLiveStatus(int liveStatus) {
        this.liveStatus = liveStatus;
    }

    public String getLiveUserId() {
        return liveUserId;
    }

    public void setLiveUserId(String liveUserId) {
        this.liveUserId = liveUserId;
    }

    public String getLiveUserName() {
        return liveUserName;
    }

    public void setLiveUserName(String liveUserName) {
        this.liveUserName = liveUserName;
    }

    public String getRoomTitleUrl() {
        return roomTitleUrl;
    }

    public void setRoomTitleUrl(String roomTitleUrl) {
        this.roomTitleUrl = roomTitleUrl;
    }

    public List<TitleUrl> getTitleUrlList() {
        List<TitleUrl> list = null;
        try {
            list = JSON.parseArray(roomTitleUrl, TitleUrl.class);
        } catch (Exception e) {
            Reporter.unreachable(e);
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public String toString() {
        return createTime + "";
    }

    public Notice getLastNotice() {
        // /room/getRoom没有notices只有notice,
        // 公告删除后notice依然存在，已经反馈，
        if (getNotices() != null && getNotices().size() > 0) {
            return getNotices().get(0);
        } else {
            return getNotice();
        }
    }

    public int getAllowModifyCard() {
        return allowModifyCard;
    }

    public void setAllowModifyCard(int allowModifyCard) {
        this.allowModifyCard = allowModifyCard;
    }

    public String getMeetingUserName() {
        return meetingUserName;
    }

    public void setMeetingUserName(String meetingUserName) {
        this.meetingUserName = meetingUserName;
    }

    public static class Notice {
        private String id;
        private String text;
        private String userId;
        private String nickname;
        private long time;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

}
