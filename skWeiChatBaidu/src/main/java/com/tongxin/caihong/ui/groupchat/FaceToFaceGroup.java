package com.tongxin.caihong.ui.groupchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.HandleActivityDestroyedDoSomeThing;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.view.verify.VerificationCodeView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class FaceToFaceGroup extends BaseActivity implements View.OnClickListener {
    private VerificationCodeView vcCodeInput;
    private VerificationCodeView vcCodeResult;
    private Adapter adapter;
    private List<MucRoomMember> data = new ArrayList<>();
    private String password;
    private boolean isQuery;
    private String jid;
    private double longitude, latitude;
    private final BroadcastReceiver UpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if (action.equals(MsgBroadcast.ACTION_FACE_GROUP_NOTIFY) && isQuery) {
                String operating = intent.getStringExtra(MsgBroadcast.EXTRA_OPERATING);
                if (TextUtils.equals(operating, "notify_list")) {
                    // 刷新页面
                    querySignalGroup();
                } else if (TextUtils.equals(operating, "join_room")) {
                    DialogHelper.dismissProgressDialog();
                    // 加入房间
                    Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), jid);
                    if (friend != null) {
                        goRoom(friend);
                    } else {
                        Toast.makeText(context, getString(R.string.tip_join_face_to_face_group_failed), Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_to_face_group);
        PermissionUtil.autoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
            MyApplication.getInstance().getBdLocationHelper().requestLocation(() -> {
                latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
                longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
            });
        }, () -> {
            finish();
        }, PermissionUtil.getLocationPermissions());
        latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude == 0 && longitude == 0) {
            DialogHelper.tipDialog(mContext, getString(R.string.sure_open_user));
        }
        initActionBar();
        initView();
        registerReceiver(UpdateReceiver, new IntentFilter(MsgBroadcast.ACTION_FACE_GROUP_NOTIFY));
        // 当前页面不支侧滑退出
        setSwipeBackEnable(false);
    }

    @Override
    protected void onDestroy() {
        if (isQuery) {
            HandleActivityDestroyedDoSomeThing.handleFaceGroupDestroyed(jid);
        }
        unregisterReceiver(UpdateReceiver);
        super.onDestroy();
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
    }

    private void initView() {
        vcCodeInput = findViewById(R.id.vcCodeInput);
        findViewById(R.id.n_0_tv).setOnClickListener(this);
        findViewById(R.id.n_1_tv).setOnClickListener(this);
        findViewById(R.id.n_2_tv).setOnClickListener(this);
        findViewById(R.id.n_3_tv).setOnClickListener(this);
        findViewById(R.id.n_4_tv).setOnClickListener(this);
        findViewById(R.id.n_5_tv).setOnClickListener(this);
        findViewById(R.id.n_6_tv).setOnClickListener(this);
        findViewById(R.id.n_7_tv).setOnClickListener(this);
        findViewById(R.id.n_8_tv).setOnClickListener(this);
        findViewById(R.id.n_9_tv).setOnClickListener(this);
        findViewById(R.id.n_back_tv).setOnClickListener(this);

        vcCodeResult = findViewById(R.id.vcCodeResult);
        GridView gdUser = findViewById(R.id.gdUser);
        adapter = new Adapter();
        gdUser.setAdapter(adapter);
        ButtonColorChange.colorChange(mContext, findViewById(R.id.btnJoin));
        findViewById(R.id.btnJoin).setOnClickListener(v -> {
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), jid);
            if (friend != null) {
                goRoom(friend);
                finish();
            } else {
                joinSignalGroup();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.n_0_tv:
                show(false, 0);
                break;
            case R.id.n_1_tv:
                show(false, 1);
                break;
            case R.id.n_2_tv:
                show(false, 2);
                break;
            case R.id.n_3_tv:
                show(false, 3);
                break;
            case R.id.n_4_tv:
                show(false, 4);
                break;
            case R.id.n_5_tv:
                show(false, 5);
                break;
            case R.id.n_6_tv:
                show(false, 6);
                break;
            case R.id.n_7_tv:
                show(false, 7);
                break;
            case R.id.n_8_tv:
                show(false, 8);
                break;
            case R.id.n_9_tv:
                show(false, 9);
                break;
            case R.id.n_back_tv:
                show(true, 0);
                break;
        }
    }

    public void show(boolean isBack, int inputNum) {
        StringBuilder stringBuilder = new StringBuilder(vcCodeInput.getInputContent());
        if (isBack && stringBuilder.length() == 0) {
            return;
        }
        vcCodeInput.clearInputContent();
        if (isBack) {
            vcCodeInput.getEditText().setText(stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length()));
        } else {
            vcCodeInput.getEditText().setText(stringBuilder.append(inputNum));
        }
        if (stringBuilder.length() == 4) {
            password = vcCodeInput.getInputContent();
            vcCodeResult.getEditText().setText(password);
            Animation animation1 = AnimationUtils.loadAnimation(mContext, R.anim.translate_dialog_out);
            Animation animation2 = AnimationUtils.loadAnimation(mContext, R.anim.translate_dialog_in);
            findViewById(R.id.llStep1).setAnimation(animation1);
            findViewById(R.id.llStep2).setAnimation(animation2);
            findViewById(R.id.llStep1).setVisibility(View.GONE);
            findViewById(R.id.llStep2).setVisibility(View.VISIBLE);
            querySignalGroup();
        }
    }

    private void querySignalGroup() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("longitude", String.valueOf(longitude));
        params.put("latitude", String.valueOf(latitude));
        params.put("password", password);
        params.put("isQuery", isQuery ? String.valueOf(1) : String.valueOf(0));

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().ROOM_LOCATION_QUERY)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                    @Override
                    public void onResponse(ObjectResult<MucRoom> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            isQuery = true;
                            jid = result.getData().getJid();
                            data = result.getData().getMembers();
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    private void joinSignalGroup() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("jid", jid);

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().ROOM_LOCATION_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                    @Override
                    public void onResponse(ObjectResult<MucRoom> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            // 服务端代发907 ，在XChatManagerListener内接收处理，处理完成之后发送广播通知该界面，本地就不建群了
                        } else {
                            DialogHelper.dismissProgressDialog();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    private void goRoom(Friend friend) {
        Intent intent = new Intent(mContext, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);
        if (friend.getUnReadNum() > 0) {// 如该群组未读消息数量大于1, 刷新MessageFragment
            MsgBroadcast.broadcastMsgNumReset(mContext);
            MsgBroadcast.broadcastMsgUiUpdate(mContext);
        }
    }

    static class GridViewHolder {
        ImageView ivAvatar;
        TextView tvName;

        GridViewHolder(View itemView) {
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }

    class Adapter extends BaseAdapter {

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
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_face_info_view, parent, false);
                GridViewHolder gridViewHolder = new GridViewHolder(convertView);
                convertView.setTag(gridViewHolder);
            }
            GridViewHolder gridViewHolder = (GridViewHolder) convertView.getTag();
            MucRoomMember mucRoomMember = data.get(position);
            if (mucRoomMember != null) {
                AvatarHelper.getInstance().displayAvatar(mucRoomMember.getNickName(), mucRoomMember.getUserId(), gridViewHolder.ivAvatar, true);
                gridViewHolder.tvName.setText(mucRoomMember.getNickName());
            }
            return convertView;
        }
    }
}
