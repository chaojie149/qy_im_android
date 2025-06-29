package com.tongxin.caihong.ui.nearby;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.EventQRCodeReady;
import com.tongxin.caihong.ui.MainActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.contacts.ContactsActivity;
import com.tongxin.caihong.ui.contacts.PublishNumberActivity;
import com.tongxin.caihong.ui.groupchat.FaceToFaceGroup;
import com.tongxin.caihong.ui.me.NearPersonActivity;
import com.tongxin.caihong.ui.message.HandleQRCodeScanUtil;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DisplayUtil;
import com.tongxin.caihong.util.PermissionUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.SearchHeader;
import com.example.qrcode.Constant;
import com.example.qrcode.ScannerActivity;

import java.io.ByteArrayOutputStream;

/**
 * 添加好友
 */
public class FriendAddActivity extends BaseActivity implements View.OnClickListener {
    public static final int REQUEST_SCANNER = 888;
    private SearchHeader etSearch;
    private Button btnSearch;
    private TextView mNotifyCountTv2;
    private View llPanel;
    private boolean scannerRequesting;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, FriendAddActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);
        initActionBar();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.add_friend));
    }

    private void initView() {
        etSearch = findViewById(R.id.shSearch);
        btnSearch = findViewById(R.id.btnSearch);
        llPanel = findViewById(R.id.llPanel);

        TextView tvAccount = findViewById(R.id.tvAccount);
        tvAccount.setText(getString(R.string.my_account_place_holder, coreManager.getSelf().getAccount()));
        tvAccount.setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(coreManager.getSelf().getAccount());
            ToastUtil.showToast(mContext, R.string.copy_success);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    btnSearch.setVisibility(View.GONE);
                    llPanel.setVisibility(View.VISIBLE);
                } else {
                    btnSearch.setVisibility(View.VISIBLE);
                    llPanel.setVisibility(View.GONE);
                }
            }
        });

        mNotifyCountTv2 = (TextView) findViewById(R.id.num_tv2);
        findViewById(R.id.face_group_rl).setOnClickListener(this);
        findViewById(R.id.scan_rl).setOnClickListener(this);
        findViewById(R.id.contacts_rl).setOnClickListener(this);
        findViewById(R.id.nearby_rl).setOnClickListener(this);
        findViewById(R.id.notice_rl).setOnClickListener(this);
        findViewById(R.id.face_group_rl).setVisibility(View.GONE);
        findViewById(R.id.nearby_rl).setVisibility(View.GONE);

        ButtonColorChange.colorChange(this, btnSearch);
        btnSearch.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etSearch.getText().toString().trim())) {
                Intent intent = new Intent(mContext, UserListGatherActivity.class);
                intent.putExtra("key_word", etSearch.getText().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String mLoginUserId = coreManager.getSelf().getUserId();
        int mNewContactsNumber = PreferenceUtils.getInt(this, Constants.NEW_CONTACTS_NUMBER + mLoginUserId, 0);
        UiUtils.updateNum(mNotifyCountTv2, mNewContactsNumber);
    }

    @Override
    public void onClick(View v) {
        if (!UiUtils.isNormalClick(v)) {
            return;
        }
        switch (v.getId()) {
            case R.id.face_group_rl:
                // 面对面建群
                startActivity(new Intent(this, FaceToFaceGroup.class));
                break;
            case R.id.scan_rl:
                // 扫一扫
                MainActivity.requestQrCodeScan(this, bitmap -> {
                    // 不能是抛给MainActivity处理，
                    helloEventBus(new EventQRCodeReady(bitmap));
                });
                break;
            case R.id.nearby_rl:
                this.startActivity(new Intent(this, NearPersonActivity.class));
                break;
            case R.id.notice_rl:
                Intent intentNotice = new Intent(this, PublishNumberActivity.class);
                this.startActivity(intentNotice);
                break;
            case R.id.contacts_rl:
                if (!PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
                    onClick(v);
                }, null, PermissionUtil.getReadContactsPermissions())) {
                    return;
                }
                // 清空新联系人数量
                String mLoginUserId = coreManager.getSelf().getUserId();
                PreferenceUtils.putInt(this, Constants.NEW_CONTACTS_NUMBER + mLoginUserId, 0);
                mNotifyCountTv2.setVisibility(View.GONE);
                Intent intentGroup = new Intent(this, ContactsActivity.class);
                this.startActivity(intentGroup);
                break;
        }
    }

    public void helloEventBus(EventQRCodeReady eventQRCodeReady) {
        if (scannerRequesting) {
            return;
        }
        if (!PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) mContext, Short.MAX_VALUE, () -> {
            helloEventBus(eventQRCodeReady);
        }, null, PermissionUtil.getCameraPermissions())) {
            return;
        }
        // todo 目前调用requestQrCodeScan方法内的ctx对象均为getActivity获取(即MainActivity)，当ctx对象为恰activity时，这里就不能直接用this了，后面有需求在调整吧
        int size = ScreenUtil.getScreenWidth(MyApplication.getContext()) / 16 * 9;
        Intent intent = new Intent(this, ScannerActivity.class);
        // 设置扫码框的宽
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_WIDTH, size);
        // 设置扫码框的高
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_HEIGHT, size);
        // 设置扫码框距顶部的位置
        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_TOP_PADDING, DisplayUtil.dip2px(this, 100));
        // 可以从相册获取
        intent.putExtra(Constant.EXTRA_IS_ENABLE_SCAN_FROM_PIC, true);
        if (eventQRCodeReady.getBitmap() != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            eventQRCodeReady.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            intent.putExtra(Constant.EXTRA_SELF_QR_CODE_BITMAP, bytes);
        }
        startActivityForResult(intent, REQUEST_SCANNER);
        scannerRequesting = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SCANNER:
                scannerRequesting = false;
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || data.getExtras() == null) {
                        return;
                    }
                    String result = data.getExtras().getString(Constant.EXTRA_RESULT_CONTENT);
                    HandleQRCodeScanUtil.handleScanResult(this, result);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
