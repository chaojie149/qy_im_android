package com.tongxin.caihong.ui.mucfile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.broadcast.OtherBroadcast;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.ui.mucfile.bean.DownBean;
import com.tongxin.caihong.ui.mucfile.bean.MucFileBean;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.ui.tool.WebViewActivity;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.video.PlayVideoActivity;
import com.daimajia.numberprogressbar.NumberProgressBar;

import java.io.File;
import java.util.UUID;

import static com.tongxin.caihong.ui.mucfile.DownManager.STATE_DOWNLOADED;
import static com.tongxin.caihong.ui.mucfile.DownManager.STATE_DOWNLOADFAILED;
import static com.tongxin.caihong.ui.mucfile.DownManager.STATE_UNDOWNLOAD;
import static com.tongxin.caihong.ui.mucfile.DownManager.STATE_WAITINGDOWNLOAD;
import static com.tongxin.caihong.ui.tool.WebViewActivity.EXTRA_URL;

/**
 * Created by Administrator on 2017/7/4.
 */
public class MucFileDetails extends BaseActivity implements DownManager.DownLoadObserver, View.OnClickListener {
    private ImageView ivInco;
    private TextView tvName;
    private TextView tvType;
    private Button btnStart;
    private RelativeLayout rlProgress;
    private NumberProgressBar progressPar;
    private TextView tvSize;
    private MucFileBean data;
    private String msgId;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), OtherBroadcast.MSG_BACK)) {
                String packetId = intent.getStringExtra("packetId");
                if (TextUtils.equals(packetId, msgId)) {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muc_dateils);
        data = (MucFileBean) getIntent().getSerializableExtra("data");
        msgId = getIntent().getStringExtra(AppConstant.EXTRA_MSG_ID);
        if (TextUtils.isEmpty(data.getUrl())) {
            ToastUtil.showToast(mContext, R.string.data_exception);
            finish();
            return;
        }
        initActionBar();
        initView();
        initDatas();
        IntentFilter filter = new IntentFilter();
        filter.addAction(OtherBroadcast.MSG_BACK);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownManager.instance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownManager.instance().deleteObserver(MucFileDetails.this);
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            // receiver可能未注册，activity就直接finish了，兼容一下
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());

        ImageView ivRight = findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.mipmap.share_icon);
        ivRight.setOnClickListener(v -> {
            shareFile();
        });

        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.detail));
    }

    private void initView() {
        ivInco = (ImageView) findViewById(R.id.item_file_inco);
        tvName = (TextView) findViewById(R.id.item_file_name);
        tvType = (TextView) findViewById(R.id.item_file_type);
        btnStart = (Button) findViewById(R.id.btn_muc_down);
        ButtonColorChange.colorChange(this, btnStart);
        rlProgress = (RelativeLayout) findViewById(R.id.muc_dateils_rl_pro);
        progressPar = (NumberProgressBar) findViewById(R.id.number_progress_bar);
        tvSize = (TextView) findViewById(R.id.muc_dateils_size);
        btnStart.setOnClickListener(this);

        findViewById(R.id.muc_dateils_stop).setOnClickListener(v -> stop());

        tvType.setOnClickListener(v -> {
            if (data.getState() == DownManager.STATE_DOWNLOADING) {
                DownManager.instance().pause(data);
            }

            if (data.getType() == 2) {
                // 音乐在线播放
                PlayVideoActivity.start(mContext, data.getUrl());
            } else if (data.getType() == 3) {
                // 视频在线播放
                PlayVideoActivity.start(mContext, data.getUrl());
            } else if (data.getType() == 4 || data.getType() == 5 || data.getType() == 6 || data.getType() == 10) {
                // 在线预览文档，需要解析，上面为微软的，下面为谷歌的(google无法访问)
/*
                String url = "https://view.officeapps.live.com/op/view.aspx?src=" + data.getUrl();
                //String url = "https://docs.google.com/viewer?url=" + data.getUrl();
                Intent intent = new Intent(MucFileDetails.this, WebViewActivity.class);
                intent.putExtra(EXTRA_URL, url);
                startActivity(intent);
*/
                ToastUtil.showToast(MucFileDetails.this, R.string.tip_preview_file_type_not_support);
            } else if (data.getType() == 8) {
                Intent intent = new Intent(this, WebViewActivity.class);
                intent.putExtra(EXTRA_URL, data.getUrl());
                startActivity(intent);
            } else {
                Intent intent = new Intent(MucFileDetails.this, MucFilePreviewActivity.class);
                intent.putExtra("data", data);
                startActivity(intent);
            }
        });
    }

    protected void initDatas() {
        Log.e(TAG, "initDatas: ");
        if (data != null) {
            updateUI();
        }
    }

    private void updateUI() {
        Log.e(TAG, "updateUI: ");
        if (data.getType() == 1) {
            // 图片直接显示
            ImageLoadHelper.showImageCenterCropWithSize(
                    MucFileDetails.this,
                    data.getUrl(),
                    100, 100,
                    ivInco
            );
        } else {
            // 加载本地
            XfileUtils.setFileInco(data.getType(), ivInco);
        }
        tvName.setText(data.getName());
        if (data.getType() == 9) {
            tvType.setText(getString(R.string.not_support_preview));
        } else if (data.getType() == 4 || data.getType() == 5 || data.getType() == 6 || data.getType() == 10 || data.getType() == 7 || data.getType() == 11) {
            tvType.setText(getString(R.string.not_support_preview));
        } else {
            SpannableString type = XfileUtils.matcherSearchTitle(Color.parseColor("#6699FF"),
                    getString(R.string.preview_online), getString(R.string.look_online));
            tvType.setText(type);
        }
        DownBean downBean = DownManager.instance().getDownloadState(data);
        // 兼容FileViewHolder跳转至此的逻辑
        if (!TextUtils.isEmpty(data.getNickname())
                && FileUtil.isExist(data.getNickname())) {
            downBean.state = STATE_DOWNLOADED;
        }
        onDownLoadInfoChange(downBean);
    }

    @Override
    public void onClick(View v) {
        if (checkNet()) {
            // 检查网络
            switch (data.getState()) {
                case STATE_DOWNLOADED:
                    open();
                    break;
                case STATE_UNDOWNLOAD:
                    down();
                    break;
                case DownManager.STATE_DOWNLOADING:
                    stop();
                    break;
                case DownManager.STATE_PAUSEDOWNLOAD:
                    start();
                    break;
                case STATE_WAITINGDOWNLOAD:
                    cancelDown();
                    break;
                case STATE_DOWNLOADFAILED:
                    down();
                    break;
            }
        }
    }

    /**
     * 发送给朋友
     */
    private void shareFile() {
        String mLoginUserId = coreManager.getSelf().getUserId();
        if (TextUtils.isEmpty(data.getUrl()) || !data.getUrl().contains("http")) {
            // 兼容FileViewHolder跳转至此的逻辑
            // 如正在上传文件时进入，url只是本地路径
            ToastUtil.showToast(mContext, R.string.wait_file_upload_success);
            return;
        }
        // todo

        ChatMessage message = new ChatMessage();
        message.setType(fileType2XmppType(data.getType()));
        message.setContent(data.getUrl());
        message.setFileSize((int) data.getSize());
        if (!TextUtils.isEmpty(data.getNickname())
                && FileUtil.isExist(data.getNickname())) {
            // 兼容FileViewHolder跳转至此的逻辑
            message.setFilePath(data.getNickname());
        } else {
            message.setFilePath(data.getName());
        }
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());

        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, AppConstant.NORMAL_INSTANT_ID, message)) {
            InstantMessageActivity.start(mContext, AppConstant.NORMAL_INSTANT_ID, message.getPacketId(), false);
            finish();
        } else {
            Toast.makeText(mContext, getString(R.string.tip_message_wrap_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private int fileType2XmppType(int type) {
        if (type == 1) {
            return XmppMessage.TYPE_IMAGE;
        } else if (type == 3) {
            return XmppMessage.TYPE_VIDEO;
        }
        return XmppMessage.TYPE_FILE;
    }

    private void open() {
        File file;
        // 兼容FileViewHolder跳转至此的逻辑
        if (!TextUtils.isEmpty(data.getNickname())
                && FileUtil.isExist(data.getNickname())) {
            file = new File(data.getNickname());
        } else {
            file = new File(DownManager.instance().getFileDir(), data.getName());
        }
        FileOpenWays openWays = new FileOpenWays(mContext);
        openWays.openFiles(file.getAbsolutePath());
    }

    private void del() {
        DownManager.instance().detele(data);
    }

    private void cancelDown() {
        DownManager.instance().cancel(data);
    }

    private void stop() {
        DownManager.instance().pause(data);
    }

    private void down() {
        DownManager.instance().download(data);
    }

    private void start() {
        DownManager.instance().download(data);
    }

    private boolean checkNet() {
        return true;
    }

    @Override
    public void onDownLoadInfoChange(final DownBean info) {
        if (!TextUtils.equals(info.url, data.getUrl())) {
            // 不是当前的下载文件，
            return;
        }
        data.setState(info.state);

        int progress = (int) (info.cur / (float) info.max * 100);
        progressPar.setProgress(progress);

        rlProgress.setVisibility(View.VISIBLE);

        switch (info.state) {
            case STATE_DOWNLOADED:
                tvType.setText(getString(R.string.download_complete));
                btnStart.setText(getString(R.string.open));
                rlProgress.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                break;
            case STATE_UNDOWNLOAD:
                tvSize.setText(getString(R.string.not_downloaded));
                btnStart.setText(getString(R.string.download) + "(" + XfileUtils.fromatSize(info.max) + ")");
                rlProgress.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                break;
            case DownManager.STATE_DOWNLOADING:
                tvSize.setText(getString(R.string.downloading) + "…(" + XfileUtils.fromatSize(info.cur) + "/" + XfileUtils.fromatSize(info.max) + ")");
                btnStart.setVisibility(View.GONE);
                rlProgress.setVisibility(View.VISIBLE);
                break;
            case DownManager.STATE_PAUSEDOWNLOAD:
                rlProgress.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                tvSize.setText(getString(R.string.in_pause) + "…(" + XfileUtils.fromatSize(info.cur) + "/" + XfileUtils.fromatSize(info.max) + ")");
                btnStart.setText(getString(R.string.continue_downloading) + "(" + XfileUtils.fromatSize((info.max - info.cur)) + ")");
                break;
            case STATE_WAITINGDOWNLOAD:
                break;
            case STATE_DOWNLOADFAILED:
                tvType.setText(getString(R.string.download_error));
                rlProgress.setVisibility(View.GONE);
                tvSize.setText(getString(R.string.redownload));
                btnStart.setVisibility(View.VISIBLE);
                break;
        }
    }
}
