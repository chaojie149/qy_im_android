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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.SetManager;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * 设置 &&  取消 管理员，隐身人，监控人
 */
public class SetManagerActivity extends BaseActivity {
    private String roomId;
    private int role;
    private String roomJid;

    private EditText mEditText;
    private ListView mListView;
    private SetManagerAdapter mSetManagerAdapter;
    private List<SetManager> setManagerList;

    private Map<String, String> mRemarksMap = new HashMap<>();
    private Map<String, String> mUserIdMap = new HashMap<>();// 去除重复数据

    public static void start(Context ctx, String roomId, String roomJid, int role) {
        Intent intent = new Intent(ctx, SetManagerActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("role", role);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_manager);
        if (getIntent() != null) {
            roomId = getIntent().getStringExtra("roomId");
            role = getIntent().getIntExtra("role", RoomMember.ROLE_MANAGER);
            roomJid = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        }
        initActionBar();
        loadData();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        int titleId;
        switch (role) {
            case RoomMember.ROLE_MANAGER:
                titleId = R.string.design_admin;
                break;
            case RoomMember.ROLE_INVISIBLE:
                titleId = R.string.set_invisible;
                break;
            case RoomMember.ROLE_GUARDIAN:
                titleId = R.string.set_guardian;
                break;
            default:
                Reporter.unreachable();
                return;
        }
        TextView textView = findViewById(R.id.tv_title_center);
        textView.setText(titleId);
    }

    private void loadData() {
        List<Friend> mFriendList = FriendDao.getInstance().getAllFriendsNotFilterStatus(CoreManager.requireSelf(this).getUserId());
        for (int i = 0; i < mFriendList.size(); i++) {
            if (!TextUtils.isEmpty(mFriendList.get(i).getRemarkName())) {// 针对该好友进行了备注
                mRemarksMap.put(mFriendList.get(i).getUserId(), mFriendList.get(i).getRemarkName());
            }
        }

        List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);

