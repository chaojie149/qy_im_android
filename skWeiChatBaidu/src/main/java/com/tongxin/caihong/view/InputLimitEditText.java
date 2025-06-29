package com.tongxin.caihong.view;

import android.content.Context;
import android.text.InputFilter;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class InputLimitEditText extends AppCompatEditText {

    public InputLimitEditText(Context context) {
        super(context);
    }

    public InputLimitEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InputLimitEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setFilters(InputFilter[] filters) {
        List<InputFilter> list = new ArrayList<>(Arrays.asList(filters));
        ListIterator<InputFilter> iterator = list.listIterator();
        while (iterator.hasNext()) {
            InputFilter inputFilter = iterator.next();
            // 把所有InputFilter.LengthFilter用InputLimitFilter替换，在InputLimitFilter里面提示字数超限，
            if (inputFilter instanceof InputFilter.LengthFilter && !(inputFilter instanceof InputLimitFilter)) {
                iterator.set(new InputLimitFilter(getContext(), ((InputFilter.LengthFilter) inputFilter).getMax()));
            }
        }
        super.setFilters(list.toArray(new InputFilter[0]));
    }
}
