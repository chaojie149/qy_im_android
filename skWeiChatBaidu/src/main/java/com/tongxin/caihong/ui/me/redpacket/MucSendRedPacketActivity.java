package com.tongxin.caihong.ui.me.redpacket;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.bean.redpacket.RedPacket;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PaySecureHelper;
import com.tongxin.caihong.helper.PayTypeHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.smarttab.SmartTabLayout;
import com.tongxin.caihong.ui.yeepay.EventYeepaySendRedSuccess;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.InputChangeListener;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.view.BottomListDialog;
import com.tongxin.caihong.view.MyGridView;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 *
 */
public class MucSendRedPacketActivity extends BaseActivity implements View.OnClickListener {
    LayoutInflater inflater;
    private SmartTabLayout smartTabLayout;
    private ViewPager viewPager;
    private List<View> views;
    private List<String> mTitleList;
    public TextView edit_person_zs;
    public EditText edit_money_zs;
    public EditText edit_words_zs;
    public Button zs;
    // 随机 type==2
    private EditText edit_count_sj;
    private EditText edit_money_sj;
    private EditText edit_words_sj;
    private Button sj;
    // 普通 type==1
    private EditText edit_count_pt;
    private EditText edit_money_pt;
    private EditText edit_words_pt;
    private Button pt;
    // 口令 type==3
    private EditText edit_count_kl;
    private EditText edit_money_kl;
    private EditText edit_words_kl;
    private Button bkl;
    // 手气 type==4
    private EditText edit_count_sq;
    private EditText edit_money_sq;
    private EditText edit_words_sq;
    private Button sq;

    private String toUserId;
    private int mCurrentItem;
    private int memberNum;
    private String type;
    private String words;
    private boolean success = false;

    private List<SingleInfo> singleInfoList;
    private ArrayList<String> arrayList = new ArrayList<>();


