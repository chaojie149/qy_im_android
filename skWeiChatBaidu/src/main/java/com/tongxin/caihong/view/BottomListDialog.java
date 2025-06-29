package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.R;

import java.util.List;

public class BottomListDialog extends Dialog {
    private List<String> data;
    private OnItemClickListener clickListener;
    private boolean cancelItemEnabled = true;

    private BottomListDialog(@NonNull Context ctx, List<String> data, OnItemClickListener clickListener) {
        super(ctx, R.style.BottomDialog);
        this.data = data;
        this.clickListener = clickListener;
    }

    public static BottomListDialog show(@NonNull Context ctx, List<String> data, OnItemClickListener clickListener) {
        BottomListDialog dialog = new BottomListDialog(ctx, data, clickListener);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bottom_list);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        RecyclerView rvList = findViewById(R.id.rvList);
        rvList.setAdapter(new Adapter());
/*
        NoLastDividerItemDecoration divider = new NoLastDividerItemDecoration(getContext(), NoLastDividerItemDecoration.VERTICAL);
        divider.setDrawable(getContext().getResources().getDrawable(R.drawable.divider_bottom_dialog));
        rvList.addItemDecoration(divider);
*/

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        // x/y坐标
        // lp.x = 100;
        // lp.y = 100;
        lp.width = ScreenUtil.getScreenWidth(getContext());
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    public interface OnItemClickListener {
        void onItemClick(String item, int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvValue = itemView.findViewById(R.id.tvValue);
        View vLine1 = itemView.findViewById(R.id.vLine1);
        View vLine2 = itemView.findViewById(R.id.vLine2);

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void apply(String s) {
            tvValue.setText(s);
/*
            if (getLayoutPosition() == 0) {
                itemView.setBackgroundResource(R.drawable.list_selector_background_ripple_radius);
            } else {
                itemView.setBackgroundResource(R.drawable.list_selector_background_ripple);
            }
*/
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_bottom_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item;
            boolean isCancel = position >= data.size();
            if (isCancel) {
                item = (holder.itemView.getContext().getString(R.string.near_cancel));

                holder.tvValue.setTextColor(getContext().getResources().getColor(R.color.gray));
            } else {
                item = (data.get(position));

                if (position == data.size() - 1) {
                    // 最后一个item为取消
                    holder.vLine2.setVisibility(View.VISIBLE);
                } else {
                    holder.vLine1.setVisibility(View.VISIBLE);
                }
            }
            holder.apply(item);
            if (isCancel) {
                holder.itemView.setOnClickListener(v -> {
                    dismiss();
                });
            } else {
                if (clickListener != null) {
                    holder.itemView.setOnClickListener(v -> {
                        clickListener.onItemClick(item, position);
                        dismiss();
                    });
                } else {
                    holder.itemView.setOnClickListener(null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return data.size() + (cancelItemEnabled ? 1 : 0);
        }
    }
}
