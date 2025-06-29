package com.tongxin.caihong.audio;

import static android.content.Context.VIBRATOR_SERVICE;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;

import androidx.annotation.NonNull;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.PrivacySetting;
import com.tongxin.caihong.helper.DisturbHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;

import java.io.IOException;

/**
 * Created by wzw on 2017/3/22.
 * <p>
 * 提示音播放 + 手机振动
 */

public class NoticeVoicePlayer {
    private static NoticeVoicePlayer instance;
    // 当手机开启静音，部分手机的多媒体不会被管控，改为Ringtone播放
    /*private MediaPlayer mediaPlayer;*/
    private Ringtone ringtone;
    private Vibrator vibrator;// 振动
    private long start;
    private MediaPlayer mediaPlayer;
    private Handler handler = new BellHandler();

    public NoticeVoicePlayer() {
        /*mediaPlayer = MediaPlayer.create(MyApplication.getContext(), R.raw.msg);*/
        ringtone = initRingtone(MyApplication.getContext());
        vibrator = (Vibrator) MyApplication.getContext().getSystemService(VIBRATOR_SERVICE);
    }

    public static NoticeVoicePlayer getInstance() {
        if (instance == null) {
            instance = new NoticeVoicePlayer();
        }
        return instance;
    }

    /**
     * init type of Ringtone
     *
     * @param context Activity
     * @return Ringtone
     */
    private Ringtone initRingtone(Context context) {
        // 系统默认通知提示音
        /*Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);*/
        Uri ringTone = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.msg);
        return RingtoneManager.getRingtone(context, ringTone);
    }

    public void start() {
        start(false);
    }

    public void start(boolean isBell) {
        if (DisturbHelper.doNotDisturbNow(MyApplication.getContext())) {
            return;
        }
        if (isBell) {
            bell();
        } else {
            ringtone.play();
        }
        // 停止 开启 停止 开启
        long[] pattern = {100, 200, 100, 200};
        // 重复两次上面的pattern 如果只想震动一次，index设为-1
        // vibrator.vibrate(pattern, 2);
        // 判断该用户是否开启振动
        PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(MyApplication.getContext());
        boolean input = privacySetting.getIsVibration() == 1;
        if (input) {
            if (System.currentTimeMillis() - start > 2000) {
                vibrator.vibrate(pattern, -1);
            } else {
                start = System.currentTimeMillis();
            }

        }
    }

    public void stop() {
        /*mediaPlayer.stop();*/
        ringtone.stop();
        PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(MyApplication.getContext());
        boolean input = privacySetting.getIsVibration() == 1;
        if (input) {
            vibrator.cancel();
        }
    }

    public void bell() {
        try {
            if (mediaPlayer == null) {
                AssetFileDescriptor mAssetFileDescriptor = MyApplication.getContext().getAssets().openFd("dial.mp3");
                mediaPlayer = new MediaPlayer();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(mAssetFileDescriptor.getFileDescriptor(), mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
            }
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
            handler.removeMessages(1);
            handler.sendEmptyMessageDelayed(1, 30000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopBell() {
        handler.removeMessages(1);
        try {
            mediaPlayer.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class BellHandler extends Handler {
        public BellHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            stopBell();
        }
    }

}
