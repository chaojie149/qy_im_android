package com.tongxin.caihong.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.R;

import net.lucode.hackware.magicindicator.FragmentContainerHelper;
import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.UIUtil;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.model.PositionData;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView;

import java.util.List;

/**
 * 气泡翻页指示器标签，
 * {@link #setAdjustMode(boolean)} 设置铺满
 * {@link #setSelectedIndicatorColor(int)} 设置选中颜色，
 * {@link View#setBackground(Drawable)} 设置背景,
 * {@link #setup(List, ViewPager)} 设置数据并绑定viewPager,
 * {@link #setData(List)} 设置备注，配合{@link #setOnPageSelectListener(OnPageSelectListener)}设置点击监听，
 */
// 继承ScrollView是因为MagicIndicator大量使用matchParent，普通父视图会导致标签铺满全屏，
@SuppressWarnings({"unused", "RedundantSuppression"})
public class BubblePagerIndicator extends ScrollView {
    private MagicIndicator magicIndicator;
    @Nullable
    private List<String> data;
    @Nullable
    private ViewPager viewPager;
    @NonNull
    private FragmentContainerHelper containerHelper = new FragmentContainerHelper();
    @NonNull
    private BubbleCommonNavigatorAdapter navigatorAdapter = new BubbleCommonNavigatorAdapter();
    @Nullable
    private OnPageSelectListener onPageSelectListener; // 绑定viewPager的话不需要这个监听，可以直接监听viewPager,
    private int innerVerticalPadding;
    private int innerHorizontalPadding;
    private CommonNavigator commonNavigator;

    public BubblePagerIndicator(Context context) {
        super(context);
        init(null);
    }

    public BubblePagerIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BubblePagerIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public BubblePagerIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setBackgroundResource(R.drawable.bubble_pager_indicator_bg);

        int outerPadding;
        TypedArray ta = null;
        if (attrs != null) {
            ta = getContext().obtainStyledAttributes(attrs, R.styleable.BubblePagerIndicator);
        }
        if (ta != null && ta.hasValue(R.styleable.BubblePagerIndicator_outerPadding)) {
            outerPadding = ta.getDimensionPixelSize(R.styleable.BubblePagerIndicator_outerPadding, UIUtil.dip2px(getContext(), 2));
        } else {
            outerPadding = UIUtil.dip2px(getContext(), 2);
        }
        if (ta != null && ta.hasValue(R.styleable.BubblePagerIndicator_innerVerticalPadding)) {
            innerVerticalPadding = ta.getDimensionPixelSize(R.styleable.BubblePagerIndicator_innerVerticalPadding, UIUtil.dip2px(getContext(), 4));
        } else {
            innerVerticalPadding = UIUtil.dip2px(getContext(), 4);
        }
        if (ta != null && ta.hasValue(R.styleable.BubblePagerIndicator_innerHorizontalPadding)) {
            innerHorizontalPadding = ta.getDimensionPixelSize(R.styleable.BubblePagerIndicator_innerHorizontalPadding, UIUtil.dip2px(getContext(), 10));
        } else {
            innerHorizontalPadding = UIUtil.dip2px(getContext(), 10);
        }

