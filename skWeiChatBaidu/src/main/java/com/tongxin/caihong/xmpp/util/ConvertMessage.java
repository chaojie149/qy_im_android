package com.tongxin.caihong.xmpp.util;

import android.text.TextUtils;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.RoomMember;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.RoomMemberDao;
import com.tongxin.caihong.ui.mucfile.XfileUtils;
import com.tongxin.caihong.util.DateFormatUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author zq
 * 将各种type的消息转换为文字
 */
public class ConvertMessage {

    /**
     * 转换支持转换的消息
     *
     * @param type
     * @param mLoginUserId
     * @param chatMessage
     * @return
     */
    public static String convertSupportChatMessage(int type, String mLoginUserId, ChatMessage chatMessage) {
        String content = convertPayMessage(type, mLoginUserId, chatMessage);
        if (TextUtils.isEmpty(content)) {
            content = convertGroupMessage(type, mLoginUserId, chatMessage);
        }
        return content;
    }

    /**
     * 转换 t 类型消息
     *
     * @param type
     * @param mLoginUserId
     * @param chatMessage
     */
    public static String convertVariousChatMessage(int type, String mLoginUserId, ChatMessage chatMessage) {
        String content = "";
        if (type == XmppMessage.TYPE_SCREENSHOT) {
            content = getString(R.string.tip_remote_screenshot);
        }
        return content;
    }

    /**
     * 转换支付相关类型消息
     *
     * @param type
     * @param mLoginUserId
     * @param chatMessage
     */
    public static String convertPayMessage(int type, String mLoginUserId, ChatMessage chatMessage) {
        String content = "";
        if (type == XmppMessage.TYPE_83) {
            String fromName = "";
            String toName = "";
            if (TextUtils.equals(chatMessage.getFromUserId(), mLoginUserId)
                    && TextUtils.equals(chatMessage.getToUserId(), mLoginUserId)) {
                // 自己领取了自己的红包
                fromName = getString(R.string.you);
                toName = getString(R.string.self);
            } else if (TextUtils.equals(chatMessage.getToUserId(), mLoginUserId)) {
                // xx领取了你的红包
                fromName = chatMessage.getFromUserName();
                toName = getString(R.string.you);
            } else if (TextUtils.equals(chatMessage.getFromUserId(), mLoginUserId)) {
                //你领取了xx的红包
                fromName = MyApplication.getContext().getString(R.string.you);
                toName = chatMessage.getToUserName();
            } else {
                return content;
            }

            String hasBennReceived = "";
            if (chatMessage.getFileSize() == 1) {// 红包已领完
                try {
                    String sRedSendTime = chatMessage.getFilePath();
                    long redSendTime = Long.parseLong(sRedSendTime);
                    long betweenTime = chatMessage.getTimeSend() / 1000 - redSendTime;
                    String sBetweenTime;
                    if (betweenTime < TimeUnit.MINUTES.toSeconds(1)) {
                        sBetweenTime = betweenTime + getString(R.string.second);
                    } else if (betweenTime < TimeUnit.HOURS.toSeconds(1)) {
                        sBetweenTime = TimeUnit.SECONDS.toMinutes(betweenTime) + getString(R.string.minute);
                    } else {
                        sBetweenTime = TimeUnit.SECONDS.toHours(betweenTime) + getString(R.string.hour);
                    }
                    hasBennReceived = MyApplication.getContext().getString(R.string.red_packet_has_received_place_holder, sBetweenTime);
                } catch (Exception e) {
                    hasBennReceived = MyApplication.getContext().getString(R.string.red_packet_has_received);
                }
            }
            content = MyApplication.getContext().getString(R.string.tip_receive_red_packet_place_holder, fromName, toName) + hasBennReceived;
        } else if (type == XmppMessage.TYPE_RED_BACK) {
            content = getString(R.string.tip_red_back);
        } else if (type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
            content = getString(R.string.transfer_received);
        } else if (type == XmppMessage.TYPE_TRANSFER_BACK) {
            content = getString(R.string.transfer_backed);
        }
        return content;
    }

