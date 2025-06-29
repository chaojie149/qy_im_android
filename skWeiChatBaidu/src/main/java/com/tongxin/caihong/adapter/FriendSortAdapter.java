package com.tongxin.caihong.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SectionIndexer;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.sortlist.BaseSortModel;

import java.util.List;

public class FriendSortAdapter extends BaseAdapter implements ListAdapter, SectionIndexer {

    private Context mContext;
    private List<BaseSortModel<Friend>> mSortFriends;
    private boolean isShowCheckBox;

    public FriendSortAdapter(Context context, List<BaseSortModel<Friend>> sortFriends) {
        mContext = context;
        mSortFriends = sortFriends;
    }

    public List<BaseSortModel<Friend>> getData() {
        return mSortFriends;
    }

    public void setData(List<BaseSortModel<Friend>> sortFriends) {
        mSortFriends = sortFriends;
        notifyDataSetChanged();
    }

    public void showCheckBox() {
        isShowCheckBox = true;
    }

    @Override
    public int getCount() {
        return mSortFriends.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            FriendSortRecyclerAdapter.ViewHolder holder = FriendSortRecyclerAdapter.ViewHolder.createViewHolder(mContext, parent);
            convertView = holder.itemView;
            convertView.setTag(holder);
        }
        FriendSortRecyclerAdapter.ViewHolder holder = (FriendSortRecyclerAdapter.ViewHolder) convertView.getTag();
        holder.apply(this, isShowCheckBox, mSortFriends.get(position), position);
        return convertView;
    }

    @Override
    public Object[] getSections() {
        return null;
    }

    /**
     * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
     */
    public int getPositionForSection(int section) {
        for (int i = 0; i < getCount(); i++) {
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
}
