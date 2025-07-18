package com.tongxin.caihong.ui.circle.range;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
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
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.UploadFileResult;
import com.tongxin.caihong.bean.VideoFile;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.helper.UploadService;
import com.tongxin.caihong.map.MapHelper;
import com.tongxin.caihong.ui.account.LoginHistoryActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.circle.util.SendTextFilter;
import com.tongxin.caihong.ui.map.MapPickerActivity;
import com.tongxin.caihong.ui.me.LocalVideoActivity;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.CameraUtil;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.RecorderUtils;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.util.VideoCompressUtil;
import com.tongxin.caihong.video.PlayVideoActivity;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.SwitchButton;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.view.TrillReleaseDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Jni.VideoUitls;
import VideoHandle.OnEditorListener;
import okhttp3.Call;

/**
 * 发布视频
 */
public class SendVideoActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // 位置
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // 谁可以看
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // 提醒谁看
    private static final int REQUEST_CODE_SELECT_COVER = 6;  // 选择封面
    private static final int REQUEST_CODE_SELECT_TAG = 7; // 选择标签
    private boolean isBoolBan = false;
    private View tvSelectCover;
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
    // Video Item
    private FrameLayout mFloatLayout;
    private ImageView mImageView;
    private ImageView mIconImageView;
    private TextView mVideoTextTv;
    // data
    private int mSelectedId;
    private String mVideoFilePath;
    private String mThumbPath;
    private Bitmap mThumbBmp;
    private long mTimeLen;
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
    private String mVideoData;
    private String mImageData;
    private SwitchButton checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_video);
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
            //    ToastUtil.showToast(mContext, getString(R.string.tip_auto_location_failed) + t.getMessage());
            latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
            longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageView.setImageBitmap(null);
        mThumbBmp = null;
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
        tvTitle.setText(getString(R.string.send_video));
    }

    private void initView() {
        tvSelectCover = findViewById(R.id.tvSelectCover);
        RelativeLayout rl_ban = findViewById(R.id.rl_ban);
        checkBox = findViewById(R.id.cb_ban);
        rl_ban.setOnClickListener(v -> {
            isBoolBan = !isBoolBan;
            checkBox.setChecked(isBoolBan);
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isBoolBan = isChecked;
        });
        TextView tv_title_right = (TextView) findViewById(R.id.tv_title_right);
        tv_title_right.setText(getResources().getString(R.string.circle_release));
        tv_title_right.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tv_title_right, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tv_title_right.setTextColor(getResources().getColor(R.color.white));
        tv_title_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mVideoFilePath)) {
                    Toast.makeText(SendVideoActivity.this, getString(R.string.add_file), Toast.LENGTH_SHORT).show();
                    return;
                }
                compress(new File(mVideoFilePath));
            }
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

        mFloatLayout = findViewById(R.id.float_layout);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mIconImageView = (ImageView) findViewById(R.id.icon_image_view);
        mIconImageView.setBackgroundResource(R.drawable.send_video);
        mVideoTextTv = (TextView) findViewById(R.id.text_tv);
        mVideoTextTv.setText(R.string.circle_add_video);
    }

    private void sendVideo(File file) {
        new UploadTask().execute(file.getPath());
    }

    private void compress(File file) {
        String path = file.getPath();
        DialogHelper.showMessageProgressDialog(this, MyApplication.getContext().getString(R.string.compressed));
        final String out = RecorderUtils.getVideoFileByTime();
        String[] cmds = RecorderUtils.ffmpegComprerssCmd(path, out);
        long duration = VideoUitls.getDuration(path);

        VideoCompressUtil.exec(cmds, duration, new OnEditorListener() {
            public void onSuccess() {
                DialogHelper.dismissProgressDialog();
                File outFile = new File(out);
                runOnUiThread(() -> {
                    if (outFile.exists()) {
                        sendVideo(outFile);
                    } else {
                        sendVideo(file);
                    }
                });
            }

            public void onFailure() {
                DialogHelper.dismissProgressDialog();
                runOnUiThread(() -> {
                    sendVideo(file);
                });
            }

            public void onProgress(float progress) {

            }
        });
    }

    // 默认经纬度、地址
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

    private void initEvent() {
        tvSelectCover.setOnClickListener(v -> {
            SelectCoverActivity.start(this, REQUEST_CODE_SELECT_COVER, mVideoFilePath);
        });
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.rl_location).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rl_location).setOnClickListener(this);
        }
        findViewById(R.id.rl_see).setOnClickListener(this);
        findViewById(R.id.rl_at).setOnClickListener(this);
        findViewById(R.id.rl_label).setOnClickListener(this);

        mFloatLayout.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(mVideoFilePath)) {
                TrillReleaseDialog trillReleaseDialog = new TrillReleaseDialog(mContext, new TrillReleaseDialog.OnItemClickListener() {
                    @Override
                    public void onItem1Click() {
                        PlayVideoActivity.start(mContext, mVideoFilePath);
                    }

                    @Override
                    public void onItem2Click() {
                        startSelect();
                    }
                });
                trillReleaseDialog.show();
                return;
            }
            startSelect();

        });
    }

    private void startSelect() {
        Intent intent = new Intent(SendVideoActivity.this, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        // 这里选择视频不支持多选，
        intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, false);
        if (mSelectedId != 0) {
            intent.putExtra(AppConstant.EXTRA_SELECT_ID, mSelectedId);
        }
        startActivityForResult(intent, 1);
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
                    ToastUtil.showToast(SendVideoActivity.this, R.string.tip_private_cannot_use_this);
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
        if (!TextUtils.isEmpty(mVideoFilePath)) {
            mSelectionFrame = new SelectionFrame(SendVideoActivity.this);
            mSelectionFrame.setSomething(getString(R.string.app_name), getString(R.string.tip_has_video_no_public), new SelectionFrame.OnSelectionFrameClickListener() {
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
            // 选择视频的返回
            String json = data.getStringExtra(AppConstant.EXTRA_VIDEO_LIST);
            List<VideoFile> fileList = JSON.parseArray(json, VideoFile.class);
            if (fileList == null || fileList.size() == 0) {
                // 不可到达，列表里有做判断，
                Reporter.unreachable();
                return;
            }
            VideoFile videoFile = fileList.get(0);

            String filePath = videoFile.getFilePath();
            if (TextUtils.isEmpty(filePath)) {
                ToastUtil.showToast(this, R.string.select_failed);

                mVideoTextTv.setText(getString(R.string.add_msg_add_video));
                mIconImageView.setBackgroundResource(R.drawable.send_video);
                return;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                ToastUtil.showToast(this, R.string.select_failed);

                mVideoTextTv.setText(getString(R.string.add_msg_add_video));
                mIconImageView.setBackgroundResource(R.drawable.send_video);
                return;
            }
            // 返回成功，隐藏录制图标
            mVideoTextTv.setText("");
            mIconImageView.setBackground(null);

            tvSelectCover.setVisibility(View.VISIBLE);
            mVideoFilePath = filePath;
            mThumbBmp = AvatarHelper.getInstance().displayVideoThumb(filePath, mImageView);
            mTimeLen = videoFile.getFileLength();
            mSelectedId = videoFile.get_id();
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
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_COVER) {
            mThumbPath = SelectCoverActivity.parseResult(data);
            AvatarHelper.getInstance().displayUrl(mThumbPath, mImageView);
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_TAG) {
            // 选择标签
            mCurrTag = data.getStringExtra("THIS_CIRCLE_LABLE");
            mTvLabel.setText(mCurrTag);
        }
    }

    public void sendAudio() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
        params.put("type", "4");
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

        // 消息内容
        params.put("text", SendTextFilter.filter(mTextEdit.getText().toString()));
        params.put("videos", mVideoData);
        if (!TextUtils.isEmpty(mImageData) && !mImageData.equals("{}") && !mImageData.equals("[{}]")) {
            params.put("images", mImageData);
        }

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

        params.put("isAllowComment", isBoolBan ? String.valueOf(1) : String.valueOf(0));

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

        DialogHelper.showDefaulteMessageProgressDialog(SendVideoActivity.this);

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
                        ToastUtil.showErrorNet(SendVideoActivity.this);
                    }
                });
    }

    private class UploadTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DialogHelper.showDefaulteMessageProgressDialog(SendVideoActivity.this);
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 视频为空，请重新录制 <br/>
         * return 3 上传出错<br/>
         * return 4 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(String... params) {
            String mVideoFilePath = params[0];
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            if (TextUtils.isEmpty(mVideoFilePath)) {
                return 2;
            }

            // 保存视频缩略图至sd卡
            String imageSavePath;
            if (TextUtils.isEmpty(mThumbPath)) {
                imageSavePath = CameraUtil.getOutputMediaFileUri(SendVideoActivity.this, CameraUtil.MEDIA_TYPE_IMAGE).getPath();
                if (!BitmapUtil.saveBitmapToSDCard(mThumbBmp, imageSavePath)) {// 保存缩略图失败
                    return 3;
                }
            } else {
                imageSavePath = mThumbPath;
            }

            List<String> dataList = new ArrayList<String>();
            dataList.add(mVideoFilePath);
            if (!TextUtils.isEmpty(imageSavePath)) {
                dataList.add(imageSavePath);
            }
            String result = new UploadService().uploadFile(dataList);
            if (TextUtils.isEmpty(result)) {
                return 3;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(SendVideoActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
                    return 3;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getVideos() != null && data.getVideos().size() > 0) {
                        while (data.getVideos().size() > 1) {// 因为正确情况下只有一个视频，所以要保证只有一个视频
                            data.getVideos().remove(data.getVideos().size() - 1);
                        }
                        data.getVideos().get(0).setSize(new File(mVideoFilePath).length());
                        data.getVideos().get(0).setLength(mTimeLen);
                        // 记录本机上传，用于快速读取，
                        UploadCacheUtils.save(SendVideoActivity.this, data.getVideos().get(0).getOriginalUrl(), mVideoFilePath);
                        mVideoData = JSON.toJSONString(data.getVideos(), UploadFileResult.sAudioVideosFilter);
                    } else {
                        return 3;
                    }
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
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
                startActivity(new Intent(SendVideoActivity.this, LoginHistoryActivity.class));
            } else if (result == 2) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendVideoActivity.this, getString(R.string.alert_not_have_file));
            } else if (result == 3) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(SendVideoActivity.this, R.string.upload_failed);
            } else {
                sendAudio();
            }
        }
    }
}
