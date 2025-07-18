package com.tongxin.caihong.adapter;

import static com.tongxin.caihong.bean.circle.PublicMessage.CIRCLE;
import static com.tongxin.caihong.bean.circle.PublicMessage.OTHER;
import static com.tongxin.caihong.bean.circle.PublicMessage.ROOM;
import static com.tongxin.caihong.bean.circle.PublicMessage.SINGLE;
import static com.tongxin.caihong.bean.circle.PublicMessage.VIDEO;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.audio.AudioPalyer;
import com.tongxin.caihong.audio_x.VoiceAnimView;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Report;
import com.tongxin.caihong.bean.ad.Advertising;
import com.tongxin.caihong.bean.circle.Comment;
import com.tongxin.caihong.bean.circle.Praise;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.circle.PublicMessage.Body;
import com.tongxin.caihong.bean.circle.PublicMessage.Resource;
import com.tongxin.caihong.bean.collection.Collectiion;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.collection.Message;
import com.tongxin.caihong.bean.event.EventPublicMessageRemoved;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.CircleMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.circle.BusinessCircleActivity;
import com.tongxin.caihong.ui.circle.BusinessCircleActivity.ListenerAudio;
import com.tongxin.caihong.ui.circle.LongTextShowActivity;
import com.tongxin.caihong.ui.circle.MessageEventComment;
import com.tongxin.caihong.ui.circle.MessageEventReply;
import com.tongxin.caihong.ui.circle.range.PraiseListActivity;
import com.tongxin.caihong.ui.circle.showCEView;
import com.tongxin.caihong.ui.map.MapActivity;
import com.tongxin.caihong.ui.me.collection.CollectionSearchActivity;
import com.tongxin.caihong.ui.me.collection.MyCollection;
import com.tongxin.caihong.ui.me.collection.PublicMessageEditWindow;
import com.tongxin.caihong.ui.mucfile.DownManager;
import com.tongxin.caihong.ui.mucfile.MucFileDetails;
import com.tongxin.caihong.ui.mucfile.XfileUtils;
import com.tongxin.caihong.ui.mucfile.bean.MucFileBean;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.ui.tool.MultiImagePreviewActivity;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.LinkMovementClickMethod;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.SystemUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.util.filter.EmojiInputFilter;
import com.tongxin.caihong.util.link.HttpTextView;
import com.tongxin.caihong.video.PlayVideoActivity;
import com.tongxin.caihong.view.BlockLinearLayout;
import com.tongxin.caihong.view.CheckableImageView;
import com.tongxin.caihong.view.MyGridView;
import com.tongxin.caihong.view.ReportDialog;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.ad.AdView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.MessageEventPublic;
import fm.jiecao.jcvideoplayer_lib.MessageEventVideo;
import okhttp3.Call;

/**
 * 1.我的空间
 * 2.我的收藏 adapter
 */
public class PublicMessageRecyclerAdapter extends RecyclerView.Adapter<PublicMessageRecyclerAdapter.BaseViewHolder> implements ListenerAudio {
    public static final int blur = 30;
    private static final int VIEW_TYPE_NORMAL_TEXT = 0;
    private static final int VIEW_TYPE_NORMAL_SINGLE_IMAGE = 2;
    private static final int VIEW_TYPE_NORMAL_MULTI_IMAGE = 4;
    private static final int VIEW_TYPE_NORMAL_VOICE = 6;
    private static final int VIEW_TYPE_NORMAL_VIDEO = 8;
    private static final int VIEW_TYPE_NORMAL_FILE = 10;
    private static final int VIEW_TYPE_NORMAL_AD = 999;
    // 分享的链接
    private static final int VIEW_TYPE_NORMAL_LINK = 11;
    private Context mContext;
    private CoreManager coreManager;
    private List<PublicMessage> mMessages;
    private LayoutInflater mInflater;
    private String mLoginUserId;
    private String mLoginNickName;
    private ViewHolder mVoicePlayViewHolder;
    private AudioPalyer mAudioPalyer;
    private String mVoicePlayId = null;
    private Map<String, Boolean> mClickOpenMaps = new HashMap<>();
    private int collectionType;
    private boolean isSelect;

    private OnItemClickListener onItemClickListener = null;
    /**
     * 缓存getShowName，
     * 每次约两三毫秒，
     */
    private WeakHashMap<String, String> showNameCache = new WeakHashMap<>();
    private PublicMessageEditWindow publicWindow;
    private showCEView showCEView;
    @Nullable
    private OnMessageActionListener onMessageActionListener;

