package com.tongxin.caihong.ui.me.collection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.audio_x.VoicePlayer;
import com.tongxin.caihong.bean.collection.Collectiion;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.smarttab.SmartTabLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardforchat;
import okhttp3.Call;

/**
 * Created by Administrator on 2017/10/20 0020.
 * 我的收藏
 */
public class MyCollection extends BaseActivity {
    private static final int REQUEST_CODE_SEARCH = 486;
    public static Map<String, String> deleteIdMap = new HashMap<>();
    private TextView tvTitleRight;

    private SmartTabLayout stlMyCollection;
    private ViewPager vpMyCollection;
    private TextView tvSelectCount;
    private boolean isSendCollection;
    private List<String> titles = new ArrayList<>();
    private List<Fragment> fragments = new ArrayList<>();
    // 是否正在多选收藏
    private boolean isMultiSelectCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_my_collection);
        if (getIntent() != null) {
            isSendCollection = getIntent().getBooleanExtra("IS_SEND_COLLECTION", false);
        }
        initActionBar();
        initView();
        initEvent();
    }

    @Override
    public void onBackPressed() {
        if (!JVCideoPlayerStandardforchat.handlerBack()) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        VoicePlayer.instance().stop();
    }

    @Override
    public void finish() {
        JCVideoPlayer.releaseAllVideos();
        super.finish();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.my_collection);
        tvTitleRight = findViewById(R.id.tv_title_right);
        tvTitleRight.setText(getResources().getString(R.string.edit));
        if (isSendCollection) {
            // 发送收藏时不支持编辑
            tvTitleRight.setVisibility(View.GONE);
        }
    }

    private void initView() {
        titles.add(getResources().getString(R.string.all));
        titles.add(getResources().getString(R.string.pictures));
        titles.add(getResources().getString(R.string.s_video));
        titles.add(getResources().getString(R.string.s_file));
        titles.add(getResources().getString(R.string.voice));
        titles.add(getResources().getString(R.string.word));
        titles.add(getResources().getString(R.string.s_link));
        for (int i = 0; i < titles.size(); i++) {
            fragments.add(new CollectionFragment(i, isSendCollection));
        }

        stlMyCollection = findViewById(R.id.stlMyCollection);
        vpMyCollection = findViewById(R.id.vpMyCollection);
        CollectionPageAdapter collectionPageAdapter = new CollectionPageAdapter(getSupportFragmentManager());
        vpMyCollection.setAdapter(collectionPageAdapter);
        stlMyCollection.setViewPager(vpMyCollection);
        stlMyCollection.setCollectionTabCheckedChanged(fragments.size(), 0);// 默认选中mSelectItems
        stlMyCollection.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                stlMyCollection.setCollectionTabCheckedChanged(fragments.size(), position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tvSelectCount = findViewById(R.id.tvSelectCount);
    }

    private void initEvent() {
        tvTitleRight.setOnClickListener(v -> {
            isMultiSelectCollection = !isMultiSelectCollection;
            changeEdit();
        });

        findViewById(R.id.btnDelete).setOnClickListener(view -> {
            if (deleteIdMap.size() == 0) {
                ToastUtil.showToast(mContext, getString(R.string.only_one_no_data));
                return;
            }
            deleteCollection();
        });

        findViewById(R.id.tvSearch).setOnClickListener(v -> CollectionSearchActivity.start(MyCollection.this, REQUEST_CODE_SEARCH, isSendCollection));
    }

    public void updateSelectCount() {
        tvSelectCount.setText(deleteIdMap.size() + "/" + 20);
    }

    private void changeEdit() {
        if (isMultiSelectCollection) {
            deleteIdMap.clear();
            tvTitleRight.setText(getString(R.string.cancel));
            tvSelectCount.setText(deleteIdMap.size() + "/" + 20);
        } else {
            tvTitleRight.setText(getString(R.string.edit));
        }
        findViewById(R.id.llDelete).setVisibility(isMultiSelectCollection ? View.VISIBLE : View.GONE);
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i) instanceof CollectionFragment) {
                ((CollectionFragment) fragments.get(i)).setIsEdit(isMultiSelectCollection);
            }
        }
    }

    private void deleteCollection() {
        List<String> messageList = new ArrayList<>(deleteIdMap.values());
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("emojiId", TextUtils.join(",", messageList));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(CoreManager.requireConfig(MyApplication.getInstance()).Collection_REMOVE)
                .params(params)
                .build()
                .execute(new BaseCallback<Collectiion>(Collectiion.class) {

                    @Override
                    public void onResponse(ObjectResult<Collectiion> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, getString(R.string.delete_success));
                            isMultiSelectCollection = false;
                            for (int i = 0; i < fragments.size(); i++) {
                                if (fragments.get(i) instanceof CollectionFragment) {
                                    ((CollectionFragment) fragments.get(i)).deleteMsg();
                                }
                            }
                            changeEdit();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SEARCH) {
            if (resultCode == Activity.RESULT_OK) {
                // 搜索收藏界面选择了发送收藏，接着回调出去
                setResult(Activity.RESULT_OK, data);
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class CollectionPageAdapter extends FragmentPagerAdapter {

        public CollectionPageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }
}
