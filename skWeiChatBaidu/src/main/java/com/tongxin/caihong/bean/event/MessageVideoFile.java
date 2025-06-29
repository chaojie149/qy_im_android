package com.tongxin.caihong.bean.event;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/10/31 0031.
 */
// 为免出现多个页面接收拍摄消息，不使用eventBus发送，改用setResult,
public class MessageVideoFile implements Serializable {
    public final int timelen;
    public final long length;
    public final String path;

    public MessageVideoFile(int timelen, long length, String path) {
        this.timelen = timelen;
        this.length = length;
        this.path = path;
    }
}
