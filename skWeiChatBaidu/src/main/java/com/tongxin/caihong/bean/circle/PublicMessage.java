package com.tongxin.caihong.bean.circle;

import android.text.TextUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.util.Constants;

import java.io.Serializable;
import java.util.List;

/**
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.chat.weichat.bean.circle
 * @作者:王阳
 * @创建时间: 2015年10月15日 下午5:02:33
 * @描述: 普通消息bean
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class PublicMessage implements Serializable {
    /**
     * 消息类型
     */
    public static final int TYPE_TEXT = 1; // 文字类型
    public static final int TYPE_IMG = 2;  // 图片类型
    public static final int TYPE_VOICE = 3;// 语音
    public static final int TYPE_VIDEO = 4;// 视频
    public static final int TYPE_FILE = 5; // 文件
    public static final int TYPE_LINK = 6; // 链接
    public static final int TYPE_SINGLE_IMAGE = 8; // 朋友圈单张大图片
    public static final int TYPE_SINGLE_VIDEO = 9; // 朋友圈单张大图片

    /**
     * 收藏来源
     */
    public static final int OTHER = 0; // 其他
    public static final int CIRCLE = 1; // 朋友圈
    public static final int VIDEO = 2;  // 视界
    public static final int ROOM = 3;// 群组
    public static final int SINGLE = 4;// 单聊

    /**
     * 消息来源
     */
    public static final int SOURCE_SELF = 0;// 0＝自己的
    public static final int SOURCE_FORWARDING = 1;// 1=转载的
    /**
     * 消息标志
     */
    public static final int FLAG_APPLY_JOB = 1;// 1 求职
    public static final int FLAG_RECRUIT = 2;  // 2 招聘
    public static final int FLAG_NORMAL = 3;   // 3 普通
    /**
     * 消息可见范围
     */
    public static final int VISIBLE_NO = 0;    // 0 不可见
    public static final int VISIBLE_FRIEND = 1;// 1 朋友可见
    public static final int VISIBLE_FANS = 2;  // 2 粉丝可见
    public static final int VISIBLE_ALL = 3;   // 3 广场可见（所有人可见）
    private static final long serialVersionUID = -2853687308018351618L;
    @JSONField(name = "msgId")
    private String messageId;// 公共消息Id
    private String userId;   // 发消息的人Id
    @JSONField(name = "nickname")
    private String nickName; // 发消息的人的昵称
    /**
     * 消息标志 {@link #FLAG_APPLY_JOB}{@link #FLAG_RECRUIT}{@link #FLAG_NORMAL}
     */
    private int visible;// 可见范围 0=不可见；1=朋友可见；2=粉丝可见；3=广场
    private Body body;   // 消息的内容
    private Count count;// 次数节点
    private long time;  // 发布的时间
    private String model;    // 手机设备信息
    private Loc loc; // 发布的经纬度
    // 保留旧版经纬度字段以便兼容旧版服务器，
    private double latitude; // 发布的经纬度
    private double longitude;// 发布的经纬度
    private String location; // 发这条消息是在什么位置
    private int isPraise;    // 0没赞过 1赞过
    private int isCollect;  // 0没收藏过 1收藏过
    private List<Comment> comments;// 回复数组
    private List<Praise> praises;  // 赞的列表
    private List<Gift> gifts;          // 收到的礼物列表
    private List<String> lables;

    private int isAllowComment; //生活圈禁止评论

    private int hiding; //隐身模式，

    /**
     * 消息来源 {@link #SOURCE_SELF}{@link #SOURCE_FORWARDING}
     */
    private int source;
    // 转载的附加Text
    private String fowardText;
    private String fowardUserId;
    private String fowardNickname;
    // 我的收藏专属id
    private String emojiId;
    private String fileName;
    private int targetType = 5; //收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊 (当在朋友圈时，没有targetType，此时默认值就是5用于判断)
    private String targetName;
    private String toUserId;
    private String toUserName;
    /**
     * 广告详情 Id
     */
    private String id;

    /**
     * 广告logo
     */
    private String logo;

    /**
     * 标题
     */
    private String name;

    /**
     * 图片
     */
    private String photo;

    /**
     * 跳转连接
     */
    private String link;
    /**
     * 音乐
     */
    private String voice;
    /**
     * 视频
     */
    private String video;
    /**
     * 广告内容
     */
    private String content;
    /**
     * 广告展示类型
     */
    private Integer showType;
    /**
     * 是否已经展示过，
     */
    private boolean showed;

    public boolean isAd() {
        return !TextUtils.isEmpty(getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getShowType() {
        return showType;
    }

    public void setShowType(Integer showType) {
        this.showType = showType;
    }

    public boolean isShowed() {
        return showed;
    }

    /**
     * 统计值
     */
//    private int count;
    public void setShowed(boolean showed) {
        this.showed = showed;
    }

    // 转成收藏用的type,
    public int getCollectionType() {
        int type;
        switch (getType()) {
            case TYPE_TEXT:
                type = CollectionEvery.TYPE_TEXT;
                break;
            case TYPE_IMG:
                type = CollectionEvery.TYPE_IMAGE;
                break;
            case TYPE_FILE:
                type = CollectionEvery.TYPE_FILE;
                break;
            case TYPE_VIDEO:
                type = CollectionEvery.TYPE_VIDEO;
                break;
            case TYPE_VOICE:
                type = CollectionEvery.TYPE_VOICE;
                break;
            case TYPE_LINK:
                type = CollectionEvery.TYPE_LINK;
                break;
            case 8:
                type = CollectionEvery.TYPE_IMAGE_SINGLE;
                break;
            case 9:
                type = CollectionEvery.TYPE_VIDEO_SINGLE;
                break;
            default:
                throw new IllegalStateException("类型<" + getType() + ">不存在，");
        }
        return type;
    }

    public int getCommnet() {
        if (count != null) {
            return count.getComment();
        }
        return 0;
    }

    public void setCommnet(int commentCount) {
        if (count == null) {
            count = new Count();
        }
        count.setComment(commentCount);
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public int getIsAllowComment() {
        return isAllowComment;
    }

    public void setIsAllowComment(int isAllowComment) {
        this.isAllowComment = isAllowComment;
    }

    public int getHiding() {
        return hiding;
    }

    public void setHiding(int hiding) {
        this.hiding = hiding;
    }

    public String getEmojiId() {
        return emojiId;
    }

    public void setEmojiId(String emojiId) {
        this.emojiId = emojiId;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFowardText() {
        return fowardText;
    }

    public void setFowardText(String fowardText) {
        this.fowardText = fowardText;
    }

    public String getFowardUserId() {
        return fowardUserId;
    }

    public void setFowardUserId(String fowardUserId) {
        this.fowardUserId = fowardUserId;
    }

    public String getFowardNickname() {
        return fowardNickname;
    }

    public void setFowardNickname(String fowardNickname) {
        this.fowardNickname = fowardNickname;
    }

    public Count getCount() {
        return count;
    }

    public void setCount(Count count) {
        this.count = count;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getVisible() {
        return visible;
    }

    public void setVisible(int visible) {
        this.visible = visible;
    }

    public int getIsPraise() {
        return isPraise;
    }

    public void setIsPraise(int isPraise) {
        this.isPraise = isPraise;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        if (getHiding() == 1) {
            return Constants.HIDING_NICKNAME;
        }
        if (TextUtils.isEmpty(nickName)) {
            return "(null)"; // 仿ios,
        }
        return nickName;
    }

    public void setNickName(String nickname) {
        this.nickName = nickname;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Praise> getPraises() {
        return praises;
    }

    public void setPraises(List<Praise> praises) {
        this.praises = praises;
    }

    public List<Gift> getGifts() {
        return gifts;
    }

    public void setGifts(List<Gift> gifts) {
        this.gifts = gifts;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    // /////////////////////快捷方法///////////////////////////
    public int getType() {
        if (body != null) {
            return body.getType();
        }
        return TYPE_TEXT;
    }

    public String getFirstImageThumbnail() {
        if (body != null && body.getImages() != null && body.getImages().size() > 0) {
            return body.getImages().get(0).getThumbnailUrl();
        }
        return null;
    }

    public String getFirstImageOriginal() {
        if (body != null && body.getImages() != null && body.getImages().size() > 0) {
            return body.getImages().get(0).getOriginalUrl();
        }
        return null;
    }

    public String getFirstAudio() {
        if (body != null && body.getAudios() != null && body.getAudios().size() > 0) {
            return body.getAudios().get(0).getOriginalUrl();
        }
        return null;
    }

    public String getFirstVideo() {
        if (body != null && body.getVideos() != null && body.getVideos().size() > 0) {
            return body.getVideos().get(0).getOriginalUrl();
        }
        return null;
    }

    public String getFirstFile() {
        if (body != null && body.getFiles() != null && body.getFiles().size() > 0) {
            return body.getFiles().get(0).getOriginalUrl();
        }
        return null;
    }

    // 获取次数的快捷方法
    public int getPlay() {
        if (count != null) {
            return count.getPlay();
        }
        return 0;
    }

    public int getForward() {
        if (count != null) {
            return count.getForward();
        }
        return 0;
    }

    public int getShare() {
        if (count != null) {
            return count.getShare();
        }
        return 0;
    }

    public int getCollect() {
        if (count != null) {
            return count.getCollect();
        }
        return 0;
    }

    public int getPraise() {
        if (count != null)
            return count.getPraise();
        return 0;
    }

    public void setPraise(int praiseCount) {
        if (count == null) {
            count = new Count();
        }
        count.setPraise(praiseCount);
    }

    public int getPraiseCount() {
        if (praises != null)
            return praises.size();
        return 0;
    }

    public int getMoney() {
        if (count != null) {
            return count.getMoney();
        }
        return 0;
    }

    public int getTotal() {
        if (count != null) {
            return count.getTotal();
        }
        return 0;
    }

    public long getFirstVideoSize() {
        if (body != null && body.getVideos() != null && body.getVideos().size() > 0) {
            return body.getVideos().size();
        }
        return 0;
    }

    public int getIsCollect() {
        return isCollect;
    }

    public void setIsCollect(int isCollect) {
        this.isCollect = isCollect;
    }

    public Loc getLoc() {
        if (loc == null) {
            loc = new Loc();
            loc.lat = latitude;
            loc.lng = longitude;
        }
        return loc;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public List<String> getLables() {
        return lables;
    }

    public void setLables(List<String> lables) {
        this.lables = lables;
    }

    public static class Resource implements Serializable {
        private static final long serialVersionUID = 1665607875044805022L;

        @JSONField(name = "oUrl")
        private String originalUrl;

        @JSONField(name = "tUrl")
        private String thumbnailUrl;

        private long size;
        private long length;

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }
    }

    public static class Body implements Serializable {
        private static final long serialVersionUID = -7082197369330985229L;
        private String lable;
        private int type;
        private String title;// 标题
        private String text; // 文字
        private List<Resource> images;
        private List<Resource> audios;
        private List<Resource> videos;
        private List<Resource> files;
        private long time;// 时间
        private String address;// 地址
        private String remark;// 备注
        // 分享
        private String sdkIcon;
        private String sdkTitle;
        private String sdkUrl;


        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<Resource> getImages() {
            return images;
        }

        public void setImages(List<Resource> images) {
            this.images = images;
        }

        public List<Resource> getAudios() {
            return audios;
        }

        public void setAudios(List<Resource> audios) {
            this.audios = audios;
        }

        public List<Resource> getVideos() {
            return videos;
        }

        public void setVideos(List<Resource> videos) {
            this.videos = videos;
        }

        public List<Resource> getFiles() {
            return files;
        }

        public void setFiles(List<Resource> files) {
            this.files = files;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getSdkIcon() {
            return sdkIcon;
        }

        public void setSdkIcon(String sdkIcon) {
            this.sdkIcon = sdkIcon;
        }

        public String getSdkTitle() {
            return sdkTitle;
        }

        public void setSdkTitle(String sdkTitle) {
            this.sdkTitle = sdkTitle;
        }

        public String getSdkUrl() {
            return sdkUrl;
        }

        public void setSdkUrl(String sdkUrl) {
            this.sdkUrl = sdkUrl;
        }

        public String getLable() {
            return lable;
        }

        public void setLable(String lable) {
            this.lable = lable;
        }
    }

    public static class Count implements Serializable {
        private static final long serialVersionUID = 5424006468612181115L;

        private int play;   // 播放次数
        private int forward;// 转载次数
        private int share;  // 分享次数
        private int collect;// 收藏次数
        private int praise; // 赞次数
        private int comment;// 评论数
        private int money;  // 金钱树
        private int total;  // 上面所有值加起来的值

        public int getPlay() {
            return play;
        }

        public void setPlay(int play) {
            this.play = play;
        }

        public int getForward() {
            return forward;
        }

        public void setForward(int forward) {
            this.forward = forward;
        }

        public int getShare() {
            return share;
        }

        public void setShare(int share) {
            this.share = share;
        }

        public int getCollect() {
            return collect;
        }

        public void setCollect(int collect) {
            this.collect = collect;
        }

        public int getPraise() {
            return praise;
        }

        public void setPraise(int praise) {
            this.praise = praise;
        }

        public int getComment() {
            return comment;
        }

        public void setComment(int comment) {
            this.comment = comment;
        }

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    public static class Loc implements Serializable {
        private double lat;
        private double lng;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }
}