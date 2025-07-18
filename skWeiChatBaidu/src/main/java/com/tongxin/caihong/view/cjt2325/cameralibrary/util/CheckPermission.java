package com.tongxin.caihong.view.cjt2325.cameralibrary.util;

import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class CheckPermission {
    public static final int STATE_RECORDING = -1;
    public static final int STATE_NO_PERMISSION = -2;
    public static final int STATE_SUCCESS = 1;
    private static AudioRecord audioRecord;

    /**
     * 用于检测是否具有录音权限
     *
     * @return
     */
    public static int getRecordState() {
        int minBuffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat
                .ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat
                .CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, (minBuffer * 100));
        short[] point = new short[minBuffer];
        int readSize = 0;
        try {
            audioRecord.startRecording();//检测是否可以进入初始化状态

            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                //6.0以下机型都会返回此状态，故使用时需要判断bulid版本
                //检测是否在录音中
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;

                    Log.d("CheckAudioPermission", "录音机被占用");
                }
                return STATE_RECORDING;
            } else {
                //检测是否可以获取录音结果

                readSize = audioRecord.read(point, 0, point.length);


                if (readSize <= 0) {
                    if (audioRecord != null) {
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;

                    }
                    Log.d("CheckAudioPermission", "录音的结果为空");
                    return STATE_NO_PERMISSION;

                } else {
                    if (audioRecord != null) {
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;
                    }

                    return STATE_SUCCESS;
                }
            }
        } catch (Exception e) {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            return STATE_NO_PERMISSION;
        } finally {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }

        }

    }

    public static void closeAudio() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }


    public synchronized static boolean isCameraUseable(int cameraID) {
        boolean canUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open(cameraID);
            // setParameters 是针对魅族MX5。MX5通过Camera.open()拿到的Camera对象不为null
            Camera.Parameters mParameters = mCamera.getParameters();
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            e.printStackTrace();
            canUse = false;
        } finally {
            if (mCamera != null) {
                mCamera.release();
            } else {
                canUse = false;
            }
            mCamera = null;
        }
        return canUse;
    }
}