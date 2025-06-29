package com.tongxin.caihong.bean;

public class AppletShare {
    public String gameId;
    public String appName;
    public String appIcon;
    public String title;
    public String imageUrl;
    public String appUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    @Override
    public String toString() {
        return "AppletShare{" +
                "gameId='" + gameId + '\'' +
                ", appName='" + appName + '\'' +
                ", appIcon='" + appIcon + '\'' +
                ", title='" + title + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", appUrl='" + appUrl + '\'' +
                '}';
    }
}
