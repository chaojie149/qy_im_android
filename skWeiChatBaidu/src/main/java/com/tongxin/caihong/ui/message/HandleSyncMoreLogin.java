package com.tongxin.caihong.ui.message;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.bean.PrivacySetting;
import com.tongxin.caihong.bean.SyncBean;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.LabelDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.CertificationHelper;
import com.tongxin.caihong.helper.FriendHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.account.RegisterUserBasicInfoActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.xmpp.CoreService;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.SeqNoManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class HandleSyncMoreLogin {
    private static final String TAG = "sync";
    private static final String SYNC_LOGIN_PASSWORD = "sync_login_password";
    private static final String SYNC_PAY_PASSWORD = "sync_pay_password";
    private static final String SYNC_PRIVATE_CHAT_PASSWORD = "sync_private_chat_password";
    private static final String SYNC_YOP_OPEN_ACCOUNT = "sync_yop_open_account";
    private static final String SYNC_AUTH_CERTIFICATION = "sync_realname_certification";
    private static final String SYNC_PRIVATE_SETTINGS = "sync_private_settings";
    private static final String SYNC_LABEL = "sync_label";

    public static void distributionService(SyncBean syncBean, CoreService coreService) {
        if (syncBean.getTag().equals("label")) {
            if (RegisterUserBasicInfoActivity.isRegisteredSyncCount > 0) {
                RegisterUserBasicInfoActivity.isRegisteredSyncCount--;
                return;// 注册成功之后服务端会调接口给用户创建两个标签，所以off..接口会获取到tag为label的数据，不处理
            }
            handleLabelUpdate();
        } else if (syncBean.getTag().equals("friend")) {
            handleUserUpdate(syncBean.getFriendId());
        } else if (syncBean.getTag().equals("room")) {
            handleGroupUpdate(syncBean.getFriendId(), coreService);
        }
    }

    public static void distributionChatMessage(ChatMessage chatMessage, CoreService coreService, boolean isDelay) {
        if (chatMessage.getType() == XmppMessage.TYPE_SYNC_OTHER) {
            if (!TextUtils.isEmpty(chatMessage.getObjectId())) {
                if (chatMessage.getObjectId().equals(SYNC_LOGIN_PASSWORD)) {
                    if (!isDelay) {
                        // 保险起见，再加一个判断，如果此消息timeSend与当前时间间隔超过1Min，也不处理
                        if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() > 60 * 1000) {
                            Log.e(TAG, "当前时间 - 消息发送时间 间隔超过1Min，保险起见，不处理此条消息");
                        } else {
                            handleChangeLoginPassword();
                        }
                    }
                } else if (chatMessage.getObjectId().equals(SYNC_PAY_PASSWORD)) {
                    handleSetPayPassword();
                } else if (chatMessage.getObjectId().equals(SYNC_PRIVATE_CHAT_PASSWORD)) {
                    handleSetPrivateChatPassword();
                } else if (chatMessage.getObjectId().equals(SYNC_YOP_OPEN_ACCOUNT)) {
                    handleSetYopOpenAccount();
                } else if (chatMessage.getObjectId().equals(SYNC_AUTH_CERTIFICATION)) {
                    handleSetAuthCertificationAccount();
                } else if (chatMessage.getObjectId().equals(SYNC_PRIVATE_SETTINGS)) {
                    handlePrivateSettingsUpdate();
                } else if (chatMessage.getObjectId().equals(SYNC_LABEL)) {
                    if (RegisterUserBasicInfoActivity.isRegisteredSyncCount > 0) {
                        RegisterUserBasicInfoActivity.isRegisteredSyncCount--;
                        return;// 注册成功之后服务端会调接口给用户创建两个标签，所以会收到两条同步消息，不处理
                    }
                    handleLabelUpdate();
                }
            }
        } else if (chatMessage.getType() == XmppMessage.TYPE_SYNC_FRIEND) {
            handleUserUpdate(chatMessage.getToUserId());
        } else if (chatMessage.getType() == XmppMessage.TYPE_SYNC_GROUP) {
            handleGroupUpdate(chatMessage.getToUserId(), coreService);
        } else if (chatMessage.getType() == XmppMessage.TYPE_SEAL) {
            if (!isDelay) {
                // 保险起见，再加一个判断，如果此消息timeSend与当前时间间隔超过1Min，也不处理
                if (TimeUtils.sk_time_current_time() - chatMessage.getTimeSend() > 60 * 1000) {
                    Log.e(TAG, "当前时间 - 消息发送时间 间隔超过1Min，保险起见，不处理此条消息");
                } else {
                    handleChangeLoginPassword();
                }
            }
        } else if (chatMessage.getType() == XmppMessage.TYPE_FRIEND_CHANGE_NAME) {
            handleChangeName(chatMessage.getFromUserId(), chatMessage.getFromUserName());
        } else if (chatMessage.getType() == XmppMessage.TYPE_FRIEND_CHANGE_ENCRYPT_TYPE) {
            handleChangeEncryptType(chatMessage);
        }
    }

    private static void handleChangeEncryptType(ChatMessage chatMessage) {
        String selfId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        Friend friend = FriendDao.getInstance().getFriend(selfId, chatMessage.getFromUserId());
        int encryptType = Integer.parseInt(chatMessage.getContent());
        if (friend != null) {
            FriendDao.getInstance().updateEncryptType(friend.getUserId(), encryptType);
        }
        chatMessage.setType(XmppMessage.TYPE_TIP);
        String content = "";
        if (encryptType == 0) {
            content = MyApplication.getContext().getString(R.string.tip_sync_encrypt_type_setting
                    , MyApplication.getContext().getString(R.string.msg_transmission_public));
        } else if (encryptType == 1) {
            content = MyApplication.getContext().getString(R.string.tip_sync_encrypt_type_setting
                    , MyApplication.getContext().getString(R.string.msg_transmission_desed));
        } else if (encryptType == 2) {
            content = MyApplication.getContext().getString(R.string.tip_sync_encrypt_type_setting
                    , MyApplication.getContext().getString(R.string.msg_transmission_aes));
        } else if (encryptType == 3) {
            content = MyApplication.getContext().getString(R.string.tip_sync_encrypt_type_setting
                    , MyApplication.getContext().getString(R.string.msg_transmission_asymmetric_aes));
        }
        chatMessage.setContent(content);
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(selfId, chatMessage.getFromUserId(), chatMessage)) {
            ListenerManager.getInstance().notifyNewMesssage(selfId, chatMessage.getFromUserId(), chatMessage, false);
        }
        // 通知聊天设置页更新
        Intent intent = new Intent(OtherBroadcast.ACTION_SYNC_ENCRYPT_TYPE);
        intent.putExtra("fromUserId", chatMessage.getFromUserId());
        intent.putExtra("encryptType", encryptType);
        intent.putExtra("content", content);
        MyApplication.getContext().sendBroadcast(intent);
    }

    private static void handleChangeName(String friendUserId, String friendUserName) {
        String selfId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        Friend friend = FriendDao.getInstance().getFriend(selfId, friendUserId);
        if (friend != null && !TextUtils.equals(friend.getNickName(), friendUserName)) {
            friend.setNickName(friendUserName);
            FriendDao.getInstance().updateNickName(selfId, friend.getUserId(), friend.getNickName());
        }
        MsgBroadcast.broadcastMsgUiUpdateSingle(MyApplication.getInstance(), friendUserId);
        // 更新通讯录页面
        CardcastUiUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
        // 刷新聊天页，
        OtherBroadcast.broadcastNameChange(MyApplication.getContext(), friendUserId, friendUserName);
    }

    private static void handleChangeLoginPassword() {
        Log.e(TAG, "多点登录同步--->登录密码在其他端被修改了");
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_OVERDUE;
        LoginHelper.broadcastLogout(MyApplication.getContext());
    }

    private static void handleSetPayPassword() {
        Log.e(TAG, "多点登录同步--->更新是否设置过支付密码的状态");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getContext()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->更新好友的信息成功");
                            User user = result.getData();
                            MyApplication.getInstance().initPayPassword(user.getUserId(),
                                    TextUtils.equals(user.getPayPassword(), "1") ? 1 : 0);
                        } else {
                            Log.e(TAG, "多点登录同步--->更新是否设置过支付密码的状态失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新是否设置过支付密码的状态失败");
                    }
                });
    }

    private static void handleSetPrivateChatPassword() {
        Log.e(TAG, "多点登录同步--->更新私密模式密码");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getContext()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->更新好友的信息成功");
                            User user = result.getData();
                            // todo
                        } else {
                            Log.e(TAG, "多点登录同步--->更新私密模式密码失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新私密模式密码失败");
                    }
                });
    }

    private static void handleSetYopOpenAccount() {
        Log.e(TAG, "多点登录同步--->更新是否设置过易宝开户的状态");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getContext()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->更新好友的信息成功");
                            User user = result.getData();
                            WeboxHelper.saveWalletId(MyApplication.getInstance(), user.getWalletId());
                        } else {
                            Log.e(TAG, "多点登录同步--->更新是否设置过易宝开户的状态失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新是否设置过易宝开户的状态失败");
                    }
                });
    }

    private static void handleSetAuthCertificationAccount() {
        Log.e(TAG, "多点登录同步--->更新是否实名认证的状态");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getContext()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->实名认证成功");
                            User user = result.getData();
                            CertificationHelper.saveAuth(MyApplication.getInstance(), user.getRealNameCertified() == 1);
                        } else {
                            Log.e(TAG, "多点登录同步--->更新是否实名认证的状态失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新是否实名认证的状态失败");
                    }
                });
    }

    private static void handlePrivateSettingsUpdate() {
        Log.e(TAG, "多点登录同步--->更新隐私设置");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getContext()).getUserId());

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).USER_GET_PRIVACY_SETTING)
                .params(params)
                .build()
                .execute(new BaseCallback<PrivacySetting>(PrivacySetting.class) {

                    @Override
                    public void onResponse(ObjectResult<PrivacySetting> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->更新隐私成功");
                            PrivacySetting settings = result.getData();
                            PrivacySettingHelper.setPrivacySettings(MyApplication.getContext(), settings);
                        } else {
                            Log.e(TAG, "多点登录同步--->更新隐私失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新隐私失败");
                    }
                });
    }

    private static void handleLabelUpdate() {
        Log.e(TAG, "多点登录同步--->更新标签");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).FRIENDGROUP_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Label>(Label.class) {
                    @Override
                    public void onResponse(ArrayResult<Label> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->更新标签成功");
                            List<Label> labelList = result.getData();
                            LabelDao.getInstance().refreshLabel(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), labelList);
                        } else {
                            Log.e(TAG, "多点登录同步--->更新标签失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新标签失败");
                    }
                });
    }

    private static void handleUserUpdate(String userId) {
        if (TextUtils.equals(userId, CoreManager.requireSelf(MyApplication.getContext()).getUserId())) {
            handleSelfUpdate();
        } else {
            handleFriendUpdate(userId);
        }
    }

    private static void handleSelfUpdate() {
        Log.e(TAG, "多点登录同步--->更新自己的信息");
        MyApplication.getContext().sendBroadcast(new Intent(OtherBroadcast.SYNC_SELF_DATE));
    }

    private static void handleFriendUpdate(String userId) {
        Log.e(TAG, "多点登录同步--->更新好友的信息");
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", userId);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            Log.e(TAG, "多点登录同步--->更新好友的信息成功");
                            User user = result.getData();
                            // 服务器的状态 与本地状态对比
                            if (FriendHelper.updateFriendRelationship(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), user)) {
                                CardcastUiUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
                            }
                            if (user.getFriends() != null) {// 更新消息免打扰状态 && 更新消息保存天数...
                                boolean isTopOld = false;
                                boolean isTopNew = false;
                                Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), userId);
                                if (friend != null) {
                                    isTopOld = friend.getTopTime() > 0;
                                    isTopNew = user.getFriends().getOpenTopChatTime() > 0;
                                }

                                FriendDao.getInstance().updateFriendPartStatus(userId, user);
                                if (!TextUtils.equals(String.valueOf(isTopOld), String.valueOf(isTopNew))) {
                                    // 置顶状态有改变，刷新消息界面
                                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                                }
                                MyApplication.getContext().sendBroadcast(new Intent(OtherBroadcast.ACTION_REFURE_CURRENT_CHAT_ACTIVITY));
                            }
                        } else {
                            Log.e(TAG, "多点登录同步--->更新好友的信息失败");
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e(TAG, "多点登录同步--->更新好友的信息失败");
                    }
                });
    }

    private static void handleGroupUpdate(String roomId, CoreService coreService) {
        Log.e(TAG, "多点登录同步--->更新群组的信息");
        String loginUserId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        Friend friend = FriendDao.getInstance().getMucFriendByRoomId(loginUserId, roomId);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// 数据结果与room/get接口一样，只是服务端没有返回群成员列表的数据
                                 Log.e(TAG, "多点登录同步--->更新群组的信息成功");
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();
                                     if (mucRoom.getMember() == null) {// 被踢出群组
                                         if (friend != null) {
                                             delete(loginUserId, friend, coreService);
                                         }
                                     } else {
                                         if (friend == null) {// 创建
                                             create(loginUserId, mucRoom, coreService);
                                         } else {// 更新
                                             update(friend, mucRoom, coreService);
                                         }
                                     }
                                 } else {// 该群组被解散了
                                     delete(loginUserId, friend, coreService);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 Log.e(TAG, "多点登录同步--->更新群组的信息失败");
                             }
                         }
                );
    }

    private static void create(String userId, MucRoom mucRoom, CoreService coreService) {
        if (!MyApplication.IS_SUPPORT_SECURE_CHAT) {
            // SecureFlagGroup 非端到端，兼容之前在端到端版本注册过的账号，过滤掉私密群组
            if (mucRoom != null && mucRoom.getIsSecretGroup() == 1) {
                return;
            }
        }
        if (mucRoom == null || mucRoom.getMember() == null) {
            // 理论上不太可能
            return;
        }
        Friend friend = new Friend();
        friend.setOwnerId(userId);
        friend.setUserId(mucRoom.getJid());
        friend.setNickName(mucRoom.getName());
        friend.setDescription(mucRoom.getDesc());
        friend.setContent("");
        friend.setRoomId(mucRoom.getId());
        friend.setRoomFlag(1);
        friend.setRoomRole(mucRoom.getMember().getRole());
        friend.setStatus(Friend.STATUS_FRIEND);
        friend.setGroupStatus(0);
        friend.setTimeSend(mucRoom.getMember().getCreateTimeL() * 1000);
        friend.setIsSecretGroup(mucRoom.getIsSecretGroup());
        friend.setJoinSeqNo(mucRoom.getMember().getJoinSeqNo());
        friend.setOfflineNoPushMsg(mucRoom.getMember().getOfflineNoPushMsg());
        friend.setTopTime(mucRoom.getMember().getOpenTopChatTime());
        if (friend.getIsSecretGroup() == 1) {
            try {
                String chatKey = new String(RSA.decryptFromBase64(mucRoom.getMember().getChatKeyGroup(), Base64.decode(SecureChatUtil.getRSAPrivateKey(userId))));
                friend.setChatKeyGroup(SecureChatUtil.encryptChatKey(mucRoom.getJid(), chatKey));
                Log.e("msg", "设置chatKey成功-->" + chatKey);
            } catch (Exception e) {
                Log.e("msg", "设置chatKey失败-->");
                friend.setIsLostChatKeyGroup(friend.getUserId(), 1);
            }
        }

        FriendDao.getInstance().createOrUpdateFriend(friend);

        long time = TimeUtils.sk_time_current_time() - mucRoom.getMember().getCreateTimeL() * 1000;
        coreService.joinMucChat2(mucRoom.getJid(), time);
        MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
        MucgroupUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
    }

    private static void delete(String userId, Friend friend, CoreService coreService) {
        if (friend != null) {
            coreService.exitMucChat(friend.getUserId());
            FriendDao.getInstance().deleteFriend(userId, friend.getUserId());
            // 消息表中删除
            ChatMessageDao.getInstance().deleteMessageTable(userId, friend.getUserId());
            RoomMemberDao.getInstance().deleteRoomMemberTable(friend.getUserId());
            // 通知界面更新
            MsgBroadcast.broadcastMsgNumReset(MyApplication.getContext());
            MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
            MucgroupUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
        }
    }

    /**
     * update 就只更新下消息免打扰与置顶即可
     */
    private static void update(Friend friend, MucRoom mucRoom, CoreService coreService) {
        if (mucRoom.getMember() != null) {
            if (friend.getGroupStatus() == 1) {
                Log.e(TAG, "多点登录同步--->更新群组的信息成功--->friend.getGroupStatus() == 1");
                // 本地群组之前为被踢出状态，离线期间被拉入群组了
                // 更新本地群组状态
                FriendDao.getInstance().updateFriendGroupStatus(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), mucRoom.getJid(), 0);
                // 群聊的907单聊处理了，此处需要+1
                Log.e(SeqNoManager.TAG, "被踢出之后又被邀请入群，更新本地maxSeqNo为joinSeqNo+1：" + (mucRoom.getMember().getJoinSeqNo() + 1));
                SeqNoManager.getInstance().updateSeqNo(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), friend.getUserId(), mucRoom.getMember().getJoinSeqNo() + 1);
                FriendDao.getInstance().updateFriendJoinSeqNo(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), friend.getUserId(), mucRoom.getMember().getJoinSeqNo() + 1);
                long time = TimeUtils.sk_time_current_time() - mucRoom.getMember().getCreateTimeL() * 1000;
                coreService.joinMucChat2(mucRoom.getJid(), time);
            }
            FriendDao.getInstance().updateOfflineNoPushMsgStatus(mucRoom.getJid(), mucRoom.getMember().getOfflineNoPushMsg());
            if (mucRoom.getMember().getOpenTopChatTime() > 0) {
                FriendDao.getInstance().updateTopFriend(mucRoom.getJid(), mucRoom.getMember().getOpenTopChatTime());
            } else {
                FriendDao.getInstance().resetTopFriend(mucRoom.getJid());
            }

            boolean isTopOld = friend.getTopTime() > 0;
            boolean isTopNew = mucRoom.getMember().getOpenTopChatTime() > 0;
            if (!TextUtils.equals(String.valueOf(isTopOld), String.valueOf(isTopNew))) {
                // 置顶状态有改变，刷新消息界面
                MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
            }
        }
    }
}
