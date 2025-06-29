package com.tongxin.caihong.view.chatHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.company.InviteSureJoinCompanyActivity;
import com.tongxin.caihong.ui.message.multi.InviteSelfVerifyActivity;
import com.tongxin.caihong.ui.other.BasicInfoActivity;
import com.tongxin.caihong.util.UiUtils;

class CardViewHolder extends AChatHolderInterface {
    ImageView ivCardImage;
    TextView tvPersonName;
    TextView tvPersonSex;
    ImageView ivUnRead;
    TextView tvBottomText;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_card : R.layout.chat_to_item_card;
    }

    @Override
    public void initView(View view) {
        ivCardImage = view.findViewById(R.id.iv_card_head);
        tvPersonName = view.findViewById(R.id.person_name);
        tvPersonSex = view.findViewById(R.id.person_sex);
        tvBottomText = view.findViewById(R.id.person_title);
        ivUnRead = view.findViewById(R.id.unread_img_view);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        if (mdata.getType() == XmppMessage.TYPE_MEETING_INVITE) {
            // 进入会议
            AvatarHelper.getInstance().displayAvatar(message.getFromUserName(), message.getFromUserId(), ivCardImage, true);
            if (mdata.getFileSize() == 1) {
                tvPersonName.setText(getString(R.string.tip_type_meeting_invite, message.getFromUserName(), getString(R.string.chat_video_conference)));
                tvBottomText.setText(getString(R.string.tip_type_meeting_invite_bottom, getString(R.string.chat_video_conference)));
            } else if (mdata.getFileSize() == 2) {
                tvPersonName.setText(getString(R.string.tip_type_meeting_invite, message.getFromUserName(), getString(R.string.screen_call)));
                tvBottomText.setText(getString(R.string.tip_type_meeting_invite_bottom, getString(R.string.screen_call)));
            } else if (mdata.getFileSize() == 3) {
                tvPersonName.setText(getString(R.string.tip_type_meeting_invite, message.getFromUserName(), getString(R.string.live_call)));
                tvBottomText.setText(getString(R.string.tip_type_meeting_invite_bottom, getString(R.string.live_call)));
            } else {
                tvPersonName.setText(getString(R.string.tip_type_meeting_invite, message.getFromUserName(), getString(R.string.chat_audio_conference)));
                tvBottomText.setText(getString(R.string.tip_type_meeting_invite_bottom, getString(R.string.chat_audio_conference)));
            }
        } else if (mdata.getType() == XmppMessage.TYPE_LIVE_INVITE) {
            // 进入直播间
            AvatarHelper.getInstance().displayAvatar(message.getFromUserName(), message.getFromUserId(), ivCardImage, true);
            tvPersonName.setText(message.getFromUserName() + getString(R.string.type_live_invite2));
            tvBottomText.setText(getString(R.string.type_live_invite_bottom));
        } else if (mdata.getType() == XmppMessage.TYPE_INVITE_JOIN_ROOM) {
            // 邀请加入群组
            AvatarHelper.getInstance().displayAvatar(message.getFromUserName(), message.getFromUserId(), ivCardImage, true);
            tvPersonName.setText(mContext.getString(R.string.type_invite_join_room2, message.getFromUserName(), message.getContent()));
            tvBottomText.setText(getString(R.string.type_invite_join_room_bottom));
        } else if (mdata.getType() == XmppMessage.TYPE_COMPANY_INVITE_JOIN) {
            // 邀请加入公司
            AvatarHelper.getInstance().displayAvatar(message.getFromUserName(), message.getFromUserId(), ivCardImage, true);
            tvPersonName.setText(mContext.getString(R.string.invite_join_company, message.getFromUserName()));
            tvBottomText.setText(getString(R.string.company_invite));
        } else {
            AvatarHelper.getInstance().displayAvatar(message.getContent(), message.getObjectId(), ivCardImage, true);
            tvPersonName.setText(String.valueOf(message.getContent()));
        }

        if (!isMysend) {
            ivUnRead.setVisibility(message.isSendRead() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onRootClick(View v) {
        if (UiUtils.isNormalClick(v)) {
            sendReadMessage(mdata);
            ivUnRead.setVisibility(View.GONE);
            if (mdata.getType() == XmppMessage.TYPE_INVITE_JOIN_ROOM) {
                // 邀请加入群组
                InviteSelfVerifyActivity.start(mContext
                        , mdata.getObjectId()
                        , mdata.getContent()
                        , mdata.getFileSize()
                        , mdata.getFromUserId()
                        , mdata.getFromUserName()
                        , mdata.getTranslation());
            } else if (mdata.getType() == XmppMessage.TYPE_COMPANY_INVITE_JOIN) {
                InviteSureJoinCompanyActivity.start(mContext, JSON.toJSONString(mdata));
            } else {
                BasicInfoActivity.start(mContext, mdata.getObjectId(), BasicInfoActivity.FROM_ADD_TYPE_CARD);
            }
        }
    }

    /**
     * 重写该方法，return true 表示显示红点
     *
     * @return
     */
    @Override
    public boolean enableUnRead() {
        return true;
    }
}
