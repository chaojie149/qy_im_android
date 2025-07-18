package com.tongxin.caihong.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.widget.ImageViewCompat;

import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.R;

/**
 * 抽象出标题栏上的控件以实现根据皮肤切换深色浅色，
 */
public class SkinImageView extends AppCompatImageView {
    public SkinImageView(Context context) {
        super(context);
        init();
    }

    public SkinImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SkinImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        SkinUtils.Skin skin = SkinUtils.getSkin(getContext());
        if (skin.isLight()) {
            ImageViewCompat.setImageTintList(this, getContext().getResources().getColorStateList(R.color.black));
        } else {
            ImageViewCompat.setImageTintList(this, getContext().getResources().getColorStateList(R.color.white));
        }
    }
}
