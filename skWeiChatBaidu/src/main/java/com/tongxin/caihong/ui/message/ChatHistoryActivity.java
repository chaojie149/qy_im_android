package com.tongxin.caihong.ui.message;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.audio_x.VoiceAnimView;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.map.MapActivity;
import com.tongxin.caihong.ui.mucfile.DownManager;
import com.tongxin.caihong.ui.mucfile.MucFileDetails;
import com.tongxin.caihong.ui.mucfile.XfileUtils;
import com.tongxin.caihong.ui.mucfile.bean.MucFileBean;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.util.SmileyParser;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.link.HttpTextView;
import com.tongxin.caihong.video.ChatVideoPreviewActivity;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;

/**
 * 原聊天记录界面，
 * 现调整，与标记界面共用
 */
public class ChatHistoryActivity extends BaseActivity {
    private MessageAdapter messageAdapter;
    private List<ChatMessage> data = new ArrayList<>();
    private String userId;
    private ChatMessage historyMessage;
    private boolean isGroup;

    public static void start(Context cxt, String userId, String msgId, boolean isGroup) {
        Intent intent = new Intent(cxt, ChatHistoryActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(AppConstant.EXTRA_MSG_ID, msgId);
        intent.putExtra("isGroup", isGroup);
        cxt.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);
        userId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        String msgId = getIntent().getStringExtra(AppConstant.EXTRA_MSG_ID);
        historyMessage = ChatMessageDao.getInstance().findMsgById(coreManager.getSelf().getUserId(), userId, msgId);
        if (historyMessage == null) {
            ToastUtil.showToast(mContext, getString(R.string.unknown));
            finish();
            return;
        }
        isGroup = getIntent().getBooleanExtra("isGroup", false);
        initActionBar();
        initView();
        loadData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(historyMessage.getObjectId());
    }

    private void initView() {
        RecyclerView rcyMessage = findViewById(R.id.rcyMessage);
        messageAdapter = new MessageAdapter();
        rcyMessage.setAdapter(messageAdapter);
    }

