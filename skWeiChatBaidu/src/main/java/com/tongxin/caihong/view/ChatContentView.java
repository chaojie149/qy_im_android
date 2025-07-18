package com.tongxin.caihong.view;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.audio_x.VoiceManager;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AtCopyHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.RecognizerHelper;
import com.tongxin.caihong.helper.TranslateHelper;
import com.tongxin.caihong.service.bean.Question;
import com.tongxin.caihong.socket.EMConnectionManager;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.EventMoreSelected;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.ui.message.MessageRemindActivity;
import com.tongxin.caihong.ui.message.multi.RoomReadListActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.ChatMsgUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.util.secure.AES;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.view.chatHolder.AChatHolderInterface;
import com.tongxin.caihong.view.chatHolder.ChatHolderFactory;
import com.tongxin.caihong.view.chatHolder.ChatHolderFactory.ChatHolderType;
import com.tongxin.caihong.view.chatHolder.ChatHolderListener;
import com.tongxin.caihong.view.chatHolder.MessageEventClickFire;
import com.tongxin.caihong.view.chatHolder.TextReplayViewHolder;
import com.tongxin.caihong.view.chatHolder.TextViewHolder;
import com.tongxin.caihong.view.chatHolder.VoiceViewHolder;
import com.tongxin.caihong.view.selectable.SelectableTextHelper;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

@SuppressWarnings("unused")
public class ChatContentView extends PullDownListView implements ChatBottomView.MoreSelectMenuListener {
    public static Map<String, String> fireVoiceStopMap = new HashMap<>();
    // 好友备注只有群组才有
    public Map<String, Bitmap> mCacheMap = new HashMap<>();
    public boolean isDevice;
    Map<String, String> cache = new HashMap<>();
    private boolean isGroupChat;// 用于标记聊天界面是否为群聊界面
    private boolean isShowReadPerson;// 是否展示群已读人数
    private boolean isShowMoreSelect;// 用于标记是否显示多选框
    private boolean isScrollBottom;// 是否正在底部，如果是，来新消息时需要跳到底部，
    private int mGroupLevel = 3;// 我在当前群组的职位，用于控制群组消息能否撤回(default==3普通成员)
    private boolean isSendRead = true;
    private int mCurClickPos = -1;// 当前点击的position
    private String mRoomNickName; // 我在房间的昵称，只有群聊才有
    private String mToUserId;// 根据self.userId和mToUserId 唯一确定一张表
    private String mRoomId;
    private User mLoginUser;// 当前登录的用户
    private Context mContext; // 界面上下文
    private ChatListType mCurChatType; // 标记当前处于什么界面
    private LayoutInflater mInflater;// 布局填充器
    private ChatBottomView mChatBottomView; // 聊天输入框控件
    private AutoVoiceModule aVoice; // 自动播放下一条未读语音消息的模块
    private ChatContentAdapter mChatContentAdapter;// 消息适配器
    private MessageEventListener mMessageEventListener; // 消息体点击监听
    private ChatTextClickPpWindow mChatPpWindow; // 长按选择框
    // 当前适配器数据源
    private List<ChatMessage> mChatMessages;
    // 即将要删除的消息的packedId列表
    private Set<String> mDeletedChatMessageId = new HashSet<>();
    // 阅后即焚 正在倒计时缓存的时间列表
    private Map<String, CountDownTimer> mTextBurningMaps = new HashMap<>();
    // 所有阅后即焚的语音消息列表
    private Map<String, String> mFireVoiceMaps = new HashMap<>();
    // 好友备注只有群组才有
    private Map<String, String> mRemarksMap = new HashMap<>();
    private Map<String, String> mNicknameMap = new HashMap<>();
    // 群管理头像
    private Map<String, Integer> memberMap = new HashMap<>();
    // 记录文本item点击事件
    private Map<String, Long> clickHistoryMap = new HashMap<>();
    // 所有隐身账号，
    private Set<String> hidingSet = new HashSet<>();

    // 滚动到底部
    private Runnable mScrollTask = new Runnable() {
        @Override
        public void run() {
            if (mChatMessages == null) {
                return;
            }
            setSelection(mChatMessages.size());
        }
    };
    private Collection<OnScrollListener> onScrollListenerList = new ArrayList<>();
    private boolean secret;
    private TranslateHelper.AutoTranslateHandler autoTranslateHandler = new TranslateHelper.AutoTranslateHandler(this);
    private GestureDetector gestureDetector;
    private SelectableTextHelper mSelectableTextHelper;
    private boolean isSearch;
    private VoicePlayListener voicePlayListener;

    public ChatContentView(Context context) {
        this(context, null);
    }

