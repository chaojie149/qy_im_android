package com.tongxin.caihong.xmpp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.audio.NoticeVoicePlayer;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.MessageListItem;
import com.tongxin.caihong.bean.MsgRoamTask;
import com.tongxin.caihong.bean.OfflineReadPersons;
import com.tongxin.caihong.bean.SyncBean;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.MessageEventBG;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.LastChatHistoryList;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.pay.chat.PayActivity;
import com.tongxin.caihong.socket.EMConnectionManager;
import com.tongxin.caihong.socket.msg.ExitGroupMessage;
import com.tongxin.caihong.socket.msg.JoinGroupMessage;
import com.tongxin.caihong.socket.msg.MessageHead;
import com.tongxin.caihong.socket.msg.PullBatchGroupMessage;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.HandleSyncMoreLogin;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.xmpp.listener.AuthStateListener;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.fanjun.keeplive.service.LocalService;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.Call;

public class CoreService extends Service {
    public static final Map<String, Integer> unReadNumberMap = new HashMap<>();
    static final boolean DEBUG = true;
    static final String TAG = "XmppCoreService";
    private static final Intent SERVICE_INTENT = new Intent();
    private static final String EXTRA_LOGIN_USER_ID = "login_user_id";
    private static final String EXTRA_LOGIN_PASSWORD = "login_password";
    private static final String EXTRA_LOGIN_NICK_NAME = "login_nick_name";
    private static final String MESSAGE_CHANNEL_ID = "20200302";
    private static final String MESSAGE_AUDIO_CHANNEL_ID = "20200408";
    private static final String MESSAGE_LOCALE_CHANNEL_ID = "20200410";

    static {
        SERVICE_INTENT.setComponent(new ComponentName(BuildConfig.APPLICATION_ID, CoreService.class.getName()));
    }

    private CoreServiceBinder mBinder;
    private boolean isInit;
    /* 当前登陆用户的基本属性 */
    private String mLoginUserId;
    @SuppressWarnings("unused")
    private String mLoginNickName;

    private XmppConnectionManager mConnectionManager;// 唯一
    private ReceiptManager mReceiptManager;// 唯一

    /**
     * 本地 发送 通知 至 通知栏
     */
    private int notifyId = 1003020303;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    /**
     * 发送 已读 消息
     */
    private ReadBroadcastReceiver receiver = new ReadBroadcastReceiver();

    private NotifyConnectionListener mNotifyConnectionListener = new NotifyConnectionListener() {
        @Override
        public void notifyConnecting() {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ING);
        }

