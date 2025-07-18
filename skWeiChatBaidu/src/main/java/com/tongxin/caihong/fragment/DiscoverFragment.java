package com.tongxin.caihong.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.adapter.PublicMessageRecyclerAdapter;
import com.tongxin.caihong.bean.EventRefreshPublicSquare;
import com.tongxin.caihong.bean.MyZan;
import com.tongxin.caihong.bean.circle.Comment;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.event.EventAvatarUploadSuccess;
import com.tongxin.caihong.bean.event.MessageEventHongdian;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.CircleMessageDao;
import com.tongxin.caihong.db.dao.MyZanDao;
import com.tongxin.caihong.db.dao.UserAvatarDao;
import com.tongxin.caihong.db.dao.UserDao;
import com.tongxin.caihong.downloader.Downloader;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.UploadingHelper;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.circle.MessageEventComment;
import com.tongxin.caihong.ui.circle.MessageEventNotifyDynamic;
import com.tongxin.caihong.ui.circle.MessageEventReply;
import com.tongxin.caihong.ui.circle.SelectPicPopupWindow;
import com.tongxin.caihong.ui.circle.range.NewZanActivity;
import com.tongxin.caihong.ui.circle.range.SendAudioActivity;
import com.tongxin.caihong.ui.circle.range.SendFileActivity;
import com.tongxin.caihong.ui.circle.range.SendShuoshuoActivity;
import com.tongxin.caihong.ui.circle.range.SendVideoActivity;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.CameraUtil;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.video.EasyCameraActivity;
import com.tongxin.caihong.video.MessageEventGpu;
import com.tongxin.caihong.video.VideoRecorderActivity;
import com.tongxin.caihong.view.MergerStatus;
import com.tongxin.caihong.view.SelectImageDialog;
import com.tongxin.caihong.view.TrillCommentInputDialog;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import fm.jiecao.jcvideoplayer_lib.MessageEventPublic;
import fm.jiecao.jcvideoplayer_lib.MessageEventVideo;
import okhttp3.Call;

/**
 * 朋友圈的Fragment
 * Created by Administrator
 */
