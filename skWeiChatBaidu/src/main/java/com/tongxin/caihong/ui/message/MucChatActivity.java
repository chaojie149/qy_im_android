package com.tongxin.caihong.ui.message;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.audio_x.VoiceManager;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.AtBean;
import com.tongxin.caihong.bean.Contacts;
import com.tongxin.caihong.bean.EventSecureNotify;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.MsgRoamTask;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.TitleUrl;
import com.tongxin.caihong.bean.VideoFile;
import com.tongxin.caihong.bean.assistant.GroupAssistantDetail;
import com.tongxin.caihong.bean.assistant.ShareParams;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.company.StructBeanNetInfo;
import com.tongxin.caihong.bean.event.EventCreateMucRoomTask;
import com.tongxin.caihong.bean.event.EventNewNotice;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.event.EventRemoveNotice;
import com.tongxin.caihong.bean.event.EventRoomBlackChange;
import com.tongxin.caihong.bean.event.EventRoomNotice;
import com.tongxin.caihong.bean.event.EventSendReadChanged;
import com.tongxin.caihong.bean.event.EventShowMarkerChanged;
import com.tongxin.caihong.bean.event.EventUploadCancel;
import com.tongxin.caihong.bean.event.EventUploadFileRate;
import com.tongxin.caihong.bean.event.EventXMPPJoinGroupFailed;
import com.tongxin.caihong.bean.event.MessageLocalVideoFile;
import com.tongxin.caihong.bean.event.MessageUploadChatRecord;
import com.tongxin.caihong.bean.event.MessageVideoFile;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.ChatRecord;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.bean.redpacket.OpenRedpacket;
import com.tongxin.caihong.bean.redpacket.RedDialogBean;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.call.CallConstants;
import com.tongxin.caihong.call.JitsiInviteActivity;
import com.tongxin.caihong.call.MessageEventMeetingInvite;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.db.dao.VideoFileDao;
import com.tongxin.caihong.downloader.DownloadListener;
import com.tongxin.caihong.downloader.Downloader;
import com.tongxin.caihong.downloader.FailReason;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.TrillStatisticsHelper;
import com.tongxin.caihong.helper.UploadEngine;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.contacts.SendContactsActivity;
import com.tongxin.caihong.ui.map.MapPickerActivity;
import com.tongxin.caihong.ui.me.collection.MyCollection;
import com.tongxin.caihong.ui.me.redpacket.MucSendRedPacketActivity;
import com.tongxin.caihong.ui.me.redpacket.RedDetailsActivity;
import com.tongxin.caihong.ui.message.multi.InviteVerifyActivity;
import com.tongxin.caihong.ui.message.multi.RoomInfoActivity;
import com.tongxin.caihong.ui.mucfile.MucFileListActivity;
import com.tongxin.caihong.ui.mucfile.XfileUtils;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.AudioModeManger;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.RecorderUtils;
import com.tongxin.caihong.util.RepeatLimitHelper;
import com.tongxin.caihong.util.SmileyParser;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.VideoCompressUtil;
import com.tongxin.caihong.util.filter.RegexInputFilter;
import com.tongxin.caihong.util.link.SpannableStringBuilderAllVer;
import com.tongxin.caihong.util.log.FileUtils;
import com.tongxin.caihong.util.secure.AES;
import com.tongxin.caihong.util.secure.MD5;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.video.MessageEventGpu;
import com.tongxin.caihong.video.VideoRecorderActivity;
import com.tongxin.caihong.view.ChatBottomView;
import com.tongxin.caihong.view.ChatBottomView.ChatBottomListener;
import com.tongxin.caihong.view.ChatContentView;
import com.tongxin.caihong.view.ChatContentView.MessageEventListener;
import com.tongxin.caihong.view.GroupNoticeDialog;
import com.tongxin.caihong.view.NoDoubleClickListener;
import com.tongxin.caihong.view.PullDownListView;
import com.tongxin.caihong.view.SelectCardPopupWindow;
import com.tongxin.caihong.view.SelectFileActivity;
import com.tongxin.caihong.view.SelectRoomMemberPopupWindow;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.view.chatHolder.FileViewHolder;
import com.tongxin.caihong.view.photopicker.PhotoPickerActivity;
import com.tongxin.caihong.view.photopicker.SelectModel;
import com.tongxin.caihong.view.photopicker.intent.PhotoPickerIntent;
import com.tongxin.caihong.view.redDialog.RedDialog;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.SeqNoManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.xmpp.listener.MucListener;
import com.google.gson.Gson;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import Jni.VideoUitls;
import VideoHandle.OnEditorListener;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardforchat;
import me.aurelion.x.ui.view.watermark.WaterMarkView;
import okhttp3.Call;
import pl.droidsonroids.gif.GifDrawable;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * 聊天主界面
 */
