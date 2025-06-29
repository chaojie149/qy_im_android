package com.tongxin.caihong.ui.groupchat;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.OnCompleteListener2;
import com.tongxin.caihong.helper.HideSearchHelper;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.NoScrollViewPager;
import com.tongxin.caihong.view.SearchHeader;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

/**
 * 群组
 * create by zq for shuaba
 */
public class KxGroupFragment extends EasyFragment {
    private static final String TAG = "KxGroupFragment";
    private List<Fragment> data = new ArrayList<>();
    private NoScrollViewPager viewPager;
    private String searchKey;
    private RefreshLayout refreshLayout;
    private Handler mHandler = new Handler();

    @Override
    protected int inflateLayoutId() {
        return R.layout.kx_fragment_group;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initView();
        }
        HideSearchHelper.hideSearchHeader(this);
    }

    private Fragment getFragment(int position) {
        return getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + position);
    }

    public String getSearchKey() {
        return searchKey;
    }

    private void initView() {
        String[] stringArray = getResources().getStringArray(R.array.kx_message_group);
        viewPager = findViewById(R.id.vpPlayGroup);
        viewPager.setScroll(true);
        data.add(RoomFragment.create(0));
        data.add(RoomFragment.create(1));
        data.add(RoomFragment.create(2));
        PlayGroupPageAdapter adapter = new PlayGroupPageAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);
        initIndicator(findViewById(R.id.llIndicator), viewPager, stringArray);

        SearchHeader mEditText = findViewById(R.id.shSearch);
        mEditText.setFocusClear(false);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchKey = s.toString().trim();
                for (int i = 0; i < adapter.getCount(); i++) {
                    RoomFragment fragment = (RoomFragment) getFragment(i);
                    if (fragment != null) {
                        fragment.onSearchKeyChanged(s);
                    }
                }
            }
        });

        refreshLayout = findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(r -> {
            updateRoom();
        });
    }

    private void initIndicator(ViewGroup layout, ViewPager viewPager, String[] tagList) {
        for (int i = 0; i < tagList.length; i++) {
            TextView child = (TextView) layout.getChildAt(i * 2);
            child.setText(tagList[i]);
            int finalI = i;
            child.setOnClickListener(v -> {
                viewPager.setCurrentItem(finalI);
            });
        }
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                selectIndicator(layout, position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        selectIndicator(layout, 0);
    }

    private void selectIndicator(ViewGroup layout, int position) {
        for (int i = 0; i < ((layout.getChildCount() + 1) / 2); i++) {
            TextView child = (TextView) layout.getChildAt(i * 2);
            if (i == position) {
                child.setTypeface(Typeface.DEFAULT_BOLD);
                child.setBackgroundResource(R.drawable.bubble_pager_indicator_selected_bg);
            } else {
                child.setTypeface(Typeface.DEFAULT);
                child.setBackground(null);
            }
        }
        for (int i = 0; i < ((layout.getChildCount() - 1) / 2); i++) {
            View child = layout.getChildAt(i * 2 + 1);
            if (i == position || i + 1 == position) {
                child.setVisibility(View.INVISIBLE);
            } else {
                child.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 下载我的群组
     */
    private void updateRoom() {
        Log.d(TAG, "updateRoom() called");
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", "0");
        params.put("pageIndex", "0");
        params.put("pageSize", String.valueOf(Integer.MAX_VALUE));// 不支持翻页，直接给最大值，

        HttpUtils.get().url(coreManager.getConfig().ROOM_LIST_HIS)
                .params(params)
                .build()
                .execute(new ListCallback<MucRoom>(MucRoom.class) {
                    @Override
                    public void onResponse(ArrayResult<MucRoom> result) {
                        Log.d(TAG, "onResponse() called with: result = [" + result.getResultCode() + "]");
                        if (result.getResultCode() == 1) {
                            if (result.getData() != null) {
                                Log.d(TAG, "onResponse: size=" + result.getData().size());
                            }
                            AsyncUtils.doAsync(getContext(), e -> {
                                Reporter.post("保存群组失败，", e);
                                AsyncUtils.runOnUiThread(getContext(), ctx -> {
                                    if (ctx != null) {
                                        ToastUtil.showToast(ctx, R.string.data_exception);
                                    }
                                });
                            }, c -> {
                                String mLoginUserId = coreManager.getSelf().getUserId();
                                FriendDao.getInstance().addRooms(mHandler, mLoginUserId, result.getData(), new OnCompleteListener2() {

                                    @Override
                                    public void onLoading(int progressRate, int sum) {
                                    }

                                    @Override
                                    public void onCompleted() {
                                        Log.d(TAG, "onCompleted() called");
                                        refreshAllFragment();
                                    }
                                });
                            });
                        } else {
                            refreshLayout.finishRefresh();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(getActivity());
                        refreshLayout.finishRefresh();
                    }
                });
    }

    private void refreshAllFragment() {
        for (int i = 0; i < viewPager.getAdapter().getCount(); i++) {
            RoomFragment fragment = (RoomFragment) getFragment(i);
            if (fragment != null) {
                fragment.loadData();
            }
        }
        refreshLayout.finishRefresh();
    }

    class PlayGroupPageAdapter extends FragmentPagerAdapter {

        public PlayGroupPageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return data.get(position);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getStringArray(R.array.kx_message_group)[position];
        }
    }
}
