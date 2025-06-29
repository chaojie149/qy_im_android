package com.tongxin.caihong.ui.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dou361.ijkplayer.bean.VideoijkBean;
import com.dou361.ijkplayer.widget.PlayStateParams;
import com.dou361.ijkplayer.widget.PlayerView;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.AddAttentionResult;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.bean.redpacket.Balance;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.db.InternationalizationHelper;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.live.adapter.MemberAdapter;
import com.tongxin.caihong.ui.live.bean.Detail;
import com.tongxin.caihong.ui.live.bean.Gift;
import com.tongxin.caihong.ui.live.bean.GiftItem;
import com.tongxin.caihong.ui.live.bean.GiftS;
import com.tongxin.caihong.ui.live.bean.Member;
import com.tongxin.caihong.ui.live.view.FragmentDialog;
import com.tongxin.caihong.ui.live.view.FragmentGiftDialog;
import com.tongxin.caihong.ui.live.view.FragmentPayDialog;
import com.tongxin.caihong.ui.live.view.GiftItemView;
import com.tongxin.caihong.ui.live.view.HorizontialListView;
import com.tongxin.caihong.ui.me.redpacket.WxPayBlance;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.FastBlurUtil;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.ChatContentView;
import com.tongxin.caihong.view.ManagerPopupWindow;
import com.tongxin.caihong.view.PullDownListView;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.ReceiptManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;
import okhttp3.Call;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tyrantgit.widget.HeartLayout;


/**
 * 观众activity
 * ijkplayer
 * playerView:基于ijkplayer项目进行的播放器界面UI封装
 * 是一个适用于Android的RTMP直播推流SDK，可高度定制化和二次开发。
 * 特色是同时支持 H.264 软编／硬编和 AAC 软编／硬编。主要是支持 RIMP、HLS、MP4、M4A 等视频格式的播放
 */

