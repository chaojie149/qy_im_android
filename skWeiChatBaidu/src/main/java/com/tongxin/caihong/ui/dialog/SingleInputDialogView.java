package com.tongxin.caihong.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tongxin.caihong.R;
import com.tongxin.caihong.helper.DialogHelper;
import com.tongxin.caihong.ui.dialog.base.BaseDialog;
import com.tongxin.caihong.ui.tool.ButtonColorChange;
import com.tongxin.caihong.util.DeviceInfoUtil;

/**
 * Created by Administrator on 2016/4/21.
 */
public class SingleInputDialogView extends BaseDialog {
    private TextView tvTitle;
    private EditText etContent;
    private Button btnSure;
    private String title;
    private String text;
    private String hint;
    private InputFilter[] inputFilters;
    private OnConfirmListener onConfirmListener;

    {
        RID = R.layout.dialog_single_input;
    }

    public SingleInputDialogView(Activity activity
            , String title
            , String text, String hint
            , OnConfirmListener onConfirmListener) {
        this(activity, title, text, hint, null, onConfirmListener);
    }

    public SingleInputDialogView(Activity activity
            , String title
            , String text, String hint
            , InputFilter[] inputFilters
            , OnConfirmListener onConfirmListener) {
        mActivity = activity;
        this.title = title;
        this.text = text;
        this.hint = hint;
        this.inputFilters = inputFilters;
        this.onConfirmListener = onConfirmListener;
        initView();
    }

    protected void initView() {
        super.initView();
        tvTitle = mView.findViewById(R.id.tvTitle);
        etContent = mView.findViewById(R.id.etContent);
        etContent.setFilters(new InputFilter[]{DialogHelper.mExpressionFilter});
        btnSure = mView.findViewById(R.id.btnSure);
        ButtonColorChange.colorChange(mActivity, btnSure);

        tvTitle.setText(TextUtils.isEmpty(title) ? getString(R.string.app_name) : title);
        if (!TextUtils.isEmpty(text)) {
            etContent.setText(text);
            etContent.setSelection(etContent.getText().toString().length());
        }
        if (!TextUtils.isEmpty(hint)) {
            etContent.setHint(hint);
        }
        // etContent.setMaxLines(maxLines);
        // etContent.setLines(lines);
        etContent.setFilters(new InputFilter[]{DialogHelper.mExpressionFilter});
        if (inputFilters != null) {
            etContent.setFilters(inputFilters);
        }
        btnSure.setOnClickListener(view -> click());
    }

    public void setInputType(int inputType) {
        etContent.setInputType(inputType);
    }

    private void click() {
        if (onConfirmListener == null || onConfirmListener.onConfirm(etContent.getText().toString().trim())) {
            mDialog.dismiss();
        } else if (DeviceInfoUtil.isEmuiRom() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 针对华为安卓9以上，输入错误情况判断当前是密码输入就隐藏输入法，否则华为安全键盘会挡住toast,
            int inputType = etContent.getInputType();
            final int variation =
                    inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
            boolean isPassword = variation
                    == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
                    || variation
                    == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                    || variation
                    == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);
            boolean isVisiblePassword = variation
                    == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            if (isPassword || isVisiblePassword) {
                InputMethodManager im = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(etContent.getWindowToken(), 0);
            }
        }
    }

    /**
     * 用于实现阻止对框关闭，
     */
    public interface OnConfirmListener {
        /**
         * @return 返回true表示关闭对话框，false不关闭，
         */
        boolean onConfirm(String text);
    }
}
