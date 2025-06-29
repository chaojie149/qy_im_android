package com.tongxin.caihong.ui.account;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.LoginRegisterResult;
import com.tongxin.caihong.bean.QQLoginResult;
import com.tongxin.caihong.bean.WXUploadResult;
import com.tongxin.caihong.bean.event.MessageLogin;
import com.tongxin.caihong.helper.AccountSwitchHelper;
import com.tongxin.caihong.helper.CertificationHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.LoginSecureHelper;
import com.tongxin.caihong.helper.PhoneAuthHelper;
import com.tongxin.caihong.helper.PrivacySettingHelper;
import com.tongxin.caihong.helper.QQHelper;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.helper.WeboxHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.me.SetConfigActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AppUtils;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.view.MergerStatus;
import com.tongxin.caihong.view.VerifyDialog;
import com.client.im.wxapi.WXEntryActivity;
import com.tencent.tauth.Tencent;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 登陆界面
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    public static final String THIRD_TYPE_PHONE_AUTH = "3";
    public static final String THIRD_TYPE_WECHAT = "2";
    public static final String THIRD_TYPE_QQ = "1";

    private TextView tvPrefix;
    private EditText etAccount;
    private String mobilePrefix ="86";
    /**
     * thirdToken 为空 third 为false
     * 正常账号密码登录
     * thirdToken 为空 third 为true
     * 此情况不存在
     * <p>
     * thirdToken 不为空 third 为false
     * 微信QQ登录授权成功(config内的isNoRegisterThirdLogin为0(即第三方登录需要绑定手机号))
     * 但login方法内的resultCode返回1040305(第三方登录未绑定手机号)时跳转至注册界面，注册界面在点击“绑定已有账号”回到该界面时出现
     * thirdToken 不为空 third 为true
     * 本机号一键登录、微信QQ登录授权成功时出现，根据thirdTokenType区分
     */
    private String thirdToken;
    private String thirdTokenType;
    private boolean third;
    // 切换账号功能相关
    private String autoLoginUserId;
    // 取消授权登录
    private boolean isCanceledAuth;
    private VerifyDialog mVerifyDialog;

    public LoginActivity() {
        noLoginRequired();
    }

    public static void bindThird(Context ctx, String thirdToken, String thirdTokenType, boolean testLogin) {
        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.putExtra("thirdToken", thirdToken);
        intent.putExtra("thirdTokenType", thirdTokenType);
        intent.putExtra("testLogin", testLogin);
        ctx.startActivity(intent);
    }

    public static void bindThird(Context ctx, String thirdToken, String thirdTokenType) {
        bindThird(ctx, thirdToken, thirdTokenType, false);
    }

    public static void bindThird(Context ctx, WXUploadResult thirdToken) {
        bindThird(ctx, JSON.toJSONString(thirdToken), THIRD_TYPE_WECHAT, true);
    }

    public static void bindThird(Context ctx, QQLoginResult thirdToken) {
        bindThird(ctx, JSON.toJSONString(thirdToken), THIRD_TYPE_QQ, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        PreferenceUtils.putBoolean(this, Constants.LOGIN_CONFLICT, false);// 重置登录冲突记录

        thirdToken = getIntent().getStringExtra("thirdToken");
        thirdTokenType = getIntent().getStringExtra("thirdTokenType");
        initActionBar();
        initView();
        // 切换账号功能已废弃
        // loadAccount();

        /**
         * 微信QQ登录授权成功，再次回到登录界面，直接调用login方法
         */
        if (!TextUtils.isEmpty(thirdToken)
                && getIntent().getBooleanExtra("testLogin", false)) {
            // 第三方进来直接登录，
            // 清空手机号以标记是第三方登录，
            etAccount.setText("");
            login(true);
        }
        EventBusHelper.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果没有保存用户定位信息，那么去地位用户当前位置
        if (!MyApplication.getInstance().getBdLocationHelper().isLocationUpdate()) {
            MyApplication.getInstance().getBdLocationHelper().requestLocation();
        }
    }

    private void initActionBar() {
        ((MergerStatus) findViewById(R.id.mergerStatus)).hideShadow();
        getSupportActionBar().hide();
        if (AccountSwitchHelper.getInSwitching() != null) {
            // 切换账号，添加新账号
            findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        } else {
            findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        }
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (TextUtils.isEmpty(thirdToken)) {
            tvTitle.setText(getString(R.string.welcome_login));
        } else {
            tvTitle.setText(getString(R.string.bind_old_account));
            findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
            findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
        }

        TextView tvRight = findViewById(R.id.tv_title_right);
        tvRight.setVisibility(View.VISIBLE);
        tvRight.setText(R.string.settings_server_address);
        tvTitle.setOnLongClickListener(v -> {
            tvRight.setVisibility(View.VISIBLE);
            return false;
        });
        tvRight.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, SetConfigActivity.class);
            startActivity(intent);
        });
        // 定制包隐藏设置服务器按钮，
        if (!AppConfig.isChat() || !BuildConfig.DEBUG) {
            // 为方便测试，留个启用方法，adb shell命令运行"setprop log.tag.ChatServer D"启用，
            if (!Log.isLoggable("ChatServer", Log.DEBUG)) {
//                tvRight.setVisibility(View.GONE);
            }
        }
    }

    private void initView() {
        tvPrefix = findViewById(R.id.tvPrefix);
        mobilePrefix = PreferenceUtils.getString(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tvPrefix.setText("+" + mobilePrefix);
        etAccount = findViewById(R.id.etAccount);
        UsernameHelper.initEditText(etAccount, coreManager.getConfig().registerUsername);
        // 下一步
        Button btnNext = findViewById(R.id.btnNext);
        ButtonColorChange.colorChange(mContext, btnNext);
        btnNext.setOnClickListener(this);
        // 注册
        TextView tvRegister = findViewById(R.id.tvRegister);
        ButtonColorChange.textChange(mContext, findViewById(R.id.tvRegister));
        // 第三方登录
        if (TextUtils.isEmpty(thirdToken)) {
            findViewById(R.id.ivWeChatLogin).setOnClickListener(this);
            if (QQHelper.ENABLE) {
                findViewById(R.id.ivQQLogin).setOnClickListener(this);
            } else {
                findViewById(R.id.flQQLogin).setVisibility(View.GONE);
            }
        } else {
            findViewById(R.id.llThirdLoginParent).setVisibility(View.INVISIBLE);
        }
        if (!coreManager.getConfig().thirdLogin) {
            findViewById(R.id.llThirdLoginParent).setVisibility(View.INVISIBLE);
        } else {
            if (TextUtils.isEmpty(BuildConfig.WECHAT_APP_ID)) {
                findViewById(R.id.flWeChatLogin).setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(BuildConfig.QQ_APP_ID)) {
                findViewById(R.id.flQQLogin).setVisibility(View.GONE);
            }
        }
        if (coreManager.getConfig().registerUsername) {
            findViewById(R.id.llPrefix).setVisibility(View.GONE);
            findViewById(R.id.tvPhoneAuthLogin).setVisibility(View.GONE);
            findViewById(R.id.llThirdLoginParent).setVisibility(View.INVISIBLE);
        } else {
            tvPrefix.setOnClickListener(this);
            initPhoneAuthLogin();
        }

        findViewById(R.id.main_content).setOnClickListener(this);

        if (coreManager.getConfig().isOpenRegister) {
            if (TextUtils.isEmpty(thirdToken)) {
                tvRegister.setOnClickListener(this);
            } else {
                // 第三方登录的不需要这个注册按钮，登录后没有账号直接跳到注册，
                findViewById(R.id.tvPhoneAuthLogin).setVisibility(View.GONE);
                findViewById(R.id.llRegister).setVisibility(View.GONE);
            }
        } else {
            findViewById(R.id.tvPhoneAuthLogin).setVisibility(View.GONE);
            findViewById(R.id.llRegister).setVisibility(View.GONE);
            findViewById(R.id.llThirdLoginParent).setVisibility(View.GONE);
            findViewById(R.id.rlCannotRegister).setVisibility(View.VISIBLE);
        }
    }

/*
    private void loadAccount() {
        if (AccountSwitchHelper.getInSwitching() != null) {
            // 从切换账号页面跳来的，不在登录页做自动登录，
            return;
        }
        String current = UserSp.getInstance(mContext).getUserId("");
        if (TextUtils.isEmpty(current)) {
            return;
        }
        etAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (autoLoginUserId != null) {
                    autoLoginUserId = null;
                    mPasswordEdit.setText("");
                }
            }
        });
        mPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (autoLoginUserId != null) {
                    autoLoginUserId = null;
                    mPasswordEdit.setText("");
                }
            }
        });
        ToggleButton tbEye = findViewById(R.id.tbEye);
        tbEye.setOnClickListener(v -> {
            if (autoLoginUserId != null) {
                autoLoginUserId = null;
                mPasswordEdit.setText("");
            }
        });
        AsyncUtils.doAsync(this, c -> {
            if (AccountSwitchHelper.load(mContext).isEmpty()) {
                return;
            }
            UserSp currentSp = UserSp.getInstance(mContext, current);
            if (!TextUtils.isEmpty(currentSp.getLoginToken())) {
                // 如果没有登录信息，可能是被删除了，不加载当前用户，
                User currentUser = UserDao.getInstance().getUserByUserId(current);
                if (currentUser != null) {
                    c.uiThread(r -> {
                        r.etAccount.setText(currentUser.getTelephoneNoAreaCode());
                        int areaCode = currentSp.getAreaCode();
                        r.mobilePrefix = areaCode;
                        r.tvPrefix.setText("+" + areaCode);
                        tbEye.setChecked(false);
                        if (!TextUtils.isEmpty(thirdToken)) {
                            autoLoginUserId = null;
                            mPasswordEdit.setText("");
                        } else {
                            r.mPasswordEdit.setText("************");
                            autoLoginUserId = current;
                        }
                    });
                }
            }
            List<Map<String, Object>> data;
            Set<String> exists = AccountSwitchHelper.load(mContext);
            data = new ArrayList<>(exists.size());
            for (String userId : exists) {
                User user = UserDao.getInstance().getUserByUserId(userId);
                if (user == null) {
                    continue;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("phone", user.getTelephoneNoAreaCode());
                map.put("user", user);
                map.put("delete", user);
                data.add(map);
            }
            if (data.isEmpty()) {
                return;
            }
            View llPopupAnchor = findViewById(R.id.llPopupAnchor);
            View ivChangeAccount = findViewById(R.id.ivChangeAccount);
            ivChangeAccount.setVisibility(View.VISIBLE);
            ivChangeAccount.setOnClickListener(new View.OnClickListener() {
                ListPopupWindow listPopupWindow;

                @Override
                public void onClick(View v) {
                    if (listPopupWindow != null) {
                        listPopupWindow.show();
                        return;
                    }
                    SimpleAdapter adapter = makeAdapter();
                    listPopupWindow = new ListPopupWindow(mContext);
                    listPopupWindow.setAdapter(adapter);
                    listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT);
                    listPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
                    listPopupWindow.setAnchorView(llPopupAnchor);
                    listPopupWindow.setModal(true);
                    listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                            listPopupWindow.dismiss();
                            User user = (User) data.get(position).get("user");
                            etAccount.setText(user.getTelephoneNoAreaCode());
                            int areaCode = UserSp.getInstance(mContext, user.getUserId()).getAreaCode();
                            mobilePrefix = areaCode;
                            tvPrefix.setText("+" + areaCode);
                            tbEye.setChecked(false);
                            if (!TextUtils.isEmpty(thirdToken)) {
                                autoLoginUserId = null;
                                mPasswordEdit.setText("");
                            } else {
                                mPasswordEdit.setText("************");
                                autoLoginUserId = user.getUserId();
                            }
                        }
                    });
                    listPopupWindow.show();
                }

                private SimpleAdapter makeAdapter() {
                    SimpleAdapter adapter = new SimpleAdapter(mContext, data, R.layout.item_account_switch_login,
                            new String[]{"phone", "user", "delete"},
                            new int[]{R.id.tvName, R.id.ivHead, R.id.ivDelete});
                    adapter.setViewBinder((view, data1, textRepresentation) -> {
                        if (view instanceof ImageView && data1 instanceof User) {
                            User user = (User) data1;
                            if (view.getId() == R.id.ivHead) {
                                AvatarHelper.getInstance().displayAvatar(user.getNickName(), user.getUserId(), (ImageView) view, false);
                            } else if (view.getId() == R.id.ivDelete) {
                                view.setOnClickListener(v1 -> {
                                    SelectionFrame dialog = new SelectionFrame(v1.getContext());
                                    dialog.setSomething(getString(R.string.app_name), getString(R.string.tip_delete_account), new SelectionFrame.OnSelectionFrameClickListener() {
                                        @Override
                                        public void cancelClick() {

                                        }

                                        @Override
                                        public void confirmClick() {
                                            AccountSwitchHelper.removeExistsUser(v1.getContext(), user.getUserId());
                                            listPopupWindow.dismiss();
                                            // 重建，
                                            if (TextUtils.equals(user.getUserId(), autoLoginUserId)) {
                                                autoLoginUserId = null;
                                                etAccount.setText("");
                                                mPasswordEdit.setText("");
                                            }
                                            listPopupWindow.setAdapter(makeAdapter());
                                        }
                                    });
                                    dialog.show();
                                });
                            }
                            return true;
                        }
                        return false;
                    });
                    return adapter;
                }
            });
        });
    }
*/

    private void initPhoneAuthLogin() {
        View tvPhoneAuthLogin = findViewById(R.id.tvPhoneAuthLogin);
        if (PhoneAuthHelper.isEnabled()) {
            tvPhoneAuthLogin.setOnClickListener(view -> {
                // 本机号码一键登录
                PhoneAuthHelper phoneAuthHelper;
                phoneAuthHelper = PhoneAuthHelper.createInstance(this, () -> {
                    Log.i(TAG, "initPhoneAuthLogin: 一键登录准备完成");
                }, token -> {
                    thirdToken = token;
                    thirdTokenType = THIRD_TYPE_PHONE_AUTH;
                    login(true);
                });
                phoneAuthHelper.callPhoneAuthPage();
            });
        } else {
            tvPhoneAuthLogin.setVisibility(View.GONE);
        }

        ButtonColorChange.textChange(mContext, findViewById(R.id.tvPhoneAuthLogin));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvPrefix:
                Intent intent = new Intent(this, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.btnNext:
                if (!HttpUtil.isGprsOrWifiConnected(mContext)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_network_error));
                    return;
                }
                login(false);
                break;
            case R.id.tvRegister:
                register();
                break;
            case R.id.ivWeChatLogin:
                if (!AppUtils.isAppInstalled(mContext, "com.tencent.mm")) {
                    Toast.makeText(mContext, getString(R.string.tip_no_wx_chat), Toast.LENGTH_SHORT).show();
                } else {
                    WXEntryActivity.wxLogin(this);
                }
                break;
            case R.id.ivQQLogin:
                if (!QQHelper.qqInstalled(mContext)) {
                    Toast.makeText(mContext, getString(R.string.tip_no_qq_chat), Toast.LENGTH_SHORT).show();
                } else {
                    QQHelper.qqLogin(this);
                }
                break;
            case R.id.main_content:
                // 点击空白区域隐藏软键盘
                InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.hideSoftInputFromWindow(findViewById(R.id.main_content).getWindowToken(), 0); //强制隐藏键盘
                }
                break;
        }
    }

    private void login(boolean third) {
        this.third = third;
        login();
    }

    private void login() {
        // 切换账号功能已废弃
/*
        if (!third && !TextUtils.isEmpty(autoLoginUserId)) {
            // 使用本地保存的loginToken去主界面自动登录
            AccountSwitchHelper.loadOldUser(mContext, autoLoginUserId);
            coreManager.setSelf(null);
            int userStatus = LoginHelper.prepareUser(mContext, coreManager);
            Intent intent = new Intent();
            switch (userStatus) {
                case LoginHelper.STATUS_USER_FULL:
                case LoginHelper.STATUS_USER_NO_UPDATE:
                case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
                    boolean login = PreferenceUtils.getBoolean(mContext, Constants.LOGIN_CONFLICT, false);
                    if (login) {// 登录冲突，退出app再次进入，跳转至历史登录界面
                        intent.setClass(mContext, LoginHistoryActivity.class);
                    } else {
                        intent.setClass(mContext, MainActivity.class);
                    }
                    break;
                case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
                    intent.setClass(mContext, LoginHistoryActivity.class);
                    break;
                case LoginHelper.STATUS_NO_USER:
                default:
                    return;// must return
            }
            startActivity(intent);
            finish();
            return;
        }
*/
        PreferenceUtils.putString(this, Constants.AREA_CODE_KEY, mobilePrefix);
        final String account = etAccount.getText().toString().trim();
        if (!third) {
            // 两种情况third为false
            // 1.输入账号，点击下一步
            // 2.第三方登录，服务器要求第三方账号必须绑定手机号，在注册页面点击绑定已有账号回到登录页时，点击下一步，此情况下third为false，thirdToken不为空
            if (!UsernameHelper.verify(this, account, coreManager.getConfig().registerUsername)) {
                return;
            }
            //  以上两种情况，直接跳转到下一步页面，跳转之前先校验下账号是否已注册吧
            verifyAccount(account);
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("xmppVersion", "1");
        // 附加信息+
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
        // 地址信息
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        if (MyApplication.IS_OPEN_CLUSTER) {
            // 服务端集群需要
            String area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA);
            if (!TextUtils.isEmpty(area)) {
                params.put("area", area);
            }
        }

        LoginSecureHelper.secureLogin(
                this, coreManager, String.valueOf(mobilePrefix), account, "", thirdToken, thirdTokenType, third,
                params,
                t -> {
                    DialogHelper.dismissProgressDialog();
                    ToastUtil.showToast(this, this.getString(R.string.tip_login_secure_place_holder, t.getMessage()));
                }, result -> {
                    DialogHelper.dismissProgressDialog();
                    if (!Result.checkSuccess(getApplicationContext(), result)) {
                        if (Result.checkError(result, Result.CODE_THIRD_NO_EXISTS)) {
                            // 如果返回1040306表示这个IM账号不存在，跳到注册页面让用户注册IM账号并绑定微信，
                            register();
                        } else if (Result.checkError(result, Result.CODE_THIRD_NO_PHONE)) {
                            // 微信没有绑定IM账号，跳到注册，注册页有回来登录老账号的按钮，
                            register();
                            finish();
                        }
                        return;
                    }
                    if (!TextUtils.isEmpty(result.getData().getAuthKey())) {
                        isCanceledAuth = false;
                        DialogHelper.showMessageProgressDialogAddCancel(this, getString(R.string.tip_need_auth_login), (DialogInterface dialogInterface) -> {
                            isCanceledAuth = true;
                        });
                        CheckAuthLoginRunnable authLogin = new CheckAuthLoginRunnable(result.getData().getAuthKey(), account, "");
                        waitAuth(authLogin);
                        return;
                    }
                    afterLogin(result, account, "");
                }
        );
    }

    private void verifyAccount(String account) {
        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", account);
        params.put("verifyType", "1");
        HttpUtils.get().url(coreManager.getConfig().VERIFY_TELEPHONE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            LoginActivity2.bindThird(mContext, account, thirdToken, thirdTokenType);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void register() {
        RegisterActivity.registerFromThird(
                this,
                thirdToken,
                thirdTokenType
        );
    }

    private void afterLogin(ObjectResult<LoginRegisterResult> result, String account, String digestPwd) {
        // 下面其实没必要判断了，走到这个方法内必定是第三方登录
        if (third) {
            if (MyApplication.IS_SUPPORT_SECURE_CHAT
                    && result.getData().getIsSupportSecureChat() == 1) {// 新用户才需要，老用户不支持端到端加密，不需要
                // SecureFlag
                // 微信/QQ登录，如未绑定手机号码，则需要输入账号密码进行绑定登录，如账号未注册，走注册流程
                // 微信/QQ登录，如绑定手机号码，可直接登录，此时将因为不需要输入登录密码，将无法解密服务端返回的私钥，需要让用户输入密码解密
                mVerifyDialog = new VerifyDialog(mContext);
                mVerifyDialog.setVerifyClickListener(getString(R.string.login)
                        , getString(R.string.input_password_to_decrypt_keys)
                        , ""
                        , new VerifyDialog.VerifyClickListener() {
                            @Override
                            public void cancel() {
                                mVerifyDialog.dismiss();
                                String sAreaCode = result.getData().getAreaCode();
                                String rTelephone = result.getData().getTelephone();
                                if (!TextUtils.isEmpty(rTelephone)) {
                                    if (!TextUtils.isEmpty(sAreaCode) && rTelephone.startsWith(sAreaCode)) {
                                        rTelephone = rTelephone.substring(sAreaCode.length());
                                    }
                                    FindPwdActivity.start(mContext,sAreaCode, rTelephone);
                                } else {
                                    FindPwdActivity.start(mContext, mobilePrefix, account);
                                }
                            }

                            @Override
                            public void send(String str) {
                                checkPasswordWXAuthCodeLogin(str, result, account, digestPwd);
                            }
                        });
                mVerifyDialog.setDismiss(false);
                mVerifyDialog.setCancelButton(R.string.forget_password);
                mVerifyDialog.show();
            } else {
                start("", result, account, digestPwd);
            }
        } else {
            start("", result, account, digestPwd);
        }
/*
        boolean success = LoginHelper.setLoginUser(mContext, coreManager, account, digestPwd, result);
        if (success) {
            // SecureFlag 本地先保存明文登录密码，之后要使用
            if (third) {
                if (MyApplication.IS_SUPPORT_SECURE_CHAT
                        && result.getData().getIsSupportSecureChat() == 1) {// 新用户才需要，老用户不支持端到端加密，不需要
                    // 微信/QQ登录，如未绑定手机号码，则需要输入账号密码进行绑定登录，如账号未注册，走注册流程
                    // 微信/QQ登录，如绑定手机号码，可直接登录，此时将因为不需要输入登录密码，将无法解密服务端返回的私钥，需要让用户输入密码解密
                    mVerifyDialog = new VerifyDialog(mContext);
                    mVerifyDialog.setVerifyClickListener(getString(R.string.input_password_to_decrypt_keys), new VerifyDialog.VerifyClickListener() {
                        @Override
                        public void cancel() {
                            mVerifyDialog.dismiss();
                            startActivity(new Intent(mContext, FindPwdActivity.class));
                        }

                        @Override
                        public void send(String str) {
                            checkPasswordWXAuthCodeLogin(str, result);
                        }
                    });
                    mVerifyDialog.setDismiss(false);
                    mVerifyDialog.setCancelButton(R.string.forget_password);
                    mVerifyDialog.show();
                } else {
                    start("", result);
                }
            } else {
                start(mPasswordEdit.getText().toString().trim(), result);
            }
        } else {
            // 偶现该异常，退出app重进又没有了
            // java.sql.SQLException: Unable to run insert stmt on object com.chat.weichat.bean.User@d9c51ec: INSERT INTO
            // `user` (`account` ,`areaId` ,`attCount` ,`birthday` ,`cityId` ,`company_id` ,`countryId` ,`description` ,`fansCount` ,`friendsCount` ,`integral` ,`integralTotal` ,`isAuth` ,`level` ,`money` ,`moneyTotal` ,`msgBackGroundUrl` ,`nickName` ,`offlineTime` ,`password` ,`phone` ,`provinceId` ,`setAccountCount` ,`sex` ,`showLastLoginTime` ,`status` ,`telephone` ,`userId` ,`userType` ,`vip` )
            // VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ToastUtil.showToast(mContext, result.getResultMsg());
        }
*/
    }

    private void waitAuth(CheckAuthLoginRunnable authLogin) {
        authLogin.waitAuthHandler.postDelayed(authLogin, 3000);
    }

    private void checkPasswordWXAuthCodeLogin(String password, ObjectResult<LoginRegisterResult> registerResult,
                                              String extra1, String extra2) {

        LoginHelper.saveUserForThirdSmsVerifyPassword(mContext, coreManager,
                extra1, extra2, registerResult);

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("password", LoginPassword.encodeMd5(password));

        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        HttpUtils.get().url(coreManager.getConfig().USER_VERIFY_PASSWORD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            mVerifyDialog.dismiss();
                            start(password, registerResult, extra1, extra2);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void start(String password, ObjectResult<LoginRegisterResult> result, String account, String digestPwd) {
        LoginHelper.setLoginUser(mContext, coreManager, account, digestPwd, result);

        if (!TextUtils.isEmpty(result.getData().getHeadimgurl())) {
            saveAvatar(result.getData().getUserId(), result.getData().getHeadimgurl());
        }

        LoginRegisterResult.Settings settings = result.getData().getSettings();
        MyApplication.getInstance().initPayPassword(result.getData().getUserId(), result.getData().getPayPassword());
        WeboxHelper.saveWalletId(mContext, result.getData().getWalletId());
        CertificationHelper.saveAuth(mContext, result.getData().getRealNameCertified() == 1);
        PrivacySettingHelper.setPrivacySettings(mContext, settings);
        MyApplication.getInstance().initMulti();

        // startActivity(new Intent(mContext, DataDownloadActivity.class));
        DataDownloadActivity.start(mContext, result.getData().getIsupdate(), password);
        finish();
    }

    /**
     * 第三方登录，将头像保存至本地
     */
    private void saveAvatar(String userId, String headImageUrl) {
        ImageLoadHelper.loadBitmapCenterCropDontAnimate(MyApplication.getContext(), headImageUrl, b -> {
            String path = FileUtil.saveBitmap(b);
            uploadAvatar(userId, new File(path));
        }, e -> {

        });
    }

    /**
     * 第三方登录，上传头像，上传成功与否都不管
     */
    private void uploadAvatar(String userId, File file) {
        if (!file.exists()) {
            // 文件不存在
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        HttpUtils.post().url(coreManager.getConfig().AVATAR_UPLOAD_URL)
                .params(params)
                .params("files", file)
                .build(true, true)
                .execute(new FileCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(Void result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN:
                if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS) {
                    return;
                }
                mobilePrefix = data.getStringExtra(Constants.MOBILE_PREFIX);
                tvPrefix.setText("+" + mobilePrefix);
                break;
            case com.tencent.connect.common.Constants.REQUEST_LOGIN:
            case com.tencent.connect.common.Constants.REQUEST_APPBAR:
                Tencent.onActivityResultData(requestCode, resultCode, data, QQHelper.getLoginListener(mContext));
                break;
        }
    }

    private class CheckAuthLoginRunnable implements Runnable {
        private final String account;
        private final String digestPwd;
        private Handler waitAuthHandler = new Handler();
        private int waitAuthTimes = 10;
        private String authKey;

        public CheckAuthLoginRunnable(String authKey, String account, String digestPwd) {
            this.authKey = authKey;
            this.account = account;
            this.digestPwd = digestPwd;
        }

        @Override
        public void run() {
            HttpUtils.get().url(coreManager.getConfig().CHECK_AUTH_LOGIN)
                    .params("authKey", authKey)
                    .build(true, true)
                    .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {
                        @Override
                        public void onResponse(ObjectResult<LoginRegisterResult> result) {
                            if (Result.checkError(result, Result.CODE_AUTH_LOGIN_SCUESS)) {
                                DialogHelper.dismissProgressDialog();
                                login();
                            } else if (Result.checkError(result, Result.CODE_AUTH_LOGIN_FAILED_1)) {
                                if (!isCanceledAuth) {
                                    waitAuth(CheckAuthLoginRunnable.this);
                                }
                            } else {
                                DialogHelper.dismissProgressDialog();
                                if (!TextUtils.isEmpty(result.getResultMsg())) {
                                    ToastUtil.showToast(mContext, result.getResultMsg());
                                } else {
                                    ToastUtil.showToast(mContext, R.string.tip_server_error);
                                }
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                            ToastUtil.showErrorNet(mContext);
                        }
                    });
        }
    }
}
