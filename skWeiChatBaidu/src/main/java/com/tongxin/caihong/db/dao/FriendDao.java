package com.tongxin.caihong.db.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.AttentionUser;
import com.tongxin.caihong.bean.EventSecureNotify;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.PublicKey;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.SQLiteHelper;
import com.tongxin.caihong.db.SQLiteRawUtil;
import com.tongxin.caihong.sp.TableVersionSp;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.HandleSecureChatMessage;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TanX;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.xmpp.SeqNoManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;


/**
 * 访问朋友数据的Dao
 */
public class FriendDao {
    private static final String TAG = "FriendDao";

    private static FriendDao instance = null;
    public Dao<Friend, Integer> friendDao;
    private SQLiteHelper mHelper;

    private FriendDao() {
        try {
            mHelper = OpenHelperManager.getHelper(MyApplication.getInstance(), SQLiteHelper.class);
            friendDao = DaoManager.createDao(mHelper.getConnectionSource(), Friend.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static final FriendDao getInstance() {
        if (instance == null) {
            synchronized (FriendDao.class) {
                if (instance == null) {
                    instance = new FriendDao();
                }
            }
        }
        return instance;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        OpenHelperManager.releaseHelper();
    }

    /**
     * 生成两个系统号
     */
    public void checkSystemFriend(String ownerId) {
        try {
            Friend friend = getFriend(ownerId, Friend.ID_SYSTEM_MESSAGE);
            if (friend == null) {// 公众号
                friend = new Friend();
                friend.setOwnerId(ownerId);
                friend.setUserId(Friend.ID_SYSTEM_MESSAGE);
                friend.setNickName(MyApplication.getInstance().getString(R.string.system_public_number));
                friend.setRemarkName(MyApplication.getInstance().getString(R.string.system_public_number));
                friend.setStatus(Friend.STATUS_SYSTEM);
                friend.setContent(MyApplication.getInstance().getString(R.string.tip_like_public_number));

                friendDao.create(friend);
                // 添加一条系统提示
/*
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
                chatMessage.setFromUserId(Friend.ID_SYSTEM_MESSAGE);
                chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                // 为了使得初始生成的系统消息排在新朋友前面，所以在时间节点上延迟一点 1s
                chatMessage.setTimeSend(TimeUtils.sk_time_current_time() + 1);
                chatMessage.setContent(MyApplication.getInstance().getString(R.string.system_public_number_welcome));
                chatMessage.setMySend(false);// 表示不是自己发的
                // 往消息表里插入一条记录
                ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, Friend.ID_SYSTEM_MESSAGE, chatMessage);
                // 往朋友表里面插入一条未读记录
                markUserMessageUnRead(ownerId, Friend.ID_SYSTEM_MESSAGE);
*/
            }

            friend = getFriend(ownerId, Friend.ID_NEW_FRIEND_MESSAGE);
            if (friend == null) {// 新的朋友
                friend = new Friend();
                friend.setOwnerId(ownerId);
                friend.setUserId(Friend.ID_NEW_FRIEND_MESSAGE);
                friend.setNickName(MyApplication.getInstance().getString(R.string.new_firend));
                friend.setRemarkName(MyApplication.getInstance().getString(R.string.new_firend));
                friend.setStatus(Friend.STATUS_SYSTEM);
                friendDao.create(friend);

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType(XmppMessage.TYPE_TIP);
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
                chatMessage.setFromUserId(Friend.ID_NEW_FRIEND_MESSAGE);
                chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                chatMessage.setContent("");
                chatMessage.setMySend(false);// 表示不是自己发的
                // 更新消息记录
                updateLastChatMessage(ownerId, Friend.ID_NEW_FRIEND_MESSAGE, chatMessage);
            }

            friend = getFriend(ownerId, Friend.ID_SK_PAY);
            if (friend == null) {// 支付公众号，
                friend = new Friend();
                friend.setOwnerId(ownerId);
                friend.setUserId(Friend.ID_SK_PAY);
                friend.setNickName(MyApplication.getInstance().getString(R.string.sk_pay));
                friend.setRemarkName(MyApplication.getInstance().getString(R.string.sk_pay));
                friend.setStatus(Friend.STATUS_SYSTEM);
                friendDao.create(friend);
            }

            checkDevice(ownerId);
            updateSystemFriendName(ownerId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 我的设备
    private void checkDevice(String ownerId) {
        // TODO 我的设备
        if (!MyApplication.IS_SUPPORT_MULTI_LOGIN) {
            List<Friend> friendList = getDevice(ownerId);
            for (Friend f : friendList) {
                deleteFriend(ownerId, f.getUserId());
                ChatMessageDao.getInstance().deleteMessageTable(ownerId, f.getUserId());
            }
        } else {
            String[] machine = new String[]{"ios", "pc", "mac", "web"};
            for (String s : machine) {
                Friend friend = getFriend(ownerId, s);
                if (friend == null) {
                    friend = new Friend();
                    friend.setOwnerId(ownerId);
                    friend.setUserId(s);
                    if (s.equals("ios")) {
                        friend.setNickName(MyApplication.getInstance().getString(R.string.my_iphone));
                        friend.setRemarkName(MyApplication.getInstance().getString(R.string.my_iphone));
                    } else if (s.equals("pc")) {
                        friend.setNickName(MyApplication.getInstance().getString(R.string.my_windows));
                        friend.setRemarkName(MyApplication.getInstance().getString(R.string.my_windows));
                    } else if (s.equals("mac")) {
                        friend.setNickName(MyApplication.getInstance().getString(R.string.my_mac));
                        friend.setRemarkName(MyApplication.getInstance().getString(R.string.my_mac));
                    } else {
                        friend.setNickName(MyApplication.getInstance().getString(R.string.my_web));
                        friend.setRemarkName(MyApplication.getInstance().getString(R.string.my_web));
                    }
                    friend.setIsDevice(1);// 标志该朋友为其它设备(userId本质为自己)
                    // friend.setStatus(Friend.STATUS_FRIEND);
                    friend.setStatus(Friend.STATUS_SYSTEM);// 将状态改为系统号，否则在更新朋友表的时候，因为服务器attentionList内 未存自己，在清除旧数据的时候会清除掉自己
                    try {
                        friendDao.create(friend);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 修改本地语言之后，重启app也需要更新下系统公众号与我的设备的名字等
     */
    private void updateSystemFriendName(String ownerId) {
        List<String> ids = new ArrayList<>();
        ids.add(Friend.ID_SYSTEM_MESSAGE);
        ids.add(Friend.ID_NEW_FRIEND_MESSAGE);
        ids.add(Friend.ID_SK_PAY);
        if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {
            String[] machine = new String[]{"ios", "pc", "mac", "web"};
            ids.addAll(Arrays.asList(machine));
        }
        for (int i = 0; i < ids.size(); i++) {
            String name = "";
            if (TextUtils.equals(ids.get(i), Friend.ID_SYSTEM_MESSAGE)) {
                name = MyApplication.getInstance().getString(R.string.system_public_number);
            } else if (TextUtils.equals(ids.get(i), Friend.ID_NEW_FRIEND_MESSAGE)) {
                name = MyApplication.getInstance().getString(R.string.new_firend);
            } else if (TextUtils.equals(ids.get(i), Friend.ID_SK_PAY)) {
                name = MyApplication.getInstance().getString(R.string.sk_pay);
            } else if (TextUtils.equals(ids.get(i), "ios")) {
                name = MyApplication.getInstance().getString(R.string.my_iphone);
            } else if (TextUtils.equals(ids.get(i), "pc")) {
                name = MyApplication.getInstance().getString(R.string.my_windows);
            } else if (TextUtils.equals(ids.get(i), "mac")) {
                name = MyApplication.getInstance().getString(R.string.my_mac);
            } else if (TextUtils.equals(ids.get(i), "web")) {
                name = MyApplication.getInstance().getString(R.string.my_web);
            }
            updateNickName(ownerId, ids.get(i), name);
            updateRemarkName(ownerId, ids.get(i), name);
        }
    }

    /**
     * 更新与某个好友的阅读状态为已读
     */
    public void markUserMessageRead(String ownerId, String friendId) {
        TanX.Log("markUserMessageRead----" + friendId + "设置为已读");
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("unReadNum", 0);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新某个好友的阅读状态，+1条未读信息
     */
    public boolean markUserMessageUnRead(String ownerId, String friendId) {
        return markUserMessageUnRead2(ownerId, friendId, 1);
    }

    /**
     * 批量获取离线消息 批量更新未读消息数量
     */
    public boolean markUserMessageUnRead2(String ownerId, String friendId, int num) {
        Log.e("markUserMessageUnRead2", "+" + num + "条未读消息");
        try {
            UpdateBuilder builder = friendDao.updateBuilder();
            builder.updateColumnExpression("unReadNum", "unReadNum + " + num);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            return builder.update() > 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addNewFriendInMsgTable(String loginUserId, String friendId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TIP);
        chatMessage.setFromUserId(friendId);
        chatMessage.setContent(MyApplication.getInstance().getString(R.string.Msg_View_Controller_Start_Chat));
        chatMessage.setMySend(false);// 表示不是自己发的
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        // 往消息表里插入一条记录
        ChatMessageDao.getInstance().saveNewSingleChatMessage(loginUserId, friendId, chatMessage);
        // 往朋友表里面插入一条未读记录
        markUserMessageUnRead(loginUserId, friendId);
        return true;
    }

    public boolean addNewFriendInMsgTable(String loginUserId, String friendId, long time) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TIP);
        chatMessage.setFromUserId(friendId);
        chatMessage.setContent(MyApplication.getInstance().getString(R.string.Msg_View_Controller_Start_Chat));
        chatMessage.setMySend(false);// 表示不是自己发的
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(time);
        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        // 往消息表里插入一条记录
        ChatMessageDao.getInstance().saveNewSingleChatMessage(loginUserId, friendId, chatMessage);
        // 往朋友表里面插入一条未读记录
        markUserMessageUnRead(loginUserId, friendId);
        return true;
    }

    /* 获取消息模块未读数量总和 */
    public int getMsgUnReadNumTotal(String ownerId) {
        boolean isPrivateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + ownerId, true);
        Object[] objects;
        if (isPrivateMode) {
            objects = new Object[]{0};
        } else {
            objects = new Object[]{0, 1};
        }
        try {
            Where<Friend, Integer> builder = friendDao.queryBuilder()
                    .selectRaw("ifnull(sum(unReadNum), 0)")
                    // 过滤条件参照MessageFragment页面加载数据的方法，com.chat.weichat.fragment.MessageFragment.loadDatas
                    .where().eq("ownerId", ownerId)
                    // getMsgUnReadNumTotal 和 getNearlyFriendMsg 的条件要同步修改，
                    .and().notIn("status", Friend.STATUS_BLACKLIST, Friend.STATUS_19, Friend.STATUS_23)
                    .and().ne("userId", Friend.ID_NEW_FRIEND_MESSAGE)
                    .and().in("hideChatSwitch", objects)// 私密模式
                    .and().ne("userId", ownerId)
                    .and().isNotNull("content");
            return Integer.valueOf(builder.queryRawFirst()[0]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 查询所有最近聊天的好友 全部
     * todo 如需要不支持陌生人机制，则可将出status_friend、status_system之外的过滤掉
     */
    public List<Friend> getNearlyFriendMsg(String ownerId) {
        boolean isPrivateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + ownerId, true);
        Object[] objects;
        if (isPrivateMode) {
            objects = new Object[]{0};
        } else {
            objects = new Object[]{0, 1};
        }
        List<Friend> friends = new ArrayList<>();
        try {
            QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
            builder.where()
                    // getMsgUnReadNumTotal 和 getNearlyFriendMsg 的条件要同步修改，
                    .notIn("status", Friend.STATUS_BLACKLIST, Friend.STATUS_19, Friend.STATUS_23)
                    .and().in("hideChatSwitch", objects)// 私密模式
                    .and().eq("ownerId", ownerId).and()
                    .isNotNull("content");
            builder.orderBy("topTime", false);
            builder.orderBy("timeSend", false);
            friends = builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Map<String, Friend> mFriendMap = new LinkedHashMap<>();
        if (friends != null && friends.size() > 0) {// 偶然发现该方法有时会查询出多条重复数据，去重
            for (int i = 0; i < friends.size(); i++) {
                if (mFriendMap.containsKey(friends.get(i).getUserId())) {
                    Friend friend = mFriendMap.get(friends.get(i).getUserId());
                    // 如果map内有friend，需要判断friend的unReadNum是否为0，如果不为0，不能put一个新的friend进来
                    // 具体的就不说了，为什么这么处理看禅道bug#10311
                    if (friend != null && friend.getUnReadNum() > 0) {
                        Log.e("getNearlyFriendMsg", "friend != null && friend.getUnReadNum() > 0，不给Map赋值");
                    } else {
                        mFriendMap.put(friends.get(i).getUserId(), friends.get(i));
                    }
                } else {
                    mFriendMap.put(friends.get(i).getUserId(), friends.get(i));
                }
            }
            Collection<Friend> values = mFriendMap.values();
            friends = new ArrayList<>(values);
        }

        // 置顶的Friend也根据timeSend排序
        if (friends != null) {
            mFriendMap.clear();
            for (int i = 0; i < friends.size(); i++) {
                if (friends.get(i).getTopTime() != 0) {
                    mFriendMap.put(friends.get(i).getUserId(), friends.get(i));
                }
            }
            Collection<Friend> values = mFriendMap.values();
            List<Friend> topFriends = new ArrayList<>(values);
            Comparator<Friend> comparator = (o1, o2) -> {
                long l1 = o1 == null ? 0 : o1.getTimeSend();
                long l2 = o2 == null ? 0 : o2.getTimeSend();
                return Long.compare(l1, l2);
            };
            Collections.sort(topFriends, comparator);

            for (int i = 0; i < topFriends.size(); i++) {
                friends.remove(topFriends.get(i));
                friends.add(0, topFriends.get(i));
            }
        }

        return friends;
    }

    /**
     * 获取备注名
     */
    public String getRemarkName(String ownerId, String userId) {
        QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
        builder.selectRaw("remarkName");
        try {
            builder.where().eq("ownerId", ownerId).and().eq("userId", userId);
            GenericRawResults<String[]> results = friendDao.queryRaw(builder.prepareStatementString());
            if (results != null) {
                String[] first = results.getFirstResult();
                if (first != null && first.length > 0) {
                    return first[0];
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void resetFriendMessage(String loginUserId, String userId) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("unReadNum", 0);
            builder.updateColumnValue("content", null);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteFriend(String ownerId, String friendId) {
        try {
            DeleteBuilder<Friend, Integer> builder = friendDao.deleteBuilder();
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.delete(builder.prepare());
            // 删除成功，将对应的model也移除掉
            SeqNoManager.getInstance().removeModel(friendId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建群组
     */
    public boolean createOrUpdateFriend(Friend friend) {
        try {
            return createOrUpdateByUserId(friend);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 用事务避免多线程冲突导致多条记录，
     * 避免使用dao的createOrUpdate方法，主要是内部判断是根据主键_id但我们实际判断重复是使用userId,
     */
    private boolean createOrUpdateByUserId(Friend friend) throws SQLException {
        Log.e(TAG, "createOrUpdateByUserId() called with: friend = [" + friend.getNickName() + "]");
        return new TransactionManager(friendDao.getConnectionSource()).callInTransaction(() -> {
            Friend existsFriend = friendDao.queryBuilder()
                    .where().eq("ownerId", friend.getOwnerId())
                    .and().eq("userId", friend.getUserId())
                    .queryForFirst();
            if (existsFriend == null) {
                return friendDao.create(friend) > 0;
            } else {
                friend.set_id(existsFriend.get_id());
                return friendDao.update(friend) > 0;
            }
        });
    }

    /**
     * 创建好友
     */
    public boolean createOrUpdateFriendByNewFriend(NewFriendMessage newFriend, int friendStatus) {
        try {
            Friend existFriend = getFriend(newFriend.getOwnerId(), newFriend.getUserId());
            if (existFriend == null) {
                existFriend = new Friend();
                existFriend.setOwnerId(newFriend.getOwnerId());
                existFriend.setUserId(newFriend.getUserId());
                existFriend.setNickName(newFriend.getNickName());
                existFriend.setTimeCreate(TimeUtils.sk_time_current_time());
                existFriend.setCompanyId(newFriend.getCompanyId());
                existFriend.setVersion(TableVersionSp.getInstance(MyApplication.getInstance()).getFriendTableVersion(newFriend.getOwnerId()));
            }
            existFriend.setStatus(friendStatus);
            return createOrUpdateByUserId(existFriend);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 将陌生人加入朋友表
     */
    public Friend createNewFriend(String toUserId, String toUserName) {
        Friend friend = new Friend();
        friend.setOwnerId(CoreManager.requireSelf(MyApplication.getInstance()).getUserId());
        friend.setUserId(toUserId);
        friend.setNickName(toUserName);
        friend.setCompanyId(0);// 公司
        friend.setRoomFlag(0);// 0朋友 1群组
        friend.setStatus(Friend.STATUS_UNKNOW);
        friend.setTimeCreate(TimeUtils.sk_time_current_time());
        friend.setVersion(TableVersionSp.getInstance(MyApplication.getInstance()).getFriendTableVersion(CoreManager.requireSelf(MyApplication.getInstance()).getUserId()));// 更新版本
        try {
            friendDao.create(friend);
            return friend;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将陌生人加入朋友表
     */
    public void createNewFriend(ChatMessage chatMessage) {
        Friend friend = new Friend();
        friend.setOwnerId(CoreManager.requireSelf(MyApplication.getInstance()).getUserId());
        friend.setUserId(chatMessage.getFromUserId());
        friend.setNickName(chatMessage.getFromUserName());
        friend.setRemarkName(chatMessage.getFromUserName());
        friend.setTimeCreate(TimeUtils.sk_time_current_time());
        friend.setContent(chatMessage.getContent());
        friend.setCompanyId(0);// 公司
        friend.setTimeSend(chatMessage.getTimeSend());
        friend.setRoomFlag(0);// 0朋友 1群组
        friend.setStatus(Friend.STATUS_UNKNOW);
        friend.setVersion(TableVersionSp.getInstance(MyApplication.getInstance()).getFriendTableVersion(CoreManager.requireSelf(MyApplication.getInstance()).getUserId()));// 更新版本
        try {
            friendDao.create(friend);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Friend> getDevice(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    .and().eq("isDevice", 1)
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    public List<Friend> getAllFriends(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    // .and().in("status", new Object[]{Friend.STATUS_FRIEND, Friend.STATUS_SYSTEM})
                    .and().in("status", Friend.STATUS_FRIEND)// 仅限我的好友
                    .and().eq("isDevice", 0)// 移除我的设备
                    .and().eq("roomFlag", 0)// 移除房间
                    .and().eq("companyId", 0)
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    public List<Friend> getAllEncryptChatUser(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    .and().gt("encryptType", 0)
                    .and().eq("roomFlag", 0)
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    public List<Friend> getAllEncryptChatGroup(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    .and().gt("encryptType", 0)
                    .and().ne("roomFlag", 0)
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    public List<Friend> getAllFriendsNotFilterStatus(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    .and().eq("isDevice", 0)// 移除我的设备
                    .and().eq("roomFlag", 0)// 移除房间
                    .and().eq("companyId", 0)
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * 获取好友用于建群，
     * <p>
     * 支持群聊的除了好友还有公众号，
     * 但是要排除系统号10000,
     * 还要新的朋友系统号10001,
     * 还要支付系统号1100,
     */
    public List<Friend> getFriendsGroupChat(String ownerId) throws SQLException {
        PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                .eq("ownerId", ownerId)
                .and().in("status", new Object[]{Friend.STATUS_FRIEND})
                .and().eq("isDevice", 0)// 移除我的设备
                .and().eq("roomFlag", 0)// 移除房间
                .and().eq("companyId", 0)
                .prepare();

        return friendDao.query(preparedQuery);
    }

    /**
     * 查询好友的数量，
     * 仅限好友，
     */
    public long getFriendsCount(String ownerId) throws SQLException {
        return friendDao.queryBuilder().where()
                .eq("ownerId", ownerId)
                // 仅限好友，
                .and().eq("status", Friend.STATUS_FRIEND)
                // 排除群组，
                .and().eq("roomFlag", 0)
                .countOf();
    }

    /**
     * 查询好友的数量，
     * 仅限好友，
     */
    public long getGroupsCount(String ownerId) throws SQLException {
        return friendDao.queryBuilder().where()
                .eq("ownerId", ownerId)
                // 仅限好友，
                .and().eq("status", Friend.STATUS_FRIEND)
                // 仅限群组，
                .and().ne("roomFlag", 0)
                .countOf();
    }

    public List<Friend> getAllSystems(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    // .and().in("status", new Object[]{Friend.STATUS_FRIEND, Friend.STATUS_SYSTEM})
                    .and().in("status", Friend.STATUS_SYSTEM)// 仅限公众号
                    .and().eq("isDevice", 0)// 移除我的设备
                    .and().eq("roomFlag", 0)// 移除房间
                    .and().eq("companyId", 0)
                    .and().ne("userId", Friend.ID_NEW_FRIEND_MESSAGE)
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    public List<Friend> getAllRooms(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId).and()
                    .in("groupStatus", 0, 3, 4).and()
                    .in("roomFlag", 1, 510)
                    .prepare();
            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * 用于遍历群组，代替{@link #getAllRooms(String)}
     * <p>
     * 为了避免读取数据过多导致占用过大内存，使用iterable而不是list,
     */
    public void getAllRooms(
            String ownerId,
            AsyncUtils.Function<Iterator<Friend>> callback
    ) throws Exception {
        PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                .eq("ownerId", ownerId).and()
                .in("groupStatus", 0, 3, 4).and()
                .in("roomFlag", 1, 510)
                .prepare();
        CloseableIterator<Friend> results = friendDao.iterator(preparedQuery);
        callback.apply(results);
        results.close();
    }

    public List<Friend> getAllBlacklists(String ownerId) {
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and()
                    .eq("status", Friend.STATUS_BLACKLIST).and()
                    .eq("roomFlag", 0)
                    .prepare();
            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * 获取单个好友 陌生人 || 好友 || 公众号 || 群组
     */
    public Friend getFriend(String ownerId, String friendId) {
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId).and().eq("userId", friendId)
                    .prepare();
            return friendDao.queryForFirst(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取单个好友 仅限好友
     */
    public Friend getFriendAndFriendStatus(String ownerId, String friendId) {
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId).and().eq("userId", friendId).and()
                    .eq("status", Friend.STATUS_FRIEND)
                    .prepare();
            return friendDao.queryForFirst(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取单个好友 仅限公众号
     */
    public Friend getFriendAndSystemStatus(String ownerId, String friendId) {
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId).and().eq("userId", friendId).and()
                    .eq("status", Friend.STATUS_SYSTEM)
                    .prepare();
            Friend existFriend = friendDao.queryForFirst(preparedQuery);
            return existFriend;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过roomId获取群组Friend
     */
    public Friend getMucFriendByRoomId(String ownerId, String roomId) {
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where().eq("ownerId", ownerId).and().eq("roomId", roomId).prepare();
            return friendDao.queryForFirst(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 更新好友的状态
    public void updateFriendStatus(String loginUserId, String userId, int status) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("status", status);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 专门更新公众号的状态
    public void updateFriendUserType(String loginUserId, String userId, int userType) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            int status = Friend.STATUS_SYSTEM;
            boolean isService = userType == 4;
            builder.updateColumnValue("status", status);
            builder.updateColumnValue("isService", isService);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新昵称
    public void updateNickName(String loginUserId, String userId, String nickName) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("nickName", nickName);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新备注名
    public void updateRemarkName(String loginUserId, String userId, String remarkName) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("remarkName", remarkName);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新描述
    public void updateDescribe(String loginUserId, String userId, String remarkName) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("describe", remarkName);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新备注名与描述
    public void updateRemarkNameAndDescribe(String loginUserId, String userId, String remarkName,
                                            String describe) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("remarkName", remarkName);
            builder.updateColumnValue("describe", describe);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新群组昵称
    public void updateMucFriendRoomName(String roomId, String roomName) {
        try {
            UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
            builder.updateColumnValue("nickName", roomName).where().eq("userId", roomId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新群内昵称
    public void updateRoomMyNickName(String roomId, String roomMyNickName) {
        try {
            UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
            builder.updateColumnValue("roomMyNickName", roomMyNickName).where().eq("userId", roomId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新群组状态
    public void updateFriendGroupStatus(String loginUserId, String userId, int groupStatus) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("groupStatus", groupStatus);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新群内昵称
    public void updateRoomName(String ownerId, String friendId, String myNickName) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            builder.updateColumnValue("roomMyNickName", myNickName);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新群创建者id
    public void updateRoomCreateUserId(String ownerId, String friendId, String roomCreateUserId) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            builder.updateColumnValue("roomCreateUserId", roomCreateUserId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新禁言时间
    public void updateRoomTalkTime(String ownerId, String friendId, int roomTalkTime) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            builder.updateColumnValue("roomTalkTime", roomTalkTime);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新群里的身份
    public void updateRoomRole(String ownerId, String roomId, int role) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.where().eq("ownerId", ownerId).and().eq("roomId", roomId);
            builder.updateColumnValue("roomRole", role);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新好友的maxSeqNo
    public void updateFriendMaxSeqNo(String loginUserId, String userId, long maxSeqNo) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("maxSeqNo", maxSeqNo);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新好友的loseSeqNos
    public void updateFriendLoseSeqNos(String loginUserId, String userId, String loseSeqNos) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("loseSeqNos", loseSeqNos);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新好友的joinSeqNo
    public void updateFriendJoinSeqNo(String loginUserId, String userId, long joinSeqNo) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("joinSeqNo", joinSeqNo);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 针对某个好友的部分设置统一更新
    public void updateFriendPartStatus(String friendId, User user) {
        FriendDao.getInstance().updateHiding(friendId,
                user.getSettings().getHiding());
        FriendDao.getInstance().updateOfflineNoPushMsgStatus(friendId,
                user.getFriends().getOfflineNoPushMsg());
        FriendDao.getInstance().updateHideChatSwitchStatus(friendId,
                user.getFriends().getHideChatSwitch());
        FriendDao.getInstance().updateSendRead(friendId,
                user.getFriends().getIsSendMsgState());
        if (user.getFriends().getOpenTopChatTime() > 0) {
            FriendDao.getInstance().updateTopFriend(friendId, user.getFriends().getOpenTopChatTime());
        } else {
            FriendDao.getInstance().resetTopFriend(friendId);
        }
        PreferenceUtils.putInt(MyApplication.getContext(), Constants.MESSAGE_READ_FIRE + friendId + CoreManager.requireSelf(MyApplication.getContext()).getUserId(),
                user.getFriends().getIsOpenSnapchat());
        FriendDao.getInstance().updateChatRecordTimeOut(friendId,
                user.getFriends().getChatRecordTimeOut());

        updateEncryptType(friendId, user.getFriends().getEncryptType());
        if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
            //  SecureFlag
            PublicKeyDao.getInstance().updatePublicKey(user.getFriends().getUserId(), user.getFriends().getToUserId(),
                    new PublicKey(user.getFriends().getUserId(), user.getFriends().getToUserId(), user.getFriends().getDhMsgPublicKey(), user.getFriends().getCreateTime()));
            updatePublicKeyDH(friendId, user.getFriends().getDhMsgPublicKey());
            updatePublicKeyRSARoom(friendId, user.getFriends().getRsaMsgPublicKey());
        }
    }

    // 更新为置顶
    public void updateTopFriend(String friendId, long time) {
        if (time == 0) {
            time = TimeUtils.sk_time_current_time();
        }
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("topTime", time);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 取消置顶
    public void resetTopFriend(String friendId) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("topTime", 0);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新消息免打扰状态
    public void updateOfflineNoPushMsgStatus(String friendId, int offlineNoPushMsg) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("offlineNoPushMsg", offlineNoPushMsg);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新私密模式状态
    public void updateHideChatSwitchStatus(String friendId, int hideChatSwitch) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("hideChatSwitch", hideChatSwitch);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新是否发送已读状态
    public void updateSendRead(String friendId, int isSendRead) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("isSendRead", isSendRead);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新账号隐身状态
    public void updateHiding(String friendId, int hiding) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("hiding", hiding);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新消息保存天数
    public void updateChatRecordTimeOut(String friendId, double chatRecordTimeOut) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getInstance()).getUserId();
            builder.updateColumnValue("chatRecordTimeOut", chatRecordTimeOut);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新@我的状态
    public void updateAtMeStatus(String friendId, int status) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
            builder.updateColumnValue("isAtMe", status);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateEncryptType(String friendId, int encryptType) {
        if (!MyApplication.IS_SUPPORT_SECURE_CHAT
                && (encryptType == 2 || encryptType == 3)) {
            // SecureFlag 非端到端，兼容之前在端到端版本注册过的账号，encryptType==2 || encryptType==3，统一当做1处理
            encryptType = 1;
        }
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
            builder.updateColumnValue("encryptType", encryptType);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePublicKeyDH(String friendId, String publicKeyDH) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
            builder.updateColumnValue("publicKeyDH", publicKeyDH);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePublicKeyRSARoom(String friendId, String publicKeyRSARoom) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
            builder.updateColumnValue("publicKeyRSARoom", publicKeyRSARoom);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateSecretGroup(String friendId) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
            builder.updateColumnValue("isSecretGroup", 1);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 私密群组相关，更新通信密钥
     *
     * @param friendId
     * @param chatKeyGroup
     */
    public void updateChatKeyGroup(String friendId, String chatKeyGroup) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
            builder.updateColumnValue("chatKeyGroup", chatKeyGroup);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
            // 更新chatKeyGroup成功的同时需要更新isLostChatKeyGroup Status
            updateIsLostChatKeyGroup(friendId, 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 私密群组相关，更新群组通信密钥状态
     *
     * @param friendId
     * @param isLostChatKeyGroup
     */
    public void updateIsLostChatKeyGroup(String friendId, int isLostChatKeyGroup) {
        String ownerId = CoreManager.requireSelf(MyApplication.getContext()).getUserId();
        Friend friend = FriendDao.getInstance().getFriend(ownerId, friendId);
        if (friend == null) {
            return;
        }
        if (isLostChatKeyGroup == 1) {
            if (friend.getIsLostChatKeyGroup() == 1) {// 之前已经标记过了，不重复更新
                return;
            }
        }
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("isLostChatKeyGroup", isLostChatKeyGroup);
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
            if (isLostChatKeyGroup == 1) {
                HandleSecureChatMessage.sendRequestChatKeyGroupMessage(false, friendId);
            } else if (friend.getIsLostChatKeyGroup() == 1 && isLostChatKeyGroup == 0) {
                // 丢失状态更新为正常状态，取出该群组内验签失败的消息，重新解密
                List<ChatMessage> messages = ChatMessageDao.getInstance().queryVerifySignatureFailedMsg(ownerId, friendId);
                for (ChatMessage message : messages) {
                    ChatMessageDao.getInstance().decrypt(friendId, message);
                }
                // 同时需要通知聊天界面更新
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setObjectId(friendId);
                EventBus.getDefault().post(new EventSecureNotify(EventSecureNotify.SINGLE_SNED_KEY_MSG, chatMessage));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 单聊 synchronizeChatHistory 调用成功后，将downloadTime 与 timeSend保持一致
     *
     * @param loginUserId
     * @param userId
     * @param time
     */
    public void updateDownloadTime(String loginUserId, String userId, long time) {
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("downloadTime", time);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新content字段
     */
    public void updateFriendContent(String loginUserId, String userId, String text, ChatMessage message) {
        String oText = text;
        int type = message.getType();
        if (type == XmppMessage.TYPE_IMAGE) {
            text = "[" + MyApplication.getInstance().getString(R.string.image) + "]";
        } else if (type == XmppMessage.TYPE_CARD) {
            text = "[" + MyApplication.getInstance().getString(R.string.chat_card) + "]";
        } else if (type == XmppMessage.TYPE_VOICE) {
            text = "[" + MyApplication.getInstance().getString(R.string.voice) + "]";
        } else if (type == XmppMessage.TYPE_LOCATION) {
            text = "[" + MyApplication.getInstance().getString(R.string.my_location) + "]";
        } else if (type == XmppMessage.TYPE_GIF) {
            text = "[" + MyApplication.getInstance().getString(R.string.animation) + "]";
        } else if (type == XmppMessage.TYPE_VIDEO) {
            text = "[" + MyApplication.getInstance().getString(R.string.s_video) + "]";
        } else if (type == XmppMessage.TYPE_FILE) {
            text = "[" + MyApplication.getInstance().getString(R.string.s_file) + "]";
        } else if (type == XmppMessage.TYPE_RED) {
            text = "[" + MyApplication.getInstance().getString(R.string.chat_red) + "]";
        } else if (type == XmppMessage.TYPE_LINK || type == XmppMessage.TYPE_SHARE_LINK) {
            text = "[" + MyApplication.getInstance().getString(R.string.link) + "]";
        } else if (type == XmppMessage.TYPE_IMAGE_TEXT || type == XmppMessage.TYPE_IMAGE_TEXT_HTML || type == XmppMessage.TYPE_IMAGE_TEXT_MANY) {
            text = "[" + MyApplication.getInstance().getString(R.string.graphic) + MyApplication.getInstance().getString(R.string.mainviewcontroller_message) + "]";
        } else if (type == XmppMessage.TYPE_SHAKE) {
            text = MyApplication.getInstance().getString(R.string.msg_shake);
        } else if (type == XmppMessage.TYPE_DICE
                || type == XmppMessage.TYPE_RPS) {
            text = MyApplication.getContext().getString(R.string.type_emoij);
        } else if (type == XmppMessage.TYPE_MEETING_INVITE) {
            text = MyApplication.getInstance().getString(R.string.type_meeting_invite);
        } else if (type == XmppMessage.TYPE_LIVE_INVITE) {
            text = MyApplication.getInstance().getString(R.string.type_live_invite);
        } else if (type == XmppMessage.TYPE_INVITE_JOIN_ROOM) {
            text = MyApplication.getInstance().getString(R.string.type_invite_join_room);
        } else if (type == XmppMessage.TYPE_COMPANY_INVITE_JOIN) {
            text = MyApplication.getInstance().getString(R.string.type_invite_join_company);
        } else if (type == XmppMessage.TYPE_APPLET) {
            text = MyApplication.getInstance().getString(R.string.type_applet);
        } else if (type == XmppMessage.TYPE_SHARE) {
            // 分享了一条动态
            text = StringUtils.getShareContent(text);
        } else if (type == XmppMessage.TYPE_CHAT_HISTORY) {
            text = MyApplication.getInstance().getString(R.string.msg_chat_history);
        } else if (type == XmppMessage.TYPE_NO_CONNECT_VOICE
                || type == XmppMessage.TYPE_NO_CONNECT_VIDEO
                || type == XmppMessage.TYPE_END_CONNECT_VOICE
                || type == XmppMessage.TYPE_END_CONNECT_VIDEO) {
            text = StringUtils.getAudioMessageContent(message);
        } else if (type == XmppMessage.TYPE_OK_MU_CONNECT_VOICE || type == XmppMessage.TYPE_EXIT_VOICE) {
            text = MyApplication.getInstance().getString(R.string.msg_voice_meeting);
        } else if (type == XmppMessage.TYPE_VIDEO_IN || type == XmppMessage.TYPE_VIDEO_OUT) {
            text = MyApplication.getInstance().getString(R.string.msg_video_meeting);
        } else if (type == XmppMessage.TYPE_TRANSFER) {
            text = MyApplication.getContext().getString(R.string.tip_transfer_money);
        } else if (type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
            text = MyApplication.getContext().getString(R.string.tip_transfer_money) + MyApplication.getContext().getString(R.string.transfer_friend_sure_save);
        } else if (type == XmppMessage.TYPE_TRANSFER_BACK) {
            text = MyApplication.getContext().getString(R.string.transfer_back);
        } else if (type == XmppMessage.TYPE_PAYMENT_OUT || type == XmppMessage.TYPE_RECEIPT_OUT) {
            text = MyApplication.getContext().getString(R.string.payment_get_notify);
        } else if (type == XmppMessage.TYPE_PAYMENT_GET || type == XmppMessage.TYPE_RECEIPT_GET) {
            text = MyApplication.getContext().getString(R.string.receipt_get_notify);
        } else if (type == XmppMessage.TYPE_PAY_CERTIFICATE) {
            text = MyApplication.getContext().getString(R.string.pay_certificate);
        } else if (type == XmppMessage.TYPE_SCAN_RECHARGE) {
            text = MyApplication.getContext().getString(R.string.scan_recharge_notify);
        } else if (type == XmppMessage.TYPE_SCAN_WITHDRAW) {
            text = MyApplication.getContext().getString(R.string.scan_withdraw_notify);
        } else if (type == XmppMessage.TYPE_SECURE_LOST_KEY) {
            text = MyApplication.getContext().getString(R.string.request_chat_key_group_thumb);
        } else if (type == XmppMessage.TYPE_MUCFILE_ADD) {
            text = "[" + MyApplication.getInstance().getString(R.string.group_file) + "]";
        } else if (type == XmppMessage.TYPE_NEW_NOTICE) {
            text = "[" + MyApplication.getInstance().getString(R.string.group_bulletin) + "]";
        } else if (type == XmppMessage.TYPE_SERVICE_MENU) {
            text = "[" + MyApplication.getInstance().getString(R.string.service_menu) + "]";
        }
        Friend friend = FriendDao.getInstance().getFriend(loginUserId, userId);
        if (friend != null && friend.getRoomFlag() != 0
                && !TextUtils.equals(oText, text)) {
            // 为群组且text经过转换，在前面拼上发送方昵称
            text = message.getFromUserName() + " : " + text;
        }
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("type", type);
            builder.updateColumnValue("content", text);
            builder.updateColumnValue("timeSend", message.getTimeSend());
            builder.updateColumnValue("privacy", message.getPacketId());
            if (friend != null && friend.getTimeSend() == friend.getDownloadTime()) {
                builder.updateColumnValue("downloadTime", message.getTimeSend());
            }
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateApartDownloadTime(String loginUserId, String userId, String text, int type, long time,
                                        int isRoom, String from, String fromUserName, String toUserName) {
        Log.d(TAG, "updateApartDownloadTime() called with: loginUserId = [" + loginUserId + "], userId = [" + userId + "], text = [" + text + "], type = [" + type + "], time = [" + time + "], isRoom = [" + isRoom + "], from = [" + from + "], fromUserName = [" + fromUserName + "], toUserName = [" + toUserName + "]");
        if (TextUtils.equals(userId, Friend.ID_SK_PAY)) {// 支付公众号消息不处理
            return;
        }
        // 给一些群组消息拼上发送者昵称
        String splitText = isRoom == 1 ? fromUserName + " : " : "";

        if (type == XmppMessage.TYPE_IMAGE) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.image) + "]";
        } else if (type == XmppMessage.TYPE_CARD) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.chat_card) + "]";
        } else if (type == XmppMessage.TYPE_VOICE) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.voice) + "]";
        } else if (type == XmppMessage.TYPE_LOCATION) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.my_location) + "]";
        } else if (type == XmppMessage.TYPE_GIF) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.animation) + "]";
        } else if (type == XmppMessage.TYPE_VIDEO) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.s_video) + "]";
        } else if (type == XmppMessage.TYPE_FILE) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.s_file) + "]";
        } else if (type == XmppMessage.TYPE_RED) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.chat_red) + "]";
        } else if (type == XmppMessage.TYPE_LINK || type == XmppMessage.TYPE_SHARE_LINK) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.link) + "]";
        } else if (type == XmppMessage.TYPE_IMAGE_TEXT || type == XmppMessage.TYPE_IMAGE_TEXT_HTML || type == XmppMessage.TYPE_IMAGE_TEXT_MANY) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.graphic) + MyApplication.getInstance().getString(R.string.mainviewcontroller_message) + "]";
        } else if (type == XmppMessage.TYPE_SHAKE) {
            text = MyApplication.getInstance().getString(R.string.msg_shake);
        } else if (type == XmppMessage.TYPE_DICE
                || type == XmppMessage.TYPE_RPS) {
            text = splitText + MyApplication.getContext().getString(R.string.type_emoij);
        } else if (type == XmppMessage.TYPE_MEETING_INVITE) {
            text = splitText + MyApplication.getInstance().getString(R.string.type_meeting_invite);
        } else if (type == XmppMessage.TYPE_LIVE_INVITE) {
            text = splitText + MyApplication.getInstance().getString(R.string.type_live_invite);
        } else if (type == XmppMessage.TYPE_INVITE_JOIN_ROOM) {
            text = splitText + MyApplication.getInstance().getString(R.string.type_invite_join_room);
        } else if (type == XmppMessage.TYPE_COMPANY_INVITE_JOIN) {
            text = splitText + MyApplication.getInstance().getString(R.string.type_invite_join_company);
        } else if (type == XmppMessage.TYPE_APPLET) {
            text = splitText + MyApplication.getInstance().getString(R.string.type_applet);
        } else if (type == XmppMessage.TYPE_SHARE) {
            // 分享了一条动态
            text = StringUtils.getShareContent(text);
        } else if (type == XmppMessage.TYPE_CHAT_HISTORY) {
            text = splitText + MyApplication.getInstance().getString(R.string.msg_chat_history);
        } else if (type == XmppMessage.TYPE_END_CONNECT_VOICE
                || type == XmppMessage.TYPE_END_CONNECT_VIDEO) {
            // getLast接口内获取到数据时更新本地Friend的content，
            // 很尴尬的是音视频消息转文字消息 很关键的一个字段timeLen服务端没返回，
            // 只能这样简单处理，无法显示具体
            text = !TextUtils.isEmpty(text) ? text : MyApplication.getInstance().getString(R.string.msg_call_end);
        } else if (type == XmppMessage.TYPE_NO_CONNECT_VOICE
                || type == XmppMessage.TYPE_NO_CONNECT_VIDEO) {
            // getLast接口内获取到数据时更新本地Friend的content，
            // 很尴尬的是音视频转文字消息消息 很关键的一个字段timeLen服务端没返回，
            // 只能这样简单处理，无法显示具体
            text = !TextUtils.isEmpty(text) ? text : MyApplication.getInstance().getString(R.string.msg_call_cancel);
        } else if (type == XmppMessage.TYPE_OK_MU_CONNECT_VOICE || type == XmppMessage.TYPE_EXIT_VOICE) {
            text = MyApplication.getInstance().getString(R.string.msg_voice_meeting);
        } else if (type == XmppMessage.TYPE_VIDEO_IN || type == XmppMessage.TYPE_VIDEO_OUT) {
            text = MyApplication.getInstance().getString(R.string.msg_video_meeting);
        } else if (type == XmppMessage.TYPE_TRANSFER) {
            text = MyApplication.getContext().getString(R.string.tip_transfer_money);
        } else if (type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
            text = MyApplication.getContext().getString(R.string.tip_transfer_money) + MyApplication.getContext().getString(R.string.transfer_friend_sure_save);
        } else if (type == XmppMessage.TYPE_TRANSFER_BACK) {
            text = MyApplication.getContext().getString(R.string.transfer_back);
        } else if (type == XmppMessage.TYPE_PAYMENT_OUT || type == XmppMessage.TYPE_RECEIPT_OUT) {
            text = MyApplication.getContext().getString(R.string.payment_get_notify);
        } else if (type == XmppMessage.TYPE_PAYMENT_GET || type == XmppMessage.TYPE_RECEIPT_GET) {
            text = MyApplication.getContext().getString(R.string.receipt_get_notify);
        } else if (type == XmppMessage.TYPE_PAY_CERTIFICATE) {
            text = MyApplication.getContext().getString(R.string.pay_certificate);
        } else if (type == XmppMessage.TYPE_SCAN_RECHARGE) {
            text = MyApplication.getContext().getString(R.string.scan_recharge_notify);
        } else if (type == XmppMessage.TYPE_SCAN_WITHDRAW) {
            text = MyApplication.getContext().getString(R.string.scan_withdraw_notify);
        } else if (type == XmppMessage.TYPE_SECURE_LOST_KEY) {
            text = splitText + MyApplication.getContext().getString(R.string.request_chat_key_group_thumb);
        } else if (type == XmppMessage.TYPE_MUCFILE_ADD) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.group_file) + "]";
        } else if (type == XmppMessage.TYPE_NEW_NOTICE) {
            text = splitText + "[" + MyApplication.getInstance().getString(R.string.group_bulletin) + "]";
        } else if (type == XmppMessage.TYPE_SERVICE_MENU) {
            text = "[" + MyApplication.getInstance().getString(R.string.service_menu) + "]";
        } else if (type == XmppMessage.TYPE_BACK || type == XmppMessage.TYPE_83
                || type == XmppMessage.TYPE_RED_BACK || type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
            text = MessageUtil.handlerGetLastSpecialMessage(isRoom, type, loginUserId, from, fromUserName, toUserName);
        } else if (type == XmppMessage.TYPE_SEND_MANAGER) {
            if (text.equals("1")) {
                text = (fromUserName + " " + MyApplication.getInstance().getString(R.string.setting) + toUserName + " " + MyApplication.getInstance().getString(R.string.message_admin));
            } else if (text.equals("0")) {
                text = (fromUserName + " " + MyApplication.getInstance().getString(R.string.sip_canceled) + toUserName + " " + MyApplication.getInstance().getString(R.string.message_admin));
            } // 以防万一，1和0以外情况认为已经处理过了，
        } else if (type == XmppMessage.TYPE_LIVE_GROUP_START) {
            text = MyApplication.getInstance().getString(R.string.tip_start_group_live);
        } else if (type == XmppMessage.TYPE_LIVE_GROUP_CLOSE
                || type == XmppMessage.TYPE_LIVE_GROUP_MANAGER_CLOSE) {
            text = MyApplication.getInstance().getString(R.string.tip_end_group_live);
        }
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("type", type);
            builder.updateColumnValue("content", text);
            builder.updateColumnValue("timeSend", time);
            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
            int update = friendDao.update(builder.prepare());
            Log.d(TAG, "updateApartDownloadTime() returned: " + update);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * todo 以下两个updateLastChatMessage 貌似都和NewFriend 有关 有点累赘了
     */

    /**
     * 更新朋友表里面的最后一条未读信息
     */
    public void updateLastChatMessage(String ownerId, String friendId, ChatMessage message) {
        Context context = MyApplication.getContext();
        int type = message.getType();
        String content = "";

        if (type == XmppMessage.TYPE_TEXT) {
            content = message.getContent();
        } else if (type == XmppMessage.TYPE_IMAGE) {
            content = "[" + MyApplication.getInstance().getString(R.string.image) + "]";
        } else if (type == XmppMessage.TYPE_CARD) {
            content = "[" + MyApplication.getInstance().getString(R.string.chat_card) + "]";
        } else if (type == XmppMessage.TYPE_VOICE) {
            content = "[" + MyApplication.getInstance().getString(R.string.voice) + "]";
        } else if (type == XmppMessage.TYPE_LOCATION) {
            content = "[" + MyApplication.getInstance().getString(R.string.my_location) + "]";
        } else if (type == XmppMessage.TYPE_GIF) {
            content = "[" + MyApplication.getInstance().getString(R.string.animation) + "]";
        } else if (type == XmppMessage.TYPE_VIDEO) {
            content = "[" + MyApplication.getInstance().getString(R.string.s_video) + "]";
        } else if (type == XmppMessage.TYPE_FILE) {
            content = "[" + MyApplication.getInstance().getString(R.string.s_file) + "]";
        } else if (type == XmppMessage.TYPE_RED) {
            content = "[" + MyApplication.getInstance().getString(R.string.chat_red) + "]";
        } else if (type == XmppMessage.TYPE_TIP) {
            //content = message.getContent();
        } else if (type == XmppMessage.TYPE_NEWSEE) {// 新关注提示
            if (!message.isMySend()) {
                content = MyApplication.getInstance().getString(R.string.friendobject_followyour);
            }
        } else if (type == XmppMessage.TYPE_SAYHELLO) {// 打招呼提示
            if (!message.isMySend()) {
                if (TextUtils.isEmpty(message.getContent())) {
                    content = context.getString(R.string.msg_be_say_hello);
                } else {
                    content = message.getContent();
                }
            }
        } else if (type == XmppMessage.TYPE_PASS) {// 验证通过提示
            if (!message.isMySend()) {
                content = MyApplication.getInstance().getString(R.string.friend_object_passgo);
                NewFriendDao.getInstance().changeNewFriendState(message.getFromUserId(), Friend.STATUS_13);
            }
        } else if (type == XmppMessage.TYPE_FRIEND) { // 新朋友提示
            if (!message.isMySend()) {
                content = message.getFromUserName() + context.getString(R.string.add_me_as_friend);
            }
        } else if (type == XmppMessage.TYPE_FEEDBACK) {// 回话
            if (!message.isMySend()) {
                if (!TextUtils.isEmpty(message.getContent())) {
                    content = message.getContent();
                }
            }
        } else if (type == XmppMessage.TYPE_BLACK) {
            if (!message.isMySend()) {
                content = context.getString(R.string.be_pull_black_place_holder, message.getFromUserId());
            } else {
                content = context.getString(R.string.pull_black_place_holder, message.getFromUserId());
            }
        } else if (type == XmppMessage.TYPE_DELALL || type == XmppMessage.TYPE_BACK_DELETE) {
            if (!message.isMySend()) {
                content = context.getString(R.string.be_delete_place_holder, message.getFromUserId());
            } else {
                content = context.getString(R.string.delete_place_holder, message.getFromUserId());
            }
        } else if (type == XmppMessage.TYPE_RECOMMEND) {
            content = context.getString(R.string.msg_has_new_recommend_friend);
        } else if (type == XmppMessage.TYPE_LINK || type == XmppMessage.TYPE_SHARE_LINK) {
            content = "[" + MyApplication.getInstance().getString(R.string.link) + "]";
        } else if (type == XmppMessage.TYPE_IMAGE_TEXT || type == XmppMessage.TYPE_IMAGE_TEXT_HTML || type == XmppMessage.TYPE_IMAGE_TEXT_MANY) {
            content = "[" + MyApplication.getInstance().getString(R.string.graphic) + MyApplication.getInstance().getString(R.string.mainviewcontroller_message) + "]";
        } else if (type == XmppMessage.TYPE_CHAT_HISTORY) {
            content = context.getString(R.string.msg_chat_history);
        } else {
            content = message.getContent();
        }

        if (TextUtils.isEmpty(content)) {
            content = "";
        }
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            builder.updateColumnValue("type", type);
            builder.updateColumnValue("content", content);
            builder.updateColumnValue("timeSend", message.getTimeSend());
            builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            friendDao.update(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateLastChatMessage(String ownerId, String friendId, String content) {
        QueryBuilder<Friend, Integer> queryBuilder = friendDao.queryBuilder();
        UpdateBuilder<Friend, Integer> builder = friendDao.updateBuilder();
        try {
            queryBuilder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
            Friend friend = queryBuilder.queryForFirst();
            if (friend != null) {
                builder.updateColumnValue("type", XmppMessage.TYPE_TEXT);
                builder.updateColumnValue("content", content);
                builder.updateColumnValue("timeSend", TimeUtils.sk_time_current_time());
                builder.where().eq("ownerId", ownerId).and().eq("userId", friendId);
                friendDao.update(builder.prepare());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addAttentionUsers(final String loginUserId, final List<AttentionUser> attentionUsers,
                                  final OnCompleteListener2 listener) throws SQLException {
        new TransactionManager(friendDao.getConnectionSource()).callInTransaction(() -> {
            checkSystemFriend(loginUserId);
            int tableVersion = TableVersionSp.getInstance(MyApplication.getInstance()).getFriendTableVersion(loginUserId);
            int newVersion = tableVersion + 1;
            if (attentionUsers != null && attentionUsers.size() > 0) {
                for (int i = 0; i < attentionUsers.size(); i++) {
                    AttentionUser attentionUser = attentionUsers.get(i);
                    if (attentionUser == null) {
                        continue;
                    }
                    String userId = attentionUser.getToUserId();// 好友的Id
                    QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
                    Friend friend = null;
                    try {
                        builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
                        friend = friendDao.queryForFirst(builder.prepare());
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    if (friend == null) {
                        friend = new Friend();
                    }
                    friend.setOwnerId(attentionUser.getUserId());
                    friend.setUserId(attentionUser.getToUserId());
                    friend.setAccount(attentionUser.getAccount());
                    if (!userId.equals(Friend.ID_SYSTEM_MESSAGE)) {
                        friend.setNickName(attentionUser.getToNickName());
                        friend.setRemarkName(attentionUser.getRemarkName());
                        friend.setTimeCreate(attentionUser.getCreateTime());
                        // 公众号的status为8，服务端返回的为2，不修改
                        int status = (attentionUser.getBlacklist() == 0) ? attentionUser.getStatus() : -1;
                        friend.setStatus(status);
                    }
                    friend.setHiding(attentionUser.getHiding());
                    if (attentionUser.getToUserType() == 2
                            || attentionUser.getToUserType() == 4) {// 公众号||客服公司号，
                        friend.setStatus(Friend.STATUS_SYSTEM);
                        if (attentionUser.getToUserType() == 4) {
                            friend.setService(true);
                        }
                    }
                    if (!TextUtils.isEmpty(attentionUser.getDescribe())) {
                        friend.setDescribe(attentionUser.getDescribe());
                    }
                    // todo 注意 IsBeenBlack==1表示 为对方拉黑了我，不能将其状态置为STATUS_BLACKLIST
                    // todo 注意 blacklist==1才表示我将对方拉入黑名单，但是我将对方拉入黑名单之后，该接口就不在返回此人了，所以在通讯录的黑名单内需要单独调用获取黑名单列表的接口
                    if (attentionUser.getBlacklist() == 1) {
                        friend.setStatus(Friend.STATUS_BLACKLIST);
                    }
                    if (attentionUser.getIsBeenBlack() == 1) {
                        // friend.setStatus(Friend.STATUS_BLACKLIST);
                        friend.setStatus(Friend.STATUS_19);
                    }

                    friend.setOfflineNoPushMsg(attentionUser.getOfflineNoPushMsg());
                    friend.setHideChatSwitch(attentionUser.getHideChatSwitch());
                    friend.setIsSendRead(attentionUser.getIsSendMsgState());
                    friend.setTopTime(attentionUser.getOpenTopChatTime());
                    PreferenceUtils.putInt(MyApplication.getContext(), Constants.MESSAGE_READ_FIRE + attentionUser.getToUserId() + CoreManager.requireSelf(MyApplication.getContext()).getUserId(),
                            attentionUser.getIsOpenSnapchat());
                    friend.setChatRecordTimeOut(attentionUser.getChatRecordTimeOut());// 消息保存天数 -1/0 永久
                    //  SecureFlag
                    friend.setEncryptType(attentionUser.getEncryptType());
                    if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
                        PublicKeyDao.getInstance().updatePublicKey(attentionUser.getUserId(), attentionUser.getToUserId(),
                                new PublicKey(attentionUser.getUserId(), attentionUser.getToUserId(), attentionUser.getDhMsgPublicKey(), attentionUser.getCreateTime()));
                        friend.setPublicKeyDH(attentionUser.getDhMsgPublicKey());
                        friend.setPublicKeyRSARoom(attentionUser.getRsaMsgPublicKey());
                    }

                    friend.setCompanyId(attentionUser.getCompanyId());
                    friend.setRoomFlag(0);
                    friend.setVersion(newVersion);// 更新版本
                    try {
                        createOrUpdateByUserId(friend);
                        if (listener != null) {
                            listener.onLoading(i + 1, attentionUsers.size());
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            // 本地Sp中保存的版本号更新（+1）
            TableVersionSp.getInstance(MyApplication.getInstance()).setFriendTableVersion(loginUserId, newVersion);
            // 更新完成，把过期的好友数据删除
            try {
                DeleteBuilder<Friend, Integer> builder = friendDao.deleteBuilder();

                builder.where()
                        .eq("ownerId", loginUserId).and().eq("roomFlag", 0).and()
                        .in("status", new Object[]{Friend.STATUS_FRIEND, Friend.STATUS_ATTENTION}).and()
                        .ne("version", newVersion);
                friendDao.delete(builder.prepare());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // 朋友数据更新了，在去删除不存在的消息表
            List<String> tables = SQLiteRawUtil.getUserChatMessageTables(mHelper.getReadableDatabase(), loginUserId);
            if (tables != null && tables.size() > 0) {
                for (int i = 0; i < tables.size(); i++) {
                    String tableName = tables.get(i);
                    String tablePrefix = SQLiteRawUtil.CHAT_MESSAGE_TABLE_PREFIX + loginUserId;
                    int index = tableName.indexOf(tablePrefix);
                    if (index == -1) {
                        continue;
                    }
                    String toUserId = tableName.substring(index + tablePrefix.length(), tableName.length());
                    if (toUserId.equals(Friend.ID_BLOG_MESSAGE) || toUserId.equals(Friend.ID_INTERVIEW_MESSAGE)
                            || toUserId.equals(Friend.ID_NEW_FRIEND_MESSAGE) || toUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {
                        continue;
                    }
                    Friend friend = getFriend(loginUserId, toUserId);
                    if (friend == null) {// 删除这张消息表

                        if (SQLiteRawUtil.isTableExist(mHelper.getWritableDatabase(), tableName)) {
                            SQLiteRawUtil.dropTable(mHelper.getReadableDatabase(), tableName);
                        }

                    }
                }
            }

            if (listener != null) {
                listener.onCompleted();
            }

            // 这里没用到返回值，但是这个方法必须返回，
            return Void.class;
        });
    }

    /**
     * 用户数据更新，下载进入的房间
     */
    public void addRooms(final Handler handler, final String loginUserId, final List<MucRoom> rooms, final OnCompleteListener2 listener) {
        try {
            new TransactionManager(friendDao.getConnectionSource()).callInTransaction(() -> {
                long startTime = System.currentTimeMillis();
                int tableVersion = TableVersionSp.getInstance(MyApplication.getInstance()).getFriendTableVersion(loginUserId);
                int newVersion = tableVersion + 1;
                if (rooms != null && rooms.size() > 0) {
                    SharedPreferences.Editor edit = PreferenceUtils.getPreferences(MyApplication.getContext()).edit();
                    for (int i = 0; i < rooms.size(); i++) {
                        MucRoom mucRoom = rooms.get(i);
                        if (mucRoom == null) {
                            continue;
                        }
                        if (!MyApplication.IS_SUPPORT_SECURE_CHAT) {
                            // SecureFlagGroup 非端到端，兼容之前在端到端版本注册过的账号，过滤掉私密群组
                            if (mucRoom.getIsSecretGroup() == 1) {
                                continue;
                            }
                        }
                        String userId = mucRoom.getJid();// 群组的jid
                        MyApplication.getInstance().saveGroupPartStatus(edit, userId, mucRoom.getShowRead(), mucRoom.getAllowSendCard(),
                                mucRoom.getAllowConference(), mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime(), mucRoom.getAllowOpenLive());

                        QueryBuilder<Friend, Integer> builder = friendDao.queryBuilder();
                        Friend friend = null;
                        try {
                            builder.where().eq("ownerId", loginUserId).and().eq("userId", userId);
                            friend = friendDao.queryForFirst(builder.prepare());
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                        if (friend == null) {
                            friend = new Friend();
                            friend.setOwnerId(loginUserId);
                            friend.setUserId(mucRoom.getJid());

                            friend.setTimeSend((int) mucRoom.getCreateTime());
                        }
                        friend.setNickName(mucRoom.getName());
                        friend.setDescription(mucRoom.getDesc());
                        friend.setRoomId(mucRoom.getId());
                        friend.setRoomCreateUserId(mucRoom.getUserId());
                        friend.setChatRecordTimeOut(mucRoom.getChatRecordTimeOut());// 消息保存天数 -1/0 永久
                        if (mucRoom.getCategory() == 510 &&
                                mucRoom.getUserId().equals(CoreManager.requireSelf(MyApplication.getInstance()).getUserId())) {
                            friend.setRoomFlag(510);// 我的手机联系人群组
                        } else {
                            friend.setRoomFlag(1);
                        }
                        friend.setStatus(Friend.STATUS_FRIEND);
                        friend.setVersion(newVersion);// 更新版本
                        MucRoomMember memberMy = mucRoom.getMember();
                        if (memberMy != null) {
                            friend.setRoomRole(memberMy.getRole());
                            friend.setRoomMyNickName(memberMy.getNickName());
                            friend.setRoomTalkTime(memberMy.getTalkTime());
                            friend.setOfflineNoPushMsg(memberMy.getOfflineNoPushMsg());
                            friend.setHideChatSwitch(memberMy.getHideChatSwitch());
                            friend.setTopTime(memberMy.getOpenTopChatTime());
                            if (memberMy.getIsBlack() == 1) {
                                friend.setGroupStatus(4);
                            }
                            //  SecureFlagGroup
                            friend.setEncryptType(mucRoom.getEncryptType());
                            friend.setIsSecretGroup(mucRoom.getIsSecretGroup());
                            friend.setJoinSeqNo(memberMy.getJoinSeqNo());
                            if (friend.getIsSecretGroup() == 1 && friend.getIsLostChatKeyGroup() != 1) {
                                String secretKey = memberMy.getChatKeyGroup();
                                try {
                                    String chatKey = new String(RSA.decryptFromBase64(secretKey,
                                            Base64.decode(SecureChatUtil.getRSAPrivateKey(CoreManager.requireSelf(MyApplication.getContext()).getUserId()))));
                                    friend.setChatKeyGroup(SecureChatUtil.encryptChatKey(mucRoom.getJid(), chatKey));
                                    Log.e("msg", "设置chatKey成功-->" + chatKey);
                                } catch (Exception e) {
                                    Log.e("msg", "设置chatKey失败");
                                    friend.setIsLostChatKeyGroup(friend.getUserId(), 1);
                                }
                            }
                        } else {
                            continue;
                        }
                        try {
                            createOrUpdateByUserId(friend);
                            if (listener != null) {
                                listener.onLoading(i + 1, rooms.size());
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    edit.apply();
                }
                // 本地Sp中保存的版本号更新（+1）
                TableVersionSp.getInstance(MyApplication.getInstance()).setFriendTableVersion(loginUserId, newVersion);
                // 更新完成，把过期的房间数据删除
                try {
                    DeleteBuilder<Friend, Integer> builder = friendDao.deleteBuilder();
                    builder.where().eq("ownerId", loginUserId).and().eq("roomFlag", 1).and().eq("status", Friend.STATUS_FRIEND).and()
                            .ne("version", newVersion);
                    friendDao.delete(builder.prepare());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // 朋友数据更新了，在去删除不存在的消息表
                List<String> tables = SQLiteRawUtil.getUserChatMessageTables(mHelper.getReadableDatabase(), loginUserId);
                if (tables != null && tables.size() > 0) {
                    for (int i = 0; i < tables.size(); i++) {
                        String tableName = tables.get(i);
                        String tablePrefix = SQLiteRawUtil.CHAT_MESSAGE_TABLE_PREFIX + loginUserId;
                        int index = tableName.indexOf(tablePrefix);
                        if (index == -1) {
                            continue;
                        }
                        String toUserId = tableName.substring(index + tablePrefix.length(), tableName.length());
                        if (toUserId.equals(Friend.ID_BLOG_MESSAGE) || toUserId.equals(Friend.ID_INTERVIEW_MESSAGE)
                                || toUserId.equals(Friend.ID_NEW_FRIEND_MESSAGE) || toUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {
                            continue;
                        }
                        Friend friend = getFriend(loginUserId, toUserId);
                        if (friend == null) {// 删除这张消息表
                            if (SQLiteRawUtil.isTableExist(mHelper.getWritableDatabase(), tableName)) {
                                SQLiteRawUtil.dropTable(mHelper.getReadableDatabase(), tableName);
                            }
                        }
                    }
                }

                if (handler != null && listener != null) {
                    handler.post(() -> listener.onCompleted());
                }
                Log.e(TAG, "addRooms: success in " + (System.currentTimeMillis() - startTime));
                // 这里没用到返回值，但是这个方法必须返回，
                return Void.class;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("保存群组异常", e);
        }
    }

    public List<Friend> getChatFriendList(String ownerId) throws SQLException {
        return friendDao.queryBuilder()
                .orderBy("topTime", false)
                .orderBy("timeSend", false)
                .where().eq("ownerId", ownerId)
                .and().eq("status", Friend.STATUS_FRIEND)
                .and().isNotNull("content")
                .query();
    }

    public List<Friend> searchFriend(String ownerId, String str) throws SQLException {
        boolean isPrivateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + ownerId, true);
        Object[] objects;
        if (isPrivateMode) {
            objects = new Object[]{0};
        } else {
            objects = new Object[]{0, 1};
        }
        String fuzzySearchKey = "%" + str + "%";
        return friendDao.queryBuilder()
                .orderBy("topTime", false)
                .orderBy("timeSend", false)
                .where().notIn("status", Friend.STATUS_BLACKLIST, Friend.STATUS_19, Friend.STATUS_23)
                .and().eq("ownerId", ownerId)
                .and().in("hideChatSwitch", objects)// 私密模式
                .and().eq("isDevice", 0)// 移除我的设备
                .and().eq("roomFlag", 0)// 移除房间
                .and().eq("companyId", 0)
                .like("nickName", fuzzySearchKey)
                .or().like("remarkName", fuzzySearchKey)
                .or().like("account", fuzzySearchKey)
                .and(2)
                .query();
    }

    public List<Friend> searchRoom(String ownerId, String str) throws SQLException {
        String fuzzySearchKey = "%" + str + "%";
        return friendDao.queryBuilder()
                .orderBy("topTime", false)
                .orderBy("timeSend", false)
                .where().eq("ownerId", ownerId).and()
                .in("groupStatus", 0, 3, 4).and()
                .in("roomFlag", 1, 510)
                .and().like("nickName", fuzzySearchKey)
                .query();
    }

    /**
     * 获取全部好友 过滤掉私密模式好友
     * todo 私密模式已调整，该方法为根据私密模式开关控制是否显示私密会话好友，仅限通讯录页面使用
     *
     * @param ownerId
     * @return
     */
    public List<Friend> getAllFriendsWithOutHideChat(String ownerId) {
        boolean isPrivateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + ownerId, true);
        Object[] objects;
        if (isPrivateMode) {
            objects = new Object[]{0};
        } else {
            objects = new Object[]{0, 1};
        }
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    .and().in("status", Friend.STATUS_FRIEND)// 仅限我的好友
                    .and().in("hideChatSwitch", objects)// 私密模式
                    .and().eq("isDevice", 0)// 移除我的设备
                    .and().eq("roomFlag", 0)// 移除房间
                    .prepare();

            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * 获取全部群组 过滤掉私密模式群组
     * todo 私密模式已调整，该方法为根据私密模式开关控制是否显示私密会话群组，仅限全局搜索以及群组列表界面使用
     *
     * @param ownerId
     * @return
     */
    public List<Friend> getAllRoomsWithOutHideChat(String ownerId) {
        boolean isPrivateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + ownerId, true);
        Object[] objects;
        if (isPrivateMode) {
            objects = new Object[]{0};
        } else {
            objects = new Object[]{0, 1};
        }
        List<Friend> query = new ArrayList<>();
        try {
            PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                    .eq("ownerId", ownerId)
                    .and().eq("groupStatus", 0)
                    .and().in("hideChatSwitch", objects)// 私密模式
                    .and().in("roomFlag", 1, 510)
                    .prepare();
            query = friendDao.query(preparedQuery);
            return query;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return query;
    }

    /**
     * 获取全部群组 过滤掉私密模式群组
     * todo 接上，方法重载
     * <p>
     * 用于遍历群组，代替{@link #getAllRooms(String)}
     * <p>
     * 为了避免读取数据过多导致占用过大内存，使用iterable而不是list,
     */
    public void getAllRoomsWithOutHideChat(
            String ownerId,
            AsyncUtils.Function<Iterator<Friend>> callback
    ) throws Exception {
        boolean isPrivateMode = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.PRIVATE_MODE + ownerId, true);
        Object[] objects;
        if (isPrivateMode) {
            objects = new Object[]{0};
        } else {
            objects = new Object[]{0, 1};
        }
        PreparedQuery<Friend> preparedQuery = friendDao.queryBuilder().where()
                .eq("ownerId", ownerId)
                .and().in("groupStatus", 0, 3, 4)
                .and().in("hideChatSwitch", objects)// 私密模式
                .and().in("roomFlag", 1, 510)
                .prepare();
        CloseableIterator<Friend> results = friendDao.iterator(preparedQuery);
        callback.apply(results);
        results.close();
    }
}