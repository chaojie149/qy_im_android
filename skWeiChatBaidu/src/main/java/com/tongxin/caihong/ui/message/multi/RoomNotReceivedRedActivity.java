package com.tongxin.caihong.ui.message.multi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.bean.redpacket.RedPacket;
import com.tongxin.caihong.bean.redpacket.RoomNotReceivedRedPacket;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.ChatContentView;
import com.tongxin.caihong.view.NoDoubleClickListener;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

public class RoomNotReceivedRedActivity extends BaseActivity {
    private ChatContentView chatContentView;
    private List<ChatMessage> data = new ArrayList<>();
    private Friend friend;

    public static void start(Context context, String roomJid) {
        Intent intent = new Intent(context, RoomNotReceivedRedActivity.class);
        intent.putExtra("roomJid", roomJid);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_not_received_red);
        String roomJid = getIntent().getStringExtra("roomJid");
        friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), roomJid);
        if (friend == null) {
            finish();
            return;
        }
        initActionBar();
        initView();
        loadData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.tip_not_received_red_title);
        TextView tvTitleRight = findViewById(R.id.tv_title_right);
        tvTitleRight.setText(R.string.tip_not_received_red_received);
        tvTitleRight.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                // 一键领取
                oneClickReceived();
            }
        });
    }

    private void initView() {
        chatContentView = findViewById(R.id.chat_content_view);
        // 禁止上拉
        chatContentView.setNeedRefresh(false);
        chatContentView.setData(data);
        chatContentView.setToUserId(friend.getUserId());
        chatContentView.setRoomId(friend.getRoomId());
        chatContentView.setCurGroup(true, friend.getRoomMyNickName());
        chatContentView.setData(data);
        chatContentView.setMessageEventListener(new ChatContentView.MessageEventListener() {
            @Override
            public void onEmptyTouch() {

            }

            @Override
            public void onTipMessageClick(ChatMessage message) {

            }

            @Override
            public void onMyAvatarClick() {

            }

            @Override
            public void onFriendAvatarClick(String friendUserId) {

            }

            @Override
            public void LongAvatarClick(ChatMessage chatMessage) {

            }

            @Override
            public void onNickNameClick(String friendUserId) {

            }

            @Override
            public void onMessageClick(ChatMessage chatMessage) {

            }

            @Override
            public void onMessageLongClick(ChatMessage chatMessage) {

            }

            @Override
            public void onSendAgain(ChatMessage chatMessage) {

            }

            @Override
            public void onMessageBack(ChatMessage chatMessage, int position) {

            }

            @Override
            public void onCallListener(int type) {

            }
        });
    }

    /**
     * 获取群内十分钟后未领完的红包
     */
    private void loadData() {
        Map<String, String> params = new HashMap<>();
        params.put("roomJid", friend.getUserId());

        HttpUtils.get().url(coreManager.getConfig().REDPACKET_GET_ROOM_UNCLAIMED)
                .params(params)
                .build()
                .execute(new BaseCallback<RoomNotReceivedRedPacket>(RoomNotReceivedRedPacket.class) {

                    @Override
                    public void onResponse(ObjectResult<RoomNotReceivedRedPacket> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            if (result.getData() != null && result.getData().getData() != null) {
                                // 服务器返回的是红包详情，自己还需要转成消息
                                List<RedPacket> redPackets = result.getData().getData();
                                for (RedPacket packet : redPackets) {
                                    ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgByAppointField(coreManager.getSelf().getUserId(), friend.getUserId()
                                            , "objectId", packet.getId());
                                    if (chatMessage == null) {
                                        // 本地不存在该红包消息，自己转
                                        chatMessage = new ChatMessage();
                                        chatMessage.setType(XmppMessage.TYPE_RED);
                                        chatMessage.setFromUserId(packet.getUserId());
                                        chatMessage.setFromUserName(packet.getUserName());
                                        chatMessage.setToUserId(friend.getUserId());
                                        chatMessage.setContent(packet.getGreetings());
                                        chatMessage.setFilePath(String.valueOf(packet.getType()));
                                        chatMessage.setObjectId(packet.getId());
                                        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                                        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                        chatMessage.setTimeSend(Long.valueOf(packet.getSendTime()) * 1000);
                                    }
                                    chatMessage.setDownload(true);
                                    data.add(chatMessage);
                                }
                            }
                            // 根据timeSend对红包消息进行排序
                            Collections.sort(data, (o1, o2) -> (int) (o1.getDoubleTimeSend() - o2.getDoubleTimeSend()));
                            chatContentView.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /**
     * 一键领取红包
     */
    private void oneClickReceived() {
        Map<String, String> params = new HashMap<>();
        params.put("roomJid", friend.getUserId());

        HttpUtils.get().url(coreManager.getConfig().REDPACKET_OPEN_ROOM_MANY)
                .params(params)
                .build()
                .execute(new ListCallback<RedPacket>(RedPacket.class) {

                    @Override
                    public void onResponse(ArrayResult<RedPacket> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            List<RedPacket> redPackets = result.getData();
                            if (redPackets.size() > 0) {
                                ToastUtil.showToast(mContext, R.string.tip_open_many_red_success);
                            } else {
                                ToastUtil.showToast(mContext, R.string.tip_open_many_red_success_but_null);
                            }
                            for (RedPacket packet : redPackets) {
                                for (ChatMessage message : data) {
                                    if (TextUtils.equals(message.getObjectId(), packet.getId())) {
                                        message.setFileSize(2);
                                        chatContentView.notifyDataSetChanged();
                                        break;
                                    }
                                }
                                ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgByAppointField(coreManager.getSelf().getUserId(), friend.getUserId()
                                        , "objectId", packet.getId());
                                if (chatMessage != null) {
                                    ChatMessageDao.getInstance().updateChatMessageReceiptStatus(coreManager.getSelf().getUserId(), friend.getUserId(), chatMessage.getPacketId());
                                    Intent receivedIntent = new Intent(OtherBroadcast.ACTION_GROUP_RECEIVED_RED_PACKET);
                                    receivedIntent.putExtra(AppConstant.EXTRA_MSG_ID, chatMessage.getPacketId());
                                    mContext.sendBroadcast(receivedIntent);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }
}
