package com.tongxin.caihong.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.adapter.PublicMessageRecyclerAdapter;
import com.tongxin.caihong.bean.EventBalanceChanged;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.MessageEventHongdian;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.UserAvatarDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.circle.BusinessCircleActivity;
import com.tongxin.caihong.ui.circle.SelectPicPopupWindow;
import com.tongxin.caihong.ui.circle.range.NewZanActivity;
import com.tongxin.caihong.ui.circle.range.SendAudioActivity;
import com.tongxin.caihong.ui.circle.range.SendFileActivity;
import com.tongxin.caihong.ui.circle.range.SendShuoshuoActivity;
import com.tongxin.caihong.ui.circle.range.SendVideoActivity;
import com.tongxin.caihong.ui.me.BasicInfoEditActivity;
import com.tongxin.caihong.ui.me.PrivacySettingActivity;
import com.tongxin.caihong.ui.me.SecureSettingActivity;
import com.tongxin.caihong.ui.me.SettingActivity;
import com.tongxin.caihong.ui.me.collection.MyCollection;
import com.tongxin.caihong.ui.me.redpacket.MyConsumeRecordNew;
import com.tongxin.caihong.ui.me.redpacket.MyWalletActivity;
import com.tongxin.caihong.ui.me.redpacket.QuXianActivity;
import com.tongxin.caihong.ui.me.redpacket.WxPayBlance;
import com.tongxin.caihong.ui.me.redpacket.scan.ScanWithdrawActivity;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.util.AvatarUtil;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.FastBlurUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

public class KxMeFragment extends EasyFragment implements View.OnClickListener {

    private ImageView ivAvatarBg;
    private ImageView mAvatarImg;
    private TextView mNickNameTv;
    private TextView mPhoneNumTv;
    private TextView tvMoney;
    private ImageView ivMoneyEye;
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, OtherBroadcast.SYNC_SELF_DATE_NOTIFY)) {
                updateUI();
            }
        }
    };
    private SelectPicPopupWindow menuWindow;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            if (menuWindow != null) {
                // 顶部一排按钮复用这个listener, 没有menuWindow,
                menuWindow.dismiss();
            }
            Intent intent = new Intent();
            switch (v.getId()) {
                case R.id.btn_send_picture:
                    // 发表图文，
                    intent.setClass(getActivity(), SendShuoshuoActivity.class);
                    startActivity(intent);
                    break;
                case R.id.btn_send_voice:
                    // 发表语音
                    intent.setClass(getActivity(), SendAudioActivity.class);
                    startActivity(intent);
                    break;
                case R.id.btn_send_video:
                    // 发表视频
                    intent.setClass(getActivity(), SendVideoActivity.class);
                    startActivity(intent);
                    break;
                case R.id.btn_send_file:
                    // 发表文件
                    intent.setClass(getActivity(), SendFileActivity.class);
                    startActivity(intent);
                    break;
                case R.id.new_comment:
                    // 最新评论&赞
                    Intent intent2 = new Intent(getActivity(), NewZanActivity.class);
                    intent2.putExtra("OpenALL", true);
                    startActivity(intent2);
                    EventBus.getDefault().post(new MessageEventHongdian(0));
                    break;
                default:
                    break;
            }
        }
    };

    public KxMeFragment() {
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.kx_fragment_me;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBusHelper.register(this);
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initView();
            initEvent();
            CoreManager.updateMyBalance();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        updateBalance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
    }

    private void initView() {
        // 关闭支付功能，隐藏我的钱包
        if (!coreManager.getConfig().enablePayModule) {
            findViewById(R.id.llWallet).setVisibility(View.INVISIBLE);
        }

        ivAvatarBg = findViewById(R.id.ivAvatarBg);
        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
        mPhoneNumTv = (TextView) findViewById(R.id.phone_number_tv);
        AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getNickName(), coreManager.getSelf().getUserId(), mAvatarImg, false);
        mNickNameTv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mNickNameTv.setText(getString(R.string.nick_name) + coreManager.getSelf().getNickName());
        tvMoney = findViewById(R.id.tvMoney);
        ivMoneyEye = findViewById(R.id.ivMoneyEye);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OtherBroadcast.SYNC_SELF_DATE_NOTIFY);
        getActivity().registerReceiver(mUpdateReceiver, intentFilter);
    }

    private void initEvent() {
        findViewById(R.id.iv_title_right_left).setOnClickListener(v -> {
            findViewById(R.id.llInfo).performClick();
        });
        findViewById(R.id.iv_title_right).setOnClickListener(v -> {
            menuWindow = new SelectPicPopupWindow(getActivity(), itemsOnClick);
            menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            menuWindow.showAsDropDown(v,
                    -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
                    0);
        });
        findViewById(R.id.local_aq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SecureSettingActivity.class));
            }
        });
        findViewById(R.id.my_monry).setOnClickListener(this);
        findViewById(R.id.local_ys).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PrivacySettingActivity.class));
            }
        });
        mAvatarImg.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SingleImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGE_URI, coreManager.getSelf().getUserId());
            startActivity(intent);
        });

        findViewById(R.id.llInfo).setOnClickListener(this);

        findViewById(R.id.llWallet).setOnClickListener(this);
        findViewById(R.id.my_space_rl).setOnClickListener(this);
        findViewById(R.id.my_collection_rl).setOnClickListener(this);
