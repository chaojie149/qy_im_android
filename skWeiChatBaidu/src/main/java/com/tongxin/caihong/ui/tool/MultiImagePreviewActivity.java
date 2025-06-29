package com.tongxin.caihong.ui.tool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.message.HandleQRCodeScanUtil;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.PlayUploadUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.SaveWindow;
import com.tongxin.caihong.view.ZoomImageView;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.imageedit.IMGEditActivity;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 多张图片预览
 * <p>
 * modify by zq
 * 目前有两个地方会进入此类，
 * 1.发布动态时
 * 2.动态、我的收藏多图预览
 * <p>
 * 现图片长按弹窗针对这两个地方做不同的ui显示处理
 * 1 弹窗只显示编辑、保存图片
 * 2 在上面的基础上在显示发送给朋友与收藏功能
 */
public class MultiImagePreviewActivity extends BaseActivity {
    public static final int REQUEST_IMAGE_EDIT = 1;
    SparseArray<View> mViews = new SparseArray<View>();
    private ImagesAdapter mAdapter;
    private TextView mIndexCountTv;
    private CheckBox mCheckBox;

    private ArrayList<String> mImages;
    // 因为编辑之后会改变数据源的数据，本地使用clone保留源数据，用于编辑图片之后收藏功能
    private ArrayList<String> clone;
    private int mPosition;
    private boolean mChangeSelected;
    private PublicMessage publicMessage;
    private boolean isMyCollection;
    private ViewPager mViewPager;

