package com.tongxin.caihong.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.ui.company.ManagerCompanyFragment;
import com.tongxin.caihong.ui.company.NewColleagueActivity;
import com.tongxin.caihong.ui.groupchat.KxGroupFragment;
import com.tongxin.caihong.ui.groupchat.RoomSearchActivity;
import com.tongxin.caihong.ui.groupchat.SelectContactsActivity;
import com.tongxin.caihong.ui.nearby.UserSearchActivity;
import com.tongxin.caihong.ui.smarttab.KxTabProvider;
import com.tongxin.caihong.ui.smarttab.SmartTabLayout;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.broadcast.OtherBroadcast;

import java.util.ArrayList;
import java.util.List;

/**
 * 通讯录，
 * create by zq for shuaba
 */
public class ContactPagerFragment extends EasyFragment {
    private SmartTabLayout smartTabLayout;
    private List<String> titles = new ArrayList<>();
    private List<Fragment> data = new ArrayList<>();
    private ViewPager viewPager;
    private ImageView ivTitleSearch;
    private View tvRedPoint;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, OtherBroadcast.ACTION_COMPANY_APPLY_JOIN_MSG)) {
                findViewById(R.id.tvRedPoint).setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_contact_pager;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initActionBar();
            initView();
            IntentFilter filter = new IntentFilter();
            filter.addAction(OtherBroadcast.ACTION_COMPANY_APPLY_JOIN_MSG);
            requireContext().registerReceiver(broadcastReceiver, filter);
        }
    }

    private void initActionBar() {
        ivTitleSearch = findViewById(R.id.iv_title_right_left);
        tvRedPoint = findViewById(R.id.tvRedPoint);
        ivTitleSearch.setOnClickListener(view -> {
            int position = viewPager.getCurrentItem();
            Fragment fragment = getFragment(position);
            if (fragment instanceof ManagerCompanyFragment) {
                PreferenceUtils.putBoolean(MyApplication.getContext(), AppConstant.COMPANY_APPLY_JOIN_MSG, false);
                tvRedPoint.setVisibility(View.GONE);
                NewColleagueActivity.start(requireContext());
            } else if (fragment instanceof KxGroupFragment) {
                RoomSearchActivity.start(requireContext());
            }
        });
        findViewById(R.id.iv_title_right).setOnClickListener(v -> {
            int position = viewPager.getCurrentItem();
            Fragment fragment = getFragment(position);
            if (fragment instanceof FriendFragment) {
                UserSearchActivity.start(requireContext());
            } else if (fragment instanceof ManagerCompanyFragment) {
                ((ManagerCompanyFragment) fragment).onActionBarAddClick(v);
            } else if (fragment instanceof KxGroupFragment) {
                startActivity(new Intent(requireContext(), SelectContactsActivity.class));
            }
        });
    }

    private Fragment getFragment(int position) {
        return getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + position);
    }

    private void initView() {
        String order = coreManager.getConfig().contactOrder;
        for (char ch : order.toCharArray()) {
            switch (ch) {
                case '1':
                    titles.add(getString(R.string.me_tab_friend));
                    data.add(new FriendFragment());
                    break;
                case '2':
                    titles.add(getString(R.string.tab_company));
                    data.add(new ManagerCompanyFragment());
                    break;
                case '3':
                    titles.add(getString(R.string.group));
                    data.add(new KxGroupFragment());
                    break;
            }
        }

        smartTabLayout = findViewById(R.id.smartTabLayout);
        smartTabLayout.setCustomTabView(new KxTabProvider(requireContext()));
        viewPager = findViewById(R.id.viewPager);
        PageAdapter pageAdapter = new PageAdapter(getChildFragmentManager());
        viewPager.setAdapter(pageAdapter);
        smartTabLayout.setViewPager(viewPager);
        ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                KxTabProvider.onPageSelected(smartTabLayout, position);
                Fragment fragment = getFragment(position);
                if (fragment instanceof KxGroupFragment && coreManager.getConfig().isOpenRoomSearch) {
                    // ivTitleSearch.setVisibility(View.VISIBLE);
                    // ivTitleSearch.setImageResource(R.mipmap.search_icon);
                    // 强制隐藏
                    ivTitleSearch.setVisibility(View.GONE);
                    tvRedPoint.setVisibility(View.GONE);
                } else if (fragment instanceof ManagerCompanyFragment) {
                    ivTitleSearch.setVisibility(View.VISIBLE);
                    ivTitleSearch.setImageResource(R.mipmap.ic_company_notify);
                    boolean isHasCompanyApplyJoinMsg = PreferenceUtils.getBoolean(MyApplication.getContext(), AppConstant.COMPANY_APPLY_JOIN_MSG, false);
                    tvRedPoint.setVisibility(isHasCompanyApplyJoinMsg ? View.VISIBLE : View.GONE);
                } else {
                    ivTitleSearch.setVisibility(View.GONE);
                    tvRedPoint.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
        smartTabLayout.setOnPageChangeListener(onPageChangeListener);
        smartTabLayout.post(() -> onPageChangeListener.onPageSelected(0));
    }

    class PageAdapter extends FragmentPagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
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
            return titles.get(position);
        }
    }

}
