package com.tongxin.caihong.ui.me.collection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.tongxin.caihong.adapter.PublicMessageRecyclerAdapter;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.view.ClearEditText;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

public class CollectionSearchActivity extends BaseActivity {
    public static final int PAGE_SIZE = 10;
    public SwipeRecyclerView rc_collection;
    private PublicMessageRecyclerAdapter mPublicMessageAdapter;
    private List<PublicMessage> mPublicMessage = new ArrayList<>();
    private boolean isLoad;
    private List<CollectionEvery> data;
    private boolean more;
    private boolean enableScrollLoad = true;
    private SmartRefreshLayout srlSmartRefresh;
    private int pagerIndex;
    private ClearEditText search_edit;
    private String keyV;
    private boolean isSendCollection;

    public static void start(Activity context, int requestCode, boolean isSendCollection) {
        Intent intent = new Intent(context, CollectionSearchActivity.class);
        intent.putExtra("IS_SEND_COLLECTION", isSendCollection);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_search);
        if (getIntent() != null) {
            isSendCollection = getIntent().getBooleanExtra("IS_SEND_COLLECTION", false);
        }
        initView();
        initActionBar();
        addListener();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void initView() {
        srlSmartRefresh = findViewById(R.id.refreshLayout);
        srlSmartRefresh.setOnLoadMoreListener(refreshLayout -> {
            searchCollection(false);
        });

        rc_collection = findViewById(R.id.rc_collection);
        rc_collection.setLayoutManager(new LinearLayoutManager(this));
        mPublicMessageAdapter = new PublicMessageRecyclerAdapter(mContext, coreManager, mPublicMessage);
        if (isSendCollection) {
            mPublicMessageAdapter.setCollectionType(2);
        } else {
            mPublicMessageAdapter.setCollectionType(1);
        }
        mPublicMessageAdapter.setOnItemClickListener(vh -> {
            if (isSendCollection) {
                int position = vh.getAdapterPosition();
                CollectionEvery collection = data.get(position);
                if (collection != null) {
                    SelectionFrame dialog = new SelectionFrame(mContext);
                    dialog.setSomething(null, getString(R.string.tip_confirm_send), new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {
                        }

                        @Override
                        public void confirmClick() {
                            Intent intent = new Intent();
                            try {
                                intent.putExtra("data", JSON.toJSONString(collection));
                                setResult(RESULT_OK, intent);
                                finish();
                            } catch (JSONException e) {
                                ToastUtil.showToast(mContext, getString(R.string.parse_exception));
                                e.printStackTrace();
                            }
                        }
                    });
                    dialog.show();
                } else {
                    Reporter.unreachable();
                    ToastUtil.showToast(mContext, R.string.tip_server_error);
                }

            }
        });
        rc_collection.setAdapter(mPublicMessageAdapter);
        search_edit = findViewById(R.id.search_edit);

