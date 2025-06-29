package com.tongxin.caihong.view.window;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.R;

import java.util.Map;

public class WebFloatingListWindow extends PopupWindow {
    private OnDismissListener onDismissListener;

    public WebFloatingListWindow(View view, Point point, Map<String, WebView> webViewCache, WebFloatingAdapter.OnRemoveClickListener onRemoveClickListener) {
        super(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        final Context ctx = view.getContext();

        //设置SelectPicPopupWindow的View
        this.setContentView(view);
        //设置SelectPicPopupWindow弹出窗体的宽
        WindowManager manager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = (manager.getDefaultDisplay().getWidth());
        this.setWidth(screenWidth);
        //设置SelectPicPopupWindow弹出窗体的高
        int screenHeight = (manager.getDefaultDisplay().getHeight());
        this.setHeight(screenHeight);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);

        this.setOutsideTouchable(true);

        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(0);

        //设置SelectPicPopupWindow弹出窗体的背景
        // 透明背景，
        this.setBackgroundDrawable(new ColorDrawable(0));
        view.setBackgroundColor(Color.parseColor("#aaffffff"));
        this.setOnDismissListener(() -> {
            view.setBackground(null);
            if (onDismissListener != null) {
                onDismissListener.onDismiss();
            }
        });

        view.setOnClickListener(v -> {
            dismiss();
        });
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setItemAnimator(null);
        WebFloatingAdapter adapter = new WebFloatingAdapter(ctx, webViewCache, point.x > screenWidth / 2,
                (position, item) -> {
                    WebViewActivity.startFromFloating(ctx, item.key);
                    dismiss();
                },
                (position, item) -> dismiss(),
                onRemoveClickListener
        );
        recyclerView.setAdapter(adapter);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // true表示要显示在点击位置上方，
        boolean top = point.y > screenHeight / 2;
        View vBase;
        RelativeLayout.LayoutParams vlp;
        if (top) {
            vlp = new RelativeLayout.LayoutParams(0, screenHeight - point.y);
            vlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            lp.addRule(RelativeLayout.ABOVE, R.id.vBaseBottom);
            vBase = view.findViewById(R.id.vBaseBottom);
        } else {
            vlp = new RelativeLayout.LayoutParams(0, point.y);
            lp.addRule(RelativeLayout.BELOW, R.id.vBaseTop);
            vBase = view.findViewById(R.id.vBaseTop);
        }
        vBase.setLayoutParams(vlp);
        recyclerView.setLayoutParams(lp);
    }

    public void addOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }
}
