package com.tongxin.caihong.xmpp;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.audio.NoticeVoicePlayer;
import com.tongxin.caihong.bean.EventSecureNotify;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.HandleSecureChatMessage;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.xmpp.util.ConvertMessage;
import com.tongxin.caihong.xmpp.util.HandleGroupMessage;

import de.greenrobot.event.EventBus;

/**
 * Created by Administrator on 2017/11/24.
 */

public class XMuChatMessageListener {
    // 不明原因可能导致listener出现多个，至少在收消息时确保去重，
    private static final LruCache<String, String> mMsgIDMap = new LruCache<>(200);
    private CoreService mService;
    private String mLoginUserId;

    public XMuChatMessageListener(CoreService coreService) {
        mService = coreService;
        mLoginUserId = CoreManager.requireSelf(mService).getUserId();
    }

    public void onReceMessage(com.tongxin.caihong.socket.msg.ChatMessage message, ChatMessage chatMessage, boolean isDelayEndMsg) {
        String roomJid;
        if (!TextUtils.isEmpty(message.getMessageHead().getTo())) {
            roomJid = message.getMessageHead().getTo();
        } else {
            roomJid = chatMessage.getToUserId();
        }
        saveGroupMessage(chatMessage, roomJid, isDelayEndMsg);
    }

    /**
     * 保存接收到的聊天信息(群聊)
     */
    private void saveGroupMessage(ChatMessage chatMessage, String roomJid,
                                  boolean isDelayEndMsg) {
        synchronized (mMsgIDMap) {
            String exists = mMsgIDMap.get(chatMessage.getPacketId());
            if (TextUtils.equals(exists, mLoginUserId)) {
                return;
            }
            mMsgIDMap.put(chatMessage.getPacketId(), mLoginUserId);
        }

        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, roomJid);
        if (friend != null
                && friend.getJoinSeqNo() > 0
                && friend.getMaxSeqNo() > 0
                && chatMessage.getSeqNo() > 0
                && (friend.getJoinSeqNo() > chatMessage.getSeqNo())) {
            Log.e(SeqNoManager.TAG, friend.getJoinSeqNo() + " > " + chatMessage.getSeqNo() + "，为被踢出群组时发送的消息，不处理");
            return;
        }

        String packetId = chatMessage.getPacketId();

        if (TextUtils.equals(chatMessage.getFromUserId(), mLoginUserId)
                && chatMessage.getType() == XmppMessage.TYPE_READ
                && TextUtils.isEmpty(chatMessage.getFromUserName())) {
            chatMessage.setFromUserName(CoreManager.requireSelf(mService).getNickName());
        }

        if (!chatMessage.validate()) {
            return;
        }

