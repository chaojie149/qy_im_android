package com.tongxin.caihong.xmpp.helloDemon;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.util.PreferenceUtils;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.ui.base.CoreManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 华为推送
 */

public class HuaweiPushRevicer {
    public static void init(Context context) {
        /**
         * getToken(String appId, String scope), This method is used to obtain a token required for accessing HUAWEI Push Kit.
         * If there is no local AAID, this method will automatically generate an AAID when it is called because the Huawei Push server needs to generate a token based on the AAID.
         * This method is a synchronous method, and you cannot call it in the main thread. Otherwise, the main thread may be blocked.
         */
        new Thread() {
            @Override
            public void run() {
                try {
                    // read from agconnect-services.json
                    String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
                    String token = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
                    if (!TextUtils.isEmpty(token)) {
                        Log.e("push", "get token success, " + token);
                        /**
                         * 得到token,上传至服务器
                         */
                        String area = PreferenceUtils.getString(context, AppConstant.EXTRA_CLUSTER_AREA);
                        if (TextUtils.isEmpty(area)) {
                            area = "CN";
                        } else {
                            if (!area.endsWith(",")) {
                                area += ",";
                            }
                            String[] split = area.split(",");
                            if (split.length > 0) {
                                area = split[0];
                            } else {
                                area = "CN";
                            }
                        }
                        Map<String, String> params = new HashMap<>();
                        params.put("packageName", BuildConfig.APPLICATION_ID);
                        params.put("access_token", CoreManager.requireSelfStatus(context).accessToken);
                        params.put("token", token);
                        params.put("adress", area);
                        params.put("deviceId", "3");

                        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).configHw)
                                .params(params)
                                .build()
                                .execute(new BaseCallback<Void>(Void.class) {

                                    @Override
                                    public void onResponse(ObjectResult<Void> result) {
                                        if (result.getResultCode() == 1) {
                                            Log.e("push", "上传成功");
                                        } else {
                                            Log.e("push", "上传失败-->" + result.getResultMsg());
                                        }
                                    }

                                    @Override
                                    public void onError(Call call, Exception e) {
                                        Log.e("push", "上传失败");
                                    }
                                });
                    }
                } catch (ApiException e) {
                    Log.e("push", "get token failed, " + e);
                }
            }
        }.start();
    }

    public static Map<String, String> extrasToMap(String extras) {
        Map<String, String> map = new HashMap<>();
        try {
            JSONArray jsonArray = JSON.parseArray(extras);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    if (entry.getValue() != null) {
                        map.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
        } catch (Exception e) {
            Reporter.post("华为通知参数解析失败", e);
        }
        return map;
    }

}
