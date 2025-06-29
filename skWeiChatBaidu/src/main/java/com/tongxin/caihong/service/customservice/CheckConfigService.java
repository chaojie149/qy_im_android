package com.tongxin.caihong.service.customservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.AppConfig;
import com.tongxin.caihong.bean.ConfigBean;
import com.tongxin.caihong.ui.me.SetConfigActivity;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.HttpUrl;

public class CheckConfigService extends Service {

    private Thread taskThread;
    private boolean isRunning = false;

    private List<SetConfigActivity.ConfigData> getDefaultList() {
//        if (AppUtils.isApkDebug(mContext)) {
        List<SetConfigActivity.ConfigData> list = new ArrayList<>();

        for (SetConfigActivity.ConfigData temp : AppConfig.ROUTE_LINE){
            list.add(temp);
        }
        return list;
//        } else {
//            return new ArrayList<>();
//        }
    }

    private List<SetConfigActivity.ConfigData> initList(String str) {
        List<SetConfigActivity.ConfigData> data = new ArrayList<>();
        JSONArray js = JSONArray.parseArray(str);
        for (int i = 0; i < js.size(); i++) {
            SetConfigActivity.ConfigData ss = new SetConfigActivity.ConfigData(((JSONObject)js.get(i)).getString("url"));
            ss.isSelect = ((JSONObject)js.get(i)).getBoolean("isSelect");
            ss.name = ((JSONObject)js.get(i)).getString("name");
            data.add(ss);
        }
        return data;
    }
    private void saveList(List<SetConfigActivity.ConfigData> data) {
        if (data == null || data.size() == 0) {
            return;
        }
        String sb = JSON.toJSONString(data);
//        StringBuilder sb = new StringBuilder();
//        sb.append("[");
//        for (int i = 0; i < data.size(); i++) {
//            sb.append("\"");
//            sb.append(data.get(i));
//            sb.append("\"");
//            sb.append(",");
//        }
//        sb.deleteCharAt(sb.length() - 1);
//        sb.append("]");
        Log.e("xuan", " " + sb.toString());

        PreferenceUtils.putString(this, "APP_LIST_CONFIG", sb.toString());
    }

    private void startTask() {
        isRunning = true;
        taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
//                while (isRunning) {
                    // 无限循环的任务
                     List<SetConfigActivity.ConfigData> mdata;
                    String str = PreferenceUtils.getString(getApplicationContext(), "APP_LIST_CONFIG", null);
                    if (str == null) {
                        mdata = getDefaultList();
                    } else {
                        mdata = initList(str);
                    }
                    for (SetConfigActivity.ConfigData temp :mdata){
                        try {
                            System.out.println("=================================开始调试");
                            String input = temp.url;
                            String checkUrl =input.replace("/","").replace("/config","")+"/config";
                            Map<String, String> params = new HashMap<>();
                            String finalInput = input;
                            HttpUtils.get().url(checkUrl)
                                    .params(params)
                                    .build(true, true)
                                    .executeSync(new BaseCallback<ConfigBean>(ConfigBean.class) {

                                        @Override
                                        public void onResponse(ObjectResult<ConfigBean> result) {
                                            System.out.println("=================================成功");
                                            System.out.println("========================="+mdata);
                                           temp.status=1;
                                            saveList(mdata);
                                        }

                                        @Override
                                        public void onError(Call call, Exception e) {
                                            System.out.println("================================失败");
                                            temp.status=2;
                                            saveList(mdata);
                                        }
                                    });


                        } catch (Exception e) {
                            System.out.println("=================================出错了");
                            e.printStackTrace();
                            temp.status=2;
                            saveList(mdata);
                        }


                    }
                    try {
                        Thread.sleep(5000);
                    }catch (Exception e){

                    }


                }
//            }
        });
        taskThread.start();
    }

    private void stopTask() {
        isRunning = false;
        if (taskThread != null) {
            taskThread.interrupt(); // 中断线程
            try {
                taskThread.join(); // 等待线程结束
            } catch (InterruptedException e) {
                // 处理异常
            }
            taskThread = null;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化工作，例如启动线程



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 服务启动时执行的操作

        startTask();
        return START_STICKY; // 返回适合的启动模式
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 如果服务可以被绑定，则返回一个IBinder实例
        // 如果不需要绑定，则返回null
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTask();
        // 服务销毁时执行的操作
    }

}
