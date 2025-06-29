package com.tongxin.caihong.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发送名片
 */
public class SelectCardPopupWindow extends PopupWindow {
    private View mMenuView;
    private EditText etSearch;
    private ListView mListView;
    private ListViewAdapter mAdapter;
    private List<BaseSortModel<Friend>> oDatas = new ArrayList<>();
    private List<BaseSortModel<Friend>> mCuttDatas = new ArrayList<>();
    private Context mContext;
    private SendCardS mSendCards;
    private Map<String, Friend> mSelectPositions = new HashMap<>();

    public SelectCardPopupWindow(FragmentActivity context, SendCardS SendCards) {
        super(context);
        this.mContext = context;
        this.mSendCards = SendCards;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.pop_send_card, null);

        //设置SelectRoomMemberPopupWindow的View
        this.setContentView(mMenuView);
        //设置SelectRoomMemberPopupWindow弹出窗体的宽
        this.setWidth(LayoutParams.MATCH_PARENT);
        //设置SelectRoomMemberPopupWindow弹出窗体的高
        this.setHeight(LayoutParams.MATCH_PARENT);
        //设置SelectRoomMemberPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectRoomMemberPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);
        //实例化一个SelectRoomMemberPopupWindow颜色为白色
        ColorDrawable dw = new ColorDrawable(context.getResources().getColor(R.color.app_white));
        //设置SelectRoomMemberPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);

        initActionBar();
        initView();
    }

    private void initActionBar() {
        mMenuView.findViewById(R.id.iv_title_left).setOnClickListener(view -> dismiss());
        TextView tvTitle = mMenuView.findViewById(R.id.tv_title_center);
        tvTitle.setText(mContext.getString(R.string.select_contacts));
        TextView tvTitleRight = mMenuView.findViewById(R.id.tv_title_right);
        tvTitleRight.setText(mContext.getString(R.string.sure));
        tvTitleRight.setOnClickListener(view -> {
            dismiss();
            List<Friend> mFriendCardList = new ArrayList<>();
            for (String key : mSelectPositions.keySet()) {
                Friend friend = mSelectPositions.get(key);
                mFriendCardList.add(friend);
            }
            mSendCards.sendCardS(mFriendCardList);
        });
    }

    private void initView() {
        etSearch = mMenuView.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mCuttDatas.clear();
                String text = etSearch.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    mCuttDatas.addAll(oDatas);
                } else {
                    for (int i = 0; i < oDatas.size(); i++) {
                        String name = !TextUtils.isEmpty(oDatas.get(i).getBean().getRemarkName()) ? oDatas.get(i).getBean().getRemarkName() : oDatas.get(i).getBean().getNickName();
                        if (name.contains(text)) {
                            // 符合搜索条件的好友
                            mCuttDatas.add(oDatas.get(i));
                        }
                    }
                }
                mAdapter.setData(mCuttDatas);
            }
        });

        mListView = mMenuView.findViewById(R.id.list_view);
        mAdapter = new ListViewAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener((adapterView, view, position, l) -> {
            if (hasSelected(position)) {
                removeSelect(position);
            } else {
                addSelect(position);
            }
        });

        SideBar sideBar = mMenuView.findViewById(R.id.sidebar);
        sideBar.setTextView(mMenuView.findViewById(R.id.text_dialog));
        sideBar.setOnTouchingLetterChangedListener(s -> {
            // 该字母首次出现的位置
            int position = mAdapter.getPositionForSection(s.charAt(0));
            if (position != -1) {
                mListView.setSelection(position);
            }
        });

        AsyncUtils.doAsync(this, contextAsyncContext -> {
            User self = CoreManager.requireSelf(mContext);
            List<Friend> friends = FriendDao.getInstance().getAllFriends(self.getUserId());
            // 自己也可选
            Friend friend = new Friend();
            friend.setUserId(self.getUserId());
            friend.setNickName(self.getNickName());
            friends.add(friend);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            AsyncUtils.runOnUiThread(this, context -> {
                DialogHelper.dismissProgressDialog();
                sideBar.setExistMap(existMap);
                oDatas = sortedList;
                mCuttDatas.addAll(oDatas);
                mAdapter.setData(sortedList);
            });
        });
    }

    private void addSelect(int position) {
        if (!hasSelected(position)) {
            Friend friend = mCuttDatas.get(position).getBean();
            mSelectPositions.put(friend.getUserId(), friend);
            mAdapter.notifyDataSetInvalidated();
        }
    }

    private boolean hasSelected(int position) {
        boolean b = false;
        Friend friend = mCuttDatas.get(position).getBean();
        if (mSelectPositions.containsKey(friend.getUserId())) {
            b = true;
        }
        return b;
    }

    private void removeSelect(int position) {
        Friend friend = mCuttDatas.get(position).getBean();
        mSelectPositions.put(friend.getUserId(), friend);
        if (mSelectPositions.containsKey(friend.getUserId())) {
            mSelectPositions.remove(friend.getUserId());
        }
        mAdapter.notifyDataSetInvalidated();
    }

    public interface SendCardS {
        void sendCardS(List<Friend> friends);
    }

    private class ListViewAdapter extends BaseAdapter implements SectionIndexer {
        private List<BaseSortModel<Friend>> data;

        public ListViewAdapter() {
            data = new ArrayList<>();
        }

        public void setData(List<BaseSortModel<Friend>> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_contacts, parent, false);
            }
            TextView sortTv = ViewHolder.get(convertView, R.id.catagory_title);
            View view = ViewHolder.get(convertView, R.id.view_bg_friend);
            CheckBox checkBox = ViewHolder.get(convertView, R.id.check_box);
            ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
            TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);

            sortTv.setVisibility(View.GONE);
            view.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            Friend mFriend = data.get(position).getBean();
            if (mFriend != null) {
                AvatarHelper.getInstance().displayAvatar(mFriend.getUserId(), avatarImg, true);
                userNameTv.setText(!TextUtils.isEmpty(mFriend.getRemarkName()) ? mFriend.getRemarkName() : mFriend.getNickName());
                checkBox.setChecked(false);
                checkBox.setButtonDrawable(mContext.getResources().getDrawable(R.drawable.sel_nor_wx2));
                if (mSelectPositions.containsKey(mFriend.getUserId())) {
                    checkBox.setChecked(true);
                    ButtonColorChange.checkChange(mContext, checkBox);
                }
            }
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
                String sortStr = data.get(i).getFirstLetter();
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
            return data.get(position).getFirstLetter().charAt(0);
        }
    }
}
