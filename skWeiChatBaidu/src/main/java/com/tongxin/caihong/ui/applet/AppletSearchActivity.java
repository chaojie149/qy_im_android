package com.tongxin.caihong.ui.applet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.bean.Applet;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppletSearchActivity extends BaseActivity implements OnAppletItemClickListener {
    private EditText mSearchEdit;
    private View flFragment;
    private View llSearchHistory;
    private AppletSearchHistoryAdapter adapter;

    public static void start(Context context) {
        Intent starter = new Intent(context, AppletSearchActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applet_search);
        initView();
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> {
            finish();
        });
        TextView ivCancel = findViewById(R.id.ivCancel);
        ivCancel.setTextColor(SkinUtils.getSkin(mContext).getAccentColor());
        ivCancel.setOnClickListener(v -> {
            finish();
        });

        mSearchEdit = findViewById(R.id.search_edit);
        mSearchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    flFragment.setVisibility(View.GONE);
                    llSearchHistory.setVisibility(View.VISIBLE);
                } else {
                    flFragment.setVisibility(View.VISIBLE);
                    llSearchHistory.setVisibility(View.GONE);
                    AppletSearchFragment fragment = getFragment();
                    fragment.search(s.toString());
                }
            }
        });

        llSearchHistory = findViewById(R.id.llSearchHistory);
        flFragment = findViewById(R.id.flFragment);
        initSearchHistory();

        Fragment fragment = new AppletSearchFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.flFragment, fragment)
                .commit();
    }

    private void initSearchHistory() {
        RecyclerView rvSearchHistory = findViewById(R.id.rvSearchHistory);
        adapter = new AppletSearchHistoryAdapter(this::onRemoveClick, this::onHistoryKeyClick);
        adapter.setData(getHistoryKeyList(mContext));
        rvSearchHistory.setAdapter(adapter);
    }

    private void onHistoryKeyClick(int position, String item) {
        mSearchEdit.setText(item);
    }

    public void onRemoveClick(int position, String item) {
        removeKey(mContext, item);
        adapter.setData(getHistoryKeyList(mContext));
    }

    @Override
    public void onItemClick(Applet item) {
        saveKey(mContext, mSearchEdit.getText().toString());
        adapter.setData(getHistoryKeyList(mContext));
    }
    private AppletSearchFragment getFragment() {
        return (AppletSearchFragment) getSupportFragmentManager().findFragmentById(R.id.flFragment);
    }

    private static final String SP_NAME = "sk_applet_search_key_history";

    @NonNull
    public static List<String> getHistoryKeyList(Context ctx) {
        String str = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .getString("historyKey", null);
        List<String> ret = JSON.parseArray(str, String.class);
        if (ret == null) {
            ret = new ArrayList<>();
        }
        return ret;
    }

    private static void putHistoryKey(Context ctx, List<String> keyList) {
        ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("historyKey", JSON.toJSONString(keyList))
                .apply();
    }

    /**
     * 保存最近的五个，
     */
    public static void saveKey(Context ctx, String key) {
        saveKey(ctx, Collections.singletonList(key));
    }

    public static void saveKey(Context ctx, List<String> keyList) {
        if (keyList == null || keyList.isEmpty()) {
            return;
        }
        List<String> historyKeyList = getHistoryKeyList(ctx);
        historyKeyList.removeAll(keyList);
        historyKeyList.addAll(0, keyList);
        putHistoryKey(ctx, historyKeyList.subList(0, Math.min(5, historyKeyList.size())));
    }

    public static void removeKey(Context ctx, String key) {
        if (key == null) {
            return;
        }
        List<String> historyKeyList = getHistoryKeyList(ctx);
        historyKeyList.remove(key);
        putHistoryKey(ctx, historyKeyList);
    }

}
