package com.tongxin.caihong.db;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.Share;
import com.tongxin.caihong.bean.circle.PublicMessage;
import com.tongxin.caihong.bean.collection.Message;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.helper.ImageLoadHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.ui.message.InstantMessageActivity;
import com.tongxin.caihong.util.BitmapUtil;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.DateFormatUtil;
import com.tongxin.caihong.util.FileUtil;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.xmpp.ListenerManager;
import com.tongxin.caihong.xmpp.util.ConvertMessage;

import java.util.UUID;

public class MessageUtil {

    /**
     * 是否为我的设备支持的消息
     * 1-9、26、40、41、80、81、82、84、85、87
     *
     * @return
     */
    public static boolean isDeviceSupportMessage(int type) {
        if ((type >= XmppMessage.TYPE_TEXT && type <= XmppMessage.TYPE_FILE)
                || type == XmppMessage.TYPE_READ
                || type == XmppMessage.TYPE_DICE || type == XmppMessage.TYPE_RPS
                || type == XmppMessage.TYPE_IMAGE_TEXT_HTML
                || type == XmppMessage.TYPE_IMAGE_TEXT || type == XmppMessage.TYPE_IMAGE_TEXT_MANY
                || type == XmppMessage.TYPE_LINK
                || type == XmppMessage.TYPE_SHAKE
                || type == XmppMessage.TYPE_CHAT_HISTORY
                || type == XmppMessage.TYPE_SHARE_LINK
                || type == XmppMessage.TYPE_REPLAY) {
            return true;
        }
        return false;
    }

    /**
     * 消息是否需要序号
     * 1-9、26、28、29、40、41、80、81、82、84、85、87
     *
     * @return
     */
    public static boolean isNeedSeqNoMessage(int type) {
        if ((type >= XmppMessage.TYPE_TEXT && type <= XmppMessage.TYPE_FILE)
                || type == XmppMessage.TYPE_RED || type == XmppMessage.TYPE_TRANSFER
                || type == XmppMessage.TYPE_DICE || type == XmppMessage.TYPE_RPS
                || (type >= XmppMessage.TYPE_MEETING_INVITE && type <= XmppMessage.TYPE_APPLET)
                || type == XmppMessage.TYPE_IMAGE_TEXT_HTML
                || type == XmppMessage.TYPE_IMAGE_TEXT || type == XmppMessage.TYPE_IMAGE_TEXT_MANY
                || type == XmppMessage.TYPE_LINK
                || type == XmppMessage.TYPE_SHAKE
                || type == XmppMessage.TYPE_CHAT_HISTORY
                || type == XmppMessage.TYPE_SHARE_LINK
                || type == XmppMessage.TYPE_REQUEST_COMMENT
                || type == XmppMessage.TYPE_SERVICE_FORWARD
                || type == XmppMessage.TYPE_SERVICE_MENU
                || type == XmppMessage.TYPE_REPLAY
                || type == XmppMessage.TYPE_BACK
                || type == XmppMessage.TYPE_SECURE_LOST_KEY
                || type == XmppMessage.TYPE_SECURE_SEND_KEY
                || type == XmppMessage.TYPE_NO_CONNECT_VOICE || type == XmppMessage.TYPE_END_CONNECT_VOICE
                || type == XmppMessage.TYPE_NO_CONNECT_VIDEO || type == XmppMessage.TYPE_END_CONNECT_VIDEO) {
            return true;
        }
        return false;
    }


