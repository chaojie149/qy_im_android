package com.tongxin.caihong.fragment;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.AllStationsNotice;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.PrivacySetting;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.SignBean;
import com.tongxin.caihong.bean.event.EventPostSuccess;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.HideSearchHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.pay.chat.PayActivity;
import com.tongxin.caihong.pay.new_ui.PaymentOrReceiptActivity;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.groupchat.SelectContactsActivity;
import com.tongxin.caihong.ui.me.SignInRedActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.message.multi.RoomInfoActivity;
import com.tongxin.caihong.ui.nearby.UserSearchActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.ui.search.SearchAllActivity;
import com.tongxin.caihong.ui.sign.SignUpActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.AllStationsNoticeDialog;
import com.tongxin.caihong.view.ChatBottomView;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.view.MessagePopupWindow;
import com.tongxin.caihong.view.SearchHeader;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.VerifyDialog;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.XmppConnectionManager;
import com.tongxin.caihong.xmpp.listener.AuthStateListener;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.SwipeMenu;
import com.yanzhenjie.recyclerview.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 消息界面
 */
public class MessageFragment extends EasyFragment implements AuthStateListener {
    private final static long intervalTime = 800;
    // 消息界面在前台展示中就不响铃新消息，
    public static boolean foreground = false;
    private boolean flag = false;
    private boolean search;
    private TextView mTvTitle;
    private ImageView mIvTitleRight;
    private View mHeadView;
    private SearchHeader mEditText;
    private LinearLayout llNotice;
    private TextView tvNotice;
    private LinearLayout mNetErrorLl;
    private ImageView mIvNoData;
    private SmartRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mListView;
    private MessageListAdapter mAdapter;
    private List<Friend> mFriendList;
    private String mLoginUserId;
    private MessagePopupWindow mMessagePopupWindow;
    private TextView mTvTitleLeft;
    private TextView tv_title_right;
    private String draftReply;
    private AllStationsNotice allStationsNotice;
    private SignBean signBean;
    // 附近的人
    private RecyclerView rcyNearlyPerson;
    // 下面的刷新方法机制简单来讲就是等某一用户的消息收完了才刷新页面，判断收完的条件就是间隔intervalTime ms没有在收到那个用户的消息了，同时该机制每个用户都是独立的，互不影响
    private Map<String, Long> timeMap = new HashMap<>();
    private Map<String, Integer> delayRefreshCountMap = new HashMap<>();
    private Map<String, RefreshTimer> timerMap = new HashMap<>();
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if (action.equals(MsgBroadcast.ACTION_MSG_UI_UPDATE)) {
                refresh();
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UI_UPDATE_SINGLE)) {
                if (!foreground) {
                    Log.e("refresh", "当前界面为不可见状态，不刷新");
                    return;
                }
                String fromUserId = intent.getStringExtra("fromUserId");
                if (timeMap.containsKey(fromUserId)) {
                    if (System.currentTimeMillis() - timeMap.get(fromUserId) >= intervalTime) {
                        Log.e("refresh", "收到刷新通知，针对-->" + fromUserId + "的上次刷新时间间隔大于等于" + intervalTime + "  ms，立即刷新");
                        resetRefreshTime(fromUserId, true);
                    } else {
                        Log.e("refresh", "收到刷新通知，针对-->" + fromUserId
                                + "的上次刷新时间间隔小于 " + intervalTime + " ms，猜测 " + fromUserId + " 等下还有消息过来，" +
                                "取出map内定时器，取消上一次待刷新事件，并重新开启定时器");
                        RefreshTimer refreshTimer = timerMap.get(fromUserId);
                        if (refreshTimer == null) {
                            refreshTimer = new RefreshTimer(fromUserId);
                            timerMap.put(fromUserId, refreshTimer);
                        } else {
                            refreshTimer.cancel();
                        }
                        refreshTimer.start();
                        int delayRefreshCount = delayRefreshCountMap.get(fromUserId);
                        if (delayRefreshCount == 20) {
                            Log.e("refresh", "延时刷新消息条数达到20条，刷新一下");
                            resetRefreshTime(fromUserId, true);
                        } else {
                            Log.e("refresh", "延时刷新消息条数未满20条，不刷新且更新时间与次数");
                            timeMap.put(fromUserId, System.currentTimeMillis());
                            delayRefreshCountMap.put(fromUserId, delayRefreshCount + 1);
                        }
                    }
                } else {
                    Log.e("refresh", "收到刷新通知，针对-->" + fromUserId + "进行第一次刷新");
                    resetRefreshTime(fromUserId, true);
                }
            } else if (action.equals(Constants.NOTIFY_MSG_SUBSCRIPT)) {
                Friend friend = (Friend) intent.getSerializableExtra(AppConstant.EXTRA_FRIEND);
                if (friend != null) {
                    clearMessageNum(friend);
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {// 网络发生改变
                if (!HttpUtil.isGprsOrWifiConnected(getActivity())) {
                    mNetErrorLl.setVisibility(View.VISIBLE);
                } else {
                    mNetErrorLl.setVisibility(View.GONE);
                }
            }
        }
    };
    /**
     * 菜单创建器，在Item要创建菜单的时候调用。
     */
    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int position) {
            swipeRightMenu.getMenuLayout().smoothCloseMenu();

            int width = DisplayUtil.dip2px(requireContext(), 80f);
            // 1. MATCH_PARENT 自适应高度，保持和Item一样高;
            // 2. 指定具体的高，比如80;
            // 3. WRAP_CONTENT，自身高度，不推荐;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;

            // 添加右侧的，如果不添加，则右侧不会出现菜单。
            {
                Friend friend = mAdapter.getItem(position);
                final long time = friend.getTopTime();

                SwipeMenuItem top = new SwipeMenuItem(requireContext()).setBackgroundColorResource(R.color.Grey_400)
                        .setText(R.string.top_tv)
                        .setTextColor(Color.WHITE)
                        .setTextSize(15)
                        .setWidth(width)
                        .setHeight(height);
                if (time == 0) {
                    top.setText(getString(R.string.top));
                } else {
                    top.setText(getString(R.string.cancel_top));
                }
                if (friend.getIsDevice() != 1) {// 我的设备去掉置顶选项
                    swipeRightMenu.addMenuItem(top);// 添加菜单到右侧。
                }
                SwipeMenuItem mark_unread = new SwipeMenuItem(requireContext()).setBackgroundColorResource(R.color.color_read_unread_item)
                        .setText(R.string.mark_unread)
                        .setTextColor(Color.WHITE)
                        .setTextSize(15)
                        .setWidth(width)
                        .setHeight(height);
                if (friend.getUnReadNum() > 0) {
                    mark_unread.setText(getString(R.string.mark_read));
                } else {
                    mark_unread.setText(getString(R.string.mark_unread));
                }
                swipeRightMenu.addMenuItem(mark_unread);// 添加菜单到右侧。
                SwipeMenuItem delete = new SwipeMenuItem(requireContext()).setBackgroundColorResource(R.color.redpacket_bg)
                        .setText(R.string.delete)
                        .setTextColor(Color.WHITE)
                        .setTextSize(15)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(delete);// 添加菜单到右侧。
            }
        }
    };
    /**
     * RecyclerView的Item的Menu点击监听。
     */
    private OnItemMenuClickListener mMenuItemClickListener = new OnItemMenuClickListener() {
        @Override
        public void onItemClick(SwipeMenuBridge menuBridge, int position) {
            menuBridge.closeMenu();

            int direction = menuBridge.getDirection(); // 左侧还是右侧菜单。
            int menuPosition = menuBridge.getPosition(); // 菜单在RecyclerView的Item中的Position。

            Friend friend = mAdapter.getData().get(position);
            if (friend.getIsDevice() == 1) {// 我的设备去掉了置顶
                menuPosition = menuPosition + 1;
            }
            if (direction == SwipeRecyclerView.RIGHT_DIRECTION) {
                if (menuPosition == 0) {
                    updateTopChatStatus(friend);
                } else if (menuPosition == 1) {
                    if (friend.getUnReadNum() > 0) {
                        clearMessageNum(friend);
                    } else {
                        FriendDao.getInstance().markUserMessageUnRead(mLoginUserId, friend.getUserId());
                        MsgBroadcast.broadcastMsgNumUpdate(MyApplication.getInstance(), true, 1);
                        MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
                    }
                } else {
                    delete(friend);
                    // 保留旧代码，
                    // 内部和外部的mFriendList都要更新到，
                    MessageFragment.this.mFriendList.remove(position);
                    mAdapter.setData(MessageFragment.this.mFriendList);
                }
            }

        }
    };

    private void refresh(String friendId) {
        if (TextUtils.isEmpty(friendId)) {
            Log.e("refresh", "TextUtils.isEmpty(friendId)，刷新全部");
            refresh();
            return;
        }

        // 要更新的除了消息还有消息数量，所以不方便外面传入，只能查数据库，又拖慢了，
        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, friendId);
        if (!mAdapter.updateContent(friend)) {
            // 当前列表没有这个好友，直接全部刷新，
            Log.e("refresh", "当前列表没有这个好友，刷新全部");
            refresh();
        } else {
            Log.e("refresh", "刷新单行成功");
        }
    }

    private void refresh() {
        if (!TextUtils.isEmpty(mEditText.getText().toString().trim())) {
            mEditText.setText("");// 内部调用了loadData
        } else {
            loadDatas();
        }
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_message;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBusHelper.register(this);
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        initActionBar();
        // 不能用createView判断不初始化，因为Fragment复用时老activity可能被销毁了，
        initView();
        loadDatas();
        loadAllStationsNotice();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        foreground = isVisibleToUser;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        // 测试发现fragment之间的切换不会回调onPause与onResume，
        // 但是会回调这个方法，这里处理下
        // mTvTitle != null判断没初始化视图情况不处理，
        if (!hidden && mTvTitle != null) {
            loadDatas();
        }
        foreground = !hidden;
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onPause() {
        foreground = false;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MainActivity.isAutoLogin) {
            int authState = XmppConnectionManager.mXMPPCurrentState;
            if (authState == AuthStateListener.AUTH_STATE_CLOSE || authState == AuthStateListener.AUTH_STATE_ERROR) {
                mTvTitle.setText(getString(R.string.msg_view_controller_going_off));
            } else if (authState == AuthStateListener.AUTH_STATE_SUCCESS) {
                findViewById(R.id.pb_title_center).setVisibility(View.GONE);
                mTvTitle.setText(getString(R.string.msg_view_controller_online));
            } else {
                findViewById(R.id.pb_title_center).setVisibility(View.GONE);
                mTvTitle.setText(getString(R.string.msg_view_controller_off_line));
            }
        }
        loadDatas();
        // 需放在loadDatas之后，因为loadDatas内有对timeMap做处理
        foreground = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
        ListenerManager.getInstance().removeAuthStateChangeListener(this);
    }

    private void initActionBar() {
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setTypeface(null, Typeface.BOLD);
        mTvTitle.setText(getString(R.string.message));
        appendClick(mTvTitle);

        mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        mIvTitleRight.setImageResource(R.mipmap.kx_message_more_icon);
        int padding = DisplayUtil.dip2px(requireContext(), 13);
        mIvTitleRight.setPadding(padding, padding, padding, padding);
        appendClick(mIvTitleRight);

        mTvTitleLeft = findViewById(R.id.tv_title_left);
        mTvTitleLeft.setText(getResources().getString(R.string.start_edit));
        mTvTitleLeft.setVisibility(View.GONE);

        tv_title_right = findViewById(R.id.tv_title_right);
        tv_title_right.setText(getResources().getString(R.string.finish));
        tv_title_right.setVisibility(View.GONE);
        appendClick(tv_title_right);

        if (coreManager.getConfig().enableSignModule) {
            ImageView ivTitleRightSign = findViewById(R.id.iv_title_right_sign);
            ivTitleRightSign.setVisibility(View.VISIBLE);
            ivTitleRightSign.setOnClickListener(view -> {
                if (signBean == null) {
                    loadSignUP(true);
                } else {
                    SignUpActivity.start(requireActivity(), signBean);
                }
            });
        }
    }

    private void initView() {
        findViewById(R.id.sign).setOnClickListener(v -> startActivity(new Intent(requireContext(), SignInRedActivity.class)));

        mLoginUserId = coreManager.getSelf().getUserId();

        mFriendList = new ArrayList<>();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (mHeadView != null) {
            // Fragment复用时可能已经添加过headerView了，
            mListView.removeHeaderView(mHeadView);
            mAdapter.notifyDataSetChanged();
        }

        mListView = findViewById(R.id.recyclerView);
        mListView.setSwipeMenuCreator(swipeMenuCreator);
        mListView.setOnItemMenuClickListener(mMenuItemClickListener);
        mListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRefreshLayout = findViewById(R.id.refreshLayout);
        mHeadView = inflater.inflate(R.layout.head_for_messagefragment, mRefreshLayout, false);
        HideSearchHelper.hideSearchHeader(this);
        View llSearch = findViewById(R.id.shSearch);
        llSearch.setOnClickListener(v -> SearchAllActivity.start(requireActivity(), "friend"));
        mEditText = findViewById(R.id.shSearch);
        llNotice = mHeadView.findViewById(R.id.llNotice);
        ImageViewCompat.setImageTintList(mHeadView.findViewById(R.id.ivNoticeTint), ColorStateList.valueOf(SkinUtils.getSkin(requireContext()).getAccentColor()));
        tvNotice = mHeadView.findViewById(R.id.tvNotice);
        llNotice.setOnClickListener(view -> showAllStationsNotice(false));
        mHeadView.findViewById(R.id.ivClose).setOnClickListener(view -> {
            SelectionFrame selectionFrame = new SelectionFrame(requireContext());
            selectionFrame.setSomething(null, getString(R.string.tip_notice_not_show), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    PreferenceUtils.putBoolean(requireContext(), "NOTICE_NOT_SHOW" + allStationsNotice.getId() + mLoginUserId, true);
                    llNotice.setVisibility(View.GONE);
                }
            });
            selectionFrame.show();
        });
        mNetErrorLl = (LinearLayout) mHeadView.findViewById(R.id.net_error_ll);
        mNetErrorLl.setOnClickListener(this);
        mIvNoData = mHeadView.findViewById(R.id.iv_no_nearly_msg);
        mListView.addHeaderView(mHeadView);
        mAdapter = new MessageListAdapter();
        mAdapter.setHasStableIds(true);
        mListView.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(rl -> {
            refresh();
        });

        ListenerManager.getInstance().addAuthStateChangeListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MsgBroadcast.ACTION_MSG_UI_UPDATE);// 刷新页面Ui
        intentFilter.addAction(MsgBroadcast.ACTION_MSG_UI_UPDATE_SINGLE);// 刷新页面Ui
        intentFilter.addAction(Constants.NOTIFY_MSG_SUBSCRIPT);// 刷新"消息"角标
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);// 网络发生改变
        getActivity().registerReceiver(mUpdateReceiver, intentFilter);
    }

    /**
     * 加载朋友数据
     */
    private void loadDatas() {
        if (mFriendList != null) {
            mFriendList.clear();
        }
        search = false;
        mFriendList = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
        List<Friend> mRemoveFriend = new ArrayList<>();
        if (mFriendList.size() > 0) {
            for (int i = 0; i < mFriendList.size(); i++) {
                Friend friend = mFriendList.get(i);
                if (friend != null) {
                    if (timeMap.containsKey(friend.getUserId())) {
                        Log.e("refresh", "loadData被调用了，更新" + friend.getNickName() + "的刷新时间为当前时间");
                        resetRefreshTime(friend.getUserId(), false);
                    }
                    if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)
                            || friend.getUserId().equals(mLoginUserId)) {
                        mRemoveFriend.add(friend);
                    }
                }
            }
            mFriendList.removeAll(mRemoveFriend);
        }

        mTvTitle.post(() -> {
            updataListView();
            mRefreshLayout.finishRefresh();
        });
    }


    private void clearMessageNum(Friend friend) {
        friend.setUnReadNum(0);
        FriendDao.getInstance().markUserMessageRead(mLoginUserId, friend.getUserId());
        MainActivity mMainActivity = (MainActivity) getActivity();
        if (mMainActivity != null) {
            mMainActivity.updateNumData();
        }
        mAdapter.updateUnReadNum(friend);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_title_left:
                if (flag) {
                    SelectionFrame selectionFrame = new SelectionFrame(getActivity());
                    selectionFrame.setSomething(null, getString(R.string.tip_sure_delete_all_data),
                            new SelectionFrame.OnSelectionFrameClickListener() {
                                @Override
                                public void cancelClick() {

                                }

                                @Override
                                public void confirmClick() {
                                    for (int i = 0; i < mFriendList.size(); i++) {
                                        final Friend friend = mFriendList.get(i);
                                        delete(friend);
                                        if (i == mFriendList.size() - 1) {
                                            mFriendList.clear();
                                            mAdapter.setData(MessageFragment.this.mFriendList);
                                        }
                                    }
                                }
                            });
                    selectionFrame.show();
                    return;
                }
                flag = true;
                mIvTitleRight.setVisibility(View.GONE);
                tv_title_right.setVisibility(View.VISIBLE);
                mTvTitleLeft.setText(getResources().getString(R.string.empty));
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.tv_title_right:
                flag = false;
                tv_title_right.setVisibility(View.GONE);
                mTvTitleLeft.setText(getResources().getString(R.string.start_edit));
                mIvTitleRight.setVisibility(View.VISIBLE);
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.tv_title_center:
                break;
            case R.id.iv_title_right:
                mMessagePopupWindow = new MessagePopupWindow(getActivity(), this, coreManager);
                mMessagePopupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mMessagePopupWindow.showAsDropDown(v,
                        -(mMessagePopupWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                        0);
                break;
            case R.id.create_group:
                // 发起群聊
                mMessagePopupWindow.dismiss();
                startActivity(new Intent(getActivity(), SelectContactsActivity.class));
                break;
            case R.id.add_friends:
                // 添加朋友
                mMessagePopupWindow.dismiss();
                UserSearchActivity.start(getActivity());
                break;
            case R.id.scanning:
                // 扫一扫
                mMessagePopupWindow.dismiss();
                MainActivity.requestQrCodeScan(getActivity());
                break;
            case R.id.receipt_payment:
                // 收付款
                mMessagePopupWindow.dismiss();
                PaymentOrReceiptActivity.start(getActivity(), coreManager.getSelf().getUserId());
                break;
            case R.id.net_error_ll:
                //网络错误
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                break;
        }
    }

    /**
     * 更新列表
     */
    private void updataListView() {
        mAdapter.setData(mFriendList);
    }

    /**
     * xmpp在线状态监听
     */
    @Override
    public void onAuthStateChange(int authState) {
        authState = XmppConnectionManager.mXMPPCurrentState;
        if (mTvTitle == null) {
            return;
        }
        if (authState == 0 || authState == 1) {
            // 登录中
            findViewById(R.id.pb_title_center).setVisibility(View.VISIBLE);
            mTvTitle.setText(getString(R.string.msg_view_controller_going_off));
        } else if (authState == 2) {
            // 在线
            findViewById(R.id.pb_title_center).setVisibility(View.GONE);
            mTvTitle.setText(getString(R.string.msg_view_controller_online));
            mNetErrorLl.setVisibility(View.GONE);// 网络判断对部分手机有时会失效，坐下兼容(当xmpp在线时，隐藏网络提示)
        } else {
            // 离线
            findViewById(R.id.pb_title_center).setVisibility(View.GONE);
            mTvTitle.setText(getString(R.string.msg_view_controller_off_line));
        }
    }

    private void updateTopChatStatus(Friend friend) {
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", mLoginUserId);
        if (friend.getRoomFlag() == 0) {
            params.put("toUserId", friend.getUserId());
        } else {
            params.put("roomId", friend.getRoomId());
        }
        if (friend.getRoomFlag() == 0) {
            params.put("type", String.valueOf(2));
        } else {
            params.put("type", String.valueOf(1));
        }
        params.put("offlineNoPushMsg", friend.getTopTime() == 0 ? String.valueOf(1) : String.valueOf(0));

        String url;
        if (friend.getRoomFlag() == 0) {
            url = CoreManager.requireConfig(MyApplication.getContext()).FRIENDS_NOPULL_MSG;
        } else {
            url = CoreManager.requireConfig(MyApplication.getContext()).ROOM_DISTURB;
        }
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (friend.getTopTime() == 0) {
                                FriendDao.getInstance().updateTopFriend(friend.getUserId(), friend.getTimeSend());
                            } else {
                                FriendDao.getInstance().resetTopFriend(friend.getUserId());
                            }
                            loadDatas();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    private void emptyServerMessage(String userId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", String.valueOf(0));// 0 清空单人 1 清空所有
        params.put("toUserId", userId);

        HttpUtils.get().url(coreManager.getConfig().EMPTY_SERVER_MESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    void delete(Friend friend) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        // 现侧滑删除都是只删除本地记录
/*
        if (friend.getRoomFlag() == 0) {// 群组不支持删除服务器上记录，因为群组只有一张消息表
            //  获取首页侧滑删除服务器聊天记录状态，
            boolean isSlideClearServerMSG = PrivacySettingHelper.getPrivacySettings(getActivity()).getIsSkidRemoveHistoryMsg() == 1;
            if (isSlideClearServerMSG) {
                emptyServerMessage(friend.getUserId());
            }
        }
*/
        // 如果是普通的人/群组，从好友表中删除最后一条消息的记录，这样就不会查出来了
        FriendDao.getInstance().resetFriendMessage(mLoginUserId, friend.getUserId());
        // 消息表中删除
        ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());
        MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(mLoginUserId, friend.getUserId());
        if (friend.getUnReadNum() > 0) {
            MsgBroadcast.broadcastMsgNumUpdate(getActivity(), false, friend.getUnReadNum());
        }
    }

    private void resetRefreshTime(String friendId, boolean refresh) {
        timeMap.put(friendId, System.currentTimeMillis());
        delayRefreshCountMap.put(friendId, 0);
        if (refresh) {
            refresh(friendId);
        }
    }

    /**
     * 加载全站公告
     */
    private void loadAllStationsNotice() {
        HttpUtils.get().url(coreManager.getConfig().FIND_NOTICE_CONFIG)
                .params(new HashMap<>())
                .build()
                .execute(new BaseCallback<AllStationsNotice>(AllStationsNotice.class) {

                    @Override
                    public void onResponse(ObjectResult<AllStationsNotice> result) {
                        if(!isAdded()) {
                            return;
                        }
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            allStationsNotice = result.getData();
                            showAllStationsNotice(true);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /**
     * 自动弹出公告弹窗时，需要判断之前是否弹出过，弹出过就不弹了
     *
     * @param isAutoShowNoticeDialog
     */
    private void showAllStationsNotice(boolean isAutoShowNoticeDialog) {
        if (allStationsNotice != null) {
            if (allStationsNotice.getStatus() == 0
                    || allStationsNotice.getStatus() == 2) {
                // 不展示 || 只展示一次，隐藏标题栏公告
                llNotice.setVisibility(View.GONE);
            } else {
                // 永久展示，显示标题栏公告
                tvNotice.setText(allStationsNotice.getContent());
                llNotice.setVisibility(View.VISIBLE);

                if (PreferenceUtils.getBoolean(requireContext(), "NOTICE_NOT_SHOW" + allStationsNotice.getId() + mLoginUserId, false)) {
                    // 用户手动关闭了该公告，该公告就不显示了
                    llNotice.setVisibility(View.GONE);
                }
            }
            if (allStationsNotice.getStatus() == 1 || allStationsNotice.getStatus() == 2) {
                // 永久展示 || 只展示一次
                if (isAutoShowNoticeDialog) {
                    boolean showed = PreferenceUtils.getBoolean(requireContext(), "SHOW_ALL_STATIONS_NOTICE" + allStationsNotice.getId() + mLoginUserId, false);
                    if (showed) {
                        // 弹出过，不弹出
                        return;
                    }
                }
                // 弹出公告弹窗
                AllStationsNoticeDialog allStationsNoticeDialog = new AllStationsNoticeDialog(requireContext(), allStationsNotice);
                allStationsNoticeDialog.show();
                PreferenceUtils.putBoolean(requireContext(), "SHOW_ALL_STATIONS_NOTICE" + allStationsNotice.getId() + mLoginUserId, true);
            }
        }
    }

    /**
     * 获取当前签到详情
     */
    private void loadSignUP(boolean showDialog) {
        if (showDialog) {
            DialogHelper.showDefaulteMessageProgressDialog(requireContext());
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userId", coreManager.getSelf().getUserId());
        HttpUtils.get().url(coreManager.getConfig().RED_SIGN_QUERY)
                .params(params)
                .build()
                .execute(new BaseCallback<SignBean>(SignBean.class) {
                    @Override
                    public void onResponse(ObjectResult<SignBean> result) {
                        if (showDialog) {
                            DialogHelper.dismissProgressDialog();
                        }
                        if (getContext() == null) {
                            // 以防万一的崩溃，
                            return;
                        }
                        if (Result.checkSuccess(requireContext(), result)) {
                            // 签到数据返回
                            signBean = result.getData();
                            if (showDialog) {
                                SignUpActivity.start(requireActivity(), signBean);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (showDialog) {
                            DialogHelper.dismissProgressDialog();
                        }
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventPostSuccess message) {
        loadSignUP(false);
    }

    private class RefreshTimer extends CountDownTimer {
        private String friendId;

        RefreshTimer(String friendId) {
            super(1000, 1000);
            this.friendId = friendId;
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            Log.e("refresh", "针对-->" + friendId + " 计时结束 " + friendId
                    + " 在" + intervalTime + " ms以内没有消息在过来了，猜测消息收完了，立即刷新，map内在记录此处操作时间");
            resetRefreshTime(friendId, true);
            // 计时结束之后，内存中移掉该计时器
            timerMap.remove(friendId);
        }
    }

    /**
     * 适配器
     */
    class MessageListAdapter extends RecyclerView.Adapter<MessageListViewHolder> {

        private List<Friend> mFriendList = new ArrayList<>();

        @NonNull
        @Override
        public MessageListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_nearly_message, viewGroup, false);
            return new MessageListViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageListViewHolder messageListViewHolder, int position) {
            Friend friend = mFriendList.get(position);
            messageListViewHolder.bind(friend, position);
        }

        // 当前页面list混乱，存在两个同时使用的list, 一个fragment持有，一个adapter持有，
        // 有可能因异步操作list导致listView崩溃，所以不能共用fragment持有的list，
        // 读取时尽量使用adapter持有的list确保数据一致，
        public Friend getItem(int position) {
            return mFriendList.get(position);
        }

        @Override
        public int getItemCount() {
            return mFriendList.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public List<Friend> getData() {
            return mFriendList;
        }

        public void setData(List<Friend> mFriendList) {
            this.mFriendList = new ArrayList<>(mFriendList);
            notifyDataSetChanged();
            mIvNoData.setVisibility(mFriendList.size() == 0 ? View.VISIBLE : View.GONE);
        }

        /**
         * 返回true表示成功刷新这个朋友消息，
         */
        boolean updateContent(Friend updateFriend) {
            if (updateFriend == null) {
                return false;
            }
            int newPosition = -1;
            int oldPosition = -1;
            for (int i = 0; i < mFriendList.size(); i++) {
                Friend friend = mFriendList.get(i);
                if (newPosition < 0 && (friend.getTopTime() == 0 || updateFriend.getTopTime() > 0) && friend.getTimeSend() <= updateFriend.getTimeSend()) {
                    newPosition = i;
                }
                if (TextUtils.equals(friend.getUserId(), updateFriend.getUserId())) {
                    oldPosition = i;
                    mFriendList.set(i, updateFriend);
                    mAdapter.notifyItemChanged(i);
                    break;
                }
            }
            if (newPosition >= 0 && oldPosition >= 0 && newPosition != oldPosition) {
                Friend remove = mFriendList.remove(oldPosition);
                mFriendList.add(newPosition, remove);
                mAdapter.notifyDataSetChanged();
            }
            return oldPosition >= 0;
        }

        /**
         * 返回true表示成功刷新这个朋友消息，
         */
        boolean updateUnReadNum(Friend updateFriend) {
            for (int i = 0; i < mFriendList.size(); i++) {
                Friend mF = mFriendList.get(i);
                if (TextUtils.equals(mF.getUserId(), updateFriend.getUserId())) {
                    mFriendList.set(i, updateFriend);
                    mAdapter.notifyItemChanged(i);
                    return true;
                }
            }
            return false;
        }
    }

    class MessageListViewHolder extends RecyclerView.ViewHolder {
        Context mContext = requireContext();
        RelativeLayout rl_warp = itemView.findViewById(R.id.item_friend_warp);
        ImageView iv_delete = itemView.findViewById(R.id.iv_delete);
        HeadView avatar = itemView.findViewById(R.id.avatar_imgS);
        TextView nick_name_tv = itemView.findViewById(R.id.nick_name_tv);
        TextView tip_tv = itemView.findViewById(R.id.item_message_tip);
        TextView content_tv = itemView.findViewById(R.id.content_tv);
        TextView time_tv = itemView.findViewById(R.id.time_tv);
        TextView num_tv = itemView.findViewById(R.id.num_tv);
        View replay_iv = itemView.findViewById(R.id.replay_iv);
        View not_push_ll = itemView.findViewById(R.id.not_push_iv);

        MessageListViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind(Friend friend, int position) {
            itemView.setOnClickListener(v -> {
                // 在跳转之前关闭软键盘
                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.hideSoftInputFromWindow(findViewById(R.id.message_fragment).getWindowToken(), 0); // 强制隐藏键盘
                }

                Intent intent = new Intent();
                if (friend.getRoomFlag() == 0) { // 个人
                    if (TextUtils.equals(friend.getUserId(), Friend.ID_SK_PAY)) {
                        intent.setClass(getActivity(), PayActivity.class);
                    } else {
                        intent.setClass(getActivity(), ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND, friend);
                    }
                } else {
                    intent.setClass(getActivity(), MucChatActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                    intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
                }

                if (search) {
                    intent.putExtra("isserch", true);
                    // 为什么改取为chatRecordTimeOut，可至queryChatMessageByContent方法内查看原因
                    // intent.putExtra("jilu_id", friend.getTimeSend());
                    intent.putExtra("jilu_id", friend.getChatRecordTimeOut());
                } else {
                    intent.putExtra(Constants.NEW_MSG_NUMBER, friend.getUnReadNum());
                }
                startActivity(intent);
                clearMessageNum(friend);
            });

            AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), friend, avatar);
            nick_name_tv.setText(!TextUtils.isEmpty(friend.getRemarkName()) ? friend.getRemarkName() : friend.getNickName());

            if (friend.getRoomFlag() != 0) {// 群组 @
                if (friend.getIsAtMe() == 1) {
                    tip_tv.setText("[有人@我]");
                    tip_tv.setVisibility(View.VISIBLE);
                } else if (friend.getIsAtMe() == 2) {
                    tip_tv.setText("[@全体成员]");
                    tip_tv.setVisibility(View.VISIBLE);
                } else {
                    tip_tv.setVisibility(View.GONE);
                }
            } else {
                tip_tv.setVisibility(View.GONE);
            }

            if (friend.getType() == XmppMessage.TYPE_TEXT || friend.getType() == XmppMessage.TYPE_REPLAY) {// 文本消息 表情
                String s = friend.getContent();
                if (s == null) {
                    // 以防万一，bugly收到过崩溃，原因不明，
                    s = "";
                }
                // TODO: 这样匹配的话正常消息里的&8824也会被替换掉，
                if (s.contains("&8824")) {// 草稿
                    s = s.replaceFirst("&8824", "");

                    tip_tv.setText(getString(R.string.draft));
                    tip_tv.setVisibility(View.VISIBLE);
                }
                CharSequence content = HtmlUtils.addSmileysToMessage(s, false);
/*
                CharSequence content = HtmlUtils.addSmileysToMessage(
                        ChatMessage.getSimpleContent(requireContext(), friend.getType(), s,
                                friend.getUserId(), friend.getPrivacy()),
                        false);
*/
                content_tv.setText(content);
            } else {
/*
                content_tv.setText(HtmlUtils.addSmileysToMessage(
                        ChatMessage.getSimpleContent(requireContext(), friend.getType(), friend.getContent(),
                                friend.getUserId(), friend.getPrivacy()),
                        false));
*/
                content_tv.setText(friend.getContent());
             /*   if (friend.getType()==202 && !friend.getContent().contains("重新编辑")) {
                    content_tv.setText("");
                }else {
                    content_tv.setText(friend.getContent());
                }*/
            }

            // 搜索下匹配关键字高亮显示
            if (search) {
                String text = content_tv.getText().toString();
                SpannableString spannableString = StringUtils.matcherSearchTitle(Color.parseColor("#fffa6015"),
                        text, mEditText.getText().toString());
                content_tv.setText(spannableString);
            }

            time_tv.setText(TimeUtils.getFriendlyTimeDesc(getActivity(), friend.getTimeSend()));
            UiUtils.updateNum(num_tv, friend.getUnReadNum());

            if (num_tv.getVisibility() == View.VISIBLE) {
                replay_iv.setVisibility(View.GONE);
            } else {
                replay_iv.setVisibility(View.VISIBLE);
            }
            if (friend.getUserId().equals(Friend.ID_SK_PAY)) {
                replay_iv.setVisibility(View.GONE);
            }
            if (friend.isService()) {
                replay_iv.setVisibility(View.GONE);
            }

            if (friend.getOfflineNoPushMsg() == 1) {
                not_push_ll.setVisibility(View.VISIBLE);
            } else {
                not_push_ll.setVisibility(View.GONE);
            }

            final long time = friend.getTopTime();

            if (time == 0) {
                rl_warp.setBackgroundResource(R.drawable.list_selector_background_ripple);
            } else {
                rl_warp.setBackgroundResource(R.color.Grey_200);
            }

            // 点击头像跳转详情
            avatar.setOnClickListener(view -> {
                if (!UiUtils.isNormalClick(view)) {
                    return;
                }
                if (friend.getRoomFlag() == 0) {   // 个人
                    if (!friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)
                            && !friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)
                            && !friend.getUserId().equals(Friend.ID_SK_PAY)
                            && friend.getIsDevice() != 1) {
                        Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                        startActivity(intent);
                    }
                } else {   // 群组
                    if (friend.getGroupStatus() == 0
                            || friend.getGroupStatus() == 4) {
                        Intent intent = new Intent(getActivity(), RoomInfoActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                        startActivity(intent);
                    }
                }
            });

            iv_delete.setVisibility(flag ? View.VISIBLE : View.GONE);
            iv_delete.setOnClickListener(v -> {
                delete(friend);
                // 保留旧代码，
                // 内部和外部的mFriendList都要更新到，
                MessageFragment.this.mFriendList.remove(position);
                mAdapter.setData(MessageFragment.this.mFriendList);
            });

            num_tv.setOnClickListener(v -> reply(friend));
            replay_iv.setOnClickListener(v -> reply(friend));
        }

        private void reply(Friend friend) {
            CharSequence hint = HtmlUtils.addSmileysToMessage(content_tv.getText().toString(), false);
            if (tip_tv.getVisibility() == View.VISIBLE
                    && TextUtils.equals(tip_tv.getText().toString(), getString(R.string.draft))) {
                // 为草稿时，text也显示出来
                draftReply = content_tv.getText().toString();
            } else {
                draftReply = "";
            }
            DialogHelper.verify(
                    requireActivity(),
                    getString(R.string.title_replay_place_holder_title),
                    nick_name_tv.getText().toString(),
                    hint,
                    draftReply,
                    ChatBottomView.LIMIT_MESSAGE_LENGTH,
                    new VerifyDialog.VerifyClickListener() {
                        @Override
                        public void cancel() {

                        }

                        @Override
                        public void send(String str) {
                            str = str.trim();
                            if (TextUtils.isEmpty(str)) {
                                ToastUtil.showToast(requireContext(), R.string.tip_replay_empty);
                                return;
                            }
                            if (!coreManager.isLogin()) {
                                Reporter.unreachable();
                                ToastUtil.showToast(requireContext(), R.string.tip_xmpp_offline);
                                return;
                            }
                            if (friend.getRoomFlag() != 0) {
                                // 用户可能不在群组里，
                                int status = friend.getGroupStatus();
                                if (1 == status) {
                                    ToastUtil.showToast(requireContext(), R.string.tip_been_kick);
                                    return;
                                } else if (2 == status) {
                                    ToastUtil.showToast(requireContext(), R.string.tip_disbanded);
                                    return;
                                } else if (3 == status) {
                                    ToastUtil.showToast(requireContext(), R.string.tip_group_disable_by_service);
                                    return;
                                }
                                RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(friend.getRoomId(), mLoginUserId);
                                boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + friend.getUserId(), false);
                                // 判断禁言状态，
                                if (member != null && member.getRole() == 3) {// 普通成员需要判断是否被禁言
                                    if (friend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                                        ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                                        return;
                                    }
                                    if (isAllShutUp) {
                                        ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                                        return;
                                    }
                                } else if (member == null) {// 也需要判断是否被禁言
                                    if (friend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                                        ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                                        return;
                                    }
                                    if (isAllShutUp) {
                                        ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                                        return;
                                    }
                                }
                                if (member != null && member.getRole() == 4) {
                                    ToastUtil.showToast(mContext, getString(R.string.hint_invisible));
                                    return;
                                }
                            }
                            ChatMessage message = new ChatMessage();
                            // 文本类型，抄自，
                            // com.chat.weichat.ui.message.ChatActivity.sendText
                            // com.chat.weichat.ui.message.MucChatActivity.sendText
                            // 黑名单没考虑，正常情况黑名单会删除会话，
                            message.setType(XmppMessage.TYPE_TEXT);
                            message.setFromUserId(mLoginUserId);
                            message.setFromUserName(coreManager.getSelf().getNickName());
                            message.setContent(str);
                            // 获取阅后即焚状态(因为用户可能到聊天设置界面 开启/关闭 阅后即焚，所以在onResume时需要重新获取下状态)
                            int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + friend.getUserId() + mLoginUserId, 0);
                            message.setIsReadDel(isReadDel);
                            if (1 != friend.getRoomFlag()) {
                                PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(requireContext());
                                boolean isSupport = privacySetting.getMultipleDevices() == 1;
                                if (isSupport) {
                                    message.setFromId("android");
                                } else {
                                    message.setFromId("chat");
                                }
                            }
                            if (1 == friend.getRoomFlag()) {
                                // 是群聊，
                                if (friend.getIsLostChatKeyGroup() == 1) {
                                    ToastUtil.showToast(mContext, getString(R.string.is_lost_key_cannot_support_send_msg, friend.getNickName()));
                                    return;
                                }
                                message.setToUserId(friend.getUserId());
                                if (friend.getChatRecordTimeOut() == -1 || friend.getChatRecordTimeOut() == 0) {// 永久
                                    message.setDeleteTime(-1);
                                } else {
                                    long deleteTime = TimeUtils.sk_time_current_time() / 1000 + (long) (friend.getChatRecordTimeOut() * 24 * 60 * 60);
                                    message.setDeleteTime(deleteTime);
                                }
                            } else if (friend.getIsDevice() == 1) {
                                message.setToUserId(mLoginUserId);
                                //  message.set(friend.getUserId());
                                // 我的设备消息不过期？
                            } else {
                                message.setToUserId(friend.getUserId());

                                // sz 消息过期时间
                                if (friend.getChatRecordTimeOut() == -1 || friend.getChatRecordTimeOut() == 0) {// 永久
                                    message.setDeleteTime(-1);
                                } else {
                                    long deleteTime = TimeUtils.sk_time_current_time() / 1000 + (long) (friend.getChatRecordTimeOut() * 24 * 60 * 60);
                                    message.setDeleteTime(deleteTime);
                                }
                            }
                            message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));
                            message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                            message.setTimeSend(TimeUtils.sk_time_current_time());
                            message.setGroup(friend.getRoomFlag() != 0);// 需要设置一下这个，不然saveNewSingleChatMessage内的updateFriendContent有差
                            // 消息保存在数据库，
                            ChatMessageDao.getInstance().saveNewSingleChatMessage(message.getFromUserId(), friend.getUserId(), message);
                            for (Friend mFriend : mFriendList) {
                                if (mFriend.getUserId().equals(friend.getUserId())) {
                                    mFriend.setType(XmppMessage.TYPE_TEXT);
                                    if (1 == friend.getRoomFlag()) {
                                        coreManager.sendMucChatMessage(message.getToUserId(), message);
                                        mFriend.setContent(message.getFromUserName() + " : " + message.getContent());
                                    } else {
                                        coreManager.sendChatMessage(message.getToUserId(), message);
                                        if (1 == isReadDel) {
                                            mFriend.setContent(getString(R.string.tip_read_fire_msg));
                                        } else {
                                            mFriend.setContent(message.getContent());
                                        }
                                    }
                                    // 需要更新timeSend，不然不会重新排序
                                    mFriend.setTimeSend(message.getTimeSend());
                                    // 清除小红点，
                                    clearMessageNum(mFriend);
                                    mAdapter.updateContent(mFriend);
                                    break;
                                }
                            }

                            if (!TextUtils.isEmpty(draftReply)) {
                                // 回复了草稿内容，将内存中的草稿内容清空
                                PreferenceUtils.putString(mContext, "WAIT_SEND" + message.getToUserId() + mLoginUserId, "");
                            }
                        }
                    });
        }
    }
}
