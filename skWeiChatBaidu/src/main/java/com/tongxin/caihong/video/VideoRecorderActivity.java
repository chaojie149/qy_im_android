package com.tongxin.caihong.video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.bean.event.MessageVideoFile;
import com.tongxin.caihong.helper.CutoutHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.google.android.material.tabs.TabLayout;
import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天界面视频录制类
 * create by TAG
 * update time 2018-11-21 19:43:13
 */

public class VideoRecorderActivity extends BaseActivity {

    private ArrayList<Fragment> fragments = new ArrayList<>();
    private List<String> listTitles = new ArrayList<>();
    private PictrueFragment pictrueFragment;
    private ViedioFragment viedioFragment;
    private AlbumOrientationEventListener mAlbumOrientationEventListener;
    // 初始状态不旋转，
    private int mOrientation = 0;
    //2. 保存MyOnTouchListener接口的列表
    private ArrayList<MyOnTouchListener> onTouchListeners = new ArrayList<MyOnTouchListener>();
    private CustomTabLayout mTabLayout;
    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener tabOnClickListener = (view, event) -> {
        int pos = (int) view.getTag();
        if (pos == 0) {
            if (viedioFragment != null && viedioFragment.isRecord()) {
                // 拦截
                return true;
            }
        }
        return false;
    };

    public static void startForResult(Fragment ctx, int requestCode) {
        Intent intent = new Intent(ctx.requireContext(), VideoRecorderActivity.class);
        ctx.startActivityForResult(intent, requestCode);
    }

    public static void startForResult(Activity ctx, int requestCode) {
        startForResult(ctx, requestCode, false);
    }

    public static void startForResult(Activity ctx, int requestCode, boolean videoOnly) {
        Intent intent = new Intent(ctx, VideoRecorderActivity.class);
        intent.putExtra("videoOnly", videoOnly);
        ctx.startActivityForResult(intent, requestCode);
    }

    /**
     * @return 是拍摄图片返回true, 是拍摄视频返回false,
     */
    public static boolean checkIsImage(Intent data) {
        return data.getExtras().containsKey("image");
    }

    public static MessageEventGpu parseImageResult(Intent data) {
        return (MessageEventGpu) data.getSerializableExtra("image");
    }

    public static MessageVideoFile parseVideoResult(Intent data) {
        return (MessageVideoFile) data.getSerializableExtra("video");
    }

    public static Intent makeImageResult(MessageEventGpu image) {
        Intent ret = new Intent();
        ret.putExtra("image", image);
        return ret;
    }

    public static Intent makeVideoResult(MessageVideoFile video) {
        Intent ret = new Intent();
        ret.putExtra("video", video);
        return ret;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        CutoutHelper.setWindowOut(getWindow());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_record);
        initView();
    }

    private void initView() {
        ViewPager vp_video = findViewById(R.id.vp_video);
        pictrueFragment = new PictrueFragment();
        viedioFragment = new ViedioFragment();
        boolean videoOnly = getIntent().getBooleanExtra("videoOnly", false);
        if (!videoOnly) {
            fragments.add(pictrueFragment);
            listTitles.add(getString(R.string.photograph));
        }
        fragments.add(viedioFragment);
        listTitles.add(getString(R.string.video));
        MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager(), fragments, listTitles);
        vp_video.setAdapter(adapter);

        mTabLayout = (CustomTabLayout) findViewById(R.id.tabLayout);
        if (!videoOnly) {
            mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.photograph)), false);
            mTabLayout.addTab(mTabLayout.newTab().setText(getString(R.string.video)), false);
            for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = mTabLayout.getTabAt(i);
                TextView textView = (TextView) LayoutInflater.from(mContext).inflate(R.layout.tab_custom, null);
                textView.setText(tab.getText());
                if (tab != null) {
                    tab.setCustomView(textView);
                    if (tab.getCustomView() != null) {
                        View tabView = (View) tab.getCustomView().getParent();
                        tabView.setTag(i);
                        tabView.setOnTouchListener(tabOnClickListener);
                    }
                }
            }
       /*     mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    TextView textView = (TextView) LayoutInflater.from(mContext).inflate(R.layout.tab_custom, null);
                    textView.setText(tab.getText());
                    tab.setCustomView(textView);
                    if (tab.getCustomView() != null) {
                        View tabView = (View) tab.getCustomView().getParent();
                        tabView.setTag(tab.getText().equals(getString(R.string.c_take_picture)) ? 0 : 1);
                        tabView.setOnClickListener(mTabOnClickListener);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    tab.setCustomView(null);
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });*/
            // 将TabLayout和ViewPager关联起来。
            mTabLayout.setupWithViewPager(vp_video);
        }
        CutoutHelper.initCutoutHolderTop(getWindow(), findViewById(R.id.vCutoutHolder));
        mAlbumOrientationEventListener = new AlbumOrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL);
        if (mAlbumOrientationEventListener.canDetectOrientation()) {
            mAlbumOrientationEventListener.enable();
        } else {
            Log.e("zx", "不能获取Orientation");
        }
    }

    //3.分发触摸事件给所有注册了MyOnTouchListener的接口
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mTabLayout == null) {
            return super.dispatchTouchEvent(ev);
        }
        if (ev.getY() > mTabLayout.getBottom()) {
            for (MyOnTouchListener listener : onTouchListeners) {
                listener.onTouch(ev);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    //4.提供给Fragment通过getActivity()方法来注册自己的触摸事件的方法
    public void registerMyOnTouchListener(MyOnTouchListener myOnTouchListener) {
        onTouchListeners.add(myOnTouchListener);
    }

    //5.提供给Fragment通过getActivity()方法来注销自己的触摸事件的方法
    public void unregisterMyOnTouchListener(MyOnTouchListener myOnTouchListener) {
        onTouchListeners.remove(myOnTouchListener);
    }

    //1.触摸事件接口
    public interface MyOnTouchListener {
        public boolean onTouch(MotionEvent ev);
    }

    public class AlbumOrientationEventListener extends OrientationEventListener {
        public AlbumOrientationEventListener(Context context) {
            super(context);
        }

        public AlbumOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                return;
            }

            //保证只返回四个方向
            int newOrientation = ((orientation + 45) / 90 * 90) % 360;

            if (newOrientation != mOrientation) {
                Log.e("zx", "onOrientationChanged: " + mOrientation);
                //返回的mOrientation就是手机方向，为0°、90°、180°和270°中的一个
                mOrientation = newOrientation;
            }
        }
    }

    class MyPagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> mfragmentList;
        private List<String> listTitles;

        public MyPagerAdapter(FragmentManager fm, List<Fragment> fragmentList, List<String> list) {
            super(fm);
            this.mfragmentList = fragmentList;
            this.listTitles = list;
        }

        @Override
        public Fragment getItem(int position) {
            return mfragmentList.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return listTitles.get(position);
        }

        @Override
        public int getCount() {
            return mfragmentList.size();
        }
    }
}