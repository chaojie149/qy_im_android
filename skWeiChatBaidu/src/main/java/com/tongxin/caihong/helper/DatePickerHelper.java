package com.tongxin.caihong.helper;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;

import com.tongxin.caihong.R;
import com.tongxin.caihong.util.ToastUtil;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DatePickerHelper {

    public static void resetQueryData(QueryWrap queryWrap, int day) {
        queryWrap.dirty = false;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        queryWrap.endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -day);
        queryWrap.startTime = calendar.getTimeInMillis();
    }

    public static long getTodayTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @SuppressWarnings("deprecation")
    public static void selectData(Context ctx, QueryWrap queryWrap, boolean startTime, OnResultListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long today = calendar.getTimeInMillis();
        if (startTime) {
            calendar.setTimeInMillis(queryWrap.startTime);
        } else {
            calendar.setTimeInMillis(queryWrap.endTime);
        }
        DatePickerDialog dialog = new DatePickerDialog(ctx, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                queryWrap.dirty = true;
                GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                long time = calendar.getTimeInMillis();
                if (startTime) {
                    if (time > queryWrap.endTime) {
                        ToastUtil.showToast(ctx, ctx.getString(R.string.tip_time_start_gt_end));
                        return;
                    }
                    queryWrap.startTime = time;
                    if (listener != null) {
                        listener.onResult(time);
                    }
                } else {
                    if (time > today) {
                        ToastUtil.showToast(ctx, ctx.getString(R.string.tip_end_gt_today));
                        return;
                    }
                    if (time < queryWrap.startTime) {
                        ToastUtil.showToast(ctx, ctx.getString(R.string.tip_end_lt_start));
                        return;
                    }
                    queryWrap.endTime = time;
                    if (listener != null) {
                        listener.onResult(time);
                    }
                }
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    public static long getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public interface OnResultListener {
        void onResult(long time);
    }

    public static class QueryWrap {
        public boolean dirty;
        public long startTime;
        public long endTime;
    }
}
