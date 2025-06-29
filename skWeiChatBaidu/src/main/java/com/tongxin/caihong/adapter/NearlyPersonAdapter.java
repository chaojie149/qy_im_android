package com.tongxin.caihong.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.NearRoomItemBean;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.NearPersonActivity;
import com.tongxin.caihong.ui.nearby.NearGroupActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;

import java.util.List;

/**
 * create by zq for shuaba
 */
public class NearlyPersonAdapter extends RecyclerView.Adapter<NearlyPersonAdapter.NearlyViewHolder> {
    private Context ctx;
    private List<User> userList;
    private List<NearRoomItemBean> roomList;
    private boolean isRoom;

    public static NearlyPersonAdapter createFromUserList(Context ctx, List<User> data) {
        NearlyPersonAdapter adapter = new NearlyPersonAdapter(ctx);
        adapter.userList = data;
        if (data.size() == 20) {
            // 数据满足一页，添加更多按钮
            data.add(null);
        }
        return adapter;
    }

    public static NearlyPersonAdapter createFromRoomList(Context ctx, List<NearRoomItemBean> data) {
        NearlyPersonAdapter adapter = new NearlyPersonAdapter(ctx);
        adapter.roomList = data;
        adapter.isRoom = true;
        if (data.size() == 20) {
            // 数据满足一页，添加更多按钮
            data.add(null);
        }
        return adapter;
    }

    public NearlyPersonAdapter(Context ctx) {
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public NearlyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NearlyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_nearly_person, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NearlyViewHolder holder, int position) {
        if (position == 20) {
            // 更多
            holder.ivAvatar.getHeadImage().setImageResource(R.mipmap.ic_new_nearly_more);
            holder.tvName.setTextSize(14);
            holder.tvDistant.setVisibility(View.GONE);
            holder.tvName.setText(ctx.getString(R.string.more));
        } else {
            holder.tvName.setTextSize(12);
            holder.tvDistant.setVisibility(View.VISIBLE);

            if (isRoom) {
                NearRoomItemBean room = roomList.get(position);
                AvatarHelper.getInstance().displayGain(room.getId(), room.getJid(), holder.ivAvatar);

                holder.tvName.setText(room.getName());
                String distance = DisplayUtil.getDistanceRoom(MyApplication.getInstance().getBdLocationHelper().getLatitude(),
                        MyApplication.getInstance().getBdLocationHelper().getLongitude(), room);
                holder.tvDistant.setText(distance);
            } else {
                User user = userList.get(position);
                if (user.getHiding() == 1) {
                    holder.ivAvatar.getHeadImage().setImageResource(Constants.HIDING_AVATAR);
                } else {
                    AvatarHelper.getInstance().displayAvatar(user.getNickName(), user.getUserId(), holder.ivAvatar.getHeadImage(), true);
                }
                holder.tvName.setText(user.getNickName());
                String distance = DisplayUtil.getDistance(MyApplication.getInstance().getBdLocationHelper().getLatitude(), MyApplication.getInstance().getBdLocationHelper().getLongitude(), user);
                holder.tvDistant.setText(distance);
            }
        }
        holder.ivAvatar.setOnClickListener(view -> {
            if (position == 20) {
                // 更多
                if (isRoom) {
                    ctx.startActivity(new Intent(ctx, NearGroupActivity.class));
                } else {
                    ctx.startActivity(new Intent(ctx, NearPersonActivity.class));
                }
            } else {
                if (isRoom) {
                    NearRoomItemBean item = roomList.get(position);
                    Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), item.getJid());
                    if (friend != null) {
                        NearGroupActivity. interMucChat(ctx, friend.getUserId(), friend.getNickName());
                    } else {
                        NearGroupActivity.goToJoin(ctx, item, null);
                    }
                } else {
                    BasicInfoActivity.start(ctx, userList.get(position).getUserId());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return isRoom ? roomList.size() : userList.size();
    }

    class NearlyViewHolder extends RecyclerView.ViewHolder {
        HeadView ivAvatar;
        TextView tvName;
        TextView tvDistant;

        public NearlyViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvDistant = itemView.findViewById(R.id.tvDistant);
        }
    }
}
