package com.tongxin.caihong.xmpp.util;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.event.EventNewNotice;
import com.tongxin.caihong.bean.event.EventRemoveNotice;
import com.tongxin.caihong.bean.event.EventRoomBlackChange;
import com.tongxin.caihong.bean.event.EventShowMarkerChanged;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.ui.message.multi.EventGroupStatus;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.xmpp.CoreService;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.SeqNoManager;
import com.tongxin.caihong.xmpp.XChatMessageListener;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * @author zq
 * 处理群聊消息
 * 群聊消息有点特殊，因为部分群组相关的消息服务器会以单聊的形式通知给被指定方，所以需要特殊处理
 */
public class HandleGroupMessage {
    /**
     * 离线被踢出在被邀请入群时，上线之后发现处理逻辑先走了904的退群mService.exitMucChat(chatMessage.getObjectId())，
     * 但是904的后续操作updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 1)还未处理的时候，居然又回调到了907的的处理内，
     * 且904剩下待处理的地方会等到907的逻辑处理完之后在处理，导致本地群组的groupStatus变成了1，(偶现，概率较大)
     * <p>
     * 此处用一个map记录一下904处理的状态，如处理907时发现904还在处理，此时延时delay ms在处理907
     */
    private static Map<String, Boolean> handleExitMsg = new HashMap<>();

    /**
     * 与我有直接关系的消息
     * 部分消息只做逻辑，不存本地，存本地的操作放在收到群组消息的那里处理
     */
    public static void handleAgainstMessage(String mLoginUserId, String toUserName, String other, ChatMessage chatMessage, CoreService mService) {
        int type = chatMessage.getType();
        String fromUserId = chatMessage.getFromUserId();
        String toUserId = chatMessage.getToUserId();
        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getObjectId());

