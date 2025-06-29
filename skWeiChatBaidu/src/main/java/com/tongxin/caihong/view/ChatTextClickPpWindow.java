package com.tongxin.caihong.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.event.EventNotifyByTag;
import com.tongxin.caihong.bean.message.ChatMessage;
import com.tongxin.caihong.bean.message.XmppMessage;
import com.tongxin.caihong.course.ChatRecordHelper;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.helper.RecognizerHelper;
import com.tongxin.caihong.helper.TranslateHelper;
import com.tongxin.caihong.ui.base.CoreManager;
import com.tongxin.caihong.util.Constants;
import com.tongxin.caihong.util.PreferenceUtils;
import com.tongxin.caihong.util.TimeUtils;
import com.tongxin.caihong.view.selectable.SelectableTextHelper;
import com.tongxin.caihong.xmpp.listener.ChatMessageListener;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;


/**
 * 聊天消息长按事件
 */
public class ChatTextClickPpWindow extends PopupWindow implements SelectableTextHelper.IPopupWindow {
    public static final String DISABLE_RECORD = "DISABLE_RECORD";
    private final String toUserId;
    private View mMenuView;
    private TextView tvTranslation;
    private TextView tvRecognizer;
    private TextView tvCopy;
    private TextView tvRelay;
    private TextView tvCollection;// 存表情
    private TextView tvCollectionOther; // 收藏其他类型的消息
    private TextView tvBack;
    private TextView tvReplay;
    private TextView tvDel;
    private TextView tvMoreSelected;
    // 开始 & 停止录制
    private TextView tvRecord;
    private TextView tvSpeaker;
    private int mWidth, mHeight;
    private boolean isGroup;
    private boolean isDevice;
    private int mRole;
    // 点击位置绝对横坐标，用于手动计算窗口偏移，
    private int xoff;
    private String messageId;
    private int messageRole;

    public ChatTextClickPpWindow(Context context, View.OnClickListener listener,
                                 final ChatMessage chatMessage, final String toUserId, boolean course,
                                 boolean group, boolean device, int role, Integer messageRole) {
        super(context);
        if (messageRole == null) {
            this.messageRole = 3;
        } else {
            this.messageRole = messageRole;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        mMenuView = inflater.inflate(R.layout.item_chat_long_click, null);
        // mMenuView = inflater.inflate(R.layout.item_chat_long_click_list_style, null);

        this.isGroup = group;
        this.isDevice = device;
        this.mRole = role;
        this.toUserId = toUserId;
        messageId = chatMessage.getPacketId();

        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mMenuView.measure(w, h);
        // 获取PopWindow宽和高
        mHeight = mMenuView.getMeasuredHeight();
        mWidth = mMenuView.getMeasuredWidth();

        tvTranslation = (TextView) mMenuView.findViewById(R.id.item_chat_translation_tv);
        tvRecognizer = (TextView) mMenuView.findViewById(R.id.item_chat_recognizer_tv);
        tvCopy = (TextView) mMenuView.findViewById(R.id.item_chat_copy_tv);
        tvRelay = (TextView) mMenuView.findViewById(R.id.item_chat_relay_tv);
        tvCollection = (TextView) mMenuView.findViewById(R.id.item_chat_collection_tv);
        tvCollectionOther = (TextView) mMenuView.findViewById(R.id.collection_other);
        tvBack = (TextView) mMenuView.findViewById(R.id.item_chat_back_tv);
        tvReplay = (TextView) mMenuView.findViewById(R.id.item_chat_replay_tv);
        tvDel = (TextView) mMenuView.findViewById(R.id.item_chat_del_tv);
        tvMoreSelected = (TextView) mMenuView.findViewById(R.id.item_chat_more_select);
        tvRecord = (TextView) mMenuView.findViewById(R.id.item_chat_record);
        tvSpeaker = (TextView) mMenuView.findViewById(R.id.item_chat_speaker);

        //设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        //设置SelectPicPopupWindow弹出窗体的宽
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        //之前是0.9 现在改成1
        mWidth = (int) (manager.getDefaultDisplay().getWidth() * 1);
        this.setWidth(mWidth);
        //	 this.setWidth(ViewPiexlUtil.dp2px(context,200));
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Buttom_Popwindow);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0000000000);
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        /*mMenuView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    } else if (y > bottom) {
                        dismiss();
                    }
                }
                return true;
            }
        });*/

