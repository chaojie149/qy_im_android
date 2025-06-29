package com.tongxin.caihong;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.tongxin.caihong.map.BaiduMapHelper;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.sp.LocationSp;
import com.tongxin.caihong.util.PermissionUtil;


public class BdLocationHelper {
    public static final String ACTION_LOCATION_UPDATE = AppConfig.sPackageName + ".action.location_update";
    private static final String TAG = "BdLocationHelper";
    private Context mContext;
    private double mLongitude;
    private double mLatitude;
    private String mAddress;
    // 省份
    private String mProvinceName;
    // 城市
    private String mCityName;
    // 街道
    private String mDistrictName;
    // 本次程序启动后，位置有没有成功更新一次
    private boolean isLocationUpdate;
    // 用于限制同意隐私政策前请求定位，
    private boolean enabled;

    public BdLocationHelper(Context context) {
        mContext = context;
        // 获取上一次的定位数据
        mLongitude = LocationSp.getInstance(context).getLongitude(0);
        mLatitude = LocationSp.getInstance(context).getLatitude(0);
        mAddress = LocationSp.getInstance(context).getAddress("");
        mProvinceName = LocationSp.getInstance(context).getProvinceName("");
        mCityName = LocationSp.getInstance(context).getCityName("");
        mDistrictName = LocationSp.getInstance(context).getDistrictName("");

        requestLocation();
    }

    public void requestLocation() {
        requestLocation(null);
    }

    public void requestLocation(@Nullable Runnable onSuccess) {
        if (isLocationUpdate) {
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }
        if (!PermissionUtil.checkSelfPermissions(mContext, PermissionUtil.getLocationPermissions())) {
            // 没权限，不定位，
            return;
        }
        if (!enabled) {
            // 部分手机会假装给了权限，然后定位时再反复弹请求，
            // 所以这里加个判断，没同意隐私政策就当没权限，不定位，
            return;
        }
        Log.d(TAG, "requestLocation() called");
        BaiduMapHelper mapHelper = BaiduMapHelper.getInstance(mContext);
        mapHelper.requestLocationOnce(location -> {
            isLocationUpdate = true;
            MapHelper.LatLng latLng = new MapHelper.LatLng(location.getLatitude(), location.getLongitude());
            Log.d(TAG, "requestLocation() success: " + latLng);
            mLatitude = latLng.getLatitude();
            mLongitude = latLng.getLongitude();
            mAddress = location.getAddrStr();
            mProvinceName = location.getProvince();
            mCityName = location.getCity();
            mDistrictName = location.getDistrict();
            LocationSp.getInstance(mContext).setLongitude((float) mLongitude);
            LocationSp.getInstance(mContext).setLatitude((float) mLatitude);
            LocationSp.getInstance(mContext).setAddress(mAddress);
            LocationSp.getInstance(mContext).setProvinceName(mProvinceName);
            LocationSp.getInstance(mContext).setCityName(mCityName);
            LocationSp.getInstance(mContext).setDistrictName(mDistrictName);
            mContext.sendBroadcast(new Intent(ACTION_LOCATION_UPDATE));// 发送广播
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, Throwable::printStackTrace);
    }

    // 获取经纬度
    public double getLongitude() {
        return mLongitude;
    }

    // 获取经纬度
    public double getLatitude() {
        return mLatitude;
    }

    // 获取地址详情
    public String getAddress() {
        return mAddress;
    }

    public String getProvinceName() {
        return mProvinceName;
    }

    public String getCityName() {
        return mCityName;
    }

    public String getDistrictName() {
        return mDistrictName;
    }

    public boolean isLocationUpdate() {
        return isLocationUpdate;
    }

    public boolean hasData() {
        return mLatitude != 0 && mLongitude != 0;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
