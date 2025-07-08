package com.tongxin.caihong.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;
import com.androidyuan.lib.screenshot.Shooter;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.OrderInfo;
import com.tongxin.caihong.bean.WebCallback;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ShareSdkHelper;
import com.tongxin.caihong.ui.account.AuthorDialog;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.circle.range.SendShuoshuoActivity;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.ui.tool.HtmlFactory;
import com.tongxin.caihong.ui.tool.JsSdkInterface;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.JsonUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.ComplaintDialog;
import com.tongxin.caihong.view.ExternalOpenDialog;
import com.tongxin.caihong.view.MatchKeyWordEditDialog;
import com.tongxin.caihong.view.ModifyFontSizeDialog;
import com.tongxin.caihong.view.PayDialog;
import com.tongxin.caihong.view.WebMoreDialog;
import com.tongxin.caihong.view.window.WindowUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.imeina.screeencaptureshare.ScreenCaptureShareActivity;
import okhttp3.Call;
import okhttp3.HttpUrl;

/**
 * web
 */
public class WebViewFragment extends EasyFragment {
    private static final String TAG = "WebViewFragment";
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    private TextView mTitleTv;
    private ImageView mTitleRightIv;
    private ProgressBar mProgressBar;
    private WebView mWebView;
    private boolean isAnimStart = false;
    private int currentProgress;
    private String mUrl; // 网址URL
    private String mDownloadUrl;// ShareSdk 分享链接进来的应用下载地址(跳转，当本地不存在对应应用时使用)
    private JsSdkInterface jsSdkInterface;
    // js sdk设置的分享数据，
    private String shareBeanContent;
    private ValueCallback<Uri[]> uploadMessageAboveL;

    @Override
    protected int inflateLayoutId() {
        return R.layout.activity_web_view;
    }

    interface CloseEvent {
        void close();
    }

    private CloseEvent closeEvent;

    public void setCloseEvent(CloseEvent mCloseEvent) {
        closeEvent = mCloseEvent;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        mUrl = coreManager.getConfig().homeAddress.getHomeUrl();
        initActionBar();
        init();
    }

    @Override
    public void onDestroy() {
        if (jsSdkInterface != null) {
            jsSdkInterface.release();
        }
        WindowUtil.getInstance().onActivityDestroy(mWebView);
        super.onDestroy();
    }

