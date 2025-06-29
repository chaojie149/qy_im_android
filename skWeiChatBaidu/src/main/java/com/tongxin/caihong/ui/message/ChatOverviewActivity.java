package com.tongxin.caihong.ui.message;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.adapter.ChatOverviewAdapter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.PlayUploadUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.view.SaveWindow;
import com.tongxin.caihong.view.imageedit.IMGEditActivity;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 多张图片预览
 * <p>
 * modify by zq
 * 目前有两个地方会进入此类，且均与chatMessage有关
 * 1.聊天页面图片查看
 * 2.搜索聊天记录图片查看
 */
public class ChatOverviewActivity extends BaseActivity {
    public static final int REQUEST_IMAGE_EDIT = 1;
    public static String imageChatMessageListStr;
    private ViewPager mViewPager;
    private ChatOverviewAdapter mChatOverviewAdapter;
    private List<ChatMessage> mChatMessages;
    // 首次显示的position
    private int mFirstShowPosition;
    // 是否为群组
    private boolean isGroup;
    // 是否显示定位到聊天
    private boolean isTargetChat;
    private String chatId;

    // 当前显示的position
    private int thisPosition;
    // 当前显示的position的url
    private String mCurrentShowUrl;

    // 编辑之后的图片路径
    private String mEditedPath;
    private SaveWindow mSaveWindow;
    private My_BroadcastReceivers my_broadcastReceiver = new My_BroadcastReceivers();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableSwipeBack = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_overview);
        // imageChatMessageListStr = getIntent().getStringExtra("imageChatMessageList");
        mChatMessages = JSON.parseArray(imageChatMessageListStr, ChatMessage.class);
        imageChatMessageListStr = "";
        if (mChatMessages == null) {
            finish();
            return;
        }
        mFirstShowPosition = getIntent().getIntExtra("imageChatMessageList_current_position", 0);
        isGroup = getIntent().getBooleanExtra("isGroup", false);
        isTargetChat = getIntent().getBooleanExtra("isTargetChat", false);
        if (isTargetChat) {
            chatId = getIntent().getStringExtra("chatId");
        }
        getCurrentShowUrl(mFirstShowPosition);

        initView();
        register();
    }

    @Override
    protected void onDestroy() {
        if (my_broadcastReceiver != null) {
            try {
                unregisterReceiver(my_broadcastReceiver);
            } catch (Exception e) {
                // 以防万一，
            }
        }
        super.onDestroy();
    }

    private void getCurrentShowUrl(int position) {
        this.thisPosition = position;
        if (position >= mChatMessages.size()) {
            // 以防万一，静态变量可能导致各种无法预料的崩溃，
            return;
        }
        ChatMessage chatMessage = mChatMessages.get(position);
        String filePath = UploadCacheUtils.get(mContext, chatMessage);
        if (!TextUtils.isEmpty(filePath) && FileUtil.isExist(filePath)) {
            mCurrentShowUrl = filePath;
        } else {
            mCurrentShowUrl = chatMessage.getContent();
        }
    }

    private void initView() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mViewPager = findViewById(R.id.chat_overview_vp);
        mChatOverviewAdapter = new ChatOverviewAdapter(this, mChatMessages);
        mViewPager.setAdapter(mChatOverviewAdapter);
        mViewPager.setCurrentItem(mFirstShowPosition);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {
                getCurrentShowUrl(arg0);
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
        filter.addAction(OtherBroadcast.MSG_BACK);
        registerReceiver(my_broadcastReceiver, filter);
    }

    public void collectionEmotion(ChatMessage message, boolean isGroup, String emoji) {
        if (TextUtils.isEmpty(message.getContent())) {
            return;
        }
        if (message.getTimeLen() == 1
                && TextUtils.isEmpty(emoji)) {
            // 编辑的图片，先上传，得到新url，在调用收藏方法，传入封装好的参数
            PlayUploadUtil.uploadSingle(mContext, message.getFilePath(), new PlayUploadUtil.UploadSingleCallback() {
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
                    collectionEmotion(message, isGroup, JSON.toJSONString(array));
                }
            });
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap<>();
        if (TextUtils.isEmpty(emoji)) {
            emoji = collectionParam(Collections.singletonList(message), isGroup);
            if (TextUtils.isEmpty(emoji)) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(mContext, getString(R.string.tip_program_error));
                return;
            }
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
            int type = CollectionEvery.TYPE_IMAGE;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_EDIT:
                    mCurrentShowUrl = mEditedPath;
                    ChatMessage chatMessage = mChatMessages.get(mViewPager.getCurrentItem());
                    chatMessage.setFilePath(mCurrentShowUrl);
                    // 标记编辑成功
                    chatMessage.setTimeLen(1);
                    mChatMessages.set(mViewPager.getCurrentItem(), chatMessage);
                    mChatOverviewAdapter.refreshItem(mCurrentShowUrl, mViewPager.getCurrentItem());
                    // 模拟那个长按，弹出菜单，
                    Intent intent = new Intent(OtherBroadcast.longpress);
                    sendBroadcast(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class My_BroadcastReceivers extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(OtherBroadcast.singledown)) {
                finish();
            } else if (intent.getAction().equals(OtherBroadcast.longpress)) {
                // 长按屏幕，弹出菜单
                if (mCurrentShowUrl.contains("http")) {
                    ImageLoadHelper.loadBitmapDontAnimate(mContext, mCurrentShowUrl, b -> {
                        String text = BitmapUtil.getTextQRCode(ChatOverviewActivity.this, b);
                        mSaveWindow = new SaveWindow(ChatOverviewActivity.this
                                , true
                                , true
                                , !TextUtils.isEmpty(text)
                                , isTargetChat
                                , new ClickListener(text));
                        mSaveWindow.show();
                    }, e -> {
                        // todo load bitmap failed
                    });
                } else {
                    // 阅后即焚已被过滤了，不需要做阅后即焚的判断
                    String text = BitmapUtil.getTextQRCode(ChatOverviewActivity.this, mCurrentShowUrl);
                    mSaveWindow = new SaveWindow(ChatOverviewActivity.this
                            , true
                            , true
                            , !TextUtils.isEmpty(text)
                            , isTargetChat
                            , new ClickListener(text));
                    mSaveWindow.show();
                }
            } else if (TextUtils.equals(intent.getAction(), OtherBroadcast.MSG_BACK)) {
                String packetId = intent.getStringExtra("packetId");
                if (mChatMessages != null) {
                    ChatMessage message = mChatMessages.get(thisPosition);
                    if (message != null && TextUtils.equals(packetId, message.getPacketId())) {
                        finish();
                    }
                }
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
                    ChatMessage message = mChatMessages.get(thisPosition);
                    if (message.getTimeLen() == 1) {
                        // 编辑的图片，先上传，得到新url，在发送(自己发的图片消息可能也会走进该判断，先不管了，影响不大)
                        PlayUploadUtil.uploadSingle(mContext, message.getFilePath(), new PlayUploadUtil.UploadSingleCallback() {
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
                        MessageUtil.generateImageMessage2Intent(mContext, coreManager.getSelf().getUserId(), message.getContent());
                    }
                    break;
                case R.id.tv_collection:
                    collectionEmotion(mChatMessages.get(thisPosition), isGroup, "");
                    break;
                case R.id.save_image:
                    FileUtil.downImageToGallery(ChatOverviewActivity.this, mCurrentShowUrl);
                    break;
                case R.id.edit_image:
                    ImageLoadHelper.loadFile(
                            ChatOverviewActivity.this,
                            mCurrentShowUrl,
                            f -> {
                                mEditedPath = FileUtil.createImageFileForEdit().getAbsolutePath();
                                IMGEditActivity.startForResult(ChatOverviewActivity.this, Uri.fromFile(f), mEditedPath, REQUEST_IMAGE_EDIT);
                            });
                    break;
                case R.id.identification_qr_code:
                    // 识别图中二维码
                    if (TextUtils.isEmpty(text)) {// 理论上不太可能了，因为该item显示时，bitmap都不为空
                        Toast.makeText(ChatOverviewActivity.this, R.string.unrecognized, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    HandleQRCodeScanUtil.handleScanResult(mContext, text);
                    break;
                case R.id.tvTargetChat:
                    Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), chatId);
                    if (friend == null) {
                        ToastUtil.showToast(mContext, getString(R.string.unknown));
                        return;
                    }
                    ChatMessage chatMessage = mChatMessages.get(thisPosition);
                    if (chatMessage != null) {
                        if (!isGroup) {
                            ChatActivity.start(mContext, friend, true, chatMessage.getDoubleTimeSend(), chatMessage.getPacketId());
                        } else {
                            MucChatActivity.start(mContext, friend, true, chatMessage.getDoubleTimeSend(), chatMessage.getPacketId());
                        }
                    }
                    break;
            }
        }
    }
}
