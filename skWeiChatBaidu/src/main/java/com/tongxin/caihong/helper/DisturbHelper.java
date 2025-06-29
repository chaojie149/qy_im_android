package com.tongxin.caihong.helper;

import android.content.Context;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 勿扰设置相关处理，
 * 保存的起止时间是设置时间与0点的差，单位毫秒，
 * 对外暴露的是当天的时刻，用Date对象，
 */
public class DisturbHelper {
    private static final String SP_NAME = "sk_disturb";

    public static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getBoolean("isEnabled", false);
    }

    public static Date getStartDate(Context ctx) {
        long time = getStartTime(ctx);
        Date date = new Date();
        date.setHours((int) TimeUnit.MILLISECONDS.toHours(time));
        date.setMinutes((int) TimeUnit.MILLISECONDS.toMinutes((time - TimeUnit.HOURS.toMillis(date.getHours()))));
        return date;
    }

    public static Date getEndDate(Context ctx) {
        long time = getEndTime(ctx);
        Date date = new Date();
        date.setHours((int) TimeUnit.MILLISECONDS.toHours(time));
        date.setMinutes((int) TimeUnit.MILLISECONDS.toMinutes((time - TimeUnit.HOURS.toMillis(date.getHours()))));
        return date;
    }

    public static void saveStartDate(Context ctx, Date date) {
        long time = date.getTime();
        saveStartTime(ctx, time - getTodayStartTime(time));
    }

    public static void saveEndDate(Context ctx, Date date) {
        long time = date.getTime();
        saveEndTime(ctx, time - getTodayStartTime(time));
    }

    private static long getStartTime(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getLong("startTime", TimeUnit.HOURS.toMillis(23));
    }

    private static long getEndTime(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getLong("endTime", TimeUnit.HOURS.toMillis(8));
    }

    public static void saveEnabled(Context ctx, boolean isEnabled) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isEnabled", isEnabled)
                .apply();
    }

    private static void saveStartTime(Context ctx, long time) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("startTime", time)
                .apply();
    }

    private static void saveEndTime(Context ctx, long time) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("endTime", time)
                .apply();
    }

    /**
     * @return 当前时间处于勿扰模式时候内，
     */
    public static boolean doNotDisturbNow(Context ctx) {
        if (!isEnabled(ctx)) {
            return false;
        }
        long startTime = getStartTime(ctx);
        long endTime = getEndTime(ctx);
        if (endTime < startTime) {
            endTime += TimeUnit.DAYS.toMillis(1);
        }
        long currentTime = System.currentTimeMillis();
        long todayTime = getTodayStartTime(currentTime);
        long nowTime = currentTime - todayTime;
        if (nowTime < startTime) {
            nowTime += TimeUnit.DAYS.toMillis(1);
        }
        return startTime < nowTime && nowTime < endTime;
    }

    private static long getTodayStartTime(long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime().getTime();
    }
}
