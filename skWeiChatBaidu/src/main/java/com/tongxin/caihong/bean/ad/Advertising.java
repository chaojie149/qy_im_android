package com.tongxin.caihong.bean.ad;

import com.tongxin.caihong.bean.circle.PublicMessage;

/**
 * 字段从服务器复制，
 */
public class Advertising {
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
     * 统计值
     */
    private int count;

    /**
     * 是否已经展示过，
     */
    private boolean showed;

    public Advertising() {
    }
    public Advertising(PublicMessage message) {
        this();
        setShowed(message.isShowed());
        setContent(message.getContent());
        setCount(0);
        setId(message.getId());
        setLink(message.getLink());
        setLogo(message.getLogo());
        setPhoto(message.getPhoto());
        setShowType(message.getShowType());
        setName(message.getName());
        setVideo(message.getVideo());
        setVoice(message.getVoice());
    }

    @Override
    public String toString() {
        return "Advertising{" +
                "id='" + id + '\'' +
                ", logo='" + logo + '\'' +
                ", name='" + name + '\'' +
                ", photo='" + photo + '\'' +
                ", link='" + link + '\'' +
                ", voice='" + voice + '\'' +
                ", video='" + video + '\'' +
                ", content='" + content + '\'' +
                ", showType=" + showType +
                ", count=" + count +
                ", showed=" + showed +
                '}';
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isShowed() {
        return showed;
    }

    public void setShowed(boolean showed) {
        this.showed = showed;
    }
}
