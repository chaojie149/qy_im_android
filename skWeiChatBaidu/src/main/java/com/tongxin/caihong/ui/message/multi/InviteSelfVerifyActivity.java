package com.tongxin.caihong.ui.message.multi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.event.EventCreateGroupFriend;
import com.tongxin.caihong.bean.event.EventPaySuccess;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.me.redpacket.PayJoinRoom;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 群聊邀请
 */
public class InviteSelfVerifyActivity extends BaseActivity {
    private HeadView ivInviteSelfGroupAvatar;
    private TextView tvInviteSelfGroupName;
    private TextView tvInviteSelfGroupNumber;
    private TextView tvInviteSelfInvitedUserName;
    private TextView tvInviteSelfJoined;
    private Button btnInviteSelfJoin;

    private String roomJid;
    private String roomName;
    private int roomNumber;
    private String inviteSelfInvitedUserId;
    private String inviteSelfInvitedUserName;
    private String chatKey;
    private MucRoom mucRoom;
    private Friend friend;

    public static void start(Context context, String roomJid, String roomName, int roomNumber, String inviteSelfInvitedUserId, String inviteSelfInvitedUserName, String chatKey) {
        Intent intent = new Intent(context, InviteSelfVerifyActivity.class);
        intent.putExtra("roomJid", roomJid);
        intent.putExtra("roomName", roomName);
        intent.putExtra("roomNumber", roomNumber);
        intent.putExtra("inviteSelfInvitedUserId", inviteSelfInvitedUserId);
        intent.putExtra("inviteSelfInvitedUserName", inviteSelfInvitedUserName);
        intent.putExtra("chatKey", chatKey);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_self_verify);
        roomJid = getIntent().getStringExtra("roomJid");
        roomName = getIntent().getStringExtra("roomName");
        roomNumber = getIntent().getIntExtra("roomNumber", 0);
        inviteSelfInvitedUserId = getIntent().getStringExtra("inviteSelfInvitedUserId");
        inviteSelfInvitedUserName = getIntent().getStringExtra("inviteSelfInvitedUserName");
        chatKey = getIntent().getStringExtra("chatKey");
        initActionBar();
        initView();
        checkInRoom();

    }

    private void checkInRoom() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomJid", roomJid);

        HttpUtils.get().url(coreManager.getConfig().CHECK_IN)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult result) {
                        if (result.getResultCode() == 1 ) {
                            //在群里直接
                            ToastUtil.showToast(InviteSelfVerifyActivity.this,"已在群里");
                            finish();
                        } else {
                            //不在群里才加入
                            refreshData(false);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showToast(InviteSelfVerifyActivity.this,e.getMessage());
                    }
                });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.type_invite_join_room_bottom);
    }

    private void initView() {
        ivInviteSelfGroupAvatar = findViewById(R.id.ivInviteSelfGroupAvatar);
        tvInviteSelfGroupName = findViewById(R.id.tvInviteSelfGroupName);
        tvInviteSelfGroupNumber = findViewById(R.id.tvInviteSelfGroupNumber);
        tvInviteSelfInvitedUserName = findViewById(R.id.tvInviteSelfInvitedUserName);
        tvInviteSelfJoined = findViewById(R.id.tvInviteSelfJoined);
        ButtonColorChange.textChange(mContext, tvInviteSelfJoined);
        btnInviteSelfJoin = findViewById(R.id.btnInviteSelfJoin);
        ButtonColorChange.colorChange(mContext, btnInviteSelfJoin);
        btnInviteSelfJoin.setOnClickListener(view -> refreshData(true));
        tvInviteSelfGroupName.setText(roomName);
        tvInviteSelfGroupNumber.setText(getString(R.string.total_count_place_holder, roomNumber));
        tvInviteSelfInvitedUserName.setText(getString(R.string.type_invite_join_room3, inviteSelfInvitedUserName));
        friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), roomJid);
        if (friend == null) {
            friend = new Friend();
            friend.setUserId(roomJid);
            friend.setRoomId(roomJid);
            friend.setRoomFlag(1);
            setBottomViewShow(true, 0);
        } else if (friend.getGroupStatus() == 1) {
            setBottomViewShow(true, 0);
        } else {
            setBottomViewShow(false, friend.getGroupStatus());
        }
        AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), friend, ivInviteSelfGroupAvatar);
    }

    /**
     * @param isCanJoinGroup 是否能加群
     * @param status         群组状态
     */
    private void setBottomViewShow(boolean isCanJoinGroup, int status) {
        if (TextUtils.equals(inviteSelfInvitedUserId, coreManager.getSelf().getUserId())) {
            // 自己发送的邀请消息，不显示底部按钮
            return;
        }
        if (isCanJoinGroup) {
            tvInviteSelfJoined.setVisibility(View.GONE);
            btnInviteSelfJoin.setVisibility(View.VISIBLE);
        } else {
            tvInviteSelfJoined.setVisibility(View.VISIBLE);
            btnInviteSelfJoin.setVisibility(View.GONE);
            // 被踢出群组不判断了，因为被踢出之后二次邀请，本地的状态也不会变，导致不能加入了
/*
            if (status == 1) {
                tvInviteSelfJoined.setText(getString(R.string.tip_been_kick));
            } else
*/
            if (status == 2) {
                tvInviteSelfJoined.setText(getString(R.string.tip_disbanded));
            } else if (status == 3) {
                tvInviteSelfJoined.setText(getString(R.string.tip_group_disable_by_service));
            } else {
                tvInviteSelfJoined.setText(getString(R.string.type_invite_join_room5));
            }
        }
    }

    /**
     * 通过接口更新群组的信息以及状态
     */
    private void refreshData(boolean isJoin) {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("roomJid", roomJid);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).ROOM_GET_ROOM_BY_JID)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(final ObjectResult<MucRoom> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(MyApplication.getContext(), result)) {
                            mucRoom = result.getData();
                            if (mucRoom != null) {
                                tvInviteSelfGroupName.setText(mucRoom.getName());
                                tvInviteSelfGroupNumber.setText(getString(R.string.total_count_place_holder, mucRoom.getUserSize()));
                                if (mucRoom.getS() == -1) {
                                    setBottomViewShow(false, 3);
                                }
                            } else {
                                setBottomViewShow(false, 2);
                            }
                            if (mucRoom.getNeedPay()==1){
                                PayJoinRoom.start(InviteSelfVerifyActivity.this,mucRoom.getId(),mucRoom.getPayForDays(),mucRoom.getPayForAmount());
                            }else if (isJoin) {
                                joinRoom(mucRoom);
                            }

                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventPaySuccess message) {
        finish();
    }

    private void joinRoom(MucRoom mucRoom) {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("roomId", mucRoom.getId());
        params.put("userId", inviteSelfInvitedUserId);
        if (!TextUtils.isEmpty(chatKey)) {
            params.put("chatKey", chatKey);
        }
        params.put(AppConstant.GROUP_ADD_STYLE, AppConstant.GROUP_INVITE_JOIN);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getContext()).ROOM_INVITE_JOIN_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(final ObjectResult<MucRoom> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(MyApplication.getContext(), result)) {
                            if (friend.getGroupStatus() == 1) {
                                // 之前为被踢出的状态，先清空掉之前的聊天记录先
                                FriendDao.getInstance().updateFriendGroupStatus(coreManager.getSelf().getUserId(), mucRoom.getJid(), 0);
                                ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), mucRoom.getJid());
                                Intent intent = new Intent(Constants.CHAT_HISTORY_EMPTY);
                                intent.putExtra("id", mucRoom.getJid());
                                MyApplication.getContext().sendBroadcast(intent);
                            }

                            EventBus.getDefault().post(new EventCreateGroupFriend(mucRoom));
                            btnInviteSelfJoin.postDelayed(() -> {
                                // 给500ms的时间缓存，防止群组还未创建好就进入群聊天界面
                                if (!TextUtils.isEmpty(chatKey)) {
                                    FriendDao.getInstance().updateEncryptType(mucRoom.getJid(), mucRoom.getEncryptType());
                                    try {
                                        String realChatKey = new String(RSA.decryptFromBase64(chatKey, Base64.decode(SecureChatUtil.getRSAPrivateKey(coreManager.getSelf().getUserId()))));
                                        FriendDao.getInstance().updateChatKeyGroup(mucRoom.getJid(), SecureChatUtil.encryptChatKey(mucRoom.getJid(), realChatKey));
                                        Log.e("msg", "设置chatKey成功-->" + realChatKey);
                                    } catch (Exception e) {
                                        Log.e("msg", "设置chatKey失败");
                                        FriendDao.getInstance().updateIsLostChatKeyGroup(mucRoom.getJid(), 1);
                                    }
                                }
                                interMucChat(mucRoom.getJid(), mucRoom.getName());
                            }, 500);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    private void interMucChat(String roomJid, String roomName) {
        Intent intent = new Intent(mContext, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);

        // 更新群组页面
        MucgroupUpdateUtil.broadcastUpdateUi(mContext);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PayJoinRoom.PAY_REQUEST && resultCode == RESULT_OK) {
            joinRoom(this.mucRoom);
            finish();
        }
    }
}
