//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.tongxin.caihong.view;

import android.content.Context;
import android.util.AttributeSet;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.base.CoreManager;

public class MessageAvatar extends HeadView {

    public MessageAvatar(Context context) {
        super(context);
    }

    public MessageAvatar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageAvatar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = this.getMeasuredWidth();
        int height = this.getMeasuredHeight();
        int dimen = Math.min(width, height);
        this.setMeasuredDimension(dimen, dimen);
    }

    public void fillData(Friend friend) {
        AvatarHelper.getInstance().displayAvatar(CoreManager.requireSelf(getContext()).getUserId(), friend, this);
    }
}
