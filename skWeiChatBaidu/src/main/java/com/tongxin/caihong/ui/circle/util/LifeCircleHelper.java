package com.tongxin.caihong.ui.circle.util;

import android.content.Context;
import android.text.TextUtils;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Area;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.builder.BaseBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LifeCircleHelper {
    private static List<String> tagList;

    public static List<String> getAllTag() {
        return tagList;
    }

    public static List<String> getSquareTagList(Context ctx) {
        List<String> ret = new ArrayList<>(1 + tagList.size());
        ret.add(ctx.getString(R.string.tag_square_new));
        ret.addAll(tagList);
        return ret;
    }

    public static BaseBuilder buildSquareParams(String url, Map<String, String> params, int tagIndex) {
        params.remove("messageId");
        if (tagIndex >= 1) {
            params.put("lable", tagList.get(tagIndex - 1));
            double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
            double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
            if (latitude != 0 && longitude != 0) {
                params.put("latitude", String.valueOf(latitude));
                params.put("longitude", String.valueOf(longitude));
            }
        } else {
            params.put("sortType", "1");
        }
        Area area = Area.getDefaultCity();
        if (area != null) {
            // 城市id
            params.put("cityId", String.valueOf(area.getId()));
        }
        return HttpUtils.get().url(url)
                .params(params);
    }

    public static void initTag(String lableListStr) {
        tagList = new ArrayList<>();
        if (TextUtils.isEmpty(lableListStr)) {
            return;
        }
        for (String tag : lableListStr.split("#")) {
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            tagList.add(tag);
        }
    }

    public static Set<String> selectedTagSet(String selectedLabels) {
        Set<String> ret = new HashSet<>();
        if (!TextUtils.isEmpty(selectedLabels)) {
            for (String tag : selectedLabels.split("#")) {
                if (!TextUtils.isEmpty(tag)) {
                    ret.add(tag);
                }
            }
        }
        return ret;
    }

}
