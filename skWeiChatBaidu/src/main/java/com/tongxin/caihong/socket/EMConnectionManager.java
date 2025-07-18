package com.tongxin.caihong.socket;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.event.EventLoginStatus;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.login.MachineDao;
import com.tongxin.caihong.socket.msg.AbstractMessage;
import com.tongxin.caihong.socket.msg.AuthMessage;
import com.tongxin.caihong.socket.msg.AuthRespMessage;
import com.tongxin.caihong.socket.msg.ChatMessage;
import com.tongxin.caihong.socket.msg.ErrorMessage;
import com.tongxin.caihong.socket.msg.ExitGroupMessage;
import com.tongxin.caihong.socket.msg.JoinGroupMessage;
import com.tongxin.caihong.socket.msg.MessageHead;
import com.tongxin.caihong.socket.msg.MessageReceiptStatus;
import com.tongxin.caihong.socket.msg.OffChatMessage;
import com.tongxin.caihong.socket.msg.PingMessage;
import com.tongxin.caihong.socket.msg.PullBatchGroupMessage;
import com.tongxin.caihong.socket.msg.PullBatchGroupRespMessage;
import com.tongxin.caihong.socket.msg.RoomMsgReadMessageBean;
import com.tongxin.caihong.socket.msg.SuccessMessage;
import com.tongxin.caihong.socket.protocol.Command;
import com.tongxin.caihong.socket.protocol.MessageProBuf;
import com.tongxin.caihong.socket.protocol.ProBufUtils;
import com.tongxin.caihong.socket.protocol.TcpDecodeException;
import com.tongxin.caihong.socket.protocol.TcpNotEnoughException;
import com.tongxin.caihong.socket.protocol.TcpPacket;
import com.tongxin.caihong.socket.protocol.TcpServerDecoder;
import com.tongxin.caihong.socket.protocol.TcpServerEncoder;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.ThreadManager;
import com.tongxin.caihong.util.log.LogUtils;
import com.tongxin.caihong.util.secure.HEX;
import com.tongxin.caihong.view.cjt2325.cameralibrary.util.LogUtil;
import com.tongxin.caihong.xmpp.NotifyConnectionListener;
import com.tongxin.caihong.xmpp.ReadManager;
import com.tongxin.caihong.xmpp.ReceiptManager;
import com.tongxin.caihong.xmpp.SendReceiptManager;
import com.tongxin.caihong.xmpp.SocketPingManager;
import com.tongxin.caihong.xmpp.XChatMessageListener;
import com.tongxin.caihong.xmpp.XMuChatMessageListener;
import com.tongxin.caihong.xmpp.listener.AuthStateListener;
import com.tongxin.caihong.xmpp.listener.ChatReceiptListener;
import com.google.protobuf.Descriptors;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * XMPP连接类
 */
public class EMConnectionManager {
    public static final int SOCKET_PORT = 5666;
    //public static final int SOCKET_PORT = 80;
    private static final String TAG = "EMConnectionManagerTag";
    public static long MY_CODE = 0;
    // 关闭多点登录时发消息用的设备名是"chat",
    public static String CURRENT_DEVICE = "android";
    public String SOCKET_HOST = "192.168.0.168";
    // 当前登录用户的用户名与密码
    private String mCurrentLoginUserToken;
    private String mCurrentLoginUserId;
    private SocketThread mSocketThread;
    private NotifyConnectionListener mConnectListener;
    private ChatReceiptListener mChatReceiptListener;
    private XChatMessageListener mChatMessageListener;
    private XMuChatMessageListener mGroupMessageListener;
    private List<ChatMessage> mOffChatMessage;
    private List<ChatMessage> mOffGroupMessage;
    private SendReceiptManager mSendReceiptManager;
    private ReadManager mReadManager;

    public EMConnectionManager(Context context) {
        SOCKET_HOST = CoreManager.requireConfig(context).XMPPHost;

        mCurrentLoginUserToken = CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken;
        mCurrentLoginUserId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        mOffChatMessage = new ArrayList<>();
        mOffGroupMessage = new ArrayList<>();
        Log.e(TAG, " new EMConnectionManager: " + mCurrentLoginUserToken + " ,  socket ip: " + SOCKET_HOST);
        mSocketThread = new SocketThread(this, SOCKET_HOST, SOCKET_PORT);
        mSocketThread.start();

        mSendReceiptManager = new SendReceiptManager(this);
        mReadManager = new ReadManager(this);
    }

