package com.tongxin.caihong.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.tongxin.caihong.bean.AllStationsNotice;
import com.tongxin.caihong.helper.AvatarHelper;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.ui.tool.SingleImagePreviewActivity;
import com.tongxin.caihong.util.ScreenUtil;
import com.tongxin.caihong.AppConstant;
import com.tongxin.caihong.R;

public class AllStationsNoticeDialog extends Dialog {
    private Context context;
    private AllStationsNotice allStationsNotice;

    public AllStationsNoticeDialog(Context context, AllStationsNotice allStationsNotice) {
        super(context, R.style.BottomDialog);
        this.context = context;
        this.allStationsNotice = allStationsNotice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_stations_notice_dialog);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        TextView tvNotice = findViewById(R.id.tvNotice);
        tvNotice.setMovementMethod(ScrollingMovementMethod.getInstance());

        ImageView ivNotice = findViewById(R.id.ivNotice);
        ViewGroup.LayoutParams params = ivNotice.getLayoutParams();
        params.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.8) - ScreenUtil.dip2px(context, 48);
        params.height = params.width / 2;
        ivNotice.setLayoutParams(params);

        ButtonColorChange.colorChangeMore(context, findViewById(R.id.btnIKnow));

        tvNotice.setText(allStationsNotice.getContent());
        if (allStationsNotice.getType() == 1) {
            ivNotice.setVisibility(View.VISIBLE);
            AvatarHelper.getInstance().displayUrl(allStationsNotice.getPicturn(), ivNotice);
        } else {
            ivNotice.setVisibility(View.GONE);
        }
        findViewById(R.id.llClose).setOnClickListener(view -> dismiss());
        ivNotice.setOnClickListener(view -> {
            Intent intent = new Intent(context, SingleImagePreviewActivity.class);
            intent.putExtra(AppConstant.EXTRA_IMAGE_URI, allStationsNotice.getPicturn());
            context.startActivity(intent);
        });
        findViewById(R.id.btnIKnow).setOnClickListener(view -> dismiss());

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        lp.width = (int) (ScreenUtil.getScreenWidth(getContext()) * 0.8);
        lp.gravity = Gravity.CENTER;
        o.setAttributes(lp);
    }
}