    public String redtype = "5";
    public String mytoUserId="";
    private String roomId;
    private RoomMember mRoomMember;
    public TextView personText;
    public EditText edit_money;
    public EditText edit_redcount;
    public EditText edit_password;
    public void  setListener(){
        edit_money = findViewById(R.id.edit_money);
        edit_redcount = findViewById(R.id.edit_redcount);
        edit_password = findViewById(R.id.edit_password);

        Button btn_sendRed = findViewById(R.id.btn_sendRed);
        roomId = getIntent().getStringExtra("roomId");
        TextView switchType = findViewById(R.id.switch_type);
        RelativeLayout person = findViewById(R.id.choose_person_layout);
//        personText = findViewById(R.id.person_text);
        switchType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomListDialog.show(mContext, Arrays.asList("专属红包","随机红包","普通红包","口令红包"),((item, position) -> {
                    switch (item){
                        case "专属红包":
                            redtype="5";
                            break;
                        case "随机红包":
                            redtype="2";
                            break;
                        case "普通红包":
                            redtype="1";
                            break;
                        case "口令红包":
                            redtype="3";
                            break;

                    }
                    switchType.setText(item);
                }));

            }
        });



        person.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String seletuserId="";
                if (mRoomMember!=null){
                    seletuserId = mRoomMember.getUserId();
                }
                SelectPersonActivity.start(mContext,seletuserId,roomId);
            }
        });

        btn_sendRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String money = null, words = null, count = null;
                money = edit_money.getText().toString();
                words = edit_redcount.getText().toString();
                count = edit_redcount.getText().toString();

                if (!TextUtils.isEmpty(count) && Integer.parseInt(count) == 0) {
                    Toast.makeText(mContext, R.string.tip_red_packet_too_slow, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > memberNum) {
                    Toast.makeText(mContext, R.string.tip_red_packet_than_member, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > coreManager.getConfig().maxRedpacktNumber) {
                    Toast.makeText(mContext, R.string.tip_red_packet_than_account, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(money) &&
                        !TextUtils.isEmpty(count) &&
                        Double.parseDouble(money) / Integer.parseInt(count) < 0.01) {
                    Toast.makeText(mContext, R.string.tip_money_too_less, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(money) &&
                        !TextUtils.isEmpty(count) &&
                        redtype.equals("1") && Double.parseDouble(money) * 100 % Integer.parseInt(count) != 0) {
                    Toast.makeText(mContext, R.string.normal_red_money_need_peace, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (eqData(money, count, words)) {
                    money = Money.fromYuan(money);
                    final String finalMoney = money;
                    final String finalWords = words;
                    final String finalCount = count;
                    String type;
                    // 随机红包与普通红包位置对调了，所以需要判断一下，其余类型红包type均为item+1
//                    if (item == 0) {
//                        type = String.valueOf(2);
//                    } else if (item == 1) {
//                        type = String.valueOf(1);
//                    } else {
//                        type = String.valueOf((item + 1));
//                    }
//                    this.type = type;
//                    this.words = words;
//                    PayTypeHelper.selectPayType(mContext, payType -> {
//                        switch (payType) {
//                            case DEFAULT:
//                                PaySecureHelper.inputPayPassword(this, getString(R.string.chat_redpacket), finalMoney, password -> {
//                                    sendRed(type, finalMoney, finalCount, finalWords, password, TextUtils.join(",", arrayList));
//                                });
//                                break;
//                            case WEBOX:
//                                WeboxHelper.sendMucRed(this, coreManager, toUserId,
//                                        type, finalMoney, finalCount, finalWords, this::result);
//                                break;
//                        }
//                    });
                }

            }
        });


    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void getRoomMember(RoomMember roomMember){
        mRoomMember = roomMember;
        if (!TextUtils.isEmpty(mRoomMember.getUserId())){
            edit_person_zs.setText(mRoomMember.getUserName());
            mytoUserId = mRoomMember.getUserId();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
//                EventBus.getDefault().register(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muc_redpacket);

        toUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        memberNum = getIntent().getIntExtra(AppConstant.EXTRA_MEMBER_NUM, 0);
        inflater = LayoutInflater.from(this);
        initActionBar();
        initView();
        setTouch(true);
        checkHasPayPassword();
        EventBusHelper.register(this);
        setListener();
    }

    private void checkHasPayPassword() {
        boolean hasPayPassword = PreferenceUtils.getBoolean(this, Constants.IS_PAY_PASSWORD_SET + coreManager.getSelf().getUserId(), true);
        if (!hasPayPassword) {
            ToastUtil.showToast(this, R.string.tip_no_pay_password);
            Intent intent = new Intent(this, ChangePayPasswordActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        findViewById(R.id.tv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.send_red_packet));
    }

    private void initView() {
        singleInfoList = new ArrayList<>();
        smartTabLayout = findViewById(R.id.muc_smarttablayout_redpacket);
        viewPager = findViewById(R.id.muc_viewpagert_redpacket);
        views = new ArrayList<View>();
        mTitleList = new ArrayList<String>();
        mTitleList.add(getString(R.string.red_random));
        mTitleList.add("专属红包");
        mTitleList.add(getString(R.string.Usual_Gift));
        mTitleList.add(getString(R.string.mes_gift));
        // mTitleList.add(getString(R.string.red_envelope));

        views.add(inflater.inflate(R.layout.muc_redpacket_pager_sj, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_zs, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_pt, null));
        views.add(inflater.inflate(R.layout.muc_redpacket_pager_kl, null));
        // views.add(inflater.inflate(R.layout.muc_redpacket_pager_sq, null));

        View temp_view = views.get(0);
        edit_count_sj = temp_view.findViewById(R.id.edit_redcount);
        edit_count_sj.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_sj));
        edit_money_sj = temp_view.findViewById(R.id.edit_money);
        edit_words_sj = temp_view.findViewById(R.id.edit_blessing);
        TextView tv_scan1 = temp_view.findViewById(R.id.tv_amount_of_money);
        sj = temp_view.findViewById(R.id.btn_sendRed);
        sj.setAlpha(0.6f);
        sj.setOnClickListener(this);

        temp_view = views.get(1);
        edit_person_zs = temp_view.findViewById(R.id.person_text);
        edit_person_zs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String seletuserId="";
                if (mRoomMember!=null){
                    seletuserId = mRoomMember.getUserId();
                }
                SelectPersonActivity.start(mContext,seletuserId,roomId);
            }
        });
        edit_money_zs = temp_view.findViewById(R.id.edit_money);
        edit_words_zs = temp_view.findViewById(R.id.edit_blessing);
        zs = temp_view.findViewById(R.id.btn_sendRed);
        zs.setAlpha(0.6f);
        zs.setOnClickListener(this);

        temp_view = views.get(2);
        edit_count_pt = temp_view.findViewById(R.id.edit_redcount);
        edit_count_pt.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_pt));
        edit_money_pt = temp_view.findViewById(R.id.edit_money);
        edit_words_pt = temp_view.findViewById(R.id.edit_blessing);
        TextView tv_scan2 = temp_view.findViewById(R.id.tv_amount_of_money);
        pt = temp_view.findViewById(R.id.btn_sendRed);
        pt.setAlpha(0.6f);
        pt.setOnClickListener(this);

        temp_view = views.get(3);
        edit_count_kl = temp_view.findViewById(R.id.edit_redcount);
        edit_count_kl.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_kl));
        edit_money_kl = temp_view.findViewById(R.id.edit_money);
        edit_words_kl = temp_view.findViewById(R.id.edit_password);
        edit_words_kl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String t = s.toString().trim();
                if (s.length() != t.length()) {
                    s.replace(0, s.length(), t);
                }
            }
        });
        // ...
        EditText edit_compatible = temp_view.findViewById(R.id.edit_compatible);
        edit_compatible.requestFocus();
        TextView tv_scan3 = temp_view.findViewById(R.id.tv_amount_of_money);
        bkl = temp_view.findViewById(R.id.btn_sendRed);
        bkl.setAlpha(0.6f);
        bkl.setOnClickListener(this);

