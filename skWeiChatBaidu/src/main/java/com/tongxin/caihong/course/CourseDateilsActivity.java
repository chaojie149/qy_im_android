package com.tongxin.caihong.course;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.MessageSendChat;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.CourseChatBean;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.FloatWindowPermissionChecker;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.ChatContentView;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 我的课件
 *
 * @author Dean Tao
 * @version 1.0
 */
public class CourseDateilsActivity extends BaseActivity {
    public static boolean isCourseDetailClassSendHandler;
    boolean isRun;
    int mPos;
    private String title;
    private String courseId;
    private ChatContentView mChatContentView;
    private List<ChatMessage> mChatMessages;
    private String toUserId;
    private String mLoginUserId;
    private boolean isGroup;
    private TextView mTvSuspen;
    private SuspenionWondow windowManager;
    private boolean isAllowSendFile;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            // todo 因为LocalCourseActivity也有一个mHandlerChat，需要坐下兼容处理
            if (msg.what == -1) {
                isCourseDetailClassSendHandler = false;
                // loadingDialog.dismiss();
                Constants.IS_SENDONG_COURSE_NOW = false;// 课程发送完成，修改发送状态
                hideSuspensionWondow();
                ToastUtil.showToast(CourseDateilsActivity.this, getString(R.string.tip_course_send_success));
                // 更新消息界面
                MsgBroadcast.broadcastMsgUiUpdate(CourseDateilsActivity.this);
                return;
            }
            if (!coreManager.isLogin()) {
                Log.e("course", "!coreManager.isLogin()");
                mPos = 0;
                isRun = false;
                isCourseDetailClassSendHandler = false;
                Constants.IS_SENDONG_COURSE_NOW = false;
                hideSuspensionWondow();
                MsgBroadcast.broadcastMsgUiUpdate(CourseDateilsActivity.this);
                return;
            }
            isCourseDetailClassSendHandler = true;
            mTvSuspen.setText(getString(R.string.sending_message_index_place_holder, msg.what + 1));

