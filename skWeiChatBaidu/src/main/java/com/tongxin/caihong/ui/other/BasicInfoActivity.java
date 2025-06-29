package com.tongxin.caihong.ui.other;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.AddAttentionResult;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Label;
import com.tongxin.caihong.bean.PublicNumDescription;
import com.tongxin.caihong.bean.Report;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.EventSyncFriendOperating;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.MucRoomMember;
import com.tongxin.caihong.bean.message.NewFriendMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.LabelDao;
import com.tongxin.caihong.db.dao.NewFriendDao;
import com.tongxin.caihong.db.dao.UserAvatarDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.FriendHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.BusinessCircleActivity;
import com.tongxin.caihong.ui.map.MapActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.single.SetRemarkActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.BasicInfoWindow;
import com.tongxin.caihong.view.ChatTextClickPpWindow;
import com.tongxin.caihong.view.CircleImageView;
import com.tongxin.caihong.view.FoldTextView;
import com.tongxin.caihong.view.NoDoubleClickListener;
import com.tongxin.caihong.view.ReportDialog;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.xmpp.listener.NewFriendListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

/**
 * 基本信息next_step_btn
 */
public class BasicInfoActivity extends BaseActivity implements NewFriendListener {
    public static final String KEY_FROM_ADD_TYPE = "KEY_FROM_ADD_TYPE";
    public static final int FROM_ADD_TYPE_QRCODE = 1;
    public static final int FROM_ADD_TYPE_CARD = 2;
    public static final int FROM_ADD_TYPE_GROUP = 3;
    public static final int FROM_ADD_TYPE_PHONE = 4;
    public static final int FROM_ADD_TYPE_NAME = 5;
    public static final int FROM_ADD_TYPE_OTHER = 6;
    private static final int REQUEST_CODE_SET_REMARK = 475;
    private static String sCurrentUserId;
    private String fromAddType;
    private String mUserId;
    private String mLoginUserId;
    private boolean isMyInfo = false;
    private User mUser;
    private Friend mFriend;
    private ImageView ivRight;
    private BasicInfoWindow menuWindow;
    private ImageView mAvatarImg;
    private TextView tv_remarks;
    private ImageView iv_remarks;
    private LinearLayout ll_nickname;
    private TextView tv_name_basic;
    private TextView tv_communication;
    private TextView tv_number;
    private LinearLayout ll_place;
    private TextView tv_place;
    private TextView photo_tv;
    private RelativeLayout photo_rl;
    private TextView tv_setting_name;
    private TextView tv_lable_basic;
    private RelativeLayout rl_describe;
    private TextView tv_describe_basic;
    private ListView lvTelephone;
    private TextView birthday_tv;
    private TextView online_tv;
    private RelativeLayout online_rl;
    private RelativeLayout look_location_rl;
    private RelativeLayout erweima;
    private Button mNextStepBtn;
    /**
     * Todo All NewFriendMessage packetId
     */
    private String addhaoyouid = null;
    private String addblackid = null;
    private String removeblack = null;
    private String deletehaoyou = null;
    private int isyanzheng = 0;// 该好友是否需要验证
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.set_remark_nameS:
                    start();
                    break;
                case R.id.add_blacklist:
                    // 加入黑名单
                    showBlacklistDialog();
                    break;
                case R.id.remove_blacklist:
                    // 移除黑名单
                    removeBlacklist();
                    break;
                case R.id.delete_tv:
                    // 彻底删除
                    showDeleteAllDialog(mFriend, false);
                    break;
                case R.id.report_tv:
                    ReportDialog mReportDialog = new ReportDialog(BasicInfoActivity.this, false, new ReportDialog.OnReportListItemClickListener() {
                        @Override
                        public void onReportItemClick(Report report) {
                            report(mUserId, report);
                        }
                    });
                    mReportDialog.show();
                    break;
            }

        }
    };
    private TextView go_publish_tv;
    private TextView deleete_publish_tv;
    private View view;
    private RelativeLayout rl_description;
    private TextView tv_description;
    private FoldTextView tv_public_de;
    // 是否为群组内点击查看，如为群组内点击查看且为群主或群管理员，显示入群方式
    private TextView tvAddGroupStyle;
    private boolean fromGroupSee;
    private String fromGroupId;
    private boolean loading = false;

    public static void start(Context ctx, String userId) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        if (TextUtils.equals(userId, sCurrentUserId)) {
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
        }
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, String userId, int fromAddType) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(KEY_FROM_ADD_TYPE, String.valueOf(fromAddType));
        if (TextUtils.equals(userId, sCurrentUserId)) {
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
        }
        ctx.startActivity(intent);
    }

    /**
     * @param ctx
     * @param userId
     * @param fromAddType
     * @param fromGroupSee 是否为群组内点击查看，如为群组内点击查看且为群主或群管理员，显示入群方式
     */
    public static void start(Context ctx, String userId, int fromAddType, boolean fromGroupSee, String fromGroupId) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(KEY_FROM_ADD_TYPE, String.valueOf(fromAddType));
        intent.putExtra("fromGroupSee", fromGroupSee);
        intent.putExtra("fromGroupId", fromGroupId);
        ctx.startActivity(intent);
    }

    /**
     * 改变屏幕背景色
     */
    public void darkenBackground(Float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);
    }

    private void start() {
        String name = "";
        String desc = "";
        String phoneRemark = "";
        if (mUser != null && mUser.getFriends() != null) {
            name = mUser.getFriends().getRemarkName();
            desc = mUser.getFriends().getDescribe();
            phoneRemark = mUser.getFriends().getPhoneRemark();
        }
        SetRemarkActivity.startForResult(BasicInfoActivity.this, mUserId,
                name, desc, phoneRemark, REQUEST_CODE_SET_REMARK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info_new);
        if (getIntent() != null) {
            mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            fromAddType = getIntent().getStringExtra(KEY_FROM_ADD_TYPE);
            fromGroupSee = getIntent().getBooleanExtra("fromGroupSee", false);
            fromGroupId = getIntent().getStringExtra("fromGroupId");
        }

        mLoginUserId = coreManager.getSelf().getUserId();
        if (TextUtils.isEmpty(mUserId)) {
            mUserId = mLoginUserId;
        }
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        initActionBar();
        initView();
        initEvent();

        if (mLoginUserId.equals(mUserId)) { // 显示自己资料
            isMyInfo = true;
            loadMyInfoFromDb();
        } else { // 显示其他用户的资料
            isMyInfo = false;
            if (Friend.ID_SYSTEM_MESSAGE.equals(mUserId)
                    || (mFriend != null && mFriend.getStatus() == Friend.STATUS_SYSTEM)) {
                // 10000号一定是公众号，先改了ui避免http太慢导致画面先显示普通用户信息页，
                findViewById(R.id.part_1).setVisibility(View.VISIBLE);
                findViewById(R.id.part_2).setVisibility(View.GONE);
            }
            loadOthersInfoFromNet();
        }

        // 自己不弹dialog
        if (isMyInfo) {
            ivRight.setVisibility(View.GONE);
        }
        if (fromGroupSee) {
            getMemberAddGroupStyle();
        }

        ListenerManager.getInstance().addNewFriendListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sCurrentUserId = mLoginUserId;
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
        tvTitle.setText(getString(R.string.base_info));

        ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.mipmap.folding_icon);
    }

    private void initView() {
        UsernameHelper.initTextView(findViewById(R.id.photo_text), coreManager.getConfig().registerUsername);
        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        tv_remarks = (TextView) findViewById(R.id.tv_remarks);
        iv_remarks = findViewById(R.id.iv_remarks);
        ll_nickname = findViewById(R.id.ll_nickname);
        tv_name_basic = (TextView) findViewById(R.id.tv_name_basic);
        tv_communication = (TextView) findViewById(R.id.tv_communication);
        tv_number = (TextView) findViewById(R.id.tv_number);
        ll_place = findViewById(R.id.ll_place);
        tv_place = (TextView) findViewById(R.id.tv_place);

        tv_setting_name = findViewById(R.id.tv_setting_name);
        tv_lable_basic = findViewById(R.id.tv_lable_basic);
        rl_describe = findViewById(R.id.rl_describe);
        tv_describe_basic = findViewById(R.id.tv_describe_basic);
        lvTelephone = findViewById(R.id.lvTelephone);
        birthday_tv = (TextView) findViewById(R.id.birthday_tv);
        online_rl = (RelativeLayout) findViewById(R.id.online_rl);
        online_tv = (TextView) findViewById(R.id.online_tv);
        erweima = (RelativeLayout) findViewById(R.id.erweima);
        look_location_rl = (RelativeLayout) findViewById(R.id.look_location_rl);
        photo_tv = findViewById(R.id.photo_tv);
        photo_rl = (RelativeLayout) findViewById(R.id.photo_rl);
        rl_description = findViewById(R.id.rl_description);
        tv_description = findViewById(R.id.tv_description);
        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
        ButtonColorChange.colorChange(this, mNextStepBtn);

        mNextStepBtn.setText(getString(R.string.sendmseeage));
    }

    private void initEvent() {
        ivRight.setOnClickListener(view -> {
            if (mUser != null) {
                menuWindow = new BasicInfoWindow(BasicInfoActivity.this, itemsOnClick, mUser);
                // 显示窗口
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // +x右,-x左,+y下,-y上
                // pop向左偏移显示
                menuWindow.showAsDropDown(view,
                        -(menuWindow.getContentView().getMeasuredWidth() - view.getWidth() / 2 - 40),
                        0);
                darkenBackground(0.6f);
                menuWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        darkenBackground(1f);
                    }
                });
            }
        });

        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                intent.putExtra(AppConstant.EXTRA_IMAGE_URI, mUserId);
                startActivity(intent);
            }
        });

        findViewById(R.id.rn_rl).setOnClickListener(v -> {
            start();
        });

        rl_describe.setOnClickListener(v -> {
            start();
        });

        findViewById(R.id.rlTelephone).setOnClickListener(v -> {
            start();
        });

        if (mLoginUserId.equals(mUserId)) {// 自己隐藏该item
            findViewById(R.id.look_bussic_cicle_per_rl).setVisibility(View.GONE);
        }

        findViewById(R.id.look_bussic_cicle_per_rl).setOnClickListener(view -> {
            // 设置生活圈权限，
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            if (mFriend == null) {
                // 以防万一，
                ToastUtil.showToast(mContext, R.string.tip_settings_only_for_friend);
                return;
            }
            LifeCircleSettingsActivity.start(mContext, mFriend);
        });
        findViewById(R.id.look_bussic_cicle_rl).setOnClickListener(view -> {
            if (mUser != null) {
                Intent intent = new Intent(BasicInfoActivity.this, BusinessCircleActivity.class);
                intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                intent.putExtra(AppConstant.EXTRA_NICK_NAME, mUser.getNickName());
                startActivity(intent);
            }
        });

        erweima.setOnClickListener(v -> {
            if (mUser != null) {
                Intent intent = new Intent(BasicInfoActivity.this, QRcodeActivity.class);
                intent.putExtra("isgroup", false);
                if (!TextUtils.isEmpty(mUser.getAccount())) {
                    intent.putExtra("userid", mUser.getAccount());
                } else {
                    intent.putExtra("userid", mUser.getUserId());
                }
                intent.putExtra("userAvatar", mUser.getUserId());
                intent.putExtra("nickName", mUser.getNickName());
                intent.putExtra("sex", mUser.getSex());
                intent.putExtra("hiding", mUser.getHiding());
                startActivity(intent);
            }
        });

        look_location_rl.setOnClickListener(view -> {
            double latitude = 0;
            double longitude = 0;
            if (mUser != null && mUser.getLoc() != null) {
                latitude = mUser.getLoc().getLat();
                longitude = mUser.getLoc().getLng();
            }
            if (latitude == 0 || longitude == 0) {
                ToastUtil.showToast(mContext, getString(R.string.this_friend_not_open_position));
                return;
            }
            Intent intent = new Intent(mContext, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("address", mUser.getNickName());
            startActivity(intent);
        });
    }

    // 加载自己的信息
    private void loadMyInfoFromDb() {
        mUser = coreManager.getSelf();
        updateUI();
        isLocationShow();
    }

    // 加载好友的信息
    private void loadOthersInfoFromNet() {
        loadOthersInfoFromNet(true);
    }

    private void loadOthersInfoFromNet(boolean showDialog) {
        if (loading) {
            return;
        }
        loading = true;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mUserId);

        if (showDialog) {
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
        }
        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {

                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        loading = false;
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(BasicInfoActivity.this, result)) {
                            mUser = result.getData();
                            if (mUser.getUserType() != 2) {// 公众号不做该操作 否则会将公众号的status变为好友
                                // 服务器的状态 与本地状态对比
                                if (FriendHelper.updateFriendRelationship(mLoginUserId, mUser)) {
                                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                                }
                            }
                            // 重新获取一下friend对象
                            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
                            updateUI();
                            isLocationShow();// 好友的基本信息界面，必须等到mUser赋值在调用此方法
                        } else {
                            // 数据没加载出来就关闭页面吧，其他操作都没有意义，
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        loading = false;
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(mContext);
                        // 数据没加载出来就关闭页面吧，其他操作都没有意义，
                        finish();
                    }
                });
    }

    /**
     * 加载用户
     */
    private void getMemberAddGroupStyle() {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", fromGroupId);
        params.put("userId", mUserId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MEMBER_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoomMember>(MucRoomMember.class) {

                    @Override
                    public void onResponse(ObjectResult<MucRoomMember> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(BasicInfoActivity.this, result)) {
                            MucRoomMember member = result.getData();
                            tvAddGroupStyle = findViewById(R.id.tvAddGroupStyle);
                            if (!TextUtils.isEmpty(member.getInviteUserId())
                                    && !TextUtils.isEmpty(member.getInviteUserNikeName())) {
                                // 被邀请入群
                                SpannableStringBuilder spannable = new SpannableStringBuilder(member.getInviteUserNikeName() + member.getAddRoomDetails());
                                spannable.setSpan(new TextClick(member.getInviteUserId()), 0, member.getInviteUserNikeName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                tvAddGroupStyle.setMovementMethod(LinkMovementMethod.getInstance());
                                tvAddGroupStyle.setText(spannable);
                            } else {
                                tvAddGroupStyle.setText(member.getAddRoomDetails());
                            }
                            findViewById(R.id.rlAddGroupStyle).setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    private void updateFriendName(User user) {
        if (user != null) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            if (friend != null) {
                FriendDao.getInstance().updateNickName(mLoginUserId, mUserId, user.getNickName());
                // 消息列表在resume会刷新，这里通知朋友列表刷新，避免显示不一致，
                CardcastUiUpdateUtil.broadcastUpdateUi(MyApplication.getContext());
                // 刷新聊天页，
                OtherBroadcast.broadcastNameChange(MyApplication.getContext(), mUserId, user.getNickName());
            }
        }
    }

    private void publicNumbleUser(User user) {
        Log.e("zx", "publicNumbleUser: " + user.toString());
        // 客服公众号
        findViewById(R.id.part_1).setVisibility(View.VISIBLE);
        findViewById(R.id.part_2).setVisibility(View.GONE);
        go_publish_tv = findViewById(R.id.go_publish_tv);
        deleete_publish_tv = findViewById(R.id.deleete_publish_tv);
        tv_public_de = findViewById(R.id.tv_public_de);
        view = findViewById(R.id.public_view);
        if (mUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {
            ivRight.setVisibility(View.GONE);
        } else {
            CircleImageView civ_public_av = findViewById(R.id.civ_public_av);
            AvatarHelper.getInstance().displayAvatar(user.getNickName(), user.getUserId(), civ_public_av, true);
            TextView tv_public_nick = findViewById(R.id.tv_public_nick);
            tv_public_nick.setText(user.getNickName());
            PublicNumDescription publicNumDescription = new PublicNumDescription();
            publicNumDescription.setPubDescription(user.getDescription());
            tv_public_de.setExpand(publicNumDescription.isExpand()).setOnTipClickListener(new FoldTextView.onTipClickListener() {
                @Override
                public void onTipClick(boolean flag) {
                    publicNumDescription.setExpand(flag);
                }
            });
            tv_public_de.setText(publicNumDescription.getPubDescription());
        }

        if (mUser.getFriends() == null) {// 未关注
            go_publish_tv.setText(getResources().getText(R.string.guan_zhu));
            if (TextUtils.equals(mLoginUserId, mUserId)) {
                // 自己不能关注自己，
                go_publish_tv.setVisibility(View.GONE);
            }
            deleete_publish_tv.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            go_publish_tv.setOnClickListener(new AddAttentionListener());
        } else {// 已关注
            if (mFriend == null) {// 存在服务器默认帮客户端关注公众号的情况，客户端需要兼容处理
                mFriend = new Friend();
                mFriend.setOwnerId(mLoginUserId);
                mFriend.setUserId(mUser.getUserId());
                mFriend.setNickName(mUser.getNickName());
                mFriend.setRemarkName(mUser.getFriends().getRemarkName());
                mFriend.setDescription(mUser.getDescription());
                mFriend.setRoomFlag(0);
                mFriend.setStatus(Friend.STATUS_SYSTEM);
                mFriend.setService(mUser.getUserType() == 5);
                FriendDao.getInstance().createOrUpdateFriend(mFriend);
            }
            view.setVisibility(View.VISIBLE);
            go_publish_tv.setText(getResources().getText(R.string.enter_public_number));
            deleete_publish_tv.setVisibility(View.VISIBLE);
            go_publish_tv.setOnClickListener(v -> {
                if (mFriend != null) {
                    Intent intent = new Intent(BasicInfoActivity.this, ChatActivity.class);
                    intent.putExtra(ChatActivity.FRIEND, mFriend);
                    startActivity(intent);
                }
            });
            deleete_publish_tv.setOnClickListener(v -> showDeleteAllDialog(mFriend, true));
        }
    }

    private void updateUI() {
        if (mUser == null) {
            return;
        }
        if (isFinishing()) {
            return;
        }
        if (mUser.isSystem()) {
            publicNumbleUser(mUser);
        } else {
            findViewById(R.id.part_1).setVisibility(View.GONE);
            findViewById(R.id.part_2).setVisibility(View.VISIBLE);
        }
        boolean hiding = mUser.getHiding() == 1;
        if (hiding) {
            // 隐藏头像就不可点击预览大图，也没意义，
            mAvatarImg.setOnClickListener(null);
        }
        // 更新用户的头像
        AvatarHelper.updateAvatar(mUser.getUserId());
        if (!hiding) {
            displayAvatar(mUser.getUserId());
        }
        updateFriendName(mUser);
        if (!hiding) {
            tv_remarks.setText(mUser.getNickName());
            tv_name_basic.setText(mUser.getNickName());
            iv_remarks.setImageResource(mUser.getSex() == 0 ? R.mipmap.basic_famale : R.mipmap.basic_male);
        } else {
            tv_remarks.setText(Constants.HIDING_NICKNAME);
            tv_name_basic.setText(Constants.HIDING_NICKNAME);
        }
        if (TextUtils.isEmpty(mUser.getAccount())) {  //如果通讯号为空则不显示
            findViewById(R.id.ll_communication).setVisibility(View.GONE);
        } else {
            tv_number.setText(mUser.getAccount());  //否则显示通讯号
        }
        String place = Area.getProvinceCityString(mUser.getProvinceId(), mUser.getCityId());
        if (!TextUtils.isEmpty(place)) {
            ll_place.setVisibility(View.VISIBLE);
            tv_place.setText(place);
        } else {
            ll_place.setVisibility(View.GONE);
        }
        if (mUser.getFriends() != null) {
            tv_setting_name.setText(getString(R.string.setting_nickname));
            if (!TextUtils.isEmpty(mUser.getFriends().getRemarkName())) {
                tv_remarks.setText(mUser.getFriends().getRemarkName());
                ll_nickname.setVisibility(View.VISIBLE);// 有备注名，原昵称也显示
                tv_setting_name.setText(getString(R.string.tag));// 有备注 || 标签 显示“标签”
            } else {
                ll_nickname.setVisibility(View.GONE);
            }

            if (mFriend != null) {
                FriendDao.getInstance().updateFriendPartStatus(mFriend.getUserId(), mUser);

                if (!TextUtils.equals(mFriend.getRemarkName(), mUser.getFriends().getRemarkName())
                        || !TextUtils.equals(mFriend.getDescribe(), mUser.getFriends().getDescribe())) {
                    // 本地备注或者描述与服务器数据不匹配时更新数据库，
                    // mUser是接口数据，
                    mFriend.setRemarkName(mUser.getFriends().getRemarkName());
                    mFriend.setDescribe(mUser.getFriends().getDescribe());
                    FriendDao.getInstance().updateRemarkNameAndDescribe(coreManager.getSelf().getUserId(),
                            mUserId, mUser.getFriends().getRemarkName(),
                            mUser.getFriends().getDescribe());
                    // 刷新消息、通讯录、聊天页面
                    MsgBroadcast.broadcastMsgUiUpdate(mContext);
                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                    OtherBroadcast.broadcastNameChange(mContext, mUserId, mUser.getNickName());
                    OtherBroadcast.broadcastRemarkChange(mContext, mUserId, mUser.getFriends().getRemarkName());
                }
            } else {
                tv_setting_name.setText(getString(R.string.tip_set_remark));
            }
        } else {
            tv_setting_name.setText(getString(R.string.tip_set_remark));
            ll_nickname.setVisibility(View.GONE);
        }

        List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mUserId);
        String labelNames = "";
        if (friendLabelList != null && friendLabelList.size() > 0) {
            for (int i = 0; i < friendLabelList.size(); i++) {
                if (i == friendLabelList.size() - 1) {
                    labelNames += friendLabelList.get(i).getGroupName();
                } else {
                    labelNames += friendLabelList.get(i).getGroupName() + "，";
                }
            }
            tv_setting_name.setText(getString(R.string.tag));// 有备注 || 标签 显示“标签”
            tv_lable_basic.setText(labelNames);
        }
        if (mUser.getFriends() != null && !TextUtils.isEmpty(mUser.getFriends().getDescribe())) {
            rl_describe.setVisibility(View.VISIBLE);
            tv_describe_basic.setText(mUser.getFriends().getDescribe());
        } else {
            rl_describe.setVisibility(View.GONE);
        }
       // findViewById(R.id.birthday_rl).setVisibility(View.GONE);
//隐藏手机号码、出生日期
      //  findViewById(R.id.photo_rl).setVisibility(View.GONE);

        if (mUser.getFriends() != null && !TextUtils.isEmpty(mUser.getFriends().getPhoneRemark())) {
            findViewById(R.id.rlTelephone).setVisibility(View.VISIBLE);
            TelephoneAdapter adapter = new TelephoneAdapter();
            lvTelephone.setAdapter(adapter);
            String[] split = mUser.getFriends().getPhoneRemark().split(";");
            List<String> list = Arrays.asList(split);
            List<String> data = new ArrayList<>(list);
            adapter.setData(data);
        } else {
            findViewById(R.id.rlTelephone).setVisibility(View.GONE);
        }

        birthday_tv.setText(TimeUtils.sk_time_s_long_2_str_for_birthday(mUser.getBirthday()));
        if (!TextUtils.isEmpty(mUser.getTelephone())) {
            photo_tv.setText(mUser.getTelephoneNoAreaCode());  //手机号不为空显示手机号
            photo_rl.setVisibility(View.VISIBLE);
        } else {
            photo_rl.setVisibility(View.GONE);
        }
        if (mUser.getShowLastLoginTime() > 0) {
            online_rl.setVisibility(View.VISIBLE);
            online_tv.setText(TimeUtils.getFriendlyTimeDesc(this, mUser.getShowLastLoginTime()));
        } else {
            online_rl.setVisibility(View.GONE);
        }
        // 个性签名不为空则显示出来  否则隐藏
        if (!TextUtils.isEmpty(mUser.getDescription())) {
            rl_description.setVisibility(View.VISIBLE);
            tv_description.setText(mUser.getDescription());
            findViewById(R.id.rl_description).setOnClickListener(v -> {
                DialogHelper.tipDialog(mContext, tv_description.getText().toString());
            });
        } else {
            rl_description.setVisibility(View.GONE);
        }

        if (isMyInfo) {
            mNextStepBtn.setVisibility(View.GONE);
            findViewById(R.id.rn_rl).setVisibility(View.GONE);
            rl_describe.setVisibility(View.GONE);
        } else {
            mNextStepBtn.setVisibility(View.VISIBLE);
            if (mUser.getFriends() == null) {// 陌生人
                isFriendShow(false);
                mNextStepBtn.setText(getString(R.string.jx_add_friend));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            } else if (mUser.getFriends().getBlacklist() == 1) {  //  需显示移除黑名单
                isFriendShow(false);
                mNextStepBtn.setText(getString(R.string.remove_blacklist));
                mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());
            } else if (mUser.getFriends().getIsBeenBlack() == 1) {//  需显示加入黑名单
                isFriendShow(false);
                mNextStepBtn.setText(getString(R.string.to_blacklist));
                mNextStepBtn.setOnClickListener(null);
            } else if (mUser.getFriends().getStatus() == 2 || mUser.getFriends().getStatus() == 4) {// 好友
                isFriendShow(true);
                mNextStepBtn.setText(getString(R.string.sendmseeage));
                mNextStepBtn.setOnClickListener(new SendMsgListener());
            } else {
                isFriendShow(false);
                mNextStepBtn.setText(getString(R.string.jx_add_friend));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            }
        }
        if (hiding) {
            findViewById(R.id.iv_remarks).setVisibility(View.GONE);
            findViewById(R.id.ll_communication).setVisibility(View.GONE);
            findViewById(R.id.ll_place).setVisibility(View.GONE);
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
            findViewById(R.id.photo_rl).setVisibility(View.GONE);
            findViewById(R.id.online_rl).setVisibility(View.GONE);
            findViewById(R.id.look_location_rl).setVisibility(View.GONE);
        }
    }

    public void displayAvatar(final String userId) {
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(userId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(userId);

            ImageLoadHelper.loadImageSignatureDontAnimateWithPlaceHolder(
                    MyApplication.getContext(),
                    mOriginalUrl,
                    R.drawable.avatar_normal,
                    time,
                    d -> {
                        mAvatarImg.setImageDrawable(d);
                    }, e -> {
                        Log.e("zq", "加载原图失败：" + mOriginalUrl);// 该用户未设置头像，网页访问该URL也是404
                        if (mUser.getFriends() != null && !TextUtils.isEmpty(mUser.getFriends().getRemarkName())) {
                            AvatarHelper.getInstance().displayAvatar(mUser.getFriends().getRemarkName(), mUser.getUserId(), mAvatarImg, true);
                        } else {
                            AvatarHelper.getInstance().displayAvatar(mUser.getNickName(), mUser.getUserId(), mAvatarImg, true);
                        }
                    }
            );
        } else {
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    // 为好友才显示的布局
    private void isFriendShow(boolean isShow) {
        if (isShow) {
            findViewById(R.id.look_bussic_cicle_per_rl).setVisibility(View.VISIBLE);
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.look_bussic_cicle_per_rl).setVisibility(View.GONE);
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
        }
        if (mLoginUserId.equals(mUserId)) {// 自己隐藏该item
            findViewById(R.id.look_bussic_cicle_per_rl).setVisibility(View.GONE);
        }
    }

    private void isLocationShow() {
        boolean isOpenPrivacyPosition;
        if (isMyInfo) {
            isOpenPrivacyPosition = PrivacySettingHelper.getPrivacySettings(mContext).getIsOpenPrivacyPosition() == 1;
        } else {
            isOpenPrivacyPosition = mUser.getSettings().getIsOpenPrivacyPosition() == 1;
        }
        if (coreManager.getConfig().disableLocationServer
                || !isOpenPrivacyPosition) {// 禁用了地理位置 || 用户关闭了位置，隐藏location item
            ll_place.setVisibility(View.GONE);
            look_location_rl.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
        if (messageState == ChatMessageListener.MESSAGE_SEND_SUCCESS) {
            msgSendSuccess(message, message.getPacketId());
        } else if (messageState == ChatMessageListener.MESSAGE_SEND_FAILED) {
            msgSendFailed(message.getPacketId());
        }
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        if (!TextUtils.equals(mUserId, mLoginUserId)
                && !TextUtils.equals(message.getFromUserId(), mLoginUserId) // 我自己发出去的消息无视，
                && message.getType() != XmppMessage.TYPE_FEEDBACK // 回话不导致刷新，
                && TextUtils.equals(message.getUserId(), mUserId)) {// 当前对象正在对我进行操作
            loadOthersInfoFromNet();
            return false;
        }
        if (message.getType() == XmppMessage.TYPE_PASS) {// 对方同意好友请求 更新当前界面
            loadOthersInfoFromNet();
        }
        return false;
    }

    // xmpp消息发送成功最终回调到这，
    // 在这里调整ui,
    // 还有存本地数据库，
    public void msgSendSuccess(NewFriendMessage message, String packet) {
        boolean needDismiss = true;
        if (addhaoyouid != null && addhaoyouid.equals(packet)) {
            if (isyanzheng == 0) {// 需要验证
                Toast.makeText(getApplicationContext(), getString(R.string.say_hi_ok), Toast.LENGTH_SHORT).show();
                isFriendShow(false);

                ChatMessage sayChatMessage = new ChatMessage();
                sayChatMessage.setContent(getString(R.string.wait_pass));
                sayChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                // 各种Dao都是本地数据库的操作，
                // 这个是朋友表里更新最后一条消息，
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, sayChatMessage);

                // 这个是新的朋友界面用的，等验证什么的都在新的朋友页面，
                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_10);// 朋友状态
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);
            } else if (isyanzheng == 1) {
                Toast.makeText(getApplicationContext(), mUser.isSystem() ? getString(R.string.add_attention_succ) : getString(R.string.addsuccess), Toast.LENGTH_SHORT).show();
                isFriendShow(true);
                mNextStepBtn.setText(getString(R.string.sendmseeage));
                mNextStepBtn.setOnClickListener(new SendMsgListener());

                NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                if (mUser.getUserType() != 2) {// 公众号不在消息表插入一条记录
                    FriendHelper.addFriendExtraOperation(mLoginUserId, mUser.getUserId());
                }

                ChatMessage addChatMessage = new ChatMessage();
                addChatMessage.setContent(getString(R.string.add_friends) + ":" + mUser.getNickName());
                addChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addChatMessage);

                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_22);//添加了xxx
                if (mUser.getUserType() != 2) {// 公众号就不提醒了
                    FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mUser.getUserId(), getString(R.string.be_friendand_chat));
                }
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

                loadOthersInfoFromNet(false);
                needDismiss = false;
                CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
            }
            // 已经是好友了，mFriend不能为空，
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            if (isyanzheng == 1 && mUser.isSystem()) {
                Intent intent = new Intent(BasicInfoActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.FRIEND, mFriend);
                startActivity(intent);
                finish();
            }

        } else if (addblackid != null && addblackid.equals(packet)) {
            Toast.makeText(getApplicationContext(), getString(R.string.add_blacklist_succ), Toast.LENGTH_SHORT).show();
            isFriendShow(false);
            mNextStepBtn.setText(getString(R.string.remove_black_list));
            mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());

            // 现陌生人也支持拉黑，mFriend做非空判断
            if (mFriend != null) {
                // 更新当前持有的Friend对象，
                mFriend.setStatus(Friend.STATUS_BLACKLIST);
                FriendDao.getInstance().updateFriendStatus(message.getOwnerId(), message.getUserId(), mFriend.getStatus());
                FriendHelper.addBlacklistExtraOperation(message.getOwnerId(), message.getUserId());

                ChatMessage addBlackChatMessage = new ChatMessage();
                addBlackChatMessage.setContent(getString(R.string.added_black_list) + " " + mUser.getNickName());
                addBlackChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addBlackChatMessage);
            }

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_18);
//            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (removeblack != null && removeblack.equals(packet)) {
            boolean isFriendBefore = false;
            if (mUser.getFriends() != null
                    && (mUser.getFriends().getStatus() == 2 || mUser.getFriends().getStatus() == 4)) {
                isFriendBefore = true;
            }
            Toast.makeText(getApplicationContext(), getString(R.string.remove_blacklist_succ), Toast.LENGTH_SHORT).show();
            isFriendShow(isFriendBefore);

            // 现陌生人也支持拉黑，仅之前为好友才支持以下操作
            if (isFriendBefore) {
                mNextStepBtn.setText(getString(R.string.send_msg));
                mNextStepBtn.setOnClickListener(new SendMsgListener());

                // 更新当前持有的Friend对象，
                if (mFriend != null) {
                    // 有可能是取消拉黑后才知道对方拉黑了我，
                    // 要确保这种情况下本地不是把对方当成好友，
                    if (mUser.getFriends() != null && mUser.getFriends().getIsBeenBlack() == 1) {
                        mFriend.setStatus(Friend.STATUS_19);
                    } else {
                        mFriend.setStatus(Friend.STATUS_FRIEND);
                    }
                }
                if (mFriend == null || mFriend.getStatus() != Friend.STATUS_19) {
                    NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                    FriendHelper.addFriendExtraOperation(message.getOwnerId(), message.getUserId());
                }

                ChatMessage removeChatMessage = new ChatMessage();
                removeChatMessage.setContent(coreManager.getSelf().getNickName() + getString(R.string.remove_black_list));
                removeChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);
            }

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(message.getUserId(), Friend.STATUS_27);
//            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            loadOthersInfoFromNet();
        } else if (deletehaoyou != null && deletehaoyou.equals(packet)) {
            Toast.makeText(getApplicationContext(), mUser.isSystem()
                    ? getString(R.string.cancel_success) : getString(R.string.delete_ok), Toast.LENGTH_SHORT).show();

            FriendHelper.removeAttentionOrFriend(mLoginUserId, message.getUserId());

            ChatMessage deleteChatMessage = new ChatMessage();
            deleteChatMessage.setContent(mUser.isSystem() ?
                    getString(R.string.delete_firend_public) + mUser.getNickName() : getString(R.string.delete_firend) + mUser.getNickName());
            deleteChatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, deleteChatMessage);

            message.setContent(deleteChatMessage.getContent());
            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_16);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            EventBus.getDefault().post(new EventSyncFriendOperating(mUserId, XmppMessage.TYPE_DELALL));
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        if (needDismiss) {
            DialogHelper.dismissProgressDialog();
        }
    }

    public void msgSendFailed(String packet) {
        DialogHelper.dismissProgressDialog();
        if (packet.equals(addhaoyouid)) {
            Toast.makeText(this, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(addblackid)) {
            Toast.makeText(this, R.string.tip_put_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(removeblack)) {
            Toast.makeText(this, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(deletehaoyou)) {
            Toast.makeText(this, mUser.isSystem()
                    ? R.string.delete_public_number_error : R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Todo NextStep && ivRight Operating
     */
    private void doAddAttention() {
        DialogHelper.showLimitSingleInputDialog(this
                , getString(R.string.jx_add_friend)
                , "", getString(R.string.input_custom_hello)
                , text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastUtil.showToast(this, getString(R.string.tip_hello_can_not_empty));
                        return false;
                    }
                    doAddAttention(text);
                    return true;
                });
    }

    // 点击加好友就调用这个方法，
    private void doAddAttention(String hello) {

        if (mUser == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        if (TextUtils.isEmpty(fromAddType)) {
            // 默认就是其他方式，
            fromAddType = String.valueOf(FROM_ADD_TYPE_OTHER);
        }
        params.put("fromAddType", fromAddType);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        // 首先是调接口，
        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {

                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        boolean needDismiss = true;
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            if (result.getData().getType() == 1 || result.getData().getType() == 3) {
                                isyanzheng = 0;// 需要验证
                                // 需要验证就发送打招呼的消息，
                                doSayHello(hello);
                                needDismiss = false;
                            } else if (result.getData().getType() == 2 || result.getData().getType() == 4) {// 已经是好友了
                                isyanzheng = 1;// 不需要验证
                                String content;
                                if (mUser.isSystem()) {
                                    content = mContext.getString(R.string.added_notice_friend) + getShowName();
                                } else {
                                    content = mContext.getString(R.string.added_friend) + getShowName();
                                }
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_FRIEND, content, mUser);
                                NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                                // 不需要验证的话直接加上，就发个xmpp消息，
                                // 这里最终调用smack的方法发送xmpp消息，
                                coreManager.sendNewFriendMessage(mUser.getUserId(), message);
                                needDismiss = false;

                                addhaoyouid = message.getPacketId();
                              /*  Intent intent = new Intent(BasicInfoActivity.this, ChatActivity.class);
                                intent.putExtra(ChatActivity.FRIEND, mFriend);
                                startActivity(intent);
                                finish();*/
                            } else if (result.getData().getType() == 5) {
                                ToastUtil.showToast(mContext, R.string.add_attention_failed);
                            }
                        } else {
                            Toast.makeText(BasicInfoActivity.this, result.getResultMsg() + "", Toast.LENGTH_SHORT).show();
                        }
                        if (needDismiss) {
                            DialogHelper.dismissProgressDialog();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Nullable
    private String getShowName() {
        if (mFriend != null) {
            return mFriend.getShowName();
        }
        if (mUser != null) {
            return mUser.getNickName();
        }
        return null;
    }

    // 打招呼
    private void doSayHello(String text) {
        if (TextUtils.isEmpty(text)) {
            text = getString(R.string.hello);
        }
        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                XmppMessage.TYPE_SAYHELLO, text, mUser);
        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
        // 这里最终调用smack的发送消息，
        coreManager.sendNewFriendMessage(mUser.getUserId(), message);

        addhaoyouid = message.getPacketId();

        // 发送打招呼的消息
        ChatMessage sayMessage = new ChatMessage();
        sayMessage.setFromUserId(coreManager.getSelf().getUserId());
        sayMessage.setFromUserName(coreManager.getSelf().getNickName());
        sayMessage.setContent(text);
        sayMessage.setType(XmppMessage.TYPE_TEXT); //文本类型
        sayMessage.setMySend(true);
        sayMessage.setSendRead(true);// 新的朋友消息默认为已读
        sayMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        sayMessage.setPacketId(message.getPacketId());// msgId需要与发送出去的NewFriendMessage msgId保持一致
        sayMessage.setObjectId(ChatTextClickPpWindow.DISABLE_RECORD);
        sayMessage.setTimeSend(TimeUtils.sk_time_current_time());
        ChatMessageDao.getInstance().saveNewSingleChatMessage(message.getOwnerId(), message.getUserId(), sayMessage);
    }

    // 加入、移除黑名单
    private void showBlacklistDialog() {
/*
        if (friend.getStatus() == Friend.STATUS_BLACKLIST) {
            removeBlacklist(friend);
        } else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
            addBlacklist(friend);
        }
*/
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.add_black_list), getString(R.string.sure_add_friend_blacklist), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                addBlacklist();
            }
        });
        mSF.show();
    }

    private void addBlacklist() {
        Friend friend = new Friend();
        friend.setUserId(mUser.getUserId());
        friend.setNickName(mUser.getNickName());

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            // 现陌生人也支持拉黑，好友限制去掉
/*
                            if (friend.getStatus() == Friend.STATUS_FRIEND
                                    || friend.getStatus() == Friend.STATUS_SYSTEM) {
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_BLACK, null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);// 加入黑名单

                                // 记录加入黑名单消息packet，如收到消息回执，在做后续操作
                                addblackid = message.getPacketId();
                            }
*/
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_BLACK, null, friend);
                            coreManager.sendNewFriendMessage(friend.getUserId(), message);// 加入黑名单

                            // 记录加入黑名单消息packet，如收到消息回执，在做后续操作
                            addblackid = message.getPacketId();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, getString(R.string.add_blacklist_fail), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeBlacklist() {
        Friend friend = new Friend();
        friend.setUserId(mUser.getUserId());
        friend.setNickName(mUser.getNickName());

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_REFUSED, null, friend);
                            coreManager.sendNewFriendMessage(friend.getUserId(), message);// 移除黑名单

                            // 记录移除黑名单消息packet，如收到消息回执，在做后续操作
                            removeblack = message.getPacketId();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 删除好友
    private void showDeleteAllDialog(final Friend friend, boolean isPublic) {
        String deletePublic;
        String sure_deletePublic;
        if (friend.getStatus() == Friend.STATUS_UNKNOW) {// 陌生人
            return;
        }
        if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)
                || friend.getUserId().equals(Friend.ID_SK_PAY)) {// 10000 与1100 号不能删除，
            Toast.makeText(mContext, getString(R.string.tip_not_allow_delete), Toast.LENGTH_SHORT).show();
            return;
        }
        deletePublic = !isPublic ? getString(R.string.delete_friend) : getString(R.string.delete_public_number);
        sure_deletePublic = !isPublic ? getString(R.string.sure_delete_friend) : getString(R.string.cancel_attention_prompt);

        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(deletePublic, sure_deletePublic, new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                deleteFriend(friend, 1);
            }
        });
        mSF.show();
    }

    private void deleteFriend(final Friend friend, final int type) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_DELALL, null, friend);
                            coreManager.sendNewFriendMessage(mUser.getUserId(), message); // 删除好友

                            // 记录删除好友消息packet，如收到消息回执，在做后续操作
                            deletehaoyou = message.getPacketId();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, mUser.isSystem() ? R.string.cancel_friend_failed : R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void report(String userId, Report report) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", userId);
        params.put("reportType", String.valueOf(1));
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
                            ToastUtil.showToast(BasicInfoActivity.this, R.string.report_success);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SET_REMARK) {
            loadOthersInfoFromNet();
        }
    }

    // 加关注
    private class AddAttentionListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            doAddAttention();
        }
    }

    // 移除黑名单  直接变为好友  不需要验证
    private class RemoveBlacklistListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            removeBlacklist();
        }
    }

    // 发消息
    private class SendMsgListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());
            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra(ChatActivity.FRIEND, friend);
            startActivity(intent);
        }
    }

    class TextClick extends ClickableSpan {
        private String userId;

        public TextClick(String userId) {
            this.userId = userId;
        }

        @Override
        public void onClick(View view) {
            BasicInfoActivity.start(mContext, userId, BasicInfoActivity.FROM_ADD_TYPE_GROUP
                    , true
                    , fromGroupId);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(getResources().getColor(R.color.link_nick_name_color));
        }
    }

    /**
     * 电话号码Adapter
     */
    class TelephoneAdapter extends BaseAdapter {
        private List<String> data = new ArrayList<>();

        private void setData(List<String> data) {
            this.data = data;
            if (this.data == null) {
                this.data = new ArrayList<>();
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.row_show_telephone, viewGroup, false);
            }
            TextView tvTelephone = ViewHolder.get(view, R.id.tvTelephone);
            View vLine = ViewHolder.get(view, R.id.vLine);
            tvTelephone.setText(data.get(i));
            vLine.setVisibility(i == data.size() - 1 ? View.GONE : View.VISIBLE);
            tvTelephone.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + data.get(i)));//跳转到拨号界面，同时传递电话号码
                startActivity(dialIntent);
            });
            return view;
        }
    }
}