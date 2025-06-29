package com.tongxin.caihong.view.ad;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.ad.Advertising;
import com.tongxin.caihong.helper.AdHelper;
import com.danikula.videocache.HttpProxyCacheServer;
import com.tongxin.caihong.R;

import java.io.File;
import java.util.List;

import fm.jiecao.jcvideoplayer_lib.JCMediaManager;
import fm.jiecao.jcvideoplayer_lib.JCVideoViewbyXuan;
import fm.jiecao.jcvideoplayer_lib.OnJcvdListener;

public class MessageAdView extends AdView {
    @Nullable
    protected ImageView image;
    @Nullable
    protected JCVideoViewbyXuan video;
    protected ImageView logo;
    private TextView name;
    private TextView content;
    private View menu;

    public MessageAdView(@NonNull Context context) {
        super(context);
    }

    public MessageAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MessageAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(AttributeSet attrs) {
        View.inflate(getContext(), R.layout.ad_view_layout_public_message, this);
        image = findViewById(R.id.image);
        video = findViewById(R.id.video);
        logo = findViewById(R.id.logo);
        name = findViewById(R.id.name);
        content = findViewById(R.id.content);
        menu = findViewById(R.id.menu);
    }

    @Override
    protected boolean realLoad(@NonNull List<Advertising> adList) {
        Advertising ad = adList.get(0);
        if (ad == null) {
            return false;
        }
        if (image != null && !TextUtils.isEmpty(ad.getPhoto())) {
            File file = AdHelper.getCachedAd(getContext(), ad.getPhoto());
            image.setVisibility(View.VISIBLE);
            if (video != null) {
                video.setVisibility(GONE);
            }
            if (file.exists()) {
                AdHelper.showImage(getContext(), file, image);
            } else {
                AdHelper.showImage(getContext(), ad.getPhoto(), image);
            }
        }
        if (video != null && !TextUtils.isEmpty(ad.getVideo())) {
            if (image != null) {
                image.setVisibility(View.GONE);
            }
            video.setVisibility(VISIBLE);
            File file = AdHelper.getCachedAd(getContext(), ad.getVideo());
            if (file.exists()) {
                video.play(file.getAbsolutePath());
            } else {
                HttpProxyCacheServer proxy = MyApplication.getProxy(getContext());
                video.play(proxy.getProxyUrl(ad.getVideo()));
            }
            video.addOnJcvdListener(new OnJcvdListener() {
                @Override
                public void onPrepared() {
                    JCMediaManager.instance().mediaPlayer.setVolume(0, 0); // 开始
                }

                @Override
                public void onCompletion() {
                }

                @Override
                public void onError() {

                }

                @Override
                public void onPause() {

                }

                @Override
                public void onReset() {

                }
            });
        }
        content.setText(ad.getContent());
        name.setText(ad.getName());
        if (logo != null && !TextUtils.isEmpty(ad.getLogo())) {
            logo.setVisibility(View.VISIBLE);
            File file = AdHelper.getCachedAd(getContext(), ad.getLogo());
            if (file.exists()) {
                AdHelper.showImage(getContext(), file, logo);
            } else {
                AdHelper.showImage(getContext(), ad.getLogo(), logo);
            }
        }
        menu.setOnClickListener(v -> {
            MessageAdEditWindow publicWindow = new MessageAdEditWindow(v.getContext());
            publicWindow.setClick(v1 -> {
                publicWindow.dismiss();
                switch (v1.getId()) {
                    case R.id.llBad:
                        setVisibility(View.GONE);
                        break;
                }
            });
            publicWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            publicWindow.showAsDropDown(v,
                    -(publicWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                    0);
        });
        return true;
    }

    @Override
    protected void realPause() {
        if (video != null) {
            video.pause();
        }
    }

    @Override
    protected void realResume() {
        if (video != null) {
            video.play("");
        }
    }
}