/*
        temp_view = views.get(3);
        edit_count_sq = temp_view.findViewById(R.id.edit_redcount);
        MyGridView mgv_money = temp_view.findViewById(R.id.mgv_money);
        edit_count_sq.addTextChangedListener(new RemoveZeroTextWatcher(edit_count_sq, mgv_money, memberNum));
        edit_money_sq = temp_view.findViewById(R.id.edit_money);
        edit_words_sq = temp_view.findViewById(R.id.edit_blessing);
        TextView tv_scan4 = temp_view.findViewById(R.id.tv_amount_of_money);
        sq = temp_view.findViewById(R.id.btn_sendRed);
        sq.setAlpha(0.6f);
        sq.setOnClickListener(this);
*/

        InputChangeListener inputChangeListenerSj = new InputChangeListener(edit_money_sj, tv_scan1, sj);
        InputChangeListener inputChangeListenerPt = new InputChangeListener(edit_money_pt, tv_scan2, pt);
        InputChangeListener inputChangeListenerKl = new InputChangeListener(edit_money_kl, tv_scan3, bkl);
        // InputChangeListener inputChangeListenerSq = new InputChangeListener(edit_money_sq, tv_scan4, sq);

        // 添加输入监听
        edit_money_sj.addTextChangedListener(inputChangeListenerSj);
        edit_money_pt.addTextChangedListener(inputChangeListenerPt);
        edit_money_kl.addTextChangedListener(inputChangeListenerKl);
        // edit_money_sq.addTextChangedListener(inputChangeListenerSq);

        // 只允许输入小数点和数字
        edit_money_sj.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_money_pt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit_money_kl.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // edit_money_sq.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        viewPager.setAdapter(new PagerAdapter());
        smartTabLayout.setViewPager(viewPager);

        /**
         * 为了实现点击Tab栏切换的时候不出现动画
         * 为每个Tab重新设置点击事件
         */
        for (int i = 0; i < mTitleList.size(); i++) {
            View view = smartTabLayout.getTabAt(i);
            view.setTag(i + "");
            view.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_sendRed) {
            final int item = viewPager.getCurrentItem();
            String money = null, words = null, count = null;
            switch (item) {
                case 0: {
                    money = edit_money_sj.getText().toString();
                    words = TextUtils.isEmpty(edit_words_sj.getText().toString()) ?
                            edit_words_sj.getHint().toString() : edit_words_sj.getText().toString();
                    count = edit_count_sj.getText().toString();
                }
                break;
                case 1: {
                    money = edit_money_zs.getText().toString();
                    words = TextUtils.isEmpty(edit_words_zs.getText().toString()) ?
                            edit_words_zs.getHint().toString() : edit_words_zs.getText().toString();
                    count = "1";
                }
                break;
                case 2: {
                    money = edit_money_pt.getText().toString();
                    words = TextUtils.isEmpty(edit_words_pt.getText().toString()) ?
                            edit_words_pt.getHint().toString() : edit_words_pt.getText().toString();
                    count = edit_count_pt.getText().toString();
                }
                break;
                case 3: {
                    money = edit_money_kl.getText().toString();
                    words = TextUtils.isEmpty(edit_words_kl.getText().toString()) ?
                            edit_words_kl.getHint().toString() : edit_words_kl.getText().toString();
                    count = edit_count_kl.getText().toString();
                }
                break;
                case 4: {
                    money = edit_money_sq.getText().toString();
                    words = TextUtils.isEmpty(edit_words_sq.getText().toString()) ?
                            edit_words_sq.getHint().toString() : edit_words_sq.getText().toString();
                    count = edit_count_sq.getText().toString();

                    arrayList = new ArrayList<>();
                    String moneySum = "0";
                    for (int i = 0; i < singleInfoList.size(); i++) {
                        if (TextUtils.isEmpty(singleInfoList.get(i).getMoneyCount())) {
                            Toast.makeText(this, R.string.tip_red_packet_no_equal2, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (Double.valueOf(singleInfoList.get(i).getMoneyCount()) == 0) {
                            Toast.makeText(this, R.string.tip_red_packet_no_equal3, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        moneySum = StringUtils.add(singleInfoList.get(i).getMoneyCount(), moneySum);
                        arrayList.add(singleInfoList.get(i).getMoneyCount());
                    }

                    if (new BigDecimal(money).compareTo(new BigDecimal(moneySum)) != 0) {
                        Toast.makeText(this, R.string.tip_red_packet_no_equal, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                break;
            }

            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) == 0) {
                Toast.makeText(this, R.string.tip_red_packet_too_slow, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > memberNum) {
                Toast.makeText(this, R.string.tip_red_packet_than_member, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(count) && Integer.parseInt(count) > coreManager.getConfig().maxRedpacktNumber) {
                Toast.makeText(this, R.string.tip_red_packet_than_account, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(money) &&
                    !TextUtils.isEmpty(count) &&
                    Double.parseDouble(money) / Integer.parseInt(count) < 0.01) {
                Toast.makeText(this, R.string.tip_money_too_less, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(money) &&
                    !TextUtils.isEmpty(count) &&
                    item == 1 && Double.parseDouble(money) * 100 % Integer.parseInt(count) != 0) {
                Toast.makeText(this, R.string.normal_red_money_need_peace, Toast.LENGTH_SHORT).show();
                return;
            }

            if (eqData(money, count, words)) {
                money = Money.fromYuan(money);
                final String finalMoney = money;
                final String finalWords = words;
                final String finalCount = count;
                String type;
                // 随机红包与普通红包位置对调了，所以需要判断一下，其余类型红包type均为item+1
                if (item == 0) {
                    type = String.valueOf(2);
                } else if (item == 1) {
                    type = String.valueOf(5);
                } else if (item==2){
                    type = String.valueOf(1);
                }else{
                    type=String.valueOf(3);
                }
                this.type = type;
                this.words = words;
                PayTypeHelper.selectPayType(mContext, payType -> {
                    switch (payType) {
                        case DEFAULT:
                            PaySecureHelper.inputPayPassword(this, getString(R.string.chat_redpacket), finalMoney, password -> {
                                sendRed(type, finalMoney, finalCount, finalWords, password, TextUtils.join(",", arrayList));
                            });
                            break;
                        case WEBOX:
                            WeboxHelper.sendMucRed(this, coreManager, toUserId,
                                    type, finalMoney, finalCount, finalWords, this::result);
                            break;
                    }
                });
            }
        } else {
            int index = Integer.parseInt(v.getTag().toString());
            if (mCurrentItem != index) {
                mCurrentItem = index;
                hideKeyboard();
            }
            viewPager.setCurrentItem(index, false);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive() && this.getCurrentFocus() != null) {
            if (this.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    private boolean eqData(String money, String count, String words) {
        // 虽然上面已经判断了money与count等参数，
        // 但不排除用户在点击发红包按钮前迅速将红包个数删除这样的迷之操作，这里在判断下
        if (TextUtils.isEmpty(money)) {
            ToastUtil.showToast(mContext, getString(R.string.need_input_money));
            return false;
        } else if (TextUtils.isEmpty(count)) {
            ToastUtil.showToast(mContext, getString(R.string.need_red_packet_count));
            return false;
        } else if (Double.parseDouble(money) > coreManager.getConfig().maxRedpacktAmount * Integer.valueOf(count) || Double.parseDouble(money) <= 0) {
            ToastUtil.showToast(mContext, getString(R.string.red_packet_range, coreManager.getConfig().maxRedpacktAmount * Integer.valueOf(count)));
            return false;
        } else if (TextUtils.isEmpty(words)) {
            return false;
        }
        return true;
    }

    public void sendRed(String type, String pMoney, String count, String words, String payPassword, String moneyListStr) {
        if (!coreManager.isLogin()) {
            return;
        }
//        type = "5";
        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        String money = Money.fromYuan(pMoney);
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("roomJid", toUserId);
        if (mRoomMember!=null&&!TextUtils.isEmpty(mRoomMember.getUserId())) {
            params.put("toUserId", mRoomMember.getUserId());
        }
        if (Integer.valueOf(type) == 4) {
            params.put("moneyListStr", moneyListStr);
        }
        PaySecureHelper.generateParam(
                this, payPassword, params,
                "" + type + money + count + words + toUserId,
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_pay_secure_place_holder, t.getMessage()));
                }, (p, code) -> {
                    HttpUtils.get().url(coreManager.getConfig().REDPACKET_SEND)
                            .params(p)
                            .build()
                            .execute(new BaseCallback<RedPacket>(RedPacket.class) {
                                @Override
                                public void onResponse(ObjectResult<RedPacket> result) {
                                    DialogHelper.dismissProgressDialog();
                                    if (Result.checkSuccess(mContext, result)) {
                                        RedPacket redPacket = result.getData();
                                        String objectId = redPacket.getId();
                                        result(objectId);
                                    } else if (result.getResultCode() == 104001) {
                                        // 余额不足
                                        SelectionFrame selectionFrame = new SelectionFrame(mContext);
                                        selectionFrame.setSomething(getString(R.string.title_balance_not_enough), getString(R.string.tip_is_pay),
                                                getString(R.string.cancel), getString(R.string.recharge), new SelectionFrame.OnSelectionFrameClickListener() {
                                                    @Override
                                                    public void cancelClick() {
                                                    }

                                                    @Override
                                                    public void confirmClick() {
                                                        startActivity(new Intent(mContext, WxPayBlance.class));
                                                    }
                                                });
                                        selectionFrame.show();
                                    }
                                }

                                @Override
                                public void onError(Call call, Exception e) {
                                    DialogHelper.dismissProgressDialog();
                                    ToastUtil.showNetError(mContext, e);
                                }
                            });
                });
    }

    private void result(String objectId) {
        if (success) {
            // 以免重复处理，
            return;
        }
        success = true;
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_RED);
        if (mRoomMember!=null&&!TextUtils.isEmpty(mRoomMember.getUserId())) {
            message.setToUserName(mRoomMember.getUserName()+"_"+mRoomMember.getUserId());
        }
        message.setFromUserId(coreManager.getSelf().getUserId());
        message.setFromUserName(coreManager.getSelf().getNickName());
        message.setTimeSend(TimeUtils.sk_time_current_time());
        message.setContent(words); // 祝福语
        message.setFilePath(type);// 用FilePath来储存红包类型
        // 群组发送普通红包
        message.setFileSize(1);   // 用filesize来储存红包状态
        message.setObjectId(objectId); // 红包id
        Intent intent = new Intent();
        intent.putExtra(AppConstant.EXTRA_CHAT_MESSAGE, message.toJsonString());
        setResult(viewPager.getCurrentItem() == 0 ? ChatActivity.REQUEST_CODE_SEND_RED_PSQ : ChatActivity.REQUEST_CODE_SEND_RED_KL, intent);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventYeepaySendRedSuccess message) {
        result(message.id);
    }

    private class RemoveZeroTextWatcher implements TextWatcher {
        private final EditText editText;
        private MyGridView gridView;
        private int memberNum;

        RemoveZeroTextWatcher(EditText editText) {
            this.editText = editText;
        }

        RemoveZeroTextWatcher(EditText editText, MyGridView myGridView, int member) {
            this.editText = editText;
            this.gridView = myGridView;
            this.memberNum = member;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // 删除开头的0，
            int end = 0;
            for (int i = 0; i < editable.length(); i++) {
                char ch = editable.charAt(i);
                if (ch == '0') {
                    end = i + 1;
                } else {
                    break;
                }
            }
            if (end > 0) {
                editable.delete(0, end);
                editText.setText(editable);
            }
            if (gridView != null) {
                if (!TextUtils.isEmpty(editable.toString()) && Integer.valueOf(editable.toString()) > memberNum) {
                    Toast.makeText(MucSendRedPacketActivity.this, R.string.tip_red_packet_than_member, Toast.LENGTH_SHORT).show();
                    gridView.setAdapter(new MoneyEditText(0));
                    return;
                }
                gridView.setAdapter(TextUtils.isEmpty(editable.toString()) || Integer.valueOf(editable.toString()) == 0 ?
                        new MoneyEditText(0) : new MoneyEditText(Integer.valueOf(editable.toString())));
            }
        }
    }

    private class PagerAdapter extends androidx.viewpager.widget.PagerAdapter {

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView(views.get(position));
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ((ViewGroup) container).addView(views.get(position));
            return views.get(position);
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitleList.get(position);
        }
    }

    private class MoneyEditText extends BaseAdapter {

        public MoneyEditText(int dataSize) {
            singleInfoList.clear();
            for (int j = 0; j < dataSize; j++) {
                singleInfoList.add(new SingleInfo());
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return singleInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return singleInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_red_money, parent, false);
            }
            TextView tvRedNo = ViewHolder.get(view, R.id.tvRedNo);
            EditText red_money_tv = ViewHolder.get(view, R.id.red_money_tv);
            tvRedNo.setText(String.valueOf(position + 1));
            if (red_money_tv.getTag() instanceof TextWatcher) {
                red_money_tv.removeTextChangedListener((TextWatcher) red_money_tv.getTag());
            }
            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.toString().contains(".")) {
                        if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                            s = s.toString().subSequence(0,
                                    s.toString().indexOf(".") + 3);
                            red_money_tv.setText(s);
                            red_money_tv.setSelection(s.length());
                        }
                    }

                    if (!TextUtils.isEmpty(s) && s.toString().trim().startsWith(".")) {
                        s = "0" + s;
                        red_money_tv.setText(s);
                        red_money_tv.setSelection(1);
                    }

                    if (s.toString().startsWith("0")
                            && s.toString().trim().length() > 1) {
                        if (!s.toString().startsWith(".", 1)) {
                            red_money_tv.setText(s.subSequence(0, 1));
                            red_money_tv.setSelection(1);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String money = s.toString().trim();
                    if (!TextUtils.isEmpty(money)) {
                        singleInfoList.get(position).setMoneyCount(money);
                    }
                }
            };
            red_money_tv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            red_money_tv.addTextChangedListener(textWatcher);
            red_money_tv.setTag(textWatcher);
            return view;
        }
    }

    class SingleInfo {
        private String moneyCount;

        public String getMoneyCount() {
            return moneyCount;
        }

        public void setMoneyCount(String money) {
            moneyCount = money;
        }
    }
}