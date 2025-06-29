package com.tongxin.caihong.ui.nearby;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.NearRoomItemBean;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.EventCreateGroupFriend;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.BaseRecAdapter;
import com.tongxin.caihong.ui.base.BaseRecViewHolder;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.HeadView;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * 附近的群
 */
public class NearGroupActivity extends BaseActivity {
    private static final int pageSize = 20;
    private static final int REQUEST_CODE_LOCATION = 10;
    private SmartRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mPager;
    private RoomAdapter roomAdapter;
    private boolean isLoad;
    private List<NearRoomItemBean> mucRooms = new ArrayList<>();
    private int pagerIndex;
    private boolean noMore;

    public static void goToJoin(Context ctx, NearRoomItemBean roomItemBean, @Nullable RecyclerView.Adapter<?> adapter) {
        MucRoom mucRoom = new MucRoom();
        mucRoom.setId(roomItemBean.getId());
        mucRoom.setJid(roomItemBean.getJid());
        mucRoom.setName(roomItemBean.getName());
        mucRoom.setUserId(roomItemBean.getUserId());
        mucRoom.setNickName(roomItemBean.getNickname());
        mucRoom.setIsNeedVerify(roomItemBean.getIsNeedVerify());
        User self = CoreManager.requireSelf(ctx);
        Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), mucRoom.getJid());
        if (friend != null) {
            // 理论上不太可能走进这里
            if (friend.getGroupStatus() == 0
                    || friend.getGroupStatus() == 3
                    || friend.getGroupStatus() == 4) {
                interMucChat(ctx, mucRoom.getJid(), mucRoom.getName());
                roomItemBean.setInGroup(true);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                return;
            } else {
                // 已被踢出该群组 || 群组已被解散
                FriendDao.getInstance().deleteFriend(self.getUserId(), friend.getUserId());
                ChatMessageDao.getInstance().deleteMessageTable(self.getUserId(), friend.getUserId());
            }
        }

        if (mucRoom.getIsNeedVerify() == 1) {
            DialogHelper.showGroupVerifyInputDialog(ctx, mucRoom.getUserId(), mucRoom.getJid());
            return;
        }
        joinRoom(ctx, mucRoom, self.getUserId(), roomItemBean, adapter);
    }

    public static void joinRoom(Context ctx, final MucRoom room, final String loginUserId, NearRoomItemBean roomItemBean, @Nullable RecyclerView.Adapter<?> adapter) {
        DialogHelper.showDefaulteMessageProgressDialog(ctx);
        Map<String, String> params = new HashMap<>();
        params.put("roomId", room.getId());
        if (room.getUserId().equals(loginUserId))
            params.put("type", "1");
        else
            params.put("type", "2");
        params.put(AppConstant.GROUP_ADD_STYLE, AppConstant.GROUP_JOIN_NEAR);

        MyApplication.mRoomKeyLastCreate = room.getJid();

        HttpUtils.get().url(CoreManager.requireConfig(ctx).ROOM_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ctx, result)) {
                            EventBus.getDefault().post(new EventCreateGroupFriend(room));
                            AsyncUtils.postDelayed(this, c -> {
                                // 给500ms的时间缓存，防止群组还未创建好就进入群聊天界面
                                interMucChat(ctx, room.getJid(), room.getName());
                                roomItemBean.setInGroup(true);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            }, 500);

                        } else {
                            MyApplication.mRoomKeyLastCreate = "compatible";
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(ctx);
                        MyApplication.mRoomKeyLastCreate = "compatible";
                    }
                });
    }

    public static void interMucChat(Context ctx, String roomJid, String roomName) {
        Intent intent = new Intent(ctx, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        ctx.startActivity(intent);

        // 更新群组页面
        MucgroupUpdateUtil.broadcastUpdateUi(ctx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_group);
        initActionBar();
        initView();
        loadData(true);
        addListener();

        PermissionUtil.autoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
            MyApplication.getInstance().getBdLocationHelper().requestLocation(() -> {
                initData();
            });
        }, () -> {
            finish();
        }, PermissionUtil.getLocationPermissions());
        MapHelper.getInstance().requestLocationOn(this, REQUEST_CODE_LOCATION);
    }

    private void initData() {
        MyApplication.getInstance().getBdLocationHelper().requestLocation(() -> {
            loadData(true);
        });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.near_group));
    }

    private void initView() {
        mPager = findViewById(R.id.rv_pager);
        roomAdapter = new RoomAdapter(mucRooms);
        mPager.setLayoutManager(new LinearLayoutManager(NearGroupActivity.this));
        mPager.setAdapter(roomAdapter);
        mRefreshLayout = findViewById(R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            if (!isLoad) {
                loadData(true);
            } else {
                mRefreshLayout.finishRefresh();
            }
        });

    }

    private void addListener() {
        mPager.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                //当前RecyclerView显示出来的最后一个的item的position
                int lastPosition = -1;

                //当前状态为停止滑动状态SCROLL_STATE_IDLE时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager instanceof LinearLayoutManager) {
                        lastPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                    }
                    // 时判断界面显示的最后item的position是否等于itemCount总数-1也就是最后一个item的position
                    // 如果相等则说明已经滑动到最后了
                    if (lastPosition == recyclerView.getLayoutManager().getItemCount() - 1) {
                        if (!noMore && !isLoad) {
                            loadData(false);
                            pagerIndex++;
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }
        });
    }

    private void loadData(final boolean clear) {
        isLoad = true;
        if (clear) {
            pagerIndex = 0;
            noMore = false;
        }

        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();

        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", Integer.toString(pagerIndex));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("latitude", String.valueOf(latitude));
        params.put("longitude", String.valueOf(longitude));

        HttpUtils.get().url(coreManager.getConfig().TRILL_NEAR_ROOM)
                .params(params)
                .build()
                .execute(new ListCallback<NearRoomItemBean>(NearRoomItemBean.class) {
                    @Override
                    public void onResponse(ArrayResult<NearRoomItemBean> result) {
                        isLoad = false;
                        if (clear) {
                            mucRooms.clear();
                        }
                        DialogHelper.dismissProgressDialog();
                        noMore = true;
                        if (Result.checkSuccess(NearGroupActivity.this, result)) {
                            List<NearRoomItemBean> roomItemBeans = result.getData();
                            if (roomItemBeans != null && roomItemBeans.size() > 0) {
                                Log.e("zx", "onResponse: " + roomItemBeans.size());
                                mucRooms.addAll(roomItemBeans);
                                if (roomItemBeans.size() == pageSize) {
                                    noMore = false;
                                }
                            }
                        }
                        roomAdapter.notifyDataSetChanged();
                        mRefreshLayout.finishRefresh();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        isLoad = false;
                        DialogHelper.dismissProgressDialog();
                        mRefreshLayout.finishRefresh();
                        ToastUtil.showErrorNet(NearGroupActivity.this);
                    }
                });
    }

    static class RoomViewHolder extends BaseRecViewHolder {
        public View rootView;
        public HeadView ivGroupHead;
        public TextView tvName;
        public TextView tvNum;
        public TextView tvDesc;
        public TextView tvJoin;
        public TextView tvDistant;

        public RoomViewHolder(View itemView) {
            super(itemView);
            this.rootView = itemView;
            this.ivGroupHead = rootView.findViewById(R.id.ivGroupHead);
            this.tvName = rootView.findViewById(R.id.tvName);
            this.tvNum = rootView.findViewById(R.id.tvNum);
            this.tvDesc = rootView.findViewById(R.id.tvDesc);
            this.tvJoin = rootView.findViewById(R.id.tvJoin);
            this.tvDistant = rootView.findViewById(R.id.tvDistant);
        }
    }

    public class RoomAdapter extends BaseRecAdapter<NearRoomItemBean, RoomViewHolder> {
        public RoomAdapter(List<NearRoomItemBean> list) {
            super(list);
        }

        @Override
        public void onHolder(RoomViewHolder holder, NearRoomItemBean bean, int position) {
            AvatarHelper.getInstance().displayGain(bean.getId(), bean.getJid(), holder.ivGroupHead);
            holder.tvName.setText(bean.getName());
            holder.tvNum.setText(MyApplication.getContext().getResources().getString(R.string.people_sums, bean.getUserSize()));
            holder.tvDesc.setText(bean.getDesc());
            holder.tvJoin.setVisibility(bean.isInGroup() ? View.INVISIBLE : View.VISIBLE);
            ButtonColorChange.rechargeChange(mContext, holder.tvJoin, R.drawable.near_bg);
            String distance = DisplayUtil.getDistanceRoom(MyApplication.getInstance().getBdLocationHelper().getLatitude(),
                    MyApplication.getInstance().getBdLocationHelper().getLongitude(), bean);
            if (TextUtils.isEmpty(distance)) {
                holder.tvDistant.setVisibility(View.INVISIBLE);
            }
            holder.tvDistant.setText(distance);

            holder.rootView.setOnClickListener(view -> {
                Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), bean.getJid());
                if (friend != null) {
                    interMucChat(mContext, friend.getUserId(), friend.getNickName());
                }
            });
            holder.tvJoin.setOnClickListener(view -> goToJoin(mContext, bean, this));
        }

        @Override
        public RoomViewHolder onCreateHolder() {
            return new RoomViewHolder(getViewByRes(R.layout.room_item));
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            initData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
