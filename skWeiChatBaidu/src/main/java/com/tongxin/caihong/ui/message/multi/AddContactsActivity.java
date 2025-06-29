package com.tongxin.caihong.ui.message.multi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import com.alibaba.fastjson.TypeReference;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.InviteResult;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.MessageUtil;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.sortlist.BaseComparator;
import com.tongxin.caihong.sortlist.BaseSortModel;
import com.tongxin.caihong.sortlist.SideBar;
import com.tongxin.caihong.sortlist.SortHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.BroadcastHelper;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.JsonUtils;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.util.secure.RSA;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;
import com.tongxin.caihong.view.CircleImageView;
import com.tongxin.caihong.view.HorizontalListView;
import com.tongxin.caihong.view.VerifyDialog;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;

import static com.tongxin.caihong.broadcast.MsgBroadcast.EXTRA_ENABLED;

/**
 * 群组邀请好友
 */
public class AddContactsActivity extends BaseActivity {
    private EditText mEditText;
    private boolean isSearch;

    private SideBar mSideBar;
    private TextView mTextDialog;
    private ListView mListView;
    private ListViewAdapter mAdapter;
    private List<BaseSortModel<Friend>> mSortFriends;
    private List<BaseSortModel<Friend>> mSearchSortFriends;
    private BaseComparator<Friend> mBaseComparator;

    private HorizontalListView mHorizontalListView;
    private HorListViewAdapter mHorAdapter;
    private List<Friend> mFriendSearch;
    private List<String> mSelectPositions;
    private Button mOkBtn;

