package com.tongxin.caihong.ui.applet;

import android.text.TextUtils;

import com.tongxin.caihong.bean.Applet;
import com.tongxin.caihong.helper.RecyclerViewLoadMoreHelper;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.builder.BaseBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AppletSearchFragment extends AppletListFragment {
    private String searchKey;

    @Override
    public void onLoadData(int pageIndex, int pageSize, RecyclerViewLoadMoreHelper.OnPageLoadedListener<Applet> onPageLoadedListener) {
        if (TextUtils.isEmpty(searchKey)) {
            onPageLoadedListener.onPageLoaded(Collections.emptyList());
            refreshLayout.finishRefresh();
            return;
        }
        super.onLoadData(pageIndex, pageSize, onPageLoadedListener);
    }

    @Override
    protected BaseBuilder.BaseCall makeRequestData(int pageIndex, int pageSize) {
        Map<String, String> params = new HashMap<>();
        params.put("pageIndex", String.valueOf(pageIndex));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("keyword", searchKey);

        return HttpUtils.get().url(coreManager.getConfig().APPLET_LIST)
                .params(params)
                .build();
    }

    public void search(String searchKey) {
        this.searchKey = searchKey;
        loadMoreHelper.initData();
    }
}
