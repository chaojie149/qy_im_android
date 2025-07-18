package com.tongxin.caihong;


@SuppressWarnings("unused")
public class AppConstant {
    /**
     * 某些地方选择数据使用的常量
     */
    public static final String EXTRA_ACTION = "action";// 进入这个类的执行的操作
    public static final int ACTION_NONE = 0;  // 不执行操作
    public static final int ACTION_SELECT = 1;// 执行选择操作
    public static final String EXTRA_SELECT_ID = "select_id";// 选择对应项目的id
    public static final String EXTRA_SELECT_NAME = "select_name";// 选择的对应项目的名称
    public static final String EXTRA_FORM_CAHT_ACTIVITY = "from_chat_activity";
    /**
     * 某些地方需要传递如ListView Position的数据
     */
    public static final String EXTRA_POSITION = "position";

    // 用户信息参数，很多地方需要
    public static final String EXTRA_USER = "user";// user
    public static final String EXTRA_USER_ACCOUNT = "account";// account
    public static final String EXTRA_USER_ID = "userId";// userId
    public static final String EXTRA_NICK_NAME = "nickName";// nickName
    public static final String EXTRA_MESSAGE_ID = "messageId";
    public static final String EXTRA_IS_GROUP_CHAT = "isGroupChat";// 是否是群聊
    public static final String EXTRA_MEMBER_NUM = "MemberNum";

    // BusinessCircleActivity需要的
    public static final String EXTRA_CIRCLE_TYPE = "circle_type";// 看的商务圈类型
    public static final int CIRCLE_TYPE_MY_BUSINESS = 0;// 看的商务圈类型,是我的商务圈
    public static final int CIRCLE_TYPE_PERSONAL_SPACE = 1;// 看的商务圈类型，是个人空间

    // 入群方式传参
    public static final String GROUP_ADD_STYLE = "operationType";

    // room/join接口：通过搜索入群
    public static final String GROUP_JOIN_SEARCH = "0";
    // room/join接口：通过扫码入群
    public static final String GROUP_JOIN_SCAN = "1";
    // room/join接口：通过附近入群
    public static final String GROUP_JOIN_NEAR = "2";
    // room/join接口：通过榜单入群
    public static final String GROUP_JOIN_LIST = "3";
    // room/join接口：通过搜索付费入群
    public static final String GROUP_JOIN_SEARCH_PAY = "4";
    // room/join接口：通过扫码付费入群
    public static final String GROUP_JOIN_SCAN_PAY = "5";
    // room/join接口：通过附近付费入群
    public static final String GROUP_JOIN_NEAR_PAY = "6";
    // room/join接口：通过榜单付费入群
    public static final String GROUP_JOIN_LIST_PAY = "7";

    // room/inviteJoinRoom接口：邀请我，我开启入群验证，我同意入群
    public static final String GROUP_INVITE_JOIN = "0";
    // room/inviteJoinRoom接口：邀请我，我开启入群验证，我同意付费入群
    public static final String GROUP_INVITE_JOIN_PAY = "1";

    // room/member/update接口：其他途径入群
    public static final String GROUP_INVITE_OTHER = "0";
    // room/member/update接口：审核通过
    public static final String GROUP_INVITE_AUDIT = "1";
    // room/member/update接口：用户邀请入群
    public static final String GROUP_INVITE_FRIEND = "2";

    /**
     * 商务圈发布的常量
     */
    /* 发说说(图文) */
    public static final String EXTRA_IMAGES = "images";// 预览的那组图片
    public static final String EXTRA_CHANGE_SELECTED = "change_selected";// 是否可以改变选择，这样在ActivityResult中会回传重新选择的结果

    public static final String EXTRA_MSG_ID = "msg_id";// 公共消息id
    public static final String EXTRA_FILE_PATH = "file_path";// 语音、视频文件路径
    public static final String FILE_PAT_NAME = "file_name";//文件的名字
    public static final String EXTRA_IMAGE_FILE_PATH = "image_file_path";// 图片文件路径
    public static final String EXTRA_TIME_LEN = "time_len";// 语音、视频文件时长
    //位置经纬度
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";
    // 位置地图截图，
    public static final String EXTRA_SNAPSHOT = "snapshot";

    /* IM */
    public static final String EXTRA_FRIEND = "friend";
    public static final String EXTRA_CHAT_MESSAGE = "chatMessage";

    /* 进入SingleImagePreviewActivity需要带上的参数 */
    public static final String EXTRA_IMAGE_URI = "image_uri";

    /* 进入ChatVideoPreviewActivity需要带上的参数 */
    public static final String EXTRA_VIDEO_FILE_URI = "video_file_url";
    public static final String EXTRA_VIDEO_FILE_PATH = "video_file_path";
    // 个人短视频预览带的特殊参数
    public static final String EXTRA_VIDEO_FILE_THUMB = "video_file_thumb";

    // 传出参数，选择的视频列表，
    public static final String EXTRA_VIDEO_LIST = "video_list";
    // 传入参数，是否支持多选，
    public static final String EXTRA_MULTI_SELECT = "multi_select";

    // 服务端集群需要的area参数
    public static final String EXTRA_CLUSTER_AREA = "cluster_area";

    public static final String PRIVATE_KEY_DH = "private_key_dh";
    public static final String PRIVATE_KEY_DH_PUBLIC = "guizhoukangniwangluo";
    public static final String PRIVATE_KEY_RSA = "private_key_rsa";
    public static final String PRIVATE_KEY_RSA_PUBLIC = "private_key_rsa_public";
    public static final String EXTRA_REAL_PASSWORD = "real_password";
    public static final String FIND_PASSWORD_STATUS = "find_password_status";

    public static final String PRIVATE_MODE = "private_mode";

    // 支付密码长度
    public static final int PASS_WORD_LENGTH = 6;

    /**
     * 账单提现常量
     */
    public static final int MANUAL_PAY_RECHARGE = 18;// 扫码手动充值
    public static final int MANUAL_PAY_WITHDRAW = 19;// 扫码手动提现

    // 延时触发事件的延时时间
    public static final long DELAY_MILLIS_TRIGGER_EVENT = 1200;

    /**
     * 非多选转发消息时，InstantMessageActivity界面getIntent要求的传值为fromUserId与messageId
     * 而不是消息，所以之前的做法是在转发前将消息封装好，fromUserId定义为一个陌生id，10010
     * 再将消息存入数据库内，之后在InstantMessageActivity内通过fromUserId与messageId取出该条消息进行后续逻辑
     * 但是先引入了短视频转发与商品转发，这俩个转发都需要调用不同的接口来标记，所以这里定义下他们两的fromUserId，专用
     */
    public static final String NORMAL_INSTANT_ID = "10010";
    public static final String TRILL_INSTANT_ID = "10020";
    public static final String DYNAMIC_INSTANT_ID = "10040";
    public static final String APPLET_INSTANT_ID = "10080";

    // 是否勾选了签到不在提醒(即首页签到ui不展示)
    public static final String SIGN_IN_NO_REMIND = "sing_in_no_remind";
    // 是否有申请加入公司消息
    public static final String COMPANY_APPLY_JOIN_MSG = "company_apply_join_msg";
}
