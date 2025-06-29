package com.tongxin.caihong.ui.live.view;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.redpacket.Balance;
import com.tongxin.caihong.db.InternationalizationHelper;
import com.tongxin.caihong.ui.base.BaseDialogFragment;
import com.tongxin.caihong.ui.live.LiveConstants;
import com.tongxin.caihong.ui.live.adapter.GiftGridViewAdapter;
import com.tongxin.caihong.ui.live.bean.Gift;
import com.tongxin.caihong.ui.me.redpacket.WxPayBlance;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;


/**
 * 直播间-礼物列表
 */
public class FragmentGiftDialog extends BaseDialogFragment {
    // 表示刷新我的余额
    private final int REFRESH_MONEY = 1;
    public OnGridViewClickListener onGridViewClickListener;
    private Dialog dialog;
    private TextView payNum;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_MONEY: {
                    int balance = (int) coreManager.getSelf().getBalance();
                    payNum.setText(String.valueOf(balance));
                }
                break;
            }
            super.handleMessage(msg);
        }
    };
    private Button goPay;
    private ViewPager vp;
    private List<View> gridViews;
    private RadioGroup radio_group;
    /**
     * ToDo 现在不存在本地，从服务器取出
     */
    // 礼物
    private ArrayList<Gift> mGifts;
    /**
     * 用于通知更新余额的广播
     */
    private BroadcastReceiver changeSomeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LiveConstants.LIVE_SEND_REFRESH_MONEY)) {
                Message msg = new Message();
                msg.what = REFRESH_MONEY;
                mHandler.sendMessage(msg);
            }
        }
    };

    public static final FragmentGiftDialog newInstance() {
        FragmentGiftDialog fragment = new FragmentGiftDialog();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    public FragmentGiftDialog setOnGridViewClickListener(ArrayList<Gift> gifts, OnGridViewClickListener onGridViewClickListener) {
        this.mGifts = gifts;
        this.onGridViewClickListener = onGridViewClickListener;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.common_gift_dialog_layout, container, false);
        initView(rootView);
        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 创建Dialog时没有保证LayoutInflater可用，所以不能在这里创建View,
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LiveConstants.LIVE_SEND_REFRESH_MONEY);
        getActivity().registerReceiver(changeSomeBroadcastReceiver, intentFilter);
        // 使用不带Theme的构造器, 获得的dialog边框距离屏幕仍有几毫米的缝隙。
        initDialogStyle();
        return dialog;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 这里确保View已经添加到Dialog才能修改LayoutParams，宽全屏才会生效，
        // 设置宽度为屏宽, 靠近屏幕底部。
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        // 底部显示
        lp.gravity = Gravity.BOTTOM;
        // 宽度持平
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);
        initViewPager();
        // 更新余额
        updateMoney();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(changeSomeBroadcastReceiver);
    }

    private void initDialogStyle() {
        dialog = new Dialog(getActivity(), R.style.CustomGiftDialog);
        // 设置Content前设定
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 外部点击取消
        dialog.setCanceledOnTouchOutside(true);
    }

    private void initView(View rootView) {
        if (mGifts == null || mGifts.size() <= 0) {
            ToastUtil.showToast(MyApplication.getContext(), "未获取到礼物列表");
            return;
        }
        Bundle args = getArguments();
        if (args == null)
            return;
        payNum = (TextView) rootView.findViewById(R.id.pay_num);
        goPay = (Button) rootView.findViewById(R.id.go_pay);
        goPay.setText(InternationalizationHelper.getString("RECHARGE"));
        goPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 下次进来才会触发init()方法
                dialog.dismiss();
                // 充值
                startActivity(new Intent(getActivity(), WxPayBlance.class));
            }
        });
        vp = (ViewPager) rootView.findViewById(R.id.view_pager);
        //radio_group = (RadioGroup) rootView.findViewById(R.id.radio_group);
        RadioButton radioButton = (RadioButton) radio_group.getChildAt(0);
        radioButton.setChecked(true);
    }

    public void initViewPager() {
        gridViews = new ArrayList<>();
        GridView gridView1 = (GridView) getLayoutInflater().inflate(R.layout.grid_fragment_home, null);
        GiftGridViewAdapter myGridViewAdapter1 = new GiftGridViewAdapter(getActivity(), 0, 8);
        gridView1.setAdapter(myGridViewAdapter1);
        myGridViewAdapter1.setGifts(mGifts);
        myGridViewAdapter1.setOnGridViewClickListener(new GiftGridViewAdapter.OnGridViewClickListener() {
            @Override
            public void click(Gift gift) {
                if (onGridViewClickListener != null) {
                    onGridViewClickListener.click(gift);
                    // 更新余额
                    updateMoney();
                }
            }
        });
        //
        GridView gridView2 = (GridView) getLayoutInflater().inflate(R.layout.grid_fragment_home, null);
        GiftGridViewAdapter myGridViewAdapter2 = new GiftGridViewAdapter(getActivity(), 1, 8);
        gridView2.setAdapter(myGridViewAdapter2);
        myGridViewAdapter2.setGifts(mGifts);
        myGridViewAdapter2.setOnGridViewClickListener(new GiftGridViewAdapter.OnGridViewClickListener() {
            @Override
            public void click(Gift gift) {
                if (onGridViewClickListener != null) {
                    onGridViewClickListener.click(gift);
                    // 更新余额
                    updateMoney();
                }
            }
        });
        //
        GridView gridView3 = (GridView)
                getLayoutInflater().inflate(R.layout.grid_fragment_home, null);
        GiftGridViewAdapter myGridViewAdapter3 = new GiftGridViewAdapter(getActivity(), 2, 8);
        gridView3.setAdapter(myGridViewAdapter3);
        myGridViewAdapter3.setGifts(mGifts);
        myGridViewAdapter3.setOnGridViewClickListener(new GiftGridViewAdapter.OnGridViewClickListener() {
            @Override
            public void click(Gift gift) {
                if (onGridViewClickListener != null) {
                    onGridViewClickListener.click(gift);
                    // 更新余额
                    updateMoney();
                }
            }
        });
        gridViews.add(gridView1);
        gridViews.add(gridView2);
        gridViews.add(gridView3);

        // 定义viewpager的PagerAdapter
        vp.setAdapter(new PagerAdapter() {
            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                // TODO Auto-generated method stub
                return arg0 == arg1;
            }

            @Override
            public int getCount() {
                // TODO Auto-generated method stub
                return gridViews.size();
            }

            @Override
            public void destroyItem(ViewGroup container, int position,
                                    Object object) {
                // TODO Auto-generated method stub
                container.removeView(gridViews.get(position));
                //super.destroyItem(container, position, object);
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                // TODO Auto-generated method stub
                container.addView(gridViews.get(position));
                return gridViews.get(position);
            }
        });
        ///注册viewPager页选择变化时的响应事件
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int position) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageSelected(int position) {
                RadioButton radioButton = (RadioButton)
                        radio_group.getChildAt(position);
                radioButton.setChecked(true);
            }
        });
    }

    // 更新余额
    public void updateMoney() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().RECHARGE_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Balance>(Balance.class) {

                    @Override
                    public void onResponse(ObjectResult<Balance> result) {
                        DecimalFormat df = new DecimalFormat("######0.00");
                        Balance b = result.getData();
                        coreManager.getSelf().setBalance(Double.parseDouble(df.format(b.getBalance())));
                        Message msg = new Message();
                        msg.what = REFRESH_MONEY;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    public interface OnGridViewClickListener {
        void click(Gift gift);

        void dismiss();
    }
}
