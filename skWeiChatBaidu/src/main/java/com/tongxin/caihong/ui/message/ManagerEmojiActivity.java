package com.tongxin.caihong.ui.message;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.collection.Collectiion;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.util.CameraUtil;
import com.tongxin.caihong.util.PlayUploadUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
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
 * Created by Administrator on 2018/3/9 0009.
 */
public class ManagerEmojiActivity extends BaseActivity {
    private static final int REQUEST_CODE_PICK_CROP_PHOTO = 1;
    private RecyclerView rcyv;
    private MyAdapter mAdapter;
    private List<Collectiion> mData;
    private List<String> mReadyData = new ArrayList<>();
    private TextView tv1, tv2, tv3;
    private boolean isEditOrSee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_emoji);
        mData = new ArrayList<>(MyApplication.mCollection);
        List<Collectiion> dataRemove = new ArrayList<>();
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).getType() == 7
                    || mData.get(i).getType() == 9
                    || mData.get(i).getType() == 10) {
                dataRemove.add(mData.get(i));
            }
        }
        mData.removeAll(dataRemove);

        initActionBar();
        initView();
        initEvent();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.title_my_collection_emoji);

        ImageView ivTitleRightRight = findViewById(R.id.iv_title_right_right);
        ivTitleRightRight.setImageResource(R.mipmap.more_icon);
        ivTitleRightRight.setOnClickListener(view -> CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_CROP_PHOTO));
        ivTitleRightRight.setVisibility(View.VISIBLE);

        TextView tvTitleRight = findViewById(R.id.tv_title_right);
        tvTitleRight.setText(R.string.edit);
        tvTitleRight.setOnClickListener(v -> {
            isEditOrSee = !isEditOrSee;
            if (isEditOrSee) {
                findViewById(R.id.rl_rl).setVisibility(View.VISIBLE);
                tvTitleRight.setText(R.string.cancel);
            } else {
                findViewById(R.id.rl_rl).setVisibility(View.GONE);
                tvTitleRight.setText(R.string.edit);
                updateData(false);
            }
        });
    }

    private void initView() {
        rcyv = findViewById(R.id.emoji_recycle);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 4);
        rcyv.setLayoutManager(layoutManager);
        mAdapter = new MyAdapter();
        rcyv.setAdapter(mAdapter);

        tv1 = findViewById(R.id.al_tv);
        tv2 = findViewById(R.id.sl_tv);
        tv3 = findViewById(R.id.dl_tv);
    }

    private void initEvent() {
        mAdapter.setmOnItemClickListener((view, position) -> {
            Collectiion C = mData.get(position);
            if (!isEditOrSee) {
                Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                intent.putExtra(AppConstant.EXTRA_IMAGE_URI, C.getUrl());
                mContext.startActivity(intent);
            } else {
                updateSingleData(C, position);
            }
        });

        tv1.setOnClickListener(v -> updateData(true));

        tv3.setOnClickListener(v -> {
            DialogHelper.showDefaulteMessageProgressDialog(ManagerEmojiActivity.this);
            String idList = "";
            for (int i = 0; i < mReadyData.size(); i++) {
                if (i == mReadyData.size() - 1) {
                    // deleteMyCollection(mReadyData.get(i), true);
                    idList += mReadyData.get(i);
                } else {
                    // deleteMyCollection(mReadyData.get(i), false);
                    idList += mReadyData.get(i) + ",";
                }
            }
            deleteMyCollection(idList);
        });
    }

    // 更新单个item
    public void updateSingleData(Collectiion C, int position) {
        if (C.getType() == 8) {
            C.setType(0);
            mReadyData.remove(C.getEmojiId());
        } else {
            C.setType(8);
            mReadyData.add(C.getEmojiId());
        }
        mData.remove(position);
        mData.add(position, C);
        mAdapter.notifyItemChanged(position);
        updateUI();
    }

    // 更新所有item 全选 || 取消
    public void updateData(boolean isAllSelectOrCancel) {
        mReadyData.clear();
        for (int i = 0; i < mData.size(); i++) {
            Collectiion C = mData.get(i);
            if (isAllSelectOrCancel) {
                C.setType(8);
                mReadyData.add(mData.get(i).getEmojiId());
            } else {
                C.setType(0);
            }
            mData.remove(i);
            mData.add(i, C);
        }
        mAdapter.notifyDataSetChanged();
        updateUI();
    }

    // 更新底部ui
    public void updateUI() {
        if (mReadyData != null) {
            if (mReadyData.size() > 0) {
                tv2.setText("选中表情 (" + mReadyData.size() + ")");
                tv3.setVisibility(View.VISIBLE);
            } else {
                tv2.setText("选中表情 (" + 0 + ")");
                tv3.setVisibility(View.GONE);
            }
        }
    }

    public void deleteMyCollection(String emojiId) {
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emojiId", emojiId);

        HttpUtils.get().url(coreManager.getConfig().Collection_REMOVE)
                .params(params)
                .build()
                .execute(new BaseCallback<Collectiion>(Collectiion.class) {

                    @Override
                    public void onResponse(ObjectResult<Collectiion> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, mContext.getString(R.string.delete_all_succ), Toast.LENGTH_SHORT).show();
                            for (int i = 0; i < mReadyData.size(); i++) {
                                for (int i1 = 0; i1 < mData.size(); i1++) {
                                    if (mReadyData.get(i).equals(mData.get(i1).getEmojiId())) {
                                        mData.remove(i1);
                                    }
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                            mReadyData.clear();
                            updateUI();
                            ManagerEmojiActivity.this.sendBroadcast(new Intent(OtherBroadcast.CollectionRefresh));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 添加为表情 && 收藏
     * 添加为表情Type 6.表情
     * 收藏Type    1.图片 2.视频 3.文件 4.语音 5.文本
     */
    public void collectionEmotion(String url) {
        Map<String, String> params = new HashMap<>();
        params.put("emoji", collectionParam(url));

        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Collectiion>(Collectiion.class) {

                    @Override
                    public void onResponse(ObjectResult<Collectiion> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, getString(R.string.add_emotion_success), Toast.LENGTH_SHORT).show();
                            mData.add(0, result.getData());
                            mAdapter.notifyDataSetChanged();
                            updateUI();
                            MyApplication.getInstance().sendBroadcast(new Intent(OtherBroadcast.CollectionRefresh));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private String collectionParam(String url) {
        JSONArray array = new JSONArray();
        int type = 6;
        JSONObject json = new JSONObject();
        json.put("type", String.valueOf(type));
        json.put("msg", url);
        // 表情url
        json.put("url", url);
        array.add(json);

        return JSON.toJSONString(array);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_CROP_PHOTO) {
            // 选择一张图片返回再去裁减
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    DialogHelper.showDefaulteMessageProgressDialog(mContext);
                    PlayUploadUtil.uploadSingle(mContext, CameraUtil.parsePickImageResult(data), new PlayUploadUtil.UploadSingleCallback() {
                        @Override
                        public void uploadSingleFail() {
                            DialogHelper.dismissProgressDialog();
                            ToastUtil.showToast(mContext, getString(R.string.upload_failed));
                        }

                        @Override
                        public void uploadSingleSuccess(String result) {
                            collectionEmotion(result);
                        }
                    });
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        }
    }

    interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private OnItemClickListener onItemClickListener;

        public void setmOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_ma_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Collectiion collection = mData.get(position);
            if (collection.getUrl() == null) return;
            if (collection.getUrl().endsWith(".gif")) {
                holder.iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                ImageLoadHelper.showGif(
                        ManagerEmojiActivity.this,
                        collection.getUrl(),
                        holder.iv
                );
            } else {
                holder.iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ImageLoadHelper.showImageDontAnimateWithPlaceHolder(
                        ManagerEmojiActivity.this,
                        collection.getUrl(),
                        R.drawable.ffb,
                        R.drawable.fez,
                        holder.iv
                );
            }

            if (collection.getType() == 8) {
                holder.ck.setVisibility(View.VISIBLE);
                holder.iv.setAlpha(0.4f);
            } else {
                holder.ck.setVisibility(View.GONE);
                holder.iv.setAlpha(1.0f);
            }

            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position1 = holder.getLayoutPosition();
                    onItemClickListener.onItemClick(holder.itemView, position1);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData == null ? 0 : mData.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iv;
            CheckBox ck;

            public ViewHolder(View itemView) {
                super(itemView);
                ck = (CheckBox) itemView.findViewById(R.id.cl_ck);
                iv = (ImageView) itemView.findViewById(R.id.cl_iv);
            }
        }
    }
}
