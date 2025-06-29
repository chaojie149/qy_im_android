package com.tongxin.caihong.ui.me.collection;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.tongxin.caihong.adapter.PublicMessageRecyclerAdapter;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.CollectionEvery;
import com.tongxin.caihong.bean.collection.Message;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.util.ToastUtil;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.tongxin.caihong.R;
import com.tongxin.caihong.Reporter;
import com.tongxin.caihong.ui.base.EasyFragment;
import com.tongxin.caihong.view.SelectionFrame;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

import static android.app.Activity.RESULT_OK;

public class CollectionFragment extends EasyFragment {
    public static final int PAGE_SIZE = 10;
    public SwipeRecyclerView srvCollection;
    private SmartRefreshLayout srlCollection;
    private PublicMessageRecyclerAdapter mPublicMessageAdapter;
    private List<CollectionEvery> data = new ArrayList<>();
    private List<PublicMessage> mPublicMessage = new ArrayList<>();
    private boolean isLoad;
    private int pagerIndex;
    private int selectFragment;
    private boolean isSendCollection;
    private boolean isCollectEdit;
    // 允许下拉触发翻页，false为不允许，默认为true
    private boolean enableScrollLoad = true;

    public CollectionFragment(int i, boolean b) {
        this.selectFragment = i;
        this.isSendCollection = b;
    }

