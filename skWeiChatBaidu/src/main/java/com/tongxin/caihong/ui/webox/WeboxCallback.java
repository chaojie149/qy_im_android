package com.tongxin.caihong.ui.webox;

import androidx.annotation.NonNull;

import com.xuan.xuanhttplibrary.okhttp.callback.TypeCallback;

import java.lang.reflect.Type;

import okhttp3.Call;

public abstract class WeboxCallback<T> extends TypeCallback<T> {

    public WeboxCallback(Type type) {
        super(type);
    }

    public WeboxCallback(Type type, boolean mainThreadCallback) {
        super(type, mainThreadCallback);
    }


    @NonNull
    @Override
    protected T parseResponse(Call call, String body) {
        if (body.contains("{") && body.contains("}")) {
            body = body.substring(body.indexOf("{"), body.lastIndexOf("}") + 1);
        }
        return super.parseResponse(call, body);
    }
}
