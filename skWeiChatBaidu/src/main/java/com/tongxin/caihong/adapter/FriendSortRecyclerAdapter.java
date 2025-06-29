package com.tongxin.caihong.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.multi.RoomInfoActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.R;

import java.util.List;

public class FriendSortRecyclerAdapter extends RecyclerView.Adapter<FriendSortRecyclerAdapter.ViewHolder> implements SectionIndexer {

    private Context mContext;
    private List<BaseSortModel<Friend>> mSortFriends;
    private OnItemClickListener onItemClickListener;
    private boolean isShowCheckBox;

    public FriendSortRecyclerAdapter(Context context, List<BaseSortModel<Friend>> sortFriends, OnItemClickListener onItemClickListener) {
        mContext = context;
        mSortFriends = sortFriends;
        this.onItemClickListener = onItemClickListener;
    }

    public void setData(List<BaseSortModel<Friend>> sortFriends) {
        mSortFriends = sortFriends;
        notifyDataSetChanged();
    }

    public void showCheckBox() {
        isShowCheckBox = true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mSortFriends.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ViewHolder.createViewHolder(mContext, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                onItemClickListener.onItemClick(position);
            });
        }
        BaseSortModel<Friend> model = mSortFriends.get(position);
        holder.apply(this, isShowCheckBox, model, position);
    }

    @Override
    public Object[] getSections() {
        return null;
    }

    /**
     * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
     */
    public int getPositionForSection(int section) {
        for (int i = 0; i < getItemCount(); i++) {
            String sortStr = mSortFriends.get(i).getFirstLetter();
            char firstChar = sortStr.toUpperCase().charAt(0);
            if (firstChar == section) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据ListView的当前位置获取分类的首字母的Char ascii值
     */
    public int getSectionForPosition(int position) {
        return mSortFriends.get(position).getFirstLetter().charAt(0);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView catagoryTitleTv = itemView.findViewById(R.id.catagory_title);
        public ImageView avatar_img_divices = itemView.findViewById(R.id.avatar_img_divices);
        public ImageView avatar_img = itemView.findViewById(R.id.avatar_img);
        public HeadView avatar_imgS = itemView.findViewById(R.id.avatar_imgS);
        public TextView nick_name_tv = itemView.findViewById(R.id.nick_name_tv);
        public CheckBox checkBox = itemView.findViewById(R.id.cb_instant);

        public static ViewHolder createViewHolder(Context ctx, @NonNull ViewGroup parent) {
            View itemView = LayoutInflater.from(ctx).inflate(R.layout.row_sort_friend, parent, false);
            return new ViewHolder(itemView);
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void apply(SectionIndexer sectionIndexer, boolean isShowCheckBox, BaseSortModel<Friend> model, int position) {
            Context mContext = itemView.getContext();
            final Friend friend = model.getBean();
            // 根据position获取分类的首字母的Char ascii值
            int section = sectionIndexer.getSectionForPosition(position);
            // 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == sectionIndexer.getPositionForSection(section)) {
                catagoryTitleTv.setVisibility(View.VISIBLE);
                catagoryTitleTv.setText(model.getFirstLetter());
            } else {
                catagoryTitleTv.setVisibility(View.GONE);
            }

            if (friend.getRoomFlag() == 0) {// 单人
                avatar_img.setVisibility(View.VISIBLE);
                avatar_imgS.setVisibility(View.GONE);
                if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)) {
                    avatar_img.setImageResource(R.drawable.im_notice);
                } else if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {
                    avatar_img.setImageResource(R.drawable.im_new_friends);
                } else if (friend.getIsDevice() == 1) {
                    avatar_img.setVisibility(View.GONE);
                    avatar_img_divices.setVisibility(View.VISIBLE);
                    if ("android".equals(friend.getUserId())) {
                        avatar_img_divices.setImageResource(R.mipmap.my_android_icon);
                    } else if ("ios".equals(friend.getUserId())) {
                        avatar_img_divices.setImageResource(R.mipmap.my_iphone_icon);
                    } else if ("mac".equals(friend.getUserId())) {
                        avatar_img_divices.setImageResource(R.mipmap.my_mac_computer_icon);
                    } else if ("web".equals(friend.getUserId())) {
                        avatar_img_divices.setImageResource(R.mipmap.my_web_icon);
                    } else if ("pc".equals(friend.getUserId())) {
                        avatar_img_divices.setImageResource(R.mipmap.my_windows_icon);
                    }
                } else {
                    avatar_img.setVisibility(View.VISIBLE);
                    AvatarHelper.getInstance().displayAvatar(friend.getUserId(), avatar_img, true);
                }
            } else {// 群组
                avatar_img.setVisibility(View.GONE);
                avatar_imgS.setVisibility(View.VISIBLE);
                AvatarHelper.getInstance().displayAvatar(CoreManager.requireSelf(mContext).getUserId(), friend, avatar_imgS);
            }

            // 昵称
            nick_name_tv.setText(friend.getShowName());
            // 个性签名
            // des_tv.setText(friend.getDescription());

            // 点击头像跳转详情
            avatar_img.setOnClickListener(view -> {
                if (!UiUtils.isNormalClick(view)) {
                    return;
                }
                if (friend.getRoomFlag() == 0) {  // 单人
                    if (!friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)
                            && !friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)
                            && friend.getIsDevice() != 1) {
                        Intent intent = new Intent(mContext, BasicInfoActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                        mContext.startActivity(intent);
                    }
                } else {  // 群组
                    Intent intent = new Intent(mContext, RoomInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                    mContext.startActivity(intent);
                }
            });

            // 点击头像跳转详情
            avatar_imgS.setOnClickListener(view -> {
                Intent intent = new Intent(mContext, RoomInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
                mContext.startActivity(intent);
            });

            // 转发时支持多选
            checkBox.setVisibility(isShowCheckBox ? View.VISIBLE : View.GONE);
            if (friend.isCheck()) {
                checkBox.setChecked(true);
                Drawable drawable = mContext.getResources().getDrawable(R.drawable.sel_check_wx2);
                drawable = DrawableCompat.wrap(drawable);
                ColorStateList tabColor = SkinUtils.getSkin(mContext).getTabColorState();
                DrawableCompat.setTintList(drawable, tabColor);
                checkBox.setButtonDrawable(drawable);
            } else {
                checkBox.setChecked(false);
                checkBox.setButtonDrawable(mContext.getResources().getDrawable(R.drawable.sel_nor_wx2));
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
