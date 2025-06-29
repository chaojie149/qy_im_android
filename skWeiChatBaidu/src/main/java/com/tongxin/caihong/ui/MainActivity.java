package com.tongxin.caihong.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alibaba.fastjson.JSON;
import com.baidu.mapapi.SDKInitializer;
import com.bumptech.glide.Glide;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.ConfigBean;
import com.tongxin.caihong.bean.Contact;
import com.tongxin.caihong.bean.Contacts;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.UploadingFile;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.collection.Collectiion;
import com.tongxin.caihong.bean.event.EventCreateGroupFriend;
import com.tongxin.caihong.bean.event.EventQRCodeReady;
import com.tongxin.caihong.bean.event.EventSelfRemoved;
import com.tongxin.caihong.bean.event.EventSendVerifyMsg;
import com.tongxin.caihong.bean.event.MessageContactEvent;
import com.tongxin.caihong.bean.event.MessageEventHongdian;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.bean.event.MessageSendChat;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.broadcast.TimeChangeReceiver;
import com.tongxin.caihong.broadcast.UpdateUnReadReceiver;
import com.tongxin.caihong.broadcast.UserLogInOutReceiver;
import com.tongxin.caihong.call.AudioOrVideoController;
import com.tongxin.caihong.call.CallConstants;
import com.tongxin.caihong.call.Jitsi_connecting_second;
import com.tongxin.caihong.call.MessageEventCancelOrHangUp;
import com.tongxin.caihong.call.MessageEventInitiateMeeting;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.ContactDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.db.dao.MyZanDao;
import com.tongxin.caihong.db.dao.NewFriendDao;
import com.tongxin.caihong.db.dao.OnCompleteListener2;
import com.tongxin.caihong.db.dao.UploadingFileDao;
import com.tongxin.caihong.db.dao.UserDao;
import com.tongxin.caihong.downloader.UpdateManger;
import com.tongxin.caihong.fragment.ContactPagerFragment;
import com.tongxin.caihong.fragment.KxMeFragment;
import com.tongxin.caihong.fragment.MeFragment;
import com.tongxin.caihong.fragment.MessageFragment;
import com.tongxin.caihong.fragment.PublicSquareFragment;
import com.tongxin.caihong.fragment.SquareFragment;
import com.tongxin.caihong.fragment.TrillFragment;
import com.tongxin.caihong.fragment.WebViewFragment;
import com.tongxin.caihong.helper.AdHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.LoginSecureHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.pay.PaymentReceiptMoneyActivity;
import com.tongxin.caihong.pay.ReceiptPayMoneyActivity;
import com.tongxin.caihong.socket.SocketException;
import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.account.LoginActivity;
import com.tongxin.caihong.ui.backup.ReceiveChatHistoryActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.company.SearchCompanyActivity;
import com.tongxin.caihong.ui.lock.DeviceLockActivity;
import com.tongxin.caihong.ui.lock.DeviceLockHelper;
import com.tongxin.caihong.ui.login.WebLoginActivity;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.ui.other.QRcodeActivity;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ContactsUtil;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.JsonUtils;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.ViewPiexlUtil;
import com.tongxin.caihong.util.log.LogUtils;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.window.WindowShowService;
import com.tongxin.caihong.xmpp.CoreService;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.helloDemon.FirebaseMessageService;
import com.tongxin.caihong.xmpp.helloDemon.HuaweiPushRevicer;
import com.tongxin.caihong.xmpp.helloDemon.MeizuPushMsgReceiver;
import com.tongxin.caihong.xmpp.helloDemon.OppoPushMessageService;
import com.tongxin.caihong.xmpp.helloDemon.OppoPushServiceAndroidQ;
import com.tongxin.caihong.xmpp.helloDemon.VivoPushMessageReceiver;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.example.qrcode.Constant;
import com.example.qrcode.ScannerActivity;
import com.fanjun.keeplive.KeepLive;
import com.fanjun.keeplive.config.KeepLiveService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.heytap.msp.push.HeytapPushManager;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.Call;

/**
 * 主界面
 */
public class MainActivity extends BaseActivity  {
    public static final int REQUEST_SCANNER = 888;
    // 小米推送
    public static final String APP_ID = BuildConfig.XIAOMI_APP_ID;
    public static final String APP_KEY = BuildConfig.XIAOMI_APP_KEY;
    // 是否重新走initView方法
    // 当切换语言、修改皮肤之后，将该状态置为true
    public static boolean isInitView = false;
    public static boolean isSquareFragmentCreatedRefresh;
    public static boolean isAutoLogin;

    /**
     * 更新我的群组
     */
    Handler mHandler = new Handler();
    private UpdateUnReadReceiver mUpdateUnReadReceiver = null;
    private UserLogInOutReceiver mUserLogInOutReceiver = null;
    private TimeChangeReceiver timeChangeReceiver = null;
    private ActivityManager mActivityManager;
    // ╔═══════════════════════════════界面组件══════════════════════════════╗
    // ╚═══════════════════════════════界面组件══════════════════════════════╝
    private int mLastFragmentId;// 当前界面
    private RadioGroup mRadioGroup;
    private RadioButton mRbTab1, mRbTab2, mRbTab3, mRbTab4, mRbTab6, mRbTab5;
    private TextView mTvMessageNum;// 显示消息界面未读数量
    private TextView mTvNewFriendNum;// 显示通讯录消息未读数量
    private TextView mTvCircleNum;// 显示朋友圈未读数量
    private int numMessage = 0;// 当前未读消息数量
    private int numCircle = 0; // 当前朋友圈未读数量
    private String mUserId;// 当前登陆的 UserID
    private My_BroadcastReceiver my_broadcastReceiver;
    private int mCurrtTabId;
    private boolean isCreate;
    /**
     * 在其他设备登录了，挤下线
     */
    private boolean isConflict;
    // 用来杜绝多次打开扫码页面，
    private boolean scannerRequesting;

