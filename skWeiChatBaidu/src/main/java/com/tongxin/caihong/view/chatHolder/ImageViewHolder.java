package com.tongxin.caihong.view.chatHolder;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.downloader.DownloadListener;
import com.tongxin.caihong.downloader.FailReason;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.message.ChatOverviewActivity;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.view.XuanProgressPar;
import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.List;

public class ImageViewHolder extends AChatHolderInterface {
    private static final int IMAGE_MIN_SIZE = 90;
    private static final int IMAGE_MAX_SIZE = 120;
    private ImageView mImageView;
    private XuanProgressPar progressPar;
    private ChatMessage message;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_image : R.layout.chat_to_item_image;
    }

    @Override
    public void initView(View view) {
        mImageView = view.findViewById(R.id.chat_image);
        progressPar = view.findViewById(R.id.img_progress);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        this.message = message;
        // 修改image布局大小，解决因图片异步加载且布局设置的warp_content导致setSelection不能滑动到最底部的问题
        changeImageLayoutSize(message);

        String filePath = UploadCacheUtils.get(mContext, message);
        if (FileUtil.isExist(filePath)) { // 本地存在
/*
            if (filePath.endsWith(".gif")) { // 加载gif
                fillImageGif(filePath);
            } else {
                if (mHolderListener != null) {
                    Bitmap bitmap = mHolderListener.onLoadBitmap(filePath, width, height);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        mImageView.setImageBitmap(null);
                    }
                }
            }
*/
            fillImage(filePath);
        } else {
            if (TextUtils.isEmpty(message.getContent())) {// 理论上不可能
                mImageView.setImageResource(R.drawable.fez);
            } else {
                mImageView.setImageDrawable(null);
                // todo 加载缩略图地址，此处就不将图片下载存到本地了，否则大图查看、保存、转发都需要用到原图，
                //  但本地filePath已经保存了缩略图且做了判断，冲突了，所以直接用glide加载缩略图地址
                // Downloader.getInstance().addDownload(message.getContent(), mSendingBar, new FileDownloadListener(message));
                // 加载缩略图地址
                String data;
                if (message.getContent().endsWith(".gif")) {
                    // gif不加载缩略图
                    data = message.getContent();
                } else {
                    data = message.getContent().replaceFirst("/o/", "/t/");
                }
                fillImage(data);
            }
        }

        // 判断是否为阅后即焚类型的图片，如果是 模糊显示该图片
        if (!isGounp) {
            mImageView.setAlpha(message.getIsReadDel() ? 0.1f : 1f);
        }

        // 上传进度条 我的消息才有进度条
        if (!isMysend || message.isUpload() || message.getUploadSchedule() >= 100) {
            progressPar.setVisibility(View.GONE);
        } else {
            progressPar.setVisibility(View.VISIBLE);
        }
        progressPar.update(message.getUploadSchedule());
    }

