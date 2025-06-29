package com.tongxin.caihong.helper;

import android.content.Context;
import android.text.TextUtils;

import com.tongxin.caihong.bean.EventTrillShareSuccess;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

public class TrillStatisticsHelper {

    public static void play(Context ctx, CoreManager coreManager, PublicMessage message) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", message.getMessageId());
        HttpUtils.get().url(coreManager.getConfig().TRILL_ADD_PLAY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        Result.checkSuccess(ctx, result);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }

    /**
     * fromUserId为AppConstant.TRILL_INSTANT_ID的就是短视频分享的消息，
     * {@see com.chat.weichat.ui.trill.common.TriListActivity#onShare(java.lang.String, long, int)}
     *
     * @return true表示是短视频分享的消息，
     */
    private static boolean checkTrillShare(String originalUserId) {
        return TextUtils.equals(originalUserId, AppConstant.TRILL_INSTANT_ID);
    }

    public static void share(Context ctx, CoreManager coreManager, ChatMessage message) {
        if (checkTrillShare(message.getFromUserId())) {
            share(ctx, coreManager, message.getPacketId());
        }
    }

    private static void share(Context ctx, CoreManager coreManager, String trillId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", trillId);
        HttpUtils.get().url(coreManager.getConfig().TRILL_ADD_FORWARD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(ctx, result)) {
                            EventBus.getDefault().post(new EventTrillShareSuccess(trillId));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ctx);
                    }
                });
    }
}
