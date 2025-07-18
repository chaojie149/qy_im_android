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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.multi.RoomInfoActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * @群成员pop
 */
public class SelectRoomMemberPopupWindow extends PopupWindow {
    private View mMenuView;
    private EditText mEditText;
    private boolean isSearch;

    private PullToRefreshListView mListView;
    private SetAtMemberAdapter mAdapter;
    private List<RoomMember> mRoomMembers;
    private List<RoomMember> mSearchRoomMembers = new ArrayList<>();

    private int mRole;
    private String mRoomId;
    private String mUserId;
    private SendMemberListener mSendMemberListener;

    private List<MucRoomMember> mSearchMucRoomMember;

    private Map<String, String> mRemarksMap = new HashMap<>();
    private int showMember;

    public SelectRoomMemberPopupWindow(FragmentActivity context, List<RoomMember> roomMembers, int role, String roomId, String userId,
                                       int showMember, SendMemberListener sendMemberListener) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.pop_at_room_member, null);
        mMenuView.findViewById(R.id.select_rl).setBackgroundColor(SkinUtils.getSkin(context).getAccentColor());
        // 因为群主设置了隐身人不会发送通知，本地的群成员表内可能会有隐身人，导致隐身人也可以@的问题
        // 改为直接从接口内获取吧
        // 先加载本地数据再调接口刷新吧，否则接口慢的时候太难看，相比之下隐身人只是小众功能不值得，
        this.mRoomMembers = roomMembers;
        this.mRole = role;
        this.mRoomId = roomId;
        this.mUserId = userId;
        this.showMember = showMember;
        this.mSendMemberListener = sendMemberListener;
        List<Friend> mFriendList = FriendDao.getInstance().getAllFriendsNotFilterStatus(CoreManager.requireSelf(context).getUserId());
        for (int i = 0; i < mFriendList.size(); i++) {
            if (!TextUtils.isEmpty(mFriendList.get(i).getRemarkName())) {// 针对该好友进行了备注
                mRemarksMap.put(mFriendList.get(i).getUserId(), mFriendList.get(i).getRemarkName());
            }
        }

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
        initView(context);
        // 因为群主设置了隐身人不会发送通知，本地的群成员表内可能会有隐身人，导致隐身人也可以@的问题
        // 改为直接从接口内获取吧
        loadDataByService(true);
    }

    private void initActionBar() {
        mMenuView.findViewById(R.id.title_iv_back).setOnClickListener(view -> dismiss());
        TextView tv1 = mMenuView.findViewById(R.id.tv_center_filter);
        tv1.setText(MyApplication.getInstance().getString(R.string.select_contacts));
    }

    private void initView(Context context) {
        LinearLayout linearLayout = mMenuView.findViewById(R.id.everybody);
        // 去掉限制
        if (mRole != RoomMember.ROLE_OWNER && mRole != RoomMember.ROLE_MANAGER) {
            linearLayout.setVisibility(View.GONE);
        }
        HeadView avatar = mMenuView.findViewById(R.id.everyone_iv);
        Friend friend = FriendDao.getInstance().getFriend(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), mUserId);
        AvatarHelper.getInstance().displayAvatar(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), friend, avatar);

        mListView = mMenuView.findViewById(R.id.pull_refresh_list);
        mAdapter = new SetAtMemberAdapter(context);
        mAdapter.setData(mRoomMembers);
        mListView.getRefreshableView().setAdapter(mAdapter);

        mEditText = mMenuView.findViewById(R.id.search_et);
        mEditText.setHint(MyApplication.getInstance().getString(R.string.search));
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
                mListView.setMode(PullToRefreshBase.Mode.DISABLED);
                mSearchRoomMembers.clear();
                String str = mEditText.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    isSearch = false;
                    mListView.setMode(PullToRefreshBase.Mode.BOTH);
                    mAdapter.setData(mRoomMembers);
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
                List<RoomMember> data = new ArrayList<>();
                for (int i = 0; i < mRoomMembers.size(); i++) {
                    if (getName(mRoomMembers.get(i)).contains(str)) {
                        // 符合搜索条件的好友
                        data.add((mRoomMembers.get(i)));
                    }
                }
                searchMember(mRoomId, str, data);
            }
        });

        linearLayout.setOnClickListener(v -> {
            mSendMemberListener.sendEveryOne("全体成员");
            dismiss();
        });

        mListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                loadDataByService(true);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                loadDataByService(false);
            }
        });

        mListView.getRefreshableView().setOnItemClickListener((parent, view, position, id) -> {
            final RoomMember roomMember;
            if (isSearch) {
                roomMember = mSearchRoomMembers.get((int) id);
            } else {
                roomMember = mRoomMembers.get((int) id);
            }
            mSendMemberListener.sendAtContent(roomMember);
            dismiss();
        });
    }

    private void searchMember(String roomId, String keyword, List<RoomMember> data) {
        mSearchRoomMembers = new ArrayList<>();
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("keyword", keyword);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).ROOM_MEMBER_SEARCH)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoomMember>(MucRoomMember.class) {

                    @Override
                    public void onResponse(ArrayResult<MucRoomMember> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            mSearchMucRoomMember = result.getData();
                            if (mSearchMucRoomMember.size() > 0) {
                                List<RoomMember> dataSimple = new ArrayList<>();
                                for (int i = 0; i < mSearchMucRoomMember.size(); i++) {
                                    if (TextUtils.equals(mSearchMucRoomMember.get(i).getUserId(),
                                            CoreManager.requireSelf(MyApplication.getContext()).getUserId())) {
                                        // 搜索到自己，直接跳过
                                        continue;
                                    }
                                    // 对比本地数据，如userId一致，取出本地数据，
                                    for (int i1 = 0; i1 < data.size(); i1++) {
                                        if (TextUtils.equals(mSearchMucRoomMember.get(i).getUserId(), data.get(i1).getUserId())) {
                                            dataSimple.add(data.get(i1));
                                            break;
                                        }
                                    }

                                    RoomMember mRoomMembers = new RoomMember();
                                    mRoomMembers.setRoomId(mRoomId);
                                    mRoomMembers.setUserId(mSearchMucRoomMember.get(i).getUserId());
                                    mRoomMembers.setUserName(mSearchMucRoomMember.get(i).getNickName());
                                    if (TextUtils.isEmpty(mSearchMucRoomMember.get(i).getRemarkName())) {
                                        mRoomMembers.setCardName(mSearchMucRoomMember.get(i).getNickName());
                                    } else {
                                        mRoomMembers.setCardName(mSearchMucRoomMember.get(i).getRemarkName());
                                    }
                                    mRoomMembers.setRole(mSearchMucRoomMember.get(i).getRole());
                                    mRoomMembers.setHiding(mSearchMucRoomMember.get(i).getHiding());
                                    mRoomMembers.setCreateTime(mSearchMucRoomMember.get(i).getCreateTime());
                                    mSearchRoomMembers.add(mRoomMembers);

                                    if (i == mSearchMucRoomMember.size() - 1) {// 最后一个
                                        data.removeAll(dataSimple);
                                        mSearchRoomMembers.addAll(data);
                                    }
                                }
                            } else {
                                mSearchRoomMembers.addAll(data);
                            }
                            mAdapter.setData(mSearchRoomMembers);
                        } else {
                            mAdapter.setData(data);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mAdapter.setData(data);
                    }
                });
    }

    private void loadDataByService(boolean reset) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("roomId", mRoomId);
        if (reset) {
            params.put("joinTime", String.valueOf(0));
        } else {
            long lastRoamingTime = PreferenceUtils.getLong(MyApplication.getContext(), Constants.MUC_MEMBER_LAST_JOIN_TIME + CoreManager.requireSelf(MyApplication.getContext()).getUserId() + mRoomId, 0);
            params.put("joinTime", String.valueOf(lastRoamingTime));
        }
        params.put("pageSize", Constants.MUC_MEMBER_PAGE_SIZE);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).ROOM_MEMBER_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoomMember>(MucRoomMember.class) {
                    @Override
                    public void onResponse(ArrayResult<MucRoomMember> result) {
                        if (reset) {
                            mListView.onPullDownRefreshComplete();
                        } else {
                            mListView.onPullUpRefreshComplete();
                        }

                        HashMap<String, String> toRepeatHashMap = new HashMap<>();
                        for (RoomMember member : mRoomMembers) {
                            toRepeatHashMap.put(member.getUserId(), member.getUserId());
                        }

                        if (Result.checkSuccess(MyApplication.getContext(), result)) {
                            List<MucRoomMember> mucRoomMemberList = result.getData();
                            if (mucRoomMemberList.size() == Integer.valueOf(Constants.MUC_MEMBER_PAGE_SIZE)) {
                                mListView.setMode(PullToRefreshBase.Mode.BOTH);
                            } else {
                                mListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
                            }
                            if (mucRoomMemberList.size() > 0) {
                                List<RoomMember> roomMemberList = new ArrayList<>();
                                for (int i = 0; i < mucRoomMemberList.size(); i++) {
                                    if (!reset &&
                                            toRepeatHashMap.containsKey(mucRoomMemberList.get(i).getUserId())) {
                                        continue;
                                    }
                                    RoomMember roomMember = new RoomMember();
                                    roomMember.setRoomId(mRoomId);
                                    roomMember.setUserId(mucRoomMemberList.get(i).getUserId());
                                    roomMember.setUserName(mucRoomMemberList.get(i).getNickName());
                                    if (TextUtils.isEmpty(mucRoomMemberList.get(i).getRemarkName())) {
                                        roomMember.setCardName(mucRoomMemberList.get(i).getNickName());
                                    } else {
                                        roomMember.setCardName(mucRoomMemberList.get(i).getRemarkName());
                                    }
                                    roomMember.setRole(mucRoomMemberList.get(i).getRole());
                                    roomMember.setHiding(mucRoomMemberList.get(i).getHiding());
                                    roomMember.setCreateTime(mucRoomMemberList.get(i).getCreateTime());
                                    roomMemberList.add(roomMember);
                                }

                                if (reset) {
                                    RoomMemberDao.getInstance().deleteRoomMemberTable(mRoomId);
                                }
                                AsyncUtils.doAsync(this, mucChatActivityAsyncContext -> {
                                    for (int i = 0; i < roomMemberList.size(); i++) {// 在异步任务内存储
                                        RoomMemberDao.getInstance().saveSingleRoomMember(mRoomId, roomMemberList.get(i));
                                    }
                                });

                                RoomInfoActivity.saveMucLastRoamingTime(CoreManager.requireSelf(MyApplication.getContext()).getUserId(), mRoomId, mucRoomMemberList.get(mucRoomMemberList.size() - 1).getCreateTime(), reset);

                                // 刷新本地数据，移除掉自己
                                for (int i = 0; i < roomMemberList.size(); i++) {
                                    if (TextUtils.equals(roomMemberList.get(i).getUserId(), CoreManager.requireSelf(MyApplication.getContext()).getUserId())) {
                                        roomMemberList.remove(roomMemberList.get(i));
                                        break;
                                    }
                                }

                                if (reset) {
                                    mRoomMembers.clear();
                                    mRoomMembers.addAll(roomMemberList);
                                    mAdapter.setData(mRoomMembers);
                                } else {
                                    mRoomMembers.addAll(roomMemberList);
                                    mAdapter.setData(mRoomMembers);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (reset) {
                            mListView.onPullDownRefreshComplete();
                        } else {
                            mListView.onPullUpRefreshComplete();
                        }
                        ToastUtil.showErrorNet(MyApplication.getContext());
                    }
                });
    }

    private String getName(RoomMember member) {
        if (mRole == 1) {
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

    public interface SendMemberListener {
        void sendEveryOne(String everyOne);

        void sendAtContent(RoomMember member);
    }

    class SetAtMemberAdapter extends BaseAdapter {

        private Context mContext;
        private List<RoomMember> data;

        public SetAtMemberAdapter(Context context) {
            mContext = context;
            data = new ArrayList<>();
        }

        public void setData(List<RoomMember> members) {
            data = members;
            if (data == null) {
                data = new ArrayList<>();
            }
            if (showMember == 0 && mRole != 1 && mRole != 2) {
                // 不显示群成员情况过滤群主和管理员以外成员，
                Iterator<RoomMember> ite = members.iterator();
                while (ite.hasNext()) {
                    RoomMember next = ite.next();
                    if (!next.isGroupOwnerOrManager()) {
                        ite.remove();
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_at_select, viewGroup, false);
            }
            if (i == 0) {
                ViewHolder.get(view, R.id.v_friend_bg).setVisibility(View.GONE);
            }
            ImageView avatar_img = ViewHolder.get(view, R.id.set_manager_iv);
            TextView nick_name_tv = ViewHolder.get(view, R.id.set_manager_tv);
            AvatarHelper.getInstance().displayAvatar(data.get(i).getUserName(), data.get(i).getUserId(), avatar_img, true);
            nick_name_tv.setText(getName(data.get(i)));
            return view;
        }
    }
}
