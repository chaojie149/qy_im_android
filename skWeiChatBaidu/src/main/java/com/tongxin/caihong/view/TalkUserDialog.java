package com.tongxin.caihong.view;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.call.talk.TalkUserAdapter;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.util.ScreenUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TalkUserDialog extends Dialog {
    private TalkUserAdapter.Item item;

    public TalkUserDialog(Context context, TalkUserAdapter.Item item) {
        super(context, R.style.BottomDialog);
        this.item = item;
    }

    public static TalkUserDialog show(Context ctx, TalkUserAdapter.Item item) {
        TalkUserDialog dialog = new TalkUserDialog(ctx, item);
        try {
            dialog.show();
        } catch (Exception ignored) {
        }
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_talk_user);
        initView();
    }

    private void initView() {
        TextView tvLastRequest = findViewById(R.id.tvLastRequest);
        TextView tvTalkLength = findViewById(R.id.tvTalkLength);
        TextView tvName = findViewById(R.id.tvName);
        ImageView ivHead = findViewById(R.id.ivHead);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
        AvatarHelper.getInstance().displayAvatar(item.name, item.userId, ivHead, true);
        tvName.setText(item.name);
        if (item.requestTime == 0) {
            tvLastRequest.setText("");
        } else {
            tvLastRequest.setText(sdfTime.format(new Date((long) (item.requestTime))));
        }
        if (item.talkLength == 0) {
            tvTalkLength.setText("");
        } else {
            long duration = (long) (double) (item.talkLength);
            tvTalkLength.setText(String.format(Locale.CHINA, "%02d:%02d:%02d",
                    duration / 3600, (duration % 3600) / 60, (duration % 60)));
        }

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.6);
        lp.gravity = Gravity.CENTER;
        o.setAttributes(lp);
        o.setDimAmount(0.2f);
    }
}
