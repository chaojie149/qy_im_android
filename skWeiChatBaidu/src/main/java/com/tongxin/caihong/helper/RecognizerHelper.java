package com.tongxin.caihong.helper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.MediaDecoder;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.util.secure.MD5;
import com.tongxin.caihong.view.ChatContentView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tongxin.caihong.bean.message.XmppMessage.TYPE_VOICE;

public class RecognizerHelper {
    private static final String TAG = "RecognizerHelper";
    private static EventManager asr;
    private static EventListener listener;
    private static ExtraWrap extraWrap;

    private static void init() {
        if (asr != null) {
            return;
        }
        synchronized (RecognizerHelper.class) {
            if (asr != null) {
                return;
            }
            asr = EventManagerFactory.create(MyApplication.getContext(), "asr");//注册自己的输出事件类
            listener = RecognizerHelper::onEvent;
            asr.registerListener(listener);//// 调用 EventListener 中 onEvent方法
        }
    }

    public static void onEvent(String name, String params, byte[] data, int offset, int length) {
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {//识别结果参数
            JSONObject json = JSON.parseObject(params);
            String result = json.getString("best_result");//取得key的识别结果
            extraWrap.message.setTranslation(result);
            extraWrap.view.onRecognizerResult(extraWrap.message);
            extraWrap.success = true;
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)) {
            if (!extraWrap.success) {
                // 没成功，
                JSONObject jsonObject = JSON.parseObject(params);
                if (jsonObject.getInteger("error") != 0) {
                    // 失败了，
                    // ToastUtil.showToast(MyApplication.getContext(), MyApplication.getContext().getString(R.string.tip_recognizer_failed_place_holder, MyApplication.getContext().getString(R.string.tip_recognizer_error)));
                    ToastUtil.showToast(MyApplication.getContext(), MyApplication.getContext().getString(R.string.tip_not_recognizer_content));
                }
            }
            extraWrap = null;
        }
    }

    public static void recognizer(ChatContentView view, ChatMessage message) {
        if (!needRecognizer(message)) {
            return;
        }
        init();
        Context mContext = view.getContext();
        Log.d(TAG, "recognizer() called with: message = [" + message.getContent() + "]");
        if (extraWrap != null) {
            ToastUtil.showToast(mContext, mContext.getString(R.string.tip_voice_to_text_only_one));
            return;
        }
        synchronized (RecognizerHelper.class) {
            if (extraWrap != null) {
                ToastUtil.showToast(mContext, mContext.getString(R.string.tip_voice_to_text_only_one));
                return;
            }
            extraWrap = new ExtraWrap(view, message);
        }
        AsyncUtils.doAsync(mContext, t -> {
            ToastUtil.showToast(mContext, mContext.getString(R.string.tip_recognizer_failed_place_holder, t.getMessage()));
            extraWrap = null;
        }, c -> {
            String filePath = UploadCacheUtils.get(mContext, message);
            final File pcmFile = new File(mContext.getCacheDir(), "baidu_speech_" + MD5.encryptHex(message.getContent()) + ".pcm");
            try (final OutputStream output = new FileOutputStream(pcmFile)) {
                // 百度指定采样率是16000，否则识别不准，
                MediaDecoder.decode(filePath, output, 16000);
            }
            baiduRecognizer(pcmFile);
        });

    }

    private static void baiduRecognizer(File pcmFile) {
        Map<String, Object> params = new LinkedHashMap<>();//传递Map<String,Object>的参数，会将Map自动序列化为json
        String event = SpeechConstant.ASR_START;
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);//回调当前音量
        params.put(SpeechConstant.IN_FILE, pcmFile.getAbsolutePath());
        String json = JSON.toJSONString(params);//demo用json数据来做数据交换的方式
        asr.send(event, json, null, 0, 0);// 初始化EventManager对象,这个实例只能创建一次，就是我们上方创建的asr，此处开始传入
    }

    public static boolean needRecognizer(ChatMessage message) {
        return message.getType() == TYPE_VOICE
                && !TextUtils.isEmpty(message.getContent())
                && TextUtils.isEmpty(message.getTranslation());
    }

    private static class ExtraWrap {
        private ChatContentView view;
        private ChatMessage message;
        private boolean success;

        public ExtraWrap(ChatContentView view, ChatMessage message) {
            this.view = view;
            this.message = message;
        }
    }
}
