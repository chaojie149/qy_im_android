package com.tongxin.caihong.bean;

public class AllStationsNotice {

    /**
     * slideshow : 1
     * id : 5ff6fa50656d5b60223f4aa8
     * type : 1
     * content : CNN官方报道截图
     * <p>
     * 新变种新冠病毒现在正在全英范围内“蔓延”，在12月16日至12月30日的短短两周内，英国的患病率增加了70%。
     * <p>
     * 据悉，英国自2020年12月29日单日确诊数首次突破5万人后，确诊人数连日走高，连续7天新增病例超过5万例。本周二（1月5日），英国单日确诊数首次突破6万人大关，达60916例。新增死亡人数达830人，是前一日的2倍多。
     * <p>
     * 尽管英国单日死亡人数仍未超过第一波疫情期间的峰值，即2020年4月21日的1224例，但惠蒂表示，因新冠住院的人数并未减少，未来将会有更多人死于这种疾病。如果人们不遵守封锁令呆在家里，那么感染风险会“非常高”。
     * status : 1
     */
    private String picturn;
    private String slideshow;
    private String id;
    private int type;
    private String content;
    //  0.不展示 1.永久展示 2.只展示一次
    private int status;

    public String getPicturn() {
        return picturn;
    }

    public void setPicturn(String picturn) {
        this.picturn = picturn;
    }

    public String getSlideshow() {
        return slideshow;
    }

    public void setSlideshow(String slideshow) {
        this.slideshow = slideshow;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
