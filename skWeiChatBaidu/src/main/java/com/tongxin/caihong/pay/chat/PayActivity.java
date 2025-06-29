package com.tongxin.caihong.pay.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tongxin.caihong.bean.Friend;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.ui.base.BaseActivity;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;
import com.tongxin.caihong.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PayActivity extends BaseActivity implements ChatMessageListener {

    private RecyclerView mSKPayRcy;
    private PayAdapter mSKPayAdapter;
    private List<ChatMessage> mChatMessageSource = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sk_pay);
        initActionBar();
        initView();
        ListenerManager.getInstance().addChatMessageListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeChatMessageListener(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        TextView titleTv = findViewById(R.id.tv_title_center);
        titleTv.setText(getString(R.string.sk_pay));
    }

    private void initView() {
        mChatMessageSource = new ArrayList<>();
        List<ChatMessage> messages = ChatMessageDao.getInstance().getSingleChatMessages(mChatMessageSource, coreManager.getSelf().getUserId(),
                Friend.ID_SK_PAY, TimeUtils.sk_time_current_time(), 100);
        Collections.reverse(messages);// 将集合倒序
        mChatMessageSource.addAll(messages);
        mSKPayRcy = findViewById(R.id.sk_pay_rcy);
        mSKPayAdapter = new PayAdapter(mChatMessageSource);
        mSKPayRcy.setLayoutManager(new LinearLayoutManager(this));
        mSKPayRcy.setAdapter(mSKPayAdapter);
        mSKPayRcy.setItemAnimator(new DefaultItemAnimator());

        mSKPayRcy.scrollToPosition(mSKPayAdapter.getItemCount() - 1);

        mSKPayRcy.setVisibility(mChatMessageSource.size() > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {

    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) {
        if (fromUserId.equals(Friend.ID_SK_PAY)) {
            mSKPayRcy.post(() -> {
                mSKPayRcy.setVisibility(View.VISIBLE);
                mChatMessageSource.add(message);
                mSKPayAdapter.notifyItemInserted(mChatMessageSource.size());
                mSKPayRcy.scrollToPosition(mSKPayAdapter.getItemCount() - 1);
            });
            return true;
        }
        return false;
    }
}