/*
    private void fillImageGif(String filePath) {
        try {
            GifDrawable gifFromFile = new GifDrawable(new File(filePath));
            mImageView.setImageGifDrawable(gifFromFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillImage(String filePath) {
        AvatarHelper.getInstance().displayUrl(filePath, mImageView, R.drawable.fez);
    }
*/

    private void fillImage(String filePath) {
        if (filePath.endsWith(".gif")) {
            ImageLoadHelper.showGifWithError(
                    MyApplication.getContext(),
                    filePath,
                    R.drawable.fez,
                    mImageView
            );
        } else {
            ImageLoadHelper.showImageDontAnimateWithError(
                    MyApplication.getContext(),
                    filePath,
                    R.drawable.fez,
                    mImageView
            );
        }
    }

    private void changeImageLayoutSize(ChatMessage message) {
        ViewGroup.LayoutParams mLayoutParams = mImageView.getLayoutParams();

        if (TextUtils.isEmpty(message.getLocation_x()) || TextUtils.isEmpty(message.getLocation_y())) {
            mLayoutParams.width = dp2px(IMAGE_MAX_SIZE);
            mLayoutParams.height = dp2px(IMAGE_MAX_SIZE);
            // todo Location_x 与Location_y为空，本地基本上不存在该file，下面会去下载，就不在这里下载了
            // Downloader.getInstance().addDownload(message.getContent(), mSendingBar, new FileDownloadListener(message));
        } else {
            float image_width = Float.parseFloat(message.getLocation_x());
            float image_height = Float.parseFloat(message.getLocation_y());

            if (image_width == 0f || image_height == 0f) {
                // 以防万一，
                mLayoutParams.width = dp2px(IMAGE_MAX_SIZE);
                mLayoutParams.height = dp2px(IMAGE_MAX_SIZE);
            } else {
                // 基于宽度进行缩放,三挡:宽图 55/100,窄图100/55
                // float width = image_width / image_height < 0.4 ? IMAGE_MIN_SIZE : IMAGE_MAX_SIZE;
                // float height = width == IMAGE_MAX_SIZE ? Math.max(width / image_width * image_height, IMAGE_MIN_SIZE) : IMAGE_MAX_SIZE;

                // https://www.jianshu.com/p/773b87e3356b
                // 实测，宽短高长的图片显示效果不是很好，自己添加一个缩放比例参数来控制
                double zoom = 3.0;
                float width = image_width;
                float height = image_height;
                float ratio = width / height;
                //根据宽高比来设置外框的size
                if (ratio < 0.4) {
                    width = 204; //这是从微信截图的长度最后需要同一除以3
                    height = 510;
                    zoom = 2.4;
                } else if (ratio >= 0.4 && ratio <= 0.5) {
                    width = 204;
                    height = 204 / ratio;
                    zoom = 2.4;
                } else if (ratio > 0.5 && ratio < 1) {
                    width = 405 * ratio;
                    height = 405;
                } else if (ratio >= 1 && ratio < 1 / 0.5) { //和前面的宽高转置
                    height = 405 * (1 / ratio);
                    width = 405;
                } else if (ratio >= 1 / 0.5 && ratio < 1 / 0.4) {
                    height = 204;
                    width = 204 / (1 / ratio);
                } else if (ratio >= 1 / 0.4) {
                    height = 204; //这是从微信截图的长度最后需要同一除以3
                    width = 510;
                }
                height /= zoom;
                width /= zoom;

                mLayoutParams.width = dp2px(width);
                mLayoutParams.height = dp2px(height);
            }
        }

        mImageView.setLayoutParams(mLayoutParams);
    }

    @Override
    public void onRootClick(View v) {
        if (mdata.getIsReadDel()) {
            // 阅后即焚图片跳转至单张图片预览类
            Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGE_URI, mdata.getContent());
            String filePath = UploadCacheUtils.get(mContext, mdata);
            intent.putExtra("image_path", filePath);
            intent.putExtra("isReadDel", mdata.getIsReadDel());
            if (!isGounp && !isMysend && mdata.getIsReadDel()) {
                intent.putExtra("DEL_PACKEDID", mdata.getPacketId());
            }
            mContext.startActivity(intent);
        } else {
            int imageChatMessageList_current_position = 0;
            List<ChatMessage> imageChatMessageList = new ArrayList<>();
            for (int i = 0; i < chatMessages.size(); i++) {
                if (chatMessages.get(i).getType() == XmppMessage.TYPE_IMAGE
                        && !chatMessages.get(i).getIsReadDel()) {
                    if (chatMessages.get(i).getPacketId().equals(mdata.getPacketId())) {
                        imageChatMessageList_current_position = imageChatMessageList.size();
                    }
                    imageChatMessageList.add(chatMessages.get(i));
                }
            }
            Intent intent = new Intent(mContext, ChatOverviewActivity.class);
            ChatOverviewActivity.imageChatMessageListStr = JSON.toJSONString(imageChatMessageList);
            intent.putExtra("imageChatMessageList_current_position", imageChatMessageList_current_position);
            intent.putExtra("isGroup", isGounp);
            mContext.startActivity(intent);
        }
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    // 启用阅后即焚
    @Override
    public boolean enableFire() {
        return true;
    }

    class FileDownloadListener implements DownloadListener {
        private ChatMessage message;

        public FileDownloadListener(ChatMessage message) {
            this.message = message;
        }

        @Override
        public void onStarted(String uri, View view) {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailed(String uri, FailReason failReason, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        @Override
        public void onComplete(String uri, String filePath, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
            message.setFilePath(filePath);
            ChatMessageDao.getInstance().updateMessageDownloadState(mLoginUserId, mToUserId, message.get_id(), true, filePath);
            // 保存图片尺寸到数据库
            saveImageSize(filePath);
/*
            if (filePath.endsWith(".gif")) { // 加载gif
                fillImageGif(filePath);
            } else { // 加载图片
                fillImage(filePath);
            }
*/
            fillImage(filePath);
        }

        @Override
        public void onCancelled(String uri, View view) {
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }

        /**
         * 获取图片宽高，保存至本地
         *
         * @param filePath
         */
        private void saveImageSize(String filePath) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options); // 此时返回的bitmap为null

            message.setLocation_x(String.valueOf(options.outWidth));
            message.setLocation_y(String.valueOf(options.outHeight));

            // 重绘图片尺寸
            changeImageLayoutSize(message);
            // 保存下载到数据库
            ChatMessageDao.getInstance().updateMessageLocationXY(message, mLoginUserId);
        }
    }
}
