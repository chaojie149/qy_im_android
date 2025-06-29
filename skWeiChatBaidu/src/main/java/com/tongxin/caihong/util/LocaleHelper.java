package com.tongxin.caihong.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tongxin.caihong.ui.me.SwitchLanguage;

import java.util.Locale;
import java.util.Map;

/**
 * Created by zq on 2017/8/26 0026.
 * <p>
 * 切换语言工具
 */

public class LocaleHelper {
    public static final String DEFAULT_LANGUAGE = "zh";
    public static final String TAG = "LocaleHelper";
    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, Locale.getDefault().getLanguage());
        return setLocale(context, lang);
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedData(context, defaultLanguage);
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        String lan = getPersistedData(context, Locale.getDefault().getLanguage());
        Map<String, String> languageFullnameMap = SwitchLanguage.getLanguageFullnameMap();
        for (String key : languageFullnameMap.keySet()) {
            if (lan.startsWith(key)) {
                return lan;
            }
        }
        return DEFAULT_LANGUAGE;
    }

    public static Context setLocale(Context context) {
        return setLocale(context, getLanguage(context));
    }

    public static Context setLocale(Context context, String language) {
        persist(context, language);
        /*
        7.0 equipment,sdk >= 24,use updateResources(context, language) method;
        but it be like useless
        So,we use updateResourcesLegacy(context, language) method
         */

        // -------------------------------------------------------------------
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("zq", "SDK >= 24");
            return updateResources(context, language);
        }
        Log.e("zq", "SDK < 24");*/
        return updateResourcesLegacy(context, language);
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_LANGUAGE, language);
        editor.apply();
    }

    public static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_LANGUAGE, "zh");
    }

    /*@TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }*/

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Log.e(TAG, "updateResourcesLegacy() called with: context = [" + context.getClass().getSimpleName() + "], language = [" + language + "]");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        if (language.equals("TW")) {
            // 繁体：因为台湾、香港都为zh,所以区别对待
            configuration.locale = Locale.TAIWAN;
        } else {
            // 简体
            configuration.locale = locale;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(configuration.locale));
        }
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        Log.e(TAG, "updateResourcesLegacy: " + context.getClass().getSimpleName() + ", " + context.getResources().getConfiguration().locale.toString());
        return context;
    }

    /**
     * @return 返回用于百度的语言设置，
     */
    public static String getBaiduLanguage(Context ctx) {
        String lan = getLanguage(ctx);
        if (lan.startsWith("zh")) {
            return "zh";
        }
        if (lan.startsWith("TW")) {
            return "cht";
        }
        if (lan.startsWith("en")) {
            return "en";
        }
        return lan;
    }
}
