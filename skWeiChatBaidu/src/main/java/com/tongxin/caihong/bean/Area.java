package com.tongxin.caihong.bean;

import android.text.TextUtils;

import com.tongxin.caihong.MyApplication;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.tongxin.caihong.db.dao.AreasDao;

import java.util.Locale;

@DatabaseTable(tableName = "tb_areas")
public class Area {

    public static final int AREA_DATA_CHINA_ID = 1;// 中国
    public static final int AREA_TYPE_COUNTRY = 1; // 国家
    public static final int AREA_TYPE_PROVINCE = 2;// 省份
    public static final int AREA_TYPE_CITY = 3;    // 城市
    public static final int AREA_TYPE_COUNTY = 4;  // 县

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private int parent_id;

    @DatabaseField
    private int type;

    @DatabaseField
    private String name;

    @DatabaseField
    private String enName;

    @DatabaseField
    private String zip;

    public static String getProvinceCityString(int provinceId, int cityId) {
        String provinceStr = null;
        String cityStr = null;
        if (provinceId != 0) {
            Area area = AreasDao.getInstance().getArea(provinceId);
            provinceStr = area != null ? area.getI18nName() : null;
        }
        if (cityId != 0) {
            Area area = AreasDao.getInstance().getArea(cityId);
            cityStr = area != null ? area.getI18nName() : null;
        }

        if (!TextUtils.isEmpty(provinceStr) && !TextUtils.isEmpty(cityStr)) {
            return provinceStr + "-" + cityStr;
        } else if (!TextUtils.isEmpty(provinceStr) && TextUtils.isEmpty(cityStr)) {
            return provinceStr;
        } else if (TextUtils.isEmpty(provinceStr) && !TextUtils.isEmpty(cityStr)) {
            return cityStr;
        } else {
            return "";
        }
    }

    public static String getCityAreaString(int cityId, int areaId) {
        String cityStr = null;
        String areaStr = null;
        if (cityId != 0) {
            Area area = AreasDao.getInstance().getArea(cityId);
            cityStr = area != null ? area.getI18nName() : null;
        }
        if (areaId != 0) {
            Area area = AreasDao.getInstance().getArea(areaId);
            areaStr = area != null ? area.getI18nName() : null;
        }
        if (!TextUtils.isEmpty(cityStr) && !TextUtils.isEmpty(areaStr)) {
            return cityStr + "-" + areaStr;
        } else if (!TextUtils.isEmpty(cityStr) && TextUtils.isEmpty(areaStr)) {
            return cityStr;
        } else if (TextUtils.isEmpty(cityStr) && !TextUtils.isEmpty(areaStr)) {
            return areaStr;
        } else {
            return "";
        }
    }

    public static int getDefaultCountyId() {// 获取默认的国家的Id
        return AREA_DATA_CHINA_ID;
    }

    public static Area getDefaultProvince() {// 获取默认的省份
        Area area = null;
        String provinceName = MyApplication.getInstance().getBdLocationHelper().getProvinceName();
        if (!TextUtils.isEmpty(provinceName)) {
            area = AreasDao.getInstance().searchByName(provinceName);
        }
        return area;
    }

    public static Area getDefaultCity() {// 获取默认的城市
        Area area = null;
        String cityName = MyApplication.getInstance().getBdLocationHelper().getCityName();
        if (!TextUtils.isEmpty(cityName)) {
            area = AreasDao.getInstance().searchByName(cityName);
        }
        return area;
    }

    public static Area getDefaultDistrict(int cityId) {// 获取默认的区县
        String districtName = MyApplication.getInstance().getBdLocationHelper().getDistrictName();
        Area area = AreasDao.getInstance().searchByNameAndParentId(cityId, districtName);
        return area;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParent_id() {
        return parent_id;
    }

    public void setParent_id(int parent_id) {
        this.parent_id = parent_id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getI18nName() {
        if (Locale.getDefault().getLanguage().startsWith("zh")) {
            return getName();
        } else {
            return getEnName();
        }
    }

}
