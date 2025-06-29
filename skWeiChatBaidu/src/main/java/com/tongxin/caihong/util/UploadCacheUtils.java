package com.tongxin.caihong.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.message.ChatMessage;

import java.io.File;

/**
 * 用于缓存网络地址到本地地址的映射，
 * 代替原来的ChatMessage中的filePath,
 * 一方面记录上传文件的本地路径，
 * 一方面记录下载文件的缓存路径，
 * 因为ChatMessage中的filePath发送出去无法解决两个手机存在同路径同名不同文件的情况，
 * <p>
 * SharedPreferences是多线程安全的所以可以在下载线程调用，
 */
public class UploadCacheUtils {
    public static final String NAME_UPLOAD_CACHE = "upload_cache";

    public static SharedPreferences getSp(Context ctx) {
        return ctx.getSharedPreferences(NAME_UPLOAD_CACHE, Context.MODE_PRIVATE);
    }

    public static void save(Context ctx, String url, String filePath) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(filePath)) {
            Reporter.unreachable();
            return;
        }
        getSp(ctx).edit()
                .putString(url, filePath)
                .apply();
    }

    /**
     * 获取文件url,用于jcvideoplayer,
     * 如果没有缓存就直接返回传入的url,
     * 如果有缓存就返回文件的url, file:///开头的，
     */
    public static String getVideoUri(Context ctx, String url) {
        String local = getOrNull(ctx, url);
        if (TextUtils.isEmpty(local)) {
            return url;
        }
        File file = new File(local);
        if (!file.exists()) {
            return url;
        }
        return file.toURI().toASCIIString();
    }

    @Nullable
    private static String getOrNull(Context ctx, String url) {
        return getSp(ctx).getString(url, null);
    }

    @Nullable
    public static String get(Context ctx, String url) {
        return getOrNull(ctx, url);
    }

    @Nullable
    public static String get(Context ctx, ChatMessage message) {
        String filePath;
        if (TextUtils.isEmpty(message.getContent())) {
            // 如果是发送中的消息，肯定没有缓存不能从UploadCacheUtils获取，直接使用filePath,
            filePath = message.getFilePath();
        } else {
            filePath = UploadCacheUtils.get(ctx, message.getContent());
        }
        return filePath;
    }
}
