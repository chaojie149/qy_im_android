package com.tongxin.caihong.xmpp;


import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.MsgRoamTask;
import com.tongxin.caihong.bean.event.EventCreateMucRoomTask;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.ChatRecord;
import com.tongxin.caihong.bean.message.FillChatMessage;
import com.tongxin.caihong.bean.message.JXSeqNoModel;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.HandleActivityDestroyedDoSomeThing;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.google.gson.Gson;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * @author zq
 * 消息序号管理
 * 用于特殊情况下的消息丢失，以及多端登录，在其他端产生了聊天记录时，将缺失的消息补回等。
 */
public class SeqNoManager {
    public static final String TAG = "SeqNoManager";
    private static SeqNoManager instance;
    // 生成单聊任务相关变量
    boolean isWaitGetLasRequestResponse;
    Map<String, String> waitGetLasRequestResponseReceivedMsg = new HashMap<>();
    Map<String, MsgRoamTask> getLastCallbackCreatedTask = new HashMap<>();
    private SeqNoDelayThread seqNoDelayThread;
    private SeqNoOnlineThread seqNoOnlineThread;
    private boolean seqNoDelayRunning, seqNoOnlineRunning;
    private long lastDelayMsgTimeSend;
    private long lastMsgTimeSend;
    private Map<String, JXSeqNoModel> jxSeqNoModelMap = new HashMap<>();
    // 因为群离线消息数量会全部返回，且在接收的地方已经更新数据库，此时如果群离线消息大于一百条小于一百五十条
    // 剩下的几十条消息就云善绘过补解决补上，此时这些消息是不需要添加未读数量的，新增一个变量，用于存储离线期间待补的消息
    private Map<String, String> groupDelayMsgSeqNoMap = new HashMap<>();

    private SeqNoManager() {

    }

    public static synchronized SeqNoManager getInstance() {
        if (instance == null) {
            instance = new SeqNoManager();
        }
        return instance;
    }

    /**
     * 开始检测，一般为长连接连上
     */
    void checkThreadStart() {
        startSingleTask();
        Log.e(TAG, "长连接连上，遍历本地好友群组，将lostSeqNos不为空的对象更新到内存中");
        jxSeqNoModelMap.clear();
        List<Friend> data = FriendDao.getInstance().getAllFriends(CoreManager.requireSelf(MyApplication.getContext()).getUserId());
        for (int i = 0; i < data.size(); i++) {
            Friend friend = data.get(i);
            if (!TextUtils.isEmpty(friend.getLoseSeqNos())) {
                JXSeqNoModel jxSeqNoModel = new JXSeqNoModel();
                jxSeqNoModel.setUserId(friend.getUserId());
                jxSeqNoModel.setMaxSeqNo(friend.getMaxSeqNo());
                if (friend.getRoomFlag() != 0) {
                    jxSeqNoModel.setGroup(true);
                }
                jxSeqNoModel.setLoseSeqNos(friend.getLoseSeqNos());
            }
        }
        Log.e(TAG, "本地遍历完成，开启检测线程");

        seqNoDelayRunning = true;
        seqNoOnlineRunning = true;

        if (seqNoDelayThread == null) {
            seqNoDelayThread = new SeqNoDelayThread();
            seqNoDelayThread.start();
        } else {
            if (!seqNoDelayThread.isAlive()) {
                // 不能直接调用run, 会阻塞当前线程导致无法读取socket数据，
                seqNoDelayThread = new SeqNoDelayThread();
                seqNoDelayThread.start();
            }
        }

        if (seqNoOnlineThread == null) {
            seqNoOnlineThread = new SeqNoOnlineThread();
            seqNoOnlineThread.start();
        } else {
            if (!seqNoOnlineThread.isAlive()) {
                // 不能直接调用run, 会阻塞当前线程导致无法读取socket数据，
                seqNoOnlineThread = new SeqNoOnlineThread();
                seqNoOnlineThread.start();
            }
        }
        Log.e(TAG, "checkThreadStart End");
    }

    /**
     * 结束检测，一般为长连接断开
     */
    void checkThreadEnd() {
        Log.e(TAG, "长连接断开，结束SeqNoDelayThread与SeqNoOnlineThread线程");
        seqNoDelayRunning = false;
        seqNoOnlineRunning = false;
    }

