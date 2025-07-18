package com.tongxin.caihong.bean.message;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tongxin.caihong.R;
import com.tongxin.caihong.db.dao.ChatMessageDao;
import com.tongxin.caihong.db.dao.ChatMessageDaoImpl;
import com.tongxin.caihong.helper.MessageSecureHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.StringUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.chat.weichat.bean.message
 * @作者:王阳
 * @创建时间: 2015年10月12日 上午11:59:36
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 聊天消息表, 其中会有上传字段的设置和解析字段的设置
 */
@DatabaseTable(daoClass = ChatMessageDaoImpl.class)
public class ChatMessage extends XmppMessage implements Parcelable {
    public static final Creator<ChatMessage> CREATOR = new Creator<ChatMessage>() {

        @Override
        public ChatMessage createFromParcel(Parcel source) {
            ChatMessage message = new ChatMessage();
            message._id = source.readInt();
            message.type = source.readInt();
            message.fromUserId = source.readString();
            message.fromUserName = source.readString();
            message.toUserId = source.readString();
            message.content = source.readString();
            message.location_x = source.readString();
            message.location_y = source.readString();
            message.filePath = source.readString();
            message.fileSize = source.readInt();
            message.timeLen = source.readInt();
            message.isReadDel = source.readInt();
            message.isEncrypt = source.readInt();
            message.signature = source.readString();
            message.deleteTime = source.readLong();
            message.objectId = source.readString();
            message.translation = source.readString();
            message.packetId = source.readString();
            message.timeSend = source.readDouble();
            // 不参与传输的字段
            message.messageState = source.readInt();
            message.fromId = source.readString();
            message.toId = source.readString();
            return message;
        }

        @Override
        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };
    // 多选，是否选中
    public boolean isMoreSelected;
    /* 本地额外存数数据 */
    @DatabaseField(generatedId = true)
    private int _id;
    @DatabaseField
    private String fromUserId;
    @DatabaseField
    private String fromUserName;// 发送者名称
    @DatabaseField
    private String toUserId;
    @DatabaseField
    private String toUserName;
    /**
     * 在不同的消息类型里，代表不同的含义：<br/>
     * {@link XmppMessage#TYPE_TEXT} 文字 <br/>
     * {@link XmppMessage#TYPE_IMAGE} 图片的Url<br/>
     * {@link XmppMessage#TYPE_VOICE} 语音的Url <br/>
     * {@link XmppMessage#TYPE_LOCATION} 地理<br/>
     * {@link XmppMessage#TYPE_GIF} Gif图的名称 <br/>
     * {@link XmppMessage#TYPE_TIP} 系统提示的字<br/>
     * {@link XmppMessage#TYPE_FILE} 文件的url<br/>
     */
    @DatabaseField
    private String content;
    @DatabaseField
    private String location_x;// 1.当为地理位置时，有效 2.特殊：当为图片时，该值为图片的宽度
    @DatabaseField
    private String location_y;// 1.当为地理位置时，有效 2.特殊：当为图片时，该值为图片的高度
    @DatabaseField
    private int fileSize;// 当为图片、语音消息时，此节点有效。图片、语音文件的大小
    @DatabaseField
    private String filePath;// 为语音视频图片文件的 本地路径（IOS端叫fileName），注意本地文件可能清除了，此节点代表的数据不一定有效
    @DatabaseField
    private int timeLen;// 当为语音消息时，此节点有效。语音信息的长度
    @DatabaseField
    private int isReadDel;
    @DatabaseField
    private int isEncrypt;
    @DatabaseField
    private String signature;// 消息签名 isEncrypt==3特有
    // 消息到期时间(当前时间+消息保存天数=到期时间)
    @DatabaseField
    private long deleteTime;
    @DatabaseField
    private String objectId;
    @DatabaseField
    private String translation;
    /**
     * 不参与传输的字段
     */
    // Smack Message 下的fromId与toId;
    @DatabaseField
    private String fromId;
    @DatabaseField
    private String toId;
    @DatabaseField
    private int messageState;// 只有当消息是我发出的，此节点才有效。消息的发送状态,默认值=0，代表发送中
    @DatabaseField
    private boolean isUpload;// 当为图片和语音类型是，此节点有效，代表是否上传完成，默认false。isMySend=true，此节点有效，
    @DatabaseField
    private int uploadSchedule;// 上传进度
    @DatabaseField
    private boolean isDownload;// 当为图片和语音类型是，此节点有效，代表是否下载完成，默认false。isMySend=false,此节点有效\
    @DatabaseField
    private boolean isVerifySignatureFailed;
    @DatabaseField(defaultValue = "0")
    // 该条消息是否过期
    // TODO 目前群组消息有一个问题，即消息过期了，本地在下次打开应用删除该条消息之后，调用XMPP加群获取离线消息有可能又将该消息拉下来了，添加该标志位记录过期消息，在群组查询时就过滤掉这种消息
    private int isExpired;
    @DatabaseField
    private boolean sendRead; // 代表我是否发送过已读， false代表我未读这条， true代表我已读这条消息
    @DatabaseField
    private boolean localRead; // 针对不发已读情况判断本地已读不显示小红点，
    @DatabaseField
    private int reSendCount;
    @DatabaseField
    private int readPersons;
    @DatabaseField
    private long readTime;
    // 消息序号
    @DatabaseField
    private long seqNo;
    @DatabaseField
    private String serviceId;
    /**
     * 局部变量
     */
    private boolean isGroup;
    private boolean showMucRead;
    private boolean isLoadRemark;
    // ChatContentView 显示专用
    private boolean isDecrypted;
    // 标记该条消息是否为离线消息
    private boolean isDelayMsg;
    // 标记该条消息是否为最后一条离线消息
    private boolean isDelayEndMsg;
    // type==907消息附带
    private String other;

