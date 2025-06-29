package com.tongxin.caihong.call.talk;

import com.tongxin.caihong.bean.message.ChatMessage;

public class MessageTalkRequestEvent {
    public ChatMessage chatMessage;

    public MessageTalkRequestEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
