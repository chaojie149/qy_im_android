package com.tongxin.caihong.ui.message.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.FriendDao;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.ui.message.ChatActivity;
import com.tongxin.caihong.ui.message.MucChatActivity;
import com.tongxin.caihong.util.CommonAdapter;
import com.tongxin.caihong.util.CommonViewHolder;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.ToastUtil;
import com.tongxin.caihong.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * 单、群 聊查找聊天记录
 */
public class SearchChatHistoryActivity extends BaseActivity {
    public static int TYPE_GROUP_MEMBER = 5;
    public static int TYPE_DATA = 6;
    private final List<ChatMessage> data = new ArrayList<>();
    private ClearEditText etSearch;
    private SearchTextAdapter searchTextAdapter;
    private List<Item> items = new ArrayList<>();
    // 单聊 || 群聊
    private boolean isSearchSingle;
    private String mFriendId;
    private Friend mFriend;
    private String searchKey;

    public static void start(Context ctx, String friendId, boolean isSearchSingle) {
        start(ctx, friendId, isSearchSingle, null);
    }

    public static void start(Context ctx, String friendId, boolean isSearchSingle, String searchKey) {
        Intent intent = new Intent(ctx, SearchChatHistoryActivity.class);
        intent.putExtra("isSearchSingle", isSearchSingle);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendId);
        intent.putExtra("searchKey", searchKey);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_chat_history);
        isSearchSingle = getIntent().getBooleanExtra("isSearchSingle", false);
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        mFriend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), mFriendId);
        if (mFriend == null) {
            ToastUtil.showErrorData(this);
            finish();
            return;
        }
        searchKey = getIntent().getStringExtra("searchKey");
        initActionBar();
        initView();
        setTouch(true);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        if (isSearchSingle) {
            tvTitle.setText(getString(R.string.find_friend_chat_history_place_holder, mFriend.getShowName()));
        } else {
            tvTitle.setText(getString(R.string.find_room_chat_history_place_holder, mFriend.getShowName()));
        }
    }

    private void initView() {
        etSearch = findViewById(R.id.search_edit);

        ListView lvSearchText = findViewById(R.id.chat_history_lv);
        searchTextAdapter = new SearchTextAdapter(this, data);
        lvSearchText.setAdapter(searchTextAdapter);

/*
        if (!isSearchSingle) {
            items.add(new Item(TYPE_GROUP_MEMBER, R.string.s_group_member));
        }
        items.add(new Item(TYPE_DATA, R.string.s_data));
*/
        items.add(new Item(SearchImageVideoContent.TYPE_IMAGE, R.string.s_image));
        items.add(new Item(SearchImageVideoContent.TYPE_VIDEO, R.string.s_video));
        items.add(new Item(SearchDesignationContent.TYPE_FILE, R.string.s_file));
        items.add(new Item(SearchDesignationContent.TYPE_LINK, R.string.s_link));
        if (coreManager.getConfig().enablePayModule) {
            items.add(new Item(SearchDesignationContent.TYPE_PAY, R.string.s_pay));
        }
        RecyclerView rcySearchType = findViewById(R.id.rcySearchType);
        rcySearchType.setLayoutManager(new GridLayoutManager(mContext, 3));
        SearchTypeAdapter searchTypeAdapter = new SearchTypeAdapter();
        rcySearchType.setAdapter(searchTypeAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                data.clear();
                searchKey = s.toString();
                if (!TextUtils.isEmpty(searchKey)) {
                    List<ChatMessage> messages = ChatMessageDao.getInstance().queryChatMessageByContent(coreManager.getSelf().getUserId()
                            , mFriendId
                            , searchKey);
                    data.addAll(messages);
                    findViewById(R.id.empty).setVisibility(data.size() > 0 ? View.GONE : View.VISIBLE);
                    findViewById(R.id.s_fl).setVisibility(View.VISIBLE);
                    findViewById(R.id.s_ll).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.s_fl).setVisibility(View.GONE);
                    findViewById(R.id.s_ll).setVisibility(View.VISIBLE);
                }
                searchTextAdapter.notifyDataSetChanged();
            }
        });
        lvSearchText.setOnItemClickListener((parent, view, position, id) -> {
            ChatMessage chatMessage = data.get(position);
            if (chatMessage != null) {
                if (isSearchSingle) {
                    ChatActivity.start(mContext, mFriend, true, chatMessage.getDoubleTimeSend(), chatMessage.getPacketId());
                } else {
                    MucChatActivity.start(mContext, mFriend, true, chatMessage.getDoubleTimeSend(), chatMessage.getPacketId());
                }
            }
        });

        etSearch.post(() -> {
            if (!TextUtils.isEmpty(searchKey)) {
                etSearch.setText(searchKey);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final View vLine;

        public ViewHolder(View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            vLine = itemView.findViewById(R.id.vLine);
        }
    }

    static class Item {
        int type;
        @StringRes
        int text;

        public Item(int type, int text) {
            this.type = type;
            this.text = text;
        }
    }

    class SearchTextAdapter extends CommonAdapter<ChatMessage> {

        public SearchTextAdapter(Context context, List<ChatMessage> data) {
            super(context, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.row_search_chat_history, position);
            ImageView avatar_iv = viewHolder.getView(R.id.avatar_img);
            TextView nick_name_tv = viewHolder.getView(R.id.nick_name_tv);
            TextView time_tv = viewHolder.getView(R.id.time_tv);
            TextView content_tv = viewHolder.getView(R.id.content_tv);
            ChatMessage chatMessage = data.get(position);
            if (chatMessage != null) {
                String name = chatMessage.getFromUserName();
                if (isSearchSingle) {
                    if (TextUtils.equals(chatMessage.getFromUserId(), coreManager.getSelf().getUserId())) {
                        name = coreManager.getSelf().getNickName();
                    } else {
                        name = mFriend.getShowName();
                    }
                }
                AvatarHelper.getInstance().displayAvatar(name, chatMessage.getFromUserId(), avatar_iv, false);
                nick_name_tv.setText(name);
                time_tv.setText(TimeUtils.getFriendlyTimeDesc(SearchChatHistoryActivity.this, chatMessage.getTimeSend()));

                // 搜索下匹配关键字高亮显示
                String text = chatMessage.getContent();
                SpannableString spannableString = StringUtils.matcherSearchTitle(Color.parseColor("#fffa6015"),
                        text, searchKey);
                content_tv.setText(spannableString);
            }
            return viewHolder.getConvertView();
        }
    }

    class SearchTypeAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.row_search_message_history, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvType.setText(item.text);
            holder.vLine.setVisibility((position + 1) % 3 == 0 ? View.INVISIBLE : View.VISIBLE);
            holder.tvType.setOnClickListener(view -> {
                Intent intent;
                if (item.type <= 4) {
                    if (item.type == 0 || item.type == 1) {
                        // 图片 && 视频
                        intent = new Intent(mContext, SearchImageVideoContent.class);
                    } else {
                        // 文件 && 链接 && 交易
                        intent = new Intent(mContext, SearchDesignationContent.class);
                    }
                    intent.putExtra("search_type", item.type);
                    intent.putExtra("search_objectId", mFriendId);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
