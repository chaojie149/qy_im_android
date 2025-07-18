package com.tongxin.caihong.broadcast;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.tongxin.caihong.ui.base.ActivityStack;
import com.tongxin.caihong.ui.base.BaseActivity;

public class ShakeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), OtherBroadcast.ACTION_SHAKE_MSG_NOTIFY)) {
            if (ActivityStack.getInstance().size() == 0) {
                // 以防万一避免崩溃，
                return;
            }
            Activity activity = ActivityStack.getInstance().getActivity(ActivityStack.getInstance().size() - 1);
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).shake(intent.getIntExtra(OtherBroadcast.ACTION_SHAKE_MSG_NOTIFY, 1));
            }
        }
    }
}
