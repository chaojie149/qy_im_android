package com.example.qrcode.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.qrcode.R;
import com.example.qrcode.ScannerActivity;
import com.google.zxing.Result;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * Created by yangyu on 2017/11/27.
 */

public class DecodeUtils {
    private static final String TAG = "DecodeUtils";

    /**
     * 从开源扫码app复制来的压缩图片方法，
     */
    public static Bitmap compressPicture(Context ctx, Uri decodeUri) throws FileNotFoundException {
        // 做些预处理提升扫码成功率，
        // 预读一遍获取图片比例，使用inSampleSize压缩图片分辨率到恰到好处，
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        ContentResolver resolver = ctx.getContentResolver();

        InputStream in = null;
        try {
            in = resolver.openInputStream(decodeUri);
            BitmapFactory.decodeStream(in, null, options);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    Log.w(TAG, ioe);
                }
            }
        }

        int height = options.outHeight;
        int width = options.outWidth;
        options.inJustDecodeBounds = false;
        options.inSampleSize = (int) Math.round(Math.sqrt(height * width / (double) (320 * 240)));

        in = null;
        Bitmap bitmap;
        try {
            in = resolver.openInputStream(decodeUri);
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    Log.w(TAG, ioe);
                }
            }
        }
        return bitmap;
    }

    public static Result decodeFromPicture(Context context, Bitmap srcBitmap) {
        if (srcBitmap == null) {
            return null;
        }
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int sw = outMetrics.widthPixels;
        int sh = outMetrics.heightPixels;
        // 保证图片的宽高不大于屏幕的宽高

        float rw = 1f * sw / srcBitmap.getWidth();
        float rh = 1f * sh / srcBitmap.getHeight();
        float r = Math.min(rw, rh);
        srcBitmap = zoomImg(srcBitmap, (int) (srcBitmap.getWidth() * r), (int) (srcBitmap.getHeight() * r));
        Result[] results = QrCodeUtils.parseMultiFromBitmap(srcBitmap);
        if (results != null && results.length > 0) {
            return results[0];
        } else {
            return null;
        }
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    public static class DecodeAsyncTask extends AsyncTask<String, Integer, Result> {

        private WeakReference<ScannerActivity> activity;
        private Result result;

        public DecodeAsyncTask(ScannerActivity activity) {
            this.activity = new WeakReference<ScannerActivity>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Result doInBackground(String... bitmaps) {
            result = decodeFromPicture(activity.get(), BitmapFactory.decodeFile(bitmaps[0]));
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (result != null) {
                if (activity.get() != null) {
                    activity.get().handDecode(result);
                }
            } else {
                if (activity.get() != null) {
                    Toast.makeText(activity.get(), activity.get().getString(R.string.tip_decode_failed), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}


