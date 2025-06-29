package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.R;

import java.util.Arrays;
import java.util.List;

public class AppletMoreDialog extends Dialog {
    private Context mContent;

    private RecyclerView mBrowserRecycleView;
    private BrowserActionAdapter mBrowserActionAdapter;
    private List<Item> mData;

    private ActionClickListener mActionClickListener;

    public AppletMoreDialog(Context context, ActionClickListener browserActionClickListener) {
        super(context, R.style.BottomDialog);
        this.mContent = context;
        this.mActionClickListener = browserActionClickListener;
        mData = getData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_applet_menu);

        mBrowserRecycleView = findViewById(R.id.browser_ry);
        mBrowserRecycleView.setLayoutManager(new GridLayoutManager(mContent, 4));
        mBrowserActionAdapter = new BrowserActionAdapter();
        mBrowserRecycleView.setAdapter(mBrowserActionAdapter);

        setCanceledOnTouchOutside(true);

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = ScreenUtil.getScreenWidth(getContext());
        lp.height = ScreenUtil.getScreenHeight(getContext()) / 3 + ScreenUtil.dip2px(getContext(), 90);
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    private List<Item> getData() {
        return Arrays.asList(
                new Item(R.mipmap.share_friend_icon, R.string.send_to_friend, () -> {
                    if (mActionClickListener != null) {
                        dismiss();
                        mActionClickListener.sendToFriend();
                    }
                }),
                new Item(R.mipmap.life_ircle_share_icon, R.string.applet_add, () -> {
                    if (mActionClickListener != null) {
                        dismiss();
                        mActionClickListener.addToMyApplet();
                    }
                }),
                new Item(R.mipmap.complain_complaint_icon, R.string.complaint, () -> {
                    if (mActionClickListener != null) {
                        dismiss();
                        mActionClickListener.complaint();
                    }
                })
        );
    }

    public interface ActionClickListener {
        void sendToFriend();

        void addToMyApplet();

        void complaint();
    }

    class BrowserActionAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.item_browser, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Item item = mData.get(position);
            holder.ivActionImage.setImageResource(item.icon);
            holder.ivActionImage.setOnClickListener(v -> item.runnable.run());
            holder.tvActionName.setText(item.text);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvActionName;
        private final ImageView ivActionImage;

        public ViewHolder(View itemView) {
            super(itemView);
            tvActionName = itemView.findViewById(R.id.tvActionName);
            ivActionImage = itemView.findViewById(R.id.ivActionImage);
        }
    }

    class Item {
        @StringRes
        int text;
        @DrawableRes
        int icon;
        Runnable runnable;

        public Item(int icon, int text, Runnable runnable) {
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
        }
    }
}
