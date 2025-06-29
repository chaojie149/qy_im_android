package com.tongxin.caihong.xmpp.util;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Contact;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.EventSelfRemoved;
import com.tongxin.caihong.bean.event.EventSyncFriendOperating;
import com.tongxin.caihong.bean.event.MessageContactEvent;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.ContactDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.NewFriendDao;
import com.tongxin.caihong.helper.AccountSwitchHelper;
import com.tongxin.caihong.helper.FriendHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import org.jsoup.helper.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * @author zq
 * 处理朋友消息
 */
public class HandleFriendMessage {
    public static void handleMessage(String mLoginUserId, ChatMessage chatMessage) {
        if (chatMessage.getFromUserId().equals(mLoginUserId)) {
            handleMessageFromMe(mLoginUserId, chatMessage);
        } else {
            handleMessageForMe(mLoginUserId, chatMessage);
        }
    }

    /**
     * 处理自己在其他端发送过来的朋友消息
     * 兼容多点登录
     *
     * @param mLoginUserId
     * @param chatMessage
     */
    private static void handleMessageFromMe(String mLoginUserId, ChatMessage chatMessage) {
        String toUserId = chatMessage.getToUserId();
        String toUserName = chatMessage.getToUserName();
        if (TextUtils.isEmpty(toUserName)) {
            toUserName = "NULL";
        }
        switch (chatMessage.getType()) {
            case XmppMessage.TYPE_SAYHELLO:
                // 我与对方打招呼
                NewFriendMessage message = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                        XmppMessage.TYPE_SAYHELLO, chatMessage.getContent(), toUserId, toUserName);
                NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                NewFriendDao.getInstance().changeNewFriendState(toUserId, Friend.STATUS_10);//朋友状态
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

                // 本地生成一条打招呼的消息
                ChatMessage sayMessage = new ChatMessage();
                sayMessage.setFromUserId(mLoginUserId);
                sayMessage.setFromUserName(CoreManager.requireSelf(MyApplication.getContext()).getNickName());
                sayMessage.setContent(chatMessage.getContent());
                sayMessage.setType(XmppMessage.TYPE_TEXT); //文本类型
                sayMessage.setMySend(true);
                sayMessage.setSendRead(true);// 新的朋友消息默认为已读
                sayMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                sayMessage.setPacketId(chatMessage.getPacketId());
                sayMessage.setTimeSend(TimeUtils.sk_time_current_time());
                ChatMessageDao.getInstance().saveNewSingleChatMessage(message.getOwnerId(), message.getUserId(), sayMessage);
                break;
            case XmppMessage.TYPE_PASS:
                // 我同意了对方的加好友请求
                NewFriendMessage passMessage = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                        XmppMessage.TYPE_PASS, null, toUserId, toUserName);