    public ChatContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // 输入法弹起时让界面跟着上去
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldh > h) {
            removeCallbacks(mScrollTask);
            //  int delay = getResources().getInteger(android.R.integer.config_shortAnimTime); // 200
            if (isSearch) {
                // 每次进入聊天界面都回调了这个方法，当搜索进入且150ms内查询出消息且刷新好界面之后，又被这个task滚动到最底部了，加个标志位判断下
                isSearch = false;
                return;
            }
            postDelayed(mScrollTask, 150);
        }
        if (mChatPpWindow != null) {
            mChatPpWindow.dismiss();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        voicePlayListener = new VoicePlayListener();
        VoicePlayer.instance().addVoicePlayListener(voicePlayListener);
    }

    private void init(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        setCacheColorHint(0x00000000);
        mLoginUser = CoreManager.requireSelf(context);
        mRoomNickName = mLoginUser.getNickName();
        aVoice = new AutoVoiceModule();


        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                for (OnScrollListener listener : onScrollListenerList) {
                    listener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                isScrollBottom = firstVisibleItem + visibleItemCount >= totalItemCount;
                for (OnScrollListener listener : onScrollListenerList) {
                    listener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }
        });

        mChatContentAdapter = new ChatContentAdapter();
        setAdapter(mChatContentAdapter);
        TranslateHelper.init(this, mChatContentAdapter, autoTranslateHandler);
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mChatPpWindow != null && mChatPpWindow.isShowing()) {
                    mChatPpWindow.dismiss();
                }
                return super.onSingleTapUp(e);
            }
        });
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    if (mSelectableTextHelper != null) {
                        mSelectableTextHelper.onScrolling();
                    }
                } else if (scrollState == SCROLL_STATE_IDLE) {
                    if (mSelectableTextHelper != null) {
                        int firstVisibleItem = view.getFirstVisiblePosition();
                        int lastVisiblePosition = view.getLastVisiblePosition();
                        if (mSelectableTextHelper.getPosition() < firstVisibleItem
                                || mSelectableTextHelper.getPosition() > lastVisiblePosition) {
                            mSelectableTextHelper.destroy();
                            mSelectableTextHelper = null;
                        } else {
                            mSelectableTextHelper.onScrollIdle();
                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    public void autoTranslate() {
        if (!TranslateHelper.isAutoTranslate(mContext, mLoginUser.getUserId(), mToUserId)) {
            Log.v("translate", "autoTranslate() disabled: " + mToUserId);
            return;
        }
        Log.d("translate", "autoTranslate() called");
        if (getFirstVisiblePosition() < 0 || getLastVisiblePosition() <= getFirstVisiblePosition()) {
            return;
        }
        for (ChatMessage chatMessage : mChatMessages.subList(getFirstVisiblePosition(), getLastVisiblePosition())) {
            if (TranslateHelper.needAutoTranslate(chatMessage)) {
                if (chatMessage.getIsReadDel()
                        && !chatMessage.isSendRead()) {
                    // 阅后即焚且未读，不自动翻译
                    continue;
                }
                TranslateHelper.translate(this, chatMessage);
            }
        }
    }

    public void onRecognizerResult(ChatMessage message) {
        onTranslateResult(message);
    }

    public void onTranslateResult(ChatMessage message) {
        ChatMessageDao.getInstance().updateTranslation(mLoginUser.getUserId(),
                message.isMySend() ? message.getToUserId() : message.getFromUserId(),
                message.getPacketId(), message.getTranslation());
        for (ChatMessage m : mChatMessages) {
            if (TextUtils.equals(m.getPacketId(), message.getPacketId())) {
                m.setTranslation(message.getTranslation());
                notifyDataSetInvalidated(shouldScrollToBottom());
            }
        }
    }

    // 返回当前是否在底部
    public boolean shouldScrollToBottom() {
        return isScrollBottom;
    }

    // 点击空位置收起输入法软键盘
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mMessageEventListener != null) {
                mMessageEventListener.onEmptyTouch();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setSelection(int position) {
        if (position >= mChatMessages.size() - 1) { // position是可以大于最后一个索引的，
            // 跳到最后一个元素的情况统一从底部显示，也就是跳到像素级最后一行，y给个够大值，又不能是Integer.MIN_VALUE会乱跳，
            setSelectionFromTop(position, -100000000);
        } else {
            super.setSelection(position);
        }
    }

    // 设置对方的userid
    public void setToUserId(String toUserId) {
        mToUserId = toUserId;
    }

    // 设置对方的userid
    public void setRoomId(String roomid) {
        mRoomId = roomid;
    }

    // 修改为多选
    public void setIsShowMoreSelect(boolean isShowMoreSelect) {
        this.isShowMoreSelect = isShowMoreSelect;
    }

    // 设置当前是否是群组，群组中显示的昵称
    public void setCurGroup(boolean group, String nickName) {
        isGroupChat = group;
        if (!TextUtils.isEmpty(nickName)) {
            mRoomNickName = nickName;
        }
        if (mRemarksMap.size() == 0) {
            AsyncUtils.doAsync(mContext, new AsyncUtils.Function<AsyncUtils.AsyncContext<Context>>() {
                @Override
                public void apply(AsyncUtils.AsyncContext<Context> contextAsyncContext) throws Exception {
                    List<Friend> friendList = FriendDao.getInstance().getAllFriendsNotFilterStatus(mLoginUser.getUserId());
                    for (int i = 0; i < friendList.size(); i++) {
                        if (!TextUtils.isEmpty(friendList.get(i).getRemarkName())) {
                            mRemarksMap.put(friendList.get(i).getUserId(), friendList.get(i).getRemarkName());
                        } else {
                            mNicknameMap.put(friendList.get(i).getUserId(), friendList.get(i).getNickName());
                        }
                    }
                }
            });
        }
    }

    public void putRemarkName(String userId, String name) {
        if (TextUtils.isEmpty(name)) {
            mRemarksMap.remove(userId);
            return;
        }
        mRemarksMap.put(userId, name);
    }

    public void putNickname(String userId, String name) {
        if (TextUtils.isEmpty(name)) {
            mNicknameMap.remove(userId);
            return;
        }
        mNicknameMap.put(userId, name);
    }

    public void setSearch(boolean isSearch) {
        this.isSearch = isSearch;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public ChatTextClickPpWindow getChatPpWindow() {
        return mChatPpWindow;
    }

    // 设置当前用户在群组的的权限
    public void setRole(int role) {
        this.mGroupLevel = role;
    }

    public void setIsSendRead(boolean isSendRead) {
        this.isSendRead = isSendRead;
    }

    public void setChatBottomView(ChatBottomView chatBottomView) {
        this.mChatBottomView = chatBottomView;
        if (mChatBottomView != null) {
            mChatBottomView.setMoreSelectMenuListener(this);
        }
    }

   /* private boolean filterBackMsgEnable;

    public void filterBackMsgEnable(boolean isEnable) {
        this.filterBackMsgEnable = isEnable;
    }*/

    public void setData(List<ChatMessage> chatMessages) {
        mChatMessages = chatMessages;
        if (mChatMessages == null) {
            mChatMessages = new ArrayList<>();
        }
       /* if (filterBackMsgEnable) {
            ChatMsgUtils.filterBackMsg(mChatMessages);
        }*/
        notifyDataSetChanged();
    }

    // 设置管理员头像控件
    public void setRoomMemberList(List<RoomMember> memberList) {
        memberMap.clear();
        for (RoomMember member : memberList) {
            memberMap.put(member.getUserId(), member.getRole());
            if (member.getHiding() == 1) {
                addHidingUser(member.getUserId());
            }
        }
/*
        if (shouldScrollToBottom()) {
            notifyDataSetInvalidated(true);
        } else {
            notifyDataSetChanged();
        }
*/
        notifyDataSetChanged();
    }

    public void putRoomMember(String userId, int role) {
        memberMap.put(userId, role);
        notifyDataSetChanged();
    }

    public void addHidingUser(String userId) {
        hidingSet.add(userId);
    }

    public void removeHidingUser(String userId) {
        hidingSet.remove(userId);
    }

    // 使用动画删除某一条消息
    public void removeItemMessage(final String packedId) {
        mDeletedChatMessageId.add(packedId);
        notifyDataSetChanged();
    }

    // 界面更新
    public void notifyDataSetInvalidated(final int position) {
        notifyDataSetChanged();
        if (mChatMessages.size() > position) {
            this.post(() -> setSelection(position));
        }
    }

    /**
     * 用于在列表头部添加元素后更新列表使用，
     * 封装处理更新后跳到正确位置，
     */
    public void notifyDataSetAddedItemsToTop(final int count) {
        int oldPosition = getFirstVisiblePosition();
        View firstView = getChildAt(0);
        int oldTop = firstView == null ? 0 : firstView.getTop();
        notifyDataSetChanged();
        int position = count + oldPosition;
        if (mChatMessages.size() > position) {
            // 不知道之前为什么用post, 会导致明显跳到前面触发onScroll再闪下来，
            // 保留其他方法的post, 只改这里，
            setSelectionFromTop(position, oldTop);
        }
    }

    public void notifyDataSetInvalidated(boolean scrollToBottom) {
/*
        notifyDataSetChanged();
        if (scrollToBottom && mChatMessages.size() > 0) {
            setSelection(mChatMessages.size());
        }
*/
        // 还是会有一些调用setSelection方法没有生效的地方，干脆全部调用notifyDataSetInvalidatedForSetSelectionInvalid方法算了
        notifyDataSetInvalidatedForSetSelectionInvalid(scrollToBottom);
    }

    // 群组偶现第一次加载setSelection方法失效的问题 专门封装一个方法针对此问题
    // https://blog.csdn.net/santamail/article/details/38821763
    // 公众号聊天出现同样问题，所以单聊页面同样处理，
    public void notifyDataSetInvalidatedForSetSelectionInvalid(boolean scrollToBottom) {
       /* if (filterBackMsgEnable) {
            ChatMsgUtils.filterBackMsg(mChatMessages);
        }*/
        notifyDataSetChanged();
        if (scrollToBottom && mChatMessages.size() > 0) {
            if (mChatContentAdapter != null) {
                this.setAdapter(mChatContentAdapter);
            }
            setSelection(mChatMessages.size());
        }
    }

    public void notifyDataSetChanged() {
       /* if (filterBackMsgEnable) {
            ChatMsgUtils.filterBackMsg(mChatMessages);
        }*/
        if (mChatContentAdapter != null) {
            mChatContentAdapter.notifyDataSetChanged();
        }
    }

    // 设置消息解监听
    public void setMessageEventListener(MessageEventListener listener) {
        mMessageEventListener = listener;
    }

    // 指定当前界面是在哪 直播 还是 课程
    public void setChatListType(ChatListType type) {
        mCurChatType = type;
    }

    // 一条消息的删除动画
    private void startRemoveAnim(View view, ChatMessage message, int position) {
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.setAlpha(1f - interpolatedTime);
            }
        };

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mChatMessages.remove(message);
                mDeletedChatMessageId.remove(message);
                view.clearAnimation();
                notifyDataSetChanged();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim.setDuration(1000);
        view.startAnimation(anim);
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 消息业务具体实现 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    // 开始一个阅后即焚的计时器
    private void startCountDownTimer(long time, AChatHolderInterface holder, ChatMessage message) {
        String TAG = "fireTimer";
        if (mTextBurningMaps.containsKey(message.getPacketId())) {
            CountDownTimer mCountDownTimer = mTextBurningMaps.get(message.getPacketId());
            mCountDownTimer.cancel();// 取消上一个事件
            Log.e(TAG, message.getPacketId() + " cancel");
            mTextBurningMaps.remove(message.getPacketId());
        }
        if (time < 1000) {
            mTextBurningMaps.remove(message.getPacketId());
            EventBus.getDefault().post(new MessageEventClickFire("delete", message.getPacketId()));
            removeItemMessage(message.getPacketId());
            return;
        }

        TextView tvFireTime;
        if (message.getType() == XmppMessage.TYPE_TEXT) {
            TextViewHolder textViewHolder = (TextViewHolder) holder;
            tvFireTime = textViewHolder.tvFireTime;
        } else {
            TextReplayViewHolder textReplayViewHolder = (TextReplayViewHolder) holder;
            tvFireTime = textReplayViewHolder.tvFireTime;
        }
        tvFireTime.setTag(R.id.tag_fire_time_message, message);
        CountDownTimer mNewCountDownTimer = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.e(TAG, message.getPacketId() + " onTick");
                ChatMessage tag = (ChatMessage) tvFireTime.getTag(R.id.tag_fire_time_message);
                if (tag != null && !TextUtils.equals(tag.getPacketId(), message.getPacketId())) {
                    // 视图复用，这个timer没用了，
                    cancel();
                    return;
                }
                tvFireTime.setText(String.valueOf(millisUntilFinished / 1000));
                message.setReadTime(millisUntilFinished);
                ChatMessageDao.getInstance().updateMessageReadTime(mLoginUser.getUserId(), message.getFromUserId(), message.getPacketId(), millisUntilFinished);
            }

            @Override
            public void onFinish() {
                mTextBurningMaps.remove(message.getPacketId());
                EventBus.getDefault().post(new MessageEventClickFire("delete", message.getPacketId()));

                removeItemMessage(message.getPacketId());
            }
        }.start();
        Log.e(TAG, message.getPacketId() + " put");
        mTextBurningMaps.put(message.getPacketId(), mNewCountDownTimer);
    }

    // 点击了阅后即焚的文字 || 回复 消息
    private void clickFireText(AChatHolderInterface holder, ChatMessage message) {
        TextView mTvContent, tvFireTime;
        if (message.getType() == XmppMessage.TYPE_TEXT) {
            TextViewHolder textViewHolder = (TextViewHolder) holder;
            mTvContent = textViewHolder.mTvContent;
            tvFireTime = textViewHolder.tvFireTime;
        } else {
            TextReplayViewHolder textReplayViewHolder = (TextReplayViewHolder) holder;
            mTvContent = textReplayViewHolder.mTvContent;
            tvFireTime = textReplayViewHolder.tvFireTime;
        }

        mTvContent.setTextColor(getResources().getColor(R.color.black));
        String s = StringUtils.replaceSpecialChar(message.getContent());
        CharSequence charSequence = HtmlUtils.transform200SpanString(s, true);
        mTvContent.setText(charSequence);

        mTvContent.post(() -> {
            final long time = mTvContent.getLineCount() * 10000;// 计算时间，一行10s
            tvFireTime.setText(String.valueOf(time / 1000));
            tvFireTime.setVisibility(VISIBLE);
            message.setReadTime(time);

            startCountDownTimer(time, holder, message);
        });
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓多选业务具体实现 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    // 判断 是否 一条消息也没有选择
    private boolean isNullSelectMore(List<ChatMessage> list) {
        if (list == null || list.size() == 0) {
            return true;
        }

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isMoreSelected) {
                return false;
            }
        }
        return true;
    }

    private boolean isContainFireMsgSelectMore(List<ChatMessage> list) {
        if (list == null || list.size() == 0) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isMoreSelected && list.get(i).getIsReadDel()) {
                return true;
            }
        }
        return false;
    }

    // 点击了 多选转发按钮
    @Override
    public void clickForwardMenu() {
        final Dialog mForwardDialog = new Dialog(mContext, R.style.BottomDialog);
        View contentView = mInflater.inflate(R.layout.forward_dialog, null);
        mForwardDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        mForwardDialog.setCanceledOnTouchOutside(true);
        mForwardDialog.getWindow().setGravity(Gravity.BOTTOM);
        mForwardDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mForwardDialog.show();
        mForwardDialog.findViewById(R.id.single_forward).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {// 逐条转发
                mForwardDialog.dismiss();
                if (isNullSelectMore(mChatMessages)) {
                    Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转至转发页面
                Intent intent = new Intent(mContext, InstantMessageActivity.class);
                intent.putExtra(Constants.IS_MORE_SELECTED_INSTANT, true);
                mContext.startActivity(intent);
            }
        });
        mForwardDialog.findViewById(R.id.sum_forward).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {// 合并转发
                mForwardDialog.dismiss();
                if (isNullSelectMore(mChatMessages)) {
                    Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转至转发页面
                Intent intent = new Intent(mContext, InstantMessageActivity.class);
                intent.putExtra(Constants.IS_MORE_SELECTED_INSTANT, true);
                intent.putExtra(Constants.IS_SINGLE_OR_MERGE, true);
                mContext.startActivity(intent);
            }
        });
        mForwardDialog.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mForwardDialog.dismiss();
            }
        });
    }

    // 点击了多选收藏按钮
    @Override
    public void clickCollectionMenu() {
        if (isNullSelectMore(mChatMessages)) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }
        if (isContainFireMsgSelectMore(mChatMessages)) {
            ToastUtil.showToast(mContext, mContext.getString(R.string.tip_cannot_collect_burn));
            return;
        }
        if (isContainFireMsgSelectMore(mChatMessages)) {
            ToastUtil.showToast(mContext, mContext.getString(R.string.tip_cannot_collect_burn));
            return;
        }
        String tip;
        if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
            tip = getContext().getString(R.string.tip_collect_allow_type) +
                    getContext().getString(R.string.dont_support_tip, getContext().getString(R.string.collection));
        } else {
            tip = getContext().getString(R.string.tip_collect_allow_type);
        }
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(null, tip, getContext().getString(R.string.cancel), getContext().getString(R.string.collection),
                new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        List<ChatMessage> temp = new ArrayList<>();
                        for (int i = 0; i < mChatMessages.size(); i++) {
                            if (mChatMessages.get(i).isMoreSelected
                                    && TextUtils.isEmpty(mChatMessages.get(i).getSignature())
                                    && (mChatMessages.get(i).getType() == XmppMessage.TYPE_TEXT
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_IMAGE
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_VOICE
                                    || mChatMessages.get(i).getType() == XmppMessage.TYPE_VIDEO
                                    || (mChatMessages.get(i).getType() == XmppMessage.TYPE_FILE && TextUtils.isEmpty(mChatMessages.get(i).getObjectId())))) {
                                // 非加密文件
                                temp.add(mChatMessages.get(i));
                            }
                        }
                        moreSelectedCollection(temp);
                        // 发送EventBus，通知聊天页面解除多选状态
                        EventBus.getDefault().post(new EventMoreSelected("MoreSelectedCollection", false, isGroupChat()));
                    }
                });
        selectionFrame.show();
    }

    // 点击了多选删除按钮
    @Override
    public void clickDeleteMenu() {
        if (isNullSelectMore(mChatMessages)) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog mDeleteDialog = new Dialog(mContext, R.style.BottomDialog);
        View contentView = mInflater.inflate(R.layout.delete_dialog, null);
        mDeleteDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        mDeleteDialog.setCanceledOnTouchOutside(true);
        mDeleteDialog.getWindow().setGravity(Gravity.BOTTOM);
        mDeleteDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mDeleteDialog.show();
        mDeleteDialog.findViewById(R.id.delete_message).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeleteDialog.dismiss();
                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedDelete", false, isGroupChat()));
            }
        });

        mDeleteDialog.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeleteDialog.dismiss();
            }
        });
    }

    // 点击了 更多按钮
    @Override
    public void clickEmailMenu() {
        if (isNullSelectMore(mChatMessages)) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog mEmailDialog = new Dialog(mContext, R.style.BottomDialog);
        View contentView = mInflater.inflate(R.layout.email_dialog, null);
        mEmailDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        mEmailDialog.setCanceledOnTouchOutside(true);
        mEmailDialog.getWindow().setGravity(Gravity.BOTTOM);
        mEmailDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mEmailDialog.show();
        mEmailDialog.findViewById(R.id.save_message).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailDialog.dismiss();
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(null, getContext().getString(R.string.save_only_image), getContext().getString(R.string.cancel), getContext().getString(R.string.save),
                        new SelectionFrame.OnSelectionFrameClickListener() {
                            @Override
                            public void cancelClick() {

                            }

                            @Override
                            public void confirmClick() {
                                for (int i = 0; i < mChatMessages.size(); i++) {
                                    if (mChatMessages.get(i).isMoreSelected && mChatMessages.get(i).getType() == XmppMessage.TYPE_IMAGE) {
                                        FileUtil.downImageToGallery(mContext, mChatMessages.get(i).getContent());
                                    }
                                }
                                EventBus.getDefault().post(new EventMoreSelected("MoreSelectedEmail", false, isGroupChat()));
                            }
                        });
                selectionFrame.show();
            }
        });

        mEmailDialog.findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailDialog.dismiss();
            }
        });
    }

    /**
     * 收藏和存表情共用一个接口，参数也基本相同，
     *
     * @param flag 是收藏就为true, 是存表情就为false,
     */
    private String collectionParam(List<ChatMessage> messageList, boolean flag, boolean isGroup, String toUserId) {
        JSONArray array = new JSONArray();
        for (ChatMessage message : messageList) {
            int type = 6;
            if (flag) {// 收藏
                if (message.getType() == XmppMessage.TYPE_IMAGE) {
                    type = CollectionEvery.TYPE_IMAGE;
                } else if (message.getType() == XmppMessage.TYPE_VIDEO) {
                    type = CollectionEvery.TYPE_VIDEO;
                } else if (message.getType() == XmppMessage.TYPE_FILE) {
                    type = CollectionEvery.TYPE_FILE;
                } else if (message.getType() == XmppMessage.TYPE_VOICE) {
                    type = CollectionEvery.TYPE_VOICE;
                } else if (message.getType() == XmppMessage.TYPE_TEXT || message.getType() == XmppMessage.TYPE_REPLAY) {
                    type = CollectionEvery.TYPE_TEXT;
                } else if (message.getType() == XmppMessage.TYPE_LINK) {
                    type = CollectionEvery.TYPE_LINK;
                }
            }
            JSONObject json = new JSONObject();
            json.put("type", String.valueOf(type));
            json.put("msg", message.getContent());
            if (flag) {
                // 收藏消息id
                json.put("msgId", message.getPacketId());
                if (isGroup) {
                    // 群组收藏需要添加jid
                    json.put("roomJid", toUserId);
                    json.put("targetId", mRoomId);
                    json.put("targetType", 3);
                    json.put("toUserId", message.getFromUserId());
                } else {
                    json.put("userId", message.getFromUserId());
                    json.put("targetId", message.getFromUserId());
                    json.put("targetType", 4);
                    json.put("toUserId", message.getFromUserId());
                }
            } else {
                // 表情url
                json.put("url", message.getContent());
            }
            array.add(json);
        }
        return JSON.toJSONString(array);
    }

    /**
     * 添加为表情 && 收藏
     * 添加为表情Type 6.表情
     * 收藏Type    1.图片 2.视频 3.文件 4.语音 5.文本
     */
    public void collectionEmotion(ChatMessage message, final boolean flag, boolean isGroup, String toUserId) {
        if (TextUtils.isEmpty(message.getContent())) {
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("emoji", collectionParam(Collections.singletonList(message), flag, isGroup, toUserId));

        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (flag) {
                                Toast.makeText(mContext, mContext.getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, mContext.getString(R.string.add_emotion_success), Toast.LENGTH_SHORT).show();
                            }
                            if (!flag) { // 添加为表情
                                // 收藏成功后将对应的url存入内存中，以防下次再次收藏该链接
                                // PreferenceUtils.putInt(mContext, self.getUserId() + message.getContent(), 1);
                                // 发送广播更新收藏列表
                                MyApplication.getInstance().sendBroadcast(new Intent(OtherBroadcast.CollectionRefresh));
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    /**
     * 多选 收藏
     */
    public void moreSelectedCollection(List<ChatMessage> chatMessageList) {
        if (chatMessageList == null || chatMessageList.size() <= 0) {
            Toast.makeText(mContext, mContext.getString(R.string.name_connot_null), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(getContext()).accessToken);
        params.put("emoji", collectionParam(chatMessageList, true, isGroupChat, mToUserId));

        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, mContext.getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        VoicePlayer.instance().removeVoicePlayListener(voicePlayListener);
        autoTranslateHandler.removeCallbacksAndMessages(null);
        for (Map.Entry<String, Bitmap> entry : mCacheMap.entrySet()) {
            Bitmap bitmap = entry.getValue();
            bitmap.recycle();
            bitmap = null;
        }
        mCacheMap.clear();
        System.gc();
    }

    public void addOnScrollListener(OnScrollListener onScrollListener) {
        onScrollListenerList.add(onScrollListener);
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 实体类 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓

    public enum ChatListType {
        // 单聊 直播 课程 设备
        SINGLE, LIVE, COURSE, DEVICE
    }

    // 适配器接口
    public interface MessageEventListener {
        // 点击空白处，让输入框归位
        void onEmptyTouch();

        void onTipMessageClick(ChatMessage message);

        /**
         * @param message 这是点击的消息，不是被回复的消息，
         */
        default void onReplayClick(ChatMessage message) {
        }

        void onMyAvatarClick();

        void onFriendAvatarClick(String friendUserId);

        void LongAvatarClick(ChatMessage chatMessage);

        void onNickNameClick(String friendUserId);

        void onMessageClick(ChatMessage chatMessage);

        void onMessageLongClick(ChatMessage chatMessage);

        void onSendAgain(ChatMessage chatMessage);

        void onMessageBack(ChatMessage chatMessage, int position);

        default void onMessageReplay(ChatMessage chatMessage) {
        }

        default void onMenuQuestionClick(ChatMessage chatMessage, Question question) {
        }

        void onCallListener(int type);
    }

    // 消息适配器
    public class ChatContentAdapter extends BaseAdapter implements ChatHolderListener {

        public int getCount() {
            if (mChatMessages != null) {
                return mChatMessages.size();
            }
            return 0;
        }

        @Override
        public ChatMessage getItem(int position) {
            return mChatMessages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return ChatHolderFactory.viewholderCount();
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage message = getItem(position);
            int messageType = message.getType();

            // 正常情况的 mySend
            boolean mySend = message.isMySend() || mLoginUser.getUserId().equals(message.getFromUserId());

            // 兼容我的设备
            isDevice = mLoginUser.getUserId().equals(message.getFromUserId()) && mLoginUser.getUserId().equals(message.getToUserId());
            if (mySend
                    && !TextUtils.isEmpty(message.getToUserId())
                    && message.getToUserId().contains(mLoginUser.getUserId())
                    && !TextUtils.isEmpty(message.getFromId())) {
                if (message.getFromId().equals(EMConnectionManager.CURRENT_DEVICE)) {
                    mySend = true;
                } else {
                    mySend = false;
                }
            }

            message.setMySend(mySend);

            // 兼容直播间
            ChatHolderType holderType = ChatHolderFactory.getChatHolderType(mySend, message);
            if (mCurChatType == ChatListType.LIVE) {
                holderType = ChatHolderType.VIEW_SYSTEM_LIVE;
            }

            return holderType.ordinal();
        }

        public View createHolder(ChatHolderType holderType, View conver, ViewGroup parent) {
            AChatHolderInterface holder = ChatHolderFactory.getHolder(holderType);

            conver = mInflater.inflate(holder.getLayoutId(holder.isMysend), parent, false);
            holder.mContext = mContext;
            holder.mLoginUserId = mLoginUser.getUserId();
            holder.mLoginNickName = mRoomNickName;
            holder.mToUserId = mToUserId;
            holder.isGounp = isGroupChat();
            holder.isDevice = mCurChatType == ChatListType.DEVICE;
            isShowReadPerson = PreferenceUtils.getBoolean(mContext, Constants.IS_SHOW_READ + mToUserId, false);
            holder.setShowPerson(isShowReadPerson);

            holder.findView(conver);
            holder.addChatHolderListener(this);
            conver.setTag(holder);

            return conver;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ChatMessage message = getItem(position);
            // 统一在getItemViewType中最终处理isMySend,
            ChatHolderType holderType = ChatHolderFactory.getChatHolderType(getItemViewType(position));

            AChatHolderInterface holder;
            if (convertView == null) {
                convertView = createHolder(holderType, convertView, parent);
                holder = (AChatHolderInterface) convertView.getTag();
            } else {
                holder = (AChatHolderInterface) convertView.getTag();
                if (holder.mHolderType != holderType) {
                    convertView = createHolder(holderType, convertView, parent);
                    holder = (AChatHolderInterface) convertView.getTag();
                }
            }

            holder.chatMessages = mChatMessages;
            holder.selfGroupRole = mGroupLevel;
            holder.isSendRead = isSendRead;
            holder.roomId = mRoomId;
            holder.mHolderType = holderType;
            holder.isDevice = isDevice;
            holder.position = position;
            holder.setMultiple(isShowMoreSelect);
            isShowReadPerson = PreferenceUtils.getBoolean(mContext, Constants.IS_SHOW_READ + mToUserId, false);
            holder.setShowPerson(isShowReadPerson);

            // 显示时间
            changeTimeVisible(holder, message);
            // 设置备注，显示与否是在基类实现的
            changeNameRemark(holder, message);

            // todo 因为存储在数据库的端到端消息都是密文，所以取出来显示时需要解密，并将该消息标记为已解密，防止listView 刷新频繁调用解密
            if (!TextUtils.isEmpty(message.getContent()) && !message.isDecrypted()) {
                String key = SecureChatUtil.getSymmetricKey(message.getPacketId());
                try {
                    // 现仅端到端消息在数据库为加密形态而非所有消息了，所以明文为纯=号时，aes解密也能正常解密，而不是抛出Exception，导致content变为空白，此处坐下判断
                    String filter = message.getContent();
                    if (!TextUtils.isEmpty(filter.replaceAll("=", ""))) {
                        String s = AES.decryptStringFromBase64(message.getContent(), Base64.decode(key));
                        message.setContent(s);
                        message.setDecrypted(true);
                    } else {// 纯=号文本-明文
                        message.setDecrypted(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            holder.prepare(message, memberMap.get(message.getFromUserId()), secret, hidingSet.contains(message.getFromUserId()));
            if (mCurChatType == ChatListType.COURSE) {
                holder.doCourse();
            }

            String filePath = UploadCacheUtils.get(mContext, message);
            if (holder.mHolderType == ChatHolderType.VIEW_TO_VOICE) {
                // 阅后即焚语音的处理
                if (!isGroupChat() && message.getIsReadDel()) {
                    if (!TextUtils.isEmpty(filePath) && !mFireVoiceMaps.containsKey(filePath)) {
                        mFireVoiceMaps.put(filePath, message.getPacketId());
                    }
                }

                // 自动播放语音的处理
                if (!message.isSendRead()) {
                    aVoice.put((VoiceViewHolder) holder);
                }
            }

            if (holder.mHolderType == ChatHolderType.VIEW_TO_TEXT
                    || holder.mHolderType == ChatHolderType.VIEW_TO_REPLAY) {// 非群组、对方发送、阅后即焚类型、已发送已读 才显示倒计时
                // 阅后即焚消息显示倒计时控件
                TextViewHolder textViewHolder = null;
                TextReplayViewHolder textReplayViewHolder = null;
                if (holder.mHolderType == ChatHolderType.VIEW_TO_TEXT) {
                    textViewHolder = (TextViewHolder) holder;
                    textViewHolder.showFireTime(mTextBurningMaps.containsKey(message.getPacketId()));
                } else {
                    textReplayViewHolder = (TextReplayViewHolder) holder;
                    textReplayViewHolder.showFireTime(mTextBurningMaps.containsKey(message.getPacketId()));
                }

                // 取出倒计时
                if (!isGroupChat() && message.getIsReadDel() && message.isSendRead()) {
                    if (holder.mHolderType == ChatHolderType.VIEW_TO_TEXT) {
                        textViewHolder.showFireTime(true);
                    } else {
                        textReplayViewHolder.showFireTime(true);
                    }
                    long time = message.getReadTime();
                    startCountDownTimer(time, holder, message);
                }
            }

            // 删除视图的淡出效果，
            convertView.setAlpha(1f);
            if (mDeletedChatMessageId.contains(message.getPacketId())) {
                startRemoveAnim(convertView, message, position);
            }

            return convertView;
        }

        private void changeTimeVisible(AChatHolderInterface holder, ChatMessage message) {
            if (mCurChatType == ChatListType.COURSE) {
                return;
            }
            int position = holder.position;
            // 与上一条消息之间的间隔如果大于5分钟就显示本消息发送时间
            String timeStr = null;
            if (position >= 1) {
                long last = mChatMessages.get(position - 1).getTimeSend();
                if (message.getTimeSend() - last > 5 * 60 * 1000) {// 小于5分钟，不显示
                    timeStr = TimeUtils.sk_time_long_to_chat_time_str(message.getTimeSend());
                }
            }
            holder.showTime(timeStr);
        }

        private void changeNameRemark(AChatHolderInterface holder, ChatMessage message) {
            if (!isGroupChat() || message.isMySend()) {
                return;
            }

            // 优先群主备注，然后好友备注，然后群名片，最后昵称，
            RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mRoomId, message.getFromUserId());
            if (mGroupLevel == 1 || mGroupLevel == 2) {// 群主优先显示自己对群组成员的群内备注
                if (member != null
                        && !TextUtils.isEmpty(member.getCardName())
                        && !TextUtils.equals(member.getUserName(), member.getCardName())) {// 已有群内备注
                    String name = member.getCardName();
                    message.setFromUserName(name);
                    return;
                }
            }
            if (mNicknameMap.containsKey(message.getFromUserId())) {
                message.setFromUserName(mNicknameMap.get(message.getFromUserId()));
            }
            if (member != null
                    && !TextUtils.isEmpty(member.getUserName())) {
                message.setFromUserName(member.getUserName());
            }
            if (mRemarksMap.containsKey(message.getFromUserId())) {
                message.setFromUserName(mRemarksMap.get(message.getFromUserId()));
            }
        }

        public void clickRootItem(AChatHolderInterface holder, ChatMessage message) {
            mCurClickPos = holder.position;

            // 点击了一条阅后即焚的消息 非群组、阅后即焚类型、未发送已读 才可以点击
            if (!isGroupChat() && message.getIsReadDel() && !message.isSendRead()) {
                // 发送已读消息回执，对方收到后会删除该条阅后即焚消息
                if (holder.mHolderType == ChatHolderType.VIEW_TO_TEXT
                        || holder.mHolderType == ChatHolderType.VIEW_TO_REPLAY) {
                    // 通知chatactivity 等待播放完成后删除
                    EventBus.getDefault().post(new MessageEventClickFire("delay", message.getPacketId()));
                    // 阅后即焚的文字处理
                    clickFireText(holder, message);
                } else if (holder.mHolderType == ChatHolderType.VIEW_TO_VIDEO) {// 阅后即焚的视频处理
                    // 通知chatactivity 等待播放完成后删除
                    EventBus.getDefault().post(new MessageEventClickFire("delay", message.getPacketId()));
                } else if (holder.mHolderType == ChatHolderType.VIEW_TO_IMAGE) {// 阅后即焚的图片处理
                    // 通知chatactivity 等待播放完成后删除
                    EventBus.getDefault().post(new MessageEventClickFire("delay", message.getPacketId()));
                } else if (holder.mHolderType == ChatHolderType.VIEW_TO_VOICE) {// 阅后即焚的声音处理
                    // 通知chatactivity 等待播放完成后删除
                    EventBus.getDefault().post(new MessageEventClickFire("delay", message.getPacketId()));
                }
            }

            if (holder.mHolderType == ChatHolderType.VIEW_FROM_MEDIA_CALL || holder.mHolderType == ChatHolderType.VIEW_TO_MEDIA_CALL) {
                if (mMessageEventListener != null) {
                    mMessageEventListener.onCallListener(message.getType());
                }
                return;
            }

            holder.sendReadMessage(message);
        }

        @Override
        public void onItemClick(View v, AChatHolderInterface holder, ChatMessage message) {
            Log.e("xuan", "onItemClick: " + holder.position);
            if (isShowMoreSelect) {
                if (message.getIsReadDel()) {
                    if (v.getId() == R.id.chat_msc) {
                        holder.setBoxSelect(false);
                    }
                    ToastUtil.showToast(mContext, mContext.getString(R.string.tip_cannot_multi_select_burn));
                    return;
                }
                message.isMoreSelected = !message.isMoreSelected;
                holder.setBoxSelect(message.isMoreSelected);
                return;
            }
            if ((message.getType() == XmppMessage.TYPE_TEXT || message.getType() == XmppMessage.TYPE_REPLAY) && !message.getIsReadDel()) {
                if (mCurChatType == ChatListType.COURSE) {
                    return;
                }
                if (clickHistoryMap.get(message.getPacketId()) != null) {
                    //noinspection ConstantConditions
                    if (System.currentTimeMillis() - clickHistoryMap.get(message.getPacketId()) <= 600) {// 文本消息两次点击间隔小于等于600ms
                        MessageRemindActivity.start(getContext(), message.toJsonString(), isGroupChat, mToUserId);
                        clickHistoryMap.clear();
                    } else {
                        clickHistoryMap.put(message.getPacketId(), System.currentTimeMillis());
                    }
                } else {
                    clickHistoryMap.put(message.getPacketId(), System.currentTimeMillis());
                }
            }

            if (!UiUtils.isNormalClick(v)) {
                // 防止过快点击多次触发事件
                return;
            }

            switch (v.getId()) {
                case R.id.tv_read:
                    // 点击了群已读人数
                    Intent intent = new Intent(mContext, RoomReadListActivity.class);
                    intent.putExtra("packetId", message.getPacketId());
                    intent.putExtra("roomJid", mToUserId);
                    intent.putExtra("roomId", mRoomId);
                    mContext.startActivity(intent);
                    break;
                case R.id.iv_failed:
                    // 点击了发送失败的消息的感叹号
                    holder.mIvFailed.setVisibility(GONE);
                    holder.mSendingBar.setVisibility(VISIBLE);
                    message.setMessageState(ChatMessageListener.MESSAGE_SEND_ING);
                    mMessageEventListener.onSendAgain(message);
                    break;
                case R.id.chat_head_iv:
                    // 点击了头像
                    if (message.isMySend()) {
                        mMessageEventListener.onFriendAvatarClick(mLoginUser.getUserId());
                    } else {
                        mMessageEventListener.onFriendAvatarClick(message.getFromUserId());
                    }
                    break;
                case R.id.chat_warp_view:
                    clickRootItem(holder, message);
                    break;
            }

            if (holder.mHolderType == ChatHolderType.VIEW_SYSTEM_TIP
                    || holder.mHolderType == ChatHolderType.VIEW_TO_GRADE) {
                if (mMessageEventListener != null) {
                    mMessageEventListener.onTipMessageClick(message);
                }
            }
        }

        @Override
        public void onItemLongClick(View v, AChatHolderInterface holder, ChatMessage message) {
            if (mCurChatType == ChatListType.LIVE) {
                return;
            }

            if (isShowMoreSelect) {
                return;
            }

            // 群组长按头像
            if (isGroupChat() && v.getId() == R.id.chat_head_iv) {
                mMessageEventListener.LongAvatarClick(message);
                return;
            }

            /**
             * 显示横向的window
             */
            if (mChatPpWindow != null && mChatPpWindow.isShowing()) {
                mChatPpWindow.dismiss();
            }
            if (mSelectableTextHelper != null) {
                mSelectableTextHelper.destroy();
            }
            mChatPpWindow = new ChatTextClickPpWindow(mContext, new ClickListener(message, holder.position),
                    message, mToUserId, mCurChatType == ChatListType.COURSE, isGroupChat(),
                    mCurChatType == ChatListType.DEVICE, mGroupLevel, memberMap.get(message.getFromUserId()));
            int offSetX = holder.mouseX - mChatPpWindow.getWidth() / 2;
            mChatPpWindow.setClickRowX(holder.mRootView.getLeft() + holder.mouseX);
            // mChatBottomView的高度是可变的，选择Edit来计算
            int offSetY;
            int clickY = 0 - (v.getHeight() - holder.mouseY);
            offSetY = clickY - mChatPpWindow.getHeight() - DisplayUtil.dip2px(mContext, 12);// 再向上偏移12dp
            int[] viewLocation = new int[2];
            int[] contentLocation = new int[2];
            v.getLocationOnScreen(viewLocation);
            ChatContentView.this.getLocationOnScreen(contentLocation);
            if (viewLocation[1] + v.getHeight() + offSetY < contentLocation[1]) {
                // 太高了，放下面，
                offSetY = clickY + DisplayUtil.dip2px(mContext, 12);
            }
            mChatPpWindow.showAsDropDown(v, offSetX, offSetY);
        }

        /**
         * 文本消息因过长时popupWindow弹出问题，先新增一个onItemLongClick重载方法，传入一个event，获取点击时的位置
         */
        @Override
        public void onItemLongClick(View v, Point eventPoint, AChatHolderInterface holder, ChatMessage message) {
            if (mCurChatType == ChatListType.LIVE) {
                return;
            }

            if (isShowMoreSelect) {
                return;
            }
            if (isGroupChat() && v.getId() == R.id.chat_head_iv) {
                mMessageEventListener.LongAvatarClick(message);
                return;
            }
            if (mChatPpWindow != null && mChatPpWindow.isShowing()) {
                mChatPpWindow.dismiss();
            }
            if (mSelectableTextHelper != null) {
                mSelectableTextHelper.destroy();
            }
            mChatPpWindow = new ChatTextClickPpWindow(mContext, new ClickListener(message, holder.position),
                    message, mToUserId, mCurChatType == ChatListType.COURSE, isGroupChat(),
                    mCurChatType == ChatListType.DEVICE, mGroupLevel, memberMap.get(message.getFromUserId()));
            int offSetX = holder.mouseX - mChatPpWindow.getWidth() / 2;
            mChatPpWindow.setClickRowX(holder.mRootView.getLeft() + holder.mouseX);
            if (v instanceof TextView) {
                mChatPpWindow.setFocusable(false);
                mSelectableTextHelper = new SelectableTextHelper.Builder((TextView) v)
                        .setPopupWindow(mChatPpWindow)
                        .setPosition(holder.position + getHeaderViewsCount())
                        .build();
                v.postDelayed(() -> {
                    mSelectableTextHelper.showSelectView((int) eventPoint.x, (int) eventPoint.y);
                }, 100);
            } else {
                mChatPpWindow.showAsDropDown(v, offSetX, (int) eventPoint.y);
            }
        }

        @Override
        public void onChangeInputText(String text) {
            if (mChatBottomView != null) {
                mChatBottomView.getmChatEdit().setText(text);
            }
        }

        @Override
        public void onCompDownVoice(ChatMessage message) {
            if (!isGroupChat() && message.getType() == XmppMessage.TYPE_VOICE && !message.isMySend()) {
                String filePath = UploadCacheUtils.get(mContext, message);
                if (message.getIsReadDel() && !TextUtils.isEmpty(filePath) && !mFireVoiceMaps.containsKey(filePath)) {
                    mFireVoiceMaps.put(filePath, message.getPacketId());
                }
            }
        }

        @Override
        public void onReplayClick(View v, AChatHolderInterface holder, ChatMessage message) {
            Log.e("xuan", "onReplayClick: " + holder.position);
            if (isShowMoreSelect) {
                message.isMoreSelected = !message.isMoreSelected;
                holder.setBoxSelect(message.isMoreSelected);
                return;
            }

            ChatMessage replayMessage = new ChatMessage(message.getObjectId());
            int index = -1;
            for (int i = 0; i < mChatMessages.size(); i++) {
                ChatMessage m = mChatMessages.get(i);
                if (TextUtils.equals(m.getPacketId(), replayMessage.getPacketId())) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                // 内存里有被回复消息的话直接处理，
                smoothScrollToPosition(index);
            } else {
                // 内存没有就回调出去查询数据库，
                if (mMessageEventListener != null) {
                    mMessageEventListener.onReplayClick(message);
                }
            }
        }

        @Override
        public void onQuestionClick(View v, AChatHolderInterface aChatHolderInterface, ChatMessage message, Question question) {
            if (mMessageEventListener != null) {
                mMessageEventListener.onMenuQuestionClick(message, question);
            }
        }
    }

    // 语音消息播放监听
    public class VoicePlayListener implements VoiceManager.VoicePlayListener {
        @Override
        public void onFinishPlay(String path) {
            VoiceViewHolder holder = aVoice.next(mCurClickPos, mChatMessages);
            if (holder != null) {
                mCurClickPos = holder.position;
                ChatMessage message = mChatMessages.get(mCurClickPos);
                holder.sendReadMessage(message);
                // todo 新老ui播放方法
                VoicePlayer.instance().playVoice(holder.voiceView);
                // VoicePlayer.instance().playVoice(holder.av_chat, message.getTimeLen(), holder.view_audio, false);
                if (message.getIsReadDel()) {
                    EventBus.getDefault().post(new MessageEventClickFire("delay", message.getPacketId()));
                }
            }

            if (mFireVoiceMaps.containsKey(path)) {
                EventBus.getDefault().post(new MessageEventClickFire("delete", mFireVoiceMaps.get(path)));
                mFireVoiceMaps.remove(path);
            }
        }

        @Override
        public void onStopPlay(String path) {
            aVoice.remove(mCurClickPos);
            if (mFireVoiceMaps.containsKey(path)) {
                fireVoiceStopMap.put(path, mFireVoiceMaps.get(path));
                EventBus.getDefault().post(new MessageEventClickFire("delete", mFireVoiceMaps.get(path)));
                mFireVoiceMaps.remove(path);
            }
        }

        @Override
        public void onErrorPlay() {
        }
    }

    // 自动播放语音消息 工具类
    public class AutoVoiceModule {
        HashMap<Integer, VoiceViewHolder> data = new HashMap<>();
        HashMap<VoiceViewHolder, Integer> last = new HashMap<>();

        // 放入消息
        public void put(VoiceViewHolder key) {
            if (last.containsKey(key)) {
                int index = last.get(key);
                data.remove(index);
                last.put(key, key.position);
                data.put(key.position, key);
            } else {
                last.put(key, key.position);
                data.put(key.position, key);
            }
        }

        // 取出消息
        public VoiceViewHolder next(int position, List<ChatMessage> list) {
            if (list == null || position + 1 >= list.size()) {
                return null;
            }

            for (int i = position + 1; i < list.size(); i++) {
                ChatMessage message = list.get(i);
                if (message.getType() == XmppMessage.TYPE_VOICE && !message.isMySend() && !message.isSendRead()) {
                    if (data.containsKey(i)) {
                        return data.get(i);
                    }
                }
            }
            return null;
        }

        // 删除消息
        public void remove(int position) {
            if (data.containsKey(position)) {
                last.remove(data.get(position));
                data.remove(position);
            }
        }
    }

    // 长按弹窗的点击事件监听
    public class ClickListener implements OnClickListener {
        private ChatMessage message;
        private int position;

        public ClickListener(ChatMessage message, int position) {
            this.message = message;
            this.position = position;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onClick(View v) {
            // mSelectableTextHelper.getSelectionContent要在window.dismiss前否则就重置了，
            String content;
            if (mSelectableTextHelper != null && mSelectableTextHelper.getSelectionContent() != null) {
                content = mSelectableTextHelper.getSelectionContent();
            } else {
                content = message.getContent();
            }
            mChatPpWindow.dismiss();
            switch (v.getId()) {
                case R.id.item_chat_translation_tv:
                    // 翻译，
                    if (message.getIsReadDel()
                            && !message.isSendRead()) {
                        Toast.makeText(mContext, R.string.tip_cannot_translated_burn, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    TranslateHelper.translate(ChatContentView.this, message);
                    break;
                case R.id.item_chat_recognizer_tv:
                    // 语音转文字，
                    if (message.getIsReadDel()
                            && !message.isSendRead()) {
                        Toast.makeText(mContext, R.string.tip_cannot_recognizer_burn, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    RecognizerHelper.recognizer(ChatContentView.this, message);
                    break;
                case R.id.item_chat_copy_tv:
                    // 复制
                    if (message.getIsReadDel()) {
                        Toast.makeText(mContext, R.string.tip_cannot_copy_burn, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String s = StringUtils.replaceSpecialChar(content);
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s, true);
                    // 获得剪切板管理者,复制文本内容
                    ClipboardManager cmb = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cmb.setText(charSequence);
                    AtCopyHelper.putAtUserList(mRoomId, mToUserId, message.getObjectId());
                    break;
                case R.id.item_chat_relay_tv:
                    // 转发消息
                    if (message.getIsReadDel()) {
                        // 为阅后即焚类型的消息，不可转发
                        Toast.makeText(mContext, mContext.getString(R.string.cannot_forwarded), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(mContext, InstantMessageActivity.class);
                    intent.putExtra("fromUserId", mToUserId);
                    intent.putExtra("messageId", message.getPacketId());
                    mContext.startActivity(intent);
                    break;
                case R.id.item_chat_collection_tv:
                    // 添加为表情
                    if (message.getIsReadDel()) {
                        Toast.makeText(mContext, R.string.tip_cannot_save_burn_image, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    collectionEmotion(message, false, isGroupChat, mToUserId);
                    break;
                case R.id.collection_other:
                    // 收藏
                    if (message.getIsReadDel()) {
                        Toast.makeText(mContext, R.string.tip_cannot_collect_burn, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!TextUtils.isEmpty(message.getSignature())) {
                        Toast.makeText(mContext, R.string.secure_msg_not_support_collection, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    collectionEmotion(message, true, isGroupChat, mToUserId);
                    break;
                case R.id.item_chat_back_tv:
                    // 撤回消息
                    mMessageEventListener.onMessageBack(message, position);
                    break;
                case R.id.item_chat_replay_tv:
                    // 回复消息
                    mMessageEventListener.onMessageReplay(message);
                    break;
                case R.id.item_chat_del_tv:
                    // 删除
                    if (mCurChatType == ChatListType.COURSE) {
                        if (mMessageEventListener != null) {
                            mMessageEventListener.onMessageClick(message);
                        }
                    } else {
                        // 发送广播去界面更新
                        Intent broadcast = new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
                        broadcast.putExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, position);
                        mContext.sendBroadcast(broadcast);
                    }
                    break;
                case R.id.item_chat_more_select:
                    // 多选
                    Intent showIntent = new Intent(Constants.SHOW_MORE_SELECT_MENU);
                    showIntent.putExtra(Constants.CHAT_SHOW_MESSAGE_POSITION, position);
                    mContext.sendBroadcast(showIntent);
                    break;
                default:
                    break;
            }
        }
    }
}