    /**
     * 检查消息序号
     *
     * @param userId
     * @param seqNo
     */
    void checkSeqNo(String userId, long seqNo, boolean isDelayMsg) {
        String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        Log.e(TAG, "准备比较序号 userId-->" + userId);
        JXSeqNoModel jxSeqNoModel = jxSeqNoModelMap.get(userId);
        if (jxSeqNoModel == null) {
            Log.e(TAG, "内存中没有JXSeqNoModel对象，多半为上线后第一次收到此用户的消息，创建一个对象，并取出朋友表中数据赋值");
            jxSeqNoModel = new JXSeqNoModel();
            Friend friend = FriendDao.getInstance().getFriend(ownerId, userId);
            if (friend != null) {
                jxSeqNoModel.setUserId(friend.getUserId());
                jxSeqNoModel.setMaxSeqNo(friend.getMaxSeqNo());
                if (friend.getRoomFlag() != 0) {
                    jxSeqNoModel.setGroup(true);
                }
                jxSeqNoModel.setLoseSeqNos(friend.getLoseSeqNos());
                jxSeqNoModelMap.put(userId, jxSeqNoModel);
            } else {
                Log.e(TAG, "Friend表没有这个Friend，那先不管了，多半为陌生人，后面逻辑会把陌生人存表");
                return;
            }
        }

        String loseSeqNos = jxSeqNoModel.getLoseSeqNos();
        Log.e(TAG, "JXSeqNoModel对象有了，开始判断内存中loseSeqNos是否包含seqNo，兼容消息先发后到的情况");
        if (!TextUtils.isEmpty(loseSeqNos) && loseSeqNos.contains(String.valueOf(seqNo))) {
            Log.e(TAG, "内存中loseSeqNos包含了seqNo，删掉ta");
            loseSeqNos = loseSeqNos.replaceAll(seqNo + ",", "");
            updateLoseSeqNos(ownerId, userId, loseSeqNos);
        } else {
            Log.e(TAG, "没有，往下走");
        }

        long maxSeqNo = jxSeqNoModel.getMaxSeqNo();
        Log.e(TAG, "本地maxSeqNo为-->，" + maxSeqNo);
        Log.e(TAG, "当前的seqNo为-->，" + seqNo + "，开始比较序号");
        if (maxSeqNo == 0) {
            Log.e(TAG, "maxSeqNo == 0，更新内存与朋友表中的maxSeqNo在return");
            updateSeqNo(ownerId, userId, seqNo);
            return;
        }

        long result = seqNo - maxSeqNo;
        if (result == 1) {
            Log.e(TAG, "序号正常，更新内存与朋友表中的maxSeqNo即可");
            updateSeqNo(ownerId, userId, seqNo);

            if (getLastCallbackCreatedTask.containsKey(userId + seqNo)) {
                Log.e(TAG, "getLastCallbackCreatedTask内存储了一个endSeqNo为当前消息的seqNo+1的任务，这个任务作废，删掉ta");
                MsgRoamTaskDao.getInstance().deleteMsgRoamTask(getLastCallbackCreatedTask.get(userId + seqNo));
            }

        } else if (result < 1) {
            // 等于就不太可能了
            Log.e(TAG, "当前序号小于maxSeqNo，此消息有可能是先发后到了，先发后到可以不做任何处理，但是还有一种可能为正在收消息时自己本地发了消息更新了maxSeqNo，此时需要处理");
            updateSeqNo(ownerId, userId, seqNo);
        } else {
            Log.e(TAG, "当前序号大于maxSeqNo，先更新内存与本地，拼接丢失的序号");
            updateSeqNo(ownerId, userId, seqNo);

            if (!jxSeqNoModel.isGroup()) {
                Log.e(SeqNoManager.TAG, "单聊：因为下面准备去补消息或者存任务了，" +
                        "所以需要先检查本地是否有startSeqNo== " + maxSeqNo + " 的任务，如果有这个任务就可以删掉了");
                List<MsgRoamTask> tasks = MsgRoamTaskDao.getInstance().getFriendMsgRoamTaskList(ownerId, jxSeqNoModel.getUserId());
                if (tasks.size() > 0) {
                    for (MsgRoamTask task : tasks) {
                        if (task.getStartSeqNo() == maxSeqNo) {
                            Log.e(SeqNoManager.TAG, "一致，删掉任务id为-->" + task.getTaskId() + " 的任务");
                            MsgRoamTaskDao.getInstance().deleteMsgRoamTask(ownerId, jxSeqNoModel.getUserId(), task.getTaskId());
                        }
                    }
                }
            }

            if (result >= 50) {
                // 多点登录时，在其他设备与单人聊天消息超过50条的时候基本都会走到这里来
                // 群离线消息超过150条的时候基本都会走到这里来，因为离线消息最多返回最后一百条
                Log.e(TAG, "此次检测丢失的消息条数大于50条，将消息生成一个任务，存入任务表，就不去更新loseSeqNos了");
                MsgRoamTask msgRoamTask = new MsgRoamTask();
                msgRoamTask.setTaskId(System.currentTimeMillis());
                msgRoamTask.setOwnerId(ownerId);
                msgRoamTask.setUserId(jxSeqNoModel.getUserId());
                msgRoamTask.setStartSeqNo(maxSeqNo);
                msgRoamTask.setEndSeqNo(seqNo);
                MsgRoamTaskDao.getInstance().createMsgRoamTask(msgRoamTask);
                if (jxSeqNoModel.isGroup()) {
                    EventBus.getDefault().post(new EventCreateMucRoomTask(msgRoamTask));
                }
                Log.e(TAG, "任务已存入任务表-->" + msgRoamTask.getStartSeqNo() + " - " + msgRoamTask.getEndSeqNo());

                List<MsgRoamTask> friendMsgRoamTaskList = MsgRoamTaskDao.getInstance().getFriendMsgRoamTaskList(ownerId, jxSeqNoModel.getUserId());
                for (int i = 0; i < friendMsgRoamTaskList.size(); i++) {
                    Log.e(TAG, "当前好友已有任务-->" + friendMsgRoamTaskList.get(i).getStartSeqNo() + " - " + friendMsgRoamTaskList.get(i).getEndSeqNo());
                }

                return;
            }

            StringBuilder cLoseSeqNos = new StringBuilder();
            for (long i = 1; i < result; i++) {
                cLoseSeqNos.append(maxSeqNo + i).append(",");
            }
            Log.e(TAG, "丢失的序号已拼接完成，cLoseSeqNos-->" + cLoseSeqNos + "，准备存到内存与表内");
            if (TextUtils.isEmpty(loseSeqNos)) {
                loseSeqNos = cLoseSeqNos.toString();
            } else {
                loseSeqNos = loseSeqNos + cLoseSeqNos.toString();
            }
            Log.e(TAG, "内存中此用户总丢失的消息序号loseSeqNos为-->" + loseSeqNos + "，准备存到内存与表内");
            if (jxSeqNoModel.isGroup() && isDelayMsg) {
                groupDelayMsgSeqNoMap.put(userId, groupDelayMsgSeqNoMap.get(userId) + loseSeqNos);
                Log.e(TAG, "群离线待补loseSeqNos，groupDelayMsgSeqNoMap内存中也存一份，values-->" + groupDelayMsgSeqNoMap.get(userId));
            }
            updateLoseSeqNos(ownerId, userId, loseSeqNos);
            Log.e(TAG, "判断loseSeqNos内数据是否已经超过二十条了");
            String[] split = loseSeqNos.split(",");
            if (split.length > 20) {
                Log.e(TAG, "超过了，准备补消息了，先判断当前是否正在补消息");
                if (jxSeqNoModel.isGetLostSeqNo()) {
                    Log.e(TAG, "正在补消息，那先不补了，等待下次检测触发");
                } else {
                    Log.e(TAG, "没有补消息，那先将待补的消息放在updateSeqNos内，isLostSeqNo状态置为true，在更新内存，调用接口补消息");
                    jxSeqNoModel.setUpdateSeqNos(loseSeqNos);
                    jxSeqNoModel.setGetLostSeqNo(true);
                    jxSeqNoModelMap.put(userId, jxSeqNoModel);
                    // todo 调接口补消息
                    fillMessage(userId, loseSeqNos, jxSeqNoModel.isGroup());
                }
            } else {
                Log.e(TAG, "没有超过，不管了，此次检查结束");
            }
        }
    }

