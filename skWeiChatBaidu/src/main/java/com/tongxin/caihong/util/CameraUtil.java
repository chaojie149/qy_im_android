package com.tongxin.caihong.util;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.view.photopicker.PhotoPickerActivity;
import com.tongxin.caihong.view.photopicker.SelectModel;
import com.tongxin.caihong.view.photopicker.intent.PhotoPickerIntent;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

public final class CameraUtil {
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static MediaScannerConnection sMediaScannerConnection;

    /**
     * Create a file Uri for saving an image or video
     *
     * @param context
     * @param type    the type of the file you want saved {@link #MEDIA_TYPE_IMAGE}
     *                {@link #MEDIA_TYPE_VIDEO}
     * @return return the uri of the file ,if create failed,return null
     */
    public static Uri getOutputMediaFileUri(Context context, int type) {
        File file = getOutputMediaFile(context, type);
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }

    /**
     * Create a file for saving an image or video,is default in the
     * ../Pictures/[you app PackageName] directory
     *
     * @param context
     * @param type    the type of the file you want saved {@link #MEDIA_TYPE_IMAGE}
     *                {@link #MEDIA_TYPE_VIDEO}
     * @return return the file you create,if create failed,return null
     */
    private static File getOutputMediaFile(Context context, int type) {
        String filePath = null;
        if (type == MEDIA_TYPE_IMAGE) {
            filePath = FileUtil.getRandomImageFilePath();
        } else if (type == MEDIA_TYPE_VIDEO) {
            filePath = FileUtil.getRandomVideoFilePath();
        } else {
            return null;
        }
        if (TextUtils.isEmpty(filePath)) {
            return null;
        } else {
            return new File(filePath);
        }
    }

