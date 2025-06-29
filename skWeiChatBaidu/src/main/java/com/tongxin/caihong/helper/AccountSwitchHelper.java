package com.tongxin.caihong.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.account.AccountSwitchFragment;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccountSwitchHelper {
    private static final String SP_NAME = "login_user_list";
    private static final String KEY_USER_LIST = "KEY_USER_LIST";
    @SuppressLint("StaticFieldLeak")
    @Nullable
    private static AccountSwitchFragment inSwitching;

    public static AccountSwitchFragment getInSwitching() {
        return inSwitching;
    }

    public static void setInSwitching(AccountSwitchFragment accountSwitchFragment) {
        inSwitching = accountSwitchFragment;
    }

    private static void save(Context ctx, String userId) {
        Set<String> exists = new HashSet<>(load(ctx));
        exists.add(userId);
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit()
                .clear()
                .putStringSet(KEY_USER_LIST, exists)
                .apply();
    }

    public static Set<String> load(Context ctx) {
        return ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_USER_LIST, Collections.emptySet());
    }

    private static void remove(Context ctx, String userId) {
        Set<String> exists = new HashSet<>(load(ctx));
        exists.remove(userId);
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit()
                .clear()
                .putStringSet(KEY_USER_LIST, exists)
                .apply();
    }

    private static void copy(SharedPreferences from, SharedPreferences to) {
        SharedPreferences.Editor ed = to.edit();
        ed.clear();
        for (Map.Entry<String, ?> entry : from.getAll().entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            if (v instanceof Boolean)
                ed.putBoolean(key, (Boolean) v);
            else if (v instanceof Float)
                ed.putFloat(key, (Float) v);
            else if (v instanceof Integer)
                ed.putInt(key, (Integer) v);
            else if (v instanceof Long)
                ed.putLong(key, (Long) v);
            else if (v instanceof String)
                ed.putString(key, ((String) v));
        }
        ed.apply();
    }

    public static void saveOldUser(Context ctx, String userId, SharedPreferences sp) {
        AccountSwitchHelper.copy(sp, UserSp.getInstance(ctx, userId).getSharePreference());
        save(ctx, userId);
    }

    public static void loadOldUser(Context ctx, String userId) {
        AccountSwitchHelper.copy(UserSp.getInstance(ctx, userId).getSharePreference(), UserSp.getInstance(ctx).getSharePreference());
        PreferenceUtils.putString(ctx, Constants.AREA_CODE_KEY, UserSp.getInstance(ctx, userId).getAreaCode()+"");
    }

    public static void removeExistsUser(Context ctx, String userId) {
        UserSp.getInstance(ctx, userId).clearUserInfo();
        remove(ctx, userId);
    }

    public static void finishSwitching() {
        if (getInSwitching() == null) {
            return;
        }
        getInSwitching().finishSwitching();
        setInSwitching(null);
    }
}