    private String mLoginUserId;
    private Friend mFriend;
    private String mRoomId;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), MsgBroadcast.ACTION_MSG_UPDATE_ROOM_INVITE)) {
                int enabled = intent.getIntExtra(EXTRA_ENABLED, -1);
                if (enabled == 0) {
                    RoomMember self = RoomMemberDao.getInstance().getSingleRoomMember(mRoomId, mLoginUserId);
                    if (!self.isGroupOwnerOrManager()) {
                        ToastUtil.showToast(mContext, getString(
                                R.string.tip_disallow_invite_role_place_holder,
                                getString(MucRoomMember.getRoleName(self.getRole()))
                        ));
                        finish();
                    }
                }
            }
        }
    };
    private String mRoomJid;
    private String mRoomDes;
    private String mRoomName;
    private Set<String> mExistIds;
    // Todo 邀请成员 是否开启群主验证 2018.5.16
    private String mCreator;
    private VerifyDialog verifyDialog;
    // SecureFlagGroup 是否为私密群组，如果为私密群组，没有rsa公钥的好友不支持选中，上传时还需要与userId对用的chatKey
    private boolean isSecretGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contacts);
        if (getIntent() != null) {
            mRoomId = getIntent().getStringExtra("roomId");
            mRoomJid = getIntent().getStringExtra("roomJid");
            mRoomDes = getIntent().getStringExtra("roomDes");
            mRoomName = getIntent().getStringExtra("roomName");
            String ids = getIntent().getStringExtra("exist_ids");
            mExistIds = JSON.parseObject(ids, new TypeReference<Set<String>>() {
            }.getType());
            mCreator = getIntent().getStringExtra("roomCreator");
        }
        mLoginUserId = coreManager.getSelf().getUserId();
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mRoomJid);
        isSecretGroup = mFriend.getIsSecretGroup() == 1;

        mSortFriends = new ArrayList<>();
        mSearchSortFriends = new ArrayList<>();
        mBaseComparator = new BaseComparator<>();
        mAdapter = new ListViewAdapter();

        mSelectPositions = new ArrayList<>();
        mHorAdapter = new HorListViewAdapter();

        initActionBar();
        initView();
        BroadcastHelper.register(this, receiver, MsgBroadcast.ACTION_MSG_UPDATE_ROOM_INVITE);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.select_contacts));
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.list_view);
        mHorizontalListView = (HorizontalListView) findViewById(R.id.horizontal_list_view);
        mListView.setAdapter(mAdapter);
        mHorizontalListView.setAdapter(mHorAdapter);
        mOkBtn = (Button) findViewById(R.id.ok_btn);
        ButtonColorChange.colorChange(mContext, mOkBtn);
        mSideBar = (SideBar) findViewById(R.id.sidebar);
        mSideBar.setVisibility(View.VISIBLE);
        mTextDialog = (TextView) findViewById(R.id.text_dialog);
        mSideBar.setTextView(mTextDialog);
        mSideBar.setOnTouchingLetterChangedListener(s -> {
            // 该字母首次出现的位置
            int position = mAdapter.getPositionForSection(s.charAt(0));
            if (position != -1) {
                mListView.setSelection(position);
            }
        });

        /**
         * 群内邀请好友搜索功能
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

            if (isSecretGroup && TextUtils.isEmpty(friend.getPublicKeyRSARoom())) {
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

        mOkBtn.setOnClickListener(v -> {
            if (mSelectPositions.size() <= 0) {
                Toast.makeText(AddContactsActivity.this, R.string.tip_select_at_lease_one_member, Toast.LENGTH_SHORT).show();
                return;
            }


            List<String> inviteIdList = new ArrayList<>();
            List<String> inviteNameList = new ArrayList<>();
            boolean isEmity = true;
            for (String fid : mSelectPositions) {
                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, fid);
                if (friend != null) {
                    inviteIdList.add(fid);
                    inviteNameList.add(friend.getNickName());
                    isEmity = false;
                }
            }

            if (isEmity) {
                return;
            }

            // 因为ios不要这样格式["10004541","10007042"]的字符串,，为了兼容他们，我们需要另外拼接一下
            String ids = JSON.toJSONString(inviteIdList); // ["10004541","10007042"]
            String names = JSON.toJSONString(inviteNameList); // ["haha","ccc"]
            final String ios_ids = ids.substring(1, ids.length() - 1).replace("\"", ""); // 10004541,10007042
            final String ios_name = names.substring(1, names.length() - 1).replace("\"", ""); // haha,ccc

            boolean isNeedOwnerAllowInviteFriend = PreferenceUtils.getBoolean(mContext, Constants.IS_NEED_OWNER_ALLOW_NORMAL_INVITE_FRIEND + mRoomJid, false);

            if (isNeedOwnerAllowInviteFriend) {// 群主开启了'群聊邀请确认'功能(需要群主确认进群)
                if (mLoginUserId.equals(mCreator)) {// 我为群主，直接邀请
                    inviteFriend(ids);
                } else {
                    verifyDialog = new VerifyDialog(AddContactsActivity.this);
                    verifyDialog.setVerifyClickListener(MyApplication.getContext().getString(R.string.title_join_group_verify)
                            , MyApplication.getContext().getString(R.string.tip_reason_invite_friends)
                            , ""
                            , new VerifyDialog.VerifyClickListener() {
                                @Override
                                public void cancel() {

                                }

                                @Override
                                public void send(String str) {
                                    List<RoomMember> roomMemberList = RoomMemberDao.getInstance().getRoomMemberAndAdminForList(mRoomId);
                                    ChatMessage message = new ChatMessage();
                                    for (RoomMember member : roomMemberList) {
                                            Log.e("角色", "用户角色为1或者2"+member.getUserName().toString());
                                            // 给群主发送一条单聊消息
                                            message.setType(XmppMessage.TYPE_GROUP_VERIFY);
                                            message.setFromUserId(mLoginUserId);
                                            message.setFromUserName(coreManager.getSelf().getNickName());
                                            message.setToUserId(member.getUserId());
                                            String s = JsonUtils.initJsonContent(ios_ids, ios_name, mRoomJid, "0", str);
                                            message.setObjectId(s);
                                            message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                            message.setTimeSend(TimeUtils.sk_time_current_time());
                                            coreManager.sendChatMessage(member.getUserId(), message);
                                    }
                                    ChatMessage chatMessage = message.clone(false);
                                    chatMessage.setType(XmppMessage.TYPE_TIP);
                                    chatMessage.setContent(getString(R.string.tip_send_reason_success));
                                    if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mRoomJid, chatMessage)) {
                                        ListenerManager.getInstance().notifyNewMesssage(mLoginUserId, mRoomJid, chatMessage, true);
                                    }
                                    finish();
                                }
                            });
                    verifyDialog.show();
                }
            } else {// 直接邀请
                inviteFriend(ids);
            }
        });

        loadData();
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
            final List<Friend> friends = FriendDao.getInstance().getAllFriends(mLoginUserId);
            Map<String, Integer> existMap = new HashMap<>();
            List<BaseSortModel<Friend>> sortedList = SortHelper.toSortedModelList(friends, existMap, f -> {
                if (isExist(f)) {
                    // 过滤掉已经在群里的，
                    return null;
                } else {
                    return f.getShowName();
                }
            });
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                mSideBar.setExistMap(existMap);
                mSortFriends = sortedList;
                mAdapter.setData(sortedList);
            });
        });
    }

    /**
     * 是否存在已经在那个房间的好友
     */
    private boolean isExist(Friend friend) {
        return mExistIds.contains(friend.getUserId());
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

    /**
     * 邀请好友
     */
    private void inviteFriend(String inviteUsers) {
        List<String> strings;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("text", inviteUsers);
        // todo SecureFlagGroup
        params.put("isSecretGroup", String.valueOf(mFriend.getIsSecretGroup()));
        strings = JSON.parseArray(inviteUsers, String.class);
        if (isSecretGroup) {
            Map<String, String> keys = new HashMap<>();
            String keysStr;
            String chatKey = SecureChatUtil.decryptChatKey(mFriend.getUserId(), mFriend.getChatKeyGroup());
            for (int i = 0; i < strings.size(); i++) {
                Friend inviteUser = FriendDao.getInstance().getFriend(mLoginUserId, strings.get(i));
                String chatKeyGroup = RSA.encryptBase64(chatKey.getBytes(),
                        Base64.decode(inviteUser.getPublicKeyRSARoom()));
                keys.put(strings.get(i), chatKeyGroup);
            }
            keysStr = JSON.toJSONString(keys);
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
                        if (Result.checkSuccess(AddContactsActivity.this, result)) {
                            InviteResult inviteResult = result.getData();
                            /**
                             * 处理邀请之前就已经在群组内的用户
                             * 出现此问题的原因还是因为群成员分页，导致本地群成员表并不是完整的，以至于本地可以邀请群成员表之外的好友
                             */
                            List<String> dataExist = inviteResult.getFailList();
                            if (dataExist != null && dataExist.size() > 0) {
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < mSortFriends.size(); i++) {
                                    for (int i1 = 0; i1 < dataExist.size(); i1++) {
                                        if (TextUtils.equals(mSortFriends.get(i).getBean().getUserId(), dataExist.get(i1))) {
                                            builder.append(!TextUtils.isEmpty(mSortFriends.get(i).getBean().getRemarkName())
                                                    ? mSortFriends.get(i).getBean().getRemarkName() : mSortFriends.get(i).getBean().getNickName() + ",");
                                        }
                                    }
                                }
                                ToastUtil.showToast(mContext, getString(R.string.exist_in_group, builder.toString()));
                            }
                            /**
                             * 处理开启邀请入群验证的用户
                             */
                            List<String> dataVerify = inviteResult.getUserIds();
                            if (dataVerify != null && dataVerify.size() > 0) {
                                if (!TextUtils.isEmpty(inviteResult.getMsg())) {
                                    // 数组不为空，理论上msg也不会为空，如果空了，基本为服务端的问题
                                    // 转换为提示消息，告知邀请者
                                    MessageUtil.generateTipMessage(mLoginUserId, mRoomJid, inviteResult.getMsg(), true);
                                }
                                // 将这些用户移掉，不存群成员表
                                strings.removeAll(dataVerify);
                            }

                            // 邀请成功之后，将被邀请的好友加入群成员表内
                            List<RoomMember> roomMemberList = new ArrayList<>();
                            for (int i = 0; i < strings.size(); i++) {
                                Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, strings.get(i));
                                if (friend == null) {
                                    continue;// 理论上不可能
                                }
                                RoomMember roomMember = new RoomMember();
                                roomMember.setRoomId(mRoomId);
                                roomMember.setUserId(friend.getUserId());
                                roomMember.setUserName(friend.getNickName());
                                roomMember.setCardName(friend.getNickName());
                                roomMember.setRole(3);// 刚刚邀请进来的都是普通身份
                                roomMember.setCreateTime((int) TimeUtils.sk_time_current_time());
                                roomMemberList.add(roomMember);
                            }
                            AsyncUtils.doAsync(this, mucChatActivityAsyncContext -> {
                                for (int i = 0; i < roomMemberList.size(); i++) {// 在异步任务内存储
                                    RoomMemberDao.getInstance().saveSingleRoomMember(mRoomId, roomMemberList.get(i));
                                }
                            });
                            // ToastUtil.showToast(mContext, getString(R.string.invite_success));
                            setResult(RESULT_OK);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    class ListViewAdapter extends BaseAdapter implements SectionIndexer {
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
            CheckBox checkBox = ViewHolder.get(convertView, R.id.check_box);
            ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
            TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);
            View view = ViewHolder.get(convertView, R.id.view_bg_friend);
            // 根据position获取分类的首字母的Char ascii值
            int section = getSectionForPosition(position);
            // 如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == getPositionForSection(section)) {
                catagoryTitleTv.setVisibility(View.VISIBLE);
                catagoryTitleTv.setText(mSortFriends.get(position).getFirstLetter());
                view.setVisibility(View.GONE);
            } else {
                catagoryTitleTv.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
            }
            Friend friend = mSortFriends.get(position).getBean();
            if (friend != null) {
                AvatarHelper.getInstance().displayAvatar(friend.getUserId(), avatarImg, true);
                userNameTv.setText(TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName());
                ColorStateList tabColor = SkinUtils.getSkin(AddContactsActivity.this).getTabColorState();
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

    class HorListViewAdapter extends BaseAdapter {
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
