package com.tongxin.caihong.bean.event;

public class EventShowMarkerChanged {
    private final int showMarker;
    private final String markContent;
    private String userId;

    public EventShowMarkerChanged(String userId, int showMarker, String markContent) {
        this.userId = userId;
        this.showMarker = showMarker;
        this.markContent = markContent;
    }

    public int getShowMarker() {
        return showMarker;
    }

    public String getMarkContent() {
        return markContent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