    private void loadData() {
        String content = historyMessage.getContent();
        try {
            List<String> messageJsonList = JSON.parseArray(content, String.class);
            for (int i = 0; i < messageJsonList.size(); i++) {
                ChatMessage chatMessage = new ChatMessage(messageJsonList.get(i).replaceAll("timeLen", "fileTime"));
                data.add(chatMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        filter();
    }

    private void filter() {
        // 只显示 文本、图片、语音、位置、gif、视频、名片、文件，其余消息类型全部当做文本处理
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getType() != XmppMessage.TYPE_TEXT
                    && data.get(i).getType() != XmppMessage.TYPE_IMAGE
                    && data.get(i).getType() != XmppMessage.TYPE_VOICE
                    && data.get(i).getType() != XmppMessage.TYPE_LOCATION
                    && data.get(i).getType() != XmppMessage.TYPE_GIF
                    && data.get(i).getType() != XmppMessage.TYPE_VIDEO
                    && data.get(i).getType() != XmppMessage.TYPE_CARD
                    && data.get(i).getType() != XmppMessage.TYPE_FILE) {
                data.get(i).setContent(StringUtils.getMessageContent(data.get(i), false));
                data.get(i).setType(XmppMessage.TYPE_TEXT);
            }
        }
        messageAdapter.notifyDataSetChanged();
    }

    class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resource;
            if (viewType == XmppMessage.TYPE_TEXT) {
                resource = R.layout.team_item_message_text;
                return new TextViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_IMAGE) {
                resource = R.layout.team_item_message_image;
                return new ImageViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_VOICE) {
                resource = R.layout.team_item_message_voice;
                return new VoiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_LOCATION) {
                resource = R.layout.team_item_message_location;
                return new LocationViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_GIF) {
                resource = R.layout.team_item_message_gif;
                return new GifViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_VIDEO) {
                resource = R.layout.team_item_message_video;
                return new VideoViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_CARD) {
                resource = R.layout.team_item_message_card;
                return new CardViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else if (viewType == XmppMessage.TYPE_FILE) {
                resource = R.layout.team_item_message_file;
                return new FileViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            } else {
                resource = R.layout.team_item_message_text;
                return new TextViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage chatMessage = data.get(position);
            if (holder instanceof CommonViewHolder) {
                AvatarHelper.getInstance().displayAvatar(chatMessage.getFromUserName(), chatMessage.getFromUserId(), ((CommonViewHolder) holder).ivHead, true);
                // ((CommonViewHolder) holder).ivHead.setOnClickListener(view -> BasicInfoActivity.start(mContext, chatMessage.getFromUserId()));
                ((CommonViewHolder) holder).tvName.setText(chatMessage.getFromUserName());
                ((CommonViewHolder) holder).tvTime.setText(getString(R.string.team_mark_tip3, TimeUtils.getFriendlyTimeDesc(mContext, chatMessage.getTimeSend())));
                ((CommonViewHolder) holder).ivMore.setVisibility(View.GONE);
                if (holder instanceof TextViewHolder) {
                    String s = StringUtils.replaceSpecialChar(chatMessage.getContent());
                    CharSequence charSequence = HtmlUtils.transform200SpanString(s, true);
                    ((TextViewHolder) holder).tvContent.setUrlText(charSequence);
                } else if (holder instanceof ImageViewHolder) {
                    String url = chatMessage.getContent();
                    if (!TextUtils.isEmpty(chatMessage.getFilePath())
                            && FileUtil.isExist(chatMessage.getFilePath())) {
                        url = chatMessage.getFilePath();
                    }
                    changeImageLayoutSize(chatMessage, ((ImageViewHolder) holder).ivImage);
                    ImageLoadHelper.showImageWithPlaceHolder(
                            mContext,
                            url,
                            R.drawable.ffb,
                            R.drawable.fez,
                            ((ImageViewHolder) holder).ivImage
                    );
                    ((ImageViewHolder) holder).ivImage.setOnClickListener(view -> {
                        Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                        intent.putExtra(AppConstant.EXTRA_IMAGE_URI, chatMessage.getContent());
                        startActivity(intent);
                    });
                } else if (holder instanceof VoiceViewHolder) {
                    ((VoiceViewHolder) holder).vaAudio.fillData(chatMessage);
                    ((VoiceViewHolder) holder).vaAudio.setOnClickListener(v -> VoicePlayer.instance().playVoice(((VoiceViewHolder) holder).vaAudio));
                } else if (holder instanceof LocationViewHolder) {
                    ImageLoadHelper.showImageWithPlaceHolder(
                            mContext,
                            chatMessage.getContent(),
                            R.drawable.chat_map_bg,
                            R.drawable.chat_map_bg,
                            ((LocationViewHolder) holder).ivLocation
                    );
                    ((LocationViewHolder) holder).tvLocation.setText(chatMessage.getObjectId());
                    ((LocationViewHolder) holder).llLocation.setOnClickListener(view -> {
                        Intent intent = new Intent(mContext, MapActivity.class);
                        intent.putExtra("latitude", Double.valueOf(chatMessage.getLocation_x()));
                        intent.putExtra("longitude", Double.valueOf(chatMessage.getLocation_y()));
                        intent.putExtra("address", chatMessage.getObjectId());
                        startActivity(intent);
                    });
                } else if (holder instanceof GifViewHolder) {
                    int resId = SmileyParser.Gifs.textMapId(chatMessage.getContent());
                    ((GifViewHolder) holder).gifImage.setImageResource(resId != -1 ? resId : null);
                } else if (holder instanceof VideoViewHolder) {
                    String url = chatMessage.getContent();
                    if (!TextUtils.isEmpty(chatMessage.getFilePath())
                            && FileUtil.isExist(chatMessage.getFilePath())) {
                        url = chatMessage.getFilePath();
                    }
                    if (!FileUtil.isExist(chatMessage.getFilePath())) {
                        AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(url, ((VideoViewHolder) holder).ivVideo);
                    } else {
                        AvatarHelper.getInstance().asyncDisplayVideoThumb(url, ((VideoViewHolder) holder).ivVideo);
                    }
                    final String intentUrl = url;
                    ((VideoViewHolder) holder).ivPlay.setOnClickListener(view -> {
                        Intent intent = new Intent(mContext, ChatVideoPreviewActivity.class);
                        intent.putExtra(AppConstant.EXTRA_VIDEO_FILE_PATH, intentUrl);
                        intent.putExtra("json", JSON.toJSONString(chatMessage));
                        intent.putExtra("isGounp", isGroup);
                        startActivity(intent);
                    });
                } else if (holder instanceof CardViewHolder) {
                    AvatarHelper.getInstance().displayAvatar(chatMessage.getContent(), chatMessage.getObjectId(), ((CardViewHolder) holder).ivCardHead, true);
                    ((CardViewHolder) holder).tvCardName.setText(String.valueOf(chatMessage.getContent()));
                    ((CardViewHolder) holder).llCard.setOnClickListener(view -> BasicInfoActivity.start(mContext, chatMessage.getObjectId(), BasicInfoActivity.FROM_ADD_TYPE_CARD));
                } else if (holder instanceof FileViewHolder) {
                    String fileName = TextUtils.isEmpty(chatMessage.getFilePath()) ? chatMessage.getContent() : chatMessage.getFilePath();
                    if (TextUtils.isEmpty(fileName)) {
                        return;
                    }
                    int pointIndex = fileName.lastIndexOf(".");
                    String fileType = "";
                    if (pointIndex != -1) {
                        fileType = fileName.substring(pointIndex + 1).toLowerCase();
                        if ((fileType.equals("png") || fileType.equals("jpg") || fileType.equals("gif"))) {
                            ImageLoadHelper.showImageWithSize(
                                    mContext,
                                    fileName,
                                    100, 100,
                                    ((FileViewHolder) holder).ivFileType
                            );
                        } else {
                            FileUtil.fillTypeIcon(FileUtil.suffix2Type(fileType), ((FileViewHolder) holder).ivFileType);
                        }
                    }

                    int start = fileName.lastIndexOf("/");
                    String name = fileName.substring(start + 1).toLowerCase();
                    ((FileViewHolder) holder).tvFileName.setText(name);
                    ((FileViewHolder) holder).tvFileSize.setText(XfileUtils.fromatSize(chatMessage.getFileSize()));
                    int finalType = XfileUtils.getFileType(fileType);
                    ((FileViewHolder) holder).llFile.setOnClickListener(view -> {
                        if (com.tongxin.caihong.view.chatHolder.FileViewHolder.checkFilePassword(mContext, chatMessage,
                                () -> openFile(chatMessage, finalType, name))) {
                            return;
                        }
                        openFile(chatMessage, finalType, name);
                    });
                }
            }
        }

        private void openFile(ChatMessage chatMessage, int type, String fileName) {
            MucFileBean data = new MucFileBean();
            data.setType(type);
            data.setNickname(chatMessage.getFromUserName());
            data.setUrl(chatMessage.getContent());
            data.setName(fileName);
            data.setSize(chatMessage.getFileSize());
            data.setState(DownManager.STATE_UNDOWNLOAD);

            Intent intent = new Intent(mContext, MucFileDetails.class);
            intent.putExtra("data", data);
            mContext.startActivity(intent);
        }

        private void changeImageLayoutSize(ChatMessage message, ImageView imageView) {
            ViewGroup.LayoutParams params = imageView.getLayoutParams();

            if (TextUtils.isEmpty(message.getLocation_x())
                    || TextUtils.isEmpty(message.getLocation_y())) {
                params.width = ScreenUtil.dip2px(mContext, 120);
                params.height = ScreenUtil.dip2px(mContext, 120);
            } else {
                float imageWidth = Float.parseFloat(message.getLocation_x());
                float imageHeight = Float.parseFloat(message.getLocation_y());

                if (imageWidth == 0f
                        || imageHeight == 0f) {
                    // 以防万一，
                    params.width = ScreenUtil.dip2px(mContext, 120);
                    params.height = ScreenUtil.dip2px(mContext, 120);
                } else {
                    // 基于宽度进行缩放,三挡:宽图 55/100,窄图100/55
                    // float width = image_width / image_height < 0.4 ? IMAGE_MIN_SIZE : IMAGE_MAX_SIZE;
                    // float height = width == IMAGE_MAX_SIZE ? Math.max(width / image_width * image_height, IMAGE_MIN_SIZE) : IMAGE_MAX_SIZE;

                    // https://www.jianshu.com/p/773b87e3356b
                    // 实测，宽短高长的图片显示效果不是很好，自己添加一个缩放比例参数来控制
                    double zoom = 3.0;
                    float width = imageWidth;
                    float height = imageHeight;
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

                    params.width = ScreenUtil.dip2px(mContext, width);
                    params.height = ScreenUtil.dip2px(mContext, height);
                }
            }
            imageView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public int getItemViewType(int position) {
            return data.get(position).getType();
        }

        class CommonViewHolder extends RecyclerView.ViewHolder {
            ImageView ivHead;
            TextView tvName;
            TextView tvTime;
            ImageView ivMore;

            public CommonViewHolder(View itemView) {
                super(itemView);
                ivHead = itemView.findViewById(R.id.ivHead);
                tvName = itemView.findViewById(R.id.tvName);
                tvTime = itemView.findViewById(R.id.tvTime);
                ivMore = itemView.findViewById(R.id.ivMore);
            }
        }

        class TextViewHolder extends CommonViewHolder {
            HttpTextView tvContent;

            public TextViewHolder(View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tvContent);
            }
        }

        class ImageViewHolder extends CommonViewHolder {
            RoundedImageView ivImage;

            public ImageViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivImage);
            }
        }

