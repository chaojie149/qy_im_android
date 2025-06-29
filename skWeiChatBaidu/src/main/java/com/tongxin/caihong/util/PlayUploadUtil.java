package com.tongxin.caihong.util;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.UploadFileResult;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.UploadService;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * create by zq
 * copy from play
 * 封装上传类
 */
public class PlayUploadUtil {

    public static void uploadSingle(Context context, String str, UploadSingleCallback uploadSingleCallback) {
        AsyncUtils.doAsync(context, e -> {

        }, c -> {
            List<String> list = new ArrayList<>();
            list.add(str);

            String result = new UploadService().uploadFile(list);
            if (TextUtils.isEmpty(result)) {
                c.uiThread(r -> {
                    DialogHelper.dismissProgressDialog();
                    uploadSingleCallback.uploadSingleFail();
                });
            }

            UploadFileResult uploadFileResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(context, uploadFileResult, true);
            if (success) {
                c.uiThread(r -> {
                    DialogHelper.dismissProgressDialog();
                    if (isHasData(uploadFileResult.getData().getImages())) {
                        uploadSingleCallback.uploadSingleSuccess(callBackSingleData(uploadFileResult.getData().getImages()));
                    } else if (isHasData(uploadFileResult.getData().getAudios())) {
                        uploadSingleCallback.uploadSingleSuccess(callBackSingleData(uploadFileResult.getData().getAudios()));
                    } else if (isHasData(uploadFileResult.getData().getVideos())) {
                        uploadSingleCallback.uploadSingleSuccess(callBackSingleData(uploadFileResult.getData().getVideos()));
                    } else if (isHasData(uploadFileResult.getData().getFiles())) {
                        uploadSingleCallback.uploadSingleSuccess(callBackSingleData(uploadFileResult.getData().getFiles()));
                    } else if (isHasData(uploadFileResult.getData().getOthers())) {
                        uploadSingleCallback.uploadSingleSuccess(callBackSingleData(uploadFileResult.getData().getOthers()));
                    } else {
                        uploadSingleCallback.uploadSingleFail();
                    }
                });
            }
        });
    }

    public static void uploadMulti(Context context, List<String> list, UploadMultiCallback uploadMultiCallback) {
        AsyncUtils.doAsync(context, e -> {

        }, c -> {
            String result = new UploadService().uploadFile(list);
            if (TextUtils.isEmpty(result)) {
                c.uiThread(r -> {
                    uploadMultiCallback.uploadMultiFail();
                });
            }

            UploadFileResult uploadFileResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(context, uploadFileResult, true);
            if (success) {
                c.uiThread(r -> {
                    if (isHasData(uploadFileResult.getData().getImages())) {
                        uploadMultiCallback.uploadMultiSuccess(callBackMultiData(uploadFileResult.getData().getImages()));
                    } else if (isHasData(uploadFileResult.getData().getAudios())) {
                        uploadMultiCallback.uploadMultiSuccess(callBackMultiData(uploadFileResult.getData().getAudios()));
                    } else if (isHasData(uploadFileResult.getData().getVideos())) {
                        uploadMultiCallback.uploadMultiSuccess(callBackMultiData(uploadFileResult.getData().getVideos()));
                    } else if (isHasData(uploadFileResult.getData().getFiles())) {
                        uploadMultiCallback.uploadMultiSuccess(callBackMultiData(uploadFileResult.getData().getFiles()));
                    } else if (isHasData(uploadFileResult.getData().getOthers())) {
                        uploadMultiCallback.uploadMultiSuccess(callBackMultiData(uploadFileResult.getData().getOthers()));
                    } else {
                        uploadMultiCallback.uploadMultiFail();
                    }
                });
            }
        });
    }

    private static boolean isHasData(List<UploadFileResult.Sources> data) {
        if (data != null && data.size() > 0) {
            return true;
        }
        return false;
    }

    private static String callBackSingleData(List<UploadFileResult.Sources> data) {
        return data.get(0).getOriginalUrl();
    }

    private static List<String> callBackMultiData(List<UploadFileResult.Sources> data) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            list.add(data.get(i).getOriginalUrl());
        }
        return list;
    }

    public interface UploadSingleCallback {
        void uploadSingleFail();

        void uploadSingleSuccess(String result);
    }

    public interface UploadMultiCallback {
        void uploadMultiFail();

        void uploadMultiSuccess(List<String> result);
    }
}
