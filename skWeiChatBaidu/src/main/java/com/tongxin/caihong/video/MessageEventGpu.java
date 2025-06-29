package com.tongxin.caihong.video;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/11/8 0008.
 */
// 为免出现多个页面接收拍摄消息，不使用eventBus发送，改用setResult,
public class MessageEventGpu implements Serializable {
    public final String event;

    public MessageEventGpu(String event) {
        this.event = event;
    }
}
