package com.tongxin.caihong.ui.me.collection;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.adapter.PublicMessageRecyclerAdapter;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.circle.Comment;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.MessageEventComment;
import com.tongxin.caihong.ui.circle.MessageEventReply;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.PMsgBottomView;
import com.tongxin.caihong.view.TrillCommentInputDialog;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.MessageEventPublic;
import fm.jiecao.jcvideoplayer_lib.MessageEventVideo;
import okhttp3.Call;

public class ShareDetailActivity extends BaseActivity implements PublicMessageRecyclerAdapter.OnMessageActionListener {

    private String mUserId;
    private String mUserName;
    private SmartRefreshLayout refreshLayout;
    private PMsgBottomView bottom_view;
    private SwipeRecyclerView recyclerView;
    private List<PublicMessage> data;
    private PublicMessageRecyclerAdapter mAdapter;
    private PublicMessage message;

    public static void start(Context context, List<PublicMessage> message) {
        Intent intent = new Intent(context, ShareDetailActivity.class);
        intent.putExtra("message", (Serializable) message);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_detail);
        data = (List<PublicMessage>) getIntent().getSerializableExtra("message");
        message = data.get(0);
        mUserId = coreManager.getSelf().getUserId();
        mUserName = coreManager.getSelf().getNickName();
        initActionBar();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_right_right);
        ((TextView) findViewById(R.id.tv_title_center)).setText(getString(R.string.detail));
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshLayout = findViewById(R.id.refreshLayout);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadMore(false);
        bottom_view = (PMsgBottomView) findViewById(R.id.bottom_view);
        mAdapter = new PublicMessageRecyclerAdapter(mContext, coreManager, data);
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnMessageActionListener(this);
    }

    @Override
    public void finish() {
        VoicePlayer.instance().stop();
        super.finish();
    }

    private void addComment(final Comment comment) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        if (!TextUtils.isEmpty(comment.getToUserId())) {
            params.put("toUserId", comment.getToUserId());
        }
        if (!TextUtils.isEmpty(comment.getToNickname())) {
            params.put("toNickname", comment.getToNickname());
        }
        params.put("body", comment.getBody());

        HttpUtils.post().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(com.xuan.xuanhttplibrary.okhttp.result.ObjectResult<String> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            List<Comment> comments = message.getComments();
                            if (comments == null) {
                                comments = new ArrayList<>();
                                message.setComments(comments);
                            }
                            comment.setCommentId(result.getData());
                            comments.add(0, comment);
                            message.getCount().setComment(message.getCount().getComment() + 1);
                            mAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getApplicationContext());
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventComment message) {
        TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(mContext,
                getString(R.string.enter_pinlunt),
                str -> {
                    Comment mComment = new Comment();
                    Comment comment = mComment.clone();
                    if (comment == null)
                        comment = new Comment();
                    comment.setBody(str);
                    comment.setUserId(mUserId);
                    comment.setNickName(mUserName);
                    comment.setTime(TimeUtils.sk_time_current_time());
                    addComment(comment);
                });
        Window window = trillCommentInputDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
            trillCommentInputDialog.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventReply message) {
        if (message.event.equals("Reply")) {
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(mContext, getString(R.string.replay) + "：" + message.comment.getNickName(),
                    str -> {
                        Comment mComment = new Comment();
                        Comment comment = mComment.clone();
                        if (comment == null)
                            comment = new Comment();
                        comment.setToUserId(message.comment.getUserId());
                        comment.setToNickname(message.comment.getNickName());
                        comment.setToBody(message.comment.getToBody());
                        comment.setBody(str);
                        comment.setUserId(mUserId);
                        comment.setNickName(mUserId);
                        comment.setTime(TimeUtils.sk_time_current_time());
                        addComment(comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventVideo msg) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_VIDEO);
        chatMessage.setContent(msg.getUrl());
        chatMessage.setFromUserId(AppConstant.DYNAMIC_INSTANT_ID);
        chatMessage.setFromUserName(AppConstant.DYNAMIC_INSTANT_ID);
        chatMessage.setToUserId(coreManager.getSelf().getUserId());
        int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + coreManager.getSelf().getUserId() + coreManager.getSelf().getUserId(), 0);
        chatMessage.setIsReadDel(isReadDel);
        chatMessage.setUpload(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), chatMessage.getFromUserId(), chatMessage)) {
            Intent intent = new Intent(mContext, InstantMessageActivity.class);
            intent.putExtra("fromUserId", chatMessage.getFromUserId());
            intent.putExtra("messageId", chatMessage.getPacketId());
            mContext.startActivity(intent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventPublic message) {
        collection(message.getMsgId(), message.getUrl(), message.getUserId());
    }

    private void collection(String messageId, String url, String mUserId) {
        Map<String, String> params = new HashMap<>();
        params.put("emoji", collectionParam(messageId, url, mUserId));

        HttpUtils.post().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private String collectionParam(String messageId, String url, String mUserId) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = 9;
        String msg = url;
        String id = messageId;

        json.put("type", String.valueOf(type));
        json.put("msg", msg);
        json.put("url", msg);
        json.put("collectType", 1);
        json.put("collectMsgId", id);
        json.put("toUserId", mUserId);
        json.put("targetType", 1); // 收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊

        array.add(json);
        return JSON.toJSONString(array);
    }
}