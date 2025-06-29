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

import androidx.core.view.ViewCompat;

import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.FriendSortAdapter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.TrillStatisticsHelper;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.BroadcastHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.SearchHeader;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * 转发 选择 好友
 */
public class SelectNewContactsActivity extends BaseActivity implements OnClickListener {
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
        EventBusHelper.register(this);
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
        tvTitle.setText(getString(R.string.select_contacts));
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
        View headView = View.inflate(this, R.layout.item_headview_creategroup_chat, null);
        mPullToRefreshListView.getRefreshableView().addHeaderView(headView);
        headView.setOnClickListener(this);
        mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
        mAdapter = new FriendSortAdapter(this, mSortFriends);
        mAdapter.showCheckBox();
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
        menuWindow = new InstantMessageConfirmNew(SelectNewContactsActivity.this, new ClickListener(friends), friends);
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
            final List<Friend> friends = FriendDao.getInstance().getAllFriends(mLoginUserId);
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

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, EventNotifyByTag.FinishInstantRelatedActivity)) {
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_headview_instant_group:
                Intent intent = new Intent(SelectNewContactsActivity.this, SelectNewGroupInstantActivity.class);
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

    /**
     * 事件的监听
     */
    class ClickListener implements OnClickListener {
        private List<Friend> friends;

        public ClickListener(List<Friend> friends) {
            this.friends = friends;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_send:
                    for (int i = 0; i < friends.size(); i++) {
                        Friend friend = friends.get(i);
                        if (isMoreSelected) {
                            // 多选转发 通知多选页面(即多选消息的单聊 || 群聊页面，在该页面获取选中的消息在发送出去)
                            EventBus.getDefault().post(new EventMoreSelected(friend.getUserId(), isSingleOrMerge, false, friend.getIsDevice() != 0));
                            if (i == friends.size() - 1) {
                                // 结束上级界面
                                EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.FinishInstantRelatedActivity));
                                // 单聊不像群组需要调用isSupportSend方法，所以判断是否是最后一个不需要搞那么多虚头巴脑的东西
                                // 发送多选收藏的event，但此event仅做关闭聊天界面多选状态用
                                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedCollection", false, true));
                                finish();
                            }
                        } else {
                            ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, messageId);
                            if (chatMessage.getType() == XmppMessage.TYPE_TEXT
                                    && !TextUtils.isEmpty(chatMessage.getObjectId())) {
                                // @消息，转发出去时变为纯文本，不具备@功能
                                chatMessage.setObjectId("");
                            }
                            chatMessage.setFromId(null);
                            chatMessage.setToId(null);
                            TrillStatisticsHelper.share(mContext, coreManager, chatMessage);
                            chatMessage.setFromUserId(mLoginUserId);
                            chatMessage.setFromUserName(coreManager.getSelf().getNickName());
                            chatMessage.setToUserId(friend.getUserId());
                            chatMessage.setUpload(true);
                            chatMessage.setMySend(true);
                            chatMessage.setReSendCount(5);
                            chatMessage.setSendRead(false);
                            chatMessage.setReadPersons(0);
                            // 因为该消息的原主人可能开启了消息传输加密，我们对于content字段解密后存入了数据库，但是isEncrypt字段并未改变
                            // 如果我们将此消息转发给另一人，对方可能会对我方已解密的消息再次进行解密
                            chatMessage.setIsEncrypt(0);
                            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, friend.getUserId(), chatMessage)) {
                                ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, friend.getUserId(), chatMessage, false);
                            }
                            coreManager.sendChatMessage(friend.getUserId(), chatMessage);

                            if (i == friends.size() - 1) {
                                // 结束上级界面
                                EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.FinishInstantRelatedActivity));
                                MsgBroadcast.broadcastMsgUiUpdate(mContext);
                                if (!isOnlyFinishNotIntent && friends.size() == 1) {
                                    if (friend.getRoomFlag() == 1) {
                                        MucChatActivity.start(mContext, friend);
                                    } else {
                                        ChatActivity.start(mContext, friend);
                                    }
                                }
                                finish();
                            }
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
