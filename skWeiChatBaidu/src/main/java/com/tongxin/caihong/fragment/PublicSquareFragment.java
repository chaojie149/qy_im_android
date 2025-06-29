package com.tongxin.caihong.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.adapter.PublicMessageRecyclerAdapter;
import com.tongxin.caihong.bean.EventRefreshPublicSquare;
import com.tongxin.caihong.bean.circle.Comment;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.event.MessageEventHongdian;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.CircleMessageDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.circle.MessageEventComment;
import com.tongxin.caihong.ui.circle.MessageEventReply;
import com.tongxin.caihong.ui.circle.SelectPicPopupWindow;
import com.tongxin.caihong.ui.circle.range.NewZanActivity;
import com.tongxin.caihong.ui.circle.range.SendAudioActivity;
import com.tongxin.caihong.ui.circle.range.SendFileActivity;
import com.tongxin.caihong.ui.circle.range.SendShuoshuoActivity;
import com.tongxin.caihong.ui.circle.range.SendVideoActivity;
import com.tongxin.caihong.ui.circle.util.LifeCircleHelper;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.TrillCommentInputDialog;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.builder.BaseBuilder;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import fm.jiecao.jcvideoplayer_lib.MessageEventPublic;
import fm.jiecao.jcvideoplayer_lib.MessageEventVideo;
import okhttp3.Call;

