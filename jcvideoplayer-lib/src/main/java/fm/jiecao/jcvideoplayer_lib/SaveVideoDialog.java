package fm.jiecao.jcvideoplayer_lib;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class SaveVideoDialog extends Dialog implements View.OnClickListener {

    private TextView tv1, tv2, tv3;

    private OnSavaVideoDialogClickListener mOnSavaVideoDialogClickListener;

    public SaveVideoDialog(Context context, OnSavaVideoDialogClickListener mOnSavaVideoDialogClickListener) {
        super(context, R.style.BottomDialog);
        this.mOnSavaVideoDialogClickListener = mOnSavaVideoDialogClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_sava_video);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);

        if (!mOnSavaVideoDialogClickListener.enableShare()) {
            tv2.setVisibility(View.GONE);
            tv3.setVisibility(View.GONE);
        }

        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
        findViewById(R.id.tvCancel).setOnClickListener(this);

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        // x/y坐标
        // lp.x = 100;
        // lp.y = 100;
        lp.width = ScreenUtil.getScreenWidth(getContext());
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        int id = v.getId();
        if (id == R.id.tv1) {
            mOnSavaVideoDialogClickListener.tv1Click();
        } else if (id == R.id.tv2) {
            mOnSavaVideoDialogClickListener.tv2Click();
        } else if (id == R.id.tv3) {
            mOnSavaVideoDialogClickListener.tv3Click();
        }
    }

    public interface OnSavaVideoDialogClickListener {
        void tv1Click();

        void tv2Click();

        void tv3Click();

        boolean enableShare();
    }
}
