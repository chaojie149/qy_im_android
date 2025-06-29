package com.tongxin.caihong.ui.company;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.List;

public class CompanySearchAdapter extends RecyclerView.Adapter<CompanySearchAdapter.ViewHolder> {
    private List<Item> data = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public CompanySearchAdapter(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_coworker_search, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = data.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
        holder.apply(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public List<Item> getData() {
        return data;
    }

    public void setData(List<Item> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public static class Item {
        public String userId;
        public String name;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead = itemView.findViewById(R.id.ivHead);
        TextView tvName = itemView.findViewById(R.id.tvName);

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void apply(Item item) {
            AvatarHelper.getInstance().displayAvatar(item.userId, ivHead);
            tvName.setText(item.name);
        }
    }

    public  interface OnItemClickListener {
        void onItemClick(Item item);
    }
}