    public MainActivity() {
        noLoginRequired();
    }

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        ctx.startActivity(intent);
    }

    /**
     * 发起二维码扫描，
     * 仅供MainActivity下属Fragment调用，
     */
    public static void requestQrCodeScan(Activity ctx) {
        requestQrCodeScan(ctx, bitmap -> {
            EventBus.getDefault().post(new EventQRCodeReady(bitmap));
        });
    }

    public static void requestQrCodeScan(Activity ctx, AsyncUtils.Function<Bitmap> starter) {
        int size = ScreenUtil.getScreenWidth(MyApplication.getContext()) / 16 * 9;
        // 生成底部自己的二维码bitmap
        QRcodeActivity.getSelfQrCodeBitmap(size,
                CoreManager.requireSelf(ctx).getUserId(),
                CoreManager.requireSelf(ctx).getNickName(), starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 自动解锁屏幕 | 锁屏也可显示 | Activity启动时点亮屏幕 | 保持屏幕常亮
/*
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
*/
        setContentView(R.layout.activity_main);
       /* if (PermissionChecker.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_DENIED||PermissionChecker.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                    111);
        }*/
         if (PermissionChecker.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_DENIED||PermissionChecker.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                    111);
        }
        // 启动保活
        if (PrivacySettingHelper.getPrivacySettings(this).getIsKeepalive() == 1) {
            initKeepLive();
        }
        initLog();
        mUserId = coreManager.getSelf().getUserId();
        initView();// 初始化控件
        initBroadcast();// 初始化广播
        initDatas();// 初始化一些数据

        AsyncUtils.doAsync(this, mainActivityAsyncContext -> {
            // 获取app关闭之前还在上传的消息，将他们的发送状态置为失败
            List<UploadingFile> uploadingFiles = UploadingFileDao.getInstance().getAllUploadingFiles(coreManager.getSelf().getUserId());
            for (int i = uploadingFiles.size() - 1; i >= 0; i--) {
                ChatMessageDao.getInstance().updateMessageState(coreManager.getSelf().getUserId(), uploadingFiles.get(i).getToUserId(),
                        uploadingFiles.get(i).getMsgId(), ChatMessageListener.MESSAGE_SEND_FAILED);
            }
        });

        UpdateManger.checkUpdate(this, coreManager.getConfig().androidAppUrl, coreManager.getConfig().androidVersion, coreManager.getConfig().androidExplain);

        EventBus.getDefault().post(new MessageLogin());
        // 设备锁，
        showDeviceLock();
        // 初始化音视频Control
        AudioOrVideoController.init(mContext, coreManager);
        initMap();

        // 主页不要侧划返回，和ios统一，
        setSwipeBackEnable(false);

        setTouch(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "onNewIntent1");
        if (isInitView) {
            Log.e(TAG, "onNewIntent2");
            // 皮肤深浅变化时需要改状态栏颜色，
            setStatusBarColor();
            FragmentManager fm = getSupportFragmentManager();
            List<Fragment> lf = fm.getFragments();
            for (Fragment f : lf) {
                fm.beginTransaction().remove(f).commitNowAllowingStateLoss();
            }
            initView();
        }
        MainActivity.isInitView = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 主要针对侧滑返回，刷新消息会话列表，
        MsgBroadcast.broadcastMsgUiUpdate(mContext);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!JCVideoPlayer.backPress()) {
                // 调用JCVideoPlayer.backPress()
                // true : 当前正在全屏播放视频
                // false: 当前未在全屏播放视频
                moveTaskToBack(true);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        // XMPP断开连接 必须调用disconnect 否则服务端不能立即检测出当前用户离线 导致推送延迟
        coreManager.disconnect();

        unregisterReceiver(mUpdateUnReadReceiver);
        unregisterReceiver(mUserLogInOutReceiver);
        unregisterReceiver(my_broadcastReceiver);
        unregisterReceiver(timeChangeReceiver);
        EventBus.getDefault().unregister(this);

        Glide.get(this).clearMemory();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(getApplicationContext()).clearDiskCache();
            }
        });
        super.onDestroy();
    }

    private void initKeepLive() {
        //启动保活服务
        KeepLive.startWork(getApplication(), KeepLive.RunMode.ENERGY,
                //你需要保活的服务，如socket连接、定时任务等，建议不用匿名内部类的方式在这里写
                new KeepLiveService() {
                    /**
                     * 运行中
                     * 由于服务可能会多次自动启动，该方法可能重复调用
                     */
                    @Override
                    public void onWorking() {
                        Log.e("xuan", "onWorking: ");
                    }

                    /**
                     * 服务终止
                     * 由于服务可能会被多次终止，该方法可能重复调用，需同onWorking配套使用，如注册和注销broadcast
                     */
                    @Override
                    public void onStop() {
                        Log.e("xuan", "onStop: ");
                    }
                }
        );
    }

    private void initLog() {
        String dir = FileUtil.getSaveDirectory("IMLogs");
        LogUtils.setLogDir(dir);
        LogUtils.setLogLevel(LogUtils.LogLevel.WARN);
    }

    private void initView() {
        getSupportActionBar().hide();
        mRadioGroup = (RadioGroup) findViewById(R.id.main_rg);
        mRbTab1 = (RadioButton) findViewById(R.id.rb_tab_1);
        mRbTab2 = (RadioButton) findViewById(R.id.rb_tab_2);
        mRbTab3 = (RadioButton) findViewById(R.id.rb_tab_3);
        mRbTab4 = (RadioButton) findViewById(R.id.rb_tab_4);
        //注释视界
        mRbTab6 = (RadioButton) findViewById(R.id.rb_tab_6);
        mRbTab5 = (RadioButton) findViewById(R.id.rb_tab_5);
        ConfigBean.PopularApp popularAPP = coreManager.getConfig().popularAPP;
        if (popularAPP.shortVideo == 0) {
            //注释视界
            mRbTab6.setVisibility(View.GONE);
            findViewById(R.id.rlTrill).setVisibility(View.GONE);
        }
        if (!coreManager.getConfig().enableOpenSquare) {
            // 隐藏新广场，
            findViewById(R.id.rb_tab_7).setVisibility(View.GONE);
            //findViewById(R.id.rlSquare).setVisibility(View.GONE);
        }
        // 强制隐藏
/*
        ConfigBean.HomeAddress homeAddress = coreManager.getConfig().homeAddress;
        if (homeAddress != null && !TextUtils.isEmpty(homeAddress.getHomeUrl())) {
            mRbTab5.setText(homeAddress.getName());
            ImageLoadHelper.loadBitmapCenterCropDontAnimate(mContext, homeAddress.getImgUrl(), b -> {
                BitmapDrawable drawable = new BitmapDrawable(b);
                drawable.setBounds(0, 0,
                        ViewPiexlUtil.dp2px(mContext, 25), ViewPiexlUtil.dp2px(mContext, 25));
                mRbTab5.setCompoundDrawables(null, drawable, null, null);
            }, e -> {
                // 加载网址icon失败...
            });
            mRbTab5.setVisibility(View.VISIBLE);
            findViewById(R.id.rlHomeAddress).setVisibility(View.VISIBLE);
        } else {
            mRbTab5.setVisibility(View.GONE);
            findViewById(R.id.rlHomeAddress).setVisibility(View.GONE);
        }
*/
        mRbTab5.setVisibility(View.GONE);
        findViewById(R.id.rlHomeAddress).setVisibility(View.GONE);

        mTvMessageNum = (TextView) findViewById(R.id.main_tab_one_tv);
        mTvNewFriendNum = (TextView) findViewById(R.id.main_tab_two_tv);
        Friend newFriend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), Friend.ID_NEW_FRIEND_MESSAGE);
        if (newFriend != null) {
            updateNewFriendMsgNum(newFriend.getUnReadNum());
        }

        mTvCircleNum = (TextView) findViewById(R.id.main_tab_three_tv);

        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            hideInput();
            if (checkedId > 0 && mCurrtTabId != checkedId) {
                mCurrtTabId = checkedId;

                changeFragment(checkedId);

                if (checkedId == R.id.rb_tab_1) {
                    updateNumData();
                }
                JCVideoPlayer.releaseAllVideos();
            }
        });

        isCreate = false;
        mRbTab1.toggle();

        // 改皮肤，
        ColorStateList tabColor = SkinUtils.getSkin(this).getMainTabColorState();
        ColorStateList tabTextColor = SkinUtils.getSkin(this).getMainTabTextColorState();
        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) mRadioGroup.getChildAt(i);
            if (radioButton != mRbTab5) {
                // 图标着色，兼容性解决方案，
                Drawable drawable = radioButton.getCompoundDrawables()[1];
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(drawable, tabColor);
                // 如果是getDrawable拿到的Drawable不能直接调setCompoundDrawables，没有宽高，
                drawable.setBounds(0, 0,
                        ViewPiexlUtil.dp2px(mContext, 23), ViewPiexlUtil.dp2px(mContext, 23));
                radioButton.setCompoundDrawables(null, drawable, null, null);
            }
            radioButton.setTextColor(tabTextColor);
        }

        // 检查是否开启通知栏权限
        checkNotifyStatus();
    }

    private void initBroadcast() {
        EventBus.getDefault().register(this);

        // 注册未读消息更新广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE);
        filter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE_NEW_FRIEND);
        filter.addAction(MsgBroadcast.ACTION_MSG_NUM_RESET);
        mUpdateUnReadReceiver = new UpdateUnReadReceiver(this);
        registerReceiver(mUpdateUnReadReceiver, filter);

        // 注册用户登录状态广播
        mUserLogInOutReceiver = new UserLogInOutReceiver(this);
        registerReceiver(mUserLogInOutReceiver, LoginHelper.getLogInOutActionFilter());

        // 刷新评论的广播和 关闭主界面的，用于切换语言，更改皮肤用
        filter = new IntentFilter();
        // 当存在阅后即焚文字类型的消息时，当计时器计时结束但聊天界面已经销毁时(即聊天界面收不到该广播，消息也不会销毁)，代替销毁
        filter.addAction(Constants.UPDATE_ROOM);
        filter.addAction(OtherBroadcast.SYNC_CLEAN_CHAT_HISTORY);
        filter.addAction(OtherBroadcast.SYNC_SELF_DATE);
        filter.addAction(OtherBroadcast.CollectionRefresh);
        filter.addAction(OtherBroadcast.SEND_MULTI_NOTIFY);  // 群发消息结束
        my_broadcastReceiver = new My_BroadcastReceiver();
        registerReceiver(my_broadcastReceiver, filter);

        // 监听系统时间设置，
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        timeChangeReceiver = new TimeChangeReceiver(this);
        registerReceiver(timeChangeReceiver, filter);
    }

    private void initDatas() {
        // 检查用户的状态，做不同的初始化工作
        User loginUser = coreManager.getSelf();
        if (!LoginHelper.isUserValidation(loginUser)) {
            LoginHelper.prepareUser(this, coreManager);
        }
        LoginSecureHelper.autoLogin(this, coreManager, t -> {
            if (t instanceof LoginSecureHelper.LoginTokenOvertimeException) {
                MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_OVERDUE;
                loginOut();
            } else if (t instanceof LoginSecureHelper.LoginTokenInvalidException) {
                // 状态置为STATUS_USER_TOKEN_CHANGE会出现“账号在其他设备登录”的提示，应置为STATUS_USER_TOKEN_OVERDUE，提示登录过期
                // MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
                MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_OVERDUE;
                loginOut();
            }
        }, () -> {
            if (isFinishing()) {
                // 如果主页已经关闭，context失效，后续操作容易崩溃，也没有意思，所以直接结束，
                return;
            }
            isAutoLogin = true;
            // 进入主页后调的接口，都在刷新accessToken后再调用，
            loginRequired();
            initCore();
            CoreManager.initLocalCollectionEmoji();
            CoreManager.updateMyBalance();
            Reporter.initAfterLogin();
            initOther();// 初始化第三方
            checkTime();
            // 上传本地通讯录
            if ((coreManager.getConfig().isSupportAddress
                    && !coreManager.getConfig().registerUsername)) {
                addressBookOperation();
            }
            login();
            updateSelfData();
            preloadAd();
            WeboxHelper.init(this, coreManager);
        });

        mUserId = loginUser.getUserId();
        FriendDao.getInstance().checkSystemFriend(mUserId); // 检查 两个公众号

        // 更新所有未读的信息
        updateNumData();
        updateCircleNumData();
    }

    private void preloadAd() {
        AdHelper.preloadAd(mContext, AdHelper.AdType.Splash);
        AdHelper.preloadAd(mContext, AdHelper.AdType.Banner);
    }

    private void showDeviceLock() {
        if (DeviceLockHelper.isLocked()) {
            // 有开启设备锁，
            DeviceLockActivity.start(this);
        } else {
            Log.e("DeviceLock", "没开启设备锁，不弹出设备锁");
        }
    }

    private void initMap() {
        // 中国大陆只能使用百度，
        // 墙外且有谷歌框架才能使用谷歌地图，
        String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
        if (TextUtils.equals(area, "CN")) {
            MapHelper.setMapType(MapHelper.MapType.BAIDU);
        } else {
            MapHelper.setMapType(MapHelper.MapType.GOOGLE);
        }
    }

    /**
     * 切换Fragment
     */
    private void changeFragment(int checkedId) {
        if (mLastFragmentId == checkedId) {
            return;
        }
        UpdateManger.checkUpdate(this, coreManager.getConfig().androidAppUrl, coreManager.getConfig().androidVersion, coreManager.getConfig().androidExplain);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(checkedId));
        if (fragment == null) {
            switch (checkedId) {
                case R.id.rb_tab_1:
                    fragment = new MessageFragment();
                    break;
                case R.id.rb_tab_2:
                    fragment = new ContactPagerFragment();
                    break;
                case R.id.rb_tab_3:
                    fragment = new SquareFragment();
                    break;
                case R.id.rb_tab_4:
                    fragment = new KxMeFragment();
                    break;
                    //注释视界
                case R.id.rb_tab_6:
                    fragment = new TrillFragment();
                    break;
                case R.id.rb_tab_5:
                    fragment = new WebViewFragment();
                    break;
                    //注释广场
                case R.id.rb_tab_7:
                    fragment = new PublicSquareFragment();
                    break;
            }
        }

        // fragment = null;
        assert fragment != null;

        if (!fragment.isAdded()) {// 未添加 add
            transaction.add(R.id.main_content, fragment, String.valueOf(checkedId));
        }

        Fragment lastFragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(mLastFragmentId));

        if (lastFragment != null) {
            transaction.hide(lastFragment);
        }
        // 以防万一出现last和current都是同一个fragment的情况，先hide再show,
        transaction.show(fragment);

        // transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);// 添加动画
        transaction.commitNowAllowingStateLoss();

        // getSupportFragmentManager().executePendingTransactions();

        mLastFragmentId = checkedId;

    }

    /**
     * OPPO手机：App的通知默认是关闭的，需要检查通知是否开启
     * OPPO手机：App后台时，调用StartActivity方法不起做用，需提示用户至 手机管家-权限隐私-自启动管理 内该App的自启动开启
     * <p>
     * 小米与魅族手机需要开启锁屏显示权限，否则在锁屏时收到音视频消息来电界面无法弹起（其他手机待测试，华为手机无该权限设置，锁屏时弹起后直接干掉弹起页面）
     */
    private void checkNotifyStatus() {
        int launchCount = PreferenceUtils.getInt(this, Constants.APP_LAUNCH_COUNT, 0);// 记录app启动的次数
        Log.e("zq", "启动app的次数:" + launchCount);
        if (launchCount == 1) {
            String tip = "";
            if (!AppUtils.isNotificationEnabled(this)) {
                tip = getString(R.string.title_notification) + "\n" + getString(R.string.content_notification);
            }
            if (DeviceInfoUtil.isOppoRom()) {// 如果Rom为OPPO，还需要提醒用户开启自启动
                tip += getString(R.string.open_auto_launcher);
            }
            if (!TextUtils.isEmpty(tip)) {
                SelectionFrame dialog = new SelectionFrame(this);
                dialog.setSomething(null, tip, new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        PermissionUtil.startApplicationDetailsSettings(MainActivity.this, 0x001);
                    }
                });
                dialog.show();
            }
        } else if (launchCount == 2) {
            if (DeviceInfoUtil.isMiuiRom() || DeviceInfoUtil.isMeizuRom()) {
                SelectionFrame dialog = new SelectionFrame(this);
                dialog.setSomething(getString(R.string.open_screen_lock_show),
                        getString(R.string.open_screen_lock_show_for_audio), new SelectionFrame.OnSelectionFrameClickListener() {
                            @Override
                            public void cancelClick() {

                            }

                            @Override
                            public void confirmClick() {
                                PermissionUtil.startApplicationDetailsSettings(MainActivity.this, 0x001);
                            }
                        });
                dialog.show();
            }
        }
    }

    private void initOther() {
        Log.d(TAG, "initOther() called");
        SDKInitializer.initialize(getApplicationContext());
        // 服务器端是根据最后调用的上传推送ID接口决定使用什么推送，
        // 也就是在这里最后初始化哪个推送就会用哪个推送，

        AsyncUtils.doAsync(this, t -> {
            Reporter.post("初始化推送失败", t);
        }, mainActivityAsyncContext -> {
            if (coreManager.getConfig().enableGoogleFcm) {
                String refreshedToken = FirebaseInstanceId.getInstance().getToken();
                if (!TextUtils.isEmpty(refreshedToken)) {
                    Log.e(TAG, "初始化推送: 谷歌推送，");
                    FirebaseMessageService.init(MainActivity.this);
                } else {// 国行手机可能无法初始化谷歌推送，还是根据机型判断使用哪种推送
                    selectPush();
                }
            } else {
                selectPush();
            }
        });
    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    private void selectPush() {
        // 判断Rom使用推送
        if (DeviceInfoUtil.isEmuiRom()) {
            Log.e(TAG, "初始化推送: 华为推送，");
            // 华为手机 华为推送
            HuaweiPushRevicer.init(this);
        } else if (DeviceInfoUtil.isMeizuRom()) {
            Log.e(TAG, "初始化推送: 魅族推送，");
            MeizuPushMsgReceiver.init(this);
        } else if (DeviceInfoUtil.isOppoRom()) {
            Log.e(TAG, "初始化推送: OPPO推送，");
            HeytapPushManager.init(mContext, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OppoPushServiceAndroidQ.init(this);
            } else {
                OppoPushMessageService.init(this);
            }
        } else if (DeviceInfoUtil.isVivoRom()) {
            Log.e(TAG, "初始化推送: VIVO推送，");
            VivoPushMessageReceiver.init(this);
        } else if (true || DeviceInfoUtil.isMiuiRom()) {
            Log.e(TAG, "初始化推送: 小米推送，");
            if (shouldInit()) {
                // 小米推送初始化
                MiPushClient.registerPush(this, APP_ID, APP_KEY);
            }
        }
    }

    public void checkTime() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        long requestTime = System.currentTimeMillis();
        HttpUtils.get().url(coreManager.getConfig().GET_CURRENT_TIME)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        // 误差比config接口大，可能是主页线程做其他操作导致的，
                        // 和ios统一，进入主页时校准时间，
                        long responseTime = System.currentTimeMillis();
                        TimeUtils.responseTime(requestTime, result.getCurrentTime(), result.getCurrentTime(), responseTime);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // 不需要提示，
                        Log.e("TimeUtils", "校准时间失败", e);
                    }
                });
    }

    public void cancelUserCheckIfExist() {
        Log.d(TAG, "cancelUserCheckIfExist() called");
    }

    /* 当注销当前用户时，将那些需要当前用户的Fragment销毁，以后重新登陆后，重新加载为初始状态 */
    public void removeNeedUserFragment() {
        mRadioGroup.clearCheck();
        mLastFragmentId = -1;
        isCreate = true;
    }

    /**
     * 登录方法
     */
    public void login() {
        Log.d(TAG, "login() called");
        User user = coreManager.getSelf();

        Intent startIntent = CoreService.getIntent(MainActivity.this, user.getUserId(), user.getPassword(), user.getNickName());
        ContextCompat.startForegroundService(MainActivity.this, startIntent);

        mUserId = user.getUserId();
        numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mUserId);
        numCircle = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        updateNumData();
        if (isCreate) {
            mRbTab1.toggle();
        }
    }

    public void loginOut() {
        Log.d(TAG, "loginOut() called");
        coreManager.logout();
        removeNeedUserFragment();
        clearNotification();
        // 清空角标数量，
        ShortcutBadger.applyCount(mContext, 0);
        cancelUserCheckIfExist();
        if (MyApplication.getInstance().mUserStatus == LoginHelper.STATUS_USER_TOKEN_OVERDUE
                || MyApplication.getInstance().mUserStatus == LoginHelper.STATUS_USER_TOKEN_CHANGE
        ) {
            UserCheckedActivity.start(MyApplication.getContext());
        }
        finish();
    }

    private void clearNotification() {
        // 清除通知栏消息
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }
        CoreService.unReadNumberMap.clear();
    }

    public void conflict() {
        Log.d(TAG, "conflict() called");
        isConflict = true;// 标记一下

        coreManager.logout();
        removeNeedUserFragment();
        clearNotification();
        cancelUserCheckIfExist();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
        UserCheckedActivity.start(this);
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        }
        mActivityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        finish();
    }

    public void need_update() {
        Log.d(TAG, "need_update() called");
        removeNeedUserFragment();
        cancelUserCheckIfExist();
        // 弹出对话框
        UserCheckedActivity.start(this);
    }

    public void login_give_up() {
        Log.d(TAG, "login_give_up() called");
        removeNeedUserFragment();
        cancelUserCheckIfExist();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_NO_UPDATE;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventSelfRemoved message) {
        stopService(new Intent(mContext, WindowShowService.class));
        UserSp.getInstance(mContext).clearUserInfo();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
        loginOut();
        Intent intent = new Intent(mContext, LoginActivity.class);
        // 清空activity栈，
        // 确保当前在任何页面都能退出，
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageSendChat message) {
        if (!message.isGroup) {
            coreManager.sendChatMessage(message.toUserId, message.chat);
        } else {
            coreManager.sendMucChatMessage(message.toUserId, message.chat);
        }
    }

    // 更新发现模块新消息数量
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventHongdian message) {
        if (message.number == -1) {
            // 好友更新了动态
            int size = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
            if (size == 0) {
                // 本地社交圈无未读数量
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(R.id.rb_tab_3));
                if (fragment == null) {
                    // 发现页面还未创建，无法处理MessageEventHongdian这条通知，本地先记录下这个待处理操作
                    isSquareFragmentCreatedRefresh = true;
                }
                numCircle = -1;
                UiUtils.updateNum(mTvCircleNum, numCircle);
            }
            return;
        }
        numCircle = message.number;
        UiUtils.updateNum(mTvCircleNum, numCircle);
    }

    // 已上传的联系人注册了IM,更新到联系人表内
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageContactEvent mMessageEvent) {
        List<Contact> mNewContactList = ContactDao.getInstance().getContactsByToUserId(coreManager.getSelf().getUserId(),
                mMessageEvent.message);
        if (mNewContactList != null && mNewContactList.size() > 0) {
            updateContactUI(mNewContactList);
        }
    }

    /**
     * 生成底部二维码返回，跳转扫一扫界面
     *
     * @param eventQRCodeReady
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventQRCodeReady eventQRCodeReady) {
        if (scannerRequesting) {
            return;
        }
        if (!PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
            helloEventBus(eventQRCodeReady);
        }, null, PermissionUtil.getCameraPermissions())) {
            return;
        }
        // todo 目前调用requestQrCodeScan方法内的ctx对象均为getActivity获取(即MainActivity)，当ctx对象为恰activity时，这里就不能直接用this了，后面有需求在调整吧
        int size = ScreenUtil.getScreenWidth(MyApplication.getContext()) / 16 * 9;
        Intent intent = new Intent(this, ScannerActivity.class);
        // 设置扫码框的宽
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_WIDTH, size);
        // 设置扫码框的高
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_HEIGHT, size);
        // 设置扫码框距顶部的位置
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_TOP_PADDING, DisplayUtil.dip2px(this, 100));
        // 可以从相册获取
        intent.putExtra(Constant.EXTRA_IS_ENABLE_SCAN_FROM_PIC, true);
        if (eventQRCodeReady.getBitmap() != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            eventQRCodeReady.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            intent.putExtra(Constant.EXTRA_SELF_QR_CODE_BITMAP, bytes);
        }
        startActivityForResult(intent, REQUEST_SCANNER);
        scannerRequesting = true;
    }

    /**
     * 扫描二维码 || 全部群组内 加入群组时群主开启了群验证 发送入群请求给群主
     *
     * @param eventSendVerifyMsg
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventSendVerifyMsg eventSendVerifyMsg) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        String mLoginUserName = coreManager.getSelf().getNickName();
        ChatMessage message = createChatMessage(eventSendVerifyMsg, mLoginUserId, mLoginUserName,eventSendVerifyMsg.getCreateUserId());
        if (coreManager.isLogin()) {
            coreManager.sendChatMessage(eventSendVerifyMsg.getCreateUserId(), message);
            List<String> groupManagerUserIds = eventSendVerifyMsg.getGroupManagerUserAccidList();
            if (!groupManagerUserIds.isEmpty()) {
                for (String groupManagerUserId : groupManagerUserIds) {
                    ChatMessage newMessage = createChatMessage(eventSendVerifyMsg, mLoginUserId, mLoginUserName,groupManagerUserId);
                    coreManager.sendChatMessage(groupManagerUserId,newMessage);
                }
            }
        }
    }

    @NonNull
    private ChatMessage createChatMessage(EventSendVerifyMsg eventSendVerifyMsg, String mLoginUserId, String mLoginUserName, String toUserId) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GROUP_VERIFY);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginUserName);
        message.setToUserId(toUserId);
        String s = JsonUtils.initJsonContent(mLoginUserId, mLoginUserName, eventSendVerifyMsg.getGroupJid(), "1", eventSendVerifyMsg.getReason());
        message.setObjectId(s);
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        return message;
    }

    /*
    扫描二维码 || 全部群组内 加入群组 将群组存入朋友表
    */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventCreateGroupFriend eventCreateGroupFriend) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        String mLoginUserName = coreManager.getSelf().getNickName();
        MucRoom room = eventCreateGroupFriend.getMucRoom();

        MyApplication.getInstance().saveGroupPartStatus(room.getJid(), room.getShowRead(), room.getAllowSendCard(),
                room.getAllowConference(), room.getAllowSpeakCourse(), room.getTalkTime(), room.getAllowOpenLive());

        Friend friend = new Friend();
        friend.setOwnerId(mLoginUserId);
        friend.setUserId(room.getJid());
        friend.setNickName(room.getName());
        friend.setDescription(room.getDesc());
        friend.setRoomId(room.getId());
        friend.setRoomCreateUserId(room.getUserId());
        friend.setChatRecordTimeOut(room.getChatRecordTimeOut());// 消息保存天数 -1/0 永久
        friend.setContent(mLoginUserName + " " + getString(R.string.Message_Object_Group_Chat));
        friend.setTimeSend(TimeUtils.sk_time_current_time());
        friend.setRoomFlag(1);
        if (room.getMember() != null) {
            friend.setRoomRole(room.getMember().getRole());
        }
        friend.setStatus(Friend.STATUS_FRIEND);
        FriendDao.getInstance().createOrUpdateFriend(friend);

        if (room.getMember() != null) {
            // 调用socket加入群组的方法
            coreManager.joinMucChat(friend.getUserId(), room.getMember().getJoinSeqNo());
        } else {
            // 全部群组内的MucRoom为之前的对象，无member
            coreManager.joinMucChat(friend.getUserId(), 0);
        }
    }

    private boolean shouldInit() {
        ActivityManager activityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        String mainProcessName = getPackageName();
        int myPid = Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 关闭软键盘
     */
    public void hideInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder token = getWindow().getDecorView().getWindowToken();
        if (imm != null && imm.isActive() && token != null) {
            imm.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 手机联系人相关操作
     */
    private void addressBookOperation() {
        boolean isReadContacts = PermissionUtil.checkSelfPermissions(this, PermissionUtil.getReadContactsPermissions());
        if (isReadContacts) {
            try {
                uploadAddressBook();
            } catch (Exception e) {
                String message = getString(R.string.tip_read_contacts_failed);
                ToastUtil.showToast(this, message);
                Reporter.post(message, e);
                ContactsUtil.cleanLocalCache(this, coreManager.getSelf().getUserId());
            }
        } else {
            // 小米上架不不允许这里请求通讯录，
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SCANNER:
                scannerRequesting = false;
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || data.getExtras() == null) {
                        return;
                    }
                    String result = data.getExtras().getString(Constant.EXTRA_RESULT_CONTENT);
                    Log.e("zq", "二维码扫描结果：" + result);
                    if (TextUtils.isEmpty(result)) {
                        return;
                    }
                    if (PaymentReceiptMoneyActivity.checkQrCode(result)) {
                        // 长度为19且 && 纯数字 扫描他人的付款码 弹起收款界面
                        Intent intent = new Intent(mContext, PaymentReceiptMoneyActivity.class);
                        intent.putExtra("PAYMENT_ORDER", result);
                        startActivity(intent);
                    } else if (result.contains("userId")
                            && result.contains("userName")) {
                        // 扫描他人的收款码 弹起付款界面
                        Intent intent = new Intent(mContext, ReceiptPayMoneyActivity.class);
                        intent.putExtra("RECEIPT_ORDER", result);
                        startActivity(intent);
                    } else if (ReceiveChatHistoryActivity.checkQrCode(result)) {
                        // 扫描他人的发送聊天记录的二维码，弹起接收聊天记录页面，
                        ReceiveChatHistoryActivity.start(this, result);
                    } else if (WebLoginActivity.checkQrCode(result)) {
                        // 扫描其他平台登录的二维码，确认登录页面，
                        WebLoginActivity.start(this, result);
                    } else {
                        if (result.contains("chatId")) {
                            // 二维码
                            Map<String, String> map = WebViewActivity.URLRequest(result);
                            String action = map.get("action");
                            String id = map.get("chatId");
                            if (TextUtils.equals(action, "group")) {
                                getRoomInfo(id);
                            } else if (TextUtils.equals(action, "user")) {
                                getUserInfo(id);
                            } else if (TextUtils.equals(action, "company")) {
                                SearchCompanyActivity.start(this, id);
                            } else {
                                Reporter.post("二维码无法识别，<" + result + ">");
                                ToastUtil.showToast(this, R.string.unrecognized);
                            }
                        } else if (!result.contains("chatId")
                                && HttpUtil.isURL(result)) {
                            // 非二维码  访问其网页
                            Intent intent = new Intent(this, WebViewActivity.class);
                            intent.putExtra(WebViewActivity.EXTRA_URL, result);
                            startActivity(intent);
                        } else {
                            Reporter.post("二维码无法识别，<" + result + ">");
                            ToastUtil.showToast(this, R.string.unrecognized);
                        }
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 通过通讯号获得userId
     */
    private void getUserInfo(String account) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken);
        params.put("account", account);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_GET_URL_ACCOUNT)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            User user = result.getData();
                            BasicInfoActivity.start(mContext, user.getUserId(), BasicInfoActivity.FROM_ADD_TYPE_QRCODE);
                        } else {
                            ToastUtil.showErrorData(MyApplication.getInstance());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MyApplication.getInstance());
                    }
                });
    }

    /**
     * 获取房间信息
     */
    private void getRoomInfo(String roomId) {
        Friend friend = FriendDao.getInstance().getMucFriendByRoomId(coreManager.getSelf().getUserId(), roomId);
        if (friend != null) {
            if (friend.getGroupStatus() == 0
                    || friend.getGroupStatus() == 3
                    || friend.getGroupStatus() == 4) {
                interMucChat(friend.getUserId(), friend.getNickName());
                return;
            } else {// 已被踢出该群组 || 群组已被解散 || 群组已被后台锁定
                FriendDao.getInstance().deleteFriend(coreManager.getSelf().getUserId(), friend.getUserId());
                ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), friend.getUserId());
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                    @Override
                    public void onResponse(ObjectResult<MucRoom> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            final MucRoom mucRoom = result.getData();
                            if (mucRoom.getIsNeedVerify() == 1) {
                                ArrayList<String> list = new ArrayList<>();
                                for (MucRoomMember member : mucRoom.getMembers()) {
                                    if (member.getRole() == 2) {
                                        list.add(member.getUserId());
                                    }
                                }
                                DialogHelper.showGroupVerifyInputDialog(MainActivity.this, mucRoom.getUserId(), mucRoom.getJid(),list);
                                return;
                            }
                            joinRoom(mucRoom, coreManager.getSelf().getUserId());
                        } else {
                            ToastUtil.showErrorData(MainActivity.this);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MainActivity.this);
                    }
                });
    }

    /**
     * 加入房间
     */
    private void joinRoom(final MucRoom room, final String loginUserId) {
        DialogHelper.showDefaulteMessageProgressDialog(MainActivity.this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", room.getId());
        if (room.getUserId().equals(loginUserId))
            params.put("type", "1");
        else
            params.put("type", "2");
        params.put(AppConstant.GROUP_ADD_STYLE, AppConstant.GROUP_JOIN_SCAN);

        MyApplication.mRoomKeyLastCreate = room.getJid();

        HttpUtils.get().url(coreManager.getConfig().ROOM_JOIN)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(MainActivity.this, result)) {
                            EventBus.getDefault().post(new EventCreateGroupFriend(room));
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {// 给500ms的时间缓存，防止群组还未创建好就进入群聊天界面
                                    interMucChat(room.getJid(), room.getName());
                                }
                            }, 500);
                        } else {
                            MyApplication.mRoomKeyLastCreate = "compatible";
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MainActivity.this);
                        MyApplication.mRoomKeyLastCreate = "compatible";
                    }
                });
    }

    /**
     * 进入房间
     */
    private void interMucChat(String roomJid, String roomName) {
        Intent intent = new Intent(MainActivity.this, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);

        MucgroupUpdateUtil.broadcastUpdateUi(MainActivity.this);
    }

    private void uploadAddressBook() {
        List<Contacts> mNewAdditionContacts = ContactsUtil.getNewAdditionContacts(this, coreManager.getSelf().getUserId());
        /**
         * 本地生成
         * [{"name":"15768779999","telephone":"8615768779999"},{"name":"好搜卡","telephone":"8615720966659"},
         * {"name":"zas","telephone":"8613000000000"},{"name":"客服助手","telephone":"864007883333"},]
         * 服务端要求
         * [{\"toTelephone\":\"15217009762\",\"toRemarkName\":\"我是电话号码备注\"},{\"toTelephone\":\"15217009762\",\"toRemarkName\":\"我是电话号码备注\"}]
         */
        if (mNewAdditionContacts.size() <= 0) {
            return;
        }

        String step1 = JSON.toJSONString(mNewAdditionContacts);
        String step2 = step1.replaceAll("name", "toRemarkName");
        String contactsListStr = step2.replaceAll("telephone", "toTelephone");
        Log.e("contact", "新添加的联系人：" + contactsListStr);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("uploadJsonStr", contactsListStr);

        HttpUtils.post().url(coreManager.getConfig().ADDRESSBOOK_UPLOAD)
                .params(params)
                .build()
                .execute(new ListCallback<Contact>(Contact.class) {

                    @Override
                    public void onResponse(ArrayResult<Contact> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<Contact> mContactList = result.getData();
                            for (int i = 0; i < mContactList.size(); i++) {
                                Contact contact = mContactList.get(i);
                                if (ContactDao.getInstance().createContact(contact)) {
                                    if (contact.getStatus() == 1) {// 服务端自动成为好友，本地也需要添加
                                        NewFriendDao.getInstance().addFriendOperating(contact.getToUserId(), contact.getToUserName(), contact.getToRemarkName());
                                    }
                                }
                            }

                            if (mContactList.size() > 0) {// 显示数量新增数量  记录新增contacts id
                                updateContactUI(mContactList);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private void updateRoom() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", "0");
        params.put("pageIndex", "0");
        params.put("pageSize", String.valueOf(Integer.MAX_VALUE));// 不支持翻页，直接给最大值，

        HttpUtils.get().url(coreManager.getConfig().ROOM_LIST_HIS)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(ArrayResult<MucRoom> result) {
                        if (result.getResultCode() == 1) {
                            FriendDao.getInstance().addRooms(mHandler, coreManager.getSelf().getUserId(), result.getData(), new OnCompleteListener2() {
                                @Override
                                public void onLoading(int progressRate, int sum) {

                                }

                                @Override
                                public void onCompleted() {
/*
                                    if (coreManager.isLogin()) {
                                        coreManager.batchMucChat();
                                    }
*/
                                    MsgBroadcast.broadcastMsgUiUpdate(MainActivity.this);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /*
    消息 发现
     */
    public void msg_num_update(int operation, int count) {
        numMessage = (operation == MsgBroadcast.NUM_ADD) ? numMessage + count : numMessage - count;
        updateNumData();
    }

    public void msg_num_reset() {
        updateNumData();
    }

    public void updateNumData() {
        numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mUserId);

        ShortcutBadger.applyCount(this, numMessage);

        UiUtils.updateNum(mTvMessageNum, numMessage);
    }

    public void updateCircleNumData() {
        numCircle = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        UiUtils.updateNum(mTvCircleNum, numCircle);
    }

    /*
    通讯录
     */
    public void updateNewFriendMsgNum(int msgNum) {
        // 通讯录页面没有手机联系人了，这里角标不加上，

        UiUtils.updateNum(mTvNewFriendNum, msgNum);
    }

    private void updateContactUI(List<Contact> mContactList) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        int mContactsNumber = PreferenceUtils.getInt(MainActivity.this, Constants.NEW_CONTACTS_NUMBER + mLoginUserId, 0);
        int mTotalContactsNumber = mContactsNumber + mContactList.size();
        PreferenceUtils.putInt(MainActivity.this, Constants.NEW_CONTACTS_NUMBER + mLoginUserId, mTotalContactsNumber);
        Friend newFriend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), Friend.ID_NEW_FRIEND_MESSAGE);
        updateNewFriendMsgNum(newFriend.getUnReadNum());

        List<String> mNewContactsIds = new ArrayList<>();
        for (int i = 0; i < mContactList.size(); i++) {
            mNewContactsIds.add(mContactList.get(i).getToUserId());
        }
        String mContactsIds = PreferenceUtils.getString(MainActivity.this, Constants.NEW_CONTACTS_IDS + mLoginUserId);
        List<String> ids = JSON.parseArray(mContactsIds, String.class);
        if (ids != null && ids.size() > 0) {
            mNewContactsIds.addAll(ids);
        }
        PreferenceUtils.putString(MainActivity.this, Constants.NEW_CONTACTS_IDS + mLoginUserId, JSON.toJSONString(mNewContactsIds));
    }

    private void updateSelfData() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            User user = result.getData();
                            // 余额从单独接口获取，不要被user/get的覆盖，
                            user.setBalance(coreManager.getSelf().getBalance());
                            boolean updateSuccess = UserDao.getInstance().updateByUser(user);
                            // 设置登陆用户信息
                            if (updateSuccess) {
                                // 如果成功，保存User变量，
                                coreManager.setSelf(user);
                                // 通知MeFragment更新
                                sendBroadcast(new Intent(OtherBroadcast.SYNC_SELF_DATE_NOTIFY));
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    public void notifyCollectionList() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", coreManager.getSelf().getUserId());

        HttpUtils.get().url(coreManager.getConfig().Collection_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<Collectiion>(Collectiion.class) {
                    @Override
                    public void onResponse(ArrayResult<Collectiion> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            MyApplication.mCollection = result.getData();
                            Collectiion collection = new Collectiion();
                            collection.setType(7);
                            Collectiion dice = new Collectiion();
                            dice.setType(9);
                            Collectiion rps = new Collectiion();
                            rps.setType(10);
                            MyApplication.mCollection.add(0, collection);
                            MyApplication.mCollection.add(1, dice);
                            MyApplication.mCollection.add(2, rps);
                            // 发送广播通知更新
                            sendBroadcast(new Intent(OtherBroadcast.CollectionRefresh_ChatFace));
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(MyApplication.getContext());
                    }
                });
    }

    private class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }

            if (action.equals(Constants.UPDATE_ROOM)) {
                updateRoom();
            } else if (action.equals(SocketException.FINISH_CONNECT_EXCEPTION)) {
                coreManager.autoReconnect(MainActivity.this);
            } else if (action.equals(OtherBroadcast.SYNC_CLEAN_CHAT_HISTORY)) {
                boolean cleanAll = intent.getBooleanExtra("CLEAN_ALL", false);
                String roomJid = intent.getStringExtra("roomJid");
                String friendId = intent.getStringExtra(AppConstant.EXTRA_USER_ID);
                AsyncUtils.doAsync(this, settingActivityAsyncContext -> {
                    if (cleanAll) {
                        // 后台清空所有聊天记录
                        List<Friend> mNearChatFriendList = FriendDao.getInstance().getNearlyFriendMsg(coreManager.getSelf().getUserId());
                        for (int i = 0; i < mNearChatFriendList.size(); i++) {
                            FriendDao.getInstance().resetFriendMessage(coreManager.getSelf().getUserId(), mNearChatFriendList.get(i).getUserId());
                            ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), mNearChatFriendList.get(i).getUserId());
                        }
                        MsgRoamTaskDao.getInstance().deleteAllMsgRoamTasks(coreManager.getSelf().getUserId());
                    } else if (!TextUtils.isEmpty(roomJid)) {
                        // 后台清空某个群组的聊天记录
                        FriendDao.getInstance().resetFriendMessage(coreManager.getSelf().getUserId(), roomJid);
                        ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), roomJid);
                        MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(coreManager.getSelf().getUserId(), roomJid);
                    } else {
                        // 后台清空某个用户与某个用户的聊天记录 或 某个用户在应用内清空双方聊天记录
                        FriendDao.getInstance().resetFriendMessage(coreManager.getSelf().getUserId(), friendId);
                        ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), friendId);
                        MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(coreManager.getSelf().getUserId(), friendId);
                    }
                    runOnUiThread(() -> {
                        // 清空聊天界面
                        Intent intent1 = new Intent(Constants.CHAT_HISTORY_EMPTY);
                        intent1.putExtra("id", cleanAll ?
                                "" : (TextUtils.isEmpty(roomJid) ? friendId : roomJid));
                        sendBroadcast(intent1);
                        MsgBroadcast.broadcastMsgUiUpdate(mContext);
                        MsgBroadcast.broadcastMsgNumReset(mContext);
                    });
                });
            } else if (action.equals(OtherBroadcast.SYNC_SELF_DATE)) {
                updateSelfData();
            } else if (action.equals(OtherBroadcast.CollectionRefresh)) {
                notifyCollectionList();
            } else if (action.equals(OtherBroadcast.SEND_MULTI_NOTIFY)) {
                mRbTab4.setChecked(false);
                mRbTab1.setChecked(true);
            }
        }
    }

    /**
     * 我方取消、挂断通话后发送XMPP消息给对方
     * copy by AudioOrVideoController
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventCancelOrHangUp event) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        ChatMessage message = new ChatMessage();
        if (event.type == 103) {          // 取消 语音通话
            message.setType(XmppMessage.TYPE_NO_CONNECT_VOICE);
        } else if (event.type == 104) {// 取消 视频通话
            message.setType(XmppMessage.TYPE_END_CONNECT_VOICE);
        } else if (event.type == 113) {// 挂断 语音通话
            message.setType(XmppMessage.TYPE_NO_CONNECT_VIDEO);
        } else if (event.type == 114) {// 挂断 视频通话
            message.setType(XmppMessage.TYPE_END_CONNECT_VIDEO);
        }
        message.setMySend(true);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(coreManager.getSelf().getNickName());
        message.setToUserId(event.toUserId);
        message.setContent(event.content);
        message.setTimeLen(event.callTimeLen);
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));

        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, event.toUserId, message)) {
            ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, message.getFromUserId(), message, false);
        }

        coreManager.sendChatMessage(event.toUserId, message);
        MsgBroadcast.broadcastMsgUiUpdate(mContext);   // 更新消息界面
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventInitiateMeeting message) {
        String mLoginUserId = coreManager.getSelf().getUserId();
        String mLoginNickName = coreManager.getSelf().getNickName();

        Jitsi_connecting_second.start(this, mLoginUserId, mLoginUserId, message.type);

        for (int i = 0; i < message.list.size(); i++) {
            ChatMessage mMeetingMessage = new ChatMessage();
            int type;
            String str;
            if (message.type == CallConstants.Audio_Meet) {
                type = XmppMessage.TYPE_IS_MU_CONNECT_VOICE;
                str = getString(R.string.tip_invite_voice_meeting);
            } else if (message.type == CallConstants.Video_Meet) {
                type = XmppMessage.TYPE_IS_MU_CONNECT_VIDEO;
                str = getString(R.string.tip_invite_video_meeting);
            } else {
                type = XmppMessage.TYPE_IS_MU_CONNECT_TALK;
                str = getString(R.string.tip_invite_talk_meeting);
            }
            mMeetingMessage.setType(type);
            mMeetingMessage.setContent(str);
            mMeetingMessage.setFromUserId(mLoginUserId);
            mMeetingMessage.setFromUserName(mLoginNickName);
            mMeetingMessage.setObjectId(mLoginUserId);
            mMeetingMessage.setTimeSend(TimeUtils.sk_time_current_time());
            mMeetingMessage.setToUserId(message.list.get(i));
            mMeetingMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            coreManager.sendChatMessage(message.list.get(i), mMeetingMessage);
            // 音视频会议消息不保存
/*
            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.list.get(i), mMeetingMessage);
            FriendDao.getInstance().updateFriendContent(mLoginUserId, message.list.get(i), str, type, TimeUtils.sk_time_current_time());
*/
        }
    }
}