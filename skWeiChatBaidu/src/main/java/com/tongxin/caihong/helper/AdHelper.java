package com.tongxin.caihong.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.bean.ad.Advertising;
import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.secure.MD5;
import com.tongxin.caihong.view.ad.AdView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AdHelper {
    public static final boolean DEBUG_SPLASH = false;
    public static final boolean DEBUG_BANNER = false;
    private static final String TAG = "AdHelper";
    private static final String SP_NAME = "sk_advertising";
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<String, ReentrantLock> lockMap = new WeakHashMap<String, ReentrantLock>();

    public static void preloadAd(Context ctx, AdType adType) {
        AsyncUtils.doAsync(ctx, executor, c -> {
            requestAd(ctx, adType);
        });
    }

    public static void bind(AppCompatActivity activity, AdView adView, AdType adType) {
        adView.bind(activity.getLifecycle());
        initAdView(activity, adView, adType);
    }

    /**
     * fragment绑定光生命同期不够，还要重写onHiddenChanged方法，
     */
    public static void bind(Fragment fragment, AdView adView, AdType adType) {
        adView.bind(fragment.getLifecycle());
        initAdView(fragment.requireContext(), adView, adType);
    }

    public static void initAdView(Context ctx, AdView adView, AdType adType) {
        if (!UserSp.getInstance(ctx).isLogged()) {
            // 没登录没有token不展示广告，
            return;
        }
        List<Advertising> adList = null;
        Advertising ad = null;
        switch (adType) {
            case Splash: {
                ad = loadAd(ctx, getKey(adType));
                break;
            }
            case Banner: {
                adList = loadAdList(ctx, getKey(adType));
                break;
            }
        }
        if (ad != null) {
            adList = Collections.singletonList(ad);
        }
        if (adList == null || adList.isEmpty() || adList.get(0).isShowed()) {
            // 本地没有就在线加载，
            List<Advertising> finalAdList = adList;
            AsyncUtils.doAsync(ctx, executor, c -> {
                List<Advertising> request;
                try {
                    request = requestAd(ctx, adType);
                } catch (Exception e) {
                    // 在线获取失败就用已缓存的，哪怕已经展示过，
                    request = finalAdList;
                }
                List<Advertising> finalRequest = request;
                c.uiThread(r -> {
                    adView.load(finalRequest);
                });
            });
        } else {
            adView.load(adList);
        }
    }

    @WorkerThread
    @SuppressWarnings("SameParameterValue")
    private static List<Advertising> requestAd(Context ctx, AdType type) throws Exception {
        List<Advertising> ret = new ArrayList<>();
        switch (type) {
            case Splash:
                runInLock(type.name(), () -> {
                    ret.add(requestSingleAd(ctx, type));
                });
                break;
            case Banner:
                runInLock(type.name(), () -> {
                    ret.addAll(requestMultiAd(ctx, type));
                });
                break;
        }
        return ret;
    }

    public static void setShowed(Context ctx, AdType adType) {
        Log.d(TAG, "setShowed() called with: ctx = [" + ctx + "], adType = [" + adType + "]");
        List<Advertising> list = null;
        Advertising exists = null;
        switch (adType) {
            case Splash:
                exists = loadAd(ctx, getKey(adType));
                break;
            case Banner:
                list = loadAdList(ctx, getKey(adType));
                break;
        }
        if (list != null && !list.isEmpty()) {
            exists = list.get(0);
        }
        if (exists == null) {
            return;
        }
        exists.setShowed(true);
        if (list != null) {
            saveAdList(ctx, getKey(adType), list);
        } else {
            saveAd(ctx, getKey(adType), exists);
        }
    }

    @NonNull
    private static String getKey(AdType type) {
        String ret = null;
        switch (type) {
            case Splash:
                ret = "KEY_SPLASH";
                break;
            case Banner:
                ret = "KEY_BANNER";
                break;
        }
        return ret;
    }

    private static String getApiUrl(AdType type, AppConfig appConfig) {
        String ret = null;
        switch (type) {
            case Splash:
                ret = appConfig.ADWARE_INDEX;
                break;
            case Banner:
                ret = appConfig.ADWARE_BANNER;
                break;
        }
        return ret;
    }

    @NonNull
    @WorkerThread
    private static Advertising requestSingleAd(Context ctx, AdType type) {
        String url = getApiUrl(type, CoreManager.requireConfig(ctx));
        String key = getKey(type);
        Advertising existsAd = loadAd(ctx, key);
        if (!TextUtils.isEmpty(existsAd.getId()) && !existsAd.isShowed()) {
            // 上一个广告存在且没展示过就不获取下一个，
            return existsAd;
        }
        final Advertising[] ret = {null};
        HashMap<String, String> params = new HashMap<>();
        params.put("count", String.valueOf(existsAd.getCount()));

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .executeSync(new BaseCallback<Advertising>(Advertising.class, false) {
                    @Override
                    public void onResponse(ObjectResult<Advertising> result) {
                        if (Result.checkSuccess(ctx, result, false)) {
                            ret[0] = result.getData();
                            saveAd(ctx, key, result.getData());
                            try {
                                downloadAd(ctx, result.getData());
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // ignored,
                    }
                });
        if (ret[0] == null) {
            ret[0] = new Advertising();
        }
        return ret[0];
    }

    @NonNull
    @WorkerThread
    private static List<Advertising> requestMultiAd(Context ctx, AdType type) {
        String url = getApiUrl(type, CoreManager.requireConfig(ctx));
        String key = getKey(type);
        List<Advertising> existsList = loadAdList(ctx, key);
        if (!existsList.isEmpty() && !existsList.get(0).isShowed()) {
            // 上一个广告存在且没展示过就不获取下一个，
            return existsList;
        }
        List<Advertising> ret = new ArrayList<>();
        HashMap<String, String> params = new HashMap<>();
        params.put("count", String.valueOf(0));

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .executeSync(new ListCallback<Advertising>(Advertising.class, false) {
                    @Override
                    public void onResponse(ArrayResult<Advertising> result) {
                        if (Result.checkSuccess(ctx, result, false)) {
                            ret.addAll(result.getData());
                            saveAdList(ctx, key, result.getData());
                            try {
                                downloadAdList(ctx, result.getData());
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // ignored,
                    }
                });
        return ret;
    }

    /**
     * 广告展示后增加曝光量，
     *
     * @param id
     */
    public static void incBurst(Context ctx, String id) {
        HashMap<String, String> params = new HashMap<>();
        params.put("id", id);

        HttpUtils.get().url(CoreManager.requireConfig(ctx).ADWARE_INC_BURST)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(ctx, result, false)) {
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // ignored,
                    }
                });

    }

    /**
     * 广告点击后增加点击量，
     *
     * @param id
     */
    public static void incClick(Context ctx, String id) {
        HashMap<String, String> params = new HashMap<>();
        params.put("id", id);

        HttpUtils.get().url(CoreManager.requireConfig(ctx).ADWARE_INC_CLICK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(ctx, result, false)) {
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // ignored,
                    }
                });

    }

    public static void showImage(
            Context ctx,
            File file,
            ImageView view
    ) {
        ImageLoadHelper.showFile(ctx, file, view);
    }

    public static void showImage(
            Context ctx,
            String url,
            ImageView view
    ) {
        File file = getCachedAd(ctx, url);
        if (file.exists()) {
            showImage(ctx, file, view);
        } else {
            ImageLoadHelper.loadFile(ctx, url, f -> {
                // 广告图片缓存起来再展示，
                AsyncUtils.doAsync(ctx, executor, c -> {
                    downloadFromUrl(ctx, url);
                    c.uiThread(r -> {
                        showImage(ctx, file, view);
                    });
                });
            });
        }
    }

    @WorkerThread
    private static void downloadAd(Context ctx, Advertising ad) throws Exception {
        Log.d(TAG, "downloadAd() called with: ctx = [" + ctx + "], ad = [" + ad + "]");
        downloadFromUrl(ctx, ad.getVideo());
        downloadFromUrl(ctx, ad.getPhoto());
        downloadFromUrl(ctx, ad.getLogo());
    }

    @WorkerThread
    private static void downloadAdList(Context ctx, List<Advertising> adList) throws Exception {
        Log.d(TAG, "downloadAdList() called with: ctx = [" + ctx + "], ad = [" + adList + "]");
        if (adList == null || adList.isEmpty()) {
            return;
        }
        for (Advertising ad : adList) {
            downloadFromUrl(ctx, ad.getVideo());
            downloadFromUrl(ctx, ad.getPhoto());
            downloadFromUrl(ctx, ad.getLogo());
        }
    }

    @WorkerThread
    private static void downloadFromUrl(Context ctx, String url) throws Exception {
        Log.d(TAG, "downloadFromUrl() called with: ctx = [" + ctx + "], url = [" + url + "]");
        if (TextUtils.isEmpty(url)) {
            return;
        }
        File cacheFile = getCachedAd(ctx, url);
        if (cacheFile.exists()) {
            return;
        }
        runInLock(url, () -> {
            File downloading = new File(cacheFile.getAbsolutePath() + ".downloading");
            Response response = HttpUtils.getInstance().getOkHttpClient().newCall(new Request.Builder()
                    .url(url)
                    .build())
                    .execute();
            ResponseBody body = response.body();
            try (InputStream inputStream = body.byteStream()) {
                try (FileOutputStream outputStream = new FileOutputStream(downloading)) {
                    copy(inputStream, outputStream);
                }
            }
            downloading.renameTo(cacheFile);
            Log.d(TAG, "downloadFromUrl() success with: ctx = [" + ctx + "], url = [" + url + "]");
        });
    }

    @WorkerThread
    private static void runInLock(String s, AsyncUtils.Function0 r) throws Exception {
        ReentrantLock lock;
        synchronized (lockMap) {
            lock = lockMap.get(s);
            if (lock == null) {
                lock = new ReentrantLock();
                lockMap.put(s, lock);
            }
        }
        lock.lock();
        try {
            r.apply();
        } finally {
            lock.unlock();
        }
    }

    @WorkerThread
    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[10 * 1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            outputStream.write(buffer, 0, length);
        }
    }

    public static File getCachedAd(Context ctx, String url) {
        // TODO: 策略，播一次下载一次或者缓存一定数据或者设置缓存容量上限，不能全部下载，
        // 找个合适的第三方库处理下载，
        File parent = new File(ctx.getCacheDir(), "skAdHelperCache");
        //noinspection ResultOfMethodCallIgnored
        parent.mkdirs();
        return new File(parent, MD5.encryptHex(url));
    }

    public static void openAndIncLink(Context ctx, String url, String id) {
        try {
            // 不处理下载，直接抛出去，
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            ctx.startActivity(i);
        } catch (Exception ignored) {
            // 无论如何不要崩溃，比如没有浏览器，
        }
        AdHelper.incClick(ctx, id);
    }

    @NonNull
    private static Advertising loadAd(Context ctx, String key) {
        Advertising ret = JSON.parseObject(getSp(ctx).getString(key, null), Advertising.class);
        if (ret == null) {
            return new Advertising();
        }
        return ret;
    }

    @NonNull
    private static List<Advertising> loadAdList(Context ctx, String key) {
        List<Advertising> ret = JSON.parseArray(getSp(ctx).getString(key, null), Advertising.class);
        if (ret == null) {
            return new ArrayList<>();
        }
        return ret;
    }

    private static void saveAd(Context ctx, String key, Advertising ad) {
        getSp(ctx).edit().putString(key, JSON.toJSONString(ad)).apply();
    }

    private static void saveAdList(Context ctx, String key, List<Advertising> ad) {
        getSp(ctx).edit().putString(key, JSON.toJSONString(ad)).apply();
    }

    private static SharedPreferences getSp(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public enum AdType {
        Splash, Banner
    }
}