//        findViewById(R.id.rlMyShare).setOnClickListener(this);

        findViewById(R.id.my_settings_rl).setOnClickListener(this);

        ivMoneyEye.setOnClickListener(this);
        findViewById(R.id.tvWalletDetail).setOnClickListener(this);
        findViewById(R.id.tvWithdraw).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!UiUtils.isNormalClick(v)) {
            return;
        }
        int id = v.getId();
        switch (id) {
            case R.id.llInfo:
                // 我的资料
                startActivityForResult(new Intent(getActivity(), BasicInfoEditActivity.class), 1);
                break;
            case R.id.my_monry:
            case R.id.llWallet:
                // 我的钱包
                MyWalletActivity.start(requireContext());
                break;
            case R.id.ivMoneyEye:
                boolean myMoneyVisible = PreferenceUtils.getBoolean(requireContext(), Constants.KEY_MY_MONEY_VISIBLE, true);
                PreferenceUtils.putBoolean(requireContext(), Constants.KEY_MY_MONEY_VISIBLE, !myMoneyVisible);
                updateBalance();
                break;
            case R.id.tvWalletDetail:
                startActivity(new Intent(requireContext(), MyConsumeRecordNew.class));
                break;
            case R.id.tvWithdraw:
                Intent intent = new Intent(requireContext(), ScanWithdrawActivity.class);
                startActivity(intent);
                break;
            case R.id.my_space_rl:
                // 我的动态
                Intent intent1 = new Intent(getActivity(), BusinessCircleActivity.class);
                intent1.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                startActivity(intent1);
                break;
            case R.id.my_collection_rl:
                // 我的收藏
                startActivity(new Intent(getActivity(), MyCollection.class));
                break;
//            case R.id.rlMyShare:
//                // 我的课件
//                startActivity(new Intent(getActivity(), LocalCourseActivity.class));
//                break;

            case R.id.my_settings_rl:
                // 设置
                startActivity(new Intent(getActivity(), SettingActivity.class));
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 || resultCode == Activity.RESULT_OK) {// 个人资料更新了
            // onResume有刷新ui，
        }
    }

    private void updateBalance() {
        boolean myMoneyVisible = PreferenceUtils.getBoolean(requireContext(), Constants.KEY_MY_MONEY_VISIBLE, true);
        if (myMoneyVisible) {
            ivMoneyEye.setImageResource(R.mipmap.my_money_eye);
            tvMoney.setText(String.format(Locale.CHINA, "￥ %.2f", coreManager.getSelf().getBalance()));
        } else {
            ivMoneyEye.setImageResource(R.mipmap.my_money_eye_hide);
            tvMoney.setText("******");
        }
    }

    /**
     * 用户的信息更改的时候，ui更新
     */
    private void updateUI() {
        if (ivAvatarBg != null) {
            Context ctx = requireContext();
            User item = coreManager.getSelf();
            ImageView ivHead = ivAvatarBg;
            String time = UserAvatarDao.getInstance().getUpdateTime(item.getUserId());
            ImageLoadHelper.loadImageSignatureDontAnimateWithPlaceHolder(
                    ctx,
                    AvatarHelper.getAvatarUrl(item.getUserId(), false),
                    R.drawable.avatar_normal,
                    time,
                    d -> {
                        mAvatarImg.setImageDrawable(d);
                        ivHead.setImageBitmap(FastBlurUtil.toBlur(FastBlurUtil.drawableToBitmap(d)
                                , 10));
                    },
                    e -> {
                        List<Object> bitmapList = new ArrayList<>();
                        bitmapList.add(item.getNickName());
                        Bitmap avatar = AvatarUtil.getBuilder(ctx)
                                .setShape(AvatarUtil.Shape.ROUND)
                                .setList(bitmapList)
                                .setTextSize(DisplayUtil.dip2px(ctx, 40))
                                .setTextColor(R.color.white)
                                .setTextBgColor(SkinUtils.getSkin(ctx).getAccentColor())
                                .setBitmapSize(DisplayUtil.dip2px(ctx, 240), DisplayUtil.dip2px(ctx, 240))
                                .create();
                        AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), mAvatarImg, true);
                        ivHead.setImageBitmap(FastBlurUtil.toBlur(avatar, PublicMessageRecyclerAdapter.blur));
                    });
        }
        if (mNickNameTv != null) {
            mNickNameTv.setText(getString(R.string.nick_name) +coreManager.getSelf().getNickName());
        }

        if (mPhoneNumTv != null) {
            //2023.11.07 更改为手机号
            //mPhoneNumTv.setText("ID:" + coreManager.getSelf().getAccount());
            mPhoneNumTv.setText(getString(R.string.select_withdraw_account)+"："+coreManager.getSelf().getTelephoneNoAreaCode());

        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventBalanceChanged message) {
        updateBalance();
    }

}