        magicIndicator = new MagicIndicator(getContext());
        magicIndicator.setPadding(outerPadding, outerPadding, outerPadding, outerPadding);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        addView(magicIndicator, lp);
        commonNavigator = new CommonNavigator(getContext());
        commonNavigator.setAdapter(navigatorAdapter);
        magicIndicator.setNavigator(commonNavigator);
        containerHelper.attachMagicIndicator(magicIndicator);
        if (ta != null) {
            ta.recycle();
        }
    }

    @Nullable
    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
        navigatorAdapter.notifyDataSetChanged();
    }

    public void bind(ViewPager viewPager) {
        this.viewPager = viewPager;
        ViewPagerHelper.bind(magicIndicator, viewPager);
    }

    public void setup(List<String> data, ViewPager viewPager) {
        setData(data);
        bind(viewPager);
    }

    /**
     * 设置铺满模式，
     *
     * @param adjustMode 为true表示铺满，平均分所有item, 为false表示自适应宽度，配合wrap_content使用或者item过多需要滑动时用，
     */
    public void setAdjustMode(boolean adjustMode) {
        commonNavigator.setAdjustMode(adjustMode);
        navigatorAdapter.setAdjustMode(adjustMode);
        navigatorAdapter.notifyDataSetChanged();
    }

    /**
     * 设置选中的指示器颜色，
     *
     * @param color 参数是颜色值，不是颜色id,
     */
    public void setSelectedIndicatorColor(int color) {
        navigatorAdapter.setSelectedIndicatorColor(color);
        navigatorAdapter.notifyDataSetChanged();
    }

    @Nullable
    public OnPageSelectListener getOnPageSelectListener() {
        return onPageSelectListener;
    }

    public void setOnPageSelectListener(@Nullable OnPageSelectListener onPageSelectListener) {
        this.onPageSelectListener = onPageSelectListener;
    }

    public interface OnPageSelectListener {
        void onPageSelected(int index);
    }

    /**
     * 通用的indicator，支持外面设置Drawable
     * Created by hackware on 2016/11/14.
     */
    public static class CommonPagerIndicator extends View implements IPagerIndicator {
        public static final int MODE_MATCH_EDGE = 0;   // drawable宽度 == title宽度 - 2 * mXOffset
        public static final int MODE_WRAP_CONTENT = 1;    // drawable宽度 == title内容宽度 - 2 * mXOffset
        public static final int MODE_EXACTLY = 2;

        private int mMode;  // 默认为MODE_MATCH_EDGE模式
        private Drawable mIndicatorDrawable;

        // 控制动画
        private Interpolator mStartInterpolator = new LinearInterpolator();
        private Interpolator mEndInterpolator = new LinearInterpolator();

        private float mDrawableHeight;
        private float mDrawableWidth;
        private float mYOffset;
        private float mXOffset;

        private List<PositionData> mPositionDataList;
        private Rect mDrawableRect = new Rect();

        public CommonPagerIndicator(Context context) {
            super(context);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mIndicatorDrawable == null) {
                return;
            }

            if (mPositionDataList == null || mPositionDataList.isEmpty()) {
                return;
            }

            // 计算锚点位置
            PositionData current = FragmentContainerHelper.getImitativePositionData(mPositionDataList, position);
            PositionData next = FragmentContainerHelper.getImitativePositionData(mPositionDataList, position + 1);

            float leftX;
            float nextLeftX;
            float rightX;
            float nextRightX;
            if (mMode == MODE_MATCH_EDGE) {
                leftX = current.mLeft + mXOffset;
                nextLeftX = next.mLeft + mXOffset;
                rightX = current.mRight - mXOffset;
                nextRightX = next.mRight - mXOffset;
                mDrawableRect.top = (int) mYOffset;
                mDrawableRect.bottom = (int) (getHeight() - mYOffset);
            } else if (mMode == MODE_WRAP_CONTENT) {
                leftX = current.mContentLeft + mXOffset;
                nextLeftX = next.mContentLeft + mXOffset;
                rightX = current.mContentRight - mXOffset;
                nextRightX = next.mContentRight - mXOffset;
                mDrawableRect.top = (int) (current.mContentTop - mYOffset);
                mDrawableRect.bottom = (int) (current.mContentBottom + mYOffset);
            } else {    // MODE_EXACTLY
                leftX = current.mLeft + (current.width() - mDrawableWidth) / 2;
                nextLeftX = next.mLeft + (next.width() - mDrawableWidth) / 2;
                rightX = current.mLeft + (current.width() + mDrawableWidth) / 2;
                nextRightX = next.mLeft + (next.width() + mDrawableWidth) / 2;
                mDrawableRect.top = (int) (getHeight() - mDrawableHeight - mYOffset);
                mDrawableRect.bottom = (int) (getHeight() - mYOffset);
            }

            mDrawableRect.left = (int) (leftX + (nextLeftX - leftX) * mStartInterpolator.getInterpolation(positionOffset));
            mDrawableRect.right = (int) (rightX + (nextRightX - rightX) * mEndInterpolator.getInterpolation(positionOffset));
            mIndicatorDrawable.setBounds(mDrawableRect);

            invalidate();
        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mIndicatorDrawable != null) {
                mIndicatorDrawable.draw(canvas);
            }
        }

        @Override
        public void onPositionDataProvide(List<PositionData> dataList) {
            mPositionDataList = dataList;
        }

        public Drawable getIndicatorDrawable() {
            return mIndicatorDrawable;
        }

        public void setIndicatorDrawable(Drawable indicatorDrawable) {
            mIndicatorDrawable = indicatorDrawable;
        }

        public Interpolator getStartInterpolator() {
            return mStartInterpolator;
        }

        public void setStartInterpolator(Interpolator startInterpolator) {
            mStartInterpolator = startInterpolator;
        }

        public Interpolator getEndInterpolator() {
            return mEndInterpolator;
        }

        public void setEndInterpolator(Interpolator endInterpolator) {
            mEndInterpolator = endInterpolator;
        }

        public int getMode() {
            return mMode;
        }

        public void setMode(int mode) {
            if (mode == MODE_EXACTLY || mode == MODE_MATCH_EDGE || mode == MODE_WRAP_CONTENT) {
                mMode = mode;
            } else {
                throw new IllegalArgumentException("mode " + mode + " not supported.");
            }
        }

        public float getDrawableHeight() {
            return mDrawableHeight;
        }

        public void setDrawableHeight(float drawableHeight) {
            mDrawableHeight = drawableHeight;
        }

        public float getDrawableWidth() {
            return mDrawableWidth;
        }

        public void setDrawableWidth(float drawableWidth) {
            mDrawableWidth = drawableWidth;
        }

        public float getYOffset() {
            return mYOffset;
        }

        public void setYOffset(float yOffset) {
            mYOffset = yOffset;
        }

        public float getXOffset() {
            return mXOffset;
        }

        public void setXOffset(float xOffset) {
            mXOffset = xOffset;
        }
    }

    private class BubbleCommonNavigatorAdapter extends CommonNavigatorAdapter {
        private int selectedIndicatorColor = Color.parseColor("#ffffff");
        private boolean adjustMode;

        @Override
        public int getCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public IPagerTitleView getTitleView(Context context, final int index) {
            SimplePagerTitleView simplePagerTitleView = new BubbleTitleView(context);
            simplePagerTitleView.setPadding(
                    innerHorizontalPadding,
                    innerVerticalPadding,
                    innerHorizontalPadding,
                    innerVerticalPadding
            );
            assert data != null;
            simplePagerTitleView.setText(data.get(index));
            simplePagerTitleView.setTextSize(13);
            simplePagerTitleView.setNormalColor(Color.parseColor("#666666"));
            simplePagerTitleView.setSelectedColor(Color.parseColor("#333333"));
            simplePagerTitleView.setOnClickListener(v -> {
                if (viewPager != null) {
                    viewPager.setCurrentItem(index);
                } else {
                    containerHelper.handlePageSelected(index, true);
                }
                if (onPageSelectListener != null) {
                    onPageSelectListener.onPageSelected(index);
                }
            });
            return simplePagerTitleView;
        }

        @Override
        public IPagerIndicator getIndicator(Context context) {
            CommonPagerIndicator indicator = new CommonPagerIndicator(context);
            if (adjustMode) {
                indicator.setMode(CommonPagerIndicator.MODE_MATCH_EDGE);
                indicator.setYOffset(0);
                indicator.setXOffset(0);
            } else {
                indicator.setMode(CommonPagerIndicator.MODE_WRAP_CONTENT);
                indicator.setYOffset(innerVerticalPadding);
                indicator.setXOffset(-innerHorizontalPadding);
            }
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.bubble_pager_indicator_selected_bg);
            assert drawable != null;
            DrawableCompat.setTint(drawable, selectedIndicatorColor);
            indicator.setIndicatorDrawable(drawable);
            return indicator;
        }

        public void setSelectedIndicatorColor(int selectedIndicatorColor) {
            this.selectedIndicatorColor = selectedIndicatorColor;
        }

        public void setAdjustMode(boolean adjustMode) {
            this.adjustMode = adjustMode;
        }
    }
}
