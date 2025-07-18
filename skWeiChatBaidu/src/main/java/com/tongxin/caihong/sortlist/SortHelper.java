package com.tongxin.caihong.sortlist;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tongxin.caihong.util.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SortHelper {

    public static void setSortCondition(BaseSortModel<?> mode, String name) {
        String wholeSpell = PingYinUtil.getPingYin(name);
        if (!TextUtils.isEmpty(wholeSpell)) {
            String firstLetter = Character.toString(wholeSpell.charAt(0));
            mode.setWholeSpell(wholeSpell);
            mode.setFirstLetter(firstLetter);
        } else {// 如果全拼为空，理论上是一种错误情况，因为这代表着昵称为空
            mode.setWholeSpell("#");
            mode.setFirstLetter("#");
        }
    }

    public static <T> BaseSortModel<T> toSortedModel(T bean, NameMapping<T> mapping) {
        String name = mapping.getName(bean);
        if (name == null) {
            return null;
        }
        BaseSortModel<T> mode = new BaseSortModel<>();
        mode.setBean(bean);
        setSortCondition(mode, name);
        return mode;
    }

    public static <T> List<BaseSortModel<T>> toSortedModelList(List<T> beanList, Map<String, Integer> existMap, NameMapping<T> mapping) {
        return toSortedModelList(new ArrayList<>(beanList.size()), beanList, existMap, mapping);
    }

    public static <T> List<BaseSortModel<T>> toSortedModelList(List<BaseSortModel<T>> base, List<T> beanList, Map<String, Integer> existMap, NameMapping<T> mapping) {
        LogUtils.log("sort: size: " + beanList.size());
        for (int i = 0; i < beanList.size(); i++) {
            BaseSortModel<T> mode = toSortedModel(beanList.get(i), mapping);
            if (mode == null) {
                continue;
            }
            Integer exists = existMap.get(mode.firstLetter);
            if (exists == null) {
                exists = 0;
            }
            ++exists;
            existMap.put(mode.firstLetter, exists);
            base.add(mode);
        }
        Collections.sort(base, (o1, o2) -> {
            if (o1.getFirstLetter().equals("#")) {
                if (o2.getFirstLetter().equals("#")) {
                    return o1.getWholeSpell().compareTo(o2.getWholeSpell());
                } else {
                    return -1;
                }
            } else {
                if (o2.getFirstLetter().equals("#")) {
                    return 1;
                } else {
                    return o1.getWholeSpell().compareTo(o2.getWholeSpell());
                }
            }
        });
        LogUtils.log("sorted: size: " + beanList.size());
        return base;
    }

    public interface NameMapping<T> {
        /**
         * 返回null表示过滤掉这个元素，
         */
        @Nullable
        String getName(T bean);
    }
}
