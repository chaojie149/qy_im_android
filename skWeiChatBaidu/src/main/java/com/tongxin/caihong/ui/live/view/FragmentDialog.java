package com.tongxin.caihong.ui.live.view;

import static com.tongxin.caihong.R.id.negtive;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.AvatarHelper;


/**
 * 直播间-用户详情
 */
public class FragmentDialog extends DialogFragment {
    /**
     * 管理
     */
    private TextView managerTv;

    /**
     * 头像
     */
    private ImageView imageIv;

    /**
     * 昵称
     */
    private TextView titleTv;


    /**
     * 描述
     */
    private TextView messageTv;

    /**
     * 取消和确认按钮
     */
    private Button negtiveBn, positiveBn;

    /**
     * 按钮之间的分割线
     */
    private View columnLineView;

    /**
     * 显示内容
     */
    private int type = 3;
    private int imageResId = -1;
    private String title;
    private String message;
    private String positive, negative;

    /**
     * 底部是否只有一个按钮
     */
    private boolean isSingle = false;

    private Dialog dialog;

    public static final FragmentDialog newInstance(int type, int imageResId, String title, String message, boolean isSingle, String positive, String negative,
                                                   OnClickBottomListener onClickBottomListener) {
        FragmentDialog fragment = new FragmentDialog();
        Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        bundle.putInt("imageResId", imageResId);
        bundle.putString("title", title);
        bundle.putString("message", message);
        bundle.putBoolean("isSingle", isSingle);
        bundle.putString("positive", positive);
        bundle.putString("negative", negative);
        fragment.onClickBottomListener = onClickBottomListener;
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.common_dialog_layout, null, false);
        // 使用不带Theme的构造器, 获得的dialog边框距离屏幕仍有几毫米的缝隙
        initDialogStyle(rootView);
        initView(rootView);
        initEvent();
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }

    private void initDialogStyle(View view) {
        dialog = new Dialog(getActivity(), R.style.CustomDialog);
        // 设置Content前设定,(自定义标题,当需要自定义标题时必须指定)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        // 外部点击取消
        dialog.setCanceledOnTouchOutside(true);
        // 设置宽度为屏宽, 靠近屏幕底部。
        Window window = dialog.getWindow();
        window.setWindowAnimations(R.style.Buttom_Popwindow);
        WindowManager.LayoutParams lp = window.getAttributes();
        // 中间显示
        lp.gravity = Gravity.CENTER;
        //  宽度持平
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);
    }

    private void initView(View rootView) {
        // get data
        Bundle args = getArguments();
        if (args == null)
            return;
        type = args.getInt("type");
        imageResId = args.getInt("imageResId");
        title = args.getString("title");
        message = args.getString("message");
        isSingle = args.getBoolean("isSingle");
        positive = args.getString("positive");
        negative = args.getString("negative");
        managerTv = (TextView) rootView.findViewById(R.id.set_manager);
        imageIv = (ImageView) rootView.findViewById(R.id.image);
        titleTv = (TextView) rootView.findViewById(R.id.title);
        messageTv = (TextView) rootView.findViewById(R.id.message);
        positiveBn = (Button) rootView.findViewById(R.id.positive);
        columnLineView = rootView.findViewById(R.id.column_line);
        negtiveBn = (Button) rootView.findViewById(negtive);
    }

    /**
     * 初始化界面控件的显示数据
     */
    private void refreshView() {
        // “管理”是否出现
        if (type == 1 || type == 2) {
            managerTv.setVisibility(View.VISIBLE);
        } else {
            managerTv.setVisibility(View.GONE);
        }
        AvatarHelper.getInstance().displayAvatar(String.valueOf(imageResId), imageIv, false);

        if (!TextUtils.isEmpty(title)) {
            titleTv.setText(title);
            titleTv.setVisibility(View.VISIBLE);
        } else {
            titleTv.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(message)) {
            messageTv.setText(message);
        } else {
            positiveBn.setText("Ta好像忘记写签名了...");
        }
        /**
         * 只显示一个按钮的时候隐藏取消按钮，回掉只执行确定的事件
         */
        if (isSingle) {
            negtiveBn.setVisibility(View.GONE);
            columnLineView.setVisibility(View.GONE);
        } else {
            negtiveBn.setVisibility(View.VISIBLE);
            columnLineView.setVisibility(View.VISIBLE);
        }
        if (!TextUtils.isEmpty(positive)) {
            positiveBn.setText(positive);
        } else {
            positiveBn.setText("主页");
        }
        if (!TextUtils.isEmpty(negative)) {
            negtiveBn.setText(negative);
        } else {
            negtiveBn.setText("朋友圈");
        }
    }

    /**
     * 初始化界面的确定和取消监听器
     */
    private void initEvent() {
        //设置管理按钮被点击后，向外界提供监听
        managerTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (onClickBottomListener != null) {
                    onClickBottomListener.onManagerClick();
                }
            }
        });

        //设置确定按钮被点击后，向外界提供监听
        positiveBn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (onClickBottomListener != null) {
                    onClickBottomListener.onPositiveClick();
                }
            }
        });
        //设置取消按钮被点击后，向外界提供监听
        negtiveBn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (onClickBottomListener != null) {
                    onClickBottomListener.onNegativeClick();
                }
            }
        });
    }

    public OnClickBottomListener onClickBottomListener;

    public interface OnClickBottomListener {
        void onManagerClick();

        void onPositiveClick();

        void onNegativeClick();
    }
}
