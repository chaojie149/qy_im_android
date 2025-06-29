package com.tongxin.caihong.ui.applet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.R;

import java.util.List;

public class AppletSearchHistoryAdapter extends RecyclerView.Adapter<AppletSearchHistoryAdapter.ViewHolder> {
    private OnRemoveClickListener onRemoveClickListener;
    private OnItemClickListener onItemClickListener;
    private List<String> data;

    public AppletSearchHistoryAdapter(OnRemoveClickListener onRemoveClickListener, OnItemClickListener onItemClickListener) {
        this.onRemoveClickListener = onRemoveClickListener;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public AppletSearchHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =  LayoutInflater.from(parent.getContext()).inflate(R.layout.item_applet_search_key_history, parent, false);
        return new ViewHolder(view, onRemoveClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull AppletSearchHistoryAdapter.ViewHolder holder, int position) {
        String item = data.get(position);
        holder.apply(item);
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSearchHistory = itemView.findViewById(R.id.tvSearchHistory);
        private final View ivDelete = itemView.findViewById(R.id.ivDelete);
        private final OnRemoveClickListener onRemoveClickListener;

        public ViewHolder(@NonNull View itemView, OnRemoveClickListener onRemoveClickListener) {
            super(itemView);
            this.onRemoveClickListener = onRemoveClickListener;
        }

        public void apply(String item) {
            tvSearchHistory.setText(item);
            ivDelete.setOnClickListener(v -> {
                onRemoveClickListener.onRemoveClick(getLayoutPosition(), item);
            });
        }
    }

    interface OnRemoveClickListener {
        void onRemoveClick(int position, String item);
    }

    interface OnItemClickListener {
        void onItemClick(int position, String item);
    }
}