    /**
     * invoke the system Camera app and capture a image。 you can received the
     * capture result in {@link Activity.onActivityResult(int,int,Intent)}。 If
     * successed,you can use the outputUri to get the image
     *
     * @param activity
     * @param outputUri   拍照后图片的存储路径
     * @param requestCode
     */
    public static void captureImage(AppCompatActivity activity, Uri outputUri, int requestCode) {
        Uri finalOutputUri = outputUri;
        if (!PermissionUtil.checkAndAutoRequestPermission(activity, Short.MAX_VALUE, () -> {
            captureImage(activity, finalOutputUri, requestCode);
        }, null, PermissionUtil.getCameraPermissions())) {
            return;
        }
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && TextUtils.equals(outputUri.getScheme(), "file")) {
            // 安卓11出现无法相机无法处理file uri,
            outputUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileProvider", new File(outputUri.getPath()));
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            // 事实上确实出现过这里的崩溃，不知道什么神仙手机，bugly#103905,
            Reporter.post("没有拍照app!?", e);
            ToastUtil.showToast(activity, R.string.c_take_picture_failed);
        }
    }

    /**
     * 调用系统裁减功能，裁减某张指定的图片，并输出到指定的位置
     *
     * @param activity
     * @param originalFileUri 原始图片位置
     * @param outputFileUri   裁减后图片的输出位置，两个地址最好不一样。如果一样的话，有的手机上面无法保存裁减的结果
     * @return
     */
    public static void cropImage(AppCompatActivity activity, Uri originalFileUri, Uri outputFileUri, int requestCode, int aspectX, int aspectY, int outputX,
                                 int outputY) {
        if (originalFileUri == null) {
            return;
        }
        UCrop.of(originalFileUri, outputFileUri)
                .withAspectRatio(aspectX, aspectY)
                .withMaxResultSize(outputX, outputY)
                .start(activity, requestCode);
    }

    public static void cropImage(Context ctx, Fragment fragment, Uri originalFileUri, Uri outputFileUri, int requestCode, int aspectX, int aspectY, int outputX,
                                 int outputY) {
        if (originalFileUri == null) {
            return;
        }
        UCrop.of(originalFileUri, outputFileUri)
                .withAspectRatio(aspectX, aspectY)
                .withMaxResultSize(outputX, outputY)
                .start(ctx, fragment, requestCode);
    }

    public static void pickImageSimple(AppCompatActivity activity, int requestCode) {
        if (!PermissionUtil.checkAndAutoRequestPermission(activity, Short.MAX_VALUE, () -> {
            pickImageSimple(activity, requestCode);
        }, null, PermissionUtil.getStoragePermissions())) {
            return;
        }
        PhotoPickerIntent intent = new PhotoPickerIntent(activity);
        intent.setSelectModel(SelectModel.SINGLE);
        intent.setLoadVideo(false);
        intent.setShowOriginal(false);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void pickImageSimple(Fragment fragment, int requestCode) {
        if (!PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) fragment.requireActivity(), Short.MAX_VALUE, () -> {
            pickImageSimple(fragment, requestCode);
        }, null, PermissionUtil.getCameraPermissions())) {
            return;
        }
        PhotoPickerIntent intent = new PhotoPickerIntent(fragment.requireContext());
        intent.setSelectModel(SelectModel.SINGLE);
        intent.setLoadVideo(false);
        intent.setShowOriginal(false);
        // 只有fragment.startActivityForResult才会回调到fragment.onActivityResult,
        fragment.startActivityForResult(intent, requestCode);
    }

    public static String parsePickImageResult(Intent intent) {
        return intent.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT).get(0);
    }

    /**
     * 获得The data stream for the file
     */
    public static String getImagePathFromUri(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                return getImagePathFromUriKitkat(context, uri);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        return getImagePathFromUriSimple(context, uri);
    }

    /**
     * 4.4以下
     *
     * @param context
     * @param uri
     */
    private static String getImagePathFromUriSimple(Context context, Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String returnStr = cursor.getString(column_index);
        cursor.close();
        return returnStr;
    }

    /**
     * 4.4以上的Document Uri
     *
     * @param context
     * @param uri
     * @return
     */
    private static String getImagePathFromUriKitkat(Context context, Uri uri) {
        String wholeID = DocumentsContract.getDocumentId(uri);
        if (TextUtils.isEmpty(wholeID) || !wholeID.contains(":")) {
            return null;
        }
        // 获得资源唯一ID
        String id = wholeID.split(":")[1];
        // 定义索引字段
        String[] column = {MediaStore.Images.Media.DATA};
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column, sel, new String[]{id}, null);
        int columnIndex = cursor.getColumnIndex(column[0]);

        String filePath = null;
        if (cursor.moveToFirst()) {
            // DATA字段就是本地资源的全路径
            filePath = cursor.getString(columnIndex);
        }
        // 切记要关闭游标
        cursor.close();
        return filePath;
    }

    /**
     * 调用系统图库选择照片,裁减后返回
     * ,4.4上无法确定用户是否是在图库里选择的照片，所以不使用该方法，使用pickImageSimple，返回后在调用裁减
     *
     * @param activity
     * @param outputUri   拍照后图片的存储路径
     * @param requestCode
     * @return
     */
    @Deprecated
    public static void pickImageCrop(Activity activity, Uri outputUri, int requestCode, int aspectX, int aspectY, int outputX, int outputY) {
        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        Intent intent = new Intent();
        // 根据版本号不同使用不同的Action
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        intent.putExtra("crop", "true");
        // 裁剪框比例
        intent.putExtra("aspectX", aspectX);
        intent.putExtra("aspectY", aspectY);
        // 图片输出大小
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true); // 部分机型没有设置该参数截图会有黑边
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // 不启用人脸识别
        intent.putExtra("noFaceDetection", false);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 扫描某张指定的图片放入系统媒体库
     */
    public static void scannerImage(Activity activity, final Uri fileUri, final ScannerResult scannerResult) {
        if (fileUri == null) {
            if (scannerResult != null) {
                scannerResult.onResult(false);
            }
            return;
        }
        sMediaScannerConnection = new MediaScannerConnection(activity, new MediaScannerConnectionClient() {
            public void onMediaScannerConnected() {
                sMediaScannerConnection.scanFile(fileUri.getPath(), "image/*");
            }

            public void onScanCompleted(String path, Uri uri) {
                sMediaScannerConnection.disconnect();
                if (scannerResult != null) {
                    scannerResult.onResult(uri != null);
                }
            }
        });
        sMediaScannerConnection.connect();
    }

    /**
     * 查询某张图片有没有被扫描到媒体库
     *
     * @param context
     * @param filePath
     * @return 返回这个图片在媒体库的Uri，如果没有扫描到媒体库，则返回null
     */
    public static Uri isImageFileInMedia(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Images.Media.DISPLAY_NAME + "='" + file.getName() + "'", null, null);
        Uri uri = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToLast();
            long id = cursor.getLong(0);
            uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        }
        return uri;
    }

    /**
     * @param path
     * @return void
     * @Title: setPictureDegreeZero
     */
    public static void setPictureDegreeZero(String path) {
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "no");
            exifInterface.saveAttributes();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 旋转bitmap
     */
    public static Bitmap restoreRotatedImage(int degrees, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return bitmap;
    }


    /**
     * 翻转bitmap (-1,1)左右翻转  (1,-1)上下翻转
     */
    public static Bitmap turnCurrentLayer(Bitmap srcBitmap, float sx, float sy) {
        Bitmap cacheBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);// 创建缓存像素的位图
        int w = cacheBitmap.getWidth();
        int h = cacheBitmap.getHeight();

        Canvas cv = new Canvas(cacheBitmap);//使用canvas在bitmap上面画像素

        Matrix mMatrix = new Matrix();//使用矩阵 完成图像变换

        mMatrix.postScale(sx, sy);

        Bitmap resultBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, w, h, mMatrix, true);
        cv.drawBitmap(resultBitmap,
                new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()),
                new Rect(0, 0, w, h), null);
        return resultBitmap;
    }

    private float[] calcScale(int rotation, int width, int height, int previewWidth, int previewHeight) {

        float ratioSurface = width > height ? (float) width / height : (float) height / width;
        float ratioPreview = (float) previewWidth / previewHeight;

        int scaledHeight = 0;
        int scaledWidth = 0;
        float scaleX = 1f;
        float scaleY = 1f;

        boolean isPortrait = rotation % 180 == 90;

        if (isPortrait && ratioPreview > ratioSurface) {
            scaledWidth = width;
            scaledHeight = (int) (((float) previewWidth / previewHeight) * width);
            scaleX = 1f;
            scaleY = (float) scaledHeight / height;
        } else if (isPortrait && ratioPreview < ratioSurface) {
            scaledWidth = (int) (height / ((float) previewWidth / previewHeight));
            scaledHeight = height;
            scaleX = (float) scaledWidth / width;
            scaleY = 1f;
        } else if (!isPortrait && ratioPreview < ratioSurface) {
            scaledWidth = width;
            scaledHeight = (int) (width / ((float) previewWidth / previewHeight));
            scaleX = 1f;
            scaleY = (float) scaledHeight / height;
        } else if (!isPortrait && ratioPreview > ratioSurface) {
            scaledWidth = (int) (((float) previewWidth / previewHeight) * width);
            scaledHeight = height;
            scaleX = (float) scaledWidth / width;
            scaleY = 1f;
        }

        return new float[]{scaleX, scaleY};
    }

    public static interface ScannerResult {
        void onResult(boolean success);
    }
}
