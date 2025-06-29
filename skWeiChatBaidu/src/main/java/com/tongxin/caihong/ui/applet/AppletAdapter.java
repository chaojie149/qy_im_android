package com.tongxin.caihong.ui.applet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.bean.Applet;
import com.tongxin.caihong.helper.RecyclerViewLoadMoreHelper;
import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.List;

class AppletAdapter extends RecyclerView.Adapter<AppletViewHolder> implements RecyclerViewLoadMoreHelper.Adapter<Applet> {
    private List<Applet> data = new ArrayList<>();
    private OnAppletItemClickListener onItemClickListener;

    AppletAdapter(OnAppletItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public void setData(List<Applet> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public void addAll(List<Applet> data) {
        int oldSize = this.data.size();
        this.data.addAll(data);
        notifyItemRangeInserted(oldSize, data.size());
    }

    @NonNull
    @Override
    public AppletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_applet, parent, false);
        return new AppletViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppletViewHolder holder, int position) {
        Applet item = data.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
            AppletActivity.start(holder.itemView.getContext(), item.getAppUrl());
        });
        holder.apply(item, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
