package com.tongxin.caihong.ui.message.multi;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.RecyclerViewLoadMoreHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.HeadView;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.view.NoLastDividerItemDecoration;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class RoomReadListFragment extends EasyFragment implements RecyclerViewLoadMoreHelper.OnLoadDataListener<RoomReadListFragment.Item> {
    private RecyclerViewLoadMoreHelper<Item> loadMoreHelper = new RecyclerViewLoadMoreHelper<>();
    private SmartRefreshLayout refreshLayout;

    private RecyclerView recyclerView;
    private Adapter adapter;
    private String roomId;
    private String roomJid;
    private String packetId;
    private int isRead;

    public static RoomReadListFragment create(int isRead) {
        RoomReadListFragment f = new RoomReadListFragment();
        Bundle b = new Bundle();
        b.putInt("isRead", isRead);
        f.setArguments(b);
        return f;
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_room_read_list;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        packetId = requireActivity().getIntent().getStringExtra("packetId");
        roomId = requireActivity().getIntent().getStringExtra("roomId");
        roomJid = requireActivity().getIntent().getStringExtra("roomJid");
        isRead = getArguments().getInt("isRead");
        initView();
    }

    @Override
    public void onLoadData(int pageIndex, int pageSize, RecyclerViewLoadMoreHelper.OnPageLoadedListener<Item> onPageLoadedListener) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomJid", roomJid);
        params.put("messageId", packetId);
        params.put("isRead", String.valueOf(isRead));
        params.put("pageIndex", String.valueOf(pageIndex));
        params.put("pageSize", String.valueOf(pageSize));

        HttpUtils.get().url(coreManager.getConfig().ROOM_MESSAGE_READ_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoomMember>(MucRoomMember.class) {

                    @Override
                    public void onResponse(ArrayResult<MucRoomMember> result) {
                        if (Result.checkSuccess(requireContext(), result)) {
                            List<Item> data = new ArrayList<>();
                            List<MucRoomMember> list = result.getData();
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            for (MucRoomMember user : list) {
                                Item item = new Item();
                                item.userId = user.getUserId();
                                item.nickname = user.getNickName();
                                item.hiding = user.getHiding();
                                data.add(item);
                            }
                            onPageLoadedListener.onPageLoaded(data);
                            refreshLayout.finishRefresh();
                        } else {
                            refreshLayout.finishRefresh();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(requireContext());
                        refreshLayout.finishRefresh();
                    }
                });
    }

    private void initView() {
        refreshLayout = findViewById(R.id.refreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        NoLastDividerItemDecoration id = new NoLastDividerItemDecoration(requireContext(), NoLastDividerItemDecoration.VERTICAL);
        id.setDrawable(requireContext().getDrawable(R.drawable.friend_divider));
        recyclerView.addItemDecoration(id);
        adapter = new Adapter(getListener());
        recyclerView.setAdapter(adapter);
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            loadMoreHelper.initData();
        });
        loadMoreHelper.setOnLoadDataListener(this);
        loadMoreHelper.setAdapter(adapter);
        loadMoreHelper.initRecyclerView(recyclerView);
        loadMoreHelper.initData();
    }

    @Nullable
    private OnItemClickListener getListener() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnItemClickListener) {
            return (OnItemClickListener) parentFragment;
        }
        Activity activity = getActivity();
        if (activity instanceof OnItemClickListener) {
            return (OnItemClickListener) activity;
        }
        return null;
    }

    interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public static class Item {
        String nickname;
        String userId;
        int hiding;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public HeadView ivInco;
        public TextView tvName;
        public TextView tvTime;
        public TextView tvTimeDuring;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.findViewById(R.id.num_tv).setVisibility(View.GONE);
            itemView.findViewById(R.id.not_push_iv).setVisibility(View.GONE);
            itemView.findViewById(R.id.replay_iv).setVisibility(View.GONE);

            ivInco = (HeadView) itemView.findViewById(R.id.avatar_imgS);
            tvName = (TextView) itemView.findViewById(R.id.nick_name_tv);
            tvTime = (TextView) itemView.findViewById(R.id.content_tv);
            tvTimeDuring = (TextView) itemView.findViewById(R.id.time_tv);
            ivInco.setVisibility(View.VISIBLE);
            tvTime.setVisibility(View.GONE);
            tvTimeDuring.setVisibility(View.GONE);
        }

        public void apply(Item item) {
            if (item.hiding == 1) {
                ivInco.getHeadImage().setImageResource(Constants.HIDING_AVATAR);
            } else {
                AvatarHelper.getInstance().displayAvatar(item.nickname, item.userId, ivInco.getHeadImage(), true);
            }
            tvName.setText(item.nickname);
        }
    }

    static class Adapter extends RecyclerView.Adapter<ViewHolder> implements RecyclerViewLoadMoreHelper.Adapter<Item> {
        private List<Item> data = new ArrayList<>();
        private OnItemClickListener onItemClickListener;

        Adapter(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        @Override
        public void setData(List<Item> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        public void addAll(List<Item> data) {
            int oldSize = this.data.size();
            this.data.addAll(data);
            notifyItemRangeInserted(oldSize, data.size());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_nearly_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = data.get(position);
            if (onItemClickListener != null) {
                holder.itemView.setOnClickListener(v -> {
                    onItemClickListener.onItemClick(item);
                });
            }
            holder.apply(item);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
