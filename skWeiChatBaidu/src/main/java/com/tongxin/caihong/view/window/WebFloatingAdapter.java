package com.tongxin.caihong.view.window;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebFloatingAdapter extends RecyclerView.Adapter<WebFloatingAdapter.ViewHolder> {
    private final OnItemClickListener onItemClickListener;
    private final OnSpaceClickListener onSpaceClickListener;
    private List<Item> data;
    private OnRemoveClickListener onRemoveClickListener;
    private boolean right;

    public WebFloatingAdapter(Context ctx, Map<String, WebView> webViewCache, boolean right, OnItemClickListener onItemClickListener, OnSpaceClickListener onSpaceClickListener, OnRemoveClickListener onRemoveClickListener) {
        this.data = new ArrayList<>(webViewCache.size());
        this.onItemClickListener = onItemClickListener;
        this.onSpaceClickListener = onSpaceClickListener;
        this.onRemoveClickListener = onRemoveClickListener;

        for (Map.Entry<String, WebView> entry : webViewCache.entrySet()) {
            Item item = new Item();
            item.key = entry.getKey();
            WebView value = entry.getValue();
            value.getSettings().setSavePassword(false);
            value.getSettings().setAllowFileAccess(false);
            item.icon = value.getFavicon();
            String title = (String) value.getTag(R.id.tag_web_view_title);
            if (!TextUtils.isEmpty(title)) {
                item.title = title;
            } else {
                item.title = value.getTitle();
            }
            data.add(item);
        }
        this.right = right;
    }

    public void remove(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int resId;
        if (right) {
            resId = R.layout.item_web_floating_list_right;
        } else {
            resId = R.layout.item_web_floating_list_left;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
        return new ViewHolder(view, onItemClickListener, onSpaceClickListener, (position, item) -> {
            remove(position);
            if (onRemoveClickListener != null) {
                onRemoveClickListener.onRemoveClick(position, item);
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = data.get(position);
        holder.apply(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int position, Item item);
    }

    // 空白的部分点击dismiss窗口，
    public interface OnSpaceClickListener {
        void onSpaceClick(int position, Item item);
    }

    public interface OnRemoveClickListener {
        void onRemoveClick(int position, Item item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private View llContent = itemView.findViewById(R.id.llContent);
        private ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        private TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        private View ivRemove = itemView.findViewById(R.id.ivRemove);
        private OnItemClickListener onItemClickListener;
        private OnSpaceClickListener onSpaceClickListener;
        private OnRemoveClickListener onRemoveClickListener;

        public ViewHolder(@NonNull View itemView, OnItemClickListener onItemClickListener, OnSpaceClickListener onSpaceClickListener, OnRemoveClickListener onRemoveClickListener) {
            super(itemView);
            this.onItemClickListener = onItemClickListener;
            this.onSpaceClickListener = onSpaceClickListener;
            this.onRemoveClickListener = onRemoveClickListener;
        }

        public void apply(Item item) {
            if (item.icon != null) {
                ivIcon.setImageBitmap(item.icon);
            } else {
                ivIcon.setImageResource(R.mipmap.ic_float_window_link);
            }
            tvTitle.setText(item.title);
            ivRemove.setOnClickListener(v -> {
                if (onRemoveClickListener != null) {
                    onRemoveClickListener.onRemoveClick(getLayoutPosition(), item);
                }
            });
            llContent.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(getLayoutPosition(), item);
                }
            });
            itemView.setOnClickListener(v -> {
                if (onSpaceClickListener != null) {
                    onSpaceClickListener.onSpaceClick(getLayoutPosition(), item);
                }
            });
        }
    }

    public static class Item {
        String key;
        Bitmap icon;
        String title;
    }
}
