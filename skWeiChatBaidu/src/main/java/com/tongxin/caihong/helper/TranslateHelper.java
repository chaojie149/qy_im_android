package com.tongxin.caihong.helper;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.AbsListView;

import androidx.annotation.NonNull;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Translation;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.LocaleHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.ChatContentView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;

import static com.tongxin.caihong.bean.message.XmppMessage.TYPE_REPLAY;
import static com.tongxin.caihong.bean.message.XmppMessage.TYPE_TEXT;

public class TranslateHelper {
    private static final String SP_NAME = "sk_auto_translate";
    private static final String KEY_AUTO_TRANSLATE = "KEY_AUTO_TRANSLATE";

    public static void init(ChatContentView view, ChatContentView.ChatContentAdapter mChatContentAdapter, AutoTranslateHandler autoTranslateHandler) {
        view.addOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    autoTranslateHandler.removeCallbacksAndMessages(null);
                } else if (scrollState == SCROLL_STATE_IDLE) {
                    autoTranslateHandler.postTranslate();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        mChatContentAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                autoTranslateHandler.postTranslate();
            }

            @Override
            public void onInvalidated() {
                autoTranslateHandler.postTranslate();
            }
        });
    }

    public static void translate(ChatContentView view, ChatMessage message) {
        if (!needTranslate(message)) {
            return;
        }
        Context mContext = view.getContext();
        Log.d("translate", "translate() called with: message = [" + message.getContent() + "]");
        Map<String, String> params = new HashMap<>();
        params.put("content", message.getContent());
        params.put("from", "auto");
        params.put("to", LocaleHelper.getBaiduLanguage(mContext));

        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getInstance()).TRANSLATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Translation>(Translation.class) {

                    @Override
                    public void onResponse(ObjectResult<Translation> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            Log.d("translate", "translate result: " + result.getData().getTranslation());
                            message.setTranslation(result.getData().getTranslation());
                            view.onTranslateResult(message);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    public static boolean needTranslate(ChatMessage message) {
        return (message.getType() == TYPE_TEXT || message.getType() == TYPE_REPLAY)
                && !TextUtils.isEmpty(message.getContent())
                && TextUtils.isEmpty(message.getTranslation());
    }

    public static boolean needAutoTranslate(ChatMessage message) {
        return needTranslate(message)
                && !message.isMySend();
    }

    public static boolean isAutoTranslate(Context ctx, String ownerId, String userId) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_TRANSLATE + ownerId + userId, false);
    }

    public static void setAutoTranslate(Context ctx, String ownerId, String userId, boolean isChecked) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_TRANSLATE + ownerId + userId, isChecked)
                .apply();
    }

    public static class AutoTranslateHandler extends Handler {
        private final WeakReference<ChatContentView> w;

        public AutoTranslateHandler(ChatContentView v) {
            super();
            w = new WeakReference<>(v);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            ChatContentView v = w.get();
            if (v == null) {
                return;
            }
            v.autoTranslate();
        }

        public void postTranslate() {
            removeCallbacksAndMessages(null);
            sendEmptyMessageDelayed(1, TimeUnit.SECONDS.toMillis(10));
        }
    }

}
