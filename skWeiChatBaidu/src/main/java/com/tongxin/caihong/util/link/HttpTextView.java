package com.tongxin.caihong.util.link;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.LinkMovementClickMethod;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.view.BottomListDialog;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tongxin.caihong.ui.tool.WebViewActivity.EXTRA_URL;

public class HttpTextView extends AppCompatTextView {
    // 创建 Pattern 对象
    private static final Pattern urlPattern = Pattern.compile(HttpUtil.REGEX_URL);
    private static final Pattern phonePattern = Pattern.compile("\\d{6,}");
    private static final int flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
    private boolean needToRegionUrl = true;//是否开启识别URL，默认开启
    private boolean disableLinkClick = false;

    public HttpTextView(Context context) {
        this(context, null);
    }

    public HttpTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HttpTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void build(SpannableStringBuilder sb, MakeSpanCallback callback) {
        ClickableSpan[] clickableSpans = sb.getSpans(0, sb.length(), ClickableSpan.class);
        int start = 0;
        for (ClickableSpan span : clickableSpans) {
            int spanStart = sb.getSpanStart(span);
            if (spanStart > start) {
                callback.makeSpan(sb, start, spanStart);
            }
            start = sb.getSpanEnd(span);
        }
        if (sb.length() > start) {
            callback.makeSpan(sb, start, sb.length());
        }
    }

    public void setUrlText(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            super.setText(text);
            return;
        }
        if (needToRegionUrl) {
            SpannableStringBuilder sb = new SpannableStringBuilder(text);
            // 优先解析网址，网址以外的文本再解析手机号，
            build(sb, this::urlMakeSpan);
            build(sb, this::phoneMakeSpan);
            super.setText(sb);
            this.setMovementMethod(LinkMovementClickMethod.getInstance());
        } else {
            super.setText(text);
        }
    }

    public boolean getIsNeedToRegionUrl() {
        return needToRegionUrl;
    }

    public void setOpenRegionUrl(boolean needToRegionUrl) {
        this.needToRegionUrl = needToRegionUrl;
    }

    private void urlMakeSpan(SpannableStringBuilder sb, int start, int end) {
        if (end <= start) {
            return;
        }
        Matcher m = urlPattern.matcher(sb.subSequence(start, end));
        //匹配成功
        while (m.find()) {
            //得到网址数
            int mStart = start + m.start();
            int mEnd = start + m.end();
            sb.setSpan(new URLClick(sb.subSequence(mStart, mEnd).toString()), mStart, mEnd, flag);
        }
    }

    private void phoneMakeSpan(SpannableStringBuilder sb, int start, int end) {
        if (end <= start) {
            return;
        }
        Matcher m = phonePattern.matcher(sb.subSequence(start, end));
        //匹配成功
        while (m.find()) {
            //得到网址数
            int mStart = start + m.start();
            int mEnd = start + m.end();
            sb.setSpan(new PhoneClick(sb.subSequence(mStart, mEnd).toString()), mStart, mEnd, flag);
        }
    }

    public void breakClick() {
        disableLinkClick = true;
    }

    public void resetClick() {
        disableLinkClick = false;
    }

    private interface MakeSpanCallback {
        void makeSpan(SpannableStringBuilder sb, int start, int end);
    }

    private class URLClick extends ClickableSpan {
        private String text;

        public URLClick(String text) {
            this.text = text;
        }

        @Override
        public void onClick(View view) {
            if (disableLinkClick) {
                return;
            }
            if (UiUtils.isNormalClick(view)) {
                Context context = view.getContext();
                Intent intent = new Intent(context, WebViewActivity.class);
                if (text.startsWith("http") || text.startsWith("https") || text.startsWith("ftp")) {
                    intent.putExtra(EXTRA_URL, text);
                } else {
                    intent.putExtra(EXTRA_URL, "http://" + text);
                }
                context.startActivity(intent);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(0xff517fae);
            ds.setUnderlineText(false);
        }
    }

    private class PhoneClick extends ClickableSpan {
        private String text;

        public PhoneClick(String text) {
            this.text = text;
        }

        @Override
        public void onClick(View view) {
            if (disableLinkClick) {
                return;
            }
            if (UiUtils.isNormalClick(view)) {
                BottomListDialog.show(view.getContext(), Arrays.asList(view.getContext().getString(R.string.dial_telephone), view.getContext().getString(R.string.copy)), (item, position) -> {
                    if (position == 0) {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + text));//跳转到拨号界面，同时传递电话号码
                        view.getContext().startActivity(dialIntent);
                    } else if (position == 1) {
                        ClipboardManager cmb = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        cmb.setText(text);
                        ToastUtil.showToast(view.getContext(), R.string.copy_success);
                    }
                });
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(0xff517fae);
            ds.setUnderlineText(false);
        }
    }
}
