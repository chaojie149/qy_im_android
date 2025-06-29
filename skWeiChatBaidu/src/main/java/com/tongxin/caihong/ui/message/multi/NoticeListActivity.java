package com.tongxin.caihong.ui.message.multi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.NoticeId;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.util.ViewHolder;
import com.tongxin.caihong.view.PullToRefreshSlideListView;
import com.tongxin.caihong.view.TipDialog;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 群公告列表
 */
public class NoticeListActivity extends BaseActivity {
    public static final int PROCLAMATION = 0x1118;
    private PullToRefreshSlideListView mListView;
    private NoticeAdapter mNoticeAdapter;
    private List<MucRoom.Notice> mNoticeList = new ArrayList<>();
    private int mRole;
    private String mRoomId;
    // 回到群组信息界面是否需要刷新ui
    private boolean isNeedUpdate;

    public static void start(Context context, int mRole, String mRoomId) {
        Intent intent = new Intent(context, NoticeListActivity.class);
        intent.putExtra("mRole", mRole);
        intent.putExtra("mRoomId", mRoomId);
        context.startActivity(intent);
    }

    public static void start(Activity activity, int mRole, String mRoomId) {
        Intent intent = new Intent(activity, NoticeListActivity.class);
        intent.putExtra("mRole", mRole);
        intent.putExtra("mRoomId", mRoomId);
        activity.startActivityForResult(intent, RoomInfoActivity.RESULT_FOR_MODIFY_NOTICE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_list);
        mRole = getIntent().getIntExtra("mRole", 3);
        mRoomId = getIntent().getStringExtra("mRoomId");
        initActionBar();
        initView();
        loadNotices();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra("isNeedUpdate", isNeedUpdate);
        setResult(RESULT_OK, intent);
        super.finish();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.group_bulletin);
    }

    private void initView() {
        mListView = findViewById(R.id.pull_refresh_list);
        mListView.setMode(PullToRefreshBase.Mode.DISABLED);
        mNoticeAdapter = new NoticeAdapter(this);
        mListView.setAdapter(mNoticeAdapter);
        ButtonColorChange.colorChange(mContext, findViewById(R.id.btnPublish));
        findViewById(R.id.btnPublish).setVisibility((mRole == 1 || mRole == 2) ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnPublish).setOnClickListener(view -> ProclamationActivity.start(NoticeListActivity.this, "", ""));
    }

    private void loadNotices() {
        HashMap<String, String> params = new HashMap<>();
        params.put("roomId", mRoomId);
        params.put("pageSize", String.valueOf(10));

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {
                                 if (Result.checkSuccess(mContext, result)) {
                                     if (result.getData().getNotices() != null) {
                                         mNoticeList = result.getData().getNotices();
                                         if (mNoticeList.size() == 0) {
                                             findViewById(R.id.empty).setVisibility(View.VISIBLE);
                                         } else {
                                             Collections.reverse(mNoticeList);
                                             mNoticeAdapter.notifyDataSetChanged();
                                         }
                                     }
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {

                             }
                         }
                );
    }

    private void updateNotice(final String text) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("notice", text);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.post().url(coreManager.getConfig().ROOM_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<NoticeId>(NoticeId.class) {

                    @Override
                    public void onResponse(ObjectResult<NoticeId> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            isNeedUpdate = true;
                            MucRoom.Notice notice = new MucRoom.Notice();
                            notice.setUserId(coreManager.getSelf().getUserId());
                            notice.setNickname(coreManager.getSelf().getNickName());
                            notice.setTime(TimeUtils.sk_time_current_time());
                            notice.setText(text);
                            if (result.getData() != null
                                    && !TextUtils.isEmpty(result.getData().getNoticeId())) {
                                notice.setId(result.getData().getNoticeId());
                                PreferenceUtils.putString(NoticeListActivity.this, notice.getId(), text);
                            }
                            mNoticeList.add(notice);
                            mNoticeAdapter.notifyDataSetChanged();
                            findViewById(R.id.empty).setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void editNotice(String noticeId, String NewNotice) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("noticeId", noticeId);
        params.put("noticeContent", NewNotice);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.post().url(coreManager.getConfig().ROOM_EDIT_NOTICE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            isNeedUpdate = true;
                            for (int i = 0; i < mNoticeList.size(); i++) {
                                if (mNoticeList.get(i).getId().equals(noticeId)) {
                                    mNoticeList.get(i).setText(NewNotice);
                                }
                            }
                            mNoticeAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void deleteNotice(final MucRoom.Notice notice) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mRoomId);
        params.put("noticeId", notice.getId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().ROOM_DELETE_NOTICE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            isNeedUpdate = true;
                            mNoticeList.remove(notice);
                            mNoticeAdapter.notifyDataSetChanged();

                            if (mNoticeList.size() <= 0) {
                                findViewById(R.id.empty).setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROCLAMATION && resultCode == RESULT_OK
                && data != null) {
            String id = data.getStringExtra("noticeId");
            String text = data.getStringExtra("text");
/*
            if (TextUtils.isEmpty(id)) {
                updateNotice(text);
            } else {
                editNotice(id, text);
            }
*/
            // 仿QQ，编辑公告相当于发布一条新公告
            updateNotice(text);
        }
    }

    class NoticeAdapter extends SlideBaseAdapter {

        public NoticeAdapter(Context context) {
            super(context);
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.row_notice;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.row_item_delete_style;
        }

        @Override
        public int getCount() {
            if (mNoticeList != null) {
                return mNoticeList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mNoticeList != null) {
                return mNoticeList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            ImageView ivAvatar = ViewHolder.get(convertView, R.id.ivAvatar);
            TextView tvName = ViewHolder.get(convertView, R.id.tvName);
            TextView tvTime = ViewHolder.get(convertView, R.id.tvTime);
            TextView tvContent = ViewHolder.get(convertView, R.id.tvContent);
            ImageView ivEdit = ViewHolder.get(convertView, R.id.ivEdit);
            ImageView ivDelete = ViewHolder.get(convertView, R.id.ivDelete);

            if (mNoticeList.size() > 0) {
                final MucRoom.Notice mNotice = mNoticeList.get(mNoticeList.size() - 1 - position);// 根据时间倒序显示

                // 仅最新的公告显示编辑
                // mEdit.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                // 仿QQ，编辑公告相当于发布一条新公告，去掉限制

                if (mNotice != null) {
                    AvatarHelper.getInstance().displayAvatar(mNotice.getNickname(), mNotice.getUserId(), ivAvatar, true);
                    tvName.setText(mNotice.getNickname());
                    tvTime.setText(TimeUtils.getFriendlyTimeDesc(NoticeListActivity.this, mNotice.getTime()));
                    tvContent.setText(mNotice.getText());
                    ivEdit.setOnClickListener(v -> {
                        if (mRole == 1 || mRole == 2) {
                            ProclamationActivity.start(NoticeListActivity.this, mNotice.getId(), mNotice.getText());
                            notifyDataSetChanged();
                        } else {
                            TipDialog tipDialog = new TipDialog(NoticeListActivity.this);
                            tipDialog.setTip(getString(R.string.tip_cannot_edit_bulletin));
                            tipDialog.show();
                        }
                    });
                    ivDelete.setOnClickListener(v -> {
                        if (mRole == 1 || mRole == 2) {
                            deleteNotice(mNotice);
                        } else {
                            TipDialog tipDialog = new TipDialog(NoticeListActivity.this);
                            tipDialog.setTip(getString(R.string.tip_cannot_remove_bulletin));
                            tipDialog.show();
                        }
                    });
                }
            }
            return convertView;
        }
    }
}
