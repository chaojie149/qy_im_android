/**
 * Copyright (C) 2015 ogaclejapan
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tongxin.caihong.ui.smarttab.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;

public class ViewPagerItem extends PagerItem {

    private final int resource;

    protected ViewPagerItem(CharSequence title, float width, @LayoutRes int resource) {
        super(title, width);
        this.resource = resource;
    }

    public static ViewPagerItem of(CharSequence title, @LayoutRes int resource) {
        return of(title, DEFAULT_WIDTH, resource);
    }

    public static ViewPagerItem of(CharSequence title, float width, @LayoutRes int resource) {
        return new ViewPagerItem(title, width, resource);
    }

    public View initiate(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(resource, container, false);
    }

}