        // 排序，简单根据role排序，
        // TODO: 讲道理，只要显示普通成员就够了的，其他身份都不能操作的，
        Collections.sort(roomMember, (o1, o2) -> o1.getRole() - o2.getRole());
        setManagerList = new ArrayList<>(roomMember.size());
        for (RoomMember member : roomMember) {
            if (!mUserIdMap.containsKey(member.getUserId())) {
                mUserIdMap.put(member.getUserId(), member.getUserId());
                SetManager setManager = new SetManager();
                setManager.setRole(member.getRole());
                setManager.setCreateTime(member.getCreateTime());
                setManager.setUserId(member.getUserId());
                setManager.setNickName(getName(member));
                setManager.setHiding(member.getHiding());
                setManagerList.add(setManager);
            }
        }
    }

    private String getName(RoomMember member) {
        if (!TextUtils.equals(member.getUserName(), member.getCardName())) {// 当userName与cardName不一致时，我们认为群主有设置群内备注
            return member.getCardName();
        } else {
            if (mRemarksMap.containsKey(member.getUserId())) {
                return mRemarksMap.get(member.getUserId());
            } else {
                return member.getUserName();
            }
        }
    }

    private String getName(MucRoomMember member) {
        if (!TextUtils.isEmpty(member.getRemarkName()) && !TextUtils.equals(member.getNickName(), member.getRemarkName())) {// 当userName与cardName不一致时，我们认为群主有设置群内备注
            return member.getRemarkName();
        } else {
            if (mRemarksMap.containsKey(member.getUserId())) {
                return mRemarksMap.get(member.getUserId());
            } else {
                return member.getNickName();
            }
        }
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.set_manager_lv);
        mSetManagerAdapter = new SetManagerAdapter(this);
        mSetManagerAdapter.setData(setManagerList);
        mListView.setAdapter(mSetManagerAdapter);

        /**
         * 群内邀请好友搜索功能
         */
        mEditText = (EditText) findViewById(R.id.search_et);
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
                String mContent = mEditText.getText().toString();
                if (TextUtils.isEmpty(mContent)) {
                    mSetManagerAdapter.setData(setManagerList);
                    return;
                }
                List<SetManager> data = new ArrayList<SetManager>();
                for (int i = 0; i < setManagerList.size(); i++) {
                    if (setManagerList.get(i).getNickName().contains(mContent)) { // 符合搜索条件的好友
                        data.add((setManagerList.get(i)));
                    }
                }
                searchMember(roomId, mContent, data);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SetManager setManager = (SetManager) mSetManagerAdapter.getItem(i);
                if (setManager.getUserId().equals(coreManager.getSelf().getUserId())) {
                    ToastUtil.showToast(SetManagerActivity.this, R.string.tip_cannot_set_self_role);
                    return;
                }
                if (setManager.getRole() == role) {
                    // 如果已经是这个身份，再点击就是取消，
                    if (role == RoomMember.ROLE_MANAGER) {
                        // 取消管理员，保留旧代码，
                        cancelManager(roomId, setManager);
                    } else {
                        // 取消隐身人，监控人，
                        cancelRole(roomId, setManager, role);
                    }
                } else {
                    if (role == RoomMember.ROLE_MANAGER) {
                        // 设置管理员，保留旧代码，
                        setManager(roomId, setManager);
                    } else {
                        // 设置隐身人，监控人，
                        setRole(roomId, setManager, role);
                    }
                }
            }
        });
    }

    private void searchMember(String roomId, String keyword, List<SetManager> data) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("keyword", keyword);

        List<SetManager> mSearchSortRoomMember = new ArrayList<>();
        HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_SEARCH)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoomMember>(MucRoomMember.class) {

                    @Override
                    public void onResponse(ArrayResult<MucRoomMember> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<MucRoomMember> roomMembers = result.getData();
                            if (roomMembers.size() > 0) {
                                List<SetManager> dataSimple = new ArrayList<>();
                                for (int i = 0; i < roomMembers.size(); i++) {
                                    // 对比本地数据，如userId一致，取出本地数据，
                                    for (int i1 = 0; i1 < data.size(); i1++) {
                                        if (TextUtils.equals(roomMembers.get(i).getUserId(), data.get(i1).getUserId())) {
                                            dataSimple.add(data.get(i1));
                                            break;
                                        }
                                    }

                                    MucRoomMember member = roomMembers.get(i);
                                    SetManager setManager = new SetManager();
                                    setManager.setRole(member.getRole());
                                    setManager.setCreateTime(member.getCreateTime());
                                    setManager.setUserId(member.getUserId());
                                    setManager.setNickName(getName(member));
                                    setManager.setHiding(member.getHiding());
                                    data.add(setManager);

                                    if (i == roomMembers.size() - 1) {// 最后一个
                                        data.removeAll(dataSimple);
                                        mSearchSortRoomMember.addAll(data);
                                    }
                                }
                            } else {
                                mSearchSortRoomMember.addAll(data);
                            }
                            mSetManagerAdapter.setData(mSearchSortRoomMember);
                        } else {
                            mSetManagerAdapter.setData(data);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mSetManagerAdapter.setData(data);
                    }
                });
    }

    private void setManager(String roomId, final SetManager setManager) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("touserId", setManager.getUserId());
        params.put("type", String.valueOf(RoomMember.ROLE_MANAGER));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MANAGER)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(SetManagerActivity.this, getString(R.string.room_member_vc_set_administrator_success), Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new EventGroupStatus(10000, 0));
                            setManager.setRole(RoomMember.ROLE_MANAGER);
                            mSetManagerAdapter.notifyDataSetChanged();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(SetManagerActivity.this, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(SetManagerActivity.this, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(SetManagerActivity.this);
                    }
                });
    }

    private void setRole(String roomId, final SetManager setManager, final int role) {
        Integer type;
        switch (role) {
            case RoomMember.ROLE_INVISIBLE:
                type = 4;
                break;
            case RoomMember.ROLE_GUARDIAN:
                type = 5;
                break;
            default:
                Reporter.unreachable();
                return;
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("touserId", setManager.getUserId());
        params.put("type", type.toString());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE_ROLE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            int tipContent;
                            switch (role) {
                                case RoomMember.ROLE_INVISIBLE: // 隐身人
                                    tipContent = R.string.tip_set_invisible_success;
                                    break;
                                case RoomMember.ROLE_GUARDIAN: // 监控人
                                    tipContent = R.string.tip_set_guardian_success;
                                    break;
                                default:
                                    Reporter.unreachable();
                                    return;
                            }
                            ToastUtil.showToast(SetManagerActivity.this, tipContent);
                            // 保留旧代码，抛出去RoomInfoActivity统一处理，
                            EventBus.getDefault().post(new EventGroupStatus(10000, 0));
                            setManager.setRole(role);
                            mSetManagerAdapter.notifyDataSetChanged();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(SetManagerActivity.this, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(SetManagerActivity.this, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(SetManagerActivity.this);
                    }
                });
    }

    private void cancelManager(String roomId, final SetManager setManager) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("touserId", setManager.getUserId());
        params.put("type", String.valueOf(RoomMember.ROLE_MEMBER));

        HttpUtils.get().url(coreManager.getConfig().ROOM_MANAGER)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(SetManagerActivity.this, getString(R.string.room_member_vc_cancel_administrator_success), Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new EventGroupStatus(10000, 0));
                            setManager.setRole(RoomMember.ROLE_MEMBER);
                            mSetManagerAdapter.notifyDataSetChanged();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(SetManagerActivity.this, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(SetManagerActivity.this, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(SetManagerActivity.this, getString(R.string.check_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cancelRole(String roomId, final SetManager setManager, final int role) {
        Integer type;
        switch (role) {
            case RoomMember.ROLE_INVISIBLE:
                type = -1;
                break;
            case RoomMember.ROLE_GUARDIAN:
                type = 0;
                break;
            default:
                Reporter.unreachable();
                return;
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("touserId", setManager.getUserId());
        params.put("type", type.toString());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE_ROLE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            int tipContent;
                            switch (role) {
                                case RoomMember.ROLE_INVISIBLE: // 隐身人
                                    tipContent = R.string.tip_cancel_invisible_success;
                                    break;
                                case RoomMember.ROLE_GUARDIAN: // 监控人
                                    tipContent = R.string.tip_cancel_guardian_success;
                                    break;
                                default:
                                    Reporter.unreachable();
                                    return;
                            }
                            ToastUtil.showToast(SetManagerActivity.this, tipContent);
                            // 保留旧代码，抛出去RoomInfoActivity统一处理，
                            EventBus.getDefault().post(new EventGroupStatus(10000, 0));
                            setManager.setRole(RoomMember.ROLE_MEMBER);
                            mSetManagerAdapter.notifyDataSetChanged();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(SetManagerActivity.this, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(SetManagerActivity.this, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(SetManagerActivity.this);
                    }
                });
    }

    private class SetManagerAdapter extends BaseAdapter {

        private List<SetManager> mSetManager;
        private Context mContext;

        public SetManagerAdapter(Context context) {
            mSetManager = new ArrayList<>();
            mContext = context;
        }

        public void setData(List<SetManager> setManager) {
            this.mSetManager = setManager;
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return mSetManager.size();
        }

        @Override
        public Object getItem(int i) {
            return mSetManager.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.a_item_set_manager, viewGroup, false);
            }
            ImageView avatar_img = ViewHolder.get(view, R.id.set_manager_iv);
            TextView roleS = ViewHolder.get(view, R.id.roles);
            TextView nick_name_tv = ViewHolder.get(view, R.id.set_manager_tv);
            // 设置头像
            if (mSetManager.get(i).getHiding() == 1) {
                avatar_img.setImageResource(Constants.HIDING_AVATAR);
            } else {
                AvatarHelper.getInstance().displayAvatar(mSetManager.get(i).getNickName(), mSetManager.get(i).getUserId(), avatar_img, true);
            }
            // 设置职位
            roleS.setBackgroundResource(R.drawable.bg_role3);
            switch (mSetManager.get(i).getRole()) {
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
            // 设置昵称
            nick_name_tv.setText(mSetManager.get(i).getNickName());
            return view;
        }
    }
}
