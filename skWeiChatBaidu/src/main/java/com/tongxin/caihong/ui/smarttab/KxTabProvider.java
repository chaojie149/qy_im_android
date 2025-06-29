package com.tongxin.caihong.ui.smarttab;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;

import com.tongxin.caihong.R;

public class KxTabProvider implements SmartTabLayout.TabProvider {

    private final LayoutInflater inflater;

    public KxTabProvider(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View createTabView(ViewGroup container, int position, PagerAdapter adapter) {
        View tabView = inflater.inflate(R.layout.tab_contact_pager, container, false);
        if (tabView != null) {
            TextView tabTitleTextView = tabView.findViewById(R.id.tvText);
            tabTitleTextView.setText(adapter.getPageTitle(position));
        }
        return tabView;
    }

    public static void onPageSelected(SmartTabLayout smartTabLayout, int position) {
        Context ctx = smartTabLayout.getContext();
        for (int i = 0; i < smartTabLayout.getTabCount(); i++) {
            View tab = smartTabLayout.getTabAt(i);
            TextView textView = tab.findViewById(R.id.tvText);
            if (i == position) {
                textView.setTextSize(18);
                textView.setTextColor(ctx.getResources().getColor(R.color.black_new_title));
                textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            } else {
                textView.setTextSize(14);
                textView.setTextColor(ctx.getResources().getColor(R.color.text_color));
                textView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            }
        }
    }

}
