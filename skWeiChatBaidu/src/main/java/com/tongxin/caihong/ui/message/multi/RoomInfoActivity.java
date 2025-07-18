package com.tongxin.caihong.ui.message.multi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.EventHideChatPasswordCreated;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Report;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.event.EventNewNotice;
import com.tongxin.caihong.bean.event.EventRemoveNotice;
import com.tongxin.caihong.bean.event.EventRoomNotice;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.TranslateHelper;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.redpacket.PayJoinRoom;
import com.tongxin.caihong.ui.message.SetupHideChatPasswordActivity;
import com.tongxin.caihong.ui.message.search.SearchChatHistoryActivity;
import com.tongxin.caihong.ui.message.single.SelectSetTypeActivity;
import com.tongxin.caihong.ui.mucfile.MucFileListActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.ui.other.QRcodeActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.CameraUtil;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.ExpandView;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.view.MsgSaveDaysDialog;
import com.tongxin.caihong.view.ReportDialog;
import com.tongxin.caihong.view.SelectImageDialog;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.SwitchButton;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.view.VerifyDialog;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.MucListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import in.srain.cube.views.GridViewWithHeaderAndFooter;
import okhttp3.Call;

/**
 * 群组信息
 */
public class RoomInfoActivity extends BaseActivity implements MucListener {
    public static final int RESULT_FOR_MODIFY_NOTICE = 5;
    private static final int RESULT_FOR_ADD_MEMBER = 1;
    private static final int REQUEST_CODE_CAPTURE_CROP_PHOTO = 4;
    private static final int REQUEST_CODE_PICK_CROP_PHOTO = 2;
    private static final int REQUEST_CODE_CROP_PHOTO = 3;
    MucRoom mucRoom;
    RefreshBroadcastReceiver receiver = new RefreshBroadcastReceiver();
    private String mRoomJid;
    private Friend mRoom;
    private Context mContext = RoomInfoActivity.this;
    // 是否从聊天界面进入
    private boolean isMucChatComing;
    private String mLoginUserId;
    private GridViewWithHeaderAndFooter mGridView;
    private GridViewAdapter mAdapter;
    private TextView mRoomNameTv;
    private TextView mRoomDescTv;
    private TextView mNoticeTv;
    private TextView mNickNameTv;
    private TextView romNameTv, romDesTv, gongGaoTv, myGroupName, shieldGroupMesTv, jinyanTv;
    private RelativeLayout room_qrcode;
    // 消息管理
    private SwitchButton mSbTopChat;
    private SwitchButton mSbDisturb;
    private SwitchButton sbHideConversation;
    private SwitchButton mSbShield;
    // 全体禁言
    private SwitchButton mSbAllShutUp;
    private Button mBtnQuitRoom;
    private ImageView mExpandIv;
    private ExpandView mExpandView;
    private TextView mCreatorTv;
    private TextView buileTimetv;
    private TextView mCreateTime;
    private TextView numberTopTv;
    private TextView mCountTv;
    private TextView mCountTv2;
    private TextView mMsgSaveDays;
    MsgSaveDaysDialog.OnMsgSaveDaysDialogClickListener onMsgSaveDaysDialogClickListener = new MsgSaveDaysDialog.OnMsgSaveDaysDialogClickListener() {
        @Override
        public void tv1Click() {
            updateChatRecordTimeOut(-1);
        }

        @Override
        public void tv2Click() {
            updateChatRecordTimeOut(0.04);
            // updateChatRecordTimeOut(0.00347); // 五分钟过期
        }

        @Override
        public void tv3Click() {
            updateChatRecordTimeOut(1);
        }

        @Override
        public void tv4Click() {
            updateChatRecordTimeOut(7);
        }

        @Override
        public void tv5Click() {
            updateChatRecordTimeOut(30);
        }

        @Override
        public void tv6Click() {
            updateChatRecordTimeOut(90);
        }

        @Override
        public void tv7Click() {
            updateChatRecordTimeOut(365);
        }
    };
    private Uri mNewPhotoUri;
    private File mCurrentFile;
    private TextView tvMemberLimit;
    // 消息管理 && 全体禁言
    SwitchButton.OnCheckedChangeListener onCheckedChangeMessageListener = new SwitchButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(SwitchButton view, boolean isChecked) {
            switch (view.getId()) {
                case R.id.sb_top_chat:// 置顶聊天
                    updateDisturbState(1, isChecked ? 1 : 0);
                    break;
                case R.id.sb_no_disturb:// 消息免打扰
                    updateDisturbState(0, isChecked ? 1 : 0);
                    break;
                case R.id.sbHideConversation:
                    // 隐藏会话
                    updateHideChatSwitchStatus(3, isChecked);
                    break;
                case R.id.sb_shield_chat:// 屏蔽群消息
                    if (isChecked) {
                        if (mRoom.getOfflineNoPushMsg() == 0) {
                            mSbDisturb.setChecked(true);
                        }
                    }
                    PreferenceUtils.putBoolean(mContext, Constants.SHIELD_GROUP_MSG + mRoomJid + mLoginUserId, isChecked);
                    mSbShield.setChecked(isChecked);
                    MessageUtil.generateTipMessage(mLoginUserId
                            , mRoomJid
                            , isChecked ? getString(R.string.you_already_shield_group_msg) : getString(R.string.you_already_receive_group_msg)
                            , true);
                    break;
                case R.id.sb_banned:// 全体禁言
                    if (isChecked) {
                        updateSingleAttribute("talkTime", String.valueOf(TimeUtils.sk_time_current_time() + 24 * 60 * 60 * 15));
                    } else {
                        updateSingleAttribute("talkTime", String.valueOf(0));
                    }
                    break;
            }
        }
    };
    private int add_minus_count = 2;
    private int role;
    // 跳转至邀请群成员界面需要的参数
    private String creator;  // 群主id
    private int isNeedVerify;// 是否开启进群验证
    // 展开与收起群成员列表
    private LinearLayout llOp;
    private ImageView mOpenMembers;
    // false表示折叠状态，
    private boolean flag;
    private List<MucRoomMember> mMembers;
    private List<MucRoomMember> mCurrentMembers = new ArrayList<>();
    private MucRoomMember mGroupOwner;// 群主
    private MucRoomMember myself;// 自己
    private List<MucRoomMember> mAdministrators = new ArrayList<>();// 群管理员
    private Map<String, String> mRemarksMap = new HashMap<>();
    private View header;
    private View footer;
    private int mMemberSize;

    /**
     * 群成员分页
     * 保存当前群组该页的最后一个成员入群时间 MucRoomMember
     */
    public static void saveMucLastRoamingTime(String ownerId, String roomId, long time, boolean reset) {
        if (reset) {
            PreferenceUtils.putLong(MyApplication.getContext(), Constants.MUC_MEMBER_LAST_JOIN_TIME + ownerId + roomId, time);
        } else {
            long lastRoamingTime = PreferenceUtils.getLong(MyApplication.getContext(), Constants.MUC_MEMBER_LAST_JOIN_TIME + ownerId + roomId, 0);
            if (lastRoamingTime < time) {
                PreferenceUtils.putLong(MyApplication.getContext(), Constants.MUC_MEMBER_LAST_JOIN_TIME + ownerId + roomId, time);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_info);
        if (getIntent() != null) {
            mRoomJid = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            isMucChatComing = getIntent().getBooleanExtra(AppConstant.EXTRA_IS_GROUP_CHAT, false);
        }
        if (TextUtils.isEmpty(mRoomJid)) {
            LogUtils.log(getIntent());
            Reporter.post("传入的RoomJid为空，");
            Toast.makeText(this, R.string.tip_group_message_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mLoginUserId = coreManager.getSelf().getUserId();
        mRoom = FriendDao.getInstance().getFriend(mLoginUserId, mRoomJid);
        if (mRoom == null || TextUtils.isEmpty(mRoom.getRoomId())) {
            LogUtils.log(getIntent());
            LogUtils.log("mLoginUserId = " + mLoginUserId);
            LogUtils.log("mRoomJid = " + mRoomJid);
            // 没有toString方法，暂且转json，不能被混淆，
            LogUtils.log("mRoom = " + JSON.toJSONString(mRoom));
            Reporter.post("传入的RoomJid找不到Room，");
            Toast.makeText(this, R.string.tip_group_message_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initActionBar();
        initView();
        registerRefreshReceiver();
        loadMembers();
        initEvent();
        EventBus.getDefault().register(this);
        ListenerManager.getInstance().addMucListener(this);
    }

    @Override
    protected void onDestroy() {
        ListenerManager.getInstance().removeMucListener(this);
        super.onDestroy();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                // onCreate异常时可能没走到绑定Receiver,
                // 无论如何都不应该在destroy时崩溃，
                // 重复上报，可以加个boolean判断避免，无所谓了，
                Reporter.post("解绑Receiver异常，", e);
            }
        }
        // 这个没注册时取消注册也不会崩溃，
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTitleTv = (TextView) findViewById(R.id.tv_title_center);
        mTitleTv.setText(getString(R.string.room_info));
    }

    private void initView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Friend> mFriendList = FriendDao.getInstance().getAllFriendsNotFilterStatus(mLoginUserId);
                for (int i = 0; i < mFriendList.size(); i++) {
                    if (!TextUtils.isEmpty(mFriendList.get(i).getRemarkName())) {// 针对该好友进行了备注
                        mRemarksMap.put(mFriendList.get(i).getUserId(), mFriendList.get(i).getRemarkName());
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }).start();

        mGridView = findViewById(R.id.grid_view);
        // 很多控件都在footer里，
        header = getLayoutInflater().inflate(R.layout.activity_room_info_header, null);
        footer = getLayoutInflater().inflate(R.layout.activity_room_info_footer, null);
        mGridView.addHeaderView(header);
        mGridView.addFooterView(footer);
        // 先加载个初始界面避免白屏，
        mAdapter = new GridViewAdapter();
        mGridView.setAdapter(mAdapter);
        SwitchButton sbAutoTranslate = (SwitchButton) footer.findViewById(R.id.sb_auto_translate);
        sbAutoTranslate.setChecked(TranslateHelper.isAutoTranslate(mContext, coreManager.getSelf().getUserId(), mRoomJid));
        sbAutoTranslate.setOnCheckedChangeListener((view, isChecked) -> {
            TranslateHelper.setAutoTranslate(mContext, coreManager.getSelf().getUserId(), mRoomJid, isChecked);
        });
        llOp = (LinearLayout) footer.findViewById(R.id.ll_op);
        mOpenMembers = (ImageView) footer.findViewById(R.id.open_members);
        mRoomNameTv = (TextView) footer.findViewById(R.id.room_name_tv);
        mRoomDescTv = (TextView) footer.findViewById(R.id.room_desc_tv);
        mNoticeTv = (TextView) footer.findViewById(R.id.notice_tv);
        mNickNameTv = (TextView) footer.findViewById(R.id.nick_name_tv);
        room_qrcode = (RelativeLayout) footer.findViewById(R.id.room_qrcode);
        //room_qrcode.setVisibility(View.GONE);

        mSbTopChat = (SwitchButton) footer.findViewById(R.id.sb_top_chat);
        mSbDisturb = (SwitchButton) footer.findViewById(R.id.sb_no_disturb);
        sbHideConversation = footer.findViewById(R.id.sbHideConversation);
        mSbShield = (SwitchButton) footer.findViewById(R.id.sb_shield_chat);

        mSbAllShutUp = (SwitchButton) footer.findViewById(R.id.sb_banned);

        gongGaoTv = (TextView) footer.findViewById(R.id.notice_text);
        romNameTv = (TextView) footer.findViewById(R.id.room_name_text);
        romDesTv = (TextView) footer.findViewById(R.id.room_desc_text);
        myGroupName = (TextView) footer.findViewById(R.id.nick_name_text);
        shieldGroupMesTv = (TextView) footer.findViewById(R.id.shield_chat_text_title);
        jinyanTv = (TextView) footer.findViewById(R.id.banned_voice_text);
        gongGaoTv.setText(getString(R.string.notice));
        romNameTv.setText(getString(R.string.room_name));
        /*romDesTv.setText(getString("JX_RoomExplain"));*/
        myGroupName.setText(getString(R.string.my_nick_name));
        shieldGroupMesTv.setText(getString(R.string.shield_chat));
        jinyanTv.setText(getString(R.string.ban));
      /*  TextView qrCode = (TextView) footer.findViewById(R.id.qr_code_tv);
        qrCode.setText(getString("JXQR_QRImage"));*/
        TextView mGroupFile = (TextView) footer.findViewById(R.id.tv_file_name);
        mGroupFile.setText(getString(R.string.ShareFile));
       /* TextView isGroupReadTv = (TextView) footer.findViewById(R.id.iskaiqiqun);
        isGroupReadTv.setText(getString("JX_RoomShowRead"));*/

        mBtnQuitRoom = (Button) footer.findViewById(R.id.room_info_quit_btn);
        ViewCompat.setBackgroundTintList(mBtnQuitRoom, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        mBtnQuitRoom.setText(getString(R.string.OutPut_Room));

        // ExpandView And His Sons
        mExpandIv = (ImageView) footer.findViewById(R.id.room_info_iv);
        mExpandView = (ExpandView) footer.findViewById(R.id.expandView);
        mExpandView.setContentView(R.layout.layout_expand);
        mCreatorTv = (TextView) footer.findViewById(R.id.creator_tv);
        buileTimetv = (TextView) footer.findViewById(R.id.create_time_text);
        buileTimetv.setText(getString(R.string.creat_time));
        mCreateTime = (TextView) footer.findViewById(R.id.create_timer);
        numberTopTv = (TextView) footer.findViewById(R.id.count_text);
        numberTopTv.setText(getString(R.string.people_count));
        mCountTv = (TextView) footer.findViewById(R.id.count_tv);
        mCountTv2 = (TextView) header.findViewById(R.id.member_count_tv);
        mMsgSaveDays = (TextView) footer.findViewById(R.id.msg_save_days_tv);

        // 获取群组数据需要一定时间，我们先从朋友表内获取部分数据赋值，待服务器返回数据后在刷新ui
        mRoomNameTv.setText(mRoom.getNickName());
        mRoomDescTv.setText(mRoom.getDescription());

        mNickNameTv.setText(mRoom.getRoomMyNickName() != null
                ? mRoom.getRoomMyNickName() : coreManager.getSelf().getNickName());

        mSbDisturb.setChecked(mRoom.getOfflineNoPushMsg() == 1);// 消息免打扰
        // 隐藏会话
        sbHideConversation.setChecked(mRoom.getHideChatSwitch() == 1);
        mMsgSaveDays.setText(conversion(mRoom.getChatRecordTimeOut()));// 消息保存天数

        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mRoom.getUserId(), false);
        mSbAllShutUp.setChecked(isAllShutUp);

        tvMemberLimit = footer.findViewById(R.id.member_limit_tv);

        AvatarHelper.getInstance().updateAvatar(mRoom.getRoomId());// 更新时间
    }

    /**
     * 所有角色都拥有的权限
     */
    private void initEvent() {
        footer.findViewById(R.id.room_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mExpandView.isExpand()) {
                    mExpandView.collapse();
                    mExpandIv.setBackgroundResource(R.drawable.open_member);
                } else {
                    mExpandView.expand();
                    mExpandIv.setBackgroundResource(R.drawable.close_member);
                }
            }
        });

        // 二维码
        if (mRoom.getIsSecretGroup() == 1) {// 私密群组不允许主动加入
            room_qrcode.setVisibility(View.GONE);
        }
        room_qrcode.setOnClickListener(v -> {
            Intent intent = new Intent(RoomInfoActivity.this, QRcodeActivity.class);
            intent.putExtra("isgroup", true);
            intent.putExtra("userid", mRoom.getRoomId());
            intent.putExtra("nickName", mRoom.getNickName());
            intent.putExtra("roomJid", mRoom.getUserId());
            startActivity(intent);
        });

        // 公告
        footer.findViewById(R.id.notice_rl).setOnClickListener(v -> {
            if (mucRoom != null) {
                NoticeListActivity.start(RoomInfoActivity.this, myself.getRole(), mRoom.getRoomId());
            }
        });

        // 修改群内昵称
        footer.findViewById(R.id.nick_name_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mucRoom.getAllowModifyCard() == 0 && myself.getRole() != 1 && myself.getRole() != 2) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_room_current_disallow_modify_card));
                    return;
                }
                if (myself.disallowPublicAction()) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_action_disallow_place_holder, getString(myself.getRoleName())));
                    return;
                }
                showChangeNickNameDialog(mNickNameTv.getText().toString().trim());
            }
        });

        // 修改聊天背景
        footer.findViewById(R.id.set_background_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentBackground = new Intent(mContext, SelectSetTypeActivity.class);
                intentBackground.putExtra(AppConstant.EXTRA_USER_ID, mRoom.getUserId());
                startActivity(intentBackground);
            }
        });

        // 修改群头像
        footer.findViewById(R.id.picture_rl).setVisibility(View.VISIBLE);
        footer.findViewById(R.id.picture_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePictureDialog();
            }
        });

        // 群共享文件
        footer.findViewById(R.id.file_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myself != null && mucRoom != null) {
                    Intent intent = new Intent(RoomInfoActivity.this, MucFileListActivity.class);
                    intent.putExtra("roomId", mRoom.getRoomId());
                    intent.putExtra("role", myself.getRole());
                    intent.putExtra("allowUploadFile", mucRoom.getAllowUploadFile());
                    startActivity(intent);
                }
            }
        });

        // 群内十分钟后未领完的红包
        footer.findViewById(R.id.rlNotReceivedRed).setOnClickListener(v -> RoomNotReceivedRedActivity.start(mContext, mRoomJid));

        // 查找聊天记录
        footer.findViewById(R.id.chat_history_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoomInfoActivity.this, SearchChatHistoryActivity.class);
                intent.putExtra("isSearchSingle", false);
                intent.putExtra(AppConstant.EXTRA_USER_ID, mRoomJid);
                startActivity(intent);
            }
        });

        mSbTopChat.setOnCheckedChangeListener(onCheckedChangeMessageListener);
        mSbDisturb.setOnCheckedChangeListener(onCheckedChangeMessageListener);
        sbHideConversation.setOnCheckedChangeListener(onCheckedChangeMessageListener);
        mSbShield.setOnCheckedChangeListener(onCheckedChangeMessageListener);

        // 清空聊天记录
        footer.findViewById(R.id.chat_history_empty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(null, getString(R.string.tip_confirm_clean_history_group2), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        updateCleanMsgTime();
                    }
                });
                selectionFrame.show();
            }
        });

        footer.findViewById(R.id.report_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReportDialog mReportDialog = new ReportDialog(RoomInfoActivity.this, true, new ReportDialog.OnReportListItemClickListener() {
                    @Override
                    public void onReportItemClick(Report report) {
                        report(mRoom.getRoomId(), report);
                    }
                });
                mReportDialog.show();
            }
        });

        // 退出群组
        mBtnQuitRoom.setOnClickListener(v -> {
            if (mucRoom == null) {
                return;
            }

            String desc;
            String url;
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("roomId", mRoom.getRoomId());
            if (mucRoom.getUserId().equals(mLoginUserId)) {// 解散群组
                desc = getString(R.string.tip_disband);
                url = coreManager.getConfig().ROOM_DELETE;
            } else {// 退出群组
                params.put("userId", mLoginUserId);
                desc = getString(R.string.tip_exit);
                url = coreManager.getConfig().ROOM_MEMBER_DELETE;
            }
            quitRoom(desc, url, params);
        });

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (add_minus_count == 1) {
                    // 现在添加了群组成员折叠功能,+ -号都是一直存在的，所以需要修改下逻辑，不过add_minus_count可用做于判断权限
                    if (position == mCurrentMembers.size() - 2) {
                        if (myself.disallowInvite()) {
                            tip(getString(R.string.tip_disallow_invite_role_place_holder, getString(myself.getRoleName())));
                        } else if (mucRoom.getAllowInviteFriend() == 1 || myself.getRole() == 1 || myself.getRole() == 2) {
                            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mucRoom.getJid());
                            if (friend.getIsLostChatKeyGroup() == 1) {
                                tip(getString(R.string.please_get_chat_key_to_invite));
                                return;
                            }
                            List<String> existIds = new ArrayList<>();
                            List<RoomMember> members = RoomMemberDao.getInstance().getRoomMember(mRoom.getRoomId());
                            for (int i = 0; i < members.size(); i++) {
                                existIds.add(members.get(i).getUserId());
                            }
                            // 邀请
                            Intent intent = new Intent(RoomInfoActivity.this, AddContactsActivity.class);
                            intent.putExtra("roomId", mRoom.getRoomId());
                            intent.putExtra("roomJid", mRoomJid);
                            intent.putExtra("roomName", mRoomNameTv.getText().toString());
                            intent.putExtra("roomDes", mRoomDescTv.getText().toString());
                            intent.putExtra("exist_ids", JSON.toJSONString(existIds));
                            intent.putExtra("roomCreator", creator);
                            startActivityForResult(intent, RESULT_FOR_ADD_MEMBER);
                        } else {
                            tip(getString(R.string.tip_disable_invite));
                        }
                    } else if (position == mCurrentMembers.size() - 1) {
                        // 群主或管理员才有权限操作
                        Toast.makeText(RoomInfoActivity.this, getString(R.string.not_admin_cannot_do_this), Toast.LENGTH_SHORT).show();
                    } else {
                        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mRoom.getUserId(), true);
                        if (isAllowSecretlyChat) {
                            MucRoomMember member = mCurrentMembers.get(position);
                            if (member != null) {
                                startBasicInfo(member.getUserId());
                            }
                        } else {
                            tip(getString(R.string.tip_member_disable_privately_chat));
                        }
                    }
                } else if (add_minus_count == 2) {// 群主与管理员
                    if (position == mCurrentMembers.size() - 2) {
                        Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mucRoom.getJid());
                        if (friend.getIsLostChatKeyGroup() == 1) {
                            tip(getString(R.string.please_get_chat_key_to_invite));
                            return;
                        }
                        List<String> existIds = new ArrayList<>();
                        List<RoomMember> members = RoomMemberDao.getInstance().getRoomMember(mRoom.getRoomId());
                        for (int i = 0; i < members.size(); i++) {
                            existIds.add(members.get(i).getUserId());
                        }
                        // 邀请
                        Intent intent = new Intent(RoomInfoActivity.this, AddContactsActivity.class);
                        intent.putExtra("roomId", mRoom.getRoomId());
                        intent.putExtra("roomJid", mRoomJid);
                        intent.putExtra("roomName", mRoomNameTv.getText().toString());
                        intent.putExtra("roomDes", mRoomDescTv.getText().toString());
                        intent.putExtra("exist_ids", JSON.toJSONString(existIds));
                        intent.putExtra("roomCreator", creator);
                        startActivityForResult(intent, RESULT_FOR_ADD_MEMBER);
                    } else if (position == mCurrentMembers.size() - 1) {
                        Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
                        intent.putExtra("roomId", mucRoom.getId());
                        intent.putExtra("isDelete", true);
                        startActivity(intent);
                    } else {
                        MucRoomMember member = mCurrentMembers.get(position);
                        if (member != null) {
                            startBasicInfo(member.getUserId());
                        }
                    }
                }
            }
        });
    }

    private void startBasicInfo(String userId) {
        BasicInfoActivity.start(mContext, userId, BasicInfoActivity.FROM_ADD_TYPE_GROUP
                , (myself != null && (myself.getRole() == 1 || myself.getRole() == 2))
                , mRoom.getRoomId());
    }

    private void loadMembers() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("pageSize", Constants.MUC_MEMBER_PAGE_SIZE);

        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {
                                 DialogHelper.dismissProgressDialog();
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     mucRoom = result.getData();
                                     myself = mucRoom.getMember();
                                     tvMemberLimit.setText(String.valueOf(mucRoom.getMaxUserSize()));

                                     MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(), mucRoom.getAllowSendCard(),
                                             mucRoom.getAllowConference(), mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime(), mucRoom.getAllowOpenLive());
                                     FriendDao.getInstance().updateRoomCreateUserId(mLoginUserId, mRoom.getUserId(), mucRoom.getUserId());
                                     PreferenceUtils.putBoolean(MyApplication.getContext(),
                                             Constants.IS_NEED_OWNER_ALLOW_NORMAL_INVITE_FRIEND + mucRoom.getJid(), mucRoom.getIsNeedVerify() == 1);
                                     PreferenceUtils.putBoolean(MyApplication.getContext(),
                                             Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mucRoom.getJid(), mucRoom.getAllowUploadFile() == 1);

                                     AsyncUtils.doAsync(this, (AsyncUtils.Function<AsyncUtils.AsyncContext<BaseCallback<MucRoom>>>) baseCallbackAsyncContext -> {
                                         for (int i = 0; i < mucRoom.getMembers().size(); i++) {// 在异步任务内存储
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
                                             RoomMemberDao.getInstance().saveSingleRoomMember(mucRoom.getId(), roomMember);
                                         }
                                     });

                                     // SecureFlagGroup
                                     FriendDao.getInstance().updateEncryptType(mRoom.getUserId(), mucRoom.getEncryptType());
                                     if (myself != null && mucRoom.getIsSecretGroup() == 1) {
                                         try {
                                             String chatKey = new String(RSA.decryptFromBase64(myself.getChatKeyGroup(), Base64.decode(SecureChatUtil.getRSAPrivateKey(mLoginUserId))));
                                             FriendDao.getInstance().updateChatKeyGroup(mucRoom.getJid(), SecureChatUtil.encryptChatKey(mucRoom.getJid(), chatKey));
                                             Log.e("msg", "设置chatKey成功-->" + chatKey);
                                         } catch (Exception e) {
                                             Log.e("msg", "设置chatKey失败");
                                             FriendDao.getInstance().updateIsLostChatKeyGroup(mucRoom.getJid(), 1);
                                         }
                                     }

                                     if (!mucRoom.getMembers().isEmpty()) {
                                         saveMucLastRoamingTime(mLoginUserId, mucRoom.getId(), mucRoom.getMembers().get(mucRoom.getMembers().size() - 1).getCreateTime(), false);
                                     }

                                     // 更新消息界面
                                     MsgBroadcast.broadcastMsgUiUpdate(RoomInfoActivity.this);
                                     // 更新群聊界面
                                     MucgroupUpdateUtil.broadcastUpdateUi(RoomInfoActivity.this);
                                     // 更新ui
                                     updateUI(result.getData());
                                 } else {
                                     ToastUtil.showErrorData(RoomInfoActivity.this);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 DialogHelper.dismissProgressDialog();
                                 ToastUtil.showErrorNet(RoomInfoActivity.this);
                             }
                         }
                );
    }

    private void updateUI(final MucRoom mucRoom) {
        if (!TextUtils.equals(mRoom.getNickName(), mucRoom.getName())) {
            // 群名不对情况手动同步，
            FriendDao.getInstance().updateMucFriendRoomName(mRoom.getUserId(), mucRoom.getName());
            ListenerManager.getInstance().notifyNickNameChanged(mRoom.getUserId(), "ROOMNAMECHANGE", mucRoom.getName());
        }
        mMemberSize = mucRoom.getUserSize();
        mMembers = mucRoom.getMembers();

        creator = mucRoom.getUserId();
        isNeedVerify = mucRoom.getIsNeedVerify();

        if (mMembers != null) {
            mAdministrators.clear();
            for (int i = 0; i < mMembers.size(); i++) {
                String userId = mMembers.get(i).getUserId();
                if (mucRoom.getUserId().equals(userId)) {
                    mGroupOwner = mMembers.get(i);
                }
                if (mMembers.get(i).getRole() == 2) {
                    mAdministrators.add(mMembers.get(i));
                }
            }

            // 将群主移动到第一个的位置
            if (mGroupOwner != null) {
                mMembers.remove(mGroupOwner);
                mMembers.add(0, mGroupOwner);
            }
        }

        if (myself == null) {
            ToastUtil.showToast(mContext, R.string.tip_kick_room);
            finish();
            return;
        }

        mAdapter = new GridViewAdapter();
        mGridView.setAdapter(mAdapter);

        mRoomNameTv.setText(mucRoom.getName());
        mRoomDescTv.setText(mucRoom.getDesc());

        mCreatorTv.setText(mucRoom.getNickName());
        mCreateTime.setText(TimeUtils.s_long_2_str(mucRoom.getCreateTime() * 1000));
        mCountTv.setText(mucRoom.getUserSize() + "/" + mucRoom.getMaxUserSize());
        mCountTv2.setText(getString(R.string.total_count_place_holder, mucRoom.getUserSize()));

        List<MucRoom.Notice> notices = mucRoom.getNotices();
        if (notices != null && !notices.isEmpty()) {
            String text = getLastNoticeText(notices);
            mNoticeTv.setText(text);
            EventBus.getDefault().post(new EventRoomNotice(text));
        } else {
            mNoticeTv.setText(getString(R.string.no_notice));
        }
        mNickNameTv.setText(myself.getNickName());

        // 更新消息免打扰、置顶状态
        mRoom.setOfflineNoPushMsg(myself.getOfflineNoPushMsg());
        FriendDao.getInstance().updateOfflineNoPushMsgStatus(mRoom.getUserId(), myself.getOfflineNoPushMsg());
        mRoom.setTopTime(myself.getOpenTopChatTime());
        if (myself.getOpenTopChatTime() > 0) {
            FriendDao.getInstance().updateTopFriend(mRoom.getUserId(), myself.getOpenTopChatTime());
        } else {
            FriendDao.getInstance().resetTopFriend(mRoom.getUserId());
        }

        // 更新消息管理状态
        updateMessageStatus();

        // 更新消息保存天数
        mMsgSaveDays.setText(conversion(mucRoom.getChatRecordTimeOut()));
        FriendDao.getInstance().updateChatRecordTimeOut(mRoom.getUserId(), mucRoom.getChatRecordTimeOut());

        // 根据我在该群职位显示UI界面
        role = myself.getRole();
        /*for (int i = 0; i < mMembers.size(); i++) {
            if (mMembers.get(i).getUserId().equals(mLoginUserId)) {
                role = mMembers.get(i).getRole();
            }
        }*/

        if (role == 1 || role == 2) {
            footer.findViewById(R.id.vNotReceivedRed).setVisibility(View.VISIBLE);
            // footer.findViewById(R.id.rlNotReceivedRed).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.rlNotReceivedRed).setVisibility(View.GONE);
        } else {
            footer.findViewById(R.id.vNotReceivedRed).setVisibility(View.GONE);
            footer.findViewById(R.id.rlNotReceivedRed).setVisibility(View.GONE);
        }

        if (role == 1) {// 群创建者，开放所有权限
            footer.findViewById(R.id.set_remarks_rl).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.set_remarks_rl).setOnClickListener(v -> {
                Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
                intent.putExtra("roomId", mucRoom.getId());
                intent.putExtra("isSetRemark", true);
                startActivity(intent);
            });
            mBtnQuitRoom.setText(getString(R.string.dissolution_group));
            footer.findViewById(R.id.room_name_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangeRoomNameDialog(mRoomNameTv.getText().toString().trim());
                }
            });
            // 修改群头像
            footer.findViewById(R.id.picture_rl).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.picture_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangePictureDialog();
                }
            });
            footer.findViewById(R.id.room_desc_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangeRoomDesDialog(mRoomDescTv.getText().toString().trim());
                }
            });

            footer.findViewById(R.id.msg_save_days_rl).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.msg_save_days_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MsgSaveDaysDialog msgSaveDaysDialog = new MsgSaveDaysDialog(RoomInfoActivity.this, onMsgSaveDaysDialogClickListener);
                    msgSaveDaysDialog.show();
                }
            });

            footer.findViewById(R.id.banned_voice_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
                    intent.putExtra("roomId", mucRoom.getId());
                    intent.putExtra("isBanned", true);
                    startActivity(intent);
                }
            });

            footer.findViewById(R.id.rl_manager).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.rl_manager).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int status_lists[] = {mucRoom.getShowRead(), mucRoom.getIsLook(), mucRoom.getIsNeedVerify(),
                            mucRoom.getShowMember(), mucRoom.getAllowSendCard(),
                            mucRoom.getAllowInviteFriend(), mucRoom.getAllowUploadFile(),
                            mucRoom.getAllowConference(), mucRoom.getAllowSpeakCourse(),
                            mucRoom.getIsAttritionNotice(), mucRoom.getAllowOpenLive(),
                            mucRoom.getShowMarker(), mucRoom.getAllowModifyCard()};
                    Intent intent = new Intent(mContext, GroupManager.class);
                    intent.putExtra("roomId", mucRoom.getId());
                    intent.putExtra("roomJid", mucRoom.getJid());
                    intent.putExtra("roomRole", myself.getRole());
                    intent.putExtra("GROUP_STATUS_LIST", status_lists);
                    intent.putExtra("copy_name", mucRoom.getName());
                    intent.putExtra("copy_size", mucRoom.getUserSize());
                    intent.putExtra("isSecretGroup", mucRoom.getIsSecretGroup());
                    intent.putExtra("needPay", mucRoom.getNeedPay());
                    startActivity(intent);
                }
            });
            footer.findViewById(R.id.rl_black).setOnClickListener(v -> BlackListActivity.start(mContext, mucRoom.getId()));


            mSbAllShutUp.setOnCheckedChangeListener(onCheckedChangeMessageListener);

            enableGroupMore(mucRoom);

            updateMemberLimit(true);
        } else if (role == 2) {// 管理员，开放部分权限
            footer.findViewById(R.id.set_remarks_rl).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.set_remarks_rl).setOnClickListener(v -> {
                Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
                intent.putExtra("roomId", mucRoom.getId());
                intent.putExtra("isSetRemark", true);
                startActivity(intent);
            });
            mBtnQuitRoom.setText(getString(R.string.OutPut_Room));
            footer.findViewById(R.id.room_name_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangeRoomNameDialog(mRoomNameTv.getText().toString().trim());
                }
            });
            // 修改群头像
            footer.findViewById(R.id.picture_rl).setVisibility(View.VISIBLE);
            footer.findViewById(R.id.picture_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangePictureDialog();
                }
            });
            footer.findViewById(R.id.room_desc_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangeRoomDesDialog(mRoomDescTv.getText().toString().trim());
                }
            });

            footer.findViewById(R.id.banned_voice_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
                    intent.putExtra("roomId", mucRoom.getId());
                    intent.putExtra("isBanned", true);
                    startActivity(intent);
                }
            });
            footer.findViewById(R.id.rl_black).setOnClickListener(v -> BlackListActivity.start(mContext, mucRoom.getId()));
            mSbAllShutUp.setOnCheckedChangeListener(onCheckedChangeMessageListener);

            enableGroupMore(mucRoom);

            updateMemberLimit(true);
        } else {
            add_minus_count = 1;
            mBtnQuitRoom.setText(getString(R.string.OutPut_Room));
            footer.findViewById(R.id.room_name_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tip(getString(R.string.tip_cannot_change_name));
                }
            });
            // 修改群头像
            footer.findViewById(R.id.picture_rl).setVisibility(View.GONE);
            footer.findViewById(R.id.room_desc_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogHelper.tipDialog(mContext, mRoomDescTv.getText().toString().trim());
                }
            });

            // 隐藏 禁言 与 全体禁言
            footer.findViewById(R.id.banned_voice_rl).setVisibility(View.GONE);
            footer.findViewById(R.id.banned_all_voice_rl).setVisibility(View.GONE);
            footer.findViewById(R.id.rl_black).setVisibility(View.GONE);

            footer.findViewById(R.id.msg_save_days_rl).setVisibility(View.GONE);
            footer.findViewById(R.id.rl_manager).setVisibility(View.GONE);

            boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mRoom.getUserId(), true);
            if (isAllowSecretlyChat) {
                enableGroupMore(mucRoom);
            }
            updateMemberLimit(false);
            //续费按钮只有收费群聊才能显示
            if (mucRoom.getNeedPay()==1){
                footer.findViewById(R.id.recharge_rl).setVisibility(View.VISIBLE);
            }else{
                footer.findViewById(R.id.recharge_rl).setVisibility(View.GONE);
            }
            footer.findViewById(R.id.recharge_rl).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PayJoinRoom.start(RoomInfoActivity.this,mucRoom.getId(),mucRoom.getPayForDays(),mucRoom.getPayForAmount());
                }
            });
        }

        // 现在添加群组成员折叠功能，让+ -号一直存在吧
        mMembers.add(null);// 一个+号
        mMembers.add(null);// 一个-号

        mCurrentMembers.clear();
        if (mucRoom.getShowMember() == 0 && role != 1 && role != 2) {// 群主已关闭 显示群成员列表功能 (群主与管理员可见) 普通成员只显示自己与+ -
            header.findViewById(R.id.ll_all_member).setVisibility(View.GONE);
            llOp.setVisibility(View.GONE);
            mCurrentMembers.add(mGroupOwner);
            mCurrentMembers.addAll(mAdministrators);
            mCurrentMembers.add(myself);
            mCurrentMembers.add(null);// +
            mCurrentMembers.add(null);// _
        } else {// 正常加载
            header.findViewById(R.id.ll_all_member).setVisibility(View.VISIBLE);
            // 减去+-两个按钮，
            if (mMembers.size() - 2 > getDefaultCount()) {
                // 可以折叠
                llOp.setVisibility(View.VISIBLE);
                // 折叠显示，
                // 确保群成员变化后刷新群成员列表默认折叠时flag没有错，
                flag = false;
                mOpenMembers.setImageResource(R.drawable.open_member);
                minimalMembers();
            } else {
                // 不可折叠，全部显示
                llOp.setVisibility(View.GONE);
                mCurrentMembers.addAll(mMembers);
            }

            llOp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    op();
                }
            });
        }
    }

    /**
     * @param isGroupManager 群主或群管理员，
     */
    private void updateMemberLimit(boolean isGroupManager) {
        View rlMemberLimit = footer.findViewById(R.id.member_limit_rl);
        View v_member_limit_rl = footer.findViewById(R.id.v_member_limit_rl);
        if (isGroupManager && coreManager.getSelf().isSuperManager()) {
            v_member_limit_rl.setVisibility(View.VISIBLE);
            rlMemberLimit.setVisibility(View.VISIBLE);
            rlMemberLimit.setOnClickListener(v -> {
                VerifyDialog verifyDialog = new VerifyDialog(this);
                verifyDialog.setVerifyClickListener(getString(R.string.Maximum_number_of_people)
                        , getString(R.string.Set_the_maximum_number)
                        , ""
                        , new VerifyDialog.VerifyClickListener() {
                            @Override
                            public void cancel() {

                            }

                            @Override
                            public void send(String str) {
                                if (TextUtils.isDigitsOnly(str)) {
                                    updateSingleAttribute("maxUserSize", str);
                                } else {
                                    Reporter.unreachable();
                                    ToastUtil.showToast(RoomInfoActivity.this, getString(R.string.the_number_format_is_incorrect));
                                }
                            }
                        });
                verifyDialog.setOkButton(R.string.sure);
                verifyDialog.show();
            });
        } else {
            rlMemberLimit.setVisibility(View.GONE);
            v_member_limit_rl.setVisibility(View.GONE);
        }
    }

    /**
     * 允许点击群人数进入群成员更多操作的页面，
     */
    private void enableGroupMore(MucRoom mucRoom) {
        header.findViewById(R.id.ll_all_member).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, GroupMoreFeaturesActivity.class);
                intent.putExtra("roomId", mucRoom.getId());
                intent.putExtra("isLoadByService", true);
                startActivity(intent);
            }
        });
    }

    private void minimalMembers() {
        int count = getDefaultCount();
        for (int i = 0; i < Math.min(count, mMembers.size() - 2); i++) {
            mCurrentMembers.add(mMembers.get(i));
        }
        mCurrentMembers.add(null);
        mCurrentMembers.add(null);
    }

    private int getDefaultCount() {
        return mGridView.getNumColumns() * 3 - 2;
    }

    /**
     * 调用该方法的情况，
     * mMembers.size > getDefaultCount() + 2
     * mMembers.size包括了+-两个按钮，
     */
    public void op() {
        Log.e("RoomInfoActivity", System.currentTimeMillis() + "start");
        flag = !flag;
        mCurrentMembers.clear();
        if (flag) {
            // 展开
            mCurrentMembers.addAll(mMembers);
            mAdapter.notifyDataSetChanged();
            mOpenMembers.setImageResource(R.drawable.close_member);
        } else {
            // 收起
            minimalMembers();
            mAdapter.notifyDataSetChanged();
            scrollToTop();
            mOpenMembers.setImageResource(R.drawable.open_member);
        }
        Log.e("RoomInfoActivity", System.currentTimeMillis() + "end");
    }

    public void tip(String tip) {
        ToastUtil.showToast(RoomInfoActivity.this, tip);
    }

    private String getLastNoticeText(List<MucRoom.Notice> notices) {
        MucRoom.Notice notice = new MucRoom.Notice();
        notice.setTime(0);
        for (MucRoom.Notice no : notices) {
            if (no.getTime() > notice.getTime())
                notice = no;
        }
        return notice.getText();
    }

    // 修改群组名称
    private void showChangeRoomNameDialog(final String roomName) {
        DialogHelper.showLimitSingleInputDialog(this
                , getString(R.string.update_roomname)
                , roomName, roomName
                , 100
                , text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastUtil.showToast(mContext, getString(R.string.room_name_empty_error));
                        return false;
                    }
                    if (TextUtils.equals(roomName, text)) {
                        return true;
                    }
                    updateRoom(text, null);
                    return true;
                });
    }

    // 修改群组描述
    private void showChangeRoomDesDialog(final String roomDes) {
        DialogHelper.showLimitSingleInputDialog(this
                , getString(R.string.update_explain)
                , roomDes, roomDes
                , 100
                , text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastUtil.showToast(mContext, getString(R.string.room_des_empty_error));
                        return false;
                    }
                    if (TextUtils.equals(roomDes, text)) {
                        return true;
                    }
                    updateRoom(null, text);
                    return true;
                });
    }

    // 修改群头像
    private void showChangePictureDialog() {
        SelectImageDialog selectImageDialog = new SelectImageDialog(this, new SelectImageDialog.OnSelectImageItemListener() {
            @Override
            public void tvTakePhotoClick() {
                takePhoto();
            }

            @Override
            public void tvSelectPhotoClick() {
                selectPhoto();
            }
        });
        selectImageDialog.show();
/*
        String[] items = new String[]{getString(R.string.c_take_picture), getString(R.string.album)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(getString(R.string.select_avatars))
                .setSingleChoiceItems(items, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    takePhoto();
                                } else {
                                    selectPhoto();
                                }
                                dialog.dismiss();
                            }
                        });
        builder.show();
*/
    }

    private void takePhoto() {
        mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_CROP_PHOTO);
    }

    private void selectPhoto() {
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_CROP_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_FOR_ADD_MEMBER && resultCode == RESULT_OK) {// 添加成员返回
            loadMembers();
        } else if (requestCode == RESULT_FOR_MODIFY_NOTICE && resultCode == RESULT_OK) {// 修改公告返回
            if (data != null) {
                boolean isNeedUpdate = data.getBooleanExtra("isNeedUpdate", false);
                if (isNeedUpdate) {
                    loadMembers();
                }
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE_CROP_PHOTO) {// 拍照返回再去裁减
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    Uri o = mNewPhotoUri;
                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 1000, 1000);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_CROP_PHOTO) {// 选择一张图片,然后立即调用裁减
            Log.e("zx", "onActivityResult: 选择一张图片");
            if (resultCode == Activity.RESULT_OK) {
                Log.e("zx", "onActivityResult: RESULT_OK 选择一张图片");

                if (data != null) {
                    Uri o = Uri.fromFile(new File(CameraUtil.parsePickImageResult(data)));
                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 1000, 1000);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_CROP_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    // 上传头像
                    uploadAvatar(mCurrentFile);
                } else {
                    ToastUtil.showToast(this, R.string.c_crop_failed);
                }
            }
        }
    }

    private void uploadAvatar(File file) {
        if (!file.exists()) {
            // 文件不存在
            return;
        }
        // 显示正在上传的ProgressDialog
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("jid", mRoomJid);

        HttpUtils.post().url(coreManager.getConfig().AVATAR_UPLOAD_GROUP_URL)
                .params(params)
                .params("file1", file)
                .build()
                .execute(new FileCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(Void result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, R.string.upload_avatar_success);
                        AvatarHelper.updateAvatar(mRoom.getRoomId());// 更新时间
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, R.string.upload_avatar_failed);
                    }
                });
    }

    // 修改昵称
    private void showChangeNickNameDialog(final String nickName) {
        DialogHelper.showLimitSingleInputDialog(this
                , getString(R.string.update_nick_name)
                , nickName, getString(R.string.please_input_name)
                , 10
                , text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastUtil.showToast(mContext, getString(R.string.name_cannot_ull));
                        return false;
                    }
                    if (TextUtils.equals(nickName, text)) {
                        return true;
                    }
                    updateNickName(text);
                    return true;
                });
    }

    // 更新消息状态 置顶 、免打扰、屏蔽
    private void updateMessageStatus() {
        mSbTopChat.setChecked(mRoom.getTopTime() != 0);
        mSbDisturb.setChecked(mRoom.getOfflineNoPushMsg() == 1);
        sbHideConversation.setChecked(mRoom.getHideChatSwitch() == 1);
        boolean mShieldStatus = PreferenceUtils.getBoolean(mContext, Constants.SHIELD_GROUP_MSG + mRoomJid + mLoginUserId, false);
        mSbShield.setChecked(mShieldStatus);
    }

    private String conversion(double outTime) {
        String outTimeStr;
        if (outTime == -1 || outTime == 0) {
            outTimeStr = getString(R.string.never_no);
        } else if (outTime == 0.04) {
            outTimeStr = getString(R.string.one_hour);
        } else if (outTime == 1) {
            outTimeStr = getString(R.string.one_day);
        } else if (outTime == 7) {
            outTimeStr = getString(R.string.one_week);
        } else if (outTime == 30) {
            outTimeStr = getString(R.string.one_month);
        } else if (outTime == 90) {
            outTimeStr = getString(R.string.one_season);
        } else {
            outTimeStr = getString(R.string.one_year);
        }
        return outTimeStr;
    }

    /**
     * ScrollView移动到最顶端
     */
    private void scrollToTop() {
        mGridView.post(() -> {
            mGridView.smoothScrollToPosition(0);
        });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventGroupStatus eventGroupStatus) {
        if (eventGroupStatus.getWhichStatus() == 0) {
            mucRoom.setShowRead(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 1) {
            mucRoom.setIsLook(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 2) {
            mucRoom.setIsNeedVerify(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 3) {
            mucRoom.setShowMember(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 4) {
            mucRoom.setAllowSendCard(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 5) {
            mucRoom.setAllowInviteFriend(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 6) {
            mucRoom.setAllowUploadFile(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 7) {
            mucRoom.setAllowConference(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 8) {
            mucRoom.setAllowSpeakCourse(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 9) {
            mucRoom.setIsAttritionNotice(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 10) {
            mucRoom.setAllowOpenLive(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 11) {
            mucRoom.setShowMarker(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 12) {
            mucRoom.setAllowModifyCard(eventGroupStatus.getGroupManagerStatus());
        } else if (eventGroupStatus.getWhichStatus() == 10000) {// 设置/取消 管理员, 隐身人，监控人，
            loadMembers();
        } else if (eventGroupStatus.getWhichStatus() == 10001) {// 删除群成员
            mMemberSize = mMemberSize - 1;
            mCountTv.setText(mMemberSize + "/" + mucRoom.getMaxUserSize());
            mCountTv2.setText(getString(R.string.total_count_place_holder, mMemberSize));
            for (int i = 0; i < mMembers.size(); i++) {
                if (mMembers.get(i).getUserId().equals(String.valueOf(eventGroupStatus.getGroupManagerStatus()))) {
                    mCurrentMembers.remove(mMembers.get(i));
                    mMembers.remove(mMembers.get(i));
                    mAdapter.notifyDataSetInvalidated();
                }
            }
        } else if (eventGroupStatus.getWhichStatus() == 10002) {// 转让群
            loadMembers();
        } else if (eventGroupStatus.getWhichStatus() == 10003) {// 备注
            loadMembers();
            // 需通知群聊页面刷新
            MsgBroadcast.broadcastMsgRoomUpdate(mContext);
        }
    }

    /**
     * 公告，
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNewNotice message) {
        if (TextUtils.equals(mRoom.getUserId(), message.getRoomJid())) {
            loadMembers();
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventRemoveNotice message) {
        if (TextUtils.equals(mRoom.getUserId(), message.getRoomJid())) {
            loadMembers();
        }
    }

    private void registerRefreshReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OtherBroadcast.REFRESH_MANAGER);
        intentFilter.addAction(OtherBroadcast.REFRESH_OWNER);
        intentFilter.addAction(MsgBroadcast.ACTION_MSG_UPDATE_ROOM_INVITE);
        intentFilter.addAction(OtherBroadcast.QC_FINISH);
        registerReceiver(receiver, intentFilter);
    }

    /**
     * Todo Http Get
     * <p>
     * 修改群名称、描述
     */
    private void updateRoom(final String roomName, final String roomDes) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        if (!TextUtils.isEmpty(roomName)) {
            params.put("roomName", roomName);
        }

        if (!TextUtils.isEmpty(roomDes)) {
            params.put("desc", roomDes);
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(RoomInfoActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            if (!TextUtils.isEmpty(roomName)) {
                                mRoomNameTv.setText(roomName);
                                mRoom.setNickName(roomName);
                                FriendDao.getInstance().updateNickName(mLoginUserId, mRoom.getUserId(), roomName);
                            }

                            if (!TextUtils.isEmpty(roomDes)) {
                                mRoomDescTv.setText(roomDes);
                                mRoom.setDescription(roomDes);
                            }
                        } else {
                            Toast.makeText(RoomInfoActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
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
     * 更改群内昵称
     */
    private void updateNickName(final String nickName) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", mLoginUserId);
        params.put("nickname", nickName);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(mContext, R.string.update_success);
                        mNickNameTv.setText(nickName);
                        String loginUserId = coreManager.getSelf().getUserId();
                        FriendDao.getInstance().updateRoomName(loginUserId, mRoom.getUserId(), nickName);
                        ChatMessageDao.getInstance().updateNickName(loginUserId, mRoom.getUserId(), loginUserId, nickName);
                        mRoom.setRoomMyNickName(nickName);
                        FriendDao.getInstance().updateRoomMyNickName(mRoom.getUserId(), nickName);
                        ListenerManager.getInstance().notifyNickNameChanged(mRoom.getUserId(), loginUserId, nickName);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 消息免打扰 置顶
     */
    private void updateDisturbState(final int type, final int disturb) {
        setSwitchButtonEnableTouch(type, false);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("userId", mLoginUserId);
        params.put("type", String.valueOf(type));
        params.put("offlineNoPushMsg", String.valueOf(disturb));

        HttpUtils.get().url(coreManager.getConfig().ROOM_DISTURB)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(type, true);
                        if (Result.checkSuccess(mContext, result)) {
                            if (type == 0) {
                                mRoom.setOfflineNoPushMsg(disturb);
                                FriendDao.getInstance().updateOfflineNoPushMsgStatus(mRoom.getUserId(), disturb);
                            } else {
                                if (disturb == 1) {
                                    mRoom.setTopTime(TimeUtils.sk_time_current_time());
                                    FriendDao.getInstance().updateTopFriend(mRoomJid, mRoom.getTimeSend());
                                } else {
                                    mRoom.setTopTime(0);
                                    FriendDao.getInstance().resetTopFriend(mRoomJid);
                                }
                                if (!isMucChatComing) {// 非聊天界面进入，需要刷新消息页面
                                    MsgBroadcast.broadcastMsgUiUpdate(RoomInfoActivity.this);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(type, true);
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 更新隐藏会话状态
     *
     * @param type
     * @param isChecked
     */
    private void updateHideChatSwitchStatus(final int type, final boolean isChecked) {
        if (isChecked) {
            String password = PreferenceUtils.getString(mContext, Constants.HIDE_CHAT_PASSWORD + CoreManager.requireSelf(mContext).getUserId());
            if (TextUtils.isEmpty(password)) {
                sbHideConversation.post(() -> {
                    setSwitchButtonCancel(type);
                });
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(MyApplication.getContext().getString(R.string.gain_hide_conversation)
                        , MyApplication.getContext().getString(R.string.gain_hide_conversation_tip)
                        , MyApplication.getContext().getString(R.string.cancel)
                        , MyApplication.getContext().getString(R.string.hint_password), new SelectionFrame.OnSelectionFrameClickListener() {
                            @Override
                            public void cancelClick() {
                            }

                            @Override
                            public void confirmClick() {
                                SetupHideChatPasswordActivity.start(mContext);
                            }
                        });
                selectionFrame.show();
                return;
            }
        }
        setSwitchButtonEnableTouch(type, false);
        Map<String, String> params = new HashMap<>();
        params.put("roomId", mRoom.getRoomId());
        params.put("hideChatSwitch", isChecked ? String.valueOf(1) : String.valueOf(0));

        HttpUtils.get().url(coreManager.getConfig().ROOM_HIDE_CHAT_SWITCH)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(type, true);
                        if (Result.checkSuccess(mContext, result)) {
                            mRoom.setHideChatSwitch(isChecked ? 1 : 0);
                            FriendDao.getInstance().updateHideChatSwitchStatus(mRoom.getUserId(), isChecked ? 1 : 0);
                            // 更新消息、群聊界面
                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            MucgroupUpdateUtil.broadcastUpdateUi(mContext);
                        } else {
                            if (isChecked) {
                                setSwitchButtonCancel(type);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(type, true);
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    /**
     * 更新群组内的某个属性
     */
    private void updateSingleAttribute(final String attributeKey, final String attributeValue) {
        setSwitchButtonEnableTouch(2, false);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put(attributeKey, attributeValue);

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(2, true);
                        if (Result.checkSuccess(mContext, result)) {
                            if (attributeKey.equals("talkTime")) {// 全体禁言
                            }
                            switch (attributeKey) {
                                case "talkTime":
                                    if (Long.parseLong(attributeValue) > 0) {// 开启全体禁言
                                        PreferenceUtils.putBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mRoom.getUserId(), true);
                                    } else {// 取消全体禁言
                                        PreferenceUtils.putBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mRoom.getUserId(), false);
                                    }
                                    break;
                                case "maxUserSize":
                                    mucRoom.setMaxUserSize(Integer.valueOf(attributeValue));
                                    tvMemberLimit.setText(attributeValue);
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(2, true);
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 更新消息保存天数
     */
    private void updateChatRecordTimeOut(final double outTime) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        params.put("chatRecordTimeOut", String.valueOf(outTime));

        HttpUtils.get().url(coreManager.getConfig().ROOM_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(RoomInfoActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            mMsgSaveDays.setText(conversion(outTime));
                            FriendDao.getInstance().updateChatRecordTimeOut(mRoom.getUserId(), outTime);

                            Intent intent = new Intent();
                            intent.setAction(Constants.CHAT_TIME_OUT_ACTION);
                            intent.putExtra("friend_id", mRoom.getUserId());
                            intent.putExtra("time_out", outTime);
                            mContext.sendBroadcast(intent);
                        } else {
                            Toast.makeText(RoomInfoActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
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
     * 清空本地聊天记录，更新清空本地聊天记录时间
     */
    private void updateCleanMsgTime() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoom.getRoomId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_SET_CLEAN_MSG_TIME)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            // 清空本地聊天记录
                            FriendDao.getInstance().resetFriendMessage(mLoginUserId, mRoomJid);
                            ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, mRoomJid);
                            MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(mLoginUserId, mRoom.getUserId());
                            // 清空聊天界面
                            Intent intent = new Intent(Constants.CHAT_HISTORY_EMPTY);
                            intent.putExtra("id", mRoomJid);
                            sendBroadcast(intent);
                            MsgBroadcast.broadcastMsgUiUpdate(RoomInfoActivity.this);
                            Toast.makeText(RoomInfoActivity.this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /*
    举报
     */
    private void report(String roomId, Report report) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("reportType", String.valueOf(2));
        params.put("reason", String.valueOf(report.getReportId()));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            ToastUtil.showToast(RoomInfoActivity.this, R.string.report_success);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    /**
     * 退出群组
     */
    private void quitRoom(String desc, final String url, final Map<String, String> params) {
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(null, desc, new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                DialogHelper.showDefaulteMessageProgressDialog(RoomInfoActivity.this);
                HttpUtils.get().url(url)
                        .params(params)
                        .build()
                        .execute(new BaseCallback<Void>(Void.class) {

                            @Override
                            public void onResponse(ObjectResult<Void> result) {
                                DialogHelper.dismissProgressDialog();
                                if (result.getResultCode() == 1) {
                                    deleteFriend();
                                    if (isMucChatComing) {// 如果从聊天界面进入，退出 / 解散 群组需要销毁聊天界面
                                        Intent intent = new Intent(RoomInfoActivity.this, MainActivity.class);
                                        startActivity(intent);
                                    }
                                    finish();
                                } else {
                                    Toast.makeText(RoomInfoActivity.this, result.getResultMsg() + "", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e) {
                                DialogHelper.dismissProgressDialog();
                                ToastUtil.showErrorNet(RoomInfoActivity.this);
                            }
                        });
            }
        });
        selectionFrame.show();
    }

    private void deleteFriend() {
        // 删除这个房间
        FriendDao.getInstance().deleteFriend(mLoginUserId, mRoom.getUserId());
        // 消息表中删除
        ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, mRoom.getUserId());
        RoomMemberDao.getInstance().deleteRoomMemberTable(mRoom.getRoomId());
        MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(mLoginUserId, mRoom.getUserId());
        // 更新消息界面
        MsgBroadcast.broadcastMsgNumReset(this);
        MsgBroadcast.broadcastMsgUiUpdate(this);
        // 更新群聊界面
        MucgroupUpdateUtil.broadcastUpdateUi(this);
        coreManager.exitMucChat(mRoom.getUserId());
    }

    private void setSwitchButtonEnableTouch(int type, boolean enableTouch) {
        if (type == 0) {
            mSbDisturb.setEnableTouch(enableTouch);
        } else if (type == 1) {
            mSbTopChat.setEnableTouch(enableTouch);
        } else if (type == 2) {
            mSbAllShutUp.setEnableTouch(enableTouch);
        } else if (type == 3) {
            sbHideConversation.setEnableTouch(enableTouch);
        }
    }

    private void setSwitchButtonCancel(int type) {
        SwitchButton sb = null;
        if (type == 3) {
            sb = sbHideConversation;
        }
        if (sb != null) {
            sb.setCheckedWithoutCallback(false);
        }
    }

    /**
     * 设置私密密码成功才显示私密模式启用，
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventHideChatPasswordCreated message) {
        sbHideConversation.setChecked(true);
    }

    @Override
    public void onDeleteMucRoom(String toUserId) {

    }

    @Override
    public void onMyBeDelete(String toUserId) {

    }

    @Override
    public void onNickNameChange(String toUserId, String changedUserId, String changedName) {
        if (toUserId != null && toUserId.equals(mRoomJid)) {
            // 群名已改变
            if (changedUserId.equals("ROOMNAMECHANGE")) {
                if (mRoom != null) {
                    mRoom.setNickName(changedName);
                }
                mRoomNameTv.setText(changedName);
                return;
            }
        }
    }

    @Override
    public void onMyVoiceBanned(String toUserId, int time) {

    }

    public class RefreshBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(OtherBroadcast.REFRESH_MANAGER)) {
                String roomId = intent.getStringExtra("roomId");
                String toUserId = intent.getStringExtra("toUserId");
                boolean isSet = intent.getBooleanExtra("isSet", false);
                if (roomId.equals(mRoomJid) && toUserId.equals(mLoginUserId)) {
                    TipDialog tipDialog = new TipDialog(RoomInfoActivity.this);
                    tipDialog.setmConfirmOnClickListener(isSet ? getString(R.string.tip_became_manager) : getString(R.string.tip_be_cancel_manager)
                            , new TipDialog.ConfirmOnClickListener() {
                                @Override
                                public void confirm() {
                                    finish();
                                }
                            });
                    tipDialog.show();
                }
            } else if (action.equals(OtherBroadcast.REFRESH_OWNER)) {
                String roomId = intent.getStringExtra("roomId");
                String toUserId = intent.getStringExtra("toUserId");
                boolean isSet = intent.getBooleanExtra("isSet", false);
                if (roomId.equals(mRoomJid) && toUserId.equals(mLoginUserId) && isSet) {
                    TipDialog tipDialog = new TipDialog(RoomInfoActivity.this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_became_owner)
                            , new TipDialog.ConfirmOnClickListener() {
                                @Override
                                public void confirm() {
                                    finish();
                                }
                            });
                    tipDialog.show();
                }
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UPDATE_ROOM_INVITE)) {
                if (mucRoom != null) {
                    int enabled = intent.getIntExtra(MsgBroadcast.EXTRA_ENABLED, -1);
                    if (enabled != -1) {
                        mucRoom.setAllowInviteFriend(enabled);
                    }
                }
            } else if (action.equals(OtherBroadcast.QC_FINISH)) {
                finish();
            }
        }
    }

    class GridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCurrentMembers.size();
        }

        @Override
        public Object getItem(int position) {
            return mCurrentMembers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_room_info_view, parent, false);
                GridViewHolder vh = new GridViewHolder(convertView);
                convertView.setTag(vh);
            }
            GridViewHolder vh = (GridViewHolder) convertView.getTag();
            ImageView imageView = vh.imageView;
            TextView memberName = vh.memberName;
            int GAT5;
            if (add_minus_count == 1) {
                GAT5 = add_minus_count + 2;
            } else {
                GAT5 = add_minus_count + 1;
            }
            if (position > mCurrentMembers.size() - GAT5) {// + -
                memberName.setText("");
                if (position == mCurrentMembers.size() - 2) {
                    imageView.setImageResource(R.drawable.bg_room_info_add_btn);
                }
                if (position == mCurrentMembers.size() - 1) {
                    imageView.setImageResource(R.drawable.bg_room_info_minus_btn);
                }
            } else {
                MucRoomMember mMucRoomMember = mCurrentMembers.get(position);
                String name;
                if (role == 1 || role == 2) {// 群主 群内备注>好友备注>群内昵称
                    if (!TextUtils.isEmpty(mMucRoomMember.getRemarkName())) {
                        name = mMucRoomMember.getRemarkName();
                    } else {
                        if (mRemarksMap.containsKey(mCurrentMembers.get(position).getUserId())) {// 群组内 我的好友 显示 我对他备注的名字
                            name = mRemarksMap.get(mMucRoomMember.getUserId());
                        } else {
                            name = mMucRoomMember.getNickName();
                        }
                    }
                } else {
                    if (mRemarksMap.containsKey(mCurrentMembers.get(position).getUserId())) {// 群组内 我的好友 显示 我对他备注的名字
                        name = mRemarksMap.get(mMucRoomMember.getUserId());
                    } else {
                        name = mMucRoomMember.getNickName();
                    }
                }

                // 判断是否禁止群成员私聊，禁止则name带上*
/*
                boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mRoom.getUserId(), true);
                if (!isAllowSecretlyChat
                        && !TextUtils.isEmpty(name)
                        && myself != null
                        && myself.getRole() == 3) {// 禁止私聊并且名字不为空
                    name = name.substring(0, name.length() - 1) + "*";
                }
*/
                if (mMucRoomMember.getHiding() == 1) {
                    imageView.setImageResource(Constants.HIDING_AVATAR);
                } else {
                    AvatarHelper.getInstance().displayAvatar(name, mMucRoomMember.getUserId(), imageView, true);
                }
                memberName.setText(name);
            }
            return convertView;
        }
    }

    class GridViewHolder {
        ImageView imageView;
        TextView memberName;

        GridViewHolder(View itemView) {
            imageView = itemView.findViewById(R.id.content);
            memberName = itemView.findViewById(R.id.member_name);
        }
    }
}
