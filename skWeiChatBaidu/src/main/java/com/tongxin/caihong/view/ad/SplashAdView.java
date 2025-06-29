package com.tongxin.caihong.view.ad;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tongxin.caihong.bean.ad.Advertising;
import com.tongxin.caihong.helper.AdHelper;
import com.tongxin.caihong.view.CheckableImageView;
import com.tongxin.caihong.R;

import java.io.File;
import java.util.List;

public class SplashAdView extends AdView {
    @Nullable
    protected ImageView image;
    @Nullable
    protected TextureVideoView video;
    @Nullable
    protected CheckableImageView mute;
    @Nullable
    protected View skip;
    @Nullable
    protected TextView time;
    @Nullable
    protected CountDownTimer timer;

    public SplashAdView(@NonNull Context context) {
        super(context);
    }

    public SplashAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SplashAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SplashAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(AttributeSet attrs) {
        View.inflate(getContext(), R.layout.ad_view_layout_media, this);
        image = findViewById(R.id.image);
        video = findViewById(R.id.video);
        mute = findViewById(R.id.mute);
        skip = findViewById(R.id.skip);
        time = findViewById(R.id.time);
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
                video.setDataSource(file.getAbsolutePath());
            } else {
                video.setDataSource(ad.getVideo());
            }
            video.play();
            video.requestFocus();
            video.setListener(new TextureVideoView.MediaPlayerListener() {
                @Override
                public void onVideoPrepared(MediaPlayer mediaPlayer) {
                    //设置为静音
                    mediaPlayer.setVolume(0f, 0f);
                    if (mute != null) {
                        mute.setVisibility(VISIBLE);
                        mute.setChecked(true);
                        mute.setOnClickListener(v -> {
                            mute.toggle();
                            if (mute.isChecked()) {
                                mediaPlayer.setVolume(0f, 0f);
                            } else {
                                mediaPlayer.setVolume(1f, 1f);
                            }
                        });
                    }
                }

                @Override
                public void onVideoEnd() {
                    callSkip();
                }
            });
        }
        if (skip != null) {
            skip.setOnClickListener(v -> {
                callSkip();
            });
        }
        if (time != null) {
            int max = 5;
            time.setText(String.valueOf(max));
            long startTime = System.currentTimeMillis();
            timer = new CountDownTimer(max * 1000, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    time.setText(String.valueOf(((max * 1000 + 999) - (System.currentTimeMillis() - startTime)) / 1000));
                }

                @Override
                public void onFinish() {
                    time.setText("0");
                    callSkip();
                }
            };
        }
        boolean ret = (image != null && image.getVisibility() == View.VISIBLE) || (video != null && video.getVisibility() == View.VISIBLE);
        if (ret) {
            if (timer != null) {
                // 有图片视频广告加载才启用计时器，
                timer.start();
            }
        }
        return ret;
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
            video.play();
        }
    }

    @Override
    protected void realDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        if (video != null) {
            video.stop();
        }
    }
}