        hideButton(chatMessage, course);
        paging();
        // 设置按钮监听
        tvTranslation.setOnClickListener(listener);
        tvRecognizer.setOnClickListener(listener);
        tvCopy.setOnClickListener(listener);
        tvRelay.setOnClickListener(listener);
        tvCollection.setOnClickListener(listener);
        tvCollectionOther.setOnClickListener(listener);
        tvBack.setOnClickListener(listener);
        tvReplay.setOnClickListener(listener);
        tvDel.setOnClickListener(listener);
        tvMoreSelected.setOnClickListener(listener);
        tvRecord.setOnClickListener(v -> {
            if (ChatRecordHelper.instance().getState() == ChatRecordHelper.STATE_UN_RECORD) {
                // 未录制 --> 开始录制
                ChatRecordHelper.instance().start(chatMessage);
                String tip;
                if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
                    tip = context.getString(R.string.course_support_type)
                            + context.getString(R.string.dont_support_tip, context.getString(R.string.record_course_tip));
                } else {
                    tip = context.getString(R.string.course_support_type);
                }
                DialogHelper.tipDialog(context, tip);
            } else {
                // 停止录制
                ChatRecordHelper.instance().stop(chatMessage, toUserId);
            }
            dismiss();
        });
    }

    private void paging() {
        LinearLayout layout = mMenuView.findViewById(R.id.item_chat_text_ll);
        layout.addOnLayoutChangeListener(new PagingListener(layout));
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getShowMessageId() {
        return messageId;
    }

    /*
    根据消息类型隐藏部分操作
     */
    private void hideButton(ChatMessage message, boolean course) {
        //隐藏翻译
        tvTranslation.setVisibility(View.GONE);
       /* if (!TranslateHelper.needTranslate(message)) {
            tvTranslation.setVisibility(View.GONE);
        }*/
        //转文字
        tvRecognizer.setVisibility(View.GONE);
/*
        if (!RecognizerHelper.needRecognizer(message)) {
            tvRecognizer.setVisibility(View.GONE);
        }
*/
        if (TextUtils.equals(message.getObjectId(), DISABLE_RECORD)
                || message.getIsReadDel()) {
            // 加好友前的打招呼消息和回话消息不能录课件，
            // 阅后即焚消息不能录课件
            tvRecord.setVisibility(View.GONE);
        }

        // 仅语音显示，扬声器、听筒切换 && 仅限聊天界面
        if (message.getType() == XmppMessage.TYPE_VOICE
                && !TextUtils.equals(MyApplication.IsRingId, "Empty")) {
            tvSpeaker.setVisibility(View.VISIBLE);
        }
        boolean isSpeaker = PreferenceUtils.getBoolean(MyApplication.getContext(),
                Constants.SPEAKER_AUTO_SWITCH + CoreManager.requireSelf(MyApplication.getContext()).getUserId(), true);
        tvSpeaker.setText(isSpeaker ? MyApplication.getContext().getString(R.string.chat_earpiece) : MyApplication.getContext().getString(R.string.chat_speaker));
        tvSpeaker.setOnClickListener(v -> {
            PreferenceUtils.putBoolean(MyApplication.getContext(),
                    Constants.SPEAKER_AUTO_SWITCH + CoreManager.requireSelf(MyApplication.getContext()).getUserId(), !isSpeaker);
            // 通知聊天界面刷新
            EventBus.getDefault().post(new EventNotifyByTag(EventNotifyByTag.Speak));
            dismiss();
        });

        int type = message.getType();
        // 文本类型可复制
        if (type != XmppMessage.TYPE_TEXT && type != XmppMessage.TYPE_REPLAY) {
            tvCopy.setVisibility(View.GONE);
        } else {
            tvCopy.setVisibility(View.VISIBLE);
        }

        // 图片类型可存表情
        if (type == XmppMessage.TYPE_IMAGE) {
            tvCollection.setVisibility(View.VISIBLE);
        } else {
            tvCollection.setVisibility(View.GONE);
        }

        // 文本、图片、语音、视频、文件类型可收藏
        // 加密文件不可收藏，
        if (type == XmppMessage.TYPE_TEXT || type == XmppMessage.TYPE_REPLAY || type == XmppMessage.TYPE_IMAGE || type == XmppMessage.TYPE_VOICE || type == XmppMessage.TYPE_VIDEO || (type == XmppMessage.TYPE_FILE && TextUtils.isEmpty(message.getObjectId())) || type == XmppMessage.TYPE_LINK) {
            if (type != XmppMessage.TYPE_LINK && type != XmppMessage.TYPE_TEXT && type != XmppMessage.TYPE_REPLAY && TextUtils.isEmpty(message.getContent())) {
                // 图片、语音、视频、文件类型如果content为空，我们当做还未上传成功，隐藏收藏
                tvCollectionOther.setVisibility(View.GONE);
            } else {
                tvCollectionOther.setVisibility(View.VISIBLE);
            }
        } else {
            tvCollectionOther.setVisibility(View.GONE);
        }

        // 撤回
        if (message.getMessageState() == ChatMessageListener.MESSAGE_SEND_ING
                || message.getMessageState() == ChatMessageListener.MESSAGE_SEND_FAILED) {
            // 消息发送中 || 消息发送失败，不支持撤回
            tvBack.setVisibility(View.GONE);
        } else {
            if (isGroup) {
                if ((message.isMySend() || mRole == 1 || (mRole == 2 && messageRole != 1 && messageRole != 2)) && type != XmppMessage.TYPE_RED) {
                    tvBack.setVisibility(View.VISIBLE);
                } else {
                    tvBack.setVisibility(View.GONE);
                }
            } else {
                if (!message.isMySend()
                        || type == XmppMessage.TYPE_RED
                        || type == XmppMessage.TYPE_TRANSFER
                        || ((type >= XmppMessage.TYPE_IS_CONNECT_VOICE && type <= XmppMessage.TYPE_EXIT_VOICE))
                        || type == XmppMessage.TYPE_SECURE_LOST_KEY) {
                    // 该条消息 NotSendByMe || 红包 || 音视频通话 类型不可撤回
                    tvBack.setVisibility(View.GONE);
                } else {
                    tvBack.setVisibility(View.VISIBLE);
                /*if (judgeTime(message.getTimeSend())) {
                    // 超时不可撤回
                    tvBack.setVisibility(View.GONE);
                } else {
                    tvBack.setVisibility(View.VISIBLE);
                }*/
                }
            }
        }

        // 红包 || 音视频通话 || 骰子|| 石头剪刀布 类型不可转发
        if (type == XmppMessage.TYPE_RED
                || type == XmppMessage.TYPE_TRANSFER
                || (type >= XmppMessage.TYPE_IS_CONNECT_VOICE && type <= XmppMessage.TYPE_EXIT_VOICE)
                || type == XmppMessage.TYPE_SECURE_LOST_KEY
                || type == XmppMessage.TYPE_DICE
                || type == XmppMessage.TYPE_RPS) {
            tvRelay.setVisibility(View.GONE);
        } else {
            tvRelay.setVisibility(View.VISIBLE);
        }

        // 阅后即焚消息不支持回复
        tvReplay.setVisibility(message.getIsReadDel() ? View.GONE : View.VISIBLE);

        // 当前正在 我的讲课-讲课详情 页面，只保留 复制 与 删除
        if (course
                || type == XmppMessage.TYPE_INVITE_JOIN_ROOM
                || type == XmppMessage.TYPE_MUCFILE_ADD
                || type == XmppMessage.TYPE_NEW_NOTICE) {
            if (type == XmppMessage.TYPE_MUCFILE_ADD
                    || type == XmppMessage.TYPE_NEW_NOTICE) {
                tvCopy.setVisibility(View.GONE);
            }
            tvRelay.setVisibility(View.GONE);
            tvCollection.setVisibility(View.GONE);
            tvCollectionOther.setVisibility(View.GONE);
            tvBack.setVisibility(View.GONE);
            tvMoreSelected.setVisibility(View.GONE);
            tvReplay.setVisibility(View.GONE);
            tvRecord.setVisibility(View.GONE);
        }
        if (isGroup) {
            boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(tvCollectionOther.getContext(), Constants.IS_SEND_CARD + toUserId, true);
            if (!isAllowSecretlyChat && (mRole != 1 && mRole != 2)) {
                // 禁止群成员私聊时禁止收藏，以免通过收藏添加好友，
                tvCollectionOther.setVisibility(View.GONE);
            }
        }
        //隐藏录课件
        //tvRecord.setVisibility(View.GONE);
        if (message.getFromUserId().equals(CoreManager.requireSelf(MyApplication.getInstance()).getUserId())) {// 只录制自己的
            ChatRecordHelper.instance().iniText(tvRecord, message);
        } else {
            tvRecord.setVisibility(View.GONE);
        }

        if (isDevice) {
            // 正在‘我的设备’聊天界面 隐藏撤回，讲课
            tvBack.setVisibility(View.GONE);
            tvRecord.setVisibility(View.GONE);
        }
        mMenuView.findViewById(R.id.item_chat_text_ll).setBackgroundResource(R.drawable.bg_chat_text_long);
    }

    /*
    判断当前消息已发送的时间是否超过五分钟
     */
    private boolean judgeTime(long timeSend) {
        return timeSend + 30000 < TimeUtils.sk_time_current_time();
    }

    public void setClickRowX(int xoff) {
        this.xoff = xoff;
    }

    private class PagingListener implements View.OnLayoutChangeListener {
        private final List<View> children;
        private final ViewGroup parent;
        private final View first;
        private final View last;
        int count;
        int from;
        int to;
        private LinearLayout layout;

        public PagingListener(LinearLayout layout) {
            parent = (ViewGroup) layout.getParent();
            this.layout = layout;
            children = new ArrayList<>();
            count = layout.getChildCount();
            for (int i = 1; i < count - 1; i++) {
                View childView = layout.getChildAt(i);
                if (childView.getVisibility() == View.VISIBLE) {
                    children.add(childView);
                }
            }
            from = 0;
            to = children.size();
            first = layout.getChildAt(0);
            last = layout.getChildAt(count - 1);
            first.setOnClickListener(v -> {
                layout.setVisibility(View.INVISIBLE);
                layout.post(() -> {
                    setMarginLeft(layout, 0);
                });
                to = from;
                from = 0;
                updatePaging();
            });
            last.setOnClickListener(v -> {
                layout.setVisibility(View.INVISIBLE);
                layout.post(() -> {
                    setMarginLeft(layout, 0);
                });
                from = to;
                to = count;
                updatePaging();
            });
            // 手动计算偏移可能出现闪烁，所以先隐藏，设置偏移再显示，
            layout.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (right - left > parent.getWidth() && from < to) {
                // 装不下就删除一个，最小留一个，
                --to;
                updatePaging();
            } else {
                int max = (parent.getRight() - parent.getLeft()) - (right - left);
                int marginLeft = (xoff - parent.getLeft()) - (right - left) / 2;
                if (max > 0 && marginLeft > 0) {
                    layout.post(() -> {
                        setMarginLeft(layout, Math.min(marginLeft, max));
                    });
                }
                layout.post(() -> {
                    layout.setVisibility(View.VISIBLE);
                });
            }
        }

        private void setMarginLeft(View view, int left) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.setMargins(left, 0, 0, 0);
            view.setLayoutParams(lp);
            layout.requestLayout();
        }

        private void updatePaging() {
            for (int i = 0; i < children.size(); i++) {
                View childView = children.get(i);
                if (i >= from && i < to) {
                    setShow(childView, true);
                } else {
                    setShow(childView, false);
                }
            }
            if (from == 0) {
                setShow(first, false);
            } else {
                setShow(first, true);
            }
            if (to == count) {
                setShow(last, false);
            } else {
                setShow(last, true);
            }
            layout.requestLayout();
        }

        private void setShow(View v, boolean show) {
            if (show) {
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
            v.requestLayout();
        }
    }
}