        search_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                keyV = s.toString();
                if (!TextUtils.isEmpty(keyV)) {
                    searchCollection(true);
                }
            }
        });
    }

    private void addListener() {
        rc_collection.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                //当前RecyclerView显示出来的最后一个的item的position
                int lastPosition = -1;

                //当前状态为停止滑动状态SCROLL_STATE_IDLE时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager instanceof LinearLayoutManager) {
                        lastPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                    }
                    // 时判断界面显示的最后item的position是否等于itemCount总数-1也就是最后一个item的position
                    // 如果相等则说明已经滑动到最后了
                    if (lastPosition == recyclerView.getLayoutManager().getItemCount() - 1) {
                        if (!isLoad && enableScrollLoad) {
                            pagerIndex++;
                            searchCollection(false);
                        } else {
                            refreshComplete();
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    private void refreshComplete() {
        rc_collection.postDelayed(new Runnable() {
            @Override
            public void run() {
                srlSmartRefresh.finishRefresh();
                srlSmartRefresh.finishLoadMore();
            }
        }, 200);
    }

    public void searchCollection(boolean clear) {
        isLoad = true;
        if (clear) {
            pagerIndex = 0;
            mPublicMessage.clear();
        }
        if (!more && !clear) {
            srlSmartRefresh.setNoMoreData(true);
            refreshComplete();
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        DialogHelper.showDefaulteMessageProgressDialog(mContext);
        params.put("pageSize", String.valueOf(10));
        params.put("pageIndex", String.valueOf(pagerIndex));
        params.put("userId", coreManager.getSelf().getUserId());
        params.put("keyword", keyV);

        HttpUtils.get().url(coreManager.getConfig().Collection_SEARCH)
                .params(params)
                .build()
                .execute(new ListCallback<CollectionEvery>(CollectionEvery.class) {
                    @Override
                    public void onResponse(ArrayResult<CollectionEvery> result) {
                        isLoad = false;
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            afterGetData(result.getData());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    /**
     * 坑太大，不应该和朋友圈共用adapter, 共用实体类，
     */
    public void afterGetData(List<CollectionEvery> data) {
        this.data = data;
        if (data.size() == 0) {
            findViewById(R.id.ll_not_data).setVisibility(View.VISIBLE);
            rc_collection.setVisibility(View.GONE);
        } else {
            findViewById(R.id.ll_not_data).setVisibility(View.GONE);
            rc_collection.setVisibility(View.VISIBLE);
        }
        if (data.size() > 0) {
            if (data.size() == PAGE_SIZE) {
                more = true;
                enableScrollLoad = true;
                srlSmartRefresh.resetNoMoreData();
            } else {
                more = false;
            }
        } else {
            more = false;
        }
        for (int i = 0; i < data.size(); i++) {
            CollectionEvery collection = data.get(i);
            PublicMessage publicMessage = new PublicMessage();
            publicMessage.setUserId(coreManager.getSelf().getUserId());
            publicMessage.setNickName(coreManager.getSelf().getNickName());
            // 显示的时间应该为收藏这条消息的时间，而不是发送这条消息的时间
            publicMessage.setTime(data.get(i).getCreateTime());
            // 播放语音需要用到messageId,否则不需要设置
            publicMessage.setMessageId(collection.getCollectMsgId());
            // 文件名，
            publicMessage.setFileName(collection.getFileName());
            publicMessage.setTargetType(collection.getTargetType());
            publicMessage.setTargetName(collection.getTargetName());
            String name = collection.getFileName();
            try {
                // 服务器给的文件名可能包含路径，
                // TODO: PC端可能给反斜杠\, 没测，
                int lastIndex = name.lastIndexOf('/');
                publicMessage.setFileName(name.substring(lastIndex + 1));
            } catch (Exception e) {
                publicMessage.setFileName(name);
            }
            // 我的收藏专属id
            publicMessage.setEmojiId(data.get(i).getEmojiId());

            PublicMessage.Body body = new PublicMessage.Body();
            // 朋友圈收藏来的所有消息类型都有collectContent文本内容字段，
            body.setText(collection.getCollectContent());
            if (collection.getType() == CollectionEvery.TYPE_TEXT) {
                // 文本
                body.setType(PublicMessage.TYPE_TEXT);
                // 聊天收藏来的文字消息没有collectCntent字段，
                // 聊天和朋友圈收藏来的文字消息都有msg字段，
                collection.setCollectContent(collection.getMsg());
                body.setText(collection.getCollectContent());
            } else if (collection.getType() == CollectionEvery.TYPE_IMAGE || collection.getType() == CollectionEvery.TYPE_IMAGE_SINGLE) {
                // 图片
                body.setType(PublicMessage.TYPE_IMG);
                if (collection.getType() == CollectionEvery.TYPE_IMAGE_SINGLE) {
                    body.setType(CollectionEvery.TYPE_IMAGE_SINGLE);
                }
                List<PublicMessage.Resource> images = new ArrayList<>();
                String allUrl = collection.getUrl();
                if (!TextUtils.isEmpty(allUrl)) {
                    for (String url : allUrl.split(",")) {
                        PublicMessage.Resource resource = new PublicMessage.Resource();
                        resource.setOriginalUrl(url);
                        images.add(resource);
                    }
                }
                body.setImages(images);
            } else if (collection.getType() == CollectionEvery.TYPE_VOICE) {
                // 语音
                body.setType(PublicMessage.TYPE_VOICE);
                List<PublicMessage.Resource> audios = new ArrayList<>();
                PublicMessage.Resource resource = new PublicMessage.Resource();
                resource.setLength(collection.getFileLength());
                resource.setSize(collection.getFileSize());
                resource.setOriginalUrl(collection.getUrl());
                audios.add(resource);
                body.setAudios(audios);
            } else if (collection.getType() == CollectionEvery.TYPE_VIDEO || collection.getType() == CollectionEvery.TYPE_VIDEO_SINGLE) {
                // 视频
                body.setType(PublicMessage.TYPE_VIDEO);
                // 视频封面
                /*List<PublicMessage.Resource> images = new ArrayList<>();
                PublicMessage.Resource resource1 = new PublicMessage.Resource();
                resource1.setOriginalUrl(message.getContent());
                images.add(resource1);
                body.setImages(images);*/
                // 视频源
                List<PublicMessage.Resource> videos = new ArrayList<>();
                PublicMessage.Resource resource2 = new PublicMessage.Resource();
                resource2.setOriginalUrl(collection.getUrl());
                resource2.setLength(collection.getFileLength());
                resource2.setSize(collection.getFileSize());
                videos.add(resource2);
                body.setVideos(videos);
            } else if (collection.getType() == CollectionEvery.TYPE_FILE) {
                // 文件
                body.setType(PublicMessage.TYPE_FILE);
                List<PublicMessage.Resource> files = new ArrayList<>();
                PublicMessage.Resource resource2 = new PublicMessage.Resource();
                resource2.setOriginalUrl(collection.getUrl());
                resource2.setLength(collection.getFileLength());
                resource2.setSize(collection.getFileSize());
                files.add(resource2);
                body.setFiles(files);
            }
            publicMessage.setBody(body);
            mPublicMessage.add(publicMessage);
            mPublicMessageAdapter.notifyDataSetChanged();
            refreshComplete();
        }
    }
}