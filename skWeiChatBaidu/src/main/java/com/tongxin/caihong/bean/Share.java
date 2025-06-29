package com.tongxin.caihong.bean;

import com.alibaba.fastjson.JSON;

public class Share {
    // 来源，0.生活圈 1.视界
    private int source;
    // 内容id
    private String id;
    // 分享id，用于付费接口
    private String shareId;
    // 是否需要付费，0.不需要 1.需要
    private int isNeedPay;
    // 发布者名字，当text为空时，显示来自xx
    private String publisherName;
    // 分享的文本内容，生活圈与视界均支持
    private String text;
    // 分享的类型，仅限生活圈，0.纯文本 1.图文 2.语音 3.视频 4.文件 5.生活圈里面分享
    private int type;
    // 生活圈图文首张图片或视频首帧，仅限生活圈图文、视频与视界
    private String image;
    // 语音或视频时长，仅限生活圈语音、视频与视界
    private long timeLen;
    // 文件名称，仅限生活圈文件
    private String fileName;
    // 文件类型，值与文件消息内定义的type一致，仅限生活圈文件)
    private long fileType;
    // 是否来自收藏分享 0 否 1 是
    private int collect;
    // 短视频url
    private String videoUrl;

    /**
     * share转json
     *
     * @param share
     * @return
     */
    public static String share2Json(Share share) {
        return JSON.toJSONString(share);
    }

    /**
     * json转share
     *
     * @param content
     * @return
     */
    public static Share json2Share(String content) {
        return JSON.parseObject(content, Share.class);
    }

    public int getCollect() {
        return collect;
    }

    public void setCollect(int collect) {
        this.collect = collect;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public int getIsNeedPay() {
        return isNeedPay;
    }

    public void setIsNeedPay(int isNeedPay) {
        this.isNeedPay = isNeedPay;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(long timeLen) {
        this.timeLen = timeLen;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }
}

