package com.tongxin.caihong.ui.live;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.InternationalizationHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.live.bean.LiveRoom;
import com.tongxin.caihong.ui.live.livelist.LiveFragment;
import com.tongxin.caihong.ui.live.livelist.LivePlayingFragment;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 直播
 */
public class LiveActivity extends BaseActivity implements View.OnClickListener {
    private TextView mTvTitle;
    private ImageView mIvTitleRight;

    private TabLayout tabLayout;
    private ViewPager mViewPager;

    private SelectionFrame mSelectionFrame;
    private String mLoginUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_viewpager);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText(getString(R.string.live));
        mIvTitleRight = (ImageView) findViewById(R.id.iv_title_right);
        mIvTitleRight.setImageResource(R.drawable.ic_app_add);
        mIvTitleRight.setOnClickListener(this);
    }

    private void initView() {
        mLoginUserId = coreManager.getSelf().getUserId();

        mViewPager = (ViewPager) findViewById(R.id.vpLive);
        List<Fragment> fragments = new ArrayList<>();
        // TODO 隐藏全部直播
        fragments.add(new LiveFragment());
        fragments.add(new LivePlayingFragment());
        mViewPager.setAdapter(new MyTabAdapter(getSupportFragmentManager(), fragments));

        tabLayout = (TabLayout) findViewById(R.id.tbLive);
        tabLayout.setTabTextColors(getResources().getColor(R.color.text_black), SkinUtils.getSkin(this).getAccentColor());
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
       // tabLayout.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        isExistLiveRoom();
    }

    private void showSelectionDialog(final String url, final String roomId, final String roomJid, final String roomName) {
        mSelectionFrame = new SelectionFrame(this);
        mSelectionFrame.setSomething(getString(R.string.app_name), InternationalizationHelper.getString("JXLive_createexistRoom"),
                new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        openLive(url, roomId, roomJid, roomName);
                    }
                });
        mSelectionFrame.show();
    }

    private void isExistLiveRoom() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);

        HttpUtils.get().url(coreManager.getConfig().LIVE_GET_LIVEROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<LiveRoom>(LiveRoom.class) {
                    @Override
                    public void onResponse(final ObjectResult<LiveRoom> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (result.getData() != null) {
                                LiveRoom liveRoom = result.getData();
                                if (liveRoom.getCurrentState() == 1) {
                                    DialogHelper.tip(LiveActivity.this, getString(R.string.tip_live_locking));
                                } else {
                                    SelectionFrame selectionFrame = new SelectionFrame(LiveActivity.this);
                                    selectionFrame.setSomething(null, getString(R.string.you_have_one_live_room) + "，" +
                                            getString(R.string.start_live) + "？", new SelectionFrame.OnSelectionFrameClickListener() {
                                        @Override
                                        public void cancelClick() {

                                        }

                                        @Override
                                        public void confirmClick() {  // 进入直播间
                                            LiveRoom liveRoom = result.getData();
                                            openLive(liveRoom.getUrl(), liveRoom.getRoomId(), liveRoom.getJid(), liveRoom.getName());
                                        }
                                    });
                                    selectionFrame.show();
                                }
                            } else { // 创建直播间
                                Intent intent = new Intent(LiveActivity.this, CreateLiveActivity.class);
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(LiveActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(LiveActivity.this);
                    }
                });
    }

    private void openLive(final String url, final String roomId, final String roomJid, final String roomName) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("userId", mLoginUserId);

        HttpUtils.get().url(coreManager.getConfig().JOIN_LIVE_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Intent intent = new Intent(LiveActivity.this, PushFlowActivity.class);
                            intent.putExtra(LiveConstants.LIVE_PUSH_FLOW_URL, url);
                            intent.putExtra(LiveConstants.LIVE_ROOM_ID, roomId);
                            intent.putExtra(LiveConstants.LIVE_CHAT_ROOM_ID, roomJid);
                            intent.putExtra(LiveConstants.LIVE_ROOM_NAME, roomName);
                            intent.putExtra(LiveConstants.LIVE_ROOM_PERSON_ID, mLoginUserId);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(LiveActivity.this);
                    }
                });
    }

    class MyTabAdapter extends FragmentPagerAdapter {
        List<String> listTitle = new ArrayList<>();
        private List<Fragment> mFragments;

        MyTabAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;

            listTitle.add(InternationalizationHelper.getString("JXLive_allLive"));
            listTitle.add(InternationalizationHelper.getString("JXLive_living"));
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