public class LivePlayingActivity extends BaseActivity implements View.OnClickListener,
        View.OnLayoutChangeListener, ChatMessageListener, ChatContentView.MessageEventListener {
    // 管理
    ManagerPopupWindow mManagerPopupWindow;
    boolean isCallbackOnInfoListener = false;// 是否回调了该监听
    Detail detail;
    private PlayerView player;
    private HorizontialListView listview;
    private MemberAdapter mAdapter;
    private ArrayList<Member> members;
    // 上
    private ImageView mAvatar;
    private TextView mNickName;
    private TextView mPeopleNum;
    private TextView mPayAttention;
    // 下
    private TextView mDanMuS;
    private EditText sendEditText;
    private TextView sendTextView;
    // 是否发送弹幕
    private boolean flag = false;
    // 烈焰弹幕
    private DanmakuView danmakuView;
    private DanmakuContext danmakuContext;
    // 礼物
    private GiftItemView giftView;
    // 专门用于显示自己赠送的礼物
    private GiftItemView giftView2;
    private ArrayList<Gift> gift;
    private ArrayList<String> gifts;
    private ArrayList<String> giftsForMe;
    // 爱心
    private HeartLayout heartLayout;
    // 随机生成rgb
    private Random mRandom;
    // Operation layout
    private View topView, bottomView, sendView;
    // 用来监听界面的变化，判断是否弹出软键盘
    private View rootView;
    // 软件盘弹起后所占高度阀值
    private int keyHeight = 0;
    /* chat room */
    private ChatContentView mChatContentView;
    // 存储聊天消息
    private List<ChatMessage> mChatMessages;
    private String mLoginUserId;
    private String mLoginNickName;
    private String mAccessToken;
    // 推流地址/直播间id/房间jid/房间名
    private String mRtmpURL;
    private String mRoomid;
    private String mRoomJid;
    private String mRoomName;
    private String mRoomUserId;
    // 是否是直播&群聊
    private boolean isLiveChat = true;
    private boolean isGroupChat = true;
    // 我
    private Member mMember;
    // 属于主播的Member对象
    private Member Anchor;
    // 禁言
    private int state;
    private InputMethodManager inputManager;
    private KeyBoardShowListener mKeyBoardShowListener;
    private int keyBoradCount = 0;
    private int mLiveRoomStatus;
    private Handler mHandler = new Handler();
    // 解析器,用来解析弹幕内容
    private BaseDanmakuParser parser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };
    private ImageView mOverBackgroundIv;
    private ImageView mOverAvatarIv;
    private TextView mOverNameTv;
    private Button mOverLeaveBtn;
    private int mMinId = 0;
    private int mPageSize = 20;
    private boolean mHasMoreData = true;
    /**
     * 用于通知更新已读状态的广播
     */
    private BroadcastReceiver changeSomeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LiveConstants.LIVE_MEMBER_ADD)) {
                // 成员加入
                Log.e("zq", "接收到成员加入广播");
                getLiveRoomMember();
            } else if (action.equals(LiveConstants.LIVE_MEMBER_DELETE)) {
                // 成员退出
                Log.e("zq", "接收到成员退出/踢人广播");
                Bundle bundle = intent.getExtras();
                String toUserId = bundle.getString("toUserId");
                if (toUserId.equals(mRoomUserId)) {
                    // 主播退出房间,所有人都需要退出直播间
                    exitRoom();
                    // Toast.makeText(LivePlayingActivity.this, "直播结束", Toast.LENGTH_SHORT).show();
                    Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("JXLiveVC_StopLive"), Toast.LENGTH_SHORT).show();
                } else if (toUserId.equals(mLoginUserId)) {
                    // 我被踢出房间,服务端已经做了删除操作
                    // exitRoom();
                    finish();
                    // Toast.makeText(LivePlayingActivity.this, "你已被踢出此直播间", Toast.LENGTH_SHORT).show();
                    Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("JXLiveVC_AlreadyKickOutRoom"), Toast.LENGTH_SHORT).show();
                } else {
                    // 其他人刷新列表
                    getLiveRoomMember();
                }
            } else if (action.equals(LiveConstants.LIVE_DANMU_DRAWABLE)) {
                // 绘制弹幕
                Log.e("zq", "接收绘制弹幕的广播");
                Bundle bundle = intent.getExtras();
                String content = bundle.getString("danmu");
                String fromUserId = bundle.getString("fromUserId");
                String fromUserName = bundle.getString("fromUserName");
                if (!TextUtils.isEmpty(fromUserId)
                        && !fromUserId.equals(mLoginUserId)) {
                    addDanmaku(content, false, false);
                }
            } else if (action.equals(LiveConstants.LIVE_SEND_GIFT)) {
                // 发送礼物
                Log.e("zq", "接收发送礼物的广播");
                Bundle bundle = intent.getExtras();
                String content = bundle.getString("gift");
                String fromUserId = bundle.getString("fromUserId");
                String fromUserName = bundle.getString("fromUserName");
                // 我是送礼物的人，在发送礼物成功的http回调中已经展示了礼物，在接收到XMPP消息的时候就不在展示了
                if (!fromUserId.equals(mLoginUserId)) {
                    GiftItem giftItem = new GiftItem();
                    giftItem.setGiftUi(fromUserId);
                    giftItem.setGiftUn(fromUserName);
                    // 得到礼物图片与礼物名称
                    String giftName = PreferenceUtils.getString(LivePlayingActivity.this, content);
                    String giftImage = PreferenceUtils.getString(LivePlayingActivity.this, giftName);
                    Log.e("zq", "礼物名称:" + giftName + "     礼物图片:" + giftImage);
                    giftItem.setName(giftName);
                    giftItem.setPhoto(giftImage);
                    if (!gifts.contains(giftName + fromUserId)) {
                        gifts.clear();
                        gifts.add(giftName + fromUserId);
                        giftView.setGift(giftItem);
                    }
                    giftView.addNum(1);
                }
            } else if (action.equals(LiveConstants.LIVE_SEND_LOVE_HEART)) {
                // 发送爱心
                Log.e("zq", "接收到发送爱心的广播");
                heartLayout.addHeart(randomColor());
            } else if (action.equals(LiveConstants.LIVE_SEND_MANAGER)) {
                // 被设置为管理员
                Log.e("zq", "接收到设置管理员的广播");
                Bundle bundle = intent.getExtras();
                String toUserId = bundle.getString("toUserId");
                if (toUserId.equals(mLoginUserId)) {
                    // 被设置管理员的人是我,更新自己的身份
                    getIdentity();
                    // Toast.makeText(LivePlayingActivity.this, "我被设置为管理员了", Toast.LENGTH_SHORT).show();
                    Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("AS_ADMIMISTRATO"), Toast.LENGTH_SHORT).show();
                }
                // 所有人刷新成员列表
                getLiveRoomMember();
            } else if (action.equals(LiveConstants.LIVE_SEND_SHUT_UP)) {
                // 被禁言/取消禁言
                Log.e("zq", "接收到禁言/取消禁言的广播");
                Bundle bundle = intent.getExtras();
                String toUserId = bundle.getString("toUserId");
                if (toUserId.equals(mLoginUserId)) {
                    // 被禁言/取消禁言的人是我,重新获取自己的身份(重置禁言状态)
                    getIdentity();
                }
                // 所有人刷新成员列表
                getLiveRoomMember();
            } else if (action.equals(LiveConstants.LIVE_SEND_LOCKED)) {
                finish();
            }
        }
    };

    private BroadcastReceiver mNetWorkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action) && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (player != null) {
                    player.pausePlay(); // 网络发生改变 先暂停播放
                    if (HttpUtil.isGprsOrWifiConnected(mContext)) {// 有网 恢复播放
                        player.startPlay();
                    } else {
                        DialogHelper.tip(mContext, getString(R.string.check_network));
                    }
                }
            }
        }
    };

    @Override
    public void onCoreReady() {
        super.onCoreReady();
        if (!TextUtils.isEmpty(mRoomJid)) {
            coreManager.joinMucChat(mRoomJid, 0);
            return;
        }
        // 在服务绑定成功后mRoomJid可能还未赋值，坐下兼容
        if (getIntent() != null) {
            mRoomJid = getIntent().getStringExtra(LiveConstants.LIVE_CHAT_ROOM_ID);
        }
        coreManager.joinMucChat(mRoomJid, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Vitamio.isInitialized(this);
        setContentView(R.layout.activity_live_play);
//        getSupportActionBar().hide();
        if (getIntent() != null) {
            mRtmpURL = getIntent().getStringExtra(LiveConstants.LIVE_GET_FLOW_URL);
            mRoomid = getIntent().getStringExtra(LiveConstants.LIVE_ROOM_ID);
            mRoomJid = getIntent().getStringExtra(LiveConstants.LIVE_CHAT_ROOM_ID);
            mRoomName = getIntent().getStringExtra(LiveConstants.LIVE_ROOM_NAME);
            mRoomUserId = getIntent().getStringExtra(LiveConstants.LIVE_ROOM_PERSON_ID);
            mLiveRoomStatus = getIntent().getIntExtra(LiveConstants.LIVE_STATUS, 0);
        }
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        mAccessToken = coreManager.getSelfStatus().accessToken;
        // 指定竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mRandom = new Random();
        // 添加消息监听
        ListenerManager.getInstance().addChatMessageListener(this);
        // 绑定服务
        // 初始化控件
        initView();
        // 初始化数据
        initData();
        // 初始化弹幕
        initDanmaku();
        // 注册成员列表/弹幕/送礼物广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(LiveConstants.LIVE_MEMBER_ADD);
        filter.addAction(LiveConstants.LIVE_MEMBER_DELETE);
        filter.addAction(LiveConstants.LIVE_DANMU_DRAWABLE);
        filter.addAction(LiveConstants.LIVE_SEND_GIFT);
        filter.addAction(LiveConstants.LIVE_SEND_LOVE_HEART);
        filter.addAction(LiveConstants.LIVE_SEND_MANAGER);
        filter.addAction(LiveConstants.LIVE_SEND_SHUT_UP);
        filter.addAction(LiveConstants.LIVE_SEND_LOCKED);
        registerReceiver(changeSomeBroadcastReceiver, filter);

        // 注册网络监听广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetWorkChangeReceiver, intentFilter);
    }

    private void initView() {
        initOverStatus();
        ijkPlayView();
        /* 成员 */
        listview = (HorizontialListView) findViewById(R.id.list);
        mAdapter = new MemberAdapter(this);
        listview.setAdapter(mAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showMember(mAdapter.datas.get(i));
            }
        });
        topView = findViewById(R.id.layout_top);
        bottomView = findViewById(R.id.layout_bottom_menu);
        /* 礼物/爱心 */
        giftView = (GiftItemView) findViewById(R.id.gift_item_first);
        giftView2 = (GiftItemView) findViewById(R.id.gift_item_second);
        heartLayout = (HeartLayout) findViewById(R.id.heart_layout);
        /* 上 */
        mAvatar = (ImageView) findViewById(R.id.avatar_iv);
        mNickName = (TextView) findViewById(R.id.nick_name);
        mPeopleNum = (TextView) findViewById(R.id.people_num);
        mPayAttention = (TextView) findViewById(R.id.attention_live);
        mPayAttention.setText(InternationalizationHelper.getString("JX_Attion"));
        // 不要关注功能，和好友系统有些冲突，
        mPayAttention.setVisibility(View.GONE);
        mAvatar.setOnClickListener(this);
        mPayAttention.setOnClickListener(this);
        /* 下 */
        sendView = findViewById(R.id.layout_send_message);
        mDanMuS = (TextView) findViewById(R.id.danmu);
        sendEditText = (EditText) findViewById(R.id.send_edit);
        sendTextView = (TextView) findViewById(R.id.send_tv);
        mDanMuS.setText(InternationalizationHelper.getString("JXLiveVC_Barrage"));
        sendEditText.setHint(InternationalizationHelper.getString("JXLiveVC_ChatPlaceHolder"));
        sendTextView.setText(InternationalizationHelper.getString("JX_Send"));
        sendTextView.setOnClickListener(this);
        mDanMuS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = !flag;
                if (flag) {
                    sendEditText.setHint(InternationalizationHelper.getString("JXLiveVC_BarragePlaceHolder"));
                    mDanMuS.setBackground(getResources().getDrawable(R.drawable.bg_danmu));
                } else {
                    sendEditText.setHint(InternationalizationHelper.getString("JXLiveVC_ChatPlaceHolder"));
                    mDanMuS.setBackground(null);
                }
            }
        });
        // Set  message
        mChatContentView = (ChatContentView) findViewById(R.id.chat_content_view);
        // 获取屏幕高度
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        // 阀值设置为屏幕高度的1/3
        keyHeight = screenHeight / 3;
        rootView = findViewById(R.id.activity_main);
        rootView.setOnClickListener(this);
        rootView.addOnLayoutChangeListener(this);
        // 发消息
        findViewById(R.id.send_message).setOnClickListener(this);
        // 送礼物
        findViewById(R.id.gift).setOnClickListener(this);
        // 退出直播间
        findViewById(R.id.close).setOnClickListener(this);
        // 更新余额
        updateMoney();
        // 获取自己的身份信息
        getIdentity();
        /**
         * 因为LivePlaying的windowSoftInputMode并没有设置adjustResize属性，如果设置adjustResize软键盘弹起会挤压Activity,
         * 这样rootView的onLayoutChange才监测的到屏幕宽/高变化，如果设置其他属性根本监测不到
         * so,换一种方式监测，这种方式又有一个新的bug产生，那就是输入框会被软键盘遮挡住一半，这时将输入框传入KeyBoardShowListener，到里面做处理吧
         */
        mKeyBoardShowListener = new KeyBoardShowListener(this, sendView);
        mKeyBoardShowListener.setKeyboardListener(new KeyBoardShowListener.OnKeyboardVisibilityListener() {
            @Override
            public void onVisibilityChanged(boolean visible) {
                if (visible) {
                    sendView.setVisibility(View.VISIBLE);
                    topView.setVisibility(View.GONE);
                    bottomView.setVisibility(View.GONE);
                } else {
                    sendView.setVisibility(View.GONE);
                    topView.setVisibility(View.VISIBLE);
                    bottomView.setVisibility(View.VISIBLE);
                }
            }
        }, this);
    }

    public void initOverStatus() {
        mOverBackgroundIv = findViewById(R.id.off_bg_iv);
        mOverAvatarIv = findViewById(R.id.off_avatar_iv);
        mOverNameTv = findViewById(R.id.off_name_tv);
        mOverLeaveBtn = findViewById(R.id.off_leave_btn);

        // 为背景添加虚化效果
        int scaleRatio = 20;// blurry multiple
        Bitmap scaledBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo_bg);
        Bitmap blurBitmap = FastBlurUtil.toBlur(scaledBitmap, scaleRatio);
        mOverBackgroundIv.setImageBitmap(blurBitmap);
        mOverLeaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coreManager.exitMucChat(mRoomJid);
                finish();
            }
        });
    }

    private void ijkPlayView() {
        /**
         1. PlayStateParams.fitParent:  可能会剪裁,保持原视频的大小，显示在中心,当原视频的大小超过View的大小超过部分裁剪处理
         2. PlayStateParams.fillParent: 可能会剪裁,等比例放大视频，直到填满View为止,超过View的部分作裁剪处理
         3. PlayStateParams.wrapcontent:将视频的内容完整居中显示，如果视频大于View,则按比例缩视频直到完全显示在View中
         4. PlayStateParams.fitXY:不剪裁,非等比例拉伸画面填满整个View
         5. PlayStateParams.f16_9:不剪裁,非等比例拉伸画面到 16:9,并完全显示在View中
         6. PlayStateParams.f4_3: 不剪裁,非等比例拉伸画面到 4:3,并完全显示在View中
         */
        VideoijkBean videoijkBean = new VideoijkBean();
        videoijkBean.setStream("高清");
        videoijkBean.setUrl(mRtmpURL);
        player = new PlayerView(this)
                // 全屏播放
                .setScaleType(PlayStateParams.fitparent)
                // 默认标清
                // .setPlaySource(mRtmpURL)
                // .setPlaySource("高清", mRtmpURL)
                .setPlaySource(videoijkBean)
                // 加载时显示网速
                .setShowSpeed(false)
                // 设置2/3/4/5G进行网络类型提示
                .setNetWorkTypeTie(false)
                // 设置自动重连的模式与重连时间
                .setAutoReConnect(true, 5000)
                .startPlay();


        // playerView播放监听
        final long beginTime = TimeUtils.sk_time_current_time();
        player.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
                isCallbackOnInfoListener = true;
                Log.e("zq", TimeUtils.sk_time_current_time() - beginTime + "，" + what);

                findViewById(R.id.fl).setVisibility(View.VISIBLE);
                findViewById(R.id.rl).setVisibility(View.GONE);

/*
                if (what == LiveConstants.MEDIA_INFO_VIDEO_INTERRUPT) {
                    findViewById(R.id.fl).setVisibility(View.GONE);
                    findViewById(R.id.rl).setVisibility(View.VISIBLE);
                }
*/
                return false;
            }
        });

        // 当Rtmp流无效时，上面的OnInfoListener之前可以正常回调到 MEDIA_INFO_VIDEO_INTERRUPT Status，
        // 现在已经不回调了（原因未知）只能用mLiveStatus来判断了
        if (mLiveRoomStatus == 0) {
            findViewById(R.id.fl).setVisibility(View.GONE);
            findViewById(R.id.rl).setVisibility(View.VISIBLE);
        }

        // mLiveRoomStatus有时可能不准，兼容
        mOverBackgroundIv.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isCallbackOnInfoListener) {// 7S后还未回调到OnInfoListener，判断Rtmp流无效
                    findViewById(R.id.fl).setVisibility(View.GONE);
                    findViewById(R.id.rl).setVisibility(View.VISIBLE);
                }
            }
        }, 7000);

    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 关注 visible or gone
        List<Friend> allFriends = FriendDao.getInstance().getAllFriends(mLoginUserId);
        for (Friend friend : allFriends) {
            if (friend.getUserId().equals(mRoomUserId)) {
                mPayAttention.setVisibility(View.GONE);
            }
        }
        // 获取直播间详情
        getLiveRoomDetail();
        // 获取直播间成员列表
        members = new ArrayList<>();
        getLiveRoomMember();
        // 消息数据
        mChatMessages = new ArrayList<>();
