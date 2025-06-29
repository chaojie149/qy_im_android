package com.tongxin.caihong.ui.search;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.broadcast.CardcastUiUpdateUtil;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.broadcast.MucgroupUpdateUtil;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.nearby.UserListGatherActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.view.NoLastDividerItemDecoration;
import com.tongxin.caihong.view.SelectionFrame;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SearchAllActivity extends BaseActivity {
    private List<SearchResultAdapter<?, ?>> adapterList = new ArrayList<>();
    private View searchNew;
    private TextView tvSearchNewKey;
    private String historyKey;
    private View llSearchHistory;
    private List<String> searchHistoryList;
    private EditText mSearchEdit;
    private SearchHistoryAdapter searchHistoryAdapter;
    private View llHideChatEnter;

    public static void start(Context ctx, String historyKey) {
        Intent intent = new Intent(ctx, SearchAllActivity.class);
        intent.putExtra("historyKey", historyKey);
        ctx.startActivity(intent);
    }

    public static List<String> loadSearchHistory(Context ctx, String historyKey) {
        if (TextUtils.isEmpty(historyKey)) {
            return new ArrayList<>();
        }
        SharedPreferences sp = getSp(ctx);
        List<String> ret = null;
        try {
            String jsonList = sp.getString(historyKey, null);
            ret = JSON.parseArray(jsonList, String.class);
        } catch (Exception e) {
            Reporter.unreachable(e);
        }
        if (ret == null) {
            ret = new ArrayList<>();
        }
        return ret;
    }

    public static void saveSearchHistory(Context ctx, String historyKey, List<String> list) {
        if (TextUtils.isEmpty(historyKey)) {
            return;
        }
        SharedPreferences sp = getSp(ctx);
        sp.edit().putString(historyKey, JSON.toJSONString(new LinkedHashSet<>(list)))
                .apply();
    }

    private static SharedPreferences getSp(Context ctx) {
        return ctx.getSharedPreferences("search_history", MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_all);
        initActionBar();
        historyKey = getIntent().getStringExtra("historyKey");
        loadData();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadData() {
        SearchResultAdapter.OnSearchResultClickListener listener = () -> {
            searchHistoryList.add(0, mSearchEdit.getText().toString());
            saveSearchHistory(this, historyKey, searchHistoryList);
            searchHistoryAdapter.setData(SearchHistoryAdapter.toData(searchHistoryList));
            findViewById(R.id.tvClearSearchHistory).setVisibility(searchHistoryList.size() > 0 ? View.VISIBLE : View.GONE);
        };
        adapterList.add(new ContactsSearchResultAdapter(this, coreManager.getSelf().getUserId(), listener));
        adapterList.add(new RoomSearchResultAdapter(this, coreManager.getSelf().getUserId(), listener));
        adapterList.add(new ChatHistorySearchResultAdapter(this, coreManager.getSelf().getUserId(), listener));
        searchHistoryList = loadSearchHistory(this, historyKey);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 可能在单类型搜索结果页记录了新的搜索历史，
        searchHistoryList = loadSearchHistory(this, historyKey);
        searchHistoryAdapter.setData(SearchHistoryAdapter.toData(searchHistoryList));
        findViewById(R.id.tvClearSearchHistory).setVisibility(searchHistoryList.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private void initView() {
        mSearchEdit = findViewById(R.id.search_edit);
        mSearchEdit.post(() -> {
            mSearchEdit.requestFocus();
        });
        findViewById(R.id.tvClearSearchHistory).setVisibility(searchHistoryList.size() > 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvClearSearchHistory).setOnClickListener(v -> {
            SelectionFrame selectionFrame = new SelectionFrame(mContext);
            selectionFrame.setSomething(getString(R.string.clear_search_history), getString(R.string.sure_clean), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    searchHistoryList.clear();
                    saveSearchHistory(mContext, historyKey, searchHistoryList);
                    searchHistoryAdapter.setData(SearchHistoryAdapter.toData(searchHistoryList));
                    findViewById(R.id.tvClearSearchHistory).setVisibility(searchHistoryList.size() > 0 ? View.VISIBLE : View.GONE);
                }
            });
            selectionFrame.show();
        });
        llSearchHistory = findViewById(R.id.llSearchHistory);
        RecyclerView rvSearchHistory = findViewById(R.id.rvSearchHistory);
        NoLastDividerItemDecoration dividerSearchHistory = new NoLastDividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerSearchHistory.setDrawable(getResources().getDrawable(R.drawable.common_divider));
        rvSearchHistory.addItemDecoration(dividerSearchHistory);
        rvSearchHistory.setLayoutManager(new LinearLayoutManager(this));
        searchHistoryAdapter = new SearchHistoryAdapter(SearchHistoryAdapter.toData(searchHistoryList), new SearchHistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchHistoryAdapter.Item item) {
                mSearchEdit.setText(item.chatHistory);
            }

            @Override
            public void onItemDelete(SearchHistoryAdapter.Item item) {
                searchHistoryList.remove(item.chatHistory);
                saveSearchHistory(mContext, historyKey, searchHistoryList);
                searchHistoryAdapter.setData(SearchHistoryAdapter.toData(searchHistoryList));
                findViewById(R.id.tvClearSearchHistory).setVisibility(searchHistoryList.size() > 0 ? View.VISIBLE : View.GONE);
            }
        });
        rvSearchHistory.setAdapter(searchHistoryAdapter);
        ViewGroup llSearchResult = findViewById(R.id.llSearchResult);

        /**
         * 私密模式
         */
        llHideChatEnter = LayoutInflater.from(this).inflate(R.layout.block_hint_chat_enter, llSearchResult, false);
        TextView tvPrivateMode = llHideChatEnter.findViewById(R.id.tvPrivateMode);
        boolean privateMode = PreferenceUtils.getBoolean(mContext, AppConstant.PRIVATE_MODE + coreManager.getSelf().getUserId(), true);
        tvPrivateMode.setText(privateMode
                ? getString(R.string.gain_hide_conversation_out) : getString(R.string.gain_hide_conversation_enter));
        // llHideChatEnter.setOnClickListener(view -> HideChatActivity.start(mContext));
        llHideChatEnter.setOnClickListener(view -> {
            boolean mode = PreferenceUtils.getBoolean(mContext, AppConstant.PRIVATE_MODE + coreManager.getSelf().getUserId(), true);
            mode = !mode;
            PreferenceUtils.putBoolean(mContext, AppConstant.PRIVATE_MODE + coreManager.getSelf().getUserId(), mode);
            tvPrivateMode.setText(mode
                    ? getString(R.string.gain_hide_conversation_out) : getString(R.string.gain_hide_conversation_enter));
            // 刷新消息、通讯录、群组界面
            MsgBroadcast.broadcastMsgUiUpdate(mContext);
            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
            MucgroupUpdateUtil.broadcastUpdateUi(mContext);
            MsgBroadcast.broadcastMsgNumReset(mContext);
            finish();
        });
        llSearchResult.addView(llHideChatEnter);

        for (SearchResultAdapter<?, ?> adapter : adapterList) {
            View block = LayoutInflater.from(this).inflate(R.layout.block_search_result, llSearchResult, false);
            View more = block.findViewById(R.id.rlMore);
            adapter.attach(block, more);
            more.setOnClickListener(v -> {
                SearchSingleTypeActivity.start(this, adapter, mSearchEdit.getText().toString(), historyKey);
            });
            TextView tvMore = more.findViewById(R.id.tvMore);
            tvMore.setText(getString(R.string.search_result_more_place_holder, getString(adapter.getSearchType())));
            tvMore.setTextColor(SkinUtils.getSkin(this).getAccentColor());
            TextView ivResultType = block.findViewById(R.id.ivResultType);
            ivResultType.setText(adapter.getSearchType());
            RecyclerView recyclerView = block.findViewById(R.id.recyclerView);
            NoLastDividerItemDecoration divider = new NoLastDividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            divider.setDrawable(getResources().getDrawable(R.drawable.divider_search_result_item));
            recyclerView.addItemDecoration(divider);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            llSearchResult.addView(block);
        }

        searchNew = LayoutInflater.from(this).inflate(R.layout.block_search_new_friend, llSearchResult, false);
        llSearchResult.addView(searchNew);
        searchNew.setOnClickListener(v -> {
            if (TextUtils.isEmpty(mSearchEdit.getText().toString().trim())) {
                return;
            }
            UserListGatherActivity.start(this, mSearchEdit.getText().toString());
            searchHistoryList.add(0, mSearchEdit.getText().toString());
            saveSearchHistory(this, historyKey, searchHistoryList);
        });
        tvSearchNewKey = searchNew.findViewById(R.id.tvSearchNewKey);
        tvSearchNewKey.setTextColor(SkinUtils.getSkin(this).getAccentColor());

        mSearchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                for (SearchResultAdapter<?, ?> adapter : adapterList) {
                    adapter.search(s.toString());
                }
                if (TextUtils.isEmpty(s)) {
                    llHideChatEnter.setVisibility(View.GONE);
                    searchNew.setVisibility(View.GONE);
                    llSearchHistory.setVisibility(View.VISIBLE);
                } else {
                    String password = PreferenceUtils.getString(mContext, Constants.HIDE_CHAT_PASSWORD + CoreManager.requireSelf(mContext).getUserId());
                    if (TextUtils.equals(password, s.toString())) {
                        llHideChatEnter.setVisibility(View.VISIBLE);
                    } else {
                        llHideChatEnter.setVisibility(View.GONE);
                    }
                    searchNew.setVisibility(View.VISIBLE);
                    tvSearchNewKey.setText(s);
                    llSearchHistory.setVisibility(View.GONE);
                }
            }
        });
    }
}
