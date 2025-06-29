package com.tongxin.caihong.ui.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.FriendSortAdapter;
import com.tongxin.caihong.bean.AttentionUser;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.ClearEditText;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 黑名单列表
 */
public class BlackActivity extends BaseActivity {
    public boolean isSearch;
    private ListView mPullToRefreshListView;
    private FriendSortAdapter mAdapter;
    private List<BaseSortModel<Friend>> mSortFriends;
    private List<BaseSortModel<Friend>> mSearchFriends;
    private BaseComparator<Friend> mBaseComparator;
    private SideBar mSideBar;
    private TextView mTextDialog;
    private String mLoginUserId;
    private ClearEditText etSearch;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
                loadData();
            }
        }
    };

    public BlackActivity() {
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        setTouch(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView mTvTitle = findViewById(R.id.tv_title_center);
        mTvTitle.setText(R.string.black_list);
    }

    private void initView() {
        mPullToRefreshListView = findViewById(R.id.pull_refresh_list);
        mPullToRefreshListView.setOnItemClickListener((parent, view1, position, id) -> {
            Friend friend;
            if (isSearch) {
                friend = mSearchFriends.get(position).getBean();
            } else {
                friend = mSortFriends.get(position).getBean();
            }
            if (friend != null) {
                Intent intent = new Intent(BlackActivity.this, BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                startActivity(intent);
            }
        });

        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mPullToRefreshListView.setSelection(position);
                }
            }
        });

        mAdapter = new FriendSortAdapter(this, mSortFriends);
        mPullToRefreshListView.setAdapter(mAdapter);
        getBlackList();

        registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());

        etSearch = findViewById(R.id.search_edit);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mSortFriends != null) {
                    String text = etSearch.getText().toString().toLowerCase();
                    if (TextUtils.isEmpty(text)) {
                        isSearch = false;
                        loadData();
                    } else {
                        isSearch = true;
                        List<BaseSortModel<Friend>> data = new ArrayList<>();
                        for (BaseSortModel<Friend> sortFriend : mSortFriends) {
                            if (sortFriend.getBean().getShowName().toLowerCase().contains(text)) {
                                data.add(sortFriend);
                            }
                        }
                        mSearchFriends = data;
                        mAdapter.setData(data);
                        if (data.size() > 0) {
                            findViewById(R.id.fl_empty).setVisibility(View.GONE);
                        } else {
                            findViewById(R.id.fl_empty).setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }

    private void loadData() {
        // DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            final List<Friend> friends = FriendDao.getInstance().getAllBlacklists(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                // DialogHelper.dismissProgressDialog();
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
                if (friends.size() == 0) {
                    findViewById(R.id.fl_empty).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.fl_empty).setVisibility(View.GONE);
                }

                if (isSearch) {
                    etSearch.setText(etSearch.getText().toString());
                }
            });
        });
    }

    /**
     * 获取黑名单列表
     */
    private void getBlackList() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACK_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<AttentionUser>(AttentionUser.class) {
                    @Override
                    public void onResponse(ArrayResult<AttentionUser> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            List<AttentionUser> attentionUsers = result.getData();
                            if (attentionUsers != null && attentionUsers.size() > 0) {
                                for (int i = 0; i < attentionUsers.size(); i++) {
                                    AttentionUser attentionUser = attentionUsers.get(i);
                                    if (attentionUser == null) {
                                        continue;
                                    }
                                    String userId = attentionUser.getToUserId();// 好友的Id
                                    Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userId);
                                    if (friend == null) {
                                        friend = new Friend();
                                        friend.setOwnerId(attentionUser.getUserId());
                                        friend.setUserId(attentionUser.getToUserId());
                                        friend.setAccount(attentionUser.getAccount());
                                        friend.setNickName(attentionUser.getToNickName());
                                        friend.setRemarkName(attentionUser.getRemarkName());
                                        friend.setTimeCreate(attentionUser.getCreateTime());
                                        friend.setStatus(Friend.STATUS_BLACKLIST);

                                        friend.setOfflineNoPushMsg(attentionUser.getOfflineNoPushMsg());
                                        friend.setTopTime(attentionUser.getOpenTopChatTime());
                                        PreferenceUtils.putInt(MyApplication.getContext(), Constants.MESSAGE_READ_FIRE + attentionUser.getUserId() + CoreManager.requireSelf(MyApplication.getContext()).getUserId(),
                                                attentionUser.getIsOpenSnapchat());
                                        friend.setChatRecordTimeOut(attentionUser.getChatRecordTimeOut());// 消息保存天数 -1/0 永久

                                        friend.setCompanyId(attentionUser.getCompanyId());
                                        friend.setRoomFlag(0);
                                        FriendDao.getInstance().createOrUpdateFriend(friend);
                                    } else {
                                        FriendDao.getInstance().updateFriendStatus(mLoginUserId, userId, Friend.STATUS_BLACKLIST);
                                    }
                                }
                            }
                            loadData();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

}