//        mChatContentView.setIsLiveChat(isLiveChat);
//        mChatContentView.set_is_group(isGroupChat);
//        mChatContentView.setRoomNickName(mLoginNickName);
        mChatContentView.setToUserId(mRoomJid);
        mChatContentView.setData(mChatMessages);
        mChatContentView.setMessageEventListener(this);
        // mChatContentView.setRoomNickName(mFriend.getRoomMyNickName());
        mChatContentView.setRefreshListener(new PullDownListView.RefreshingListener() {
            @Override
            public void onHeaderRefreshing() {
                loadDatas(false);
            }
        });

        gift = new ArrayList<>();
        gifts = new ArrayList<>();
        giftsForMe = new ArrayList<>();
        // 初始化时就获取礼物列表
        GetGiftS();
    }

    private void initDanmaku() {
        danmakuView = (DanmakuView) findViewById(R.id.danmaku_view);
        danmakuContext = DanmakuContext.create();
        // 设置弹幕的最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        // 滚动弹幕最大显示3行
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 3);
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_LR, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_BOTTOM, true);

        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3) //设置描边样式
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(1.2f) //是否启用合并重复弹幕
                .setScaleTextSize(1.2f) //设置弹幕滚动速度系数,只对滚动弹幕有效
                // .setCacheStuffer(new SpannedCacheStuffer(), mCacheStufferAdapter) // 图文混排使用SpannedCacheStuffer  设置缓存绘制填充器，默认使用{@link SimpleTextCacheStuffer}只支持纯文字显示, 如果需要图文混排请设置{@link SpannedCacheStuffer}如果需要定制其他样式请扩展{@link SimpleTextCacheStuffer}|{@link SpannedCacheStuffer}
                .setMaximumLines(maxLinesPair) //设置最大显示行数
                .preventOverlapping(overlappingEnablePair); //设置防弹幕重叠，null为允许重叠

        danmakuView.setCallback(new DrawHandler.Callback() {

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }

            @Override
            public void prepared() {
                // 是否显示FPS
                danmakuView.showFPS(false);
                // 节省绘制时间,在模拟器上可能会报错
                danmakuView.enableDanmakuDrawingCache(true);
                danmakuView.start();
            }
        });
        danmakuView.prepare(parser, danmakuContext);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.attention_live) {
            doAddAttention();
        }
        if (id == R.id.avatar_iv) {
            if (Anchor != null) {
                showMember(Anchor);
            }
        }
        if (id == R.id.send_message) {
            keyBoradCount++;
            Log.e("zq", String.valueOf(keyBoradCount));
            mKeyBoardShowListener.setKeyBoardCount(keyBoradCount);
             /*
             发消息/弹幕
             */
            sendEditText.requestFocus();
            // 软键盘弹起
            inputManager.showSoftInput(sendEditText, 0);
        }
        if (id == R.id.send_tv) {
            String content = sendEditText.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                return;
            }
            if (isAuthenticated()) {
                return;
            }
            if (flag) {
                // 在查余额之前需要先更新余额
                int balance = (int) coreManager.getSelf().getBalance();
                Log.e("zq", "当前余额:" + String.valueOf(balance));
                if (balance >= 1) {
                    // 发送弹幕
                    SendDanmu(content);
                } else {
                    // 余额不足，去充值
                    // 充值
                    FragmentPayDialog.newInstance(new FragmentPayDialog.OnClickBottomListener() {
                        @Override
                        public void onPositiveClick() {
                            // 充值
                            startActivity(new Intent(LivePlayingActivity.this, WxPayBlance.class));
                        }

                        @Override
                        public void onNegativeClick() {
                            // 取消
                        }
                    }).show(getSupportFragmentManager(), "dialog");
                }
            } else {
                // 聊天
                if (state == 0) {
                    sendOnlyWord(content);
                } else {
                    // Toast.makeText(LivePlayingActivity.this, "你已经被禁言了，要就发弹幕吧", Toast.LENGTH_SHORT).show();
                    Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("JXLiveVC_GagToBarrage"), Toast.LENGTH_SHORT).show();
                }
            }
            // 清空EditText
            sendEditText.getText().clear();
            // 软键盘消失
            inputManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
        if (id == R.id.gift) {
            /*
            送礼物
             */
            FragmentGiftDialog.newInstance().setOnGridViewClickListener(gift, new FragmentGiftDialog.OnGridViewClickListener() {
                @Override
                public void click(Gift gift) {
                    if (isAuthenticated()) {
                        return;
                    }
                    // TODO Auto-generated method stub
                    int balance = (int) coreManager.getSelf().getBalance();
                    if (balance >= gift.getPrice()) {
                        // 发礼物
                        SendGift(gift);
                    } else {
                        // 余额不足，请充值
                        // 充值
                        FragmentPayDialog.newInstance(new FragmentPayDialog.OnClickBottomListener() {
                            @Override
                            public void onPositiveClick() {
                                // 充值
                                startActivity(new Intent(LivePlayingActivity.this, WxPayBlance.class));
                            }

                            @Override
                            public void onNegativeClick() {
                                // 取消
                            }
                        }).show(getSupportFragmentManager(), "dialog");
                    }
                }

                @Override
                public void dismiss() {
                }
            }).show(getSupportFragmentManager(), "dialog");
        }
        if (id == R.id.activity_main) {
            if (isAuthenticated()) {
                return;
            }
            SendHeart();
        }
        if (id == R.id.close) {
            exitRoom();
        }
        // other user interface change
    }

    // 展示成员
    private void showMember(final Member member) {
        // mines type mines type member state
        // manager isShow?
        // modify and setManager isShow?
        // shutUp or cancelShutUp
        if (mMember == null) {
            return;
        }
        // 得到自己的类型
        int type = mMember.getType();
        // 如果自己是管理员并且查看的信息是自己的，让“管理”消失
        if (String.valueOf(member.getUserId()).equals(mLoginUserId)) {
            type = 3;
        }
        // 如果点击的是主播的昵称，让“管理”消失
        if (String.valueOf(member.getUserId()).equals(mRoomUserId)) {
            type = 3;
        }
        FragmentDialog.newInstance(type, member.getUserId(), member.getNickName(),
                InternationalizationHelper.getString("FORGOTTEN_SINGNATURE"), true,
                InternationalizationHelper.getString("HOMEPAGE"), InternationalizationHelper.getString("JXUserInfoVC_Space"), new FragmentDialog.OnClickBottomListener() {
                    @Override
                    public void onManagerClick() {
                        if (isAuthenticated()) {
                            return;
                        }
                        if (member.getType() == 2) {
                            // 此人同样也是管理员
                            Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("ALSO_ADMINNISTOR"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        bottomView.setVisibility(View.GONE);
                        mManagerPopupWindow = new ManagerPopupWindow(LivePlayingActivity.this, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mManagerPopupWindow.dismiss();
                                bottomView.setVisibility(View.VISIBLE);
                                switch (view.getId()) {
                                    case R.id.shut_up:
                                        if (member.getState() == 0) {
                                            // 禁言
                                            shutUpMember(member);
                                        } else {
                                            // 取消禁言
                                            cancelShutUpMember(member);
                                        }
                                        break;
                                    case R.id.kick_room:
                                        kickMember(member);
                                        break;
                                    case R.id.cancel:
                                        break;
                                }
                            }
                        }, mMember.getType(), member.getState());
                        mManagerPopupWindow.showAtLocation(rootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                    }

                    @Override
                    public void onPositiveClick() {
                        // 查看详情
                        Intent intent = new Intent(LivePlayingActivity.this, BasicInfoActivity.class);
                        intent.putExtra(AppConstant.EXTRA_USER_ID, String.valueOf(member.getUserId()));
                        startActivity(intent);
                    }

                    @Override
                    public void onNegativeClick() {

                    }
                }).show(getSupportFragmentManager(), "dialog");
    }

    private void loadDatas(final boolean scrollToBottom) {
        if (mChatMessages.size() > 0) {
            mMinId = mChatMessages.get(0).get_id();
        } else {
            mMinId = 0;
        }
        List<ChatMessage> chatLists = ChatMessageDao.getInstance().getSingleChatMessages(mLoginUserId, mRoomJid, mMinId, mPageSize);
        if (chatLists == null || chatLists.size() <= 0) {
            mHasMoreData = false;
        } else {
            long currentTime = System.currentTimeMillis() / 1000;

            for (int i = 0; i < chatLists.size(); i++) {
                ChatMessage message = chatLists.get(i);
                if (message.isMySend() && message.getMessageState() == ChatMessageListener.MESSAGE_SEND_ING) {
                    // 如果是我发的消息，有时候在消息发送中，直接退出了程序，此时消息发送状态可能使用是发送中，
                    if (currentTime - message.getTimeSend() > ReceiptManager.MESSAGE_DELAY / 1000) {
                        ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mRoomJid, message.get_id(),
                                ChatMessageListener.MESSAGE_SEND_FAILED);
                        message.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    }
                }
                mChatMessages.add(0, message);
            }
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mChatContentView.notifyDataSetInvalidated(scrollToBottom);
                mChatContentView.headerRefreshingCompleted();
                if (!mHasMoreData) {
                    mChatContentView.setNeedRefresh(false);
                }
            }
        }, 0);
    }

    /***********************
     * 直播聊天，只发送文字
     ***********************/
    public void sendOnlyWord(String content) {
        if (isAuthenticated()) {
            return;
        }

        if (TextUtils.isEmpty(content)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(content);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    private void sendMessage(ChatMessage message) {
        message.setIsEncrypt(0);// 直播不加密
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));

        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mRoomJid, message);
        coreManager.sendMucChatMessage(mRoomJid, message);
    }

    /**
     * 添加文本弹幕
     *
     * @param content    弹幕的具体内容
     * @param withBorder 弹幕是否有边框
     */
    private void addDanmaku(String content, boolean withBorder, boolean whoSend) {
        BaseDanmaku danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        danmaku.text = content;
        danmaku.padding = 5;
        // 0 表示可能会被各种过滤器过滤并隐藏显示,1表示一定会显示,一般用于本机发送的弹幕
        danmaku.priority = 1;
        // 直播弹幕
        danmaku.isLive = true;
        danmaku.textSize = sp2px(14);
        if (whoSend) {
            // is my send,change text color
            danmaku.textColor = getResources().getColor(R.color.chat_background);
        } else {
            danmaku.textColor = Color.WHITE;
        }
        // 阴影/描边颜色
        danmaku.textShadowColor = Color.WHITE;
        danmaku.setTime(danmakuView.getCurrentTime());
        if (withBorder) {
            // 边框颜色，0表示无边框
            danmaku.borderColor = Color.GREEN;
        }
        danmakuView.addDanmaku(danmaku);
    }

    /********************
     * ToDo 直播间接口操作
     ********************/
    // 更新余额
    public void updateMoney() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);

        HttpUtils.get().url(coreManager.getConfig().RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {
                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        DecimalFormat df = new DecimalFormat("######0.00");
                        Balance b = result.getData();
                        coreManager.getSelf().setBalance(Double.parseDouble(df.format(b.getBalance())));
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 获取自己的身份信息
    public void getIdentity() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", mLoginUserId);
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_GET_IDENTITY)
                .params(params)
                .build()
                .execute(new BaseCallback<Member>(Member.class) {
                    @Override
                    public void onResponse(ObjectResult<Member> result) {
                        // 重置mMember
                        mMember = result.getData();
                        if (mMember == null) {
                            return;
                        }
                        // 获得自己的禁言状态
                        state = mMember.getState();
                        Log.e("zq", "Online:" + mMember.getOnline() + "      state" + mMember.getState() + "      type" + mMember.getType());
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 获取他人的身份信息,用于直播昵称点击
    public void getOtherIdentity(String userId) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", userId);
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_GET_IDENTITY)
                .params(params)
                .build()
                .execute(new BaseCallback<Member>(Member.class) {
                    @Override
                    public void onResponse(ObjectResult<Member> result) {
                        if (result.getResultCode() == 1) {
                            Member member = result.getData();
                            showMember(member);
                        } else {
                            Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("FAILED_IDENTITY"), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 获取直播间详情
    public void getLiveRoomDetail() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_DETAIL)
                .params(params)
                .build()
                .execute(new BaseCallback<Detail>(Detail.class) {
                    @Override
                    public void onResponse(ObjectResult<Detail> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            detail = result.getData();
                            AvatarHelper.getInstance().displayAvatar(String.valueOf(detail.getUserId()), mAvatar, false);
                            mNickName.setText(detail.getNickName());
                            // mPeopleNum.setText(String.valueOf(detail.getNumbers()));

                            AvatarHelper.getInstance().displayAvatar(String.valueOf(detail.getUserId()), mOverAvatarIv, false);
                            mOverNameTv.setText(detail.getNickName());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 获取直播间成员列表
    public void getLiveRoomMember() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_MEMBER_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Member>(Member.class) {

                    @Override
                    public void onResponse(ArrayResult<Member> result) {
                        members.clear();
                        for (int i = 0; i < result.getData().size(); i++) {
                            // 主播不显示
                            if (String.valueOf(result.getData().get(i).getUserId()).equals(mRoomUserId)) {
                                // 主播，不显示在成员列表中
                                Anchor = result.getData().get(i);
                            } else {
                                members.add(result.getData().get(i));
                            }
                            Log.e("zq", "MemberSType:" + result.getData().get(i).getType());
                        }
                        mPeopleNum.setText(String.valueOf(members.size()));
                        mAdapter.setDatas(members);
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 获取礼物列表
    public void GetGiftS() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("pageIndex", String.valueOf(0));
        params.put("pageSize", String.valueOf(50));

        HttpUtils.get().url(coreManager.getConfig().GET_LIVE_GIFT_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Gift>(Gift.class) {

                    @Override
                    public void onResponse(ArrayResult<Gift> result) {
                        if (result.getResultCode() == 1) {
                            gift = (ArrayList<Gift>) result.getData();
                            // 将数据保存至共享参数内
                            for (int i = 0; i < gift.size(); i++) {
                                PreferenceUtils.putString(LivePlayingActivity.this, gift.get(i).getGiftId(), gift.get(i).getName());
                            }
                            for (int i = 0; i < gift.size(); i++) {
                                PreferenceUtils.putString(LivePlayingActivity.this, gift.get(i).getName(), gift.get(i).getPhoto());
                            }
                        } else {
                            Toast.makeText(LivePlayingActivity.this, "获取礼物列表失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorData(LivePlayingActivity.this);
                    }
                });
    }

    // 发送弹幕
    public void SendDanmu(final String context) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", mLoginUserId);
        params.put("text", context);
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_DANMU)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            // 更新余额
                            updateMoney();
                            addDanmaku(context, false, true);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 发送礼物
    public void SendGift(final Gift gift) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", mLoginUserId);
        params.put("toUserId", mRoomUserId);
        // 礼物名字
        params.put("giftId", gift.getGiftId());
        params.put("count", String.valueOf(1));
        params.put("price", String.valueOf(gift.getPrice()));
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_GIFT)
                .params(params)
                .build()
                .execute(new BaseCallback<GiftS>(GiftS.class) {
                    @Override
                    public void onResponse(ObjectResult<GiftS> result) {
                        if (result.getResultCode() == 1) {
                            // 更新余额
                            updateMoney();

                            GiftS giftS = result.getData();
                            String giftId = giftS.getGiftId();
                            GiftItem giftItem = new GiftItem();
                            giftItem.setGiftUi(mLoginUserId);
                            giftItem.setGiftUn(mLoginNickName);
                            // 得到礼物图片与礼物名称
                            String giftName = PreferenceUtils.getString(LivePlayingActivity.this, giftId);
                            String giftImage = PreferenceUtils.getString(LivePlayingActivity.this, giftName);
                            Log.e("zq", "礼物名称:" + giftName + "     礼物图片:" + giftImage);
                            giftItem.setName(giftName);
                            giftItem.setPhoto(giftImage);
                            if (!giftsForMe.contains(giftName)) {
                                // 不属于同一种礼物
                                giftsForMe.clear();
                                giftsForMe.add(giftName);
                                giftView2.setGift(giftItem);
                            }
                            giftView2.addNum(1);
                        } else {
                            // Toast.makeText(LivePlayingActivity.this, "余额不足", Toast.LENGTH_SHORT).show();
                            Toast.makeText(LivePlayingActivity.this, InternationalizationHelper.getString("CREDIT_LOW"), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 发送爱心
    public void SendHeart() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_PRAISE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        // 服务器进行XMPP推送
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 禁言
    public void shutUpMember(final Member member) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", String.valueOf(member.getUserId()));
        params.put("state", String.valueOf(1));
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_SHUT_UP)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        Toast.makeText(LivePlayingActivity.this,
                                InternationalizationHelper.getString("JXLiveVC_SetGag") + " " + InternationalizationHelper.getString("JX_Success"),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 取消禁言
    public void cancelShutUpMember(final Member member) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", String.valueOf(member.getUserId()));
        params.put("state", String.valueOf(0));
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_SHUT_UP)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        Toast.makeText(LivePlayingActivity.this,
                                InternationalizationHelper.getString("JXLiveVC_GagCancel") + " " + InternationalizationHelper.getString("JX_Success"),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 踢人
    public void kickMember(final Member member) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", String.valueOf(member.getUserId()));
        HttpUtils.get().url(coreManager.getConfig().LIVE_ROOM_KICK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            // Toast.makeText(LivePlayingActivity.this, "踢出成功", Toast.LENGTH_SHORT).show();
                            Toast.makeText(LivePlayingActivity.this,
                                    InternationalizationHelper.getString("JXLiveVC_KickSuccess"), Toast.LENGTH_SHORT).show();
                        } else {
                            // Toast.makeText(LivePlayingActivity.this, "踢人失败", Toast.LENGTH_SHORT).show();
                            Toast.makeText(LivePlayingActivity.this,
                                    InternationalizationHelper.getString("JXLiveVC_Kick") + " " + InternationalizationHelper.getString("JX_Failed"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /**
     * 加关注
     */
    private void doAddAttention() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mRoomUserId);
        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {
                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getData().getType() == 1 || result.getData().getType() == 3) {
                            mPayAttention.setVisibility(View.GONE);
                        } else if (result.getData().getType() == 2 || result.getData().getType() == 4) {
                            CardcastUiUpdateUtil.broadcastUpdateUi(LivePlayingActivity.this);
                            invalidateOptionsMenu();
                            mPayAttention.setVisibility(View.GONE);
                        } else if (result.getData().getType() == 5) {
                            ToastUtil.showToast(mContext, R.string.add_attention_failed);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    // 退出直播间
    public void exitRoom() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", mAccessToken);
        params.put("roomId", mRoomid);
        params.put("userId", mLoginUserId);
        HttpUtils.get().url(coreManager.getConfig().EXIT_LIVE_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        coreManager.exitMucChat(mRoomJid);
                        finish();
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // 现在认为只要控件将Activity向上推的高度超过了1/3屏幕高，就认为软键盘弹起
        if (oldBottom != 0 && bottom != 0 && (oldBottom - bottom > keyHeight)) {
            sendView.setVisibility(View.VISIBLE);
            topView.setVisibility(View.GONE);
            bottomView.setVisibility(View.GONE);
        } else if (oldBottom != 0 && bottom != 0 && (bottom - oldBottom > keyHeight)) {
            sendView.setVisibility(View.GONE);
            topView.setVisibility(View.VISIBLE);
            bottomView.setVisibility(View.VISIBLE);
        }
    }


    /**********************
     * MUC Message Listener
     ********************/
//    @Override
//    public void onMessageSendStateChange(int messageState, int msg_id) {
//
//    }

    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {

    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        if (isGroupMsg != isGroupChat) {
            return false;
        }
        if (mRoomJid.compareToIgnoreCase(fromUserId) == 0) {
            mChatMessages.add(message);
            mChatContentView.notifyDataSetInvalidated(true);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
/*
        if (player != null) {
            player.onResume();
        }
*/
    }

    @Override
    public void onPause() {
        super.onPause();
/*
        if (player != null) {
            player.onPause();
        }
*/
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        exitRoom();
        super.onDestroy();
        if (player != null) {
            player.onDestroy();
        }
        unregisterReceiver(changeSomeBroadcastReceiver);
        unregisterReceiver(mNetWorkChangeReceiver);
    }

    @Override
    public void onNickNameClick(String friendUserId) {
        getOtherIdentity(friendUserId);
    }

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
    public void onMessageClick(ChatMessage chatMessage) {
    }

    @Override
    public void onMessageLongClick(ChatMessage chatMessage) {
    }

    @Override
    public void onMessageBack(ChatMessage chatMessage, int position) {
    }

    @Override
    public void onSendAgain(ChatMessage chatMessage) {
    }

    @Override
    public void onCallListener(int type) {
    }

    /**
     * sp转px的
     */
    public int sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 随机生成rgb
     */
    private int randomColor() {
        return Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255));
    }

    /*******************************************
     * 是否离线&&重连
     ******************************************/
    public boolean isAuthenticated() {
        boolean isLogin = coreManager.isLogin();
        if (!isLogin) {
            coreManager.autoReconnect(this);
        }
        return !isLogin;
    }
}

