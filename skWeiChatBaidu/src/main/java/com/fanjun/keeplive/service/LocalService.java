package com.fanjun.keeplive.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.fanjun.keeplive.KeepLive;
import com.fanjun.keeplive.receiver.OnepxReceiver;
import com.fanjun.keeplive.utils.ServiceUtils;
import com.tongxin.caihong.R;

import java.util.Objects;

public final class LocalService extends Service {
    private static final String channelId = "channelId1";//渠道id
    private OnepxReceiver mOnepxReceiver;
    private ScreenStateReceiver screenStateReceiver;
    private boolean isPause = true;//控制暂停
    private MediaPlayer mediaPlayer;
    private MyBilder mBilder;
    private Handler handler;
    private boolean mIsBoundRemoteService;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (ServiceUtils.isServiceRunning(getApplicationContext(), "com.fanjun.keeplive.service.LocalService")) {
                Intent remoteService = new Intent(LocalService.this,
                        RemoteService.class);
                ContextCompat.startForegroundService(LocalService.this, remoteService);
                Intent intent = new Intent(LocalService.this, RemoteService.class);
                mIsBoundRemoteService = LocalService.this.bindService(intent, connection,
                        Context.BIND_ABOVE_CLIENT);
            }
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            if (isScreenOn) {
                sendBroadcast(new Intent("_ACTION_SCREEN_ON"));
            } else {
                sendBroadcast(new Intent("_ACTION_SCREEN_OFF"));
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                if (mBilder != null && KeepLive.foregroundNotification != null) {
                    GuardAidl guardAidl = GuardAidl.Stub.asInterface(service);
                    guardAidl.wakeUp(KeepLive.foregroundNotification.getTitle(), KeepLive.foregroundNotification.getDescription(), KeepLive.foregroundNotification.getIconRes());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification createNotification(Context ctx) {
        Notification.Builder builder;
        //第二个参数与channelId对应
        builder = new Notification.Builder(ctx, channelId);
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(ctx.getString(R.string.app_name));
        builder.setContentText("");
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setAutoCancel(true);
        builder.setShowWhen(true);
        builder.setSmallIcon(android.R.drawable.stat_notify_chat);
        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createChannel(Context ctx) {
        if (disableChannel(ctx)) return;
        NotificationManager nm = Objects.requireNonNull(ContextCompat.getSystemService(ctx, NotificationManager.class));
        if (nm.getNotificationChannel(channelId) != null) {
            return;
        }
        //创建通知渠道
        CharSequence name = ctx.getString(R.string.keeplive);
        String description = ctx.getString(R.string.keeplive);
        int importance = NotificationManager.IMPORTANCE_MIN; //重要性级别 开启通知，不会弹出，但没有提示音，状态栏中无显示
        NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);
        mChannel.setDescription(description);//渠道描述
        mChannel.setVibrationPattern(new long[]{0});//震动频率
        mChannel.enableLights(false);//是否显示通知指示灯
        mChannel.enableVibration(false);//是否振动
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);//设置在锁屏界面上显示这条通知
        mChannel.setLightColor(Color.GREEN);//如果显示的话就为绿色
        mChannel.setName(name);
        mChannel.setSound(null, null);
        nm.createNotificationChannel(mChannel);//创建通知渠道
    }

    private static boolean disableChannel(Context ctx) {
        // 测试表明，目标版本在26时不用创建渠道，可以实现高优先级和无通知兼得，
        // 目标版本在29时不创建渠道会导致安卓10必然崩溃，
        int targetSdkVersion = ctx.getApplicationInfo().targetSdkVersion;
        if (targetSdkVersion == Build.VERSION_CODES.O) {
            return true;
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mBilder == null) {
            mBilder = new MyBilder();
        }
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        isPause = pm.isScreenOn();
        if (handler == null) {
            handler = new Handler();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBilder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(this);
            Notification notification = createNotification(this);
            //启用前台服务，提升优先级
            startForeground(13691, notification);
        }
        if (KeepLive.useSilenceMusice) {
            //播放无声音乐
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.novioce);
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0f, 0f);
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            if (!isPause) {
                                if (KeepLive.runMode == KeepLive.RunMode.ROGUE) {
                                    play();
                                } else {
                                    if (handler != null) {
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                play();
                                            }
                                        }, 5000);
                                    }
                                }
                            }
                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            return false;
                        }
                    });
                    play();
                }
            }
        }
        //像素保活
        if (mOnepxReceiver == null) {
            mOnepxReceiver = new OnepxReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        registerReceiver(mOnepxReceiver, intentFilter);
        //屏幕点亮状态监听，用于单独控制音乐播放
        if (screenStateReceiver == null) {
            screenStateReceiver = new ScreenStateReceiver();
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("_ACTION_SCREEN_OFF");
        intentFilter2.addAction("_ACTION_SCREEN_ON");
        registerReceiver(screenStateReceiver, intentFilter2);
        //绑定守护进程
        try {
            Intent intent3 = new Intent(this, RemoteService.class);
            mIsBoundRemoteService = this.bindService(intent3, connection, Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
        }
        //隐藏服务通知
        try {
            if (Build.VERSION.SDK_INT < 25) {
                startService(new Intent(this, HideForegroundService.class));
            }
        } catch (Exception e) {
        }
        if (KeepLive.keepLiveService != null) {
            KeepLive.keepLiveService.onWorking();
        }
        return START_STICKY;
    }

    private void play() {
        if (KeepLive.useSilenceMusice) {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }
    }

    private void pause() {
        if (KeepLive.useSilenceMusice) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            try {
                if (mIsBoundRemoteService) {
                    unbindService(connection);
                }
            } catch (Exception e) {
            }
        }
        try {
            unregisterReceiver(mOnepxReceiver);
            unregisterReceiver(screenStateReceiver);
        } catch (Exception e) {
        }
        if (KeepLive.keepLiveService != null) {
            KeepLive.keepLiveService.onStop();
        }
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals("_ACTION_SCREEN_OFF")) {
                isPause = false;
                play();
            } else if (intent.getAction().equals("_ACTION_SCREEN_ON")) {
                isPause = true;
                pause();
            }
        }
    }

    private final class MyBilder extends GuardAidl.Stub {

        @Override
        public void wakeUp(String title, String discription, int iconRes) throws RemoteException {

        }
    }
}
