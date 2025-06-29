package com.tongxin.caihong.ui.contacts;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.CommonAdapter;
import com.tongxin.caihong.util.CommonViewHolder;
import com.tongxin.caihong.util.HtmlUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.secure.AES;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.secure.chat.SecureChatUtil;

import java.util.List;


/**
 * Created by Administrator on 2018/4/24 0024.
 * 回话记录
 */

public class TalkHistoryActivity extends BaseActivity {
    private PullToRefreshListView mPullToRefreshListView;
    private TalkHistoryAdapter mTalkHistoryAdapter;
    private List<ChatMessage> mChatMessages;

    private String mLoginUserId;
    private String mFriendId, mFriendName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_pullrefresh_list);
        if (getIntent() != null) {
            mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            mFriendName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
        }
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        loadData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(mFriendName);
    }

    private void initView() {
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        mPullToRefreshListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
    }

    private void loadData() {
        mChatMessages = ChatMessageDao.getInstance().getSingleChatMessages(mChatMessages, mLoginUserId, mFriendId, TimeUtils.sk_time_current_time(), 500);
        if (mChatMessages == null || mChatMessages.size() <= 0) {
            return;
        }

        // 数据库取出来时，content需要解密
        for (int i = 0; i < mChatMessages.size(); i++) {
            ChatMessage message = mChatMessages.get(i);
            if (!TextUtils.isEmpty(message.getContent())) {
                String key = SecureChatUtil.getSymmetricKey(message.getPacketId());
                try {
                    String s = AES.decryptStringFromBase64(message.getContent(), Base64.decode(key));
                    message.setContent(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mTalkHistoryAdapter = new TalkHistoryAdapter(this, mChatMessages);
        mPullToRefreshListView.getRefreshableView().setAdapter(mTalkHistoryAdapter);
    }

    class TalkHistoryAdapter extends CommonAdapter<ChatMessage> {

        TalkHistoryAdapter(Context context, List<ChatMessage> data) {
            super(context, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CommonViewHolder viewHolder = CommonViewHolder.get(mContext, convertView, parent,
                    R.layout.item_talk_history, position);
            RelativeLayout rl1 = viewHolder.getView(R.id.rl1);
            ImageView avatar1 = viewHolder.getView(R.id.ava1);
            TextView tv1 = viewHolder.getView(R.id.tv1);
            ImageView iv1 = viewHolder.getView(R.id.iv1);
            RelativeLayout rl2 = viewHolder.getView(R.id.rl2);
            ImageView avatar2 = viewHolder.getView(R.id.ava2);
            TextView tv2 = viewHolder.getView(R.id.tv2);
            ImageView iv2 = viewHolder.getView(R.id.iv2);
            if (data.size() > 0) {
                ChatMessage chatMessage = data.get(data.size() - (position + 1));// 倒过来拿数据
                if (chatMessage.getFromUserId().equals(mLoginUserId)) {
                    rl1.setVisibility(View.VISIBLE);
                    rl2.setVisibility(View.GONE);

                    AvatarHelper.getInstance().displayAvatar(mLoginUserId, avatar1);

                    if (chatMessage.getType() == XmppMessage.TYPE_TEXT
                            && !chatMessage.getIsReadDel()) {// 文本 非阅后即焚消息
                        String s = StringUtils.replaceSpecialChar(chatMessage.getContent());
                        CharSequence content = HtmlUtils.transform200SpanString(s, true);
                        tv1.setText(content);
                    } else {
                        tv1.setText(StringUtils.getMessageContent(chatMessage, false));
                    }

                    if (chatMessage.getIsReadDel()) {// 阅后即焚 添加图标
                        iv1.setVisibility(View.VISIBLE);
                    } else {
                        iv1.setVisibility(View.GONE);
                    }
                } else {
                    rl1.setVisibility(View.GONE);
                    rl2.setVisibility(View.VISIBLE);

                    AvatarHelper.getInstance().displayAvatar(chatMessage.getFromUserName(), chatMessage.getFromUserId(), avatar2, false);

                    if (chatMessage.getType() == XmppMessage.TYPE_TEXT
                            && !chatMessage.getIsReadDel()) {// 文本 非阅后即焚消息
                        String s = StringUtils.replaceSpecialChar(chatMessage.getContent());
                        CharSequence content = HtmlUtils.transform200SpanString(s, true);
                        tv2.setText(content);
                    } else {
                        tv2.setText(StringUtils.getMessageContent(chatMessage, false));
                    }

                    if (chatMessage.getIsReadDel()) {
                        iv2.setVisibility(View.VISIBLE);
                    } else {
                        iv2.setVisibility(View.GONE);
                    }
                }
            }
            return viewHolder.getConvertView();
        }
    }
}
