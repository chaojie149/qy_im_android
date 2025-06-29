package com.tongxin.caihong.xmpp;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.NewFriendDao;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.xmpp.listener.AuthStateListener;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.xmpp.listener.MucListener;
import com.tongxin.caihong.xmpp.listener.NewFriendListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zq
 * 监听管理，支持添加和移除长连接状态、新的朋友、消息、群组操作监听
 */
public class ListenerManager {
    private static ListenerManager instance;
    /* 回调监听 */
    // 连接状态监听
    private List<AuthStateListener> mAuthStateListeners = new ArrayList<AuthStateListener>();
    // 新的朋友消息监听
    private List<NewFriendListener> mNewFriendListeners = new ArrayList<NewFriendListener>();
    // 聊天消息监听
    private List<ChatMessageListener> mChatMessageListeners = new ArrayList<ChatMessageListener>();
    // 群组操作监听
    private List<MucListener> mMucListeners = new ArrayList<MucListener>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

    private ListenerManager() {
    }

    public static ListenerManager getInstance() {
        if (instance == null) {
            instance = new ListenerManager();
        }
        return instance;
    }

    public void reset() {
        instance = null;
    }

    /**********************
     * 注册和移除监听
     **************************/
    public void addAuthStateChangeListener(AuthStateListener authStateChangeListener) {
        mAuthStateListeners.add(authStateChangeListener);
    }

    public void removeAuthStateChangeListener(AuthStateListener authStateChangeListener) {
        mAuthStateListeners.remove(authStateChangeListener);
    }

    public void addNewFriendListener(NewFriendListener listener) {
        mNewFriendListeners.add(listener);
    }

    public void removeNewFriendListener(NewFriendListener listener) {
        mNewFriendListeners.remove(listener);
    }

    public void addChatMessageListener(ChatMessageListener messageListener) {
        mChatMessageListeners.add(messageListener);
    }

    public void removeChatMessageListener(ChatMessageListener messageListener) {
        mChatMessageListeners.remove(messageListener);
    }

    public void addMucListener(MucListener listener) {
        mMucListeners.add(listener);
    }

    public void removeMucListener(MucListener listener) {
        mMucListeners.remove(listener);
    }

    /**********************
     * 监听回调
     **************************/
    /**
     * 通知连接状态变化【连接状态监听】
     *
     * @param authState 连接状态
     */
    public void notifyAuthStateChange(final int authState) {
        if (mAuthStateListeners.size() <= 0) {
            return;
        }
        mHandler.post(new Runnable() {
            public void run() {
                for (AuthStateListener authStateChangeListener : mAuthStateListeners) {
                    authStateChangeListener.onAuthStateChange(authState);
                }
            }
        });
    }


    /**
     * 通知发送新朋友相关消息的状态变化【新的朋友消息监听】
     *
     * @param toUserId
     * @param message
     * @param messageState 消息发送状态
     */
    public void notifyNewFriendSendStateChange(final String toUserId, final NewFriendMessage message, final int messageState) {
        if (mNewFriendListeners.size() <= 0) {
            return;
        }
        mHandler.post(new Runnable() {
            public void run() {
                for (NewFriendListener listener : mNewFriendListeners) {
                    listener.onNewFriendSendStateChange(toUserId, message, messageState);
                }
            }
        });
    }

