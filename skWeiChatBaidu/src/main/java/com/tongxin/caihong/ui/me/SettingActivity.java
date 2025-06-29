package com.tongxin.caihong.ui.me;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tongxin.caihong.BuildConfig;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.LoginHelper;
import com.tongxin.caihong.sp.UserSp;
import com.tongxin.caihong.ui.account.AccountSwitchActivity;
import com.tongxin.caihong.ui.account.ChangePasswordActivity;
import com.tongxin.caihong.ui.account.FindPwdActivity;
import com.tongxin.caihong.ui.account.LoginActivity;
import com.tongxin.caihong.ui.backup.BackupHistoryActivity;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.contacts.BlackActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.GetFileSizeUtil;
import com.tongxin.caihong.util.LocaleHelper;
import com.tongxin.caihong.util.SkinUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.view.SelectionFrame;
import com.tongxin.caihong.view.window.WindowShowService;
import com.tongxin.caihong.xmpp.helloDemon.IntentWrapper;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 设置
 */
public class SettingActivity extends BaseActivity implements View.OnClickListener {
    private TextView mCacheTv;
    private String mLoginUserId;
    private My_BroadcastReceiver mMyBroadcastReceiver = new My_BroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.tongxin.caihong.broadcast.OtherBroadcast.SEND_MULTI_NOTIFY);
        intentFilter.addAction(LoginHelper.ACTION_LOGOUT);
        intentFilter.addAction(com.tongxin.caihong.broadcast.OtherBroadcast.NO_EXECUTABLE_INTENT);
        registerReceiver(mMyBroadcastReceiver, intentFilter);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.settings));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyBroadcastReceiver != null) {
            unregisterReceiver(mMyBroadcastReceiver);
        }
    }

    private void initView() {
        mCacheTv = findViewById(R.id.cache_tv);
        long cacheSize = GetFileSizeUtil.getFileSize(new File(MyApplication.getInstance().mAppDir));
        mCacheTv.setText(GetFileSizeUtil.formatFileSize(cacheSize));
        TextView tv_language_scan = findViewById(R.id.tv_language_scan);
        tv_language_scan.setText(SwitchLanguage.getLanguageFullnameMap().get(LocaleHelper.getLanguage(this)));
        TextView tv_skin_scan = findViewById(R.id.tv_skin_scan);
        tv_skin_scan.setText(SkinUtils.getSkin(this).getColorName());
        findViewById(R.id.account_rl).setOnClickListener(this);
        findViewById(R.id.clear_cache_rl).setOnClickListener(this);
        findViewById(R.id.rl_cencel_chat).setOnClickListener(this);
        findViewById(R.id.rl_backup_chat).setOnClickListener(this);
        findViewById(R.id.change_password_rl).setOnClickListener(this);
        findViewById(R.id.switch_language).setOnClickListener(this);
        findViewById(R.id.skin_rl).setOnClickListener(this);
        findViewById(R.id.chat_font_size_rl).setOnClickListener(this);
        findViewById(R.id.send_gMessage_rl).setOnClickListener(this);
        findViewById(R.id.privacy_settting_rl).setOnClickListener(this);
        findViewById(R.id.black).setOnClickListener(this);
        findViewById(R.id.secure_setting_rl).setOnClickListener(this);
        findViewById(R.id.bind_account_rl).setOnClickListener(this);
        if (coreManager.getConfig().thirdLogin) {
            findViewById(R.id.bind_account_rl).setOnClickListener(this);
        } else {
            findViewById(R.id.bind_account_rl).setVisibility(View.GONE);
        }
        findViewById(R.id.tuisongmsg).setOnClickListener(this);
        findViewById(R.id.about_us_rl).setOnClickListener(this);
        if (TextUtils.isEmpty(BuildConfig.COMPANY_NAME)) {
            findViewById(R.id.about_us_rl).setVisibility(View.GONE);
        }
        Button mExitBtn = findViewById(R.id.exit_btn);
        ButtonColorChange.colorChange(mContext, mExitBtn);
        mExitBtn.setOnClickListener(v -> showExitDialog());

        List<IntentWrapper> intentWrapperList = IntentWrapper.getWhiteListMatters(this, "");
        if (intentWrapperList.size() == 0) {
            findViewById(R.id.tuisongmsg).setVisibility(View.GONE);
        }
        findViewById(R.id.disturb_setting_rl).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!UiUtils.isNormalClick(v)) {
            return;
        }
        switch (v.getId()) {
            case R.id.account_rl:
                AccountSwitchActivity.start(mContext);
                break;
            case R.id.clear_cache_rl:
                // 清除缓存
                clearCache();
                break;
            case R.id.rl_cencel_chat:
                SelectionFrame selectionFrame = new SelectionFrame(this);
                selectionFrame.setSomething(null, getString(R.string.is_empty_all_chat), new SelectionFrame.OnSelectionFrameClickListener() {
                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        emptyServerMessage();

                        // 清除所有聊天记录
                        delAllChatRecord();
                    }
                });
                selectionFrame.show();
                break;
            case R.id.rl_backup_chat:
                BackupHistoryActivity.start(this);
                break;
            case R.id.change_password_rl:
                // 修改密码
                if (coreManager.isBindTelephone()) {
                    checkUserExistPassword();
                } else {
                    DialogHelper.tipDialog(mContext, getString(R.string.band_phone_start));
                }
                break;
            case R.id.switch_language:
                // 切换语言
                startActivity(new Intent(this, SwitchLanguage.class));
                break;
            case R.id.skin_rl:
                // 更换皮肤
                startActivity(new Intent(this, SkinStore.class));
                break;
            case R.id.chat_font_size_rl:
                // 更换聊天字体
                startActivity(new Intent(this, FontSizeActivity.class));
                break;
            case R.id.send_gMessage_rl:
                // 群发消息
                startActivity(new Intent(this, SelectFriendsActivity.class));
                break;
            case R.id.privacy_settting_rl:
                // 开启验证
                startActivity(new Intent(mContext, PrivacySettingActivity.class));
                break;
            case R.id.black:
                // 黑名单
                startActivity(new Intent(mContext, BlackActivity.class));
                break;
            case R.id.secure_setting_rl:
                // 安全设置，
                startActivity(new Intent(mContext, SecureSettingActivity.class));
                break;
            case R.id.bind_account_rl:
                // 绑定第三方
                startActivity(new Intent(mContext, BandAccountActivity.class));
                break;
            case R.id.tuisongmsg:
                IntentWrapper.whiteListMatters(this, "");
                break;
            case R.id.about_us_rl:
                // 关于我们
                startActivity(new Intent(mContext, AboutActivity.class));
                break;
            case R.id.disturb_setting_rl:
                // 关于我们
                startActivity(new Intent(mContext, DisturbSettingActivity.class));
                break;
        }
    }

    private void checkUserExistPassword() {
        Map<String, String> params = new HashMap<>();

        HttpUtils.get().url(coreManager.getConfig().USER_CHECK_EXIST_PASSWORD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            startActivity(new Intent(mContext, ChangePasswordActivity.class));
                        } else if (result != null && result.getResultCode() == 0) {
                            // 没有原始密码
                            SelectionFrame selectionFrame = new SelectionFrame(mContext);
                            selectionFrame.setSomething(null, getString(R.string.tip_no_password_go_reset_password),
                                    new SelectionFrame.OnSelectionFrameClickListener() {
                                        @Override
                                        public void cancelClick() {

                                        }

                                        @Override
                                        public void confirmClick() {
                                            // 退出登录，并跳转至重置密码界面
                                            exit(true);
                                        }
                                    });
                            selectionFrame.show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 清楚缓存
     */
    private void clearCache() {
        String filePath = MyApplication.getInstance().mAppDir;
        new ClearCacheAsyncTaska(filePath).execute(true);
    }

    // 服务器上所有的单人聊天记录也需要删除
    private void emptyServerMessage() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", String.valueOf(1));// 0 清空单人 1 清空所有

        HttpUtils.get().url(coreManager.getConfig().EMPTY_SERVER_MESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    /**
     * 清空所有聊天记录
     */
    private void delAllChatRecord() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        AsyncUtils.doAsync(this, settingActivityAsyncContext -> {
            // 不需要查询出所有好友，只需查询出最近聊天的好友即可
/*
            List<Friend> mAllFriend = new ArrayList<>();
            // 我的设备
            List<Friend> allDevices = FriendDao.getInstance().getDevice(mLoginUserId);
            mAllFriend.addAll(allDevices);
            // 公众号
            List<Friend> allSystems = FriendDao.getInstance().getAllSystems(mLoginUserId);
            mAllFriend.addAll(allSystems);
            // 我的好友
            List<Friend> allFriends = FriendDao.getInstance().getAllFriends(mLoginUserId);
            mAllFriend.addAll(allFriends);
            // 我的群组
            List<Friend> allRooms = FriendDao.getInstance().getAllRooms(mLoginUserId);
            mAllFriend.addAll(allRooms);
*/
            List<Friend> mNearChatFriendList = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
            for (int i = 0; i < mNearChatFriendList.size(); i++) {
                FriendDao.getInstance().resetFriendMessage(mLoginUserId, mNearChatFriendList.get(i).getUserId());
                ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, mNearChatFriendList.get(i).getUserId());
            }

            MsgRoamTaskDao.getInstance().deleteAllMsgRoamTasks(mLoginUserId);

            runOnUiThread(() -> {
                // 更新消息界面
                DialogHelper.dismissProgressDialog();
                MsgBroadcast.broadcastMsgUiUpdate(SettingActivity.this);
                MsgBroadcast.broadcastMsgNumReset(SettingActivity.this);
                ToastUtil.showToast(SettingActivity.this, getString(R.string.delete_success));
            });
        });
    }

    // 退出当前账号
    private void showExitDialog() {
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(null, getString(R.string.sure_exit_account), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                exit(false);
            }
        });
        mSF.show();
    }

    private void exit(boolean isResetPassword) {
        stopService(new Intent(mContext, WindowShowService.class));
        // 不退出以便于账号切换回来自动登录，
        logout();
        UserSp.getInstance(mContext).clearUserInfo();
        MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
        coreManager.logout();
        LoginHelper.broadcastLogout(mContext);
        if (isResetPassword) {
            startActivity(new Intent(mContext, FindPwdActivity.class));
        } else {
            startActivity(new Intent(mContext, LoginActivity.class));
        }
        finish();
    }

    private void logout() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("deviceKey", "android");

        HttpUtils.get().url(coreManager.getConfig().USER_LOGOUT_DELETE_PUT)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    private class ClearCacheAsyncTaska extends AsyncTask<Boolean, String, Integer> {

        private File rootFile;
        private ProgressDialog progressDialog;

        private int filesNumber = 0;
        private boolean canceled = false;
        private long notifyTime = 0;

        public ClearCacheAsyncTaska(String filePath) {
            this.rootFile = new File(filePath);
        }

        @Override
        protected void onPreExecute() {
            filesNumber = GetFileSizeUtil.getFolderSubFilesNumber(rootFile);
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(getString(R.string.deleteing));
            progressDialog.setMax(filesNumber);
            progressDialog.setProgress(0);
            // 设置取消按钮
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int i) {
                    canceled = true;
                }
            });
            progressDialog.show();
        }

        /**
         * 返回true代表删除完成，false表示取消了删除
         */
        @Override
        protected Integer doInBackground(Boolean... params) {
            if (filesNumber == 0) {
                return 0;
            }
            // 是否删除已清空的子文件夹
            boolean deleteSubFolder = params[0];
            return deleteFolder(rootFile, true, deleteSubFolder, 0);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // String filePath = values[0];
            int progress = Integer.parseInt(values[1]);
            // progressDialog.setMessage(filePath);
            progressDialog.setProgress(progress);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            if (!canceled && result == filesNumber) {
                ToastUtil.showToast(mContext, R.string.clear_completed);
            }
            long cacheSize = GetFileSizeUtil.getFileSize(rootFile);
            mCacheTv.setText(GetFileSizeUtil.formatFileSize(cacheSize));
        }

        /**
         * 是否删除完毕
         *
         * @param file
         * @param deleteSubFolder
         * @return
         */
        private int deleteFolder(File file, boolean rootFolder, boolean deleteSubFolder, int progress) {
            if (file == null || !file.exists() || !file.isDirectory()) {
                return 0;
            }
            File flist[] = file.listFiles();
            for (File subFile : flist) {
                if (canceled) {
                    return progress;
                }
                if (subFile.isFile()) {
                    subFile.delete();
                    progress++;
                    long current = System.currentTimeMillis();
                    if (current - notifyTime > 200) {// 200毫秒更新一次界面
                        notifyTime = current;
                        publishProgress(subFile.getAbsolutePath(), String.valueOf(progress));
                    }
                } else {
                    progress = deleteFolder(subFile, false, deleteSubFolder, progress);
                    if (deleteSubFolder) {
                        subFile.delete();
                    }
                }
            }
            return progress;
        }
    }

    private class My_BroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                if (action.equals(com.tongxin.caihong.broadcast.OtherBroadcast.SEND_MULTI_NOTIFY)) {// 群发消息结束，关闭当前界面
                    finish();
                } else if (action.equals(LoginHelper.ACTION_LOGOUT)) {// 退出当前登录了，
                    finish();
                } else if (action.equals(com.tongxin.caihong.broadcast.OtherBroadcast.NO_EXECUTABLE_INTENT)) {// 无可执行的intent 需提醒用户
                    DialogHelper.tip(SettingActivity.this, getString(R.string.no_executable_intent));
                }
            }
        }
    }
}