    public ChatMessage() {
    }

    public ChatMessage(String jsonData) {
        parserJsonData(jsonData);
    }

    /**
     * 所有消息类型都用文字描述，
     * 比如[图片],
     * 部分类型可能返回空字符串，
     * <p>
     * todo 感觉这个方法没什么用，该更新的updateFriendContent方法内都更新好了，，，
     * MessageFragment使用ta的两个地方全部改为使用friend自己的content
     */
    @NonNull
    public static String getSimpleContent(Context ctx, int type, String content, String toUserId, String msgId) {
        switch (type) {
            case XmppMessage.TYPE_TEXT:
                break;
            case XmppMessage.TYPE_VOICE:
                content = ctx.getString(R.string.msg_voice);
                break;
            case XmppMessage.TYPE_GIF:
                content = ctx.getString(R.string.msg_animation);
                break;
            case XmppMessage.TYPE_IMAGE:
                content = ctx.getString(R.string.msg_picture);
                break;
            case XmppMessage.TYPE_VIDEO:
                content = ctx.getString(R.string.msg_video);
                break;
            case XmppMessage.TYPE_RED:
                content = ctx.getString(R.string.msg_red_packet);
                break;
            case XmppMessage.TYPE_LOCATION:
                content = ctx.getString(R.string.msg_location);
                break;
            case XmppMessage.TYPE_CARD:
                content = ctx.getString(R.string.msg_card);
                break;
            case XmppMessage.TYPE_FILE:
                content = ctx.getString(R.string.msg_file);
                break;
            case XmppMessage.TYPE_TIP:
                break;
            case XmppMessage.TYPE_IMAGE_TEXT:
            case XmppMessage.TYPE_IMAGE_TEXT_HTML:
            case XmppMessage.TYPE_IMAGE_TEXT_MANY:
                content = ctx.getString(R.string.msg_image_text);
                break;
            case XmppMessage.TYPE_LINK:
            case XmppMessage.TYPE_SHARE_LINK:
                content = ctx.getString(R.string.msg_link);
                break;
            case XmppMessage.TYPE_SHAKE:
                content = ctx.getString(R.string.msg_shake);
                break;
            case XmppMessage.TYPE_DICE:
            case XmppMessage.TYPE_RPS:
                content = ctx.getString(R.string.type_emoij);
                break;
            case XmppMessage.TYPE_MEETING_INVITE:
                content = ctx.getString(R.string.type_meeting_invite);
                break;
            case XmppMessage.TYPE_LIVE_INVITE:
                content = ctx.getString(R.string.type_live_invite);
                break;
            case XmppMessage.TYPE_INVITE_JOIN_ROOM:
                content = ctx.getString(R.string.type_invite_join_room);
                break;
            case XmppMessage.TYPE_COMPANY_INVITE_JOIN:
                content = ctx.getString(R.string.type_invite_join_company);
                break;
            case XmppMessage.TYPE_APPLET:
                content = ctx.getString(R.string.type_applet);
                break;
            case XmppMessage.TYPE_SHARE:
                // 分享了一条动态
                content = StringUtils.getShareContent(content);
                break;
            case XmppMessage.TYPE_CHAT_HISTORY:
                content = ctx.getString(R.string.msg_chat_history);
                break;
            case XmppMessage.TYPE_TRANSFER:
                content = ctx.getString(R.string.tip_transfer_money);
                break;
            case XmppMessage.TYPE_TRANSFER_RECEIVE:
                content = ctx.getString(R.string.tip_transfer_money) + ctx.getString(R.string.transfer_friend_sure_save);
                break;
            case XmppMessage.TYPE_TRANSFER_BACK:
                content = ctx.getString(R.string.transfer_back);
                break;
            case XmppMessage.TYPE_PAY_CERTIFICATE:
                content = ctx.getString(R.string.pay_certificate);
                break;
            case XmppMessage.TYPE_SCAN_RECHARGE:
                content = ctx.getString(R.string.scan_recharge_notify);
                break;
            case XmppMessage.TYPE_SCAN_WITHDRAW:
                content = ctx.getString(R.string.scan_withdraw_notify);
                break;
            case XmppMessage.TYPE_SECURE_LOST_KEY:
                content = ctx.getString(R.string.request_chat_key_group_thumb);
                break;
            case XmppMessage.TYPE_NEW_NOTICE:
                content = "[" + ctx.getString(R.string.group_bulletin) + "]";
                break;

            case XmppMessage.TYPE_NO_CONNECT_VOICE:
            case XmppMessage.TYPE_NO_CONNECT_VIDEO:
            case XmppMessage.TYPE_END_CONNECT_VOICE:
            case XmppMessage.TYPE_END_CONNECT_VIDEO:
                ChatMessage message = ChatMessageDao.getInstance().findMsgById(CoreManager.requireSelf(ctx).getUserId(), toUserId, msgId);
                if (message != null) {
                    content = StringUtils.getAudioMessageContent(message);
                } else if (TextUtils.isEmpty(content)) {
                    content = ctx.getString(R.string.unknown);
                }
                break;
            case XmppMessage.TYPE_IS_CONNECT_VOICE:
                content = ctx.getString(R.string.suffix_invite_you_voice);
                break;
            case XmppMessage.TYPE_IS_CONNECT_VIDEO:
                content = ctx.getString(R.string.suffix_invite_you_video);
                break;
            case XmppMessage.TYPE_IS_MU_START_CONNECT_VOICE:
                content = ctx.getString(R.string.tip_audio_meet_start);
                break;
            case XmppMessage.TYPE_IS_MU_START_CONNECT_VIDEO:
                content = ctx.getString(R.string.tip_video_meet_start);
                break;
            case XmppMessage.TYPE_IS_MU_END_CONNECT_VOICE:
                content = ctx.getString(R.string.tip_audio_meet_end);
                break;
            case XmppMessage.TYPE_IS_MU_END_CONNECT_VIDEO:
                content = ctx.getString(R.string.tip_video_meet_end);
                break;

            case XmppMessage.TYPE_SAYHELLO:// 打招呼
                content = ctx.getString(R.string.apply_to_add_me_as_a_friend);
                break;
            case XmppMessage.TYPE_PASS:    // 同意加好友
                content = ctx.getString(R.string.agree_with_my_plus_friend_request);
                break;
            case XmppMessage.TYPE_FRIEND:  // 直接成为好友
                content = ctx.getString(R.string.added_me_as_a_friend);
                break;

            case XmppMessage.DIANZAN:// 朋友圈点赞
                content = ctx.getString(R.string.notification_praise_me_life_circle);
                break;
            case XmppMessage.PINGLUN:    // 朋友圈评论
                content = ctx.getString(R.string.notification_comment_me_life_circle);
                break;
            case XmppMessage.ATMESEE:  // 朋友圈提醒我看
                content = ctx.getString(R.string.notification_at_me_life_circle);
                break;
            case XmppMessage.FRIEND_PUBLISH:  // 朋友圈好友动态更新
                content = ctx.getString(R.string.notification_refresh_life_circle);
                break;

        }
        return content;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_id);
        dest.writeInt(type);
        dest.writeString(fromUserId);
        dest.writeString(fromUserName);
        dest.writeString(toUserId);
        dest.writeString(location_x);
        dest.writeString(location_y);
        dest.writeString(content);
        dest.writeString(filePath);
        dest.writeInt(fileSize);
        dest.writeInt(timeLen);
        dest.writeInt(isReadDel);
        dest.writeInt(isEncrypt);
        dest.writeString(signature);
        dest.writeLong(deleteTime);
        dest.writeString(objectId);
        dest.writeString(translation);
        dest.writeString(packetId);
        // dest.writeLong(timeSend);
        dest.writeDouble(timeSend);
        // 不参与传输的字段
        dest.writeInt(messageState);
        dest.writeString(fromId);
        dest.writeString(toId);
    }

    public String toJsonString() {
        return toJsonString(null);
    }

    public String toJsonString(@Nullable String messageKey) {
        return toJsonObject(messageKey).toString();
    }

    public JSONObject toJsonObject(@Nullable String messageKey) {
        JSONObject object = new JSONObject();
        object.put("type", this.type);
        if (!TextUtils.isEmpty(this.fromUserId)) {
            object.put("fromUserId", this.fromUserId);
        }
        if (!TextUtils.isEmpty(this.fromUserName)) {
            object.put("fromUserName", this.fromUserName);
        }
        if (!TextUtils.isEmpty(this.toUserId)) {
            object.put("toUserId", this.toUserId);
        }
        if (!TextUtils.isEmpty(this.toUserName)) {
            object.put("toUserName", this.toUserName);
        }
        if (!TextUtils.isEmpty(this.content)) {
            object.put("content", this.content);
        }
        if (!TextUtils.isEmpty(this.location_x)) {
            object.put("location_x", this.location_x);
        }

        if (!TextUtils.isEmpty(this.location_y)) {
            object.put("location_y", this.location_y);
        }
        if (this.fileSize > 0) {
            object.put("fileSize", this.fileSize);
        }
        // 增加filePath
        if (!TextUtils.isEmpty(this.filePath)) {
            object.put("fileName", this.filePath);
        }
        if (this.timeLen > 0) {
            object.put("fileTime", this.timeLen);
        }
        if (isReadDel != 0) {
            object.put("isReadDel", this.isReadDel);
        }
        if (isEncrypt != 0) {
            object.put("isEncrypt", this.isEncrypt);
        }
        if (!TextUtils.isEmpty(signature)) {
            object.put("signature", this.signature);
        }
        object.put("deleteTime", this.deleteTime);
        if (!TextUtils.isEmpty(this.objectId)) {
            object.put("objectId", this.objectId);
        }
        object.put("messageId", this.packetId);
        object.put("timeSend", this.timeSend);
        object.put("srvId", this.serviceId);

        if (messageKey != null) {
            MessageSecureHelper.mac(messageKey, object);
        }
        return object;
    }

    /**
     * 解析接收到的消息
     */
    private void parserJsonData(String jsonData) {
        try {
            JSONObject jObject = JSON.parseObject(jsonData);
            type = getIntValueFromJSONObject(jObject, "type");
            fromUserId = getStringValueFromJSONObject(jObject, "fromUserId");
            fromUserName = getStringValueFromJSONObject(jObject, "fromUserName");
            toUserId = getStringValueFromJSONObject(jObject, "toUserId");
            toUserName = getStringValueFromJSONObject(jObject, "toUserName");
            content = getStringValueFromJSONObject(jObject, "content");
            location_x = getStringValueFromJSONObject(jObject, "location_x");
            location_y = getStringValueFromJSONObject(jObject, "location_y");
            fileSize = getIntValueFromJSONObject(jObject, "fileSize");
            filePath = getStringValueFromJSONObject(jObject, "fileName");// 增加解析文件路径
            timeLen = getIntValueFromJSONObject(jObject, "fileTime");
            isReadDel = getIntValueFromJSONObject(jObject, "isReadDel");
            isEncrypt = getIntValueFromJSONObject(jObject, "isEncrypt");
            signature = getStringValueFromJSONObject(jObject, "signature");
            deleteTime = getLongValueFromJSONObject(jObject, "deleteTime");
            objectId = getStringValueFromJSONObject(jObject, "objectId");
            packetId = getStringValueFromJSONObject(jObject, "messageId");
            timeSend = getDoubleFromJSONObject(jObject, "timeSend");
            serviceId = getStringValueFromJSONObject(jObject, "srvId");

            isMySend = false;
            isDownload = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLocation_x() {
        return location_x;
    }

    public void setLocation_x(String location_x) {
        this.location_x = location_x;
    }

    public String getLocation_y() {
        return location_y;
    }

    public void setLocation_y(String location_y) {
        this.location_y = location_y;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(int timeLen) {
        this.timeLen = timeLen;
    }

    public boolean getIsReadDel() {
        return isReadDel == 1;
    }

    public void setIsReadDel(int isReadDel) {
        this.isReadDel = isReadDel;
    }

    public int getIsEncrypt() {
        return isEncrypt;
    }

    public void setIsEncrypt(int isEncrypt) {
        this.isEncrypt = isEncrypt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(long deleteTime) {
        this.deleteTime = deleteTime;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public int getMessageState() {
        return messageState;
    }

    public void setMessageState(int messageState) {
        this.messageState = messageState;
    }

    public boolean isUpload() {
        return isUpload;
    }

    public void setUpload(boolean upload) {
        isUpload = upload;
    }

    public int getUploadSchedule() {
        return uploadSchedule;
    }

    public void setUploadSchedule(int uploadSchedule) {
        this.uploadSchedule = uploadSchedule;
    }

    public boolean isDownload() {
        return isDownload;
    }

    public void setDownload(boolean download) {
        isDownload = download;
    }

    public boolean isVerifySignatureFailed() {
        return isVerifySignatureFailed;
    }

    public void setVerifySignatureFailed(boolean verifySignatureFailed) {
        isVerifySignatureFailed = verifySignatureFailed;
    }

    public int getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(int isExpired) {
        this.isExpired = isExpired;
    }

    public boolean isSendRead() {
        return sendRead;
    }

    public void setSendRead(boolean sendRead) {
        this.sendRead = sendRead;
    }

    public boolean isLocalRead() {
        return localRead;
    }

    public void setLocalRead(boolean localRead) {
        this.localRead = localRead;
    }

    public int getReSendCount() {
        return reSendCount;
    }

    public void setReSendCount(int reSendCount) {
        this.reSendCount = reSendCount;
    }

    public int getReadPersons() {
        return readPersons;
    }

    public void setReadPersons(int readPersons) {
        this.readPersons = readPersons;
    }

    public long getReadTime() {
        return readTime;
    }

    public void setReadTime(long readTime) {
        this.readTime = readTime;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public boolean isShowMucRead() {
        return showMucRead;
    }

    public void setShowMucRead(boolean showMucRead) {
        this.showMucRead = showMucRead;
    }

    public boolean isLoadRemark() {
        return isLoadRemark;
    }

    public void setLoadRemark(boolean loadRemark) {
        isLoadRemark = loadRemark;
    }

    public boolean isMoreSelected() {
        return isMoreSelected;
    }

    public void setMoreSelected(boolean moreSelected) {
        isMoreSelected = moreSelected;
    }

    public boolean isDecrypted() {
        return isDecrypted;
    }

    public void setDecrypted(boolean decrypted) {
        isDecrypted = decrypted;
    }

    public boolean isDelayMsg() {
        return isDelayMsg;
    }

    public void setDelayMsg(boolean delayMsg) {
        isDelayMsg = delayMsg;
    }

    public boolean isDelayEndMsg() {
        return isDelayEndMsg;
    }

    public void setDelayEndMsg(boolean delayEndMsg) {
        isDelayEndMsg = delayEndMsg;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public boolean validate() {
        // return type != 0 && !TextUtils.isEmpty(fromUserId) && !TextUtils.isEmpty(fromUserName) && timeSend != 0;
        if (TextUtils.isEmpty(fromUserName)) {
            fromUserName = "unknow";
        }
        return type != 0 && !TextUtils.isEmpty(fromUserId) && timeSend != 0;
    }

    public boolean isExpired() {
        return deleteTime != 0 && deleteTime != -1 && deleteTime < TimeUtils.sk_time_current_time() / 1000;
    }

    /**
     * 只clone传输的数据
     */
    public ChatMessage clone(boolean isGroup) {
        String json = this.toJsonString();
        return new ChatMessage(json);
    }

    /**
     * clone 该条消息的所有数据(因为现在发消息都是先存后发，但存入到数据库的消息content都做了加密，
     * 又因为引用变量的问题，导致发出去的消息也加密了，将消息完完全全的clone一份存入数据库
     *
     * @return
     */
    public ChatMessage cloneAll() {
        ChatMessage message = clone(false);
        message.isMySend = this.isMySend;
        message.fromId = this.fromId;
        message.toId = this.toId;
        message.messageState = this.messageState;
        message.isUpload = this.isUpload;
        message.uploadSchedule = this.uploadSchedule;
        message.isDownload = this.isDownload;
        message.isVerifySignatureFailed = this.isVerifySignatureFailed;
        message.isExpired = this.isExpired;
        message.signature = this.signature;
        message.sendRead = this.sendRead;
        message.localRead = this.localRead;
        message.reSendCount = this.reSendCount;
        message.readPersons = this.readPersons;
        message.readTime = this.readTime;
        message.seqNo = this.seqNo;
        message.serviceId = this.serviceId;
        message.isGroup = this.isGroup;
        message.showMucRead = this.showMucRead;
        message.isLoadRemark = this.isLoadRemark;
        message.isMoreSelected = this.isMoreSelected;
        message.isDecrypted = this.isDecrypted;
        message.other = this.other;
        return message;
    }

    /**
     * 所有消息类型都用文字描述，
     * 比如[图片],
     * 部分类型可能返回空字符串，
     */
    @NonNull
    public String getSimpleContent(Context ctx) {
        String content;
        switch (getType()) {
            case XmppMessage.TYPE_REPLAY:
            case XmppMessage.TYPE_TEXT:
                if (getIsReadDel()) {
                    content = ctx.getString(R.string.tip_click_to_read);
                } else {
                    content = getContent();
                }
                break;
            default:
                content = StringUtils.getMessageContent(this, false);
                break;
        }
        return content;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", fromUserId='" + fromUserId + '\'' +
                ", fromUserName='" + fromUserName + '\'' +
                ", toUserId='" + toUserId + '\'' +
                ", toUserName='" + toUserName + '\'' +
                ", content='" + content + '\'' +
                ", location_x='" + location_x + '\'' +
                ", location_y='" + location_y + '\'' +
                ", fileSize=" + fileSize +
                ", filePath='" + filePath + '\'' +
                ", timeLen=" + timeLen +
                ", isReadDel=" + isReadDel +
                ", isEncrypt=" + isEncrypt +
                ", deleteTime=" + deleteTime +
                ", objectId='" + objectId + '\'' +
                ", packedId=" + packetId +
                ", messageId='" + packetId + '\'' +
                ", timeSend=" + timeSend +
                ", fromId='" + fromId + '\'' +
                ", messageState=" + messageState +
                ", isUpload=" + isUpload +
                ", uploadSchedule=" + uploadSchedule +
                ", isDownload=" + isDownload +
                ", isVerifySignatureFailed=" + isVerifySignatureFailed +
                ", isExpired=" + isExpired +
                ", signature=" + signature +
                ", sendRead=" + sendRead +
                ", localRead=" + localRead +
                ", reSendCount=" + reSendCount +
                ", readPersons=" + readPersons +
                ", readTime=" + readTime +
                ", isGroup=" + isGroup +
                ", showMucRead=" + showMucRead +
                ", isLoadRemark=" + isLoadRemark +
                ", other=" + other +
                ", srvId=" + serviceId +
                '}';
    }
}
