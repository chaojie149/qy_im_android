package com.tongxin.caihong.helper;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mobile.auth.gatewayauth.AuthRegisterViewConfig;
import com.mobile.auth.gatewayauth.AuthUIConfig;
import com.mobile.auth.gatewayauth.CustomInterface;
import com.mobile.auth.gatewayauth.PhoneNumberAuthHelper;
import com.mobile.auth.gatewayauth.PreLoginResultListener;
import com.mobile.auth.gatewayauth.TokenResultListener;
import com.mobile.auth.gatewayauth.model.TokenRet;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.SkinUtils;

/**
 * 本机号码一键登录工具类，
 */
public class PhoneAuthHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "PhoneAuthHelper";
    private Context ctx;
    private OnReady onReady;
    private OnGetToken onGetToken;
    private PhoneNumberAuthHelper mAlicomAuthHelper;
    private TokenResultListener mTokenListener;
    private boolean checkRet;
    private boolean clicked;

    private PhoneAuthHelper(Context ctx, OnReady onReady, OnGetToken onGetToken) {
        this.ctx = ctx;
        this.onReady = onReady;
        this.onGetToken = onGetToken;
        init();
    }

    public static PhoneAuthHelper createInstance(Context ctx, OnReady onReady, OnGetToken onGetToken) {
        PhoneAuthHelper ret = new PhoneAuthHelper(ctx, onReady, onGetToken);
        return ret;
    }

    public static boolean isEnabled() {
        return !TextUtils.isEmpty(BuildConfig.PHONE_AUTH_SECRET) && !Build.SUPPORTED_ABIS[0].contains("x86");
    }

    private void init() {
        if (!isEnabled()) {
            return;
        }
        /*
         *   1.init get token callback Listener
         */
        mTokenListener = new TokenResultListener() {
            @Override
            public void onTokenSuccess(final String ret) {
                AsyncUtils.runOnUiThread(this, c -> {
                    Log.e(TAG, "onTokenSuccess:" + ret);

                    /*
                     *   setText just show the result for get token。
                     *   use ret to verfiy number。
                     */
                    DialogHelper.dismissProgressDialog();
                    TokenRet tokenRet = null;
                    try {
                        tokenRet = JSON.parseObject(ret, TokenRet.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (tokenRet != null && !("600001").equals(tokenRet.getCode())) {
                        if (onGetToken != null) {
                            onGetToken.onGetToken(tokenRet.getToken());
                        }
                        mAlicomAuthHelper.quitLoginPage();
                    }
                });
            }

            @Override
            public void onTokenFailed(final String ret) {
                Log.e(TAG, "onTokenFailed:" + ret);
                AsyncUtils.runOnUiThread(this, c -> {
                    /*
                     *  setText just show the result for get token
                     *  do something when getToken failed, such as use sms verify code.
                     */
                    DialogHelper.dismissProgressDialog();
                    mAlicomAuthHelper.hideLoginLoading();
                    mAlicomAuthHelper.quitLoginPage();
                    if (DEBUG) {
                        DialogHelper.tipDialog(ctx, ret);
                    } else {
                        if (clicked) {
                            // 主动点击请求一键登录前不提示错误，
                            JSONObject json = JSON.parseObject(ret, JSONObject.class);
                            String code = json.getString("code");
                            if (!TextUtils.equals("700000", code)
                                    && !TextUtils.equals("700001", code)) {
                                // 用户主动取消登录或者点其他方式登录不提示错误，
                                String msg = json.getString("msg");
                                if (HttpUtil.isConnectedWifi(ctx)) {
                                    if (!TextUtils.isEmpty(msg)) {
                                        msg += "\n";
                                    }
                                    msg += "建议关闭wifi后重试";
                                }
                                DialogHelper.tipDialog(ctx, msg);
                            }
                        }
                    }
                });
            }
        };

        /*
         *   2.init AlicomAuthHelper with tokenListener
         */
        mAlicomAuthHelper = PhoneNumberAuthHelper.getInstance(ctx, mTokenListener);
        mAlicomAuthHelper.setAuthSDKInfo(BuildConfig.PHONE_AUTH_SECRET);
        checkRet = mAlicomAuthHelper.checkEnvAvailable();
        mAlicomAuthHelper.setAuthListener(mTokenListener);

        if (!checkRet) {
            Log.i(TAG, "当前网络不支持，请检测蜂窝网络后重试");
        }

        /*
         *   3.set debugMode when app is in debug mode, sdk will print log in debug mode
         */
        mAlicomAuthHelper.setLoggerEnable(DEBUG);


        /**
         * 控件点击事件回调
         */
        mAlicomAuthHelper.setUIClickListener((code, context, jsonObj) -> Log.e("authSDK", "OnUIControlClick:code=" + code + ", jsonObj=" + jsonObj));


        mAlicomAuthHelper.accelerateLoginPage(5000, new PreLoginResultListener() {
            @Override
            public void onTokenSuccess(final String vendor) {
                AsyncUtils.runOnUiThread(this, c -> {
                    Log.i(TAG, vendor + "预取号成功！");
                    if (onReady != null && checkRet) {
                        onReady.onReady();
                    }
                });
            }

            @Override
            public void onTokenFailed(final String vendor, final String ret) {
                AsyncUtils.runOnUiThread(this, c -> {
                    Log.i(TAG, vendor + "预取号失败:\n" + ret);
                    if (DEBUG) {
                        DialogHelper.tipDialog(ctx, ret);
                    }
                });
            }
        });
    }

    public void callPhoneAuthPage() {
        if (!isEnabled()) {
            return;
        }
        clicked = true;
        configLoginTokenPort();

        DialogHelper.showMessageProgressDialog(ctx, ctx.getString(R.string.tip_wait_phone_auth));
        mAlicomAuthHelper.getLoginToken(ctx, 5000);
    }

    private void configLoginTokenPort() {
//        initDynamicView();
        mAlicomAuthHelper.removeAuthRegisterXmlConfig();
        mAlicomAuthHelper.removeAuthRegisterViewConfig();
        mAlicomAuthHelper.addAuthRegistViewConfig("switch_acc_tv", new AuthRegisterViewConfig.Builder()
//                .setView(switchTV)
                .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_BODY)
                .setCustomInterface(new CustomInterface() {
                    @Override
                    public void onClick(Context context) {
                        mAlicomAuthHelper.quitLoginPage();
                    }
                }).build());
        int authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        if (Build.VERSION.SDK_INT == 26) {
            authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
        }
        SkinUtils.Skin skin = SkinUtils.getSkin(ctx);
        AuthUIConfig.Builder builder = new AuthUIConfig.Builder()
                .setAppPrivacyColor(Color.GRAY, Color.parseColor("#002E00"))
                .setPrivacyState(false)
                .setCheckboxHidden(true)
                .setStatusBarColor(skin.getPrimaryColor())
                .setWebViewStatusBarColor(skin.getPrimaryColor())
                .setNavColor(skin.getPrimaryColor())
                .setWebNavColor(skin.getPrimaryColor())
                .setLightColor(skin.isLight())
                .setAuthPageActIn("in_activity", "out_activity")
                .setAuthPageActOut("in_activity", "out_activity")
                .setVendorPrivacyPrefix("《")
                .setVendorPrivacySuffix("》")
                .setLogoImgPath("icon")
                .setScreenOrientation(authPageOrientation);
        if (skin.isLight()) {
            builder.setNavTextColor(ctx.getResources().getColor(R.color.text_black));
            builder.setWebNavTextColor(ctx.getResources().getColor(R.color.text_black));
            builder.setNavReturnImgPath("finish_back");
            builder.setWebNavReturnImgPath("finish_back");
        }
        if (!TextUtils.isEmpty(CoreManager.requireConfig(ctx).privacyPolicyPrefix)) {
            builder.setAppPrivacyOne("《" + ctx.getString(R.string.yinsizhengce) + "》", CoreManager.requireConfig(ctx).privacyPolicyPrefix + "privacy.html");
        }
        mAlicomAuthHelper.setAuthUIConfig(builder.create());
    }

    public interface OnGetToken {
        void onGetToken(String token);
    }

    public interface OnReady {
        void onReady();
    }
}