    @Override
    protected int inflateLayoutId() {
        return R.layout.fragment_collection;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        if (createView) {
            initView();
            getMyCollectionList(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPublicMessageAdapter != null) {
            mPublicMessageAdapter.setCollectionSelect(isCollectEdit);
            mPublicMessageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (mPublicMessageAdapter != null) {
                mPublicMessageAdapter.setCollectionSelect(isCollectEdit);
                mPublicMessageAdapter.notifyDataSetChanged();
            }
        } else {

        }
    }

    public void setIsEdit(boolean isEdit) {
        this.isCollectEdit = isEdit;
        if (mPublicMessageAdapter != null) {
            mPublicMessageAdapter.setCollectionSelect(isCollectEdit);
            mPublicMessageAdapter.notifyDataSetChanged();
        }
    }

    public void deleteMsg() {
        if (mPublicMessageAdapter != null && data != null && mPublicMessage != null) {
            List<PublicMessage> rv1 = new ArrayList<>();
            List<CollectionEvery> rv2 = new ArrayList<>();
            for (int i = 0; i < mPublicMessage.size(); i++) {
                if (MyCollection.deleteIdMap.containsKey(mPublicMessage.get(i).getEmojiId())) {
                    rv1.add(mPublicMessage.get(i));
                    rv2.add(data.get(i));
                }
            }
            mPublicMessage.removeAll(rv1);
            data.removeAll(rv2);
            mPublicMessageAdapter.notifyDataSetChanged();
            findViewById(R.id.llEmpty).setVisibility(data.size() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void initView() {
        srlCollection = findViewById(R.id.srlCollection);
        srlCollection.setOnRefreshListener(refreshLayout -> getMyCollectionList(true));

        srvCollection = findViewById(R.id.srvCollection);
        srvCollection.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mPublicMessageAdapter = new PublicMessageRecyclerAdapter(requireContext(), coreManager, mPublicMessage);
        if (isSendCollection || isCollectEdit) {
            mPublicMessageAdapter.setCollectionType(2);
        } else {
            mPublicMessageAdapter.setCollectionType(1);
        }
        srvCollection.setAdapter(mPublicMessageAdapter);

        srvCollection.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                //当前RecyclerView显示出来的最后一个的item的position
                int lastPosition = -1;

                //当前状态为停止滑动状态SCROLL_STATE_IDLE时
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        if (layoutManager instanceof LinearLayoutManager) {
                            // 通过LayoutManager找到当前显示的最后的item的position
                            lastPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                        }

                        if (lastPosition == layoutManager.getItemCount() - 1 && !isLoad) {
                            if (enableScrollLoad) {
                                pagerIndex++;
                                Log.e("CF", "enableScrollLoad = true，触发翻页" +
                                        "，mPagerIndex-->" + pagerIndex);
                                getMyCollectionList(false);
                            } else {
                                Log.e("CF", "enableScrollLoad = false，不触发翻页" +
                                        "，mPagerIndex-->" + pagerIndex);
                            }
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mPublicMessageAdapter.setOnItemClickListener(vh -> {
            if (isSendCollection) {
                int position = vh.getAdapterPosition();
                CollectionEvery collection = data.get(position);
                if (collection != null) {
                    SelectionFrame dialog = new SelectionFrame(requireContext());
                    dialog.setSomething(null, getString(R.string.tip_confirm_send), new SelectionFrame.OnSelectionFrameClickListener() {
                        @Override
                        public void cancelClick() {
                        }

                        @Override
                        public void confirmClick() {
                            Intent intent = new Intent();
                            try {
                                if ((collection.getTargetType() == PublicMessage.CIRCLE || collection.getTargetType() == PublicMessage.VIDEO)
                                        && (collection.getType() != PublicMessage.TYPE_SINGLE_IMAGE && collection.getType() != PublicMessage.TYPE_SINGLE_VIDEO)) {
                                    /**
                                     * 发送完整的动态或视界
                                     */
                                    intent.putExtra("data", JSON.toJSONString(mPublicMessage.get(position)));
                                    intent.putExtra("isDynamicTrill", true);
                                    requireActivity().setResult(RESULT_OK, intent);
                                    requireActivity().finish();
                                } else {
                                    /**
                                     * 发送来自单群聊的收藏、来自社交圈单独收藏的图片与视频
                                     */
                                    intent.putExtra("data", JSON.toJSONString(collection));
                                    requireActivity().setResult(RESULT_OK, intent);
                                    requireActivity().finish();
                                }
                            } catch (JSONException e) {
                                ToastUtil.showToast(requireActivity(), getString(R.string.parse_exception));
                                e.printStackTrace();
                            }
                        }
                    });
                    dialog.show();
                } else {
                    Reporter.unreachable();
                    ToastUtil.showToast(requireContext(), R.string.tip_server_error);
                }
            } else if (isCollectEdit) {
                int position = vh.getAdapterPosition();
                if (MyCollection.deleteIdMap.containsKey(mPublicMessage.get(position).getEmojiId())) {
                    MyCollection.deleteIdMap.remove(mPublicMessage.get(position).getEmojiId());
                } else {
                    if (MyCollection.deleteIdMap.size() == 20) {
                        ToastUtil.showToast(requireContext(), getString(R.string.select_over));
                        return;
                    }
                    MyCollection.deleteIdMap.put(mPublicMessage.get(position).getEmojiId(), mPublicMessage.get(position).getEmojiId());
                }
                mPublicMessageAdapter.notifyDataSetChanged();
                if (requireActivity() instanceof MyCollection) {
                    ((MyCollection) requireActivity()).updateSelectCount();
                }
            }
        });
    }

    private void getMyCollectionList(boolean isClear) {
        isLoad = true;
        if (isClear) {
            pagerIndex = 0;
            data.clear();
            mPublicMessage.clear();
            mPublicMessageAdapter.notifyDataSetChanged();
        }

        if (selectFragment == 6) {
            // 收藏类型  0.全部  1.图片   2.视频    3.文件  4.语音  5.文本   6.表情  7.SDK分享的链接
            selectFragment += 1;
        }
        DialogHelper.showDefaulteMessageProgressDialog(requireActivity());
        Map<String, String> params = new HashMap<>();
        params.put("userId", coreManager.getSelf().getUserId());
        params.put("pageSize", String.valueOf(PAGE_SIZE));
        params.put("pageIndex", String.valueOf(pagerIndex));
        params.put("type", String.valueOf(selectFragment));

        HttpUtils.get().url(coreManager.getConfig().Collection_LIST_OTHER)
                .params(params)
                .build()
                .execute(new ListCallback<CollectionEvery>(CollectionEvery.class) {
                    @Override
                    public void onResponse(ArrayResult<CollectionEvery> result) {
                        isLoad = false;
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(requireContext(), result)) {
                            List<CollectionEvery> collectionEveryList = result.getData();
                            if (collectionEveryList == null) {
                                collectionEveryList = new ArrayList<>();
                            }
                            // 判断是否还能够翻页
                            enableScrollLoad = collectionEveryList.size() == PAGE_SIZE;
                            if (collectionEveryList.size() > 0) {
                                data.addAll(collectionEveryList);
                                afterGetData(collectionEveryList);
                            }
                            findViewById(R.id.llEmpty).setVisibility(data.size() == 0 ? View.VISIBLE : View.GONE);
                            srlCollection.finishRefresh();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        isLoad = false;
                        DialogHelper.dismissProgressDialog();
                        srlCollection.finishRefresh();
                        ToastUtil.showErrorNet(requireContext());
                    }
                });
    }

    /**
     * 坑太大，不应该和朋友圈共用adapter, 共用实体类，
     */
    private void afterGetData(List<CollectionEvery> data) {
        for (int i = 0; i < data.size(); i++) {
            CollectionEvery collection = data.get(i);
            PublicMessage publicMessage = new PublicMessage();
            if (TextUtils.isEmpty(collection.getToUserId())) {
                // 兼容老收藏，没有toUserId的情况
                publicMessage.setUserId(coreManager.getSelf().getUserId());
                publicMessage.setNickName(coreManager.getSelf().getNickName());
            } else {
                publicMessage.setUserId(collection.getToUserId());
                publicMessage.setNickName(collection.getToUserName());
            }
            // 显示的时间应该为收藏这条消息的时间，而不是发送这条消息的时间
            publicMessage.setTime(data.get(i).getCreateTime());
            // 播放语音需要用到messageId,否则不需要设置
            publicMessage.setMessageId(collection.getCollectMsgId());
            // 文件名，
            publicMessage.setFileName(collection.getFileName());
            publicMessage.setTargetType(collection.getTargetType());
            publicMessage.setTargetName(collection.getTargetName());
            publicMessage.setToUserId(collection.getToUserId());
            publicMessage.setToUserName(collection.getToUserName());
            if (collection.getTargetType() == 4) {
                // 单聊里面取反
                publicMessage.setToUserId(collection.getTargetId());
                publicMessage.setToUserName(collection.getTargetName());
            }
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
                if (collection.getType() == CollectionEvery.TYPE_VIDEO_SINGLE) {
                    body.setType(PublicMessage.TYPE_SINGLE_VIDEO);
                }
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
            } else if (collection.getType() == CollectionEvery.TYPE_LINK) {
                Message message = JSON.parseObject(collection.getMsg(), Message.class);
                body.setType(PublicMessage.TYPE_LINK);
                body.setSdkUrl(message.getUrl());
                body.setSdkIcon(message.getImg());
                body.setSdkTitle(message.getTitle());
            }
            publicMessage.setBody(body);
            mPublicMessage.add(publicMessage);
        }
        mPublicMessageAdapter.notifyDataSetChanged();
    }
}