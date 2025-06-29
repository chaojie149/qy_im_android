package com.tongxin.caihong.helper;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ehking.sdk.wepay.interfaces.WalletPay;
import com.ehking.sdk.wepay.net.bean.AuthType;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.webox.WeboxOrder;
import com.tongxin.caihong.bean.webox.WeboxStatus;
import com.tongxin.caihong.bean.webox.WeboxWalletDetail;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.webox.EventWeboxRechargeSuccess;
import com.tongxin.caihong.ui.webox.WeboxOpenActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.Money;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class WeboxHelper {
    private static final String TAG = "WeboxHelper";
    private static final String SP_NAME = "sk_webox";
    public static boolean ENABLE = true;
    public static String merchantId = BuildConfig.WEBOX_MERCHANT_ID;
    public static String environment = "pro";
    private static boolean DEBUG = false;

    public static void init(Activity ctx, CoreManager coreManager) {
        WalletPay.Companion.getInstance().setDebug(DEBUG);
        WalletPay.Companion.getInstance().destroy();
    }

    public static boolean isOpened(Context ctx) {
        return !TextUtils.isEmpty(getWalletId(ctx));
    }

    public static void saveWalletId(Context ctx, String walletId) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("walletId", walletId)
                .apply();
    }

    public static String getWalletId(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getString("walletId", null);
    }

    public static void clean(Context ctx) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    public static boolean checkOpened(Context ctx) {
        if (!isOpened(ctx)) {
            WeboxOpenActivity.start(ctx);
            return false;
        }
        return true;
    }

    public static boolean checkOpenedOrAsk(Context ctx) {
        if (!isOpened(ctx)) {
            SelectionFrame dialog = new SelectionFrame(ctx);
            dialog.setSomething(ctx.getString(R.string.app_name), ctx.getString(R.string.tip_yeepay_ask_open), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    WeboxOpenActivity.start(ctx);
                }
            });
            dialog.show();

            return false;
        }
        return true;
    }

    public static void sendRed(Activity ctx, CoreManager coreManager, String toUserId,
                               final String type, String pMoney, String count,
                               final String words, RequestCallBack<String> callBack) {
        String money = Money.fromYuan(pMoney);
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("toUserId", toUserId);
        request(ctx, coreManager.getConfig().WEBOX_SEND_RED, params, WeboxOrder.class, bean -> {
            WalletPay walletPay = WalletPay.Companion.getInstance();
            walletPay.init(ctx);
            walletPay.setWalletPayCallback((s, s1, s2) -> {
                if (checkSuccess(ctx, s, s1, s2)) {
                    AsyncUtils.postDelayed(callBack, c -> {
                        queryRed(ctx, coreManager, bean.getId(), () -> {
                            callBack.result(bean.getId());
                        });
                    }, 1000);
                }
            });
            walletPay.evoke(WeboxHelper.merchantId, WeboxHelper.getWalletId(ctx), bean.getToken(), AuthType.REDPACKET.name());
        });
    }

    public static void sendMucRed(Activity ctx, CoreManager coreManager, String toUserId,
                                  final String type, String pMoney, String count,
                                  final String words, RequestCallBack<String> callBack) {
        String money = Money.fromYuan(pMoney);
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("roomJid", toUserId);
        request(ctx, coreManager.getConfig().WEBOX_SEND_RED, params, WeboxOrder.class, bean -> {
            WalletPay walletPay = WalletPay.Companion.getInstance();
            walletPay.init(ctx);
            walletPay.setWalletPayCallback((s, s1, s2) -> {
                if (checkSuccess(ctx, s, s1, s2)) {
                    AsyncUtils.postDelayed(callBack, c -> {
                        queryRed(ctx, coreManager, bean.getId(), () -> {
                            callBack.result(bean.getId());
                        });
                    }, 1000);
                }
            });
            walletPay.evoke(WeboxHelper.merchantId, WeboxHelper.getWalletId(ctx), bean.getToken(), AuthType.REDPACKET.name());
        });
    }

    public static void transfer(Activity ctx, CoreManager coreManager,
                                String mTransferredUserId, String money, String words, RequestCallBack<String> callBack) {
        Map<String, String> params = new HashMap<>();
        params.put("toUserId", mTransferredUserId);
        params.put("amount", money);
        params.put("money", money);
        if (!TextUtils.isEmpty(words)) {
            params.put("remark", words);
        }
        request(ctx, coreManager.getConfig().WEBOX_TRANSFER, params, WeboxOrder.class, bean -> {
            WalletPay walletPay = WalletPay.Companion.getInstance();
            walletPay.init(ctx);
            walletPay.setWalletPayCallback((s, s1, s2) -> {
                if (checkSuccess(ctx, s, s1, s2)) {
                    AsyncUtils.postDelayed(callBack, c -> {
                        queryTransfer(ctx, coreManager, bean.getId(), () -> {
                            callBack.result(bean.getId());
                        });
                    }, 1000);
                }
            });
            walletPay.evoke(WeboxHelper.merchantId, WeboxHelper.getWalletId(ctx), bean.getToken(), AuthType.TRANSFER.name());
        });
    }

    public static void recharge(Activity ctx, CoreManager coreManager, String money) {
        HashMap<String, String> params = new HashMap<>();
        params.put("amount", Money.toCent(money));
        request(ctx, coreManager.getConfig().WEBOX_RECHARGE, params, WeboxOrder.class, bean -> {
            WalletPay walletPay = WalletPay.Companion.getInstance();
            walletPay.init(ctx);
            walletPay.setWalletPayCallback((s, s1, s2) -> {
                if (checkSuccess(ctx, s, s1, s2)) {
                    ToastUtil.showToast(ctx, R.string.recharge_success);
                    EventBus.getDefault().post(new EventWeboxRechargeSuccess());
                    ctx.finish();
                }
            });
            walletPay.evoke(WeboxHelper.merchantId, WeboxHelper.getWalletId(ctx), bean.getToken(), AuthType.RECHARGE.name());
        });
    }

    public static void queryRed(Context ctx, CoreManager coreManager, String yeepayRedId, Runnable callback) {
        Map<String, String> params = new HashMap<>();
        params.put("id", yeepayRedId);
        request(ctx, coreManager.getConfig().WEBOX_QUERY_RED, params, WeboxStatus.class, bean -> {
            if (checkSendSuccess(ctx, bean)) {
                callback.run();
            }
        });
    }

    public static void queryTransfer(Context ctx, CoreManager coreManager, String tradeNo, Runnable callback) {
        Map<String, String> params = new HashMap<>();
        params.put("id", tradeNo);
        request(ctx, coreManager.getConfig().WEBOX_QUERY_TRANSFER, params, WeboxStatus.class, bean -> {
            if (checkSendSuccess(ctx, bean)) {
                callback.run();
            }
        });
    }

    private static boolean checkSendSuccess(Context ctx, WeboxStatus bean) {
        Log.d(TAG, "checkSendSuccess() called with: bean = [" + bean + "]");
        boolean ret = TextUtils.equals(bean.getOrderStatus(), "SEND");
        if (!ret) {
            ToastUtil.showToast(ctx, bean.getOrderErrorMessage());
        }
        return ret;
    }

    private static boolean checkSuccess(Context ctx, String source, String status, String errorMessage) {
        Log.d(TAG, "checkSuccess() called with: source = [" + source + "], status = [" + status + "], errorMessage = [" + errorMessage + "]");
        boolean ret = TextUtils.equals("SUCCESS", status) || TextUtils.equals("PROCESS", status);
        if (!ret && !TextUtils.equals("CANCEL", status)) {
            ToastUtil.showToast(ctx, errorMessage);
        }
        return ret;
    }

    // 查询不需要已开户，
    public static void query(Activity ctx, CoreManager coreManager, RequestCallBack<WeboxWalletDetail> callBack) {
        HashMap<String, String> params = new HashMap<>();
        DialogHelper.showDefaulteMessageProgressDialog(ctx);
        HttpUtils.get().url(coreManager.getConfig().WEBOX_MONEY)
                .params(params)
                .build()
                .execute(new BaseCallback<WeboxWalletDetail>(WeboxWalletDetail.class) {

                    @Override
                    public void onResponse(ObjectResult<WeboxWalletDetail> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ctx, result)) {
                            if (result == null) {
                                return;
                            }
                            WeboxWalletDetail weboxWalletDetail = result.getData();
                            if (weboxWalletDetail != null) {
                                saveWalletId(ctx, weboxWalletDetail.getWalletId());
                                if (callBack != null) {
                                    callBack.result(weboxWalletDetail);
                                }
                            } else {
                                clean(ctx);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }

    public static void withdraw(Activity ctx, CoreManager coreManager, String money, String withdrawType) {
        HashMap<String, String> params = new HashMap<>();
        params.put("amount", Money.toCent(money));
        params.put("withdrawType", withdrawType);
        request(ctx, coreManager.getConfig().WEBOX_WITHDRAW, params, WeboxOrder.class, bean -> {
            WalletPay walletPay = WalletPay.Companion.getInstance();
            walletPay.init(ctx);
            walletPay.setWalletPayCallback((s, s1, s2) -> {
                if (checkSuccess(ctx, s, s1, s2)) {
                    ToastUtil.showToast(ctx, R.string.tip_withdraw_success);
                    ctx.finish();
                }
            });
            walletPay.evoke(WeboxHelper.merchantId, WeboxHelper.getWalletId(ctx), bean.getToken(), AuthType.WITHHOLDING.name());
        });
    }

    public static void bind(Activity ctx, CoreManager coreManager) {
        HashMap<String, String> params = new HashMap<>();
        requestUrl(ctx, coreManager.getConfig().WEBOX_BIND, params, AuthType.ACCESS_CARDlIST);
    }

    public static void secure(Activity ctx, CoreManager coreManager) {
        HashMap<String, String> params = new HashMap<>();
        requestUrl(ctx, coreManager.getConfig().WEBOX_SECURE, params, AuthType.ACCESS_SAFETY);
    }

    public static void requestUrl(Activity ctx, String url, Map<String, String> params, AuthType authType) {
        request(ctx, url, params, WeboxOrder.class, bean -> {
            WalletPay walletPay = WalletPay.Companion.getInstance();
            walletPay.init(ctx);
            walletPay.setWalletPayCallback((s, s1, s2) -> {
                if (checkSuccess(ctx, s, s1, s2)) {
                    Log.d(TAG, "requestUrl callback with: source = [" + s + "], status = [" + s1 + "], errorMessage = [" + s2 + "]");
                }
            });
            walletPay.evoke(WeboxHelper.merchantId, WeboxHelper.getWalletId(ctx), bean.getToken(), authType.name());
        });
    }

    public static <T> void request(Context ctx, String url, Map<String, String> params, Class<T> clazz, RequestCallBack<T> callBack) {
        if (!checkOpened(ctx)) return;
        DialogHelper.showDefaulteMessageProgressDialog(ctx);
        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<T>(clazz) {

                    @Override
                    public void onResponse(ObjectResult<T> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ctx, result)) {
                            callBack.result(result.getData());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }

    public static void deleteCert(Context ctx) {
        WalletPay.Companion.getInstance().deleteCer(ctx, merchantId, getWalletId(ctx));
    }

    public interface RequestCallBack<T> {
        void result(T t);
    }
}
