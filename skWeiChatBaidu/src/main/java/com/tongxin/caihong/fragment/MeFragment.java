package com.tongxin.caihong.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.ConfigBean;
import com.tongxin.caihong.bean.event.MessageEventHongdian;
import com.tongxin.caihong.bean.redpacket.Balance;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.course.LocalCourseActivity;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.MainActivity;
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
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class MeFragment extends EasyFragment implements View.OnClickListener {

    private ImageView mAvatarImg;
    private TextView mNickNameTv;
    private TextView mPhoneNumTv;
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

    public MeFragment() {
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_me;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initTitleBackground();
            initView();
            initEvent();
        }
    }

    boolean isReqWallet = false;
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        initWallData();
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser){
            initWallData();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            initWallData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
    }

    private void initTitleBackground() {
        SkinUtils.Skin skin = SkinUtils.getSkin(requireContext());
        findViewById(R.id.tool_bar).setBackgroundColor(skin.getAccentColor());
        findViewById(R.id.rlInfoBackground).setBackgroundColor(skin.getAccentColor());
    }


    private void initWallData() {
        if(isReqWallet) return;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            DecimalFormat decimalFormat = new DecimalFormat("0.00");
                            Balance balance = result.getData();
                            coreManager.getSelf().setBalance(balance.getBalance());
                            ((TextView) findViewById(R.id.mTvMoney)).setText("￥" + decimalFormat.format(((balance.getBalance()))));
                        } else {
//                            ToastUtil.showErrorData(WxPayBlance.this);
                        }
                        isReqWallet = false;
                    }

                    @Override
                    public void onError(Call call, Exception e) {
//                        ToastUtil.showNetError(WxPayBlance.this);
                        isReqWallet = false;
                    }
                });
        isReqWallet =true;
    }
    private void initView() {
        // 关闭支付功能，隐藏我的钱包
        if (!coreManager.getConfig().enablePayModule) {
            findViewById(R.id.my_monry).setVisibility(View.GONE);
        }

        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
        mPhoneNumTv = (TextView) findViewById(R.id.phone_number_tv);
        AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getNickName(), coreManager.getSelf().getUserId(), mAvatarImg, false);
        mNickNameTv.setText(coreManager.getSelf().getNickName());

        TextView mTvMoney = (TextView) findViewById(R.id.mTvMoney);
        findViewById(R.id.my_bill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), MyConsumeRecordNew.class);
                startActivity(intent);
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OtherBroadcast.SYNC_SELF_DATE_NOTIFY);
        getActivity().registerReceiver(mUpdateReceiver, intentFilter);

        ImageView ivRight = findViewById(R.id.iv_title_add);
        ivRight.setImageResource(R.drawable.messaeg_scnning);
        ivRight.setVisibility(View.VISIBLE);
        ivRight.setOnClickListener(v -> {
            MainActivity.requestQrCodeScan(getActivity());
        });

    }

    private String removeSuffix(final String s, final String suffix) {
        if (s != null && suffix != null && s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    private void initEvent() {
//        findViewById(R.id.iv_title_add).setOnClickListener(v -> {
//            menuWindow = new SelectPicPopupWindow(getActivity(), itemsOnClick);
//            menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//            menuWindow.showAsDropDown(v,
//                    -(menuWindow.getContentView().getMeasuredWidth() - v.getWidth() / 2 - 40),
//                    0);
//        });

        mAvatarImg.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SingleImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGE_URI, coreManager.getSelf().getUserId());
            startActivity(intent);
        });

        findViewById(R.id.info_rl).setOnClickListener(this);

        findViewById(R.id.my_monry).setOnClickListener(this);
        findViewById(R.id.my_space_rl).setOnClickListener(this);
        findViewById(R.id.my_collection_rl).setOnClickListener(this);
        findViewById(R.id.local_course_rl).setOnClickListener(this);

        findViewById(R.id.my_settings_rl).setOnClickListener(this);

        findViewById(R.id.local_aq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SecureSettingActivity.class));
            }
        });

        findViewById(R.id.local_ys).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PrivacySettingActivity.class));
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (!UiUtils.isNormalClick(v)) {
            return;
        }
        int id = v.getId();
        switch (id) {
            case R.id.info_rl:
                // 我的资料
                startActivityForResult(new Intent(getActivity(), BasicInfoEditActivity.class), 1);
                break;
            case R.id.my_monry:
                // 我的钱包
                MyWalletActivity.start(requireContext());
                break;
            case R.id.my_space_rl:
                // 我的动态
                Intent intent = new Intent(getActivity(), BusinessCircleActivity.class);
                intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                startActivity(intent);
                break;
            case R.id.my_collection_rl:
                // 我的收藏
                startActivity(new Intent(getActivity(), MyCollection.class));
                break;
            case R.id.local_course_rl:
                // 我的课件
                startActivity(new Intent(getActivity(), LocalCourseActivity.class));
                break;

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
            updateUI();
        }
    }

    /**
     * 用户的信息更改的时候，ui更新
     */
    private void updateUI() {
        if (mAvatarImg != null) {
            AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getUserId(), mAvatarImg, true);
        }
        if (mNickNameTv != null) {
            mNickNameTv.setText(coreManager.getSelf().getNickName());
        }

        if (mPhoneNumTv != null) {
            String phoneNumber = coreManager.getSelf().getTelephoneNoAreaCode();
            mPhoneNumTv.setText(phoneNumber);
        }
    }
}
