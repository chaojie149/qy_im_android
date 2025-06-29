package me.kareluo.imaging.core;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;

/**
 * Created by felix on 2017/12/1 下午2:43.
 */

public class IMGText {

    private String text;

    private int color = Color.WHITE;
    private boolean tintBackground;

    public IMGText(String text, int color, boolean tintBackground) {
        this.text = text;
        this.color = color;
        this.tintBackground = tintBackground;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getRealColor() {
        if (!tintBackground) {
            return color;
        } else {
            return Color.WHITE;
        }
    }

    public ColorStateList getRealBackgroundTint() {
        if (!tintBackground) {
            return null;
        } else {
            return ColorStateList.valueOf(getRealBackgroundColor());
        }
    }

    public int getRealBackgroundColor() {
        if (color == Color.WHITE) {
            return Color.BLACK;
        } else {
            return color;
        }
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(text);
    }

    public int length() {
        return isEmpty() ? 0 : text.length();
    }

    @Override
    public String toString() {
        return "IMGText{" +
                "text='" + text + '\'' +
                ", color=" + color +
                '}';
    }

    public boolean isTintBackground() {
        return tintBackground;
    }

    public void setTintBackground(boolean tintBackground) {
        this.tintBackground = tintBackground;
    }

    public IMGText copy() {
        return new IMGText(text, color, tintBackground);
    }
}