        @Override
        public void notifyConnected() {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ING);
        }

        @Override
        public void notifyAuthenticated() {
            SeqNoManager.getInstance().checkThreadStart();
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_SUCCESS);// 通知登陆成功
            authenticatedOperating();
        }

        @Override
        public void notifyConnectionClosed() {
            SeqNoManager.getInstance().checkThreadEnd();
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_CLOSE);
        }

        @Override
        public void notifyConnectionClosedOnError(String arg0) {
            SeqNoManager.getInstance().checkThreadEnd();
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ERROR);
        }
    };

    public static Intent getIntent() {
        return SERVICE_INTENT;
    }

    // 要用ContextCompat.startForegroundService启动，否则安卓8.0以上可能崩溃，而且是不一定复现的那种，
    public static Intent getIntent(Context context, String userId, String password, String nickName) {
        Intent intent = new Intent(context, CoreService.class);
        intent.putExtra(EXTRA_LOGIN_USER_ID, userId);
        intent.putExtra(EXTRA_LOGIN_PASSWORD, password);
        intent.putExtra(EXTRA_LOGIN_NICK_NAME, nickName);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new CoreServiceBinder();
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService OnCreate :" + android.os.Process.myPid());
        }
        register(); // 注册发送已读消息的广播监听
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 绑定服务只是为了提供一些外部调用的方法
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onBind");
        }
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onStartCommand");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationBuilder();
            // 用代优先级的channel弹这个startForeground通知，避免出现顶部通知，
            LocalService.createChannel(this);
            Notification notification = LocalService.createNotification(this);
            startForeground(1, notification);
            stopForeground(true);
        }

        init();

        return START_STICKY;
    }

    public void login() {
        mConnectionManager.login();
    }

    public void initConnection() {
        mConnectionManager = new XmppConnectionManager(this, mNotifyConnectionListener);
        mReceiptManager = new ReceiptManager(this, mConnectionManager.getConnection());
        mConnectionManager.addMessageListener();
    }

    private void release() {
        if (mConnectionManager != null) {
            mConnectionManager.release();
            mConnectionManager = null;
        }
    }

    private void init() {
        if (isInit) {
            login();
            return;
        }
        isInit = true;
        User self = CoreManager.requireSelf(this);
        mLoginUserId = self.getUserId();
        mLoginNickName = self.getNickName();

        if (mConnectionManager != null) {
            release();
        }

        if (mConnectionManager == null) {
            initConnection();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onDestroy");
        }

        release();

        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    /**
     * 获得XmppConnectionManager对象
     */
    public XmppConnectionManager getmConnectionManager() {
        return mConnectionManager;
    }

    public boolean isAuthenticated() {
        if (mConnectionManager != null && mConnectionManager.isAuthenticated()) {
            return true;
        }
        return false;
    }

    public void logout() {
        isInit = false;
        if (CoreService.DEBUG)
            Log.e(CoreService.TAG, "Xmpp登出");
        if (mConnectionManager != null) {
            mConnectionManager.logout();
        }
        stopSelf();
    }

    // 发送忙线消息
    public void sendBusyMessage(String toUserId, int type) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_IS_BUSY);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(toUserId);

        chatMessage.setObjectId(String.valueOf(type));
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(toUserId, chatMessage);
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, toUserId, chatMessage)) {
            ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, toUserId, chatMessage, false);
        }
    }

    public void sendMucRead(String roomJid, String fromUserId, String messageId) {
        if (TextUtils.isEmpty(messageId)) {
            return;
        }
        if (mConnectionManager != null) {
            mConnectionManager.addRead(new ReadManager.MucReadItem(roomJid, fromUserId, messageId));
        } else {
            Reporter.post("初始化异常，已读管理器为空");
        }
    }

    /**
     * 发送新的朋友消息
     */
    public void sendNewFriendMessage(String toUserId, NewFriendMessage message) {
        if (!isAuthenticated() || mReceiptManager == null) {
            ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, message, ChatMessageListener.MESSAGE_SEND_FAILED);
            return;
        }

        ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, message, ChatMessageListener.MESSAGE_SEND_ING);
        ChatMessage chatMessage = message.toChatMessage();
        chatMessage.setToUserId(toUserId);
        mReceiptManager.addWillSendMessage(chatMessage);
        mConnectionManager.sendMessage(com.tongxin.caihong.socket.msg.ChatMessage.toSocketMessage(mLoginUserId, chatMessage, true));
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(String toUserId, ChatMessage chatMessage) {
        if (mReceiptManager == null
                || (!isAuthenticated() && !HttpUtil.isGprsOrWifiConnected(MyApplication.getContext()))) {
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(), ChatMessageListener.MESSAGE_SEND_FAILED);
            return;
        }

        /**
         * 先添加一个等待接收回执的消息
         * 然后再发送这条消息
         */
        chatMessage.setToUserId(toUserId);
        mReceiptManager.addWillSendMessage(chatMessage);
        mConnectionManager.sendMessage(com.tongxin.caihong.socket.msg.ChatMessage.toSocketMessage(mLoginUserId, chatMessage, false));
    }

    public void sendMucChatMessage(String toUserId, ChatMessage chatMessage) {
        if (mReceiptManager == null
                || (!isAuthenticated() && !HttpUtil.isGprsOrWifiConnected(MyApplication.getContext()))) {
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(), ChatMessageListener.MESSAGE_SEND_FAILED);
            return;
        }

        chatMessage.setToUserId(toUserId);
        chatMessage.setGroup(true);
        mReceiptManager.addWillSendMessage(chatMessage);
        mConnectionManager.sendMessage(com.tongxin.caihong.socket.msg.ChatMessage.toSocketMessage(mLoginUserId, chatMessage, false));
    }

    /* 批量加入群组 */
    public void batchJoinMucChat() {
        if (mConnectionManager == null) {
            // 可能http回调后已经掉线CoreService已经释放了，就不继续了，
            return;
        }
        List<String> jidList = new ArrayList<>();
        long lastSeconds;
        long offlineTime = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);// 离线时间存的为秒，需要毫秒
        if (offlineTime == 0) {
            lastSeconds = 1546272000000L;// 2019年1月1日
        } else {
            lastSeconds = offlineTime * 1000;
        }

        List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);// 获取本地所有群组
        if (friends != null && friends.size() > 0) {
            for (int i = 0; i < friends.size(); i++) {
                Friend friend = friends.get(i);
                // 群组状态正常才去获取离线消息
                if (friend.getMaxSeqNo() > 0) {
                    // 如果该群组的maxSeqNo不为空，将friend.getMaxSeqNo()作为拉取条件
                    jidList.add(friend.getUserId() + "," + 0 + "," + friend.getMaxSeqNo());
                } else {
                    // 该群组本地无消息记录，取全局的离线时间
                    jidList.add(friend.getUserId() + "," + lastSeconds + "," + 0);
                }
            }
        }
        if (jidList.isEmpty()) {
            // 空数组就不发了，
            return;
        }
        PullBatchGroupMessage pullBatchGroupMessage = new PullBatchGroupMessage();
        pullBatchGroupMessage.setJidList(jidList);
        pullBatchGroupMessage.setEndTime(TimeUtils.sk_time_current_time());

        MessageHead head = new MessageHead();
        head.setMessageId(UUID.randomUUID().toString().replaceAll("-", ""));
        head.setFrom(mLoginUserId + "/" + EMConnectionManager.CURRENT_DEVICE);
        head.setTo("service");
        head.setChatType((byte) 2);
        pullBatchGroupMessage.setMessageHead(head);
        if (mConnectionManager != null) {
            mConnectionManager.getConnection().batchJoinRoom(pullBatchGroupMessage);
        }
    }

    /* 创建群聊 */
    public String createMucRoom(String roomName) {
        String roomJid = UUID.randomUUID().toString().replaceAll("-", "");
        joinMucChat(roomJid, 0);
        return roomJid;
    }

    public String joinMucChat(String roomJid, long joinSeqNo) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGroup(true);
        chatMessage.setContent(roomJid);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        chatMessage.setType(XmppMessage.TYPE_JOIN_ROOM);
        mReceiptManager.addWillSendMessage(chatMessage);

        JoinGroupMessage join = new JoinGroupMessage();
        join.setJid(roomJid);
        join.setSeconds(0);

        MessageHead head = new MessageHead();
        head.setMessageId(chatMessage.getPacketId());
        head.setFrom(mLoginUserId + "/" + EMConnectionManager.CURRENT_DEVICE);
        head.setTo("service");
        head.setChatType((byte) 2);
        join.setMessageHead(head);
        if (mConnectionManager != null) {
            mConnectionManager.getConnection().joinRoom(join);
        }
        return roomJid;
    }

    /**
     * @param roomJid
     * @param seconds 时间段，毫秒(为获取当前时间-seconds这个时间段内的消息)
     * @return
     */
    public String joinMucChat2(String roomJid, long seconds) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGroup(true);
        chatMessage.setContent(roomJid);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        chatMessage.setType(XmppMessage.TYPE_JOIN_ROOM);
        mReceiptManager.addWillSendMessage(chatMessage);

        JoinGroupMessage join = new JoinGroupMessage();
        join.setJid(roomJid);
        join.setSeconds(seconds);

        MessageHead head = new MessageHead();
        head.setMessageId(chatMessage.getPacketId());
        head.setFrom(mLoginUserId + "/" + EMConnectionManager.CURRENT_DEVICE);
        head.setTo("service");
        head.setChatType((byte) 2);
        join.setMessageHead(head);
        if (mConnectionManager != null) {
            mConnectionManager.getConnection().joinRoom(join);
        }
        return roomJid;
    }

    /* 退出群聊 */
    public void exitMucChat(String roomJid) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGroup(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        chatMessage.setContent(roomJid);
        chatMessage.setTimeSend(0); // 新建群没有历史消息
        chatMessage.setType(XmppMessage.TYPE_EXIT_ROOM);
        mReceiptManager.addWillSendMessage(chatMessage);

        ExitGroupMessage exit = new ExitGroupMessage();
        exit.setJid(roomJid);

        MessageHead head = new MessageHead();
        head.setMessageId(chatMessage.getPacketId());
        head.setFrom(mLoginUserId + "/" + EMConnectionManager.CURRENT_DEVICE);
        head.setTo("service");
        head.setChatType((byte) 2);

        exit.setMessageHead(head);
        if (mConnectionManager != null) {
            mConnectionManager.getConnection().exitRoom(exit);
        }
    }

    /********************************************************************************
     *  其他操作
     ********************************************************************************/
    /*
    XMPP认证后需要做的操作
    */
    public void authenticatedOperating() {
        Log.e("zq", "认证之后需要调用的操作");
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 删除本地已过期的消息
                List<Friend> nearlyFriendMsg = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
                for (int i = 0; i < nearlyFriendMsg.size(); i++) {
                    if (nearlyFriendMsg.get(i).getRoomFlag() == 0) {// 单聊可删除
                        ChatMessageDao.getInstance().deleteOutTimeChatMessage(mLoginUserId, nearlyFriendMsg.get(i).getUserId());
                    } else {// 群聊修改字段
                        ChatMessageDao.getInstance().updateExpiredStatus(mLoginUserId, nearlyFriendMsg.get(i).getUserId());
                    }
                }
            }
        }).start();

        MessageEventBG.isAuthenticated = true;
        // 从服务端获取与其它好友 || 群组内最后一条聊天消息列表(单聊：我在其他端的产生的聊天记录 群聊：离线消息大于100条时，之前的数据)
        getLastChatHistory();
        getInterfaceTransferInOfflineTime();
    }

    public void getInterfaceTransferInOfflineTime() {
        long syncTimeLen = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);

        Map<String, String> params = new HashMap();
        params.put("access_token", CoreManager.requireSelfStatus(this).accessToken);
        params.put("offlineTime", String.valueOf(syncTimeLen));

        HttpUtils.get().url(CoreManager.requireConfig(this).USER_OFFLINE_OPERATION)
                .params(params)
                .build()
                .execute(new ListCallback<SyncBean>(SyncBean.class) {
                    @Override
                    public void onResponse(ArrayResult<SyncBean> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<SyncBean> syncBeans = result.getData();
                            for (int i = 0; i < syncBeans.size(); i++) {
                                HandleSyncMoreLogin.distributionService(syncBeans.get(i), CoreService.this);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    public void getLastChatHistory() {
        Map<String, String> params = new HashMap();
        params.put("access_token", CoreManager.requireSelfStatus(this).accessToken);

        long offlineTime = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);
        long syncTimeLen;

        boolean isUsed = PreferenceUtils.getBoolean(MyApplication.getContext(), Constants.OFFLINE_TIME_SINGLE_USE + mLoginUserId, false);
        if (!isUsed) {
            // 当前用户首次登录，读取消息漫游时长，
            // 使用完之后就标记为true
            PreferenceUtils.putBoolean(MyApplication.getContext(), Constants.OFFLINE_TIME_SINGLE_USE + mLoginUserId, true);
            String chatSyncTimeLen = String.valueOf(PrivacySettingHelper.getPrivacySettings(this).getChatSyncTimeLen());
            Double realSyncTime = Double.parseDouble(chatSyncTimeLen);
            if (realSyncTime == -2) {// 不同步
                SeqNoManager.getInstance().releaseSingleTask();
                batchJoinMucChat();
                //  joinExistGroup();
                return;
            } else if (realSyncTime == -1 || realSyncTime == 0) {// 同步 永久 syncTime == 0
                // 同步 永久 syncTime == 0
                syncTimeLen = 0;
            } else {
                // 这里变量名有点混乱，syncTimeLen应该是startTime, 是个时间点，不是时间段，
                // kx当前时间要除以一千，因为整块代码时间单位都是秒，
                syncTimeLen = TimeUtils.sk_time_current_time() / 1000 - (long) (realSyncTime * 24 * 60 * 60);// 得到消息同步时长
            }
        } else {// syncTime为上一次本地保存的离线时间
            syncTimeLen = offlineTime;
        }
        params.put("startTime", String.valueOf(syncTimeLen * 1000));

        getReadPersons(syncTimeLen);

        HttpUtils.get().url(CoreManager.requireConfig(this).GET_LAST_CHAT_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<LastChatHistoryList>(LastChatHistoryList.class) {
                    @Override
                    public void onResponse(ArrayResult<LastChatHistoryList> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final List<LastChatHistoryList> data = result.getData();
                                    // 单聊列表
                                    List<LastChatHistoryList> singleChatList = new ArrayList<>();
                                    for (int i = 0; i < data.size(); i++) {
                                        LastChatHistoryList mLastChatHistoryList = data.get(i);
                                        if (mLastChatHistoryList.getType() == XmppMessage.TYPE_SECURE_REFRESH_KEY) {
                                            // 803消息额外处理过滤，服务器和客服端都处理掉，以防万一，
                                            continue;
                                        }
                                        if (mLastChatHistoryList.getIsRoom() != 1) {
                                            singleChatList.add(mLastChatHistoryList);
                                        }
                                        String str;
                                        if (mLastChatHistoryList.getEncryptType() != 0) {
                                            mLastChatHistoryList.setIsEncrypt(mLastChatHistoryList.getEncryptType());
                                        }
                                        if (mLastChatHistoryList.getIsEncrypt() != 0
                                                && !TextUtils.isEmpty(mLastChatHistoryList.getContent())) {// 需要解密
                                            str = ChatMessageDao.getInstance().decrypt(mLastChatHistoryList.getIsRoom() == 1,
                                                    mLastChatHistoryList.getJid(), mLastChatHistoryList.getContent(), mLastChatHistoryList.getIsEncrypt(),
                                                    mLastChatHistoryList.getMessageId(), mLastChatHistoryList.getTimeSend());
                                        } else {
                                            str = mLastChatHistoryList.getContent();
                                        }

                                        FriendDao.getInstance().updateApartDownloadTime(mLastChatHistoryList.getUserId(), mLastChatHistoryList.getJid(),
                                                str, mLastChatHistoryList.getType(), mLastChatHistoryList.getTimeSend(),
                                                mLastChatHistoryList.getIsRoom(), mLastChatHistoryList.getFrom(), mLastChatHistoryList.getFromUserName(),
                                                mLastChatHistoryList.getToUserName());
                                    }

                                    for (int i = 0; i < singleChatList.size(); i++) {
                                        LastChatHistoryList chatHistory = singleChatList.get(i);
                                        if (SeqNoManager.getInstance().waitGetLasRequestResponseReceivedMsg.containsKey(chatHistory.getJid())) {
                                            Log.e(SeqNoManager.TAG, "getLast接口调用完毕，在此期间收到过 " + chatHistory.getJid() + " 的消息，不处理");
                                            continue;
                                        }
                                        Log.e(SeqNoManager.TAG, "getLast接口调用完毕，在此期间没有收到过 " + chatHistory.getJid() + " 的消息，比较seqNo是否有差值");
                                        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatHistory.getJid());
                                        // 服务端有可能返回已删除好友过来，需要做非空判断
                                        if (friend != null && friend.getMaxSeqNo() == 0) {
                                            Log.e(SeqNoManager.TAG, "friend.getMaxSeqNo() == 0，本地无消息，不处理");
                                        } else if (friend != null && chatHistory.getSeqNo() - friend.getMaxSeqNo() > 0) {
                                            Log.e(SeqNoManager.TAG, "序号差值为-->" + (chatHistory.getSeqNo() - friend.getMaxSeqNo())
                                                    + "，检查本地是否有startSeqNo== " + friend.getMaxSeqNo() + " 的任务");
                                            List<MsgRoamTask> tasks = MsgRoamTaskDao.getInstance().getFriendMsgRoamTaskList(mLoginUserId, chatHistory.getJid());
                                            if (tasks.size() > 0) {
                                                for (MsgRoamTask task : tasks) {
                                                    if (task.getStartSeqNo() == friend.getMaxSeqNo()) {
                                                        Log.e(SeqNoManager.TAG, "一致，删掉任务id为-->" + task.getTaskId() + " 的任务");
                                                        MsgRoamTaskDao.getInstance().deleteMsgRoamTask(mLoginUserId, chatHistory.getJid(), task.getTaskId());
                                                    }
                                                }
                                            }
                                            MsgRoamTask msgRoamTask = new MsgRoamTask();
                                            msgRoamTask.setTaskId(System.currentTimeMillis());
                                            msgRoamTask.setOwnerId(mLoginUserId);
                                            msgRoamTask.setUserId(chatHistory.getJid());
                                            msgRoamTask.setStartSeqNo(friend.getMaxSeqNo());
                                            msgRoamTask.setEndSeqNo(chatHistory.getSeqNo() + 1);// getLast得到的seqNo本地也是没有的
                                            if (SeqNoManager.getInstance().waitGetLasRequestResponseReceivedMsg.containsKey(chatHistory.getJid())) {
                                                Log.e(SeqNoManager.TAG, "在准备调用createMsgRoamTask方法时，在判断一下，因为可能在上面代码检查的过程中又收到了离线消息，" +
                                                        "getLast接口调用完毕，在此期间收到过 " + chatHistory.getJid() + " 的消息，不处理");
                                                continue;
                                            }
                                            MsgRoamTaskDao.getInstance().createMsgRoamTask(msgRoamTask);
                                            Log.e(SeqNoManager.TAG, "任务已存入任务表-->" + msgRoamTask.getStartSeqNo() + " - " + msgRoamTask.getEndSeqNo());

                                            /**
                                             * getLast接口回调内创建任务时，将发送方id+任务endSeqNo-1作为key，当前任务作为value放入内存map内
                                             * 这个map的主要作用为针对有离线消息时，getLast接口优先处理完且已经生成了任务，并在此之后才收到离线消息的场景，
                                             */
                                            SeqNoManager.getInstance().getLastCallbackCreatedTask.put(chatHistory.getJid() + chatHistory.getSeqNo(), msgRoamTask);
                                            Log.e(SeqNoManager.TAG, "getLast接口回调内创建的任务，将任务存入map内，key-->" + chatHistory.getJid() + chatHistory.getSeqNo());
                                        }
                                    }

                                    // 以上任务生成之后，在通知XMPP加入群组 获取群组离线消息
                                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                                    SeqNoManager.getInstance().releaseSingleTask();
                                    batchJoinMucChat();
                                    //  joinExistGroup();
                                }
                            }).start();
                        } else {// 数据异常，也需要调用XMPP加入群组
                            SeqNoManager.getInstance().releaseSingleTask();
                            batchJoinMucChat();
                            //  joinExistGroup();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // 同上
                        SeqNoManager.getInstance().releaseSingleTask();
                        batchJoinMucChat();
                        //  joinExistGroup();
                    }
                });
    }

    // 现在加入了群组分页漫游，群组的离线消息不能立即获取，必须要等到'tigase/getLastChatList'接口调用完毕后在加入群组，获取离线消息记录
