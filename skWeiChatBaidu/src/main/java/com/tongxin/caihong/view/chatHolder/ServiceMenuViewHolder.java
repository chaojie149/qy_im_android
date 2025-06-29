package com.tongxin.caihong.view.chatHolder;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.service.bean.Question;
import com.tongxin.caihong.ui.me.FontSizeActivity;
import com.tongxin.caihong.util.UiUtils;
import com.tongxin.caihong.util.link.SpannableStringBuilderAllVer;

import java.util.List;

public class ServiceMenuViewHolder extends AChatHolderInterface {

    public TextView mTvContent;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_service_menu : R.layout.chat_to_item_service_menu;
    }

    @Override
    public void initView(View view) {
        mTvContent = view.findViewById(R.id.chat_text);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        // 修改字体功能
        int size = FontSizeActivity.getFontSize(mContext);
        mTvContent.setTextSize(size);
        mTvContent.setTextColor(mContext.getResources().getColor(R.color.black));

        SpannableStringBuilderAllVer sb = new SpannableStringBuilderAllVer();
        sb.append(message.getContent());

        makeMenu(sb, message.getObjectId(), !TextUtils.isEmpty(message.getContent()), !TextUtils.isEmpty(message.getFilePath()));

        sb.append(message.getFilePath());
        mTvContent.setText(sb);
        mTvContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void makeMenu(SpannableStringBuilderAllVer sb, String objectId, boolean firstBreakLine, boolean lastBreakLine) {
        if (TextUtils.isEmpty(objectId)) {
            if (firstBreakLine && lastBreakLine) {
                sb.append('\n').append('\n');
            }
            return;
        }
        List<Question> questionList = JSON.parseArray(objectId, Question.class);
        if (questionList.isEmpty()) {
            if (firstBreakLine && lastBreakLine) {
                sb.append('\n').append('\n');
            }
            return;
        }
        if (firstBreakLine) {
            sb.append('\n').append('\n');
        }
        for (int i = 0; i < questionList.size(); i++) {
            Question question = questionList.get(i);
            sb.append(question.getQuestion(), new MenuClick(question), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (i < questionList.size() - 1) {
                sb.append('\n');
                sb.append('\n');
            }
        }
        if (lastBreakLine) {
            sb.append('\n').append('\n');
        }
    }

    @Override
    protected void onRootClick(View v) {

    }

    @Override
    public boolean enableFire() {
        return true;
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    private class MenuClick extends ClickableSpan {
        private Question question;

        public MenuClick(Question question) {
            this.question = question;
        }

        @Override
        public void onClick(View view) {
            if (UiUtils.isNormalClick(view)) {
                mHolderListener.onQuestionClick(view, ServiceMenuViewHolder.this, mdata, question);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(0xff517fae);
            ds.setUnderlineText(false);
        }
    }
}