public class PublicSquareFragment extends EasyFragment implements PublicMessageRecyclerAdapter.OnMessageActionListener {
    private static final String TAG = "PublicSquareFragment";
    private static final int REQUEST_CODE_SEND_MSG = 1;
    private static int PAGER_SIZE = 10;
    private boolean postRefresh;
    private SmartRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mPager;
    private PublicMessageRecyclerAdapter mAdapter;
    private List<PublicMessage> mMessages = new ArrayList<>();
    private String mUserId;
    private String mUserName;
    private boolean more;
    private String messageId;
    private int pageIndex;
    private int mIndex = 0;
    private List<String> tabList;
    private SelectPicPopupWindow menuWindow;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            if (menuWindow != null) {
                // 顶部一排按钮复用这个listener, 没有menuWindow,
                menuWindow.dismiss();
            }
            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.btn_send_picture:
                    // 发表图文，
                    intent.setClass(getActivity(), SendShuoshuoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_voice:
                    // 发表语音
                    intent.setClass(getActivity(), SendAudioActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_video:
                    // 发表视频
                    intent.setClass(getActivity(), SendVideoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_file:
                    // 发表文件
                    intent.setClass(getActivity(), SendFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.new_comment:
                    // 最新评论&赞
                    Intent intent2 = new Intent(getActivity(), NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    EventBus.getDefault().post(new MessageEventHongdian(0));
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_public_square;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBusHelper.register(this);
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        mUserId = coreManager.getSelf().getUserId();
        mUserName = coreManager.getSelf().getNickName();
        initData();
        initView();
    }

    private void initData() {
    }

    private void initView() {
        if (requireActivity() instanceof MainActivity) {
            findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        } else {
            findViewById(R.id.iv_title_left).setOnClickListener(this);
        }
        findViewById(R.id.iv_title_add).setOnClickListener(this);

        mRefreshLayout = findViewById(R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            requestData(true);
        });
        mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            requestData(false);
        });
        mPager = findViewById(R.id.rv_pager);
        ViewGroup llTab = findViewById(R.id.llTab);
        tabList = LifeCircleHelper.getSquareTagList(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View.OnClickListener tabClickListener = v -> {
            for (int i = 0; i < llTab.getChildCount(); i++) {
                View tab = llTab.getChildAt(i);
                TextView textView = tab.findViewById(R.id.tvText);
                if (tab == v) {
                    mIndex = i;
                    requestData(true);
                    textView.setTextSize(18);
                    textView.setTextColor(v.getContext().getResources().getColor(R.color.black_new_title));
                    textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                } else {
                    textView.setTextSize(14);
                    textView.setTextColor(v.getContext().getResources().getColor(R.color.text_color));
                    textView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                }
            }
        };
        for (int i = 0; i < tabList.size(); i++) {
            View tabView = inflater.inflate(R.layout.tab_contact_pager, llTab, false);
            llTab.addView(tabView);
            TextView textView = tabView.findViewById(R.id.tvText);
            textView.setText(tabList.get(i));
            tabView.setOnClickListener(tabClickListener);
        }
        llTab.getChildAt(0).performClick();

        mAdapter = new PublicMessageRecyclerAdapter(getActivity(), coreManager, mMessages);
        mAdapter.setOnMessageActionListener(this);
        mPager.setLayoutManager(new LinearLayoutManager(requireContext()));
        mPager.setAdapter(mAdapter);

        if (requireContext().getApplicationContext().getResources().getConfiguration().uiMode == 0x21) {
            // 安卓10的夜间模式，也就是深色模式，隐藏这个白色的渐变，
            findViewById(R.id.vTabCover).setVisibility(View.GONE);
        } else {
            findViewById(R.id.vTabCover).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SEND_MSG) {
            if (resultCode == Activity.RESULT_OK) {
                // 发布说说成功,刷新Fragment
                String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
                CircleMessageDao.getInstance().addMessage(mUserId, messageId);
                requestData(true);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventRefreshPublicSquare message) {
        requestData(true);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        if (message.message.equals("prepare")) {
            // 准备播放视频，关闭语音播放
            mAdapter.stopVoice();
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
        int isReadDel = PreferenceUtils.getInt(requireContext(), Constants.MESSAGE_READ_FIRE + coreManager.getSelf().getUserId() + coreManager.getSelf().getUserId(), 0);
        chatMessage.setIsReadDel(isReadDel);
        chatMessage.setUpload(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), chatMessage.getFromUserId(), chatMessage)) {
            Intent intent = new Intent(requireContext(), InstantMessageActivity.class);
            intent.putExtra("fromUserId", chatMessage.getFromUserId());
            intent.putExtra("messageId", chatMessage.getPacketId());
            requireContext().startActivity(intent);
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
                            Toast.makeText(requireContext(), requireContext().getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(requireContext(), result.getResultMsg());
                        } else {
                            ToastUtil.showToast(requireContext(), R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(requireContext());
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

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventComment message) {
        TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(getActivity(),
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
                    addComment(message, comment);
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
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(getActivity(), getString(R.string.replay) + "：" + message.comment.getNickName(),
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
                        Reply(message, comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        }
    }

    private void addComment(MessageEventComment message, final Comment comment) {
        String messageId = message.id;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        if (comment.isReplaySomeBody()) {
            params.put("toUserId", comment.getToUserId() + "");
            params.put("toNickname", comment.getToNickname());
            params.put("toBody", comment.getToBody());
        }
        params.put("body", comment.getBody());

        HttpUtils.post().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        if (getContext() != null && Result.checkSuccess(requireContext(), result)) {
                            comment.setCommentId(result.getData());
                            message.pbmessage.setCommnet(message.pbmessage.getCommnet() + 1);
                            PublicMessageRecyclerAdapter.CommentAdapter adapter = (PublicMessageRecyclerAdapter.CommentAdapter) message.view.getAdapter();
                            adapter.addComment(comment);
                            mAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    /**
     * 回复
     */
    private void Reply(MessageEventReply event, final Comment comment) {
        final int position = event.id;
        final PublicMessage message = mMessages.get(position);
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
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        if (getContext() != null && Result.checkSuccess(requireContext(), result)) {
                            comment.setCommentId(result.getData());
                            message.setCommnet(message.getCommnet() + 1);
                            PublicMessageRecyclerAdapter.CommentAdapter adapter = (PublicMessageRecyclerAdapter.CommentAdapter) event.view.getAdapter();
                            adapter.addComment(comment);
                            mAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    private void requestData(boolean isPullDownToRefresh) {
        if (!isResumed()) {
            postRefresh = true;
            return;
        }
        if (isPullDownToRefresh) {// 上拉刷新
            messageId = null;
            pageIndex = 0;
            more = true;
            mMessages.clear();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        if (!more) {
            // ToastUtil.showToast(getContext(), getString(R.string.tip_last_item));
            mRefreshLayout.setNoMoreData(true);
            refreshComplete();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(pageIndex));
        params.put("pageSize", String.valueOf(PAGER_SIZE));
        if (messageId != null) {
            params.put("messageId", messageId);
        }
        DialogHelper.showDefaulteMessageProgressDialog(requireContext());
        BaseBuilder builder = LifeCircleHelper.buildSquareParams(coreManager.getConfig().MSG_SQUARE_TAG_LIST, params, mIndex);
        builder.build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(ArrayResult<PublicMessage> result) {
                        DialogHelper.dismissProgressDialog();
                        if (getContext() != null && Result.checkSuccess(requireContext(), result)) {
                            List<PublicMessage> data = result.getData();
                            if (isPullDownToRefresh) {
                                mMessages.clear();
                            }
                            if (data != null && data.size() > 0) {
                                int size = data.size();
                                for (PublicMessage m : data) {
                                    mMessages.add(m);
                                    if (!TextUtils.isEmpty(m.getId())) {
                                        size--;
                                    } else {
                                        // 记录最后一条说说的id
                                        messageId = m.getMessageId();
                                    }
                                }
                                ++pageIndex;
                                if (size == PAGER_SIZE) {
                                    more = true;
                                    mRefreshLayout.resetNoMoreData();
                                } else {
                                    // 服务器返回未满10条，下拉不在去请求
                                    more = false;
                                }
                            } else {
                                // 服务器未返回数据，下拉不再去请求
                                more = false;
                            }
                            mAdapter.notifyDataSetChanged();
                            refreshComplete();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        if (getContext() != null) {
                            ToastUtil.showNetError(requireContext());
                            refreshComplete();
                        }
                    }
                });
    }

    /**
     * 停止刷新动画
     */
    private void refreshComplete() {
        mPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.finishRefresh();
                mRefreshLayout.finishLoadMore();
            }
        }, 200);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.d(TAG, "setUserVisibleHint() called with: isVisibleToUser = [" + isVisibleToUser + "]");
        super.setUserVisibleHint(isVisibleToUser);
        boolean foreground = isVisibleToUser;
        if (!foreground) {
            release();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.d(TAG, "onHiddenChanged() called with: hidden = [" + hidden + "]");
        boolean foreground = !hidden;
        if (!foreground) {
            release();
        }
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshIfNeed();
    }

    private void refreshIfNeed() {
        if (postRefresh) {
            postRefresh = false;
            requestData(true);
        }
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }

    private void release() {
        // 退出页面时关闭视频和语音，
        JCVideoPlayer.releaseAllVideos();
        if (mAdapter != null) {
            mAdapter.stopVoice();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_title_left:
                requireActivity().finish();
                break;
            case R.id.iv_title_add:
                menuWindow = new SelectPicPopupWindow(getActivity(), itemsOnClick);
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                menuWindow.showAsDropDown(v,
                        -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                        0);
                break;
        }
    }

}
