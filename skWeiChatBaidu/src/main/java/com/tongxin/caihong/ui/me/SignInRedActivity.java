package com.tongxin.caihong.ui.me;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.SignInEntity;
import com.tongxin.caihong.bean.SignInfoEntity;
import com.tongxin.caihong.bean.SignInfoListEntity;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.me.redpacket.WxPayBlance;
import com.tongxin.caihong.ui.me.redpacket.scan.ScanWithdrawActivity;
import com.tongxin.caihong.ui.sign.SignUpActivity;
import com.tongxin.caihong.ui.sign.WithdrawalActivity;
import com.tongxin.caihong.util.CalendarUtil;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.CalendarView;
import com.tongxin.caihong.view.HeadView;
import com.tongxin.caihong.view.listener.OnPagerChangeListener;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import okhttp3.Call;


/**
 * @auther WangQX
 * created at 2019/3/4 0004
 * 描述：签到红包界面
 */
public class SignInRedActivity extends BaseActivity implements OnClickListener {

    public static final String TAG = "SignInRedActivity";
    public static final int SIGN_IN_NO = 2;//未签到
    public static final int SIGN_IN_ALREADY = 1;//已经签到
    private Dialog mSignInCalendarDialog;
    TextView tv_sign_in_or_not_today;
    TextView tv_sign_in_balance;
    TextView tv_sign_sum;
    TextView tv_sign_in_now;
    private LinearLayout ll_imageview;
    private LinearLayout ll_day;
//    TextView tv_go_to_balance;
//    TextView tv_go_to_lucky_wheel;

