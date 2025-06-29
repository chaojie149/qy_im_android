package com.tongxin.caihong.ui.message;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.TrillStatisticsHelper;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.BroadcastHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.view.HorizontalListView;
import com.tongxin.caihong.view.MessageAvatar;
import com.tongxin.caihong.view.SearchHeader;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 转发 最近联系人
 */
public class InstantMessageActivity extends BaseActivity implements OnClickListener {
    private TextView mCreateChat;
    private ListView mLvRecentlyMessage;
    private MessageRecentlyAdapter messageRecentlyAdapter;
    private List<Friend> friends;
    private List<BaseSortModel<Friend>> mOriginalSortFriends;
    private List<BaseSortModel<Friend>> mSortFriends;

    private HorizontalListView mHorizontalListView;
    private HorListViewAdapter mHorAdapter;
    private List<String> mSelectPositions;
    private Button mOkBtn;
    private InstantMessageConfirmNew menuWindow;

    // 是否为多选转发
    private boolean isMoreSelected;
    // 逐条还是合并转发
    private boolean isSingleOrMerge;
    // 通过toUserId与messageId从数据库查询到需要转发的消息，进行转发
    // 如为多选转发则不需要，直接通过EventBus通知到聊天界面转发
    private String toUserId;
    private String messageId;
    // 仅结束转发界面，不跳转
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), OtherBroadcast.MSG_BACK)) {
                if (messageId != null) {
                    String packetId = intent.getStringExtra("packetId");
                    if (TextUtils.equals(packetId, messageId)) {
                        finish();
                    }
                }
            }
        }
    };
    private String mLoginUserId;
    private RoomMember mRoomMember;
    // 转发数量，当前转发序号
    private int sendSize;
    private int currentSendSerialNumber;
    private boolean isOnlyFinishNotIntent;

    public static void start(Context context, String fromUserId, String messageId, boolean isOnlyFinishNotIntent) {
        Intent intent = new Intent(context, InstantMessageActivity.class);
        intent.putExtra("fromUserId", fromUserId);
        intent.putExtra("messageId", messageId);
        intent.putExtra("isOnlyFinishNotIntent", isOnlyFinishNotIntent);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messageinstant);
        isMoreSelected = getIntent().getBooleanExtra(Constants.IS_MORE_SELECTED_INSTANT, false);
        isSingleOrMerge = getIntent().getBooleanExtra(Constants.IS_SINGLE_OR_MERGE, false);
        isOnlyFinishNotIntent = true; // 统一不跳转到转发对象的聊天页面，

        toUserId = getIntent().getStringExtra("fromUserId");
        messageId = getIntent().getStringExtra("messageId");

        mOriginalSortFriends = new ArrayList<>();
        mSortFriends = new ArrayList<>();
        mSelectPositions = new ArrayList<>();

        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        loadData();
        initView();
        EventBusHelper.register(this);
        BroadcastHelper.register(this, receiver, OtherBroadcast.MSG_BACK);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.most_recent_contact));
    }

    private void loadData() {
        friends = FriendDao.getInstance().getNearlyFriendMsg(coreManager.getSelf().getUserId());
        List<Friend> filter = new ArrayList<>();
        for (int i = 0; i < friends.size(); i++) {
            if (friends.get(i).getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)
                    || friends.get(i).getUserId().equals(Friend.ID_SK_PAY)
                    || friends.get(i).getUserId().equals(mLoginUserId)) {
                filter.add(friends.get(i));
            }
        }
        friends.removeAll(filter);
        loadDataFriend(friends);
    }

    private void initView() {
        mCreateChat = findViewById(R.id.tv_create_newmessage);
        mCreateChat.setOnClickListener(this);

        mLvRecentlyMessage = findViewById(R.id.lv_recently_message);
        messageRecentlyAdapter = new MessageRecentlyAdapter();
        mLvRecentlyMessage.setAdapter(messageRecentlyAdapter);
        mLvRecentlyMessage.setOnItemClickListener((parent, view, position, id) -> {
            if (hasSelected(mSortFriends.get(position).bean.getUserId())) {
                removeSelect(mSortFriends.get(position).bean.getUserId());
            } else {
                addSelect(mSortFriends.get(position).bean.getUserId());
            }
            messageRecentlyAdapter.notifyDataSetChanged();
        });

        mHorAdapter = new HorListViewAdapter();
        mHorizontalListView = (HorizontalListView) findViewById(R.id.horizontal_list_view);
        mHorizontalListView.setAdapter(mHorAdapter);
        mHorAdapter.notifyDataSetChanged();
        mOkBtn = (Button) findViewById(R.id.ok_btn);
        ButtonColorChange.colorChange(mContext, mOkBtn);
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));

        mHorizontalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mSelectPositions.remove(position);
                mHorAdapter.notifyDataSetChanged();
                messageRecentlyAdapter.notifyDataSetChanged();
                mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
            }
        });

        mOkBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectPositions.isEmpty()) {
                    ToastUtil.showToast(v.getContext(), R.string.tip_select_at_lease_one_contacts);
                    return;
                }
                showPopuWindow(v, mSelectPositions);
            }
        });

        SearchHeader shSearch = findViewById(R.id.shSearch);
        shSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mSortFriends.clear();
                String text = editable.toString().toLowerCase();
                if (TextUtils.isEmpty(text)) {
                    mSortFriends.addAll(mOriginalSortFriends);
                } else {
                    for (BaseSortModel<Friend> friend : mOriginalSortFriends) {
                        if (friend.getBean().getShowName().toLowerCase().contains(text)) {
                            mSortFriends.add(friend);
                        }
                    }
                }
                messageRecentlyAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean hasSelected(String userId) {
        for (String s : mSelectPositions) {
            if (TextUtils.equals(s, userId)) {
                return true;
            }
        }
        return false;
    }

    private void addSelect(String userId) {
        mSelectPositions.add(userId);
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void removeSelect(String userId) {
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i).equals(userId)) {
                mSelectPositions.remove(i);
            }
        }
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void showPopuWindow(View view, List<String> stringsId) {
        if (menuWindow != null) {
            menuWindow.dismiss();
        }
        List<Friend> friendList = new ArrayList<>();
        for (int i = 0; i < stringsId.size(); i++) {
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), stringsId.get(i));
            friendList.add(friend);
        }
        menuWindow = new InstantMessageConfirmNew(InstantMessageActivity.this, new ClickListener(friendList), friendList);
        menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, EventNotifyByTag.FinishInstantRelatedActivity)) {
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.tv_create_newmessage:
                Intent intent = new Intent(this, SelectNewContactsActivity.class);
                intent.putExtra(Constants.IS_MORE_SELECTED_INSTANT, isMoreSelected);
                intent.putExtra(Constants.IS_SINGLE_OR_MERGE, isSingleOrMerge);
                intent.putExtra("fromUserId", toUserId);
                intent.putExtra("messageId", messageId);
                intent.putExtra("isOnlyFinishNotIntent", isOnlyFinishNotIntent);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    private void forwardingStep(Friend friend) {
        if (isMoreSelected) {
            // 多选转发 通知多选页面(即多选消息的单聊 || 群聊页面，在该页面获取选中的消息在发送出去)
            EventBus.getDefault().post(new EventMoreSelected(friend.getUserId(), isSingleOrMerge, friend.getRoomFlag() != 0, friend.getIsDevice() != 0));
        } else {
            // 普通转发
            if (friend.getRoomFlag() == 0) {
                // 单聊
                int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + friend.getUserId() + mLoginUserId, 0);
                ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, messageId);
                cleanAtStatus(chatMessage);
                chatMessage.setFromId(null);
                chatMessage.setToId(null);
                TrillStatisticsHelper.share(this, coreManager, chatMessage);
                if (chatMessage.getType() == XmppMessage.TYPE_TEXT
                        || chatMessage.getType() == XmppMessage.TYPE_VOICE
                        || chatMessage.getType() == XmppMessage.TYPE_IMAGE
                        || chatMessage.getType() == XmppMessage.TYPE_VIDEO) {
                    chatMessage.setIsReadDel(isReadDel);
                }
                chatMessage.setFromUserId(mLoginUserId);
                chatMessage.setFromUserName(coreManager.getSelf().getNickName());
                if (friend.getIsDevice() == 1) {
                    chatMessage.setToUserId(mLoginUserId);
                    chatMessage.setToUserName(friend.getUserId());
                } else {
                    chatMessage.setToUserId(friend.getUserId());

                    // sz 消息过期时间
                    if (friend.getChatRecordTimeOut() == -1 || friend.getChatRecordTimeOut() == 0) {
                        // 永久
                        chatMessage.setDeleteTime(-1);
                    } else {
                        long deleteTime = TimeUtils.sk_time_current_time() + (long) (friend.getChatRecordTimeOut() * 24 * 60 * 60);
                        chatMessage.setDeleteTime(deleteTime);
                    }
                }
                chatMessage.setUpload(true);
                chatMessage.setMySend(true);
                chatMessage.setSendRead(false);
                chatMessage.setReadPersons(0);
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                    ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, false);
                }
                coreManager.sendChatMessage(chatMessage.getToUserId(), chatMessage);
            } else {
                // 群聊
                instantChatMessage(friend, toUserId, messageId);
            }
        }
        finishThis();
    }

    /**
     * 针对获取群组信息接口回调时，未走到forwardingStep方法内调用
     */
    private void faultTolerance() {
        currentSendSerialNumber++;
        finishThis();
    }

    /**
     * 转发到最后一个结束当前界面
     */
    public void finishThis() {
        if (currentSendSerialNumber == sendSize) {
            if (isMoreSelected) {
                // 发送多选收藏的event，但此event仅做关闭聊天界面多选状态用
                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedCollection", false, true));
            } else {
                MsgBroadcast.broadcastMsgUiUpdate(mContext);
            }
            if (!isOnlyFinishNotIntent && mSelectPositions.size() == 1) {
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mSelectPositions.get(0));
                if (friend.getRoomFlag() == 1) {
                    MucChatActivity.start(mContext, friend);
                } else {
                    ChatActivity.start(mContext, friend);
                }
            }
            finish();
        }
    }

    public void cleanAtStatus(ChatMessage chatMessage) {
        if (chatMessage.getType() == XmppMessage.TYPE_TEXT
                && !TextUtils.isEmpty(chatMessage.getObjectId())) {
            // @消息，转发出去时变为纯文本，不具备@功能
            chatMessage.setObjectId("");
        }
    }

    public boolean isAuthenticated() {
        boolean isLogin = coreManager.isLogin();
        if (!isLogin) {
            coreManager.autoReconnect(this);
        }
        // Todo 离线时发消息也不能return，自动重连...，让消息转圈(有重发)
        return false;
    }

    private void send(String UserId, ChatMessage message) {
        // 一些异步回调进来的也要判断xmpp是否在线，
        // 比如图片上传成功后，
        if (isAuthenticated()) {
            return;
        }
        coreManager.sendMucChatMessage(UserId, message);
    }

    public boolean isOk() {// 群主与管理员不受限制
        boolean isOk = true;
        if (mRoomMember != null) {
            if (mRoomMember.getRole() == 1 || mRoomMember.getRole() == 2) {
                isOk = true;
            } else {
                isOk = false;
            }
        }
        return isOk;
    }

    private void instantChatMessage(Friend mFriend, String toUserId, String messageId) {
        if (!TextUtils.isEmpty(messageId)) {
            ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, messageId);
            cleanAtStatus(chatMessage);
            chatMessage.setFromId(null);
            chatMessage.setToId(null);
            boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mFriend.getUserId(), true);
            if (mFriend.getGroupStatus() == 0
                    || mFriend.getGroupStatus() == 3
                    || mFriend.getGroupStatus() == 4) {
                // 正常状态
                List<RoomMember> roomMemberList = RoomMemberDao.getInstance().getRoomMember(mFriend.getRoomId());
                if (roomMemberList.size() > 0) {
                    mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), mLoginUserId);
                }
            }

            if (chatMessage.getType() == ChatMessage.TYPE_FILE && !isAllowSendFile && !isOk()) {
                Toast.makeText(this, getString(R.string.tip_cannot_upload), Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isAllowSendCard = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mFriend.getUserId(), true);
            if (chatMessage.getType() == ChatMessage.TYPE_CARD && !isAllowSendCard && !isOk()) {
                Toast.makeText(this, getString(R.string.tip_member_disable_privately_chat), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mRoomMember != null && MucRoomMember.disallowPublicAction(mRoomMember.getRole())) {
                ToastUtil.showToast(mContext, getString(R.string.tip_action_disallow_place_holder, getString(MucRoomMember.getRoleName(mRoomMember.getRole()))));
                return;
            }
            TrillStatisticsHelper.share(this, coreManager, chatMessage);
            chatMessage.setFromUserId(mLoginUserId);
            chatMessage.setFromUserName(coreManager.getSelf().getNickName());
            chatMessage.setToUserId(mFriend.getUserId());
            chatMessage.setUpload(true);
            chatMessage.setMySend(true);
            chatMessage.setReadPersons(0);
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, mFriend.getUserId(), chatMessage, true);
            }
            send(mFriend.getUserId(), chatMessage);
        }
    }

    private void loadDataFriend(List<Friend> friends) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            // 改成不排序，其他用于排序的老代码保留，
            List<BaseSortModel<Friend>> sortedList = new ArrayList<>(friends.size());
            for (Friend f : friends) {
                sortedList.add(SortHelper.toSortedModel(f, Friend::getShowName));
            }
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mOriginalSortFriends = sortedList;
                mSortFriends.addAll(mOriginalSortFriends);
                messageRecentlyAdapter.setData(mSortFriends);
            });
        });
    }

    /**
     * 获取自己在该群组的信息(职位、昵称、禁言时间等)以及群属性
     */
    private void isSupportSend(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", friend.getRoomId());

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// 数据结果与room/get接口一样，只是服务端没有返回群成员列表的数据
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();
                                     if (mucRoom.getMember() == null) {// 被踢出该群组
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 1);// 更新本地群组状态
                                         DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_forward_kick));
                                         faultTolerance();
                                     } else {// 正常状态
                                         if (mucRoom.getS() == -1) {// 该群组已被锁定
                                             FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 3);// 更新本地群组状态
                                             DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_group_disable_by_service));
                                             faultTolerance();
                                             return;
                                         }
                                         int role = mucRoom.getMember().getRole();
                                         // 更新禁言状态
                                         FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, mucRoom.getJid(), mucRoom.getMember().getTalkTime());

                                         // 更新部分群属性
                                         MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(),
                                                 mucRoom.getAllowSendCard(), mucRoom.getAllowConference(),
                                                 mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime(), mucRoom.getAllowOpenLive());

                                         // 更新个人职位
                                         RoomMemberDao.getInstance().updateRoomMemberRole(mucRoom.getId(), mLoginUserId, role);

                                         if (role == 4) {
                                             DialogHelper.tip(mContext, getString(R.string.hint_invisible));
                                             faultTolerance();
                                             return;
                                         }
                                         if (role == 1 || role == 2) {// 群组或管理员 直接转发出去
                                             currentSendSerialNumber++;
                                             forwardingStep(friend);
                                         } else {
                                             if (mucRoom.getTalkTime() > 0) {// 全体禁言
                                                 DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_now_ban_all));
                                                 faultTolerance();
                                             } else if ((mucRoom.getMember().getTalkTime() > System.currentTimeMillis() / 1000)
                                                     || (mucRoom.getMember().getTalkTime() == -1)) {// 禁言
                                                 DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_forward_ban));
                                                 faultTolerance();
                                             } else {
                                                 currentSendSerialNumber++;
                                                 forwardingStep(friend);
                                             }
                                         }
                                     }
                                 } else if (Result.checkError(result, Result.CODE_ROOM_GONE)) {
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, friend.getUserId(), 2);// 更新本地群组状态
                                     DialogHelper.tip(InstantMessageActivity.this, getString(R.string.tip_forward_disbanded));
                                     faultTolerance();
                                 } else {
                                     Result.toastElse(mContext, result);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                                 faultTolerance();
                             }
                         }
                );
    }

    /**
     * 事件的监听
     */
    class ClickListener implements OnClickListener {
        private List<Friend> friends;

        public ClickListener(List<Friend> friends) {
            sendSize = friends.size();
            this.friends = friends;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_send:// 发送
                    for (int i = 0; i < friends.size(); i++) {
                        if (friends.get(i).getRoomFlag() != 0) {// 群组，调接口判断一些群属性状态
                            if (friends.get(i).getIsLostChatKeyGroup() == 1) {
                                ToastUtil.showToast(mContext, getString(R.string.is_lost_key_cannot_support_send_msg, friends.get(i).getNickName()));
                                faultTolerance();
                            } else {
                                isSupportSend(friends.get(i));
                            }
                        } else {
                            currentSendSerialNumber++;
                            forwardingStep(friends.get(i));
                        }
                    }
                    break;
                case R.id.btn_cancle:
                    break;
                default:
                    break;
            }
        }
    }

    class MessageRecentlyAdapter extends BaseAdapter implements SectionIndexer {
        List<BaseSortModel<Friend>> mSortFriends;

        public MessageRecentlyAdapter() {
            mSortFriends = new ArrayList<>();
        }

        public void setData(List<BaseSortModel<Friend>> sortFriends) {
            mSortFriends = sortFriends;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mSortFriends != null) {
                return mSortFriends.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mSortFriends != null) {
                return mSortFriends.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (mSortFriends != null) {
                return position;
            }

            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(InstantMessageActivity.this, R.layout.item_recently_contacts, null);
                holder = new ViewHolder();
                holder.mIvHead = (MessageAvatar) convertView.findViewById(R.id.iv_recently_contacts_head);
                holder.mTvName = (TextView) convertView.findViewById(R.id.tv_recently_contacts_name);
                holder.checkBox = convertView.findViewById(R.id.cb_instant);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.checkBox.setVisibility(View.VISIBLE);

            Friend friend = mSortFriends.get(position).getBean();
            ColorStateList tabColor = SkinUtils.getSkin(InstantMessageActivity.this).getTabColorState();
            if (friend != null) {
                holder.checkBox.setChecked(false);
                if (hasSelected(friend.getUserId())) {
                    holder.checkBox.setChecked(true);
                    Drawable drawable = getResources().getDrawable(R.drawable.sel_check_wx2);
                    drawable = DrawableCompat.wrap(drawable);
                    DrawableCompat.setTintList(drawable, tabColor);
                    holder.checkBox.setButtonDrawable(drawable);
                } else {
                    holder.checkBox.setChecked(false);
                    holder.checkBox.setButtonDrawable(getResources().getDrawable(R.drawable.sel_nor_wx2));
                }
            }
            holder.mIvHead.fillData(friend);
            holder.mTvName.setText(TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName());
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortFriends.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == sectionIndex) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortFriends.get(position).getFirstLetter().charAt(0);
        }
    }

    class ViewHolder {
        MessageAvatar mIvHead;
        TextView mTvName;
        CheckBox checkBox;
    }

    private class HorListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mSelectPositions.size();
        }

        @Override
        public Object getItem(int position) {
            return mSelectPositions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new HeadView(mContext);
                int size = DisplayUtil.dip2px(mContext, 37);
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(size, size);
                convertView.setLayoutParams(param);
            }
            HeadView imageView = (HeadView) convertView;
            String selectPosition = mSelectPositions.get(position);
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), selectPosition);
            AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), friend, imageView);
            return convertView;
        }
    }
}
