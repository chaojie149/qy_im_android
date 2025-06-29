package com.tongxin.caihong.ui.company;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.bean.company.Companys;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.util.ToastUtil;
import com.example.qrcode.utils.CommonUtils;
import com.tongxin.caihong.R;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.watermark.androidwm_light.WatermarkBuilder;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 公司二维码
 */
public class CompanyQRCodeActivity extends BaseActivity {
    private TextView tvPeopleNum;
    private ImageView ivQRCode;
    private String companyId;
    private String companyName;

    public static void start(Context ctx, String companyId, String companyName) {
        Intent intent = new Intent(ctx, CompanyQRCodeActivity.class);
        intent.putExtra("companyId", companyId);
        intent.putExtra("companyName", companyName);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_qr_code);
        if (getIntent() != null) {
            companyId = getIntent().getStringExtra("companyId");
            companyName = getIntent().getStringExtra("companyName");
        }
        initActionBar();
        initView();
        getCompanyNum();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.qrcode));
    }

    private void initView() {
        TextView tvName = findViewById(R.id.tvName);
        tvPeopleNum = findViewById(R.id.tvPeopleNum);
        ivQRCode = findViewById(R.id.ivQRCode);

        tvName.setText(companyName);
        String str = coreManager.getConfig().website + "?action=" + "company" + "&chatId=" + companyId;
        Log.e("zq", "二维码链接：" + str);
        // 生成二维码
        Bitmap bitmap = CommonUtils.createQRCode(str, ScreenUtil.getScreenWidth(mContext) - 200, ScreenUtil.getScreenWidth(mContext) - 200);
        ivQRCode.setImageBitmap(bitmap);

        findViewById(R.id.btnSave).setOnClickListener(this::saveImageToGalleryCompany);
        findViewById(R.id.btnShare).setOnClickListener(this::shareSingleImageCompany);
    }

    private void getCompanyNum() {
        Map<String, String> params = new HashMap<>();
        params.put("companyId", companyId);
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        HttpUtils.get().url(coreManager.getConfig().ORG_COMPANY_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<Companys>(Companys.class) {

                    @Override
                    public void onResponse(ObjectResult<Companys> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            if (result.getData() != null) {
                                tvPeopleNum.setText(getString(R.string.people_sums, String.valueOf(result.getData().getEmpNum())));
                            } else {
                                ToastUtil.showToast(mContext, getString(R.string.tip_server_error));
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(CompanyQRCodeActivity.this);
                    }
                });
    }

    public void saveImageToGalleryCompany(View view) {
        FileUtil.saveImageToGallery2(mContext, WatermarkBuilder.create(this, ivQRCode)
                .getWatermark().getOutputImage(), true);
    }

    public void shareSingleImageCompany(View view) {
        String imagePath = FileUtil.saveImageToGallery2(mContext, WatermarkBuilder.create(this, ivQRCode)
                .getWatermark().getOutputImage(), false);
        Uri imageUri = null; //imagePath--本地的文件路径
        try {
            imageUri = Uri.fromFile(new File(imagePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.toShare)));
    }
}