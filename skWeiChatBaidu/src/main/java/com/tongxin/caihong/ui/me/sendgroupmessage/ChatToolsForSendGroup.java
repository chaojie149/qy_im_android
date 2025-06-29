package com.tongxin.caihong.ui.me.sendgroupmessage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.tongxin.caihong.R;

public class ChatToolsForSendGroup extends RelativeLayout {

    public ChatToolsForSendGroup(Context context) {
        super(context);
        init(context);
    }

    public ChatToolsForSendGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatToolsForSendGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context mContext) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(R.layout.chat_tools_view_for_sg, this);
    }

    public void setOnToolsClickListener(OnClickListener listener) {
        findViewById(R.id.im_photo_tv).setOnClickListener(listener);
        findViewById(R.id.im_camera_tv).setOnClickListener(listener);
        findViewById(R.id.im_video_tv).setOnClickListener(listener);
        findViewById(R.id.im_loc_tv).setOnClickListener(listener);
        findViewById(R.id.im_card_tv).setOnClickListener(listener);
        findViewById(R.id.im_file_tv).setOnClickListener(listener);
        findViewById(R.id.im_collection_tv).setOnClickListener(listener);
    }
}
