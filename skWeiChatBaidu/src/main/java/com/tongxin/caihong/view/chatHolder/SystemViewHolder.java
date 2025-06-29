package com.tongxin.caihong.view.chatHolder;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.util.CommonUtils;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.util.link.SpannableStringBuilderAllVer;

// 系统消息的holder
class SystemViewHolder extends AChatHolderInterface {
    private ImageView iv_red;
    private TextView mTvContent;
    private String time;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return R.layout.chat_item_system;
    }

    @Override
    public void initView(View view) {
        iv_red = view.findViewById(R.id.iv_red);
        mTvContent = view.findViewById(R.id.chat_content_tv);
        if (CommonUtils.isSetChatBackground(mLoginUserId, mToUserId)) {
            // 设置了聊天背景，字体变黑
            mTvContent.setTextColor(MyApplication.getContext().getResources().getColor(R.color.app_black));
        } else {
            mTvContent.setTextColor(MyApplication.getContext().getResources().getColor(R.color.system_default_text_color));
        }
        mRootView = mTvContent;
    }

    @Override
    public void fillData(ChatMessage message) {
        CharSequence content;
        iv_red.setVisibility(View.GONE);
        if (message.getFileSize() == XmppMessage.TYPE_83) {
            iv_red.setVisibility(View.VISIBLE);
            // 红包被领取的提示
            content = StringUtils.matcherSearchTitle(Color.parseColor("#EB9F4F"), message.getContent(), getString(R.string.chat_red));
        } else if (message.getFileSize() == XmppMessage.TYPE_REQUEST_COMMENT) {
            String sure = message.isDownload() ? getString(R.string.service_has_grade) : getString(R.string.service_to_grade);
            // 客服请求评价的提示
            SpannableStringBuilderAllVer sb = new SpannableStringBuilderAllVer(getString(R.string.service_content_request_comment));
            sb.append(", ");
            sb.append(sure, new ForegroundColorSpan(Color.parseColor("#6699FF")), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content = sb;
        } else if (message.getType() == XmppMessage.TYPE_SERVICE_FORWARD) {
            if (message.getFileSize() == 0) {
                // 自动分配，
                content = (MyApplication.getContext().getString(R.string.service_tip_auto_forward_place_holder, message.getToUserName()));
            } else if (message.getFileSize() == 1) {
                // 手动转接，
                content = (MyApplication.getContext().getString(R.string.service_tip_forward_place_holder, message.getToUserName()));
            } else {
                // 无客服在线，
                content = (MyApplication.getContext().getString(R.string.service_tip_forward_offline));
            }
        } else if (message.getFileSize() == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME) {
            content = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"), message.getContent(), getString(R.string.tip_sync_set_msg_auto_delete));
        } else if (message.getFileSize() == XmppMessage.TYPE_BACK) {
            content = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"), message.getContent(), getString(R.string.tip_edit_again));
        } else {
            //  验证该提示是否为邀请好友入群的验证提示，是的话高亮显示KeyWord 并针对Click事件进行处理
            // todo  应该效仿红包被领取的提示，将原消息type与关键信息存在其他字段内，这样结构会更加清晰且不会出错
            String sure = message.isDownload() ? getString(R.string.has_confirm) : getString(R.string.to_confirm);
            content = StringUtils.matcherSearchTitle(Color.parseColor("#6699FF"), message.getContent(), sure);
        }
        if (message.getFileSize() == XmppMessage.TYPE_IS_MU_END_CONNECT_VIDEO
                || message.getFileSize() == XmppMessage.TYPE_IS_MU_END_CONNECT_VOICE) {
            this.time = TimeUtils.sk_time_long_to_chat_time_str(message.getTimeSend());
        }
        setText(content);
        mTvContent.setOnClickListener(this);
    }

    @Override
    protected void onRootClick(View v) {

    }

    @Override
    public void showTime(String time) {
        this.time = time;
        setText(mTvContent.getText());
    }

    private void setText(CharSequence content) {
        if (!TextUtils.isEmpty(time)) {
            // 需要支持SpannableString， 不能直接使用StringBuilder之类，
            Editable editable = Editable.Factory.getInstance().newEditable(content);
            editable.append("(").append(time).append(")");
            mTvContent.setText(editable);
        } else {
            mTvContent.setText(content);
        }
    }

    @Override
    public boolean isLongClick() {
        return false;
    }

    @Override
    public boolean isOnClick() {
        return true;
    }

    @Override
    public boolean enableNormal() {
        return false;
    }
}
