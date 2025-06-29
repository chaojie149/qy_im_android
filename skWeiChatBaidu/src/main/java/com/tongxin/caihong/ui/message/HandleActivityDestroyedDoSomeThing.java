package com.tongxin.caihong.ui.message;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.ui.base.CoreManager;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 处理一些界面onDestroy时需要执行的操作
 */
public class HandleActivityDestroyedDoSomeThing {

    /**
     * 面对面建群 退出当前界面需要退出群组
     */
    public static void handleFaceGroupDestroyed(String jid) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getInstance()).accessToken);
        params.put("jid", jid);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).ROOM_LOCATION_EXIT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /**
     * 定时上传位置信息
     *
     * @param userId
     */
    public static void handleUploadLocation(String userId) {
        Map<String, String> params = new HashMap<>();
        params.put("countryId", String.valueOf(Area.getDefaultCountyId()));
        Area area = Area.getDefaultProvince();
        if (area != null) {
            params.put("provinceId", String.valueOf(area.getId()));
        }
        area = Area.getDefaultCity();
        if (area != null) {
            params.put("cityId", String.valueOf(area.getId()));
            area = Area.getDefaultDistrict(area.getId());
            if (area != null) {
                params.put("areaId", String.valueOf(area.getId()));
            }
        }
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        if (latitude != 0) {
            params.put("latitude", String.valueOf(latitude));
        }
        if (longitude != 0) {
            params.put("longitude", String.valueOf(longitude));
        }

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).USER_MODIFY_LOCATION)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            PreferenceUtils.putLong(MyApplication.getContext(), Constants.LAST_UPLOAD_LOCATION_TIME + userId, TimeUtils.sk_time_current_time());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }
}
