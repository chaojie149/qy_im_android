package com.tongxin.caihong.call;


import com.tongxin.caihong.AppConfig;

/**
 * Created by Administrator on 2018/1/22 0022.
 */

public class CallConstants {
    public static final String AUDIO_OR_VIDEO_OR_MEET = "Audio_Or_Video_Or_Meet";
    // 刷新、关闭悬浮窗
    public static final String REFRESH_FLOATING = AppConfig.sPackageName + "Refresh_Floating";
    public static final String CLOSE_FLOATING = AppConfig.sPackageName + "Close_Floating";
    // 通话、会议
    public static final int Audio = 1;
    public static final int Video = 2;
    public static final int Audio_Meet = 3;
    public static final int Video_Meet = 4;
    public static final int Talk_Meet = 6;
    public static final int Screen_Call = 7;//屏幕共享
}
