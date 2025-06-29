package com.tongxin.caihong.ui.me.sendgroupmessage;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.SelectFriendItem;
import com.tongxin.caihong.bean.VideoFile;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.downloader.Downloader;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.UploadEngine;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.map.MapPickerActivity;
import com.tongxin.caihong.ui.me.LocalVideoActivity;
import com.tongxin.caihong.ui.me.collection.MyCollection;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.RecorderUtils;
import com.tongxin.caihong.util.SmileyParser;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.VideoCompressUtil;
import com.tongxin.caihong.util.filter.RegexInputFilter;
import com.tongxin.caihong.util.secure.MD5;
import com.tongxin.caihong.video.EasyCameraActivity;
import com.tongxin.caihong.video.MessageEventGpu;
import com.tongxin.caihong.video.VideoRecorderActivity;
import com.tongxin.caihong.view.ChatContentView;
import com.tongxin.caihong.view.SelectCardPopupWindow;
import com.tongxin.caihong.view.SelectFileActivity;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.photopicker.PhotoPickerActivity;
import com.tongxin.caihong.view.photopicker.SelectModel;
import com.tongxin.caihong.view.photopicker.intent.PhotoPickerIntent;
import com.tongxin.caihong.xmpp.CoreService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import Jni.VideoUitls;
import VideoHandle.OnEditorListener;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;


/**
 * 群发消息
 */