            ChatMessage chatMessage = mChatMessages.get(msg.what).cloneAll();
            if (!isAllowSendFile &&
                    chatMessage.getType() == XmppMessage.TYPE_FILE) {
                ToastUtil.showToast(mContext, getString(R.string.tip_cannot_upload_course_filter_file));
                return;
            }
            // cloneAll，一些不必要的都clone下来了
            chatMessage.setFromId("");
            chatMessage.setToId("");
            chatMessage.setFromUserId(mLoginUserId);
            chatMessage.setFromUserName(coreManager.getSelf().getNickName());
            chatMessage.setToUserId(toUserId);
            chatMessage.setIsReadDel(0);
            chatMessage.setMySend(true);
            chatMessage.setGroup(isGroup);
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());

            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, toUserId, chatMessage);
            MessageSendChat chat = new MessageSendChat(isGroup, toUserId, chatMessage);
            EventBus.getDefault().post(chat);
        }
    };
    private Runnable sendMessageTask = new Runnable() {
        @Override
        public void run() {
            while (isRun) {
                mHandler.sendEmptyMessage(mPos);
                mPos++;
                if (mPos == mChatMessages.size()) {
                    // 最后一条已发送完成
                    isRun = false;
                    SystemClock.sleep(200);
                    mHandler.sendEmptyMessage(-1);
                } else {
                    long sleepTime = 100;
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_course_dateils);
        courseId = getIntent().getStringExtra("data");
        title = getIntent().getStringExtra("title");
        mChatMessages = new ArrayList<>();
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(title);
    }

    private void initView() {
        mChatContentView = (ChatContentView) findViewById(R.id.chat_content_view);
        mChatContentView.setToUserId("123");
        mChatContentView.setMessageEventListener(new ChatContentView.MessageEventListener() {

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
            public void onMessageClick(ChatMessage chatMessage) {
                // 删除
                deleteMessage(chatMessage);
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
            public void onNickNameClick(String friendUserId) {

            }

            @Override
            public void onCallListener(int type) {

            }

            @Override
            public void LongAvatarClick(ChatMessage chatMessage) {

            }
        });
        mChatContentView.setNeedRefresh(false);

        mChatContentView.setChatListType(ChatContentView.ChatListType.COURSE);

        ButtonColorChange.colorChange(mContext, findViewById(R.id.sure_btn));
        findViewById(R.id.sure_btn).setOnClickListener(v -> {
            if (mChatMessages == null || mChatMessages.size() == 0) {
                Toast.makeText(CourseDateilsActivity.this, getString(R.string.tip_get_course_detail_faled), Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(mContext, SelectFriendsActivity.class));
            SelectFriendsActivity.isIntentLocalCourseActivity = false;
        });
        loadData();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventSendCourse message) {
        toUserId = message.getToUserId();
        isGroup = message.isGroup();

        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, toUserId);
        if (friend.getRoomFlag() != 0) {
            boolean admin = false;
            RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(friend.getRoomId(), mLoginUserId);
            if (member != null && member.isGroupOwnerOrManager()) {
                admin = true;
            }
            boolean isAllowSendFileAgain = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + toUserId, true);
            if (!isAllowSendFileAgain && !admin) {
                isAllowSendFile = false;
            } else {
                isAllowSendFile = true;
            }
        } else {
            isAllowSendFile = true;
        }

        sendRecordMessage();
    }

    /**
     * 加载数据
     */
    private void loadData() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("courseId", courseId);
        HttpUtils.get().url(coreManager.getConfig().USER_COURSE_DATAILS)
                .params(params)
                .build()
                .execute(new ListCallback<CourseChatBean>(CourseChatBean.class) {
                    @Override
                    public void onResponse(ArrayResult<CourseChatBean> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            fromatDatas(result.getData());
                            mChatContentView.setData(mChatMessages);
                            mChatContentView.headerRefreshingCompleted();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(CourseDateilsActivity.this);
                        mChatContentView.headerRefreshingCompleted();
                    }
                });
    }

    private void fromatDatas(List<CourseChatBean> result) {
        mChatMessages.clear();
        for (int i = 0; i < result.size(); i++) {
            CourseChatBean data = result.get(i);
            String messageBody = data.getMessage();
            ChatMessage chatMessage = JSON.parseObject(messageBody, com.tongxin.caihong.socket.msg.ChatMessage.class).toSkMessage(coreManager.getSelf().getUserId());
            ChatMessageDao.getInstance().decrypt(chatMessage.isGroup(), chatMessage);
            if (chatMessage.getType() == XmppMessage.TYPE_IMAGE
                    || chatMessage.getType() == XmppMessage.TYPE_VIDEO
                    || chatMessage.getType() == XmppMessage.TYPE_FILE) {
                chatMessage.setUploadSchedule(100);
                chatMessage.setUpload(true);
            }
            // messageI/from/to...在messageHead内
            try {
                JSONObject jsonObject1 = new JSONObject(messageBody);
                JSONObject jsonObject2 = jsonObject1.getJSONObject("messageHead");
                String messageId = jsonObject2.getString("messageId");
                chatMessage.setPacketId(messageId);
                chatMessage.setMySend(true);
                chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                mChatMessages.add(chatMessage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从课件里删除某条消息
     *
     * @param chatMessage 要删除的消息
     */
    private void deleteMessage(final ChatMessage chatMessage) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("courseMessageId", chatMessage.getPacketId());
        params.put("courseId", courseId);
        params.put("updateTime", TimeUtils.sk_time_current_time() + "");
        HttpUtils.get().url(coreManager.getConfig().USER_EDIT_COURSE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(CourseDateilsActivity.this, getString(R.string.delete_success));
                            deleteAdapterData(chatMessage);
                            mChatContentView.setData(mChatMessages);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(CourseDateilsActivity.this);
                    }
                });
    }

    private boolean deleteCourse() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("courseId", courseId);

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_COURSE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            EventBus.getDefault().post(new EventCourseRemoved(courseId));
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, R.string.delete_failed);
                    }
                });

        return true;
    }

    private void deleteAdapterData(ChatMessage chatMessage) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            ChatMessage chat = mChatMessages.get(i);
            if (chatMessage.getPacketId().equals(chat.getPacketId())) {
                mChatMessages.remove(i);
                if (mChatMessages.isEmpty()) {
                    deleteCourse();
                }
                return;
            }
        }
    }

    /**
     * 显示悬浮窗
     */
    private void showSuspensionWondow() {
        mTvSuspen = new TextView(this);
        mTvSuspen.setGravity(Gravity.CENTER);
        mTvSuspen.setBackgroundResource(R.drawable.course_connors);
        mTvSuspen.setTextAppearance(this, R.style.TextStyle);
        mTvSuspen.setText(R.string.sending_course);
        windowManager = new SuspenionWondow(CourseDateilsActivity.this);
        windowManager.show(mTvSuspen);
    }

    /**
     * 隐藏悬浮窗
     */
    private void hideSuspensionWondow() {
        windowManager.hide();
    }

    /**
     * 发送课程
     */
    private void sendRecordMessage() {
        if (AppUtils.checkAlertWindowsPermission(this)) { // 已开启悬浮窗权限
            showSuspensionWondow();
            mPos = 0;
            isRun = true;
            new Thread(sendMessageTask).start();
        } else {
            Constants.IS_SENDONG_COURSE_NOW = false;

            SelectionFrame selectionFrame = new SelectionFrame(this);
            selectionFrame.setSomething(null, getString(R.string.av_no_float), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    FloatWindowPermissionChecker.tryJumpToPermissionPage(CourseDateilsActivity.this);
                }
            });
            selectionFrame.show();
        }
    }
}
