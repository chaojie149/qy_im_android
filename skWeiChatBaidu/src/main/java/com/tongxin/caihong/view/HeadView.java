package com.tongxin.caihong.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.makeramen.roundedimageview.RoundedImageView;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.ScreenUtil;

import java.util.Locale;

/**
 * 单人的头像，
 * 群组是另外的组合头像，
 */
public class HeadView extends RelativeLayout {

    private float width;
    private float height;
    private RoundedImageView ivHead;
    private ImageView ivFrame;
    private View layout;

    public HeadView(Context context) {
        super(context);
        init(null);
    }

    public HeadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public HeadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HeadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        layout = View.inflate(getContext(), R.layout.view_head, this);
        ivHead = layout.findViewById(R.id.ivHead);
        ivFrame = layout.findViewById(R.id.ivFrame);
        initImageLayout(attrs);
    }

    private void initImageLayout(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        // 如果HeadView的宽高确定了，ImageView的宽高也能确定，尽早确定下来以便glide加载内存缓存，
        int[] attrsArray = new int[]{
                android.R.attr.layout_width,
                android.R.attr.layout_height
        };
        TypedArray ta = getContext().obtainStyledAttributes(attrs, attrsArray);
        width = ta.getDimension(0, -1);
        height = ta.getDimension(1, -1);
        ta.recycle();
        if (width > 0 && height > 0) {
            ViewGroup.LayoutParams lp = ivHead.getLayoutParams();
            lp.width = (int) (width);
            lp.height = (int) (height);
            ivHead.setLayoutParams(lp);
        }
    }

    public ImageView getHeadImage() {
        return ivHead;
    }

    public void updateFramePadding(boolean enabledPadding) {
        MarginLayoutParams lp = (MarginLayoutParams) ivHead.getLayoutParams();
        if (enabledPadding) {
            lp.setMargins(px(2), px(1), px(2), px(3));
            lp.width = (int) (width - px(4));
            lp.height = (int) (height - px(4));
        } else {
            lp.setMargins(0, 0, 0, 0);
            lp.width = (int) (width);
            lp.height = (int) (height);
        }
        ivHead.setLayoutParams(lp);
    }

    public void setGroupRole(Integer role) {
        if (role == null) {
            ivFrame.setVisibility(View.GONE);
            updateFramePadding(false);
            return;
        }
        switch (role) {
            case RoomMember.ROLE_OWNER:
                if (!Locale.getDefault().getLanguage().equals("zh")) {
                    ivFrame.setImageResource(R.drawable.i_u_owner);
                }else {
                    ivFrame.setImageResource(R.mipmap.frame_group_owner);
                }
                ivFrame.setVisibility(View.VISIBLE);
                break;
            case RoomMember.ROLE_MANAGER:
                if (!Locale.getDefault().getLanguage().equals("zh")) {
                    ivFrame.setImageResource(R.drawable.i_u_admin);
                }else {
                    ivFrame.setImageResource(R.mipmap.frame_group_manager);
                }
                ivFrame.setVisibility(View.VISIBLE);
                break;
            default:
                ivFrame.setVisibility(View.GONE);
        }
        ivHead.requestLayout();
    }

    private int px(int dp) {
        return DisplayUtil.dip2px(getContext(), dp);
    }

    public void setRound(boolean round) {
        ivHead.setOval(round);
    }

    public void setBorder(float dp, int color) {
        ivHead.setBorderWidth(Float.valueOf(ScreenUtil.dip2px(getContext(), dp)));
        ivHead.setBorderColor(color);
    }

}