public class ChatActivityForSendGroup extends BaseActivity implements
        ChatBottomForSendGroup.ChatBottomListener, SelectCardPopupWindow.SendCardS {
    // 相册、视频、位置
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_SELECT_VIDE0 = 3;
    private static final int REQUEST_CODE_SEND_COLLECTION = 4;
    private static final int REQUEST_CODE_SELECT_Locate = 5;
    public static boolean isAlive;
    public static String tempData;
    private boolean isSending;// 标志位，防止多次点击
    private TextView mCountTv;
    private TextView mNameTv;
    private ChatContentView mChatContentView;
    private ChatBottomForSendGroup mChatBottomView;
    private CoreService mService;
    private String mLoginUserId;
    private String mLoginNickName;
    private List<SelectFriendItem> friendItemList;
    private List<SelectFriendItem> mCloneFriendItemList;
    private Map<String, Friend> friendCache = new HashMap<>();
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((CoreService.CoreServiceBinder) service).getService();
        }
    };
    private Toast mToast;
    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {

        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            message.setUpload(true);
            message.setUploadSchedule(100);
            send(message);
        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {
            DialogHelper.dismissProgressDialog();
            Toast.makeText(mContext, getString(R.string.upload_failed), Toast.LENGTH_SHORT).show();
        }
    };

    public static void start(Context ctx, Collection<SelectFriendItem> items) {
        Intent intent = new Intent(ctx, ChatActivityForSendGroup.class);
        // 数据量过大，导致了TransactionTooLargeException，改为静态变量
        // intent.putExtra("items", JSON.toJSONString(items));
        tempData = JSON.toJSONString(items);
        ctx.startActivity(intent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_for_sg);
        SmileyParser.getInstance(MyApplication.getContext()).notifyUpdate();

        isAlive = true;

        // String items = getIntent().getStringExtra("items");
        friendItemList = JSON.parseArray(tempData, SelectFriendItem.class);
        mCloneFriendItemList = new ArrayList<>(friendItemList);
        tempData = "";

        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();

        bindService(CoreService.getIntent(), mConnection, BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + mLoginUserId
                + File.separator + Environment.DIRECTORY_MUSIC);

        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChatBottomView != null) {
            mChatBottomView.addRecordListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mChatBottomView != null) {
            mChatBottomView.removeRecordListener();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isAlive = false;
        // unbindService(mConnection);
        if (mChatBottomView != null) {
            mChatBottomView.recordCancel();
        }
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.mass));
    }

    private void initView() {
        mCountTv = (TextView) findViewById(R.id.send_size_tv);
        mNameTv = (TextView) findViewById(R.id.send_name_tv);
        showNameList();

        mChatContentView = findViewById(R.id.chat_content_view);
        mChatContentView.setNeedRefresh(false);
        mChatBottomView = (ChatBottomForSendGroup) findViewById(R.id.chat_bottom_view);
        mChatBottomView.setChatBottomListener(this);
    }

    private void showNameList() {
        mCountTv.setText(getString(R.string.you_will_send_a_message_to) + friendItemList.size() + getString(R.string.bit) + getString(R.string.friend));
        final StringBuilder userNames = new StringBuilder();
        userNames.append(friendItemList.get(0).getName());
        for (int i = 1; i < friendItemList.size(); i++) {
            userNames.append(",");
            userNames.append(friendItemList.get(i).getName());
        }
        mNameTv.setText(userNames);
    }

    private void setSameParams(ChatMessage message) {
        DialogHelper.showDefaulteMessageProgressDialogAddCancel(this, null);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setIsReadDel(0);
        message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));
        sendMessage(message);
    }

    private void sendMessage(ChatMessage message) {
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), message.getToUserId(), message, mUploadResponse, false);
            } else {
                message.setUpload(true);
                message.setUploadSchedule(100);
                send(message);
            }
        } else {
            send(message);
        }
    }

    private void send(ChatMessage oMessage) {
        new Thread(() -> {
            for (int i = 0; i < friendItemList.size(); i++) {
                try {
                    Thread.sleep(100);// 每发送一条消息给100ms的时间缓冲
                    // 加密可能影响到消息对象复用，所以拷贝一份，
                    ChatMessage message = oMessage.clone(false);
                    message.setMySend(true);
                    SelectFriendItem item = friendItemList.get(i);
                    if (checkGroupStatus(item)) {
                        friendItemList.remove(i);
                        mCloneFriendItemList.remove(i);
                        i--;
                        showNameList();
                        continue;
                    }
                    String userId = item.getUserId();
                    message.setToUserId(userId);
                    message.setUploadSchedule(oMessage.getUploadSchedule());
                    message.setUpload(oMessage.isUpload());
                    message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                    message.setTimeSend(TimeUtils.sk_time_current_time());
                    // 群发消息存本地但不更新朋友表
                    if (ChatMessageDao.getInstance().saveChatMessageWithOutRefreshFriend(mLoginUserId, userId, message)) {
                        if (item.getIsRoom() == 1) {
                            coreManager.sendMucChatMessage(userId, message);
                        } else {
                            coreManager.sendChatMessage(userId, message);
                        }
                    }
                    // 群发消息不存盘
/*
                    if (item.getIsRoom() == 1) {
                        // 群组存本地，不过标记为过期，因为拉离线消息有可能拉到此条消息
                        message.setIsExpired(1);
                        ChatMessageDao.getInstance().saveRoamingChatMessage(mLoginUserId, userId, message, true);
                        coreManager.sendMucChatMessage(userId, message);
                    } else {
                        coreManager.sendChatMessage(userId, message);
                    }
*/
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean checkGroupStatus(SelectFriendItem item) {
        Friend friend = getFriend(item);
        if (friend == null) {
            return false;
        }
        if (friend.getRoomFlag() != 0) {
            // 用户可能不在群组里，
            int status = friend.getGroupStatus();
            if (1 == status) {
                toast(R.string.tip_been_kick);
                return true;
            } else if (2 == status) {
                toast(R.string.tip_disbanded);
                return true;
            } else if (3 == status) {
                toast(R.string.tip_group_disable_by_service);
                return true;
            }
            RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(friend.getRoomId(), mLoginUserId);
            boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + friend.getUserId(), false);
            // 判断禁言状态，
            if (member != null && member.getRole() == 3) {// 普通成员需要判断是否被禁言
                if (friend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                    toast(R.string.has_been_banned);
                    return true;
                }
                if (isAllShutUp) {
                    toast(R.string.has_been_banned);
                    return true;
                }
            } else if (member == null) {// 也需要判断是否被禁言
                if (friend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                    toast(R.string.has_been_banned);
                    return true;
                }
                if (isAllShutUp) {
                    toast(R.string.has_been_banned);
                    return true;
                }
            }
            if (member != null && member.getRole() == 4) {
                toast(R.string.hint_invisible);
                return true;
            }
        }
        return false;
    }

    private void toast(int stringId) {
        runOnUiThread(() -> {
            if (mToast == null) {
                mToast = Toast.makeText(mContext, stringId, Toast.LENGTH_SHORT);
            } else {
                mToast.setText(stringId);
            }
            mToast.show();
        });
    }

    private Friend getFriend(SelectFriendItem item) {
        Friend ret = friendCache.get(item.getUserId());
        if (ret == null) {
            ret = FriendDao.getInstance().getFriend(mLoginUserId, item.getUserId());
            friendCache.put(item.getUserId(), ret);
        }
        return ret;
    }

    @Override
    public void stopVoicePlay() {
        VoicePlayer.instance().stop();
    }

    @Override
    public void sendVoice(String filePath, int timeLen) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VOICE);
        message.setContent("");
        message.setFilePath(filePath);
        File file = new File(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        setSameParams(message);
    }

    @Override
    public void sendText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(text);
        setSameParams(message);
    }

    @Override
    public void sendGif(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        // 防止多次点击
        if (isSending) {
            return;
        }
        isSending = true;

        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setContent(text);
        setSameParams(message);
    }

    @Override
    public void sendCollection(String collection) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent(collection);
        message.setUpload(true);// 已上传服务器
        setSameParams(message);
    }

    public void sendImage(File file) {
        if (!file.exists()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        int[] imageParam = BitmapUtil.getImageParamByIntsFile(filePath);
        message.setLocation_x(String.valueOf(imageParam[0]));
        message.setLocation_y(String.valueOf(imageParam[1]));
        setSameParams(message);
    }

    public void sendVideo(File file) {
        if (!file.exists()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        setSameParams(message);
    }

    private void requestFilePassword(AsyncUtils.Function<String> onSuccess) {
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(getString(R.string.app_name), getString(R.string.ask_need_file_password),
                getString(R.string.no), getString(R.string.yes), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {
                        try {
                            onSuccess.apply(null);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }

                    @Override
                    public void confirmClick() {
                        DialogHelper.showLimitSingleInputDialog((Activity) mContext
                                , getString(R.string.file_password)
                                , "", getString(R.string.hint_input_file_password)
                                , new InputFilter[]{new RegexInputFilter("[a-zA-Z0-9]*")}
                                , text -> {
                                    if (TextUtils.isEmpty(text)) {
                                        ToastUtil.showToast(mContext, R.string.tip_file_password_empty);
                                        return false;
                                    }
                                    if (!TextUtils.isEmpty(text)) {
                                        text = MD5.encryptHex(text);
                                    }
                                    try {
                                        onSuccess.apply(text);
                                    } catch (Exception e) {
                                        throw new IllegalStateException(e);
                                    }
                                    return true;
                                }).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    }
                });
        selectionFrame.show();
    }

    public void sendFile(File file, String password) {
        if (!file.exists()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_FILE);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        long fileSize = file.length();
        message.setFileSize((int) fileSize);
        if (!TextUtils.isEmpty(password)) {
            message.setObjectId(password);
        }
        setSameParams(message);
    }

    public void sendLocate(double latitude, double longitude, String address, String snapshot) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setContent("");
        message.setFilePath(snapshot);
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setObjectId(address);
        setSameParams(message);
    }

    public void sendCard(Friend friend) {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setContent(friend.getNickName());
        message.setObjectId(friend.getUserId());
        setSameParams(message);
    }

    @Override
    public void clickPhoto() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(ChatActivityForSendGroup.this);
        intent.setSelectModel(SelectModel.SINGLE);
        intent.setSelectedPaths(imagePaths);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickCamera() {
        EasyCameraActivity.startForResult(this, REQUEST_CODE_CAPTURE_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickVideo() {
        Intent intent = new Intent(mContext, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, false);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDE0);
    }

    @Override
    public void clickFile() {
        SelectFileActivity.start(this, new SelectFileActivity.OptionFileListener() {
            @Override
            public void option(List<File> files) {
                if (files != null && files.size() > 0) {
                    requestFilePassword(s -> {
                        for (int i = 0; i < files.size(); i++) {
                            sendFile(files.get(i), s);
                        }
                    });
                }
            }

            @Override
            public void intent() {

            }
        });
    }

    @Override
    public void clickCollection() {
        Intent intent = new Intent(this, MyCollection.class);
        intent.putExtra("IS_SEND_COLLECTION", true);
        startActivityForResult(intent, REQUEST_CODE_SEND_COLLECTION);
    }

    @Override
    public void clickDice() {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_DICE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        int a = (int) (Math.random() * 6 + 1);
        message.setContent("" + a);
        setSameParams(message);
    }

    @Override
    public void clickRPS() {
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_RPS);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        int a = (int) (Math.random() * 3 + 1);
        message.setContent("" + a);
        setSameParams(message);
    }

    @Override
    public void clickLocation() {
        Intent intent = new Intent(mContext, MapPickerActivity.class);
        intent.putExtra(AppConstant.EXTRA_FORM_CAHT_ACTIVITY, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_Locate);
    }

    @Override
    public void clickCard() {
        for (SelectFriendItem friendItem : friendItemList) {
            if (friendItem.getIsRoom() == 1) {
                boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + friendItem.getUserId(), true);
                if (!isAllowSecretlyChat) {
                    Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), friendItem.getUserId());
                    RoomMember selfMember = RoomMemberDao.getInstance().getSingleRoomMember(friend.getRoomId(), coreManager.getSelf().getUserId());
                    if (selfMember == null || (selfMember.getRole() != 1 && selfMember.getRole() != 2)) {
                        // 被限制发送名片，
                        DialogHelper.tipDialog(mContext, getString(R.string.tip_group_disallowed_send_card, friendItem.getName()));
                        return;
                    }
                }
            }
        }
        SelectCardPopupWindow mSelectCardPopupWindow = new SelectCardPopupWindow(this, this);
        mSelectCardPopupWindow.showAtLocation(findViewById(R.id.root_view),
                Gravity.CENTER, 0, 0);
    }

    @Override
    public void sendCardS(List<Friend> friends) {
        for (int i = 0; i < friends.size(); i++) {
            sendCard(friends.get(i));
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {// 拍照返回
        photograph(new File(message.event));
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        Iterator<SelectFriendItem> iterator = mCloneFriendItemList.iterator();
        while (iterator.hasNext()) {
            SelectFriendItem item = iterator.next();
            String s = item.getUserId();
            if (message.message.equals(s)) {// 该条消息发送成功
                iterator.remove();
                if (mCloneFriendItemList.size() == 0) {// 最后一条消息也发送成功 更新消息页面
                    Log.e("TAG", "over: " + s);
                    ToastUtil.showToast(mContext, getString(R.string.success));
                    DialogHelper.dismissProgressDialog();
                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getInstance());
                    // 可能还需要继续发送
                    mCloneFriendItemList = new ArrayList<>(friendItemList);
                    mChatBottomView.reset(true);
                    // sendBroadcast(new Intent(OtherBroadcast.SEND_MULTI_NOTIFY));
                    // finish();
                }
                break;
            }
        }
    }

    private void compress(File file) {
        String path = file.getPath();
        DialogHelper.showMessageProgressDialog(this, MyApplication.getContext().getString(R.string.compressed));
        final String out = RecorderUtils.getVideoFileByTime();
        String[] cmds = RecorderUtils.ffmpegComprerssCmd(path, out);
        long duration = VideoUitls.getDuration(path);

        VideoCompressUtil.exec(cmds, duration, new OnEditorListener() {
            public void onSuccess() {
                DialogHelper.dismissProgressDialog();
                File outFile = new File(out);
                runOnUiThread(() -> {
                    if (outFile.exists()) {
                        sendVideo(outFile);
                    } else {
                        sendVideo(file);
                    }
                });
            }

            public void onFailure() {
                DialogHelper.dismissProgressDialog();
                runOnUiThread(() -> {
                    sendVideo(file);
                });
            }

            public void onProgress(float progress) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO && resultCode == RESULT_OK) {// 拍照返回
            if (VideoRecorderActivity.checkIsImage(data)) {
                helloEventBus(VideoRecorderActivity.parseImageResult(data));
            }
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO && resultCode == RESULT_OK) {// 相册返回
            if (data != null) {
                boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
            } else {
                ToastUtil.showToast(this, R.string.c_photo_album_failed);
            }
        } else if (requestCode == REQUEST_CODE_SELECT_VIDE0 && resultCode == RESULT_OK) {// 选中视频返回
            if (data == null) {
                return;
            }
            String json = data.getStringExtra(AppConstant.EXTRA_VIDEO_LIST);
            List<VideoFile> fileList = JSON.parseArray(json, VideoFile.class);
            if (fileList == null || fileList.size() == 0) {
                // 不可到达，列表里有做判断，
                Reporter.unreachable();
            } else {
                for (VideoFile videoFile : fileList) {
                    String filePath = videoFile.getFilePath();
                    if (TextUtils.isEmpty(filePath)) {
                        // 不可到达，列表里有做过滤，
                        Reporter.unreachable();
                    } else {
                        File file = new File(filePath);
                        if (!file.exists()) {
                            // 不可到达，列表里有做过滤，
                            Reporter.unreachable();
                        } else {
                            compress(file);
                        }
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_SEND_COLLECTION && resultCode == RESULT_OK) {
            boolean isDynamicTrill = data.getBooleanExtra("isDynamicTrill", false);
            String json = data.getStringExtra("data");
            if (isDynamicTrill) {
                PublicMessage publicMessage = JSON.parseObject(json, PublicMessage.class);
                if (publicMessage != null) {
                    ChatMessage message = MessageUtil.generateShareMessage2Intent(mContext
                            , mLoginUserId
                            , publicMessage.getTargetType() == PublicMessage.CIRCLE ? 0 : 1, publicMessage
                            , false);
                    setSameParams(message);
                }
            } else {
                CollectionEvery collection = JSON.parseObject(json, CollectionEvery.class);
                clickCollectionSend(collection);
            }
        } else if (requestCode == REQUEST_CODE_SELECT_Locate && resultCode == RESULT_OK) {// 选择位置的返回
            double latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            double longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            String address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
            String snapshot = data.getStringExtra(AppConstant.EXTRA_SNAPSHOT);

            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)
                    && !TextUtils.isEmpty(snapshot)) {
                sendLocate(latitude, longitude, address, snapshot);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.loc_startlocnotice));
            }
        }
    }

    // 单张图片压缩 拍照
    private void photograph(final File file) {
        Log.e("zq", "压缩前图片路径:" + file.getPath() + "压缩前图片大小:" + file.length() / 1024 + "KB");
        // 拍照出来的图片Luban一定支持，
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // 原图小于100kb 不压缩
                // .putGear(2)     // 设定压缩档次，默认三挡
                // .setTargetDir() // 指定压缩后的图片路径
                .setCompressListener(new OnCompressListener() { // 设置回调
                    @Override
                    public void onStart() {
                        Log.e("zq", "开始压缩");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.e("zq", "压缩成功，压缩后图片位置:" + file.getPath() + "压缩后图片大小:" + file.length() / 1024 + "KB");
                        sendImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("zq", "压缩失败,原图上传");
                        sendImage(file);
                    }
                }).launch();// 启动压缩
    }

    // 多张图片压缩 相册
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// 原图发送，不压缩
            Log.e("zq", "原图发送，不压缩，开始发送");
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                sendImage(new File(stringArrayListExtra.get(i)));
            }
            Log.e("zq", "原图发送，不压缩，发送结束");
            return;
        }

        List<String> list = new ArrayList<>();
        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            // Luban只处理特定后缀的图片，不满足的不处理也不走回调，
            // 只能挑出来不压缩，
            List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp");
            boolean support = false;
            for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                    support = true;
                    break;
                }
            }
            if (!support) {
                list.add(stringArrayListExtra.get(i));
                fileList.add(new File(stringArrayListExtra.get(i)));
            }
        }

        if (fileList.size() > 0) {
            for (File file : fileList) {// 不压缩的部分，直接发送
                sendImage(file);
            }
        }

        // 移除掉不压缩的图片
        stringArrayListExtra.removeAll(list);

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// 原图小于100kb 不压缩
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        sendImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// 启动压缩
    }

    private void clickCollectionSend(CollectionEvery collection) {
        // 不管什么收藏消息类型，都可能有文字，单独发一条文字消息，
        if (!TextUtils.isEmpty(collection.getCollectContent())) {
            sendText(collection.getCollectContent());
        }
        int type = collection.getXmppType();
        if (type == XmppMessage.TYPE_TEXT) {
            // 文字消息发出了文字就可以结束了，
            return;
        } else if (type == XmppMessage.TYPE_IMAGE) {
            // 图片可能有多张，分开发送，
            String allUrl = collection.getUrl();
            for (String url : allUrl.split(",")) {
                clickCollectionSend(type, url, collection.getFileLength(), collection.getFileName(), collection.getFileSize());
            }
            return;
        } else if (type == XmppMessage.TYPE_LINK) {
            clickCollectionSend(type, collection.getMsg(), collection.getFileLength(), collection.getFileName(), collection.getFileSize());
            return;
        }
        clickCollectionSend(type, collection.getUrl(), collection.getFileLength(), collection.getFileName(), collection.getFileSize());
    }


    private void clickCollectionSend(
            int type,
            String content,
            int timeLen,
            String filePath,
            long fileSize
    ) {
        if (TextUtils.isEmpty(content)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(type);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(content);
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        message.setUpload(true);
        if (!TextUtils.isEmpty(filePath)) {
            message.setFilePath(filePath);
        }
        setSameParams(message);
    }
}
