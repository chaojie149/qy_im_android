package com.tongxin.caihong.video;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.downloadTask;
import com.tongxin.caihong.view.SaveVideoDialog;
import com.tongxin.caihong.view.chatHolder.MessageEventClickFire;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import fm.jiecao.jcvideoplayer_lib.JCMediaManager;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JCVideoViewbyXuan;
import fm.jiecao.jcvideoplayer_lib.OnJcvdListener;
import fm.jiecao.jcvideoplayer_lib.VideotillManager;
import okhttp3.Call;

/**
 * 聊天视频播放
 * <p>
 * modify by zq
 * 目前有两个地方会进入此类，且均与chatMessage有关
 * 1.聊天页面视频播放
 * 2.搜索聊天记录视频查看
 */
public class ChatVideoPreviewActivity extends BaseActivity implements View.OnClickListener {
    private static long refreshTime = 50;
    private JCVideoViewbyXuan mVideoView;
    private ProgressBar mLoadBar;
    private RelativeLayout rlControl;
    private ImageView ivStart;
    private TextView tvCurrt, tvTotal;
    private SeekBar mSeekBar;

    private String mVideoPath, mDelPackedID;
    private ChatMessage chatMessage;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), OtherBroadcast.MSG_BACK)) {
                if (chatMessage != null) {
                    String packetId = intent.getStringExtra("packetId");
                    if (TextUtils.equals(packetId, chatMessage.getPacketId())) {
                        finish();
                    }
                }
            }
        }
    };
    private boolean isGounp;
    // 是否显示定位到聊天
    private boolean isTargetChat;
    private String chatId;
    private boolean isTouchSeek;
    private Timer mProgressTimer;
    private long mCurTimer; // 毫秒
    private long mDuration;  // 总时长
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                if (mVideoView.isPlaying()) {
                    if (isTouchSeek) {
                        // todo 拖动之后，不知道getCurrentProgress()什么时候才会变正常，需要做容错处理...
                        isTouchSeek = false;
                        mCurTimer = (long) (mSeekBar.getProgress() / 100.0 * mDuration);
                    } else {
                        // todo getCurrentProgress有个坑，即mediaPlayer调用seekTo方法之后立刻调用getCurrentProgress方法，
                        //  得到的还是seekTo之前的进度,且mediaPlayer setSeekCompleteListener监听不知为何在这里不管用，所以当isTouchSeek为true时，通过计算获取mCurTimer
                        mCurTimer = mVideoView.getCurrentProgress();
                        mCurTimer += refreshTime;
                    }
                    // todo 因为mediaPlayer 的seekTo方法有大问题(问题就是seekTo失败了，有可能不会抛异常，但是mCurTimer一下子变为了拖动到此进度的时间，与当前播放进度对不上)，
                    //  导致不能直接使用mCurTimer += refreshTime的方法去计算mCurTimer，所以需要用getCurrentProgress方法回滚到当前播放进度
                    tvCurrt.setText(TimeUtils.timeParse(mCurTimer));
                    int pro = (int) (mCurTimer / (float) mDuration * 100);
                    mSeekBar.setProgress(pro);
                }
            }
            return false;
        }
    });
    OnJcvdListener jcvdListener = new OnJcvdListener() {
        @Override
        public void onPrepared() {
            mLoadBar.setVisibility(View.GONE);
            mDuration = mVideoView.getDuration();
            mCurTimer = mVideoView.getCurrentProgress();
            tvTotal.setText(TimeUtils.timeParse(mDuration));
            tvCurrt.setText(TimeUtils.timeParse(mCurTimer));
            ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_pause_selector);
        }

        @Override
        public void onPause() {
            ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_play_selector);
        }

        @Override
        public void onCompletion() {
            mCurTimer = 0;
            tvCurrt.setText(TimeUtils.timeParse(mCurTimer));
            mSeekBar.setProgress(0);
            ivStart.setImageResource(fm.jiecao.jcvideoplayer_lib.R.drawable.jc_click_play_selector);
        }

        @Override
        public void onReset() {

        }

        @Override
        public void onError() {

        }
    };
    private int delayTime = 0;
    SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isTouchSeek = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mSeekBar.post(() -> {
                if (!mVideoView.isPlaying()) {
                    delayTime = 500;
                    mVideoView.play(mVideoPath);
                } else {
                    delayTime = 0;
                }
            });
            // 如果为非播放状态下拖动seekBar，需要先播放视频，此时给一个delayTime进行缓冲...，延时delayTime之后在去执行seek代码
            rlControl.postDelayed(() -> {
                try {
                    mCurTimer = (long) (seekBar.getProgress() / 100.0 * mDuration);
                    // mVideoView.seekTo((int) mCurTimer + delayTime > (int) mDuration ? (int) mDuration : (int) mCurTimer + delayTime);
                    // todo  此方法有大问题
                    mVideoView.seekTo((int) mCurTimer);
                    tvCurrt.setText(TimeUtils.timeParse(mCurTimer));
                } catch (IllegalStateException e) {
                    // if the internal player engine has not been initialized
                    isTouchSeek = false;
                    ToastUtil.showToast(mContext, R.string.tip_seek_failed);
                }
                handler.sendEmptyMessage(1);
            }, delayTime);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview_chat);
        mVideoPath = getIntent().getStringExtra(AppConstant.EXTRA_VIDEO_FILE_PATH);
        // 表示为阅后即焚消息 msgId
        mDelPackedID = getIntent().getStringExtra("DEL_PACKEDID");
        if (!TextUtils.isEmpty(mDelPackedID)) {
            // 不支持截屏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
        String json = getIntent().getStringExtra("json");
        chatMessage = JSON.parseObject(json, ChatMessage.class);
        isGounp = getIntent().getBooleanExtra("isGounp", false);
        // 是否显示定位到聊天
        isTargetChat = getIntent().getBooleanExtra("isTargetChat", false);
        if (isTargetChat) {
            chatId = getIntent().getStringExtra("chatId");
        }
        initActionBar();
        initView();
        initEvent();
        if (!TextUtils.isEmpty(mDelPackedID)) {
            // 发送广播去更新聊天界面，移除该message
            EventBus.getDefault().post(new MessageEventClickFire("delete", mDelPackedID));
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(OtherBroadcast.MSG_BACK);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            if (!JCMediaManager.instance().mediaPlayer.isPlaying() && JCMediaManager.instance().mediaPlayer.getCurrentPosition() > 1) {
                JCMediaManager.instance().mediaPlayer.start();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onStop() {
        try {
            if (JCMediaManager.instance().mediaPlayer.isPlaying()) {
                JCMediaManager.instance().mediaPlayer.pause();
            }
        } catch (Exception ignored) {
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        doBack();
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
    }

    private void initView() {
        mVideoView = findViewById(R.id.x_video);
        mLoadBar = findViewById(R.id.loading);
        rlControl = findViewById(R.id.rl_control);
        ivStart = findViewById(R.id.iv_start);
        tvTotal = findViewById(R.id.total);
        tvCurrt = findViewById(R.id.current);
        mSeekBar = findViewById(R.id.bottom_seek_progress);

        mVideoView.setLoop(false); // 禁止循环播放
        mVideoView.addOnJcvdListener(jcvdListener);
        mSeekBar.setOnSeekBarChangeListener(seekBarListener);

        mVideoView.play(mVideoPath);

        mProgressTimer = new Timer();
        mProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isTouchSeek) {// 非拖动状态
                    handler.sendEmptyMessage(1);
                }
            }
        }, 0, refreshTime);// 每隔refreshTime ms刷新一次进度条
    }

    private void initEvent() {
        findViewById(R.id.back_tiny).setOnClickListener(this);
        ivStart.setOnClickListener(this);
        mVideoView.setOnClickListener(this);
        rlControl.setOnClickListener(this);
        rlControl.setOnLongClickListener(v -> {
            if (!TextUtils.isEmpty(mDelPackedID)) {
                ToastUtil.showToast(mContext, getString(R.string.tip_burn_image_cannot_more));
                return false;
            }
            SaveVideoDialog saveVideoDialog = new SaveVideoDialog(ChatVideoPreviewActivity.this, isTargetChat, new SaveVideoDialog.OnSavaVideoDialogClickListener() {
                @Override
                public void tvTargetChatClick() {
                    Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), chatId);
                    if (friend == null) {
                        ToastUtil.showToast(mContext, getString(R.string.unknown));
                        return;
                    }
                    if (chatMessage != null) {
                        if (!isGounp) {
                            ChatActivity.start(mContext, friend, true, chatMessage.getDoubleTimeSend(), chatMessage.getPacketId());
                        } else {
                            MucChatActivity.start(mContext, friend, true, chatMessage.getDoubleTimeSend(), chatMessage.getPacketId());
                        }
                    }
                }

                @Override
                public void tv1Click() {
                    File file = new File(mVideoPath);
                    ContentResolver localResolver = ChatVideoPreviewActivity.this.getContentResolver();
                    ContentValues localContentValue = getVideoContentValues(file, System.currentTimeMillis());
                    localResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValue);
                    ToastUtil.showToast(ChatVideoPreviewActivity.this, R.string.tip_video_save_success);
                    if (mVideoPath.startsWith("http")) {
                        String path = MyApplication.getInstance().mVideosDir + "/" +
                                mVideoPath.substring(mVideoPath.length() - 6, mVideoPath.length() - 4) + ".mp4";
                        File f = new File(path);
                        if (!f.exists()) {
                            new downloadTask(mVideoPath, 2, path).start();
                            ContentResolver localContentResolver = ChatVideoPreviewActivity.this.getContentResolver();
                            ContentValues localContentValues = getVideoContentValues(f, System.currentTimeMillis());
                            localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
                            ToastUtil.showToast(ChatVideoPreviewActivity.this, R.string.tip_video_save_success);
                        } else {
                            ToastUtil.showToast(ChatVideoPreviewActivity.this, R.string.tip_video_exists);
                        }
                    }
                }

                @Override
                public void tv2Click() {
                    Intent intent = new Intent(mContext, InstantMessageActivity.class);
                    intent.putExtra("fromUserId", chatMessage.getToUserId());
                    intent.putExtra("messageId", chatMessage.getPacketId());
                    mContext.startActivity(intent);
                    ((Activity) mContext).finish();
                }

                @Override
                public void tv3Click() {
                    collectionEmotion(chatMessage, isGounp);
                }
            });
            saveVideoDialog.show();
            return false;
        });
    }

    private void doBack() {
        if (mProgressTimer != null) {
            mProgressTimer.cancel();
        }
        VideotillManager.instance().releaseVideo();
        // 有可能在查看时强行关闭app，导致可以无限查看，直接在进入该activity时删除
/*
        if (!TextUtils.isEmpty(mDelPackedID)) {
            // 发送广播去更新聊天界面，移除该message
            EventBus.getDefault().post(new MessageEventClickFire("delete", mDelPackedID));
        }
*/
    }

    public void collectionEmotion(ChatMessage message, boolean isGroup) {
        if (TextUtils.isEmpty(message.getContent())) {
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        String emoji = collectionParam(Collections.singletonList(message), isGroup);
        if (TextUtils.isEmpty(emoji)) {
            DialogHelper.dismissProgressDialog();
            ToastUtil.showToast(mContext, getString(R.string.tip_program_error));
            return;
        }
        params.put("emoji", emoji);

        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(mContext, mContext.getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private String collectionParam(List<ChatMessage> messageList, boolean isGroup) {
        JSONArray array = new JSONArray();
        for (ChatMessage message : messageList) {
            int type = CollectionEvery.TYPE_VIDEO;
            JSONObject json = new JSONObject();
            json.put("type", String.valueOf(type));
            json.put("msg", message.getContent());
            // 收藏消息id
            json.put("msgId", message.getPacketId());
            if (isGroup) {
                // 群组收藏需要添加jid
                json.put("roomJid", message.getToUserId());
                // targetId为roomId，非jid
                // json.put("targetId", message.getToUserId());
                Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), message.getToUserId());
                if (friend != null) {
                    // friend为空，直接不让收藏成功，
                    json.put("targetId", friend.getRoomId());
                } else {
                    return "";
                }
                json.put("targetType", 3);
                json.put("toUserId", message.getFromUserId());
            } else {
                json.put("userId", message.getFromUserId());
                json.put("targetId", message.getFromUserId());
                json.put("targetType", 4);
                json.put("toUserId", message.getFromUserId());
            }
            array.add(json);
        }
        return JSON.toJSONString(array);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_tiny) {
            finish();
        } else if (v.getId() == R.id.iv_start) {
            if (mVideoView.mCurrState == JCVideoPlayer.CURRENT_STATE_PLAYING) {
                mVideoView.pause();
            } else if (mVideoView.mCurrState != JCVideoPlayer.CURRENT_STATE_ERROR) {
                mVideoView.play(mVideoPath);
            }
        } else {
            if (rlControl.getVisibility() == View.VISIBLE) {
                rlControl.setVisibility(View.GONE);
            } else {
                rlControl.setVisibility(View.VISIBLE);
            }
        }
    }

    private ContentValues getVideoContentValues(File paramFile, long paramLong) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("title", paramFile.getName());
        localContentValues.put("_display_name", paramFile.getName());
        localContentValues.put("mime_type", "video/mp4");
        localContentValues.put("datetaken", Long.valueOf(paramLong));
        localContentValues.put("date_modified", Long.valueOf(paramLong));
        localContentValues.put("date_added", Long.valueOf(paramLong));
        localContentValues.put("_data", paramFile.getAbsolutePath());
        localContentValues.put("_size", Long.valueOf(paramFile.length()));
        return localContentValues;
    }
}
