package com.tongxin.caihong.course;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.MessageUploadChatRecord;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.mucfile.DownManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * 聊天消息录制类
 */
public class ChatRecordHelper {
    /*############### 状态 ###############*/
    public static final int STATE_UN_RECORD = 0;                // 未录制
    public static final int STATE_RECORDING = 1;                // 录制中
    public static final int STATE_PAUSE_RECORD = 2;         // 暂停录制
    public static final int STATE_WAITING_RECORD = 3;   // 录制完成
    public static final int STATE_RECORD_FAILED = 4;       // 录制失败
    private volatile static ChatRecordHelper instance;
    private int mState; // 当前状态
    private double mStartTime;

    /*############### 单例 ###############*/
    private ChatRecordHelper() {
        mState = STATE_UN_RECORD;
    }

    public static ChatRecordHelper instance() {
        if (instance == null) {
            synchronized (DownManager.class) {
                if (instance == null) {
                    instance = new ChatRecordHelper();
                }
            }
        }
        return instance;
    }

    public int getState() { // 对外暴露
        return mState;
    }

    public void iniText(TextView tvRecord, ChatMessage message) {
        if (mState == STATE_RECORDING && message.getDoubleTimeSend() < mStartTime) {
            tvRecord.setVisibility(View.GONE);
            return;
        }

        int rid;
        if (mState == STATE_UN_RECORD) {
            rid = R.drawable.recording;
            tvRecord.setText(MyApplication.getInstance().getString(R.string.record_course));
        } else {
            rid = R.drawable.stoped;
            tvRecord.setText(MyApplication.getInstance().getString(R.string.save_course));
        }
    }

    public void start(ChatMessage chat) {
        if (mState == STATE_UN_RECORD && chat != null) {
            mStartTime = chat.getDoubleTimeSend();
            mState = STATE_RECORDING;
        }
    }

    public void stop(ChatMessage chatMessage, String toUserId) {
        if (mState == STATE_RECORDING && chatMessage != null) {
            List<ChatMessage> chatMessages = ChatMessageDao.getInstance().getCourseChatMessage(CoreManager.requireSelf(MyApplication.getInstance()).getUserId(), toUserId,
                    mStartTime, chatMessage.getDoubleTimeSend());

            List<ChatMessage> mCourseChatMessage = new ArrayList<>();
            for (int i = 0; i < chatMessages.size(); i++) {
                if (chatMessages.get(i).isMySend()
                        && TextUtils.isEmpty(chatMessages.get(i).getSignature())
                        && chatMessages.get(i).getMessageState() == ChatMessageListener.MESSAGE_SEND_SUCCESS
                        && (chatMessages.get(i).getType() == XmppMessage.TYPE_TEXT
                        || chatMessages.get(i).getType() == XmppMessage.TYPE_VOICE
                        || chatMessages.get(i).getType() == XmppMessage.TYPE_IMAGE
                        || chatMessages.get(i).getType() == XmppMessage.TYPE_VIDEO
                        || chatMessages.get(i).getType() == XmppMessage.TYPE_FILE)) {// 只录制自己发送的文本消息、语音、图片、视频、文件，且非端到端消息
                    mCourseChatMessage.add(chatMessages.get(i));
                }
            }

            Log.e("xuan", "stop: size:" + mCourseChatMessage.size());
            Collections.reverse(mCourseChatMessage);// 将集合倒序
            upLoadChatList(mCourseChatMessage, toUserId);
            mState = STATE_UN_RECORD;
        }
    }

    public void reset() {
        mState = STATE_UN_RECORD;
    }

    /**
     * @param chatMessages
     * @param toUserId
     * @see ChatActivity
     */
    private void upLoadChatList(List<ChatMessage> chatMessages, String toUserId) {
        if (chatMessages != null && chatMessages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (ChatMessage chat : chatMessages) {
                sb.append(chat.getPacketId());
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            String chatIds = sb.toString();

            EventBus.getDefault().post(new MessageUploadChatRecord(chatIds, toUserId));
        }
    }
}
