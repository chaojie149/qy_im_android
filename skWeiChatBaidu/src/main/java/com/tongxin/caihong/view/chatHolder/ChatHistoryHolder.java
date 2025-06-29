package com.tongxin.caihong.view.chatHolder;

import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.ui.message.ChatHistoryActivity;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.StringUtils;

import java.util.List;

class ChatHistoryHolder extends AChatHolderInterface {

    TextView tvHistoryTitle;
    TextView chat_tv1;
    TextView chat_tv2;
    TextView chat_tv3;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_history : R.layout.chat_to_item_history;
    }

    @Override
    public void initView(View view) {
        tvHistoryTitle = (TextView) view.findViewById(R.id.tv_history_title);
        chat_tv1 = (TextView) view.findViewById(R.id.chat_history_tv1);
        chat_tv2 = (TextView) view.findViewById(R.id.chat_history_tv2);
        chat_tv3 = (TextView) view.findViewById(R.id.chat_history_tv3);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        tvHistoryTitle.setText(message.getObjectId());
        String detail = message.getContent();

        chat_tv1.setVisibility(View.GONE);
        chat_tv2.setVisibility(View.GONE);
        chat_tv3.setVisibility(View.GONE);

        try {
            List<String> mStringHistory = JSON.parseArray(detail, String.class);
            for (int i = 0; i < 3; i++) {
                if (i < mStringHistory.size()) {
                    ChatMessage chatMessage = new ChatMessage(mStringHistory.get(i));// 解析json,还原ChatMessage
                    if (i == 0) {
                        fillChatTv(chat_tv1, chatMessage);
                    } else if (i == 1) {
                        fillChatTv(chat_tv2, chatMessage);
                    } else if (i == 2) {
                        fillChatTv(chat_tv3, chatMessage);
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    protected void onRootClick(View v) {
        ChatHistoryActivity.start(mContext, mToUserId, mdata.getPacketId(), isGounp);
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    private void fillChatTv(TextView view, ChatMessage chatMessage) {
        String s = StringUtils.replaceSpecialChar(StringUtils.getMessageContent(chatMessage, true));
        CharSequence charSequence = HtmlUtils.transform200SpanString(s, true);
        view.setVisibility(View.VISIBLE);
        view.setText(charSequence);
    }

}
