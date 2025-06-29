package com.tongxin.caihong.xmpp;

import static android.content.Context.VIBRATOR_SERVICE;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.audio.NoticeVoicePlayer;
import com.tongxin.caihong.bean.CodePay;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.event.EventTransfer;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.call.CallConstants;
import com.tongxin.caihong.call.JitsistateMachine;
import com.tongxin.caihong.call.MessageCallTypeChange;
import com.tongxin.caihong.call.MessageCallingEvent;
import com.tongxin.caihong.call.MessageEventMeetingInvited;
import com.tongxin.caihong.call.MessageEventSipEVent;
import com.tongxin.caihong.call.MessageHangUpPhone;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.pay.EventPaymentSuccess;
import com.tongxin.caihong.pay.EventReceiptSuccess;
import com.tongxin.caihong.service.xmpp.HandleCustomerMessage;
import com.tongxin.caihong.socket.EMConnectionManager;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.login.AuthLoginActivity;
import com.tongxin.caihong.ui.message.HandleSecureChatMessage;
import com.tongxin.caihong.ui.message.HandleSyncMoreLogin;
import com.tongxin.caihong.ui.yeepay.EventYeepaySendRedSuccess;
import com.tongxin.caihong.ui.yeepay.EventYeepayTransferSuccess;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.DateFormatUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.xmpp.util.ConvertMessage;
import com.tongxin.caihong.xmpp.util.HandleCompanyMessage;
import com.tongxin.caihong.xmpp.util.HandleFriendMessage;
import com.tongxin.caihong.xmpp.util.HandleGroupMessage;
import com.tongxin.caihong.xmpp.util.HandleLifeCircleMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by Administrator on 2017/11/24.
 */

public class XChatMessageListener {
    // 不明原因可能导致listener出现多个，至少在收消息时确保去重，
    private static final LruCache<String, String> mMsgIDMap = new LruCache<>(200);
    public static Map<String, Long> exitGroupTimeMap = new HashMap<>();
    private CoreService mService;
    private String mLoginUserId;
    private List<ChatMessage> delayHandleGroupMsgList = new ArrayList<>();// 延迟处理队列内的群控制消息

    public XChatMessageListener(CoreService service) {
        mService = service;
        mLoginUserId = CoreManager.requireSelf(service).getUserId();
    }

    public void onReceMessage(ChatMessage chatMessage, boolean isDelayEndMsg) {
        newIncomingMessage(chatMessage, isDelayEndMsg);
    }

