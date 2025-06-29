package com.tongxin.caihong.ui.message.multi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.util.SkinUtils;
import com.google.android.material.tabs.TabLayout;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * Created by Administrator on 2017/6/28 0028.
 * 群已读人数
 */
public class RoomReadListActivity extends BaseActivity {

    private ViewPager mViewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_read_list);
        initView();
        getReadCount();
    }

    private void initView() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> {
            finish();
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.read_list));

        mViewPager = (ViewPager) findViewById(R.id.tab1_vp);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(RoomReadListFragment.create(0));
        fragments.add(RoomReadListFragment.create(1));
        mViewPager.setOffscreenPageLimit(3); // 一共没几页，全部缓存，
        mViewPager.setAdapter(new MyTabAdapter(getSupportFragmentManager(), fragments));

        tabLayout = (TabLayout) findViewById(R.id.tab1_layout);
        int accentColor = SkinUtils.getSkin(mContext).getAccentColor();
        tabLayout.setTabTextColors(getResources().getColor(R.color.text_black), accentColor);
        tabLayout.setSelectedTabIndicatorColor(accentColor);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }

    /**
     * 通过接口获取群已读人数，用于校准外面已读人数不准的问题
     */
    private void getReadCount() {
        String roomJid = getIntent().getStringExtra("roomJid");
        String messageId = getIntent().getStringExtra("packetId");
        Map<String, String> params = new HashMap<>();
        params.put("roomJid", roomJid);
        params.put("messageId", messageId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_MESSAGE_READ_COUNT)
                .params(params)
                .build()
                .execute(new BaseCallback<Integer>(Integer.class) {

                    @Override
                    public void onResponse(ObjectResult<Integer> result) {
                        if (result.getResultCode() == 1) {
                            ChatMessageDao.getInstance().setMessageRead(coreManager.getSelf().getUserId(), roomJid, messageId, result.getData());
                            // 通知聊天界面刷新
                            Intent intent = new Intent(OtherBroadcast.ACTION_GROUP_REFRESH);
                            intent.putExtra("type", 0);
                            intent.putExtra(AppConstant.EXTRA_MSG_ID, messageId);
                            intent.putExtra("readCount", result.getData());
                            sendBroadcast(intent);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    class MyTabAdapter extends FragmentPagerAdapter {
        List<String> listTitle = new ArrayList<>();
        private List<Fragment> mFragments;

        MyTabAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;

            listTitle.add(getString(R.string.read_list));
            listTitle.add(getString(R.string.unread_list));
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
