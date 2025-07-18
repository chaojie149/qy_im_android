package com.tongxin.caihong;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import androidx.multidex.MultiDex;

import com.tongxin.caihong.bean.PrivacySetting;
import com.tongxin.caihong.bean.collection.Collectiion;
import com.tongxin.caihong.bean.event.MessageEventBG;
import com.tongxin.caihong.db.SQLiteHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.socket.EMConnectionManager;
import com.tongxin.caihong.ui.tool.MyFileNameGenerator;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.LocaleHelper;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ScreenShotListenManager;
import com.danikula.videocache.HttpProxyCacheServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class MyApplication extends Application {
    public static final String TAG = "MyApplication";
    // 服务器是否开启集群 如开启，在登录、自动登录时需要传area，在发起音视频通话(单人)时会要调接口获取通话地址
    public static boolean IS_OPEN_CLUSTER = false;
    public static boolean IS_OPEN_RECEIPT = true;
    // 是否支持端到端加密
    public static boolean IS_SUPPORT_SECURE_CHAT = true;
    // 是否支持多端登录
    public static boolean IS_SUPPORT_MULTI_LOGIN;

    public static String IsRingId = "Empty";// 当前聊天对象的id/jid 用于控制消息来时是否响铃通知
    // 本地建群时的jid(给个初始值坐下兼容) 用于防止收到服务端的907消息时本地也在建群而造成群组重复
    public static String mRoomKeyLastCreate = "compatible";
    public static List<Collectiion> mCollection = new ArrayList<>();
    private static MyApplication INSTANCE = null;
    private static Context context;
    /* 文件缓存的目录 */
    public String mAppDir01;
    public String mPicturesDir01;
    public String mVoicesDir01;
    public String mVideosDir01;
    public String mFilesDir01;
    public int mActivityCount = 0;
    /* 文件缓存的目录 */
    public String mAppDir;
    public String mPicturesDir;
    public String mVoicesDir;
    public String mVideosDir;
    public String mFilesDir;
    public int mUserStatus;
    public boolean mUserStatusChecked = true;
    /*********************
     * 百度地图定位服务
     ************************/
    private BdLocationHelper mBdLocationHelper;
    private LruCache<String, Bitmap> mMemoryCache;
    // 抖音模块缓存
    private HttpProxyCacheServer proxy;

    public static MyApplication getInstance() {
        return INSTANCE;
    }

    public static Context getContext() {
        return context;
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        MyApplication app = (MyApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    /**
     * https://bugly.qq.com/docs/user-guide/instruction-manual-android/#_6
     * 获取进程号对应的进程名
     *
     * @return 进程名
     */
    private static String getMyProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        int pid = android.os.Process.myPid();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        context = getApplicationContext();

        if (AppConfig.DEBUG) {
            Log.d(AppConfig.TAG, "MyApplication onCreate");
        }

        initMulti();

        // 在7.0的设备上，开启该模式访问相机或裁剪居然不会抛出FileUriExposedException异常，记录一下
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());

        // 初始化数据库
        SQLiteHelper.copyDatabaseFile(this);
        // 初始化定位
        getBdLocationHelper();
        // 初始化App目录
        initAppDir();
        initAppDirsecond();
        // 初始化图片加载 缓存
        initLruCache();

        // 判断前后台切换
        getAppBackground();
        // 监听屏幕截图
        ListeningScreenshots();

        if (TextUtils.equals(getMyProcessName(), getPackageName())) {
            int launchCount = PreferenceUtils.getInt(this, Constants.APP_LAUNCH_COUNT, 0);// 记录app启动的次数
            PreferenceUtils.putInt(this, Constants.APP_LAUNCH_COUNT, ++launchCount);
        }

        initMap();

        initLanguage();

        initReporter();

        disableAPIDialog();

        disableWatchdog();
    }

    /**
     * 有个watchdog负责监控垃圾对象回收，
     * 在oppo上总是超时导致崩溃，直接禁用，
     * https://www.jianshu.com/p/89e2719be9c7
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private void disableWatchdog() {
        try {
            Class clazz = Class.forName("java.lang.Daemons$FinalizerWatchdogDaemon");
            Method method = clazz.getSuperclass().getDeclaredMethod("stop");
            method.setAccessible(true);
            Field field = clazz.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            method.invoke(field.get(null));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 安卓9以上的hide api警告对话框，
     * 各种库大量使用hide api, 无法都解决掉，
     * 用hide api解决hide api警告，感觉算漏洞，以后可能失效，
     * <p>
     * 反射 禁止弹窗
     */
    @SuppressWarnings("all")
    private void disableAPIDialog() {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            Class clazz = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = clazz.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);
            Field mHiddenApiWarningShown = clazz.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initReporter() {
        Reporter.init(this);
    }

    private void initLanguage() {
        // 应用程序里设置的语言，否则程序杀死后重启又会是系统语言，
        LocaleHelper.setLocale(this, LocaleHelper.getLanguage(this));
    }

    private void initMap() {
        MapHelper.initContext(this);
        // 默认为百度地图，
        PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(this);
        boolean isGoogleMap = privacySetting.getIsUseGoogleMap() == 1;
        if (isGoogleMap) {
            MapHelper.setMapType(MapHelper.MapType.GOOGLE);
        } else {
            MapHelper.setMapType(MapHelper.MapType.BAIDU);
        }
    }

    private void ListeningScreenshots() {
        ScreenShotListenManager manager = ScreenShotListenManager.newInstance(this);
        manager.setListener(new ScreenShotListenManager.OnScreenShotListener() {
            @Override
            public void onShot(String imagePath) {
                PreferenceUtils.putString(getApplicationContext(), Constants.SCREEN_SHOTS, imagePath);
            }
        });
        manager.startListen();
    }

    private void getAppBackground() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (mActivityCount == 0) {
                    Log.e(TAG, "程序已到前台,检查XMPP是否验证");
                    EventBus.getDefault().post(new MessageEventBG(true, false));
                }
                mActivityCount++;
                Log.e(TAG, "onActivityStarted-->" + mActivityCount);
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                mActivityCount--;
                Log.e(TAG, "onActivityStopped-->" + mActivityCount);
                if (!AppUtils.isAppForeground(getContext())) {// 在app启动时，当启动页stop，而MainActivity还未start时，又会回调到该方法内，所以需要判断到底是不是真的处于后台
                    EventBus.getDefault().post(new MessageEventBG(false, false));
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public void initMulti() {
        // 只能在登录的时候修改，所以不能放到 setPrivacySettings 内
        PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(this);
        boolean isSupport = privacySetting.getMultipleDevices() == 1;
        if (isSupport) {
            IS_SUPPORT_MULTI_LOGIN = true;
            EMConnectionManager.CURRENT_DEVICE = "android";
        } else {
            IS_SUPPORT_MULTI_LOGIN = false;
            EMConnectionManager.CURRENT_DEVICE = "chat";
        }
    }

    /*
    保存某群组的部分属性
     */
    public void saveGroupPartStatus(String groupJid, int mGroupShowRead, int mGroupAllowSecretlyChat,
                                    int mGroupAllowConference, int mGroupAllowSendCourse, long mGroupTalkTime, int mGroupAllowLive) {
        SharedPreferences.Editor edit = PreferenceUtils.getPreferences(context).edit();
        saveGroupPartStatus(edit, groupJid, mGroupShowRead, mGroupAllowSecretlyChat, mGroupAllowConference, mGroupAllowSendCourse, mGroupTalkTime, mGroupAllowLive);
        edit.apply();
    }

    public void saveGroupPartStatus(SharedPreferences.Editor edit, String groupJid, int mGroupShowRead, int mGroupAllowSecretlyChat,
                                    int mGroupAllowConference, int mGroupAllowSendCourse, long mGroupTalkTime, int mGroupAllowLive) {
        // 是否显示群消息已读人数
        edit.putBoolean(Constants.IS_SHOW_READ + groupJid, mGroupShowRead == 1);
        // 是否允许普通成员私聊
        edit.putBoolean(Constants.IS_SEND_CARD + groupJid, mGroupAllowSecretlyChat == 1);
        // 是否允许普通成员发起会议
        edit.putBoolean(Constants.IS_ALLOW_NORMAL_CONFERENCE + groupJid, mGroupAllowConference == 1);
        // 是否允许普通成员发送课程
        edit.putBoolean(Constants.IS_ALLOW_NORMAL_SEND_COURSE + groupJid, mGroupAllowSendCourse == 1);
        // 是否开启了全体禁言
        edit.putBoolean(Constants.GROUP_ALL_SHUP_UP + groupJid, mGroupTalkTime > 0);
        // 是否允许普通成员发起直播
        edit.putBoolean(Constants.IS_ALLOW_NORMAL_LIVE + groupJid, mGroupAllowLive == 1);
    }

    /**
     * 初始化支付密码设置状态，
     * 登录接口返回支付密码是否设置，在这里保存起来，
     *
     * @param payPassword 支付密码是否已经设置，
     */
    public void initPayPassword(String userId, int payPassword) {
        Log.d(TAG, "initPayPassword() called with: userId = [" + userId + "], payPassword = [" + payPassword + "]");
        // 和initPrivateSettingStatus中的其他变量保存方式统一，
        PreferenceUtils.putBoolean(this, Constants.IS_PAY_PASSWORD_SET + userId, payPassword == 1);
    }

    public BdLocationHelper getBdLocationHelper() {
        if (mBdLocationHelper == null) {
            mBdLocationHelper = new BdLocationHelper(this);
        }
        return mBdLocationHelper;
    }

    // 意义不明，
    private void initAppDirsecond() {
        File innerFile = new File(getFilesDir(), "external");
        File file = getExternalFilesDir(null);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = innerFile;
        }
        mAppDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_PICTURES);
        }
        mPicturesDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_MUSIC);
        }
        mVoicesDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_MOVIES);
        }
        mVideosDir01 = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_DOWNLOADS);
        }
        mFilesDir01 = file.getAbsolutePath();
    }

    private void initAppDir() {
        File innerFile = new File(getFilesDir(), "external");
        File file = getExternalFilesDir(null);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = innerFile;
        }
        mAppDir = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_PICTURES);
        }
        mPicturesDir = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_MUSIC);
        }
        mVoicesDir = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_MOVIES);
        }
        mVideosDir = file.getAbsolutePath();

        file = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (file != null && !file.exists()) {
            file.mkdirs();
        }
        if (file == null) {
            // 不能为空，
            file = new File(innerFile, Environment.DIRECTORY_DOWNLOADS);
        }
        mFilesDir = file.getAbsolutePath();
    }

    /**
     * 在程序内部关闭时，调用此方法
     */
    public void destory() {
        if (AppConfig.DEBUG) {
            Log.d(AppConfig.TAG, "MyApplication destory");
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void destoryRestart() {
        if (AppConfig.DEBUG) {
            Log.d(AppConfig.TAG, "MyApplication destory");
        }
    }

    /***********************
     * 保存其他用户坐标信息
     ***************/

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public void initLruCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                .maxCacheSize(1024 * 1024 * 1024)       // 1 Gb for cache
                .fileNameGenerator(new MyFileNameGenerator()).build();
    }
}