    private void newIncomingMessage(ChatMessage chatMessage, boolean isDelayEndMsg) {
        String fromUserId = chatMessage.getFromUserId();
        String toUserName = chatMessage.getToUserName();
        String toUserId = chatMessage.getToUserId();
        Log.e("msg", "收到单聊消息" + chatMessage.toString());

        synchronized (mMsgIDMap) {
            String exists = mMsgIDMap.get(chatMessage.getPacketId());
            if (TextUtils.equals(exists, mLoginUserId)) {
                return;
            }
            mMsgIDMap.put(chatMessage.getPacketId(), mLoginUserId);
        }

        int type = chatMessage.getType();
        if (type == 0) { // 消息过滤
            return;
        }
        ChatMessageDao.getInstance().decrypt(false, chatMessage);// 解密

        if (SeqNoManager.getInstance().isWaitGetLasRequestResponse
                && chatMessage.getSeqNo() > 0) {
            Log.e(SeqNoManager.TAG, "正在调用getLast接口，收到了 " + chatMessage.getFromUserName() + "的消息，且seqNo大于0，记录一下");
            SeqNoManager.getInstance().waitGetLasRequestResponseReceivedMsg.put(fromUserId, String.valueOf(chatMessage.getSeqNo()));
        }

        /*
        服务器发的消息 fromId='10005/Server'
         */
        /**
         *  我的设备发送过来的消息
         * 1.fromUserId等于当前登录的id
         * 2.toUserId等于当前登录的id
         * 3.因为服务端发送的消息有可能会符合以上的两个条件(ex:部分群控制消息...)，所以还需要判断下fromId
         */
        if (fromUserId.equals(mLoginUserId)
                && toUserId.equals(mLoginUserId)
                && !chatMessage.getFromId().toLowerCase().contains("server")) {

            if (!TextUtils.equals(chatMessage.getToId(), EMConnectionManager.CURRENT_DEVICE)) {
                // 以上基本锁定为我的设备发的消息了，在判断to是否为android，是则处理，否则return
                Log.e("msg", "我的设备-->" + chatMessage.getFromId()
                        + "发给我的设备-->" + chatMessage.getToId() + "的消息");
                return;
            }

            if (chatMessage.getType() == 26) {
                String packetId = chatMessage.getContent();
                ChatMessageDao.getInstance().updateMessageRead(mLoginUserId, chatMessage.getFromId(), packetId, true);
                boolean isReadChange = ChatMessageDao.getInstance().updateReadMessage(mLoginUserId, chatMessage.getFromId(), packetId);
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("packetId", packetId);
                bundle.putBoolean("isReadChange", isReadChange);
                intent.setAction(OtherBroadcast.IsRead);
                intent.putExtras(bundle);
                mService.sendBroadcast(intent);
                return;
            }

            // 我的其他设备发来的消息也要通知，
            mService.notificationMessage(chatMessage, false);
            NoticeVoicePlayer.getInstance().start();
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getFromId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getFromId(), chatMessage, false);
            }
            return;
        }

        /**
         * 转发消息且我为发送方，该条消息需要换表存储
         */
        boolean isNeedChangeMsgTableSave = false;
        if (fromUserId.equals(mLoginUserId)
                && !chatMessage.getFromId().toLowerCase().contains("server")) {
            isNeedChangeMsgTableSave = true;
            // 多点登录下 其他端发过来的消息肯定是已经上传成功了，这里加上
            chatMessage.setUpload(true);
            chatMessage.setUploadSchedule(100);
            chatMessage.setMySend(true);
            chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        } else {
            // 收到了别人的消息
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getFromUserId());
            // todo
            if (friend != null && friend.getStatus() != -1 && friend.getOfflineNoPushMsg() == 0) {
                // 本地通知
                if (friend.getHideChatSwitch() == 1) {
                    // 私密模式
                    boolean privateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + mLoginUserId, true);
                    if (!privateMode) {
                        // 关闭了私密模式
                        mService.notificationMessage(chatMessage, false);
                    }
                } else {
                    mService.notificationMessage(chatMessage, false);
                }
            }
        }

        /*
        已读消息
         */
        if (chatMessage.getType() == XmppMessage.TYPE_READ) {
            String packetId = chatMessage.getContent();
            String objectId = chatMessage.getObjectId();

            if (chatMessage.getFromUserId().equals(mLoginUserId)) {// 其他端发送过来的已读
                ChatMessage msgById = ChatMessageDao.getInstance().findMsgById(mLoginUserId, chatMessage.getToUserId(), packetId);
                if (msgById != null && msgById.getIsReadDel()) {// 在其他端已读了该条阅后即焚消息，本端也需要删除
                    if (ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, chatMessage.getToUserId(), packetId)) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("MULTI_LOGIN_READ_DELETE_PACKET", packetId);
                        intent.setAction(OtherBroadcast.MULTI_LOGIN_READ_DELETE);
                        intent.putExtras(bundle);
                        mService.sendBroadcast(intent);
                    }
                }
            } else if (!TextUtils.isEmpty(objectId)) {
                String roomJid = objectId;
                ChatMessage msgById = ChatMessageDao.getInstance().findMsgById(mLoginUserId, roomJid, packetId);
                if (msgById != null) {
                    boolean repeat = ChatMessageDao.getInstance().checkRepeatRead(mLoginUserId, roomJid, fromUserId, packetId);
                    if (!repeat) {
                        int count = msgById.getReadPersons();// 查看人数+1
                        msgById.setReadPersons(count + 1);
                        // 覆盖最后时间
                        msgById.setReadTime(chatMessage.getTimeSend());
                        // 更新消息数据
                        ChatMessageDao.getInstance().updateMessageRead(mLoginUserId, roomJid, msgById);
                        // 保存新消息
                        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, roomJid, chatMessage);
                        // 通知刷新
                        MsgBroadcast.broadcastMsgReadUpdate(MyApplication.getInstance(), packetId);
                    }
                }

            } else {
                ChatMessageDao.getInstance().updateMessageRead(mLoginUserId, fromUserId, packetId, true);// 更新状态为已读
                boolean isReadChange = ChatMessageDao.getInstance().updateReadMessage(mLoginUserId, fromUserId, packetId);
                // 发送广播通知聊天页面，将未读的消息修改为已读
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("packetId", packetId);
                bundle.putBoolean("isReadChange", isReadChange);
                intent.setAction(OtherBroadcast.IsRead);
                intent.putExtras(bundle);
                mService.sendBroadcast(intent);
            }
            return;
        }

        /*
        同步消息，收到后判断objectId在调用对应接口同步离线时其他设备做的操作
        与多点登录相关
         */
        if ((chatMessage.getType() >= XmppMessage.TYPE_SYNC_OTHER
                && chatMessage.getType() <= XmppMessage.TYPE_SYNC_GROUP)
                || chatMessage.getType() == XmppMessage.TYPE_SEAL
                || chatMessage.getType() == XmppMessage.TYPE_FRIEND_CHANGE_NAME
                || chatMessage.getType() == XmppMessage.TYPE_FRIEND_CHANGE_ENCRYPT_TYPE) {
            HandleSyncMoreLogin.distributionChatMessage(chatMessage, mService, chatMessage.isDelayMsg());
            return;
        }

        /*
        旧设备授权登录消息
         */
        if (chatMessage.getType() == XmppMessage.TYPE_AUTH_LOGIN) {
            AuthLoginActivity.start(mService, chatMessage.getContent());
            return;
        }

        /*
        阅后即焚截屏，双向清空，撤回，正在输入...消息
         */
        if (type == XmppMessage.TYPE_SCREENSHOT
                || type == XmppMessage.TYPE_SYNC_CLEAN_CHAT_HISTORY
                || type == XmppMessage.TYPE_INPUT
                || type == XmppMessage.TYPE_BACK) {
            if (type == XmppMessage.TYPE_SCREENSHOT) {
                // 阅后即焚时对方截屏提醒
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(ConvertMessage.convertVariousChatMessage(XmppMessage.TYPE_SCREENSHOT, mLoginUserId, chatMessage));
                // 处理成tip后不return，正常流程处理，
            } else if (type == XmppMessage.TYPE_SYNC_CLEAN_CHAT_HISTORY) {
                // 双向清空聊天记录
                Intent intent = new Intent(OtherBroadcast.SYNC_CLEAN_CHAT_HISTORY);
                if (TextUtils.equals(chatMessage.getContent(), "claer_all")) {
                    // 后台清空所有聊天记录
                    intent.putExtra("CLEAN_ALL", true);
                } else if (!TextUtils.isEmpty(chatMessage.getObjectId())) {
                    // 后台清空某个群组的聊天记录
                    intent.putExtra("roomJid", chatMessage.getObjectId());
                } else {
                    // 后台清空某个用户与某个用户的聊天记录 或 某个用户在应用内清空双方聊天记录
                    intent.putExtra(AppConstant.EXTRA_USER_ID, isNeedChangeMsgTableSave
                            ? chatMessage.getToUserId() : chatMessage.getFromUserId());
                }
                // 如果非自动登录进入，可能主界面receive的广播可能都来不及注册，长连接就连上了服务器并且收到了该消息，
                // 延时DELAY_MILLIS_TRIGGER_EVENT ms在发送吧
                new Handler(Looper.getMainLooper()).postDelayed(() -> mService.sendBroadcast(intent), AppConstant.DELAY_MILLIS_TRIGGER_EVENT);
                return;
            } else if (type == XmppMessage.TYPE_BACK) {
                // 撤回
                backMessage(chatMessage);
                return;
            } else if (type == XmppMessage.TYPE_INPUT) {
                // 正在输入
                Intent intent = new Intent();
                intent.putExtra("fromId", chatMessage.getFromUserId());
                intent.setAction(OtherBroadcast.TYPE_INPUT);
                mService.sendBroadcast(intent);
                return;
            }
        }

        if (chatMessage.getSeqNo() > 0) {
            Log.e(SeqNoManager.TAG, "消息序号大于0，准备检验序号");
            SeqNoManager.getInstance().checkSeqNo(!isNeedChangeMsgTableSave ? fromUserId : toUserId, chatMessage.getSeqNo(), chatMessage.isDelayMsg());

            if (isDelayEndMsg) {
                Log.e(SeqNoManager.TAG, "离线消息已收完，检查loseSeqNos");
                SeqNoManager.getInstance().checkLoseSeqNos();
            }
        }

        /*
        支付相关消息
         */
        if (type == XmppMessage.TYPE_83
                || type == XmppMessage.TYPE_RED_BACK
                || type == XmppMessage.TYPE_TRANSFER_RECEIVE
                || type == XmppMessage.TYPE_TRANSFER_BACK
                || type == XmppMessage.TYPE_SCAN_RECHARGE
                || type == XmppMessage.TYPE_SCAN_WITHDRAW
                || type == XmppMessage.TYPE_PAYMENT_OUT
                || type == XmppMessage.TYPE_PAYMENT_GET
                || type == XmppMessage.TYPE_RECEIPT_OUT
                || type == XmppMessage.TYPE_RECEIPT_GET
                || type == XmppMessage.TYPE_YEEPAY_SEND_RED_SUCCESS
                || type == XmppMessage.TYPE_YEEPAY_TRANSFER_SUCCESS) {
            boolean isSaveTable = true;
            if (type == XmppMessage.TYPE_83) {
                // 红包领取通知
                // 针对红包领取的提示消息 需要做点击事件处理，将红包的type与id存入其他字段内
                chatMessage.setFileSize(XmppMessage.TYPE_83);
                chatMessage.setFilePath(chatMessage.getContent());

                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(ConvertMessage.convertPayMessage(type, mLoginUserId, chatMessage));
            } else if (type == XmppMessage.TYPE_RED_BACK) {
                // 红包退回通知
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(ConvertMessage.convertPayMessage(type, mLoginUserId, chatMessage));
                if (!TextUtils.isEmpty(chatMessage.getObjectId())) {
                    // 群组红包退回
                    fromUserId = chatMessage.getObjectId();
                    if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
                        ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, true);
                    }
                } else {
                    // 单聊红包退回
                    if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
                        ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                    }
                }
                // 红包退回与群聊牵扯在一起了，直接在此处return
                return;
            } else if (type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
                // 转账领取通知
                // 更新数据库内转账消息为被领取状态
                List<ChatMessage> chatMessages = ChatMessageDao.getInstance().getAllSameObjectIdMessages(mLoginUserId, fromUserId, chatMessage.getContent());
                for (int i = 0; i < chatMessages.size(); i++) {
                    ChatMessageDao.getInstance().updateChatMessageReceiptStatus(mLoginUserId, fromUserId, chatMessages.get(i).getPacketId());
                }
                // 通知到聊天界面
                EventBus.getDefault().post(new EventTransfer(chatMessage.clone(false)));
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(ConvertMessage.convertPayMessage(type, mLoginUserId, chatMessage));
            } else if (type == XmppMessage.TYPE_TRANSFER_BACK) {
                // 转账退回通知
                // 转账退回在支付公众号内显示了，不能修改这条消息的content...
/*
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setContent(ConvertMessage.convertPayMessage(type, mLoginUserId, chatMessage));
*/
            } else if (type == XmppMessage.TYPE_SCAN_RECHARGE) {
                // 扫码充值返回通知
            } else if (type == XmppMessage.TYPE_SCAN_WITHDRAW) {
                // 扫码提现返回通知
            } else if (type == XmppMessage.TYPE_PAYMENT_OUT) {
                // 付款码-已付款通知
                CodePay codePay = JSON.parseObject(chatMessage.getContent(), CodePay.class);
                EventBus.getDefault().post(new EventPaymentSuccess(codePay.getToUserName()));
                CoreManager.updateMyBalance();
            } else if (type == XmppMessage.TYPE_PAYMENT_GET) {
                // 付款码-已到账通知
                CoreManager.updateMyBalance();
            } else if (type == XmppMessage.TYPE_RECEIPT_OUT) {
                // 收款码-已付款通知
                CoreManager.updateMyBalance();
            } else if (type == XmppMessage.TYPE_RECEIPT_GET) {
                // 收款码-已到账通知
                CodePay codePay = JSON.parseObject(chatMessage.getContent(), CodePay.class);
                EventBus.getDefault().post(new EventReceiptSuccess(codePay.getToUserName()));
                CoreManager.updateMyBalance();
            } else if (type == XmppMessage.TYPE_YEEPAY_SEND_RED_SUCCESS) {
                // 易宝发红包支付成功回执通知
                isSaveTable = false;
                EventBus.getDefault().post(new EventYeepaySendRedSuccess(chatMessage.getContent()));
            } else if (type == XmppMessage.TYPE_YEEPAY_TRANSFER_SUCCESS) {
                // 易宝发转账支付成功回执通知
                isSaveTable = false;
                EventBus.getDefault().post(new EventYeepayTransferSuccess(chatMessage.getContent()));
            }

            if (isSaveTable) {
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                }
            }
        }

        /*
        端到端相关消息
         */
        if (chatMessage.getType() == XmppMessage.TYPE_SECURE_REFRESH_KEY
                || chatMessage.getType() == XmppMessage.TYPE_SECURE_SEND_KEY) {
            if (chatMessage.getType() == XmppMessage.TYPE_SECURE_REFRESH_KEY) {
                HandleSecureChatMessage.distributionChatMessage(chatMessage);
            } else {
                if (!TextUtils.equals(chatMessage.getFromUserId(), mLoginUserId)) {
                    // 需要考虑多点登录的情况
                    try {
                        // 单聊收到805消息，群成员发送了chatKey给我，解密存入本地，更新界面
                        String chatKey = new String(RSA.decryptFromBase64(chatMessage.getContent(), Base64.decode(SecureChatUtil.getRSAPrivateKey(mLoginUserId))));
                        HandleSecureChatMessage.distributionChatMessage(chatMessage);
                        FriendDao.getInstance().updateChatKeyGroup(chatMessage.getObjectId(), SecureChatUtil.encryptChatKey(chatMessage.getObjectId(), chatKey));
                        Log.e("msg", "设置chatKey成功-->" + chatKey);
                    } catch (Exception e) {
                        Log.e("msg", "设置chatKey失败");
                        FriendDao.getInstance().updateIsLostChatKeyGroup(chatMessage.getObjectId(), 1);
                    }
                }
            }
            return;
        }

        /*
        生活圈消息 301-305
         */
        if (type >= XmppMessage.DIANZAN && type <= XmppMessage.FRIEND_PUBLISH) {
            HandleLifeCircleMessage.handleMessage(mLoginUserId, chatMessage);
            return;
        }

        // 音视频相关消息100-134
        if (type >= XmppMessage.TYPE_IS_CONNECT_VOICE && type <= XmppMessage.TYPE_TALK_KICK) {
            chatAudioVideo(chatMessage);
            return;
        }

        /*
        公司消息 320-321
        321消息让其继续往下走吧
         */
        if (type == XmppMessage.TYPE_COMPANY_APPLY_JOIN) {
            HandleCompanyMessage.handleMessage(mLoginUserId, chatMessage);
            return;
        }

        /*
        朋友消息 500-516
         */
        if (type >= XmppMessage.TYPE_SAYHELLO && type <= XmppMessage.TYPE_DELETE_ME) {
            HandleFriendMessage.handleMessage(mLoginUserId, chatMessage);
            return;
        }

        /*
        群组，与我有直接关系的消息
         */
        if (type == XmppMessage.NEW_MEMBER
                || type == XmppMessage.TYPE_DELETE_MEMBER
                || type == XmppMessage.TYPE_DELETE_ROOM
                || type == XmppMessage.TYPE_GAG
                || type == XmppMessage.TYPE_SEND_MANAGER
                || type == XmppMessage.TYPE_UPDATE_ROLE
                || type == XmppMessage.TYPE_GROUP_VERIFY
                || type == XmppMessage.TYPE_GROUP_ALL_SHAT_UP
                || type == XmppMessage.TYPE_GROUP_TRANSFER
                || type == XmppMessage.TYPE_FACE_GROUP_NOTIFY
                || type == XmppMessage.TYPE_DISABLE_GROUP
                || type == XmppMessage.TYPE_ROOM_ADD_BLACK_LIST
                || type == XmppMessage.TYPE_ROOM_SHOW_MARKER) {
            HandleGroupMessage.handleAgainstMessage(mLoginUserId, toUserName, chatMessage.getOther(), chatMessage, mService);
            return;
        }

        if (type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME) {
            // 单聊收到这条消息，是客户端自己发的，拿出来处理
            chatMessage.setType(XmppMessage.TYPE_TIP);
            String content;
            double outTime = Double.parseDouble(chatMessage.getContent());
            String autoTime = DateFormatUtil.timeStr(Double.parseDouble(chatMessage.getContent()));
            if (isNeedChangeMsgTableSave) {
                // 自己设置的
                // 本地数据库需要同步
                FriendDao.getInstance().updateChatRecordTimeOut(chatMessage.getToUserId(), outTime);
                if (outTime == 0 || outTime == -1) {
                    content = MyApplication.getContext().getString(R.string.tip_set_msg_no_auto_delete);
                } else {
                    content = MyApplication.getContext().getString(R.string.tip_set_msg_auto_delete, autoTime, autoTime);
                }
            } else {
                // 对方设置的
                // 保留真实type，保留设置的过期自动销毁时长，用于点击tip消息进行同步
                chatMessage.setFileSize(type);
                chatMessage.setObjectId(chatMessage.getContent());
                if (outTime == 0 || outTime == -1) {
                    content = MyApplication.getContext().getString(R.string.tip_each_set_msg_no_auto_delete, MyApplication.getContext().getString(R.string.tip_sync_set_msg_auto_delete));
                } else {
                    content = MyApplication.getContext().getString(R.string.tip_each_set_msg_auto_delete, autoTime, autoTime
                            , MyApplication.getContext().getString(R.string.tip_sync_set_msg_auto_delete));
                }
            }
            chatMessage.setContent(content);
        }

        if (HandleCustomerMessage.process(mLoginUserId, chatMessage)) {
            return;
        }

        // 存储消息
        if (chatMessage.isExpired()) {// 该条消息为过期消息(基本可以判断为离线消息)，不进行存库通知
            Log.e("msg", "该条消息为过期消息(基本可以判断为离线消息)，不进行存库通知");
            return;
        }

        // 戳一戳
        if (type == XmppMessage.TYPE_SHAKE) {
            Vibrator vibrator = (Vibrator) MyApplication.getContext().getSystemService(VIBRATOR_SERVICE);
            long[] pattern = {100, 400, 100, 400};
            if (vibrator != null) {
                vibrator.vibrate(pattern, -1);
            }
            // 发送戳一戳动画广播
            Intent intent = new Intent(OtherBroadcast.ACTION_SHAKE_MSG_NOTIFY);
            intent.putExtra(OtherBroadcast.ACTION_SHAKE_MSG_NOTIFY, 1);
            intent.setComponent(new ComponentName(AppConfig.sPackageName, AppConfig.shakeReceiverClass));
            MyApplication.getContext().sendBroadcast(intent);
        }

        // 风险提示，换下type即可
        if (type == XmppMessage.TYPE_SERVER_TIP) {
            chatMessage.setType(XmppMessage.TYPE_TIP);
        }

        // 邀请入群验证消息
        if (type == XmppMessage.TYPE_INVITE_JOIN_ROOM) {
            // 私密群组的邀请，chatKey放在了other字段内，other在ChatMessage内只是局部变量
            // 存到translation字段内吧，后面会有用到
            if (!TextUtils.isEmpty(chatMessage.getOther())) {
                chatMessage.setTranslation(chatMessage.getOther());
            }
        }

        SeqNoManager.getInstance().updateTime(chatMessage.isDelayMsg());

        if (isNeedChangeMsgTableSave) {
            Log.e("msg", "转发消息且我为发送方");
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getToUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getFromUserId(), chatMessage, false);
            }
            return;
        }

        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getFromUserId());
        if (friend != null) {
            if (friend.getStatus() != Friend.STATUS_BLACKLIST) {
                Log.e("msg", "朋友发送过来的消息");
                saveCurrentMessage(chatMessage);
                if (friend.getOfflineNoPushMsg() == 0) {// 未开启消息免打扰 可通知
                    if (!chatMessage.getFromUserId().equals(MyApplication.IsRingId)) {// 收到该消息时不处于与发送方的聊天界面 && 非转发消息
                        if (friend.getStatus() != Friend.STATUS_23) {
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
                    }
                } else {
                    Log.e("msg", "已针对该好友开启了消息免打扰，不通知");
                }
            } else {
                Log.e("msg", "黑名单发送过来的消息，不做任何处理");
            }
        } else {
            Log.e("msg", "陌生人发过来的消息");
            FriendDao.getInstance().createNewFriend(chatMessage);
            saveCurrentMessage(chatMessage);
            // 铃声通知
            // NoticeVoicePlayer.getInstance().start();
        }
    }

    /**
     * 撤回消息
     *
     * @param chatMessage
     */
    private void backMessage(ChatMessage chatMessage) {
        String packetId = chatMessage.getContent();
        if (TextUtils.isEmpty(packetId)) {
            return;
        }
        if (chatMessage.getFromUserId().equals(mLoginUserId)) {
            // 其他端撤回
            ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, chatMessage.getToUserId(), packetId, MyApplication.getContext().getString(R.string.you));
        } else {
            ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, chatMessage.getFromUserId(), packetId, chatMessage.getFromUserName());
        }

        Intent intent = new Intent();
        intent.putExtra("packetId", packetId);
        intent.setAction(OtherBroadcast.MSG_BACK);
        mService.sendBroadcast(intent);

        // 更新UI界面
        if (chatMessage.getFromUserId().equals(mLoginUserId)) {
            ChatMessage message = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, chatMessage.getToUserId());
            if (message != null && message.getPacketId().equals(packetId)) {
                // 要撤回的消息正是朋友表的最后一条消息
                message.setType(XmppMessage.TYPE_TEXT);
                    FriendDao.getInstance().updateFriendContent(mLoginUserId, chatMessage.getToUserId(),
                             MyApplication.getContext().getString(R.string.you) + " " + MyApplication.getContext().getString(R.string.other_with_draw), message);
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
            }
        } else {
            ChatMessage message = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, chatMessage.getFromUserId());
            if (message != null && message.getPacketId().equals(packetId)) {
                // 要撤回的消息正是朋友表的最后一条消息
                message.setType(XmppMessage.TYPE_TEXT);

                   FriendDao.getInstance().updateFriendContent(mLoginUserId, chatMessage.getFromUserId(),
                        chatMessage.getFromUserName() + " " + MyApplication.getContext().getString(R.string.other_with_draw), message);
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
            }
        }
    }

    private void saveCurrentMessage(ChatMessage chatMessage) {
        // 数据库保存message对象
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getFromUserId(), chatMessage)) {
            // 成功后去刷新UI
            ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, chatMessage.getFromUserId(), chatMessage, false);
        }
    }


    private void chatAudioVideo(ChatMessage chatMessage) {
        int type = chatMessage.getType();
        Log.e("AVI", type + "");
        String fromUserId = chatMessage.getFromUserId();
        if (fromUserId.equals(mLoginUserId)) {
            switch (chatMessage.getType()) {
                case XmppMessage.TYPE_IS_CONNECT_VOICE:
                    // 其他端发起语音通话请求，转发给本端，不处理
                    break;
                case XmppMessage.TYPE_CONNECT_VOICE:
                    // 其他端已接听语音通话，本端需要结束当前来电显示界面
                    EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
                    break;
                case XmppMessage.TYPE_NO_CONNECT_VOICE:
                    // 其他端拒接 || 无响应 语音通话，本端需要结束当前来电显示界面
                    EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
                    break;
                case XmppMessage.TYPE_END_CONNECT_VOICE:
                    // 其他端结束了语音通话，不处理
                    break;
                case XmppMessage.TYPE_IS_CONNECT_VIDEO:
                    // 其他端发起视频通话请求，转发给本端， 不处理
                    break;
                case XmppMessage.TYPE_CONNECT_VIDEO:
                    // 其他端已接听视频通话，本端需要结束当前来电显示界面
                    EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
                    break;
                case XmppMessage.TYPE_NO_CONNECT_VIDEO:
                    // 其他端拒接 || 无响应 视频通话，本端需要结束当前来电显示界面
                    EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
                    break;
                case XmppMessage.TYPE_END_CONNECT_VIDEO:
                    // 其他端结束了视频通话，不处理
                    break;
                case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                    // 其他端发起语音会议请求，不处理
                    break;
                case XmppMessage.TYPE_IS_MU_CONNECT_VIDEO:
                    // 其他端发起视频会议请求，不处理
                    break;
                case XmppMessage.TYPE_IS_MU_CONNECT_TALK:
                    // 其他端发起对讲机请求，不处理
                    break;

                case XmppMessage.TYPE_IN_CALLING:
                    // 其他端发送的通话中消息，不处理
                    break;
                case XmppMessage.TYPE_IS_BUSY:
                    // 其他端发送的忙线消息，不处理
                    break;
            }
        } else {
            if (chatMessage.getType() == XmppMessage.TYPE_IN_CALLING
                    || chatMessage.getType() == XmppMessage.TYPE_IS_BUSY) {
                if (chatMessage.getType() == XmppMessage.TYPE_IS_BUSY) {// 延迟两秒发送该通知，防止自己拨号页面还未拉起就收到了
                    Log.e("zq", "收到" + chatMessage.getFromUserName() + "的busy消息");
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("zq", "发送busy通知给" + chatMessage.getFromUserName());
                            EventBus.getDefault().post(new MessageCallingEvent(chatMessage));
                        }
                    }, 2000);
                    if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getFromUserId(), chatMessage)) {
                        ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                    }
                } else {
                    EventBus.getDefault().post(new MessageCallingEvent(chatMessage));
                }
                return;
            }

            if (chatMessage.getType() == XmppMessage.TYPE_CHANGE_VIDEO_ENABLE) {
                EventBus.getDefault().post(new MessageCallTypeChange(chatMessage));
            }

            /*
            单聊 语音通话
             */
            if (chatMessage.getType() == XmppMessage.TYPE_IS_CONNECT_VOICE) {
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getFromUserId());
                if (friend != null && friend.getStatus() == -1) {
                    // 不处理黑名单来的音视频邀请，
                    return;
                }
                // 对方来电
                if (JitsistateMachine.isInCalling
                        && !TextUtils.isEmpty(JitsistateMachine.callingOpposite)) {
                    if (JitsistateMachine.callingOpposite.equals(chatMessage.getFromUserId())) {
                        // 当前正在通话中且该条消息的发送方与通话对象一致(对方可能异常断开了，且本地的ping机制还未检测出来，自行挂断，
                        // 在发送通知弹起来电界面)
                        EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.Interrupt));
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                EventBus.getDefault().post(new MessageEventSipEVent(100, fromUserId, chatMessage));
                            }
                        }, 500);
                    } else {
                        // 当前正在通话中且该条消息的发送方与通话对象不一致 通知发送方忙线中...
                        Log.e("zq", "发送busy消息给" + chatMessage.getFromUserName());
                        mService.sendBusyMessage(chatMessage.getFromUserId(), 0);
                    }
                    return;
                }
                Log.e("AVI", TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() + "");
                if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() <= 30 * 1000) {// 当前时间与对方发送邀请的时间间隔在30s以内
                    EventBus.getDefault().post(new MessageEventSipEVent(100, fromUserId, chatMessage));
                } else {
                    Log.e("AVI", "离线消息");
                }
            } else if (chatMessage.getType() == XmppMessage.TYPE_CONNECT_VOICE) {
                // 对方接听语音通话，发送102
                EventBus.getDefault().post(new MessageEventSipEVent(102, null, chatMessage));
            } else if (chatMessage.getType() == XmppMessage.TYPE_NO_CONNECT_VOICE) {
                // 对方拒接 || 无响应
                EventBus.getDefault().post(new MessageEventSipEVent(103, null, chatMessage));
                String content = "";
                chatMessage.setMySend(false);
                if (chatMessage.getTimeLen() == 0) {
                    content = MyApplication.getContext().getString(R.string.sip_canceled) + MyApplication.getContext().getString(R.string.voice_chat);
                } else {
                    content = MyApplication.getContext().getString(R.string.sip_noanswer);
                }
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage);
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, content, chatMessage);
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
                    }
                }, 1000);// 延迟一秒在发送挂断消息，防止当我们离线时，对方发起通话之后又取消了通话，我们30秒内上线，在来点界面拉起时该Event也发送出去了
            } else if (chatMessage.getType() == XmppMessage.TYPE_END_CONNECT_VOICE) {
                // 通话后，对方挂断
                chatMessage.setMySend(false);
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage);
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, MyApplication.getContext().getString(R.string.finished) + MyApplication.getContext().getString(R.string.voice_chat) + "," +
                        MyApplication.getContext().getString(R.string.time_len) + ":" + chatMessage.getTimeLen() + MyApplication.getContext().getString(R.string.second),  chatMessage);
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
                // 通知通话界面挂断
                EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
            }

             /*
            单聊  视频通话
             */
            if (type == XmppMessage.TYPE_IS_CONNECT_VIDEO) {
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getFromUserId());
                if (friend != null && friend.getStatus() == -1) {
                    // 不处理黑名单来的音视频邀请，
                    return;
                }
                if (JitsistateMachine.isInCalling
                        && !TextUtils.isEmpty(JitsistateMachine.callingOpposite)) {
                    if (JitsistateMachine.callingOpposite.equals(chatMessage.getFromUserId())) {
                        // 当前正在通话中且该条消息的发送方与通话对象一致(对方可能异常断开了，且本地的ping机制还未检测出来，自行挂断，
                        // 在发送通知弹起来电界面)
                        EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.Interrupt));
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                EventBus.getDefault().post(new MessageEventSipEVent(110, fromUserId, chatMessage));
                            }
                        }, 500);
                    } else {
                        // 当前正在通话中且该条消息的发送方与通话对象不一致 通知发送方忙线中...
                        Log.e("zq", "发送busy消息给" + chatMessage.getFromUserName());
                        mService.sendBusyMessage(chatMessage.getFromUserId(), 1);
                    }
                    return;
                }
                Log.e("AVI", TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() + "");
                if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() <= 30 * 1000) {// 当前时间与对方发送邀请的时间间隔在30s以内
                    EventBus.getDefault().post(new MessageEventSipEVent(110, fromUserId, chatMessage));
                } else {
                    Log.e("AVI", "离线消息");
                }
            } else if (type == XmppMessage.TYPE_CONNECT_VIDEO) {
                EventBus.getDefault().post(new MessageEventSipEVent(112, null, chatMessage));
            } else if (type == XmppMessage.TYPE_NO_CONNECT_VIDEO) {
                EventBus.getDefault().post(new MessageEventSipEVent(113, null, chatMessage));
                chatMessage.setMySend(false);
                String content = "";
                if (chatMessage.getTimeLen() == 0) {
                    content = MyApplication.getContext().getString(R.string.sip_canceled) + MyApplication.getContext().getString(R.string.voice_chat);
                } else {
                    content = MyApplication.getContext().getString(R.string.sip_noanswer);
                }
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage);
                FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, content,  chatMessage);
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
                    }
                }, 1000);// 延迟一秒在发送挂断消息，防止当我们离线时，对方发起通话之后又取消了通话，我们30秒内上线，在来点界面拉起时该Event也发送出去了
            } else if (type == XmppMessage.TYPE_END_CONNECT_VIDEO) {
                chatMessage.setMySend(false);
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage);
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, MyApplication.getContext().getString(R.string.finished) + MyApplication.getContext().getString(R.string.video_call) + "," +
                        MyApplication.getContext().getString(R.string.time_len) + ":" + chatMessage.getTimeLen() + MyApplication.getContext().getString(R.string.second), chatMessage);
                EventBus.getDefault().post(new MessageHangUpPhone(chatMessage));
            }

            /**
             群组 音视频会议邀请
             */
            if (type == XmppMessage.TYPE_IS_MU_CONNECT_VOICE) {
                Log.e("AVI", TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() + "");
                if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() <= 30 * 1000) {// 当前时间与对方发送邀请的时间间隔在30s以内
                    EventBus.getDefault().post(new MessageEventMeetingInvited(CallConstants.Audio_Meet, chatMessage));
                } else {
                    Log.e("AVI", "离线消息");
                }
                // 音视频会议消息不保存