                NewFriendDao.getInstance().ascensionNewFriend(passMessage, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mLoginUserId, toUserId, chatMessage.getTimeSend());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, toUserId, ConvertMessage.getString(R.string.be_friendand_chat));
                NewFriendDao.getInstance().changeNewFriendState(toUserId, Friend.STATUS_12);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, passMessage, true);
                break;
            case XmppMessage.TYPE_FEEDBACK:
                // 我发送给对方的回话
                NewFriendMessage feedBackMessage = NewFriendDao.getInstance().getNewFriendById(mLoginUserId, toUserId);
                if (feedBackMessage == null) {
                    feedBackMessage = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                            XmppMessage.TYPE_FEEDBACK, chatMessage.getContent(), toUserId, toUserName);
                    NewFriendDao.getInstance().createOrUpdateNewFriend(feedBackMessage);
                }
                if (feedBackMessage.getState() == Friend.STATUS_11 || feedBackMessage.getState() == Friend.STATUS_15) {
                    NewFriendDao.getInstance().changeNewFriendState(feedBackMessage.getUserId(), Friend.STATUS_15);
                } else {
                    NewFriendDao.getInstance().changeNewFriendState(feedBackMessage.getUserId(), Friend.STATUS_14);
                }
                NewFriendDao.getInstance().updateNewFriendContent(feedBackMessage.getUserId(), chatMessage.getContent(), chatMessage.getDoubleTimeSend());

                // 本地也保存一份
                ChatMessage chatFeedMessage = new ChatMessage();
                chatFeedMessage.setType(XmppMessage.TYPE_TEXT); // 文本类型
                chatFeedMessage.setFromUserId(mLoginUserId);
                chatFeedMessage.setFromUserName(CoreManager.requireSelf(MyApplication.getContext()).getNickName());
                chatFeedMessage.setContent(chatMessage.getContent());
                chatFeedMessage.setMySend(true);
                chatFeedMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                chatFeedMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                chatFeedMessage.setTimeSend(TimeUtils.sk_time_current_time());
                ChatMessageDao.getInstance().saveNewSingleAnswerMessage(mLoginUserId, toUserId, chatFeedMessage);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, feedBackMessage, true);
                break;
            case XmppMessage.TYPE_FRIEND:
                // 对方未开启验证，我直接将对方添加为好友
                NewFriendMessage friendMessage = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                        XmppMessage.TYPE_FRIEND, null, toUserId, toUserName);
                NewFriendDao.getInstance().ascensionNewFriend(friendMessage, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mLoginUserId, toUserId, chatMessage.getTimeSend());
                NewFriendDao.getInstance().changeNewFriendState(toUserId, Friend.STATUS_22);
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, toUserId, ConvertMessage.getString(R.string.Msg_View_Controller_Start_Chat));
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, friendMessage, true);
                break;
            case XmppMessage.TYPE_BLACK:
                // 我将对方拉黑
                NewFriendMessage blackMessage = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                        XmppMessage.TYPE_BLACK, null, toUserId, toUserName);
                FriendDao.getInstance().updateFriendStatus(mLoginUserId, toUserId, Friend.STATUS_BLACKLIST);
                FriendHelper.addBlacklistExtraOperation(blackMessage.getOwnerId(), blackMessage.getUserId());
                NewFriendDao.getInstance().createOrUpdateNewFriend(blackMessage);
                NewFriendDao.getInstance().changeNewFriendState(toUserId, Friend.STATUS_18);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, blackMessage, true);
                // 通知聊天界面刷新
                EventBus.getDefault().post(new EventSyncFriendOperating(chatMessage.getToUserId(), chatMessage.getType()));
                break;
            case XmppMessage.TYPE_REFUSED:
                // 我将对方移除黑名单
                NewFriendMessage removeMessage = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                        XmppMessage.TYPE_REFUSED, null, toUserId, toUserName);
                NewFriendDao.getInstance().ascensionNewFriend(removeMessage, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(removeMessage.getOwnerId(), removeMessage.getUserId(), chatMessage.getTimeSend());
                NewFriendDao.getInstance().createOrUpdateNewFriend(removeMessage);
                NewFriendDao.getInstance().changeNewFriendState(toUserId, Friend.STATUS_27);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, removeMessage, true);
                break;
            case XmppMessage.TYPE_DELALL:
                // 我删除了对方
                NewFriendMessage deleteMessage = NewFriendMessage.createLocalMessage(CoreManager.requireSelf(MyApplication.getContext()),
                        XmppMessage.TYPE_DELALL, null, chatMessage.getToUserId(), toUserName);
                // 先从朋友表取出该用户，判断为公众号还是好友
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, chatMessage.getToUserId());
                if (friend != null) {
                    // 可能已经删除了，不重复操作，避免覆盖了content, 主要是删除后status无法判断是否公众号，
                    if (friend.getStatus() != Friend.STATUS_23) {
                        if (friend.getStatus() == Friend.STATUS_SYSTEM) {
                            deleteMessage.setContent(ConvertMessage.getString(R.string.delete_firend_public) + friend.getNickName());
                        } else {
                            deleteMessage.setContent(ConvertMessage.getString(R.string.delete_firend) + toUserName);
                        }
                    }
                    FriendHelper.removeAttentionOrFriend(mLoginUserId, chatMessage.getToUserId());
                    NewFriendDao.getInstance().createOrUpdateNewFriend(deleteMessage);
                    NewFriendDao.getInstance().changeNewFriendState(chatMessage.getToUserId(), Friend.STATUS_16);
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, deleteMessage, true);
                }
                // 通知聊天界面刷新
                EventBus.getDefault().post(new EventSyncFriendOperating(chatMessage.getToUserId(), chatMessage.getType()));
                break;
        }
        // 更新通讯录页面
        CardcastUiUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
    }

    /**
     * 处理朋友发送过来的朋友消息
     */
    private static void handleMessageForMe(String mLoginUserId, ChatMessage chatMessage) {
        // json:fromUserId fromUserName type  content timeSend
        NewFriendMessage mNewMessage = new NewFriendMessage();
        mNewMessage.parserJsonData(chatMessage);
        mNewMessage.setOwnerId(mLoginUserId);
        mNewMessage.setRead(false);
        mNewMessage.setMySend(false);
        String content = "";
        switch (chatMessage.getType()) {
            case XmppMessage.TYPE_SAYHELLO:
                // 对方发过来的打招呼消息
                NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_11);

                ChatMessage sayHelloMessage = new ChatMessage();
                sayHelloMessage.setType(XmppMessage.TYPE_TEXT); //文本类型
                sayHelloMessage.setFromUserId(chatMessage.getFromUserId());
                sayHelloMessage.setFromUserName(chatMessage.getFromUserName());
                if (StringUtil.isBlank(chatMessage.getContent())){
                    sayHelloMessage.setContent(ConvertMessage.getString(R.string.hello));
                }else{
                    sayHelloMessage.setContent(chatMessage.getContent());
                }
                sayHelloMessage.setMySend(false);
                sayHelloMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                sayHelloMessage.setPacketId(chatMessage.getPacketId());
                sayHelloMessage.setTimeSend(chatMessage.getTimeSend());
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, chatMessage.getFromUserId(), sayHelloMessage);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                break;
            case XmppMessage.TYPE_PASS:
                // 对方同意加我为好友
                NewFriendDao.getInstance().ascensionNewFriend(mNewMessage, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId(), chatMessage.getTimeSend());
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_13);//添加了xxx
                content = ConvertMessage.getString(R.string.be_friendand_chat);
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mNewMessage.getUserId(), content);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                break;
            case XmppMessage.TYPE_FEEDBACK: {
                // 对方的回话
                NewFriendMessage feedBackMessage = NewFriendDao.getInstance().getNewFriendById(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);
                if (feedBackMessage.getState() == Friend.STATUS_11 || feedBackMessage.getState() == Friend.STATUS_15) {
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_15);
                } else {
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_14);
                }
                NewFriendDao.getInstance().updateNewFriendContent(mNewMessage.getUserId(), chatMessage.getContent(), chatMessage.getTimeSend());

                ChatMessage message = new ChatMessage();
                message.setType(XmppMessage.TYPE_TEXT);// 文本类型
                message.setFromUserId(mNewMessage.getUserId());
                message.setFromUserName(mNewMessage.getNickName());
                message.setContent(mNewMessage.getContent());
                message.setMySend(false);
                message.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                message.setPacketId(chatMessage.getPacketId());
                message.setTimeSend(TimeUtils.sk_time_current_time());
                ChatMessageDao.getInstance().saveNewSingleAnswerMessage(mLoginUserId, mNewMessage.getUserId(), message);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                break;
            }
            case XmppMessage.TYPE_FRIEND:
                // 我未开启好友验证，对方直接添加我为好友
                NewFriendDao.getInstance().ascensionNewFriend(mNewMessage, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId(), chatMessage.getTimeSend());
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_21);//添加了xxx
                content = ConvertMessage.getString(R.string.be_friendand_chat);
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mNewMessage.getUserId(), content);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                break;
            case XmppMessage.TYPE_BLACK:
                // 对方将我拉黑
                NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);// 本地可能没有该NewFriend，需要先创建在修改其status
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_19);
                FriendHelper.beBlacklistExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                // 关闭聊天界面
                ChatActivity.callFinish(MyApplication.getContext(), ConvertMessage.getString(R.string.be_pulled_black), mNewMessage.getUserId());
                break;
            case XmppMessage.TYPE_REFUSED:
                // 对方将我移出了黑名单
                NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_24);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                // 恢复被拉黑前的关系，无法本地判断，只能调接口，
                Map<String, String> params = new HashMap<>();
                params.put("userId", mNewMessage.getUserId());
                HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_GET_URL)
                        .params(params)
                        .build()
                        .execute(new BaseCallback<User>(User.class) {
                            @Override
                            public void onResponse(ObjectResult<User> result) {
                                if (result.getResultCode() == 1 && result.getData() != null) {
                                    User user = result.getData();
                                    // 本地同步服务器上的好友关系状态，
                                    FriendHelper.updateFriendRelationship(mNewMessage.getOwnerId(), user);
                                    // 获取本地同步后的好友对象，
                                    Friend friend = FriendDao.getInstance().getFriend(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                                    if (friend.getStatus() == Friend.STATUS_FRIEND) {
                                        // 确定取消拉黑恢复了好友关系才更新好友提示，
                                        FriendHelper.addFriendExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId(), chatMessage.getTimeSend());
                                        String content = ConvertMessage.getString(R.string.be_friendand_chat);
                                        FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mNewMessage.getUserId(), content);
                                    }
                                    // 更新通讯录页面
                                    CardcastUiUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e) {

                            }
                        });
                break;
            case XmppMessage.TYPE_DELALL:
                // 对方删除了我
                NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);// 本地可能没有该NewFriend，需要先创建在修改其status
                FriendHelper.removeAttentionOrFriend(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_17);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                // 关闭聊天界面
                ChatActivity.callFinish(MyApplication.getContext(), ConvertMessage.getString(R.string.delete_firend), mNewMessage.getUserId());
                break;
            case XmppMessage.TYPE_CONTACT_BE_FRIEND:
                // 对方通过 手机联系人 添加我 直接成为好友
                NewFriendDao.getInstance().ascensionNewFriend(mNewMessage, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId(), chatMessage.getTimeSend());
                NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_25);// 通过手机联系人添加
                content = ConvertMessage.getString(R.string.be_friendand_chat);
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mNewMessage.getUserId(), content);
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                break;
            case XmppMessage.TYPE_NEW_CONTACT_REGISTER: {
                // 我之前上传给服务端的联系人表内有人注册了，更新 手机联系人
                JSONObject jsonObject = JSONObject.parseObject(chatMessage.getContent());
                Contact contact = new Contact();
                contact.setTelephone(jsonObject.getString("telephone"));
                contact.setToTelephone(jsonObject.getString("toTelephone"));
                String toUserId = jsonObject.getString("toUserId");
                contact.setToUserId(toUserId);
                contact.setToUserName(jsonObject.getString("toUserName"));
                contact.setUserId(jsonObject.getString("userId"));
                if (ContactDao.getInstance().createContact(contact)) {// 本地创建成功 更新未读数量
                    EventBus.getDefault().post(new MessageContactEvent(toUserId));
                }
                break;
            }
            case XmppMessage.TYPE_REMOVE_ACCOUNT: {
                // 用户被后台删除，用于客户端更新本地数据 ，from是系统管理员 ObjectId是被删除人的userId，
                String removedAccountId = chatMessage.getObjectId();
                Friend toUser = FriendDao.getInstance().getFriend(mLoginUserId, removedAccountId);
                if (toUser != null) {
                    mNewMessage.setUserId(removedAccountId);
                    mNewMessage.setNickName(toUser.getNickName());
                    NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);// 本地可能没有该NewFriend，需要先创建在修改其status
                    FriendHelper.friendAccountRemoved(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_26);
                    NewFriendDao.getInstance().updateNewFriendContent(mNewMessage.getUserId(), chatMessage.getContent(), chatMessage.getTimeSend());
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                    // 关闭聊天界面
                    ChatActivity.callFinish(MyApplication.getContext(), chatMessage.getContent(), removedAccountId);
                }
                AccountSwitchHelper.removeExistsUser(MyApplication.getContext(), removedAccountId);
                break;
            }
            case XmppMessage.TYPE_BACK_DELETE: {
                // 后台删除了我的一个好友关系，
                JSONObject json = JSON.parseObject(chatMessage.getObjectId());
                String fromUserId = json.getString("fromUserId");
                String fromUserName = json.getString("fromUserName");
                String toUserId = json.getString("toUserId");
                String toUserName = json.getString("toUserName");
                if (TextUtils.equals(fromUserId, mLoginUserId)) {
                    // 我删除别人，
                    mNewMessage.setUserId(toUserId);
                    mNewMessage.setNickName(toUserName);
                    // 先从朋友表取出该用户，判断为公众号还是好友
                    Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
                    if (friend != null && friend.getStatus() == Friend.STATUS_SYSTEM) {
                        mNewMessage.setContent(ConvertMessage.getString(R.string.delete_firend_public) + toUserName);
                    } else {
                        mNewMessage.setContent(ConvertMessage.getString(R.string.delete_firend) + toUserName);
                    }
                    NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);// 本地可能没有该NewFriend，需要先创建在修改其status
                    FriendHelper.removeAttentionOrFriend(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_16);
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                } else {
                    // 别人删除我，
                    mNewMessage.setUserId(fromUserId);
                    mNewMessage.setNickName(fromUserName);
                    NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);// 本地可能没有该NewFriend，需要先创建在修改其status
                    FriendHelper.removeAttentionOrFriend(mNewMessage.getOwnerId(), mNewMessage.getUserId());
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_17);
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                }
                // 关闭聊天界面
                ChatActivity.callFinish(MyApplication.getContext(), ConvertMessage.getString(R.string.delete_firend), mNewMessage.getUserId());
                break;
            }
            case XmppMessage.TYPE_DELETE_ME: {
                AccountSwitchHelper.removeExistsUser(MyApplication.getContext(), mLoginUserId);
                EventBus.getDefault().post(new EventSelfRemoved());
                break;
            }
            case XmppMessage.TYPE_BACK_BLACK: {
                // 后台拉黑了我的好友或者拉黑了我本身，
                JSONObject json = JSON.parseObject(chatMessage.getObjectId());
                String fromUserId = json.getString("fromUserId");
                String fromUserName = json.getString("fromUserName");
                String toUserId = json.getString("toUserId");
                if (TextUtils.equals(fromUserId, mLoginUserId)) {
                    // 我拉黑别人，
                    mNewMessage.setUserId(toUserId);
                    Friend toUser = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
                    if (toUser == null) {
                        Reporter.post("后台拉黑了个不存在的好友，" + toUserId);
                        return;
                    }
                    mNewMessage.setNickName(toUser.getNickName());
                    FriendDao.getInstance().updateFriendStatus(mLoginUserId, toUserId, Friend.STATUS_BLACKLIST);
                    FriendHelper.addBlacklistExtraOperation(mLoginUserId, toUserId);

                    ChatMessage addBlackChatMessage = new ChatMessage();
                    addBlackChatMessage.setContent(ConvertMessage.getString(R.string.added_black_list) + " " + toUser.getShowName());
                    addBlackChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                    FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addBlackChatMessage);

                    NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_18);
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                    // 关闭聊天界面
                    ChatActivity.callFinish(MyApplication.getContext(), chatMessage.getContent(), toUserId);
                } else {
                    // 我被拉黑，
                    mNewMessage.setUserId(fromUserId);
                    mNewMessage.setNickName(fromUserName);
                    NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);// 本地可能没有该NewFriend，需要先创建在修改其status
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_19);
                    FriendHelper.beBlacklistExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId());
               /* content = mNewMessage.getNickName() + " " + getString("JXFriendObject_PulledBlack");
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, content);*/
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                    // 关闭聊天界面
                    ChatActivity.callFinish(MyApplication.getContext(), ConvertMessage.getString(R.string.be_pulled_black), mNewMessage.getUserId());
                }
                break;
            }
            case XmppMessage.TYPE_BACK_REFUSED: {
                // 后台取消拉黑了我的好友或者取消拉黑了我本身，
                JSONObject json = JSON.parseObject(chatMessage.getObjectId());
                String fromUserId = json.getString("fromUserId");
                String fromUserName = json.getString("fromUserName");
                String toUserId = json.getString("toUserId");
                if (TextUtils.equals(fromUserId, mLoginUserId)) {
                    // 取消拉黑了我的黑名单，
                    mNewMessage.setUserId(toUserId);
                    Friend toUser = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
                    if (toUser == null) {
                        Reporter.post("后台取消拉黑了个不存在的好友，" + toUserId);
                    } else {
                        mNewMessage.setNickName(toUser.getNickName());
                    }
                    NewFriendDao.getInstance().ascensionNewFriend(mNewMessage, Friend.STATUS_FRIEND);
                    FriendHelper.addFriendExtraOperation(mLoginUserId, toUserId, chatMessage.getTimeSend());

                    User self = CoreManager.requireSelf(MyApplication.getContext());
                    ChatMessage removeChatMessage = new ChatMessage();
                    removeChatMessage.setContent(self.getNickName() + ConvertMessage.getString(R.string.remove_blacklist_succ));
                    removeChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                    FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);
                    NewFriendDao.getInstance().createOrUpdateNewFriend(mNewMessage);
                    NewFriendDao.getInstance().changeNewFriendState(toUserId, Friend.STATUS_27);
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                } else {
                    // 我被取消拉黑，
                    mNewMessage.setUserId(fromUserId);
                    mNewMessage.setNickName(fromUserName);
                    NewFriendDao.getInstance().ascensionNewFriend(mNewMessage, Friend.STATUS_FRIEND);
                    FriendHelper.addFriendExtraOperation(mNewMessage.getOwnerId(), mNewMessage.getUserId(), chatMessage.getTimeSend());
                    NewFriendDao.getInstance().changeNewFriendState(mNewMessage.getUserId(), Friend.STATUS_24);//添加了xxx
                    content = ConvertMessage.getString(R.string.be_friendand_chat);
                    FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mNewMessage.getUserId(), content);
                    ListenerManager.getInstance().notifyNewFriend(mLoginUserId, mNewMessage, true);
                }
                break;
            }
            case XmppMessage.TYPE_NEWSEE:// 对方单向关注了我
            case XmppMessage.TYPE_DELSEE:// 对方取消了对我的单向关注
                // 单向关注 功能已去掉
                break;
            case XmppMessage.TYPE_RECOMMEND:
                // 新推荐好友 好像无此功能
                break;
            default:
                break;
        }
        // 更新通讯录页面
        CardcastUiUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
    }

}
