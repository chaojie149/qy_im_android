package com.tongxin.caihong.view.listener;

import android.view.View;
import android.widget.TextView;

import com.tongxin.caihong.bean.DateBean;


public interface CalendarViewAdapter {
    /**
     * 返回阳历、阴历两个TextView
     *
     * @param view
     * @param date
     * @return
     */
    TextView[] convertView(View view, DateBean date);
}
