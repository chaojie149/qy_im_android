package com.joe.camera2recorddemo.Utils;

import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;

/**
 * Created by Administrator on 2017/10/10.
 */

public class CameraUtils {
    /**
     * 解决前置摄像头上下颠倒的问题
     *
     * @param cameraId
     * @param camera
     */
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        //当前屏幕的方向，0为自然方向，1为顺时针旋转90，2为顺时针180，3为顺时针270
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;//当前手机顺时针旋转的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//前置
            //前置摄像头会进行水平镜像旋转（逆时针旋转180）
            result = (cameraInfo.orientation + 180 - degree) % 360;
        } else {
            //后置摄像头无需水平翻转，直接是差值
            result = (cameraInfo.orientation - degree + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * @param cameraId {@link Camera.CameraInfo#CAMERA_FACING_FRONT} {@link Camera.CameraInfo#CAMERA_FACING_BACK}
     * @return 前置摄像头或者后置摄像头的id,
     */
    public static int getCamera(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cnt = Camera.getNumberOfCameras();
        for (int i = 0; i < cnt; i++) {
            Camera.getCameraInfo(i, info);

            if (info.facing == cameraId) {
                return i;
            }
        }
        return 0;
    }
}