    /**
     * 内存中移除对应的model
     *
     * @param userId
     */
    public void removeModel(String userId) {
        if (jxSeqNoModelMap != null) {
            jxSeqNoModelMap.remove(userId);
        }
    }

    /**
     * 更新消息序号
     * 1.收消息时检查序号更新内存与本地的seqNo
     * 2.发消息时更新内存与本地的seqNo，如之前未收到对方的聊天消息，内存中无值，不更新内存，仅更新数据库(因为收消息校验时内存中无值会给ta赋值，且值从数据库取)
     *
     * @param ownerId
     * @param userId
     * @param seqNo
     */
    public void updateSeqNo(String ownerId, String userId, long seqNo) {
        JXSeqNoModel jxSeqNoModel = jxSeqNoModelMap.get(userId);
        if (jxSeqNoModel != null) {
            jxSeqNoModel.setMaxSeqNo(seqNo);
            jxSeqNoModelMap.put(userId, jxSeqNoModel);
        }
        FriendDao.getInstance().updateFriendMaxSeqNo(ownerId, userId, seqNo);
    }

    void checkLoseSeqNos() {
        Log.e(TAG, "准备检查内存中lostSeqNos不为空的对象");
        List<JXSeqNoModel> jxSeqNoModels = new ArrayList<>(jxSeqNoModelMap.values());
        for (int i = 0; i < jxSeqNoModels.size(); i++) {
            JXSeqNoModel jxSeqNoModel = jxSeqNoModels.get(i);
            if (!TextUtils.isEmpty(jxSeqNoModel.getLoseSeqNos())) {
                Log.e(TAG, jxSeqNoModel.getUserId() + "的loseSeqNos为--->，" + jxSeqNoModel.getLoseSeqNos() + "，准备处理");
                if (jxSeqNoModel.isGetLostSeqNo()) {
                    Log.e(TAG, jxSeqNoModel.getUserId() + "当前正在补消息，那先不补了");
                } else {
                    Log.e(TAG, "没有补消息，那先将待补的消息放在updateSeqNos内，isLostSeqNo状态置为true，在更新内存，调用接口补消息");
                    jxSeqNoModel.setUpdateSeqNos(jxSeqNoModel.getLoseSeqNos());
                    jxSeqNoModel.setGetLostSeqNo(true);
                    jxSeqNoModelMap.put(jxSeqNoModel.getUserId(), jxSeqNoModel);
                    // todo 调接口补消息
                    fillMessage(jxSeqNoModel.getUserId(), jxSeqNoModel.getLoseSeqNos(), jxSeqNoModel.isGroup());
                }
            } else {
                Log.e(TAG, jxSeqNoModel.getUserId() + "的loseSeqNos为空，不处理");
            }
        }
    }

