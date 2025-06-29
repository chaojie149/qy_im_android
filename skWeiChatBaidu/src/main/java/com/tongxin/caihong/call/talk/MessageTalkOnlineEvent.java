package com.tongxin.caihong.call.talk;

import com.tongxin.caihong.bean.message.ChatMessage;

public class MessageTalkOnlineEvent {
    public ChatMessage chatMessage;

    public MessageTalkOnlineEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}