    /**
     * 通知收到新的朋友相关消息【新的朋友消息监听】
     *
     * @param loginUserId
     * @param message     消息
     * @param isPreRead
     */
    public void notifyNewFriend(final String loginUserId, final NewFriendMessage message, final boolean isPreRead) {
        mHandler.post(new Runnable() {
            public void run() {
                boolean hasRead = false;// 是否已经被读了 (如果有类添加)
                for (NewFriendListener listener : mNewFriendListeners) {
                    if (listener.onNewFriend(message)) {
                        hasRead = true;
                    }
                }
                if (!hasRead && isPreRead) {
                    Log.e("msg", "新的朋友刷新");
                    int i = NewFriendDao.getInstance().getNewFriendUnRead(message.getOwnerId(), message.getUserId());
                    if (i <= 0) {// 当该新的朋友存在一条未读消息时，不在更新
                        NewFriendDao.getInstance().markNewFriendUnRead(message.getOwnerId(), message.getUserId());
                        FriendDao.getInstance().markUserMessageUnRead(loginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
                    }
                    MsgBroadcast.broadcastMsgNumUpdateNewFriend(MyApplication.getInstance());
                }
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
            }
        });
    }

    /**
     * 通知消息发送状态变化【聊天消息监听】
     *
     * @param mLoginUserId
     * @param toUserId
     * @param msgId        消息id
     * @param messageState 消息发送状态
     */
    public void notifyMessageSendStateChange(String mLoginUserId, String toUserId, final String msgId, final int messageState) {
        if (mLoginUserId.equals(toUserId)) {
            String[] machine = new String[]{"ios", "pc", "mac", "web"};
            for (String device : machine) {
                ChatMessageDao.getInstance().updateMessageState(mLoginUserId, device, msgId, messageState);
            }
        } else {
            ChatMessageDao.getInstance().updateMessageState(mLoginUserId, toUserId, msgId, messageState);
        }

        mHandler.post(new Runnable() {
            public void run() {
                for (ChatMessageListener listener : mChatMessageListeners) {
                    listener.onMessageSendStateChange(messageState, msgId);
                }
            }
        });
    }

    /**
     * 通知收到消息【聊天消息监听】
     *
     * @param loginUserId
     * @param fromUserId
     * @param message     消息
     * @param isGroupMsg
     */
    public void notifyNewMesssage(final String loginUserId, final String fromUserId
            , final ChatMessage message, final boolean isGroupMsg) {
        // 压测时，就是以下操作阻塞了主线程，以下操作放在线程池中处理
        // 需要注意的一点是放在子线程时，onNewMessage方法回调的地方变成子线程了，在此方法内回调的地方如果需要做ui操作需要回调到主线程做
        AsyncUtils.doAsync(this,
                executorService,
                c -> {
                    if (message != null) {
                        boolean hasRead = false;
                        for (int i = mChatMessageListeners.size() - 1; i >= 0; i--) {
                            ChatMessage tempMessage = message.cloneAll();
                            if (hasRead) {
                                // 如果他是true，证明已经有类说明他是已读的了，所以就不用再赋值了
                                mChatMessageListeners.get(i).onNewMessage(fromUserId, tempMessage, isGroupMsg);
                            } else {
                                // 进行接口回调,为添加了该监听的类赋值
                                hasRead = mChatMessageListeners.get(i).onNewMessage(fromUserId, tempMessage, isGroupMsg);
                            }
                        }
                        String selfId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
                        // 自己发的消息认为已读不加角标，我的设备除外，
                        final boolean hasRead2 = hasRead
                                || (message.getFromUserId().equals(selfId) && !TextUtils.equals(message.getFromUserId(), message.getToUserId()));
                        if (isGroupMsg) {
                            if (!hasRead2) {
                                // 更新朋友表中该朋友的消息未读数量
                                boolean isRepeatFriend = FriendDao.getInstance().markUserMessageUnRead(loginUserId, fromUserId);
                                if (isRepeatFriend) {// 同一个群组不止一个，需要更新
                                    MyApplication.getContext().sendBroadcast(new Intent(Constants.UPDATE_ROOM));
                                }
                                // 发送广播更新总未读消息数量
                                MsgBroadcast.broadcastMsgNumUpdate(MyApplication.getInstance(), true, 1);
                            }
                        } else {
                            if (!hasRead2) {
                                // 更新朋友表中该朋友的消息未读数量
                                FriendDao.getInstance().markUserMessageUnRead(loginUserId, fromUserId);
                                // 发送广播更新总未读消息数量
                                MsgBroadcast.broadcastMsgNumUpdate(MyApplication.getInstance(), true, 1);
                            }
                        }

                        MsgBroadcast.broadcastMsgUiUpdateSingle(MyApplication.getInstance(), fromUserId);
                    }
                });
    }

    //////////////////////Muc Listener//////////////////////
    /**
     * 群聊操作很多，但是大多数都不是通过以下监听回调去通知界面刷新的，
     * 大多数都是在收到对应群组操作后立即发送广播通过eventBus通知界面刷新等
     */

    /**
     * 通知群组被解散【群组操作监听】
     *
     * @param toUserId
     */
    public void notifyDeleteMucRoom(final String toUserId) {
        if (mMucListeners.size() <= 0) {
            return;
        }
        mHandler.post(new Runnable() {
            public void run() {
                for (MucListener listener : mMucListeners) {
                    listener.onDeleteMucRoom(toUserId);
                }
            }
        });
    }

    /**
     * 通知我被踢出群组【群组操作监听】
     *
     * @param toUserId
     */
    public void notifyMyBeDelete(final String toUserId) {
        if (mMucListeners.size() <= 0) {
            return;
        }
        mHandler.post(new Runnable() {
            public void run() {
                for (MucListener listener : mMucListeners) {
                    listener.onMyBeDelete(toUserId);
                }
            }
        });
    }

    /**
     * 通知群组名称、群内昵称发生变化【群组操作监听】
     *
     * @param toUserId
     * @param changedUserId
     * @param changedName
     */
    public void notifyNickNameChanged(final String toUserId, final String changedUserId, final String changedName) {
        if (mMucListeners.size() <= 0) {
            return;
        }
        mHandler.post(new Runnable() {
            public void run() {
                for (MucListener listener : mMucListeners) {
                    listener.onNickNameChange(toUserId, changedUserId, changedName);
                }
            }
        });
    }

    /**
     * 通知我被禁言【群组操作监听】
     *
     * @param toUserId
     * @param time
     */
    public void notifyMyVoiceBanned(final String toUserId, final int time) {
        if (mMucListeners.size() <= 0) {
            return;
        }
        mHandler.post(new Runnable() {
            public void run() {
                for (MucListener listener : mMucListeners) {
                    listener.onMyVoiceBanned(toUserId, time);
                }
            }
        });
    }

    public void notifyMessageSendStateChange(String loginUserid, ChatMessage chatMessage, int messageState) {
        notifyMessageSendStateChange(loginUserid, chatMessage.getToUserId(), chatMessage.getPacketId(), messageState);
    }
}