public class DiscoverFragment extends EasyFragment implements PublicMessageRecyclerAdapter.OnMessageActionListener {
    private static final int REQUEST_CODE_SEND_MSG = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 3;
    private static final int REQUEST_CODE_CROP_PHOTO = 4;
    private static int PAGER_SIZE = 10;
    private String mUserId;
    private String mUserName;
    private MergerStatus mergerStatus;
    private RelativeLayout rl_title;
    private TextView mTvTitle;
    private ImageView mIvTitleRight;
    private SelectPicPopupWindow menuWindow;
    // 头部
    private View mHeadView;
    private ImageView ivHeadBg, ivHead;
    // 通知...
    private LinearLayout mTipLl;
    private ImageView mTipIv;
    private TextView mTipTv;
    // 页面
    private SmartRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mListView;
    private PublicMessageRecyclerAdapter mAdapter;
    private List<PublicMessage> mMessages = new ArrayList<>();
    private boolean more;
    private String messageId;
    private boolean showTitle = true;
    private Uri uri;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            if (menuWindow != null) {
                // 顶部一排按钮复用这个listener, 没有menuWindow,
                menuWindow.dismiss();
            }
            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.btn_send_picture:
                    // 发表图文，
                    intent.setClass(getActivity(), SendShuoshuoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_voice:
                    // 发表语音
                    intent.setClass(getActivity(), SendAudioActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_video:
                    // 发表视频
                    intent.setClass(getActivity(), SendVideoActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.btn_send_file:
                    // 发表文件
                    intent.setClass(getActivity(), SendFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SEND_MSG);
                    break;
                case R.id.new_comment:
                    // 最新评论&赞
                    Intent intent2 = new Intent(getActivity(), NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    mTipLl.setVisibility(View.GONE);
                    EventBus.getDefault().post(new MessageEventHongdian(0));
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_discover;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        initActionBar();
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + coreManager.getSelf().getUserId()
                + File.separator + Environment.DIRECTORY_MOVIES);// 初始化视频下载目录
        initViews();
        initData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.stopVoice();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        // 退出页面时关闭视频和语音，
        JCVideoPlayer.releaseAllVideos();
        if (mAdapter != null) {
            mAdapter.stopVoice();
        }
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().finish();
            }
        });
        findViewById(R.id.iv_title_left_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().finish();
            }
        });

        mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        mIvTitleRight.setImageResource(R.mipmap.more_icon);
        mIvTitleRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuWindow = new SelectPicPopupWindow(getActivity(), itemsOnClick);
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                menuWindow.showAsDropDown(v,
                        -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                        0);
            }
        });
    }

    public void initViews() {
        more = true;
        mUserId = coreManager.getSelf().getUserId();
        mUserName = coreManager.getSelf().getNickName();
        mergerStatus = findViewById(R.id.mergerStatus);
        rl_title = findViewById(R.id.rl_title);
        // ---------------------------初始化头部-----------------------
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mListView = findViewById(R.id.recyclerView);
        mListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mHeadView = inflater.inflate(R.layout.space_cover_view, mListView, false);
        TextView tv_user_name = mHeadView.findViewById(R.id.tv_user_name);
        tv_user_name.setText(coreManager.getSelf().getNickName());
        ivHeadBg = (ImageView) mHeadView.findViewById(R.id.cover_img);
        ivHeadBg.setOnClickListener(v -> {
            if (UiUtils.isNormalClick(v)) {
                changeBackgroundImage();
            }
        });
        ivHead = (ImageView) mHeadView.findViewById(R.id.avatar_img);
        ivHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UiUtils.isNormalClick(v)) {
                    Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                    startActivity(intent);
                }
            }
        });

        displayAvatar();

        mTipLl = (LinearLayout) mHeadView.findViewById(R.id.tip_ll);
        mTipIv = (ImageView) mHeadView.findViewById(R.id.tip_avatar);
        mTipTv = (TextView) mHeadView.findViewById(R.id.tip_content);
        mTipLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipLl.setVisibility(View.GONE);
                EventBus.getDefault().post(new MessageEventHongdian(0));

                Intent intent = new Intent(getActivity(), NewZanActivity.class);
                intent.putExtra("OpenALL", false); // 是否展示全部还是单条
                startActivity(intent);
            }
        });

        // ---------------------------初始化主视图-----------------------
        mRefreshLayout = findViewById(R.id.refreshLayout);
        mListView.addHeaderView(mHeadView);

        mRefreshLayout.setOnRefreshListener(refreshLayout -> {
            requestData(true);
        });
        mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            requestData(false);
        });

        EventBus.getDefault().register(this);

        mHeadView.findViewById(R.id.btn_send_picture).setOnClickListener(itemsOnClick);
        mHeadView.findViewById(R.id.btn_send_voice).setOnClickListener(itemsOnClick);
        mHeadView.findViewById(R.id.btn_send_video).setOnClickListener(itemsOnClick);
        mHeadView.findViewById(R.id.btn_send_file).setOnClickListener(itemsOnClick);
        mHeadView.findViewById(R.id.new_comment).setOnClickListener(itemsOnClick);

        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int totalScroll;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalScroll += dy;
                if (totalScroll < 0) {
                    totalScroll = 0;
                }
                rl_title.setAlpha(totalScroll > 500 ? 0 : 1f - (Float.valueOf(totalScroll) / 500.0f));
                mergerStatus.setAlpha(totalScroll > 500 ? 1f : Float.valueOf(totalScroll) / 500.0f);
                if (dy > 2) {
                    startTranslateAnim(false);
                }

                if (dy < -4 && mHeadView.getTop() == 0) {
                    startTranslateAnim(true);
                }
            }
        });
    }

    public void startTranslateAnim(boolean show) {
        if (showTitle == show) {
            return;
        }
        showTitle = show;
        float fromy = -300;
        float toy = 0;

        if (!show) {
            fromy = 0;
            toy = -300;
        }
        TranslateAnimation animation = new TranslateAnimation(0, 0, fromy, toy);
        animation.setDuration(500);
        animation.setFillAfter(true);
    }

    private void changeBackgroundImage() {
        SelectImageDialog selectImageDialog = new SelectImageDialog(requireActivity(), new SelectImageDialog.OnSelectImageItemListener() {
            @Override
            public void tvTakePhotoClick() {
                EasyCameraActivity.startForResult(DiscoverFragment.this, REQUEST_CODE_CAPTURE_PHOTO);
            }

            @Override
            public void tvSelectPhotoClick() {
                CameraUtil.pickImageSimple(DiscoverFragment.this, REQUEST_CODE_PICK_PHOTO);
            }
        });
        selectImageDialog.show();
/*
        String[] items = new String[]{getString(R.string.photograph), getString(R.string.album)};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setSingleChoiceItems(items, 0,
                (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(getActivity(), EasyCameraActivity.class);
                        startActivity(intent);
                    } else {
                        CameraUtil.pickImageSimple(DiscoverFragment.this, REQUEST_CODE_PICK_PHOTO);
                    }
                    dialog.dismiss();
                });
        builder.show();
*/
    }

    private void updateBackgroundImage(String path) {
        File bg = new File(path);
        if (!bg.exists()) {
            LogUtils.log(path);
            Reporter.unreachable();
            ToastUtil.showToast(requireContext(), R.string.image_not_found);
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(requireActivity());
        UploadingHelper.uploadFile(coreManager.getSelf().getUserId()
                , new File(path)
                , new UploadingHelper.OnUpFileListener() {
                    @Override
                    public void onSuccess(String url, String filePath) {
                        Map<String, String> params = new HashMap<>();
                        params.put("msgBackGroundUrl", url);

                        HttpUtils.get().url(coreManager.getConfig().USER_UPDATE)
                                .params(params)
                                .build()
                                .execute(new BaseCallback<Void>(Void.class) {

                                    @Override
                                    public void onResponse(ObjectResult<Void> result) {
                                        DialogHelper.dismissProgressDialog();
                                        coreManager.getSelf().setMsgBackGroundUrl(url);
                                        UserDao.getInstance().updateMsgBackGroundUrl(coreManager.getSelf().getUserId(), url);
                                        if (getContext() == null) {
                                            return;
                                        }
                                        displayAvatar();
                                    }

                                    @Override
                                    public void onError(Call call, Exception e) {
                                        DialogHelper.dismissProgressDialog();
                                        if (getContext() == null) {
                                            return;
                                        }
                                        ToastUtil.showErrorNet(requireContext());
                                    }
                                });
                    }

                    @Override
                    public void onFailure(String err, String filePath) {
                        DialogHelper.dismissProgressDialog();
                        if (getContext() == null) {
                            return;
                        }
                        ToastUtil.showToast(requireContext(), err);
                    }
                }, false);

    }

    public void initData() {
        mAdapter = new PublicMessageRecyclerAdapter(getActivity(), coreManager, mMessages);
        mAdapter.setOnMessageActionListener(this);
        mListView.setAdapter(mAdapter);
        requestData(true);
    }

    private void requestData(boolean isPullDownToRefresh) {
        if (isPullDownToRefresh) {// 上拉刷新
            updateTip();
            displayAvatar();
            messageId = null;
            more = true;
        }

        if (!more) {
            // ToastUtil.showToast(getContext(), getString(R.string.tip_last_item));
            mRefreshLayout.setNoMoreData(true);
            refreshComplete();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("pageSize", String.valueOf(PAGER_SIZE));
        if (messageId != null) {
            params.put("messageId", messageId);
        }

        HttpUtils.get().url(coreManager.getConfig().MSG_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<PublicMessage>(PublicMessage.class) {
                    @Override
                    public void onResponse(ArrayResult<PublicMessage> result) {
                        if (getContext() != null && Result.checkSuccess(requireContext(), result)) {
                            List<PublicMessage> data = result.getData();
                            if (isPullDownToRefresh) {
                                mMessages.clear();
                            }
                            if (data != null && data.size() > 0) {
                                int size = data.size();
                                for (PublicMessage m : data) {
                                    mMessages.add(m);
                                    if (!TextUtils.isEmpty(m.getId())) {
                                        size--;
                                    } else {
                                        // 记录最后一条说说的id
                                        messageId = m.getMessageId();
                                    }
                                }
                                if (size == PAGER_SIZE) {
                                    more = true;
                                    mRefreshLayout.resetNoMoreData();
                                } else {
                                    // 服务器返回未满10条，下拉不在去请求
                                    more = false;
                                }
                            } else {
                                // 服务器未返回数据，下拉不再去请求
                                more = false;
                            }
                            mAdapter.notifyDataSetChanged();
                            refreshComplete();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (getContext() != null) {
                            ToastUtil.showNetError(requireContext());
                            refreshComplete();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
            // 拍照返回
            if (VideoRecorderActivity.checkIsImage(data)) {
                // helloEventBus(VideoRecorderActivity.parseImageResult(data));
                // 不直接上传了，先去裁剪
                MessageEventGpu messageEventGpu = (MessageEventGpu) data.getSerializableExtra("image");
                if (messageEventGpu != null) {
                    Uri o = Uri.fromFile(new File(messageEventGpu.event));
                    uri = CameraUtil.getOutputMediaFileUri(requireContext(), CameraUtil.MEDIA_TYPE_IMAGE);
                    CameraUtil.cropImage(requireActivity(), this, o, uri, REQUEST_CODE_CROP_PHOTO, 1, 1, 1000, 1000);
                } else {
                    ToastUtil.showToast(requireContext(), R.string.c_take_picture_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            // 选择背景，相册返回
            if (data != null) {
                String path = CameraUtil.parsePickImageResult(data);
                // updateBackgroundImage(path);
                // 不直接上传了，先去裁剪
                Uri o = Uri.fromFile(new File(path));
                uri = CameraUtil.getOutputMediaFileUri(requireContext(), CameraUtil.MEDIA_TYPE_IMAGE);
                CameraUtil.cropImage(requireActivity(), this, o, uri, REQUEST_CODE_CROP_PHOTO, 1, 1, 1000, 1000);
            } else {
                ToastUtil.showToast(requireContext(), R.string.c_photo_album_failed);
            }
        } else if (requestCode == REQUEST_CODE_CROP_PHOTO) {
            if (uri != null) {
                updateBackgroundImage(uri.getPath());
            } else {
                ToastUtil.showToast(requireContext(), R.string.c_crop_failed);
            }
        } else if (requestCode == REQUEST_CODE_SEND_MSG) {
            // 发布说说成功,刷新Fragment
            String messageId = data.getStringExtra(AppConstant.EXTRA_MSG_ID);
            CircleMessageDao.getInstance().addMessage(mUserId, messageId);
            requestData(true);
            // 刷新广场，
            EventBus.getDefault().post(new EventRefreshPublicSquare());
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventAvatarUploadSuccess message) {
        if (message.event) {
            // 头像更新了，但该界面没有被销毁，不会去重新加载头像，所以这里更新一下
            displayAvatar();
        }
    }

    public void displayAvatar() {
        // 加载小头像，
        AvatarHelper.getInstance().displayAvatar(mUserId, ivHead, true);
        // 优先加载user信息中的背景图片，失败就加载头像，
        String bg = coreManager.getSelf().getMsgBackGroundUrl();
        if (TextUtils.isEmpty(bg)) {
            realDisplayAvatar();
        }
        ImageLoadHelper.loadImageDontAnimateWithPlaceholder(
                requireContext().getApplicationContext(),
                bg,
                R.drawable.avatar_normal,
                d -> {
                    ivHeadBg.setImageDrawable(d);
                }, e -> {
                    realDisplayAvatar();
                }
        );
    }

    private void realDisplayAvatar() {
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(mUserId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(mUserId);
            ImageLoadHelper.loadImageSignatureDontAnimateWithPlaceHolder(
                    MyApplication.getContext(),
                    mOriginalUrl,
                    R.drawable.avatar_normal,
                    time,
                    d -> {
                        ivHeadBg.setImageDrawable(d);
                    }, e -> {
                        Log.e("zq", "加载原图失败：" + mOriginalUrl);
                    }
            );
        } else {
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventVideo msg) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_VIDEO);
        chatMessage.setContent(msg.getUrl());
        chatMessage.setFromUserId(AppConstant.DYNAMIC_INSTANT_ID);
        chatMessage.setFromUserName(AppConstant.DYNAMIC_INSTANT_ID);
        chatMessage.setToUserId(coreManager.getSelf().getUserId());
        int isReadDel = PreferenceUtils.getInt(requireContext(), Constants.MESSAGE_READ_FIRE + coreManager.getSelf().getUserId() + coreManager.getSelf().getUserId(), 0);
        chatMessage.setIsReadDel(isReadDel);
        chatMessage.setUpload(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), chatMessage.getFromUserId(), chatMessage)) {
            Intent intent = new Intent(requireContext(), InstantMessageActivity.class);
            intent.putExtra("fromUserId", chatMessage.getFromUserId());
            intent.putExtra("messageId", chatMessage.getPacketId());
            requireContext().startActivity(intent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventPublic message) {
        collection(message.getMsgId(), message.getUrl(), message.getUserId());
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        // 选择背景，拍照返回
        updateBackgroundImage(message.event);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        if (message.message.equals("prepare")) {
            // 准备播放视频，关闭语音播放
            mAdapter.stopVoice();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageEventNotifyDynamic message) {
        // 收到赞 || 评论 || 提醒我看  || 好友更新动态 协议 刷新页面
        requestData(true);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventComment message) {
        TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(getActivity(),
                getString(R.string.enter_pinlunt),
                str -> {
                    Comment mComment = new Comment();
                    Comment comment = mComment.clone();
                    if (comment == null)
                        comment = new Comment();
                    comment.setBody(str);
                    comment.setUserId(mUserId);
                    comment.setNickName(mUserName);
                    comment.setTime(TimeUtils.sk_time_current_time());
                    addComment(message, comment);
                });
        Window window = trillCommentInputDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
            trillCommentInputDialog.show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventReply message) {
        if (message.event.equals("Reply")) {
            TrillCommentInputDialog trillCommentInputDialog = new TrillCommentInputDialog(getActivity(), getString(R.string.replay) + "：" + message.comment.getNickName(),
                    str -> {
                        Comment mComment = new Comment();
                        Comment comment = mComment.clone();
                        if (comment == null)
                            comment = new Comment();
                        comment.setToUserId(message.comment.getUserId());
                        comment.setToNickname(message.comment.getNickName());
                        comment.setToBody(message.comment.getToBody());
                        comment.setBody(str);
                        comment.setUserId(mUserId);
                        comment.setNickName(mUserId);
                        comment.setTime(TimeUtils.sk_time_current_time());
                        Reply(message, comment);
                    });
            Window window = trillCommentInputDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
                trillCommentInputDialog.show();
            }
        }
    }

    /**
     * 停止刷新动画
     */
    private void refreshComplete() {
        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.finishRefresh();
                mRefreshLayout.finishLoadMore();
            }
        }, 200);
    }

    private void collection(String messageId, String url, String mUserId) {
        Map<String, String> params = new HashMap<>();
        params.put("emoji", collectionParam(messageId, url, mUserId));

        HttpUtils.post().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(requireContext(), requireContext().getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(requireContext(), result.getResultMsg());
                        } else {
                            ToastUtil.showToast(requireContext(), R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(requireContext());
                    }
                });
    }

    private String collectionParam(String messageId, String url, String mUserId) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = 9;
        String msg = url;
        String id = messageId;

        json.put("type", String.valueOf(type));
        json.put("msg", msg);
        json.put("url", msg);
        json.put("collectType", 1);
        json.put("collectMsgId", id);
        json.put("toUserId", mUserId);
        json.put("targetType", 1); // 收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊

        array.add(json);
        return JSON.toJSONString(array);
    }

    private void updateTip() {
        int tipCount = MyZanDao.getInstance().getZanSize(coreManager.getSelf().getUserId());
        if (tipCount == 0) {
            mTipLl.setVisibility(View.GONE);
            EventBus.getDefault().post(new MessageEventHongdian(0));
        } else {
            List<MyZan> zanList = MyZanDao.getInstance().queryZan(coreManager.getSelf().getUserId());
            if (zanList == null || zanList.size() == 0) {
                return;
            }
            MyZan zan = zanList.get(0);
            AvatarHelper.getInstance().displayAvatar(zan.getFromUsername(), zan.getFromUserId(), mTipIv, true);
            mTipTv.setText(tipCount + getString(R.string.piece_new_message));
            mTipLl.setVisibility(View.VISIBLE);
            EventBus.getDefault().post(new MessageEventHongdian(tipCount));
        }
    }

    private void addComment(MessageEventComment message, final Comment comment) {
        String messageId = message.id;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", messageId);
        if (comment.isReplaySomeBody()) {
            params.put("toUserId", comment.getToUserId() + "");
            params.put("toNickname", comment.getToNickname());
            params.put("toBody", comment.getToBody());
        }
        params.put("body", comment.getBody());

        HttpUtils.post().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        if (getContext() != null && Result.checkSuccess(requireContext(), result)) {
                            comment.setCommentId(result.getData());
                            message.pbmessage.setCommnet(message.pbmessage.getCommnet() + 1);
                            PublicMessageRecyclerAdapter.CommentAdapter adapter = (PublicMessageRecyclerAdapter.CommentAdapter) message.view.getAdapter();
                            adapter.addComment(comment);
                            mAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    /**
     * 回复
     */
    private void Reply(MessageEventReply event, final Comment comment) {
        final int position = event.id;
        final PublicMessage message = mMessages.get(position);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());

        if (!TextUtils.isEmpty(comment.getToUserId())) {
            params.put("toUserId", comment.getToUserId());
        }
        if (!TextUtils.isEmpty(comment.getToNickname())) {
            params.put("toNickname", comment.getToNickname());
        }
        params.put("body", comment.getBody());

        HttpUtils.post().url(coreManager.getConfig().MSG_COMMENT_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        // 评论成功
                        if (getContext() != null && Result.checkSuccess(requireContext(), result)) {
                            comment.setCommentId(result.getData());
                            message.setCommnet(message.getCommnet() + 1);
                            PublicMessageRecyclerAdapter.CommentAdapter adapter = (PublicMessageRecyclerAdapter.CommentAdapter) event.view.getAdapter();
                            adapter.addComment(comment);
                            mAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(getActivity());
                    }
                });
    }

    /**
     * 定位到评论位置
     */
    public void showToCurrent(String mCommentId) {
        int pos = -1;
        for (int i = 0; i < mMessages.size(); i++) {
            if (StringUtils.strEquals(mCommentId, mMessages.get(i).getMessageId())) {
                pos = i + 2;
                break;
            }
        }
        // 如果找到就定位到这条说说
        if (pos != -1) {
            mListView.scrollToPosition(pos);
        }
    }
}