    private void initActionBar() {
        findViewById(R.id.iv_title_left).setOnClickListener(v -> {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                if (closeEvent != null) {
                    closeEvent.close();
                }
            }
        });
        findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleRightIv = findViewById(R.id.iv_title_right);
        mTitleRightIv.setImageResource(R.drawable.chat_more);
        mTitleRightIv.setOnClickListener(this);
    }

    private void init() {
        initView();
        initClient();
        initEvent();
        UiUtils.supportChangeRootHeightAdaptationKeyboard(requireActivity(), findViewById(R.id.ll));
        int openStatus = openApp(mUrl);
        if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
            requireActivity().finish();
        }
        if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应用，加载该应用下载地址
            load(mWebView, mDownloadUrl);
        } else if (openStatus == 5) {// 该链接为跳转链接，跳转到本地授权
        } else {// 0 | 3 | 4
            load(mWebView, mUrl);
        }
    }

    private void initView() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mWebView = (WebView) findViewById(R.id.mWebView);
        /* 设置支持Js */
        mWebView.getSettings().setJavaScriptEnabled(true);
        /* 设置为true表示支持使用js打开新的窗口 */
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        /* 设置缓存模式 */
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setDomStorageEnabled(true);

        /* 设置为使用webview推荐的窗口 */
        mWebView.getSettings().setUseWideViewPort(true);
        /* 设置为使用屏幕自适配 */
        mWebView.getSettings().setLoadWithOverviewMode(true);
        /* 设置是否允许webview使用缩放的功能,我这里设为false,不允许 */
        mWebView.getSettings().setBuiltInZoomControls(false);
        /* 提高网页渲染的优先级 */
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        /* HTML5的地理位置服务,设置为true,启用地理定位 */
        mWebView.getSettings().setGeolocationEnabled(true);
        /* 设置可以访问文件 */
        mWebView.getSettings().setAllowFileAccess(true);

        // 网页播放视频有画面没声音<http://web.meiyanchat.com/aa.html>添加如下代码
        // 自动播放网页音乐
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // 授予摄像头/麦克风权限
                request.grant(request.getResources());
            }
        });

        // 设置UserAgent标识
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString() + " app-chatimapp");
        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setAllowFileAccess(false);
    }

    private void initClient() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setAlpha(1.0f);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.e(TAG, "shouldOverrideUrlLoading: " + url);
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                int openStatus = openApp(url);
                if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
                    return true;
                } else if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应用，加载该应用下载地址
                    load(view, mDownloadUrl);
                } else if (openStatus == 5) {// 该链接为跳转链接， 该链接为跳转链接，跳转到本地授权

                } else { // 0 | 3 | 4
//                    load(view, url);
                    return false;
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        // 获取网页加载进度
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                currentProgress = mProgressBar.getProgress();
                if (newProgress >= 100 && !isAnimStart) {
                    // 防止调用多次动画
                    isAnimStart = true;
                    mProgressBar.setProgress(newProgress);
                    // 开启属性动画让进度条平滑消失
                    startDismissAnimation(mProgressBar.getProgress());
                    updateBack();
                } else {
                    // 开启属性动画让进度条平滑递增
                    startProgressAnimation(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                mTitleTv.setText(title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_RESULT_CODE);
                return true;
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                // 不处理下载，直接抛出去，
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {
                // 无论如何不要崩溃，比如没有浏览器，
                ToastUtil.showToast(requireActivity(), R.string.download_error);
            }
        });

        jsSdkInterface = new JsSdkInterface(requireContext(), new MyJsSdkListener());
        mWebView.addJavascriptInterface(jsSdkInterface, "AndroidWebView");
    }

    private void updateBack() {
        if (mWebView != null && mWebView.canGoBack()) {
            findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) { //处理返回的图片，并进行上传
            if (null == uploadMessageAboveL) return;
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            }
        }

        switch (requestCode) {

            case REQ_CODE_ACT: {
                if (resultCode == getActivity().RESULT_OK && data != null) {
                    toast("Screenshot saved at " + data.getData().toString());
                }
                else{
                    toast("You got wrong.");
                }
            }
            break;
            case REQ_CODE_PER: {
                if (resultCode == getActivity().RESULT_OK && data != null) {
                    Shooter shooter = new Shooter(getActivity(), resultCode, data);
                    shooter.startScreenShot(getSavedPath(), new Shooter.OnShotListener() {
                                @Override
                                public void onFinish(String path) {
                                    //here is done status.
//                                    toast("Screenshot saved at " + path);
                                    ScreenCaptureShareActivity.screenCapture(mWebView, path);
                                }

                                @Override
                                public void onError() {
                                    toast("You got wrong.");
                                }
                            }
                    );
                } else if (resultCode == getActivity().RESULT_CANCELED) {
                    //user canceled.
                } else {

                }
            }
        }
    }
    private String getSavedPath() {
        return getActivity().getExternalFilesDir("screenshot").getAbsoluteFile() + "/"
                + SystemClock.currentThreadTimeMillis() + ".png";
    }



    private void toast(String str) {
        Toast.makeText(getActivity(), str, Toast.LENGTH_LONG).show();
    }

    private static final int REQ_CODE_PER = 0x2304;
    private static final int REQ_CODE_ACT = 0x2305;
    /**
     * This is an example for using Shooter.
     * This method will request permission and take screenshot on this Activity.
     */
    public void onClickReqPermission() {
        if (Build.VERSION.SDK_INT >= 21) {
            startActivityForResult(createScreenCaptureIntent(), REQ_CODE_PER);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Intent createScreenCaptureIntent() {
        //Here using media_projection instead of Context.MEDIA_PROJECTION_SERVICE to  make it successfully build on low api.
        return ((MediaProjectionManager) getActivity().getSystemService("media_projection")).createScreenCaptureIntent();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

    private void initEvent() {
        mTitleRightIv.setOnClickListener(view -> {
            WebMoreDialog mWebMoreDialog = new WebMoreDialog(requireActivity(), getCurrentUrl(), true, new WebMoreDialog.BrowserActionClickListener() {
                @Override
                public void floatingWindow() {
                    // 首页浮窗功能不可用，ui已隐藏
                }

                @Override
                public void sendToFriend() {
                    forwardToFriend();
                }

                @Override
                public void shareToLifeCircle() {
                    shareMoment();
                }

                @Override
                public void collection() {
                    selectShareImage(true);
                }

                @Override
                public void searchContent() {
                    search();
                }

                @Override
                public void copyLink() {
                    ClipboardManager clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(getCurrentUrl());
                    Toast.makeText(requireActivity(), getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void openOutSide() {
                    ExternalOpenDialog externalOpenDialog = new ExternalOpenDialog(requireActivity(), getCurrentUrl());
                    externalOpenDialog.show();
                }

                @Override
                public void modifyFontSize() {
                    setWebFontSiz();
                }

                @Override
                public void refresh() {
                    mWebView.reload();
                }

                @Override
                public void complaint() {
                    report();
                }

                @Override
                public void shareWechat() {
                    String title = mTitleTv.getText().toString().trim();
                    String url = getCurrentUrl();
                    ShareSdkHelper.shareWechat(
                            requireActivity(), title, url, url
                    );
                }

                @Override
                public void shareWechatMoments() {
                    String title = mTitleTv.getText().toString().trim();
                    String url = getCurrentUrl();
                    ShareSdkHelper.shareWechatMoments(
                            requireActivity(), title, url, url
                    );
                }

                @Override
                public void edit() {
                    WebViewFragment.this.onClickReqPermission();
                }
            });
            mWebMoreDialog.show();
        });
    }

    /**
     * 根据url跳转至其他app
     */
    private int openApp(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        try {
            // 内部授权
            //  http://192.168.0.141:8080/websiteAuthorh/appAuth.html?appId=sk7c4fd05f92c7460a&callbackUrl=http://192.168.0.141:8080/websiteAuthorh/test.html
            if (url.contains("websiteAuthorh/index.html")) {
                String webAppName = WebViewActivity.URLRequest(url).get("webAppName");
                String webAppsmallImg = WebViewActivity.URLRequest(url).get("webAppsmallImg");
                String appId = WebViewActivity.URLRequest(url).get("appId");
                String redirectURL = WebViewActivity.URLRequest(url).get("callbackUrl");

                Log.e(TAG, "openApp: " + webAppName + "," + webAppsmallImg + "," + url);
                AuthorDialog dialog = new AuthorDialog(requireActivity());
                dialog.setDialogData(webAppName, webAppsmallImg);
                dialog.setmConfirmOnClickListener(new AuthorDialog.ConfirmOnClickListener() {
                    @Override
                    public void confirm() {
                        HttpUtils.get().url(coreManager.getConfig().AUTHOR_CHECK)
                                .params("appId", appId)
                                .params("state", coreManager.getSelfStatus().accessToken)
                                .params("callbackUrl", redirectURL)
                                .build().execute(new BaseCallback<WebCallback>(WebCallback.class) {

                                    @Override
                                    public void onResponse(ObjectResult<WebCallback> result) {
                                        if (Result.checkSuccess(requireContext(), result) && result.getData() != null) {
                                            String html = HttpUrl.parse(result.getData().getCallbackUrl()).newBuilder()
                                                    .addQueryParameter("code", result.getData().getCode())
                                                    .build()
                                                    .toString();
                                            load(mWebView, html);
                                        }
                                    }

                                    @Override
                                    public void onError(Call call, Exception e) {

                                    }
                                });
                    }

                    @Override
                    public void AuthorCancel() {

                    }
                });

                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                return 5;
            }

            if (!url.startsWith("http") && !url.startsWith("https") && !url.startsWith("ftp")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                // host 和 scheme 都不能为null
                if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(scheme)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (AppUtils.isSupportIntent(requireActivity(), intent)) {
                        startActivity(intent);
                        return 1;
                    } else {
                        return 2;
                    }
                }
            }
        } catch (Exception e) {
            return 3;
        }
        return 4;
    }

    private void load(WebView view, String url) {
        view.loadUrl(url);
    }

    /****************************************************
     * Start
     ***************************************************/
    private String getCurrentUrl() {
        if (mWebView == null) {
            // 至少不能崩溃，初始化前有http请求，可能耗时，期间其他代码调用该方法不能崩溃，
            return "";
        }
        Log.e(TAG, mWebView.getUrl());
        String currentUrl = mWebView.getUrl();
        if (TextUtils.isEmpty(currentUrl)) {
            currentUrl = mUrl;
        }

        if (currentUrl.contains("https://view.officeapps.live.com/op/view.aspx?src=")) {
            currentUrl = currentUrl.replace("https://view.officeapps.live.com/op/view.aspx?src=", "");
        }

        return currentUrl;
    }

    /**
     * 发送给朋友
     */
    private void initChatByUrl(String url, boolean isCollection) {
        String title = mTitleTv.getText().toString().trim();
        String content = JsonUtils.initJsonContent(title, getCurrentUrl(), url);
        if (isCollection) {
            onCollection(content);
            return;
        }
        initChatByContent(content, XmppMessage.TYPE_LINK);
    }

    private void initChatByContent(String content, int type) {
        String mLoginUserId = coreManager.getSelf().getUserId();

        ChatMessage message = new ChatMessage();
        message.setType(type);
        if (type == XmppMessage.TYPE_LINK) {
            message.setContent(content);
        } else if (type == XmppMessage.TYPE_SHARE_LINK) {
            message.setObjectId(content);
        } else {
            throw new IllegalStateException("未知类型: " + type);
        }
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        // Todo 将封装好的消息存入10010 号的msg 表内，在跳转至转发->聊天界面(跳转传值均为10010号与msgId)，之后在聊天界面内通过这两个值查询到对用消息，发送
        String mNewUserId = "10010";
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mNewUserId, message)) {
            InstantMessageActivity.start(requireActivity(), AppConstant.NORMAL_INSTANT_ID, message.getPacketId(), false);
        } else {
            Toast.makeText(requireContext(), R.string.tip_message_wrap_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void forwardToFriend() {
        if (shareBeanContent != null) {
            initChatByContent(shareBeanContent, XmppMessage.TYPE_SHARE_LINK);
        } else {
            selectShareImage(false);
        }
    }

    private void selectShareImage(boolean isCollection) {
        String str = mWebView.getUrl();
        if (TextUtils.isEmpty(str)) {
            str = getCurrentUrl();
        }
        HtmlFactory.instance().queryImage(str, new HtmlFactory.DataListener<String>() {// 检索该网页包含的图片

            @Override
            public void onResponse(List<String> data, String title) {
                if (data != null && data.size() > 0) {
                    String url = "";
                    for (int i = 0; i < data.size(); i++) {
                        if (!TextUtils.isEmpty(data.get(i))) {
                            url = data.get(i);
                            break;
                        }
                    }
                    initChatByUrl(url, isCollection);
                } else {
                    initChatByUrl("", isCollection);
                }
            }

            @Override
            public void onError(String error) {
                initChatByUrl("", isCollection);
            }
        });
    }

    /**
     * 分享到生活圈
     */
    private void shareMoment() {
        Intent intent = new Intent(requireContext(), SendShuoshuoActivity.class);
        intent.putExtra(Constants.BROWSER_SHARE_MOMENTS_CONTENT, getCurrentUrl());
        if (mWebView != null && !TextUtils.isEmpty(mWebView.getTitle())) {
            intent.putExtra(Constants.BROWSER_SHARE_MOMENTS_SUBJECT, mWebView.getTitle());
        }
        startActivity(intent);
    }

    /**
     * 收藏
     * 链接 当做 文本类型 收藏
     */
    private String collectionParam(String content) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = CollectionEvery.TYPE_LINK;
        json.put("type", String.valueOf(type));
        String msg = "";
        String collectContent = "";
        msg = content;
        collectContent = content;
        json.put("msg", msg);
        // json.put("collectContent", collectContent);
        json.put("collectType", -1);// 与消息无关的收藏
        array.add(json);
        return JSON.toJSONString(array);
    }

    private void onCollection(final String content) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emoji", collectionParam(content));

        HttpUtils.get().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(requireContext(), getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(requireContext(), result.getResultMsg());
                        } else {
                            ToastUtil.showToast(requireContext(), R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(requireContext());
                    }
                });
    }

    /**
     * 搜索页面内容
     */
    private void search() {
        MatchKeyWordEditDialog matchKeyWordEditDialog = new MatchKeyWordEditDialog(requireContext(), mWebView);
        Window window = matchKeyWordEditDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
            matchKeyWordEditDialog.show();
        }
    }

    /**
     * 调整字体
     */
    private void setWebFontSiz() {
        ModifyFontSizeDialog modifyFontSizeDialog = new ModifyFontSizeDialog(requireContext(), mWebView);
        modifyFontSizeDialog.show();
    }

    /**
     * 投诉
     */
    private void report() {
        ComplaintDialog complaintDialog = new ComplaintDialog(requireActivity(), report -> {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("webUrl", getCurrentUrl());
            params.put("reportType", String.valueOf(3));
            params.put("reason", String.valueOf(report.getReportId()));
            DialogHelper.showDefaulteMessageProgressDialog(requireContext());

            HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(requireActivity(), R.string.report_success);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        });
        complaintDialog.show();
    }

    /****************************************************
     * End
     ***************************************************/

    /**
     * progressBar递增动画
     */
    private void startProgressAnimation(int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", currentProgress, newProgress);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /**
     * progressBar消失动画
     */
    private void startDismissAnimation(final int progress) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mProgressBar, "alpha", 1.0f, 0.0f);
        anim.setDuration(1500);  // 动画时长
        anim.setInterpolator(new DecelerateInterpolator());
        // 关键, 添加动画进度监听器
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fraction = valueAnimator.getAnimatedFraction();      // 0.0f ~ 1.0f
                int offset = 100 - progress;
                mProgressBar.setProgress((int) (progress + offset * fraction));
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                // 动画结束
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.GONE);
                isAnimStart = false;
            }
        });
        anim.start();
    }

    private class MyJsSdkListener implements JsSdkInterface.Listener {

        @Override
        public void onFinishPlay(String path) {
            mWebView.evaluateJavascript("playFinish()", value -> {
            });
        }

        @Override
        public void onUpdateShareData(String shareBeanContent) {
            WebViewFragment.this.shareBeanContent = shareBeanContent;
        }

        @Override
        public void onChooseSKPayInApp(String appId, String prepayId, String sign) {
            DialogHelper.showDefaulteMessageProgressDialog(requireContext());
            Map<String, String> params = new HashMap<String, String>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("appId", appId);
            params.put("prepayId", prepayId);
            params.put("sign", sign);

            // 获取订单信息
            HttpUtils.get().url(coreManager.getConfig().PAY_GET_ORDER_INFO)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<OrderInfo>(OrderInfo.class) {

                        @Override
                        public void onResponse(ObjectResult<OrderInfo> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1 && result.getData() != null) {
                                PayDialog payDialog = new PayDialog(requireContext(), appId, prepayId, sign, result.getData(), new PayDialog.PayResultListener() {
                                    @Override
                                    public void payResult(String result) {
                                        mWebView.loadUrl("javascript:sk.paySuccess(" + result + ")");
                                    }
                                });
                                payDialog.show();
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        }

    }
}
