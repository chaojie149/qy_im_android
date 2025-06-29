package com.tongxin.caihong.xmpp;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.socket.EMConnectionManager;
import com.tongxin.caihong.ui.base.CoreManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ReadManager {
    private static final String TAG = "ReadManager";

    private EMConnectionManager mConnectionManager;
    private List<MucReadItem> messageQueue = new ArrayList<>();
    private SendThread sendThread;
    private String roomJid;
    private boolean stop = false;

    public ReadManager(EMConnectionManager connectionManager) {
        mConnectionManager = connectionManager;

        sendThread = new SendThread();
        sendThread.start();
    }

    public void release() {
        stop = true;

        if (sendThread != null) {
            sendThread.interrupt();
            sendThread = null;
        }
        messageQueue.clear();
    }

    public void addRead(ReadManager.MucReadItem item) {
        if (!TextUtils.equals(roomJid, item.roomJid)) {
            if (!messageQueue.isEmpty()) {
                // 出现其他群的已读说明切换页面了，之前攒的已读都可以发了，
                sendThread.flush();
            }
            roomJid = item.roomJid;
        }
        messageQueue.add(item);
    }

    public static class MucReadItem {
        @NonNull
        public String roomJid;
        @NonNull
        public String fromUserId;
        @NonNull
        public String messageId;

        public MucReadItem(@NonNull String roomJid, @NonNull String fromUserId, @NonNull String messageId) {
            this.roomJid = roomJid;
            this.fromUserId = fromUserId;
            this.messageId = messageId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MucReadItem that = (MucReadItem) o;
            return messageId.equals(that.messageId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId);
        }

        @Override
        public String toString() {
            return "MucReadItem{" +
                    "roomJid='" + roomJid + '\'' +
                    ", fromUserId='" + fromUserId + '\'' +
                    ", messageId='" + messageId + '\'' +
                    '}';
        }
    }

    private class SendThread extends Thread {
        private long flushTime;

        @Override
        public void run() {
            try {
                while (!stop) {
                    // 每秒醒来一次，判断5秒没发回执就发一次，或者消息数量大于100也发一次，
                    if (!messageQueue.isEmpty()) {
                        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flushTime) > 5
                                || messageQueue.size() > 100)
                            flush();
                    }
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                Log.e(TAG, "发回执线程结束", e);
            }
        }

        private void flush() {
            flushTime = System.currentTimeMillis();
            // 消息列表的使用是异步的，为免被马上清空，克隆一份，
            sendReceipt(new ArrayList<>(messageQueue));
            for (MucReadItem item : messageQueue) {
                ChatMessageDao.getInstance().updateMessageRead(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), item.roomJid, item.messageId, true); // 消息设置为已读避免重复发，
            }
            messageQueue.clear();
        }

        private void sendReceipt(List<MucReadItem> messageIdList) {
            mConnectionManager.sendRead(roomJid, messageIdList);
        }
    }

}
