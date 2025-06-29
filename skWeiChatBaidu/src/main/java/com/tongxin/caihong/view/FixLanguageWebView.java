package com.tongxin.caihong.view;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.tongxin.caihong.util.LocaleHelper;

/**
 * webView初始化会把context中的语言设置重置为默认，影响app自己的语言设置，所以webView初始化后再设置一次app语言，
 * https://stackoverflow.com/questions/40398528/android-webview-language-changes-abruptly-on-android-7-0-and-above
 */
public class FixLanguageWebView extends WebView {
    public FixLanguageWebView(Context context) {
        super(context);
        init();
    }

    public FixLanguageWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FixLanguageWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FixLanguageWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        LocaleHelper.setLocale(getContext().getApplicationContext());
        LocaleHelper.setLocale(getContext());
    }
}
