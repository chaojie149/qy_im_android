package com.tongxin.caihong.ui.contacts.label;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.LabelDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.PullToRefreshSlideListView;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * Search Location:
 * <p>
 * AddContactsActivity:        activity_select_contacts.xml | row_select_contacts
 * <p>
 * SelectContactsActivity:     activity_select_contacts.xml | row_select_contacts
 * <p>
 * SelectLabelFriendActivity: activity_select_contacts.xml | row_select_contacts_clone
 * <p>
 * SelectFriendsActivity:        activity_select_friends | row_select_friend
 * <p>
 * SelectCardPopupWindow: pop_send_card.xml | row_select_contacts
 * <p>
 * AtSeeCircleActivity:            activity_select_contacts | row_select_remind_see_circle
 * <p>
 * SelectRoomMemberPopupWindow: pop_at_room_member | a_item_set_manager
 * <p>
 * SetManagerActivity&&CancelManagerActivity: activity_set_manager.xml | a_item_set_manager
 * <p>
 * JitsiInviteActivity: activity_jitsi_invite | row_select_contacts_jitsi
 * <p>
 * SelectPrefixActivity: a_act_selectaddr | a_item_resume_fnid
 * <p>
 * OK Button Style: activity_select_contacts | activity_add_muc_file | activity_map_picker
 * <p>
 * Select Location:
 * <p>
 * type 1
 * SelectContactsActivity
 * AddContactsActivity
 * SelectLabelFriendActivity
 * SelectFriendsActivity
 * type 2
 * FriendFragment
 * RoomFragment
 * BlackActivity
 * SelectNewContactsActivity
 * SelectNewGroupInstantActivity
 * type 3
 * GroupMoreFeaturesActivity
 * GroupTransferActivity
 * <p>
 * 发送名片、@
 * 提醒谁看、会议邀请、管理员
 */
public class LabelActivity extends BaseActivity {
    private PullToRefreshSlideListView prsLabel;
    private LabelAdapter labelAdapter;
    private List<Label> data = new ArrayList<>();

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, LabelActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);
        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.mergerStatus).setBackgroundColor(getResources().getColor(R.color.normal_bg));
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
    }

    private void initView() {
        prsLabel = findViewById(R.id.prsLabel);
        labelAdapter = new LabelAdapter(this);
        prsLabel.getRefreshableView().setAdapter(labelAdapter);

        prsLabel.setOnRefreshListener(refreshView -> refreshLabelListFromService());
        prsLabel.getRefreshableView().setOnItemClickListener((parent, view, position, id) -> {
            position = (int) id;
            Label label = data.get(position);
            if (label != null) {
                Intent intent = new Intent(LabelActivity.this, CreateLabelActivity.class);
                intent.putExtra("isEditLabel", true);
                intent.putExtra("labelId", label.getGroupId());
                startActivity(intent);
            }
        });

        ButtonColorChange.colorChange(this, findViewById(R.id.btnCreateLabel));
        findViewById(R.id.btnCreateLabel).setOnClickListener(view -> {
            Intent intent = new Intent(LabelActivity.this, CreateLabelActivity.class);
            intent.putExtra("isEditLabel", false);
            startActivity(intent);
        });
    }

    private void loadData() {
        data.clear();
        data = LabelDao.getInstance().getAllLabels(coreManager.getSelf().getUserId());
        labelAdapter.notifyDataSetChanged();
    }

    private void refreshLabelListFromService() {
        Map<String, String> params = new HashMap<>();

        HttpUtils.get().url(coreManager.getConfig().FRIENDGROUP_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Label>(Label.class) {
                    @Override
                    public void onResponse(ArrayResult<Label> result) {
                        prsLabel.onRefreshComplete();
                        if (Result.checkSuccess(mContext, result)) {
                            List<Label> labelList = result.getData();
                            if (labelList == null) {
                                labelList = new ArrayList<>();
                            }
                            LabelDao.getInstance().refreshLabel(coreManager.getSelf().getUserId(), labelList);
                            data = labelList;
                            labelAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        prsLabel.onRefreshComplete();
                    }
                });
    }

    class LabelAdapter extends SlideBaseAdapter {

        public LabelAdapter(Context context) {
            super(context);
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.row_label;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.row_item_lable_delete;
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            TextView tvName = ViewHolder.get(convertView, R.id.tvName);
            TextView tvNum = ViewHolder.get(convertView, R.id.tvNum);
            TextView tvUsersName = ViewHolder.get(convertView, R.id.tvUsersName);
            ImageView ivDelete = ViewHolder.get(convertView, R.id.ivDelete);
            final Label label = data.get(position);
            if (label != null) {
                tvName.setText(label.getGroupName());
                List<String> userIds = JSON.parseArray(label.getUserIdList(), String.class);
                if (userIds != null) {
                    tvNum.setText("(" + userIds.size() + ")");
                } else {
                    tvNum.setText("(0)");
                }
                tvUsersName.setText("");
                StringBuilder usersName = new StringBuilder();
                if (userIds != null && userIds.size() > 0) {
                    for (int i = 0; i < userIds.size(); i++) {
                        Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), userIds.get(i));
                        if (friend != null) {
                            if (i == userIds.size() - 1) {
                                usersName.append(friend.getShowName());
                            } else {
                                usersName.append(friend.getShowName()).append("、");
                            }
                        }
                    }
                    tvUsersName.setText(usersName.toString());
                }
            }
            ivDelete.setOnClickListener(v -> deleteLabel(label));
            return convertView;
        }

        private void deleteLabel(final Label label) {
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
            Map<String, String> params = new HashMap<>();
            params.put("groupId", label.getGroupId());
            HttpUtils.get().url(coreManager.getConfig().FRIENDGROUP_DELETE)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {
                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (Result.checkSuccess(mContext, result)) {
                                LabelDao.getInstance().deleteLabel(coreManager.getSelf().getUserId(), label.getGroupId());
                                data.remove(label);
                                labelAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        }
    }
}
