package com.tongxin.caihong.ui.notification;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.pay.chat.PayActivity;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.SplashActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.xiaomi.mipush.sdk.MiPushMessage;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * 通知的点击事件统一跳到这个页面处理，
 */
public class NotificationProxyActivity extends BaseActivity {
    private static final String TAG = "NotificationProxy";
    private boolean isNeedExecuteLogin;

    public NotificationProxyActivity() {
        noLoginRequired();
    }

    public static void start(Context ctx, Map<String, String> data) {
        Intent intent = new Intent(ctx, NotificationProxyActivity.class);
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        intent.putExtras(bundle);
        // 可能是点推送直接进来的，context可能是service，
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public static boolean processIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (intent.getData() == null) {
            // 针对小米推送点击没有打开指定activity情况的兼容处理，
            // 已经查明是服务器推送问题漏配置了notify_effect，但兼容代码保留，
            Serializable key_message = intent.getSerializableExtra("key_message");
            if (key_message instanceof MiPushMessage) {
                String intent_uri = ((MiPushMessage) key_message).getExtra().get("intent_uri");
                if (!TextUtils.isEmpty(intent_uri)) {
                    try {
                        Intent realIntent = Intent.parseUri(intent_uri, 0);
                        return !TextUtils.isEmpty(realIntent.getStringExtra("userId"))
                                || !TextUtils.isEmpty(realIntent.getStringExtra("roomJid"))
                                || !TextUtils.isEmpty(realIntent.getStringExtra("url"));
                    } catch (URISyntaxException e) {
                        Reporter.post("unknown intent: " + intent_uri, e);
                    }
                }
            }
        }
        return !TextUtils.isEmpty(intent.getStringExtra("userId"))
                || !TextUtils.isEmpty(intent.getStringExtra("roomJid"))
                || !TextUtils.isEmpty(intent.getStringExtra("url"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_proxy);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Intent intent = getIntent();
        LogUtils.log(TAG, intent);

        // 判断本地登录状态
        int userStatus = LoginHelper.prepareUser(mContext, coreManager);
        switch (userStatus) {
            case LoginHelper.STATUS_USER_FULL:
            case LoginHelper.STATUS_USER_NO_UPDATE:
            case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
                boolean isConflict = PreferenceUtils.getBoolean(this, Constants.LOGIN_CONFLICT, false);
                if (isConflict) {
                    isNeedExecuteLogin = true;
                }
                break;
            case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                isNeedExecuteLogin = true;
                break;
            case LoginHelper.STATUS_NO_USER:
            default:
                isNeedExecuteLogin = true;
        }

        if (isNeedExecuteLogin) {// 需要先执行登录操作
            startActivity(new Intent(mContext, SplashActivity.class));
            finish();
            return;
        }

        MyApplication.IS_SUPPORT_SECURE_CHAT = coreManager.getConfig().isSupportSecureChat == 1;
        MainActivity.start(this);

        if (intent.getData() != null) {
            try {
                Uri data = intent.getData();
                for (String key : data.getQueryParameterNames()) {
                    String value = data.getQueryParameter(key);
                    // 参数统一存在intent.extras里，有的推送不支持，所以要提前处理一下，
                    intent.putExtra(key, value);
                }
            } catch (Exception e) {
                Reporter.post("通知点击intent.data解析失败", e);
            }
        } else {
            // 针对小米推送点击没有打开指定activity情况的兼容处理，
            // 已经查明是服务器推送问题漏配置了notify_effect，但兼容代码保留，
            Serializable key_message = intent.getSerializableExtra("key_message");
            if (key_message instanceof MiPushMessage) {
                String intent_uri = ((MiPushMessage) key_message).getExtra().get("intent_uri");
                if (!TextUtils.isEmpty(intent_uri)) {
                    try {
                        Intent realIntent = Intent.parseUri(intent_uri, 0);
                        if (realIntent.getExtras() != null) {
                            intent.putExtras(realIntent.getExtras());
                        }
                    } catch (URISyntaxException e) {
                        Reporter.post("unknown intent: " + intent_uri, e);
                    }
                }
            }
        }

        String userId = intent.getStringExtra("userId");
        String roomJid = intent.getStringExtra("roomJid");
        String url = intent.getStringExtra("url");
        Log.i(TAG, "args: " + "userId=" + userId + "roomJid=" + roomJid + ", url=" + url);

        if (!TextUtils.isEmpty(userId)) {
            AsyncUtils.doAsync(this, t -> {
                Reporter.post("解析通知点击参数失败， intent=" + intent.toUri(Intent.URI_INTENT_SCHEME));
                runOnUiThread(this::finish);
            }, c -> {
                Friend friend;
                if (!TextUtils.isEmpty(roomJid)) {
                    friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), roomJid);
                } else {
                    friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), userId);
                }
                c.uiThread(r -> {
                    if (friend == null) {
                        Reporter.post("朋友不存在， userId=" + userId);
                    } else if (friend.getRoomFlag() == 1) {
                        MucChatActivity.start(r, friend);
                    } else {
                        if (TextUtils.equals(friend.getUserId(), Friend.ID_SK_PAY)) {
                            r.startActivity(new Intent(r, PayActivity.class));
                        } else {
                            ChatActivity.start(r, friend);
                        }
                    }
                    r.finish();
                });
            });
        } else if (!TextUtils.isEmpty(url)) {
            try {
                Uri uri = Uri.parse(url);
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception e) {
                // 无论如何不能崩溃，
                Reporter.post("打开浏览器失败", e);
                ToastUtil.showToast(this, getString(R.string.tip_notification_open_url_failed));
            }
            finish();
        } else {
            Reporter.unreachable();
            finish();
        }

    }
}
