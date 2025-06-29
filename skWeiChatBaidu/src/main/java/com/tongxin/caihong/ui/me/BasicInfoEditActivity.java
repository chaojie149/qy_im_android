package com.tongxin.caihong.ui.me;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.bean.event.EventAvatarUploadSuccess;
import com.tongxin.caihong.db.dao.UserAvatarDao;
import com.tongxin.caihong.db.dao.UserDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.CertificationHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.UsernameHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.other.QRcodeActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.ui.tool.SelectAreaActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.CameraUtil;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.filter.RegexInputFilter;
import com.tongxin.caihong.view.SelectImageDialog;
import com.tongxin.caihong.view.SelectSexView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import okio.ByteString;

/**
 * 编辑个人资料
 */
public class BasicInfoEditActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CAPTURE_CROP_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_CROP_PHOTO = 2;
    private static final int REQUEST_CODE_CROP_PHOTO = 3;
    private static final int REQUEST_CODE_SET_ACCOUNT = 5;
    // widget
    private ImageView mAvatarImg;
    private EditText mNameEdit;
    private TextView mSexTv;
    private SelectSexView ssView;
    private TextView mBirthdayTv;
    private TextView mCityTv;
    private TextView tvDesc;
    private Button mNextStepBtn;
    private TextView mAccountDesc;
    private TextView mAccount;
    private User mUser;
    // Temp
    private User mTempData;
    private File mCurrentFile;
    private Uri mNewPhotoUri;
    private PictureType mPictureType; // 选择头像或者选择营业执照，

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info_edit);
        mUser = coreManager.getSelf();
        if (!LoginHelper.isUserValidation(mUser)) {
            finish();
            return;
        }
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.base_info));
    }

    private void initView() {
        TextView tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        UsernameHelper.initTextView(tvPhoneNumber, coreManager.getConfig().registerUsername);

        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        mNameEdit = (EditText) findViewById(R.id.name_edit);
        ArrayList<InputFilter> filters = new ArrayList<>(Arrays.asList(mNameEdit.getFilters()));
        filters.add(new RegexInputFilter("[^@<>/\\s]*"));
        mNameEdit.setFilters(filters.toArray(new InputFilter[0]));
        mSexTv = (TextView) findViewById(R.id.sex_tv);
        ssView = findViewById(R.id.ssView);
        mBirthdayTv = (TextView) findViewById(R.id.birthday_tv);
        mCityTv = (TextView) findViewById(R.id.city_tv);
        tvDesc = findViewById(R.id.tv_scan_desc);
        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
        ButtonColorChange.colorChange(this, mNextStepBtn);
        mAccountDesc = (TextView) findViewById(R.id.sk_account_desc_tv);
        mAccountDesc.setText(getString(R.string.sk_account, getString(R.string.sk_account_code)));
        mAccount = (TextView) findViewById(R.id.sk_account_tv);

        mAvatarImg.setOnClickListener(this);
        findViewById(R.id.rl_avatar).setOnClickListener(this);
        // findViewById(R.id.sex_select_rl).setOnClickListener(this);
        ssView.setOnSexChangeListener(sex -> mTempData.setSex(sex));
        findViewById(R.id.birthday_select_rl).setOnClickListener(this);
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.city_select_rl).setVisibility(View.GONE);
        } else {
            findViewById(R.id.city_select_rl).setOnClickListener(this);
        }
        findViewById(R.id.rl_desc).setOnClickListener(this);
        findViewById(R.id.qccodeforchat).setOnClickListener(this);
        mNextStepBtn.setOnClickListener(this);

        if (coreManager.getConfig().registerInviteCode == 2
                && !TextUtils.isEmpty(coreManager.getSelf().getMyInviteCode())) {
            TextView tvInviteCode = findViewById(R.id.invite_code_tv);
            tvInviteCode.setText(coreManager.getSelf().getMyInviteCode());
        } else {
            findViewById(R.id.rlInviteCode).setVisibility(View.GONE);
        }

        findViewById(R.id.rlLicense).setOnClickListener(this);

        updateUI();
        findViewById(R.id.rlCertification).setOnClickListener(this);
        updateCertificationStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvDesc != null) {
            tvDesc.setText(coreManager.getSelf().getDescription());
        }
    }

    private void updateCertificationStatus() {
        if (CertificationHelper.isAuth(mContext)) {
            ((TextView) findViewById(R.id.tvCertification)).setText(R.string.certification_already);
        } else {
            ((TextView) findViewById(R.id.tvCertification)).setText(R.string.certification_never);
            AsyncUtils.doAsync(mContext, c -> {
                CertificationHelper.init(mContext);
            });
        }
    }

    private void updateUI() {
        // clone一份临时数据，用来存数变化的值，返回的时候对比有无变化
        try {
            mTempData = (User) mUser.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        AvatarHelper.getInstance().updateAvatar(mTempData.getUserId());
        // AvatarHelper.getInstance().displayAvatar(mTempData.getUserId(), mAvatarImg, false);
        displayAvatar(mTempData.getUserId());

        mNameEdit.setText(mTempData.getNickName());
        if (mTempData.getSex() == 1) {
            mSexTv.setText(getString(R.string.sex_man));
        } else {
            mSexTv.setText(getString(R.string.sex_woman));
        }
        ssView.setSex(mTempData.getSex());
        mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str_for_birthday(mTempData.getBirthday()));
        mCityTv.setText(Area.getProvinceCityString(mTempData.getCityId(), mTempData.getAreaId()));

        TextView mPhoneTv = (TextView) findViewById(R.id.phone_tv);
        String phoneNumber = coreManager.getSelf().getTelephoneNoAreaCode();
        mPhoneTv.setText(phoneNumber);

        initAccount();
    }

    private void initAccount() {
        if (mTempData != null) {
            if (mTempData.getSetAccountCount() == 0) {
                // 之前未设置过sk号 前往设置
                findViewById(R.id.sk_account_rl).setOnClickListener(v -> {
                    Intent intent = new Intent(mContext, SetAccountActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mTempData.getUserId());
                    intent.putExtra(AppConstant.EXTRA_NICK_NAME, mTempData.getNickName());
                    startActivityForResult(intent, REQUEST_CODE_SET_ACCOUNT);
                });
                findViewById(R.id.city_arrow_img_05).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.sk_account_rl).setOnClickListener(null);
                findViewById(R.id.city_arrow_img_05).setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(mTempData.getAccount())) {
                mAccount.setText(mTempData.getAccount());
            }
        }
    }

    public void displayAvatar(final String userId) {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(userId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(userId);

            ImageLoadHelper.loadImageSignatureDontAnimateWithPlaceHolder(
                    MyApplication.getContext(),
                    mOriginalUrl,
                    R.drawable.avatar_normal,
                    time,
                    d -> {
                        DialogHelper.dismissProgressDialog();
                        mAvatarImg.setImageDrawable(d);
                    }, e -> {
                        DialogHelper.dismissProgressDialog();
                        Log.e("zq", "加载原图失败：" + mOriginalUrl);// 该用户未设置头像，网页访问该URL也是404
                        AvatarHelper.getInstance().displayAvatar(mTempData.getNickName(), userId, mAvatarImg, true);
                    }
            );
        } else {
            DialogHelper.dismissProgressDialog();
            Log.e("zq", "未获取到原图地址");// 基本上不会走这里
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlCertification:
                if (!CertificationHelper.isAuth(mContext)) {
                    CertificationHelper.startCloudReal(mContext, coreManager, this::updateCertificationStatus);
                } else {
                    CertificationHelper.askCertificationCancel(mContext, coreManager, this::updateCertificationStatus);
                }
                break;
            case R.id.rlLicense:
                showSelectLicenseDialog();
                break;
            case R.id.rl_avatar:
            case R.id.avatar_img:
                showSelectAvatarDialog();
                break;
            case R.id.sex_select_rl:
                showSelectSexDialog();
                break;
            case R.id.birthday_select_rl:
                showSelectBirthdayDialog();
                break;
            case R.id.city_select_rl:
                Intent intent = new Intent(BasicInfoEditActivity.this, SelectAreaActivity.class);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_TYPE, Area.AREA_TYPE_PROVINCE);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_PARENT_ID, Area.AREA_DATA_CHINA_ID);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_DEEP, Area.AREA_TYPE_CITY);
                startActivityForResult(intent, 4);
                break;
            case R.id.rl_desc:
                startActivity(new Intent(BasicInfoEditActivity.this, DescriptionActivity.class));
                break;
            case R.id.qccodeforchat:
                Intent intent2 = new Intent(BasicInfoEditActivity.this, QRcodeActivity.class);
                intent2.putExtra("isgroup", false);
                if (!TextUtils.isEmpty(mUser.getAccount())) {
                    intent2.putExtra("userid", mUser.getAccount());
                } else {
                    intent2.putExtra("userid", mUser.getUserId());
                }
                intent2.putExtra("userAvatar", mUser.getUserId());
                intent2.putExtra("nickName", mUser.getNickName());
                intent2.putExtra("sex", mUser.getSex());
                startActivity(intent2);
                break;
            case R.id.next_step_btn:
                next();
                break;
        }
    }

    private void showSelectLicenseDialog() {
        mPictureType = PictureType.LICENSE;
        SelectImageDialog selectImageDialog = new SelectImageDialog(this, new SelectImageDialog.OnSelectImageItemListener() {
            @Override
            public void tvTakePhotoClick() {
                takePhoto();
            }

            @Override
            public void tvSelectPhotoClick() {
                selectPhoto();
            }
        });
        selectImageDialog.show();
/*
        String[] items = new String[]{getString(R.string.photograph), getString(R.string.album)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("选择营业执照")
                .setSingleChoiceItems(items, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    takePhoto();
                                } else {
                                    selectPhoto();
                                }
                                dialog.dismiss();
                            }
                        });
        builder.show();
*/
    }

    private void showSelectAvatarDialog() {
        mPictureType = PictureType.AVATAR;
        SelectImageDialog selectImageDialog = new SelectImageDialog(this, new SelectImageDialog.OnSelectImageItemListener() {
            @Override
            public void tvTakePhotoClick() {
                takePhoto();
            }

            @Override
            public void tvSelectPhotoClick() {
                selectPhoto();
            }
        });
        selectImageDialog.show();
/*
        String[] items = new String[]{getString(R.string.photograph), getString(R.string.album)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(getString(R.string.select_avatars))
                .setSingleChoiceItems(items, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    takePhoto();
                                } else {
                                    selectPhoto();
                                }
                                dialog.dismiss();
                            }
                        });
        builder.show();
*/
    }

    private void takePhoto() {
        mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_CROP_PHOTO);
    }

    private void selectPhoto() {
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_CROP_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE_CROP_PHOTO) {// 拍照返回再去裁减
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    if (mPictureType == PictureType.LICENSE) {
                        uploadLicense(new File(mNewPhotoUri.getPath()));
                        return;
                    }
                    Uri o = mNewPhotoUri;
                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 1000, 1000);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_CROP_PHOTO) {// 选择一张图片,然后立即调用裁减
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (mPictureType == PictureType.LICENSE) {
                        uploadLicense(new File(CameraUtil.parsePickImageResult(data)));
                        return;
                    }
                    Uri o = Uri.fromFile(new File(CameraUtil.parsePickImageResult(data)));
                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 1000, 1000);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_CROP_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    mCurrentFile = new File(mNewPhotoUri.getPath());
                    switch (mPictureType) {
                        case AVATAR:
                            AvatarHelper.getInstance().displayUrl(mNewPhotoUri.toString(), mAvatarImg);
                            // 上传头像
                            uploadAvatar(mCurrentFile);
                            break;
                        case LICENSE:
                            uploadLicense(mCurrentFile);
                            break;
                    }
                } else {
                    ToastUtil.showToast(this, R.string.c_crop_failed);
                }
            }
        } else if (requestCode == 4) {// 选择城市
            if (resultCode == RESULT_OK && data != null) {
                int countryId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTRY_ID, 0);
                int provinceId = data.getIntExtra(SelectAreaActivity.EXTRA_PROVINCE_ID, 0);
                int cityId = data.getIntExtra(SelectAreaActivity.EXTRA_CITY_ID, 0);
                int countyId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTY_ID, 0);

                String province_name = data.getStringExtra(SelectAreaActivity.EXTRA_PROVINCE_NAME);
                String city_name = data.getStringExtra(SelectAreaActivity.EXTRA_CITY_NAME);
                /*String county_name = data.getStringExtra(SelectAreaActivity.EXTRA_COUNTY_ID);*/
                mCityTv.setText(province_name + "-" + city_name);

                mTempData.setCountryId(countryId);
                mTempData.setProvinceId(provinceId);
                mTempData.setCityId(cityId);
                mTempData.setAreaId(countyId);
            }
        } else if (requestCode == REQUEST_CODE_SET_ACCOUNT) {
            if (resultCode == RESULT_OK && data != null) {
                String account = data.getStringExtra(AppConstant.EXTRA_USER_ACCOUNT);
                mTempData.setAccount(account);
                mTempData.setSetAccountCount(1);
                initAccount();
            }
        }
    }

    private void uploadLicense(File file) {
        Objects.requireNonNull(file);
        String url;
        try {
            FileInputStream in = new FileInputStream(file);
            url = ByteString.read(in, (int) file.length()).base64();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Map<String, String> params = new HashMap<>();
        params.put("imageCoding", url);
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.post().url(coreManager.getConfig().GET_BUSINESS_LICENSE_INFO)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        DialogHelper.tipDialog(mContext, JSON.toJSONString(result));
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        DialogHelper.tipDialog(mContext, JSON.toJSONString(e.getMessage()));
                    }
                });
    }

    private void uploadAvatar(File file) {
        if (!file.exists()) {
            // 文件不存在
            return;
        }
        // 显示正在上传的ProgressDialog
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        final String loginUserId = coreManager.getSelf().getUserId();
        params.put("userId", loginUserId);

        HttpUtils.post().url(coreManager.getConfig().AVATAR_UPLOAD_URL)
                .params(params)
                .params("files", file)
                .build(false)
                .execute(new FileCallback<Result>(Result.class) {
                    @Override
                    public void onResponse(Result result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_success);
                            AvatarHelper.getInstance().updateAvatar(loginUserId);// 更新时间
                            EventBus.getDefault().post(new EventAvatarUploadSuccess(true));
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_failed);
                    }
                });
    }

    private void showSelectSexDialog() {
        String[] sexs = new String[]{getString(R.string.sex_man), getString(R.string.sex_woman)};
        new AlertDialog.Builder(this).setTitle(getString(R.string.gender_selection))
                .setSingleChoiceItems(sexs, mTempData.getSex() == 1 ? 0 : 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            mTempData.setSex(1);
                            mSexTv.setText(getString(R.string.sex_man));
                        } else {
                            mTempData.setSex(0);
                            mSexTv.setText(getString(R.string.sex_woman));
                        }
                        dialog.dismiss();
                    }
                }).setCancelable(true).create().show();
    }

    @SuppressWarnings("deprecation")
    private void showSelectBirthdayDialog() {
        Date date = new Date(mTempData.getBirthday() * 1000);
        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                mTempData.setBirthday(TimeUtils.getSpecialBeginTime(mBirthdayTv, calendar.getTime().getTime() / 1000));
                long currentTime = System.currentTimeMillis() / 1000;
                long birthdayTime = calendar.getTime().getTime() / 1000;
                if (birthdayTime > currentTime) {
                    ToastUtil.showToast(mContext, R.string.data_of_birth);
                }
            }
        }, date.getYear() + 1900, date.getMonth(), date.getDate());
        dialog.show();
    }

    private void loadPageData() {
        mTempData.setNickName(mNameEdit.getText().toString().trim());
    }

    private void next() {
        loadPageData();

        if (TextUtils.isEmpty(mTempData.getNickName())) {
            mNameEdit.requestFocus();
            mNameEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.name_empty_error));
            return;
        }