    public void login(final String token, String userId) {
        if (mSocketThread == null) {
            Log.e(TAG, "SocketThread对象空了，创建一个SocketThread对象");
            mSocketThread = new SocketThread(this, SOCKET_HOST, SOCKET_PORT);
            mSocketThread.start();
        } else {
            // TODO: 已连接情况重新发送登录要么发送失败，要么发送成功也收不到消息，这种情况是否应该断开重连，
            Log.e(TAG, "login: " + token + " ,  " + userId + " ,  " + getCurrentState());
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                ThreadManager.getPool().execute(() -> {
                    mSocketThread.login(token, userId);
                });
            } else {
                mSocketThread.login(token, userId);
            }
        }
    }

    /**
     * 返回当前登陆账号
     *
     * @return
     */
    public String getLoginUserID() {
        return mCurrentLoginUserId;
    }

    public int getCurrentState() {
        if (mSocketThread == null) {
            return AuthStateListener.AUTH_STATE_INIT;
        }
        return mSocketThread.mSocketConnectState;
    }

    public boolean isConnected() {
        if (getCurrentState() > AuthStateListener.AUTH_STATE_INIT) {
            return true;
        }
        return false;
    }

    public boolean isAuthenticated() {
        Log.i("socket conn state", "isAuthenticated: " + getCurrentState());
        return getCurrentState() == AuthStateListener.AUTH_STATE_SUCCESS;
    }

    public void disconnect() {
        if (mSocketThread != null) {
            mSocketThread.disconnect();
            mSocketThread = null;
            Log.e(TAG, "Socket disconnect success");
        } else {
            Log.e(TAG, "SocketThread = null");
        }
    }

    /**
     * 主动断开socket长连接
     * <p>
     * 网络断开后，socket长连接已经失效，此时正常情况下decodeSocket方法内阻塞的read操作会抛出io异常
     * 但模拟器上调试发现，网络断开之后read操作处无任何变化，一直阻塞在那里，且此时判断socket对象的isClosed状态还为false
     * 当网络连接上之后，因为长连接状态还为已连接的状态，导致没有去重新创建一个socket长连接
     */
    public void socketConnectClose() {
        if (mSocketThread != null) {
            mSocketThread.close();
        }
    }

    public void sendMessage(ChatMessage chatMessage) {
//        if (EMConnectionManager.MY_CODE != 0) {
        if(mSocketThread != null) {
            mSocketThread.send(chatMessage);
        }
//        }
    }

    public void sendPingMessage() {
        mSocketThread.ping();
    }

    // 批量回执
    private void addReceipt(MessageHead messageHead) {
        mSendReceiptManager.addReceipt(messageHead);
    }

    public void sendReceipt(List<MessageHead> messageHeadList) {
        mSocketThread.sendReceipt(messageHeadList);
    }

    // 批量回执
    public void addRead(ReadManager.MucReadItem item) {
        mReadManager.addRead(item);
    }

    public void sendRead(String roomJid, List<ReadManager.MucReadItem> list) {
        mSocketThread.sendRead(roomJid, list);
    }

    // 批量加入群组，拉取群离线消息
    public void batchJoinRoom(PullBatchGroupMessage message) {
        if (mSocketThread != null) {
            mSocketThread.batchJoinRoom(message);
        }
    }

    public void joinRoom(JoinGroupMessage message) {
        if (mSocketThread != null) {
            mSocketThread.joinRoom(message);
        }
    }

    public void exitRoom(ExitGroupMessage message) {
        mSocketThread.exitRoom(message);
    }

    /**
     * 连接监听
     *
     * @param connectionListener
     */
    public void addConnectionListener(NotifyConnectionListener connectionListener) {
        this.mConnectListener = connectionListener;
    }

    public void removeConnectionListener() {
        this.mConnectListener = null;
    }

    /**
     * 消息回执监听
     *
     * @param chatReceiptListener
     */
    public void addReceiptReceivedListener(ChatReceiptListener chatReceiptListener) {
        this.mChatReceiptListener = chatReceiptListener;
    }

    /**
     * 单聊消息监听
     *
     * @param messageListener
     */
    public void addIncomingListener(XChatMessageListener messageListener) {
        if (messageListener == null) {
            return;
        }

        this.mChatMessageListener = messageListener;
        if (mOffChatMessage != null && mOffChatMessage.size() > 0) {
            for (int i = mOffChatMessage.size() - 1; i >= 0; i--) {
                mChatMessageListener.onReceMessage(mOffChatMessage.get(i).toSkMessage(mCurrentLoginUserId),
                        false);
                mOffChatMessage.remove(i);
            }
        }
    }

    /**
     * 群聊消息监听
     *
     * @param groupMessageListener
     */
    public void addMuChatMessageListener(XMuChatMessageListener groupMessageListener) {
        if (groupMessageListener == null) {
            return;
        }
        this.mGroupMessageListener = groupMessageListener;
    }

    /**
     * 核心
     */
    private class SocketThread extends Thread {
        private static final int MAX_SIZE = 2048; // max size 256
        private String mIp;
        private int mPort;

        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        private boolean mLoginIng;
        private int mSocketConnectState = AuthStateListener.AUTH_STATE_INIT;
        // 输入缓冲区，
        private ByteBuffer dataBuffer = ByteBuffer.allocate(MAX_SIZE);

        private int pingFailedCount = 0;
        private EMConnectionManager mConnectionManager;
        // 用户退出登录等主动退出的情况，
        private boolean disconnected = false;

        public SocketThread(EMConnectionManager connectionManager, String ip, int port) {
            this.mIp = ip;
            this.mPort = port;
            this.mConnectionManager = connectionManager;
            // 准备连接
            notifyConnect(1, AuthStateListener.AUTH_STATE_ING);
        }

        private void notifyConnect(int which, int authState) {
            Log.e(TAG, "which：" + which);
            mSocketConnectState = authState;
            if (authState == AuthStateListener.AUTH_STATE_ING) {
                MY_CODE = 0;
                if (mConnectListener != null) {
                    mConnectListener.notifyConnecting();
                }
            } else if (authState == AuthStateListener.AUTH_STATE_SUCCESS) {
                if (mConnectListener != null) {
                    mConnectListener.notifyAuthenticated();
                }
            }
        }

        private void notifyClose() {
            MY_CODE = 0;
            mLoginIng = false;
            mSocketConnectState = AuthStateListener.AUTH_STATE_CLOSE;
            if (mConnectListener != null) {
                mConnectListener.notifyConnectionClosed();
            }
        }

        private void notifyError(String exception) {
            MY_CODE = 0;
            mLoginIng = false;
            mSocketConnectState = AuthStateListener.AUTH_STATE_ERROR;
            if (mConnectListener != null) {
                mConnectListener.notifyConnectionClosedOnError(exception);
            } else {
                LogUtil.e(TAG, "notifyError-->mConnectListener空了");
            }
        }

        @Override
        public void run() {
            initSocket();
            try {
                startRead();
            } catch (IOException e) {
                LogUtils.e(TAG, "decodeSocket: read抛异常", e);
                closeAll();
                notifyError(SocketException.SELECTION_KEY_INVALID);
            }
        }

        private void initSocket() {
            try {
                // socket重新连接次数，
                int tryTimes = 3;
                // 重连间隔时间，避免连不上服务器时无限触发外部的重连机制，
                long durationTime = TimeUnit.SECONDS.toMillis(1);
                long startTime = System.currentTimeMillis();
                while (tryTimes-- > 0) {
                    try {
                        // 构造同时连接，
                        socket = new Socket(mIp, mPort);
                        break;
                    } catch (IOException e) {
                        // 连接失败，休息后重新连接，
                        // 计算休息时间，确保尝试连接一次的时间不小于durationTime,
                        // 避免无意义的重连，因为当服务器没有启动时socket连接会立即抛出异常，
                        LogUtils.d(TAG, "连接失败，剩余连接次数 " + tryTimes, e);
                        long sleepTime = startTime + durationTime - System.currentTimeMillis();
                        if (sleepTime > 0) {
                            try {
                                sleep(sleepTime);
                            } catch (InterruptedException e1) {
                                throw new IOException("连接中断", e1);
                            }
                        }
                        startTime = System.currentTimeMillis();
                    }
                }
                if (socket == null) {
                    throw new IOException("连接失败, host=" + mIp + ", port=" + mPort);
                }
                if (mSocketThread == null) {
                    Log.e(TAG, "mSocketThread == null");
                    socket.close();
                    return;
                }
                // 输出不需要缓冲，因为有自己维护缓冲区，
                inputStream = socket.getInputStream();
                // 输出需要缓冲，否则会一个一个字节发送，
                outputStream = new BufferedOutputStream(socket.getOutputStream());
                // 已连接
                notifyConnect(2, AuthStateListener.AUTH_STATE_ING);
                Log.e(TAG, "已连接服务器: " + mIp);
                login(mCurrentLoginUserToken, mCurrentLoginUserId);
            } catch (IOException e) {
                LogUtils.e(TAG, "initSocket  : 连接服务器失败", e);
                notifyError(SocketException.FINISH_CONNECT_EXCEPTION);
            }
        }

        private void startRead() throws IOException {
            while (isConnected()) {
                decodeSocket();
            }
        }

        public void disconnect() {
            disconnected = true;
            mSocketThread.mLoginIng = false;
            closeAll();
            mSocketThread.interrupt();
        }

        public void close() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeAll() {
            if (inputStream != null) {
                finallyClose(inputStream);
            }
            if (outputStream != null) {
                finallyClose(outputStream);
            }
            if (socket != null) {
                finallyClose(socket);
            }
        }

        private void finallyClose(Closeable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /********************************************************************************
         *   Todo 发送各种类型消息，与服务器进行交互
         ********************************************************************************/
        private boolean isConnected() {
            return socket != null && socket.isConnected() && !disconnected;
        }

        private boolean inSocket(AbstractMessage message, Descriptors.Descriptor descriptor, short comm) {
            boolean success = false;
            byte[] bytes = ProBufUtils.encodeMessageBody(message, descriptor);
            TcpPacket packet = new TcpPacket(comm, bytes);
            ByteBuffer dataBuffer = TcpServerEncoder.encode(packet);
            dataBuffer.flip();
            // 输出到通道
            try {
                if (isConnected() && outputStream != null) {
                    // 强行保留旧代码的ByteBuffer,
                    outputStream.write(dataBuffer.array(), dataBuffer.position(), dataBuffer.remaining());
                    outputStream.flush();
                    dataBuffer.position(dataBuffer.limit());
                } else {
                    return false;
                }
                success = true;
            } catch (IOException e) {
                LogUtils.e(TAG, "initSocket  : 发送数据失败", e);
                notifyError(SocketException.SELECTION_KEY_INVALID);
                closeAll();
            }
            return success;
        }

        public void login(final String token, String userId) {
            if (mLoginIng) {
                Log.e(TAG, "login：当前正在登录，不允许重复登录");
                return;
            }
            if (mSocketConnectState == AuthStateListener.AUTH_STATE_SUCCESS) {
                Log.e(TAG, "login：已经登录成功，不允许重复登录");
                return;
            }
            if (socket == null || !socket.isConnected()) {
                Log.e(TAG, "login：socket == null || !socket.isConnected()，不允许登录");
                return;
            }
            mLoginIng = true;
            AuthMessage auth = new AuthMessage();
            auth.setToken(token);
            auth.setPassword("");
            auth.setDeviceId(DeviceInfoUtil.getDeviceId(MyApplication.getContext()));
            auth.setVersion(4);
            auth.setApiKey("1");
            auth.setAppName("1");
            auth.setCompanyName("1");
            auth.setSecret("1");

            MessageHead head = new MessageHead();
            head.setChatType((byte) 1);
            head.setFrom(userId + "/" + EMConnectionManager.CURRENT_DEVICE);
            head.setTo("service");
            head.setMessageId(UUID.randomUUID().toString().replaceAll("-", ""));
            auth.setMessageHead(head);

            Descriptors.Descriptor descriptor = MessageProBuf.AuthMessage.getDescriptor();
            notifyConnect(3, AuthStateListener.AUTH_STATE_ING);
            if (!inSocket(auth, descriptor, Command.COMMAND_AUTH_REQ)) {
                notifyError(SocketException.LOGIN_MESSAGE_SEND_FAILED_EXCEPTION);
            }
        }

        /**
         * 获取中文的app_name
         *
         * @param resources
         * @return
         */
        private String getResourcesByLocale(Resources resources) {
            Configuration configuration = new Configuration(resources.getConfiguration());
            configuration.locale = new Locale("zh");
            Resources zhResources = new Resources(resources.getAssets(), resources.getDisplayMetrics(), configuration);
            return zhResources.getString(R.string.app_name);
        }

        /**
         * @param message
         */
        private void send(final ChatMessage message) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    String str = message.messageHead.getChatType() == 2 ? "群聊" : "单聊";
                    if (inSocket(message, MessageProBuf.ChatMessage.getDescriptor(), Command.COMMAND_CHAT_REQ)) {
                        Log.e(TAG, "发送" + str + "聊天消息 成功: " + message.toString());
                    } else {
                        Log.e(TAG, "发送" + str + "聊天消息 失败: " + message.toString());
                        mChatReceiptListener.onReceiveReceipt(ReceiptManager.RECEIPT_ERR, message.getMessageId(), 0);
                    }
                }
            });
        }

        private void ping() {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    String messageId = UUID.randomUUID().toString().replace("-", "");
                    PingMessage ping = new PingMessage();
                    MessageHead head = new MessageHead();
                    head.setChatType((byte) 1);
                    head.setFrom(mCurrentLoginUserId + "/" + CURRENT_DEVICE);
                    head.setTo("service");
                    head.setMessageId(messageId);
                    ping.setMessageHead(head);

                    Descriptors.Descriptor descriptor = MessageProBuf.PingMessageProBuf.getDescriptor();
                    if (inSocket(ping, descriptor, Command.COMMAND_PING_REQ)) {
                        pingFailedCount = 0;
                        Log.e("ping", "发送Ping消息给服务器 成功");
                    } else {
                        pingFailedCount++;
                        Log.e("ping", "发送Ping消息给服务器 失败--->pingFailedCount==" + pingFailedCount);
                        if (pingFailedCount == 2) {
                            Log.e("ping", "Ping失败两次，本地连接置为离线");
                            notifyError(SocketException.SOCKET_PING_FAILED);
                        }
                    }
                }
            });
        }

        /**
         * @param messageHead
         */
        private void addReceipt(MessageHead messageHead) {
            mConnectionManager.addReceipt(messageHead);
        }

        /**
         * @param messageHeadList
         */
        private void sendReceipt(List<MessageHead> messageHeadList) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    StringBuilder messageId = new StringBuilder();
                    for (int i = 0; i < messageHeadList.size(); i++) {
                        messageId.append(messageHeadList.get(i).getMessageId()).append(",");
                    }
                    MessageReceiptStatus messageReceiptStatus = new MessageReceiptStatus();
                    messageReceiptStatus.setMessageId(messageId.toString());
                    messageReceiptStatus.setStatus((byte) 2);

                    MessageHead head = new MessageHead();
                    head.setChatType(messageHeadList.get(0).getChatType());
                    head.setFrom(mCurrentLoginUserId + "/" + EMConnectionManager.CURRENT_DEVICE);
                    head.setTo(messageHeadList.get(0).getTo());
                    head.setMessageId(UUID.randomUUID().toString().replaceAll("-", ""));
                    messageReceiptStatus.setMessageHead(head);

                    Descriptors.Descriptor descriptor = MessageProBuf.MessageReceiptStatusProBuf.getDescriptor();
                    if (inSocket(messageReceiptStatus, descriptor, Command.COMMAND_MESSAGE_RECEIPT_REQ)) {
                        Log.e(TAG, "发送回执消息给服务器 成功" + messageReceiptStatus.toString());
                    } else {
                        Log.e(TAG, "发送回执消息给服务器 失败" + messageReceiptStatus.toString());
                    }
                }
            });
        }

        /**
         * @param roomJid
         * @param list
         */
        private void sendRead(String roomJid, List<ReadManager.MucReadItem> list) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    StringBuilder content = new StringBuilder();
                    for (int i = 0; i < list.size(); i++) {
                        ReadManager.MucReadItem item = list.get(i);
                        content.append(item.fromUserId).append(':')
                                .append(item.messageId).append(';');
                    }
                    RoomMsgReadMessageBean roomMsgReadMessage = new RoomMsgReadMessageBean();
                    roomMsgReadMessage.setJid(roomJid);
                    roomMsgReadMessage.setContent(content.toString());

                    MessageHead head = new MessageHead();
                    head.setChatType((byte) 2);
                    head.setFrom(mCurrentLoginUserId + "/" + EMConnectionManager.CURRENT_DEVICE);
                    head.setTo("service");
                    head.setMessageId(UUID.randomUUID().toString().replaceAll("-", ""));
                    roomMsgReadMessage.setMessageHead(head);

                    Descriptors.Descriptor descriptor = MessageProBuf.RoomMsgReadMessage.getDescriptor();
                    if (inSocket(roomMsgReadMessage, descriptor, Command.GROUP_READ_MESSAGE)) {
                        Log.e(TAG, "发送已读给服务器 成功" + roomMsgReadMessage.toString());
                    } else {
                        Log.e(TAG, "发送已读给服务器 失败" + roomMsgReadMessage.toString());
                    }
                }
            });
        }

        private void batchJoinRoom(final PullBatchGroupMessage message) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    Descriptors.Descriptor descriptor = MessageProBuf.PullBatchGroupMessageReqProBuf.getDescriptor();
                    if (inSocket(message, descriptor, Command.COMMAND_BATCH_JOIN_GROUP_REQ)) {
                        Log.e("batchJoinRoom", "发送消息 批量获取群组离线消息 成功: messageId :" + message.getMessageHead().getMessageId() + "  jidList:" + message.getJidList().toString());
                    } else {
                        Log.e("batchJoinRoom", "发送消息 批量获取群组离线消息 失败: messageId :" + message.getMessageHead().getMessageId() + "  jidList:" + message.getJidList().toString());
                    }
                }
            });
        }

        private void joinRoom(final JoinGroupMessage message) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    Descriptors.Descriptor descriptor = MessageProBuf.JoinGroupMessageProBuf.getDescriptor();
                    if (inSocket(message, descriptor, Command.COMMAND_JOIN_GROUP_REQ)) {
                        Log.e(TAG, "发送消息 加入房间 成功: messageId :" + message.getMessageHead().getMessageId());
                    } else {
                        Log.e(TAG, "发送消息 加入房间 失败: messageId :" + message.getMessageHead().getMessageId());
                        mChatReceiptListener.onReceiveReceipt(ReceiptManager.RECEIPT_ERR, message.getMessageHead().getMessageId(), 0);
                    }
                }
            });
        }

        private void exitRoom(final ExitGroupMessage message) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    Descriptors.Descriptor descriptor = MessageProBuf.ExitGroupMessageProBuf.getDescriptor();
                    if (inSocket(message, descriptor, Command.COMMAND_EXIT_GROUP_REQ)) {
                        Log.e(TAG, "发送消息 退出房间 成功: messageId : " + message.getMessageHead().getMessageId());
                    } else {
                        Log.e(TAG, "发送消息 退出房间 失败: messageId : " + message.getMessageHead().getMessageId());
                        mChatReceiptListener.onReceiveReceipt(ReceiptManager.RECEIPT_ERR, message.getMessageHead().getMessageId(), 0);
                    }
                }
            });
        }

        /********************************************************************************
         *  Todo 收到各种类型消息，解析分发
         ********************************************************************************/
        private void decodeSocket() throws IOException {
            // 复用dataBuffer, 以便接上一个tcp剩下的数据一起处理，
            if (dataBuffer.remaining() == 0) {
                realloc();
            }
            // 强行保留旧代码的ByteBuffer,
            int count = inputStream.read(dataBuffer.array(), dataBuffer.position(), dataBuffer.remaining());

            if (count == -1) {
                // 读到EOF, 基本上表示服务器关闭了输出流，
                // 抛出去结束连接，
                throw new EOFException();
            } else if (count > 0) {
                dataBuffer.position(dataBuffer.position() + count);
                // 重新遍历缓冲区，排除剩下没读取的0，
                // flip后limit就会是已经读取了的字节数，而不是缓冲区总长度，
                dataBuffer.flip();
                // 标记是否有剩下数据，
                boolean left = false;
                while (dataBuffer.hasRemaining()) {
                    try {
                        while (dataBuffer.get(dataBuffer.position()) == 0) {
                            dataBuffer.position(dataBuffer.position()+1);
                        }
                        if (dataBuffer.get(dataBuffer.position()) == 0) {
                            // 保留旧代码，协议为0时跳出循环，实际上应该不会出现，
                            LogUtils.e(TAG, "异常数据报，协议版本为0，直接清了这个缓冲区，length = " + dataBuffer.remaining());
                            // 恢复指针和limit,
                            dataBuffer.clear();
                            break;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        dataBuffer.clear();
                        break;
                    }
                    TcpPacket packet;
                    try {
                        packet = TcpServerDecoder.decode(dataBuffer);
                    } catch (TcpNotEnoughException e) {
                        // dataBuffer可能还有剩，比如一个tcp包放不下，最后一个业务包有部分在下一个tcp包，
                        // 此时当前tcp包的最后一个业务包是残缺的，不能处理，要留在缓冲区里，
                        // 这里重置dataBuffer, 剩下的数据放在开头等下个包来接上再处理，
                        // 可以断点等待阻塞输入流模拟分包情况，
                        LogUtils.e(TAG, "decodeSocket: 业务包不完全读取，整理缓冲区等下个socket包，\n" +
                                "capacity=" + dataBuffer.capacity() + "\n" +
                                "limit=" + dataBuffer.limit() + "\n" +
                                "position=" + dataBuffer.position() + "\n" +
                                "headerLength=" + e.getHeaderLength() + "\n" +
                                "bodyLen=" + e.getBodyLen() + "\n" +
                                "", e);
                        byte[] headerArray = new byte[Math.max(0, Math.min(4 + e.getHeaderLength(), dataBuffer.limit() - dataBuffer.position()))];
                        for (int i = 0; i < headerArray.length; i++) {
                            headerArray[i] = dataBuffer.get(dataBuffer.position() + i);
                        }
                        LogUtils.e(TAG, "decodeSocket: header: " + HEX.encode(headerArray));
                        if (dataBuffer.position() != 0) {
                            int leftLength = dataBuffer.remaining();
                            byte[] tmp = new byte[leftLength];
                            dataBuffer.get(tmp);
                            // 恢复指针和limit,
                            dataBuffer.clear();
                            dataBuffer.put(tmp);
                        } else {
                            dataBuffer.position(dataBuffer.limit());
                            dataBuffer.limit(dataBuffer.capacity());
                        }
                        left = true;
                        break;
                    } catch (TcpDecodeException e) {
                        LogUtils.e(TAG, "decodeSocket: 异常数据报，解析失败，直接清了这个缓冲区，length = " + dataBuffer.remaining());
                        // 恢复指针和limit,
                        dataBuffer.clear();
                        break;
                    }
                    decodePacket(packet);
                }
                // 如果没有数据剩下，直接清空缓冲区，
                if (!left) {
                    // 恢复指针和limit,
                    dataBuffer.clear();
                }
            } else {
                LogUtils.e(TAG, "decodeSocket: 读取到0个字节，\n" +
                        "capacity=" + dataBuffer.capacity() + "\n" +
                        "limit=" + dataBuffer.limit() + "\n" +
                        "position=" + dataBuffer.position() + "\n" +
                        "");
            }
        }

        private void realloc() {
            // 考虑一个业务包就大于整个输入缓冲区的情况，会导致输入缓冲区永远被充满，
            // 这种情况需要扩容输入缓冲区，分配两倍空间，保留旧数据，继续读取后续部分，
            LogUtils.e(TAG, "decodeSocket: 缓冲区满，增加空间，\n" +
                    "capacity=" + dataBuffer.capacity() + "\n" +
                    "limit=" + dataBuffer.limit() + "\n" +
                    "position=" + dataBuffer.position() + "\n" +
                    "");
            dataBuffer.rewind();
            ByteBuffer newBuffer = ByteBuffer.allocate(dataBuffer.capacity() * 2);
            newBuffer.put(dataBuffer);
            dataBuffer = newBuffer;
        }

        private void decodePacket(TcpPacket packet) {
            if (packet == null) {
                Log.e(TAG, "decode: TCP解码失败");
                return;
            }
            Log.e(TAG, "decode: TCP解码成功" + packet.getCommand());
            /**
             * 登录结果返回 || 登录冲突
             */
            if (packet.getCommand() == Command.COMMAND_AUTH_RESP
                    || packet.getCommand() == Command.COMMAND_LOGIN_CONFLICT_RESP) {
                if (packet.getCommand() == Command.COMMAND_AUTH_RESP) {
                    AuthRespMessage auth = ProBufUtils.decoderMessageBody(packet.getBytes(), MessageProBuf.AuthRespMessageProBuf.getDescriptor(), AuthRespMessage.class);
                    if (auth.getStatus() == 1) { // 0 登陆失败 1 登陆成功
                        if (mSocketConnectState == AuthStateListener.AUTH_STATE_SUCCESS) {
                            // 这个是有其他设备登录进来了
                            Log.e(TAG, "收到其他设备登录的信息: 当前在线设备: " + auth.getResources());
                        } else {
                            if (auth.getVersion() != 4) {
                                Log.e(TAG, "登录失败:：version不一致--->" + auth.getVersion());
                            } else {
                                Log.e(TAG, "登录成功，在线用户:" + mCurrentLoginUserId + "/" + auth.getResources());
                                Log.e(TAG, "登录成功，mcode:" + auth.getMcode());
                                //MY_CODE = ProtoBufHelper.getCode(MyApplication.getContext(), auth.getMcode(), BuildConfig.COMPANY_NAME);
                                notifyConnect(4, AuthStateListener.AUTH_STATE_SUCCESS);
                                // 开始Ping 服务器
                                SocketPingManager.getInstance().registerPing(mConnectionManager);
                            }
                        }
                    } else {
                        Log.e(TAG, "登录失败:：" + mCurrentLoginUserId + " ,  " + auth.getArg());
                    }
                    if (!TextUtils.isEmpty(auth.getResources())) {
                        MachineDao.getInstance().changeDevice(auth.getResources());
                        EventBus.getDefault().post(new EventLoginStatus(auth.getResources(), true));
                    }
                    mLoginIng = false;
                    // Todo  断网重连之后会先回调到登录成功，间隔几十毫秒之后又突然回调到这里且Status==0 auth.getArg()==null，
                    // Todo 与服务端联调，服务端说只收到一条登录消息请求，同时也只回了一条过来。服务端说他只收到
                    // Todo  所以结果就是断网重连之后连不上。但是当我下面notifyError的代码注释掉之后，Socket的连接居然正常了，让人摸不着头脑，先记录一下
                    /*else {
                        Log.e(TAG, "登录失败:：" + mCurrentLoginUserId + " ,  " + auth.getArg());
                        notifyError(SocketException.LOGIN_FAILED_EXCEPTION + "--->HashCode：" + mSelector.hashCode());
                    }*/
                } else {
                    Log.e(TAG, "有其他安卓设备登录我的账号: 我被挤下线了");
                    notifyError(SocketException.LOGIN_CONFLICT_EXCEPTION);
                }
                return;
            }

            if (packet.getCommand() == Command.COMMAND_ERROR) {
                Log.e(TAG, "消息发送失败: ");
                ErrorMessage errorMessage = ProBufUtils.decoderMessageBody(packet.getBytes(), MessageProBuf.CommonErrorProBuf.getDescriptor(), ErrorMessage.class);
                if (errorMessage != null) {
                    //{"arg":"敏感词","code":-1,"messageHead":{"chatType":1,"from":"10002111/android","messageId":"287548f0c8a84d4c936b27172f155ea1","offline":false,"to":"10008295"}}
                    if (errorMessage.getCode() == -2) {// 敏感词error
                        mChatReceiptListener.onReceiveReceipt(ReceiptManager.RECEIPT_ERR, errorMessage.getMessageHead().getMessageId(), 0);
                        MessageUtil.generateTipMessage(mCurrentLoginUserId, errorMessage.getMessageHead().getTo(), MyApplication.getContext().getString(R.string.tip_sensitive), errorMessage.getMessageHead().getChatType() == 2);
                    }
                }
            } else if (packet.getCommand() == Command.COMMAND_CHAT_REQ) {
                // 解析数据包消息
                ChatMessage chatMessage = ProBufUtils.decoderMessageBody(packet.getBytes(), MessageProBuf.ChatMessage.getDescriptor(), ChatMessage.class);
                if (chatMessage == null || chatMessage.getMessageHead() == null) {
                    Log.e(TAG, "decodePacket: 解析出错");
                    return;
                }
                // 发消息回执给服务器通知服务器客户端已收到
                byte chatType = chatMessage.getMessageHead().getChatType();
                // 发送回执给服务器
                addReceipt(chatMessage.getMessageHead());
                if (chatType == 2) {
                    // 群组聊天
                    if (chatMessage.getFromUserId().equals(mCurrentLoginUserId)) {
                        // 自己发送的消息
                        String device = chatMessage.getMessageHead().getFrom().replaceAll(mCurrentLoginUserId + "/", "");
                        if (CURRENT_DEVICE.equals(device)) {
                            // 自己发送的消息且为此端发送，当做回执处理
                            Log.e(TAG, "收到群聊回执  消息发送成功: " + chatMessage.toString());
                            mChatReceiptListener.onReceiveReceipt(ReceiptManager.RECEIPT_YES, chatMessage.getMessageId(), chatMessage.getSeqNo());
                            if (chatMessage.getType() == XmppMessage.TYPE_READ
                                    || chatMessage.getType() == XmppMessage.TYPE_SECURE_LOST_KEY
                                    || chatMessage.getType() == XmppMessage.TYPE_SECURE_SEND_KEY) {
                                // 收到自己发送的群已读回执的同时，还需要在onReceMessage方法内处理一下
                                mGroupMessageListener.onReceMessage(chatMessage
                                        , chatMessage.toSkMessage(mCurrentLoginUserId),
                                        false);
                            }
                        } else {
                            Log.e(TAG, "收到群组自己在其他端发送的聊天消息    content: " + chatMessage.getContent());
                            mGroupMessageListener.onReceMessage(chatMessage
                                    , chatMessage.toSkMessage(mCurrentLoginUserId)
                                    , false);
                        }
                    } else {
                        if (mGroupMessageListener != null) {
                            Log.e(TAG, "收到群组聊天消息  来自 ：" + chatMessage.getFromUserId() + "   content: " + chatMessage.getContent());
                            mGroupMessageListener.onReceMessage(chatMessage
                                    , chatMessage.toSkMessage(mCurrentLoginUserId)
                                    , false);
                        } else {
                            Log.e(TAG, "收到群组离线消息来自 ：" + chatMessage.messageHead.getTo() + "   content: " + chatMessage.getContent());
                            mOffGroupMessage.add(0, chatMessage);
                        }
                    }
                } else {
                    if (mChatMessageListener != null) {
                        Log.e(TAG, "收到单聊聊天消息来自 ：" + chatMessage.getFromUserId() + "   content: " + chatMessage.getContent());
                        mChatMessageListener.onReceMessage(chatMessage.toSkMessage(mCurrentLoginUserId),
                                false);
                    } else {
                        Log.e(TAG, "收到单聊离线消息来自 ：" + chatMessage.getFromUserId() + "   content: " + chatMessage.getContent());
                        mOffChatMessage.add(0, chatMessage);
                    }
                }
            } else if (packet.getCommand() == Command.CHAT_OFF_MESSAGE_RESP) {
                OffChatMessage offChatMessage = ProBufUtils.decoderMessageBody(packet.getBytes(),
                        MessageProBuf.OffChatMessage.getDescriptor(), OffChatMessage.class);
                if (mChatMessageListener != null) {
                    Log.e(TAG, "收到单聊聊天离线消息来自 ：" + offChatMessage.getFromUserId() + "   content: " + offChatMessage.getContent());
                    mChatMessageListener.onReceMessage(offChatMessage.toSkMessage(mCurrentLoginUserId)
                            , offChatMessage.isOffend());
                } else {
                    Log.e(TAG, "收到单聊离线消息来自 ：" + offChatMessage.getFromUserId() + "   content: " + offChatMessage.getContent());
                    mOffChatMessage.add(0, offChatMessage);
                }
            } else if (packet.getCommand() == Command.GROUP_READ_MESSAGE) {
                RoomMsgReadMessageBean roomMsgReadMessage = ProBufUtils.decoderMessageBody(packet.getBytes(),
                        MessageProBuf.RoomMsgReadMessage.getDescriptor(), RoomMsgReadMessageBean.class);
                String roomJid = roomMsgReadMessage.getJid();
                Log.e("ReadManager", "收到群消息已读：" + roomMsgReadMessage);
                for (String packetId : roomMsgReadMessage.getContent().split(",")) {
                    ChatMessageDao.getInstance().increaseMessageRead(mCurrentLoginUserId, roomJid, packetId);
                    // 通知刷新
                    MsgBroadcast.broadcastMsgReadUpdate(MyApplication.getInstance(), packetId);
                }
            } else if (packet.getCommand() == Command.COMMAND_BATCH_JOIN_GROUP_RESP) {
                PullBatchGroupRespMessage pullBatchGroupRespMessage = ProBufUtils.decoderMessageBody(packet.getBytes(),
                        MessageProBuf.PullGroupMessageRespProBuf.getDescriptor(), PullBatchGroupRespMessage.class);

                Log.e("batchJoinRoom", "批量拉取群组消息结果返回：" + pullBatchGroupRespMessage.getMessageId());
                Friend friend = FriendDao.getInstance().getFriend(mCurrentLoginUserId, pullBatchGroupRespMessage.getJid());
                String name = friend != null ? friend.getNickName() : pullBatchGroupRespMessage.getJid();
                List<OffChatMessage> offLineChatMessageList = pullBatchGroupRespMessage.getMessageList();
                Log.e("batchJoinRoom", "群组：" + name + "，一共有" + pullBatchGroupRespMessage.getCount() + "条离线消息，" + "实际返回" + offLineChatMessageList.size() + "条消息");
                if (mGroupMessageListener != null) {
                    for (int i = 0; i < offLineChatMessageList.size(); i++) {
                        mGroupMessageListener.onReceMessage(offLineChatMessageList.get(i)
                                , offLineChatMessageList.get(i).toSkMessage(mCurrentLoginUserId)
                                , offLineChatMessageList.get(i).isOffend());
/*
                        Log.e("batchJoinRoom", "离线消息-->" + offLineChatMessageList.get(i).getContent()
                                + " ，是否为最后一条offLineChatMessageList.get(i).isOffend()" + offLineChatMessageList.get(i).isOffend());
*/
                    }
                } else {
                    mOffGroupMessage.addAll(offLineChatMessageList);
                }
                // 服务端最多返回1000条离线消息，剩下的需要靠漫游获取，但未读消息数量我们要显示准确，这里更新一下
                if (pullBatchGroupRespMessage.getCount() > 1000) {
                    FriendDao.getInstance().markUserMessageUnRead2(mCurrentLoginUserId, pullBatchGroupRespMessage.getJid(), (int) (pullBatchGroupRespMessage.getCount() - 1000));
                }
                // 拉到一次离线消息数量就更新离线时间，避免再拉到这部分离线消息数量，
                Constants.OFFLINE_TIME_IS_FROM_SERVICE = false;
                long time = System.currentTimeMillis() / 1000;
                CoreManager.saveOfflineTime(MyApplication.getContext(), CoreManager.requireSelf(MyApplication.getContext()).getUserId(), time);
            } else if (packet.getCommand() == Command.COMMAND_SUCCESS) {
                SuccessMessage success = ProBufUtils.decoderMessageBody(packet.getBytes(), MessageProBuf.CommonSuccessProBuf.getDescriptor(), SuccessMessage.class);
                MessageHead head = success.getMessageHead();
                Log.e(TAG, "收到回执消息  消息发送成功: " + head.getMessageId());
                mChatReceiptListener.onReceiveReceipt(ReceiptManager.RECEIPT_YES, head.getMessageId(), success.getSeqNo());
            }
        }
    }
}
