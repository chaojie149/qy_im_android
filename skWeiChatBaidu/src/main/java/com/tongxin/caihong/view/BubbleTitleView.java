package com.tongxin.caihong.view;

import android.content.Context;
import android.graphics.Typeface;

import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView;

public class BubbleTitleView extends SimplePagerTitleView {
    public BubbleTitleView(Context context) {
        super(context);
    }

    @Override
    public void onSelected(int index, int totalCount) {
        super.onSelected(index, totalCount);
        setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    }

    @Override
    public void onDeselected(int index, int totalCount) {
        super.onDeselected(index, totalCount);
        setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
    }
}
