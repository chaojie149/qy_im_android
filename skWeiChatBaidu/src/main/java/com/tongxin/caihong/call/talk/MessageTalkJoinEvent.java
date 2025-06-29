package com.tongxin.caihong.call.talk;

import com.tongxin.caihong.bean.message.ChatMessage;

public class MessageTalkJoinEvent {
    public ChatMessage chatMessage;

    public MessageTalkJoinEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
