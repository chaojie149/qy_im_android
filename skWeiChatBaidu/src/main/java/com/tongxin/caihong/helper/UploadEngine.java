package com.tongxin.caihong.helper;

import android.text.TextUtils;
import android.util.Log;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.UploadFileResult;
import com.tongxin.caihong.bean.UploadingFile;
import com.tongxin.caihong.bean.event.EventUploadCancel;
import com.tongxin.caihong.bean.event.EventUploadFileRate;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.UploadingFileDao;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.GetFileSizeUtil;
import com.tongxin.caihong.util.Md5Util;
import com.tongxin.caihong.util.TanX;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.FileCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

import static com.tongxin.caihong.bean.message.XmppMessage.TYPE_VOICE;

/**
 * 消息文件上传类
 */
public class UploadEngine {
    private static Map<String, Call> requestHandleMap = new HashMap<>();

    /**
     * @param accessToken
     * @param loginUserId
     * @param toUserId
     * @param message
     * @param response
     * @param isCheckedFileMd5 是否检查过文件md5值
     */
    public static void uploadImFile(String accessToken, final String loginUserId, final String toUserId, final ChatMessage message, final ImFileUploadResponse response
            , boolean isCheckedFileMd5) {
        // 将正在上传的消息存入本地
        UploadingFile mUploadingFile = new UploadingFile();
        mUploadingFile.setUserId(loginUserId);
        mUploadingFile.setToUserId(message.getToUserId());
        mUploadingFile.setMsgId(message.getPacketId());
        UploadingFileDao.getInstance().createUploadingFile(mUploadingFile);
        // 先存个空值，确保checkFileMd5耗时太久时，消息状态在AChatHolderInterface内被识别为未上传
        requestHandleMap.put(message.getPacketId(), null);

        if (!TextUtils.isEmpty(message.getFilePath()) && !isCheckedFileMd5) {
            Log.e("HTTP", "上传文件，此文件未检查过文件md5值，判断是否需要检查");
            File file = new File(message.getFilePath());
            if (GetFileSizeUtil.getFileSize(file) / 1048576 > 10) {
                // 文件大于10M，准备校验文件md5
                Log.e("HTTP", "文件大小为-->" + GetFileSizeUtil.getFileSize(file) / 1048576 + " M，大于10M，准备检查文件md5值");
                checkFileMd5(accessToken, loginUserId, toUserId, message, response, file);
                return;
            } else {
                Log.e("HTTP", "文件大小为-->" + GetFileSizeUtil.getFileSize(file) / 1048576 + " M，小于等于10M，不检查文件md5值，直接上传");
            }
        } else {
            Log.e("HTTP", "上传文件，此文件检查过md5值，服务端无匹配，直接上传");
        }
        Map<String, String> params = new HashMap<>();
        params.put("userId", loginUserId);

        // 目前文件有效期只用于聊天内产生的文件，其他文件如群共享文件、朋友圈上传的文件...有效期为永久 (-1 永久 default==7天)
        Friend friend = FriendDao.getInstance().getFriend(loginUserId, toUserId);
        if (friend != null) {
            params.put("validTime", String.valueOf(friend.getChatRecordTimeOut()));// 文件有效时长为用户的消息过期时长
        } else {
            params.put("validTime", "7");
        }

        String url = CoreManager.requireConfig(MyApplication.getInstance()).UPLOAD_URL;
        Call call = HttpUtils.post().url(url)
                .params(params)
                .params("files", new File(message.getFilePath()))
                .build()
                .execute(new FileCallback<UploadFileResult>(UploadFileResult.class) {
                    long mCurrentBytesWritten = 0;

                    @Override
                    public void onProgress(String key, long current, long total) {
                        if (current == total) {
                            EventBus.getDefault().post(new EventUploadFileRate(message.getPacketId(), 100));
                            ChatMessageDao.getInstance().updateMessageUploadSchedule(loginUserId, toUserId, message.getPacketId(), 100);
                        } else {
                            long mOnePercentage = total / 100;
                            if (current - mCurrentBytesWritten >= mOnePercentage) {
                                mCurrentBytesWritten = current;
                                int rate = (int) (current / mOnePercentage);
                                EventBus.getDefault().post(new EventUploadFileRate(message.getPacketId(), rate));
                                ChatMessageDao.getInstance().updateMessageUploadSchedule(loginUserId, toUserId, message.getPacketId(), rate);
                            }
                        }
                    }

                    @Override
                    public void onResponse(UploadFileResult result) {
                        // 无论上传成功或失败，回调后删除"正在上传.."状态
                        UploadingFileDao.getInstance().deleteUploadingFile(loginUserId, message.getPacketId());
                        requestHandleMap.remove(message.getPacketId());
                        if (result.getFailure() == 1) {//上传失败
                            if (response != null) {
                                response.onFailure(toUserId, message);
                            }
                            Reporter.post("上传文件失败，");
                            return;
                        }
                        String url = null;
                        //上传成功
                        if (result.getResultCode() != Result.CODE_SUCCESS || result.getData() == null
                                || result.getSuccess() != result.getTotal()) {

                        } else {
                            UploadFileResult.Data data = result.getData();
                            if (message.getType() == XmppMessage.TYPE_IMAGE
                                    || message.getType() == XmppMessage.TYPE_LOCATION) {
                                // 位置消息的地图截图也是一样的当成图片处理，
                                url = getImagesUrl(data);
                            } else if (message.getType() == TYPE_VOICE) {
                                url = getAudiosUrl(data);
                            } else if (message.getType() == XmppMessage.TYPE_VIDEO) {
                                url = getVideosUrl(data);
                            } else if (message.getType() == XmppMessage.TYPE_FILE) {
                                if (TextUtils.isEmpty(url = getFilesUrl(data)))//如果获取到的文件名为空，就获取其他的
                                    if (TextUtils.isEmpty(url = getVideosUrl(data)))
                                        if (TextUtils.isEmpty(url = getAudiosUrl(data)))
                                            if (TextUtils.isEmpty(url = getImagesUrl(data)))
                                                url = getOthersUrl(data);
                            }
                        }
                        if (TextUtils.isEmpty(url)) {//返回成功，但是却获取不到对应的URL，服务器返回值异常<概率极小>
                            if (response != null) {
                                response.onFailure(toUserId, message);
                                ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.getPacketId(), false, url);
                            }
                        } else {
                            // 记录本机上传，用于快速读取，
                            UploadCacheUtils.save(MyApplication.getInstance(), url, message.getFilePath());
                            ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.getPacketId(), true, url);
                            if (response != null) {
                                message.setContent(url);
                                message.setUpload(true);
                                response.onSuccess(toUserId, message);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // 无论上传成功或失败，回调后删除"正在上传.."状态
                        UploadingFileDao.getInstance().deleteUploadingFile(loginUserId, message.getPacketId());
                        requestHandleMap.remove(message.getPacketId());

                        Reporter.post("上传文件<" + message.getFilePath() + ">失败，", e);
                        if (response != null) {
                            response.onFailure(toUserId, message);
                        }
                    }
                });
        requestHandleMap.put(message.getPacketId(), call);
    }

