package com.tongxin.caihong.ui.message.multi;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.SelectionFrame;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * Features: 展示群成员
 * Features  禁言 && 删除群成员
 * Features: 群主对群内成员备注
 * <p>
 * 因为管理员也可以进入该界面进行前三种操作，且管理员需要显示userName 群主显示cardName 所以需要区分下
 */
public class BlackListActivity extends BaseActivity {
    private EditText mEditText;
    private boolean isSearch;

    private PullToRefreshListView mListView;
    private GroupMoreFeaturesAdapter mAdapter;
    private List<BaseSortModel<RoomMember>> mSortRoomMember;
    private List<BaseSortModel<RoomMember>> mSearchSortRoomMember;
    private boolean sortMode = true;

    private SideBar mSideBar;
    private TextView mTextDialog;

    private String mRoomId;

    private RoomMember mRoomMember;

    private Map<String, String> mRemarksMap = new HashMap<>();
    private List<String> toBlackList = new ArrayList<>();

    public static void start(Context ctx, String roomId) {
        Intent intent = new Intent(ctx, BlackListActivity.class);
        intent.putExtra("roomId", roomId);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_all_member);
        mRoomId = getIntent().getStringExtra("roomId");

        initActionBar();
        initView();
        setTouch(true);
        initData();
        loadData();
        EventBusHelper.register(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.black_list_all);
        ImageView ivTitleRight = findViewById(R.id.iv_title_right);
        ivTitleRight.setImageResource(R.mipmap.more_icon);
        ivTitleRight.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
            intent.putExtra("roomId", mRoomId);
            intent.putExtra("isBlack", true);
            intent.putExtra("toBlackList", TextUtils.join(",", toBlackList));
            startActivity(intent);
        });
        mSideBar = findViewById(R.id.sidebar);
    }

    private void initData() {
        mSortRoomMember = new ArrayList<>();
        mSearchSortRoomMember = new ArrayList<>();
        mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(mRoomId, coreManager.getSelf().getUserId());
        List<Friend> mFriendList = FriendDao.getInstance().getAllFriendsNotFilterStatus(coreManager.getSelf().getUserId());
        for (int i = 0; i < mFriendList.size(); i++) {
            if (!TextUtils.isEmpty(mFriendList.get(i).getRemarkName())) {// 针对该好友进行了备注
                mRemarksMap.put(mFriendList.get(i).getUserId(), mFriendList.get(i).getRemarkName());
            }
        }
    }

    private void initView() {
        mListView = findViewById(R.id.pull_refresh_list);
        mListView.setMode(PullToRefreshBase.Mode.DISABLED);
        mAdapter = new GroupMoreFeaturesAdapter(mSortRoomMember);
        mListView.getRefreshableView().setAdapter(mAdapter);

        mTextDialog = findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(s -> {
            // 该字母首次出现的位置
            int position = mAdapter.getPositionForSection(s.charAt(0));
            if (position != -1) {
                mListView.getRefreshableView().setSelection(position);
            }
        });

        mEditText = findViewById(R.id.search_et);
        mEditText.setHint(getString(R.string.search));
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                isSearch = true;
                mSearchSortRoomMember.clear();
                String str = mEditText.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    isSearch = false;
                    mAdapter.setData(mSortRoomMember);
                    return;
                }
                // 本地搜索
                // 问题1，本地群成员表没有群成员的就搜索不到了
                // 接口搜索
                // 问题1，如群成员为我好友且我对该好友设备了备注，无法搜索到
                // 问题2，如我是群主且针对群成员设置了群内备注，无法搜索到
                // todo 问题3，如好友的昵称内有1，但是你对他的备注或者群内昵称没有1，搜素1这些结果也出来了。。。
                // 将两者结合起来，先本地搜索，找到匹配关键字的数据后，在调接口搜索得到结果，
                // 最后将接口搜索到的数据与本地同userId的数据干掉，在合并显示
                // todo 优化，当群总人数未满一页时，可直接调用本地搜索，不用调接口搜索
                List<BaseSortModel<RoomMember>> data = new ArrayList<>();
                for (int i = 0; i < mSortRoomMember.size(); i++) {
                    if (getName(mSortRoomMember.get(i).getBean()).contains(str)) { // 符合搜索条件的好友
                        data.add((mSortRoomMember.get(i)));
                    }
                }
                mSearchSortRoomMember.addAll(data);
                mAdapter.setData(mSearchSortRoomMember);
                // 黑名单列表只支持本地搜索
                // searchMember(mRoomId, str, data);
            }
        });

        mListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BaseSortModel<RoomMember> roomMemberModel;
                if (isSearch) {
                    roomMemberModel = mSearchSortRoomMember.get((int) id);
                } else {
                    roomMemberModel = mSortRoomMember.get((int) id);
                }
                final RoomMember roomMember = roomMemberModel.getBean();
                SelectionFrame mSF = new SelectionFrame(BlackListActivity.this);
                mSF.setSomething(null, getString(R.string.sure_remove_black_member_for_group, getName(roomMember)),
                        new SelectionFrame.OnSelectionFrameClickListener() {
                            @Override
                            public void cancelClick() {

                            }

                            @Override
                            public void confirmClick() {
                                removeBlackMember(roomMemberModel, roomMember.getUserId());
                            }
                        });
                mSF.show();
            }
        });
    }

    private void loadData() {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", mRoomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_BLACK_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoomMember>(MucRoomMember.class) {
                    @Override
                    public void onResponse(ArrayResult<MucRoomMember> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            mSortRoomMember.clear();
                            List<MucRoomMember> mucRoomMembers = result.getData();
                            for (int i = 0; i < mucRoomMembers.size(); i++) {
                                RoomMember roomMember = new RoomMember();
                                roomMember.setRoomId(mRoomId);
                                roomMember.setUserId(mucRoomMembers.get(i).getUserId());
                                roomMember.setUserName(mucRoomMembers.get(i).getNickName());
                                if (TextUtils.isEmpty(mucRoomMembers.get(i).getRemarkName())) {
                                    roomMember.setCardName(mucRoomMembers.get(i).getNickName());
                                } else {
                                    roomMember.setCardName(mucRoomMembers.get(i).getRemarkName());
                                }
                                roomMember.setRole(mucRoomMembers.get(i).getRole());
                                roomMember.setHiding(mucRoomMembers.get(i).getHiding());
                                roomMember.setCreateTime(mucRoomMembers.get(i).getCreateTime());
                                mSortRoomMember.add(SortHelper.toSortedModel(roomMember, BlackListActivity.this::getName));
                            }
                            for (BaseSortModel<RoomMember> bMember : mSortRoomMember) {
                                RoomMember member = bMember.getBean();
                                toBlackList.add(member.getUserId());
                            }
                            mAdapter.setData(mSortRoomMember);
                        }
                        mAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                    }
                });
    }

    private void removeBlackMember(BaseSortModel<RoomMember> roomMemberModel, String userId) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", mRoomId);
        params.put("userId", userId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().REMOVE_ROOM_BLACK_LIST)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, R.string.remove_black_success);
                            remove(mSortRoomMember, roomMemberModel);
                            mAdapter.notifyDataSetChanged();
                            mEditText.setText("");
                            toBlackList.clear();
                            for (BaseSortModel<RoomMember> bMember : mSortRoomMember) {
                                RoomMember member = bMember.getBean();
                                toBlackList.add(member.getUserId());
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void remove(List<BaseSortModel<RoomMember>> list, BaseSortModel<RoomMember> other) {
        Iterator<BaseSortModel<RoomMember>> ite = list.iterator();
        while (ite.hasNext()) {
            BaseSortModel<RoomMember> next = ite.next();
            if (TextUtils.equals(next.getBean().getUserId(), other.getBean().getUserId())) {
                ite.remove();
                return;
            }
        }
    }

    private String getName(RoomMember member) {
        if (mRoomMember != null
                && (mRoomMember.getRole() == 1
                || mRoomMember.getRole() == 2)) {
            if (!TextUtils.equals(member.getUserName(), member.getCardName())) {// 当userName与cardName不一致时，我们认为群主有设置群内备注
                return member.getCardName();
            } else {
                if (mRemarksMap.containsKey(member.getUserId())) {
                    return mRemarksMap.get(member.getUserId());
                } else {
                    return member.getUserName();
                }
            }
        } else {
            if (mRemarksMap.containsKey(member.getUserId())) {
                return mRemarksMap.get(member.getUserId());
            } else {
                return member.getUserName();
            }
        }
    }

    /**
     * 有人被加入黑名单了
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, EventNotifyByTag.GroupAddBlackList)) {
            loadData();
        }
    }

    class GroupMoreFeaturesAdapter extends BaseAdapter implements SectionIndexer {
        List<BaseSortModel<RoomMember>> mSortRoomMember;

        GroupMoreFeaturesAdapter(List<BaseSortModel<RoomMember>> sortRoomMember) {
            this.mSortRoomMember = new ArrayList<>();
            this.mSortRoomMember = sortRoomMember;
        }

        public void setData(List<BaseSortModel<RoomMember>> sortRoomMember) {
            this.mSortRoomMember = sortRoomMember;
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            if (mSortRoomMember == null) return 0;
            return mSortRoomMember.size();
        }

        @Override
        public Object getItem(int position) {
            return mSortRoomMember.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_room_all_member, parent, false);
            }
            TextView catagoryTitleTv = ViewHolder.get(convertView, R.id.catagory_title);
            ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
            TextView roleS = ViewHolder.get(convertView, R.id.roles);
            TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);

            if (sortMode) {
                // 根据position获取分类的首字母的Char ascii值
                int section = getSectionForPosition(position);
                // 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
                if (position == getPositionForSection(section)) {
                    catagoryTitleTv.setVisibility(View.VISIBLE);
                    catagoryTitleTv.setText(mSortRoomMember.get(position).getFirstLetter());
                } else {
                    catagoryTitleTv.setVisibility(View.GONE);
                }
            } else {
                catagoryTitleTv.setVisibility(View.GONE);
            }
            RoomMember member = mSortRoomMember.get(position).getBean();
            if (member != null) {
                AvatarHelper.getInstance().displayAvatar(getName(member), member.getUserId(), avatarImg, true);
                switch (member.getRole()) {
                    case RoomMember.ROLE_OWNER:
                        roleS.setText(getString(R.string.group_owner));
                        ViewCompat.setBackgroundTintList(roleS, ColorStateList.valueOf(getResources().getColor(R.color.color_role1)));
                        break;
                    case RoomMember.ROLE_MANAGER:
                        roleS.setText(getString(R.string.group_manager));
                        ViewCompat.setBackgroundTintList(roleS, ColorStateList.valueOf(getResources().getColor(R.color.color_role2)));
                        break;
                    case RoomMember.ROLE_MEMBER:
                        roleS.setText(getString(R.string.group_role_normal));
                        ViewCompat.setBackgroundTintList(roleS, ColorStateList.valueOf(getResources().getColor(R.color.color_role3)));
                        break;
                    case RoomMember.ROLE_INVISIBLE:
                        roleS.setText(R.string.role_invisible);
                        ViewCompat.setBackgroundTintList(roleS, ColorStateList.valueOf(getResources().getColor(R.color.color_role4)));
                        break;
                    case RoomMember.ROLE_GUARDIAN:
                        roleS.setText(R.string.role_guardian);
                        ViewCompat.setBackgroundTintList(roleS, ColorStateList.valueOf(getResources().getColor(R.color.color_role5)));
                        break;
                    default:
                        Reporter.unreachable();
                        roleS.setVisibility(View.GONE);
                        break;
                }

                userNameTv.setText(getName(member));
            }
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int section) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortRoomMember.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == section) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortRoomMember.get(position).getFirstLetter().charAt(0);
        }
    }
}