/*
    public void joinExistGroup() {
        // 先获取全局的离线-->上线 这个时间段的时间
        long lastSeconds;
        long offlineTime = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);// 离线时间存的为秒，需要毫秒
        if (offlineTime == 0) {
            lastSeconds = 1546272000000l;// 2019年1月1日
        } else {
            lastSeconds = offlineTime * 1000;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

        List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);// 获取本地所有群组
        if (friends != null && friends.size() > 0) {
            for (int i = 0; i < friends.size(); i++) {
                Friend friend = friends.get(i);
                if (friend.getGroupStatus() == 0) {// 群组状态正常才去获取离线消息
                    AsyncUtils.doAsync(this, e -> {
                        Reporter.post("加入群组出异常，", e);
                    }, executorService, c -> {
                        ChatMessage mLastChatMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, friend.getUserId());
                        if (mLastChatMessage != null) {// 如果该群组的最后一条消息不为空，将该条消息的timeSend作为当前群组的离线时间，这样比上面全局的离线时间更加准确
                            long lastMessageTimeSend = mLastChatMessage.getTimeSend();
                            joinMucChat(friend.getUserId(), lastMessageTimeSend - 3);
                        } else {// 该群组本地无消息记录，取全局的离线时间
                            joinMucChat(friend.getUserId(), lastSeconds);
                        }
                    });
                }
            }
        }
    }
*/

    private void getReadPersons(long syncTimeLen) {
        Map<String, String> params = new HashMap<>();
        params.put("lastTime", String.valueOf(syncTimeLen));

        HttpUtils.get().url(CoreManager.requireConfig(this).GET_READ_PERSONS)
                .params(params)
                .build()
                .execute(new ListCallback<OfflineReadPersons>(OfflineReadPersons.class) {
                    @Override
                    public void onResponse(ArrayResult<OfflineReadPersons> result) {
                        if (Result.checkSuccess(CoreService.this, result)) {
                            List<OfflineReadPersons> data = result.getData();
                            if (data == null) {
                                return;
                            }
                            for (OfflineReadPersons readPersons : data) {
                                String roomJid = readPersons.getRoomJid();
                                for (MessageListItem item : readPersons.getMessageList()) {
                                    String packetId = item.getMessageId();
                                    ChatMessageDao.getInstance().setMessageRead(mLoginUserId, roomJid, packetId, item.getCount());
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(CoreService.this);
                    }
                });
    }

    /*
    发送本地通知
     */
    // 这个方法有被异步线程调用，
    @WorkerThread
    public void notificationMessage(ChatMessage chatMessage, boolean isGroupChat) {
        boolean isAppForeground = AppUtils.isAppForeground(this);
        Log.e(TAG, "notificationMessage() called with: chatMessage = [" + chatMessage.getContent() + "], isGroupChat = [" + isGroupChat + "], isAppForeground = [" + isAppForeground + "]");

        if (isAppForeground) {// 在前台 不通知
            return;
        }

        int messageType = chatMessage.getType();
        String title;
        String content;
        boolean isSpecialMsg = false;// 特殊消息 跳转至主界面 而非聊天界面
        boolean isBell = false;
        // 群会议邀请，处理的是单聊消息但是该打开群聊才对，
        boolean isMeet = false;

        switch (messageType) {
            case XmppMessage.TYPE_REPLAY:
            case XmppMessage.TYPE_TEXT:
                if (chatMessage.getIsReadDel()) {
                    if (TextUtils.equals(chatMessage.getFromUserId()
                            , CoreManager.requireSelf(MyApplication.getContext()).getUserId())) {
                        content = getString(R.string.tip_read_fire_msg);
                    } else {
                        content = getString(R.string.tip_click_to_read);
                    }
                } else {
                    content = chatMessage.getContent();
                }
                break;
            case XmppMessage.TYPE_REQUEST_COMMENT:
                content = "[" + getString(R.string.service_title_grade) + "]";
                break;
            case XmppMessage.TYPE_SERVICE_MENU:
                content = "[" + getString(R.string.service_menu) + "]";
                break;
            case XmppMessage.TYPE_VOICE:
                content = getString(R.string.msg_voice);
                break;
            case XmppMessage.TYPE_GIF:
                content = getString(R.string.msg_animation);
                break;
            case XmppMessage.TYPE_IMAGE:
                content = getString(R.string.msg_picture);
                break;
            case XmppMessage.TYPE_VIDEO:
                content = getString(R.string.msg_video);
                break;
            case XmppMessage.TYPE_RED:
                content = getString(R.string.msg_red_packet);
                break;
            case XmppMessage.TYPE_LOCATION:
                content = getString(R.string.msg_location);
                break;
            case XmppMessage.TYPE_CARD:
                content = getString(R.string.msg_card);
                break;
            case XmppMessage.TYPE_FILE:
                content = getString(R.string.msg_file);
                break;
            case XmppMessage.TYPE_TIP:
                content = getString(R.string.msg_system);
                break;
            case XmppMessage.TYPE_IMAGE_TEXT:
            case XmppMessage.TYPE_IMAGE_TEXT_HTML:
            case XmppMessage.TYPE_IMAGE_TEXT_MANY:
                content = getString(R.string.msg_image_text);
                break;
            case XmppMessage.TYPE_LINK:
            case XmppMessage.TYPE_SHARE_LINK:
                content = getString(R.string.msg_link);
                break;
            case XmppMessage.TYPE_SHAKE:
                content = getString(R.string.msg_shake);
                break;
            case XmppMessage.TYPE_DICE:
            case XmppMessage.TYPE_RPS:
                content = getString(R.string.type_emoij);
                break;
            case XmppMessage.TYPE_MEETING_INVITE:
                content = getString(R.string.type_meeting_invite);
                break;
            case XmppMessage.TYPE_LIVE_INVITE:
                content = getString(R.string.type_live_invite);
                break;
            case XmppMessage.TYPE_INVITE_JOIN_ROOM:
                content = getString(R.string.type_invite_join_room);
                break;
            case XmppMessage.TYPE_COMPANY_INVITE_JOIN:
                content = getString(R.string.type_invite_join_company);
                break;
            case XmppMessage.TYPE_APPLET:
                content = getString(R.string.type_applet);
                break;
            case XmppMessage.TYPE_SHARE:
                content = StringUtils.getShareContent(chatMessage.getContent());
                break;
            case XmppMessage.TYPE_CHAT_HISTORY:
                content = getString(R.string.msg_chat_history);
                break;
            case XmppMessage.TYPE_TRANSFER:
                content = getString(R.string.tip_transfer_money);
                break;
            case XmppMessage.TYPE_TRANSFER_RECEIVE:
                content = getString(R.string.tip_transfer_money) + getString(R.string.transfer_friend_sure_save);
                break;
            case XmppMessage.TYPE_TRANSFER_BACK:
                content = getString(R.string.transfer_back);
                break;
            case XmppMessage.TYPE_PAY_CERTIFICATE:
                content = getString(R.string.pay_certificate);
                break;
            case XmppMessage.TYPE_SCAN_RECHARGE:
                content = getString(R.string.scan_recharge_notify);
                break;
            case XmppMessage.TYPE_SCAN_WITHDRAW:
                content = getString(R.string.scan_withdraw_notify);
                break;
            case XmppMessage.TYPE_SECURE_LOST_KEY:
                content = getString(R.string.request_chat_key_group_thumb);
                break;
            case XmppMessage.TYPE_NEW_NOTICE:
                content = "[" + getString(R.string.group_bulletin) + "]";
                break;

            case XmppMessage.TYPE_IS_CONNECT_VOICE:
                content = getString(R.string.suffix_invite_you_voice);
                isBell = true;
                break;
            case XmppMessage.TYPE_IS_CONNECT_VIDEO:
                content = getString(R.string.suffix_invite_you_video);
                isBell = true;
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                isMeet = true;
                content = getString(R.string.suffix_invite_you_voice_meeting);
                isBell = true;
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_LIVE:
                isMeet = true;
                content = getString(R.string.suffix_invite_you_live);
                isBell = true;
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VIDEO:
                isMeet = true;
                content = getString(R.string.suffix_invite_you_video_meeting);
                isBell = true;
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_TALK:
                isMeet = true;
                content = getString(R.string.suffix_invite_you_talk);
                isBell = true;
                break;

            case XmppMessage.TYPE_SAYHELLO:// 打招呼
                isSpecialMsg = true;
                content = getString(R.string.apply_to_add_me_as_a_friend);
                break;
            case XmppMessage.TYPE_PASS:    // 同意加好友
                isSpecialMsg = true;
                content = getString(R.string.agree_with_my_plus_friend_request);
                break;
            case XmppMessage.TYPE_FRIEND:  // 直接成为好友
                isSpecialMsg = true;
                content = getString(R.string.added_me_as_a_friend);
                break;

            case XmppMessage.DIANZAN:// 朋友圈点赞
                isSpecialMsg = true;
                content = getString(R.string.notification_praise_me_life_circle);
                break;
            case XmppMessage.PINGLUN:    // 朋友圈评论
                isSpecialMsg = true;
                content = getString(R.string.notification_comment_me_life_circle);
                break;
            case XmppMessage.ATMESEE:  // 朋友圈提醒我看
                isSpecialMsg = true;
                content = getString(R.string.notification_at_me_life_circle);
                break;
            case XmppMessage.FRIEND_PUBLISH:  // 朋友圈好友动态更新
                isSpecialMsg = true;
                content = getString(R.string.notification_refresh_life_circle);
                break;

            default:// 其他消息类型不通知
                return;
        }

        createNotificationBuilder();

        String id;
        PendingIntent pendingIntent;
        if (isSpecialMsg) {
            title = chatMessage.getFromUserName();
            content = chatMessage.getFromUserName() + content;
            pendingIntent = pendingIntentForSpecial();
        } else {
            if (isMeet) {
                id = chatMessage.getObjectId();
            } else if (isGroupChat) {
                id = chatMessage.getToUserId();
                content = chatMessage.getFromUserName() + "：" + content;// 群组消息通知需要带上消息发送方的名字
            } else {
                id = chatMessage.getFromUserId();
            }

            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, id);
            if (friend != null) {
                title = TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
            } else {
                title = chatMessage.getFromUserName();
            }

            if (isGroupChat || isMeet) {
                pendingIntent = pendingIntentForMuc(friend);
            } else {
                pendingIntent = pendingIntentForSingle(friend);
            }

        }
        if (pendingIntent == null)
            return;

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(title) // 通知标题
                .setContentText(content)  // 通知内容
                .setTicker(getString(R.string.tip_new_message))
                .setWhen(System.currentTimeMillis()) // 通知时间
                .setPriority(Notification.PRIORITY_HIGH) // 通知优先级
                .setAutoCancel(true)// 当用户单击面板就可以让通知自动取消
                .setOngoing(false)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.mipmap.icon); // 通知icon
        Notification n = mBuilder.build();
        String userId;
        if (isGroupChat) {
            userId = chatMessage.getToUserId();
        } else {
            userId = chatMessage.getFromUserId();
        }
/*
         miui角标数量是计在这个通知上的，
         这里仿wx的逻辑，第一个通知带上所有角标数量，之后的其他用户的通知只带自己的角标数量
         具体逻辑是，保存每一个userId弹通知时带的数字，下次只是简单加一，不查数据库，
         第一个通知也就是map为空的情况，查所有未读数，
         在主页取消通知的位置重置map,
*/
        int numMessage;
        if (unReadNumberMap.isEmpty()) {
            // 先通知后保存的数据库，所以数据库里读出来的未读消息数要加1，
            numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mLoginUserId) + 1;
        } else {
            Integer lastNumber = unReadNumberMap.get(userId);
            if (lastNumber == null) {
                lastNumber = 0;
            }
            numMessage = lastNumber + 1;
        }
        unReadNumberMap.put(userId, numMessage);
        ShortcutBadger.applyNotification(getApplicationContext(), n, numMessage);
        mNotificationManager.notify(userId, notifyId, n);
        if (isSpecialMsg) {// 特殊消息响铃通知
            NoticeVoicePlayer.getInstance().start();
        } else if (isBell) {
            NoticeVoicePlayer.getInstance().start(true);
        }
    }

    private void createNotificationBuilder() {
        // 同步锁防止线程冲突，大量消息通知时可能需要，
        if (mNotificationManager == null) {
            synchronized (this) {
                if (mNotificationManager == null) {
                    mNotificationManager = (NotificationManager) getApplicationContext()
                            .getSystemService(NOTIFICATION_SERVICE);
                }
            }
        }
        if (mBuilder == null) {
            synchronized (this) {
                if (mBuilder == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        List<NotificationChannel> channels = new ArrayList<>();
                        // 消息channel，用于普通消息通知(第三方推送使用)
                        NotificationChannel channel = new NotificationChannel(
                                MESSAGE_CHANNEL_ID,
                                getString(R.string.message_channel_name),
                                NotificationManager.IMPORTANCE_DEFAULT);
                        // 音视频channel，用户音视频消息通知(第三方推送使用)
                        NotificationChannel channelAudio = new NotificationChannel(
                                MESSAGE_AUDIO_CHANNEL_ID,
                                getString(R.string.message_audio_channel_name),
                                NotificationManager.IMPORTANCE_HIGH);
                        // 本地通知channel，用于应用置后台且长连接未断开收到消息时，本地通知使用
                        NotificationChannel channelLocal = new NotificationChannel(
                                MESSAGE_LOCALE_CHANNEL_ID,
                                getString(R.string.message_local_channel_name),
                                NotificationManager.IMPORTANCE_HIGH);
                        // 关闭通知铃声，我们有自己播放，
                        channelLocal.setSound(null, null);

                        channels.add(channelAudio);
                        channels.add(channel);
                        channels.add(channelLocal);

                        mNotificationManager.createNotificationChannels(channels);
                        mBuilder = new NotificationCompat.Builder(this, channelLocal.getId()).setSmallIcon(R.mipmap.icon);
                    } else {
                        //noinspection deprecation
                        mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.icon);
                    }
                }
            }
        }
    }

    /**
     * <跳到单人聊天界面>
     */
    public PendingIntent pendingIntentForSingle(Friend friend) {
        Intent intent;
        if (friend != null) {
            if (TextUtils.equals(friend.getUserId(), Friend.ID_SK_PAY)) {
                intent = new Intent(getApplicationContext(), PayActivity.class);
            } else {
                intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra(AppConstant.EXTRA_FRIEND, friend);
            }
        } else {
            intent = new Intent(getApplicationContext(), MainActivity.class);
        }
        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * <跳到群组聊天界面>
     */
    public PendingIntent pendingIntentForMuc(Friend friend) {
        Intent intent;
        if (friend != null) {
            intent = new Intent(getApplicationContext(), MucChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
            intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        } else {
            intent = new Intent(getApplicationContext(), MainActivity.class);
        }
        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * <跳到主界面>
     */
    public PendingIntent pendingIntentForSpecial() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OtherBroadcast.Read);
        registerReceiver(receiver, intentFilter);
    }

    /*
    发送已读消息
     */
    public class ReadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(OtherBroadcast.Read)) {
                Bundle bundle = intent.getExtras();
                String packetId = bundle.getString("packetId");
                boolean isGroup = bundle.getBoolean("isGroup");
                String friendId = bundle.getString("friendId");
                String objectId = bundle.getString("objectId");
                String serviceId = bundle.getString("serviceId");
                String toId = bundle.getString("toId");
                String friendName = bundle.getString("fromUserName");

                ChatMessage msg = new ChatMessage();
                msg.setType(XmppMessage.TYPE_READ);
                msg.setFromUserId(mLoginUserId);
                msg.setFromUserName(friendName);
                msg.setToUserId(friendId);
                if (!TextUtils.isEmpty(toId)) {
                    // resource这里sk用的是toId，这里用的是toUserName,
                    msg.setToUserName(toId);
                }
                msg.setContent(packetId);
                msg.setObjectId(objectId);
                // 发送已读消息 本地置为已读
                msg.setSendRead(true);
                msg.setServiceId(serviceId);
                msg.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                msg.setTimeSend(TimeUtils.sk_time_current_time());
                if (isGroup) {
                    sendMucRead(objectId, friendId, packetId);
                } else {
                    sendChatMessage(friendId, msg);
                }
            }
        }
    }

    // Binder
    public class CoreServiceBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }
}
