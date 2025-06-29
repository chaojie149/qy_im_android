package com.tongxin.caihong.ui.message.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatOverviewActivity;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.video.ChatVideoPreviewActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 查找指定内容
 * 图片与视频
 */
public class SearchImageVideoContent extends BaseActivity {

    public static int TYPE_IMAGE = 0;
    public static int TYPE_VIDEO = 1;

    private int mSearchType;
    private String mSearchObject;
    private RecyclerView mRecyclerView;
    private ImageVideoAdapter mImageVideoAdapter;
    private List<ChatMessage> imageList = new ArrayList<>();
    private List<ChatMessage> mChatMessage = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_image_video_content);
        mSearchType = getIntent().getIntExtra("search_type", TYPE_IMAGE);
        mSearchObject = getIntent().getStringExtra("search_objectId");

        if (mSearchType == TYPE_IMAGE) {
            // 全局变量，用于滑动展示
            imageList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_IMAGE);
            // 根据timeSend排序
            Comparator<ChatMessage> comparator = (o1, o2) -> (int) (o2.getDoubleTimeSend() - o1.getDoubleTimeSend());
            Collections.sort(imageList, comparator);
            mChatMessage.addAll(imageList);
        } else {
            List<ChatMessage> videoList = ChatMessageDao.getInstance().queryChatMessageByType(coreManager.getSelf().getUserId(), mSearchObject, XmppMessage.TYPE_VIDEO);
            // 根据timeSend排序
            Comparator<ChatMessage> comparator = (o1, o2) -> (int) (o2.getDoubleTimeSend() - o1.getDoubleTimeSend());
            Collections.sort(videoList, comparator);
            mChatMessage.addAll(videoList);
        }

        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        if (mSearchType == TYPE_IMAGE) {
            tvTitle.setText(getString(R.string.s_image));
        } else {
            tvTitle.setText(getString(R.string.s_video));
        }
    }

    private void initView() {
        mRecyclerView = findViewById(R.id.s_image_video_rcy);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        mImageVideoAdapter = new ImageVideoAdapter(mChatMessage);
        mRecyclerView.setAdapter(mImageVideoAdapter);
    }

    class ImageVideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<ChatMessage> mChatMessageSource;

        public ImageVideoAdapter(List<ChatMessage> chatMessages) {
            this.mChatMessageSource = chatMessages;
            if (mChatMessageSource == null) {
                mChatMessageSource = new ArrayList<>();
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ImageVideoHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_image_video, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            ChatMessage chatMessage = mChatMessageSource.get(position);
            boolean isExist = false;
            String filePath = UploadCacheUtils.get(mContext, chatMessage);
            if (!TextUtils.isEmpty(filePath) && FileUtil.isExist(filePath)) {
                isExist = true;
            }
            if (chatMessage.getType() == XmppMessage.TYPE_IMAGE) {
                ((ImageVideoHolder) viewHolder).mVideoIv.setVisibility(View.GONE);
                if (isExist) {
                    ImageLoadHelper.showFileWithError(
                            mContext,
                            new File(filePath),
                            R.drawable.image_download_fail_icon,
                            ((ImageVideoHolder) viewHolder).mIv
                    );
                } else {
                    ImageLoadHelper.showImageWithError(
                            mContext,
                            chatMessage.getContent(),
                            R.drawable.image_download_fail_icon,
                            ((ImageVideoHolder) viewHolder).mIv
                    );
                }
            } else {
                ((ImageVideoHolder) viewHolder).mVideoIv.setVisibility(View.VISIBLE);
                if (isExist) {
                    AvatarHelper.getInstance().asyncDisplayVideoThumb(filePath, (((ImageVideoHolder) viewHolder).mIv));
                } else {
                    AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(chatMessage.getContent(), (((ImageVideoHolder) viewHolder).mIv));
                }
            }

            ((ImageVideoHolder) viewHolder).mContentRl.setOnClickListener(v -> {
                if (chatMessage.getType() == XmppMessage.TYPE_IMAGE) {
                    int imageChatMessageList_current_position = 0;
                    for (int i = 0; i < imageList.size(); i++) {
                        if (imageList.get(i).getPacketId().equals(chatMessage.getPacketId())) {
                            imageChatMessageList_current_position = i;
                        }
                    }
                    Intent intent = new Intent(mContext, ChatOverviewActivity.class);
                    // intent.putExtra("imageChatMessageList", JSON.toJSONString(imageList));
                    ChatOverviewActivity.imageChatMessageListStr = JSON.toJSONString(imageList);
                    intent.putExtra("imageChatMessageList_current_position", imageChatMessageList_current_position);
                    Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), mSearchObject);
                    if (friend != null) {
                        intent.putExtra("isGroup", friend.getRoomFlag() != 0);
                    } else {
                        intent.putExtra("isGroup", false);
                    }
                    intent.putExtra("isTargetChat", true);
                    intent.putExtra("chatId", mSearchObject);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(mContext, ChatVideoPreviewActivity.class);
                    intent.putExtra(AppConstant.EXTRA_VIDEO_FILE_PATH, (!TextUtils.isEmpty(filePath) && FileUtil.isExist(filePath)) ?
                            filePath : chatMessage.getContent());
                    intent.putExtra("json", JSON.toJSONString(chatMessage));
                    Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), mSearchObject);
                    if (friend != null) {
                        intent.putExtra("isGounp", friend.getRoomFlag() != 0);
                    } else {
                        intent.putExtra("isGounp", false);
                    }
                    intent.putExtra("isTargetChat", true);
                    intent.putExtra("chatId", mSearchObject);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mChatMessageSource.size();
        }
    }

    class ImageVideoHolder extends RecyclerView.ViewHolder {

        private RelativeLayout mContentRl;
        private ImageView mIv, mVideoIv;

        public ImageVideoHolder(@NonNull View itemView) {
            super(itemView);
            mContentRl = itemView.findViewById(R.id.content_rl);
            mIv = itemView.findViewById(R.id.iv);
            mVideoIv = itemView.findViewById(R.id.video_iv);
        }
    }
}
