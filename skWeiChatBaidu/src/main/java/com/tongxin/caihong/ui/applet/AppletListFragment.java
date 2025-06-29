package com.tongxin.caihong.ui.applet;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tongxin.caihong.bean.Applet;
import com.tongxin.caihong.helper.RecyclerViewLoadMoreHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.builder.BaseBuilder;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class AppletListFragment extends EasyFragment implements RecyclerViewLoadMoreHelper.OnLoadDataListener<Applet> {
    protected RecyclerViewLoadMoreHelper<Applet> loadMoreHelper = new RecyclerViewLoadMoreHelper<>();
    protected SmartRefreshLayout refreshLayout;

    private SwipeRecyclerView recyclerView;
    private AppletAdapter adapter;

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_applet_list;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        initView();
    }

    @Override
    public void onLoadData(int pageIndex, int pageSize, RecyclerViewLoadMoreHelper.OnPageLoadedListener<Applet> onPageLoadedListener) {
        BaseBuilder.BaseCall build = makeRequestData(pageIndex, pageSize);
        build.execute(new ListCallback<Applet>(Applet.class) {

                    @Override
                    public void onResponse(ArrayResult<Applet> result) {
                        if (getContext() == null) {
                            // 防崩溃，
                            return;
                        }
                        if (Result.checkSuccess(requireContext(), result)) {
                            List<Applet> data = result.getData();
                            onPageLoadedListener.onPageLoaded(data);
                        }
                        refreshLayout.finishRefresh();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (getContext() == null) {
                            // 防崩溃，
                            return;
                        }
                        ToastUtil.showNetError(requireContext());
                        refreshLayout.finishRefresh();
                    }
                });
    }

    protected BaseBuilder.BaseCall makeRequestData(int pageIndex, int pageSize) {
        Map<String, String> params = new HashMap<>();
        params.put("pageIndex", String.valueOf(pageIndex));
        params.put("pageSize", String.valueOf(pageSize));

        return HttpUtils.get().url(coreManager.getConfig().APPLET_LIST)
                .params(params)
                .build();
    }

    private void initView() {
        refreshLayout = findViewById(R.id.refreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        initHeader();
        adapter = new AppletAdapter(getListener());
        recyclerView.setAdapter(adapter);
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            loadMoreHelper.initData();
        });
        loadMoreHelper.setOnLoadDataListener(this);
        loadMoreHelper.setAdapter(adapter);
        loadMoreHelper.initRecyclerView(recyclerView);
        loadMoreHelper.initData();
    }

    protected void initHeader() {
    }

    @Nullable
    protected OnAppletItemClickListener getListener() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnAppletItemClickListener) {
            return (OnAppletItemClickListener) parentFragment;
        }
        Activity activity = getActivity();
        if (activity instanceof OnAppletItemClickListener) {
            return (OnAppletItemClickListener) activity;
        }
        return null;
    }

}
