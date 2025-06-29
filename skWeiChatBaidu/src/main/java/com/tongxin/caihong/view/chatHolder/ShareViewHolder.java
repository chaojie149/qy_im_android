package com.tongxin.caihong.view.chatHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Share;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.collection.ShareDetailActivity;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

class ShareViewHolder extends AChatHolderInterface {
    private LinearLayout llShareMsg;
    private TextView tvTop;
    private LinearLayout llMiddle;
    private TextView tvMiddle;
    private ImageView ivMiddle;
    private TextView tvBottom;
    private RelativeLayout rlShareVideoMsg;
    private ImageView ivVideoFirstFrame;
    private TextView tvSource;
    private TextView tvContent;
    private TextView tvTimeLen;
    private Share share;
    private List<PublicMessage> data = new ArrayList<>();
    private PublicMessage message;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_share : R.layout.chat_to_item_share;
    }

    @Override
    public void initView(View view) {
        llShareMsg = view.findViewById(R.id.llShareMsg);
        tvTop = view.findViewById(R.id.tvTop);
        llMiddle = view.findViewById(R.id.llMiddle);
        tvMiddle = view.findViewById(R.id.tvMiddle);
        ivMiddle = view.findViewById(R.id.ivMiddle);
        tvBottom = view.findViewById(R.id.tvBottom);
        rlShareVideoMsg = view.findViewById(R.id.rlShareVideoMsg);
        ivVideoFirstFrame = view.findViewById(R.id.ivVideoFirstFrame);
        tvSource = view.findViewById(R.id.tvSource);
        tvContent = view.findViewById(R.id.tvContent);
        tvTimeLen = view.findViewById(R.id.tvTimeLen);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        share = Share.json2Share(message.getContent());
        if (share.getSource() == 0) {
            // 来自生活圈
            llShareMsg.setVisibility(View.VISIBLE);
            rlShareVideoMsg.setVisibility(View.GONE);
            if (share.getType() == 0) {
                // 纯文本
                tvTop.setText(share.getText());

                tvTop.setVisibility(View.VISIBLE);
                llMiddle.setVisibility(View.GONE);
            } else if (share.getType() == 1) {
                // 图片
                if (TextUtils.isEmpty(share.getText())) {
                    tvMiddle.setText(getString(R.string.source_from_people, share.getPublisherName(), getString(R.string.image)));

                    tvTop.setVisibility(View.GONE);
                    tvMiddle.setVisibility(View.VISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                } else {
                    tvTop.setText(share.getText());

                    tvTop.setVisibility(View.VISIBLE);
                    tvMiddle.setVisibility(View.INVISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                }

                ImageLoadHelper.showImageDontAnimateWithError(
                        MyApplication.getContext(),
                        share.getImage(),
                        R.drawable.black_bg,
                        ivMiddle
                );
            } else if (share.getType() == 2) {
                // 语音
                if (TextUtils.isEmpty(share.getText())) {
                    tvMiddle.setText(getString(R.string.source_from_people, share.getPublisherName(), getString(R.string.voice)));

                    tvTop.setVisibility(View.GONE);
                    tvMiddle.setVisibility(View.VISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                } else {
                    tvTop.setText(share.getText());

                    tvTop.setVisibility(View.VISIBLE);
                    tvMiddle.setVisibility(View.INVISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                }
                ivMiddle.setImageResource(R.mipmap.ic_gain_dynamic_voice);
            } else if (share.getType() == 3) {
                // 视频
                if (TextUtils.isEmpty(share.getText())) {
                    tvMiddle.setText(getString(R.string.source_from_people, share.getPublisherName(), getString(R.string.s_video)));
                    tvTop.setVisibility(View.GONE);
                    tvMiddle.setVisibility(View.VISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                } else {
                    tvTop.setText(share.getText());

                    tvTop.setVisibility(View.VISIBLE);
                    tvMiddle.setVisibility(View.INVISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                }
                if (TextUtils.isEmpty(share.getImage())) {
                    AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(share.getVideoUrl(), ivMiddle);
                } else {
                    ImageLoadHelper.showImageDontAnimateWithError(
                            MyApplication.getContext(),
                            share.getImage(),
                            R.drawable.black_bg,
                            ivMiddle);
                }
            } else if (share.getType() == 4) {
                // 文件
                tvMiddle.setText(share.getFileName());
                // ios没传fileType，还是直接从文件名内截图出后缀吧
                // FileUtil.fillTypeIcon(share.getFileType(), ivMiddle);
                int pointIndex = share.getFileName().lastIndexOf(".");
                if (pointIndex != -1) {
                    String type = share.getFileName().substring(pointIndex + 1).toLowerCase();
                    FileUtil.fillTypeIcon(FileUtil.suffix2Type(type), ivMiddle);
                }
                tvTop.setVisibility(View.GONE);
                llMiddle.setVisibility(View.VISIBLE);
            } else if (share.getType() == 5) {
                // 链接
                if (TextUtils.isEmpty(share.getText())) {
                    tvMiddle.setText(getString(R.string.source_from_people, share.getPublisherName(), getString(R.string.link)));

                    tvTop.setVisibility(View.GONE);
                    tvMiddle.setVisibility(View.VISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                } else {
                    tvTop.setText(share.getText());

                    tvTop.setVisibility(View.VISIBLE);
                    tvMiddle.setVisibility(View.INVISIBLE);
                    llMiddle.setVisibility(View.VISIBLE);
                }

                ImageLoadHelper.showImageDontAnimateWithError(
                        MyApplication.getContext(),
                        share.getImage(),
                        R.drawable.black_bg,
                        ivMiddle
                );
            }
            if (share.getCollect() == 1) {
                tvBottom.setText(getString(R.string.source_from_collect));
            }
        } else {
            // 来自视界
            llShareMsg.setVisibility(View.GONE);
            rlShareVideoMsg.setVisibility(View.VISIBLE);
            tvContent.setText(share.getText());
            tvTimeLen.setText(TimeUtils.timeParse(share.getTimeLen() * 1000));
            if (TextUtils.isEmpty(share.getImage())) {
                AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(share.getVideoUrl(), ivVideoFirstFrame);
            } else {
                ImageLoadHelper.showImageDontAnimateWithError(
                        MyApplication.getContext(),
                        share.getImage(),
                        R.drawable.black_bg,
                        ivVideoFirstFrame);
            }
            if (share.getCollect() == 1) {
                tvSource.setText(getString(R.string.source_from_collect));
            }
        }

    }

    @Override
    public void showTime(String time) {

    }

    @Override
    protected void onRootClick(View v) {
        if (share.getSource() == 0) {
            //生活圈
            loadData();
        } else {
            //视界
        }
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    private void loadData() {
        Map<String, String> params = new HashMap<>();
        params.put("messageId", share.getId());

        HttpUtils.get().url(CoreManager.requireConfig(mContext).MSG_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(ObjectResult<PublicMessage> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            if (result.getData() == null) {
                                DialogHelper.tipDialog(mContext, mContext.getString(R.string.tip_share_deleted));
                                return;
                            }
                            data.clear();
                            message = result.getData();
                            if (result.getData() == null) {
                                return;
                            }
                            // 传递文件名，
                            message.setFileName(share.getFileName());
                            data.add(message);
                            ShareDetailActivity.start(mContext, data);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

}
