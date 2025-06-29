package com.tongxin.caihong.ui.applet;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.AppletShare;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.CutoutHelper;
import com.tongxin.caihong.helper.PaySecureHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.view.AppletMoreDialog;
import com.tongxin.caihong.view.window.WindowUtil;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 小程序，
 */
public class AppletActivity extends BaseActivity implements ChatMessageListener, AppletMoreDialog.ActionClickListener {
    private String appletId;
    private String appUrl;
    private ShareAppInterface shareAppInterface;
    private WebView webView;
    private String forwardContent;
    private String forwardPackageId;

    public static void start(Context ctx, String appUrl) {
        Intent intent = new Intent(ctx, AppletActivity.class);
        intent.putExtra("appUrl", appUrl);
        ctx.startActivity(intent);
    }

    public static void startFromId(Context ctx, String appletId) {
        Intent intent = new Intent(ctx, AppletActivity.class);
        intent.putExtra("appletId", appletId);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applet);
        if (getIntent() != null) {
            appletId = getIntent().getStringExtra("appletId");
            appUrl = getIntent().getStringExtra("appUrl");
        }
        if (TextUtils.isEmpty(appUrl)) {
            Reporter.post("applet empty");
            ToastUtil.showErrorData(mContext);
            finish();
            return;
        }

        CutoutHelper.initCutoutHolderTop(getWindow(), findViewById(R.id.vCutoutHolder));
        findViewById(R.id.ivClose).setOnClickListener(v -> {
            finish();
        });
        findViewById(R.id.ivMenu).setOnClickListener(v -> {
            Dialog dialog = new AppletMoreDialog(mContext, AppletActivity.this);
            dialog.show();
        });
        webView = findViewById(R.id.webView);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings = webView.getSettings();
        // 设置与Js交互的权限
        webSettings.setJavaScriptEnabled(true);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSavePassword(false);
        webSettings.setAllowFileAccess(false);

        shareAppInterface = new ShareAppInterface(this, webView);
        webView.addJavascriptInterface(shareAppInterface, "skobj");

        webView.loadUrl(appUrl);

        getLifecycle().addObserver(new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            void create() {
                ListenerManager.getInstance().addChatMessageListener(AppletActivity.this);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void destroy() {
                ListenerManager.getInstance().removeChatMessageListener(AppletActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(forwardContent)) {
            webView.postDelayed(() -> {
                if (!TextUtils.isEmpty(forwardContent)) {
                    // 消息发送不论成功失败，真正发送前就会收到回调，所以延迟还没收到表示没有转发，
                    if (!TextUtils.isEmpty(forwardContent)) {
                        forwardContent = null;
                        shareAppInterface.onShareError("cancel");
                    }
                }
            }, 500);
        }
    }

    @Override
    protected void onDestroy() {
        WindowUtil.getInstance().onActivityDestroy(webView);
        super.onDestroy();
    }

    public void shareMessage(String gameId, String title, String imgUrl, String appName, String appInco) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_APPLET);
        chatMessage.setFromUserId(AppConstant.APPLET_INSTANT_ID);
        chatMessage.setFromUserName(AppConstant.APPLET_INSTANT_ID);
        AppletShare bean = new AppletShare();
        bean.setAppUrl(appUrl);
        bean.setAppIcon(appInco);
        bean.setTitle(title);
        bean.setImageUrl(imgUrl);
        bean.setAppName(appName);
        bean.setGameId(gameId);
        chatMessage.setContent(JSON.toJSONString(bean));
        String userid = coreManager.getSelf().getUserId();
        chatMessage.setToUserId(userid);
        chatMessage.setUpload(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        ChatMessageDao.getInstance().saveNewSingleChatMessage(userid, AppConstant.APPLET_INSTANT_ID, chatMessage);

        forwardContent = chatMessage.getContent();
        InstantMessageActivity.start(mContext, AppConstant.APPLET_INSTANT_ID, chatMessage.getPacketId(), false);
    }

    public void requestPayment(String gameId, String mode, String currencyType, String title, float money, int buyQuantity) {
        String sMoney = Money.fromYuan(String.valueOf(money));
        AtomicBoolean success = new AtomicBoolean(false);
        Dialog dialog = PaySecureHelper.inputPayPassword(mContext, title, sMoney, s -> {
            success.set(true);
            shareAppInterface.onPaySuccess(s);
        });
        dialog.setOnDismissListener(dialog1 -> {
            webView.postDelayed(() -> {
                if (!success.get()) {
                    shareAppInterface.onPayError("dismiss");
                }
            }, 100);
        });
    }

    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {
        if (messageState == MESSAGE_SEND_ING) {
            return;
        }
        if (!TextUtils.equals(forwardPackageId, msgId)) {
            return;
        }
        if (messageState == MESSAGE_SEND_SUCCESS) {
            shareAppInterface.onShareSuccess(msgId);
        } else if (messageState == MESSAGE_SEND_FAILED) {
            shareAppInterface.onShareError(msgId);
        }
        forwardPackageId = null;
    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        if (message.getType() != XmppMessage.TYPE_APPLET) {
            return false;
        }
        if (!TextUtils.equals(forwardContent, message.getContent())) {
            return false;
        }
        // 确实转发了，等待转发结果，
        forwardContent = null;
        forwardPackageId = message.getPacketId();
        return false;
    }

    @Override
    public void sendToFriend() {
        ToastUtil.showToast(mContext, R.string.tip_coming_soon);
    }

    @Override
    public void addToMyApplet() {
        ToastUtil.showToast(mContext, R.string.tip_coming_soon);

    }

    @Override
    public void complaint() {
        ToastUtil.showToast(mContext, R.string.tip_coming_soon);

    }
}