public class MucChatActivity extends BaseActivity implements
        MessageEventListener, ChatBottomListener, ChatMessageListener, MucListener,
        SelectRoomMemberPopupWindow.SendMemberListener, SelectCardPopupWindow.SendCardS {

    private static final int REQUEST_CODE_INVITE = 895;
    /***********************
     * 拍照和选择照片
     **********************/
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_SELECT_VIDEO = 3;
    private static final int REQUEST_CODE_SEND_COLLECTION = 4;// 我的收藏 返回
    private static final int REQUEST_CODE_SELECT_Locate = 5;
    private static final int REQUEST_CODE_QUICK_SEND = 6;
    private static final int REQUEST_CODE_SELECT_FILE = 7;
    private static final int REQUEST_CODE_SEND_CONTACT = 21;
    public static String currentChatUserId;
    /**
     * 得到被@的群成员,设置聊天消息,高亮显示
     */
    Map<ForegroundColorSpan, AtBean> atBeanMap = new HashMap<>();
    // 该群组不存在本地聊天记录，根据漫游时长去服务端同步
    List<ChatMessage> chatMessages;
    @SuppressWarnings("unused")
    private ChatContentView mChatContentView;
    // 存储聊天消息
    private List<ChatMessage> mChatMessages;
    private ChatBottomView mChatBottomView;
    private ImageView mChatBgIv;// 聊天背景
    private AudioModeManger mAudioModeManger;
    // 当前聊天对象
    private Friend mFriend;
    private String mLoginUserId;
    private String mLoginNickName;
    private String instantMessage;
    // 是否为通知栏进入
    private boolean isNotificationComing;
    // 当前聊天对象的UserId（就是房间jid）
    private String mUseId;
    // 当前聊天对象的昵称（就是房间名称）
    private String mNickName;
    // 是否是群聊
    private boolean isGroupChat;
    private String roomId;
    private boolean isSearch;
    private double mSearchTime;
    private LinearLayout mNewMsgLl;
    private TextView mNewMsgTv;
    private int mNewMsgNum;
    private TextView mTvTitleLeft;
    private TextView mTvTitle;
    // 背景水印，
    private WaterMarkView marker;
    private boolean isFriendNull = false;
    /*
    头部展示控件
     */
    // 后台配置群组网址
    private View llWeb;
    private TextView tvWeb1, tvWeb2, tvWeb3, tvWeb4;
    // 群组公告
    private View llNotice;
    private TextView tvNotice;
    // @群成员的popWindow
    private SelectRoomMemberPopupWindow mSelectRoomMemberPopupWindow;
    // 发送名片的popWindow
    private SelectCardPopupWindow mSelectCardPopupWindow;
    private RedDialog mRedDialog;
    private RoomMember mRoomMember;
    private double mMinId = 0;
    private int mPageSize = 21;
    private boolean mHasMoreData = true;
    private boolean isSecureAlreadyTipd;// 端到端是否提示过了，仅提醒一次
    private String groupOwnerId;// 群主id
    private int showMember;
    private int isAtMePosition = -1; // -1表示不在被@状态，
    private long startTime;
    // 兼容服务端返回的群成员数量与服务端记录的群人数不一致时，本地loadMember无限调用的问题
    private boolean isLoaded;
    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {

        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            send(message);
        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {
            for (int i = 0; i < mChatMessages.size(); i++) {
                ChatMessage msg = mChatMessages.get(i);
                if (TextUtils.equals(message.getPacketId(), msg.getPacketId())) {
                    msg.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    ChatMessageDao.getInstance().updateMessageState(mLoginUserId, mFriend.getUserId(),
                            message.getPacketId(), ChatMessageListener.MESSAGE_SEND_FAILED);
                    mChatContentView.notifyDataSetInvalidated(false);
                    break;
                }
            }
        }
    };
    private ChatMessage replayMessage;
    private TipDialog tipDialog;
    private int mCurrentMemberNum;
    private GroupNoticeDialog groupNoticeDialog;
    private Runnable getMyInfoInThisRoomRunnable = this::getMyInfoInThisRoom;
    /*******************************************
     * 接收到广播后的后续操作
     ******************************************/
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MsgBroadcast.ACTION_MSG_STATE_UPDATE)) {
                // 改变某条消息的状态  显示已读人数
                String packetId = intent.getStringExtra("packetId");
                for (int i = 0; i < mChatMessages.size(); i++) {
                    ChatMessage chatMessage = mChatMessages.get(i);
                    if (packetId.equals(chatMessage.getPacketId())) {
                        /* if (chatMessage.getFromUserId().equals(mLoginUserId)) { return; } // 有时候可能会收到自己的已读回执，不记录  */
                        chatMessage.setReadPersons(chatMessage.getReadPersons() + 1);
                        // mChatContentView.changeReadPersons(i, count + 1);
                        mChatContentView.notifyDataSetChanged();
                        break;
                    }
                }
            } else if (action.equals(OtherBroadcast.NAME_CHANGE)) {
                String userId = intent.getStringExtra("userId");
                String name = intent.getStringExtra("name");
                mChatContentView.putNickname(userId, name);
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(OtherBroadcast.REMARK_CHANGE)) {
                String userId = intent.getStringExtra("userId");
                String name = intent.getStringExtra("name");
                mChatContentView.putRemarkName(userId, name);
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(OtherBroadcast.MSG_BACK)) {
                // 撤回消息
                String packetId = intent.getStringExtra("packetId");
                if (TextUtils.isEmpty(packetId)) {
                    return;
                }
                for (ChatMessage chatMessage : mChatMessages) {
                    if (packetId.equals(chatMessage.getPacketId())) {
                        if (chatMessage.getType() == XmppMessage.TYPE_VOICE
                                && !TextUtils.isEmpty(VoicePlayer.instance().getVoiceMsgId())
                                && packetId.equals(VoicePlayer.instance().getVoiceMsgId())) {// 语音 && 正在播放的msgId不为空 撤回的msgId==正在播放的msgId
                            // 停止播放语音
                            VoicePlayer.instance().stop();
                        }
                        ChatMessage chat = ChatMessageDao.getInstance().findMsgById(mLoginUserId, mUseId, packetId);
                        chatMessage.setContent(chat.getContent());
                        chatMessage.setType(chat.getType());

                        if (mChatContentView.getChatPpWindow() != null
                                && TextUtils.equals(mChatContentView.getChatPpWindow().getShowMessageId(), packetId)) {
                            // 有可能撤回的消息正在显示长按弹窗，要dismiss掉
                            mChatContentView.getChatPpWindow().dismiss();
                        }
                        break;
                    }
                }
                mChatContentView.notifyDataSetInvalidated(false);
            } else if (action.equals(Constants.CHAT_MESSAGE_DELETE_ACTION)) {
                // 删除消息
                if (mChatContentView != null) {
                    int position = intent.getIntExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, 10000);
                    if (position == 10000) {
                        return;
                    }
                    ChatMessage message = mChatMessages.get(position);

                    deleteMessage(message.getPacketId());// 服务端也需要删除

                    if (ChatMessageDao.getInstance().updateExpiredStatus(mLoginUserId, mFriend.getUserId(), message.getPacketId())) {
                        if (mChatMessages.size() > 0 && mChatMessages.size() - 1 == position) {// 删除的为最后一条消息，更新LastContent
                            message.setType(XmppMessage.TYPE_TEXT);
                            message.setContent("");
                            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mUseId, message);
                        }
                        mChatMessages.remove(position);
                        mChatContentView.notifyDataSetInvalidated(false);
                        Toast.makeText(mContext, getString(R.string.delete_all_succ), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (action.equals(Constants.SHOW_MORE_SELECT_MENU)) {// 显示多选菜单
                int position = intent.getIntExtra(Constants.CHAT_SHOW_MESSAGE_POSITION, 0);
                moreSelected(true, position);
            } else if (action.equals(Constants.CHAT_TIME_OUT_ACTION)) {
                String friendid = intent.getStringExtra("friend_id");
                double timeOut = intent.getDoubleExtra("time_out", -1);
                mFriend.setChatRecordTimeOut(timeOut);

            } else if (action.equals(Constants.CHAT_HISTORY_EMPTY)) {
                // 清空聊天记录
                String id = intent.getStringExtra("id");
                if (TextUtils.isEmpty(id) || TextUtils.equals(id, mFriend.getUserId())) {
                    mChatMessages.clear();
                    mChatContentView.notifyDataSetChanged();
                }
            } else if (action.equals(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE)) {
                // 群组已被锁定
                mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriend.getUserId());// 重新获取friend对象
                if (mFriend.getGroupStatus() == 3) {
                    groupTip(getString(R.string.tip_group_disable_by_service));
                }
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UPDATE_ROOM)) {
                // 显示已读人数 | 群主对群成员备注
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(OtherBroadcast.REFRESH_MANAGER)) {
                // 设置|| 取消 管理员  对自己和别人，
                String roomId = intent.getStringExtra("roomId");
                String toUserId = intent.getStringExtra("toUserId");
                boolean isSet = intent.getBooleanExtra("isSet", false);
                int role = isSet ? 2 : 3;
                if (!TextUtils.equals(roomId, mFriend.getUserId())) {
                    return;
                }
                if (TextUtils.equals(mLoginUserId, toUserId)) {
                    onRoleChanged(role);
                }
                mChatContentView.putRoomMember(toUserId, role);
            } else if (action.equals(OtherBroadcast.REFRESH_OWNER)) {
                // 群组转让  对自己和别人，
                String roomId = intent.getStringExtra("roomId");
                String toUserId = intent.getStringExtra("toUserId");
                boolean isSet = intent.getBooleanExtra("isSet", false);
                int role = isSet ? 1 : 3;
                if (!TextUtils.equals(roomId, mFriend.getUserId())) {
                    return;
                }
                if (TextUtils.equals(mLoginUserId, toUserId)) {
                    onRoleChanged(role);
                }
                mChatContentView.putRoomMember(toUserId, role);
            } else if (action.equals(MsgBroadcast.ACTION_MSG_ROLE_CHANGED)) {
                // 设置|| 取消 管理员、隐身人、监控人   对我，
                getMyInfoInThisRoom();
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UPDATE_ROOM_GET_ROOM_STATUS)) {
                // 进群 | 退群 | 全体禁言 | 群组直播开/关
                if (tipDialog != null && tipDialog.isShowing()) {
                    tipDialog.dismiss();
                }
                RepeatLimitHelper.run(2000, getMyInfoInThisRoomRunnable);
            } else if (action.equals(OtherBroadcast.QC_FINISH)) {
                int mOperationCode = intent.getIntExtra("Operation_Code", 0);
                if (mOperationCode == 1) {// 更换聊天背景成功 更新当前页面
                    loadBackdrop();
                } else {
                    finish();
                }
            } else if (action.equals(OtherBroadcast.ACTION_GROUP_FILE_DELETE)) {
                boolean isGroupNoticeDelete = intent.getBooleanExtra("isGroupNoticeDelete", false);
                String id = intent.getStringExtra("id");
                if (isGroupNoticeDelete) {
                    for (ChatMessage message : mChatMessages) {
                        if (TextUtils.equals(message.getPacketId(), id)) {
                            message.setDownload(true);
                            mChatContentView.notifyDataSetChanged();
                            break;
                        }
                    }
                } else {
                    String text = intent.getStringExtra("text");
                    for (ChatMessage message : mChatMessages) {
                        if (TextUtils.equals(message.getPacketId(), id)) {
                            message.setType(XmppMessage.TYPE_TIP);
                            message.setContent(text);
                            mChatContentView.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            } else if (action.equals(OtherBroadcast.ACTION_GROUP_MEMBER_BE_DELETE)) {
                String fromUserId = intent.getStringExtra("fromUserId");
                List<ChatMessage> data = new ArrayList<>();
                for (ChatMessage message : mChatMessages) {
                    if (TextUtils.equals(message.getFromUserId(), fromUserId)) {
                        data.add(message);
                    }
                }
                if (data.size() > 0) {
                    mChatMessages.removeAll(data);
                    mChatContentView.notifyDataSetChanged();
                }
            } else if (action.equals(OtherBroadcast.ACTION_GROUP_RECEIVED_RED_PACKET)) {
                String msgId = intent.getStringExtra(AppConstant.EXTRA_MSG_ID);
                for (ChatMessage message : mChatMessages) {
                    if (TextUtils.equals(message.getPacketId(), msgId)) {
                        message.setFileSize(2);
                        mChatContentView.notifyDataSetChanged();
                        break;
                    }
                }
            } else if (action.equals(OtherBroadcast.ACTION_GROUP_REFRESH)) {
                // 刷新类型 0.已读人数刷新
                int type = intent.getIntExtra("type", 0);
                String msgId = intent.getStringExtra(AppConstant.EXTRA_MSG_ID);
                for (ChatMessage message : mChatMessages) {
                    if (TextUtils.equals(message.getPacketId(), msgId)) {
                        if (type == 0) {
                            int readCount = intent.getIntExtra("readCount", 0);
                            message.setReadPersons(readCount);
                        }
                        mChatContentView.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    };

    public static void start(Context ctx, Friend friend) {
        Intent intent = new Intent(ctx, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        if (TextUtils.equals(friend.getUserId(), currentChatUserId)) {
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
        }
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, Friend friend, boolean isSearch, double searchTime, String msgId) {
        Intent intent = new Intent(ctx, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        intent.putExtra("isserch", isSearch);
        intent.putExtra("jilu_id", searchTime);
        intent.putExtra("msgId", msgId);
        if (TextUtils.equals(friend.getUserId(), currentChatUserId)) {
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
        }
        ctx.startActivity(intent);
    }

    @Override
    public void onCoreReady() {
        super.onCoreReady();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(AppConstant.EXTRA_USER_ID, mUseId);
        outState.putString(AppConstant.EXTRA_NICK_NAME, mNickName);
        outState.putBoolean(AppConstant.EXTRA_IS_GROUP_CHAT, isGroupChat);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        SmileyParser.getInstance(MyApplication.getContext()).notifyUpdate();
        /*AndroidBug5497Workaround.assistActivity(this);*/
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        if (getIntent() != null) {
            mUseId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            mNickName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
            isGroupChat = getIntent().getBooleanExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
            isSearch = getIntent().getBooleanExtra("isserch", false);
            if (isSearch) {
                mSearchTime = getIntent().getDoubleExtra("jilu_id", 0);
            }
            instantMessage = getIntent().getStringExtra("messageId");
            isNotificationComing = getIntent().getBooleanExtra(Constants.IS_NOTIFICATION_BAR_COMING, false);
        }
        mNewMsgNum = getIntent().getIntExtra(Constants.NEW_MSG_NUMBER, 0);

        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
        if (mFriend == null) {
            ToastUtil.showToast(mContext, getString(R.string.tip_program_error));
            isFriendNull = true;
            finish();
            return;
        }
        currentChatUserId = mUseId;
        roomId = mFriend.getRoomId();
        mAudioModeManger = new AudioModeManger();
        mAudioModeManger.register(mContext);
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + mLoginUserId
                + File.separator + Environment.DIRECTORY_MUSIC);

        initView();
        checkDeadLine();

        // 绑定服务，添加监听，注册EventBus,注册广播
        mTvTitle.post(() -> ListenerManager.getInstance().addChatMessageListener(MucChatActivity.this));// 界面可见在添加该监听，因为loadData也是界面可见才开始为mChatMessages赋值的
        ListenerManager.getInstance().addMucListener(this);
        EventBus.getDefault().register(this);
        IntentFilter filter = new IntentFilter();
        // 消息状态的改变
        filter.addAction(MsgBroadcast.ACTION_MSG_STATE_UPDATE);
        filter.addAction(OtherBroadcast.NAME_CHANGE);
        filter.addAction(OtherBroadcast.REMARK_CHANGE);
        // 消息撤回
        filter.addAction(OtherBroadcast.MSG_BACK);
        filter.addAction(Constants.CHAT_MESSAGE_DELETE_ACTION);
        filter.addAction(Constants.SHOW_MORE_SELECT_MENU);
        filter.addAction(Constants.CHAT_HISTORY_EMPTY);
        filter.addAction(Constants.CHAT_TIME_OUT_ACTION);
        filter.addAction(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE);
        filter.addAction(MsgBroadcast.ACTION_MSG_UPDATE_ROOM);
        filter.addAction(OtherBroadcast.REFRESH_MANAGER);
        filter.addAction(OtherBroadcast.REFRESH_OWNER);
        filter.addAction(MsgBroadcast.ACTION_MSG_ROLE_CHANGED);
        filter.addAction(MsgBroadcast.ACTION_MSG_UPDATE_ROOM_GET_ROOM_STATUS);
        filter.addAction(OtherBroadcast.QC_FINISH);
        filter.addAction(OtherBroadcast.ACTION_GROUP_FILE_DELETE);
        filter.addAction(OtherBroadcast.ACTION_GROUP_MEMBER_BE_DELETE);
        filter.addAction(OtherBroadcast.ACTION_GROUP_RECEIVED_RED_PACKET);
        filter.addAction(OtherBroadcast.ACTION_GROUP_REFRESH);
        registerReceiver(broadcastReceiver, filter);
    }

    private void checkDeadLine() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().CHECK_DEAD)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult result) {
                        if (result.getResultCode() == 1 ) {

                        } else {
                            ToastUtil.showToast(MucChatActivity.this,result.getResultMsg());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showToast(MucChatActivity.this,e.getMessage());
                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String newUserId = intent.getStringExtra(AppConstant.EXTRA_USER_ID);
        if (TextUtils.equals(newUserId, mUseId)) {
            // 重复打开相同聊天页情况无视，
            isSearch = intent.getBooleanExtra("isserch", false);
            if (isSearch) {
                String msgId = intent.getStringExtra("msgId");
                if (mChatMessages != null && mChatMessages.size() > 0) {
                    for (int i = 0; i < mChatMessages.size(); i++) {
                        if (TextUtils.equals(mChatMessages.get(i).getPacketId(), msgId)) {
                            isSearch = false;
                            mChatContentView.notifyDataSetInvalidated(i);
                            return;
                        }
                    }
                }
                mSearchTime = intent.getDoubleExtra("jilu_id", 0);
                mChatMessages.clear();
                loadDatas(false);
            }
            return;
        }
        startActivity(intent);
        finish();
    }

    /**
     * 展示后台配置群组网址，此处处理网站显示隐藏逻辑...
     */
    private void setWebAddress(List<TitleUrl> data) {
        if (data == null) {
            data = new ArrayList<>();
        }
        llWeb.setVisibility(data.size() == 0 ? View.GONE : View.VISIBLE);
        if (data.size() == 1) {
            tvWeb1.setVisibility(View.VISIBLE);
            tvWeb2.setVisibility(View.GONE);
            tvWeb3.setVisibility(View.GONE);
            tvWeb4.setVisibility(View.GONE);
            findViewById(R.id.view1).setVisibility(View.GONE);
            findViewById(R.id.view2).setVisibility(View.GONE);
            findViewById(R.id.view3).setVisibility(View.GONE);
            tvWeb1.setText(data.get(0).getName());
        } else if (data.size() == 2) {
            tvWeb1.setVisibility(View.VISIBLE);
            tvWeb2.setVisibility(View.VISIBLE);
            tvWeb3.setVisibility(View.GONE);
            tvWeb4.setVisibility(View.GONE);
            findViewById(R.id.view1).setVisibility(View.VISIBLE);
            findViewById(R.id.view2).setVisibility(View.GONE);
            findViewById(R.id.view3).setVisibility(View.GONE);
            tvWeb1.setText(data.get(0).getName());
            tvWeb2.setText(data.get(1).getName());
        } else if (data.size() == 3) {
            tvWeb1.setVisibility(View.VISIBLE);
            tvWeb2.setVisibility(View.VISIBLE);
            tvWeb3.setVisibility(View.VISIBLE);
            tvWeb4.setVisibility(View.GONE);
            tvWeb1.setText(data.get(0).getName());
            tvWeb2.setText(data.get(1).getName());
            tvWeb3.setText(data.get(2).getName());
            findViewById(R.id.view1).setVisibility(View.VISIBLE);
            findViewById(R.id.view2).setVisibility(View.VISIBLE);
            findViewById(R.id.view3).setVisibility(View.GONE);
        } else if (data.size() == 4) {
            tvWeb1.setVisibility(View.VISIBLE);
            tvWeb2.setVisibility(View.VISIBLE);
            tvWeb3.setVisibility(View.VISIBLE);
            tvWeb4.setVisibility(View.VISIBLE);
            findViewById(R.id.view1).setVisibility(View.VISIBLE);
            findViewById(R.id.view2).setVisibility(View.VISIBLE);
            findViewById(R.id.view3).setVisibility(View.VISIBLE);
            tvWeb1.setText(data.get(0).getName());
            tvWeb2.setText(data.get(1).getName());
            tvWeb3.setText(data.get(2).getName());
            tvWeb4.setText(data.get(3).getName());
        }
        List<TitleUrl> dataClone = data;
        tvWeb1.setOnClickListener(v -> WebViewActivity.startWithTitle(mContext, dataClone.get(0).getUrl(), dataClone.get(0).getName()));
        tvWeb2.setOnClickListener(v -> WebViewActivity.startWithTitle(mContext, dataClone.get(1).getUrl(), dataClone.get(1).getName()));
        tvWeb3.setOnClickListener(v -> WebViewActivity.startWithTitle(mContext, dataClone.get(2).getUrl(), dataClone.get(2).getName()));
        tvWeb4.setOnClickListener(v -> WebViewActivity.startWithTitle(mContext, dataClone.get(3).getUrl(), dataClone.get(3).getName()));
    }

    private void setLastNotice(MucRoom.Notice notice) {
        if (mFriend.getGroupStatus() == 4) {
            // 被拉黑不显示公告，
            llNotice.setVisibility(View.GONE);
            return;
        }
        // 公告置顶7天，
        if (notice != null && !TextUtils.isEmpty(notice.getText())) {
            setLastNotice(notice.getText());
        } else {
            llNotice.setVisibility(View.GONE);
        }
    }

    // 展示置顶公告，
    private void setLastNotice(String notice) {
        llNotice.setVisibility(View.VISIBLE);
        tvNotice.setText(notice);
        tvNotice.setSelected(true);
    }

    private void initView() {
        marker = findViewById(R.id.marker);
        mChatMessages = new ArrayList<>();
        mChatBottomView = (ChatBottomView) findViewById(R.id.chat_bottom_view);

        initActionBar();
        mChatBottomView.setChatBottomListener(this);
        mChatBottomView.getmShotsLl().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChatBottomView.getmShotsLl().setVisibility(View.GONE);
                String shots = PreferenceUtils.getString(mContext, Constants.SCREEN_SHOTS, "No_Shots");
                QuickSendPreviewActivity.startForResult(MucChatActivity.this, shots, REQUEST_CODE_QUICK_SEND);
            }
        });
        mChatBottomView.setGroup(true, mFriend.getRoomId(), mFriend.getUserId());

        mChatContentView = (ChatContentView) findViewById(R.id.chat_content_view);
        //mChatContentView.filterBackMsgEnable(true);//过滤撤回消息

        mChatContentView.setToUserId(mUseId);
        mChatContentView.setIsSendRead(true);
        mChatContentView.setRoomId(mFriend.getRoomId());
        mChatContentView.setCurGroup(true, mFriend.getRoomMyNickName());
        mChatContentView.setSearch(isSearch);
        mChatContentView.setData(mChatMessages);
        mChatContentView.setChatBottomView(mChatBottomView);// 需要获取多选菜单的点击事件
        mChatContentView.setMessageEventListener(this);
        mChatContentView.setRefreshListener(new PullDownListView.RefreshingListener() {
            @Override
            public void onHeaderRefreshing() {
                loadDatas(false);
            }
        });
        mChatContentView.addOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (isAtMePosition < 0) {
                    return;
                }
                if (firstVisibleItem <= isAtMePosition && isAtMePosition <= firstVisibleItem + visibleItemCount) {
                    // 看到了@我的消息，隐藏有人@我的提示，
                    mNewMsgLl.setVisibility(View.GONE);
                    isAtMePosition = -1;
                }
            }
        });

        // 表示已读
        Intent intent = new Intent();
        intent.putExtra(AppConstant.EXTRA_FRIEND, mFriend);
        intent.setAction(Constants.NOTIFY_MSG_SUBSCRIPT);
        sendBroadcast(intent);

        mNewMsgLl = findViewById(R.id.msg_up_ll);
        mNewMsgTv = findViewById(R.id.msg_up_tv);
        mNewMsgLl.setOnClickListener(v -> {
            mNewMsgLl.setVisibility(View.GONE);
            if (mNewMsgTv.getText().toString().contains("[有人@我]")) {
                mChatContentView.smoothScrollToPosition(isAtMePosition);
                isAtMePosition = -1;
                return;
            }
            mChatContentView.setSelection(0);
        });

        initHead();

        mChatBottomView.isLostChatKeyGroup(mFriend.getIsLostChatKeyGroup() == 1);
        if (mFriend.getIsLostChatKeyGroup() == 1) {
            findViewById(R.id.msg_up_ll2).setVisibility(View.VISIBLE);
        }
        findViewById(R.id.msg_up_ll2).setOnClickListener(v -> HandleSecureChatMessage.sendRequestChatKeyGroupMessage(false, mFriend.getUserId()));

        mTvTitle.post(() -> {
            loadDatas(true);
        });

        initRoomMember();
        getMyInfoInThisRoom();
    }

    private void initHead() {
        llWeb = findViewById(R.id.llWeb);
        tvWeb1 = findViewById(R.id.tvWeb1);
        tvWeb2 = findViewById(R.id.tvWeb2);
        tvWeb3 = findViewById(R.id.tvWeb3);
        tvWeb4 = findViewById(R.id.tvWeb4);

        llNotice = findViewById(R.id.llNotice);
        tvNotice = findViewById(R.id.tvNotice);
        llNotice.setOnClickListener(v -> llNotice.setVisibility(View.GONE));
    }

    private void loadDatas(boolean scrollToBottom) {
        // scrollToBottom为true表示第一次进入
        if (mChatMessages.size() > 0) {
            mMinId = mChatMessages.get(0).getDoubleTimeSend();
        } else {
            ChatMessage mLastChatMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, mFriend.getUserId());
            if (mLastChatMessage != null && mLastChatMessage.getTimeSend() != 0) {
                mMinId = mLastChatMessage.getDoubleTimeSend() + 1;  // sq < mMinId
            } else {
                mMinId = TimeUtils.sk_time_current_time();
            }
        }

        List<ChatMessage> chatLists;
        if (isSearch) {
            chatLists = ChatMessageDao.getInstance().searchMessagesByTime(mLoginUserId,
                    mFriend.getUserId(), mSearchTime);
        } else {
            if (scrollToBottom && mNewMsgNum > 20) {// 第一次进入当前页面且新消息数量>20,查出所有新消息
                chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mChatMessages, mLoginUserId,
                        mFriend.getUserId(), mMinId, mNewMsgNum);

                mNewMsgTv.setText(getString(R.string.new_message_count_place_holder, mNewMsgNum));
                mNewMsgLl.setVisibility(View.VISIBLE);
            } else {
                chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mChatMessages, mLoginUserId,
                        mFriend.getUserId(), mMinId, mPageSize);
            }

            if (mFriend.getIsAtMe() == 1 || mFriend.getIsAtMe() == 2) {
                String messageId = PreferenceUtils.getString(MyApplication.getContext(), "messageId" + mFriend.getUserId());
                for (int i = 0; i < chatLists.size(); i++) {
                    if (chatLists.get(i).getPacketId().equals(messageId)) {
                        isAtMePosition = chatLists.size() - i;
                        mNewMsgTv.setText("[有人@我]");
                        mNewMsgLl.setVisibility(View.VISIBLE);
                        mFriend.setIsAtMe(0);
                    }
                }
            }
        }
        if (chatLists == null || chatLists.size() <= 0) {
            /** 加载漫游 */
            if (!scrollToBottom) {
                getNetSingle();
            } else {
                if (mFriend.getMaxSeqNo() == 0) {
                    getNetSingle();
                } else {
                    if (mFriend.getIsSecretGroup() == 1) {
                        sendSecureChatReadyTip();
                    }
                }
            }
        } else {
            long currTime = TimeUtils.sk_time_current_time();
            for (int i = 0; i < chatLists.size(); i++) {
                ChatMessage message = chatLists.get(i);
                // 防止过期的消息出现在列表中
                if (message.getDeleteTime() > 0 && message.getDeleteTime() < currTime / 1000) {
                    // ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                    continue;
                }
                mChatMessages.add(0, message);
            }

            if (isSearch) {// 查找聊天记录 进入
                isSearch = false;
                int position = 0;
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).getDoubleTimeSend() == mSearchTime) {
                        position = i;
                    }
                }
                mChatContentView.notifyDataSetInvalidated(position);// 定位到该条消息
            } else {
                if (mFriend.getEncryptType() == 3) {// 本地有消息记录
                    sendSecureChatReadyTip();
                }
                if (scrollToBottom) {
                    mChatContentView.notifyDataSetInvalidatedForSetSelectionInvalid(scrollToBottom);
                } else {
                    mChatContentView.notifyDataSetAddedItemsToTop(chatLists.size());
                }
            }
            mChatContentView.headerRefreshingCompleted();
            if (!mHasMoreData) {
                mChatContentView.setNeedRefresh(false);
            }
        }
    }

    // 草稿
    protected void onSaveContent() {
        if (mChatBottomView == null) {
            return;
        }
        String str = mChatBottomView.getmChatEdit().getText().toString().trim();
        if (TextUtils.isEmpty(str)) {
            if (XfileUtils.isNotEmpty(mChatMessages)) {
                // todo No.2 不知道为什么oppo手机在以下情况下多选状态下侧滑返回还是报这个错误，length=0; index=-1，先干脆另外创建一个list来处理一下逻辑。
                List<ChatMessage> chatMessages = new ArrayList<>(mChatMessages);
                for (int i = 0; i < chatMessages.size(); i++) {
                    if (TextUtils.equals(chatMessages.get(i).getPacketId(), AppConfig.getApiKey() + "tip")) {
                        // 此提示消息不参与其他业务逻辑，仅首次进入提示
                        chatMessages.remove(i);
                        // 移除时必须刷新适配器，之前未刷新时小米手机会崩溃，而且是在消息页面崩溃的，且崩溃日志如下，
                        // java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
                        //	at java.util.ArrayList.get(ArrayList.java:437)
                        //	at android.widget.HeaderViewListAdapter.isEnabled(HeaderViewListAdapter.java:164)
                        //	at android.widget.ListView.dispatchDraw(ListView.java:3575)
                        //	at android.view.View.draw(View.java:20237)
                        // 非常的莫名其妙，还一直以为是MessageFragment的问题，最后排查是这里的问题
                        // todo No.1注释原因见todo No.2
                        // if (mChatContentView != null) {
                        //   mChatContentView.notifyDataSetChanged();
                        // }
                        break;
                    }
                }
                if (!XfileUtils.isNotEmpty(chatMessages)) {
                    return;
                }
                ChatMessage chatMessage = chatMessages.get(chatMessages.size() - 1);
                String fromUserName;
                if (chatMessage.getType() == XmppMessage.TYPE_TIP) {// 群组控制消息不添加FromUserId
                    fromUserName = "";
                } else {
                    fromUserName = TextUtils.isEmpty(chatMessage.getFromUserName()) ? "" : chatMessage.getFromUserName() + " : ";
                }

                // 调用到该方法时，如果msg的isDecrypted状态为未解密[基本为搜索聊天记录跳转]，先解密在更新Friend content
                String content = chatMessage.getContent();
                if (!TextUtils.isEmpty(content) && !chatMessage.isDecrypted()) {
                    String key = SecureChatUtil.getSymmetricKey(chatMessage.getPacketId());
                    try {
                        content = AES.decryptStringFromBase64(chatMessage.getContent(), Base64.decode(key));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                FriendDao.getInstance().updateFriendContent(
                        mLoginUserId,
                        mFriend.getUserId(),
                        fromUserName + content,
                        chatMessage);
            }
        } else {
            // 更新朋友表最后一次事件
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(XmppMessage.TYPE_TEXT);
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            FriendDao.getInstance().updateFriendContent(
                    mLoginUserId,
                    mFriend.getUserId(),
                    "&8824" + str,
                    chatMessage);
        }
        PreferenceUtils.putString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, str);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 忽略双指操作，避免引起莫名的问题，
        if (ev.getActionIndex() > 0) {
            return true;
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (IllegalArgumentException ignore) {
            // 可能触发ViewPager的bug, 找不到手指头，
            // https://stackoverflow.com/a/31306753
            return true;
        }
    }

    private void doBack() {
        if (!TextUtils.isEmpty(instantMessage)) {
            SelectionFrame selectionFrame = new SelectionFrame(this);
            selectionFrame.setSomething(null, getString(R.string.tip_forwarding_quit), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    finish();
                }
            });
            selectionFrame.show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (!JVCideoPlayerStandardforchat.handlerBack()) {
            doBack();
        }
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        // 听筒/扬声器模式切换
        boolean isSpeaker = PreferenceUtils.getBoolean(mContext,
                Constants.SPEAKER_AUTO_SWITCH + mLoginUserId, true);
        findViewById(R.id.iv_title_center).setVisibility(isSpeaker ? View.GONE : View.VISIBLE);
        mAudioModeManger.onStart(isSpeaker);

        // 私密模式禁止截屏
        if (mFriend.getHideChatSwitch() == 1) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // 获取[草稿]
        String draft = PreferenceUtils.getString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, "");
        if (!TextUtils.isEmpty(draft)) {
            String s = StringUtils.replaceSpecialChar(draft);
            CharSequence content = HtmlUtils.transform200SpanString(s, true);
            if (draft.contains("@")) {
                // 防止SelectRoomMemberPopupWindow还未初始化的时候被调用
                mChatBottomView.getmChatEdit().setText(content + ",");
            } else {
                mChatBottomView.getmChatEdit().setText(content);
            }
            softKeyboardControl(true, 200);
        }
        // 记录当前聊天对象的id
        MyApplication.IsRingId = mFriend.getUserId();

        if (mFriend.getIsAtMe() != 0) {// 更新@状态
            FriendDao.getInstance().updateAtMeStatus(mFriend.getUserId(), 0);
        }

        if (mChatBottomView != null) {
            mChatBottomView.addRecordListener();
        }
    }

    private void updateSecret(boolean secret) {
        // 不允许群聊且为普通权限身份，
        mChatContentView.setSecret(secret);
    }

    @Override
    protected void onPause() {
        super.onPause();
        VoicePlayer.instance().stop();

        // 恢复扬声器模式
        mAudioModeManger.onStop();

        if (TextUtils.isEmpty(mChatBottomView.getmChatEdit().getText().toString())) {// 清空草稿，以防消息发送出去后，通过onPause--onResume的方式给输入框赋值
            PreferenceUtils.putString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, "");
        }
        // 将当前聊天对象id重置
        MyApplication.IsRingId = "Empty";

        if (mChatBottomView != null) {
            mChatBottomView.removeRecordListener();
        }
    }

    @Override
    public void finish() {
        onSaveContent();
        MsgBroadcast.broadcastMsgUiUpdate(mContext);
        FileViewHolder.unlockFileSet.clear();// 清空记住了的加密文件解密状态，
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFriendNull) {
            return;
        }
        if (mAudioModeManger != null) {
            mAudioModeManger.unregister();
        }
        JCVideoPlayer.releaseAllVideos();
        if (mChatBottomView != null) {
            mChatBottomView.recordCancel();
        }
        ListenerManager.getInstance().removeChatMessageListener(this);
        ListenerManager.getInstance().removeMucListener(this);
        EventBus.getDefault().unregister(this);
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            // 不能在这崩溃，无法判断是否已经注册这个广播，
        }
    }

    /***************************************
     * ChatContentView的回调
     ***************************************/
    @Override
    public void onMyAvatarClick() {
        mChatBottomView.reset();
        mChatBottomView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mContext, BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
                intent.putExtra("fromGroupSee", isOk());
                intent.putExtra("fromGroupId", mFriend.getRoomId());
                startActivity(intent);
            }
        }, 100);
    }

    @Override
    public void onFriendAvatarClick(final String friendUserId) {
        if (mFriend != null) {
            if (mFriend.getGroupStatus() == 1) {
                tip(getString(R.string.tip_kick_room));
                return;
            } else if (mFriend.getGroupStatus() == 2) {
                tip(getString(R.string.tip_group_been_disbanded));
                return;
            }
        }
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        if (isAllowSecretlyChat || isOk()) {
            mChatBottomView.reset();
            mChatBottomView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BasicInfoActivity.start(mContext, friendUserId, BasicInfoActivity.FROM_ADD_TYPE_GROUP
                            , isOk()
                            , mFriend.getRoomId());
                }
            }, 100);
        } else {
            tip(getString(R.string.tip_member_disable_privately_chat));
        }
    }

    // 长按头像@群成员
    @Override
    public void LongAvatarClick(ChatMessage chatMessage) {
        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mUseId, false);
        if (isAllShutUp && !isOk()) {
            tip(getString(R.string.tip_shut_all));
            return;
        }
        if (chatMessage.getFromUserId().equals(mLoginUserId)) {
            // @自己不处理
            return;
        }
        // 没有监听AT被删除的情况，所以不能这样处理，
        // AT过了的不再处理，
