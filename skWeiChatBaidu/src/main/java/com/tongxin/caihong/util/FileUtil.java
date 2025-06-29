package com.tongxin.caihong.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.User;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.CoreManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.UUID;

public class FileUtil {

    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_ADUIO = 2;
    private static final int TYPE_VIDEO = 3;

    /**
     * {@link #TYPE_IMAGE}<br/>
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    private static String getPublicFilePath(int type) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = MyApplication.getInstance().mVoicesDir;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = MyApplication.getInstance().mVideosDir;
                fileSuffix = ".mp4";
                break;
            case TYPE_IMAGE:
                fileDir = MyApplication.getInstance().mPicturesDir;
                fileSuffix = ".jpg";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        return fileDir + File.separator + UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

    /**
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    private static String getPrivateFilePath(int type, String userId) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = MyApplication.getInstance().mAppDir + File.separator + userId + File.separator + Environment.DIRECTORY_MUSIC;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = MyApplication.getInstance().mAppDir + File.separator + userId + File.separator + Environment.DIRECTORY_MOVIES;
                fileSuffix = ".mp4";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        return fileDir + File.separator + UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

    public static String getRandomImageFilePath() {
        return getPublicFilePath(TYPE_IMAGE);
    }

    public static String getRandomAudioFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            return getPrivateFilePath(TYPE_ADUIO, user.getUserId());
        } else {
            return getPublicFilePath(TYPE_ADUIO);
        }
    }

    public static String getRandomAudioAmrFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        String filePath = null;
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            filePath = getPrivateFilePath(TYPE_ADUIO, user.getUserId());
        } else {
            filePath = getPublicFilePath(TYPE_ADUIO);
        }
        if (!TextUtils.isEmpty(filePath)) {
            return filePath.replace(".mp3", ".amr");
        } else {
            return null;
        }
    }

    public static String getRandomVideoFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            return getPrivateFilePath(TYPE_VIDEO, user.getUserId());
        } else {
            return getPublicFilePath(TYPE_VIDEO);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////
    public static boolean isImageFile(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            if (fileName.endsWith(".png")
                    || fileName.endsWith(".jpg")
                    || fileName.endsWith(".gif")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideoFile(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            if (fileName.endsWith(".mp4")
                    || fileName.endsWith(".avi")) {
                return true;
            }
        }
        return false;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static void createFileDir(String fileDir) {
        File fd = new File(fileDir);
        if (!fd.exists()) {
            fd.mkdirs();
        }
    }

    /**
     * @param fullName
     */
    public static boolean delFile(String fullName) {
        File file = new File(fullName);
        if (file.exists()) {
            if (file.isFile()) {
                try {
                    file.delete();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 删除文件夹里面的所有文件
     *
     * @param path String 文件夹路径 如 /sdcard/data/
     */
    public static void delAllFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            System.out.println(path + tempList[i]);
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]); // 先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]); // 再删除空文件夹
            }
        }
    }

    /**
     * 删除文件夹
     * <p>
     * String 文件夹路径及名称 如/sdcard/data/
     * String
     *
     * @return boolean
     */
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // 删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); // 删除空文件夹
        } catch (Exception e) {
            System.out.println("删除文件夹操作出错");
            e.printStackTrace();
        }
    }

    public static File saveFileByBitmap(Bitmap bitmap, String fileDir, String fileName) {
        File dirFile = new File(fileDir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }

        File myCaptureFile = new File(fileName);
        BufferedOutputStream bufferedOutputStream;
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e("xuan", "saveFileByBitmap: " + myCaptureFile.getAbsolutePath());
        return myCaptureFile;
    }

    @NonNull
    public static String getSaveDirectory(String str) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + str + "/";
            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return MyApplication.getContext().getDir(str, Context.MODE_PRIVATE).getAbsolutePath();
                }
            }
            return rootDir;
        } else {
            return MyApplication.getContext().getDir(str, Context.MODE_PRIVATE).getAbsolutePath();
        }
    }

    private static void addPictureToAlbum(Context ctx, String fileName, String mime, WriteImageCallback writeImageCallback) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, fileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mime);
        Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        try (OutputStream output = ctx.getContentResolver().openOutputStream(uri)) {
            writeImageCallback.writeTo(output);
        } catch (Exception e) {
            throw new IllegalStateException("保存图片异常", e);
        }
    }

    /**
     * 保存二维码
     *
     * @param context
     * @param bitmap
     * @param isSendBroadcast：是否发送广播更新图库
     * @return
     */
    public static String saveImageToGallery2(Context context, Bitmap bitmap, boolean isSendBroadcast) {
        if (PermissionUtil.needMediaPermission()
                && !PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) context, Short.MAX_VALUE, () -> {
            saveImageToGallery2(context, bitmap,isSendBroadcast);
        }, null, PermissionUtil.getStoragePermissions())) {
            return null;
        }

        if (bitmap == null) {
            ToastUtil.showToast(context, context.getString(R.string.creating_qr_code));
        }
        // 1.保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2.把文件插入到系统图库
        // todo 文件插入系统图库，系统又生成了张缩略图，不插入了，直接通知图库更新
