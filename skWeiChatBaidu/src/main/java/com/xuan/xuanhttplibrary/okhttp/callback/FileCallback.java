package com.xuan.xuanhttplibrary.okhttp.callback;

import java.lang.reflect.Type;

public abstract class FileCallback<T> extends TypeCallback<T> {

    public FileCallback(Type type) {
        super(type);
    }

    public void onProgress(String key, long current, long total) {
    }
}