/*
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                    FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, chatMessage.getContent(), XmppMessage.TYPE_IS_MU_CONNECT_VOICE, chatMessage.getTimeSend());
                }
*/
            } else if (type == XmppMessage.TYPE_IS_MU_CONNECT_VIDEO) {
                Log.e("AVI", TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() + "");
                if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() <= 30 * 1000) {// 当前时间与对方发送邀请的时间间隔在30s以内
                    EventBus.getDefault().post(new MessageEventMeetingInvited(CallConstants.Video_Meet, chatMessage));
                } else {
                    Log.e("AVI", "离线消息");
                }
                // 音视频会议消息不保存
/*
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                    FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, chatMessage.getContent(), XmppMessage.TYPE_IS_MU_CONNECT_Video, chatMessage.getTimeSend());
                }
*/
            } else if (type == XmppMessage.TYPE_IS_MU_CONNECT_TALK) {
                Log.e("AVI", TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() + "");
                if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() <= 30 * 1000) {// 当前时间与对方发送邀请的时间间隔在30s以内
                    EventBus.getDefault().post(new MessageEventMeetingInvited(CallConstants.Talk_Meet, chatMessage));
                } else {
                    Log.e("AVI", "离线消息");
                }
                // 音视频会议消息不保存
/*
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, fromUserId, chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, fromUserId, chatMessage, false);
                    FriendDao.getInstance().updateFriendContent(mLoginUserId, fromUserId, chatMessage.getContent(), XmppMessage.TYPE_IS_MU_CONNECT_Video, chatMessage.getTimeSend());
                }
*/
            }
        }
    }
}
