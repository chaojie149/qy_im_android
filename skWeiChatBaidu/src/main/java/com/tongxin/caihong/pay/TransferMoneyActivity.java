package com.tongxin.caihong.pay;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.Transfer;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.PaySecureHelper;
import com.tongxin.caihong.helper.PayTypeHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.EventTransfer;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.me.redpacket.ChangePayPasswordActivity;
import com.tongxin.caihong.ui.me.redpacket.WxPayBlance;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.ui.yeepay.EventYeepayTransferSuccess;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EditTextUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.InputChangeListener;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.view.KeyBoad;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.VerifyDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 转账
 */
public class TransferMoneyActivity extends BaseActivity {
    private String mTransferredUserId, mTransferredName;

    private ImageView mTransferredIv;
    private TextView mTransferredTv;

    private String money, words;// 转账金额与转账说明
    private TextView mMoneyTv;
    private TextView mTransferDescTv, mTransferDescClickTv;

    private EditText et_transfer;
    private KeyBoad keyBoad;
    private boolean isUiCreat = false;
    private boolean success = false;
    private String number;

    public static void start(Context context, String userId, String name) {
        Intent intent = new Intent(context, TransferMoneyActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, name);
        context.startActivity(intent);
    }

    public static void start(Context context, String userId, String name, String number) {
        Intent intent = new Intent(context, TransferMoneyActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, name);
        intent.putExtra(AppConstant.EXTRA_USER_ACCOUNT, number);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_money);
        mTransferredUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mTransferredName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
        number = getIntent().getStringExtra(AppConstant.EXTRA_USER_ACCOUNT);
        initActionBar();
        initView();
        initEvent();
        initKeyBoad();
        checkHasPayPassword();

