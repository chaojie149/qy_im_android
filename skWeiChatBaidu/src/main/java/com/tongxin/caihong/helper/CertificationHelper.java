package com.tongxin.caihong.helper;

import android.content.Context;
import android.util.Log;

import com.alibaba.security.cloud.CloudRealIdentityTrigger;
import com.alibaba.security.realidentity.ALRealIdentityCallback;
import com.alibaba.security.realidentity.ALRealIdentityResult;
import com.tongxin.caihong.bean.CertificationToken;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;

import okhttp3.Call;

public class CertificationHelper {
    private static final String TAG = "CertificationHelper";
    private static final String SP_NAME = "sk_certification";
    public static boolean ENABLE;

    public static boolean isAuth(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getBoolean("auth", false);
    }

    public static void saveAuth(Context ctx, boolean isOpened) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("auth", isOpened)
                .apply();
    }

    public static void init(Context ctx) {
        CloudRealIdentityTrigger.initialize(ctx.getApplicationContext());//CloudRealIdentityTrigger
    }

    public static void startCloudReal(Context ctx, CoreManager coreManager, Runnable onSuccess) {
        DialogHelper.showDefaulteMessageProgressDialog(ctx);
        HashMap<String, String> params = new HashMap<>();

        HttpUtils.get().url(coreManager.getConfig().CERTIFICATION_TOKEN)
                .params(params)
                .build()
                .execute(new BaseCallback<CertificationToken>(CertificationToken.class) {

                    @Override
                    public void onResponse(ObjectResult<CertificationToken> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ctx, result)) {
                            cloudReal(ctx, coreManager, result.getData().getToken(), onSuccess);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }

    private static void cloudReal(Context ctx, CoreManager coreManager, String token, Runnable onSuccess) {
        CloudRealIdentityTrigger.start(ctx, token, new ALRealIdentityCallback() {
            @Override
            public void onAuditResult(ALRealIdentityResult alRealIdentityResult, String s) {
                Log.e(TAG, "onAuditResult() called with: alRealIdentityResult = [" + alRealIdentityResult + "], s = [" + s + "]");

                if (alRealIdentityResult == ALRealIdentityResult.AUDIT_PASS) {
                    // 认证通过。建议接入方调用实人认证服务端接口DescribeVerifyResult来获取最终的认证状态，并以此为准进行业务上的判断和处理
                    certification(ctx, coreManager, onSuccess);
                } else if (alRealIdentityResult == ALRealIdentityResult.AUDIT_FAIL) {
                    // 认证不通过。建议接入方调用实人认证服务端接口DescribeVerifyResult来获取最终的认证状态，并以此为准进行业务上的判断和处理
                } else if (alRealIdentityResult == ALRealIdentityResult.AUDIT_NOT) {
                    // 未认证，具体原因可通过code来区分（code取值参见下方表格），通常是用户主动退出或者姓名身份证号实名校验不匹配等原因，导致未完成认证流程
                }
            }
        });

    }

    private static void certification(Context ctx, CoreManager coreManager, Runnable onSuccess) {
        DialogHelper.showDefaulteMessageProgressDialog(ctx);
        HashMap<String, String> params = new HashMap<>();

        HttpUtils.get().url(coreManager.getConfig().CERTIFICATION)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ctx, result)) {
                            ToastUtil.showToast(ctx, R.string.tip_certification_success);
                            CertificationHelper.saveAuth(ctx, true);
                            onSuccess.run();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }

    public static void askCertificationCancel(Context ctx, CoreManager coreManager, Runnable onSuccess) {
        SelectionFrame selectionFrame = new SelectionFrame(ctx);
        selectionFrame.setSomething(null, ctx.getString(R.string.tip_certification_cancel), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                certificationCancel(ctx, coreManager, onSuccess);
            }
        });
        selectionFrame.show();
    }

    private static void certificationCancel(Context ctx, CoreManager coreManager, Runnable onSuccess) {
        DialogHelper.showDefaulteMessageProgressDialog(ctx);
        HashMap<String, String> params = new HashMap<>();

        HttpUtils.get().url(coreManager.getConfig().CERTIFICATION_CANCEL)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(ctx, result)) {
                            ToastUtil.showToast(ctx, R.string.tip_certification_cancel_success);
                            CertificationHelper.saveAuth(ctx, false);
                            onSuccess.run();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }
}
