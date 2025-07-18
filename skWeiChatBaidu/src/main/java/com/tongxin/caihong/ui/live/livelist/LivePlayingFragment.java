package com.tongxin.caihong.ui.live.livelist;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.db.InternationalizationHelper;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.live.LiveConstants;
import com.tongxin.caihong.ui.live.LivePlayingActivity;
import com.tongxin.caihong.ui.live.PushFlowActivity;
import com.tongxin.caihong.ui.live.bean.LiveRoom;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.ViewHolder;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 正在直播的列表
 */
public class LivePlayingFragment extends EasyFragment {
    private PullToRefreshListView mPullToRefreshListView;
    private List<LiveRoom> mMucRoomS;
    private LiveRoomAdapter mAdapter;
    private String mAccessToken;
    private String mLoginUserId;
    private int mPageIndex = 0;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MucgroupUpdateUtil.ACTION_UPDATE)) {
                requestData(true);
            }
        }
    };

    public LivePlayingFragment() {
        mMucRoomS = new ArrayList<>();
        mAdapter = new LiveRoomAdapter();
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.layout_address;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requestData(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
    }

    @SuppressLint("InflateParams")
    private void initView() {
        mAccessToken = coreManager.getSelfStatus().accessToken;
        mLoginUserId = coreManager.getSelf().getUserId();
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        mPullToRefreshListView.setAdapter(mAdapter);
        View emptyView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_list_empty_view, null);
        mPullToRefreshListView.setEmptyView(emptyView);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                requestData(true);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                requestData(false);
            }
        });

        mPullToRefreshListView.getRefreshableView().setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (UiUtils.isNormalClick()) {// 防止过快点击
                            LiveRoom room = mMucRoomS.get((int) id);
                            if (String.valueOf(room.getUserId()).equals(mLoginUserId)) {// 开启直播
                                gotoLiveRoom(room, true);
                            } else { // 进入直播间
                                gotoLiveRoom(room, false);
                            }
                        }
                    }
                });

        getActivity().registerReceiver(mUpdateReceiver, MucgroupUpdateUtil.getUpdateActionFilter());

        requestData(true);
    }

    // 获取正在直播的直播间列表
    private void requestData(final boolean isPullDwonToRefersh) {
        if (isPullDwonToRefersh) {
            mPageIndex = 0;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", mAccessToken);
        params.put("pageIndex", String.valueOf(mPageIndex));
        params.put("pageSize", String.valueOf(AppConfig.PAGE_SIZE));
        params.put("status", "1");
        HttpUtils.get().url(coreManager.getConfig().GET_LIVE_ROOM_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<LiveRoom>(LiveRoom.class) {
                    @Override
                    public void onResponse(ArrayResult<LiveRoom> result) {
                        mPageIndex++;
                        if (isPullDwonToRefersh) {
                            mMucRoomS.clear();
                        }
                        List<LiveRoom> data = result.getData();
                        if (data != null && data.size() > 0) {
                            mMucRoomS.addAll(data);
                        }
                        mAdapter.notifyDataSetChanged();
                        mPullToRefreshListView.onRefreshComplete();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                        mPullToRefreshListView.onRefreshComplete();
                    }
                });
    }

    private void gotoLiveRoom(final LiveRoom room, final boolean liver) {
        DialogHelper.showDefaulteMessageProgressDialog(getActivity());
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", room.getRoomId());
        params.put("userId", mLoginUserId);
        params.put("status", "1");

        HttpUtils.get().url(coreManager.getConfig().JOIN_LIVE_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (liver) {
                                Intent intent = new Intent(getActivity(), PushFlowActivity.class);
                                intent.putExtra(LiveConstants.LIVE_PUSH_FLOW_URL, room.getUrl());
                                intent.putExtra(LiveConstants.LIVE_ROOM_ID, room.getRoomId());
                                intent.putExtra(LiveConstants.LIVE_CHAT_ROOM_ID, room.getJid());
                                intent.putExtra(LiveConstants.LIVE_ROOM_NAME, room.getName());
                                intent.putExtra(LiveConstants.LIVE_ROOM_PERSON_ID, String.valueOf(room.getUserId()));
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(getActivity(), LivePlayingActivity.class);
                                intent.putExtra(LiveConstants.LIVE_GET_FLOW_URL, room.getUrl());
                                intent.putExtra(LiveConstants.LIVE_ROOM_ID, room.getRoomId());
                                intent.putExtra(LiveConstants.LIVE_CHAT_ROOM_ID, room.getJid());
                                intent.putExtra(LiveConstants.LIVE_ROOM_NAME, room.getName());
                                intent.putExtra(LiveConstants.LIVE_ROOM_PERSON_ID, String.valueOf(room.getUserId()));
                                intent.putExtra(LiveConstants.LIVE_STATUS, room.getStatus());
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(getActivity(), InternationalizationHelper.getString("KICKED_NOT_IN"), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    // 删除直播间
    public void deleteRoom(String roomId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", mAccessToken);
        params.put("roomId", roomId);
        HttpUtils.get().url(coreManager.getConfig().DELETE_LIVE_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        Toast.makeText(getActivity(), InternationalizationHelper.getString("JXAlert_DeleteOK"), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    class LiveRoomAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMucRoomS.size();
        }

        @Override
        public Object getItem(int position) {
            return mMucRoomS.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.row_live_room, parent, false);
            }
            final LiveRoom room = mMucRoomS.get(position);
            ImageView live_default = ViewHolder.get(convertView, R.id.live_default);
            ImageView avatar_img = ViewHolder.get(convertView, R.id.live_avatar_img);
            AvatarHelper.getInstance().displayAvatar(String.valueOf(room.getUserId()), live_default, false);
            AvatarHelper.getInstance().displayAvatar(String.valueOf(room.getUserId()), avatar_img, false);
            TextView tv1 = ViewHolder.get(convertView, R.id.live_title);
            TextView tv2 = ViewHolder.get(convertView, R.id.live_nick_name);
            //TextView tv4 = ViewHolder.get(convertView, R.id.live_notice);
            TextView tv5 = ViewHolder.get(convertView, R.id.islive);
            tv1.setText(room.getName());
            tv2.setText(room.getNickName());
            //tv4.setText(room.getNotice());
            avatar_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (String.valueOf(room.getUserId()).equals(mLoginUserId)) {
                        deleteRoom(room.getRoomId());
                    }
                }
            });
            return convertView;
        }
    }
}
