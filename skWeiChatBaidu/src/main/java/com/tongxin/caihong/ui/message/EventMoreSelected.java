package com.tongxin.caihong.ui.message;

/**
 * Created by Administrator on 2018/4/24 0024.
 */

public class EventMoreSelected {
    private final String toUserId;
    private boolean isSingleOrMerge;
    private boolean isGroupMsg;
    private boolean isDevice;

    public EventMoreSelected(String toUserId, boolean isSingleOrMerge, boolean isGroupMsg) {
        this(toUserId, isSingleOrMerge, isGroupMsg, false);
    }

    public EventMoreSelected(String toUserId, boolean isSingleOrMerge, boolean isGroupMsg, boolean isDevice) {
        this.toUserId = toUserId;
        this.isSingleOrMerge = isSingleOrMerge;
        this.isGroupMsg = isGroupMsg;
        this.isDevice = isDevice;
    }

    public boolean isDevice() {
        return isDevice;
    }

    public String getToUserId() {
        return toUserId;
    }

    public boolean isSingleOrMerge() {
        return isSingleOrMerge;
    }

    public boolean isGroupMsg() {
        return isGroupMsg;
    }
}