        ChatMessageDao.getInstance().decrypt(true, chatMessage);// 解密
        int type = chatMessage.getType();
        chatMessage.setGroup(true);
        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);

        Log.e("msg", "收到群聊消息" + chatMessage.toString());

        if (chatMessage.isDelayMsg()) {
            if (chatMessage.isExpired()) {// 该条消息为过期消息，存入本地后直接Return ，不通知
                Log.e("msg_muc", "// 该条消息为过期消息，存入本地后直接Return ，不通知");
                chatMessage.setIsExpired(1);
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, roomJid, chatMessage);
                return;
            }
        }

        /*
        端到端相关消息
         */
        if (type == XmppMessage.TYPE_SECURE_SEND_KEY
                || type == XmppMessage.TYPE_SECURE_NOTIFY_REFRESH_KEY) {
            if (type == XmppMessage.TYPE_SECURE_SEND_KEY) {
                // 群聊收到805消息，群成员发送了chatKey给请求者，更新界面
                if (TextUtils.equals(chatMessage.getFromUserId(), mLoginUserId)) {
                    ChatMessageDao.getInstance().updateChatMessageFileSize(mLoginUserId, chatMessage.getToUserId(), chatMessage.getContent(), 2);
                } else {
                    ChatMessageDao.getInstance().updateChatMessageFileSize(mLoginUserId, chatMessage.getToUserId(), chatMessage.getContent(), 3);
                }
                EventBus.getDefault().post(new EventSecureNotify(EventSecureNotify.MULTI_SNED_KEY_MSG, chatMessage));
            } else {
                HandleSecureChatMessage.distributionChatMessage(chatMessage);
            }
            return;
        }

        boolean isShieldGroupMsg = PreferenceUtils.getBoolean(MyApplication.getContext(), Constants.SHIELD_GROUP_MSG + roomJid + mLoginUserId, false);
        if (isShieldGroupMsg || (friend != null && friend.getGroupStatus() == 4)) {
            // 已屏蔽消息不能直接return，需要校准本地序号之后在return
            if (chatMessage.getSeqNo() > 0) {
                Log.e(SeqNoManager.TAG, "消息序号大于0，准备检验序号");
                SeqNoManager.getInstance().checkSeqNo(roomJid, chatMessage.getSeqNo(), chatMessage.isDelayMsg());

                if (isDelayEndMsg) {
                    Log.e(SeqNoManager.TAG, "离线消息已收完，检查loseSeqNos");
                    SeqNoManager.getInstance().checkLoseSeqNos();
                }
            }
            if (type == XmppMessage.TYPE_SEND_MANAGER
                    || (type >= XmppMessage.TYPE_CHANGE_SHOW_READ && type <= XmppMessage.TYPE_GROUP_TRANSFER)
                    || type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME
                    || type == XmppMessage.TYPE_ALLOW_OPEN_LIVE
                    || type == XmppMessage.TYPE_ROOM_MODIFY_CARD) {
                HandleGroupMessage.handleMessage(mLoginUserId, chatMessage, friend, true);
            }
            return;
        }

        if (type == XmppMessage.TYPE_TEXT
                && !TextUtils.isEmpty(chatMessage.getObjectId())) {// 判断为@消息
            friend = FriendDao.getInstance().getFriend(mLoginUserId, roomJid);
            if (friend != null) {
                if (friend.getIsAtMe() == 0
                        && !TextUtils.equals(MyApplication.IsRingId, roomJid)) {// 本地无@通知 && 收到该条消息时不处于当前群组的聊天界面
                    if (chatMessage.getObjectId().equals(roomJid)) {// @全体成员
                        FriendDao.getInstance().updateAtMeStatus(roomJid, 2);
                        PreferenceUtils.putString(MyApplication.getContext(), "messageId" + roomJid, chatMessage.getPacketId());
                    } else if (chatMessage.getObjectId().contains(mLoginUserId)) {// @我
                        FriendDao.getInstance().updateAtMeStatus(roomJid, 1);
                        PreferenceUtils.putString(MyApplication.getContext(), "messageId" + roomJid, chatMessage.getPacketId());
                    }
                }
            }
        }

        /*
        已读消息
         */
        if (type == XmppMessage.TYPE_READ) {
            packetId = chatMessage.getContent();
            ChatMessage chat = ChatMessageDao.getInstance().findMsgById(mLoginUserId, roomJid, packetId);
            if (chat != null) {
                String fromUserId = chatMessage.getFromUserId();
                boolean repeat = ChatMessageDao.getInstance().checkRepeatRead(mLoginUserId, roomJid, fromUserId, packetId);
                if (!repeat) {
                    int count = chat.getReadPersons();// 查看人数+1
                    chat.setReadPersons(count + 1);
                    // 覆盖最后时间
                    chat.setReadTime(chatMessage.getTimeSend());
                    // 更新消息数据
                    ChatMessageDao.getInstance().updateMessageRead(mLoginUserId, roomJid, chat);
                    // 保存新消息
                    ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, roomJid, chatMessage);
                    // 通知刷新
                    MsgBroadcast.broadcastMsgReadUpdate(MyApplication.getInstance(), packetId);
                }
            }
            return;
        }

        if (chatMessage.getSeqNo() > 0) {
            Log.e(SeqNoManager.TAG, "消息序号大于0，准备检验序号");
            SeqNoManager.getInstance().checkSeqNo(roomJid, chatMessage.getSeqNo(), chatMessage.isDelayMsg());

            if (isDelayEndMsg) {
                Log.e(SeqNoManager.TAG, "离线消息已收完，检查loseSeqNos");
                SeqNoManager.getInstance().checkLoseSeqNos();
            }
        }

        /*
        撤回消息
         */
        if (type == XmppMessage.TYPE_BACK) {
            // 本地数据库处理
            packetId = chatMessage.getContent();
            if (chatMessage.getFromUserId().equals(mLoginUserId)) {// 自己发的不用处理
                ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, roomJid, packetId, MyApplication.getContext().getString(R.string.you));
            } else {
                ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, roomJid, packetId, chatMessage.getFromUserName(), chatMessage.getFromUserId());
            }

            Intent intent = new Intent();
            intent.putExtra("packetId", packetId);
            intent.setAction(OtherBroadcast.MSG_BACK);
            mService.sendBroadcast(intent);

            // 更新UI界面
            ChatMessage message = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, roomJid);
            if (message != null) {
                message.setType(XmppMessage.TYPE_TEXT);
                if (message.getPacketId().equals(packetId)) {
                    // 要撤回的消息正是朋友表的最后一条消息
                    if (chatMessage.getFromUserId().equals(mLoginUserId)) {// 自己发的不用处理
                        FriendDao.getInstance().updateFriendContent(mLoginUserId, roomJid,
                                MyApplication.getContext().getString(R.string.you) + " " + MyApplication.getInstance().getString(R.string.other_with_draw), message);
                    } else {
                        FriendDao.getInstance().updateFriendContent(mLoginUserId, roomJid,
                                chatMessage.getFromUserName() + " " + MyApplication.getInstance().getString(R.string.other_with_draw), message);

                    }
                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
                }
            }
            return;
        }

        /*
        领取红包消息
         */
        if (type == XmppMessage.TYPE_83) {
            String fromUserId = chatMessage.getFromUserId();// 红包领取方
            String toUserId = chatMessage.getToUserId();// 红包发送方

            if (!TextUtils.equals(fromUserId, mLoginUserId) && !TextUtils.equals(toUserId, mLoginUserId)) {// xx 领取了xx的红包
                // 与我无关的红包领取消息就不显示了吧，直接return
                return;
            }

            // 针对红包领取的提示消息 需要做点击事件处理，将红包的type与id存入其他字段内
            chatMessage.setFileSize(XmppMessage.TYPE_83);
            chatMessage.setFilePath(chatMessage.getContent());

            chatMessage.setType(XmppMessage.TYPE_TIP);
            chatMessage.setContent(ConvertMessage.convertPayMessage(type, mLoginUserId, chatMessage));
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
            }
            return;
        }

        /*
        群组相关消息协议 930.931.933在单聊处理了
         */
        if ((type >= XmppMessage.TYPE_MUCFILE_ADD && type <= XmppMessage.TYPE_MUCFILE_DOWN)
                || (type >= XmppMessage.TYPE_CHANGE_NICK_NAME && type <= XmppMessage.NEW_MEMBER)
                || type == XmppMessage.TYPE_SEND_MANAGER
                || (type >= XmppMessage.TYPE_CHANGE_SHOW_READ && type <= XmppMessage.TYPE_GROUP_TRANSFER)
                || type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME
                || type == XmppMessage.TYPE_EDIT_GROUP_NOTICE
                || (type >= XmppMessage.TYPE_ALLOW_OPEN_LIVE && type <= XmppMessage.TYPE_DELETE_NOTICE)) {
            if (TextUtils.isEmpty(chatMessage.getObjectId())) {
                Log.e("msg_muc", "Return 4");
                return;
            }
            if (ChatMessageDao.getInstance().hasSameMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage.getPacketId())) {// 本地已经保存了这条消息，不处理
                Log.e("msg_muc", "Return 5");
                return;
            }
            friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getObjectId());
            if (friend != null || type <= XmppMessage.NEW_MEMBER) {
                HandleGroupMessage.handleMessage(mLoginUserId, chatMessage, friend, false);
            } else {
                Log.e("msg_muc", "Return 6");
            }
            return;
        }

        // 风险提示，换下type即可
        if (type == XmppMessage.TYPE_SERVER_TIP) {
            chatMessage.setType(XmppMessage.TYPE_TIP);
        }

        if (chatMessage.getFromUserId().equals(mLoginUserId) &&
                (chatMessage.getType() == XmppMessage.TYPE_IMAGE
                        || chatMessage.getType() == XmppMessage.TYPE_VIDEO
                        || chatMessage.getType() == XmppMessage.TYPE_FILE)) {
            Log.e("msg_muc", "多点登录，需要显示上传进度的消息");
            chatMessage.setUpload(true);
            chatMessage.setUploadSchedule(100);
        }

        friend = FriendDao.getInstance().getFriend(mLoginUserId, roomJid);
        if (friend != null) {
            if (friend.getJoinSeqNo() > 0
                    && friend.getMaxSeqNo() > 0
                    && chatMessage.getSeqNo() > 0
                    && (friend.getJoinSeqNo() > chatMessage.getSeqNo())) {
                Log.e("msg_muc", "friend.getJoinSeqNo() > chatMessage.getSeqNo()，为被踢出群组的消息，不存");
                return;
            }
            if (friend.getGroupStatus() != 0) {
                // 可能存在离线情况下将你邀请入群>发消息>将你踢出群组>发消息的情况，因为收到907有单独获取离线消息，所以此时不保存不应该显示的消息
                Long timeSend = XChatMessageListener.exitGroupTimeMap.get(friend.getUserId());// 取出被踢出群组消息的timeSend
                if (timeSend != null && timeSend > 0 && chatMessage.getTimeSend() > timeSend) {
                    // 接收到被踢出群组之后的群离线消息，不处理
                    return;
                }
            }
        }
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, roomJid, chatMessage)) {
            SeqNoManager.getInstance().updateTime(chatMessage.isDelayMsg());

            if (friend != null) {
                if (friend.getOfflineNoPushMsg() == 0
                        && !TextUtils.equals(chatMessage.getFromUserId(), mLoginUserId)) {
                    // 需要考虑多点登录的情况，如多点登录下收到自己的消息也不通知
                    // 本地通知
                    if (friend.getHideChatSwitch() == 1) {
                        // 私密模式
                        boolean privateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + mLoginUserId, true);
                        if (!privateMode) {
                            // 关闭了私密模式
                            mService.notificationMessage(chatMessage, true);
                        }
                    } else {
                        mService.notificationMessage(chatMessage, true);
                    }
                    if (!roomJid.equals(MyApplication.IsRingId)
                            && !chatMessage.getFromUserId().equals(mLoginUserId)) {
                        // 收到该消息时不处于与发送方的聊天界面 && 不是自己发送的消息
                        // 铃声通知
                        if (friend.getHideChatSwitch() == 1) {
                            // 私密模式
                            boolean privateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + mLoginUserId, true);
                            if (!privateMode) {
                                // 关闭了私密模式
                                NoticeVoicePlayer.getInstance().start();
                            }
                        } else {
                            NoticeVoicePlayer.getInstance().start();
                        }
                    }
                } else {
                    Log.e("msg_muc", "已针对该群组开启了消息免打扰 || 其他端发过来的消息，不通知");
                }
            } else {
                // friend == null 为直播间消息，直接跳过
                Log.e("msg_muc", "friend == null 为直播间消息，直接跳过");
            }
            ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, roomJid, chatMessage, true);
        }
    }
}
