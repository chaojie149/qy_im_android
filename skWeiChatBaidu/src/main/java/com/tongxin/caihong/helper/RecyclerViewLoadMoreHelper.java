package com.tongxin.caihong.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.List;

/**
 * 辅助实现上拉加载更多，
 */
public class RecyclerViewLoadMoreHelper<T> {
    private int pageIndex;
    private boolean noMore;
    @Nullable
    private Adapter<T> adapter;
    @Nullable
    private OnLoadDataListener<T> onLoadDataListener;
    @Nullable
    private OnNoMoreListener onNoMoreListener;
    private int pageSize = 20;
    private boolean calcNoMore;

    public void setAdapter(@Nullable Adapter<T> adapter) {
        this.adapter = adapter;
    }

    public void setOnLoadDataListener(@Nullable OnLoadDataListener<T> onLoadDataListener) {
        this.onLoadDataListener = onLoadDataListener;
    }

    public void setOnNoMoreListener(@Nullable OnNoMoreListener onNoMoreListener) {
        this.onNoMoreListener = onNoMoreListener;
    }

    public void loadMore() {
        if (noMore) {
            return;
        }
        ++pageIndex;
        callLoadData(pageIndex, false);
    }

    public void initData() {
        pageIndex = 0;
        noMore = false;
        if (adapter != null) {
            adapter.setNoMore(false);
        }
        if (onNoMoreListener != null) {
            onNoMoreListener.onNoMore(false);
        }
        callLoadData(pageIndex, true);
    }

    private void callLoadData(int pageIndex, boolean clean) {
        if (onLoadDataListener == null) {
            setNoMore();
            return;
        }
        onLoadDataListener.onLoadData(pageIndex, getPageSize(), data -> {
            if (clean) {
                if (adapter != null) {
                    adapter.setData(data);
                }
            } else {
                if (adapter != null) {
                    int oldSize = adapter.getItemCount();
                    adapter.addAll(data);
                    if (oldSize > 0) {
                        // 更新分割线，
                        adapter.notifyItemChanged(oldSize - 1, false);
                    }
                }
            }
            if (data.size() < getPageSize()) {
                setNoMore();
            }
        });
    }

    public void setNoMore() {
        noMore = true;
        if (adapter != null) {
            adapter.setNoMore(true);
        }
        if (onNoMoreListener != null) {
            onNoMoreListener.onNoMore(true);
        }
    }

    /**
     * 如果数据有筛选，就不在统一在这里计算是否还有剩，
     */
    public void calcNoMore(boolean calcNoMore) {
        this.calcNoMore = calcNoMore;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void initRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int lastPosition;
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager == null) {
                        // 不处理，
                        return;
                    }
                    if (layoutManager instanceof GridLayoutManager) {
                        //通过LayoutManager找到当前显示的最后的item的position
                        lastPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
                    } else if (layoutManager instanceof LinearLayoutManager) {
                        lastPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                    } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                        //因为StaggeredGridLayoutManager的特殊性可能导致最后显示的item存在多个，所以这里取到的是一个数组
                        //得到这个数组后再取到数组中position值最大的那个就是最后显示的position值了
                        int[] lastPositions = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                        ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(lastPositions);
                        lastPosition = findMax(lastPositions);
                    } else {
                        // 不支持，
                        return;
                    }
                    if (lastPosition == recyclerView.getLayoutManager().getItemCount() - 1) {
                        loadMore();
                    }
                }
            }
        });
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public interface Adapter<T> {
        int getItemCount();

        // 用于统一更新分割线，recyclerView.adapter都有，
        void notifyItemChanged(int position, @Nullable Object payload);

        void setData(List<T> tList);

        void addAll(List<T> tList);

        default void setNoMore(boolean noMore) {
        }
    }

    public interface OnLoadDataListener<T> {
        void onLoadData(int pageIndex, int pageSize, OnPageLoadedListener<T> onPageLoadedListener);
    }

    public interface OnNoMoreListener {
        void onNoMore(boolean noMore);
    }

    public interface OnPageLoadedListener<T> {
        void onPageLoaded(List<T> data);
    }
}
