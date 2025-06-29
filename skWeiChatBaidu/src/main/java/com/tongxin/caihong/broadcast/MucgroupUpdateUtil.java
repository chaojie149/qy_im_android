package com.tongxin.caihong.broadcast;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.tongxin.caihong.AppConfig;

/**
 * 我的群组 && 全部群组
 * 全部直播 && 正在直播
 */
public class MucgroupUpdateUtil {

    public static final String ACTION_UPDATE = AppConfig.sPackageName + ".action.muc_group.update";// 群聊界面
    public static final String ACTION_ROLE_UPDATE = AppConfig.sPackageName + ".action.muc_group.role.update";// 更新群组页，

    public static IntentFilter getUpdateActionFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UPDATE);
        intentFilter.addAction(ACTION_ROLE_UPDATE);
        return intentFilter;
    }

    public static void broadcastUpdateUi(Context context) {
        context.sendBroadcast(new Intent(ACTION_UPDATE));
    }

    public static void broadcastUpdateRole(Context context) {
        context.sendBroadcast(new Intent(ACTION_ROLE_UPDATE));
    }
}
