package com.tongxin.caihong.util;

import android.os.Handler;
import android.os.Looper;

import java.util.WeakHashMap;

public class RepeatLimitHelper {
    private static final WeakHashMap<Runnable, Long> timeCache = new WeakHashMap<>();
    private static final WeakHashMap<Runnable, Runnable> waitCache = new WeakHashMap<>();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 限制重复调用，每个delta时间内只调用一次，最后一次调用会延迟到delta时间之后再调用，
     * 重点是确保最后一次调用不会被取消，只会延迟，
     *
     * @param delta    两次重复调用间隔时间，毫秒，
     * @param runnable 重复调用的必须是同一个对象才有限制作用，
     */
    public static void run(long delta, Runnable runnable) {
        Long last = timeCache.get(runnable);
        long current = System.currentTimeMillis();
        timeCache.put(runnable, System.currentTimeMillis());
        if (last == null || last + delta < current) {
            runnable.run();
        } else {
            long delay = last + delta - current;
            Runnable handlerRunnable = waitCache.get(runnable);
            if (handlerRunnable == null) {
                handlerRunnable = () -> {
                    waitCache.remove(runnable);
                    runnable.run();
                };
                waitCache.put(runnable, handlerRunnable);
                handler.postDelayed(handlerRunnable, delay);
            }
        }
    }
}
