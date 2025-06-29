package com.tongxin.caihong.ui.groupchat;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.InviteResult;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.call.CallConstants;
import com.tongxin.caihong.call.MessageEventInitiateMeeting;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.dialog.TowInputDialogView;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.CharUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.view.CircleImageView;
import com.tongxin.caihong.view.HorizontalListView;
import com.tongxin.caihong.view.NoDoubleClickListener;
import com.tongxin.caihong.view.TipDialog;
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
import okhttp3.Call;

/**
 * 选择联系人 发起群聊
 */
public class SelectContactsActivity extends BaseActivity {
    private EditText mEditText;
    private boolean isSearch;
    private SideBar mSideBar;
    private TextView mTextDialog;
    private ListView mListView;
    private ListViewAdapter mAdapter;
    private List<Friend> mFriendList;
    private List<BaseSortModel<Friend>> mSortFriends;
    private List<BaseSortModel<Friend>> mSearchSortFriends;
    private BaseComparator<Friend> mBaseComparator;
    private HorizontalListView mHorizontalListView;
    private HorListViewAdapter mHorAdapter;
    private List<String> mSelectPositions;
    private Button mOkBtn;
    private String mLoginUserId;
    // 是否是通过单人聊天快速创建的群组
    private boolean mQuicklyCreate;
    // 快速建群时聊天对象的id与备注名/昵称
    private String mQuicklyId;
    private String mQuicklyName;
    private TowInputDialogView towInputDialogView;
    // SecureFlagGroup 创建私密群组，建群ui交互调整
    private String roomName, roomDesc;
    private int isRead, isLook, isNeedVerify, isShowMember, isAllowSendCard, isSecretGroup;
    private String chatKey;
    private MucRoom mucRoom;

    // 快速发起会议
    private boolean mQuicklyInitiateMeeting;
    private int meetType;