/*
        if (atUserId.contains(chatMessage.getFromUserId())) {
            return;
        }
*/
        Editable editContent = mChatBottomView.getmChatEdit().getText();
        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), chatMessage.getFromUserId());
        String content = chatMessage.getFromUserName();
        if (member != null) {
            content = member.getUserName();
        }
        SpannableString atContent = new SpannableString("@" + content + " ");
        ForegroundColorSpan span = new ForegroundColorSpan(Color.parseColor("#63B8FF"));
        atContent.setSpan(span, 0, atContent.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editContent.insert(mChatBottomView.getmChatEdit().getSelectionStart(), atContent);
        atBeanMap.put(span, new AtBean(chatMessage.getFromUserId()));
    }

    @Override
    public void onNickNameClick(String friendUserId) {

    }

    @Override
    public void onMessageClick(ChatMessage chatMessage) {

    }

    @Override
    public void onMessageLongClick(ChatMessage chatMessage) {

    }

    @Override
    public void onEmptyTouch() {
        mChatBottomView.reset();
    }

    @Override
    public void onTipMessageClick(ChatMessage message) {
        if (message.getFileSize() == XmppMessage.TYPE_83) {
            showRedReceivedDetail(message.getFilePath());
        } else if (message.getFileSize() == XmppMessage.TYPE_MUCFILE_ADD) {
            if (mRoomMember != null) {
                boolean isAllowUploadFile = PreferenceUtils.getBoolean(MyApplication.getContext(),
                        Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mFriend.getUserId(), true);

                Intent intent = new Intent(this, MucFileListActivity.class);
                intent.putExtra("roomId", mFriend.getRoomId());
                intent.putExtra("role", mRoomMember.getRole());
                intent.putExtra("allowUploadFile", isAllowUploadFile ? 1 : 0);
                startActivity(intent);
            }
        } else if (message.getFileSize() == XmppMessage.TYPE_BACK) {
            if (mChatBottomView != null && mChatBottomView.getmChatEdit() != null) {
                String oText = mChatBottomView.getmChatEdit().getText().toString();
                String nText = oText + message.getTranslation();
                mChatBottomView.getmChatEdit().setText(nText);
                mChatBottomView.getmChatEdit().setSelection(nText.length());
            }
        } else if (!TextUtils.isEmpty(message.getObjectId())
                && message.getObjectId().contains("userIds")
                && message.getObjectId().contains("userNames")
                && !TextUtils.equals(mLoginUserId, message.getFromUserId())// 邀请发送方也满足以上条件，兼容ta
                && message.getObjectId().contains("isInvite")) {
            //  验证该提示是否为邀请好友入群的验证提示，是的话高亮显示KeyWord 并针对Click事件进行处理
            // todo  应该效仿红包被领取的提示，将原消息type与关键信息存在其他字段内，这样结构会更加清晰且不会出错
            Intent intent = new Intent(MucChatActivity.this, InviteVerifyActivity.class);
            intent.putExtra("VERIFY_MESSAGE_FRIEND_ID", mUseId);
            intent.putExtra("VERIFY_MESSAGE_PACKET", message.getPacketId());
            intent.putExtra("VERIFY_MESSAGE_ROOM_ID", mFriend.getRoomId());
            startActivityForResult(intent, REQUEST_CODE_INVITE);
        }
    }

    // 查看红包领取详情
    private void showRedReceivedDetail(String redId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("id", redId);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).RENDPACKET_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (result.getData() != null) {
                            // 当resultCode==1时，表示可领取
                            // 当resultCode==0时，表示红包已过期、红包已退回、红包已领完
                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 0);
                            if (!TextUtils.isEmpty(result.getResultMsg())) //resultMsg不为空表示红包已过期
                            {
                                bundle.putInt("timeOut", 1);
                            } else {
                                bundle.putInt("timeOut", 0);
                            }

                            bundle.putBoolean("isGroup", true);
                            bundle.putString("mToUserId", mFriend.getUserId());
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                        } else {
                            Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    @Override
    public void onReplayClick(ChatMessage message) {
        ChatMessage replayMessage = new ChatMessage(message.getObjectId());
        AsyncUtils.doAsync(this, t -> {
            Reporter.post("查询被回复的消息出错<" + message.getObjectId() + ">", t);
        }, c -> {
            List<ChatMessage> chatMessages = ChatMessageDao.getInstance().searchFromMessage(c.getRef(), mLoginUserId, mFriend.getUserId(), replayMessage);
            if (chatMessages == null) {
                // 没查到消息，
                Log.e("Replay", "本地没有查到被回复的消息<" + message.getObjectId() + ">");
                return;
            }
            int index = -1;
            for (int i = 0; i < chatMessages.size(); i++) {
                ChatMessage m = chatMessages.get(i);
                if (TextUtils.equals(m.getPacketId(), replayMessage.getPacketId())) {
                    index = i;
                }
            }
            if (index == -1) {
                Reporter.unreachable();
                return;
            }
            int finalIndex = index;
            c.uiThread(r -> {
                mChatMessages = chatMessages;
                mChatContentView.setData(mChatMessages);
                mChatContentView.notifyDataSetInvalidated(finalIndex);
            });
        });
    }

    @Override
    public void onSendAgain(ChatMessage message) {
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                // 将需要上传的消息状态置为发送中，防止在上传的时候退出当前界面，回来后[还未上传成功]读取数据库又变为了感叹号
                ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mFriend.getUserId(),
                        message.get_id(), ChatMessageListener.MESSAGE_SEND_ING);
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mUseId, message, mUploadResponse, false);
            } else {
                send(message);
            }
        } else if (message.getType() == XmppMessage.TYPE_DICE) {
            if (isAuthenticated()) {
                return;
            }
            // 以防有人作弊，假装断网看到数字再发出去
            message.setContent(String.valueOf((int) (Math.random() * 6 + 1)));
            ChatMessageDao.getInstance().updateMessageContent(mLoginUserId, mUseId, message.getPacketId(), message.getContent());
            send(message);
        } else if (message.getType() == XmppMessage.TYPE_RPS) {
            if (isAuthenticated()) {
                return;
            }
            // 以防有人作弊，假装断网看到结果再发出去
            message.setContent(String.valueOf((int) (Math.random() * 3 + 1)));
            ChatMessageDao.getInstance().updateMessageContent(mLoginUserId, mUseId, message.getPacketId(), message.getContent());
            send(message);
        } else {
            send(message);
        }
    }

    public void deleteMessage(String msgIdListStr) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", msgIdListStr);
        params.put("delete", "1");  // 1单方删除 2-双方删除
        params.put("type", "2");    // 1单聊记录 2-群聊记录

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_CHATMESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    /**
     * 消息撤回
     */
    @Override
    public void onMessageBack(final ChatMessage chatMessage, final int position) {
        if (TextUtils.equals(chatMessage.getFromUserId(), groupOwnerId)) {
            // 群主发的消息，判断撤回人是否为群主自己
            if (!TextUtils.equals(mLoginUserId, groupOwnerId)) {
                // 自己不是群主，不允许撤回
                ToastUtil.showToast(mContext, getString(R.string.cannot_back_group_owner_msg));
                return;
            }
        }
        DialogHelper.showMessageProgressDialog(MucChatActivity.this, getString(R.string.message_revocation));
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", chatMessage.getPacketId());
        params.put("roomJid", mUseId);
        params.put("type", "2");
        params.put("delete", "2");

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_CHATMESSAGE)
                .params(params)
                .build()
                .execute(new ListCallback<StructBeanNetInfo>(StructBeanNetInfo.class) {
                    @Override
                    public void onResponse(ArrayResult<StructBeanNetInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (chatMessage.getType() == XmppMessage.TYPE_VOICE) {// 撤回的为语音消息，停止播放
                                if (VoicePlayer.instance().getVoiceMsgId().equals(chatMessage.getPacketId())) {
                                    VoicePlayer.instance().stop();
                                }
                            } else if (chatMessage.getType() == XmppMessage.TYPE_VIDEO) {
                                JCVideoPlayer.releaseAllVideos();
                            }
                            // 发送撤回消息
                            ChatMessage message = new ChatMessage();
                            message.setType(XmppMessage.TYPE_BACK);
                            message.setFromUserId(mLoginUserId);
                            message.setFromUserName(mLoginNickName);
                            if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
                                message.setFromUserName(mFriend.getRoomMyNickName());
                            }
                            message.setToUserId(mUseId);
                            message.setContent(chatMessage.getPacketId());
                            message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                            message.setTimeSend(TimeUtils.sk_time_current_time());
                            send(message);

                            ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, mFriend.getUserId(), chatMessage.getPacketId(), getString(R.string.you));
                            int oType = chatMessage.getType();
                            String oText = chatMessage.getContent();
                            chatMessage.setType(XmppMessage.TYPE_TIP);
                            chatMessage.setContent(getString(R.string.already_with_draw));
                            if (oType == XmppMessage.TYPE_TEXT) {
                                // 撤回自己发送的文本消息，自己支持重新编辑
                                chatMessage.setContent(getString(R.string.already_with_draw) + "," + getString(R.string.tip_edit_again));
                                // 将原消息内容保存在translation字段内
                                chatMessage.setTranslation(oText);
                                chatMessage.setFileSize(XmppMessage.TYPE_BACK);
                            }
                            mChatContentView.notifyDataSetInvalidated(false);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MucChatActivity.this);
                    }
                });
    }

    @Override
    public void onMessageReplay(ChatMessage chatMessage) {
        replayMessage = chatMessage.cloneAll();
        mChatBottomView.setReplay(chatMessage);
    }

    @Override
    public void cancelReplay() {
        replayMessage = null;
        // 可能需要恢复禁言文字的显示，
        updateBannedStatus();
    }

    @Override
    public void onCallListener(int type) {

    }

    /***************************************
     * ChatBottomView的回调
     ***************************************/

    private void softKeyboardControl(boolean isShow, long delayMillis) {
        // 软键盘消失
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (isShow) {
            mChatBottomView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mChatBottomView.getmChatEdit().requestFocus();
                    mChatBottomView.getmChatEdit().setSelection(mChatBottomView.getmChatEdit().getText().toString().length());
                    imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
                }
            }, delayMillis);
        } else {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void send(ChatMessage message) {
        // 一些异步回调进来的也要判断xmpp是否在线，
        // 比如图片上传成功后，
        if (isAuthenticated()) {
            return;
        }
        coreManager.sendMucChatMessage(mUseId, message);
    }

    private void sendMessage(ChatMessage message) {
        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mUseId, false);
        if (isAllShutUp && !isOk()) {
            tip(getString(R.string.tip_shut_all));
            return;
        }
        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), mLoginUserId);
        if (member != null && member.getRole() == 3) {// 普通成员需要判断是否被禁言
            if (mFriend != null && mFriend.getRoomTalkTime() > (TimeUtils.sk_time_current_time() / 1000)) {
                ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                mChatMessages.remove(message);
                mChatContentView.notifyDataSetInvalidated(true);
                return;
            }
        } else if (member == null) {// 也需要判断是否被禁言
            if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                mChatMessages.remove(message);
                mChatContentView.notifyDataSetInvalidated(true);
                return;
            }
        }

        // 群组的口令红包检测需要放到这里来，因为有@消息与禁言等限制，不能放在sendText方法内
        // 遍历消息集合，查询红包类型消息
        if (message.getType() == XmppMessage.TYPE_TEXT) {
            for (ChatMessage msg : mChatMessages) {
                if (msg.getType() == XmppMessage.TYPE_RED// 红包
                        && StringUtils.strEquals(msg.getFilePath(), "3")// 口令红包
                        && message.getContent().equalsIgnoreCase(msg.getContent())// 发送的文本与口令一致
                        && msg.getFileSize() == 1) // 可以领取的状态
                {
                    // todo 红包领取状态为本地记录，当对方领取之后清空本地聊天记录时在漫游获取到该消息时，在发送口令，不能让mRedDialog弹出
                    // todo 调接口获取红包领取状态
                    clickRedPacket(msg);
                    // 可能存在多个口令一致的未领取的口令红包，匹配到一个就直接跳出循环
                    break;
                }
            }
        }

        message.setToUserId(mUseId);
        if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
            message.setFromUserName(mFriend.getRoomMyNickName());
        }
        message.setToUserId(mUseId);

        if (mFriend.getChatRecordTimeOut() == -1 || mFriend.getChatRecordTimeOut() == 0) {// 永久
            message.setDeleteTime(-1);
        } else {
            long deleteTime = TimeUtils.sk_time_current_time() / 1000 + (long) (mFriend.getChatRecordTimeOut() * 24 * 60 * 60);
            message.setDeleteTime(deleteTime);
        }
        message.setDecrypted(true);
        message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));
        message.setGroup(true);
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());

        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mUseId, message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mUseId, message, mUploadResponse, false);
            } else {
                send(message);
            }
        } else {
            send(message);
        }
    }

    @Override
    public void stopVoicePlay() {
        VoicePlayer.instance().stop();
    }

    @Override
    public Map<ForegroundColorSpan, AtBean> getAtBeanMap() {
        return atBeanMap;
    }

    // 展示@界面
    @Override
    public void sendAt() {
        List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);
        if (mRoomMember != null && roomMember.size() > 0) {
            // 移除掉自己
            for (int i = 0; i < roomMember.size(); i++) {
                if (roomMember.get(i).getUserId().equals(mLoginUserId)) {
                    roomMember.remove(roomMember.get(i));
                }
            }
            mSelectRoomMemberPopupWindow = new SelectRoomMemberPopupWindow(this,
                    roomMember, mRoomMember.getRole(), mFriend.getRoomId(), mFriend.getUserId(), showMember, this);
            mSelectRoomMemberPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
            mSelectRoomMemberPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    softKeyboardControl(true, 200);
                }
            });
        } else {
            loadMembers(roomId, true); //加载除了自己的群成员列表
        }
    }

    // 选择了@群成员的回调
    @Override
    public void sendAtContent(RoomMember member) {
        makeAtSpan(member.getUserId(), member.getUserName());
    }

    // 选择了@全体成员的回调
    @Override
    public void sendEveryOne(String everyOne) {
        makeAtSpan(mUseId, everyOne);
    }

    private void makeAtSpan(String userId, String nickname) {
        EditText et = mChatBottomView.getmChatEdit();
        int current = et.getSelectionStart();
        CharSequence text = et.getText();
        String keyword = "@" + nickname + " ";
        SpannableStringBuilderAllVer sb = new SpannableStringBuilderAllVer();
        sb.append(text.subSequence(0, current - 1));
        ForegroundColorSpan span = new ForegroundColorSpan(Color.parseColor("#63B8FF"));
        sb.append(keyword, span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(text.subSequence(current, text.length()));
        mChatBottomView.getmChatEdit().setText(sb);
        atBeanMap.put(span, new AtBean(userId));
    }

    // 发送@消息
    @Override
    public void sendAtMessage(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(text);
        // 空格隔开
        //[10008295 109232...]
        String at = null;
        List<String> atUserIdList = new ArrayList<>();
        for (AtBean atBean : atBeanMap.values()) {
            if (TextUtils.equals(atBean.userId, mUseId)) {
                // roomJid
                at = mUseId;
                break;
            }
            atUserIdList.add(atBean.userId);
        }
        if (at == null && !atUserIdList.isEmpty()) {
            at = TextUtils.join(" ", atUserIdList);
        }
        message.setObjectId(at);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
        atBeanMap.clear();
    }

    @Override
    public void sendText(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        if (replayMessage != null) {
            message.setType(XmppMessage.TYPE_REPLAY);
            message.setObjectId(replayMessage.toJsonString());
            replayMessage = null;
            mChatBottomView.resetReplay();
        }
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(text);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    /**
     * 点击红包
     */
    public void clickRedPacket(ChatMessage msg) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("id", msg.getObjectId());

        HttpUtils.get().url(CoreManager.requireConfig(mContext).RENDPACKET_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (result.getResultCode() == 1) {
                            RedDialogBean redDialogBean = new RedDialogBean(msg.getFromUserId(), msg.getFromUserName(),
                                    msg.getContent(), null);
                            mRedDialog = new RedDialog(mContext, redDialogBean, new RedDialog.OnClickRedListener() {
                                @Override
                                public void clickRed() {
                                    // 打开红包
                                    openRedPacket(msg, result.getData().getPacket());
                                }

                                @Override
                                public void clickTail() {
                                    showRedReceivedDetail(msg.getObjectId());
                                }
                            }, msg.isMySend());
                            mRedDialog.setAutoOpen();
                            mRedDialog.show();
                        } else {
                            // 红包不可领，统统当做已领取处理
                            msg.setFileSize(2);
                            ChatMessageDao.getInstance().updateChatMessageReceiptStatus(mLoginUserId, mFriend.getUserId(), msg.getPacketId());
                            mChatContentView.notifyDataSetChanged();
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    /**
     * 打开红包
     */
    public void openRedPacket(final ChatMessage message, OpenRedpacket.PacketEntity packetEntity) {
        String url;
        if (!TextUtils.isEmpty(packetEntity.getuPayRedPacketId())) {
            if (!WeboxHelper.checkOpenedOrAsk(mContext)) {
                if (mRedDialog != null) {
                    mRedDialog.dismiss();
                }
                return;
            }
            url = CoreManager.requireConfig(mContext).WEBOX_ACCEPT_RED;
        } else {
            url = CoreManager.requireConfig(mContext).REDPACKET_OPEN;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        String redId = message.getObjectId();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("id", redId);

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (mRedDialog != null) {
                            mRedDialog.dismiss();
                        }
                        if (result.getData() != null) {
                            // 表示已经领取过了一次,不可再领取
                            message.setFileSize(2);
                            ChatMessageDao.getInstance().updateChatMessageReceiptStatus(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                            mChatContentView.notifyDataSetChanged();

                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 1);
                            bundle.putInt("timeOut", 0);

                            bundle.putBoolean("isGroup", true);
                            bundle.putString("mToUserId", mFriend.getUserId());
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                            // 更新余额
                            coreManager.updateMyBalance();
                        } else {
                            Toast.makeText(MucChatActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (mRedDialog != null) {
                            mRedDialog.dismiss();
                        }
                    }
                });
    }

    @Override
    public void sendGif(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(text);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendCollection(String collection) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(collection);
        message.setUpload(true);// 自定义表情，不需要上传
        sendImage(message);
    }

    @Override
    public void sendVoice(String filePath, int timeLen, ArrayList<String> stringAudio) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VOICE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent("");
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        message.setObjectId(TextUtils.join(",", stringAudio));
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendImage(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        int[] imageParam = BitmapUtil.getImageParamByIntsFile(filePath);
        message.setLocation_x(String.valueOf(imageParam[0]));
        message.setLocation_y(String.valueOf(imageParam[1]));
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendVideo(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent("");
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
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
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        String filePath = file.getAbsolutePath();
        if (TextUtils.isEmpty(password)) {
            if (FileUtil.isImageFile(filePath)) {
                sendImage(file);
                return;
            } else if (FileUtil.isVideoFile(filePath)) {
                sendVideo(file);
                return;
            }
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_FILE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent("");
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        message.setObjectId(password);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    private void sendContacts(List<Contacts> contactsList) {
        for (Contacts contacts : contactsList) {
            sendText(contacts.getName() + '\n' + contacts.getTelephone());
        }
    }

    public void sendLocate(double latitude, double longitude, String address, String snapshot) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent("");
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setFilePath(snapshot);
        message.setObjectId(address);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    /**
     * 得到选中的名片
     */
    @Override
    public void sendCardS(List<Friend> friends) {
        for (int i = 0; i < friends.size(); i++) {
            sendCard(friends.get(i));
        }
    }

    public void sendCard(Friend friend) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(friend.getNickName());
        message.setObjectId(friend.getUserId());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void clickPhoto() {
        // 将其置为true
        /*MyApplication.GalleyNotBackGround = true;
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);*/
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(MucChatActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        // 已选中的照片地址， 用于回显选中状态
        intent.setSelectedPaths(imagePaths);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickCamera() {
        mChatBottomView.reset();
        VideoRecorderActivity.startForResult(this, REQUEST_CODE_CAPTURE_PHOTO);
    }

    @Override
    public void clickStartRecord() {
        // 现拍照录像ui和二为一，统一在clickCamera内处理
       /* Intent intent = new Intent(this, VideoRecorderActivity.class);
        startActivity(intent);*/
    }

    @Override
    public void clickLocalVideo() {
        // 现拍照录像ui和二为一，统一在clickCamera内处理
       /* Intent intent = new Intent(this, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);*/
    }


    private void inviteMeet(int type) {
        if (getGroupStatus()) {
            return;
        }
        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), mLoginUserId);
        if (member != null && member.getRole() == 3) {// 普通成员需要判断是否被禁言
            if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                return;
            }
        } else if (member == null) {// 也需要判断是否被禁言
            if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                ToastUtil.showToast(mContext, getString(R.string.has_been_banned));
                return;
            }
        }
        boolean isAllowConference = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_CONFERENCE + mFriend.getUserId(), true);
        if (isAllowConference || isOk()) {
            if (coreManager.isLogin()) {
                JitsiInviteActivity.start(this, type, mUseId, roomId);
            } else {
                coreManager.autoReconnectShowProgress(this);
            }
        } else {
            tip(getString(R.string.tip_group_manager_close_conference_features));
        }
    }
    @Override
    public void clickVideoChat() {
        inviteMeet(CallConstants.Video_Meet);
    }

    @Override
    public void clickAudio() {
        inviteMeet(CallConstants.Audio_Meet);
    }

    @Override
    public void clickLiveChat() {
        inviteMeet(CallConstants.Video_Meet);
    }

    @Override
    public void clickTalk() {
        inviteMeet(CallConstants.Talk_Meet);
    }

    @Override
    public void clickFile() {
        boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mUseId, true);
        boolean admin = isOk();
        if (isAllowSendFile || admin) {
            SelectFileActivity.start(this, new SelectFileActivity.OptionFileListener() {
                @Override
                public void option(List<File> files) {
                    //用戶选择完文件之后  再判断此时的状态  如果此时群主已关闭群成员上传且也不为管理员 则return
                    boolean isAllowSendFileAgain = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mUseId, true);
                    if (!isAllowSendFileAgain && !admin) {
                        ToastUtil.showToast(MucChatActivity.this, getString(R.string.tip_cannot_upload));
                        return;
                    }
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
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
                }
            });
        } else {
            tip(getString(R.string.tip_cannot_upload));
        }
    }

    @Override
    public void clickContact() {
        SendContactsActivity.start(this, REQUEST_CODE_SEND_CONTACT);
    }

    @Override
    public void clickLocation() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        intent.putExtra(AppConstant.EXTRA_FORM_CAHT_ACTIVITY, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_Locate);
    }

    @Override
    public void clickCard() {
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        if (isAllowSecretlyChat || isOk()) {
            mSelectCardPopupWindow = new SelectCardPopupWindow(MucChatActivity.this, this);
            mSelectCardPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
        } else {
            tip(getString(R.string.tip_card_disable_privately_chat));
        }
    }

    @Override
    public void clickRedpacket() {
        Intent intent = new Intent(this, MucSendRedPacketActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra(AppConstant.EXTRA_USER_ID, mUseId);
        intent.putExtra(AppConstant.EXTRA_MEMBER_NUM, mCurrentMemberNum);
        startActivityForResult(intent, ChatActivity.REQUEST_CODE_SEND_RED);
    }

    @Override
    public void clickTransferMoney() {
        // 群组暂不支持转账
    }

    @Override
    public void clickCollection() {
        Intent intent = new Intent(this, MyCollection.class);
        intent.putExtra("IS_SEND_COLLECTION", true);
        startActivityForResult(intent, REQUEST_CODE_SEND_COLLECTION);
    }

    private void clickCollectionSend(
            int type,
            String content,
            int timeLen,
            String filePath,
            long fileSize
    ) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }

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
        if (!TextUtils.isEmpty(filePath)) {
            message.setFilePath(filePath);
        }
        message.setUpload(true);
        if (type == XmppMessage.TYPE_IMAGE) {
            sendImage(message);
        } else {
            mChatMessages.add(message);
            mChatContentView.notifyDataSetInvalidated(true);
            sendMessage(message);
        }
    }

    private void clickCollectionSend(CollectionEvery collection) {
        int type = collection.getXmppType();
        if (type == XmppMessage.TYPE_FILE) {
            boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mUseId, true);
            if (!isAllowSendFile && !isOk()) {
                tip(getString(R.string.tip_cannot_upload));
                return;
            }
        }
        // 不管什么收藏消息类型，都可能有文字，单独发一条文字消息，
        if (!TextUtils.isEmpty(collection.getCollectContent())) {
            sendText(collection.getCollectContent());
        }
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

    @Override
    public void clickShake() {

    }

    @Override
    public void clickGroupAssistant(GroupAssistantDetail groupAssistantDetail) {
        if (groupAssistantDetail == null) {
            return;
        }
        if (groupAssistantDetail.getHelper().getType() == 1) {
            // 自动回复信息 不处理
            Toast.makeText(mContext, getString(R.string.tip_group_assistant_auto_replay_not_support), Toast.LENGTH_SHORT).show();
        } else if (groupAssistantDetail.getHelper().getType() == 2) {
            // 直接跳转 软件 || 网页
            ShareParams shareParams = new ShareParams(mLoginUserId, mFriend.getRoomId(), mFriend.getUserId());

            String appPackName = groupAssistantDetail.getHelper().getAppPackName();
            String callBackClassName = groupAssistantDetail.getHelper().getCallBackClassName();
            Log.e("zq", "appPackName-->" + appPackName
                    + "，callBackClassName-->" + callBackClassName
                    + "，isAppInstalled-->" + AppUtils.isAppInstalled(mContext, appPackName));
            if (!TextUtils.isEmpty(appPackName)
                    && !TextUtils.isEmpty(callBackClassName)
                    && AppUtils.isAppInstalled(mContext, appPackName)) {
                Intent intent = new Intent();
                intent.setClassName(appPackName, callBackClassName);
                intent.putExtra("shareParams", JSON.toJSONString(shareParams));
                startActivity(intent);
            } else {
                WebViewActivity.start(mContext, groupAssistantDetail.getHelper().getLink(), JSON.toJSONString(shareParams));
            }
        } else if (groupAssistantDetail.getHelper().getType() == 3) {
            // 发送图文消息 点击消息跳转 软件 || 网页
            ChatMessage message = new ChatMessage();
            message.setType(XmppMessage.TYPE_SHARE_LINK);
            message.setFromUserId(mLoginUserId);
            message.setFromUserName(coreManager.getSelf().getNickName());
            message.setObjectId(JSON.toJSONString(groupAssistantDetail.getHelper().getOther()));
            mChatMessages.add(message);
            mChatContentView.notifyDataSetInvalidated(true);
            sendMessage(message);
        }
    }

    @Override
    public void clickLive() {
    }

    @Override
    public void onInputState() {

    }

    @Override
    public void clickDice() {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_DICE);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        int a = (int) (Math.random() * 6 + 1);
        message.setContent("" + a);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void clickRPS() {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_RPS);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        int a = (int) (Math.random() * 3 + 1);
        message.setContent("" + a);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    /**
     * 发送收藏图片或自定义表情，需要先下载到本地，得到图片宽高
     *
     * @param message
     */
    private void sendImage(ChatMessage message) {
        Downloader.getInstance().addDownload(message.getContent(), null, new DownloadListener() {
            @Override
            public void onStarted(String uri, View view) {

            }

            @Override
            public void onFailed(String uri, FailReason failReason, View view) {
                mChatMessages.add(message);
                mChatContentView.notifyDataSetInvalidated(true);
                sendMessage(message);
            }

            @Override
            public void onComplete(String uri, String filePath, View view) {
                message.setFilePath(filePath);
                File file = new File(filePath);
                long fileSize = file.length();
                message.setFileSize((int) fileSize);
                int[] imageParam = BitmapUtil.getImageParamByIntsFile(filePath);
                message.setLocation_x(String.valueOf(imageParam[0]));
                message.setLocation_y(String.valueOf(imageParam[1]));

                mChatMessages.add(message);
                mChatContentView.notifyDataSetInvalidated(true);
                sendMessage(message);
            }

            @Override
            public void onCancelled(String uri, View view) {
                mChatMessages.add(message);
                mChatContentView.notifyDataSetInvalidated(true);
                sendMessage(message);
            }
        });
    }

    /**
     * 复制自com.chat.weichat.ui.me.LocalVideoActivity#helloEventBus(com.chat.weichat.bean.event.MessageVideoFile)
     * 主要是CameraDemoActivity录制结束不走activity result, 而是发EventBus,
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUploadFileRate message) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            if (mChatMessages.get(i).getPacketId().equals(message.getPacketId())) {
                mChatMessages.get(i).setUploadSchedule(message.getRate());
                // 不能在这里setUpload，上传完成不代表上传成功，服务器可能没有正确返回url,相当于上传失败，
                mChatContentView.notifyDataSetChanged();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventCreateMucRoomTask message) {
        if (!TextUtils.equals(message.msgRoamTask.getUserId(), mUseId)) {
            // 不是这个群，无视，
            return;
        }
        mChatMessages.clear();
        mChatContentView.notifyDataSetChanged();

    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventRoomBlackChange message) {
        if (!TextUtils.equals(message.getRoomJid(), mUseId)) {
            // 不是这个群，无视，
            return;
        }
        getMyInfoInThisRoom();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventShowMarkerChanged message) {
        if (!TextUtils.equals(message.getUserId(), mUseId)) {
            // 不是这个群，无视，
            return;
        }
        updateMarker(message.getShowMarker());
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUploadCancel message) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            if (mChatMessages.get(i).getPacketId().equals(message.getPacketId())) {
                mChatMessages.remove(i);
                mChatContentView.notifyDataSetChanged();
                ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                break;
            }
        }
    }

    /**
     * SecureFlagGroup
     *
     * @param message
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventSecureNotify message) {
        if (message.getType() == EventSecureNotify.MULTI_SNED_RESET_KEY_MSG
                && TextUtils.equals(message.getChatMessage().getObjectId(), mFriend.getUserId())) {
            // 群主重置通信密钥，清空聊天记录
            mChatMessages.clear();
            mChatContentView.notifyDataSetChanged();
            // 收到此event时，onNewMessage方法会回调到，此处就不调用loadDatas了
            // loadDatas(true);
            return;
        }
        if (TextUtils.equals(message.getChatMessage().getObjectId(), mFriend.getUserId())) {
            if (message.getType() == EventSecureNotify.SINGLE_SNED_KEY_MSG) {
                // 单聊下收到群成员发送的chatKey，判断是否是发送到此群组的，是则刷新isLostKey状态
                // 群主重置了chatKey或其他途径导致我chatKey更新成功(isLostKey状态由1变0)，刷新页面
                mChatBottomView.isLostChatKeyGroup(false);
                findViewById(R.id.msg_up_ll2).setVisibility(View.GONE);
                // 清空消息重新加载数据以便
                mChatMessages.clear();
                loadDatas(true);
            } else if (message.getType() == EventSecureNotify.MULTI_SNED_KEY_MSG) {
                // 群聊下收到群成员发送的chatKey，判断本地是否有请求chatKey的那条消息，有则改变点击状态
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (TextUtils.equals(mChatMessages.get(i).getPacketId(), message.getChatMessage().getContent())) {
                        if (TextUtils.equals(message.getChatMessage().getFromUserId(), mLoginUserId)) {
                            // 自己发送的
                            mChatMessages.get(i).setFileSize(2);
                        } else {
                            // 别人发送的
                            mChatMessages.get(i).setFileSize(3);
                        }
                        mChatContentView.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageVideoFile message) {
        VideoFile videoFile = new VideoFile();
        videoFile.setCreateTime(TimeUtils.f_long_2_str(System.currentTimeMillis()));
        videoFile.setFileLength(message.timelen);
        videoFile.setFileSize(message.length);
        videoFile.setFilePath(message.path);
        videoFile.setOwnerId(coreManager.getSelf().getUserId());
        VideoFileDao.getInstance().addVideoFile(videoFile);
        String filePath = message.path;
        if (TextUtils.isEmpty(filePath)) {
            ToastUtil.showToast(this, R.string.record_failed);
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            ToastUtil.showToast(this, R.string.record_failed);
            return;
        }
        sendVideo(file);
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

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageLocalVideoFile message) {
        compress(message.file);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_FILE: // 系统管理器返回文件
                    String file_path = FileUtils.getPath(MucChatActivity.this, data.getData());
                    Log.e("xuan", "conversionFile: " + file_path);
                    if (file_path == null) {
                        ToastUtil.showToast(mContext, R.string.tip_file_not_supported);
                    } else {
                        requestFilePassword(s -> {
                            sendFile(new File(file_path), s);
                        });
                    }
                    break;
                case REQUEST_CODE_CAPTURE_PHOTO:
                    // 拍照返回
                    if (VideoRecorderActivity.checkIsImage(data)) {
                        helloEventBus(VideoRecorderActivity.parseImageResult(data));
                    } else {
                        helloEventBus(VideoRecorderActivity.parseVideoResult(data));
                    }
                    break;
                case REQUEST_CODE_PICK_PHOTO:
                    if (data != null) {
                        boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                        album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                    } else {
                        ToastUtil.showToast(this, R.string.c_photo_album_failed);
                    }
                    break;
                case REQUEST_CODE_SELECT_VIDEO: {
                    // 选择视频的返回
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
                                    sendVideo(file);
                                }
                            }
                        }
                    }
                    break;
                }
                case REQUEST_CODE_SELECT_Locate:
                    double latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
                    double longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
                    String address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
                    String snapshot = data.getStringExtra(AppConstant.EXTRA_SNAPSHOT);

                    if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)
                            && !TextUtils.isEmpty(snapshot)) {
                        sendLocate(latitude, longitude, address, snapshot);
                    } else {
                        ToastUtil.showToast(mContext, getString(R.string.server_cannot_location));
                    }
                    break;
                case REQUEST_CODE_SEND_COLLECTION: {
                    boolean isDynamicTrill = data.getBooleanExtra("isDynamicTrill", false);
                    String json = data.getStringExtra("data");
                    if (isDynamicTrill) {
                        PublicMessage publicMessage = JSON.parseObject(json, PublicMessage.class);
                        if (publicMessage != null) {
                            ChatMessage message = MessageUtil.generateShareMessage2Intent(mContext
                                    , mLoginUserId
                                    , publicMessage.getTargetType() == PublicMessage.CIRCLE ? 0 : 1, publicMessage
                                    , false);
                            message.setFromUserId(mLoginUserId);
                            message.setFromUserName(mLoginNickName);
                            if (message.getType() == XmppMessage.TYPE_IMAGE) {
                                sendImage(message);
                            } else {
                                mChatMessages.add(message);
                                mChatContentView.notifyDataSetInvalidated(true);
                                sendMessage(message);
                            }
                        }
                    } else {
                        CollectionEvery collection = JSON.parseObject(json, CollectionEvery.class);
                        clickCollectionSend(collection);
                    }
                    break;
                }
                case REQUEST_CODE_QUICK_SEND:
                    String image = QuickSendPreviewActivity.parseResult(data);
                    sendImage(new File(image));
                    break;
                case REQUEST_CODE_INVITE:
                    if (data != null && data.getExtras() != null) {
                        String packet = data.getStringExtra("VERIFY_MESSAGE_PACKET");
                        for (ChatMessage message : mChatMessages) {
                            if (TextUtils.equals(message.getPacketId(), packet)) {
                                message.setContent(message.getContent().replace(getString(R.string.to_confirm), getString(R.string.has_confirm)));
                                message.setDownload(true);
                                mChatContentView.notifyDataSetChanged();
                                break;
                            }
                        }
                    }
                    break;
                case REQUEST_CODE_SEND_CONTACT: {
                    List<Contacts> contactsList = SendContactsActivity.parseResult(data);
                    if (contactsList == null) {
                        ToastUtil.showToast(mContext, R.string.simple_data_error);
                    } else {
                        sendContacts(contactsList);
                    }
                    break;
                }
            }
        } else {
            switch (requestCode) {
                case ChatActivity.REQUEST_CODE_SEND_RED:
                    if (data != null) {
                        ChatMessage chatMessage = new ChatMessage(data.getStringExtra(AppConstant.EXTRA_CHAT_MESSAGE));
                        mChatMessages.add(chatMessage);
                        mChatContentView.notifyDataSetInvalidated(true);
                        sendMessage(chatMessage);
                        // 更新余额
                        CoreManager.updateMyBalance();
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
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
            // todo luban支持压缩.gif图，但是压缩之后的.gif图用glide加载与转换为gifDrawable都会出问题，所以,gif图不压缩了
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

    /**********************
     * MUC Message Listener
     ********************/
    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {
        if (TextUtils.isEmpty(msgId)) {
            return;
        }
        for (int i = 0; i < mChatMessages.size(); i++) {
            ChatMessage msg = mChatMessages.get(i);
            if (msgId.equals(msg.getPacketId())) {
                /**
                 * 之前发现对方已经收到消息了，这里还在转圈，退出重进之后又变为送达了，
                 * 调试时发现出现该问题是因为消息状态先更新的1，在更新的0，这里处理下
                 *
                 * 同时判断传入的参数因为重发成功发通知前加上了修改消息state，会导致这里msg和参数都是1的情况，也要刷新，
                 */
                if (msg.getMessageState() == ChatMessageListener.MESSAGE_SEND_SUCCESS
                        && messageState == ChatMessageListener.MESSAGE_SEND_ING) {
                    return;
                }
                msg.setMessageState(messageState);
                if (mChatContentView.shouldScrollToBottom()) {
                    mChatContentView.notifyDataSetInvalidated(true);
                } else {
                    mChatContentView.notifyDataSetChanged();
                }
                break;
            }
        }
    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) { // 新消息到来
        /**
         *  因为重发机制，当对方处于弱网时，不能及时接收我方的消息回执而给我方发送了两条甚至多条一样的消息
         *  而我方则会收到两条甚至多条一样的消息存入数据库(数据库已去重)，如果我正好处于消息发送方的聊天界面
         *  则会回调多次onNewMessage方法，而该方法内又没做去重，所以会出现显示两条一模一样的消息，退出当前界面在进入
         *  该界面又只有一条的问题
         *
         */
        if (mChatMessages.size() > 0) {
            for (int i = 0; i < mChatMessages.size(); i++) {// 群组控制消息可能一下子来几条
                if (TextUtils.equals(mChatMessages.get(i).getPacketId(), message.getPacketId())) {
                    return false;
                }
            }

            if (message.getTimeSend() < mChatMessages.get(0).getTimeSend()) {
                // 新消息的发送时间小于集合内最早的一条消息的发送时间，我们不直接直接添加到集合内，
                // 只能让他下拉出来
                return true;
            }
        }

        if (isGroupMsg != isGroupChat) {
            return false;
        }

        message.setDecrypted(true);// 回调过来的消息默认为已解密
        if (mUseId.compareToIgnoreCase(fromUserId) == 0) {// 是该人的聊天消息
            mChatContentView.post(() -> {
                if (mChatMessages.size() > 0
                        && mChatMessages.get(mChatMessages.size() - 1).getTimeSend() > message.getTimeSend()) {
                    // 集合长度大于0且集合内最后一条消息的timeSend大于新消息的timeSend，
                    // 将消息先加入到集合内，在根据timeSend对整个集合重新排序
                    mChatMessages.add(message);
                    Collections.sort(mChatMessages, (o1, o2) -> (int) (o1.getTimeSend() - o2.getTimeSend()));
                } else {
                    mChatMessages.add(message);
                }
                if (mChatContentView.shouldScrollToBottom()) {
                    mChatContentView.notifyDataSetInvalidated(true);
                } else {
                    // 判断该用户是否开启振动
/*
                    PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(MyApplication.getContext());
                    boolean input = privacySetting.getIsVibration() == 1;
                    if (input) {
                        if (System.currentTimeMillis() - startTime > 5000) {
                            // 振动提示一下
                            Vibrator vibrator = (Vibrator) MyApplication.getContext().getSystemService(VIBRATOR_SERVICE);
                            long[] pattern = {100, 400, 100, 400};
                            if (vibrator != null) {
                                vibrator.vibrate(pattern, -1);
                            }
                        }
                    } else {
                        startTime = System.currentTimeMillis();
                    }
*/
                    // 此处不判断是否开启振动
                    if (System.currentTimeMillis() - startTime > 2000) {
                        // 振动提示一下
                        Vibrator vibrator = (Vibrator) MyApplication.getContext().getSystemService(VIBRATOR_SERVICE);
                        long[] pattern = {100, 200, 100, 200};
                        if (vibrator != null) {
                            vibrator.vibrate(pattern, -1);
                        }
                    } else {
                        startTime = System.currentTimeMillis();
                    }
                    mChatContentView.notifyDataSetChanged();
                }
            });
            return true;
        }
        return false;
    }

    /**********************
     * MUC Operation Listener
     ********************/
    @Override
    public void onMyBeDelete(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {// 当前群组
            Toast.makeText(this, R.string.tip_been_kick, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onDeleteMucRoom(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {// 当前群组
            Toast.makeText(this, R.string.tip_group_been_disbanded, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onNickNameChange(String toUserId, String changedUserId, String changedName) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            // 群名已改变
            if (changedUserId.equals("ROOMNAMECHANGE")) {
                mFriend.setNickName(changedName);
                updateMemberCount(mCurrentMemberNum);
                return;
            }
            // 群内成员名改变
            if (changedUserId.equals(mLoginUserId)) {// 自己改变需要做些操作
                mFriend.setRoomMyNickName(changedName);
                mChatContentView.setCurGroup(true, changedName);
            }
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (TextUtils.equals(mChatMessages.get(i).getFromUserId(), changedUserId)) {
                    mChatMessages.get(i).setFromUserName(changedName);
                }
            }
            mChatContentView.notifyDataSetChanged();
        }
    }

    /*******************************************
     * 接收到EventBus后的后续操作
     ******************************************/

    @Override
    public void onMyVoiceBanned(String toUserId, int time) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            mFriend.setRoomTalkTime(time);
        }
    }

    /**
     * 收到新公告，
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNewNotice message) {
        if (TextUtils.equals(mFriend.getUserId(), message.getRoomJid())) {
            setLastNotice(message.getText());
            getMyInfoInThisRoom();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventRemoveNotice message) {
        if (TextUtils.equals(mFriend.getUserId(), message.getRoomJid())) {
            getMyInfoInThisRoom();
        }
    }

    /**
     * 收到新公告，
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventRoomNotice message) {
        setLastNotice(message.getText());
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, "GroupAssistant")) {
            if (mChatBottomView != null) {
                mChatBottomView.notifyAssistant();
            }
        } else if (TextUtils.equals(message.tag, EventNotifyByTag.Speak)) {
            boolean isSpeaker = PreferenceUtils.getBoolean(MyApplication.getContext(),
                    Constants.SPEAKER_AUTO_SWITCH + CoreManager.requireSelf(MyApplication.getContext()).getUserId(), true);
            findViewById(R.id.iv_title_center).setVisibility(isSpeaker ? View.GONE : View.VISIBLE);
            if (VoiceManager.instance().getMediaPlayer().isPlaying()) {
                // 当前正在播放语音，如果为扬声器切换到语音，仿微信，重新播放一遍
                if (!isSpeaker) {
                    VoiceManager.instance().earpieceUser();
                }
                mAudioModeManger.setSpeakerPhoneOn(isSpeaker);
                if (!isSpeaker) {
                    mTvTitle.postDelayed(() -> VoiceManager.instance().earpieceUser(), 200);
                }
            } else {
                mAudioModeManger.setSpeakerPhoneOn(isSpeaker);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventXMPPJoinGroupFailed message) {
        if (message.roomJId.equals(mFriend.getUserId())) {
            DialogHelper.tip(MucChatActivity.this, "加入群组失败，暂时无法收发此群组的消息，可尝试退出当前界面重进或关闭app重进");
        }
    }

    // 发送多选消息
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventMoreSelected message) {
        List<ChatMessage> mSelectedMessageList = new ArrayList<>();
        if (message.getToUserId().equals("MoreSelectedCollection") || message.getToUserId().equals("MoreSelectedEmail")) {// 多选 收藏 || 保存
            moreSelected(false, 0);
            return;
        }
        if (message.getToUserId().equals("MoreSelectedDelete")) {// 多选 删除
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (mChatMessages.get(i).isMoreSelected) {
                    if (ChatMessageDao.getInstance().updateExpiredStatus(mLoginUserId, mUseId, mChatMessages.get(i).getPacketId())) {
                        Log.e("more_selected", "删除成功");
                    } else {
                        Log.e("more_selected", "删除失败");
                    }
                    mSelectedMessageList.add(mChatMessages.get(i));
                }
            }

            String mMsgIdListStr = "";
            for (int i = 0; i < mSelectedMessageList.size(); i++) {
                if (i == mSelectedMessageList.size() - 1) {
                    mMsgIdListStr += mSelectedMessageList.get(i).getPacketId();
                } else {
                    mMsgIdListStr += mSelectedMessageList.get(i).getPacketId() + ",";
                }
            }
            deleteMessage(mMsgIdListStr);// 服务端也需要删除

            mChatMessages.removeAll(mSelectedMessageList);
            moreSelected(false, 0);
        } else {// 多选 转发
            if (message.isSingleOrMerge()) {// 合并转发
                List<String> mStringHistory = new ArrayList<>();
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).isMoreSelected) {
                        String body = mChatMessages.get(i).toJsonString();
                        ChatMessage chatMessage = new ChatMessage(body);
                        boolean isAllowSendCard = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mFriend.getUserId(), true);
                        if (chatMessage.getType() == ChatMessage.TYPE_CARD && !isAllowSendCard && !isOk()) {
                            Toast.makeText(this, getString(R.string.tip_member_disable_privately_chat), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mStringHistory.add(body);
                    }
                }
                String detail = JSON.toJSONString(mStringHistory);
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType(XmppMessage.TYPE_CHAT_HISTORY);
                chatMessage.setFromUserId(mLoginUserId);
                chatMessage.setFromUserName(mLoginNickName);
                chatMessage.setToUserId(message.getToUserId());
                chatMessage.setContent(detail);
                chatMessage.setObjectId(getString(R.string.group_chat_history));
                chatMessage.setMySend(true);
                chatMessage.setSendRead(false);
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                if (message.isDevice()) {
                    chatMessage.setToId(chatMessage.getToUserId());
                    chatMessage.setToUserId(mLoginUserId);
                }
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.getToUserId(), chatMessage);
                if (message.isGroupMsg()) {
                    coreManager.sendMucChatMessage(chatMessage.getToUserId(), chatMessage);
                } else {
                    coreManager.sendChatMessage(chatMessage.getToUserId(), chatMessage);
                }
                if (message.getToUserId().equals(mFriend.getUserId())) {// 转发给当前对象
                    mChatMessages.add(chatMessage);
                }
            } else {// 逐条转发
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).isMoreSelected) {
                        ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, mFriend.getUserId(), mChatMessages.get(i).getPacketId());
                        if (chatMessage.getType() == XmppMessage.TYPE_TEXT) {
                            // @消息，转发出去时变为纯文本，不具备@功能
                            chatMessage.setObjectId("");
                        } else if (chatMessage.getType() == XmppMessage.TYPE_RED) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_red_packet));
                        } else if (chatMessage.getType() == XmppMessage.TYPE_TRANSFER) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.tip_transfer_money));
                        } else if (chatMessage.getType() == XmppMessage.TYPE_SHAKE) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_shake));
                        } else if (chatMessage.getType() == XmppMessage.TYPE_DICE
                                || chatMessage.getType() == XmppMessage.TYPE_RPS) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.type_emoij));
                        } else if (chatMessage.getType() >= XmppMessage.TYPE_IS_CONNECT_VOICE
                                && chatMessage.getType() <= XmppMessage.TYPE_IS_MU_START_CONNECT_LIVE) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_video_voice));
                        } else if (chatMessage.getType() == XmppMessage.TYPE_SECURE_LOST_KEY) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.request_chat_key_group));
                        }
                        chatMessage.setFromUserId(mLoginUserId);
                        chatMessage.setFromUserName(mLoginNickName);
                        chatMessage.setToUserId(message.getToUserId());
                        chatMessage.setUpload(true);
                        chatMessage.setMySend(true);
                        chatMessage.setSendRead(false);
                        chatMessage.setReadPersons(0);
                        chatMessage.setMessageState(0);
                        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                        chatMessage.setFromId(null);
                        chatMessage.setToId(null);
                        if (message.isDevice()) {
                            chatMessage.setToId(chatMessage.getToUserId());
                            chatMessage.setToUserId(mLoginUserId);
                        }
                        boolean isAllowSendCard = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mFriend.getUserId(), true);
                        if (chatMessage.getType() == ChatMessage.TYPE_CARD && !isAllowSendCard && !isOk()) {
                            Toast.makeText(this, getString(R.string.tip_member_disable_privately_chat), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mSelectedMessageList.add(chatMessage);
                    }
                }

                for (int i = 0; i < mSelectedMessageList.size(); i++) {
                    ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.getToUserId(), mSelectedMessageList.get(i));
                    if (message.isGroupMsg()) {
                        coreManager.sendMucChatMessage(mSelectedMessageList.get(i).getToUserId(), mSelectedMessageList.get(i));
                    } else {
                        coreManager.sendChatMessage(mSelectedMessageList.get(i).getToUserId(), mSelectedMessageList.get(i));
                    }

                    if (message.getToUserId().equals(mFriend.getUserId())) {// 转发给当前对象
                        mChatMessages.add(mSelectedMessageList.get(i));
                    }
                }
            }
        }
        // 现多选转发的取消多选状态放到转发类了，这个方法每个else块单独调用，因为选择转发至多个好友时，如果在第一次回调的event内将多选状态取消
        // 剩余回调多选的消息全部被清空了
        // moreSelected(false, 0);
    }

    public void moreSelected(boolean isShow, int position) {
        mChatBottomView.showMoreSelectMenu(isShow);
        if (isShow) {
            findViewById(R.id.iv_title_left).setVisibility(View.GONE);
            mTvTitleLeft.setVisibility(View.VISIBLE);
            mChatMessages.get(position).setMoreSelected(true);
        } else {
            findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
            mTvTitleLeft.setVisibility(View.GONE);
            for (int i = 0; i < mChatMessages.size(); i++) {
                mChatMessages.get(i).setMoreSelected(false);
            }
        }
        mChatContentView.setIsShowMoreSelect(isShow);
        mChatContentView.notifyDataSetChanged();
    }

    /**
     * 发送我的课程
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageUploadChatRecord message) {
        if (!TextUtils.equals(mFriend.getUserId(), message.toUserId)) {
            return;
        }
        DialogHelper.showLimitSingleInputDialog(this
                , getString(R.string.create_course)
                , "", getString(R.string.input_course_name)
                , text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastUtil.showToast(this, getString(R.string.name_course_error));
                        return false;
                    }
                    upLoadChatList(message.chatIds, text);
                    return true;
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventSendReadChanged message) {
        if (TextUtils.equals(message.userId, mFriend.getUserId())) {
            mChatContentView.setIsSendRead(message.isSendRead);
            mChatContentView.notifyDataSetChanged();
        }
    }

    private void upLoadChatList(String chatIds, String name) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageIds", chatIds);
        params.put("userId", mLoginUserId);
        params.put("courseName", name);
        params.put("createTime", TimeUtils.sk_time_current_time() / 1000 + "");
        params.put("roomJid", mUseId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.post().url(coreManager.getConfig().USER_ADD_COURSE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(MucChatActivity.this, R.string.tip_create_cource_success);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MucChatActivity.this);
                    }
                });
    }

    private void initRoomMember() {
        if (mFriend.getGroupStatus() == 0
                || mFriend.getGroupStatus() == 3
                || mFriend.getGroupStatus() == 4) {
            // 正常状态
            List<RoomMember> roomMemberList = RoomMemberDao.getInstance().getRoomMember(roomId);
            if (roomMemberList.size() > 0) {
                mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(roomId, mLoginUserId);
                if (mRoomMember != null) {// 更新群成员表
                    onRoleChanged(mRoomMember.getRole());
                }
                // 成员列表传进去为了显示管理员的头像，
                mChatContentView.setRoomMemberList(roomMemberList);
            } else {
                loadMembers(roomId, false);
            }
        }
    }

    /*******************************************
     * 初始化ActionBar与其点击事件
     ******************************************/

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBack();
            }
        });

        mTvTitleLeft = (TextView) findViewById(R.id.tv_title_left);
        mTvTitleLeft.setVisibility(View.GONE);
        mTvTitleLeft.setText(getString(R.string.cancel));
        mTvTitleLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreSelected(false, 0);
            }
        });
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        if (!TextUtils.isEmpty(mNickName)) {
            mTvTitle.setText(mNickName);
        }
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.chat_more);
        ivRight.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                if (mFriend.getGroupStatus() == 0
                        || mFriend.getGroupStatus() == 4) {
                    mChatBottomView.reset();
                    mChatBottomView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 进入房间信息的Activity
                            Intent intent = new Intent(MucChatActivity.this, RoomInfoActivity.class);
                            intent.putExtra(AppConstant.EXTRA_USER_ID, mUseId);
                            intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
                            startActivity(intent);
                        }
                    }, 100);
                }
            }
        });

        // 加载聊天背景
        mChatBgIv = findViewById(R.id.chat_bg);
        loadBackdrop();
    }

    public void loadBackdrop() {
        String mChatBgPath = PreferenceUtils.getString(this, Constants.SET_CHAT_BACKGROUND_PATH
                + mFriend.getUserId() + mLoginUserId, "reset");

        String mChatBg = PreferenceUtils.getString(this, Constants.SET_CHAT_BACKGROUND
                + mFriend.getUserId() + mLoginUserId, "reset");

        if (TextUtils.isEmpty(mChatBgPath)
                || mChatBg.equals("reset")) {// 未设置聊天背景或者还原了聊天背景
            mChatBgIv.setImageDrawable(null);
            return;
        }

        File file = new File(mChatBgPath);
        if (file.exists()) {// 加载本地
            if (mChatBgPath.toLowerCase().endsWith("gif")) {
                try {
                    GifDrawable gifDrawable = new GifDrawable(file);
                    mChatBgIv.setImageDrawable(gifDrawable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ImageLoadHelper.showFileWithError(
                        MucChatActivity.this,
                        file,
                        R.drawable.fez,
                        mChatBgIv
                );
            }
        } else {// 加载网络
            ImageLoadHelper.showImageWithError(
                    MucChatActivity.this,
                    mChatBg,
                    R.color.chat_bg,
                    mChatBgIv
            );
        }
    }

    /**
     * 消息漫游
     */
    public void getNetSingle() {
        Map<String, String> params = new HashMap<>();

        long startSeqNo = 0;
        long endSeqNo = 0;
        if (mChatMessages != null && mChatMessages.size() > 0) {
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (mChatMessages.get(i).getSeqNo() > 0) {
                    endSeqNo = mChatMessages.get(i).getSeqNo();
                    break;
                }
            }
            if (endSeqNo == 0) {
                // 本地消息均无序号，同下
                endSeqNo = 1000000000;
            }
        } else {
            // 本地无消息时，endSeqNo定一个非常大的数字，服务端就会取出当前群组真实的最大序号做为endSeqNo
            endSeqNo = 1000000000;
        }

        final MsgRoamTask mLastMsgRoamTask = MsgRoamTaskDao.getInstance().getFriendLastMsgRoamTask(mLoginUserId, mFriend.getUserId());
        if (mLastMsgRoamTask != null) {// 该群组存在任务，为startSeqNo与endSeqNo重新赋值
            startSeqNo = mLastMsgRoamTask.getStartSeqNo();
            endSeqNo = mLastMsgRoamTask.getEndSeqNo();
            Log.e(SeqNoManager.TAG, "本地有任务列表，根据序号任务拉漫游==>" + startSeqNo + " - " + endSeqNo);

            List<MsgRoamTask> friendMsgRoamTaskList = MsgRoamTaskDao.getInstance().getFriendMsgRoamTaskList(mLoginUserId, mFriend.getUserId());
            for (int i = 0; i < friendMsgRoamTaskList.size(); i++) {
                Log.e(SeqNoManager.TAG, "当前好友已有任务-->" + friendMsgRoamTaskList.get(i).getStartSeqNo() + " - " + friendMsgRoamTaskList.get(i).getEndSeqNo());
            }
        }

        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomJid", mUseId);
        params.put("startSeqNo", String.valueOf(startSeqNo));
        params.put("endSeqNo", String.valueOf(endSeqNo));
        params.put("pageSize", String.valueOf(Constants.MSG_ROMING_PAGE_SIZE));

        HttpUtils.get().url(coreManager.getConfig().GET_CHAT_MSG_MUC_BY_SEQ_NO_MY)
                .params(params)
                .build()
                .execute(new ListCallback<ChatRecord>(ChatRecord.class) {
                    @Override
                    public void onResponse(ArrayResult<ChatRecord> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<ChatRecord> chatRecordList = result.getData();
                            long mLastTaskNewEndSeqNo = 0;
                            if (chatRecordList != null && chatRecordList.size() > 0) {
                                for (int i = 0; i < chatRecordList.size(); i++) {
                                    ChatRecord data = chatRecordList.get(i);
                                    String body = data.getMessage();
                                    // body = body.replaceAll("&quot;", "\"");
                                    ChatMessage chatMessage = jsonToMessage(body);

                                    if (!TextUtils.isEmpty(chatMessage.getFromUserId())
                                            && chatMessage.getFromUserId().equals(mLoginUserId)) {
                                        chatMessage.setMySend(true);
                                    }
                                    chatMessage.setReadPersons(data.getReadCount());
                                    // 漫游的群聊消息，默认为已读
                                    chatMessage.setSendRead(true);
                                    // 漫游的默认已上传
                                    chatMessage.setUpload(true);
                                    chatMessage.setUploadSchedule(100);
                                    chatMessage.setMessageState(MESSAGE_SEND_SUCCESS);

                                    if (TextUtils.isEmpty(chatMessage.getPacketId())) {
                                        if (!TextUtils.isEmpty(data.getMessageId())) {
                                            chatMessage.setPacketId(data.getMessageId());
                                        } else {
                                            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                        }
                                    }
                                    chatMessage.setSeqNo(data.getSeqNo());
                                    if (i == chatRecordList.size() - 1) {
                                        // 消息为倒序返回
                                        mLastTaskNewEndSeqNo = chatMessage.getSeqNo();
                                        Log.e(SeqNoManager.TAG, "此次漫游结束，查看最后一条消息的seqNo为多少-->" + mLastTaskNewEndSeqNo);
                                    }

                                    // 有一种情况，因为服务器1个小时删除一次，所以可能会拉到已过期的时间
                                    if (chatMessage.getDeleteTime() > 1 && chatMessage.getDeleteTime() < TimeUtils.sk_time_current_time() / 1000) {
                                        // 已过期的消息,扔掉
                                        Log.e("msg", "消息过期，扔掉");
                                        continue;
                                    }

                                    if (MessageUtil.filterRoamingMessage(chatMessage.getType(), true)) {
                                        ChatMessageDao.getInstance().saveRoamingChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage, true);
                                    }
                                }
                                mHasMoreData = chatRecordList.size() >= mPageSize;
                            } else {
                                // 漫游下来的消息不足一页
                                mHasMoreData = false;
                                mChatContentView.headerRefreshingCompleted();
                                mChatContentView.setNeedRefresh(false);
                            }

                            if (mLastMsgRoamTask != null) {
                                // 任务不为空，必须支持继续下拉
                                mHasMoreData = true;
                                mChatContentView.setNeedRefresh(true);

                                if (chatRecordList != null && chatRecordList.size() > 0) {
                                    // 正常返回消息，该任务还未完成，更新最后一条任务的endSeqNo
                                    MsgRoamTaskDao.getInstance().updateMsgRoamTaskEndSeqNo(mLoginUserId, mLastMsgRoamTask.getUserId(),
                                            mLastMsgRoamTask.getTaskId(), mLastTaskNewEndSeqNo);
                                    Log.e(SeqNoManager.TAG, " 正常返回消息，该任务还未完成，更新最后一条任务的endSeqNo-->" + mLastTaskNewEndSeqNo);
                                } else {
                                    // 该段任务已结束，可删除
                                    Log.e(SeqNoManager.TAG, " 该段任务已结束，删除");
                                    MsgRoamTaskDao.getInstance().deleteMsgRoamTask(mLoginUserId, mLastMsgRoamTask.getUserId(), mLastMsgRoamTask.getTaskId());
                                }
                            }
                            // 必须要放到updateMsgRoamTaskEndSeqNo方法后面
                            notifyChatAdapter();
                        } else {
                            ToastUtil.showErrorData(MucChatActivity.this);
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    public ChatMessage jsonToMessage(String json) {
        Gson gson = new Gson();
        com.tongxin.caihong.socket.msg.ChatMessage chatMessage = gson.fromJson(json, com.tongxin.caihong.socket.msg.ChatMessage.class);
        return chatMessage.toSkMessage(mLoginUserId);
    }

    private void notifyChatAdapter() {
        // 代码运行到这里说明之前一定没有查出消息，同步了漫游之后我们再次使用 mMinId 去查询一下数据
        if (mChatMessages.size() > 0) {
            mMinId = mChatMessages.get(0).getTimeSend();
        } else {
            mMinId = TimeUtils.sk_time_current_time();
        }
        List<ChatMessage> chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mChatMessages, mLoginUserId,
                mFriend.getUserId(), mMinId, mPageSize);

        for (int i = 0; i < chatLists.size(); i++) {
            ChatMessage message = chatLists.get(i);
            mChatMessages.add(0, message);
        }

        mChatContentView.notifyDataSetAddedItemsToTop(chatLists.size());
        mChatContentView.headerRefreshingCompleted();
        if (!mHasMoreData) {
            mChatContentView.setNeedRefresh(false);
        }
    }

    /*******************************************
     * 获取音视频会议id && @群成员(当数据库内无该张群组表时，再去访问服务器)
     * 转发 && 消息加密 && 高亮显示@消息
     ******************************************/
    private void loadMembers(final String roomId, final boolean isAtAction) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("pageSize", Constants.MUC_MEMBER_PAGE_SIZE);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     isLoaded = true;
                                     final MucRoom mucRoom = result.getData();
                                     update(mucRoom, isAtAction);
                                 } else {
                                     ToastUtil.showErrorData(mContext);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                             }
                         }
                );
    }

    @SuppressLint("SetTextI18n")
    private void updateMemberCount(int userSize) {
        mCurrentMemberNum = userSize;
        mTvTitle.setText(mFriend.getNickName() + "（" + userSize + "" + getString(R.string.people) + "）");
    }

    private void instantChatMessage() {
        if (!TextUtils.isEmpty(instantMessage)) {
            String toUserId = getIntent().getStringExtra("fromUserId");
            ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, instantMessage);
            chatMessage.setFromId(null);
            chatMessage.setToId(null);
            boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mUseId, true);
            if (chatMessage.getType() == ChatMessage.TYPE_FILE && !isAllowSendFile && !isOk()) {
                tip(getString(R.string.tip_cannot_upload));
                return;
            }
            boolean isAllowSendCard = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mFriend.getUserId(), true);
            if (chatMessage.getType() == ChatMessage.TYPE_CARD && !isAllowSendCard && !isOk()) {
                Toast.makeText(this, getString(R.string.tip_member_disable_privately_chat), Toast.LENGTH_SHORT).show();
                return;
            }
            TrillStatisticsHelper.share(mContext, coreManager, chatMessage);
            chatMessage.setFromUserId(mLoginUserId);
            chatMessage.setFromUserName(mLoginNickName);
            chatMessage.setToUserId(mFriend.getUserId());
            chatMessage.setUpload(true);
            chatMessage.setMySend(true);
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            mChatMessages.add(chatMessage);
            mChatContentView.notifyDataSetInvalidated(true);
            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage);
            send(chatMessage);
            instantMessage = null;
        }
    }

    /**
     * 获取自己在该群组的信息以及群属性
     * 获取该群组的群主与管理员信息
     */
    private void getMyInfoInThisRoom() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// 数据结果与room/get接口一样，只是服务端没有返回群成员列表的数据
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();

                                     if (mucRoom.getS() == -1) {// 该群组已被后台禁用
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mFriend.getUserId(), 3);// 更新本地群组状态

                                         groupTip(getString(R.string.tip_group_disable_by_service));
                                         return;
                                     }

                                     if (mucRoom.getMember() == null) {// 被踢出该群组
                                         coreManager.exitMucChat(mucRoom.getJid());// XMPP退群
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 1);// 更新本地群组状态

                                         groupTip(getString(R.string.tip_been_kick));
                                     } else {// 正常状态
                                         if (mucRoom.getMember().getIsBlack() == 1) {
                                             //如果我在群组中已被拉黑
                                             mFriend.setGroupStatus(4);
                                             FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 4);// 更新本地群组状态
                                         } else {
                                             mFriend.setGroupStatus(0);
                                             FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 0);// 更新本地群组状态
                                         }
                                         List<RoomMember> roomMemberList = update(mucRoom, false);

                                         // 更新禁言状态
                                         FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, mucRoom.getJid(), mucRoom.getMember().getTalkTime());
                                         onMyVoiceBanned(mucRoom.getJid(), mucRoom.getMember().getTalkTime());

                                         // 更新个人职位
                                         // 此时可能正在初始化群成员信息，本地可能没有自己的信息，
                                         RoomMemberDao.getInstance().updateRoomMemberRole(mucRoom.getId(), mLoginUserId, mucRoom.getMember().getRole());
                                         onRoleChanged(mucRoom.getMember().getRole());
                                         mChatContentView.setRoomMemberList(roomMemberList);

                                         // 如果有转发进来的消息就处理一下，
                                         instantChatMessage();
                                         if (mucRoom.getMember().getReadNotice() == 0
                                                 && mucRoom.getLastNotice() != null
                                                 && !TextUtils.isEmpty(mucRoom.getLastNotice().getText())) {
                                             // 有未读公告，弹窗
                                             if (groupNoticeDialog != null && groupNoticeDialog.isShowing()) {
                                                 groupNoticeDialog.dismiss();
                                             }
                                             groupNoticeDialog = new GroupNoticeDialog(mContext, mucRoom.getMember().getRole(), mucRoom.getId()
                                                     , mucRoom.getLastNotice());
                                             groupNoticeDialog.show();
                                             updateNoticeRead(mucRoom.getId());
                                         }
                                     }
                                 } else if (Result.checkError(result, Result.CODE_ROOM_GONE)) {
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mFriend.getUserId(), 2);// 更新本地群组状态
                                     groupTip(TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.tip_group_been_disbanded) : result.getResultMsg());
                                 } else {
                                     Result.toastElse(mContext, result);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                             }
                         }
                );
    }

    private void groupTip(String tip) {
        tip(tip, true);
    }

    /*******************************************
     * 是否被踢出群组|| 群组已经解散 && 是否离线&&重连
     ******************************************/
    public boolean getGroupStatus() {
        if (mFriend.getGroupStatus() == 1) {
            tip(getString(R.string.tip_been_kick));
            return true;
        } else if (mFriend.getGroupStatus() == 2) {
            tip(getString(R.string.tip_disbanded));
            return true;
        } else {
            return false;
        }
    }

    public void tip(String tip) {
        tip(tip, false);
    }

    /**
     * @param finish 确定后是否结束当前页面，为ture,
     */
    private void tip(String tip, boolean finish) {
        if (isFinishing()) {
            return;
        }
        if (tipDialog == null) {
            tipDialog = new TipDialog(MucChatActivity.this);
        }
        // 需要直接结束页面的情况不能让对话框被返回键干掉，
        tipDialog.setCancelable(!finish);
        if (tipDialog.isShowing()) {
            tipDialog.dismiss();
        }
        if (finish) {
            tipDialog.setmConfirmOnClickListener(tip, this::finish);
        } else {
            tipDialog.setTip(tip);
        }
        tipDialog.show();
    }

    public boolean isOk() {// 群主与管理员不受限制
        boolean isOk = true;
        if (mRoomMember != null) {
            if (mRoomMember.getRole() == 1 || mRoomMember.getRole() == 2) {
                isOk = true;
            } else {
                isOk = false;
            }
        }
        return isOk;
    }

    public boolean isAuthenticated() {
        boolean isLogin = coreManager.isLogin();
        if (!isLogin) {
            coreManager.autoReconnect(this);
        }
        // Todo 离线时发消息也不能return，自动重连...，让消息转圈(有重发)
        // return !isLogin;
        return false;
    }

    // 更新禁言状态，影响全体禁言以及隐身人禁言，
    private void updateBannedStatus() {
        // 禁言状态
        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mFriend.getUserId(), false);
        if (mRoomMember != null) {
            if (mRoomMember.isInvisible()) {
                mChatBottomView.isBanned(true, R.string.hint_invisible);
            } else {
                mChatBottomView.isAllBanned(isAllShutUp && mRoomMember.isAllBannedEffective());
            }
        } else {
            mChatBottomView.isAllBanned(isAllShutUp);
        }
    }

    private void onRoleChanged(int role) {
        if (mRoomMember != null) {
            mRoomMember.setRole(role);
        }
        mChatContentView.setRole(role);
        mChatBottomView.setRole(role);
        updateBannedStatus();
        // 更新私密设置，禁止私聊状态传入ChatContentView,
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        updateSecret(!isAllowSecretlyChat && !isOk());
    }

    /**
     * 更群群公告为已读状态
     *
     * @param roomId
     */
    private void updateNoticeRead(String roomId) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_NOTICE_READ)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(final ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private List<RoomMember> update(MucRoom mucRoom, boolean isAtAction) {
        if (!TextUtils.equals(mFriend.getNickName(), mucRoom.getName())) {
            // 群名不对情况手动同步，
            FriendDao.getInstance().updateMucFriendRoomName(mFriend.getUserId(), mucRoom.getName());
            ListenerManager.getInstance().notifyNickNameChanged(mFriend.getUserId(), "ROOMNAMECHANGE", mucRoom.getName());
        }
        updateMarker(mucRoom.getShowMarker());
        showMember = mucRoom.getShowMember();
        groupOwnerId = mucRoom.getUserId();
        // 更新部分群属性
        MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(), mucRoom.getAllowSendCard(),
                mucRoom.getAllowConference(), mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime(), mucRoom.getAllowOpenLive());
        PreferenceUtils.putBoolean(MyApplication.getContext(),
                Constants.IS_NEED_OWNER_ALLOW_NORMAL_INVITE_FRIEND + mucRoom.getJid(), mucRoom.getIsNeedVerify() == 1);
        PreferenceUtils.putBoolean(MyApplication.getContext(),
                Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mucRoom.getJid(), mucRoom.getAllowUploadFile() == 1);
        // 强制隐藏
        // 展示后台配置群组网址
        // setWebAddress(mucRoom.getTitleUrlList());
        // 置顶展示最新公告，
        setLastNotice(mucRoom.getLastNotice());
        // 更新私密设置，禁止私聊状态传入ChatContentView,
        updateSecret(mucRoom.getAllowSendCard() != 1 && !isOk());
        // 更新群成员人数，
        updateMemberCount(mucRoom.getUserSize());
        // 更新消息保存时长
        mFriend.setChatRecordTimeOut(mucRoom.getChatRecordTimeOut());
        FriendDao.getInstance().updateChatRecordTimeOut(mFriend.getUserId(), mucRoom.getChatRecordTimeOut());

        List<RoomMember> roomMemberList = new ArrayList<>();
        for (int i = 0; i < mucRoom.getMembers().size(); i++) {
            RoomMember roomMember = new RoomMember();
            roomMember.setRoomId(mucRoom.getId());
            roomMember.setUserId(mucRoom.getMembers().get(i).getUserId());
            roomMember.setUserName(mucRoom.getMembers().get(i).getNickName());
            if (TextUtils.isEmpty(mucRoom.getMembers().get(i).getRemarkName())) {
                roomMember.setCardName(mucRoom.getMembers().get(i).getNickName());
            } else {
                roomMember.setCardName(mucRoom.getMembers().get(i).getRemarkName());
            }
            roomMember.setRole(mucRoom.getMembers().get(i).getRole());
            roomMember.setHiding(mucRoom.getMembers().get(i).getHiding());
            roomMember.setCreateTime(mucRoom.getMembers().get(i).getCreateTime());
            roomMemberList.add(roomMember);
        }
        MucRoomMember myself = mucRoom.getMember();
        if (myself != null) {
            // 更新群内昵称
            mFriend.setRoomMyNickName(myself.getNickName());
            FriendDao.getInstance().updateRoomMyNickName(mFriend.getUserId(), myself.getNickName());

            RoomMember roomMember = new RoomMember();
            roomMember.setRoomId(mucRoom.getId());
            roomMember.setUserId(myself.getUserId());
            roomMember.setUserName(myself.getNickName());
            if (TextUtils.isEmpty(myself.getRemarkName())) {
                roomMember.setCardName(myself.getNickName());
            } else {
                roomMember.setCardName(myself.getRemarkName());
            }
            roomMember.setRole(myself.getRole());
            roomMember.setHiding(myself.getHiding());
            roomMember.setCreateTime(myself.getCreateTime());
            mRoomMember = roomMember;
            onRoleChanged(roomMember.getRole());
            roomMemberList.add(roomMember);
        }

        AsyncUtils.doAsync(this, mucChatActivityAsyncContext -> {
            for (int i = 0; i < roomMemberList.size(); i++) {// 在异步任务内存储
                RoomMemberDao.getInstance().saveSingleRoomMember(mucRoom.getId(), roomMemberList.get(i));
            }
        });
        int existsMemberSize = RoomMemberDao.getInstance().getRoomMemberCount(roomId);
        if (existsMemberSize < mucRoom.getUserSize() && existsMemberSize < Integer.parseInt(Constants.MUC_MEMBER_PAGE_SIZE)) {
            // 本地连第一页成员都没有，至少先获取第一页，避免用到群成员列表时出问题，
            if (!isLoaded) {
                loadMembers(roomId, false);
            }
        }

        // SecureFlagGroup
        FriendDao.getInstance().updateEncryptType(mFriend.getUserId(), mucRoom.getEncryptType());
        if (myself != null && mucRoom.getIsSecretGroup() == 1) {
            try {
                FriendDao.getInstance().updateSecretGroup(mucRoom.getJid());
                String chatKey = new String(RSA.decryptFromBase64(myself.getChatKeyGroup(), Base64.decode(SecureChatUtil.getRSAPrivateKey(mLoginUserId))));
                FriendDao.getInstance().updateChatKeyGroup(mucRoom.getJid(), SecureChatUtil.encryptChatKey(mucRoom.getJid(), chatKey));
                Log.e("msg", "设置chatKey成功-->" + chatKey);
            } catch (Exception e) {
                Log.e("msg", "设置chatKey失败");
                FriendDao.getInstance().updateIsLostChatKeyGroup(mucRoom.getJid(), 1);
                mChatBottomView.isLostChatKeyGroup(true);
                findViewById(R.id.msg_up_ll2).setVisibility(View.VISIBLE);
            }
        }

        if (isAtAction) {// 为@操作 存表之后在查询 跳转至@界面
            // 移除掉自己
            for (int i = 0; i < roomMemberList.size(); i++) {
                if (roomMemberList.get(i).getUserId().equals(mLoginUserId)) {
                    roomMemberList.remove(roomMemberList.get(i));
                }
            }
            mSelectRoomMemberPopupWindow = new SelectRoomMemberPopupWindow(this,
                    roomMemberList, mRoomMember.getRole(), mFriend.getRoomId(), mFriend.getUserId(), showMember, this);
            mSelectRoomMemberPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
        }

        return roomMemberList;
    }

    private void updateMarker(int showMarker) {
        if (showMarker == 1) {
            marker.setText(coreManager.getSelf().getAccount());
            marker.setVisibility(View.VISIBLE);
        } else {
            marker.setVisibility(View.GONE);
        }
    }

    private void sendSecureChatReadyTip() {
        if (isSecureAlreadyTipd) {
            return;
        }
        isSecureAlreadyTipd = true;
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TIP);
        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setContent(getString(R.string.msg_open_secure_chat_ready));
        chatMessage.setPacketId(AppConfig.getApiKey() + "tip");
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        mChatMessages.add(chatMessage);
        mChatContentView.notifyDataSetChanged();
    }


    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventMeetingInvite message) {
        for (int i = 0; i < message.meetinglist.size(); i++) {
            ChatMessage messagevoice = new ChatMessage();
            int type;
            String str;
            if (message.type == CallConstants.Audio_Meet) {
                type = XmppMessage.TYPE_IS_MU_CONNECT_VOICE;
                str = getString(R.string.tip_invite_voice_meeting);
            } else if (message.type == CallConstants.Video_Meet) {
                type = XmppMessage.TYPE_IS_MU_CONNECT_VIDEO;
                str = getString(R.string.tip_invite_video_meeting);
            } else {
                type = XmppMessage.TYPE_IS_MU_CONNECT_TALK;
                str = getString(R.string.tip_invite_talk_meeting);
            }
            messagevoice.setType(type);
            messagevoice.setContent(str);
            messagevoice.setFromUserId(mLoginUserId);
            messagevoice.setFromUserName(mLoginNickName);
            messagevoice.setFilePath(message.roomid);
            messagevoice.setObjectId(message.objectId);
            messagevoice.setTimeSend(TimeUtils.sk_time_current_time());
            // 这里还是发送单聊消息  115 || 120
            messagevoice.setToUserId(message.meetinglist.get(i));
            messagevoice.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            coreManager.sendChatMessage(message.meetinglist.get(i), messagevoice);
            // 音视频会议消息不保存
/*
            ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), message.meetinglist.get(i), messagevoice);
            FriendDao.getInstance().updateFriendContent(coreManager.getSelf().getUserId(), message.meetinglist.get(i), str, type, TimeUtils.sk_time_current_time());
*/
        }
    }

}
