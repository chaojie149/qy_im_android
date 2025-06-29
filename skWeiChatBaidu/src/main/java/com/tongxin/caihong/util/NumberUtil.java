package com.tongxin.caihong.util;

import java.math.BigDecimal;

public class NumberUtil {
    /**
     * 保留两位小数点
     *
     * @param value
     * @return
     */
    public static double getTwoDecimalPlaces(double value) {
        BigDecimal bg = new BigDecimal(value);
        return bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 保留两位小数点
     *
     * @param value
     * @return
     */
    public static String getTwoDecimalPlaces(String value) {
        BigDecimal bg = BigDecimal.valueOf(Double.parseDouble(value));
        return String.valueOf(bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
    }
}
