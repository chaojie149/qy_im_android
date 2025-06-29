package com.tongxin.caihong.ui.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.NewFriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.FriendHelper;
import com.tongxin.caihong.pay.chat.PayActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.nearby.PublicNumberSearchActivity;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.SearchHeader;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.roamer.slidelistview.SlideListView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class PublishNumberActivity extends BaseActivity {

    private SearchHeader etSearch;
    private SlideListView mNoticeAccountList;
    private NoticeAdapter mNoticeAdapter;
    private List<Friend> mNoticeFriendList;
    private List<Friend> mSearchNoticeFriendList;
    private long mOldTime;
    private long mDelayMilliseconds = 1000;

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
                mNoticeFriendList = FriendDao.getInstance().getAllSystems(coreManager.getSelf().getUserId());
                if (mNoticeFriendList == null) {
                    mNoticeFriendList = new ArrayList<>();
                }
                // 关闭支付功能，移除支付公众号
                if (!coreManager.getConfig().enablePayModule) {
                    for (int i = 0; i < mNoticeFriendList.size(); i++) {
                        if (mNoticeFriendList.get(i).getUserId().equals(Friend.ID_SK_PAY)) {
                            mNoticeFriendList.remove(i);
                            break;
                        }
                    }
                }
                if (mNoticeAdapter != null) {
                    refreshList();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);
        initActionBar();
        initView();
        setTouch(true);
        registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.public_number);
        if (coreManager.getConfig().enableMpModule) {
            ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
            ivRight.setImageResource(R.mipmap.icon_public_number_add);
            ivRight.setOnClickListener(v -> {
                PublicNumberSearchActivity.start(this);
            });
        }
    }

    private void initView() {
        mNoticeFriendList = FriendDao.getInstance().getAllSystems(coreManager.getSelf().getUserId());
        if (mNoticeFriendList == null) {
            mNoticeFriendList = new ArrayList<>();
        }
        // 关闭支付功能，移除支付公众号
        if (!coreManager.getConfig().enablePayModule) {
            for (int i = 0; i < mNoticeFriendList.size(); i++) {
                if (mNoticeFriendList.get(i).getUserId().equals(Friend.ID_SK_PAY)) {
                    mNoticeFriendList.remove(i);
                    break;
                }
            }
        }
        mNoticeAccountList = findViewById(R.id.notice_account_lv);
        mNoticeAdapter = new NoticeAdapter(this);
        mNoticeAdapter.setData(mNoticeFriendList);
        mNoticeAccountList.setAdapter(mNoticeAdapter);
        mNoticeAccountList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long nowTime = SystemClock.elapsedRealtime();
                long intervalTime = nowTime - mOldTime;
                if (mOldTime == 0 || intervalTime >= mDelayMilliseconds) {
                    mOldTime = nowTime;

                    Friend mFriend = mNoticeAdapter.getData().get(position);
                    if (mFriend != null) {
                        if (mFriend.getUserId().equals(Friend.ID_SK_PAY)) {
                            startActivity(new Intent(mContext, PayActivity.class));
                        } else {
                            Intent intent = new Intent(mContext, ChatActivity.class);
                            intent.putExtra(ChatActivity.FRIEND, mFriend);
                            startActivity(intent);
                        }
                    }
                }
            }
        });

        initSearch();
    }

    private void initSearch() {
        etSearch = findViewById(R.id.shSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    mSearchNoticeFriendList = null;
                    mNoticeAdapter.setData(mNoticeFriendList);
                } else {
                    makeSearchList(s.toString());
                    mNoticeAdapter.setData(mSearchNoticeFriendList);
                }
            }
        });
    }

    // 删除公众号，
    private void showDeleteAllDialog(final int position) {
        Friend friend = mNoticeAdapter.getData().get(position);
        if (friend.getStatus() == Friend.STATUS_UNKNOW) {// 陌生人
            return;
        }
        if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)
                || friend.getUserId().equals(Friend.ID_SK_PAY)) {// 10000 与1100 号不能删除，
            Toast.makeText(mContext, getString(R.string.tip_not_allow_delete), Toast.LENGTH_SHORT).show();
            return;
        }
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.delete_public_number), getString(R.string.ask_delete_public_number), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                deleteFriend(position, 1);
            }
        });
        mSF.show();
    }

    private void deleteFriend(final int position, final int type) {
        Friend friend = mNoticeAdapter.getData().get(position);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_DELALL, null, friend);
                            coreManager.sendNewFriendMessage(coreManager.getSelf().getUserId(), message); // 删除好友
                            FriendHelper.removeAttentionOrFriend(coreManager.getSelf().getUserId(), message.getUserId());

                            ChatMessage deleteChatMessage = new ChatMessage();
                            deleteChatMessage.setContent(getString(R.string.has_delete_public_number_place_holder, coreManager.getSelf().getNickName()));
                            deleteChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                            FriendDao.getInstance().updateLastChatMessage(coreManager.getSelf().getUserId(), Friend.ID_NEW_FRIEND_MESSAGE, deleteChatMessage);

                            message.setContent(getString(R.string.delete_firend_public) + friend.getNickName());
                            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                            NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_16);
                            ListenerManager.getInstance().notifyNewFriend(coreManager.getSelf().getUserId(), message, true);

                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                            mNoticeFriendList.remove(friend);
                            refreshList();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(getApplicationContext());
                    }
                });
    }

    private void makeSearchList(String key) {
        if (mNoticeFriendList == null || mNoticeFriendList.isEmpty()) {
            return;
        }
        mSearchNoticeFriendList = new ArrayList<>();
        for (Friend friend : mNoticeFriendList) {
            String name = friend.getShowName();
            if (!TextUtils.isEmpty(name) && name.contains(key)) {
                mSearchNoticeFriendList.add(friend);
            }
        }
    }

    private void refreshList() {
        if (mSearchNoticeFriendList != null) {
            makeSearchList(etSearch.getText().toString());
            mNoticeAdapter.setData(mSearchNoticeFriendList);
        } else {
            mNoticeAdapter.setData(mNoticeFriendList);
        }
    }

    class NoticeAdapter extends SlideBaseAdapter {
        private List<Friend> data;

        NoticeAdapter(Context context) {
            super(context);
        }

        public List<Friend> getData() {
            return data;
        }

        public void setData(List<Friend> data) {
            this.data = data;
            notifyDataSetInvalidated();
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.item_notice_account;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.item_notice_right;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Friend getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            ImageView ivHead = ViewHolder.get(convertView, R.id.ivHead);
            TextView tvName = ViewHolder.get(convertView, R.id.tvName);
            TextView tvDisc = ViewHolder.get(convertView, R.id.tvDisc);
            TextView tvTime = ViewHolder.get(convertView, R.id.tvTime);

            Friend friend = getItem(position);
            if (friend != null) {
                AvatarHelper.getInstance().displayAvatar(friend.getUserId(), ivHead);
                tvName.setText(friend.getShowName());
                if (TextUtils.isEmpty(friend.getDescription())) {
                    tvDisc.setVisibility(View.GONE);
                } else {
                    tvDisc.setVisibility(View.VISIBLE);
                    tvDisc.setText(friend.getDescription());
                }
                tvTime.setText(TimeUtils.getFriendlyTimeDesc(tvTime.getContext(), friend.getTimeSend()));
            }

            TextView delete_tv = ViewHolder.get(convertView, R.id.delete_tv);

            delete_tv.setOnClickListener(v -> {
                showDeleteAllDialog(position);
            });
            return convertView;
        }

    }
}
