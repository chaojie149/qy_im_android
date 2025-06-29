package com.tongxin.caihong.view.ad;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.tongxin.caihong.bean.ad.Advertising;
import com.tongxin.caihong.helper.AdHelper;

import java.util.Collections;
import java.util.List;

public class AdView extends FrameLayout {
    protected final String TAG = getClass().getSimpleName();
    protected boolean loaded = false;
    private OnAdSkipListener onAdSkipListener;
    private OnAdClickListener onAdClickListener;

    public AdView(@NonNull Context context) {
        super(context);
        init(null);
    }

    public AdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public AdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void setOnAdSkipListener(OnAdSkipListener onAdSkipListener) {
        this.onAdSkipListener = onAdSkipListener;
    }

    public void setOnAdClickListener(OnAdClickListener onAdClickListener) {
        this.onAdClickListener = onAdClickListener;
    }

    protected void init(AttributeSet attrs) {
    }

    protected boolean realLoad(@NonNull List<Advertising> adList) {
        return true;
    }

    protected void realPause() {
    }

    protected void realResume() {
    }

    protected void realDestroy() {
    }

    public void load(Advertising ad) {
        load(Collections.singletonList(ad));
    }

    /**
     * 轮播图有多个广告，
     */
    public void load(@Nullable List<Advertising> adList) {
        Log.d(TAG, "load() called with: adList = [" + adList + "]");
        if (adList == null || adList.isEmpty()) {
            return;
        }
        // 广告不可以为null,
        adList.remove(null);
        if (adList.size() == 1 && !TextUtils.isEmpty(adList.get(0).getLink())) {
            setOnClickListener(v -> {
                openAndIncLink(adList.get(0));
                callClick();
            });
        }

        // 确实有广告加载出来就返回loaded为true,
        loaded = realLoad(adList);
        if (!loaded) {
            // 没广告加载出来就隐藏广告控件，
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
            if (adList.size() == 1 && !TextUtils.isEmpty(adList.get(0).getLink())) {
                incBurst(adList.get(0));
            }
        }
    }

    protected void incBurst(Advertising ad) {
        AdHelper.incBurst(getContext(), ad.getId());
    }

    protected void callSkip() {
        if (onAdSkipListener != null) {
            onAdSkipListener.onAdSkip();
        }
    }

    protected void callClick() {
        if (onAdClickListener != null) {
            onAdClickListener.onAdClick();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resume();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();
    }

    public void pause() {
        if (!loaded) {
            return;
        }
        realPause();
    }

    public void resume() {
        if (!loaded) {
            return;
        }
        realResume();
    }

    public void destroy() {
        if (!loaded) {
            return;
        }
        realDestroy();
    }

    public boolean isLoaded() {
        return loaded;
    }

    protected void openAndIncLink(Advertising ad) {
        AdHelper.openAndIncLink(getContext(), ad.getLink(), ad.getId());
    }

    public void bind(Lifecycle lifecycle) {
        lifecycle.addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            void onResume() {
                resume();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            void onPause() {
                pause();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void onDestroy() {
                destroy();
            }
        });
    }

    public interface OnAdSkipListener {
        void onAdSkip();
    }

    public interface OnAdClickListener {
        void onAdClick();
    }
}
