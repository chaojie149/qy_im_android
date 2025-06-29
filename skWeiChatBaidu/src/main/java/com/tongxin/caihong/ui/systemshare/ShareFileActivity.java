package com.tongxin.caihong.ui.systemshare;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.UploadFileResult;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.UploadService;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.ui.account.LoginHistoryActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.range.AtSeeCircleActivity;
import com.tongxin.caihong.ui.circle.range.SeeCircleActivity;
import com.tongxin.caihong.ui.circle.range.SquareTagPickerActivity;
import com.tongxin.caihong.ui.circle.util.SendTextFilter;
import com.tongxin.caihong.ui.map.MapPickerActivity;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.LogUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.log.FileUtils;
import com.tongxin.caihong.view.SelectFileActivity;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.SwitchButton;
import com.tongxin.caihong.view.TipDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 发布文件
 */
public class ShareFileActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // 位置
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // 谁可以看
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // 提醒谁看
    private static final int REQUEST_CODE_SELECT_TAG = 7; // 选择标签
    private boolean isBoolBan = false;
    private EditText mTextEdit;
    // 所在位置
    private TextView mTVLocation;
    // 谁可以看
    private TextView mTVSee;
    // 提醒谁看
    private TextView mTVAt;
    // 标签
    private TextView mTvLabel;
    private String mCurrTag;
    // File item
    private ImageView mAddFileIv;
    private RelativeLayout mSendFileRl;
    private ImageView mFileIv;
    private TextView mFileNameTv;
    // data
    private String mFilePath;
    private SelectionFrame mSelectionFrame;
    // 部分可见 || 不给谁看 有值 用于恢复谁可以看的界面
    private String str1;
    private String str2;
    private String str3;
    // 默认为公开
    private int visible = 5;
    // 谁可以看 || 不给谁看
    private String lookPeople;
    // 提醒谁看
    private String atlookPeople;
    // 默认不发位置
    private double latitude;
    private double longitude;
    private String address;
    private String mFileData;
    private SwitchButton checkBox;
    // Video Item
    private FrameLayout mFloatLayout;
    private ImageView mImageView;
    private ImageView mIconImageView;
    private TextView mVideoTextTv;

    public static void start(Context ctx, Intent intent) {
        intent.setClass(ctx, ShareFileActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        initActionBar();
        initView();
        initEvent();
        initLocation();
    }

    private void initLocation() {
        MapHelper mapHelper = MapHelper.getInstance();
        mapHelper.requestLatLng(latLng -> {
            latitude = latLng.getLatitude();
            longitude = latLng.getLongitude();
            mapHelper.requestPlaceList(latLng, places -> {
                if (places.isEmpty()) {
                    return;
                }
                MapHelper.Place place = places.get(0);
                address = place.getName();
                if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
                    Log.e("zq", "纬度:" + latitude + "   经度：" + longitude + "   位置：" + address);
                    mTVLocation.setText(address);
                }
            }, t -> ToastUtil.showToast(mContext, getString(R.string.tip_places_around_failed) + t.getMessage()));
        }, t -> {
            //      ToastUtil.showToast(mContext, getString(R.string.tip_auto_location_failed) + t.getMessage());
            latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
            longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isExitNoPublish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.public_a_file));
    }

    private void initView() {
        RelativeLayout rl_ban = findViewById(R.id.rl_ban);
        checkBox = findViewById(R.id.cb_ban);
        rl_ban.setOnClickListener(v -> {
            isBoolBan = !isBoolBan;
            checkBox.setChecked(isBoolBan);
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isBoolBan = isChecked;
        });
        mTextEdit = (EditText) findViewById(R.id.text_edit);
        // 解决EditText与ScrollView嵌套的问题
        mTextEdit.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        // 限制了EditText输入最大长度为600，到达限制时提示
        mTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mTextEdit.getText().toString().trim().length() >= 10000) {
                    Toast.makeText(mContext, getString(R.string.tip_edit_max_input_length, 10000), Toast.LENGTH_SHORT).show();
                }
            }
        });
        mTextEdit.setHint(getString(R.string.add_msg_mind));
        // 所在位置
        mTVLocation = (TextView) findViewById(R.id.tv_location);
        // 谁可以看
        mTVSee = (TextView) findViewById(R.id.tv_see);
        // 提醒谁看
        mTVAt = (TextView) findViewById(R.id.tv_at);
        // 标签
        mTvLabel = (TextView) findViewById(R.id.tv_label);
        mTvLabel.setText(mCurrTag);

        TextView tv_title_right = (TextView) findViewById(R.id.tv_title_right);
        tv_title_right.setText(getResources().getString(R.string.circle_release));
        tv_title_right.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tv_title_right, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tv_title_right.setTextColor(getResources().getColor(R.color.white));
        tv_title_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mFilePath)) {
                    Toast.makeText(mContext, getString(R.string.add_file), Toast.LENGTH_SHORT).show();
                    return;
                }
                new UploadTask().execute();
            }
        });
        mSendFileRl = findViewById(R.id.send_file_rl);
        mFileIv = findViewById(R.id.file_img);
        mFileNameTv = findViewById(R.id.file_name);

        mFloatLayout = findViewById(R.id.float_layout);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mIconImageView = (ImageView) findViewById(R.id.icon_image_view);
        mIconImageView.setBackgroundResource(R.mipmap.send_file);
        mVideoTextTv = (TextView) findViewById(R.id.text_tv);
        mVideoTextTv.setText(R.string.circle_select_file);
    }

    private void initEvent() {
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.rl_location).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rl_location).setOnClickListener(this);
        }
        findViewById(R.id.rl_see).setOnClickListener(this);
        findViewById(R.id.rl_at).setOnClickListener(this);
        findViewById(R.id.rl_label).setOnClickListener(this);

        mFloatLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectFileActivity.startSingleChoice(mContext, new SelectFileActivity.OptionFileListener() {
                    @Override
                    public void option(List<File> files) {
                        if (files != null && files.size() > 0) {
                            File file = files.get(0);// 最多选择一个文件，取第一个即可
                            mFilePath = file.getPath();

                            int index = mFilePath.lastIndexOf(".");
                            if (index != -1) {
                                String type = mFilePath.substring(index + 1).toLowerCase();
                                if (type.equals("png") || type.equals("jpg")) {
                                    ImageLoadHelper.showImageWithError(
                                            mContext,
                                            mFilePath,
                                            R.drawable.image_download_fail_icon,
                                            mFileIv
                                    );
                                } else {
                                    AvatarHelper.getInstance().fillFileView(type, mFileIv);
                                }
                            }
                            mFileNameTv.setText(file.getName());
                            mSendFileRl.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void intent() {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, FileUtils.REQUEST_CODE_SELECT_FILE);
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_location:
                // 所在位置
                Intent intent1 = new Intent(this, MapPickerActivity.class);
                startActivityForResult(intent1, REQUEST_CODE_SELECT_LOCATE);
                break;
            case R.id.rl_see:
                // 谁可以看
                Intent intent2 = new Intent(this, SeeCircleActivity.class);
                intent2.putExtra("THIS_CIRCLE_TYPE", visible);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER1", str1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER2", str2);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER3", str3);
                intent2.putExtra("THIS_CIRCLE_LABLE", mCurrTag);
                startActivityForResult(intent2, REQUEST_CODE_SELECT_TYPE);
                break;
            case R.id.rl_at:
                // 提醒谁看
                if (visible == 2) {
                    final TipDialog tipDialog = new TipDialog(this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_use_this), new TipDialog.ConfirmOnClickListener() {
                        @Override
                        public void confirm() {
                            tipDialog.dismiss();
                        }
                    });
                    tipDialog.show();
                } else {
                    Intent intent3 = new Intent(this, AtSeeCircleActivity.class);
                    intent3.putExtra("REMIND_TYPE", visible);
                    intent3.putExtra("REMIND_PERSON", lookPeople);
                    intent3.putExtra("REMIND_SELECT_PERSON", atlookPeople);
                    startActivityForResult(intent3, REQUEST_CODE_SELECT_REMIND);
                }
                break;
            case R.id.rl_label: // 选择标签
                Intent intent3 = new Intent(this, SquareTagPickerActivity.class);
                intent3.putExtra("THIS_CIRCLE_LABLE", mCurrTag);
                startActivityForResult(intent3, REQUEST_CODE_SELECT_TAG);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        isExitNoPublish();
    }

    private void isExitNoPublish() {
        if (!TextUtils.isEmpty(mFilePath)) {
            mSelectionFrame = new SelectionFrame(mContext);
            mSelectionFrame.setSomething(getString(R.string.app_name), getString(R.string.tip_has_file_no_public), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    finish();
                }
            });
            mSelectionFrame.show();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {

        } else if (resultCode == RESULT_OK && requestCode == FileUtils.REQUEST_CODE_SELECT_FILE) {
            mFilePath = FileUtils.getPath(mContext, data.getData());
            if (mFilePath == null) {
                ToastUtil.showToast(mContext, R.string.tip_file_not_supported);
                return;
            }

            int index = mFilePath.lastIndexOf(".");
            if (index != -1) {
                String type = mFilePath.substring(index + 1).toLowerCase();
                if (type.equals("png") || type.equals("jpg")) {
                    ImageLoadHelper.showImageWithError(
                            mContext,
                            mFilePath,
                            R.drawable.image_download_fail_icon,
                            mFileIv
                    );
                } else {
                    AvatarHelper.getInstance().fillFileView(type, mFileIv);
                }
            }

            File file = new File(mFilePath);
            mFileNameTv.setText(file.getName());
            mSendFileRl.setVisibility(View.VISIBLE);
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_LOCATE) {
            // 选择位置返回
            latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
            if (!TextUtils.isEmpty(address)
                    && TextUtils.equals(address, String.valueOf(-1))) {
                // 不显示位置
                address = "";
                mTVLocation.setText(getString(R.string.location));
                return;
            }
            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
                Log.e("zq", "纬度:" + latitude + "   经度：" + longitude + "   位置：" + address);
                mTVLocation.setText(address);
            } else {
                ToastUtil.showToast(mContext, getString(R.string.loc_startlocnotice));
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_TYPE) {
            // 谁可以看返回
            int mOldVisible = visible;
            visible = data.getIntExtra("THIS_CIRCLE_TYPE", 1);
            // 当前可见范围与上一次不一致时，情况提醒谁看列表
            if (mOldVisible != visible
                    || visible == 3 || visible == 4) {
                // 清空提醒谁看列表 3/4 为可选范围，可能和已经选择好的提醒谁看冲突
                atlookPeople = "";
                mTVAt.setText("");
            }
            if (visible == 5) {
                mTVSee.setText(R.string.publics);
                mCurrTag = data.getStringExtra("THIS_CIRCLE_LABLE");
                mTvLabel.setText(mCurrTag);
            } else if (visible == 1) {
                mTVSee.setText(R.string.see_circle_friend);
            } else if (visible == 2) {
                mTVSee.setText(R.string.privates);
                if (!TextUtils.isEmpty(atlookPeople)) {
                    final TipDialog tipDialog = new TipDialog(this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_notify), new TipDialog.ConfirmOnClickListener() {
                        @Override
                        public void confirm() {
                            tipDialog.dismiss();
                        }
                    });
                    tipDialog.show();
                }
            } else if (visible == 3) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String looKenName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText(looKenName);
            } else if (visible == 4) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String lookName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText(getString(R.string.not_allow, lookName));
            }
            str1 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER1");
            str2 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER2");
            str3 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER3");
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_REMIND) {
            // 提醒谁看返回
            atlookPeople = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON");
            String atLookPeopleName = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON_NAME");
            mTVAt.setText(atLookPeopleName);
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_TAG) {
            // 选择标签
            mCurrTag = data.getStringExtra("THIS_CIRCLE_LABLE");
            mTvLabel.setText(mCurrTag);
        }
    }

    public void sendFile() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息 5=文件消息；
        params.put("type", "5");
        // 消息标记：1：求职消息；2：招聘消息；3：普通消息；
        params.put("flag", "3");

        // 消息隐私范围：1=好友；2=私密；3=部分选中好友可见；4=不给谁看; 5=公开
        params.put("visible", String.valueOf(visible));
        if (visible == 3) {
            // 谁可以看
            params.put("userLook", lookPeople);
        } else if (visible == 4) {
            // 不给谁看
            params.put("userNotLook", lookPeople);
        }
        // 提醒谁看
        if (!TextUtils.isEmpty(atlookPeople)) {
            params.put("userRemindLook", atlookPeople);
        }
        params.put("isAllowComment", isBoolBan ? String.valueOf(1) : String.valueOf(0));

        // 消息内容
        params.put("text", SendTextFilter.filter(mTextEdit.getText().toString()));
        params.put("files", mFileData);

        /**
         * 所在位置
         */
        if (!TextUtils.isEmpty(address)) {
            // 纬度
            params.put("latitude", String.valueOf(latitude));
            // 经度
            params.put("longitude", String.valueOf(longitude));
            // 位置
            params.put("location", address);
        }

        // 必传，之前删除该字段，发布说说，服务器返回接口内部异常
        Area area = Area.getDefaultCity();
        if (area != null) {
            params.put("cityId", String.valueOf(area.getId()));// 城市Id
        } else {
            params.put("cityId", "0");
        }

        /**
         * 附加信息
         */
        // 手机型号
        params.put("model", DeviceInfoUtil.getModel());
        // 手机操作系统版本号
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        if (!TextUtils.isEmpty(DeviceInfoUtil.getDeviceId(mContext))) {
            // 设备序列号
            params.put("serialNumber", DeviceInfoUtil.getDeviceId(mContext));
        }

        if (!TextUtils.isEmpty(mCurrTag)) {
            params.put("lables", String.valueOf(mCurrTag));
        }

        DialogHelper.showDefaulteMessageProgressDialog(mContext);

        HttpUtils.post().url(coreManager.getConfig().MSG_ADD_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        DialogHelper.dismissProgressDialog();
                        if (com.xuan.xuanhttplibrary.okhttp.result.Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, getString(R.string.tip_public_success));
                            Intent intent = new Intent();
                            intent.putExtra(AppConstant.EXTRA_MSG_ID, result.getData());
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private class UploadTask extends AsyncTask<Void, Integer, Integer> {
        private String message;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 视频为空，请重新录制 <br/>
         * return 3 上传出错<br/>
         * return 4 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            if (TextUtils.isEmpty(mFilePath)) {
                return 2;
            }

            List<String> dataList = new ArrayList<String>();
            dataList.add(mFilePath);

            String result = new UploadService().uploadFile(dataList);
            if (TextUtils.isEmpty(result)) {
                return 3;
            }

            LogUtils.log("上传文件<" + mFilePath + ">返回：" + result);
            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(mContext, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
                    return 3;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        data.getImages().get(0).setSize(new File(mFilePath).length());
                        mFileData = JSON.toJSONString(data.getImages());
                    } else if (data.getAudios() != null && data.getAudios().size() > 0) {
                        data.getAudios().get(0).setSize(new File(mFilePath).length());
                        mFileData = JSON.toJSONString(data.getAudios());
                    } else if (data.getVideos() != null && data.getVideos().size() > 0) {
                        data.getVideos().get(0).setSize(new File(mFilePath).length());
                        mFileData = JSON.toJSONString(data.getVideos());
                    } else if (data.getOthers() != null && data.getOthers().size() > 0) {
                        data.getOthers().get(0).setSize(new File(mFilePath).length());
                        mFileData = JSON.toJSONString(data.getOthers());
                    } else {
                        return 3;
                    }
                    return 4;
                } else {// 没有文件数据源，失败
                    return 3;
                }
            } else {
                return 3;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {
                DialogHelper.dismissProgressDialog();
                startActivity(new Intent(mContext, LoginHistoryActivity.class));
            } else if (result == 2) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(mContext, getString(R.string.alert_not_have_file));
            } else if (result == 3) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(mContext, R.string.upload_failed);
            } else {
                sendFile();
            }
        }
    }
}
