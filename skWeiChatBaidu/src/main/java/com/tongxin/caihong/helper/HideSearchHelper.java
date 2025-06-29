package com.tongxin.caihong.helper;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

public class HideSearchHelper {
    public static void hideSearchHeader(Activity activity) {
        RefreshLayout refreshLayout = activity.findViewById(R.id.refreshLayout);
        RefreshLayout innerRefresh = activity.findViewById(R.id.innerRefresh);
        hideSearchHeader(refreshLayout, innerRefresh);
    }

    // fragment回收重建情况也要调用，否则inner会自动收起，
    public static void hideSearchHeader(EasyFragment fragment) {
        RefreshLayout refreshLayout = fragment.findViewById(R.id.refreshLayout);
        RefreshLayout innerRefresh = fragment.findViewById(R.id.innerRefresh);
        hideSearchHeader(refreshLayout, innerRefresh);
    }

    private static void hideSearchHeader(@Nullable RefreshLayout refreshLayout, @NonNull RefreshLayout innerRefresh) {
        if (refreshLayout != null) {
            refreshLayout.setEnableRefresh(false);
        }
        innerRefresh.setOnRefreshListener(r -> {
            if (refreshLayout != null) {
                refreshLayout.setEnableRefresh(true);
            }
            r.setOnRefreshListener(null);
        });
    }

    public static void hideSearch(RecyclerView recyclerView, View searchView) {
        searchView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                if (height == 0) {
                    return;
                }
                v.removeOnLayoutChangeListener(this);
                int margin;
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    margin = mlp.topMargin + mlp.bottomMargin;
                } else {
                    margin = 0;
                }
                recyclerView.post(() -> {
                    recyclerView.smoothScrollBy(0, height + margin, null, 0);
                });
            }
        });
    }

    public static void hideSearch(ListView recyclerView, View searchView) {
        searchView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                if (height == 0) {
                    return;
                }
                v.removeOnLayoutChangeListener(this);
                int margin;
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    margin = mlp.topMargin + mlp.bottomMargin;
                } else {
                    margin = 0;
                }
                recyclerView.post(() -> {
                    recyclerView.smoothScrollBy(height + margin, 0);
                });
            }
        });
    }

}
