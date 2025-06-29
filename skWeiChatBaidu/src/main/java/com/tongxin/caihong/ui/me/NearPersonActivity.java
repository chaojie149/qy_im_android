package com.tongxin.caihong.ui.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.nearby.NearbyGridFragment;
import com.tongxin.caihong.ui.nearby.NearbyMapFragment;
import com.tongxin.caihong.ui.nearby.UserSearchActivity;
import com.tongxin.caihong.util.MyFragmentManager;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.view.NearSeachDialog;
import com.tongxin.caihong.view.TabView;

/**
 * 附近的人
 */
public class NearPersonActivity extends BaseActivity {
    private static final int REQUEST_CODE_LOCATION = 10;
    private TabView tabView;
    private MyFragmentManager mMyFragmentManager;
    // 列表
    private NearbyGridFragment mGridFragment;
    // 地图
    private NearbyMapFragment mMapFragment;
    private NearSeachDialog nearSeachDialog;
    NearSeachDialog.OnNearSeachDialogClickListener onNearSeachDialogClickListener = new NearSeachDialog.OnNearSeachDialogClickListener() {

        @Override
        public void tv1Click() {
            mGridFragment.refreshData("");
            mMapFragment.refreshData("");
        }

        @Override
        public void tv2Click() {
            mGridFragment.refreshData("1");
            mMapFragment.refreshData("1");
        }

        @Override
        public void tv3Click() {
            mGridFragment.refreshData("0");
            mMapFragment.refreshData("0");
        }

        @Override
        public void tv4Click() {
            nearSeachDialog.dismiss();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardcast);
        initActionBar();
        initView();
        PermissionUtil.autoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
            MyApplication.getInstance().getBdLocationHelper().requestLocation(() -> {
                runOnUiThread(this::initData);
            });
        }, () -> {
            finish();
        }, PermissionUtil.getLocationPermissions());
        MapHelper.getInstance().requestLocationOn(this, REQUEST_CODE_LOCATION);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.near_person));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right_right);
        ivRight.setVisibility(View.GONE);
        ivRight.setImageResource(R.mipmap.search_icon);
        ivRight.setOnClickListener((view) -> UserSearchActivity.start(NearPersonActivity.this));

        ImageView iv_title_right = (ImageView) findViewById(R.id.iv_title_right);
        iv_title_right.setVisibility(View.VISIBLE);
        iv_title_right.setImageResource(R.mipmap.ic_near_person3);
        iv_title_right.setOnClickListener((view) -> {
            nearSeachDialog = new NearSeachDialog(NearPersonActivity.this, onNearSeachDialogClickListener);
            nearSeachDialog.show();
        });
    }

    private void initView() {
        tabView = new TabView(this);
        mGridFragment = new NearbyGridFragment();
        mMapFragment = new NearbyMapFragment();
        mMyFragmentManager = new MyFragmentManager(this, R.id.fl_fragments);
        if (coreManager.getConfig().isOpenUI == 0) {
            mMyFragmentManager.add(mGridFragment);
            mMyFragmentManager.show(0);
        } else {
            mMyFragmentManager.add(mGridFragment, mMapFragment);
            tabView.getAttention_single_tv().setText(getString(R.string.map));
            tabView.getAttention_each_tv().setText(getString(R.string.near_person));
            ((LinearLayout) findViewById(R.id.ll_content)).addView(tabView.getView(), 0);
            tabView.setOnTabSelectedLisenter(index -> mMyFragmentManager.show(index));
            tabView.callOnSelect(1);
        }
    }

    private void initData() {
        mGridFragment.initDatas(0);
        mMapFragment.initBeginLatLng();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            initData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