    /**
     * 过滤漫游下来的消息
     * 1-9、28、29、40、41、80-89、94、804
     * groupChat：401-403、901-907、913、915-925、932、934、941
     *
     * @return 返回true表示这条漫游消息需要保存处理，false就无视该消息，
     */
    public static boolean filterRoamingMessage(int type, boolean isGroup) {
        if (isGroup) {
            if ((type >= XmppMessage.TYPE_TEXT && type <= XmppMessage.TYPE_FILE)
                    || type == XmppMessage.TYPE_RED || type == XmppMessage.TYPE_TRANSFER
                    || type == XmppMessage.TYPE_DICE || type == XmppMessage.TYPE_RPS
                    || (type >= XmppMessage.TYPE_MEETING_INVITE && type <= XmppMessage.TYPE_APPLET)
                    || type == XmppMessage.TYPE_IMAGE_TEXT_HTML
                    || (type >= XmppMessage.TYPE_IMAGE_TEXT && type <= XmppMessage.TYPE_TRANSFER_BACK)
                    || type == XmppMessage.TYPE_REQUEST_COMMENT
                    || type == XmppMessage.TYPE_SERVICE_FORWARD
                    || type == XmppMessage.TYPE_SERVICE_MENU
                    || type == XmppMessage.TYPE_REPLAY
                    || type == XmppMessage.TYPE_SECURE_LOST_KEY
                    || (type >= XmppMessage.TYPE_MUCFILE_ADD && type <= XmppMessage.TYPE_MUCFILE_DOWN)
                    || (type >= XmppMessage.TYPE_CHANGE_NICK_NAME && type <= XmppMessage.NEW_MEMBER)
                    || type == XmppMessage.TYPE_SEND_MANAGER
                    || (type >= XmppMessage.TYPE_CHANGE_SHOW_READ && type <= XmppMessage.TYPE_GROUP_TRANSFER)
                    || type == XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME
                    || type == XmppMessage.TYPE_EDIT_GROUP_NOTICE
                    || type == XmppMessage.TYPE_ALLOW_OPEN_LIVE
                    || type == XmppMessage.TYPE_ROOM_MODIFY_CARD) {
                return true;
            }
        } else {
            if ((type >= XmppMessage.TYPE_TEXT && type <= XmppMessage.TYPE_FILE)
                    || type == XmppMessage.TYPE_RED || type == XmppMessage.TYPE_TRANSFER
                    || type == XmppMessage.TYPE_DICE || type == XmppMessage.TYPE_RPS
                    || (type >= XmppMessage.TYPE_MEETING_INVITE && type <= XmppMessage.TYPE_APPLET)
                    || type == XmppMessage.TYPE_IMAGE_TEXT_HTML
                    || (type >= XmppMessage.TYPE_IMAGE_TEXT && type <= XmppMessage.TYPE_TRANSFER_BACK)
                    || type == XmppMessage.TYPE_REQUEST_COMMENT
                    || type == XmppMessage.TYPE_SERVICE_FORWARD
                    || type == XmppMessage.TYPE_SERVICE_MENU
                    || type == XmppMessage.TYPE_REPLAY
                    || type == XmppMessage.TYPE_NO_CONNECT_VOICE || type == XmppMessage.TYPE_END_CONNECT_VOICE
                    || type == XmppMessage.TYPE_NO_CONNECT_VIDEO || type == XmppMessage.TYPE_END_CONNECT_VIDEO) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理漫游下来的特殊类型的消息
     *
     * @param chatMessage
     * @return
     */
    public static void handleRoamingSpecialMessage(ChatMessage chatMessage) {
        String content = ConvertMessage.convertSupportChatMessage(chatMessage.getType(), CoreManager.requireSelf(MyApplication.getContext()).getUserId(), chatMessage);
        if (!TextUtils.isEmpty(content)) {
            chatMessage.setType(XmppMessage.TYPE_TIP);
            chatMessage.setContent(content);
        }
    }

    /**
     * 处理处理tigase/getLastChatList 获取到的特殊消息
     * todo 有待改进
     */
    public static String handlerGetLastSpecialMessage(int isRoom, int type, String loginUserId, String from, String fromUserName, String toUserName) {
        String text = "";
        if (type == XmppMessage.TYPE_BACK) {
            if (TextUtils.equals(from, loginUserId)) {
                text = MyApplication.getContext().getString(R.string.you) + " " + MyApplication.getInstance().getString(R.string.other_with_draw);
            } else {
                text = fromUserName + " " + MyApplication.getInstance().getString(R.string.other_with_draw);
            }
        } else if (type == XmppMessage.TYPE_83) {
            // 单聊群聊一样的处理，
            if (TextUtils.equals(from, loginUserId)) {
                // 我领取了别人的红包 正常聊天该条消息是不会显示的，但是获取漫游的时候能将该条消息拉下来
                text = MyApplication.getContext().getString(R.string.red_received_self, toUserName);
            } else {
                // 别人领取了我的红包
                text = MyApplication.getContext().getString(R.string.tip_receive_red_packet_place_holder, fromUserName, MyApplication.getContext().getString(R.string.you));
            }
        } else if (type == XmppMessage.TYPE_RED_BACK) {
            text = MyApplication.getContext().getString(R.string.tip_red_back);
        } else if (type == XmppMessage.TYPE_TRANSFER_RECEIVE) {
            if (TextUtils.equals(from, loginUserId)) {
                // 我领取了对方的转账 正常聊天该条消息是不会显示的，但是获取漫游的时候能将该条消息拉下来
                text = MyApplication.getContext().getString(R.string.transfer_received_self);
            } else {
                // 对方领取了我的转账
                text = MyApplication.getContext().getString(R.string.transfer_received);
            }
        }
        return text;
    }

    /**
     * 生成提示类型消息
     */
    public static void generateTipMessage(String ownerId, String userId, String content, boolean isGroup) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TIP);
        chatMessage.setContent(content);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, userId, chatMessage)) {
            ListenerManager.getInstance().notifyNewMesssage(ownerId, userId, chatMessage, isGroup);
        }
    }

    /**
     * 生成邀请观看直播转发消息
     *
     * @param ownerId
     * @param liveRoomId
     * @return 消息id
     */
    public static String generateInviteWatchLiveMessage(String ownerId, String liveRoomId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_LIVE_INVITE);
        chatMessage.setContent(MyApplication.getContext().getString(R.string.type_live_invite2));
        chatMessage.setObjectId(liveRoomId);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, AppConstant.NORMAL_INSTANT_ID, chatMessage)) {
            return chatMessage.getPacketId();
        }
        return null;
    }

    /**
     * 生成分享消息并去分享
     *
     * @param context
     * @param ownerId
     * @param source
     * @param message
     * @return
     */
    public static ChatMessage generateShareMessage2Intent(Context context, String ownerId, int source, PublicMessage message, boolean isIntent) {
        ChatMessage chatMessage = new ChatMessage();
        //收藏来源类型 0其他 1朋友圈 2视界 3群组 4单聊 (当在朋友圈时，没有targetType，此时默认值就是5用于判断)
        if (message.getTargetType() != 1 && message.getTargetType() != 2 && message.getTargetType() != 5
                || (message.getType() == PublicMessage.TYPE_SINGLE_IMAGE || message.getType() == PublicMessage.TYPE_SINGLE_VIDEO)) {
            /**
             * 分享来自单群聊的收藏、来自社交圈单独收藏的图片与视频
             */
            chatMessage.setFromUserId(AppConstant.NORMAL_INSTANT_ID);
            chatMessage.setFromUserName(AppConstant.NORMAL_INSTANT_ID);

            if (message.getType() == PublicMessage.TYPE_TEXT) {
                chatMessage.setType(XmppMessage.TYPE_TEXT);
                chatMessage.setContent(message.getBody().getText());
            } else if (message.getType() == PublicMessage.TYPE_IMG || message.getType() == PublicMessage.TYPE_SINGLE_IMAGE) {
                chatMessage.setType(XmppMessage.TYPE_IMAGE);
                chatMessage.setContent(message.getFirstImageOriginal());
            } else if (message.getType() == PublicMessage.TYPE_VOICE) {
                chatMessage.setType(XmppMessage.TYPE_VOICE);
                chatMessage.setContent(message.getFirstAudio());
                chatMessage.setTimeLen((int) message.getBody().getAudios().get(0).getLength());
            } else if (message.getType() == PublicMessage.TYPE_VIDEO || message.getType() == PublicMessage.TYPE_SINGLE_VIDEO) {
                chatMessage.setType(XmppMessage.TYPE_VIDEO);
                chatMessage.setContent(message.getFirstVideo());
            } else if (message.getType() == PublicMessage.TYPE_FILE) {
                chatMessage.setType(XmppMessage.TYPE_FILE);
                chatMessage.setContent(message.getFirstFile());
                chatMessage.setFilePath(message.getFileName());
                chatMessage.setFileSize((int) message.getBody().getFiles().get(0).getSize());
            } else if (message.getType() == PublicMessage.TYPE_LINK) {
                chatMessage.setType(XmppMessage.TYPE_LINK);
                Message msg = new Message();
                msg.setImg(message.getBody().getSdkIcon());
                msg.setTitle(message.getBody().getSdkTitle());
                msg.setUrl(message.getBody().getSdkUrl());
                chatMessage.setContent(JSON.toJSONString(msg));
            }

            if (message.getType() == XmppMessage.TYPE_VOICE
                    || message.getType() == XmppMessage.TYPE_IMAGE
                    || message.getType() == XmppMessage.TYPE_VIDEO) {
                int isReadDel = PreferenceUtils.getInt(context, Constants.MESSAGE_READ_FIRE + ownerId + ownerId, 0);
                chatMessage.setIsReadDel(isReadDel);
                chatMessage.setUpload(true);
            }
        } else {
            /**
             * 分享整个社交圈或视界
             */
            chatMessage.setType(XmppMessage.TYPE_SHARE);
            chatMessage.setFromUserId(source == 0 ? AppConstant.DYNAMIC_INSTANT_ID
                    : AppConstant.TRILL_INSTANT_ID);
            chatMessage.setFromUserName(source == 0 ? AppConstant.DYNAMIC_INSTANT_ID
                    : AppConstant.TRILL_INSTANT_ID);

            Share share = new Share();
            share.setSource(source);
            share.setId(message.getMessageId());
            share.setPublisherName(message.getNickName());
            share.setText(message.getBody().getText());
            if (!TextUtils.isEmpty(message.getEmojiId())) {
                share.setCollect(1);
            }
            if (source == 0) {
                // 生活圈
                share.setType(message.getType() - 1);
                if (message.getType() == PublicMessage.TYPE_IMG) {
                    share.setImage(message.getFirstImageOriginal());
                } else if (message.getType() == PublicMessage.TYPE_VOICE) {
                    share.setTimeLen(message.getBody().getAudios().get(0).getLength());
                } else if (message.getType() == PublicMessage.TYPE_VIDEO) {
                    share.setImage(message.getFirstImageOriginal());
                    share.setVideoUrl(message.getFirstVideo());
                    share.setTimeLen(message.getBody().getVideos().get(0).getLength());
                } else if (message.getType() == PublicMessage.TYPE_FILE) {
                    String fileName;
                    if (!TextUtils.isEmpty(message.getFileName())) {
                        fileName = message.getFileName();
                    } else {
                        try {
                            fileName = message.getFirstFile().substring(message.getFirstFile().lastIndexOf('/') + 1);
                        } catch (Exception e) {
                            fileName = message.getFileName();
                        }
                    }
                    share.setFileName(fileName);
                    if (message.getFirstFile() != null) {
                        int index = message.getFirstFile().lastIndexOf(".");
                        String suffix = message.getFirstFile().substring(index + 1).toLowerCase();
                        share.setFileType(FileUtil.suffix2Type(suffix));
                    } else {
                        share.setFileType(9);
                    }
                } else if (message.getType() == PublicMessage.TYPE_LINK) {
                    share.setImage(message.getBody().getSdkIcon());
                }
            } else {
                // 视界
                share.setImage(message.getFirstImageOriginal());
                share.setVideoUrl(message.getBody().getVideos().get(0).getOriginalUrl());
                share.setTimeLen(message.getBody().getVideos().get(0).getLength());
            }
            chatMessage.setContent(Share.share2Json(share));
        }
        chatMessage.setToUserId(ownerId);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
        if (isIntent) {
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, chatMessage.getFromUserId(), chatMessage)) {
                Intent intent = new Intent(context, InstantMessageActivity.class);
                intent.putExtra("fromUserId", chatMessage.getFromUserId());
                intent.putExtra("messageId", chatMessage.getPacketId());
                context.startActivity(intent);
            }
        }
        return chatMessage;
    }

    /**
     * 生成图片消息并去转发
     * 用于多个大图预览类
     *
     * @param context
     * @param ownerId
     * @return
     */
    public static void generateImageMessage2Intent(Context context, String ownerId, String url) {
        ImageLoadHelper.loadFile(context, url, f -> {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setFromUserId(AppConstant.NORMAL_INSTANT_ID);
            chatMessage.setFromUserName(AppConstant.NORMAL_INSTANT_ID);

            int[] imageParam = BitmapUtil.getImageParamByIntsFile(f.getPath());
            chatMessage.setLocation_x(String.valueOf(imageParam[0]));
            chatMessage.setLocation_y(String.valueOf(imageParam[1]));

            chatMessage.setType(XmppMessage.TYPE_IMAGE);
            chatMessage.setContent(url);
            int isReadDel = PreferenceUtils.getInt(context, Constants.MESSAGE_READ_FIRE + ownerId + ownerId, 0);
            chatMessage.setIsReadDel(isReadDel);
            chatMessage.setUpload(true);
            chatMessage.setToUserId(ownerId);
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
            chatMessage.setTimeSend(TimeUtils.sk_time_current_time());
            if (ChatMessageDao.getInstance().saveNewSingleChatMessage(ownerId, chatMessage.getFromUserId(), chatMessage)) {
                Intent intent = new Intent(context, InstantMessageActivity.class);
                intent.putExtra("fromUserId", chatMessage.getFromUserId());
                intent.putExtra("messageId", chatMessage.getPacketId());
                context.startActivity(intent);
            }
        });

        return;
    }

    /**
     * 生成设置消息过期自动销毁通知
     *
     * @param userId
     * @param userName
     * @param toUserId
     * @param content
     * @return
     */
    public static ChatMessage generateMessageExpiredAutoDeleteTip(String userId, String userName, String toUserId, double content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_GROUP_UPDATE_MSG_AUTO_DESTROY_TIME);
        chatMessage.setFromUserId(userId);
        chatMessage.setFromUserName(userName);
        chatMessage.setToUserId(toUserId);
        chatMessage.setContent(String.valueOf(content));
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));// 随机产生一个PacketId
        chatMessage.setTimeSend(TimeUtils.sk_time_current_time());

        // 自己本地存的话还是转成tip消息存储
        ChatMessage clone = chatMessage.clone(false);
        clone.setType(XmppMessage.TYPE_TIP);
        if (content == 0 || content == -1) {
            clone.setContent(MyApplication.getContext().getString(R.string.tip_set_msg_no_auto_delete));
        } else {
            clone.setContent(MyApplication.getContext().getString(R.string.tip_set_msg_auto_delete, DateFormatUtil.timeStr(content), DateFormatUtil.timeStr(content)));
        }
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(userId, toUserId, clone)) {
            ListenerManager.getInstance().notifyNewMesssage(userId, userId, clone, false);
        }

        return chatMessage;
    }
}
