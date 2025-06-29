package com.tongxin.caihong.ui.systemshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Area;
import com.tongxin.caihong.bean.UploadFileResult;
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
import com.tongxin.caihong.ui.tool.HtmlFactory;
import com.tongxin.caihong.ui.tool.MultiImagePreviewActivity;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DeviceInfoUtil;
import com.tongxin.caihong.util.HttpUtil;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.video.EasyCameraActivity;
import com.tongxin.caihong.video.MessageEventGpu;
import com.tongxin.caihong.video.VideoRecorderActivity;
import com.tongxin.caihong.view.SelectImageDialog;
import com.tongxin.caihong.view.SquareCenterFrameLayout;
import com.tongxin.caihong.view.SwitchButton;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.view.photopicker.PhotoPickerActivity;
import com.tongxin.caihong.view.photopicker.SelectModel;
import com.tongxin.caihong.view.photopicker.intent.PhotoPickerIntent;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;


/**
 * 发布文字 || 图片
 */
public class ShareShuoshuoActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;  // 拍照
    private static final int REQUEST_CODE_PICK_PHOTO = 2;     // 图库
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // 位置
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // 谁可以看
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // 提醒谁看
    private static final int REQUEST_CODE_SELECT_TAG = 7; // 选择标签
    private final int mType = 1;
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
    private ArrayList<String> mPhotoList;
    private String mImageData;
    // 部分可见 || 不给谁看 有值 用于恢复谁可以看的界面
    private String str1;
    private String str2;
    private String str3;
    // 拍照和图库，获得图片的Uri
    private Uri mNewPhotoUri;
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
    private String mShareContent;
    private SwitchButton checkBox;
    private PostArticleImgAdapter postArticleImgAdapter;
    private ItemTouchHelper itemTouchHelper;
    private RecyclerView rcvImg;
    private TextView tv;
    private TextView tv_title_right;
    private String subject;

    public static void start(Context ctx, Intent intent) {
        intent.setClass(ctx, ShareShuoshuoActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_shuoshuo);
        mPhotoList = new ArrayList<>();
        postArticleImgAdapter = new PostArticleImgAdapter(this, mPhotoList);
        initActionBar();
        initView();
        initEvent();
        EventBus.getDefault().register(this);

        String text = ShareUtil.parseText(getIntent());
        subject = ShareUtil.parseSubject(getIntent());
        if (TextUtils.isEmpty(text) && !TextUtils.isEmpty(subject)) {
            text = subject;
        }
        if (!TextUtils.isEmpty(text)) {
            mTextEdit.setText(text + " ");
            mTextEdit.setSelection(text.length() + 1);
        }
        String image = ShareUtil.getFilePathFromStream(this, getIntent());
        if (!TextUtils.isEmpty(image) && new File(image).exists()) {
            ArrayList<String> list = new ArrayList<>(1);
            list.add(image);
            album(list, true);
        }
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
            // ToastUtil.showToast(mContext, getString(R.string.tip_auto_location_failed) + t.getMessage());
            latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
            longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.send_image_text);
        tv_title_right = (TextView) findViewById(R.id.tv_title_right);
        tv_title_right.setText(getResources().getString(R.string.circle_release));
        tv_title_right.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tv_title_right, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tv_title_right.setTextColor(getResources().getColor(R.color.white));
        tv_title_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoList.size() <= 0) {
                    // 发布文字
                    sendShuoshuo();
                } else {
                    // 发布图片+文字
                    new UploadTask().execute();
                }
            }
        });
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
        if (getIntent() != null) { // WebView内分享进来的
            mShareContent = getIntent().getStringExtra(Constants.BROWSER_SHARE_MOMENTS_CONTENT);
            if (!TextUtils.isEmpty(mShareContent)) {
                mTextEdit.setText(mShareContent + " ");
                mTextEdit.setSelection(mShareContent.length() + 1);
            }
        }

        rcvImg = (RecyclerView) findViewById(R.id.rcv_img);

        // 所在位置
        mTVLocation = (TextView) findViewById(R.id.tv_location);
        // 谁可以看
        mTVSee = (TextView) findViewById(R.id.tv_see);
        // 提醒谁看
        mTVAt = (TextView) findViewById(R.id.tv_at);
        // 标签
        mTvLabel = (TextView) findViewById(R.id.tv_label);
        mTvLabel.setText(mCurrTag);

        tv = findViewById(R.id.tv);
        initRcv();
    }

    private void initRcv() {
        rcvImg.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        rcvImg.setAdapter(postArticleImgAdapter);
        MyCallBack myCallBack = new MyCallBack(tv, postArticleImgAdapter, mPhotoList, null);
        itemTouchHelper = new ItemTouchHelper(myCallBack);
        itemTouchHelper.attachToRecyclerView(rcvImg);//绑定RecyclerView

        myCallBack.setDragListener(new DragListener() {
            @Override
            public void deleteState(boolean delete) {
                if (delete) {
                    tv.setBackgroundResource(R.color.holo_red_dark);
                    tv.setText(getResources().getString(R.string.post_delete_tv_s));
                } else {
                    tv.setText(getResources().getString(R.string.post_delete_tv_d));
                    tv.setBackgroundResource(R.color.holo_red_light);
                }
            }

            @Override
            public void dragState(boolean start) {
                if (start) {
                    tv.setVisibility(View.VISIBLE);
                } else {
                    tv.setVisibility(View.GONE);
                }
            }

            @Override
            public void clearView() {

            }
        });
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
                    ToastUtil.showToast(mContext, R.string.tip_private_cannot_use_this);
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

    private void showSelectPictureDialog() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setSingleChoiceItems(items, 0,
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

    private void showPictureActionDialog(final int position) {
        String[] items = new String[]{getString(R.string.look_over), getString(R.string.delete)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(getString(R.string.image))
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            previewImage(position);
                        } else {
                            // 删除
                            deletePhoto(position);
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void previewImage(int position) {
        // 查看
        Intent intent = new Intent(mContext, MultiImagePreviewActivity.class);
        intent.putExtra(AppConstant.EXTRA_IMAGES, mPhotoList);
        intent.putExtra(AppConstant.EXTRA_POSITION, position);
        intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
        startActivity(intent);
    }

    private void deletePhoto(final int position) {
        mPhotoList.remove(position);
        postArticleImgAdapter.notifyDataSetChanged();
    }

    // 拍照
    private void takePhoto() {
        EasyCameraActivity.startForResult(this, REQUEST_CODE_CAPTURE_PHOTO);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }

    // 默认经纬度、地址
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

    /**
     * 相册
     * 可以多选的图片选择器
     */
    private void selectPhoto() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(mContext);
        intent.setSelectModel(SelectModel.MULTI);
        // 是否显示拍照， 默认false
        intent.setShowCarema(false);
        // 最多选择照片数量，默认为9
        intent.setMaxTotal(9 - mPhotoList.size());
        // 已选中的照片地址， 用于回显选中状态
        intent.setSelectedPaths(imagePaths);
        // intent.setImageConfig(config);
        // 是否加载视频，默认true
        intent.setLoadVideo(false);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                // 拍照返回
                if (VideoRecorderActivity.checkIsImage(data)) {
                    helloEventBus(VideoRecorderActivity.parseImageResult(data));
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            // 选择图片返回
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                    album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
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

    // 单张图片压缩 拍照
    private void photograph(final File file) {
        Log.e("zq", "压缩前图片路径:" + file.getPath() + "压缩前图片大小:" + file.length() / 1024 + "KB");
        // 拍照出来的图片Luban一定支持，
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // 原图小于100kb 不压缩
                // .putGear(2)     // 设定压缩档次，默认三挡
                // .setTargetDir() // 指定压缩后的图片路径
                .setCompressListener(new OnCompressListener() { //设置回调
                    @Override
                    public void onStart() {
                        Log.e("zq", "开始压缩");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.e("zq", "压缩成功，压缩后图片位置:" + file.getPath() + "压缩后图片大小:" + file.length() / 1024 + "KB");
                        mPhotoList.add(file.getPath());
                        postArticleImgAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("zq", "压缩失败,原图上传");
                        mPhotoList.add(file.getPath());
                        postArticleImgAdapter.notifyDataSetChanged();
                    }
                }).launch();// 启动压缩
    }

    // 多张图片压缩 相册
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// 原图发送，不压缩
            Log.e("zq", "原图上传，不压缩，选择原文件路径");
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                mPhotoList.add(stringArrayListExtra.get(i));
                postArticleImgAdapter.notifyDataSetChanged();
            }
            return;
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            // Luban只处理特定后缀的图片，不满足的不处理也不走回调，
            // 只能挑出来不压缩，
            // todo luban支持压缩.gif图，但是压缩之后的.gif图用glide加载与转换为gifDrawable都会出问题，所以,gif图不压缩了
            List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp");
            boolean support = false;
            for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                    support = true;
                    break;
                }
            }
            if (!support) {
                list.add(stringArrayListExtra.get(i));
            }
        }

        if (list.size() > 0) {
            // 不压缩的部分，直接发送
            mPhotoList.addAll(list);
            postArticleImgAdapter.notifyDataSetChanged();
        }

        // 移除掉不压缩的图片
        // 这里不能移除mPhotoList避免移除之前的选择，
        stringArrayListExtra.removeAll(list);

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// 原图小于100kb 不压缩
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        mPhotoList.add(file.getPath());
                        postArticleImgAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// 启动压缩
    }

    // 发布一条说说
    public void sendShuoshuo() {
        Map<String, String> params = new HashMap<>();
        sendShuoshuo(params);
    }

    public void sendShuoshuo(Map<String, String> params) {
        if (TextUtils.isEmpty(mTextEdit.getText().toString().trim()) && mPhotoList.size() == 0) {
            DialogHelper.tip(mContext, getString(R.string.leave_select_image_or_edit_text));
            return;
        }
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
        List<String> urlList = HttpUtil.getURLList(mTextEdit.getText().toString());
        if (!TextUtils.isEmpty(mImageData)) {
            params.put("type", "2");
        } else if (!urlList.isEmpty()) {
            String url = urlList.get(0);
            // 判断params避免重复解析了，
            if (!params.containsKey("type")) {
                DialogHelper.showMessageProgressDialog(mContext, getString(R.string.tip_now_parse_link));
                HtmlFactory.instance().queryImage(url, new HtmlFactory.DataListener<String>() {
                    @Override
                    public void onResponse(List<String> datas, String title) {
                        DialogHelper.dismissProgressDialog();
                        if (TextUtils.isEmpty(title) && !TextUtils.isEmpty(subject)) {
                            title = subject;
                        } else {
                            subject = title;
                        }
                        params.put("type", "6");
                        if (!datas.isEmpty()) {
                            params.put("sdkIcon", datas.get(datas.size() - 1));// 参数名为sdkIcon，实际传的为链接内的图片url
                        }
                        params.put("sdkTitle", title);
                        params.put("sdkUrl", url);
                        sendShuoshuo(params);
                    }

                    @Override
                    public void onError(String err) {
                        DialogHelper.dismissProgressDialog();
                        params.put("type", "1");
                        sendShuoshuo(params);
                    }
                });
                return;
            }
        } else {
            params.put("type", "1");
        }
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
        String text;
        if (!params.containsKey("sdkUrl")) {
            text = SendTextFilter.filter(mTextEdit.getText().toString());
        } else {
            text = mTextEdit.getText().toString();
            String url = params.get("sdkUrl");
            text = text.replace(url, "");
            text = SendTextFilter.filter(text);
            if (TextUtils.isEmpty(text.trim())
                    && !TextUtils.isEmpty(subject)) {
                // 把链接标题作为动态中的文字，
                text = subject;
            }
        }
        params.put("text", text);
        if (!TextUtils.isEmpty(mImageData)) {
            // 图片
            params.put("images", mImageData);
        }

        params.put("isAllowComment", isBoolBan ? String.valueOf(1) : String.valueOf(0));
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
            // 城市id
            params.put("cityId", String.valueOf(area.getId()));
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

        DialogHelper.showDefaulteMessageProgressDialog(this);

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

    interface DragListener {
        /**
         * 用户是否将 item拖动到删除处，根据状态改变颜色
         *
         * @param delete
         */
        void deleteState(boolean delete);

        /**
         * 是否于拖拽状态
         *
         * @param start
         */
        void dragState(boolean start);

        /**
         * 当用户与item的交互结束并且item也完成了动画时调用
         */
        void clearView();
    }

    public static class MyCallBack extends ItemTouchHelper.Callback {

        private int dragFlags;
        private int swipeFlags;
        private View removeView;
        private PostArticleImgAdapter adapter;
        private List<String> images;//图片经过压缩处理
        private List<String> originImages;//图片没有经过处理，这里传这个进来是为了使原图片的顺序与拖拽顺序保持一致
        private boolean up;//手指抬起标记位
        private DragListener dragListener;

        public MyCallBack(View tv, PostArticleImgAdapter adapter, List<String> images, List<String> originImages) {
            removeView = tv;
            this.adapter = adapter;
            this.images = images;
            this.originImages = originImages;
        }

        /**
         * 设置item是否处理拖拽事件和滑动事件，以及拖拽和滑动操作的方向
         *
         * @param recyclerView
         * @param viewHolder
         * @return
         */
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            //判断 recyclerView的布局管理器数据
            if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {//设置能拖拽的方向
                dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                swipeFlags = 0;//0则不响应事件
            }
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        /**
         * 当用户从item原来的位置拖动可以拖动的item到新位置的过程中调用
         *
         * @param recyclerView
         * @param viewHolder
         * @param target
         * @return
         */
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();//得到item原来的position
            int toPosition = target.getAdapterPosition();//得到目标position
            if (toPosition == images.size() || images.size() == fromPosition) {
                return true;
            }
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(images, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(images, i, i - 1);
                }
            }
            adapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        /**
         * 设置是否支持长按拖拽
         *
         * @return
         */
        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        /**
         * @param viewHolder
         * @param direction
         */
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        /**
         * 当用户与item的交互结束并且item也完成了动画时调用
         *
         * @param recyclerView
         * @param viewHolder
         */
        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            adapter.notifyDataSetChanged();
            initData();
            if (dragListener != null) {
                dragListener.clearView();
            }
        }

        /**
         * 重置
         */
        private void initData() {
            if (dragListener != null) {
                dragListener.deleteState(false);
                dragListener.dragState(false);
            }
            up = false;
        }

        /**
         * 自定义拖动与滑动交互
         *
         * @param c
         * @param recyclerView
         * @param viewHolder
         * @param dX
         * @param dY
         * @param actionState
         * @param isCurrentlyActive
         */
        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (null == dragListener) {
                return;
            }
            int[] location = new int[2];
            viewHolder.itemView.getLocationInWindow(location); //获取在当前窗口内的绝对坐标
            if (location[1] + viewHolder.itemView.getHeight() > removeView.getTop()) {//拖到删除处
                dragListener.deleteState(true);
                if (up) {//在删除处放手，则删除item
                    viewHolder.itemView.setVisibility(View.INVISIBLE);//先设置不可见，如果不设置的话，会看到viewHolder返回到原位置时才消失，因为remove会在viewHolder动画执行完成后才将viewHolder删除
                    images.remove(viewHolder.getAdapterPosition());
                    adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    initData();
                    return;
                }
            } else {//没有到删除处
                if (View.INVISIBLE == viewHolder.itemView.getVisibility()) {//如果viewHolder不可见，则表示用户放手，重置删除区域状态
                    dragListener.dragState(false);
                }
                dragListener.deleteState(false);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        /**
         * 当长按选中item的时候（拖拽开始的时候）调用
         *
         * @param viewHolder
         * @param actionState
         */
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (ItemTouchHelper.ACTION_STATE_DRAG == actionState && dragListener != null) {
                dragListener.dragState(true);
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        /**
         * 设置手指离开后ViewHolder的动画时间，在用户手指离开后调用
         *
         * @param recyclerView
         * @param animationType
         * @param animateDx
         * @param animateDy
         * @return
         */
        @Override
        public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            //手指放开
            up = true;
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
        }

        void setDragListener(DragListener dragListener) {
            this.dragListener = dragListener;
        }
    }

    private class UploadTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
        }

        /**
         * 上传的结果： <br/>
         * return 1 Token过期，请重新登陆 <br/>
         * return 2 上传出错<br/>
         * return 3 上传成功<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }


            String result = new UploadService().uploadFile(mPhotoList);
            if (TextUtils.isEmpty(result)) {
                return 2;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(mContext, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {
                    // 上传丢失了某些文件
                    return 2;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    } else {
                        return 2;
                    }
                    return 3;
                } else {
                    // 没有文件数据源，失败
                    return 2;
                }
            } else {
                return 2;
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
                ToastUtil.showToast(mContext, getString(R.string.upload_failed));
            } else {
                sendShuoshuo();
            }
        }
    }

    class PostArticleImgAdapter extends RecyclerView.Adapter<PostArticleImgAdapter.MyViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<String> mDatas;

        public PostArticleImgAdapter(Context context, List<String> datas) {
            this.mDatas = datas;
            this.mContext = context;
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(mLayoutInflater.inflate(R.layout.item_post_activity, parent, false));
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            if (getItemViewType(position) == 0) { // 普通的视图
                holder.squareCenterFrameLayout.setVisibility(View.GONE);
                ImageLoadHelper.showImageWithSizeError(
                        mContext,
                        mDatas.get(position),
                        R.drawable.pic_error,
                        150, 150,
                        holder.imageView
                );
                holder.ivRemove.setVisibility(View.VISIBLE);
                holder.ivRemove.setOnClickListener(v -> {
                    deletePhoto(holder.getAdapterPosition());
                });
                holder.itemView.setOnClickListener(v -> {
                    previewImage(holder.getAdapterPosition());
                });
                holder.itemView.setOnLongClickListener(v -> {
                    itemTouchHelper.startDrag(holder);
                    return true;
                });
            } else {
                holder.squareCenterFrameLayout.setVisibility(View.VISIBLE);
                holder.ivRemove.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> {
                    showSelectPictureDialog();
                });
                holder.itemView.setOnLongClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            if (mDatas.size() >= 9) {
                return 9;
            }
            return mDatas.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (mDatas.size() == 0) {
                // View Type 1代表添加更多的视图
                return 1;
            } else if (mDatas.size() < 9) {
                if (position < mDatas.size()) {
                    // View Type 0代表普通的ImageView视图
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            SquareCenterFrameLayout squareCenterFrameLayout;
            View ivRemove;

            MyViewHolder(View itemView) {
                super(itemView);
                squareCenterFrameLayout = itemView.findViewById(R.id.add_sc);
                imageView = itemView.findViewById(R.id.sdv);
                ivRemove = itemView.findViewById(R.id.ivRemove);
            }
        }
    }

}
