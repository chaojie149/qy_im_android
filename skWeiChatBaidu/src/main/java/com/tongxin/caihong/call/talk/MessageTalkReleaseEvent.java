package com.tongxin.caihong.call.talk;

import com.tongxin.caihong.bean.message.ChatMessage;

public class MessageTalkReleaseEvent {
    public ChatMessage chatMessage;

    public MessageTalkReleaseEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