    private List<Integer> mRemovePosition = new ArrayList<Integer>();
    private String imageUrl;
    private String mRealImageUrl;// 因为viewPager的预加载机制，需要记录当前页面真正的url
    private String mEditedPath;
    private SaveWindow mSaveWindow;
    private My_BroadcastReceivers my_broadcastReceiver = new My_BroadcastReceivers();

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableSwipeBack = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images_preview);

        if (getIntent() != null) {
            mImages = (ArrayList<String>) getIntent().getSerializableExtra(AppConstant.EXTRA_IMAGES);
            if (mImages == null) {
                mImages = new ArrayList<>();
            }
            clone = new ArrayList<>(mImages);
            mPosition = getIntent().getIntExtra(AppConstant.EXTRA_POSITION, 0);
            mChangeSelected = getIntent().getBooleanExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
            String message = getIntent().getStringExtra("json");
            publicMessage = JSON.parseObject(message, PublicMessage.class);
            isMyCollection = getIntent().getBooleanExtra("isMyCollection", false);
        }
        initView();
        register();
    }

    @Override
    public void onBackPressed() {
        doFinish();
    }

    @Override
    protected boolean onHomeAsUp() {
        doFinish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (my_broadcastReceiver != null) {
            unregisterReceiver(my_broadcastReceiver);
        }
    }

    private void initView() {
        getSupportActionBar().hide();
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mIndexCountTv = (TextView) findViewById(R.id.index_count_tv);
        mCheckBox = (CheckBox) findViewById(R.id.check_box);
        mViewPager.setPageMargin(10);

        mAdapter = new ImagesAdapter();
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnClickListener(view -> finish());

        updateSelectIndex(mPosition);

        if (mPosition < mImages.size()) {
            mViewPager.setCurrentItem(mPosition);
        }

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {
                updateSelectIndex(arg0);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }

    private void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(OtherBroadcast.singledown);
        filter.addAction(OtherBroadcast.longpress);
        registerReceiver(my_broadcastReceiver, filter);
    }

    private void doFinish() {
        if (mChangeSelected) {
            Intent intent = new Intent();
            ArrayList<String> resultImages = null;
            if (mRemovePosition.size() == 0) {
                resultImages = mImages;
            } else {
                resultImages = new ArrayList<String>();
                for (int i = 0; i < mImages.size(); i++) {
                    if (!isInRemoveList(i)) {
                        resultImages.add(mImages.get(i));
                    }
                }
            }
            intent.putExtra(AppConstant.EXTRA_IMAGES, resultImages);
            setResult(RESULT_OK, intent);
        }
        finish();
        overridePendingTransition(0, 0);// 关闭过场动画
    }

    public void updateSelectIndex(final int index) {
        if (mPosition >= mImages.size()) {
            mIndexCountTv.setText(null);
        } else {
            mRealImageUrl = mImages.get(index);
            mIndexCountTv.setText((index + 1) + "/" + mImages.size());
        }

        if (!mChangeSelected) {
            mCheckBox.setVisibility(View.GONE);
            return;
        }

        mCheckBox.setOnCheckedChangeListener(null);
        boolean removed = isInRemoveList(index);
        if (removed) {
            mCheckBox.setChecked(false);
        } else {
            mCheckBox.setChecked(true);
        }
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    removeFromRemoveList(index);
                } else {
                    addInRemoveList(index);
                }
            }
        });
    }

    void addInRemoveList(int position) {
        if (!isInRemoveList(position)) {
            mRemovePosition.add(Integer.valueOf(position));
        }
    }

    void removeFromRemoveList(int position) {
        if (isInRemoveList(position)) {
            mRemovePosition.remove(Integer.valueOf(position));
        }
    }

    boolean isInRemoveList(int position) {
        return mRemovePosition.indexOf(Integer.valueOf(position)) != -1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_EDIT:
                    mRealImageUrl = mEditedPath;
                    mImages.set(mViewPager.getCurrentItem(), mEditedPath);
                    // 刷新当前页面，
                    mAdapter.refreshCurrent();
                    // 模拟那个长按，弹出菜单，
                    Intent intent = new Intent(OtherBroadcast.longpress);
                    sendBroadcast(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void collection(String emoji) {
        Map<String, String> params = new HashMap<>();
        if (FileUtil.isExist(mImages.get(mViewPager.getCurrentItem()))
                && TextUtils.isEmpty(emoji)) {
            // 编辑的图片，先上传，得到新url，在调用收藏方法，传入封装好的参数
            PlayUploadUtil.uploadSingle(mContext, mEditedPath, new PlayUploadUtil.UploadSingleCallback() {
                @Override
                public void uploadSingleFail() {
                    ToastUtil.showToast(mContext, getString(R.string.upload_failed));
                }

                @Override
                public void uploadSingleSuccess(String result) {
                    com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
                    com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
                    json.put("type", String.valueOf(CollectionEvery.TYPE_IMAGE));
                    json.put("msg", result);
                    json.put("collectType", -1);// 与消息无关的收藏
                    array.add(json);
                    collection(JSON.toJSONString(array));
                }
            });
            return;
        }
        if (TextUtils.isEmpty(emoji)) {
            emoji = collectionParam(publicMessage);
        }
        params.put("emoji", emoji);

        HttpUtils.post().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, mContext.getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private String collectionParam(PublicMessage message) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = CollectionEvery.TYPE_IMAGE_SINGLE;
        String id = message.getMessageId();
        json.put("type", String.valueOf(type));
        json.put("msg", clone.get(mViewPager.getCurrentItem()));
        json.put("url", clone.get(mViewPager.getCurrentItem()));
        json.put("collectType", 1);
        json.put("collectMsgId", id);
        json.put("toUserId", message.getUserId());
        json.put("targetType", 1); // 收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊

        array.add(json);
        return JSON.toJSONString(array);
    }

    class My_BroadcastReceivers extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(OtherBroadcast.singledown)) {
                doFinish();
            } else if (intent.getAction().equals(OtherBroadcast.longpress)) {
                // 长按屏幕，弹出菜单
                if (TextUtils.isEmpty(imageUrl)) {
                    Toast.makeText(MultiImagePreviewActivity.this, getString(R.string.image_is_null), Toast.LENGTH_SHORT).show();
                    return;
                }
                ImageLoadHelper.loadBitmapDontAnimate(mContext, mRealImageUrl, b -> {
                    String text = BitmapUtil.getTextQRCode(MultiImagePreviewActivity.this, b);
                    boolean isShowCollection = publicMessage != null;
                    if (isMyCollection) {
                        isShowCollection = false;
                    }
                    mSaveWindow = new SaveWindow(MultiImagePreviewActivity.this
                            , publicMessage != null
                            , isShowCollection
                            , !TextUtils.isEmpty(text), new ClickListener(text));
                    mSaveWindow.show();
                }, e -> {
                    // todo load bitmap failed
                });
            }
        }
    }

    class ClickListener implements View.OnClickListener {

        private String text;

        public ClickListener(String text) {
            this.text = text;
        }

        @Override
        public void onClick(View v) {
            mSaveWindow.dismiss();
            switch (v.getId()) {
                case R.id.tv_send_to_friend:
                    if (FileUtil.isExist(mImages.get(mViewPager.getCurrentItem()))) {
                        // 编辑的图片，先上传，得到新url，在发送
                        PlayUploadUtil.uploadSingle(mContext, mEditedPath, new PlayUploadUtil.UploadSingleCallback() {
                            @Override
                            public void uploadSingleFail() {
                                ToastUtil.showToast(mContext, getString(R.string.upload_failed));
                            }

                            @Override
                            public void uploadSingleSuccess(String result) {
                                MessageUtil.generateImageMessage2Intent(mContext, coreManager.getSelf().getUserId(), result);
                            }
                        });
                    } else {
                        // 未编辑的图片，直接发送原url
                        MessageUtil.generateImageMessage2Intent(mContext, coreManager.getSelf().getUserId(), mImages.get(mViewPager.getCurrentItem()));
                    }
                    break;
                case R.id.tv_collection:
                    collection("");
                    break;
                case R.id.save_image:
                    FileUtil.downImageToGallery(MultiImagePreviewActivity.this, mRealImageUrl);
                    break;
                case R.id.edit_image:
                    ImageLoadHelper.loadFile(
                            MultiImagePreviewActivity.this,
                            mRealImageUrl,
                            f -> {
                                mEditedPath = FileUtil.createImageFileForEdit().getAbsolutePath();
                                IMGEditActivity.startForResult(MultiImagePreviewActivity.this, Uri.fromFile(f), mEditedPath, REQUEST_IMAGE_EDIT);
                            }
                    );
                    break;
                case R.id.identification_qr_code:
                    // 识别图中二维码
                    if (TextUtils.isEmpty(text)) {// 理论上不太可能了，因为该item显示时，bitmap都不为空
                        Toast.makeText(MultiImagePreviewActivity.this, R.string.unrecognized, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    HandleQRCodeScanUtil.handleScanResult(mContext, text);
                    break;
            }
        }
    }

    class ImagesAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void refreshCurrent() {
            AvatarHelper.getInstance().displayUrl(mRealImageUrl, (ZoomImageView) mViews.get(mViewPager.getCurrentItem()));
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View view = mViews.get(position);
            if (view == null) {
                view = new ZoomImageView(MultiImagePreviewActivity.this);
                mViews.put(position, view);
            }
            // init status
            imageUrl = mImages.get(position);

            // copy from com.chat.weichat.ui.tool.SingleImagePreviewActivity.initView
            ImageView mImageView = (ImageView) view;
            String mImageUri = imageUrl;
            // 网络加载
            if (mImageUri.endsWith(".gif")) {
                ImageLoadHelper.showGifWithError(
                        mContext,
                        mImageUri,
                        R.drawable.image_download_fail_icon,
                        mImageView
                );
            } else {
                ImageLoadHelper.loadBitmapCenterCropDontAnimateWithError(
                        mContext,
                        mImageUri,
                        R.drawable.image_download_fail_icon,
                        b -> {
                            mImageView.setImageBitmap(b);
                        }, e -> {
                            mImageView.setImageResource(R.drawable.image_download_fail_icon);
                        });
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
}