//
//        if (!coreManager.getConfig().disableLocationServer) {
//            if (mTempData.getCityId() <= 0) {
//                ToastUtil.showToast(this, getString(R.string.live_address_empty_error));
//                return;
//            }
//        }

        if (mUser != null && !mUser.equals(mTempData)) {// 数据改变了，提交数据
            updateData();
        } else {
            finish();
        }
    }

    private void updateData() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        if (!mUser.getNickName().equals(mTempData.getNickName())) {
            params.put("nickname", mTempData.getNickName());
        }
        if (mUser.getSex() != mTempData.getSex()) {
            params.put("sex", String.valueOf(mTempData.getSex()));
        }
        if (mUser.getBirthday() != mTempData.getBirthday()) {
            params.put("birthday", String.valueOf(mTempData.getBirthday()));
        }
        if (mUser.getCountryId() != mTempData.getCountryId()) {
            params.put("countryId", String.valueOf(mTempData.getCountryId()));
        }
        if (mUser.getProvinceId() != mTempData.getProvinceId()) {
            params.put("provinceId", String.valueOf(mTempData.getProvinceId()));
        }
        if (mUser.getCityId() != mTempData.getCityId()) {
            params.put("cityId", String.valueOf(mTempData.getCityId()));
        }
        if (mUser.getAreaId() != mTempData.getAreaId()) {
            params.put("areaId", String.valueOf(mTempData.getAreaId()));
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, getString(R.string.update_success));
                            saveData();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(BasicInfoEditActivity.this);
                    }
                });
    }

    private void saveData() {
        if (!mUser.getNickName().equals(mTempData.getNickName())) {
            coreManager.getSelf().setNickName(mTempData.getNickName());
            UserDao.getInstance().updateNickName(mTempData.getUserId(), mTempData.getNickName());     // 更新数据库
        }
        if (mUser.getSex() != mTempData.getSex()) {
            coreManager.getSelf().setSex(mTempData.getSex());
            UserDao.getInstance().updateSex(mTempData.getUserId(), mTempData.getSex() + "");          // 更新数据库
        }
        if (mUser.getBirthday() != mTempData.getBirthday()) {
            coreManager.getSelf().setBirthday(mTempData.getBirthday());
            UserDao.getInstance().updateBirthday(mTempData.getUserId(), mTempData.getBirthday() + "");// 更新数据库
        }

        if (mUser.getCountryId() != mTempData.getCountryId()) {
            coreManager.getSelf().setCountryId(mTempData.getCountryId());
            UserDao.getInstance().updateCountryId(mTempData.getUserId(), mTempData.getCountryId());
        }
        if (mUser.getProvinceId() != mTempData.getProvinceId()) {
            coreManager.getSelf().setProvinceId(mTempData.getProvinceId());
            UserDao.getInstance().updateProvinceId(mTempData.getUserId(), mTempData.getProvinceId());
        }
        if (mUser.getCityId() != mTempData.getCityId()) {
            coreManager.getSelf().setCityId(mTempData.getCityId());
            UserDao.getInstance().updateCityId(mTempData.getUserId(), mTempData.getCityId());
        }
        if (mUser.getAreaId() != mTempData.getAreaId()) {
            coreManager.getSelf().setAreaId(mTempData.getAreaId());
            UserDao.getInstance().updateAreaId(mTempData.getUserId(), mTempData.getAreaId());
        }

        setResult(RESULT_OK);
        finish();
    }

    enum PictureType {
        AVATAR, LICENSE
    }
}
