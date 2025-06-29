package com.tongxin.caihong.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;
import com.scwang.smartrefresh.layout.constant.SpinnerStyle;
import com.scwang.smartrefresh.layout.internal.InternalAbstract;

/**
 * 包含搜索框的header,
 * 一方面配合SmartRefreshLayout实现下拉出现搜索框，注意SmartRefreshLayout必须有个listener，否则会自动折叠，，
 * 一方面使用TextView+EditText实现默认hint显示在中间，输入时移到左边，
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class SearchHeader extends InternalAbstract {
    private ViewGroup llSearch;
    private TextView tvSearch;
    private ClearEditText etSearch;

    public SearchHeader(Context context) {
        this(context, null);
    }

    public SearchHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSpinnerStyle = SpinnerStyle.FixedBehind;
        View.inflate(context, R.layout.mix_search_bar, this);

        llSearch = findViewById(R.id.llSearch);
        tvSearch = findViewById(R.id.tvSearch);
        etSearch = findViewById(R.id.etSearch);
        int[] set = {
                android.R.attr.hint
        };
        TypedArray ta = context.obtainStyledAttributes(attrs, set);
        if (ta.hasValue(0)) {
            setHint(ta.getText(0));
        }

        ta.recycle();

        llSearch.setOnClickListener(v -> {
            v.setVisibility(View.GONE);
            etSearch.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
            etSearch.postDelayed(() -> {
                InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(etSearch.getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            }, 200);
        });
    }

    public final void setHint(CharSequence hint) {
        tvSearch.setHint(hint);
        etSearch.setHint(hint);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        llSearch.setOnClickListener(l);
    }

    public void addTextChangedListener(TextWatcher watcher) {
        etSearch.addTextChangedListener(watcher);
    }

    public CharSequence getText() {
        return etSearch.getText();
    }

    public void setText(CharSequence text) {
        etSearch.setText(text);
    }

    public void setFocusClear(boolean b) {
        etSearch.setFocusClear(b);
    }
}
