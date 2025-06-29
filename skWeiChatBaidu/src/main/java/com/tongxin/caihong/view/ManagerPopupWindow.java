package com.tongxin.caihong.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.fragment.app.FragmentActivity;

import com.tongxin.caihong.R;
import com.tongxin.caihong.db.InternationalizationHelper;

public class ManagerPopupWindow  extends PopupWindow {
    private Button mModifyRoom, mSetManager, mShutUp, mKick, mCancel;
    private View mMenuView;

    public ManagerPopupWindow(FragmentActivity context, View.OnClickListener itemsOnClick, int type, int state) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.manager_liveroom_dialog, null);
        mModifyRoom = (Button) mMenuView.findViewById(R.id.modify_liveroom);
        mSetManager = (Button) mMenuView.findViewById(R.id.set_manager);
        mShutUp = (Button) mMenuView.findViewById(R.id.shut_up);
        mKick = (Button) mMenuView.findViewById(R.id.kick_room);
        mCancel = (Button) mMenuView.findViewById(R.id.cancel);
        mSetManager.setText(InternationalizationHelper.getString("JXLiveVC_SetTheAdmin"));
        // "管理"范围
        //  type == 3 this is gone
        if (type == 2) {
            mModifyRoom.setVisibility(View.GONE);
            mSetManager.setVisibility(View.GONE);
        }
        if (state == 0) {
            //            mShutUp.setText("禁言");
            mShutUp.setText(InternationalizationHelper.getString("JXLiveVC_SetGag"));
        } else {
            // 已经处于禁言状态
            //            mShutUp.setText("取消禁言");
            mShutUp.setText(InternationalizationHelper.getString("JXLiveVC_GagCancel"));
        }
        mKick.setText(InternationalizationHelper.getString("JXLiveVC_Kick"));
        mCancel.setText(InternationalizationHelper.getString("JX_Cencal"));
        //设置按钮监听
        mModifyRoom.setOnClickListener(itemsOnClick);
        mSetManager.setOnClickListener(itemsOnClick);
        mShutUp.setOnClickListener(itemsOnClick);
        mKick.setOnClickListener(itemsOnClick);
        mCancel.setOnClickListener(itemsOnClick);
        //设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        //	        this.setWidth(ViewPiexlUtil.dp2px(context,200));
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LinearLayout.LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(context.getResources().getColor(R.color.alp_background));
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        //        mMenuView.setOnTouchListener(new OnTouchListener() {
        //
        //            public boolean onTouch(View v, MotionEvent event) {
        //
        //                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
        //                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
        //                int y = (int) event.getY();
        //                if (event.getAction() == MotionEvent.ACTION_UP) {
        //                    if (y < height) {
        //                        dismiss();
        //                    } else if (y > bottom) {
        //                        dismiss();
        //                    }
        //                }
        //                return true;
        //            }
        //        });
    }
}
