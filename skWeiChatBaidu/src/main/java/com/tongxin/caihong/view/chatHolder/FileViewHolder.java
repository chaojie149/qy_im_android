package com.tongxin.caihong.view.chatHolder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.helper.UploadEngine;
import com.tongxin.caihong.ui.mucfile.DownManager;
import com.tongxin.caihong.ui.mucfile.MucFileDetails;
import com.tongxin.caihong.ui.mucfile.MucFileListActivity;
import com.tongxin.caihong.ui.mucfile.XfileUtils;
import com.tongxin.caihong.ui.mucfile.bean.MucFileBean;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UploadCacheUtils;
import com.tongxin.caihong.util.filter.RegexInputFilter;
import com.tongxin.caihong.util.secure.MD5;
import com.tongxin.caihong.view.FileProgressPar;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;

import java.util.HashSet;
import java.util.Set;

public class FileViewHolder extends AChatHolderInterface {
    // 记住解密了的文件消息，
    public static Set<String> unlockFileSet = new HashSet<>();

    ImageView ivCardImage;
    TextView tvPersonName;
    TextView tvPersonSex;
    FileProgressPar progressPar;
    ImageView ivUploadCancel;
    TextView tvType;

    /**
     * @param onSuccess 密码输入成功时回调，
     * @return 返回true表示走了输入密码的逻辑，需要阻止继续打开文件的操作，
     */
    public static boolean checkFilePassword(Context mContext, ChatMessage mdata, Runnable onSuccess) {
        if (!unlockFileSet.contains(mdata.getPacketId()) && !TextUtils.isEmpty(mdata.getObjectId())) {
            // 有密码，要验证，
            DialogHelper.showLimitSingleInputDialog((Activity) mContext
                    , mContext.getString(R.string.file_password)
                    , "", mContext.getString(R.string.hint_input_file_password)
                    , new InputFilter[]{new RegexInputFilter("[a-zA-Z0-9]*")}
                    , text -> {
                        if (TextUtils.isEmpty(text)) {
                            ToastUtil.showToast(mContext, R.string.tip_file_password_empty);
                            return false;
                        }
                        if (!TextUtils.isEmpty(text)) {
                            text = MD5.encryptHex(text);
                        }
                        if (!text.equals(mdata.getObjectId())) {
                            ToastUtil.showToast(mContext, R.string.tip_file_password_wrong);
                            return false;
                        }
                        unlockFileSet.add(mdata.getPacketId());
                        onSuccess.run();
                        return true;
                    }).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            return true;
        }
        return false;
    }

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_card : R.layout.chat_to_item_card;
    }

    @Override
    public void initView(View view) {
        ivCardImage = view.findViewById(R.id.iv_card_head);
        tvPersonName = view.findViewById(R.id.person_name);
        tvPersonSex = view.findViewById(R.id.person_sex);
        progressPar = view.findViewById(R.id.chat_card_light);
        ivUploadCancel = view.findViewById(R.id.chat_upload_cancel_iv);
        tvType = view.findViewById(R.id.person_title);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        String filePath;
        if (message.getType() == XmppMessage.TYPE_MUCFILE_ADD) {
            tvType.setText(getString(R.string.group_file));
            message.setUpload(true);
            filePath = message.getFilePath();
        } else {
            tvType.setText(getString(R.string.chat_file));
            filePath = UploadCacheUtils.get(mContext, message);
            filePath = FileUtil.isExist(filePath) ? filePath : message.getContent();
        }
        if (TextUtils.isEmpty(filePath)) return;
        // 设置图标
        int pointIndex = filePath.lastIndexOf(".");
        if (pointIndex != -1) {
            String type = filePath.substring(pointIndex + 1).toLowerCase();
            if ((type.equals("png") || type.equals("jpg") || type.equals("gif"))
                    && message.getType() != XmppMessage.TYPE_MUCFILE_ADD) {
                ImageLoadHelper.showImageWithSize(
                        mContext,
                        filePath,
                        100, 100,
                        ivCardImage
                );
                message.setTimeLen(1);
            } else {
                fillFileIcon(type, ivCardImage, message);
            }
        }

        // 设置文件名称
        // 只取文件名，不读取文件，从filePath读取比从缓存读取有效，缓存文件可能不存在，
        String fileName = TextUtils.isEmpty(message.getFilePath()) ? message.getContent() : message.getFilePath();
        int start = fileName.lastIndexOf("/");
        String name = fileName.substring(start + 1).toLowerCase();
        tvPersonName.setText(name);
        tvPersonSex.setText(XfileUtils.fromatSize(message.getFileSize()));
        tvPersonSex.setVisibility(View.VISIBLE);

        if (isMysend) { // 判断是否上传
            // 没有上传
            boolean show = !message.isUpload() && message.getUploadSchedule() < 100
                    && message.getMessageState() == ChatMessageListener.MESSAGE_SEND_ING;
            changeVisible(progressPar, show);

            if (show) {
                if (ivUploadCancel != null) {
                    ivUploadCancel.setVisibility(View.VISIBLE);
                }
            } else {
                if (ivUploadCancel != null) {
                    ivUploadCancel.setVisibility(View.GONE);
                }
            }
        } else {
            changeVisible(progressPar, false);
        }

        progressPar.update(message.getUploadSchedule());

        if (ivUploadCancel != null) {
            ivUploadCancel.setOnClickListener(v -> {
                SelectionFrame selectionFrame = new SelectionFrame(mContext);
                selectionFrame.setSomething(getString(R.string.cancel_upload), getString(R.string.sure_cancel_upload), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        // 用户可能在弹窗弹起后停留很久，所以点击确认的时候还需要判断一下
                        if (!mdata.isUpload()) {
                            UploadEngine.cancel(mdata.getPacketId());
                        }
                    }
                });
                selectionFrame.show();
            });
        }

        // 消息发送失败有可能与文件上传失败一起出现(上传时杀死app)，发送失败按钮 > 取消上传按钮显示，如两个一起显示会重叠
        if (mdata.getMessageState() == ChatMessageListener.MESSAGE_SEND_FAILED) {
            ivUploadCancel.setVisibility(View.GONE);
            progressPar.update(0);
        }
    }

    @Override
    protected void onRootClick(View v) {
        if (mdata.getType() == XmppMessage.TYPE_MUCFILE_ADD) {
            boolean isAllowUploadFile = PreferenceUtils.getBoolean(MyApplication.getContext(),
                    Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mToUserId, true);

            Intent intent = new Intent(mContext, MucFileListActivity.class);
            intent.putExtra("roomId", roomId);
            intent.putExtra("role", selfGroupRole);
            intent.putExtra("allowUploadFile", isAllowUploadFile ? 1 : 0);
            mContext.startActivity(intent);
            return;
        }
        if (TextUtils.isEmpty(mdata.getContent())
                && TextUtils.isEmpty(UploadCacheUtils.get(mContext, mdata))) {
            // 容错
            ToastUtil.showToast(mContext, getString(R.string.alert_not_have_file));
            return;
        }
        if (checkFilePassword(mContext, mdata, () -> onRootClick(v))) {
            return;
        }

        sendReadMessage(mdata);
        ivUnRead.setVisibility(View.GONE);

        MucFileBean data = new MucFileBean();
        String url = mdata.getContent();
        String filePath = UploadCacheUtils.get(mContext, mdata);
        if (TextUtils.isEmpty(url)
                && !TextUtils.isEmpty(filePath)
                && FileUtil.isExist(filePath)) {
            // 符合此条件，此时文件多半未上传成功，url赋值本地路径
            url = filePath;
        }
        if (TextUtils.isEmpty(filePath)) {
            // 符合此条件，不清楚是什么原因。。。，不管，filePath赋值网络路径
            filePath = url;
        }
        // 取出文件名称
        int start = mdata.getFilePath().lastIndexOf("/");
        String name = mdata.getFilePath().substring(start + 1).toLowerCase();
        data.setName(name);
        data.setUrl(url);
        // 因为MucFileBean是针对群文件共享的实体bean，我们跳转到MucFileDetails只能自己根据那边的逻辑自己封装类，
        // 此时NickName字段放真的路径，MucFileDetails那特别处理一下
        data.setNickname(filePath);
        int size = mdata.getFileSize();
        data.setSize(size);
        data.setState(FileUtil.isExist(filePath) ? DownManager.STATE_DOWNLOADED : DownManager.STATE_UNDOWNLOAD);
        data.setType(mdata.getTimeLen());
        Intent intent = new Intent(mContext, MucFileDetails.class);
        intent.putExtra("data", data);
        intent.putExtra(AppConstant.EXTRA_MSG_ID, mdata.getPacketId());
        mContext.startActivity(intent);
    }

    private void fillFileIcon(String type, ImageView v, ChatMessage chat) {
        if (type.equals("mp3")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_y);
            chat.setTimeLen(2);
        } else if (type.equals("mp4") || type.equals("avi")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_v);
            chat.setTimeLen(3);
        } else if (type.equals("xls") || type.equals("xlsx")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_x);
            chat.setTimeLen(5);
        } else if (type.equals("doc") || type.equals("docx")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_w);
            chat.setTimeLen(6);
        } else if (type.equals("ppt")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_p);
            chat.setTimeLen(4);
        } else if (type.equals("pdf")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_f);
            chat.setTimeLen(10);
        } else if (type.equals("apk")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_a);
            chat.setTimeLen(11);
        } else if (type.equals("txt")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_t);
            chat.setTimeLen(8);
        } else if (type.equals("rar") || type.equals("zip")) {
            v.setImageResource(R.drawable.ic_muc_flie_type_z);
            chat.setTimeLen(7);
        } else {
            v.setImageResource(R.drawable.ic_muc_flie_type_what);
            chat.setTimeLen(9);
        }
    }

    @Override
    public boolean enableUnRead() {
        return true;
    }
}