        chatMessage.setGroup(false);
        if (type == XmppMessage.NEW_MEMBER) {
            // 加入群组 || 我被邀请入群组
            if (!TextUtils.equals(mLoginUserId, toUserId)) {
                return;
            }
            long delay = 1;
            if (handleExitMsg.get(chatMessage.getObjectId()) != null
                    && handleExitMsg.get(chatMessage.getObjectId())) {
                delay = 120;
                Log.e(SeqNoManager.TAG, "收到单聊907加群消息，但检测到此时正在处理904退群消息，延时" + delay + "ms在处理");
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 此时friend不能取外面的，要拿最新的
                final Friend joinFriend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getObjectId());
                if (joinFriend != null && joinFriend.getGroupStatus() == 1) {
                    // 之前被踢出群组
                    FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, joinFriend.getUserId(), 0);
                    ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, joinFriend.getUserId());
                    Intent intent = new Intent(Constants.CHAT_HISTORY_EMPTY);
                    intent.putExtra("id", joinFriend.getUserId());
                    MyApplication.getContext().sendBroadcast(intent);
                }

                // todo 待优化
                // 将当前群组部分属性存入共享参数内
                int isSecretGroup = 0;
                String chatKeyGroup = "";
                long joinSeqNo = 0;
                try {
                    // 群已读、公开、群验证、群成员列表可见、允许普通成员私聊
                    JSONObject jsonObject = JSONObject.parseObject(other);
                    int showRead = jsonObject.getInteger("showRead");
                    int allowSecretlyChat = jsonObject.getInteger("allowSendCard");
                    isSecretGroup = jsonObject.getInteger("isSecretGroup");
                    joinSeqNo = jsonObject.getLong("joinSeqNo");
                    if (joinFriend != null) {
                        Log.e(SeqNoManager.TAG, "收到单聊907消息，被邀请入群，更新本地maxSeqNo为joinSeqNo+1：" + (joinSeqNo + 1));
                        SeqNoManager.getInstance().updateSeqNo(mLoginUserId, friend.getUserId(), joinSeqNo + 1);
                        // 因为群聊也发了一条907，此处需要+1
                        FriendDao.getInstance().updateFriendJoinSeqNo(mLoginUserId, friend.getUserId(), joinSeqNo + 1);
                    }
                    if (!MyApplication.IS_SUPPORT_SECURE_CHAT) {
                        // SecureFlagGroup 非端到端，兼容之前在端到端版本注册过的账号，过滤掉私密群组
                        if (isSecretGroup == 1) {
                            return;
                        }
                    }
                    chatKeyGroup = jsonObject.getString("chatKeyGroup");
                    MyApplication.getInstance().saveGroupPartStatus(chatMessage.getObjectId(), showRead, allowSecretlyChat,
                            1, 1, 0, 1);
                } catch (Exception e) {
                    Log.e("msg", "解析时抛出异常");
                }

                String content = ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage);

                if (FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getObjectId()) == null) {
                    Friend mCreateFriend = new Friend();
                    mCreateFriend.setOwnerId(mLoginUserId);
                    mCreateFriend.setUserId(chatMessage.getObjectId());
                    mCreateFriend.setNickName(chatMessage.getContent());
                    mCreateFriend.setDescription("");
                    mCreateFriend.setContent(content);
                    mCreateFriend.setRoomId(chatMessage.getFilePath());
                    mCreateFriend.setRoomFlag(1);
                    mCreateFriend.setRoomRole(3);
                    mCreateFriend.setStatus(Friend.STATUS_FRIEND);
                    mCreateFriend.setGroupStatus(0);
                    mCreateFriend.setTimeCreate(chatMessage.getTimeSend());
                    mCreateFriend.setTimeSend(chatMessage.getTimeSend());
                    mCreateFriend.setIsSecretGroup(isSecretGroup);
                    if (isSecretGroup == 1) {
                        try {
                            String chatKey = new String(RSA.decryptFromBase64(chatKeyGroup, Base64.decode(SecureChatUtil.getRSAPrivateKey(mLoginUserId))));
                            mCreateFriend.setChatKeyGroup(SecureChatUtil.encryptChatKey(chatMessage.getObjectId(), chatKey));
                            Log.e("msg", "设置chatKey成功-->" + chatKey);
                        } catch (Exception e) {
                            Log.e("msg", "设置chatKey失败-->");
                            mCreateFriend.setIsLostChatKeyGroup(mCreateFriend.getUserId(), 1);
                        }
                    }
                    FriendDao.getInstance().createOrUpdateFriend(mCreateFriend);
                }

                // 调用smack加入群组的方法
                // 被邀请加入群组，lastSeconds == 当前时间 - 被邀请时的时间 + 3[容错]
                if (joinSeqNo == 0) {
                    joinSeqNo = 1;
                }
                mService.joinMucChat(chatMessage.getObjectId(), joinSeqNo);
                // 正在面对面建群情况的通知，
                MsgBroadcast.broadcastFaceGroupNotify(MyApplication.getContext(), "join_room");

                // 更新数据库
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(content);
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
                    MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
                }
                if (TextUtils.equals(toUserId, mLoginUserId)) {
                    MucgroupUpdateUtil.broadcastUpdateRole(MyApplication.getContext());
                }
            }, delay);
        } else if (type == XmppMessage.TYPE_DELETE_MEMBER) {
            // 退出群组 || 我被踢出群组
            if (!TextUtils.equals(mLoginUserId, toUserId)) {
                return;
            }
            if (fromUserId.equals(toUserId)) {
                // 自己退出了群组
                mService.exitMucChat(chatMessage.getObjectId());
                // 删除这个房间
                FriendDao.getInstance().deleteFriend(mLoginUserId, chatMessage.getObjectId());
                RoomMemberDao.getInstance().deleteRoomMemberTable(chatMessage.getObjectId());
                // 消息表中删除
                ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, chatMessage.getObjectId());
                // 通知界面更新
                MsgBroadcast.broadcastMsgNumReset(MyApplication.getContext());
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                MucgroupUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
            } else {
                handleExitMsg.put(chatMessage.getObjectId(), true);
                // 被xx踢出了群组
                mService.exitMucChat(chatMessage.getObjectId());
                // 标志被踢出该群组， 更新朋友表
                FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 1);
                // 可能存在离线情况下将你邀请入群>发消息>将你踢出群组>发消息的情况，因为收到907有单独获取离线消息，所以此时不保存不应该显示的消息
                // 本地做一个局部变量保存当前消息timeSend，在XMucChatMessageListener内使用
                XChatMessageListener.exitGroupTimeMap.put(chatMessage.getObjectId(), chatMessage.getTimeSend());
                handleExitMsg.put(chatMessage.getObjectId(), false);
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
                }
                ListenerManager.getInstance().notifyMyBeDelete(chatMessage.getObjectId());// 通知群组聊天界面
            }
            if (TextUtils.equals(toUserId, mLoginUserId)) {
                MucgroupUpdateUtil.broadcastUpdateRole(MyApplication.getContext());
            }
        } else if (type == XmppMessage.TYPE_DELETE_ROOM) {
            // 群组被解散
            if (fromUserId.equals(toUserId)) {
                // 我为群主
                FriendDao.getInstance().deleteFriend(mLoginUserId, chatMessage.getObjectId());
                // 消息表中删除
                ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, chatMessage.getObjectId());
                RoomMemberDao.getInstance().deleteRoomMemberTable(chatMessage.getObjectId());
                // 通知界面更新
                MsgBroadcast.broadcastMsgNumReset(MyApplication.getContext());
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                MucgroupUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
            } else {
                mService.exitMucChat(chatMessage.getObjectId());
                //  标志该群已被解散  更新朋友表
                if (chatMessage.getFileSize() > 0) {
                    // 消息传输方式为加密的群组，解散后直接清除相关数据
                    FriendDao.getInstance().deleteFriend(mLoginUserId, chatMessage.getObjectId());
                    // 消息表中删除
                    ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, chatMessage.getObjectId());
                    RoomMemberDao.getInstance().deleteRoomMemberTable(chatMessage.getObjectId());
                    // 通知界面更新
                    MsgBroadcast.broadcastMsgNumReset(MyApplication.getContext());
                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                    MucgroupUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
                } else {
                    FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 2);
                    chatMessage.setType(XmppMessage.TYPE_TIP);
                    chatMessage.setContent(MyApplication.getContext().getString(R.string.tip_disbanded));
                    if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                        ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
                    }
                }
            }
            ListenerManager.getInstance().notifyDeleteMucRoom(chatMessage.getObjectId());
        } else if (type == XmppMessage.TYPE_GAG) {
            // 我被禁言
            if (!TextUtils.equals(mLoginUserId, toUserId)) {
                return;
            }
            long time = Long.parseLong(chatMessage.getContent());
            // 被禁言了|| 取消禁言 更新RoomTalkTime字段
            FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, friend.getUserId(), (int) time);
            ListenerManager.getInstance().notifyMyVoiceBanned(friend.getUserId(), (int) time);
        } else if (type == XmppMessage.TYPE_SEND_MANAGER) {
            String content = chatMessage.getContent();
            int role;
            if (content.equals("1")) {
                role = 2;
            } else {
                role = 3;
            }
            if (friend != null) {
                RoomMemberDao.getInstance().updateRoomMemberRole(friend.getRoomId(), toUserId, role);
            }
            // todo 群聊内也做了同样的处理，此处就不处理了
/*
            Intent intent = new Intent();
            intent.putExtra("roomId", friend.getUserId());
            intent.putExtra("toUserId", chatMessage.getToUserId());
            intent.putExtra("isSet", content.equals("1"));
            intent.setAction(OtherBroadcast.REFRESH_MANAGER);
            MyApplication.getContext().sendBroadcast(intent);
*/
        } else if (type == XmppMessage.TYPE_UPDATE_ROLE) {
            // 我被..隐身人/监控人，这条协议只有单聊
            int tipContent;
            int role = RoomMember.ROLE_MEMBER;
            switch (chatMessage.getContent()) {
                case "1": //  1:设置隐身人
                    tipContent = R.string.tip_set_invisible_place_holder;
                    role = RoomMember.ROLE_INVISIBLE;
                    break;
                case "-1": // -1:取消隐身人
                    tipContent = R.string.tip_cancel_invisible_place_holder;
                    break;
                case "2": // 2：设置监控人
                    tipContent = R.string.tip_set_guardian_place_holder;
                    role = RoomMember.ROLE_GUARDIAN;
                    break;
                case "0": // 0：取消监控人
                    tipContent = R.string.tip_cancel_guardian_place_holder;
                    break;
                default:
                    Reporter.unreachable();
                    return;
            }
            if (friend != null) {
                RoomMemberDao.getInstance().updateRoomMemberRole(friend.getRoomId(), toUserId, role);
            }

            chatMessage.setType(XmppMessage.TYPE_TIP);
            chatMessage.setContent(MyApplication.getContext().getString(tipContent, chatMessage.getFromUserName(), toUserName));
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
                MsgBroadcast.broadcastMsgRoleChanged(MyApplication.getContext());
            }
        } else if (type == XmppMessage.TYPE_GROUP_VERIFY) {
            // 916协议分为两种，单聊的这里只能为第二种
            // 第一种为服务端发送，触发条件为群主在群组信息内 开/关 进群验证按钮，群组内每个人都能收到
            // 第二种为邀请、申请加入该群组，由邀请人或加入方发送给群主的消息，只有群主可以收到
            try {
                org.json.JSONObject json = new org.json.JSONObject(chatMessage.getObjectId());
                String isInvite = json.getString("isInvite");
                if (TextUtils.isEmpty(isInvite)) {
                    isInvite = "0";
                }
                if (isInvite.equals("0")) {
                    String id = json.getString("userIds");
                    String[] ids = id.split(",");
                    chatMessage.setContent(MyApplication.getContext().getString(R.string.tip_invite_need_verify_place_holder, chatMessage.getFromUserName(), ids.length));
                } else {
                    chatMessage.setContent(chatMessage.getFromUserName() + MyApplication.getContext().getString(R.string.tip_need_verify_place_holder));
                }
                if (TextUtils.equals(mLoginUserId, chatMessage.getFromUserId())) {
                    // 自己发送的那就不能是去确认了，
                    chatMessage.setContent(MyApplication.getContext().getString(R.string.tip_send_reason_success));
                }
                chatMessage.setType(XmppMessage.TYPE_TIP);
                String roomJid = json.getString("roomJid");
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, roomJid, chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, roomJid, chatMessage, true);
                }
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
        } else if (type == XmppMessage.TYPE_GROUP_ALL_SHAT_UP) {
            PreferenceUtils.putBoolean(MyApplication.getContext(),
                    Constants.GROUP_ALL_SHUP_UP + chatMessage.getObjectId(), !TextUtils.equals(chatMessage.getContent(), "0"));
            MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
        } else if (type == XmppMessage.TYPE_GROUP_TRANSFER) {
            // 群主转让给我
            if (friend != null) {
                FriendDao.getInstance().updateRoomCreateUserId(mLoginUserId, chatMessage.getObjectId(), chatMessage.getToUserId());
                RoomMemberDao.getInstance().updateRoomMemberRole(friend.getRoomId(), chatMessage.getToUserId(), 1);
                MsgBroadcast.broadcastMsgRoleChanged(MyApplication.getContext());
            }
        } else if (type == XmppMessage.TYPE_FACE_GROUP_NOTIFY) {
            //  面对面建群有人加入、退出
            MsgBroadcast.broadcastFaceGroupNotify(MyApplication.getContext(), "notify_list");
        } else if (type == XmppMessage.TYPE_DISABLE_GROUP) {
            // 后台锁定/解锁群组
            if (chatMessage.getContent().equals("-1")) {// 锁定
                FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 3);// 更新本地群组状态
            } else if (chatMessage.getContent().equals("1")) {// 解锁
                FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 0);// 更新本地群组状态
            }
            MyApplication.getContext().sendBroadcast(new Intent(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE));
        } else if (type == XmppMessage.TYPE_ROOM_ADD_BLACK_LIST) {
            if (chatMessage.getContent().equals("1")) {
                // 我被拉黑了
                FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 4);// 更新本地群组状态
                chatMessage.setContent(MyApplication.getContext().getString(R.string.in_black_list));
            } else if (chatMessage.getContent().equals("0")) {
                // 我被解除了拉黑
                chatMessage.setContent(MyApplication.getContext().getString(R.string.out_black_list));
                FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, chatMessage.getObjectId(), 0);// 更新本地群组状态
            }
            chatMessage.setType(XmppMessage.TYPE_TIP);
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
            }
            MsgBroadcast.broadcastMsgRoomUpdate(MyApplication.getContext());
            EventBus.getDefault().post(new EventRoomBlackChange(chatMessage.getObjectId()));
        } else if (type == XmppMessage.TYPE_ROOM_SHOW_MARKER) {
            EventBus.getDefault().post(new EventShowMarkerChanged(chatMessage.getObjectId(), Integer.parseInt(chatMessage.getContent()), chatMessage.getFilePath()));
        }
    }

    /**
     * 针对handleAgainstMessage处理的消息，群聊也收到的话就不做逻辑处理，只做转换tip显示逻辑
     *
     * @param mLoginUserId
     * @param chatMessage
     * @param isShieldGroupMsg 群控制消息在屏蔽情况也要生效，所以要传进来，但又不能保存和通知，
     */
    public static void handleMessage(String mLoginUserId, ChatMessage chatMessage, Friend friend, boolean isShieldGroupMsg) {
        int type = chatMessage.getType();
        String fromUserId = chatMessage.getFromUserId();
        String toUserId = chatMessage.getToUserId();

        if (!TextUtils.isEmpty(toUserId)) {
            if (TextUtils.equals(mLoginUserId, toUserId)) {
                // 针对我的操作，只需要为fromUserName赋值
                String fNameClone = getName(mLoginUserId, fromUserId, friend);
                if (!TextUtils.isEmpty(fNameClone)) {
                    chatMessage.setFromUserName(fNameClone);
                }
            } else {
                // 针对其他人的操作，fromUserName与toUserName都需要赋值
                String fNameClone = getName(mLoginUserId, fromUserId, friend);
                if (!TextUtils.isEmpty(fNameClone)) {
                    chatMessage.setFromUserName(fNameClone);
                }
                String tNameClone = getName(mLoginUserId, toUserId, friend);
                if (!TextUtils.isEmpty(tNameClone)) {
                    chatMessage.setToUserName(tNameClone);
                }
            }
        }
        chatMessage.setGroup(true);
        chatMessage.setType(XmppMessage.TYPE_TIP);

        // 群设置消息通知，0 不通知 1 通知
        int isAttritionNotice = 1;
        if (!TextUtils.isEmpty(chatMessage.getOther())) {
            try {
                JSONObject jsonObject = JSONObject.parseObject(chatMessage.getOther());
                isAttritionNotice = jsonObject.getInteger("isAttritionNotice");
            } catch (Exception ignored) {

            }
        }

        /*
        群文件
         */
        if (type == XmppMessage.TYPE_MUCFILE_ADD
                || type == XmppMessage.TYPE_MUCFILE_DEL
                || type == XmppMessage.TYPE_MUCFILE_DOWN) {
            if (type == XmppMessage.TYPE_MUCFILE_ADD) {
                // 仿QQ，上传的群文件使用一种文件类型消息气泡，所以群文件消息type不转换
                chatMessage.setType(type);
            } else if (type == XmppMessage.TYPE_MUCFILE_DEL) {
                // 仿QQ，删除群文件时，群文件气泡变为提示消息，
                // 群文件上传、删除消息，content字段均为群文件id，找到表内content一致的消息，更新type与content
                String fileId = chatMessage.getContent();
                ChatMessage msg = ChatMessageDao.getInstance().findMsgByAppointField(mLoginUserId, friend.getUserId(), "content", fileId);
                if (msg != null && ChatMessageDao.getInstance().updateMessageTypeContent(mLoginUserId, friend.getUserId()
                        , msg.getPacketId(), ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage))) {
                    // 本地有fileId一致的上传群文件的消息且数据库更新成功，通知聊天界面刷新
                    Intent intent = new Intent(OtherBroadcast.ACTION_GROUP_FILE_DELETE);
                    intent.putExtra("id", msg.getPacketId());
                    intent.putExtra("text", ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                    MyApplication.getContext().sendBroadcast(intent);
                }
                return;
            } else {
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
            }
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
            }
            return;
        }

         /*
        群组修改群内昵称，公告，拉人，踢人，禁言，解散等(901-907 913 934)
         */
        if (type == XmppMessage.TYPE_CHANGE_NICK_NAME) {
            // 修改群内昵称
            String content = chatMessage.getContent();
            if (!TextUtils.isEmpty(toUserId) && toUserId.equals(mLoginUserId)) {
                // 我修改了昵称
                if (!TextUtils.isEmpty(content)) {
                    friend.setRoomMyNickName(content);
                    FriendDao.getInstance().updateRoomMyNickName(friend.getUserId(), content);
                    ListenerManager.getInstance().notifyNickNameChanged(friend.getUserId(), toUserId, content);
                    ChatMessageDao.getInstance().updateNickName(mLoginUserId, friend.getUserId(), toUserId, content);
                }
            } else {
                // 其他人修改了昵称，先更新数据库再通知，
                RoomMemberDao.getInstance().updateRoomMemberUserName(friend.getRoomId(), toUserId, content);
                ListenerManager.getInstance().notifyNickNameChanged(friend.getUserId(), toUserId, content);
                ChatMessageDao.getInstance().updateNickName(mLoginUserId, friend.getUserId(), toUserId, content);
            }
            if (isAttritionNotice == 1 || TextUtils.equals(toUserId, mLoginUserId)) {
                // 群设置消息通知 || 与我有关 ，才通知
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
                }
            }
        } else if (type == XmppMessage.TYPE_CHANGE_ROOM_NAME) {
            // 修改群组名称
            String content = chatMessage.getContent();
            FriendDao.getInstance().updateMucFriendRoomName(friend.getUserId(), content);
            ListenerManager.getInstance().notifyNickNameChanged(friend.getUserId(), "ROOMNAMECHANGE", content);
            if (isAttritionNotice == 1 || TextUtils.equals(fromUserId, mLoginUserId)) {
                // 群设置消息通知 || 与我有关 ，才通知
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
                }
            }
        } else if (type == XmppMessage.TYPE_DELETE_ROOM) {
            // 群主解散该群
            // todo
        } else if (type == XmppMessage.TYPE_DELETE_MEMBER) {
            // 自己 退出 || 踢出
            if (TextUtils.equals(mLoginUserId, toUserId)) {
                return;
            } else {
                if (isAttritionNotice == 0) {
                    // 不通知时被踢出，该人群内发的消息全部删除
                    if (!TextUtils.equals(fromUserId, toUserId)) {
                        // 被踢出，该人群内发的消息全部删除
                        if (ChatMessageDao.getInstance().deleteMessageByFromUserId(mLoginUserId, friend.getUserId(), toUserId)) {
                            Intent intent = new Intent(OtherBroadcast.ACTION_GROUP_MEMBER_BE_DELETE);
                            intent.putExtra("fromUserId", toUserId);
                            MyApplication.getContext().sendBroadcast(intent);
                        }
                    }

                    // 更新RoomMemberDao、更新群聊界面
                    operatingRoomMemberDao(1, friend.getRoomId(), toUserId, null);
                    MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
                    return;
                }
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                // 更新RoomMemberDao、更新群聊界面
                operatingRoomMemberDao(1, friend.getRoomId(), toUserId, null);
                MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            }

            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
            }
        } else if (type == XmppMessage.TYPE_NEW_NOTICE
                || type == XmppMessage.TYPE_EDIT_GROUP_NOTICE
                || type == XmppMessage.TYPE_DELETE_NOTICE) {
            if (type == XmppMessage.TYPE_DELETE_NOTICE) {
                // 仿QQ，删除群公告时，公告气泡不删除，仅改变文字描述
                // 发布的公告noticeId在filePath字段内，删除的公告noticeId在content内，通过noticeId关联操作
                ChatMessage msg = ChatMessageDao.getInstance().findMsgByAppointField(mLoginUserId, friend.getUserId(), "filePath", chatMessage.getContent());
                if (msg != null && ChatMessageDao.getInstance().updateServiceGradeMessageStatus(mLoginUserId, friend.getUserId()
                        , msg.getPacketId(), true)) {
                    // 本地有noticeId一致的发布群公告的消息且数据库更新成功，通知聊天界面刷新
                    Intent intent = new Intent(OtherBroadcast.ACTION_GROUP_FILE_DELETE);
                    intent.putExtra("isGroupNoticeDelete", true);
                    intent.putExtra("id", msg.getPacketId());
                    MyApplication.getContext().sendBroadcast(intent);
                }
                EventBus.getDefault().post(new EventRemoveNotice(chatMessage));
                return;
            }
            // 发布公告 || 编辑公告
            EventBus.getDefault().post(new EventNewNotice(chatMessage));
            // 仿QQ，编辑公告相当于发布一条新公告，发布的公告使用一种新气泡类型，所以公告消息type不转换
            // todo 需要本地通知与铃声通知时，需移至XMucChatMessageListener内处理
            // chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
            if (type == XmppMessage.TYPE_EDIT_GROUP_NOTICE) {
                // 兼容一下旧版本
                type = XmppMessage.TYPE_NEW_NOTICE;
            }
            chatMessage.setType(type);
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
            }
        } else if (type == XmppMessage.TYPE_GAG) {
            // 禁言
            if (isAttritionNotice == 1 || TextUtils.equals(fromUserId, mLoginUserId) || TextUtils.equals(toUserId, mLoginUserId)) {
                // 群设置消息通知 || 与我有关 ，才通知
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, true);
                }
            }
        } else if (type == XmppMessage.NEW_MEMBER) {
            if (TextUtils.equals(fromUserId, toUserId)
                    && TextUtils.equals(mLoginUserId, toUserId)) {
                // 自己邀请自己也就是主动加群，不在这处理，
                // 别人扫码进群也是from和to相同，不能return了，
                return;
            }
            chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
            operatingRoomMemberDao(0, chatMessage.getFilePath(), chatMessage.getToUserId(), chatMessage.getToUserName());
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
                MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            }
        } else if (type == XmppMessage.TYPE_SEND_MANAGER
                || (type >= XmppMessage.TYPE_CHANGE_SHOW_READ && type <= XmppMessage.TYPE_GROUP_TRANSFER)
                || type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME
                || type == XmppMessage.TYPE_ALLOW_OPEN_LIVE
                || type == XmppMessage.TYPE_ROOM_MODIFY_CARD) {
             /*
             群管理相关
              */
            if (type == XmppMessage.TYPE_SEND_MANAGER) {
                int role = chatMessage.getContent().equals("1") ? 2 : 3;
                RoomMemberDao.getInstance().updateRoomMemberRole(friend.getRoomId(), toUserId, role);
                Intent intent = new Intent();
                intent.putExtra("roomId", friend.getUserId());
                intent.putExtra("toUserId", chatMessage.getToUserId());
                intent.putExtra("isSet", role == 2);
                intent.setAction(OtherBroadcast.REFRESH_MANAGER);
                MyApplication.getContext().sendBroadcast(intent);
                if (TextUtils.equals(toUserId, mLoginUserId)) {
                    MucgroupUpdateUtil.broadcastUpdateRole(MyApplication.getContext());
                }
            } else if (type == XmppMessage.TYPE_CHANGE_SHOW_READ) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_SHOW_READ + chatMessage.getObjectId(), chatMessage.getContent().equals("1"));
                MsgBroadcast.broadcastMsgRoomUpdate(MyApplication.getContext());
            } else if (type == XmppMessage.TYPE_GROUP_VERIFY) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_NEED_OWNER_ALLOW_NORMAL_INVITE_FRIEND + chatMessage.getObjectId(), chatMessage.getContent().equals("1"));
            } else if (type == XmppMessage.TYPE_GROUP_LOOK) {
            } else if (type == XmppMessage.TYPE_GROUP_SHOW_MEMBER) {
                MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            } else if (type == XmppMessage.TYPE_GROUP_SEND_CARD) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_SEND_CARD + chatMessage.getObjectId(), chatMessage.getContent().equals("1"));
                MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            } else if (type == XmppMessage.TYPE_GROUP_ALL_SHAT_UP) {
                // 单聊收到处理了，群聊这里还是要处理，以便兼容旧版服务器，
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.GROUP_ALL_SHUP_UP + chatMessage.getObjectId(), !chatMessage.getContent().equals("0"));
                MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_INVITE) {
                if (!chatMessage.getContent().equals("0")) {
                    MsgBroadcast.broadcastMsgRoomUpdateInvite(MyApplication.getContext(), 1);
                } else {
                    MsgBroadcast.broadcastMsgRoomUpdateInvite(MyApplication.getContext(), 0);
                }
            } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_UPLOAD) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + chatMessage.getObjectId(), !chatMessage.getContent().equals("0"));
            } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_CONFERENCE) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_ALLOW_NORMAL_CONFERENCE + chatMessage.getObjectId(), !chatMessage.getContent().equals("0"));
            } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_SEND_COURSE) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_ALLOW_NORMAL_SEND_COURSE + chatMessage.getObjectId(), !chatMessage.getContent().equals("0"));
            } else if (type == XmppMessage.TYPE_GROUP_TRANSFER) {
                if (friend != null) {
                    FriendDao.getInstance().updateRoomCreateUserId(mLoginUserId,
                            chatMessage.getObjectId(), chatMessage.getToUserId());
                    RoomMemberDao.getInstance().updateRoomMemberRole(friend.getRoomId(), chatMessage.getFromUserId(), 3);
                    RoomMemberDao.getInstance().updateRoomMemberRole(friend.getRoomId(), chatMessage.getToUserId(), 1);
                    Intent intent = new Intent();
                    intent.putExtra("roomId", friend.getUserId());
                    intent.putExtra("toUserId", chatMessage.getToUserId());
                    intent.putExtra("isSet", TextUtils.equals(mLoginUserId, chatMessage.getToUserId()));
                    intent.setAction(OtherBroadcast.REFRESH_OWNER);
                    MsgBroadcast.broadcastMsgRoleChanged(MyApplication.getContext());
                    if (TextUtils.equals(toUserId, mLoginUserId)) {
                        MucgroupUpdateUtil.broadcastUpdateRole(MyApplication.getContext());
                    }
                }
            } else if (type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME) {
                FriendDao.getInstance().updateChatRecordTimeOut(chatMessage.getObjectId(), Double.parseDouble(chatMessage.getContent()));
            } else if (type == XmppMessage.TYPE_ALLOW_OPEN_LIVE) {
                PreferenceUtils.putBoolean(MyApplication.getContext(),
                        Constants.IS_ALLOW_NORMAL_LIVE + chatMessage.getObjectId(), chatMessage.getContent().equals("1"));
                MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            } else if (type == XmppMessage.TYPE_ROOM_MODIFY_CARD) {
                EventBus.getDefault().post(new EventGroupStatus(12, Integer.valueOf(chatMessage.getContent())));// 更新群组信息页面
            }
            if (isShieldGroupMsg) {
                // 群控制消息在屏蔽情况也要生效，所以要传进来，但又不能保存和通知，
                return;
            }
            if (isAttritionNotice == 1 || TextUtils.equals(fromUserId, mLoginUserId) || TextUtils.equals(toUserId, mLoginUserId)) {
                // 群设置消息通知 || 与我有关 ，才通知
                chatMessage.setContent(ConvertMessage.convertGroupMessage(type, mLoginUserId, chatMessage));
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getObjectId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getObjectId(), chatMessage, true);
                }
            }
        } else if (type == XmppMessage.TYPE_ROOM_SHOW_MARKER) {
            MsgBroadcast.broadcastMsgRoomUpdateGetRoomStatus(MyApplication.getContext());
            EventBus.getDefault().post(new EventShowMarkerChanged(chatMessage.getObjectId(), Integer.parseInt(chatMessage.getContent()), chatMessage.getFilePath()));
        }
    }

    /**
     * 获取name
     *
     * @param mLoginUserId
     * @param toUserId
     * @param friend
     * @return
     */
    private static String getName(String mLoginUserId, String toUserId, Friend friend) {
        if (friend == null) {
            return null;
        }
        RoomMember mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(friend.getRoomId(), mLoginUserId);
        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(friend.getRoomId(), toUserId);
        if (mRoomMember != null && (mRoomMember.getRole() == 1 || mRoomMember.getRole() == 2)) {
            // 我为群主或管理员 群内备注>备注>群内昵称
            if (member != null && !TextUtils.equals(member.getUserName(), member.getCardName())) {
                // 当userName与cardName不一致时，我们认为有设置群内备注
                return member.getCardName();
            } else {
                Friend mFriend = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
                if (mFriend != null && !TextUtils.isEmpty(mFriend.getRemarkName())) {
                    return mFriend.getRemarkName();
                }
            }
        } else {
            Friend mFriend = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
            if (mFriend != null && !TextUtils.isEmpty(mFriend.getRemarkName())) {
                // 为好友 显示备注
                return mFriend.getRemarkName();
            }
        }
        if (!TextUtils.equals(mLoginUserId, toUserId) && member != null && member.getHiding() == 1) {
            return Constants.HIDING_NICKNAME;
        }
        return null;
    }

    /**
     * 更新群成员表
     *
     * @param type
     * @param roomId
     * @param userId
     * @param userName
     */
    private static void operatingRoomMemberDao(int type, String roomId, String userId, String userName) {
        if (type == 0) {
            RoomMember roomMember = new RoomMember();
            roomMember.setRoomId(roomId);
            roomMember.setUserId(userId);
            roomMember.setUserName(userName);
            roomMember.setCardName(userName);
            roomMember.setRole(3);
            roomMember.setCreateTime(0);
            RoomMemberDao.getInstance().saveSingleRoomMember(roomId, roomMember);
        } else {
            RoomMemberDao.getInstance().deleteRoomMember(roomId, userId);
        }
    }
}
