package com.tongxin.caihong.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.util.SkinUtils;
import com.google.android.material.tabs.TabLayout;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.groupchat.RoomFragment;
import com.tongxin.caihong.ui.groupchat.RoomSearchActivity;
import com.tongxin.caihong.ui.groupchat.SelectContactsActivity;

import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends BaseActivity {
    private TabLayout tabLayout;
    private ViewPager mViewPager;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, RoomActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.group));
        ImageView mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        if (coreManager.getLimit().cannotCreateGroup()
                && coreManager.getConfig().isOpenRoomSearch) {
            // 关闭建群&&开启搜索群组，右侧icon变为搜索
            mIvTitleRight.setImageResource(R.mipmap.search_icon);
            mIvTitleRight.setOnClickListener(v -> RoomSearchActivity.start(this));
        } else {
            // 开启建群，右侧icon变为搜索
            if (coreManager.getLimit().cannotCreateGroup()) {
                mIvTitleRight.setVisibility(View.GONE);
            }
            mIvTitleRight.setImageResource(R.mipmap.more_icon);
            mIvTitleRight.setOnClickListener(v -> startActivity(new Intent(this, SelectContactsActivity.class)));

            if (coreManager.getConfig().isOpenRoomSearch) {
                findViewById(R.id.iv_title_right_right).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.iv_title_right_right).setOnClickListener(v -> RoomSearchActivity.start(this));
        }
    }

    private void initView() {
        mViewPager = (ViewPager) findViewById(R.id.tab1_vp);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new RoomFragment());
        // 取消全部群组，
        // fragments.add(new AllRoomFragment());
        mViewPager.setAdapter(new MyTabAdapter(getSupportFragmentManager(), fragments));

        tabLayout = (TabLayout) findViewById(R.id.tab1_layout);
        // 取消全部群组，只剩一个，不要标签栏，
        tabLayout.setVisibility(View.GONE);
        tabLayout.setTabTextColors(getResources().getColor(R.color.text_black), SkinUtils.getSkin(this).getAccentColor());
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }

    class MyTabAdapter extends FragmentPagerAdapter {
        List<String> listTitle = new ArrayList<>();
        private List<Fragment> mFragments;

        MyTabAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;

            listTitle.add(getString(R.string.my_group));
            listTitle.add(getString(R.string.all_group));
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            if (mFragments != null) {
                return mFragments.size();
            }
            return 0;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (listTitle != null) {
                return listTitle.get(position);
            }
            return "";
        }
    }
}
