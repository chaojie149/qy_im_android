package com.tongxin.caihong.call;

import com.tongxin.caihong.bean.message.ChatMessage;

/**
 * Created by Administrator on 2017/6/26 0026.
 */
public class MessageCallingEvent {
    public ChatMessage chatMessage;

    public MessageCallingEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}