        EventBusHelper.register(this);
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
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.transfer_money));
    }

    private void initView() {
        TextView tv_number = findViewById(R.id.tv_number);
        tv_number.setVisibility(TextUtils.isEmpty(number) ? View.GONE : View.VISIBLE);
        if (!TextUtils.isEmpty(number)) {
            tv_number.setText(number);
        }
        mTransferredIv = findViewById(R.id.tm_iv);
        mTransferredTv = findViewById(R.id.tm_tv);
        AvatarHelper.getInstance().displayAvatar(mTransferredUserId, mTransferredIv);
        mTransferredTv.setText(mTransferredName);

        mMoneyTv = findViewById(R.id.transfer_je_tv);
        mMoneyTv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);// 允许输入数字与小数点
        mTransferDescTv = findViewById(R.id.transfer_desc_tv);
        mTransferDescClickTv = findViewById(R.id.transfer_edit_desc_tv);

        et_transfer = findViewById(R.id.et_transfer);
        // 禁止输入框复制粘贴
        EditTextUtil.disableCopyAndPaste(et_transfer);
        keyBoad = new KeyBoad(TransferMoneyActivity.this, getWindow().getDecorView(), et_transfer);
    }

    private void initEvent() {
        mTransferDescClickTv.setOnClickListener(v -> {
            VerifyDialog verifyDialog = new VerifyDialog(mContext);
            verifyDialog.setVerifyClickListener(getString(R.string.transfer_money_desc)
                    , ""
                    , getString(R.string.transfer_desc_max_length_10)
                    , words
                    , 10
                    , new VerifyDialog.VerifyClickListener() {
                        @Override
                        public void cancel() {

                        }

                        @Override
                        public void send(String str) {
                            words = str;
                            if (TextUtils.isEmpty(words)) {
                                mTransferDescTv.setText("");
                                mTransferDescTv.setVisibility(View.GONE);
                                mTransferDescClickTv.setText(getString(R.string.transfer_money_desc));
                            } else {
                                mTransferDescTv.setText(str);
                                mTransferDescTv.setVisibility(View.VISIBLE);
                                mTransferDescClickTv.setText(getString(R.string.transfer_modify));
                            }
                            keyBoad.show();
                        }

                    });
            verifyDialog.setOkButton(R.string.sure);
            keyBoad.dismiss();
            Window window = verifyDialog.getWindow();

            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); // 软键盘弹起
            }
            verifyDialog.show();
        });

        ButtonColorChange.colorChange(this, findViewById(R.id.transfer_btn));
        findViewById(R.id.transfer_btn).setOnClickListener(v -> {
            money = et_transfer.getText().toString().trim();

            if (TextUtils.isEmpty(money) || Double.parseDouble(money) <= 0) {
                Toast.makeText(mContext, getString(R.string.transfer_input_money), Toast.LENGTH_SHORT).show();
                return;
            }

            money = Money.fromYuan(money);

            if (Double.parseDouble(money) > coreManager.getConfig().maxTransferAmount || Double.parseDouble(money) <= 0) {
                ToastUtil.showToast(mContext, getString(R.string.transfer_range, coreManager.getConfig().maxTransferAmount));
                return;
            }
            PayTypeHelper.selectPayType(mContext, type -> {
                switch (type) {
                    case DEFAULT:
                        PaySecureHelper.inputPayPassword(this, getString(R.string.transfer_money_to_someone, mTransferredName), money, password -> transfer(money, words, password));
                        break;
                    case WEBOX:
                        WeboxHelper.transfer(this, coreManager, mTransferredUserId,
                                money, words, this::result);
                        break;
                }
            });
        });
    }

    private void initKeyBoad() {
        et_transfer.setFocusable(true);
        et_transfer.setOnFocusChangeListener((v, hasFocus) -> {
            if (keyBoad != null && isUiCreat) {
                keyBoad.refreshKeyboardOutSideTouchable(!hasFocus);
            } else if (isUiCreat) {
                keyBoad.show();
            }
            if (hasFocus) {
                et_transfer.post(() -> {
                    keyBoad.show();
                });
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(et_transfer.getWindowToken(), 0);
            }
        });

        et_transfer.addTextChangedListener(new InputChangeListener(et_transfer, null, null));
        et_transfer.setOnClickListener(v -> {
            if (keyBoad != null) {
                keyBoad.show();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        isUiCreat = true;
    }

    public void transfer(String money, final String words, String payPassword) {
        if (!coreManager.isLogin()) {
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        Map<String, String> params = new HashMap();
        params.put("toUserId", mTransferredUserId);
        params.put("money", money);
        if (!TextUtils.isEmpty(words)) {
            params.put("remark", words);
        }

        PaySecureHelper.generateParam(
                this, payPassword, params,
                "" + mTransferredUserId + money + (words == null ? "" : words),
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_pay_secure_place_holder, t.getMessage()));
                }, (p, code) -> {
                    HttpUtils.get().url(coreManager.getConfig().SKTRANSFER_SEND_TRANSFER)
                            .params(p)
                            .build()
                            .execute(new BaseCallback<Transfer>(Transfer.class) {

                                @Override
                                public void onResponse(ObjectResult<Transfer> result) {
                                    DialogHelper.dismissProgressDialog();
                                    if (Result.checkSuccess(mContext, result)) {
                                        Transfer transfer = result.getData();
                                        String objectId = transfer.getId();
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
        message.setType(XmppMessage.TYPE_TRANSFER);
        message.setFromUserId(coreManager.getSelf().getUserId());
        message.setFromUserName(coreManager.getSelf().getNickName());
        message.setToUserId(mTransferredUserId);
        message.setContent(money);// 转账金额
        message.setFilePath(words); // 转账说明
        message.setObjectId(objectId); // 红包id
        CoreManager.updateMyBalance();
        Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), mTransferredUserId);
        if (friend != null && friend.getStatus() == Friend.STATUS_23) {
            // 本地标记 我将对方删除 || 对方将我删除，消息页已过滤了该status，将该好友更新为陌生人
            FriendDao.getInstance().updateFriendStatus(friend.getOwnerId(), friend.getUserId(), Friend.STATUS_UNKNOW);
        }
        if (friend == null) {
            FriendDao.getInstance().createNewFriend(mTransferredUserId, mTransferredName);
        }
        if (TextUtils.isEmpty(number)) {
            // 消息页面处理
            EventBus.getDefault().post(new EventTransfer(message));
        } else {
            message.setDecrypted(true);
            message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));
            message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            message.setTimeSend(TimeUtils.sk_time_current_time());
            ChatMessageDao.getInstance().saveNewSingleChatMessage(coreManager.getSelf().getUserId(), mTransferredUserId, message);
            coreManager.sendChatMessage(mTransferredUserId, message);
        }
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventYeepayTransferSuccess message) {
        result(message.id);
    }
}
