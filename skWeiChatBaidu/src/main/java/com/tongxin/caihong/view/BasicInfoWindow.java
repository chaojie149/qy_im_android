package com.tongxin.caihong.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.User;

public class BasicInfoWindow extends PopupWindow {
    private TextView setName, addBlackList, removeBlackList, delete, reportTv;
    private View mMenuView;

    public BasicInfoWindow(FragmentActivity context, OnClickListener itemsOnClick, User user) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.popu_basicinfo, null);

        reportTv = (TextView) mMenuView.findViewById(R.id.report_tv);
        setName = (TextView) mMenuView.findViewById(R.id.set_remark_nameS);
        addBlackList = (TextView) mMenuView.findViewById(R.id.add_blacklist);
        removeBlackList = (TextView) mMenuView.findViewById(R.id.remove_blacklist);
        delete = (TextView) mMenuView.findViewById(R.id.delete_tv);
        LinearLayout ll_basic = mMenuView.findViewById(R.id.ll_basic);

        // 举报与设置备注默认显示，其余隐藏，先根据status显示隐藏ui
        if (user.isSystem()) {
            // 公众号，只显示举报
            setName.setVisibility(View.GONE);
        } else if (user.getFriends() == null) {
            // 陌生人，且之前无备注 || 拉黑 || 移除拉黑等操作，显示拉黑
            addBlackList.setVisibility(View.VISIBLE);
        } else if (user.getFriends().getStatus() == 2 || user.getFriends().getStatus() == 4) {
            // 好友，显示拉黑、删除
            addBlackList.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        } else {
            // 陌生人，且之前有备注 || 拉黑 || 移除拉黑等操作，先显示拉黑
            addBlackList.setVisibility(View.VISIBLE);
        }

        if (user.getFriends() != null
                && user.getFriends().getBlacklist() == 1) {
            // 将对方加入了黑名单，显示移出黑名单，隐藏拉黑、删除
            addBlackList.setVisibility(View.GONE);
            removeBlackList.setVisibility(View.VISIBLE);
            delete.setVisibility(View.GONE);
        } else if (user.getFriends() != null
                && user.getFriends().getIsBeenBlack() == 1) {
            // 对方将你拉入了黑名单，隐藏拉黑、删除
            addBlackList.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
        }

        //设置按钮监听
        setName.setOnClickListener(itemsOnClick);
        addBlackList.setOnClickListener(itemsOnClick);
        removeBlackList.setOnClickListener(itemsOnClick);
        delete.setOnClickListener(itemsOnClick);
        reportTv.setOnClickListener(itemsOnClick);

        this.setContentView(mMenuView);
        this.setWidth(LayoutParams.WRAP_CONTENT);
        this.setHeight(LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);

        this.setOutsideTouchable(true);

        this.setAnimationStyle(R.style.Buttom_Popwindow);
        ColorDrawable dw = new ColorDrawable(0000000000);
        this.setBackgroundDrawable(dw);
    }
}