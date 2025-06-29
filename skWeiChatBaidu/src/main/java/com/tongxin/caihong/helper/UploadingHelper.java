package com.tongxin.caihong.helper;

import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.bean.UploadFileResult;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.GetFileSizeUtil;
import com.tongxin.caihong.util.Md5Util;
import com.tongxin.caihong.util.TanX;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 普通文件上传类
 */
public class UploadingHelper {
    /**
     * @param userId
     * @param file
     * @param onUpFileListener
     * @param isCheckedFileMd5 是否检查过文件md5值
     */
    public static void uploadFile(String userId, final File file, final OnUpFileListener onUpFileListener, boolean isCheckedFileMd5) {
        if (!file.exists()) {
            onUpFileListener.onFailure(MyApplication.getContext().getString(R.string.alert_not_have_file), file.getAbsolutePath());
        }

        if (TextUtils.isEmpty(userId)) {
            onUpFileListener.onFailure(MyApplication.getContext().getString(R.string.tip_user_id_empty), file.getAbsolutePath());
        }

        if (onUpFileListener == null) {
            onUpFileListener.onFailure(MyApplication.getContext().getString(R.string.tip_upload_listener_empty), file.getAbsolutePath());
        }

        if (!isCheckedFileMd5) {
            Log.e("HTTP", "上传文件，此文件未检查过文件md5值，判断是否需要检查");
            if (GetFileSizeUtil.getFileSize(file) > 1 * 1048576) {
                // 文件大于10M，准备校验文件md5
                Log.e("HTTP", "文件大小为-->" + GetFileSizeUtil.getFileSize(file) / 1048576 + " M，大于10M，准备检查文件md5值");
                checkFileMd5(userId, file, onUpFileListener);
                return;
            } else {
                Log.e("HTTP", "文件大小为-->" + GetFileSizeUtil.getFileSize(file) / 1048576 + " M，小于等于10M，不检查文件md5值，直接上传");
            }
        } else {
            Log.e("HTTP", "上传文件，此文件检查过md5值，服务端无匹配，直接上传");
        }

        TanX.Log("上传文件：" + file.getAbsolutePath());
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("validTime", "-1");// 文件有效期

        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getInstance()).UPLOAD_URL)
                .params(params)
                .params("files", file)
                .build()
                .execute(new FileCallback<UploadFileResult>(UploadFileResult.class) {
                    @Override
                    public void onResponse(UploadFileResult result) {
                        String url;
                        //上传成功
                        if (result == null || result.getResultCode() != Result.CODE_SUCCESS || result.getData() == null
                                || result.getSuccess() != result.getTotal()) {
                            onUpFileListener.onFailure(MyApplication.getContext().getString(R.string.upload_failed), file.getAbsolutePath());
                            return;
                        } else {
                            UploadFileResult.Data data = result.getData();
                            if (!TextUtils.isEmpty(url = getImagesUrl(data))) {
                            } else if (!TextUtils.isEmpty(url = getVideosUrl(data))) {
                            } else if (!TextUtils.isEmpty(url = getAudiosUrl(data))) {
                            } else if (!TextUtils.isEmpty(url = getFilesUrl(data))) {
                            } else if (!TextUtils.isEmpty(url = getOthersUrl(data))) {
                                url = getOthersUrl(data);
                            }
                        }
                        if (TextUtils.isEmpty(url)) {
                            // 返回成功，但是却获取不到对应的URL，服务器返回值异常<概率极小>
                            Log.i("roamer", "上传文件成功了 但是URL 是空的");
                            onUpFileListener.onFailure(MyApplication.getContext().getString(R.string.tip_upload_result_empty), file.getAbsolutePath());
                        } else {
                            Log.i("roamer", "上传文件成功了");
                            onUpFileListener.onSuccess(url, file.getAbsolutePath());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        onUpFileListener.onFailure(MyApplication.getContext().getString(R.string.upload_failed), file.getAbsolutePath());
                    }
                });
    }

    private static void checkFileMd5(String userId, final File file, final OnUpFileListener onUpFileListener) {
        Map<String, String> params = new HashMap<>();
        Log.e("HTTP", "文件md5值-->" + Md5Util.getFileMd5(file));
        params.put("md5Code", Md5Util.getFileMd5(file));
        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getContext()).UPLOAD_MD5_CHECK)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        String url = result.getData();
                        if (TextUtils.isEmpty(url)) {
                            Log.e("HTTP", "检查文件md5值，服务端未返回结果，继续上传");
                            uploadFile(userId, file, onUpFileListener, true);
                        } else {
                            Log.e("HTTP", "检查文件md5值，服务端返回结果-->" + url + "，不上传，进行后续处理");
                            onUpFileListener.onSuccess(url, file.getAbsolutePath());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e("HTTP", "检查文件md5值，接口调用失败，继续上传");
                        uploadFile(userId, file, onUpFileListener, true);
                    }
                });
    }

    private static String getAudiosUrl(UploadFileResult.Data data) {
        TanX.Log("语音格式");
        if (data.getAudios() != null && data.getAudios().size() > 0) {
            return data.getAudios().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getFilesUrl(UploadFileResult.Data data) {
        TanX.Log("文件格式");
        if (data.getFiles() != null && data.getFiles().size() > 0) {
            return data.getFiles().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getVideosUrl(UploadFileResult.Data data) {
        TanX.Log("视频格式");
        if (data.getVideos() != null && data.getVideos().size() > 0) {
            return data.getVideos().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getImagesUrl(UploadFileResult.Data data) {
        TanX.Log("图片格式");
        if (data.getImages() != null && data.getImages().size() > 0) {
            return data.getImages().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getOthersUrl(UploadFileResult.Data data) {
        TanX.Log("其他格式");
        if (data.getOthers() != null && data.getOthers().size() > 0) {
            return data.getOthers().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    public interface OnUpFileListener {
        void onSuccess(String url, String filePath);

        void onFailure(String err, String filePath);
    }
}