    private int[] signInRedPacketImgId = {R.id.iv_sign_in_one, R.id.iv_sign_in_two, R.id.iv_sign_in_three, R.id.iv_sign_in_four, R.id.iv_sign_in_five, R.id.iv_sign_in_six, R.id.iv_sign_in_seven};
    private int[] signInRedPacketTextId = {R.id.tv_sign_in_one, R.id.tv_sign_in_two, R.id.tv_sign_in_three, R.id.tv_sign_in_four, R.id.tv_sign_in_five, R.id.tv_sign_in_six, R.id.tv_sign_in_seven};
    private int signCount = 0;
    private Map<Integer, HashSet<String>> mSignInMap = new HashMap<>();
    private String startDateStr = "2019.1";
    private String endDateStr = "2020.12";
    private List<String> signInDateList = new ArrayList<>();
    private int startDate[] = CalendarUtil.strToArray(startDateStr);
    private int endDate[] = CalendarUtil.strToArray(endDateStr);
    private CalendarView calendarView;
    private String mLoginUserId;
    private TextView tv_title_right;
    private Integer[] integers = {1, 3, 5, 7, 9, 11, 13, 15};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        tv_title_right = findViewById(R.id.tv_title_right);
        tv_title_right.setVisibility(View.VISIBLE);
//        tv_title_right.setText(getResources().getString(R.string.sign_calendar));
//        tv_title_right.setOnClickListener(this);
        findViewById(R.id.bt_sign).setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.sign_in_red_envelope));
        mLoginUserId = coreManager.getSelf().getUserId();
        initView();
    }

    public void initView() {
        AvatarHelper.getInstance().displayAvatar(coreManager.getSelf().getNickName(), coreManager.getSelf().getUserId(), ((HeadView)findViewById(R.id.avatar_img)).getHeadImage(), true);
        findViewById(R.id.bt_withdrawal).setOnClickListener(this);
        ll_imageview = findViewById(R.id.ll_imageview);
        ll_day = findViewById(R.id.ll_day);
//        tv_sign_in_or_not_today = findViewById(R.id.tv_sign_in_or_not_today);
        tv_sign_in_balance = findViewById(R.id.tv_money);
        tv_sign_sum = findViewById(R.id.tv_sign_sum);
//        tv_go_to_lucky_wheel = findViewById(R.id.tv_go_to_lucky_wheel);
//        tv_go_to_balance = findViewById(R.id.tv_go_to_balance);
//        tv_go_to_balance.setOnClickListener(this);
        tv_sign_in_now = findViewById(R.id.bt_sign);
//        tv_sign_in_day_continuous.setText(String.format(getString(R.string.sign_in_day_continuous), "0"));
        getUserSignInInfo();
    }

    /**
     * 获取 用户签到信息
     */
    private void getUserSignInInfo() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        HttpUtils.get().url(coreManager.getConfig().GET_USER_SIGN_INFO)
                .params(params)
                .build()
                .execute(new BaseCallback<SignInfoEntity>(SignInfoEntity.class) { //获取通证成功
                    @Override
                    public void onResponse(ObjectResult<SignInfoEntity> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == Result.CODE_SUCCESS && result.getData() != null) {
                            SignInfoEntity infoBean = result.getData();
                            signCount = Integer.parseInt(infoBean.getSignCount());
                            if (Integer.parseInt(infoBean.getSignStatus()) == SIGN_IN_ALREADY) {
//                                tv_sign_in_or_not_today.setText(R.string.sign_in_already_today);
                                tv_sign_in_now.setAlpha(0.5f);
                                tv_sign_in_now.setText(R.string.sign_in_already);
                                tv_sign_in_now.setEnabled(false);
                                tv_sign_in_balance.setText(infoBean.getSignAward());
                                loadView(true,signCount, Integer.parseInt(infoBean.getSevenCount()),Float.parseFloat(infoBean.getSignAward()));
                            } else {
                                tv_sign_in_now.setAlpha(1f);
                                tv_sign_in_now.setEnabled(true);
//                                tv_sign_in_or_not_today.setText(R.string.sign_in_not_today);
                                tv_sign_in_now.setText(R.string.sign_in_now);
                                loadView(false,signCount, Integer.parseInt(infoBean.getSevenCount()),Float.parseFloat(infoBean.getSignAward()));
                            }
                            Log.i(TAG, "onNext: signCount:" + signCount);
                            tv_sign_sum.setText(String.format(getString(R.string.sign_in_day_continuous), infoBean.getSeriesSignCount()));
                            setSignInRedPacketStatus(Integer.parseInt(infoBean.getSevenCount()));
                        } else {
                            ToastUtil.showToast(SignInRedActivity.this, result.getResultMsg());
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(SignInRedActivity.this);
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (!UiUtils.isNormalClick(v)) {
            return;
        }
        switch (v.getId()) {
            case R.id.tv_title_right:
                int currentDate[] = CalendarUtil.getCurrentDate();
                getSignInMonthDate(currentDate);
                break;
            case R.id.bt_sign:
                signInNow();
                break;
//            case R.id.tv_go_to_balance:
//                startActivity(new Intent(SignInRedActivity.this, QuXianActivity.class));
//                break;
//            case R.id.tv_go_to_lucky_wheel:

//                break;
            case R.id.bt_withdrawal:
                Intent intent = new Intent(this, ScanWithdrawActivity.class);
                startActivity(intent);
                break;
        }
    }

    public void signInNow() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        params.put("device", DeviceInfoUtil.getDeviceId(this));
        HttpUtils.get().url(coreManager.getConfig().SIGN_IN_NOW)
                .params(params)
                .build()
                .execute(new BaseCallback<SignInEntity>(SignInEntity.class) { //获取通证成功
                    @Override
                    public void onResponse(ObjectResult<SignInEntity> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == Result.CODE_SUCCESS && result.getData() != null) {
                            SignInEntity infoBean = result.getData();
                            if (Float.parseFloat(infoBean.getSignAward()) >= 0) {
                                tv_sign_in_balance.setText(Float.parseFloat(infoBean.getSignAward()) + "");
//                                tv_sign_in_or_not_today.setText(R.string.sign_in_already_today);
                            }
                            tv_sign_sum.setText(String.format(getString(R.string.sign_in_day_continuous), infoBean.getSeriesSignCount()));
                            tv_sign_in_now.setText(R.string.sign_in_already);
                            tv_sign_in_now.setAlpha(0.5f);
                            tv_sign_in_now.setEnabled(false);
                            signCount = Integer.parseInt(infoBean.getSignCount());
                            int sevenCount = Integer.parseInt(infoBean.getSevenCount());
                            if (sevenCount > 0) {
//                                ImageView imageView = (ImageView) findViewById(signInRedPacketImgId[sevenCount - 1]);
//                                imageView.setImageResource(R.mipmap.sign_in_red_packet_open);
//                                TextView textView = (TextView) findViewById(signInRedPacketTextId[sevenCount - 1]);
//                                textView.setTextColor(getResources().getColor(R.color.sign_in_already_text_color));
                                loadView(true,signCount, sevenCount,Float.parseFloat(infoBean.getSignAward()));
                            }
                            // 更新余额
                            coreManager.updateMyBalance();
                        } else {
                            ToastUtil.showToast(SignInRedActivity.this, result.getResultMsg());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(SignInRedActivity.this);
                    }
                });
    }

    private void loadView(boolean signin,int signCount, int sevenCount,float signAward) {
        //判断哪些位置是有红包的
        List<Integer> numList = new ArrayList();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < signCount; i++) {
            numList.add(i);
            builder.append(i + 1);
            builder.append(getString(R.string.tip_sign_day));
            if (i != 6) {
                builder.append(",");
            }
        }
        if (numList.size() == 0) {
            // 每天签到都没有红包领取
            findViewById(R.id.tv_des).setVisibility(View.GONE);
        } else if (numList.size() == 7) {
            // 每天签到均可领取一个红包
            ((TextView) findViewById(R.id.tv_des)).setText(getString(R.string.tip_sign_every_get));
        } else {
            // 指定签到天数均可领取一个红包
            ((TextView) findViewById(R.id.tv_des)).setText(getString(R.string.sign_title_des, builder.toString()));
        }
        if (signin) {
            ((TextView) ll_day.getChildAt(sevenCount - 1)).setTextColor(0xfff2ab69);
        } else {
//            ((TextView) ll_day.getChildAt(sevenCount)).setTextColor(0xfff2ab69);
        }
        tv_sign_sum.setText(getString(R.string.sign_sum, String.valueOf(sevenCount)));
        if (numList.size() > 0) {
            for (int i = 0; i < numList.size(); i++) {
                if (ll_imageview.getChildAt(integers[numList.get(i)]) instanceof ImageView) {
                    ((ImageView) (ll_imageview.getChildAt(integers[numList.get(i)])))
                            .setImageDrawable(getResources().getDrawable(R.mipmap.sign_red));
                }
            }
        }
        if (signCount > 0) {
            for (int i = 0; i < signCount * 2; i++) {
                if ((i + 1) % 2 == 0) {
                    if (ll_imageview.getChildAt(i) instanceof ImageView) {
                        ((ImageView) (ll_imageview.getChildAt(i))).setImageDrawable(getResources().getDrawable(R.mipmap.sign_up_ed));
                    }
                } else {
                    ll_imageview.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.gray));
                }
            }
        }

        //根据服务端返回的数据  显示相应的签到过的UI
        if (signCount > 0) {
            //先初始化  签到的都给染成灰色背景
            for (int i = 0; i <signCount; i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.layout_sign_money_item, null);
                // 服务端返回的数据 [10,0,10,0,10,0,10]
                if (signAward != 0) {
                    ((TextView) view.findViewById(R.id.tv_money)).setText(String.valueOf(signAward));
                    ll_imageview.removeViewAt(integers[i]);
                    ll_imageview.addView(view, integers[i]);
                }
            }
        }
    }

    /**
     * 获取某月的签到的信息
     */
    public void getSignInMonthDate(final int date[]) {
        String yearAndMonth = date[0] + "-" + date[1];
        final int datePosition = CalendarUtil.dateToPosition(date[0], date[1], startDate[0], startDate[1]);
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        params.put("monthStr", yearAndMonth);
        HttpUtils.get().url(coreManager.getConfig().GET_USER_SIGN_INFO_BY_MONTH)
                .params(params)
                .build()
                .execute(new ListCallback<SignInfoListEntity>(SignInfoListEntity.class) {
                    @Override
                    public void onResponse(ArrayResult<SignInfoListEntity> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == Result.CODE_SUCCESS && result.getData() != null) {
                            List<SignInfoListEntity> signInfoListEntity = result.getData();
                            for (SignInfoListEntity infoBean : signInfoListEntity) {
                                long time = Long.parseLong(infoBean.getSignDate());
                                String date = TimeUtils.millis2String(time, new SimpleDateFormat("yyyy.MM.dd"));
                                Log.i(TAG, "onNext: date:" + date);
                                HashSet<String> signInSet;
                                if (mSignInMap.get(datePosition) == null) {
                                    signInSet = new HashSet<>();
                                } else {
                                    signInSet = mSignInMap.get(datePosition);
                                }
                                if (!signInSet.contains(date)) {
                                    signInDateList.add(date);
                                    signInSet.add(date);
                                }
                                mSignInMap.put(datePosition, signInSet);
                            }
                            showSignInCalendarDialog(datePosition, signInDateList);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(SignInRedActivity.this);
                    }
                });

    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 显示签到日历弹出框
     */
    private void showSignInCalendarDialog(int position, List<String> signInDateList) {
        if (mSignInCalendarDialog == null) {
            initSignInCalendarDialog();
        } else {
            if (calendarView != null) {
                calendarView.refreshSignInMonthDate(position, signInDateList);
            } else {
                initSignInCalendarDialog();
            }
        }
        mSignInCalendarDialog.show();
    }


    /**
     * 初始化签到日历弹出框
     */
    private void initSignInCalendarDialog() {
        mSignInCalendarDialog = new Dialog(this, R.style.dialog_bottom_full);
        mSignInCalendarDialog.setCanceledOnTouchOutside(true);
        mSignInCalendarDialog.setCancelable(true);
        Window window = mSignInCalendarDialog.getWindow();
        window.setGravity(Gravity.CENTER);
        //window.setWindowAnimations(R.style.share_animation);
        View dialogView = View.inflate(this, R.layout.activity_sign_in_calendar, null);
        final TextView title = dialogView.findViewById(R.id.title);
        TextView tv_sign_in_calendar_continuous = dialogView.findViewById(R.id.tv_sign_in_calendar_continuous);
        View iv_last = dialogView.findViewById(R.id.iv_last);
        View iv_next = dialogView.findViewById(R.id.iv_next);
        View iv_close = dialogView.findViewById(R.id.iv_close);
        calendarView = dialogView.findViewById(R.id.calendar);
        iv_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignInCalendarDialog.dismiss();
            }
        });
        tv_sign_in_calendar_continuous.setText(String.format(getResources().getString(R.string.sign_in_day_count), signCount + ""));
        iv_last.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarView.lastMonth();
            }
        });
        iv_next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarView.nextMonth();
            }
        });
        int currentDate[] = CalendarUtil.getCurrentDate();
        calendarView
                .setStartEndDate(startDateStr, endDateStr)
                .setInitDate(currentDate[0] + "." + currentDate[1])
                .showCurrentDate(true)
                .setMultiDate(signInDateList)
                .init();
        title.setText(currentDate[0] + " - " + currentDate[1]);
        calendarView.setOnPagerChangeListener(new OnPagerChangeListener() {
            @Override
            public void onPagerChanged(int[] date) {
                Log.i(TAG, "onPagerChanged: onPagerChanged");
                title.setText(date[0] + " - " + date[1]);
                getSignInMonthDate(date);
            }
        });
        calendarView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // TODO Auto-generated method stub

            }
        });
        window.setContentView(dialogView);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);//设置横向全屏
    }

    /**
     * 设置红包状态
     *
     * @param days
     */
    public void setSignInRedPacketStatus(int days) {
//        for (int i = 0; i < days; i++) {
//            ImageView imageView = (ImageView) findViewById(signInRedPacketImgId[i]);
//            imageView.setImageResource(R.mipmap.sign_in_red_packet_open);
//            TextView textView = (TextView) findViewById(signInRedPacketTextId[i]);
//            textView.setTextColor(getResources().getColor(R.color.sign_in_already_text_color));
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSignInMap != null) {
            mSignInMap.clear();
        }
        if (mSignInCalendarDialog != null) {
            mSignInCalendarDialog.dismiss();
            mSignInCalendarDialog = null;
        }
        if (calendarView != null) {
            calendarView = null;
        }
    }

}
