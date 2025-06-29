package com.tongxin.caihong.xmpp.listener;

public interface ChatReceiptListener {

    void onReceiveReceipt(int state, String messageId, long seqNo);
}