    public PublicMessageRecyclerAdapter(Context context, CoreManager coreManager, List<PublicMessage> messages) {
        setHasStableIds(true);
        mContext = context;
        this.coreManager = coreManager;
        mMessages = messages;
        publicWindow = new PublicMessageEditWindow(mContext);
        mInflater = LayoutInflater.from(mContext);
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        mAudioPalyer = new AudioPalyer();
        mAudioPalyer.setAudioPlayListener(new AudioPalyer.AudioPlayListener() {
            @Override
            public void onSeekComplete() {
            }

            @Override
            public void onPrepared() {
            }

            @Override
            public void onError() {
                mVoicePlayId = null;
                if (mVoicePlayViewHolder != null) {
                    updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
                }
                mVoicePlayViewHolder = null;
            }

            @Override
            public void onCompletion() {
                mVoicePlayId = null;
                if (mVoicePlayViewHolder != null) {
                    updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
                }
                mVoicePlayViewHolder = null;
            }

            @Override
            public void onBufferingUpdate(int percent) {
            }

            @Override
            public void onPreparing() {
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnMessageActionListener(@Nullable OnMessageActionListener onMessageActionListener) {
        this.onMessageActionListener = onMessageActionListener;
    }

    @Override
    public long getItemId(int position) {
        PublicMessage message = mMessages.get(position);
        if (!TextUtils.isEmpty(message.getMessageId())) {
            return message.getMessageId().hashCode();
        } else if (!TextUtils.isEmpty(message.getEmojiId())) {
            return message.getEmojiId().hashCode();
        } else {
            return position;
        }
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == VIEW_TYPE_NORMAL_AD) {
            return new AdViewHolder(mInflater.inflate(R.layout.p_msg_item_ad_body, viewGroup, false));
        }
        View convertView = mInflater.inflate(R.layout.p_msg_item_main_body, viewGroup, false);
        if (collectionType == 2) {
            // 发送收藏页整个item点击都视为发送收藏，
            ((BlockLinearLayout) convertView).setBlock(true);
        }
        View innerView = null;
        ViewHolder viewHolder;
        if (viewType == VIEW_TYPE_NORMAL_TEXT) {
            viewHolder = new NormalTextHolder(convertView);
        } else if (viewType == VIEW_TYPE_NORMAL_SINGLE_IMAGE) {
            NormalSingleImageHolder holder = new NormalSingleImageHolder(convertView);
            innerView = mInflater.inflate(R.layout.p_msg_item_normal_single_img, holder.content_fl, false);
            holder.image_view = (ImageView) innerView.findViewById(R.id.image_view);
            viewHolder = holder;
        } else if (viewType == VIEW_TYPE_NORMAL_MULTI_IMAGE) {
            NormalMultiImageHolder holder = new NormalMultiImageHolder(convertView);
            innerView = mInflater.inflate(R.layout.p_msg_item_normal_multi_img, holder.content_fl, false);
            holder.grid_view = (MyGridView) innerView.findViewById(R.id.grid_view);
            viewHolder = holder;
        } else if (viewType == VIEW_TYPE_NORMAL_VOICE) {
            NormalVoiceHolder holder = new NormalVoiceHolder(convertView);
            innerView = mInflater.inflate(R.layout.p_msg_item_normal_voice, holder.content_fl, false);
            holder.img_view = (ImageView) innerView.findViewById(R.id.img_view);
            holder.voice_action_img = (ImageView) innerView.findViewById(R.id.voice_action_img);
            holder.voice_desc_tv = (TextView) innerView.findViewById(R.id.voice_desc_tv);
            holder.chat_to_voice = (VoiceAnimView) innerView.findViewById(R.id.chat_to_voice);
            viewHolder = holder;
        } else if (viewType == VIEW_TYPE_NORMAL_VIDEO) {
            NormalVideoHolder holder = new NormalVideoHolder(convertView);
            innerView = mInflater.inflate(R.layout.p_msg_item_normal_video, holder.content_fl, false);
            holder.ivImage = innerView.findViewById(R.id.ivImage);
            viewHolder = holder;
        } else if (viewType == VIEW_TYPE_NORMAL_FILE) {
            NormalFileHolder holder = new NormalFileHolder(convertView);
            innerView = mInflater.inflate(R.layout.p_msg_item_normal_file, holder.content_fl, false);
            holder.file_click = (RelativeLayout) innerView.findViewById(R.id.collection_file);
            holder.file_image = (ImageView) innerView.findViewById(R.id.file_img);
            holder.text_tv = (TextView) innerView.findViewById(R.id.file_name);
            viewHolder = holder;
        } else if (viewType == VIEW_TYPE_NORMAL_LINK) {
            NormalLinkHolder holder = new NormalLinkHolder(convertView);
            innerView = mInflater.inflate(R.layout.p_msg_item_normal_link, holder.content_fl, false);
            holder.link_click = (LinearLayout) innerView.findViewById(R.id.link_ll);
            holder.link_image = (ImageView) innerView.findViewById(R.id.link_iv);
            holder.link_tv = (TextView) innerView.findViewById(R.id.link_text_tv);
            viewHolder = holder;
        } else {
            throw new IllegalStateException("unkown viewType: " + viewType);
        }

        if (collectionType == 1 || collectionType == 2 || collectionType == 3) {
            // 当前适配器用于我的收藏列表，隐藏评论 && 赞功能
            viewHolder.llOperator.setVisibility(View.GONE);
        } else {
            viewHolder.llOperator.setVisibility(View.VISIBLE);
        }
        viewHolder.llPriseAndComment = convertView.findViewById(R.id.llPriseAndComment);
        viewHolder.llPrise = convertView.findViewById(R.id.llPrise);
        viewHolder.multi_praise_tv = (TextView) convertView.findViewById(R.id.multi_praise_tv);
        viewHolder.tvLoadMore = (TextView) convertView.findViewById(R.id.tvLoadMore);
        viewHolder.line_v = convertView.findViewById(R.id.line_v);
        viewHolder.command_listView = (ListView) convertView.findViewById(R.id.command_listView);
        viewHolder.location_tv = (TextView) convertView.findViewById(R.id.location_tv);
        if (innerView != null) {
            viewHolder.content_fl.addView(innerView);
        }
        viewHolder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(viewHolder);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder baseViewHolder, @SuppressLint("RecyclerView") int position) {
        int viewType = getItemViewType(position);
        // set data
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        if (viewType == VIEW_TYPE_NORMAL_AD) {
            AdViewHolder adViewHolder = (AdViewHolder) baseViewHolder;
            adViewHolder.apply(message);
            return;
        }
        ViewHolder viewHolder = (ViewHolder) baseViewHolder;
        // 和ViewHolder一样的，只不过用作匿名内部类里面调用需要final
        final ViewHolder finalHolder = viewHolder;
        if (isSelect) {
            viewHolder.check_box.setVisibility(View.VISIBLE);
            if (MyCollection.deleteIdMap.containsKey(message.getEmojiId())) {
                viewHolder.check_box.setChecked(true);
                ColorStateList tabColor = SkinUtils.getSkin(mContext).getTabColorState();
                Drawable drawable = mContext.getResources().getDrawable(R.drawable.sel_check_wx2);
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(drawable, tabColor);
                viewHolder.check_box.setButtonDrawable(drawable);
            } else {
                viewHolder.check_box.setChecked(false);
                viewHolder.check_box.setButtonDrawable(mContext.getResources().getDrawable(R.drawable.sel_nor_wx2));
            }
        } else {
            viewHolder.check_box.setVisibility(View.GONE);
        }
        publicWindow = new PublicMessageEditWindow(mContext);
        /* 设置头像 */
        if (message.getHiding() == 1 && !TextUtils.equals(mLoginUserId, message.getUserId())) {
            viewHolder.avatar_img.setImageResource(Constants.HIDING_AVATAR);
        } else {
            AvatarHelper.getInstance().displayAvatar(TextUtils.isEmpty(message.getToUserId()) ? message.getUserId() : message.getToUserId(), viewHolder.avatar_img);
        }
        /* 设置昵称 */
        SpannableStringBuilder nickNamebuilder = new SpannableStringBuilder();
        final String userId = TextUtils.isEmpty(message.getToUserId()) ? message.getUserId() : message.getToUserId();
        String showName = getShowName(userId, TextUtils.isEmpty(message.getToUserName()) ? message.getNickName() : message.getToUserName());
        UserClickableSpan.setClickableSpan(mContext, nickNamebuilder, showName, userId);
        viewHolder.nick_name_tv.setText(nickNamebuilder);
        viewHolder.nick_name_tv.setLinksClickable(true);
        viewHolder.nick_name_tv.setMovementMethod(LinkMovementClickMethod.getInstance());

        // 设置头像的点击事件
        viewHolder.avatar_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isNormalClick(v)) {
                    return;
                }
                BasicInfoActivity.start(mContext, userId);
            }
        });

        // 获取消息本身的内容
        Body body = message.getBody();
        if (body == null) {
            return;
        }

        // 是否是转载的
        boolean isForwarding = message.getSource() == PublicMessage.SOURCE_FORWARDING;

        // 设置body_tv
        // todo 注释掉的代码为 展开/全文的方式显示文本，文本长度过长时会卡顿，先隐藏这种方式，换种方式(最多显示6行，操作的跳转显示)
        if (TextUtils.isEmpty(body.getText())) {
            viewHolder.body_tv.setVisibility(View.GONE);
        } else {
            // 支持emoji显示
            viewHolder.body_tv.setFilters(new InputFilter[]{new EmojiInputFilter(mContext)});
            viewHolder.body_tv.setUrlText(body.getText());
            viewHolder.body_tv.setVisibility(View.VISIBLE);
        }
        // 判断是否超出6行限制，超过则显示"全文"
        viewHolder.body_tv.post(() -> {
            Layout layout = viewHolder.body_tv.getLayout();
            if (layout != null) {
                int lines = layout.getLineCount();
                // setText换成setUrlText之后，layout.getEllipsisCount有点问题，换一个判断方法
/*
                if (lines > 0) {
                    if (layout.getEllipsisCount(lines - 1) > 0) {
                        viewHolder.open_tv.setVisibility(View.VISIBLE);
                        viewHolder.open_tv.setOnClickListener(v -> LongTextShowActivity.start(mContext, body.getText()));
                    } else {
                        viewHolder.open_tv.setVisibility(View.GONE);
                        viewHolder.open_tv.setOnClickListener(null);
                    }
                }
*/
                if (lines > 6) {
                    viewHolder.open_tv.setVisibility(View.VISIBLE);
                    viewHolder.open_tv.setOnClickListener(v -> LongTextShowActivity.start(mContext, body.getText()));
                } else {
                    viewHolder.open_tv.setVisibility(View.GONE);
                    viewHolder.open_tv.setOnClickListener(null);
                }
            }
        });

