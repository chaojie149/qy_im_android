package com.tongxin.caihong.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.FriendSortRecyclerAdapter;
import com.tongxin.caihong.bean.AttentionUser;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.OnCompleteListener2;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.HideSearchHelper;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.contacts.DeviceActivity;
import com.tongxin.caihong.ui.contacts.NewFriendActivity;
import com.tongxin.caihong.ui.contacts.PublishNumberActivity;
import com.tongxin.caihong.ui.contacts.label.LabelActivityNewUI;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.nearby.UserSearchActivity;
import com.tongxin.caihong.ui.search.SearchAllActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.SearchHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 通讯录
 */
public class FriendFragment extends EasyFragment {
    private static final String TAG = "FriendFragment";
    private TextView mTvTitle;
    private TextView tvFriendCount;
    private ImageView mIvTitleRight;
    private SmartRefreshLayout refreshLayout;
    private SwipeRecyclerView recyclerView;
    private FriendSortRecyclerAdapter mAdapter;
    private SideBar mSideBar;
    private TextView mTextDialog;
    private List<BaseSortModel<Friend>> mSortFriends;
    private List<BaseSortModel<Friend>> mSearchSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private View mHeadView;
    /*
        private EditText mEditText;
        private boolean isSearch;
    */
    private TextView mNotifyCountTv;
    private boolean isSearch;
    /*private RelativeLayout mNewFriendRl;
    private RelativeLayout mGroupRl;
    private RelativeLayout mNoticeRl;*/
    private String mLoginUserId;
    private String mLoginUserName;
    private Handler mHandler = new Handler();
    private LinearLayout mAllView;
    private TextView mLoadView;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
                // update();
                loadData();
            } else if (action.equals(MsgBroadcast.ACTION_MSG_NUM_UPDATE_NEW_FRIEND)) {// 更新消息数量
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
                if (friend != null && friend.getUnReadNum() > 0) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.updateNewFriendMsgNum(friend.getUnReadNum());// 更新底部Tab栏通讯录角标

                    UiUtils.updateNum(mNotifyCountTv, friend.getUnReadNum());
                }
            }
        }
    };

    public FriendFragment() {
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
    }

    /*private boolean mNeedUpdate = true;

    public void update() {
        if (isResumed()) {
            loadData();
        } else {
            mNeedUpdate = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNeedUpdate) {
            loadData();
            mNeedUpdate = false;
        }
    }*/

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_friend;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginUserName = coreManager.getSelf().getNickName();
        if (createView) {
            initView();
            loadData();
        }
        HideSearchHelper.hideSearchHeader(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
        if (friend != null && friend.getUnReadNum() > 0) {
            UiUtils.updateNum(mNotifyCountTv, friend.getUnReadNum());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
    }

    private void initView() {
        mAllView = (LinearLayout) findViewById(R.id.friend_rl);
        mLoadView = (TextView) findViewById(R.id.load_fragment);

        refreshLayout = findViewById(R.id.refreshLayout);
        recyclerView = findViewById(R.id.recyclerView);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        mHeadView = inflater.inflate(R.layout.fragment_contacts, recyclerView, false);

        SearchHeader mEditText = findViewById(R.id.shSearch);
        findViewById(R.id.shSearch).setOnClickListener(v -> SearchAllActivity.start(requireActivity(), "chatHistory"));
        mNotifyCountTv = (TextView) mHeadView.findViewById(R.id.num_tv);
        mHeadView.findViewById(R.id.new_friend_rl).setOnClickListener(this);
        mHeadView.findViewById(R.id.label_rl).setOnClickListener(this);
        mHeadView.findViewById(R.id.notice_rl).setOnClickListener(this);
        mHeadView.findViewById(R.id.device_rl).setOnClickListener(this);
        mHeadView.findViewById(R.id.device_rl).setVisibility(View.GONE);

        recyclerView.addHeaderView(mHeadView);
        View footerView = inflater.inflate(R.layout.footer_friend_fragment, recyclerView, false);
        tvFriendCount = footerView.findViewById(R.id.tvFriendCount);
        recyclerView.addFooterView(footerView);
        mAdapter = new FriendSortRecyclerAdapter(getActivity(), mSortFriends, this::onItemClick);
        recyclerView.setAdapter(mAdapter);
        refreshLayout.setOnRefreshListener(r -> {
            upDataFriend();
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
                    recyclerView.scrollToPosition(position);
                }
            }
        });

        /*
        Add Search Friend
         */
        // Todo 跳转至新页面搜索
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                isSearch = true;
                String mContent = mEditText.getText().toString();
                mSearchSortFriends = new ArrayList<>();
                if (TextUtils.isEmpty(mContent)) {
                    isSearch = false;
                    mAdapter.setData(mSortFriends);
                }
                for (int i = 0; i < mSortFriends.size(); i++) {
                    final Friend friend = mSortFriends.get(i).getBean();
                    String name = friend.getRemarkName();
                    if (TextUtils.isEmpty(name)) {
                        name = friend.getNickName();
                    }
                    if (name.contains(mContent)) {
                        // 符合搜索条件的好友
                        mSearchSortFriends.add((mSortFriends.get(i)));
                    }
                }
                mAdapter.setData(mSearchSortFriends);
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CardcastUiUpdateUtil.ACTION_UPDATE_UI);
        intentFilter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE_NEW_FRIEND);
        getActivity().registerReceiver(mUpdateReceiver, intentFilter);
    }

    private void onItemClick(int position) {
        Friend friend = mSortFriends.get(position).getBean();
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatActivity.FRIEND, friend);
        intent.putExtra("isserch", false);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        if (!UiUtils.isNormalClick(v)) {
            return;
        }
        switch (v.getId()) {
            // Title And Window Click Listener
            case R.id.iv_title_right:
                // 添加朋友
                UserSearchActivity.start(getActivity());
                break;

            // Head Click Listener
            case R.id.new_friend_rl:
                Friend mNewFriend = FriendDao.getInstance().getFriend(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
                if (mNewFriend != null) {
                    mNotifyCountTv.setVisibility(View.GONE);
                    mNewFriend.setUnReadNum(0);

                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.updateNewFriendMsgNum(0);// 更新底部Tab栏通讯录角标
                    }
                }
                Intent intentNewFriend = new Intent(getActivity(), NewFriendActivity.class);
                getActivity().startActivity(intentNewFriend);
                break;
            case R.id.label_rl:
                LabelActivityNewUI.start(requireContext());
                break;
            case R.id.notice_rl:
                Intent intentNotice = new Intent(getActivity(), PublishNumberActivity.class);
                getActivity().startActivity(intentNotice);
                break;
            case R.id.device_rl:
                if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {
                    Intent intentDevice = new Intent(getActivity(), DeviceActivity.class);
                    getActivity().startActivity(intentDevice);
                } else {
                    ToastUtil.showToast(getContext(), R.string.tip_disable_multi_login);
                }
                break;
        }
    }

    private void loadData() {
        if (!DialogHelper.isShowing()) {
            DialogHelper.showDefaulteMessageProgressDialog(getActivity());
        }
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(requireContext(), ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            final List<Friend> friends = FriendDao.getInstance().getAllFriendsWithOutHideChat(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                tvFriendCount.setText(String.valueOf(sortedList.size()));
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
                refreshLayout.finishRefresh();
            });
        });
    }

    /**
     * 从服务端获取好友列表，更新数据库
     */
    private void upDataFriend() {
        refreshLayout.finishRefresh();
        // 使用这个对话框阻止其他操作，以免主线程读写数据库被阻塞anr,
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<AttentionUser>(AttentionUser.class) {
                    @Override
                    public void onResponse(ArrayResult<AttentionUser> result) {
                        if (result.getResultCode() == 1) {
                            AsyncUtils.doAsync(FriendFragment.this, e -> {
                                Reporter.post("保存好友失败，", e);
                                AsyncUtils.runOnUiThread(requireContext(), ctx -> {
                                    DialogHelper.dismissProgressDialog();
                                    ToastUtil.showToast(ctx, R.string.data_exception);
                                });
                            }, c -> {
                                FriendDao.getInstance().addAttentionUsers(coreManager.getSelf().getUserId(), result.getData(),
                                        new OnCompleteListener2() {

                                            @Override
                                            public void onLoading(int progressRate, int sum) {

                                            }

                                            @Override
                                            public void onCompleted() {
                                                c.uiThread(r -> {
                                                    r.loadData();
                                                });
                                            }
                                        });
                            });
                        } else {
                            DialogHelper.dismissProgressDialog();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }
}
