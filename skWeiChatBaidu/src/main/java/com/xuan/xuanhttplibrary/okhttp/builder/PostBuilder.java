package com.xuan.xuanhttplibrary.okhttp.builder;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.tongxin.caihong.Reporter;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.AbstractCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Source;

/**
 * @author Administrator
 * @time 2017/3/30 0:11
 * @des ${TODO}
 */

public class PostBuilder extends BaseBuilder {
    private final Map<String, File> files = new LinkedHashMap<>();
    @Nullable
    private FileCallback fileCallback = null;
    private boolean encoded;

    @Override
    public PostBuilder url(String url) {
        if (!TextUtils.isEmpty(url)) {
            this.url = url;
        }
        return this;
    }

    @Override
    public PostBuilder tag(Object tag) {
        return this;
    }

    @Override
    public PostCall abstractBuild() {
        try {
            RequestBody requestBody = appenParams();

            build = new Request.Builder()
                    .header("User-Agent", getUserAgent())
                    .url(url).post(requestBody)
                    .build();
        } catch (Exception e) {
            // url异常不要直接崩溃，
            crashed = new IOException(e);
        }
        return new PostCall();
    }

    private RequestBody appenParams() {
        RequestBody ret;
        FormBody.Builder builder = new FormBody.Builder();
        StringBuffer sb = new StringBuffer();
        sb.append(url);
        if (!params.isEmpty()) {
            sb.append("?");
            for (String key : params.keySet()) {
                String v = params.get(key);
                if (v == null) {
                    continue;
                }
                if (!encoded) {
                    try {
                        // url安全，部分字符不能直接放进url, 要改成百分号开头%的，
                        v = URLEncoder.encode(v, "UTF-8");
                    } catch (Exception e) {
                        // 不可到达，UTF-8不可能不支持，
                        Reporter.unreachable(e);
                    }
                }
                // 不能用FormBody封装的urlEncode, 服务器收到的加号还是空格，原因不明，
                builder.addEncoded(key, v);
                sb.append(key).append("=").append(v).append("&");
            }
            sb = sb.deleteCharAt(sb.length() - 1); // 去掉后面的&
        }
        ret = builder.build();
        if (!files.isEmpty()) {
            if (!params.isEmpty()) {
                sb.append("&");
            } else {
                sb.append("?");
            }
            MultipartBody.Builder mb = new MultipartBody.Builder();
            FormBody fb = (FormBody) ret;
            for (int i = 0; i < fb.size(); i++) {
                mb.addFormDataPart(fb.name(i), fb.value(i));
            }
            for (String key : files.keySet()) {
                File v = files.get(key);
                if (v == null) {
                    continue;
                }
                sb.append(key).append("=").append(v).append("&");
                String name = v.getName();
                try {
                    // url安全，部分字符不能直接放进url, 要改成百分号开头%的，
                    name = URLEncoder.encode(name, "UTF-8");
                } catch (Exception e) {
                    // 不可到达，UTF-8不可能不支持，
                    Reporter.unreachable(e);
                }
                mb.addFormDataPart(key, name, new FileRequestBody(key, v));
            }
            sb = sb.deleteCharAt(sb.length() - 1); // 去掉后面的&
            ret = mb.build();
        }

        Log.i(HttpUtils.TAG, "网络请求参数：" + sb.toString());
        return ret;
    }

    @Override
    public PostBuilder params(String k, String v) {
        params.put(k, v);
        return this;
    }

    public PostBuilder params(String k, File v) {
        files.put(k, v);
        return this;
    }

    public PostBuilder params(Map<String, String> params) {
        this.params.putAll(params);
        return this;
    }

    public PostBuilder encoded() {
        encoded = true;
        return this;
    }

    public class FileRequestBody extends RequestBody {
        private String key;
        private File file;

        public FileRequestBody(String key, File file) {
            this.key = key;
            this.file = file;
        }

        @Override
        public long contentLength() {
            return file.length();
        }

        @Override
        public MediaType contentType() {
            return null;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            sink = Okio.buffer(new ForwardingSink(sink) {
                long contentLength = contentLength();
                long totalBytesRead = 0;

                @Override
                public void write(Buffer source, long byteCount) throws IOException {
                    super.write(source, byteCount);
                    totalBytesRead += byteCount;
                    if (fileCallback != null) {
                        fileCallback.onProgress(key, totalBytesRead, contentLength);
                    }
                }
            });
            Source source = null;
            try {
                source = Okio.source(file);
                sink.flush();
                sink.writeAll(source);
                sink.flush();
            } finally {
                Util.closeQuietly(source);
            }
        }
    }

    public class PostCall extends BaseCall {
        @Nullable
        @Override
        public Call execute(AbstractCallback<?> callback) {
            if (callback instanceof FileCallback) {
                fileCallback = (FileCallback) callback;
            }
            return super.execute(callback);
        }

        @Override
        public void executeSync(AbstractCallback<?> callback) {
            if (callback instanceof FileCallback) {
                fileCallback = (FileCallback) callback;
            }
            super.executeSync(callback);
        }
    }
}
