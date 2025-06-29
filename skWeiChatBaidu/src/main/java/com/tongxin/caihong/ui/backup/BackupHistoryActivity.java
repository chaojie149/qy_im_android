package com.tongxin.caihong.ui.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.event.EventSentChatHistory;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.EventBusHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.secure.LoginPassword;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.db.dao.MsgRoamTaskDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.VerifyDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

public class BackupHistoryActivity extends BaseActivity implements View.OnClickListener {

    private int vTopRightClickCount, vBottomLeftClickCount, vBottomRightClickCount;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, BackupHistoryActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_history);

        initActionBar();
        initView();

        Button btnSelectChat = findViewById(R.id.btnSelectChat);
        btnSelectChat.setOnClickListener((v) -> {
            SelectChatActivity.start(this);
        });
        ButtonColorChange.colorChange(this, btnSelectChat);
        EventBusHelper.register(this);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener((v) -> {
            onBackPressed();
        });
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.backup_chat_history));
    }

    private void initView() {
        findViewById(R.id.vTopRight).setVisibility(View.VISIBLE);
        ButtonColorChange.colorChange(this, findViewById(R.id.btnCleanEncryptChatUser));
        ButtonColorChange.colorChange(this, findViewById(R.id.btnCleanEncryptChatGroup));

        findViewById(R.id.vTopRight).setOnClickListener(this);
        findViewById(R.id.vBottomLeft).setOnClickListener(this);
        findViewById(R.id.vBottomRight).setOnClickListener(this);
        findViewById(R.id.btnCleanEncryptChatUser).setOnClickListener(this);
        findViewById(R.id.btnCleanEncryptChatGroup).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.vTopRight:
                vTopRightClickCount++;
                check();
                break;
            case R.id.vBottomLeft:
                vBottomLeftClickCount++;
                check();
                break;
            case R.id.vBottomRight:
                vBottomRightClickCount++;
                check();
                break;
            case R.id.btnCleanEncryptChatUser:
                cleanEncryptChatUser();
                break;
            case R.id.btnCleanEncryptChatGroup:
                cleanEncryptChatGroup();
                break;
        }
    }

    private void check() {
        if (vTopRightClickCount >= 2
                && vBottomLeftClickCount >= 2
                && vBottomRightClickCount >= 2) {
            Map<String, String> params = new HashMap<>();
            HttpUtils.get().url(coreManager.getConfig().USER_CHECK_PERMISSION)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {
                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            if (result.getResultCode() == 1) {
                                findViewById(R.id.btnCleanEncryptChatUser).setVisibility(View.VISIBLE);
                                findViewById(R.id.btnCleanEncryptChatGroup).setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                        }
                    });
        }
    }

    private void clean(String id) {
        FriendDao.getInstance().resetFriendMessage(coreManager.getSelf().getUserId(), id);
        ChatMessageDao.getInstance().deleteMessageTable(coreManager.getSelf().getUserId(), id);
        MsgRoamTaskDao.getInstance().deleteFriendMsgRoamTaskList(coreManager.getSelf().getUserId(), id);
        Intent intent = new Intent(Constants.CHAT_HISTORY_EMPTY);
        intent.putExtra("id", id);
        sendBroadcast(intent);
    }

    private void cleanEncryptChatUser() {
        VerifyDialog verifyDialog = new VerifyDialog(mContext);
        verifyDialog.setVerifyClickListener(getString(R.string.app_name)
                , "请输入登录密码用于校验身份"
                , ""
                , new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {
                        verifyDialog.dismiss();
                    }

                    @Override
                    public void send(String str) {
                        Map<String, String> params = new HashMap<>();
                        params.put("password", LoginPassword.encodeMd5(str));
                        HttpUtils.get().url(coreManager.getConfig().CLEAN_ENCRYPT_CHAT_USER)
                                .params(params)
                                .build()
                                .execute(new BaseCallback<Void>(Void.class) {
                                    @Override
                                    public void onResponse(ObjectResult<Void> result) {
                                        if (Result.checkSuccess(mContext, result)) {
                                            ToastUtil.showToast(mContext, getString(R.string.success));
                                            List<Friend> allEncryptChatUser = FriendDao.getInstance().getAllEncryptChatUser(coreManager.getSelf().getUserId());
                                            if (allEncryptChatUser != null) {
                                                for (Friend friend : allEncryptChatUser) {
                                                    clean(friend.getUserId());
                                                }
                                                MsgBroadcast.broadcastMsgUiUpdate(mContext);
                                                MsgBroadcast.broadcastMsgNumReset(mContext);

                                                cleanChatColleague();
                                            }

                                        }
                                    }

                                    @Override
                                    public void onError(Call call, Exception e) {
                                        ToastUtil.showErrorNet(mContext);
                                    }
                                });
                    }
                });
        verifyDialog.show();
    }

    private void cleanChatColleague() {
        Map<String, String> params = new HashMap<>();
        HttpUtils.get().url(coreManager.getConfig().GET_EMPLOYEE_ID_LIST)
                .params(params)
                .build().execute(new ListCallback<String>(String.class) {
            @Override
            public void onResponse(ArrayResult<String> result) {
                if (Result.checkSuccess(mContext, result)) {
                    List<String> idList = result.getData();
                    if (idList != null) {
                        for (String id : idList) {
                            if (!TextUtils.equals(coreManager.getSelf().getUserId(), id)) {
                                if (FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), id) != null) {
                                    clean(id);
                                }
                            }
                        }
                        MsgBroadcast.broadcastMsgUiUpdate(mContext);
                        MsgBroadcast.broadcastMsgNumReset(mContext);
                    }
                }
            }

            @Override
            public void onError(Call call, Exception e) {
                ToastUtil.showErrorNet(mContext);
            }
        });
    }

    private void cleanEncryptChatGroup() {
        VerifyDialog verifyDialog = new VerifyDialog(mContext);
        verifyDialog.setVerifyClickListener(getString(R.string.app_name)
                , "请输入登录密码用于校验身份"
                , ""
                , new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {
                        verifyDialog.dismiss();
                    }

                    @Override
                    public void send(String str) {
                        Map<String, String> params = new HashMap<>();
                        params.put("password", LoginPassword.encodeMd5(str));
                        HttpUtils.get().url(coreManager.getConfig().CLEAN_ENCRYPT_CHAT_GROUP)
                                .params(params)
                                .build()
                                .execute(new BaseCallback<Void>(Void.class) {
                                    @Override
                                    public void onResponse(ObjectResult<Void> result) {
                                        if (Result.checkSuccess(mContext, result)) {
                                            ToastUtil.showToast(mContext, getString(R.string.success));
                                            List<Friend> allEncryptChatGroup = FriendDao.getInstance().getAllEncryptChatGroup(coreManager.getSelf().getUserId());
                                            if (allEncryptChatGroup != null) {
                                                for (Friend friend : allEncryptChatGroup) {
                                                    clean(friend.getUserId());
                                                }
                                                MsgBroadcast.broadcastMsgUiUpdate(mContext);
                                                MsgBroadcast.broadcastMsgNumReset(mContext);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onError(Call call, Exception e) {
                                        ToastUtil.showErrorNet(mContext);
                                    }
                                });
                    }
                });
        verifyDialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventSentChatHistory message) {
        finish();
    }
}
