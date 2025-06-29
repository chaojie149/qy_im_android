package com.tongxin.caihong.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.tongxin.caihong.audio_x.VoiceManager;

public class AudioModeManger {
    private static final String TAG = "AudioModeManger";
    private AudioManager audioManager;
    private SensorManager sensorManager;
    private Context ctx;
    private Sensor mProximiny;
    private onSpeakerListener mOnSpeakerListener;
    private HeadsetReceiver headsetReceiver;

    /**
     * 在识别扬声器与听筒时，mDistanceSensorListener会连续回调多次，我们取第一次作为有效回调
     */
    private long mLastSpeakerSwitchingTime;// 上一次扬声器切换时间
    private long mLastEarpieceSwitchingTime;// 上一次听筒时间
    /**
     * 距离传感器监听者
     */
    private SensorEventListener mDistanceSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float f_proximiny = event.values[0];
            //扬声器模式
            //魅蓝E传感器得到的值竟然比最大值都要大？what fuck ？
            if (f_proximiny >= mProximiny.getMaximumRange()) {
                if (mOnSpeakerListener != null) {
                    if (System.currentTimeMillis() - mLastSpeakerSwitchingTime >= 1000) {
                        mLastSpeakerSwitchingTime = System.currentTimeMillis();
                        mOnSpeakerListener.onSpeakerChanged(true);
                    }
                }
            } else {//听筒模式
                if (mOnSpeakerListener != null) {
                    if (System.currentTimeMillis() - mLastEarpieceSwitchingTime >= 1000) {
                        mLastEarpieceSwitchingTime = System.currentTimeMillis();
                        mOnSpeakerListener.onSpeakerChanged(false);
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private boolean speakerPhoneOn;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private boolean enableSensorSpeaker = true;
    // 只在页面前台时自动切换，
    private boolean foreground;

    public AudioModeManger() {

    }

    public void setOnSpeakerListener(onSpeakerListener listener) {
        mOnSpeakerListener = listener;
    }

    /**
     * 只在页面前台时开始监听，
     */
    public void onStart(boolean speakerPhoneOn) {
        foreground = true;
        setSpeakerPhoneOn(speakerPhoneOn);
    }

    /**
     * 页面后台时停止监听，
     */
    public void onStop() {
        foreground = false;
        // 恢复扬声器模式
        setSpeakerPhoneOn(true);
    }

    /**
     * 听筒、扬声器切换
     * <p>
     * 注释： 敬那些年踩过的坑和那些网上各种千奇百怪坑比方案！！
     * <p>
     * AudioManager设置声音类型有以下几种类型（调节音量用的是这个）:
     * <p>
     * STREAM_ALARM 警报
     * STREAM_MUSIC 音乐回放即媒体音量
     * STREAM_NOTIFICATION 窗口顶部状态栏Notification,
     * STREAM_RING 铃声
     * STREAM_SYSTEM 系统
     * STREAM_VOICE_CALL 通话
     * STREAM_DTMF 双音多频,不是很明白什么东西
     * <p>
     * ------------------------------------------
     * <p>
     * AudioManager设置声音模式有以下几个模式（切换听筒和扬声器时setMode用的是这个）
     * <p>
     * MODE_NORMAL 正常模式，即在没有铃音与电话的情况
     * MODE_RINGTONE 铃响模式
     * MODE_IN_CALL 通话模式 5.0以下
     * MODE_IN_COMMUNICATION 通话模式 5.0及其以上
     *
     * @param speakerPhoneOn
     */
    public void setSpeakerPhoneOn(boolean speakerPhoneOn) {
        setSpeakerPhoneOn(speakerPhoneOn, true);
    }

    public void setSpeakerPhoneOn(boolean speakerPhoneOn, boolean sensorCheck) {
        this.speakerPhoneOn = speakerPhoneOn;
        if (sensorCheck) {
            // 扬声器模式才启动距离判断切换听筒，
            enableSensorSpeaker = speakerPhoneOn;
        }
        if (isHeadsetExists()) {
            // 有耳机连接时不改变听筒扬声器，
            return;
        }
        audioManager.setSpeakerphoneOn(speakerPhoneOn);
        if (speakerPhoneOn) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            //设置音量，解决有些机型切换后没声音或者声音突然变大的问题
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), AudioManager.FX_KEY_CLICK);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {  //5.0及其以上
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
            }
            //设置音量，解决有些机型切换后没声音或者声音突然变大的问题
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.FX_KEY_CLICK);
        }
    }

    private boolean isHeadsetExists() {
        return audioManager.isWiredHeadsetOn() || audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn();
    }

    /**
     * 注册距离传感器监听
     */
    public void register(Context context) {
        audioManager = (AudioManager) context.getSystemService(android.app.Service.AUDIO_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(android.app.Service.SENSOR_SERVICE);
        ctx = context;
        if (sensorManager != null && mDistanceSensorListener != null) {
            mProximiny = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            sensorManager.registerListener(mDistanceSensorListener, mProximiny,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        enableSensor();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        context.registerReceiver(getHeadsetReceiver(), intentFilter);
        speakerPhoneOn = audioManager.isSpeakerphoneOn();
    }

    private HeadsetReceiver getHeadsetReceiver() {
        if (headsetReceiver != null) {
            return headsetReceiver;
        }
        synchronized (this) {
            if (headsetReceiver != null) {
                return headsetReceiver;
            }
            headsetReceiver = new HeadsetReceiver();
        }
        return headsetReceiver;
    }

    private void changeToSpeaker() {
        setSpeakerPhoneOn(speakerPhoneOn, false);
    }

    private void changeToHeadset() {
        setSpeakerPhoneOn(speakerPhoneOn, false);
    }

    private void enableSensor() {
        setOnSpeakerListener(isSpeakerOn -> {
            Log.d(TAG, "onSpeakerChanged() called with: isSpeakerOn = [" + isSpeakerOn + "]");
            if (isSpeakerOn) {
                setScreenOn();
            } else {
                setScreenOff();
            }
            if (enableSensorSpeaker && foreground) {
                setSpeakerPhoneOn(isSpeakerOn, false);
            }
        });
    }


    /**
     * 取消注册距离传感器监听
     */
    public void unregister() {
        if (sensorManager != null && mDistanceSensorListener != null) {
            sensorManager.unregisterListener(mDistanceSensorListener);
        }
        setOnSpeakerListener(null);
        ctx.unregisterReceiver(headsetReceiver);
    }

    private void setScreenOff() {
        if (!VoiceManager.instance().getMediaPlayer().isPlaying()) {
            // 只有语音播放时才靠近自动息屏，
            return;
        }
        if (wakeLock == null) {
            if (powerManager == null) {
                powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            }
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, ctx.getPackageName() + TAG);
        }
        wakeLock.acquire();
    }

    private void setScreenOn() {
        if (wakeLock != null) {
            wakeLock.setReferenceCounted(false);
            wakeLock.release();
            wakeLock = null;
        }
    }

    /**
     * 扬声器状态监听器
     * 如果要做成类似微信那种切换后重新播放音频的效果，需要这个监听回调
     * isSpeakerOn 扬声器是否打开
     */
    public interface onSpeakerListener {
        void onSpeakerChanged(boolean isSpeakerOn);
    }

    class HeadsetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_HEADSET_PLUG:
                    // 有线耳机监控，
                    int state = intent.getIntExtra("state", 0);
                    if (state == 1) {
                        changeToHeadset();
                    } else if (state == 0) {
                        changeToSpeaker();
                    }
                    break;
                case AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED:
                    // 无线耳机监控，
                    if (intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        changeToHeadset();
                    } else {
                        changeToSpeaker();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
