package com.tongxin.caihong.ui.company;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.HorizontalListView;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.view.CircleImageView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

/**
 * 添加成员
 */
public class AddEmployee extends BaseActivity {
    private Button btnAdd;
    private ListViewAdapter adapter;
    private HorListViewAdapter hlvAdapter;
    private List<Friend> data = new ArrayList<>();
    private List<Integer> selectPositions = new ArrayList<>();
    private String departmentId;
    private String companyId;
    private boolean inviteJoinCompanyISNeedUserConfirm;

    public static void start(Context ctx, String companyId, String departmentId, String idsStr, boolean inviteJoinCompanyISNeedUserConfirm) {
        Intent intent = new Intent(ctx, AddEmployee.class);
        intent.putExtra("companyId", companyId);
        intent.putExtra("departmentId", departmentId);
        intent.putExtra("idsStr", idsStr);
        intent.putExtra("inviteJoinCompanyISNeedUserConfirm", inviteJoinCompanyISNeedUserConfirm);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employe);
        if (getIntent() != null) {
            companyId = getIntent().getStringExtra("companyId");
            departmentId = getIntent().getStringExtra("departmentId");
            inviteJoinCompanyISNeedUserConfirm = getIntent().getBooleanExtra("inviteJoinCompanyISNeedUserConfirm", false);
            String idsStr = getIntent().getStringExtra("idsStr");
            initData(JSON.parseArray(idsStr, String.class));
        }
        initActionBar();
        initView();
    }

    private void initData(List<String> ids) {
        data = FriendDao.getInstance().getAllFriends(coreManager.getSelf().getUserId());
        List<Friend> existFriends = new ArrayList<>();
        for (Friend friend : data) {
            for (String id : ids) {
                if (TextUtils.equals(friend.getUserId(), id)) {
                    existFriends.add(friend);
                }
            }
        }
        data.removeAll(existFriends);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.add_employee);
    }

    private void initView() {
        ListView lvAdd = findViewById(R.id.lvAdd);
        adapter = new ListViewAdapter();
        lvAdd.setAdapter(adapter);

        HorizontalListView hlvAdded = findViewById(R.id.hlvAdded);
        hlvAdapter = new HorListViewAdapter();
        hlvAdded.setAdapter(hlvAdapter);

        btnAdd = findViewById(R.id.btnAdd);
        ButtonColorChange.colorChange(mContext, btnAdd);
        btnAdd.setText(getString(R.string.add_chat_ok_btn, selectPositions.size()));

        lvAdd.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            if (hasSelected(position)) {
                removeSelect(position);
            } else {
                addSelect(position);
            }
        });

        hlvAdded.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            selectPositions.remove(position);
            adapter.notifyDataSetInvalidated();
            hlvAdapter.notifyDataSetInvalidated();
            btnAdd.setText(getString(R.string.add_chat_ok_btn, selectPositions.size()));
        });

        btnAdd.setOnClickListener(v -> inviteFriend(companyId, departmentId));
    }

    private boolean hasSelected(int position) {
        for (int i = 0; i < selectPositions.size(); i++) {
            if (selectPositions.get(i) == position) {
                return true;
            }
        }
        return false;
    }

    private void addSelect(int position) {
        selectPositions.add(0, position);
        adapter.notifyDataSetInvalidated();
        hlvAdapter.notifyDataSetInvalidated();
        btnAdd.setText(getString(R.string.add_chat_ok_btn, selectPositions.size()));
    }

    private void removeSelect(int position) {
        for (int i = 0; i < selectPositions.size(); i++) {
            if (selectPositions.get(i) == position) {
                selectPositions.remove(i);
                adapter.notifyDataSetInvalidated();
                hlvAdapter.notifyDataSetInvalidated();
                btnAdd.setText(getString(R.string.add_chat_ok_btn, selectPositions.size()));
                return;
            }
        }
    }

    /**
     * 添加成员
     */
    private void inviteFriend(String companyId, String departmentId) {
        if (selectPositions.size() < 1) {
            ToastUtil.showToast(mContext, getString(R.string.tip_select_at_lease_one_member));
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("departmentId", departmentId);
        String inviteUserIdStr = "";
        // 邀请好友
        for (int i = 0; i < selectPositions.size(); i++) {
            String userId = data.get(selectPositions.get(i)).getUserId();
            if (i == selectPositions.size() - 1) {
                inviteUserIdStr += userId;
            } else {
                inviteUserIdStr += userId + ",";
            }
        }
        params.put("inviteUserIdStr", inviteUserIdStr);

        HttpUtils.get().url(coreManager.getConfig().ORG_COMPANY_INVITE_USER_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (inviteJoinCompanyISNeedUserConfirm) {
                                ToastUtil.showToast(mContext, R.string.tip_msg_send_success_wait_user_verify_pass);
                                finish();
                            } else {
                                // 添加成员成功，通知同事页面刷新
                                ToastUtil.showToast(mContext, R.string.add_employee_succ);
                                EventBus.getDefault().post(new MessageEvent("Update"));// 数据有更新
                                finish();
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

    private class ListViewAdapter extends BaseAdapter {
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
            TextView catagory_title = ViewHolder.get(convertView, R.id.catagory_title);
            View view_bg_friend = ViewHolder.get(convertView, R.id.view_bg_friend);
            ImageView avatar_img = ViewHolder.get(convertView, R.id.avatar_img);
            TextView user_name_tv = ViewHolder.get(convertView, R.id.user_name_tv);
            CheckBox check_box = ViewHolder.get(convertView, R.id.check_box);

            Friend friend = data.get(position);
            catagory_title.setVisibility(View.GONE);
            view_bg_friend.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            if (friend.getHiding() == 1) {
                avatar_img.setImageResource(Constants.HIDING_AVATAR);
                user_name_tv.setText(Constants.HIDING_NICKNAME);
            } else {
                AvatarHelper.getInstance().displayAvatar(friend.getShowName(), friend.getUserId(), avatar_img, true);
                user_name_tv.setText(friend.getShowName());
            }
            check_box.setChecked(false);
            if (selectPositions.contains(position)) {
                check_box.setChecked(true);
                ButtonColorChange.checkChange(AddEmployee.this, check_box);
            } else {
                check_box.setChecked(false);
                check_box.setButtonDrawable(getResources().getDrawable(R.drawable.sel_nor_wx2));
            }
            return convertView;
        }
    }

    private class HorListViewAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return selectPositions.size();
        }

        @Override
        public Object getItem(int position) {
            return selectPositions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new CircleImageView(mContext);
                int size = DisplayUtil.dip2px(mContext, 37);
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(size, size);
                convertView.setLayoutParams(param);
            }
            ImageView imageView = (ImageView) convertView;
            AvatarHelper.getInstance().displayAvatar(data.get(selectPositions.get(position)).getShowName()
                    , data.get(selectPositions.get(position)).getUserId()
                    , imageView
                    , true);
            return convertView;
        }
    }
}
