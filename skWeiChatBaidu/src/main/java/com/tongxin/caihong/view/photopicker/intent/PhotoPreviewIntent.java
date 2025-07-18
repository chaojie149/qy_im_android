package com.tongxin.caihong.view.photopicker.intent;

import android.content.Context;
import android.content.Intent;

import com.tongxin.caihong.view.photopicker.PhotoPreviewActivity;

import java.util.ArrayList;

/**
 * 预览照片
 * Created by foamtrace on 2015/8/25.
 */
public class PhotoPreviewIntent extends Intent {

    public PhotoPreviewIntent(Context packageContext) {
        super(packageContext, PhotoPreviewActivity.class);
    }

    /**
     * 照片地址
     *
     * @param paths
     */
    public void setPhotoPaths(ArrayList<String> paths) {
        this.putStringArrayListExtra(PhotoPreviewActivity.EXTRA_PHOTOS, paths);
    }

    /**
     * 为了支持图片编辑，传第二套地址，编辑后的，
     *
     * @param paths
     */
    public void setRealPhotoPaths(ArrayList<String> paths) {
        this.putStringArrayListExtra(PhotoPreviewActivity.EXTRA_REAL_PHOTOS, paths);
    }

    /**
     * 当前照片的下标
     *
     * @param currentItem
     */
    public void setCurrentItem(int currentItem) {
        this.putExtra(PhotoPreviewActivity.EXTRA_CURRENT_ITEM, currentItem);
    }
}
