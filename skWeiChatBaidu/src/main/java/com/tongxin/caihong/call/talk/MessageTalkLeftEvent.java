package com.tongxin.caihong.call.talk;

import com.tongxin.caihong.bean.message.ChatMessage;

public class MessageTalkLeftEvent {
    public ChatMessage chatMessage;

    public MessageTalkLeftEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
