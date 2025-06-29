package com.xuan.xuanhttplibrary.okhttp.callback;

import com.alibaba.fastjson.TypeReference;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

public abstract class ListCallback<T> extends TypeCallback<ArrayResult<T>> {

    public ListCallback(Class<T> mClazz) {
        this(mClazz, true);
    }

    public ListCallback(Class<T> mClazz, boolean mainThreadCallback) {
        super(new TypeReference<ArrayResult<T>>(mClazz) {
        }.getType(), mainThreadCallback);
    }
}
