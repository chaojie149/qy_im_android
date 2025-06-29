package com.tongxin.caihong.ui.message;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.view.HorizontalListView;

import java.util.List;

/**
 * 转发多选
 */
public class InstantMessageConfirmNew extends PopupWindow {
    private Context mContext;
    private View mMenuView;
    private HorizontalListView mHorizontalListView;
    private HorListViewAdapter mHorAdapter;
    private TextView mSend, mCancle;

    private List<Friend> friendList;

    public InstantMessageConfirmNew(Activity context, View.OnClickListener itemsOnClick, List<Friend> friend) {
        super(context);
        this.mContext = context;
        this.friendList = friend;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.message_instantconfirm_new, null);
        mHorAdapter = new HorListViewAdapter();

        mHorizontalListView = (HorizontalListView) mMenuView.findViewById(R.id.horizontal_list_view);
        mHorizontalListView.setAdapter(mHorAdapter);

        mSend = (TextView) mMenuView.findViewById(R.id.btn_send);
        ButtonColorChange.textChange(mContext, mSend);
        mCancle = (TextView) mMenuView.findViewById(R.id.btn_cancle);
        mSend.setOnClickListener(itemsOnClick);
        mCancle.setOnClickListener(itemsOnClick);
        // 设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        // 设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        // 设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LinearLayout.LayoutParams.MATCH_PARENT);
        // 设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        // 设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0xb0000000);
        // 设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        // mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mMenuView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    } else if (y > bottom) {
                        dismiss();
                    }
                }
                return true;
            }
        });
    }

    private static class ViewHolder {
        HeadView ivHeadView;
        TextView tvName;
    }

    private class HorListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return friendList.size();
        }

        @Override
        public Object getItem(int position) {
            return friendList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_message_instantconfirm_new, parent, false);
                holder.ivHeadView = convertView.findViewById(R.id.ivHeadView);
                holder.tvName = convertView.findViewById(R.id.tvName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Friend friend = friendList.get(position);
            AvatarHelper.getInstance().displayAvatar(CoreManager.requireSelf(mContext).getUserId(), friend, holder.ivHeadView);
            holder.tvName.setText(friend.getShowName());
            return convertView;
        }
    }
}
