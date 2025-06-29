package com.tongxin.caihong.ui.message.single;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.EventHideChatPasswordCreated;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.EventSendReadChanged;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.LabelDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.TranslateHelper;
import com.tongxin.caihong.pay.TransferRecordActivity;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.groupchat.SelectContactsActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.SetupHideChatPasswordActivity;
import com.tongxin.caihong.ui.message.search.SearchChatHistoryActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.view.MsgSaveDaysDialog;
import com.tongxin.caihong.view.SelectChatModeDialog;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.SwitchButton;
import com.example.qrcode.Constant;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * Created by Administrator on 2018/4/18 0018.
 */

public class PersonSettingActivity extends BaseActivity implements View.OnClickListener {

    private ImageView mFriendAvatarIv;
    private TextView mFriendNameTv;
    private TextView mRemarkNameTv;
    private TextView mLabelNameTv;
    private SwitchButton mIsReadFireSb;
    private SwitchButton mTopSb;
    private SwitchButton mIsDisturbSb;
    private SwitchButton sbHideConversation;
    private SwitchButton sbMarker;
    private SwitchButton sbIsSendRead;
    private TextView mMsgSaveDays;

    private String mLoginUserId;
    private String mFriendId;
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
    private String toShowMarker;
    private Friend mFriend;
    private String mFriendName;
    private RefreshBroadcastReceiver receiver = new RefreshBroadcastReceiver();
    // SecureFlag
    private SwitchButton sb1, sb2, sb3, sb4;
    private List<SwitchButton> buttons = new ArrayList<>();
    private int mCurrentCheck = -1;
    private int mLastType;// 网络请求失败之后，还原为上一次type
    // todo changed ui
    private SelectChatModeDialog mSelectChatModeDialog;
    private TextView mSelectTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_setting);

        mLoginUserId = coreManager.getSelf().getUserId();
        mFriendId = getIntent().getStringExtra("ChatObjectId");
        toShowMarker = getIntent().getStringExtra("toShowMarker");
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);

        if (mFriend == null) {
            LogUtils.log(getIntent());
            Reporter.unreachable();
            ToastUtil.showToast(this, R.string.tip_friend_not_found);
            finish();
            return;
        }

        initActionBar();
        initView();
        registerReceiver();
        EventBusHelper.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);// Friend也更新下
        if (mFriend == null) {
            Toast.makeText(this, R.string.tip_friend_removed, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            mFriendName = TextUtils.isEmpty(mFriend.getRemarkName()) ? mFriend.getNickName() : mFriend.getRemarkName();
            mFriendNameTv.setText(mFriendName);
            if (mFriend.getRemarkName() != null) {
                mRemarkNameTv.setText(mFriend.getRemarkName());
            }
            List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mFriendId);
            String labelNames = "";
            if (friendLabelList != null && friendLabelList.size() > 0) {
                for (int i = 0; i < friendLabelList.size(); i++) {
                    if (i == friendLabelList.size() - 1) {
                        labelNames += friendLabelList.get(i).getGroupName();
                    } else {
                        labelNames += friendLabelList.get(i).getGroupName() + "，";
                    }
                }
            }
            mLabelNameTv.setText(labelNames);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            // 无论如何不应该在destroy崩溃，
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.chat_settings));
    }

    private void initView() {
        SwitchButton sbAutoTranslate = (SwitchButton) findViewById(R.id.sb_auto_translate);
        sbAutoTranslate.setChecked(TranslateHelper.isAutoTranslate(mContext, coreManager.getSelf().getUserId(), mFriend.getUserId()));
        sbAutoTranslate.setOnCheckedChangeListener((view, isChecked) -> {
            TranslateHelper.setAutoTranslate(mContext, coreManager.getSelf().getUserId(), mFriend.getUserId(), isChecked);
        });
        mFriendAvatarIv = (ImageView) findViewById(R.id.avatar);
        AvatarHelper.getInstance().displayAvatar(mFriendId, mFriendAvatarIv, true);
        mFriendNameTv = (TextView) findViewById(R.id.name);
        mRemarkNameTv = (TextView) findViewById(R.id.remark_name);
        mLabelNameTv = (TextView) findViewById(R.id.label_name);
        TextView mNoDisturbTv = (TextView) findViewById(R.id.no_disturb_tv);
        mNoDisturbTv.setText(getString(R.string.message_not_disturb));
        // 阅后即焚 && 置顶 && 消息免打扰
        mIsReadFireSb = (SwitchButton) findViewById(R.id.sb_read_fire);
        int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + mFriendId + mLoginUserId, 0);
        mIsReadFireSb.setChecked(isReadDel == 1);
        mIsReadFireSb.setOnCheckedChangeListener((view, isChecked) -> updateDisturbStatus(1, isChecked));

        mTopSb = (SwitchButton) findViewById(R.id.sb_top_chat);
        mTopSb.setChecked(mFriend.getTopTime() != 0);// TopTime不为0，当前状态为置顶
        mTopSb.setOnCheckedChangeListener((view, isChecked) -> updateDisturbStatus(2, isChecked));

        mIsDisturbSb = (SwitchButton) findViewById(R.id.sb_no_disturb);
        mIsDisturbSb.setChecked(mFriend.getOfflineNoPushMsg() == 1);
        mIsDisturbSb.setOnCheckedChangeListener((view, isChecked) -> updateDisturbStatus(0, isChecked));

        sbHideConversation = findViewById(R.id.sbHideConversation);
        sbHideConversation.setChecked(mFriend.getHideChatSwitch() == 1);
        sbHideConversation.setOnCheckedChangeListener((view, isChecked) -> updateHideChatSwitchStatus(3, isChecked));

        sbMarker = findViewById(R.id.sbMarker);
        sbMarker.setChecked(!TextUtils.isEmpty(toShowMarker));
        sbMarker.setOnCheckedChangeListener((view, isChecked) -> updateMarker(4, isChecked));

        sbIsSendRead = findViewById(R.id.sbIsSendRead);
        sbIsSendRead.setChecked(mFriend.getIsSendRead() == 1);
        sbIsSendRead.setOnCheckedChangeListener((view, isChecked) -> updateSendRead(5, isChecked));

        initSecureChatCheck();

        mMsgSaveDays = (TextView) findViewById(R.id.msg_save_days_tv);
        mMsgSaveDays.setText(conversion(mFriend.getChatRecordTimeOut()));

        findViewById(R.id.avatar).setOnClickListener(this);
        if (coreManager.getLimit().cannotCreateGroup() || mFriend.getStatus() == Friend.STATUS_SYSTEM) {
            findViewById(R.id.add_contacts).setVisibility(View.GONE);
        } else {
            findViewById(R.id.add_contacts).setOnClickListener(this);
        }
        // 关闭支付功能，隐藏交易记录
        if (!coreManager.getConfig().enablePayModule) {
            findViewById(R.id.rl_transfer).setVisibility(View.GONE);
        }

        findViewById(R.id.chat_history_search).setOnClickListener(this);
        findViewById(R.id.remark_rl).setOnClickListener(this);
        findViewById(R.id.label_rl).setOnClickListener(this);
        findViewById(R.id.msg_save_days_rl).setOnClickListener(this);
        findViewById(R.id.set_background_rl).setOnClickListener(this);
        findViewById(R.id.chat_history_empty).setOnClickListener(this);
        findViewById(R.id.sync_chat_history_empty).setVisibility(View.GONE);
        findViewById(R.id.rl_transfer).setOnClickListener(this);

        findViewById(R.id.rl_transmission_public).setOnClickListener(this);
        findViewById(R.id.rl_transmission_desed).setOnClickListener(this);
        findViewById(R.id.rl_transmission_aes).setOnClickListener(this);
        findViewById(R.id.rl_transmission_asymmetric_aes).setOnClickListener(this);
        findViewById(R.id.rl_transmission_select).setOnClickListener(this);

        if (mFriend.getStatus() == Friend.STATUS_SYSTEM) {
            findViewById(R.id.remark_rl).setVisibility(View.GONE);
            findViewById(R.id.label_rl).setVisibility(View.GONE);
            if (mFriend.isService()) {
                findViewById(R.id.rl_transmission_select).setVisibility(View.GONE);
                findViewById(R.id.sync_chat_history_empty).setVisibility(View.GONE);
            }
        }
    }

    private void initSecureChatCheck() {
        sb1 = findViewById(R.id.sb_transmission_public);
        sb2 = findViewById(R.id.sb_transmission_desed);
        sb3 = findViewById(R.id.sb_transmission_aes);
        sb4 = findViewById(R.id.sb_transmission_asymmetric_aes);
        buttons.add(sb1);
        buttons.add(sb2);
        buttons.add(sb3);
        buttons.add(sb4);
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setEnableTouch(false);
        }

        // todo changed ui
        mSelectTv = findViewById(R.id.rl_transmission_select_tv);

        changeCheck(mFriend.getEncryptType(), false);
    }

    private void changeCheck(int encryptType, boolean isUpdate) {
        boolean isSameClick = mCurrentCheck == encryptType;
        mLastType = mCurrentCheck;
        mCurrentCheck = encryptType;

        if (!isUpdate) {
            for (int i = 0; i < buttons.size(); i++) {
                if (encryptType == i) {
                    buttons.get(i).setChecked(true);
                } else {
                    buttons.get(i).setChecked(false);
                }
            }

            // todo changed ui
            if (encryptType == 0) {
                mSelectTv.setText(getString(R.string.msg_transmission_public));
            } else if (encryptType == 1) {
                mSelectTv.setText(getString(R.string.msg_transmission_desed));
            } else if (encryptType == 2) {
                mSelectTv.setText(getString(R.string.msg_transmission_aes));
            } else if (encryptType == 3) {
                mSelectTv.setText(getString(R.string.msg_transmission_asymmetric_aes));
            }
        }

        if (isUpdate && !isSameClick) {
            if (encryptType == 2 || encryptType == 3) {// 兼容老版本与其它端，自己与好友都有dh公钥，才能开启端到端聊天和AES加密
                String key = SecureChatUtil.getDHPrivateKey(coreManager.getSelf().getUserId());
                if (TextUtils.isEmpty(key)) {
                    ToastUtil.showToast(mContext, getString(encryptType == 2 ? R.string.you_are_not_eligible_for_encrypt_aes : R.string.you_are_not_eligible_for_encrypt));
                    return;
                }
                if (TextUtils.isEmpty(mFriend.getPublicKeyDH())) {
                    ToastUtil.showToast(mContext, getString(encryptType == 2 ? R.string.friend_are_not_eligible_for_encrypt_aes : R.string.friend_are_not_eligible_for_encrypt));
                    return;
                }
            }
            DialogHelper.showDefaulteMessageProgressDialog(this);
            updateEncryptType(encryptType);
        }
    }

    private void updateEncryptType(int type) {
        Map<String, String> params = new HashMap<>();
        params.put("toUserId", mFriendId);
        params.put("encryptType", String.valueOf(type));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_FRIENDS_MODIFY_ENCRYPT_TYPE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            FriendDao.getInstance().updateEncryptType(mFriendId, type);
                            for (int i = 0; i < buttons.size(); i++) {
                                if (type == i) {
                                    buttons.get(i).setChecked(true);
                                } else {
                                    buttons.get(i).setChecked(false);
                                }
                            }

                            // todo changed ui
                            if (mSelectChatModeDialog != null) {
                                mSelectChatModeDialog.dismiss();
                            }
                            if (type == 0) {
                                mSelectTv.setText(getString(R.string.msg_transmission_public));
                            } else if (type == 1) {
                                mSelectTv.setText(getString(R.string.msg_transmission_desed));
                            } else if (type == 2) {
                                mSelectTv.setText(getString(R.string.msg_transmission_aes));
                            } else if (type == 3) {
                                mSelectTv.setText(getString(R.string.msg_transmission_asymmetric_aes));
                            }
                        } else {
                            mCurrentCheck = mLastType;// 还原type
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                        mCurrentCheck = mLastType;// 还原type
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.avatar:
                Intent intentBasic = new Intent(this, BasicInfoActivity.class);
                intentBasic.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentBasic);
                break;
            case R.id.add_contacts:
                Intent intentAdd = new Intent(this, SelectContactsActivity.class);
                intentAdd.putExtra("QuicklyCreateGroup", true);
                intentAdd.putExtra("ChatObjectId", mFriendId);
                intentAdd.putExtra("ChatObjectName", mFriendName);
                startActivity(intentAdd);
                break;
            case R.id.chat_history_search:
                Intent intentChat = new Intent(this, SearchChatHistoryActivity.class);
                intentChat.putExtra("isSearchSingle", true);
                intentChat.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentChat);
                break;
            case R.id.remark_rl:
                start();
                break;
            case R.id.label_rl:
                Intent intentLabel = new Intent(this, SetLabelActivity.class);
                intentLabel.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentLabel);
                break;
            case R.id.msg_save_days_rl:
                MsgSaveDaysDialog msgSaveDaysDialog = new MsgSaveDaysDialog(this, onMsgSaveDaysDialogClickListener);
                msgSaveDaysDialog.show();
                break;
            case R.id.set_background_rl:
                Intent intentBackground = new Intent(this, SelectSetTypeActivity.class);
                intentBackground.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentBackground);
                break;
            case R.id.chat_history_empty:
                clean(false);
                break;
            case R.id.sync_chat_history_empty:
                clean(true);
                break;
            case R.id.rl_transfer:
                Intent intentTransfer = new Intent(this, TransferRecordActivity.class);
                intentTransfer.putExtra(Constant.TRANSFE_RRECORD, mFriendId);
                startActivity(intentTransfer);
                break;
            case R.id.rl_transmission_public:
                changeCheck(0, true);
                break;
            case R.id.rl_transmission_desed:
                changeCheck(1, true);
                break;
            case R.id.rl_transmission_aes:
                changeCheck(2, true);
                break;
            case R.id.rl_transmission_asymmetric_aes:
                changeCheck(3, true);
                break;
            case R.id.rl_transmission_select:
                mSelectChatModeDialog = new SelectChatModeDialog(mContext, new SelectChatModeDialog.OnBannedDialogClickListener() {
                    @Override
                    public void tv1Click() {
                        changeCheck(0, true);
                    }

                    @Override
                    public void tv2Click() {
                        changeCheck(1, true);
                    }

                    @Override
                    public void tv3Click() {
                        changeCheck(2, true);
                    }

                    @Override
                    public void tv4Click() {
                        changeCheck(3, true);
                    }
                });
                mSelectChatModeDialog.show();
                break;
        }
    }

    /**
     * 备注电话号码未存本地，跳转设置时只能先从接口内取出了
     */
    private void start() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mFriendId);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {
                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            User user = result.getData();
                            if (user.getFriends() == null) {// 理论上不可能
                                SetRemarkActivity.start(mContext, mFriendId, "");
                            } else {
                                SetRemarkActivity.start(mContext, mFriendId, user.getFriends().getPhoneRemark());
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void clean(boolean isSync) {
        String tittle = isSync ? getString(R.string.sync_chat_history_clean) : getString(R.string.clean_chat_history);
        String tip = isSync ? getString(R.string.tip_sync_chat_history_clean) : getString(R.string.clean_chat_history_server);

        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(tittle, tip, new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                if (isSync) {
                    // 发送一条双向清除的消息给对方，对方收到消息后也将本地消息删除
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setType(XmppMessage.TYPE_SYNC_CLEAN_CHAT_HISTORY);
                    chatMessage.setFromUserId(mLoginUserId);
                    chatMessage.setFromUserName(coreManager.getSelf().getNickName());
                    chatMessage.setToUserId(mFriendId);
                    chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                    chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                    coreManager.sendChatMessage(mFriendId, chatMessage);
                }
                emptyServerMessage();
            }
        });
        selectionFrame.show();
    }

    // 更新消息免打扰状态
    private void updateDisturbStatus(final int type, final boolean isChecked) {
        setSwitchButtonEnableTouch(type, false);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        params.put("toUserId", mFriendId);
        params.put("type", String.valueOf(type));
        params.put("offlineNoPushMsg", isChecked ? String.valueOf(1) : String.valueOf(0));

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_NOPULL_MSG)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(type, true);
                        if (Result.checkSuccess(mContext, result)) {
                            if (type == 0) {// 消息免打扰
                                FriendDao.getInstance().updateOfflineNoPushMsgStatus(mFriendId, isChecked ? 1 : 0);
                            } else if (type == 1) {// 阅后即焚
                                PreferenceUtils.putInt(mContext, Constants.MESSAGE_READ_FIRE + mFriendId + mLoginUserId, isChecked ? 1 : 0);
                                if (isChecked) {
                                    ToastUtil.showToast(PersonSettingActivity.this, R.string.tip_status_burn);
                                }
                            } else {// 置顶聊天
                                if (isChecked) {
                                    FriendDao.getInstance().updateTopFriend(mFriendId, mFriend.getTimeSend());
                                } else {
                                    FriendDao.getInstance().resetTopFriend(mFriendId);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(type, true);
                        ToastUtil.showNetError(PersonSettingActivity.this);
                    }
                });
    }

    // 更新隐藏会话状态
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
        params.put("toUserId", mFriendId);
        params.put("hideChatSwitch", isChecked ? String.valueOf(1) : String.valueOf(0));

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_HIDE_CHAT_SWITCH)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(type, true);
                        if (Result.checkSuccess(mContext, result)) {
                            FriendDao.getInstance().updateHideChatSwitchStatus(mFriendId, isChecked ? 1 : 0);
                            // 更新消息、通讯录界面
                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                        } else {
                            if (isChecked) {
                                setSwitchButtonCancel(type);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(type, true);
                        ToastUtil.showNetError(PersonSettingActivity.this);
                    }
                });
    }

    // 更新发送已读状态
    private void updateSendRead(final int type, final boolean isChecked) {
        setSwitchButtonEnableTouch(type, false);
        Map<String, String> params = new HashMap<>();
        params.put("toUserId", mFriendId);
        params.put("isSendMsgState", isChecked ? String.valueOf(1) : String.valueOf(0));

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_SEND_READ)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(type, true);
                        if (Result.checkSuccess(mContext, result)) {
                            FriendDao.getInstance().updateSendRead(mFriendId, isChecked ? 1 : 0);
                            EventBus.getDefault().post(new EventSendReadChanged(mFriendId, isChecked));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(type, true);
                        ToastUtil.showNetError(PersonSettingActivity.this);
                    }
                });
    }

    // 更新消息保存天数
    private void updateChatRecordTimeOut(final double outTime) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("chatRecordTimeOut", String.valueOf(outTime));

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            // 发一条通知给对方
                            ChatMessage chatMessage = MessageUtil.generateMessageExpiredAutoDeleteTip(mLoginUserId, coreManager.getSelf().getNickName(), mFriendId, outTime);
                            coreManager.sendChatMessage(mFriendId, chatMessage);

                            Toast.makeText(PersonSettingActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            mMsgSaveDays.setText(conversion(outTime));
                            FriendDao.getInstance().updateChatRecordTimeOut(mFriendId, outTime);
                        } else {
                            Toast.makeText(PersonSettingActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 更新水印开关，
    private void updateMarker(final int type, final boolean isChecked) {
        if (isChecked) {
            updateMarker(type, coreManager.getSelf().getAccount());
            DialogHelper.showLimitSingleInputDialog(this
                    , getString(R.string.label_marker_settings)
                    , coreManager.getSelf().getAccount(), getString(R.string.label_marker_settings)
                    , 15
                    , text -> {
                        if (TextUtils.isEmpty(text)) {
                            ToastUtil.showToast(mContext, R.string.tip_marker_empty);
                            return false;
                        }
                        updateMarker(type, text);
                        return true;
                    });
        } else {
            updateMarker(type, null);
        }
    }

    // 更新水印，
    private void updateMarker(final int type, String sMarker) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        int showMarker;
        String markContent;
        if (TextUtils.isEmpty(sMarker)) {
            showMarker = 0;
            markContent = "";
        } else {
            showMarker = 1;
            markContent = sMarker;
        }
        params.put("showMarker", String.valueOf(showMarker));
        params.put("markContent", markContent);

        setSwitchButtonEnableTouch(type, false);
        HttpUtils.get().url(coreManager.getConfig().FRIENDS_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        setSwitchButtonEnableTouch(type, true);
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(PersonSettingActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            toShowMarker = sMarker;
                            ChatActivity.toShowMarker = sMarker;
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        setSwitchButtonEnableTouch(type, true);
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 服务器上与该人的聊天记录也需要删除
    private void emptyServerMessage() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", String.valueOf(2));// 0 清空单人 1 清空所有
        params.put("toUserId", mFriendId);

        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().EMPTY_SERVER_MESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            FriendDao.getInstance().resetFriendMessage(mLoginUserId, mFriendId);
                            ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, mFriendId);
                            MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(mLoginUserId, mFriendId);
                            // 清空聊天界面
                            Intent intent = new Intent(Constants.CHAT_HISTORY_EMPTY);
                            intent.putExtra("id", mFriendId);
                            sendBroadcast(intent);
                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            Toast.makeText(PersonSettingActivity.this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void setSwitchButtonEnableTouch(int type, boolean enableTouch) {
        if (type == 0) {
            mIsDisturbSb.setEnableTouch(enableTouch);
        } else if (type == 1) {
            mIsReadFireSb.setEnableTouch(enableTouch);
        } else if (type == 2) {
            mTopSb.setEnableTouch(enableTouch);
        } else if (type == 3) {
            sbHideConversation.setEnableTouch(enableTouch);
        } else if (type == 4) {
            sbMarker.setEnableTouch(enableTouch);
        } else if (type == 5) {
            sbIsSendRead.setEnableTouch(enableTouch);
        }
    }

    private void setSwitchButtonCancel(int type) {
        SwitchButton sb = null;
        if (type == 0) {
            sb = mIsDisturbSb;
        } else if (type == 1) {
            sb = mIsReadFireSb;
        } else if (type == 2) {
            sb = mTopSb;
        } else if (type == 3) {
            sb = sbHideConversation;
        } else if (type == 4) {
            sb = sbMarker;
        }
        if (sb != null) {
            sb.setCheckedWithoutCallback(false);
        }
    }

    private String conversion(double outTime) {
        String outTimeStr;
        if (outTime == -1 || outTime == 0) {
            outTimeStr = getString(R.string.never_no);
        } else if (outTime == -2) {
            outTimeStr = getString(R.string.no_sync);
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

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OtherBroadcast.QC_FINISH);
        intentFilter.addAction(OtherBroadcast.ACTION_SYNC_ENCRYPT_TYPE);
        registerReceiver(receiver, intentFilter);
    }

    /**
     * 设置私密密码成功才显示私密模式启用，
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventHideChatPasswordCreated message) {
        sbHideConversation.setChecked(true);
    }

    public class RefreshBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(OtherBroadcast.QC_FINISH)) {
                // 快速创建群组 || 更换聊天背景 成功，接收到该广播结束当前界面
                finish();
            } else if (action.equals(OtherBroadcast.ACTION_SYNC_ENCRYPT_TYPE)) {
                String fromUserId = intent.getStringExtra("fromUserId");
                int encryptType = intent.getIntExtra("encryptType", 0);
                String content = intent.getStringExtra("content");
                if (TextUtils.equals(mFriendId, fromUserId)) {
                    if (encryptType == 0) {
                        mSelectTv.setText(getString(R.string.msg_transmission_public));
                    } else if (encryptType == 1) {
                        mSelectTv.setText(getString(R.string.msg_transmission_desed));
                    } else if (encryptType == 2) {
                        mSelectTv.setText(getString(R.string.msg_transmission_aes));
                    } else if (encryptType == 3) {
                        mSelectTv.setText(getString(R.string.msg_transmission_asymmetric_aes));
                    }
                    ToastUtil.showToast(mContext, content);
                }
            }
        }
    }
}