        viewHolder.body_tv.setOnLongClickListener(v -> {
            showBodyTextLongClickDialog(body.getText());
            return false;
        });
        viewHolder.ll_edit.setOnClickListener(v -> {
            initPublicWindow(message);
            publicWindow.setClick(new PublicPopuWindowClickListener(position));
            publicWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            publicWindow.showAsDropDown(v,
                    -(publicWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                    0);
        });

        // 设置发布时间 MyCollection
        viewHolder.time_tv.setText(TimeUtils.getFriendlyTimeDesc(mContext, (int) message.getTime()));
        if (MyCollection.class.toString().contains(mContext.getClass().toString())) {
            viewHolder.rl_collect_from.setVisibility(View.VISIBLE);
            switch (message.getTargetType()) {
                case CIRCLE:
                    viewHolder.iv_from_icon.setImageResource(R.mipmap.from_circle_icon);
                    viewHolder.iv_from_icon.setVisibility(View.VISIBLE);
                    viewHolder.tv_from_text.setVisibility(View.GONE);
                    break;
                case VIDEO:
                    viewHolder.iv_from_icon.setImageResource(R.mipmap.from_video_icon);
                    viewHolder.iv_from_icon.setVisibility(View.VISIBLE);
                    viewHolder.tv_from_text.setVisibility(View.GONE);
                    break;
                case OTHER:
                    viewHolder.tv_from_text.setText(mContext.getResources().getString(R.string.other));
                    viewHolder.iv_from_icon.setVisibility(View.GONE);
                    viewHolder.tv_from_text.setVisibility(View.VISIBLE);
                    break;
                case SINGLE:
                case ROOM:
                    viewHolder.iv_from_icon.setVisibility(View.GONE);
                    viewHolder.tv_from_text.setVisibility(View.VISIBLE);
                    viewHolder.tv_from_text.setText(message.getTargetName());
                    break;
            }
        } else {
            viewHolder.rl_collect_from.setVisibility(View.GONE);
            // 删除移至ll_edit内了
        }
        if (collectionType == 1) {
            publicWindow.setOpenDelete(true);
        } else if (collectionType == 2) {
            publicWindow.setOpenDelete(false);
        } else {
/*
            if (userId.equals(mLoginUserId)) {
                // 是我发的消息
                viewHolder.delete_tv.setVisibility(View.VISIBLE);
                viewHolder.delete_tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (UiUtils.isNormalClick(v)) {
                            showDeleteMsgDialog(position);
                        }
                    }
                });
            } else {
                viewHolder.delete_tv.setVisibility(View.GONE);
                viewHolder.delete_tv.setOnClickListener(null);
            }
*/
            // 删除移至ll_edit内了
        }
        if (mContext instanceof BusinessCircleActivity) {
            publicWindow.setOpenDelete(true);
        }

        if (mContext instanceof MyCollection || mContext instanceof CollectionSearchActivity) {
            publicWindow.setOpenReport(false);
        }
        initPublicWindow(message);
        final ViewHolder vh = viewHolder;
        vh.ivThumb.setChecked(1 == message.getIsPraise());
        vh.tvThumb.setText(String.valueOf(message.getPraise()));
        vh.llThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UiUtils.isNormalClick(v)) {
                    // 是否是点过赞，
                    final boolean isPraise = vh.ivThumb.isChecked();
                    // 调接口，旧代码保留，传的是相反的状态，
                    onPraise(position, !isPraise);
                    // 更新赞数，调接口完成还会刷新，
                    int praiseCount = message.getPraise();
                    if (isPraise) {
                        praiseCount--;
                    } else {
                        praiseCount++;
                    }
                    vh.tvThumb.setText(String.valueOf(praiseCount));
                    vh.ivThumb.toggle();
                }
            }
        });
        // 是否点评论过，
        // TODO: 不准了，评论分页加载，
        boolean isComment = false;
        if (message.getComments() != null) {
            for (Comment comment : message.getComments()) {
                if (mLoginUserId.equals(comment.getUserId())) {
                    isComment = true;
                }
            }
        }
        vh.ivComment.setChecked(isComment);
        vh.tvComment.setText(String.valueOf(message.getCommnet()));
        vh.llComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 调接口，旧代码保留，
                onComment(position, vh.command_listView);
                // 评论数在调接口完成后还会刷新，
                if (showCEView != null) {
                    showCEView.showView(position, mMessages.get(position).getUserId(), mMessages.get(position).getNickName(), "");
                }
            }
        });
        vh.ivCollection.setChecked(1 == message.getIsCollect());
        // 收藏移至ll_edit内了

        vh.llCollection.setOnClickListener(v -> {
            onCollection(position);
        });

        /* 显示多少人赞过 */
        List<Praise> praises = message.getPraises();
        if (praises != null && praises.size() > 0) {
            viewHolder.llPrise.setVisibility(View.VISIBLE);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            for (int i = 0; i < praises.size(); i++) {
                String praiseName = getShowName(praises.get(i).getUserId(), praises.get(i).getNickName());
                UserClickableSpan.setClickableSpan(mContext, builder, praiseName, praises.get(i).getUserId());
                if (i < praises.size() - 1)
                    builder.append(",");
            }
            builder.append(mContext.getString(R.string.praise_ending_place_holder, message.getPraise()));
            viewHolder.multi_praise_tv.setText(builder);
        } else {
            viewHolder.llPrise.setVisibility(View.GONE);
            viewHolder.multi_praise_tv.setText("");
        }
        viewHolder.multi_praise_tv.setLinksClickable(true);
        viewHolder.multi_praise_tv.setMovementMethod(LinkMovementClickMethod.getInstance());
        viewHolder.multi_praise_tv.setOnClickListener(v -> {
            PraiseListActivity.start(mContext, message.getMessageId());
        });

        /* 设置回复 */
        final List<Comment> comments = message.getComments();
        CommentAdapter adapter = new CommentAdapter(position, comments);
        viewHolder.command_listView.setAdapter(adapter);
        viewHolder.tvLoadMore.setVisibility(View.GONE);
        if (comments != null && comments.size() > 0) {
            viewHolder.command_listView.setVisibility(View.VISIBLE);
            if (message.getCommnet() > comments.size()) {
                // 需要分页加载，
                viewHolder.tvLoadMore.setVisibility(View.VISIBLE);
                viewHolder.tvLoadMore.setOnClickListener(v -> {
                    loadCommentsNextPage(vh.tvLoadMore, message.getMessageId(), adapter, comments.size());
                });
            }
        } else {
            viewHolder.command_listView.setVisibility(View.GONE);
        }

        // 赞与评论之间的横线，两者都有才显示
        if (praises != null && praises.size() > 0 && comments != null && comments.size() > 0) {
            viewHolder.line_v.setVisibility(View.VISIBLE);
        } else {
            viewHolder.line_v.setVisibility(View.GONE);
        }
        // 点赞评论上方留白，有一个就显示，
        if ((praises != null && praises.size() > 0) || (comments != null && comments.size() > 0)) {
            viewHolder.llPriseAndComment.setVisibility(View.VISIBLE);
        } else {
            viewHolder.llPriseAndComment.setVisibility(View.GONE);
        }

