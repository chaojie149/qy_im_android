package com.tongxin.caihong.ui.tool;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/8/18.
 */

public class HtmlFactory {

    // 单例
    private static HtmlFactory sington = null;
    private List<String> datas = new ArrayList<>();
    private DataListener mListener;
    private Handler mHandler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case -1:
                    mListener.onError(MyApplication.getContext().getString(R.string.error));
                    break;
                case 0:
                    datas.clear();
                    break;
                case 200:
                    datas.add((String) msg.obj);
                    break;
                case 401:
                    mListener.onResponse(datas, (String) msg.obj);
                    break;
            }
        }
    };

    private HtmlFactory() {
    }

    public static HtmlFactory instance() {
        if (sington == null) {
            synchronized (HtmlFactory.class) {
                if (sington == null) {
                    sington = new HtmlFactory();
                }
            }
        }
        return sington;
    }

    public void queryImage(String s, DataListener listener) {
        final String url;
        if (!s.startsWith("http")) {
            url = "http://" + s;
        } else {
            url = s;
        }
        mListener = listener;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(0); // 准备
                try {
                    // 井号#后的hash部分不能传给服务器，否则可能导致404，
                    int sharpIndex = url.indexOf('#');
                    String finalUrl;
                    if (sharpIndex >= 0) {
                        finalUrl = url.substring(0, sharpIndex);
                    } else {
                        finalUrl = url;
                    }
                    Document document = Jsoup.connect(finalUrl).userAgent("Mozilla/5.0 (Linux; U; Android 4.0.3; zh-cn; M032 Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30")
                            .timeout(6000).get();

                    Elements elements = document.select("img[src]");
                    for (Element element : elements) {
                        // 开始填装数据
                        String url = element.absUrl("src");
                        sendImage(url);
                    }
                    // 获取网页logo图标，
                    // https://www.baidu.com/
                    // <link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
                    // https://m.toutiaoimg.cn/i7046928648917484043/
                    // <link rel="shortcut icon" href="//p1.toutiaoimg.com/large/113f2000647359d21b305" type="image/x-icon">
                    for (Element element : document.head().select("link[rel$=icon]")) {
                        String url = element.absUrl("href");
                        sendImage(url);
                    }
                    // https://www.google.com/
                    // <meta content="/images/branding/googleg/1x/googleg_standard_color_128dp.png" itemprop="image">
                    for (Element element : document.head().select("meta[itemprop=image]")) {
                        String url = element.absUrl("content");
                        sendImage(url);
                    }
                    // https://m.toutiaoimg.cn/i7046928648917484043/

                    Message message = new Message();
                    message.what = 401;
                    message.obj = document.title();
                    mHandler.sendMessage(message); // 结束，将标题也传过去
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(-1); // 异常
                }
            }
        }).start();
    }

    private void sendImage(String url) {
        Log.e("xuan", "queryImage: " + url);
        Message msg = new Message();
        msg.what = 200;
        msg.obj = url;
        mHandler.sendMessage(msg);
    }

    public interface DataListener<T> {
        void onResponse(List<T> datas, String title);

        void onError(String err);
    }
}