    /**
     * 更新丢失消息序号丢列
     * 1.收消息时检查序号更新内存与本地的loseSeqNos
     *
     * @param ownerId
     * @param userId
     * @param loseSeqNos
     */
    void updateLoseSeqNos(String ownerId, String userId, String loseSeqNos) {
        JXSeqNoModel jxSeqNoModel = jxSeqNoModelMap.get(userId);
        if (jxSeqNoModel != null) {
            // jxSeqNoModel理论上不会为空
            jxSeqNoModel.setLoseSeqNos(loseSeqNos);
            jxSeqNoModelMap.put(userId, jxSeqNoModel);
        }
        FriendDao.getInstance().updateFriendLoseSeqNos(ownerId, userId, loseSeqNos);
    }

    void updateTime(boolean isDelay) {
        if (isDelay) {
            lastDelayMsgTimeSend = System.currentTimeMillis();
        } else {
            lastMsgTimeSend = System.currentTimeMillis();
        }
    }

    private void fillMessage(String userId, String loseSeqNos, boolean isGroup) {
        Log.e(TAG, "准备调接口，补充 " + userId + " 丢失的 " + loseSeqNos + " 消息");
        Map<String, String> params = new HashMap();
        params.put(!isGroup ? "toUserId" : "roomJid", userId);
        params.put("seqNos", loseSeqNos);
        String url = !isGroup ? CoreManager.requireConfig(MyApplication.getContext()).GET_CHAT_MSG_BY_SEQ_NO
                : CoreManager.requireConfig(MyApplication.getContext()).GET_CHAT_MSG_MUC_BY_SEQ_NO;
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<FillChatMessage>(FillChatMessage.class) {
                    @Override
                    public void onResponse(ObjectResult<FillChatMessage> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            boolean isGroup = false;
                            List<ChatRecord> data = result.getData().getResultList();
                            Log.e(TAG, "服务端响应回来，返回了" + data.size() + "消息，清空掉内存中" + result.getData().getUserId() + " 与updateSeqNos对应的loseSeqNos，与updateSeqNos，状态置为false");
                            JXSeqNoModel jxSeqNoModel = jxSeqNoModelMap.get(result.getData().getUserId());
                            if (jxSeqNoModel != null) {
                                isGroup = jxSeqNoModel.isGroup();
                                // 不能直接replaceAll，因为有可能在接口调用期间loseSeqNos移除掉了部分序号
                                // jxSeqNoModel.setLoseSeqNos(jxSeqNoModel.getLoseSeqNos().replaceAll(jxSeqNoModel.getUpdateSeqNos(), ""));
                                List<String> loseSeqNosList = new ArrayList<>(Arrays.asList(jxSeqNoModel.getLoseSeqNos().split(",")));
                                List<String> updateSeqNosList = new ArrayList<>(Arrays.asList(jxSeqNoModel.getUpdateSeqNos().split(",")));
                                loseSeqNosList.removeAll(updateSeqNosList);
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < loseSeqNosList.size(); i++) {
                                    stringBuilder.append(loseSeqNosList.get(i)).append(",");
                                }
                                jxSeqNoModel.setLoseSeqNos(stringBuilder.toString());

                                jxSeqNoModel.setGetLostSeqNo(false);
                                jxSeqNoModel.setUpdateSeqNos("");
                                jxSeqNoModelMap.put(result.getData().getUserId(), jxSeqNoModel);
                                FriendDao.getInstance().updateFriendLoseSeqNos(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), result.getData().getUserId(), jxSeqNoModel.getLoseSeqNos());
                                Log.e(TAG, "清空完了，看下loseSeqNos-->" + jxSeqNoModel.getLoseSeqNos());
                            }
                            for (int i = 0; i < data.size(); i++) {
                                ChatRecord chatRecord = data.get(i);
                                String body = chatRecord.getMessage();
                                // body = body.replaceAll("&quot;", "\"");
                                ChatMessage chatMessage = jsonToMessage(body);
                                if (chatMessage.getType() == XmppMessage.TYPE_READ) {
                                    // 序列号被消息不处理已读26消息，
                                    continue;
                                }
                                if (!TextUtils.isEmpty(chatMessage.getFromUserId()) &&
                                        chatMessage.getFromUserId().equals(CoreManager.requireSelf(MyApplication.getContext()).getUserId())) {
                                    chatMessage.setMySend(true);
                                }

                                // 单聊的接口有返回是否已读，
                                chatMessage.setSendRead(chatRecord.getIsRead() > 0);
                                chatMessage.setReadPersons(chatRecord.getReadCount());
                                // 漫游的默认已上传
                                chatMessage.setUpload(true);
                                chatMessage.setUploadSchedule(100);
                                chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);

                                if (TextUtils.isEmpty(chatMessage.getPacketId())) {
                                    if (!TextUtils.isEmpty(chatRecord.getMessageId())) {
                                        chatMessage.setPacketId(chatRecord.getMessageId());
                                    } else {
                                        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                    }
                                }
                                if (chatRecord.getSeqNo() > 0) {
                                    chatMessage.setSeqNo(chatRecord.getSeqNo());
                                }
                                if (ChatMessageDao.getInstance().saveRoamingChatMessage(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), result.getData().getUserId(), chatMessage, isGroup)) {
                                    if (isGroup
                                            && !TextUtils.isEmpty(groupDelayMsgSeqNoMap.get(jxSeqNoModel.getUserId()))
                                            && groupDelayMsgSeqNoMap.get(jxSeqNoModel.getUserId()).contains(String.valueOf(chatMessage.getSeqNo()))) {
                                        Log.e(TAG, "群离线待补消息，不调用notifyNewMesssage方法");
                                        continue;
                                    }
                                    ListenerManager.getInstance().notifyNewMesssage(CoreManager.requireSelf(MyApplication.getContext()).getUserId()
                                            , chatMessage.isMySend() ? CoreManager.requireSelf(MyApplication.getContext()).getUserId() : result.getData().getUserId()
                                            , chatMessage
                                            , isGroup);
                                }
                            }
                        } else {
                            Log.e(TAG, "服务端响应回来，未返回消息，清空掉内存中" + userId + " 的updateSeqNos，状态置为false");
                            // todo 这个方法在请求时可能多次被调用，又因为异步，到时候可能会移错了...
                            JXSeqNoModel jxSeqNoModel = jxSeqNoModelMap.get(userId);
                            if (jxSeqNoModel != null) {
                                jxSeqNoModel.setGetLostSeqNo(false);
                                jxSeqNoModel.setUpdateSeqNos("");
                                jxSeqNoModelMap.put(userId, jxSeqNoModel);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "补充 " + userId + " 的消息时异常，清空掉内存中" + userId + " 的updateSeqNos，状态置为false");
                        JXSeqNoModel jxSeqNoModel = jxSeqNoModelMap.get(userId);
                        if (jxSeqNoModel != null) {
                            jxSeqNoModel.setGetLostSeqNo(false);
                            jxSeqNoModel.setUpdateSeqNos("");
                            jxSeqNoModelMap.put(userId, jxSeqNoModel);
                        }
                    }
                });
    }

    void startSingleTask() {
        isWaitGetLasRequestResponse = true;
        waitGetLasRequestResponseReceivedMsg.clear();
    }

    void releaseSingleTask() {
        isWaitGetLasRequestResponse = false;
        waitGetLasRequestResponseReceivedMsg.clear();
    }

    private ChatMessage jsonToMessage(String json) {
        Gson gson = new Gson();
        com.tongxin.caihong.socket.msg.ChatMessage chatMessage = gson.fromJson(json, com.tongxin.caihong.socket.msg.ChatMessage.class);
        return chatMessage.toSkMessage(CoreManager.requireSelf(MyApplication.getContext()).getUserId());
    }

    /**
     * 离线消息序号检查
     */
    private class SeqNoDelayThread extends Thread {

        SeqNoDelayThread() {

        }

        @Override
        public void run() {
            while (seqNoDelayRunning) {
                try {
                    // 每隔3s检测一次
                    Thread.sleep(3000);
                    if (System.currentTimeMillis() - lastDelayMsgTimeSend > 12 * 1000) {
                        Log.e(TAG, "当前时间 - 收到离线消息时的时间 > 12 * 1000，判断离线消息已收完，检查内存中的lostSeqNos，判断是否有离线消息丢失，同时结束该线程");
                        // 如当前时间 - 收到离线消息时的时间 > 12s
                        // 我们判断离线消息已收完，检查内存中的lostSeqNos，判断是否有离线消息丢失
                        // 同时结束该线程
                        checkLoseSeqNos();
                        seqNoDelayRunning = false;
                        seqNoDelayThread = null;
                    } else {
                        Log.e(TAG, "当前时间 - 收到离线消息时的时间<= 12 * 1000，判断离线消息还在收，继续执行while循环");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 在线消息序号检查
     */
    private class SeqNoOnlineThread extends Thread {

        SeqNoOnlineThread() {
        }

        @Override
        public void run() {
            while (seqNoOnlineRunning) {
                try {
                    // 每隔6s检测一次
                    Thread.sleep(6000);
                    if (System.currentTimeMillis() - lastMsgTimeSend < 6 * 1000) {
                        // 如当前时间 - 收到在线消息时的时间 < 10s
                        // 我们判读此时该用户还在聊天，检查内存中的lostSeqNos，判断在此期间是否有在线消息丢失
                        // 同时将lastMsgTimeSend置为当前时间
                        Log.e(TAG, "当前时间 - 收到在线消息时的时间 < 6 * 1000，此时该用户还在聊天，检查内存中的lostSeqNos，判断在此期间是否有在线消息丢失，同时将lastMsgTimeSend置为当前时间");
                        checkLoseSeqNos();
                        lastMsgTimeSend = System.currentTimeMillis();
                    } else {
                        // 如当前时间 - 收到在线消息时的时间 >= 10s
                        // 我们判断用户一直都没有聊天消息产生，将lastMsgTimeSend置为当前时间即可
                        lastMsgTimeSend = System.currentTimeMillis();
                        Log.e(TAG, "当前时间 - 收到离线消息时的时间<= 6 * 1000，我们判断用户一直都没有聊天消息产生，将lastMsgTimeSend置为当前时间即可，继续执行while循环");
                    }

                    /**
                     * 实现后台采集位置信息功能，借用序号功能的定时器
                     * 因为该线程是和长连接的连上与断开有关，所以该功能目前只支持长连接已连上时，定时上传位置信息
                     * 如果想实现app开启之后就一直定时上传位置信息，需要单独开启一个线程做以下操作
                     */
                    if (PrivacySettingHelper.getPrivacySettings(MyApplication.getContext()).getAllowLocation() == 1) {
                        // 允许后台采集位置信息
                        String userId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
                        long lastUploadLocationTime = PreferenceUtils.getLong(MyApplication.getContext(),
                                Constants.LAST_UPLOAD_LOCATION_TIME + userId, 0);
                        if (TimeUtils.sk_time_current_time() / 1000 - lastUploadLocationTime >= CoreManager.requireConfig(MyApplication.getContext()).locateInterval) {
                            // 当前时间 - 上次上传位置信息时间大于 >= 规定的时间间隔，上传位置信息
                            HandleActivityDestroyedDoSomeThing.handleUploadLocation(userId);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