/*
        mAdapter = (CommentAdapter) viewHolder.command_listView.getAdapter();
        if (mAdapter == null) {
            mAdapter = new CommentAdapter();
            viewHolder.command_listView.setAdapter(mAdapter);
        }

        if (comments != null && comments.size() > 0) {
            viewHolder.line_v.setVisibility(View.VISIBLE);
            viewHolder.command_listView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.line_v.setVisibility(View.GONE);
            viewHolder.command_listView.setVisibility(View.GONE);
        }
        mAdapter.setData(position, comments);
*/


        if (!TextUtils.isEmpty(message.getLocation())) {
            String location = message.getLocation();
            // 距离展示仅供测试，
/*
            double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
            double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
            if (latitude != 0 && longitude != 0) {
                String distance = DisplayUtil.getDistance(latitude, longitude, message.getLoc().getLat(), message.getLoc().getLng());
                location = location + "(" + distance + ")";
            }
*/
            viewHolder.location_tv.setText(location);
            viewHolder.location_tv.setVisibility(View.VISIBLE);
        } else {
            viewHolder.location_tv.setVisibility(View.GONE);
        }

        viewHolder.location_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MapActivity.class);
                intent.putExtra("latitude", message.getLoc().getLat());
                intent.putExtra("longitude", message.getLoc().getLng());
                intent.putExtra("userName", message.getLocation());
                mContext.startActivity(intent);
            }
        });

        // //////////////////上面是公用的部分，下面是每个Type不同的部分/////////////////////////////////////////
        // 转载的消息会有一个转载人和text
        SpannableStringBuilder forwardingBuilder = null;
        if (isForwarding) {// 转载的那个人和说的话
            forwardingBuilder = new SpannableStringBuilder();
            String forwardName = getShowName(message.getFowardUserId(), message.getFowardNickname());
            UserClickableSpan.setClickableSpan(mContext, forwardingBuilder, forwardName, message.getFowardUserId());
            if (!TextUtils.isEmpty(message.getFowardText())) {
                forwardingBuilder.append(" : ");
                forwardingBuilder.append(message.getFowardText());
            }
        }
        if (viewType == VIEW_TYPE_NORMAL_TEXT) {
            viewHolder.content_fl.setVisibility(View.GONE);
        } else if (viewType == VIEW_TYPE_NORMAL_SINGLE_IMAGE) {
            ImageView image_view = ((NormalSingleImageHolder) viewHolder).image_view;
            String url = message.getFirstImageOriginal();
            if (!TextUtils.isEmpty(url)) {
                if (url.endsWith(".gif")) {
                    ImageLoadHelper.showGifWithPlaceHolder(
                            mContext,
                            url,
                            R.drawable.default_gray,
                            R.drawable.image_download_fail_icon,
                            image_view
                    );
                } else {
                    ImageLoadHelper.showImageCenterCrop(
                            mContext,
                            url,
                            R.drawable.default_gray,
                            R.drawable.image_download_fail_icon,
                            image_view
                    );
                }
                image_view.setOnClickListener(new SingleImageClickListener(url, message));
                image_view.setVisibility(View.VISIBLE);
            } else {
                image_view.setImageBitmap(null);
                image_view.setVisibility(View.GONE);
            }
        } else if (viewType == VIEW_TYPE_NORMAL_MULTI_IMAGE) {
            MyGridView grid_view = ((NormalMultiImageHolder) viewHolder).grid_view;
            if (body.getImages() != null) {
                grid_view.setAdapter(new ImagesInnerGridViewAdapter(mContext, body.getImages()));
                grid_view.setOnItemClickListener(new MultipleImagesClickListener(body.getImages(), message));
            } else {
                grid_view.setAdapter(null);
            }
        } else if (viewType == VIEW_TYPE_NORMAL_VOICE) {
            // 相关代码相当混乱且有大量废弃代码，
            // 修改语音消息外观时尽少修改代码，
            final NormalVoiceHolder holder = (NormalVoiceHolder) viewHolder;
            holder.chat_to_voice.fillData(message);
            holder.chat_to_voice.setOnClickListener(v -> {
                VoicePlayer.instance().playVoice(holder.chat_to_voice);
            });
        } else if (viewType == VIEW_TYPE_NORMAL_VIDEO) {
            NormalVideoHolder holder = (NormalVideoHolder) viewHolder;
            String imageUrl = message.getFirstImageOriginal();
            // 判断是自己处理就直接使用本地文件，
            String videoUrl = UploadCacheUtils.getVideoUri(mContext, message.getFirstVideo());
            if (!TextUtils.isEmpty(videoUrl)) {
                if (videoUrl.equals(message.getFirstVideo())) {
                    // 如果不是自己上传的，就使用视频缓存库统一缓存，
                    videoUrl = MyApplication.getProxy(mContext).getProxyUrl(message.getFirstVideo());
                }
                String finalVideoUrl = videoUrl;
                holder.ivImage.setOnClickListener(view -> PlayVideoActivity.start(mContext, finalVideoUrl));
            } else {
                holder.ivImage.setOnClickListener(null);
            }
            if (TextUtils.isEmpty(imageUrl)) {
                AvatarHelper.getInstance().asyncDisplayOnlineVideoThumb(videoUrl, holder.ivImage);
            } else {
                ImageLoadHelper.showImageWithPlaceHolder(
                        mContext,
                        imageUrl,
                        R.drawable.default_gray,
                        R.drawable.default_gray,
                        holder.ivImage
                );
            }
        } else if (viewType == VIEW_TYPE_NORMAL_FILE) {
            // 文件
            NormalFileHolder holder = (NormalFileHolder) viewHolder;
            final String mFileUrl = message.getFirstFile();

            if (TextUtils.isEmpty(mFileUrl)) {
                return;
            }
            // 朋友圈的接口数据没有fileName,
            if (!TextUtils.isEmpty(message.getFileName())) {
                holder.text_tv.setText(mContext.getString(R.string.msg_file) + message.getFileName());
            } else {
                try {
                    message.setFileName(mFileUrl.substring(mFileUrl.lastIndexOf('/') + 1));
                    holder.text_tv.setText(mContext.getString(R.string.msg_file) + message.getFileName());
                } catch (Exception ignored) {
                    // 万一url有问题，没有斜杠/直接抛异常下来显示url,
                    holder.text_tv.setText(mContext.getString(R.string.msg_file) + mFileUrl);
                }
            }

            String suffix = "";
            int index = mFileUrl.lastIndexOf(".");
            if (index != -1) {
                suffix = mFileUrl.substring(index + 1).toLowerCase();
                if (suffix.equals("png") || suffix.equals("jpg")) {
                    ImageLoadHelper.showImageWithSize(
                            mContext,
                            mFileUrl,
                            100, 100,
                            holder.file_image
                    );
                } else {
                    AvatarHelper.getInstance().fillFileView(suffix, holder.file_image);
                }
            }

            final long size = message.getBody().getFiles().get(0).getSize();
            Log.e("xuan", "setOnClickListener: " + size);

            holder.file_click.setOnClickListener(v -> intentPreviewFile(mFileUrl, message.getFileName(), message.getNickName(), size));
        } else if (viewType == VIEW_TYPE_NORMAL_LINK) {
            NormalLinkHolder holder = (NormalLinkHolder) viewHolder;
            if (TextUtils.isEmpty(message.getBody().getSdkIcon())) {
                holder.link_image.setImageResource(R.drawable.browser);
            } else {
                AvatarHelper.getInstance().displayUrl(message.getBody().getSdkIcon(), holder.link_image);
            }
            holder.link_tv.setText(message.getBody().getSdkTitle());

            holder.link_click.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra(WebViewActivity.EXTRA_URL, message.getBody().getSdkUrl());
                mContext.startActivity(intent);
            });
        }
    }

    private void initPublicWindow(PublicMessage message) {
        if (TextUtils.equals(message.getUserId(), mLoginUserId)) {
            publicWindow.setOpenDelete(true);
            publicWindow.setOpenReport(false);
        } else {
            publicWindow.setOpenDelete(false);
            publicWindow.setOpenReport(true);
        }

        if (collectionType == 1 || collectionType == 2) {
            publicWindow.setOpenDelete(true);
            publicWindow.setOpenReport(false);
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    /**
     * @see PublicMessage#getType() <br/>
     * 1=文字消息；2=图文消息；3=语音消息； 4=视频消息；5、转载<br/>
     * 分的视图类型有： <br/>
     * {@link #VIEW_TYPE_NORMAL_TEXT}0、普通文字消息视图<br/>
     * {@link #VIEW_TYPE_NORMAL_SINGLE_IMAGE} 2、普通单张图片的视图<br/>
     * {@link #VIEW_TYPE_NORMAL_MULTI_IMAGE}4、普通多张图片的视图<br/>
     * {@link #VIEW_TYPE_NORMAL_VOICE} 6、普通音频视图<br/>
     * {@link #VIEW_TYPE_NORMAL_VIDEO}8、普通视频视图<br/>
     */

    @Override
    public int getItemViewType(int position) {
        PublicMessage message = mMessages.get(position);
        if (!TextUtils.isEmpty(message.getId())) {
            return VIEW_TYPE_NORMAL_AD;
        }
        // boolean fromSelf = message.getSource() == PublicMessage.SOURCE_SELF;
        if (message == null || message.getBody() == null) {
            // 如果为空，那么可能是数据错误，直接返回一个普通的文本视图
            return VIEW_TYPE_NORMAL_TEXT;
        }
        Body body = message.getBody();
        if (message.getIsAllowComment() == 1) {
            message.setIsAllowComment(1);
        } else {
            message.setIsAllowComment(0);
        }
        if (body.getType() == PublicMessage.TYPE_TEXT) {
            // 文本视图
            return VIEW_TYPE_NORMAL_TEXT;
        } else if (body.getType() == PublicMessage.TYPE_IMG || body.getType() == PublicMessage.TYPE_SINGLE_IMAGE) {
            if (body.getImages() == null || body.getImages().size() == 0) {
                // 莫名出现类型为图片，但是没有图片的朋友圈消息，略做兼容，
                body.setType(PublicMessage.TYPE_TEXT);
                // 文本视图
                return VIEW_TYPE_NORMAL_TEXT;
            } else if (body.getImages().size() <= 1 || body.getType() == PublicMessage.TYPE_SINGLE_IMAGE) {
                // 普通的单张图片的视图
                return VIEW_TYPE_NORMAL_SINGLE_IMAGE;
            } else {// 普通的多张图片视图
                return VIEW_TYPE_NORMAL_MULTI_IMAGE;
            }
        } else if (body.getType() == PublicMessage.TYPE_VOICE) {// 普通音频
            return VIEW_TYPE_NORMAL_VOICE;
        } else if (body.getType() == PublicMessage.TYPE_VIDEO || body.getType() == PublicMessage.TYPE_SINGLE_VIDEO) {// 普通视频
            return VIEW_TYPE_NORMAL_VIDEO;
        } else if (body.getType() == PublicMessage.TYPE_FILE) {
            // 文件
            return VIEW_TYPE_NORMAL_FILE;
        } else if (body.getType() == PublicMessage.TYPE_LINK) {
            // 链接
            return VIEW_TYPE_NORMAL_LINK;
        } else {
            // 其他，数据错误
            return VIEW_TYPE_NORMAL_TEXT;
        }
    }

    /**
     * 跳转到文件预览
     *
     * @param filePath
     */
    private void intentPreviewFile(String filePath, String fileName, String fromName, long size) {
        MucFileBean data = new MucFileBean();

        // 取出文件后缀
        int start = filePath.lastIndexOf(".");
        String suffix = start > -1 ? filePath.substring(start + 1).toLowerCase() : "";

        int fileType = XfileUtils.getFileType(suffix);
        data.setNickname(fromName);
        data.setUrl(filePath);
        data.setName(fileName);
        data.setSize(size);
        data.setState(DownManager.STATE_UNDOWNLOAD);
        data.setType(fileType);
        Intent intent = new Intent(mContext, MucFileDetails.class);
        intent.putExtra("data", data);
        mContext.startActivity(intent);
    }

    /**
     * 第一次调用时就已经是阅读完第一页，加载第二页了，
     */
    private void loadCommentsNextPage(TextView view, String messageId, CommentAdapter adapter, int pageSize) {
        // isLoading同时有noMore效果，只有加载出新数据时才设置isLoading为false,
        if (adapter.isLoading()) {
            return;
        }
        adapter.setLoading(true);
        // 有20个就加载第二页也就是index==1, 21个是加载第三页，得到空列表，就能停止了，
        int index = (adapter.getCount() + (pageSize - 1)) / pageSize;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageIndex", String.valueOf(index));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("messageId", messageId);

        String url = coreManager.getConfig().MSG_COMMENT_LIST;

        view.setTag(messageId);
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new ListCallback<Comment>(Comment.class) {
                    @Override
                    public void onResponse(ArrayResult<Comment> result) {
                        List<Comment> data = result.getData();
                        if (data.size() > 0) {
                            adapter.addAll(data);
                            adapter.setLoading(false);
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_no_more);
                            if (view.getTag() == messageId) {
                                // 隐藏加载按钮，
                                view.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Reporter.post("评论分页加载失败，", e);
                        ToastUtil.showToast(mContext, mContext.getString(R.string.tip_comment_load_error));
                    }
                });

    }

    private String getShowName(String userId, String defaultName) {
        if (TextUtils.isEmpty(defaultName)) {
            defaultName = "";
        }
        String cache = showNameCache.get(userId);
        if (!TextUtils.isEmpty(cache)) {
            return cache;
        }
        String showName = "";

        if (userId.equals(mLoginUserId)) {
            showName = coreManager.getSelf().getNickName();
        } else {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, userId);
            if (friend != null) {
                showName = TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
            }
        }

        if (TextUtils.isEmpty(showName)) {
            showName = defaultName;
        }
        showNameCache.put(userId, showName);
        return showName;
    }

    /* 操作事件 */
    private void showDeleteMsgDialog(final int position) {
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        int tip;
        if (mContext instanceof MyCollection) {
            tip = R.string.sure_cancel_collection;
        } else {
            tip = R.string.delete_prompt;
        }
        selectionFrame.setSomething(null, mContext.getString(tip), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                if (collectionType == 1 || collectionType == 2) {
                    // 删除收藏
                    deleteCollection(position);
                } else {
                    // 删除评论
                    deleteMsg(position);
                }
            }
        });
        selectionFrame.show();
    }

    private void showBodyTextLongClickDialog(final String text) {
        CharSequence[] items = new CharSequence[]{mContext.getString(R.string.copy)};
        new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // 复制文字
                        SystemUtil.copyText(mContext, text);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    private void showCommentLongClickDialog(final int messagePosition, final int commentPosition,
                                            final CommentAdapter adapter) {
        if (messagePosition < 0 || messagePosition >= mMessages.size()) {
            return;
        }
        final PublicMessage message = mMessages.get(messagePosition);
        if (message == null) {
            return;
        }
        final List<Comment> comments = message.getComments();
        if (comments == null) {
            return;
        }
        if (commentPosition < 0 || commentPosition >= comments.size()) {
            return;
        }
        final Comment comment = comments.get(commentPosition);

        CharSequence[] items;
        if (comment.getUserId().equals(mLoginUserId) || message.getUserId().equals(mLoginUserId)) {
            // 我的评论 || 我的消息，那么我就可以删除
            items = new CharSequence[]{mContext.getString(R.string.copy), mContext.getString(R.string.delete)};
        } else {
            items = new CharSequence[]{mContext.getString(R.string.copy)};
        }
        new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:// 复制文字
                        if (TextUtils.isEmpty(comment.getBody())) {
                            return;
                        }
                        SystemUtil.copyText(mContext, comment.getBody());
                        break;
                    case 1:
                        deleteComment(message, messagePosition, comment.getCommentId(), comments, commentPosition, adapter);
                        break;
                }
            }
        }).setCancelable(true).create().show();
    }

    private String collectionParam(PublicMessage message) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = message.getCollectionType();
        String msg = "";
        String collectContent = "";
        String fileName = "";
        long fileLength = 0;
        long fileSize = 0;
        String id = message.getMessageId();
        Resource res = null;
        if (message.getBody() != null) {
            collectContent = message.getBody().getText();
            switch (type) {
                case CollectionEvery.TYPE_TEXT:
                    msg = message.getBody().getText();
                    break;
                case CollectionEvery.TYPE_IMAGE:
                case CollectionEvery.TYPE_IMAGE_SINGLE:
                    List<Resource> images = message.getBody().getImages();
                    // 莫名出现类型为图片，但是没有图片的朋友圈消息，略做兼容，
                    if (images == null || images.isEmpty()) {
                        type = CollectionEvery.TYPE_TEXT;
                        msg = message.getBody().getText();
                        break;
                    }
                    StringBuilder sb = new StringBuilder();
                    boolean firstTime = true;
                    for (Resource token : images) {
                        String url = token.getOriginalUrl();
                        if (TextUtils.isEmpty(url)) {
                            continue;
                        }
                        if (firstTime) {
                            firstTime = false;
                        } else {
                            sb.append(',');
                        }
                        sb.append(url);
                    }
                    msg = sb.toString();
                    break;
                case CollectionEvery.TYPE_FILE:
                    res = firstOrNull(message.getBody().getFiles());
                    break;
                case CollectionEvery.TYPE_VIDEO:
                case CollectionEvery.TYPE_VIDEO_SINGLE:
                    res = firstOrNull(message.getBody().getVideos());
                    break;
                case CollectionEvery.TYPE_VOICE:
                    res = firstOrNull(message.getBody().getAudios());
                    break;

                case CollectionEvery.TYPE_LINK:
                    Message linkMessage = new Message();
                    linkMessage.setImg(message.getBody().getSdkIcon());
                    linkMessage.setTitle(message.getBody().getSdkTitle());
                    linkMessage.setUrl(message.getBody().getSdkUrl());
                    msg = JSON.toJSONString(linkMessage);
                    break;
                default:
                    throw new IllegalStateException("类型<" + type + ">不存在，");
            }
        }

        if (res != null) {
            if (!TextUtils.isEmpty(res.getOriginalUrl())) {
                msg = res.getOriginalUrl();
            }
            fileLength = res.getLength();
            fileSize = res.getSize();
        }
        if (!TextUtils.isEmpty(message.getFileName())) {
            fileName = message.getFileName();
        }
        json.put("type", String.valueOf(type));
        json.put("msg", msg);
        json.put("fileName", fileName);
        json.put("fileSize", fileSize);
        json.put("fileLength", fileLength);
        json.put("collectContent", collectContent);
        json.put("collectType", 1);
        json.put("collectMsgId", id);
        json.put("toUserId", message.getUserId());
        json.put("targetType", 1); // 收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊
        array.add(json);
        return JSON.toJSONString(array);
    }

    private <T> T firstOrNull(List<T> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public void onReport(final int messagePosition) {
        ReportDialog mReportDialog = new ReportDialog(mContext, false, report -> report(messagePosition, report));
        mReportDialog.show();
    }

    public void onPraise(int messagePosition, boolean isPraise) {
        praiseOrCancel(messagePosition, isPraise);
    }

    public void onComment(int messagePosition, ListView view) {
        PublicMessage message = mMessages.get(messagePosition);
        if (message != null
                && message.getIsAllowComment() == 1
                && !TextUtils.equals(message.getUserId(), mLoginUserId)) {
            Toast.makeText(mContext, MyApplication.getContext().getString(R.string.ban_comment), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mContext instanceof BusinessCircleActivity) {
            ((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition, null, null, null);
        } else {
            String path = "";
            if (message.getType() == 3) {
                //语音
                path = message.getFirstAudio();
            } else if (message.getType() == 2) {
                //图片
                path = message.getFirstImageOriginal();
            } else if (message.getType() == 6) {
                //视频
                path = message.getFirstVideo();
            }
            view.setTag(message);
            if (onMessageActionListener != null) {
                onMessageActionListener.helloEventBus(new MessageEventComment("Comment", message.getMessageId(), message.getIsAllowComment(),
                        message.getType(), path, message, view));
            }
        }
    }

    private void onCollection(final int messagePosition) {
        PublicMessage message = mMessages.get(messagePosition);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        if (message.getIsCollect() == 1) {
            params.put("messageId", message.getMessageId());

            HttpUtils.get().url(coreManager.getConfig().MSG_COLLECT_DELETE)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(mContext, R.string.tip_collection_canceled);
                                message.setIsCollect(0);
                                notifyDataSetChanged();
                            } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                                ToastUtil.showToast(mContext, result.getResultMsg());
                            } else {
                                ToastUtil.showToast(mContext, R.string.tip_server_error);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            ToastUtil.showNetError(mContext);
                        }
                    });
        } else {
            params.put("emoji", collectionParam(message));

            HttpUtils.post().url(coreManager.getConfig().Collection_ADD)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            if (result.getResultCode() == 1) {
                                Toast.makeText(mContext, mContext.getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                                message.setIsCollect(1);
                                notifyDataSetChanged();
                            } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                                ToastUtil.showToast(mContext, result.getResultMsg());
                            } else {
                                ToastUtil.showToast(mContext, R.string.tip_server_error);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            ToastUtil.showNetError(mContext);
                        }
                    });
        }
    }

    public void deleteCollection(final int position) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emojiId", message.getEmojiId());
        DialogHelper.showDefaulteMessageProgressDialog((Activity) mContext);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_REMOVE)
                .params(params)
                .build()
                .execute(new BaseCallback<Collectiion>(Collectiion.class) {

                    @Override
                    public void onResponse(ObjectResult<Collectiion> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            mMessages.remove(position);
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void report(int position, Report report) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", message.getUserId());
        params.put("reportType", String.valueOf(4));
        params.put("reportInfo", message.getMessageId());
        params.put("reason", String.valueOf(report.getReportId()));

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_REPORT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, mContext.getString(R.string.report_success));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private void deleteMsg(final int position) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        DialogHelper.showDefaulteMessageProgressDialog((Activity) mContext);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).CIRCLE_MSG_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            EventBus.getDefault().post(new EventPublicMessageRemoved(message.getType(), message.getMessageId()));
                            CircleMessageDao.getInstance().deleteMessage(message.getMessageId());// 删除数据库的记录（如果存在的话）
                            mMessages.remove(position);
                            notifyDataSetChanged();
                            if (mMessages.size() == 0
                                    && mContext instanceof BusinessCircleActivity) {
                                ((BusinessCircleActivity) mContext).addNullTV2LV();
                            }

                            // 删除成功，停止正在播放的视频、音频
                            JCVideoPlayer.releaseAllVideos();
                            stopVoice();
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
     * 赞 || 取消赞
     */
    private void praiseOrCancel(final int position, final boolean isPraise) {
        final PublicMessage message = mMessages.get(position);
        if (message == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        String requestUrl;
        if (isPraise) {
            requestUrl = CoreManager.requireConfig(MyApplication.getInstance()).MSG_PRAISE_ADD;
        } else {
            requestUrl = CoreManager.requireConfig(MyApplication.getInstance()).MSG_PRAISE_DELETE;
        }
        HttpUtils.get().url(requestUrl)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            message.setIsPraise(isPraise ? 1 : 0);
                            List<Praise> praises = message.getPraises();
                            if (praises == null) {
                                praises = new ArrayList<>();
                                message.setPraises(praises);
                            }
                            int praiseCount = message.getPraise();
                            if (isPraise) {
                                // 代表我点赞
                                // 消息实体的改变
                                Praise praise = new Praise();
                                praise.setUserId(mLoginUserId);
                                praise.setNickName(mLoginNickName);
                                // praises.add(0, praise);
                                praises.add(praise);// 不建议将其添加到第一位，否则赞与取消赞的操作会造成点赞名单闪烁的现象
                                praiseCount++;
                                message.setPraise(praiseCount);
                            } else {
                                // 取消我的赞
                                // 消息实体的改变
                                for (int i = 0; i < praises.size(); i++) {
                                    if (mLoginUserId.equals(praises.get(i).getUserId())) {
                                        praises.remove(i);
                                        praiseCount--;
                                        message.setPraise(praiseCount);
                                        break;
                                    }
                                }
                            }
                            notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 删除一条回复
     */
    private void deleteComment(PublicMessage message, int messagePosition, String commentId, final List<Comment> comments,
                               final int commentPosition, final CommentAdapter adapter) {
        String messageId = message.getMessageId();
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        params.put("commentId", commentId);
        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).MSG_COMMENT_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            int size = message.getCommnet() - 1;
                            if (size < 0) {
                                // 测试测出评论条数变为-1的情况，无法复现，先兼容吧
                                size = 0;
                            }
                            message.setCommnet(size);
                            comments.remove(commentPosition);
                            notifyItemChanged(messagePosition);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 停止播放声音
     */
    public void stopVoice() {
        if (mAudioPalyer != null) {
            mAudioPalyer.stop();
        }
        VoicePlayer.instance().stop();
    }

    /**
     * @param viewHolder
     */
    private void play(ViewHolder viewHolder, PublicMessage message) {
        JCVideoPlayer.releaseAllVideos();

        String voiceUrl = message.getFirstAudio();
        if (mVoicePlayId == null) {
            // 没有在播放
            try {
                mAudioPalyer.play(voiceUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mVoicePlayId = message.getMessageId();
            updateVoiceViewHolderIconStatus(true, viewHolder);
            mVoicePlayViewHolder = viewHolder;
        } else {
            if (mVoicePlayId == message.getMessageId()) {
                mAudioPalyer.stop();
                mVoicePlayId = null;
                updateVoiceViewHolderIconStatus(false, viewHolder);
                mVoicePlayViewHolder = null;
            } else {
                // 正在播放别的， 在播放这个
                mAudioPalyer.stop();
                mVoicePlayId = null;
                if (mVoicePlayViewHolder != null) {
                    updateVoiceViewHolderIconStatus(false, mVoicePlayViewHolder);
                }
                try {
                    mAudioPalyer.play(voiceUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mVoicePlayId = message.getMessageId();
                updateVoiceViewHolderIconStatus(true, viewHolder);
                mVoicePlayViewHolder = viewHolder;
            }
        }
    }

    /**
     * 跳转到文件预览
     *
     * @param filePath
     */
    private void intentPreviewFile(PublicMessage message, String filePath, String fileName, String fromName, long size) {
        MucFileBean data = new MucFileBean();

        // 取出文件后缀
        int start = filePath.lastIndexOf(".");
        String suffix = start > -1 ? filePath.substring(start + 1).toLowerCase() : "";

        int fileType = XfileUtils.getFileType(suffix);
        data.setNickname(fromName);
        data.setUrl(filePath);
        data.setName(fileName);
        data.setSize(size);
        data.setState(DownManager.STATE_UNDOWNLOAD);
        data.setType(fileType);
        Intent intent = new Intent(mContext, MucFileDetails.class);
        intent.putExtra("data", data);
        intent.putExtra("publicMessage", message);
        mContext.startActivity(intent);
    }

    private void updateVoiceViewHolderIconStatus(boolean play, ViewHolder viewHolder) {
        if (viewHolder instanceof NormalVoiceHolder) {
            // 普通音频
            if (play) {
                ((NormalVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
            } else {
                ((NormalVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_play);
            }
        } else {
            // 转载音频
            if (play) {
                ((FwVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_pause);
            } else {
                ((FwVoiceHolder) viewHolder).voice_action_img.setImageResource(R.drawable.feed_main_player_play);
            }
        }
    }

    /**
     * 节口回调的方法
     */
    @Override
    public void ideChange() {
        stopVoice();
    }

    /**
     * 刷新适配器
     **/
    public void setData(List<PublicMessage> mMessages) {
        this.mMessages = mMessages;
        this.notifyDataSetChanged();
    }

    public void setInterface(showCEView showCEView) {
        this.showCEView = showCEView;
    }

    /**
     * 0:正常 default==0
     * 1:查看我的收藏 隐藏赞与评论功能 删除按钮一直显示
     * 2.发送我的收藏 隐藏赞与评论功能 删除按钮一直隐藏
     */
    public void setCollectionType(int collectionType) {
        this.collectionType = collectionType;
    }

    public void setCollectionSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }

    public interface OnItemClickListener {
        void onItemClick(PublicMessageRecyclerAdapter.ViewHolder vh);
    }

    /**
     * 取代原本的eventBus通知，多fragment使用有异常，
     * 方法名和参数沿用老代码，减少冲突，
     */
    public interface OnMessageActionListener {
        void helloEventBus(final MessageEventComment message);

        void helloEventBus(final MessageEventReply message);

        void helloEventBus(final MessageEventVideo message);

        void helloEventBus(final MessageEventPublic message);
    }

    public static class BaseViewHolder extends RecyclerView.ViewHolder {

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class AdViewHolder extends BaseViewHolder {
        private final AdView adView = itemView.findViewById(R.id.adView);

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void apply(PublicMessage message) {
            adView.load(new Advertising(message));
        }
    }

    public static class ViewHolder extends BaseViewHolder {
        ImageView avatar_img;
        TextView nick_name_tv;
        TextView time_tv;
        HttpTextView body_tv;
        //  HttpTextView body_tvS;
        TextView open_tv;
        FrameLayout content_fl;
        TextView multi_praise_tv;
        View line_v;
        ListView command_listView;
        TextView tvLoadMore;
        TextView location_tv;

        View llOperator;
        View llThumb;
        CheckableImageView ivThumb;
        TextView tvThumb;
        View llComment;
        CheckableImageView ivComment;
        TextView tvComment;
        View llCollection;
        CheckableImageView ivCollection;
        View llReport;
        View llPriseAndComment;
        View llPrise;
        View ll_edit;
        View rl_collect_from;
        ImageView iv_from_icon;
        TextView tv_from_text;
        CheckBox check_box;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar_img = (ImageView) itemView.findViewById(R.id.avatar_img);
            nick_name_tv = (TextView) itemView.findViewById(R.id.nick_name_tv);
            time_tv = (TextView) itemView.findViewById(R.id.time_tv);
            body_tv = itemView.findViewById(R.id.body_tv);
            // body_tvS = itemView.findViewById(R.id.body_tvS);
            open_tv = (TextView) itemView.findViewById(R.id.open_tv);
            content_fl = (FrameLayout) itemView.findViewById(R.id.content_fl);

            llOperator = itemView.findViewById(R.id.llOperator);
            llThumb = itemView.findViewById(R.id.llThumb);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            tvThumb = itemView.findViewById(R.id.tvThumb);
            llComment = itemView.findViewById(R.id.llComment);
            ivComment = itemView.findViewById(R.id.ivComment);
            tvComment = itemView.findViewById(R.id.tvComment);
            llCollection = itemView.findViewById(R.id.llCollection);
            ivCollection = itemView.findViewById(R.id.ivCollection);
            llReport = itemView.findViewById(R.id.llReport);
            ll_edit = itemView.findViewById(R.id.ll_edit);
            rl_collect_from = itemView.findViewById(R.id.rl_collect_from);
            iv_from_icon = itemView.findViewById(R.id.iv_from_icon);
            tv_from_text = itemView.findViewById(R.id.tv_from_text);
            check_box = itemView.findViewById(R.id.check_box);
        }
    }

    /* 普通的Text */
    static class NormalTextHolder extends ViewHolder {

        public NormalTextHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 转载的Text */
    static class FwTextHolder extends ViewHolder {
        TextView text_tv;

        public FwTextHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 普通的单张图片 */
    static class NormalSingleImageHolder extends ViewHolder {
        ImageView image_view;

        public NormalSingleImageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 转载的单张图片 */
    static class FwSingleImageHolder extends ViewHolder {
        TextView text_tv;
        ImageView image_view;

        public FwSingleImageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 普通的多张图片 */
    static class NormalMultiImageHolder extends ViewHolder {
        MyGridView grid_view;

        public NormalMultiImageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 转载的多张图片 */
    static class FwMultiImageHolder extends ViewHolder {
        TextView text_tv;
        MyGridView grid_view;

        public FwMultiImageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 普通的音频 */
    static class NormalVoiceHolder extends ViewHolder {
        ImageView img_view;
        ImageView voice_action_img;
        TextView voice_desc_tv;
        VoiceAnimView chat_to_voice;

        public NormalVoiceHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 转载的音频 */
    static class FwVoiceHolder extends ViewHolder {
        TextView text_tv;
        ImageView img_view;
        ImageView voice_action_img;
        TextView voice_desc_tv;

        public FwVoiceHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 普通的视频 */
    static class NormalVideoHolder extends ViewHolder {
        ImageView ivImage;

        public NormalVideoHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 转载的视频 */
    static class FwVideoHolder extends ViewHolder {
        TextView text_tv;
        ImageView video_thumb_img;
        TextView video_desc_tv;

        public FwVideoHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /************************* 播放声音 ******************************/

    /* 普通的文件 */
    static class NormalFileHolder extends ViewHolder {
        RelativeLayout file_click;
        ImageView file_image;
        TextView text_tv;

        public NormalFileHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /* 普通的链接 */
    static class NormalLinkHolder extends ViewHolder {
        LinearLayout link_click;
        ImageView link_image;
        TextView link_tv;

        public NormalLinkHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class CommentViewHolder {
        TextView text_view;
    }

    public class CommentAdapter extends BaseAdapter {
        private int messagePosition;
        private boolean loading;
        private List<Comment> datas;

        CommentAdapter(int messagePosition, List<Comment> data) {
            this.messagePosition = messagePosition;
            if (data == null) {
                datas = new ArrayList<>();
            } else {
                datas = new ArrayList<>(data);
            }
        }

/*
        public void setData(int messagePosition, List<Comment> data) {
            this.messagePosition = messagePosition;
            this.datas = data;
            notifyDataSetChanged();
        }
*/

        public void addAll(List<Comment> data) {
            this.datas.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CommentViewHolder holder;
            if (convertView == null) {
                holder = new CommentViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.p_msg_comment_list_item, null);
                holder.text_view = (TextView) convertView.findViewById(R.id.text_view);
                convertView.setTag(holder);
            } else {
                holder = (CommentViewHolder) convertView.getTag();
            }
            final Comment comment = datas.get(position);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            String showName = getShowName(comment.getUserId(), comment.getNickName());
            UserClickableSpan.setClickableSpan(mContext, builder, showName, comment.getUserId());            // 设置评论者的ClickSpanned
            if (!TextUtils.isEmpty(comment.getToUserId()) && !TextUtils.isEmpty(comment.getToNickname())) {
                builder.append(mContext.getString(R.string.replay_infix_comment));
                String toShowName = getShowName(comment.getToUserId(), comment.getToNickname());
                UserClickableSpan.setClickableSpan(mContext, builder, toShowName, comment.getToUserId());// 设置被评论者的ClickSpanned
            }
            builder.append(":");
            // 设置评论内容
            String commentBody = comment.getBody();
            if (!TextUtils.isEmpty(commentBody)) {
                commentBody = StringUtils.replaceSpecialChar(comment.getBody());
                CharSequence charSequence = HtmlUtils.transform200SpanString(commentBody, true);
                builder.append(charSequence);
            }
            holder.text_view.setText(builder);
            holder.text_view.setLinksClickable(true);
            holder.text_view.setMovementMethod(LinkMovementClickMethod.getInstance());

            holder.text_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (comment.getUserId().equals(mLoginUserId)) {
                        // 如果消息是我发的，那么就弹出删除和复制的对话框
                        showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
                    } else {
                        // 弹出回复的框
                        String toShowName = getShowName(comment.getUserId(), comment.getNickName());
                        if (mContext instanceof BusinessCircleActivity) {
                            ((BusinessCircleActivity) mContext).showCommentEnterView(messagePosition, comment.getUserId(), comment.getNickName(), toShowName);
                        } else {
                            if (onMessageActionListener != null) {
                                onMessageActionListener.helloEventBus(new MessageEventReply("Reply", comment, messagePosition, toShowName,
                                        (ListView) parent));
                            }
                        }
                    }
                }
            });

            holder.text_view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showCommentLongClickDialog(messagePosition, position, CommentAdapter.this);
                    return true;
                }
            });

            return convertView;
        }

        public boolean isLoading() {
            return loading;
        }

        public void setLoading(boolean loading) {
            this.loading = loading;
        }

        public void addComment(Comment comment) {
            this.datas.add(0, comment);
            notifyDataSetChanged();
        }
    }

    private class SingleImageClickListener implements View.OnClickListener {
        private String url;
        private PublicMessage message;

        SingleImageClickListener(String url, PublicMessage message) {
            this.url = url;
            this.message = message;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGE_URI, url);
            intent.putExtra("json", JSON.toJSONString(message));
            if (mContext instanceof MyCollection) {
                intent.putExtra("isMyCollection", true);
            }
            mContext.startActivity(intent);
        }
    }

    private class MultipleImagesClickListener implements AdapterView.OnItemClickListener {
        private List<Resource> images;
        private PublicMessage message;

        MultipleImagesClickListener(List<Resource> images, PublicMessage message) {
            this.images = images;
            this.message = message;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (images == null || images.size() <= 0) {
                return;
            }
            ArrayList<String> lists = new ArrayList<String>();
            for (int i = 0; i < images.size(); i++) {
                lists.add(images.get(i).getOriginalUrl());
            }
            Intent intent = new Intent(mContext, MultiImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGES, lists);
            intent.putExtra(AppConstant.EXTRA_POSITION, position);
            intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
            intent.putExtra("json", JSON.toJSONString(message));
            if (mContext instanceof MyCollection) {
                intent.putExtra("isMyCollection", true);
            }
            mContext.startActivity(intent);
        }
    }

    private class PublicPopuWindowClickListener implements View.OnClickListener {
        private int position;

        PublicPopuWindowClickListener(int i) {
            this.position = i;
        }


        @Override
        public void onClick(View v) {
            publicWindow.dismiss();
            switch (v.getId()) {
                case R.id.llShare:
                    try {
                        MessageUtil.generateShareMessage2Intent(mContext, mLoginUserId, 0, mMessages.get(position), true);
                    } catch (Exception e) {
                        // 视频等可能因其他异常导致不存在相应文件导致崩溃，
                        Reporter.post("分享失败, " + JSON.toJSONString(mMessages.get(position)), e);
                        ToastUtil.showToast(mContext, R.string.share_failed);
                    }
                    break;
                case R.id.llReport:
                    onReport(position);
                    break;
                case R.id.llDelete:
                    showDeleteMsgDialog(position);
                    break;
            }
        }
    }

}
