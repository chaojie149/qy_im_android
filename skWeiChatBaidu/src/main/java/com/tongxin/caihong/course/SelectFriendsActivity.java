package com.tongxin.caihong.course;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.InstantMessageConfirm;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.MessageAvatar;
import com.tongxin.caihong.view.SearchHeader;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * message目录下有一个InstantMessageActivity SelectNewContactsActivity SelectNewGroupInstantActivity[消息转发用]
 * share目录下有一个ShareNearChatFriend ShareNewFriend ShareNewGroup [集成shareSdk的应用分享用]
 * systemshare目录下也有一个ShareNearChatFriend ShareNewFriend ShareNewGroup[系统分享用]
 * courser目录有一个SelectFriendsActivity SelectNewContactsActivity SelectNewGroupActivity[发送讲课用]
 * 分别为[类分别为最近联系人列表，好友列表，群组列表]
 * todo 比较混乱，感觉其实都可以复用，不过逻辑会复杂一点
 * <p>
 * InstantMessageActivity | share.ShareNearChatFriend | systemshare.ShareNearChatFriend |SelectFriendsActivity
 * all user local adapter, adapter all user item_recently_contacts.xml
 * <p>
 * other all user FriendSortAdapter
 */
public class SelectFriendsActivity extends BaseActivity {
    // 因为LocalCourseActivity与CourseDetail内的mHandle/Evnetbus处理的是同样的消息，所以需要区分
    public static boolean isIntentLocalCourseActivity;
    private ListView mLvRecentlyMessage;
    private List<Friend> mFriends;
    private MessageRecentlyAdapter messageRecentlyAdapter;
    private InstantMessageConfirm menuWindow;
    private String mLoginUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messageinstant);
        mLoginUserId = coreManager.getSelf().getUserId();

        EventBusHelper.register(this);
        initActionBar();
        loadData();
        initView();
        initSearch();
    }

    private void initSearch() {
        SearchHeader shSearch = findViewById(R.id.shSearch);
        shSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    messageRecentlyAdapter.setData(mFriends);
                } else {
                    messageRecentlyAdapter.setData(search(mFriends, s.toString().trim().toLowerCase()));
                }
            }
        });
    }

    private List<Friend> search(List<Friend> data, String key) {
        List<Friend> ret = new ArrayList<>();
        for (Friend friend : data) {
            if (friend.getShowName().toLowerCase().contains(key)) {
                ret.add(friend);
            }
        }
        return ret;
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
        tvTitle.setText(getString(R.string.most_recent_contact));
    }

    private void loadData() {
        mFriends = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
        List<Friend> disableList = new ArrayList<>();
        for (int i = 0; i < mFriends.size(); i++) {
            if (mFriends.get(i).getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)
                    || mFriends.get(i).getUserId().equals(Friend.ID_SK_PAY)
                    || mFriends.get(i).getIsDevice() == 1) {
                disableList.add(mFriends.get(i));
            }
        }
        mFriends.removeAll(disableList);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, EventNotifyByTag.FinishInstantRelatedActivity)) {
            finish();
        }
    }

    private void initView() {
        findViewById(R.id.tv_create_newmessage).setOnClickListener(v -> {
            startActivity(new Intent(mContext, SelectNewContactsActivity.class));
        });
        findViewById(R.id.added_layout).setVisibility(View.GONE);
        mLvRecentlyMessage = (ListView) findViewById(R.id.lv_recently_message);
        messageRecentlyAdapter = new MessageRecentlyAdapter(mFriends);
        mLvRecentlyMessage.setAdapter(messageRecentlyAdapter);
        mLvRecentlyMessage.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                Friend friend = messageRecentlyAdapter.getData().get(position);
                showPopuWindow(view, friend);
            }
        });
    }

    private void showPopuWindow(View view, Friend friend) {
        if (menuWindow != null) {
            menuWindow.dismiss();
        }
        menuWindow = new InstantMessageConfirm(SelectFriendsActivity.this, new ClickListener(friend), friend);
        menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    /**
     * 获取自己在该群组的信息(职位、昵称、禁言时间等)以及群属性
     */
    private void isSupportSend(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", friend.getRoomId());

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// 数据结果与room/get接口一样，只是服务端没有返回群成员列表的数据
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();
                                     if (mucRoom.getMember() == null) {// 被踢出该群组
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 1);// 更新本地群组状态
                                         DialogHelper.tip(SelectFriendsActivity.this, getString(R.string.tip_forward_kick));
                                     } else {// 正常状态
                                         int role = mucRoom.getMember().getRole();
                                         // 更新禁言状态
                                         FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, mucRoom.getJid(), mucRoom.getMember().getTalkTime());

                                         // 更新部分群属性
                                         MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(),
                                                 mucRoom.getAllowSendCard(), mucRoom.getAllowConference(),
                                                 mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime(), mucRoom.getAllowOpenLive());

                                         // 更新个人职位
                                         RoomMemberDao.getInstance().updateRoomMemberRole(mucRoom.getId(), mLoginUserId, role);

                                         if (role == 4) {
                                             DialogHelper.tip(mContext, getString(R.string.hint_invisible));
                                             return;
                                         }
                                         if (role == 1 || role == 2) {// 群组或管理员 直接转发出去
                                             sendStep(friend);
                                         } else {
                                             if (mucRoom.getTalkTime() > 0) {// 全体禁言
                                                 DialogHelper.tip(SelectFriendsActivity.this, getString(R.string.tip_now_ban_all));
                                             } else if ((mucRoom.getMember().getTalkTime() > System.currentTimeMillis() / 1000)
                                                     || (mucRoom.getMember().getTalkTime() == -1)) {// 禁言
                                                 DialogHelper.tip(SelectFriendsActivity.this, getString(R.string.tip_forward_ban));
                                             } else if (mucRoom.getAllowSpeakCourse() == 0) {// 禁止发送讲课
                                                 DialogHelper.tip(SelectFriendsActivity.this, getString(R.string.tip_disabled_send_cource));
                                             } else if (mucRoom.getMember().disallowPublicAction()) {
                                                 DialogHelper.tip(SelectFriendsActivity.this,
                                                         getString(R.string.tip_action_disallow_place_holder, getString(mucRoom.getMember().getRoleName())));
                                             } else {
                                                 sendStep(friend);
                                             }
                                         }
                                     }
                                 } else if (Result.checkError(result, Result.CODE_ROOM_GONE)) {
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, friend.getUserId(), 2);// 更新本地群组状态
                                     DialogHelper.tip(SelectFriendsActivity.this, getString(R.string.tip_forward_disbanded));
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

    private void send(Friend friend) {
        menuWindow.dismiss();

        if (Constants.IS_SENDONG_COURSE_NOW) {
            DialogHelper.tip(SelectFriendsActivity.this, getString(R.string.send_course_wait));
            return;
        }

        if (friend.getRoomFlag() != 0) {// 群组
            if (friend.getIsLostChatKeyGroup() == 1) {
                ToastUtil.showToast(mContext, getString(R.string.is_lost_key_cannot_support_send_msg, friend.getNickName()));
                return;
            }
            isSupportSend(friend);
            return;
        }
        sendStep(friend);
    }

    private void sendStep(Friend friend) {
        Constants.IS_SENDONG_COURSE_NOW = true;

        EventBus.getDefault().post(new EventSendCourse(friend.getUserId(), friend.getRoomFlag() != 0));
        finish();
    }

    /**
     * 事件的监听
     */
    class ClickListener implements OnClickListener {
        private Friend friend;

        public ClickListener(Friend friend) {
            this.friend = friend;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_send:
                    // 发送
                    send(friend);
                    break;
                case R.id.btn_cancle:
                    // 取消
                    menuWindow.dismiss();
                    break;
                default:
                    break;
            }
        }
    }

    class MessageRecentlyAdapter extends BaseAdapter {

        private List<Friend> mFriends;

        public MessageRecentlyAdapter(List<Friend> data) {
            this.mFriends = data;
        }

        public List<Friend> getData() {
            return mFriends;
        }

        public void setData(List<Friend> data) {
            this.mFriends = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mFriends != null) {
                return mFriends.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mFriends != null) {
                return mFriends.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (mFriends != null) {
                return position;
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(SelectFriendsActivity.this, R.layout.item_recently_contacts, null);
                holder = new ViewHolder();
                holder.mIvHead = (MessageAvatar) convertView.findViewById(R.id.iv_recently_contacts_head);
                holder.mTvName = (TextView) convertView.findViewById(R.id.tv_recently_contacts_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Friend friend = mFriends.get(position);
            holder.mIvHead.fillData(friend);
            holder.mTvName.setText(TextUtils.isEmpty(friend.getRemarkName())
                    ? friend.getNickName() : friend.getRemarkName());
            return convertView;
        }
    }

    class ViewHolder {
        MessageAvatar mIvHead;
        TextView mTvName;
    }
}
