package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.bean.message.MucRoom;
import com.tongxin.caihong.ui.me.FontSizeActivity;
import com.tongxin.caihong.ui.message.multi.NoticeListActivity;
import com.tongxin.caihong.util.ScreenUtil;

public class GroupNoticeDialog extends Dialog {
    private Context context;
    private int role;
    private String roomId;
    private MucRoom.Notice notice;

    public GroupNoticeDialog(Context context, int role, String roomId, MucRoom.Notice notice) {
        super(context, R.style.BottomDialog);
        this.context = context;
        this.role = role;
        this.roomId = roomId;
        this.notice = notice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_notice_dialog);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        TextView tvNotice = findViewById(R.id.tvNotice);
        tvNotice.setMovementMethod(ScrollingMovementMethod.getInstance());
        // 修改字体功能
        int size = FontSizeActivity.getFontSize(getContext());
        tvNotice.setTextSize(size);
        tvNotice.setText(notice.getText());
        findViewById(R.id.llNoticeHistory).setOnClickListener(view -> NoticeListActivity.start(context, role, roomId));
        findViewById(R.id.tvIKnow).setOnClickListener(view -> dismiss());

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.9);
        lp.gravity = Gravity.CENTER;
        o.setAttributes(lp);
    }
}
