package com.tongxin.caihong.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.adapter.NewFriendAdapter;
import com.tongxin.caihong.bean.AddAttentionResult;
import com.tongxin.caihong.bean.AttentionUser;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.NewFriendDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.FriendHelper;
import com.tongxin.caihong.helper.ShareSdkHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.nearby.UserSearchActivity;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.ShareDialog;
import com.tongxin.caihong.view.VerifyDialog;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.xmpp.listener.NewFriendListener;
import com.roamer.slidelistview.SlideListView;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;

public class NewFriendActivity extends BaseActivity implements NewFriendListener {
    private SmartRefreshLayout mRefreshLayout;
    private SlideListView mPullToRefreshListView;
    private NewFriendAdapter mAdapter;
    private List<NewFriendMessage> mNewFriends;
    private String mLoginUserId;
    private Handler mHandler = new Handler();
    private NewFriendAdapter.NewFriendActionListener mNewFriendActionListener = new NewFriendAdapter.NewFriendActionListener() {

        @Override
        public void addAttention(int position) {
            doAgreeOrAttention(position, 0);
        }

        @Override
        public void removeBalckList(int position) {
            removeBlacklist(position);
        }

        @Override
        public void agree(int position) {
            doAgreeOrAttention(position, 1);
        }

        @Override
        public void feedback(int position) {
            doFeedbackOrSayHello(position, 1);
        }

        @Override
        public void removeLine(int position) {
            NewFriendDao.getInstance().deleteNewFriendById(mLoginUserId, mNewFriends.get(position).getUserId());
            mNewFriends.remove(position);
            mAdapter.notifyDataSetChanged();
        }
    };
    private ShareDialog shareDialog;
    private ShareDialog.OnShareDialogClickListener onShareDialogClickListener = new ShareDialog.OnShareDialogClickListener() {
        @Override
        public void tv1Click() {
            ShareSdkHelper.shareWechat(mContext, getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    coreManager.getConfig().website);
        }

        @Override
        public void tv2Click() {
            ShareSdkHelper.shareWechatMoments(mContext, MyApplication.getContext().getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    MyApplication.getContext().getString(R.string.app_name) + getString(R.string.suffix_share_content),
                    coreManager.getConfig().website);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);
        mLoginUserId = coreManager.getSelf().getUserId();
        mNewFriends = new ArrayList<>();
        initActionBar();
        initView();
        ListenerManager.getInstance().addNewFriendListener(this);

        FriendDao.getInstance().markUserMessageRead(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
        // 将所有消息都变为已读
        NewFriendDao.getInstance().markNewFriendRead(mLoginUserId);

        NewFriendDao.getInstance().resetAllNewFriendUnRead(mLoginUserId);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.new_firend));
        TextView tvTitleRight = (TextView) findViewById(R.id.tv_title_right);
        tvTitleRight.setText(getString(R.string.add_friends));
        tvTitleRight.setVisibility(coreManager.getLimit().cannotSearchFriend() ? View.GONE : View.VISIBLE);
        tvTitleRight.setOnClickListener(v -> UserSearchActivity.start(mContext));
    }

    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
    }

    private void initView() {


        findViewById(R.id.ll1).setOnClickListener(v -> {
            if (!PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
                v.performClick();
            }, null, PermissionUtil.getReadContactsPermissions())) {
                return;
            }
            startActivity(new Intent(mContext, ContactsActivity.class));
        });
        findViewById(R.id.ll2).setOnClickListener(v -> {
            shareDialog = new ShareDialog(mContext, onShareDialogClickListener);
            shareDialog.show();
        });
        //2023.11.06
        findViewById(R.id.ll).setVisibility(View.GONE);
        mPullToRefreshListView = (SlideListView) findViewById(R.id.pull_refresh_list);
        mAdapter = new NewFriendAdapter(this, coreManager.getSelf().getUserId(), mNewFriends, mNewFriendActionListener);
        mPullToRefreshListView.setAdapter(mAdapter);
        mRefreshLayout = findViewById(R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(r -> {
            loadData();
        });

        mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NewFriendMessage mNewFriendMessage = mNewFriends.get(position);
                goTalkHistoryActivity(mNewFriendMessage);
            }
        });
    }

    private void goTalkHistoryActivity(NewFriendMessage mNewFriendMessage) {
        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mNewFriendMessage.getUserId());
        if (friend != null && friend.getStatus() == Friend.STATUS_FRIEND) {
            ChatActivity.start(mContext, friend);
        } else {
            Intent intent = new Intent(this, TalkHistoryActivity.class);
            intent.putExtra(AppConstant.EXTRA_USER_ID, mNewFriendMessage.getUserId());
            intent.putExtra(AppConstant.EXTRA_NICK_NAME, mNewFriendMessage.getNickName());
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * 请求公共消息
     * <p>
     * 是下拉刷新，还是上拉加载
     */
    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                final List<NewFriendMessage> friends = NewFriendDao.getInstance().getAllNewFriendMsg(mLoginUserId);
                // 保证至少200ms的刷新过程
                long delayTime = 200 - (startTime - System.currentTimeMillis());
                if (delayTime < 0) {
                    delayTime = 0;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mNewFriends.clear();
                        if (friends != null && friends.size() > 0) {
                            mNewFriends.addAll(friends);
                        }
                        mAdapter.notifyDataSetChanged();
                        mRefreshLayout.finishRefresh();
                    }
                }, delayTime);
            }
        }).start();

    }

    /**
     * 加关注或者同意别人的加好友
     *
     * @param position 1、同意加好友
     */
    private void doAgreeOrAttention(final int position, final int type) {
        if (!UiUtils.isNormalClick()) { // 有出现两条成为好友消息的情况，只能怀疑是接口可能靠不住了，客户端限制快速双击，
            return;
        }
        final NewFriendMessage friend = mNewFriends.get(position);
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        HttpUtils.get().url(coreManager.getConfig().ADD_FRIENDS)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {
                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            int toastResId = type == 0 ? R.string.add_friend_succ : R.string.agreed;
                            ToastUtil.showToast(NewFriendActivity.this, toastResId);

                            // int messageType = type == 0 ? XmppMessage.TYPE_FRIEND : XmppMessage.TYPE_PASS;
                            int messageType = XmppMessage.TYPE_PASS;
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                                    messageType, null, friend);
                            coreManager.sendNewFriendMessage(friend.getUserId(), message);
                            NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                            FriendHelper.addFriendExtraOperation(mLoginUserId, friend.getUserId());

                            mNewFriends.set(position, message);
                            mAdapter.notifyDataSetChanged();

                            NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_12);
                            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, friend, true);
                            // 刷新通讯录
                            CardcastUiUpdateUtil.broadcastUpdateUi(NewFriendActivity.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(NewFriendActivity.this);
                    }
                });
    }

    /**
     * @param position
     * @param type     0打招呼<br/>
     *                 1回话<br/>
     */
    public void doFeedbackOrSayHello(final int position, final int type) {
        int titleResId = type == 0 ? R.string.say_hello_dialog_title : R.string.feedback;
        String hint;
        if (type == 0) {
            hint = getString(R.string.say_hello_dialog_hint);
        } else {
            hint = getString(R.string.feedback);
        }
        NewFriendMessage friend = mNewFriends.get(position);
        DialogHelper.verify(this, getString(titleResId), friend.getNickName(), hint, "", 0, new VerifyDialog.VerifyClickListener() {
            @Override
            public void cancel() {

            }

            @Override
            public void send(String str) {
                doFeedbackOrSayHello(position, type, str);
            }
        });
    }

    public void doFeedbackOrSayHello(int position, int type, String text) {
        if (TextUtils.isEmpty(text)) {
            ToastUtil.showToast(mContext, getString(R.string.name_connot_null));
            return;
        }
        NewFriendMessage friend = mNewFriends.get(position);
        int messageType = type == 0 ? XmppMessage.TYPE_SAYHELLO : XmppMessage.TYPE_FEEDBACK;
        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(), messageType, text, friend);
        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
        if (friend.getState() == Friend.STATUS_11 || friend.getState() == Friend.STATUS_15) {
            NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_15);
        } else {
            NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_14);
        }
        NewFriendDao.getInstance().updateNewFriendContent(friend.getUserId(), text, message.getTimeSend());

        coreManager.sendNewFriendMessage(friend.getUserId(), message);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TEXT); //文本类型
        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(coreManager.getSelf().getNickName());
        chatMessage.setToUserId(friend.getUserId());
        chatMessage.setContent(text);
        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        chatMessage.setMySend(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        ChatMessageDao.getInstance().saveNewSingleAnswerMessage(mLoginUserId, friend.getUserId(), chatMessage);
/*
        ListenerManager.getInstance().notifyNewFriend(mLoginUserId, friend, true);
*/
        ToastUtil.showToast(this, R.string.feedback_succ);
        loadData();
        //发送回话的消息
        mAdapter.notifyDataSetChanged();
/*
        // 通知主界面更新UI
        EventBus.getDefault().post(new MessageEventHongdian(123));
*/
    }

    private void removeBlacklist(final int position) {
        final NewFriendMessage friend = mNewFriends.get(position);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<AttentionUser>(AttentionUser.class) {

                    @Override
                    public void onResponse(ObjectResult<AttentionUser> result) {
                        DialogHelper.dismissProgressDialog();
                        int currentStatus = Friend.STATUS_UNKNOW;
                        if (result.getData() != null) {
                            currentStatus = result.getData().getStatus();
                        }
                        FriendDao.getInstance().updateFriendStatus(friend.getOwnerId(), friend.getUserId(), currentStatus);

                        NewFriendMessage message = null;
                        switch (currentStatus) {
                            case Friend.STATUS_ATTENTION:
                                message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(), XmppMessage.TYPE_NEWSEE,
                                        null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);
                                break;
                            case Friend.STATUS_FRIEND:
                                message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(), XmppMessage.TYPE_FRIEND,
                                        null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);
                                FriendHelper.addFriendExtraOperation(friend.getOwnerId(), friend.getUserId());
/*
                                EventBus.getDefault().post(new MessageEventHongdian(123));
*/
                                break;
                            default:// 其他，理论上不可能
                                break;
                        }
                        ToastUtil.showToast(NewFriendActivity.this, R.string.remove_blacklist_succ);
                        mNewFriends.set(position, message);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(NewFriendActivity.this);
                    }
                });
    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        loadData();
        return true;
    }
}
