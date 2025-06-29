package com.tongxin.caihong.util;

import android.text.TextUtils;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天消息漫游工具类  不要动随便改，灰飞烟灭
 * author：Administrator on 2016/12/26 10:32
 * description:文件说明
 * version:版本
 */
public class CharUtils {
    /**
     * 获取String字符长度
     *
     * @param str
     * @return
     */
    public static int getLength(String str) {
        int length = 0;
        if (TextUtils.isEmpty(str)) {
            return length;
        }
/*
        for (int i = 0; i < str.length(); i++) {
            String substring = str.substring(i, i + 1);
            if (CharUtils.isChinese(substring)) {
                // 中文占两个字符
                length += 2;
            } else {
                length += 1;
            }
        }
*/
        return str.length();
    }

    /**
     * 是否为中文
     *
     * @param str
     * @return
     */
    private static boolean isChinese(String str) {
        String regEx = "[\\u4e00-\\u9fa5]+";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    public static String getRandomString(int length) {
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(3);
            long result = 0;
            switch (number) {
                case 0:
                    result = Math.round(Math.random() * 25 + 65);
                    sb.append(String.valueOf((char) result));
                    break;
                case 1:
                    result = Math.round(Math.random() * 25 + 97);
                    sb.append(String.valueOf((char) result));
                    break;
                case 2:
                    sb.append(String.valueOf(new Random().nextInt(10)));
                    break;
            }
        }
        return sb.toString();
    }
}
