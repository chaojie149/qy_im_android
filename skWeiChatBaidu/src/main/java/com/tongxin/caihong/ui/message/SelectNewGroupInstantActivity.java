package com.tongxin.caihong.ui.message;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.FriendSortAdapter;
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
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.TrillStatisticsHelper;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.BroadcastHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.SearchHeader;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
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
import okhttp3.Call;

/**
 * 转发 选择 群组
 */
public class SelectNewGroupInstantActivity extends BaseActivity {
    private PullToRefreshListView mPullToRefreshListView;
    private FriendSortAdapter mAdapter;
    private TextView mTextDialog;
    private SideBar mSideBar;
    private List<BaseSortModel<Friend>> mSortFriends;
    private String mLoginUserId;
    // 是否为多选转发
    private boolean isMoreSelected;
    // 逐条还是合并转发
    private boolean isSingleOrMerge;
    // 通过toUserId与messageId从数据库查询到需要转发的消息，进行转发
    // 如为多选转发则不需要，直接通过EventBus通知到聊天界面转发
    private String toUserId;
    private String messageId;
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
    private InstantMessageConfirmNew menuWindow;
    private RoomMember mRoomMember;
    // 转发数量，当前转发序号
    private int sendSize;
    private int currentSendSerialNumber;
    private List<Friend> selectedFriendList;
    private boolean isOnlyFinishNotIntent;
    private String searchKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newchat_person_selected);
        isMoreSelected = getIntent().getBooleanExtra(Constants.IS_MORE_SELECTED_INSTANT, false);
        isSingleOrMerge = getIntent().getBooleanExtra(Constants.IS_SINGLE_OR_MERGE, false);
        // 在ChatContentView内长按转发才需要以下参数
        toUserId = getIntent().getStringExtra("fromUserId");
        messageId = getIntent().getStringExtra("messageId");
        isOnlyFinishNotIntent = getIntent().getBooleanExtra("isOnlyFinishNotIntent", false);

        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mLoginUserId = coreManager.getSelf().getUserId();

        initActionBar();
        initView();
        initSearch();
        loadData();
        BroadcastHelper.register(this, receiver, OtherBroadcast.MSG_BACK);
    }

    private void initSearch() {
        SearchHeader shSearch = findViewById(R.id.shSearch);
        shSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchKey = s.toString().trim().toLowerCase();
                if (TextUtils.isEmpty(searchKey)) {
                    mAdapter.setData(mSortFriends);
                } else {
                    mAdapter.setData(search(mSortFriends, searchKey));
                }
            }
        });
    }

    private List<BaseSortModel<Friend>> search(List<BaseSortModel<Friend>> data, String searchKey) {
        List<BaseSortModel<Friend>> ret = new ArrayList<>();
        for (BaseSortModel<Friend> item : data) {
            if (item.bean.getShowName().toLowerCase().contains(searchKey)) {
                ret.add(item);
            }
        }
        return ret;
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
        tvTitle.setText(getString(R.string.select_group_chat_instant));
        TextView tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setTextColor(getResources().getColor(R.color.white));
        tvRight.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tvRight, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tvRight.setText(R.string.finish);
        tvRight.setOnClickListener(v -> {
            List<Friend> friends = new ArrayList<>();
            for (int i = 0; i < mSortFriends.size(); i++) {
                if (mSortFriends.get(i).getBean().isCheck()) {
                    friends.add(mSortFriends.get(i).getBean());
                }
            }
            if (friends.size() > 0) {
                showPopuWindow(v, friends);
            }
        });
    }

    private void initView() {
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        mAdapter = new FriendSortAdapter(SelectNewGroupInstantActivity.this, mSortFriends);
        mAdapter.showCheckBox();
        mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
        mPullToRefreshListView.setOnRefreshListener(refreshView -> loadData());

        mPullToRefreshListView.setOnItemClickListener((parent, view, position, id) -> {
            Friend friend = mAdapter.getData().get((int) id).getBean();
            friend.setCheck(!friend.isCheck());
            mAdapter.notifyDataSetChanged();
        });

        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mSideBar.setTextView(mTextDialog);

        mSideBar.setOnTouchingLetterChangedListener(s -> {
            // 该字母首次出现的位置
            int position = mAdapter.getPositionForSection(s.charAt(0));
            if (position != -1) {
                mPullToRefreshListView.getRefreshableView().setSelection(position);
            }
        });
    }

    private void showPopuWindow(View view, List<Friend> friends) {
        if (menuWindow != null) {
            menuWindow.dismiss();
        }
        this.selectedFriendList = friends;
        menuWindow = new InstantMessageConfirmNew(SelectNewGroupInstantActivity.this, new SelectNewGroupInstantActivity.ClickListener(friends), friends);
        menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    private void loadData() {
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            long startTime = System.currentTimeMillis();
            final List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);

            long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
            if (delayTime < 0) {
                delayTime = 0;
            }
            c.postDelayed(r -> {
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                if (TextUtils.isEmpty(searchKey)) {
                    mAdapter.setData(mSortFriends);
                } else {
                    mAdapter.setData(search(mSortFriends, searchKey));
                }
                mPullToRefreshListView.onRefreshComplete();
            }, delayTime);
        });
    }

    private void forwardingStep(Friend friend) {
        if (isMoreSelected) {// 多选转发 通知多选页面(即多选消息的单聊 || 群聊页面，在该页面获取选中的消息在发送出去)
            EventBus.getDefault().post(new EventMoreSelected(friend.getUserId(), isSingleOrMerge, true, friend.getIsDevice() != 0));
        } else {
            instantChatMessage(friend, toUserId, messageId);
        }
        finishThis();
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
            if (chatMessage.getType() == XmppMessage.TYPE_TEXT
                    && !TextUtils.isEmpty(chatMessage.getObjectId())) {
                // @消息，转发出去时变为纯文本，不具备@功能
                chatMessage.setObjectId("");
            }
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
            // 因为该消息的原主人可能开启了消息传输加密，我们对于content字段解密后存入了数据库，但是isEncrypt字段并未改变
            // 如果我们将此消息转发给另一人，对方可能会对我方已解密的消息再次进行解密
            chatMessage.setIsEncrypt(0);
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage)) {
                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, mFriend.getUserId(), chatMessage, true);
            }
            send(mFriend.getUserId(), chatMessage);
        }
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
            // 结束上级界面
            EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.FinishInstantRelatedActivity));
            if (isMoreSelected) {
                // 发送多选收藏的event，但此event仅做关闭聊天界面多选状态用
                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedCollection", false, true));
            } else {
                MsgBroadcast.broadcastMsgUiUpdate(mContext);
            }
            if (!isOnlyFinishNotIntent && selectedFriendList.size() == 1) {
                Friend friend = selectedFriendList.get(0);
                if (friend.getRoomFlag() == 1) {
                    MucChatActivity.start(mContext, friend);
                } else {
                    ChatActivity.start(mContext, friend);
                }
            }
            finish();
        }
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
                                         DialogHelper.tip(SelectNewGroupInstantActivity.this, getString(R.string.tip_forward_kick));
                                         faultTolerance();
                                     } else {// 正常状态
                                         if (mucRoom.getS() == -1) {// 该群组已被锁定
                                             FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 3);// 更新本地群组状态
                                             DialogHelper.tip(SelectNewGroupInstantActivity.this, getString(R.string.tip_group_disable_by_service));
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
                                                 DialogHelper.tip(SelectNewGroupInstantActivity.this, getString(R.string.tip_now_ban_all));
                                                 faultTolerance();
                                             } else if ((mucRoom.getMember().getTalkTime() > System.currentTimeMillis() / 1000)
                                                     || (mucRoom.getMember().getTalkTime() == -1)) {// 禁言
                                                 DialogHelper.tip(SelectNewGroupInstantActivity.this, getString(R.string.tip_forward_ban));
                                                 faultTolerance();
                                             } else {
                                                 currentSendSerialNumber++;
                                                 forwardingStep(friend);
                                             }
                                         }
                                     }
                                 } else if (Result.checkError(result, Result.CODE_ROOM_GONE)) {
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, friend.getUserId(), 2);// 更新本地群组状态
                                     DialogHelper.tip(SelectNewGroupInstantActivity.this, getString(R.string.tip_forward_disbanded));
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
                        if (friends.get(i).getIsLostChatKeyGroup() == 1) {
                            ToastUtil.showToast(mContext, getString(R.string.is_lost_key_cannot_support_send_msg, friends.get(i).getNickName()));
                            faultTolerance();
                        } else {
                            isSupportSend(friends.get(i));
                        }
                    }
                    break;
                case R.id.btn_cancle:// 取消
                    break;
                default:
                    break;
            }
        }
    }
}