/*
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, fileName);
            Toast.makeText(context, R.string.tip_saved_qr_code, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
*/
        // 3.通知图库更新
        if (isSendBroadcast) {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
            Toast.makeText(context, R.string.tip_saved_qr_code, Toast.LENGTH_SHORT).show();
        }
        return file.getAbsolutePath();
    }

    /**
     * 将图片(本地/网络)保存至相册
     */
    public static void downImageToGallery(final Context context, String url) {
        if (PermissionUtil.needMediaPermission()
                && !PermissionUtil.checkAndAutoRequestPermission((AppCompatActivity) context, Short.MAX_VALUE, () -> {
            downImageToGallery(context, url);
        }, null, PermissionUtil.getStoragePermissions())) {
            return;
        }

        if (url.toLowerCase().endsWith("gif")) {
            ImageLoadHelper.loadFile(
                    context,
                    url,
                    f -> {
                        // 文件名保留旧代码，
                        String fileName = System.currentTimeMillis() + ".gif";
                        addPictureToAlbum(context, fileName, "image/gif", output -> {
                            try (InputStream input = new FileInputStream(f)) {
                                copy(input, output);
                            } catch (Exception e) {
                                throw new IllegalStateException("保存gif图片异常", e);
                            }
                        });
                        Toast.makeText(context, R.string.tip_save_image_success, Toast.LENGTH_SHORT).show();
                    }
            );
        } else {
            ImageLoadHelper.loadBitmapDontAnimate(
                    context,
                    url,
                    b -> {
                        // 文件名保留旧代码，
                        String fileName = System.currentTimeMillis() + ".jpg";
                        addPictureToAlbum(context, fileName, "image/jpeg", output -> {
                            b.compress(Bitmap.CompressFormat.JPEG, 100, output);
                        });
                        Toast.makeText(context, R.string.tip_save_image_success, Toast.LENGTH_SHORT).show();
                    }, e -> {
                        ToastUtil.showToast(context, context.getString(R.string.tip_save_image_failed));
                    }
            );
        }
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesum = 0;
        int byteread = 0;
        byte[] buffer = new byte[8 * 1024];
        while ((byteread = input.read(buffer)) >= 0) {
            bytesum += byteread; //字节数 文件大小
            output.write(buffer, 0, byteread);
        }
    }

    /**
     * 保存bitmap到本地
     */
    public static String saveBitmap(Bitmap bitmap) {
        // 都是用于临时保存图片，所以直接存到私有缓存目录里，
        File imageDir = new File(MyApplication.getContext().getExternalCacheDir(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".png";
        File file = new File(imageDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        readPictureDegree(file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    public static String getDesignationFilePath(String fileName, Bitmap bitmap) {
        File imageDir = new File(MyApplication.getContext().getFilesDir(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }
        fileName = fileName + ".png";
        File file = new File(imageDir, fileName);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        readPictureDegree(file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.i("zx", "读取角度-" + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("zx", "readPictureDegree: " + degree);
        return degree;
    }

    public static File createImageFileForEdit() {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        return new File(imageDir, fileName);
    }

    public static boolean isExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        return file.exists();
    }

    // 返回一个byte数组
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        // 获取文件大小
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            // 文件太大，无法读取
            throw new IOException("File is to large " + file.getName());
        }
        // 创建一个数据来保存文件数据
        byte[] bytes = new byte[(int) length];
        // 读取数据到byte数组中
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    public static int suffix2Type(String suffix) {
        if (TextUtils.isEmpty(suffix)) {
            return 9;
        }
        if (TextUtils.equals(suffix, "mp3")) {
            return 0;
        } else if (TextUtils.equals(suffix, "mp4") || TextUtils.equals(suffix, "avi")) {
            return 1;
        } else if (TextUtils.equals(suffix, "doc")) {
            return 2;
        } else if (TextUtils.equals(suffix, "xls")) {
            return 3;
        } else if (TextUtils.equals(suffix, "ppt")) {
            return 4;
        } else if (TextUtils.equals(suffix, "pdf")) {
            return 5;
        } else if (TextUtils.equals(suffix, "txt")) {
            return 6;
        } else if (TextUtils.equals(suffix, "apk")) {
            return 7;
        } else if (TextUtils.equals(suffix, "rar") || TextUtils.equals(suffix, "zip")) {
            return 8;
        } else {
            return 9;
        }
    }

    public static void fillTypeIcon(long type, ImageView iv) {
        if (type == 0) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_y);
        } else if (type == 1) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_v);
        } else if (type == 2) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_w);
        } else if (type == 3) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_x);
        } else if (type == 4) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_f);
        } else if (type == 5) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_p);
        } else if (type == 6) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_t);
        } else if (type == 7) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_a);
        } else if (type == 8) {
            iv.setImageResource(R.drawable.ic_muc_flie_type_z);
        } else {
            iv.setImageResource(R.drawable.ic_muc_flie_type_what);
        }
    }

    public static int getFileType(String fileName) {
        int i = 0;
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(fileName);
        if (contentTypeFor != null && contentTypeFor.contains("image")) {
            i = 1;
        }
        return i;
    }

    public interface WriteImageCallback {
        void writeTo(OutputStream output);
    }

}
