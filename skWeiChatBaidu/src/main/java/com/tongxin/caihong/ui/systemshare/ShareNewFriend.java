package com.tongxin.caihong.ui.systemshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.FriendSortAdapter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.UploadEngine;
import com.tongxin.caihong.socket.EMConnectionManager;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.InstantMessageConfirm;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.LoadFrame;
import com.tongxin.caihong.view.SearchHeader;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 分享 选择 好友
 */
public class ShareNewFriend extends BaseActivity implements OnClickListener, ChatMessageListener {
    private PullToRefreshListView mPullToRefreshListView;
    private FriendSortAdapter mAdapter;
    private TextView mTextDialog;
    private SideBar mSideBar;
    private List<BaseSortModel<Friend>> mSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private String mLoginUserId;

    private InstantMessageConfirm menuWindow;
    private LoadFrame mLoadFrame;

    private ChatMessage mShareChatMessage;
    private BroadcastReceiver mShareBroadCast = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.isEmpty(intent.getAction())
                    && intent.getAction().equals(ShareBroadCast.ACTION_FINISH_ACTIVITY)) {
                finish();
            }
        }
    };
    private String searchKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newchat_person_selected);
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
        mLoginUserId = coreManager.getSelf().getUserId();

        mShareChatMessage = new ChatMessage();
        if (ShareUtil.shareInit(this, mShareChatMessage)) return;

        initActionBar();
        initView();
        initSearch();
        loadData();

        ListenerManager.getInstance().addChatMessageListener(this);
        registerReceiver(mShareBroadCast, new IntentFilter(ShareBroadCast.ACTION_FINISH_ACTIVITY));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeChatMessageListener(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.select_contacts));
    }

    private void initView() {
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        View headView = View.inflate(this, R.layout.item_headview_creategroup_chat, null);
        mPullToRefreshListView.getRefreshableView().addHeaderView(headView);
        headView.setOnClickListener(this);
        mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
        mAdapter = new FriendSortAdapter(this, mSortFriends);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                loadData();
            }
        });

        mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Friend friend = mAdapter.getData().get((int) id).getBean();
                showPopuWindow(view, friend);
            }
        });

        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mSideBar.setTextView(mTextDialog);

        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mPullToRefreshListView.getRefreshableView().setSelection(position);
                }
            }
        });
    }

    private void showPopuWindow(View view, Friend friend) {
        if (menuWindow != null) {
            menuWindow.dismiss();
        }
        menuWindow = new InstantMessageConfirm(this, new ClickListener(friend), friend);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_headview_instant_group:
                Intent intent = getIntent();
                intent.setClass(this, ShareNewGroup.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {
        if (TextUtils.isEmpty(msgId)) {
            return;
        }
        // 更新消息Fragment的广播
        MsgBroadcast.broadcastMsgUiUpdate(mContext);
        if (mShareChatMessage != null && TextUtils.equals(mShareChatMessage.getPacketId(), msgId)) {
            if (messageState == ChatMessageListener.MESSAGE_SEND_SUCCESS) {// 发送成功
                if (mLoadFrame != null) {
                    mLoadFrame.change();
                }
            }
        }
    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        return false;
    }

    private void sendMessage(Friend friend) {
        if (friend.getRoomFlag() == 1) {
            coreManager.sendMucChatMessage(friend.getUserId(), mShareChatMessage);
        } else {
            if (friend.getIsDevice() == 1) {
                mShareChatMessage.setFromId(EMConnectionManager.CURRENT_DEVICE);
                mShareChatMessage.setToUserName(friend.getUserId());
                mShareChatMessage.setToUserId(coreManager.getSelf().getUserId());
            }
            coreManager.sendChatMessage(mShareChatMessage.getToUserId(), mShareChatMessage);
        }
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), friend.getUserId(), mShareChatMessage)) {
            ListenerManager.getInstance().notifyNewMesssage(coreManager.getSelf().getUserId(), friend.getUserId(), mShareChatMessage, friend.getRoomFlag() == 1);
        }
    }

    /**
     * 事件的监听
     */
    class ClickListener implements OnClickListener {
        private Friend friend;

        public ClickListener(Friend friend) {
            this.friend = friend;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_send:
                    mLoadFrame = new LoadFrame(ShareNewFriend.this);
                    mLoadFrame.setSomething(getString(R.string.back_last_page), getString(R.string.open_im, getString(R.string.app_name)), new LoadFrame.OnLoadFrameClickListener() {
                        @Override
                        public void cancelClick() {
                            ShareBroadCast.broadcastFinishActivity(ShareNewFriend.this);
                        }

                        @Override
                        public void confirmClick() {
                            ShareBroadCast.broadcastFinishActivity(ShareNewFriend.this);
                            startActivity(new Intent(ShareNewFriend.this, MainActivity.class));
                            finish();
                        }
                    });
                    mLoadFrame.show();

                    mShareChatMessage.setFromUserId(coreManager.getSelf().getUserId());
                    mShareChatMessage.setFromUserName(coreManager.getSelf().getNickName());
                    mShareChatMessage.setToUserId(friend.getUserId());
                    mShareChatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                    mShareChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                    int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + friend.getUserId() + coreManager.getSelf().getUserId(), 0);
                    if (mShareChatMessage.getType() == XmppMessage.TYPE_TEXT
                            || mShareChatMessage.getType() == XmppMessage.TYPE_VOICE
                            || mShareChatMessage.getType() == XmppMessage.TYPE_IMAGE
                            || mShareChatMessage.getType() == XmppMessage.TYPE_VIDEO) {
                        mShareChatMessage.setIsReadDel(isReadDel);
                    }
                    switch (mShareChatMessage.getType()) {
                        case XmppMessage.TYPE_TEXT:
                            sendMessage(friend);
                            break;
                        case XmppMessage.TYPE_IMAGE:
                        case XmppMessage.TYPE_VIDEO:
                        case XmppMessage.TYPE_FILE:
                            if (!mShareChatMessage.isUpload()) {// 未上传
                                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), friend.getUserId(), mShareChatMessage, new UploadEngine.ImFileUploadResponse() {
                                    @Override
                                    public void onSuccess(String toUserId, ChatMessage message) {
                                        sendMessage(friend);
                                    }

                                    @Override
                                    public void onFailure(String toUserId, ChatMessage message) {
                                        mLoadFrame.dismiss();
                                        ToastUtil.showToast(ShareNewFriend.this, getString(R.string.upload_failed));
                                    }
                                }, false);
                            } else {// 已上传 自定义表情默认为已上传
                                sendMessage(friend);
                            }
                            break;
                        default:
                            Reporter.unreachable();
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