    private static void checkFileMd5(String accessToken, final String loginUserId, final String toUserId, final ChatMessage message, final ImFileUploadResponse response
            , File file) {
        Map<String, String> params = new HashMap<>();
        Log.e("HTTP", "文件md值-->" + Md5Util.getFileMd5(file));
        params.put("md5Code", Md5Util.getFileMd5(file));
        HttpUtils.post().url(CoreManager.requireConfig(MyApplication.getContext()).UPLOAD_MD5_CHECK)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {
                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        UploadingFileDao.getInstance().deleteUploadingFile(loginUserId, message.getPacketId());
                        requestHandleMap.remove(message.getPacketId());

                        String url = result.getData();
                        if (TextUtils.isEmpty(url)) {
                            Log.e("HTTP", "检查文件md5值，服务端未返回结果，继续上传");
                            uploadImFile(accessToken, loginUserId, toUserId, message, response, true);
                        } else {
                            Log.e("HTTP", "检查文件md5值，服务端返回结果-->" + url + "，不上传，进行后续处理");
                            EventBus.getDefault().post(new EventUploadFileRate(message.getPacketId(), 100));
                            ChatMessageDao.getInstance().updateMessageUploadSchedule(loginUserId, toUserId, message.getPacketId(), 100);
                            // 记录本机上传，用于快速读取，
                            UploadCacheUtils.save(MyApplication.getInstance(), url, message.getFilePath());
                            ChatMessageDao.getInstance().updateMessageUploadState(loginUserId, toUserId, message.getPacketId(), true, url);
                            if (response != null) {
                                message.setContent(url);
                                message.setUpload(true);
                                response.onSuccess(toUserId, message);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.e("HTTP", "检查文件md5值，接口调用失败，继续上传");
                        UploadingFileDao.getInstance().deleteUploadingFile(loginUserId, message.getPacketId());
                        requestHandleMap.remove(message.getPacketId());

                        uploadImFile(accessToken, loginUserId, toUserId, message, response, true);
                    }
                });
    }

    public static void cancel(String msgId) {
        EventBus.getDefault().post(new EventUploadCancel(msgId));

        Call call = requestHandleMap.get(msgId);
        if (call != null) {
            new Thread(() -> call.cancel()).start();
        }
    }

    private static String getAudiosUrl(UploadFileResult.Data data) {
        TanX.Log("语音格式");
        if (data.getAudios() != null && data.getAudios().size() > 0) {
            return data.getAudios().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getFilesUrl(UploadFileResult.Data data) {
        TanX.Log("文件格式");
        if (data.getFiles() != null && data.getFiles().size() > 0) {
            return data.getFiles().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getVideosUrl(UploadFileResult.Data data) {
        TanX.Log("视频格式");
        if (data.getVideos() != null && data.getVideos().size() > 0) {
            return data.getVideos().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getImagesUrl(UploadFileResult.Data data) {
        TanX.Log("图片格式");
        if (data.getImages() != null && data.getImages().size() > 0) {
            return data.getImages().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    private static String getOthersUrl(UploadFileResult.Data data) {
        TanX.Log("其他格式");
        if (data.getOthers() != null && data.getOthers().size() > 0) {
            return data.getOthers().get(0).getOriginalUrl();
        } else {
            return "";
        }
    }

    public static boolean containsMessage(String packetId) {
        return requestHandleMap.containsKey(packetId);
    }

    public interface ImFileUploadResponse {
        void onSuccess(String toUserId, ChatMessage message);

        void onFailure(String toUserId, ChatMessage message);
    }
}
