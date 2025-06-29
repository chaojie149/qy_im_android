package com.tongxin.caihong.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import com.tongxin.caihong.view.ZoomImageView;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.UploadCacheUtils;

import java.io.File;
import java.util.List;

public class ChatOverviewAdapter extends PagerAdapter {
    private Context mContext;
    private List<ChatMessage> mChatMessages;
    private SparseArray<View> mViews = new SparseArray<>();

    public ChatOverviewAdapter(Context context, List<ChatMessage> chatMessages) {
        mContext = context;
        mChatMessages = chatMessages;
    }

    public void refreshItem(String url, int index) {
        AvatarHelper.getInstance().displayUrl(url, (ZoomImageView) mViews.get(index));
    }

    @Override
    public int getCount() {
        return mChatMessages.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mViews.get(position);
        if (view == null) {
            view = new ZoomImageView(mContext);
            mViews.put(position, view);
        }

        ChatMessage chatMessage = mChatMessages.get(position);
        String filePath = UploadCacheUtils.get(mContext, chatMessage);
        if (!TextUtils.isEmpty(filePath) && FileUtil.isExist(filePath)) {
            ImageLoadHelper.showFile(
                    mContext, new File(filePath), (ZoomImageView) view
            );
        } else {
            ImageLoadHelper.showImage(
                    mContext, chatMessage.getContent(), (ZoomImageView) view, true
            );
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = mViews.get(position);
        if (view == null) {
            super.destroyItem(container, position, object);
        } else {
            container.removeView(view);
        }

    }
}
