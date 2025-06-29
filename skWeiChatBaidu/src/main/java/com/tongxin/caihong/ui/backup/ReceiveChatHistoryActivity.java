package com.tongxin.caihong.ui.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.broadcast.MsgBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.AsyncUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.TipDialog;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

import okhttp3.HttpUrl;

public class ReceiveChatHistoryActivity extends BaseActivity {
    public static final String QR_CODE_ACTION_SEND_CHAT_HISTORY = "sendChatHistory";
    private String ip;
    private int port;
    @Nullable
    private Socket socket;

    public static void start(Context ctx, String qrCodeResult) {
        Intent intent = new Intent(ctx, ReceiveChatHistoryActivity.class);
        intent.putExtra("qrCodeResult", qrCodeResult);
        ctx.startActivity(intent);
    }

    /**
     * 检查这个二维码是不是用于接收聊天记录的，
     */
    public static boolean checkQrCode(String qrCodeResult) {
        return qrCodeResult.contains("action=" + QR_CODE_ACTION_SEND_CHAT_HISTORY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_chat_history);

        initActionBar();

        String qrCodeResult = getIntent().getStringExtra("qrCodeResult");
        Log.i(TAG, "onCreate: qrCodeResult" + qrCodeResult);
        if (TextUtils.isEmpty(qrCodeResult)) {
            // 按理说不会为空，但bugly#352251确实有收到这个空的反馈，可能某种情况恢复activity丢失了intent?
            finish();
            return;
        }
        if (Objects.requireNonNull(qrCodeResult).startsWith("?")) {
            // 二维码不是个合法的地址，可能是因为后台website没配置，这里补个默认值，
            qrCodeResult = coreManager.getConfig().website + qrCodeResult;
        }
        HttpUrl httpUrl = HttpUrl.parse(qrCodeResult);
        if (!checkUserId(httpUrl)) {
            ToastUtil.showToast(this, R.string.tip_migrate_chat_history_wrong_user);
            finish();
            return;
        }
        ip = httpUrl.queryParameter("ip");
        port = Integer.parseInt(httpUrl.queryParameter("port"));

        AsyncUtils.doAsync(this, t -> {
            DialogHelper.dismissProgressDialog();
            String message = getString(R.string.tip_receive_chat_history_failed);
            if (!this.isFinishing()) {
                Reporter.post(message, t);
                runOnUiThread(() -> {
                    TipDialog dialog = new TipDialog(this);
                    dialog.setmConfirmOnClickListener(getString(R.string.tip_migrate_chat_history_failed), () -> {
                        finish();
                    });
                    dialog.show();
                });
            } else {
                // 可能是主动退出页面关闭了socket,
                Log.w(TAG, message, t);
            }
        }, c -> {
            try (Socket socket = new Socket(ip, port)) {
                this.socket = socket;
                try (DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
                    c.uiThread(r -> {
                        DialogHelper.showMessageProgressDialog(this, getString(R.string.tip_migrate_chat_history_sending));
                    });
                    String ownerId = null;
                    String friendId = null;
                    // 死循环读，读到结束会抛EOFException,
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        String line = readLine(input);
                        if (!line.startsWith("{")) {
                            String[] split = line.split(",");
                            ownerId = split[0];
                            friendId = split[1];
                            Log.i(TAG, "read: ownerId=" + ownerId + ", friendId=" + friendId);
                        } else {
                            ChatMessage chatMessage = new ChatMessage(line);
                            Log.i(TAG, "input chatMessage, fromUserName: " + chatMessage.getFromUserName() + ", content: " + chatMessage.getContent());
                            if (!TextUtils.isEmpty(chatMessage.getFromUserId())
                                    && chatMessage.getFromUserId().equals(coreManager.getSelf().getUserId())) {
                                chatMessage.setMySend(true);
                            }
                            chatMessage.setSendRead(true);// 漫游消息，默认为已读
                            // 漫游的默认已上传
                            chatMessage.setUpload(true);
                            chatMessage.setUploadSchedule(100);
                            chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
                            if (chatMessage.getIsEncrypt() == 1) {
                                ChatMessageDao.getInstance().decrypt(false, chatMessage);
                            }
                            ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, friendId, chatMessage);
                        }
                    }
                } catch (EOFException e) {
                    Log.i(TAG, "读取完成");
                    c.uiThread(r -> {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(this, getString(R.string.tip_receive_chat_history_finish));
                        // 刷新消息会话列表，
                        MsgBroadcast.broadcastMsgUiUpdate(mContext);
                        finish();
                    });
                }
            }
        });
    }

    private boolean checkUserId(HttpUrl httpUrl) {
        String userId = httpUrl.queryParameter("userId");
        return TextUtils.equals(userId, coreManager.getSelf().getUserId());
    }

    @Override
    protected void onDestroy() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "onDestroy: socket关闭失败", e);
            }
        }
        super.onDestroy();
    }


    private String readLine(DataInputStream input) throws IOException {
        int length = input.readInt();
        byte[] buf = new byte[length];
        input.readFully(buf);
        return new String(buf);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener((v) -> {
            onBackPressed();
        });
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.send_chat_history));
    }

}