    /**
     * 转换群组相关类型消息
     * <p>
     * 2021.1.11 群公告使用新消息气泡展示，不转换type与content
     *
     * @param type
     * @param mLoginUserId
     * @param chatMessage
     */
    public static String convertGroupMessage(int type, String mLoginUserId, ChatMessage chatMessage) {
        String content;
        /*
        群文件
         */
        if (type == XmppMessage.TYPE_MUCFILE_DEL
                || type == XmppMessage.TYPE_MUCFILE_DOWN) {
            if (type == XmppMessage.TYPE_MUCFILE_DEL) {
                content = chatMessage.getFromUserName() + " " + getString(R.string.message_file_delete) + ":" + chatMessage.getFilePath();
            } else {
                content = chatMessage.getFromUserName() + " " + getString(R.string.message_file_down) + ":" + chatMessage.getFilePath();
            }
            return content;
        }

        /*
        群组修改群内昵称，公告，拉人，踢人，禁言，解散等(901-907 913 934)
         */
        if (type == XmppMessage.TYPE_CHANGE_NICK_NAME) {
            // 修改群内昵称
            content = chatMessage.getToUserName() + " " + getString(R.string.message_object_update_nickname) + "‘" + chatMessage.getContent() + "’";
        } else if (type == XmppMessage.TYPE_CHANGE_ROOM_NAME) {
            content = chatMessage.getFromUserName() + " " + getString(R.string.Message_Object_Update_RoomName) + chatMessage.getContent();
        } else if (type == XmppMessage.TYPE_DELETE_MEMBER) {
            // 退出群组/被踢出群组
            if (TextUtils.equals(mLoginUserId, chatMessage.getToUserId())) {
                // 自己
                if (TextUtils.equals(chatMessage.getFromUserId(), chatMessage.getToUserId())) {
                    // 主动退出
                    content = getString(R.string.OutPut_Room);
                } else {
                    // 被踢出
                    content = MyApplication.getContext().getString(R.string.tip_been_kick_place_holder, chatMessage.getFromUserName());
                }
            } else {
                // 他人
                if (TextUtils.equals(chatMessage.getFromUserId(), chatMessage.getToUserId())) {
                    // 主动退出
                    content = chatMessage.getFromUserName() + " " + getString(R.string.quit_group);
                } else {
                    // 被踢出
                    content = chatMessage.getToUserName() + " " + getString(R.string.kicked_out_group);
                }
            }
        }
/*
        else if (type == XmppMessage.TYPE_NEW_NOTICE
                || type == XmppMessage.TYPE_EDIT_GROUP_NOTICE) {
            // 发布、编辑公告
            content = chatMessage.getFromUserName()
                    + " "
                    + (type == XmppMessage.TYPE_NEW_NOTICE ? getString(R.string.Message_Object_Add_NewAdv) : getString(R.string.edit_group_notice))
                    + chatMessage.getContent();
        }
*/
        else if (type == XmppMessage.TYPE_GAG) {
            // 禁言/取消禁言
            long time = Long.parseLong(chatMessage.getContent());
            // 为防止其他用户接收不及时，给3s的误差
            if (time > chatMessage.getTimeSend() / 1000 + 3) {
                String formatTime = XfileUtils.fromatTime((time * 1000), "MM-dd HH:mm");
                content = chatMessage.getFromUserName()
                        + " "
                        + getString(R.string.message_object_yes)
                        + chatMessage.getToUserName()
                        + getString(R.string.Message_Object_Set_Gag_With_Time)
                        + formatTime;
            } else if (time == -1) {
                content = chatMessage.getFromUserName()
                        + " "
                        + getString(R.string.message_object_yes)
                        + chatMessage.getToUserName()
                        + getString(R.string.tip_set_permanent_talk);
            } else {
                content = chatMessage.getToUserName()
                        + MyApplication.getContext().getString(R.string.tip_been_cancel_ban_place_holder, chatMessage.getFromUserName());
            }
        } else if (type == XmppMessage.NEW_MEMBER) {
            if (TextUtils.equals(chatMessage.getFromUserId(), chatMessage.getToUserId())) {
                // 主动加入
                content = chatMessage.getFromUserName() + " " + getString(R.string.Message_Object_Group_Chat);
            } else {
                // 被邀请加入
                RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(chatMessage.getFilePath(), chatMessage.getFromUserId());
                String fromUserName = chatMessage.getFromUserName();
                if (member != null) {
                    fromUserName = member.getUserName();
                }
                content = fromUserName + " " + getString(R.string.message_object_inter_friend) + chatMessage.getToUserName();
            }
        } else if (type == XmppMessage.TYPE_SEND_MANAGER) {
            // 指定/取消管理员
            content = chatMessage.getFromUserName()
                    + " "
                    + (chatMessage.getContent().equals("1") ? getString(R.string.setting) : getString(R.string.sip_canceled))
                    + chatMessage.getToUserName()
                    + " "
                    + getString(R.string.message_admin);
        } else if (type == XmppMessage.TYPE_ROOM_MODIFY_CARD) {
            // 是否禁止群成员修改群名片，
            content = chatMessage.getFromUserName()
                    + " "
                    + (chatMessage.getContent().equals("1") ? getString(R.string.tip_room_allow_modify_card) : getString(R.string.tip_room_disallow_modify_card));
        } else {
            content = convertGroupManagerMessage(type, chatMessage);
        }
        return content;
    }

