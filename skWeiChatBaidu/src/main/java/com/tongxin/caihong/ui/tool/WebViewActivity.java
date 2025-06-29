package com.tongxin.caihong.ui.tool;

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
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
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
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.range.SendShuoshuoActivity;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.JsonUtils;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.ComplaintDialog;
import com.tongxin.caihong.view.ExternalOpenDialog;
import com.tongxin.caihong.view.MatchKeyWordEditDialog;
import com.tongxin.caihong.view.ModifyFontSizeDialog;
import com.tongxin.caihong.view.PayDialog;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.WebMoreDialog;
import com.tongxin.caihong.view.window.WindowUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.HttpUrl;

/**
 * web
 */
public class WebViewActivity extends BaseActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_DOWNLOAD_URL = "download_url";
    private static final String TAG = "WebViewActivity";
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    public boolean isFloating;
    boolean isReported;
    private TextView mTitleTv;
    private ImageView mTitleLeftIv;
    private ImageView mTitleRightIv;
    private ProgressBar mProgressBar;
    private WebView mWebView;
    private boolean isAnimStart = false;
    private int currentProgress;
    private String mUrl; // 网址URL
    private String title; // 固定标题，为空就不固定，
    private String mDownloadUrl;// ShareSdk 分享链接进来的应用下载地址(跳转，当本地不存在对应应用时使用)
    private JsSdkInterface jsSdkInterface;
    // js sdk设置的分享数据，
    private String shareBeanContent;
    // 群助手 交互的数据
    private String mShareParams;
    private FrameLayout frameLayout;
    private ViewGroup flWebView;
    private String floatingKey; // 从悬浮窗进来的获取缓存的webView用，
    private ValueCallback<Uri[]> uploadMessageAboveL;

    public static void start(Context ctx, String url) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, String url, String shareParams) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra("shareParams", shareParams);
        ctx.startActivity(intent);
    }

    public static void startWithTitle(Context ctx, String url, String title) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra("title", title);
        ctx.startActivity(intent);
    }

    public static void startFromFloating(Context ctx, String key) {
        Intent intent = new Intent(ctx, WebViewActivity.class);
        intent.putExtra("floatingKey", key);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     *
     * @param URL url地址
     * @return url请求参数部分
     */
    public static Map<String, String> URLRequest(String URL) {
        Map<String, String> mapRequest = new HashMap<String, String>();

        String[] arrSplit = null;

        String strUrlParam = TruncateUrlPage(URL);
        if (strUrlParam == null) {
            return mapRequest;
        }
        //每个键值为一组 www.2cto.com
        arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = null;
            arrSplitEqual = strSplit.split("[=]");

            //解析出键值
            if (arrSplitEqual.length > 1) {
                //正确解析
                try {
                    arrSplitEqual[1] = URLDecoder.decode(arrSplitEqual[1], "UTF-8");
                } catch (Exception ignored) {
                }
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);

            } else {
                if (arrSplitEqual[0] != "") {
                    //只有参数没有值，不加入
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        Log.d(TAG, "URLRequest: " + mapRequest.get("webAppName"));
        return mapRequest;
    }

    /**
     * 去掉url中的路径，留下请求参数部分
     *
     * @param strURL url地址
     * @return url请求参数部分
     */
    private static String TruncateUrlPage(String strURL) {
        String strAllParam = null;
        String[] arrSplit = null;

        strURL = strURL.trim();

        arrSplit = strURL.split("[?]");
        if (strURL.length() > 1) {
            if (arrSplit.length > 1) {
                if (arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            }
        }

        return strAllParam;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_support_cache);
        if (getIntent() != null) {
            mUrl = getIntent().getStringExtra(EXTRA_URL);
            title = getIntent().getStringExtra("title");
            floatingKey = getIntent().getStringExtra("floatingKey");
            mDownloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);
            mShareParams = getIntent().getStringExtra("shareParams");
        }
        initActionBar();
        if (TextUtils.isEmpty(floatingKey)) {
            initCheck();
        } else {
            loadCacheWebView();
        }
    }

    private void loadCacheWebView() {
        isFloating = true;
        mWebView = WindowUtil.getInstance().pop(mContext, floatingKey);
        if (mWebView == null) {
            ToastUtil.showErrorData(mContext);
            finish();
        } else {
            mUrl = mWebView.getUrl();
            // 读取缓存的固定标题，
            title = (String) mWebView.getTag(R.id.tag_web_view_title);
            updateTitle();
            init();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String newFloatingKey = intent.getStringExtra("floatingKey");
        if (TextUtils.isEmpty(newFloatingKey)) {
            return;
        }
        // 不管当前是否是悬浮窗进来的都要删除当前webView,
        if (flWebView != null) {
            flWebView.removeView(mWebView);
        }
        // 保存当前网页，如果是悬浮窗进来的，
        if (isFloating) {
            WindowUtil.getInstance().push(mContext, this.floatingKey, mWebView);
        }
        this.floatingKey = newFloatingKey;
        loadCacheWebView();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFloating) {
            WindowUtil.getInstance().pop(mContext, floatingKey);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //点击悬浮窗进来之后是不会有弹窗的，返回之后才有，这里判断一下进来的场景
        if (isFloating) {
            WindowUtil.getInstance().push(mContext, floatingKey, mWebView);
        }
    }

    @Override
    protected void onDestroy() {
        if (jsSdkInterface != null) {
            jsSdkInterface.release();
        }
        if (flWebView != null) {
            flWebView.removeView(mWebView);
        }
        WindowUtil.getInstance().onActivityDestroy(mWebView);
        super.onDestroy();
    }

    private void initCheck() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("webUrl", mUrl);
        init();

//        HttpUtils.get().url(coreManager.getConfig().URL_CHECK)
//                .params(params)
//                .build(true, true)
//                .execute(new BaseCallback<String>(String.class) {
//                    @Override
//                    public void onResponse(ObjectResult<String> result) {
//                        if (isFinishing()) {
//                            return;
//                        }
//                        int urlType = 1; // 兼容旧版，默认不提示，
//                        try {
//                            urlType = JSON.parseObject(result.getData()).getInteger("urlType");
//                        } catch (Exception ignored) {
//                        }
//                        if (result.getResultCode() != 1 || urlType == -1) {// 网址被多次举报，将网址改为警告地址，不让访问
//                            isReported = true;
//                        } else if (urlType == 0) {
//                            SelectionFrame selectionFrame = new SelectionFrame(mContext);
//                            selectionFrame.setSomething(getString(R.string.web_check_title), getString(R.string.web_check_alert_place_holder, mUrl),
//                                    getString(R.string.cancel), getString(R.string.continue_browser),
//                                    new SelectionFrame.OnSelectionFrameClickListener() {
//                                        @Override
//                                        public void cancelClick() {
//                                            finish();
//                                        }
//
//                                        @Override
//                                        public void confirmClick() {
//                                            init();
//                                        }
//                                    });
//                            selectionFrame.show();
//                            return;
//                        }
//                        init();
//                    }
//
//                    @Override
//                    public void onError(Call call, Exception e) {
//                        init();
//                    }
//                });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleLeftIv = findViewById(R.id.iv_title_left);
        mTitleLeftIv.setImageResource(R.drawable.icon_close);
        mTitleRightIv = findViewById(R.id.iv_title_right);
        mTitleRightIv.setImageResource(R.drawable.chat_more);
    }

    private void init() {
        initView();
        initClient();
        initEvent();
        UiUtils.supportChangeRootHeightAdaptationKeyboard(this, findViewById(R.id.ll));
        if (isReported) {// 加载警告网址
            mWebView.loadUrl("file:////android_asset/prohibit.html");
            mTitleRightIv.setVisibility(View.GONE);
        } else {
            if (isFloating) {
                // 缓存的webView不用再次页面，
                return;
            }
            int openStatus = openApp(mUrl);
            if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
                finish();
            } else if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应用，加载该应用下载地址
                load(mWebView, mDownloadUrl);
            } else if (openStatus == 5) {// 该链接为跳转链接，跳转到本地授权

            } else {// 0 | 3 | 4
                load(mWebView, mUrl);
            }
        }
    }

    private void initView() {
        frameLayout = findViewById(R.id.fl);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        flWebView = findViewById(R.id.flWebView);
        if (mWebView != null) {
            // 从悬浮窗进来的，就已经获取了缓存的webView，
        } else {
            // 不能用activity的context避免影响activity销毁，
            mWebView = new WebView(getApplicationContext());
        }
        mWebView.setId(R.id.mWebView);
        mWebView.setVerticalScrollBarEnabled(false);
        flWebView.addView(mWebView);
        /* 设置支持Js */
        mWebView.getSettings().setJavaScriptEnabled(true);
        /* 设置为true表示支持使用js打开新的窗口 */
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        /* 设置缓存模式 */
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        //webView  加载视频
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        /* 设置为使用webview推荐的窗口 */
        mWebView.getSettings().setUseWideViewPort(true);
        /* 设置为使用屏幕自适配 */
        mWebView.getSettings().setLoadWithOverviewMode(true);
        /* 设置是否允许webview使用缩放的功能,我这里设为false,不允许 */
        mWebView.getSettings().setBuiltInZoomControls(false);
        /* 提高网页渲染的优先级 */
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        /* HTML5的地理位置服务,设置为true,启用地理定位 */
        mWebView.getSettings().setGeolocationEnabled(true);
        /* 设置可以访问文件 */
        mWebView.getSettings().setAllowFileAccess(true);

        // 网页播放视频有画面没声音<http://web.meiyanchat.com/aa.html>添加如下代码
        // 自动播放网页音乐
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

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
                    if (url.equals("https://www.vip1001.icu/mobile/")){
                        return false;
                    }
                    load(view, url);
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
            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                currentProgress = mProgressBar.getProgress();
                if (newProgress >= 100 && !isAnimStart) {
                    // 防止调用多次动画
                    isAnimStart = true;
                    mProgressBar.setProgress(newProgress);
                    // 开启属性动画让进度条平滑消失
                    startDismissAnimation(mProgressBar.getProgress());
                } else {
                    // 开启属性动画让进度条平滑递增
                    startProgressAnimation(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                updateTitle();
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

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);

                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                frameLayout.addView(mCustomView);
                mCustomViewCallback = callback;
                mWebView.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();

                mWebView.setVisibility(View.VISIBLE);
                if (mCustomView == null) {
                    return;
                }
                mCustomView.setVisibility(View.GONE);
                frameLayout.removeView(mCustomView);
                mCustomViewCallback.onCustomViewHidden();
                mCustomView = null;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                super.onHideCustomView();
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
                ToastUtil.showToast(WebViewActivity.this, R.string.download_error);
            }
        });

        jsSdkInterface = new JsSdkInterface(this, new MyJsSdkListener());
        jsSdkInterface.setShareParams(mShareParams);
        mWebView.addJavascriptInterface(jsSdkInterface, "AndroidWebView");
    }

    private void updateTitle() {
        if (!TextUtils.isEmpty(title)) {
            mTitleTv.setText(title);
            mWebView.setTag(R.id.tag_web_view_title, title);
        } else {
            mTitleTv.setText(mWebView.getTitle());
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
            WebMoreDialog mWebMoreDialog = WebMoreDialog.createSupportFloating(WebViewActivity.this, getCurrentUrl(), isFloating, new WebMoreDialog.BrowserActionClickListener() {
                @Override
                public void floatingWindow() {
                    showFloating();
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
                    ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(getCurrentUrl());
                    Toast.makeText(WebViewActivity.this, getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void openOutSide() {
                    ExternalOpenDialog externalOpenDialog = new ExternalOpenDialog(mContext, getCurrentUrl());
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
                            WebViewActivity.this, title, url, url
                    );
                }

                @Override
                public void shareWechatMoments() {
                    String title = mTitleTv.getText().toString().trim();
                    String url = getCurrentUrl();
                    ShareSdkHelper.shareWechatMoments(
                            WebViewActivity.this, title, url, url
                    );
                }

                @Override
                public void edit() {

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
                String webAppName = URLRequest(url).get("webAppName");
                String webAppsmallImg = URLRequest(url).get("webAppsmallImg");
                String appId = URLRequest(url).get("appId");
                String redirectURL = URLRequest(url).get("callbackUrl");

                Log.e(TAG, "openApp: " + webAppName + "," + webAppsmallImg + "," + url);
                AuthorDialog dialog = new AuthorDialog(mContext);
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
                                if (Result.checkSuccess(mContext, result) && result.getData() != null) {
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
                    if (AppUtils.isSupportIntent(this, intent)) {
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
        Log.e(TAG, mWebView.getUrl() + "");
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
     * 浮窗
     */
    private void showFloating() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || AppUtils.checkAlertWindowsPermission(this)) {
            if (!isFloating && !WindowUtil.getInstance().checkCanPush()) {
                DialogHelper.tip(mContext, getString(R.string.tip_floating_count_limit));
                return;
            }
            isFloating = !isFloating;
            if (isFloating) {
                isFloating = true;
                // 在onStop缓存webView,
                finish();
            }
        } else {
            SelectionFrame dialog = new SelectionFrame(this);
            dialog.setSomething(null, getString(R.string.av_no_float),
                    new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {

                        }

                        @Override
                        public void confirmClick() {
                            PermissionUtil.startApplicationDetailsSettings(WebViewActivity.this, 0x01);
                        }
                    });
            dialog.show();
        }
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
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, AppConstant.NORMAL_INSTANT_ID, message)) {
            InstantMessageActivity.start(mContext, AppConstant.NORMAL_INSTANT_ID, message.getPacketId(), false);
            finish();
        } else {
            Toast.makeText(mContext, R.string.tip_message_wrap_failed, Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(WebViewActivity.this, SendShuoshuoActivity.class);
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
                            Toast.makeText(mContext, getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    /**
     * 搜索页面内容
     */
    private void search() {
        MatchKeyWordEditDialog matchKeyWordEditDialog = new MatchKeyWordEditDialog(this, mWebView);
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
        ModifyFontSizeDialog modifyFontSizeDialog = new ModifyFontSizeDialog(this, mWebView);
        modifyFontSizeDialog.show();
    }

    /**
     * 投诉
     */
    private void report() {
        ComplaintDialog complaintDialog = new ComplaintDialog(this, report -> {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("webUrl", getCurrentUrl());
            params.put("reportType", String.valueOf(3));
            params.put("reason", String.valueOf(report.getReportId()));
            DialogHelper.showDefaulteMessageProgressDialog(WebViewActivity.this);

            HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(WebViewActivity.this, R.string.report_success);
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
            WebViewActivity.this.shareBeanContent = shareBeanContent;
        }

        @Override
        public void onChooseSKPayInApp(String appId, String prepayId, String sign) {
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
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
                                PayDialog payDialog = new PayDialog(mContext, appId, prepayId, sign, result.getData(), new PayDialog.PayResultListener() {
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
