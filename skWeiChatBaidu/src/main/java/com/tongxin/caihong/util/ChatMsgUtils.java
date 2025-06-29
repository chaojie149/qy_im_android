package com.tongxin.caihong.util;

import android.text.TextUtils;

import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;

import java.util.ArrayList;
import java.util.List;

public final class ChatMsgUtils {
    public static void filterBackMsg(List<ChatMessage> chatMessageList) {
        List<ChatMessage> removeList = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessageList) {
            if (chatMessage!=null&&!TextUtils.isEmpty(chatMessage.getContent())&&chatMessage.getContent().contains("撤回了一条") && !chatMessage.getContent().contains("重新编辑")) {
                removeList.add(chatMessage);
            }
        }
        for (ChatMessage chatMessage : removeList) {
            chatMessageList.remove(chatMessage);
        }
    }
}