    public static void startQuicklyInitiateMeeting(Context ctx, int meetType) {
        Intent quicklyInitiateMeetingIntent = new Intent(ctx, SelectContactsActivity.class);
        quicklyInitiateMeetingIntent.putExtra("QuicklyInitiateMeeting", true);
        quicklyInitiateMeetingIntent.putExtra("meetType", meetType);
        ctx.startActivity(quicklyInitiateMeetingIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contacts);
        if (getIntent() != null) {
            mQuicklyCreate = getIntent().getBooleanExtra("QuicklyCreateGroup", false);
            mQuicklyId = getIntent().getStringExtra("ChatObjectId");
            mQuicklyName = getIntent().getStringExtra("ChatObjectName");

            mQuicklyInitiateMeeting = getIntent().getBooleanExtra("QuicklyInitiateMeeting", false);
            meetType = getIntent().getIntExtra("meetType", CallConstants.Video_Meet);
        }
        mLoginUserId = coreManager.getSelf().getUserId();

        mFriendList = new ArrayList<>();
        mSortFriends = new ArrayList<>();
        mSearchSortFriends = new ArrayList<>();
        mBaseComparator = new BaseComparator<>();
        mAdapter = new ListViewAdapter();

        mSelectPositions = new ArrayList<>();
        mHorAdapter = new HorListViewAdapter();

        initActionBar();
        initView();
        setTouch(true);
        if (coreManager.getLimit().cannotCreateGroup()) {
            Reporter.unreachable();
            TipDialog tipDialog = new TipDialog(this);
            tipDialog.setTip(getString(R.string.tip_not_allow_create_room));
            tipDialog.setOnDismissListener(dialog -> {
                finish();
            });
            tipDialog.show();
        }
        //2023/11/30 新建群组填写信息
        if (!mQuicklyInitiateMeeting && !mQuicklyCreate) {
            showCreateGroupChatDialog();
        }
//2023/10/18 注释
       /* if (!mQuicklyCreate) {
            showCreateGroupChatDialog();
        }*/
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        //2023/10/18 注释 修改点击视频会议 触发的操作
        //tvTitle.setText(getString(R.string.select_group_members));
        if (mQuicklyInitiateMeeting) {
            tvTitle.setText(getString(R.string.select_contacts));
        } else {
            tvTitle.setText(getString(R.string.select_group_members));
        }
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(mAdapter);
        mHorizontalListView = (HorizontalListView) findViewById(R.id.horizontal_list_view);
        mHorizontalListView.setAdapter(mHorAdapter);
        mOkBtn = (Button) findViewById(R.id.ok_btn);
        ButtonColorChange.colorChange(mContext, mOkBtn);
        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mSideBar.setVisibility(View.VISIBLE);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                // 该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListView.setSelection(position);
                }
            }
        });

        /**
         * 创建群组邀请好友搜索功能
         */
        mEditText = (EditText) findViewById(R.id.search_et);
        mEditText.setHint(getString(R.string.search));
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                isSearch = true;
                mSearchSortFriends.clear();
                String str = mEditText.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    isSearch = false;
                    mAdapter.setData(mSortFriends);
                    return;
                }
                for (int i = 0; i < mSortFriends.size(); i++) {
                    Friend friend = mSortFriends.get(i).getBean();
                    if (friend.getNickName().contains(str)
                            || (!TextUtils.isEmpty(friend.getRemarkName())
                            && friend.getRemarkName().contains(str))) {
                        // 符合搜索条件的好友
                        mSearchSortFriends.add((mSortFriends.get(i)));
                    }
                }
                mAdapter.setData(mSearchSortFriends);
            }
        });

        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));

        mListView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            Friend friend;
            if (isSearch) {
                friend = mSearchSortFriends.get(position).bean;
            } else {
                friend = mSortFriends.get(position).bean;
            }

            if (isSecretGroup == 1 && TextUtils.isEmpty(friend.getPublicKeyRSARoom())) {
                Toast.makeText(mContext, getString(R.string.friend_are_not_eligible_for_join_secret_group), Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < mSortFriends.size(); i++) {
                if (mSortFriends.get(i).getBean().getUserId().equals(friend.getUserId())) {
                    if (friend.getStatus() != 100) {
                        friend.setStatus(100);
                        mSortFriends.get(i).getBean().setStatus(100);
                        addSelect(friend.getUserId());
                    } else {
                        friend.setStatus(101);
                        mSortFriends.get(i).getBean().setStatus(101);
                        removeSelect(friend.getUserId());
                    }

                    if (isSearch) {
                        mAdapter.setData(mSearchSortFriends);
                    } else {
                        mAdapter.setData(mSortFriends);
                    }
                }
            }
        });

        mHorizontalListView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            for (int i = 0; i < mSortFriends.size(); i++) {
                if (mSortFriends.get(i).getBean().getUserId().equals(mSelectPositions.get(position))) {
                    mSortFriends.get(i).getBean().setStatus(101);
                    if (isSearch) {
                        mAdapter.setData(mSearchSortFriends);
                    } else {
                        mAdapter.setData(mSortFriends);
                    }
                }
            }
            mSelectPositions.remove(position);
            mHorAdapter.notifyDataSetInvalidated();
            mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
        });

        mOkBtn.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                if (!coreManager.isLogin()) {
                    ToastUtil.showToast(mContext, R.string.service_start_failed);
                    return;
                }

                if (mQuicklyInitiateMeeting) {// 快速发起音视频会议
                    showSelectMeetingTypeDialog();
                    finish();
                    return;
                }


                if (mQuicklyCreate) {
                    // 因为前面已经为mSelectPositions增加了一个虚线框,So
                    if (mSelectPositions.size() <= 0) {
                        ToastUtil.showToast(mContext, getString(R.string.tip_create_group_at_lease_one_friend));
                        return;
                    }
                    String sc = coreManager.getSelf().getNickName() + "、" + mQuicklyName + "、";
                    for (int i = 0; i < mSelectPositions.size(); i++) {
                        String name = "";
                        for (int i1 = 0; i1 < mFriendList.size(); i1++) {
                            if (mFriendList.get(i1).getUserId().equals(mSelectPositions.get(i))) {
                                name = !TextUtils.isEmpty(mFriendList.get(i1).getRemarkName()) ? mFriendList.get(i1).getRemarkName() : mFriendList.get(i1).getNickName();
                            }
                        }
                        if (i == mSelectPositions.size() - 1) {
                            sc += name;
                        } else {
                            sc += name + "、";
                        }
                    }
                    createGroupChat(sc, "", 0, 1, 0, 1, 1, 0);
                } else {
                    createGroupChat(roomName, roomDesc, isRead, isLook, isNeedVerify, isShowMember, isAllowSendCard, isSecretGroup);
                }
            }
        });

        loadData();
    }

    private void showSelectMeetingTypeDialog() {
        if (mSelectPositions.size() == 0) {
            DialogHelper.tip(this, getString(R.string.tip_select_at_lease_one_member));
            return;
        }
        EventBus.getDefault().post(new MessageEventInitiateMeeting(meetType, mSelectPositions));
    }

    private void loadData() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, e -> {
            Reporter.post("加载数据失败，", e);
            AsyncUtils.runOnUiThread(this, ctx -> {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(ctx, R.string.data_exception);
            });
        }, c -> {
            final List<Friend> friends = FriendDao.getInstance().getFriendsGroupChat(mLoginUserId);
            if (friends != null) {
                for (Friend friend : friends) {
                    if (TextUtils.equals(friend.getUserId(), Friend.ID_SYSTEM_MESSAGE)) {
                        // 未知原因导致系统号的status变为好友，兼容一下
                        friends.remove(friend);
                        break;
                    } else if (mQuicklyCreate && TextUtils.equals(friend.getUserId(), mQuicklyId)) {
                        friends.remove(friend);
                        break;
                    }
                }
            }

            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, Friend::getShowName);
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mSideBar.setExistMap(existMap);
                mFriendList = friends;
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
            });
        });
    }

    private void addSelect(String userId) {
        mSelectPositions.add(userId);
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void removeSelect(String userId) {
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i).equals(userId)) {
                mSelectPositions.remove(i);
            }
        }
        mHorAdapter.notifyDataSetInvalidated();
        mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size()));
    }

    private void showCreateGroupChatDialog() {
        towInputDialogView = DialogHelper.showTowInputDialogAndReturnDialog(this,
                getString(R.string.create_room),
                getString(R.string.jx_inputroomname),
                getString(R.string.jxalert_inputsomething),
                (roomNameEdit, roomDescEdit, isRead, isLook, isNeedVerify, isShowMember, isAllowSendCard, isSecretGroup) -> {
                    String roomName = roomNameEdit.getText().toString().trim();
                    if (TextUtils.isEmpty(roomName)) {
                        Toast.makeText(SelectContactsActivity.this, getString(R.string.room_name_empty_error), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String roomDesc = roomDescEdit.getText().toString();
                    if (TextUtils.isEmpty(roomDesc)) {
                        Toast.makeText(SelectContactsActivity.this, getString(R.string.room_des_empty_error), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (CharUtils.getLength(roomName) > 100) {
                        Toast.makeText(SelectContactsActivity.this, R.string.tip_group_name_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (CharUtils.getLength(roomDesc) > 100) {
                        Toast.makeText(SelectContactsActivity.this, R.string.tip_group_description_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isSecretGroup == 1) {
                        // SecureFlag
                        if (TextUtils.isEmpty(SecureChatUtil.getDHPrivateKey(coreManager.getSelf().getUserId()))) {
                            ToastUtil.showToast(mContext, getString(R.string.friend_are_not_eligible_for_create_secret_group));
                            return;
                        }
                    }

                    this.roomName = roomName;
                    this.roomDesc = roomDesc;
                    this.isRead = isRead;
                    this.isLook = isLook;
                    this.isNeedVerify = isNeedVerify;
                    this.isShowMember = isShowMember;
                    this.isAllowSendCard = isAllowSendCard;
                    this.isSecretGroup = isSecretGroup;

                    if (towInputDialogView != null) {
                        towInputDialogView.dismiss();
                    }

                });

        if (towInputDialogView.getDialog() != null) {
            towInputDialogView.getDialog().setOnDismissListener(dialog -> {
                if (TextUtils.isEmpty(roomName)) {
                    finish();// 未填写如何信息，直接按back键
                }
            });
        }
    }

    private void createGroupChat(final String roomName, final String roomDesc, int isRead, int isLook,
                                 int isNeedVerify, int isShowMember, int isAllowSendCard, int isSecretGroup) {
        if (mucRoom != null) {
            // 如果建群已经成功，邀请好友时出问题，比如超过人数上限，重新确定时不能重新建群，
            createRoomSuccess(mucRoom);
            return;
        }
        final String roomJid = coreManager.createMucRoom(roomName);
        if (TextUtils.isEmpty(roomJid)) {
            ToastUtil.showToast(mContext, getString(R.string.create_room_failed));
            return;
        }
        MyApplication.mRoomKeyLastCreate = roomJid;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("jid", roomJid);
        params.put("name", roomName);
        params.put("desc", roomDesc);
        params.put("countryId", String.valueOf(Area.getDefaultCountyId()));

        params.put("showRead", isRead + "");
        // 显示已读人数
        PreferenceUtils.putBoolean(mContext, Constants.IS_SHOW_READ + roomJid, isRead == 1);
        // 是否公开
        params.put("isLook", isLook + "");
        // 是否开启进群验证
        params.put("isNeedVerify", isNeedVerify + "");
        // 其他群管理
        params.put("showMember", isShowMember + "");
        params.put("allowSendCard", isAllowSendCard + "");

        params.put("allowInviteFriend", "1");
        params.put("allowUploadFile", "1");
        params.put("allowConference", "1");
        params.put("allowSpeakCourse", "1");

        PreferenceUtils.putBoolean(mContext, Constants.IS_SEND_CARD + roomJid, isAllowSendCard == 1);

        Area area = Area.getDefaultProvince();
        if (area != null) {
            params.put("provinceId", String.valueOf(area.getId()));    // 省份Id
        }
        area = Area.getDefaultCity();
        if (area != null) {
            params.put("cityId", String.valueOf(area.getId()));            // 城市Id
            area = Area.getDefaultDistrict(area.getId());
            if (area != null) {
                params.put("areaId", String.valueOf(area.getId()));        // 城市Id
            }
        }

        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        // SecureFlagGroup
        params.put("isSecretGroup", String.valueOf(isSecretGroup));
        if (isSecretGroup == 1) {
            chatKey = UUID.randomUUID().toString().replaceAll("-", "");
            String chatKeyGroup = RSA.encryptBase64(chatKey.getBytes(),
                    Base64.decode(SecureChatUtil.getRSAPublicKey(coreManager.getSelf().getUserId())));
            Map<String, String> keys = new HashMap<>();
            keys.put(coreManager.getSelf().getUserId(), chatKeyGroup);
            String keysStr = JSON.toJSONString(keys);
            params.put("keys", keysStr);
        }

        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                    @Override
                    public void onResponse(ObjectResult<MucRoom> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (mQuicklyCreate) {
                                sendBroadcast(new Intent(OtherBroadcast.QC_FINISH)); // 快速建群成功，发送广播关闭之前的单聊界面
                            }
                            createRoomSuccess(result.getData());
                        } else {
                            MyApplication.mRoomKeyLastCreate = "compatible";// 还原回去
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        MyApplication.mRoomKeyLastCreate = "compatible";// 还原回去
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // 创建成功的时候将会调用此方法，将房间也存为好友
    private void createRoomSuccess(MucRoom mucRoom) {
        this.mucRoom = mucRoom;
        Friend friend = new Friend();
        friend.setOwnerId(mLoginUserId);
        friend.setUserId(mucRoom.getJid());
        friend.setNickName(mucRoom.getName());
        friend.setDescription(mucRoom.getDesc());
        friend.setRoomId(mucRoom.getId());
        friend.setRoomCreateUserId(mLoginUserId);
        friend.setRoomRole(1);
        friend.setRoomFlag(1);
        friend.setStatus(Friend.STATUS_FRIEND);
        // timeSend作为取群聊离线消息的标志，所以要在这里设置一个初始值
        friend.setTimeSend(TimeUtils.sk_time_current_time());
        // SecureFlagGroup
        friend.setIsSecretGroup(mucRoom.getIsSecretGroup());
        if (friend.getIsSecretGroup() == 1) {
            friend.setChatKeyGroup(SecureChatUtil.encryptChatKey(mucRoom.getJid(), chatKey));
        }
        if (mucRoom.getMember() != null) {
            friend.setJoinSeqNo(mucRoom.getMember().getJoinSeqNo());
        }
        FriendDao.getInstance().createOrUpdateFriend(friend);

        // 更新群组
        MucgroupUpdateUtil.broadcastUpdateUi(this);

        // 本地发送一条消息至该群 否则未邀请其他人时在消息列表不会显示
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TIP);
        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(coreManager.getSelf().getNickName());
        chatMessage.setToUserId(mucRoom.getJid());
        chatMessage.setContent(getString(R.string.new_friend_chat));
        chatMessage.setPacketId(coreManager.getSelf().getNickName());
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mucRoom.getJid(), chatMessage)) {
            // 更新聊天界面
            MsgBroadcast.broadcastMsgUiUpdate(SelectContactsActivity.this);
        }

        mucRoom.setMaxUserSize(4);

        // 邀请好友
        List<String> inviteUsers = new ArrayList<>(mSelectPositions);
        List<String> realInviteUsers = new ArrayList<>();
        if (mQuicklyCreate) {
            inviteUsers.add(0, mQuicklyId);
        }
        if (inviteUsers.size() > mucRoom.getMaxUserSize() - 1) {
            // 超过群组人数上限
            ToastUtil.showToast(mContext, getString(R.string.tip_over_member_size_tips, mucRoom.getMaxUserSize()));
            for (int i = 0; i < mucRoom.getMaxUserSize() - 1; i++) {
                realInviteUsers.add(inviteUsers.get(i));
            }
        } else {
            realInviteUsers.addAll(inviteUsers);
        }
        // SecureFlagGroup
        Map<String, String> keys = new HashMap<>();
        String keysStr = "";
        if (mucRoom.getIsSecretGroup() == 1) {
            for (int i = 0; i < realInviteUsers.size(); i++) {
                Friend inviteUser = FriendDao.getInstance().getFriend(mLoginUserId, realInviteUsers.get(i));
                String chatKeyGroup = RSA.encryptBase64(chatKey.getBytes(),
                        Base64.decode(inviteUser.getPublicKeyRSARoom()));
                keys.put(realInviteUsers.get(i), chatKeyGroup);
            }
            keysStr = JSON.toJSONString(keys);
        }
        inviteFriend(JSON.toJSONString(realInviteUsers), keysStr, mucRoom);
    }

    /**
     * 邀请好友
     */
    private void inviteFriend(String text, String keysStr, MucRoom mucRoom) {
        if (mSelectPositions.size() <= 0) {
            start(mucRoom.getJid(), mucRoom.getName());
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mucRoom.getId());
        params.put("text", text);
        // SecureFlagGroup
        params.put("isSecretGroup", String.valueOf(mucRoom.getIsSecretGroup()));
        if (mucRoom.getIsSecretGroup() == 1) {
            params.put("keys", keysStr);
        }
        params.put("version", String.valueOf(1));
        params.put(AppConstant.GROUP_ADD_STYLE, AppConstant.GROUP_INVITE_FRIEND);

        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<InviteResult>(InviteResult.class) {

                    @Override
                    public void onResponse(ObjectResult<InviteResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            InviteResult inviteResult = result.getData();
                            List<String> dataVerify = inviteResult.getUserIds();
                            if (dataVerify != null && dataVerify.size() > 0) {
                                if (!TextUtils.isEmpty(inviteResult.getMsg())) {
                                    // 数组不为空，理论上msg也不会为空，如果空了，基本为服务端的问题
                                    // 转换为提示消息，告知邀请者
                                    MessageUtil.generateTipMessage(mLoginUserId, mucRoom.getJid(), inviteResult.getMsg(), true);
                                }
                            }
                        }
                        setResult(RESULT_OK);
                        start(mucRoom.getJid(), mucRoom.getName());
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void start(String jid, String name) {
        // 进入群聊界面，结束当前的界面
        Intent intent = new Intent(SelectContactsActivity.this, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, jid);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, name);
        intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);
        finish();
    }

    private class ListViewAdapter extends BaseAdapter implements SectionIndexer {
        List<BaseSortModel<Friend>> mSortFriends;

        public ListViewAdapter() {
            mSortFriends = new ArrayList<>();
        }

        public void setData(List<BaseSortModel<Friend>> sortFriends) {
            mSortFriends = sortFriends;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSortFriends.size();
        }

        @Override
        public Object getItem(int position) {
            return mSortFriends.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_contacts, parent, false);
            }
            TextView catagoryTitleTv = ViewHolder.get(convertView, R.id.catagory_title);
            View view_bg_friend = ViewHolder.get(convertView, R.id.view_bg_friend);
            CheckBox checkBox = ViewHolder.get(convertView, R.id.check_box);
            ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
            TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);

            // 根据position获取分类的首字母的Char ascii值
            int section = getSectionForPosition(position);
            // 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == getPositionForSection(section)) {
                catagoryTitleTv.setVisibility(View.VISIBLE);
                view_bg_friend.setVisibility(View.GONE);
                catagoryTitleTv.setText(mSortFriends.get(position).getFirstLetter());
            } else {
                view_bg_friend.setVisibility(View.VISIBLE);
                catagoryTitleTv.setVisibility(View.GONE);
            }
            Friend friend = mSortFriends.get(position).getBean();
            if (friend != null) {
                AvatarHelper.getInstance().displayAvatar(friend.getUserId(), avatarImg, true);
                userNameTv.setText(TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName());
                checkBox.setChecked(false);
                ColorStateList tabColor = SkinUtils.getSkin(SelectContactsActivity.this).getTabColorState();
                if (friend.getStatus() == 100) {
                    checkBox.setChecked(true);
                    Drawable drawable = getResources().getDrawable(R.drawable.sel_check_wx2);
                    drawable = DrawableCompat.wrap(drawable);
                    DrawableCompat.setTintList(drawable, tabColor);
                    checkBox.setButtonDrawable(drawable);
                } else {
                    checkBox.setChecked(false);
                    checkBox.setButtonDrawable(getResources().getDrawable(R.drawable.sel_nor_wx2));
                }

                // 快速建群，添加两行item项，默认选中且不可点击
                if (mQuicklyCreate) {
                    if (friend.getUserId().equals(mLoginUserId) || friend.getUserId().equals(mQuicklyId)) {
                        checkBox.setChecked(true);
                        Drawable drawable = getResources().getDrawable(R.drawable.sel_check_wx2);
                        drawable = DrawableCompat.wrap(drawable);
                        DrawableCompat.setTintList(drawable, tabColor);
                        checkBox.setButtonDrawable(drawable);
                    }
                }
            }
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return null;
        }

        @Override
        public int getPositionForSection(int section) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mSortFriends.get(i).getFirstLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == section) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mSortFriends.get(position).getFirstLetter().charAt(0);
        }
    }

    private class HorListViewAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSelectPositions.size();
        }

        @Override
        public Object getItem(int position) {
            return mSelectPositions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new CircleImageView(mContext);
                int size = DisplayUtil.dip2px(mContext, 37);
                AbsListView.LayoutParams param = new AbsListView.LayoutParams(size, size);
                convertView.setLayoutParams(param);
            }
            ImageView imageView = (ImageView) convertView;
            String selectPosition = mSelectPositions.get(position);
            AvatarHelper.getInstance().displayAvatar(selectPosition, imageView, true);
            return convertView;
        }
    }
}