    /**
     * 转换群管理相关类型消息
     *
     * @param type
     * @param chatMessage
     * @return
     */
    private static String convertGroupManagerMessage(int type, ChatMessage chatMessage) {
        String content = "";
        if (type == XmppMessage.TYPE_CHANGE_SHOW_READ) {
            if (chatMessage.getContent().equals("1")) {
                content = getString(R.string.tip_owner_enable_read);
            } else {
                content = getString(R.string.tip_owner_disable_read);
            }
        } else if (type == XmppMessage.TYPE_GROUP_VERIFY) {
            if (chatMessage.getContent().equals("1")) {
                content = getString(R.string.tip_group_enable_verify);
            } else {
                content = getString(R.string.tip_group_disable_verify);
            }
        } else if (type == XmppMessage.TYPE_GROUP_LOOK) {
            if (chatMessage.getContent().equals("1")) {
                content = getString(R.string.tip_owner_private);
            } else {
                content = getString(R.string.tip_owner_public);
            }
        } else if (type == XmppMessage.TYPE_GROUP_SHOW_MEMBER) {
            if (chatMessage.getContent().equals("1")) {
                content = getString(R.string.tip_owner_enable_member);
            } else {
                content = getString(R.string.tip_owner_disable_member);
            }
        } else if (type == XmppMessage.TYPE_GROUP_SEND_CARD) {
            if (chatMessage.getContent().equals("1")) {
                content = getString(R.string.tip_owner_enable_chat_privately);
            } else {
                content = getString(R.string.tip_owner_disable_chat_privately);
            }
        } else if (type == XmppMessage.TYPE_GROUP_ALL_SHAT_UP) {
            if (!chatMessage.getContent().equals("0")) {
                content = getString(R.string.tip_now_ban_all);
            } else {
                content = getString(R.string.tip_now_disable_ban_all);
            }
        } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_INVITE) {
            if (!chatMessage.getContent().equals("0")) {
                content = getString(R.string.tip_owner_enable_invite);
            } else {
                content = getString(R.string.tip_owner_disable_invite);
            }
        } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_UPLOAD) {
            if (!chatMessage.getContent().equals("0")) {
                content = getString(R.string.tip_owner_enable_upload);
            } else {
                content = getString(R.string.tip_owner_disable_upload);
            }
        } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_CONFERENCE) {
            if (!chatMessage.getContent().equals("0")) {
                content = getString(R.string.tip_owner_enable_meeting);
            } else {
                content = getString(R.string.tip_owner_disable_meeting);
            }
        } else if (type == XmppMessage.TYPE_GROUP_ALLOW_NORMAL_SEND_COURSE) {
            if (!chatMessage.getContent().equals("0")) {
                content = getString(R.string.tip_owner_enable_cource);
            } else {
                content = getString(R.string.tip_owner_disable_cource);
            }
        } else if (type == XmppMessage.TYPE_GROUP_TRANSFER) {
            content = MyApplication.getContext().getString(R.string.tip_new_group_owner_place_holder, chatMessage.getToUserName());
        } else if (type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME) {
            content = MyApplication.getContext().getString(R.string.tip_group_owner_update_msg_auto_destroy_time, DateFormatUtil.timeStr(Double.parseDouble(chatMessage.getContent())));
        } else if (type == XmppMessage.TYPE_ALLOW_OPEN_LIVE) {
            if (chatMessage.getContent().equals("1")) {
                content = getString(R.string.tip_owner_enable_live);
            } else {
                content = getString(R.string.tip_owner_disable_live);
            }
        }
        return content;
    }

    public static String getString(int resId) {
        return MyApplication.getContext().getString(resId);
    }
}
