package com.tongxin.caihong.view.ad;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.tongxin.caihong.bean.ad.Advertising;
import com.tongxin.caihong.helper.AdHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.R;
import com.zhpan.bannerview.BannerViewPager;
import com.zhpan.bannerview.BaseBannerAdapter;
import com.zhpan.bannerview.BaseViewHolder;
import com.zhpan.bannerview.constants.PageStyle;
import com.zhpan.bannerview.utils.BannerUtils;
import com.zhpan.indicator.enums.IndicatorSlideMode;
import com.zhpan.indicator.enums.IndicatorStyle;

import java.util.List;

public class BannerAdView extends AdView {
    private BannerViewPager<Advertising, ViewHolder> viewPager;
    private Adapter adapter;

    public BannerAdView(@NonNull Context context) {
        super(context);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(AttributeSet attrs) {
        View.inflate(getContext(), R.layout.ad_view_layout_banner, this);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setIndicatorStyle(IndicatorStyle.ROUND_RECT)
                .setIndicatorSliderGap(BannerUtils.dp2px(4))
                .setIndicatorSlideMode(IndicatorSlideMode.NORMAL)
                .setIndicatorHeight(BannerUtils.dp2px(4))
                .setIndicatorSliderColor(Color.parseColor("#ccffffff"), SkinUtils.getSkin(getContext()).getAccentColor())
                .setIndicatorSliderWidth(BannerUtils.dp2px(4), BannerUtils.dp2px(20))
                .setPageMargin(BannerUtils.dp2px(16))
                .setRevealWidth(BannerUtils.dp2px(15))
                .setPageStyle(PageStyle.MULTI_PAGE_SCALE);
        adapter = new Adapter();
        viewPager.setAdapter(adapter);
    }

    @Override
    protected boolean realLoad(@NonNull List<Advertising> adList) {
        if (adList.isEmpty()) {
            return false;
        }
        viewPager.setOnPageClickListener(position -> {
            Advertising ad = adList.get(position);
            openAndIncLink(ad);
            callClick();
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected() called with: position = [" + position + "]");
                incBurst(adList.get(position));
            }
        });
        viewPager.create(adList);
        incBurst(adList.get(0));
        return true;
    }

    @Override
    protected void realResume() {
        viewPager.startLoop();
    }

    @Override
    protected void realPause() {
        viewPager.stopLoop();
    }

    private static class ViewHolder extends BaseViewHolder<Advertising> {
        private ImageView image = itemView.findViewById(R.id.image);

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void bindData(Advertising data, int position, int pageSize) {
            AdHelper.showImage(itemView.getContext(), data.getPhoto(), image);
        }
    }

    private static class Adapter extends BaseBannerAdapter<Advertising, ViewHolder> {

        @Override
        protected void onBind(ViewHolder holder, Advertising data, int position, int pageSize) {
            holder.bindData(data, position, pageSize);
        }

        @Override
        public ViewHolder createViewHolder(View itemView, int viewType) {
            return new ViewHolder(itemView);
        }

        @Override
        public int getLayoutId(int viewType) {
            return R.layout.ad_view_item_banner;
        }
    }
}
