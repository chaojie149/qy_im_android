package com.tongxin.caihong.ui.groupchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.FriendSortRecyclerAdapter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 我的群组
 */
public class RoomFragment extends EasyFragment {
    private static final String TAG = "RoomFragment";
    private SwipeRecyclerView mPullToRefreshListView;
    private FriendSortRecyclerAdapter mAdapter;
    private List<BaseSortModel<Friend>> mSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private SideBar mSideBar;
    private TextView mTextDialog;

    private String mLoginUserId;
    private boolean mNeedUpdate = true;
    private String searchKey;
    private Future<?> loadDataFuture;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    /**
     * 0.我创建的群
     * 1.我管理的群
     * 2.我加入的群
     */
    private int index;
    private LinearLayout llGroupFooter;
    private TextView tvFriendCount;
    private RelativeLayout rlDefaultGroupList;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MucgroupUpdateUtil.ACTION_UPDATE)
                    || intent.getAction().equals(MucgroupUpdateUtil.ACTION_ROLE_UPDATE)) {
                update();
            }
        }
    };

    public RoomFragment() {
        mSortFriends = new ArrayList<BaseSortModel<Friend>>();
        mBaseComparator = new BaseComparator<Friend>();
    }

    public static RoomFragment create(int index) {
        RoomFragment f = new RoomFragment();
        Bundle b = new Bundle();
        b.putInt("index", index);
        f.setArguments(b);
        return f;
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_room;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (getArguments() != null) {
            index = getArguments().getInt("index", 2);
        } else {
            index = 2;
        }
        if (getParentFragment() instanceof KxGroupFragment) {
            searchKey = ((KxGroupFragment) getParentFragment()).getSearchKey();
        }
        mLoginUserId = coreManager.getSelf().getUserId();
        if (createView) {
            initView();
        }
    }

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
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mUpdateReceiver);
        super.onDestroy();
    }

    private void initView() {
        mPullToRefreshListView = findViewById(R.id.pull_refresh_list);
        View footerView = LayoutInflater.from(requireContext()).inflate(R.layout.footer_room_fragment, mPullToRefreshListView, false);
        llGroupFooter = footerView.findViewById(R.id.llGroupFooter);
        tvFriendCount = footerView.findViewById(R.id.tvFriendCount);
        rlDefaultGroupList = footerView.findViewById(R.id.rlDefaultGroupList);
        mPullToRefreshListView.addFooterView(footerView);

        mAdapter = new FriendSortRecyclerAdapter(getActivity(), mSortFriends, position -> {
            Friend friend = mSortFriends.get(position).getBean();
            Intent intent = new Intent(getActivity(), MucChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
            intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
            intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
            startActivity(intent);
            if (friend.getUnReadNum() > 0) {// 如该群组未读消息数量大于1, 刷新MessageFragment
                MsgBroadcast.broadcastMsgNumReset(getActivity());
                MsgBroadcast.broadcastMsgUiUpdate(getActivity());
            }
        });
        mPullToRefreshListView.setAdapter(mAdapter);

        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);

        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mPullToRefreshListView.scrollToPosition(position);
                }
            }
        });
        getActivity().registerReceiver(mUpdateReceiver, MucgroupUpdateUtil.getUpdateActionFilter());
    }

    public void onSearchKeyChanged(Editable s) {
        searchKey = s.toString();
        loadData();
    }

    public void loadData() {
        Log.d(TAG, "loadData() called");
        if (loadDataFuture != null && !loadDataFuture.isCancelled() && !loadDataFuture.isDone()) {
            Log.i(TAG, "loadData: cancel " + Integer.toHexString(loadDataFuture.hashCode()));
            loadDataFuture.cancel(true);
        }
        loadDataFuture = AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(requireContext(), ctx -> {
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, executorService, c -> {
            Log.i(TAG, "loadData: start " + Thread.currentThread().getName());
            long startTime = System.currentTimeMillis();
            final List<Friend> friends = new ArrayList<>();
            FriendDao.getInstance().getAllRoomsWithOutHideChat(mLoginUserId, iterator -> {
                Log.d(TAG, "loadData: iterator " + Thread.currentThread().getName() + ", interrupted " + Thread.currentThread().isInterrupted());
                while (iterator.hasNext() && !Thread.currentThread().isInterrupted()) {
//                    Log.v(TAG, "loadData: next " + Thread.currentThread().getName());
                    Friend friend = iterator.next();
                    if (!TextUtils.isEmpty(searchKey)) {
                        if (friend.getNickName().contains(searchKey)) {
                            filterAdd(friends, friend);
                            continue;
                        }
                    } else {
                        filterAdd(friends, friend);
                    }
                }
            });
            if (Thread.currentThread().isInterrupted()) {
                // 线程已经被取消，
                Log.i(TAG, "loadData: interrupted " + Thread.currentThread().getName());
                return;
            }
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);

            long delayTime = 200 - (System.currentTimeMillis() - startTime);// 保证至少200ms的刷新过程
            if (delayTime < 0) {
                delayTime = 0;
            }
            Log.i(TAG, "loadData: end " + Thread.currentThread().getName() + ", post " + delayTime);
            c.postDelayed(r -> {
                tvFriendCount.setText(getString(getCountPlaceHolder(), sortedList.size()));
                if (sortedList.size() == 0) {
                    llGroupFooter.setVisibility(View.GONE);
                    rlDefaultGroupList.setVisibility(View.VISIBLE);
                } else {
                    llGroupFooter.setVisibility(View.VISIBLE);
                    rlDefaultGroupList.setVisibility(View.GONE);
                }
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
            }, delayTime);
        });
    }

    private int getCountPlaceHolder() {
        if (index == 0) {
            return R.string.room_count_create_place_holder;
        } else if (index == 1) {
            return R.string.room_count_manage_place_holder;
        } else {
            return R.string.room_count_enter_place_holder;
        }
    }

    private void filterAdd(List<Friend> friends, Friend friend) {
        if (index == 0) {
            // 只添加我创建的群组
            if (friend.getRoomRole() == 1) {
                friends.add(friend);
            }
        } else if (index == 1) {
            // 只添加我管理的群组
            if (friend.getRoomRole() == 2) {
                friends.add(friend);
            }
        } else {
            // 只添加我加入的群组
            if (friend.getRoomRole() == 3 || friend.getRoomRole() == 4) {
                // 普通身份 || 隐身人
                friends.add(friend);
            }
        }
    }

}
