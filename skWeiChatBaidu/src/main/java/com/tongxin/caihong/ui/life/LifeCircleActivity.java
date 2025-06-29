package com.tongxin.caihong.ui.life;

import android.os.Bundle;

import com.tongxin.caihong.R;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.ui.base.BaseActivity;

public class LifeCircleActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_life_circle);
        getSupportActionBar().hide();
    }

    @Override
    public void finish() {
        VoicePlayer.instance().stop();
        super.finish();
    }

    @Override
    public void setRequestedOrientation(int requestedOrientation) {
    }
}