        class VoiceViewHolder extends CommonViewHolder {
            VoiceAnimView vaAudio;

            public VoiceViewHolder(View itemView) {
                super(itemView);
                vaAudio = itemView.findViewById(R.id.vaAudio);
            }
        }

        class LocationViewHolder extends CommonViewHolder {
            LinearLayout llLocation;
            ImageView ivLocation;
            TextView tvLocation;

            public LocationViewHolder(View itemView) {
                super(itemView);
                llLocation = itemView.findViewById(R.id.llLocation);
                ivLocation = itemView.findViewById(R.id.ivLocation);
                tvLocation = itemView.findViewById(R.id.tvLocation);
            }
        }

        class GifViewHolder extends CommonViewHolder {
            GifImageView gifImage;

            public GifViewHolder(View itemView) {
                super(itemView);
                gifImage = itemView.findViewById(R.id.gifImage);
            }
        }

        class VideoViewHolder extends CommonViewHolder {
            ImageView ivVideo;
            ImageView ivPlay;

            public VideoViewHolder(View itemView) {
                super(itemView);
                ivVideo = itemView.findViewById(R.id.ivVideo);
                ivPlay = itemView.findViewById(R.id.ivPlay);
            }
        }

        class CardViewHolder extends CommonViewHolder {
            LinearLayout llCard;
            ImageView ivCardHead;
            TextView tvCardName;

            public CardViewHolder(View itemView) {
                super(itemView);
                llCard = itemView.findViewById(R.id.llCard);
                ivCardHead = itemView.findViewById(R.id.ivCardHead);
                tvCardName = itemView.findViewById(R.id.tvCardName);
            }
        }

        class FileViewHolder extends CommonViewHolder {
            LinearLayout llFile;
            ImageView ivFileType;
            TextView tvFileName;
            TextView tvFileSize;

            public FileViewHolder(View itemView) {
                super(itemView);
                llFile = itemView.findViewById(R.id.llFile);
                ivFileType = itemView.findViewById(R.id.ivFileType);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
            }
        }
    }
}
